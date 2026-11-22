package com.example.ui.viewmodel

import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.MaxStudioStorage
import com.example.data.WorkspaceFolderInfo
import com.example.model.BackendMode
import com.example.model.FirebaseJsonConfig
import com.example.model.LibraryDep
import com.example.model.MaxProject
import com.example.model.StarterTemplates
import com.example.model.ThemeColors
import com.example.ui.theme.ThemeMode
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

class ProjectsViewModel(
    private val storage: MaxStudioStorage,
) : ViewModel() {

    val projectsState: StateFlow<UiState<List<MaxProject>>> = storage.observeProjects()
        .map<List<MaxProject>, UiState<List<MaxProject>>> { UiState.Success(it) }
        .catch { error ->
            if (error is CancellationException) throw error
            Log.e(TAG, "Error loading local .maxstudio projects", error)
            emit(UiState.Error(error.message ?: "Failed to read .maxstudio storage"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading,
        )

    val globalLibraries: StateFlow<List<LibraryDep>> = storage.globalLibraries
    val globalFirebase: StateFlow<FirebaseJsonConfig?> = storage.globalFirebase
    val folderSummary: StateFlow<List<WorkspaceFolderInfo>> = storage.folderSummary
    val linkedSafTreeUri: StateFlow<String?> = storage.linkedSafTreeUri
    val allFilesGranted: StateFlow<Boolean> = storage.allFilesGranted
    val installUnknownGranted: StateFlow<Boolean> = storage.installUnknownGranted
    val storageRootPath: String get() = storage.rootDisplayPath

    private val _themeMode = MutableStateFlow(ThemeMode.SYSTEM)
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    private val _actionMessage = MutableStateFlow<String?>(null)
    val actionMessage: StateFlow<String?> = _actionMessage.asStateFlow()

    private val _isCreating = MutableStateFlow(false)
    val isCreating: StateFlow<Boolean> = _isCreating.asStateFlow()

    fun setThemeMode(mode: ThemeMode) {
        _themeMode.value = mode
    }

    fun clearActionMessage() {
        _actionMessage.value = null
    }

    fun hasAllFilesAccess(): Boolean = storage.hasAllFilesAccess()

    fun buildAllFilesPermissionIntent(): Intent = storage.buildAllFilesPermissionIntent()

    fun hasInstallUnknownAppsPermission(): Boolean = storage.hasInstallUnknownAppsPermission()

    fun buildInstallUnknownAppsIntent(): Intent = storage.buildInstallUnknownAppsIntent()

    fun refreshStorageAccessAndSync() {
        storage.refreshStorageAccessAndSync()
    }

    fun createProject(
        appName: String,
        projectName: String,
        packageName: String,
        backendMode: BackendMode,
        themeColors: ThemeColors,
        versionCode: Int,
        versionName: String,
        logoUri: String,
        logoCornerRadiusDp: Int = 20,
        templateKey: String = "blank",
        onCreated: (MaxProject) -> Unit,
    ) {
        if (_isCreating.value) return
        viewModelScope.launch {
            _isCreating.value = true
            val base = when (templateKey) {
                "counter" -> StarterTemplates.counterPro().copy(
                    appName = appName.ifBlank { "MAX COUNTER" },
                    projectName = projectName.ifBlank { "COUNTER_PRO" },
                    packageName = packageName.ifBlank { "com.maxstudio.counterpro" },
                    backendMode = backendMode,
                    themeColors = themeColors,
                    versionCode = versionCode,
                    versionName = versionName,
                    logoUri = logoUri,
                    logoCornerRadiusDp = logoCornerRadiusDp,
                )
                else -> StarterTemplates.blankProject(
                    appName = appName.ifBlank { "My App" },
                    projectName = projectName.ifBlank { "NewProject" },
                    packageName = packageName.ifBlank { "com.my.newproject" },
                    backendMode = backendMode,
                    themeColors = themeColors,
                    versionCode = versionCode,
                    versionName = versionName,
                    logoUri = logoUri,
                    logoCornerRadiusDp = logoCornerRadiusDp,
                )
            }
            val result = storage.createProject(base)
            _isCreating.value = false
            result.onSuccess { created ->
                _actionMessage.value = "Saved to ${storage.rootDisplayPath}/data/${created.numericId}"
                onCreated(created)
            }.onFailure { err ->
                _actionMessage.value = err.localizedMessage ?: "Failed to create project"
            }
        }
    }

    fun updateProjectDetails(
        existing: MaxProject,
        appName: String,
        projectName: String,
        packageName: String,
        backendMode: BackendMode,
        themeColors: ThemeColors,
        versionCode: Int,
        versionName: String,
        logoUri: String,
        logoCornerRadiusDp: Int,
        onSaved: (MaxProject) -> Unit,
    ) {
        if (_isCreating.value) return
        viewModelScope.launch {
            _isCreating.value = true
            val updated = existing.copy(
                appName = appName.trim().ifBlank { existing.appName }.take(30),
                projectName = projectName.trim().replace(Regex("[^a-zA-Z0-9_ ]"), "").ifBlank { existing.projectName }.take(30),
                packageName = packageName.trim().ifBlank { existing.packageName }.take(80),
                backendMode = backendMode,
                themeColors = themeColors,
                versionCode = versionCode.coerceAtLeast(1),
                versionName = versionName.trim().ifBlank { "1.0" },
                logoUri = logoUri,
                logoCornerRadiusDp = logoCornerRadiusDp.coerceIn(0, 44),
            ).let { p ->
                if (p.backendMode != com.example.model.BackendMode.JAVA_ONLY) {
                    p.copy(javaCode = com.example.codegen.CodeGenerator.generateJavaFromProject(p))
                } else p
            }
            val result = storage.updateProject(updated)
            _isCreating.value = false
            result.onSuccess {
                _actionMessage.value = "Updated ${updated.appName} (#${updated.numericId})"
                onSaved(updated)
            }.onFailure { err ->
                _actionMessage.value = err.localizedMessage ?: "Failed to update project"
            }
        }
    }

    fun copyPickedLogo(uri: Uri, onSaved: (String) -> Unit) {
        viewModelScope.launch {
            storage.copyImageToResources(uri, prefix = "icon")
                .onSuccess { path ->
                    onSaved(path)
                    _actionMessage.value = "App logo saved in .maxstudio/resources/"
                }
                .onFailure { err ->
                    _actionMessage.value = err.localizedMessage ?: "Failed saving logo"
                }
        }
    }

    fun duplicateProject(project: MaxProject) {
        viewModelScope.launch {
            val copyName = ("${project.appName} Copy").take(30)
            val copy = project.copy(
                id = "",
                numericId = 0,
                appName = copyName,
                projectName = "${project.projectName}_Copy",
            )
            storage.createProject(copy)
                .onSuccess { _actionMessage.value = "Duplicated as #${it.numericId}" }
                .onFailure { err -> _actionMessage.value = err.localizedMessage ?: "Duplicate failed" }
        }
    }

    fun deleteProject(project: MaxProject) {
        viewModelScope.launch {
            storage.deleteProject(project.id)
                .onSuccess { _actionMessage.value = "Deleted \"${project.appName}\" (backup kept in .mss)" }
                .onFailure { err -> _actionMessage.value = err.localizedMessage ?: "Delete failed" }
        }
    }

    fun extractProjectToMss(project: MaxProject, onReadyForSaf: ((MaxProject) -> Unit)? = null) {
        viewModelScope.launch {
            storage.extractProjectToMss(project)
                .onSuccess { file ->
                    _actionMessage.value = "Extracted: ${file.name} in .maxstudio/backups/"
                    onReadyForSaf?.invoke(project)
                }
                .onFailure { err ->
                    _actionMessage.value = err.localizedMessage ?: "Failed to extract .mss"
                }
        }
    }

    fun exportMssToSafUri(project: MaxProject, destUri: Uri) {
        viewModelScope.launch {
            storage.writeMssToUri(project, destUri)
                .onSuccess { _actionMessage.value = "Saved ${project.projectName}.mss to selected folder!" }
                .onFailure { err -> _actionMessage.value = err.localizedMessage ?: "Export failed" }
        }
    }

    fun importMssFromUri(uri: Uri) {
        viewModelScope.launch {
            storage.importMssFromUri(uri)
                .onSuccess { restored ->
                    _actionMessage.value = "Restored ${restored.appName} (#${restored.numericId})"
                }
                .onFailure { err ->
                    _actionMessage.value = err.localizedMessage ?: "Failed to import .mss"
                }
        }
    }

    fun importSwbFromUri(uri: Uri) {
        viewModelScope.launch {
            storage.importSwbFromUri(uri)
                .onSuccess { restored ->
                    _actionMessage.value = "Imported Sketchware project: ${restored.appName} (#${restored.numericId})"
                }
                .onFailure { err ->
                    _actionMessage.value = err.localizedMessage ?: "Failed to import Sketchware .swb"
                }
        }
    }

    fun recoverProjectsFromStorage() {
        viewModelScope.launch {
            storage.recoverProjectsFromStorage()
                .onSuccess { count ->
                    _actionMessage.value = if (count > 0) {
                        "Recovered $count project(s) from .maxstudio/"
                    } else {
                        "All projects in .maxstudio/ are already loaded!"
                    }
                }
                .onFailure { err ->
                    _actionMessage.value = err.localizedMessage ?: "Recovery failed"
                }
        }
    }

    fun linkSafFolder(uri: Uri) {
        viewModelScope.launch {
            storage.linkSafFolder(uri)
                .onSuccess { msg -> _actionMessage.value = msg }
                .onFailure { err -> _actionMessage.value = err.localizedMessage ?: "Folder link failed" }
        }
    }

    fun importLibraryArchive(uri: Uri) {
        viewModelScope.launch {
            storage.importLibraryArchive(uri)
                .onSuccess { lib -> _actionMessage.value = "Added library: ${lib.name} (.maxstudio/libs/)" }
                .onFailure { err -> _actionMessage.value = err.localizedMessage ?: "Library import failed" }
        }
    }

    fun importFirebaseJson(uri: Uri) {
        viewModelScope.launch {
            storage.importFirebaseJson(uri)
                .onSuccess { cfg -> _actionMessage.value = "Imported Firebase JSON: ${cfg.projectId}" }
                .onFailure { err -> _actionMessage.value = err.localizedMessage ?: "Invalid google-services.json" }
        }
    }

    fun toggleLibrary(id: String) {
        storage.toggleGlobalLibrary(id)
    }

    private companion object {
        const val TAG = "ProjectsVM"
        const val STOP_TIMEOUT_MILLIS = 5000L
    }
}
