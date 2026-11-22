package com.example.ui.viewmodel

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.codegen.CodeGenerator
import com.example.data.MaxStudioStorage
import com.example.diagnostics.DiagnosticSeverity
import com.example.diagnostics.ProjectDiagnostics
import com.example.diagnostics.ProjectValidationReport
import com.example.model.MaxProject
import com.example.model.ProjectSourceFile
import com.example.model.ThemeColors
import com.example.model.LibraryDep
import com.example.model.FirebaseJsonConfig
import com.example.service.BuildForegroundService
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

enum class SaveStatus {
    SAVED,
    SAVING,
    UNSAVED,
    ERROR,
}

class StudioViewModel(
    private val storage: MaxStudioStorage,
) : ViewModel() {

    private val _project = MutableStateFlow<MaxProject?>(null)
    val project: StateFlow<MaxProject?> = _project.asStateFlow()

    private val _activeFilePath = MutableStateFlow<String>("")
    val activeFilePath: StateFlow<String> = _activeFilePath.asStateFlow()

    private val _openTabs = MutableStateFlow<List<ProjectSourceFile>>(emptyList())
    val openTabs: StateFlow<List<ProjectSourceFile>> = _openTabs.asStateFlow()

    private val _saveStatus = MutableStateFlow(SaveStatus.SAVED)
    val saveStatus: StateFlow<SaveStatus> = _saveStatus.asStateFlow()

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    private val _showBuildSheet = MutableStateFlow(false)
    val showBuildSheet: StateFlow<Boolean> = _showBuildSheet.asStateFlow()

    private val _requestedBuildSubMode = MutableStateFlow(0)
    val requestedBuildSubMode: StateFlow<Int> = _requestedBuildSubMode.asStateFlow()

    val diagnosticsReport: StateFlow<ProjectValidationReport> = _project
        .map { p -> if (p == null) ProjectValidationReport() else ProjectDiagnostics.analyze(p) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, ProjectValidationReport())

    val installUnknownGranted: StateFlow<Boolean> = storage.installUnknownGranted

    private val undoStack = ArrayDeque<MaxProject>()
    private val redoStack = ArrayDeque<MaxProject>()

    private val _canUndo = MutableStateFlow(false)
    val canUndo: StateFlow<Boolean> = _canUndo.asStateFlow()

    private val _canRedo = MutableStateFlow(false)
    val canRedo: StateFlow<Boolean> = _canRedo.asStateFlow()

    private var saveJob: Job? = null

    fun openProject(project: MaxProject) {
        saveJob?.cancel()
        undoStack.clear()
        redoStack.clear()
        _canUndo.value = false
        _canRedo.value = false
        _project.value = project

        val files = if (project.sourceFiles.isNotEmpty()) {
            project.sourceFiles
        } else {
            val defaultFiles = listOf(
                ProjectSourceFile("app/src/main/java/${project.packageName.replace('.', '/')}/MainActivity.java", project.javaCode.ifBlank { CodeGenerator.generateJavaFromProject(project) }, "JAVA"),
                ProjectSourceFile("app/src/main/res/layout/activity_main.xml", project.xmlOverride.ifBlank { CodeGenerator.generateXmlLayout(project.widgets) }, "XML"),
                ProjectSourceFile("app/src/main/res/values/strings.xml", "<resources>\n    <string name=\"app_name\">${project.appName}</string>\n</resources>", "XML"),
                ProjectSourceFile("app/src/main/res/values/colors.xml", "<resources>\n    <color name=\"colorPrimary\">${project.themeColors.colorPrimary}</color>\n</resources>", "XML"),
                ProjectSourceFile("app/src/main/AndroidManifest.xml", CodeGenerator.generateManifestXml(project), "XML"),
                ProjectSourceFile("app/build.gradle.kts", CodeGenerator.generateGradleDependencies(project), "KOTLIN"),
            )
            // Save initial source files back to project
            viewModel.launch { storage.updateProject(project.copy(sourceFiles = defaultFiles)) }
            defaultFiles
        }

        _openTabs.value = listOf(files.first())
        _activeFilePath.value = files.first().path
        _showBuildSheet.value = false
        _saveStatus.value = SaveStatus.SAVED
    }

    private fun viewModelScopeLaunch(block: suspend () -> Unit) {
        viewModelScope.launch { block() }
    }

    private val viewModel = viewModelScope

    fun closeProject() {
        saveJob?.cancel()
        val current = _project.value
        if (current != null && _saveStatus.value == SaveStatus.UNSAVED) {
            viewModelScope.launch { storage.updateProject(current) }
        }
        _project.value = null
        _openTabs.value = emptyList()
        _activeFilePath.value = ""
        _showBuildSheet.value = false
        undoStack.clear()
        redoStack.clear()
        _canUndo.value = false
        _canRedo.value = false
    }

    fun openFile(path: String) {
        val proj = _project.value ?: return
        val file = proj.sourceFiles.firstOrNull { it.path == path } ?: return
        _activeFilePath.value = path
        if (_openTabs.value.none { it.path == path }) {
            _openTabs.value = _openTabs.value + file
        }
    }

    fun closeTab(path: String) {
        val tabs = _openTabs.value
        if (tabs.size <= 1) return // Keep at least one tab open
        val nextTabs = tabs.filterNot { it.path == path }
        _openTabs.value = nextTabs
        if (_activeFilePath.value == path) {
            _activeFilePath.value = nextTabs.last().path
        }
    }

    fun updateSourceFileContent(path: String, newContent: String) {
        mutateProject { current ->
            val nextFiles = current.sourceFiles.map { sf ->
                if (sf.path == path) sf.copy(content = newContent) else sf
            }
            // Keep legacy javaCode and xmlOverride in sync if updating MainActivity or activity_main
            val javaCode = if (path.endsWith("MainActivity.java")) newContent else current.javaCode
            val xmlOverride = if (path.endsWith("activity_main.xml")) newContent else current.xmlOverride
            current.copy(sourceFiles = nextFiles, javaCode = javaCode, xmlOverride = xmlOverride)
        }
        _openTabs.value = _openTabs.value.map { tab ->
            if (tab.path == path) tab.copy(content = newContent) else tab
        }
    }

    fun createNewFile(path: String, content: String) {
        mutateProject { current ->
            if (current.sourceFiles.any { it.path == path }) return@mutateProject current
            val lang = if (path.endsWith(".java")) "JAVA" else if (path.endsWith(".kt")) "KOTLIN" else "XML"
            val newFile = ProjectSourceFile(path, content, lang)
            current.copy(sourceFiles = current.sourceFiles + newFile)
        }
        openFile(path)
    }

    fun deleteSourceFile(path: String) {
        mutateProject { current ->
            current.copy(sourceFiles = current.sourceFiles.filterNot { it.path == path })
        }
        _openTabs.value = _openTabs.value.filterNot { it.path == path }
        if (_activeFilePath.value == path) {
            _activeFilePath.value = _openTabs.value.firstOrNull()?.path ?: ""
        }
    }

    fun setShowBuildSheet(show: Boolean, subMode: Int? = null) {
        if (subMode != null) _requestedBuildSubMode.value = subMode
        _showBuildSheet.value = show
    }

    fun selectBuildSubMode(subMode: Int) {
        _requestedBuildSubMode.value = subMode
    }

    fun clearToast() {
        _toastMessage.value = null
    }

    fun showToast(msg: String) {
        _toastMessage.value = msg
    }

    fun undo() {
        val current = _project.value ?: return
        val previous = undoStack.removeLastOrNull() ?: return
        if (redoStack.size >= 25) redoStack.removeFirst()
        redoStack.addLast(current)
        _canUndo.value = undoStack.isNotEmpty()
        _canRedo.value = redoStack.isNotEmpty()
        _project.value = previous
        scheduleAutoSave(previous)
    }

    fun redo() {
        val current = _project.value ?: return
        val next = redoStack.removeLastOrNull() ?: return
        if (undoStack.size >= 25) undoStack.removeFirst()
        undoStack.addLast(current)
        _canUndo.value = undoStack.isNotEmpty()
        _canRedo.value = redoStack.isNotEmpty()
        _project.value = next
        scheduleAutoSave(next)
    }

    fun saveNow() {
        val current = _project.value ?: return
        saveJob?.cancel()
        viewModelScope.launch {
            _saveStatus.value = SaveStatus.SAVING
            storage.updateProject(current)
                .onSuccess {
                    _saveStatus.value = SaveStatus.SAVED
                    _toastMessage.value = "Saved project to ${storage.rootDisplayPath}"
                }
                .onFailure { _saveStatus.value = SaveStatus.ERROR }
        }
    }

    private fun mutateProject(transform: (MaxProject) -> MaxProject) {
        val current = _project.value ?: return
        if (undoStack.size >= 25) undoStack.removeFirst()
        undoStack.addLast(current)
        redoStack.clear()
        _canUndo.value = true
        _canRedo.value = false
        val transformed = transform(current)
        _project.value = transformed
        scheduleAutoSave(transformed)
    }

    private fun scheduleAutoSave(updated: MaxProject) {
        _saveStatus.value = SaveStatus.UNSAVED
        saveJob?.cancel()
        saveJob = viewModelScope.launch {
            delay(450)
            _saveStatus.value = SaveStatus.SAVING
            storage.updateProject(updated)
                .onSuccess { _saveStatus.value = SaveStatus.SAVED }
                .onFailure { _saveStatus.value = SaveStatus.ERROR }
        }
    }

    // --- Image Manager & Resource Imports ---

    fun importImageIntoProject(uri: Uri) {
        viewModelScope.launch {
            storage.copyImageToResources(uri, prefix = "res_img")
                .onSuccess { path ->
                    mutateProject { current ->
                        current.copy(images = current.images + path)
                    }
                    _toastMessage.value = "Image imported to res/drawable/"
                }
                .onFailure { err ->
                    _toastMessage.value = err.localizedMessage ?: "Image import failed"
                }
        }
    }

    fun removeProjectImage(path: String) {
        mutateProject { current ->
            current.copy(images = current.images.filterNot { it == path })
        }
    }

    fun togglePermission(permission: String) {
        mutateProject { current ->
            val next = if (permission in current.permissions) {
                current.permissions - permission
            } else {
                current.permissions + permission
            }
            current.copy(permissions = next)
        }
    }

    fun updateMetadata(
        appName: String,
        projectName: String,
        packageName: String,
        versionCode: Int,
        versionName: String,
        themeColors: ThemeColors,
    ) {
        mutateProject { current ->
            current.copy(
                appName = appName.trim().ifBlank { current.appName }.take(30),
                projectName = projectName.trim().ifBlank { current.projectName }.take(30),
                packageName = packageName.trim().ifBlank { current.packageName }.take(80),
                versionCode = versionCode.coerceAtLeast(1),
                versionName = versionName.trim().ifBlank { "1.0" },
                themeColors = themeColors,
            )
        }
    }

    fun toggleProjectLibrary(libId: String) {
        mutateProject { current ->
            current.copy(
                libraries = current.libraries.map {
                    if (it.id == libId) it.copy(enabled = !it.enabled) else it
                }
            )
        }
    }

    fun importLibraryIntoProject(uri: Uri) {
        viewModelScope.launch {
            storage.importLibraryArchive(uri)
                .onSuccess { lib ->
                    mutateProject { current ->
                        current.copy(libraries = current.libraries.filterNot { it.id == lib.id } + lib)
                    }
                    _toastMessage.value = "Added ${lib.name} to project!"
                }
                .onFailure { err ->
                    _toastMessage.value = err.localizedMessage ?: "Failed importing library"
                }
        }
    }

    fun importFirebaseJsonIntoProject(uri: Uri) {
        viewModelScope.launch {
            storage.importFirebaseJson(uri)
                .onSuccess { cfg ->
                    mutateProject { current ->
                        current.copy(firebaseConfig = cfg)
                    }
                    _toastMessage.value = "Firebase connected: ${cfg.projectId}"
                }
                .onFailure { err ->
                    _toastMessage.value = err.localizedMessage ?: "Invalid google-services.json"
                }
        }
    }

    // --- Build & Run Pipeline ---

    fun hasInstallUnknownAppsPermission(): Boolean = storage.hasInstallUnknownAppsPermission()

    fun buildInstallUnknownAppsIntent(): Intent = storage.buildInstallUnknownAppsIntent()

    fun buildSystemApkInstallIntent(apkPath: String): Intent? = storage.buildSystemApkInstallIntent(apkPath)

    fun buildShareApkIntent(apkPath: String): Intent? = storage.buildShareApkIntent(apkPath)

    fun saveApkToUri(apkPath: String, destUri: Uri) {
        viewModelScope.launch {
            storage.writeFileToUri(apkPath, destUri)
                .onSuccess { _toastMessage.value = "APK saved to device storage!" }
                .onFailure { err -> _toastMessage.value = err.localizedMessage ?: "Failed saving APK" }
        }
    }

    fun refreshInstallPermission() {
        storage.refreshInstallUnknownOnly()
    }

    fun triggerApkBuild(context: Context) {
        val current = _project.value ?: return
        val report = ProjectDiagnostics.analyze(current)
        _requestedBuildSubMode.value = if (report.hasFatalBlocker) 2 else 0
        _showBuildSheet.value = true
        viewModelScope.launch {
            storage.updateProject(current)
            val mssResult = storage.extractProjectToMss(current)
            val apkResult = if (report.hasFatalBlocker) Result.failure(IllegalStateException("Compile errors found"))
            else storage.exportDebugApkPackage(current)
            val mssPath = mssResult.getOrNull()?.absolutePath ?: ""
            val apkPath = apkResult.getOrNull()?.absolutePath ?: ""
            val diagLines = ArrayList(
                report.items.map { item ->
                    val prefix = when (item.severity) {
                        DiagnosticSeverity.FATAL -> "[FATAL]"
                        DiagnosticSeverity.ERROR -> "[ERROR]"
                        DiagnosticSeverity.WARNING -> "[WARN]"
                    }
                    "$prefix ${item.source}: ${item.message}"
                }
            )
            BuildForegroundService.startBuild(
                context = context,
                appName = current.appName,
                packageName = current.packageName,
                projectName = current.projectName,
                widgetCount = current.widgets.size,
                customViewCount = current.customViews.size,
                libCount = current.libraries.count { it.enabled },
                hasFirebaseJson = current.firebaseConfig != null,
                javaLines = current.javaCode.lines().size,
                apkPath = apkPath,
                mssPath = mssPath,
                fatalCount = report.items.count { it.severity == DiagnosticSeverity.FATAL },
                errorCount = report.errorCount,
                warningCount = report.warningCount,
                diagnosticLines = diagLines,
            )
        }
    }

    fun extractCurrentProjectToMss() {
        val current = _project.value ?: return
        viewModelScope.launch {
            storage.extractProjectToMss(current)
                .onSuccess { file -> _toastMessage.value = "Extracted ${file.name} to backups/" }
                .onFailure { err -> _toastMessage.value = err.localizedMessage ?: "Failed extracting .mss" }
        }
    }

    fun writeCurrentMssToUri(destUri: Uri) {
        val current = _project.value ?: return
        viewModelScope.launch {
            storage.writeMssToUri(current, destUri)
                .onSuccess { _toastMessage.value = "Saved ${current.projectName}.mss!" }
                .onFailure { err -> _toastMessage.value = err.localizedMessage ?: "Export failed" }
        }
    }
}
