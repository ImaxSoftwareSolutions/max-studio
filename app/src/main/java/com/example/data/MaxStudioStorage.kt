package com.example.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.OpenableColumns
import android.provider.Settings
import android.util.Log
import androidx.core.content.FileProvider
import com.example.apk.ApkPackager
import com.example.codegen.CodeGenerator
import com.example.model.BackendMode
import com.example.model.BlockNode
import com.example.model.ComponentDef
import com.example.model.CustomViewDef
import com.example.model.FirebaseJsonConfig
import com.example.model.LibraryDep
import com.example.model.MaxProject
import com.example.model.MoreblockDef
import com.example.model.StarterTemplates
import com.example.model.ThemeColors
import com.example.model.VariableDef
import com.example.model.WidgetNode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

data class WorkspaceFolderInfo(
    val name: String,
    val itemCount: Int,
)

class MaxStudioStorage(private val context: Context) {

    private val appContext = context.applicationContext
    private val ioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mutex = Mutex()
    @Volatile
    private var syncedExternalOnce = false

    // Primary hidden root folder: /storage/emulated/0/.maxstudio (exactly like .sketchware)
    private val primaryExternalHiddenRoot: File =
        File(Environment.getExternalStorageDirectory(), ".maxstudio")

    // Scoped fallback inside Documents & app files until All Files Access is granted
    private val fallbackDocsRoot: File =
        File(File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "MaxStudio"), ".maxstudio")
    private val internalRoot: File = File(appContext.filesDir, ".maxstudio")

    @Volatile
    private var activeRoot: File = if (hasAllFilesAccess()) primaryExternalHiddenRoot else internalRoot

    private val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val _projects = MutableStateFlow<List<MaxProject>>(emptyList())
    private val _globalLibraries = MutableStateFlow<List<LibraryDep>>(LibraryDep.defaultBuiltins)
    private val _globalFirebase = MutableStateFlow<FirebaseJsonConfig?>(null)
    private val _linkedSafTree = MutableStateFlow<String?>(prefs.getString(KEY_SAF_TREE_URI, null))
    private val _folderSummary = MutableStateFlow<List<WorkspaceFolderInfo>>(emptyList())
    private val _allFilesGranted = MutableStateFlow(hasAllFilesAccess())
    private val _installUnknownGranted = MutableStateFlow(hasInstallUnknownAppsPermission())

    val globalLibraries: StateFlow<List<LibraryDep>> = _globalLibraries.asStateFlow()
    val globalFirebase: StateFlow<FirebaseJsonConfig?> = _globalFirebase.asStateFlow()
    val linkedSafTreeUri: StateFlow<String?> = _linkedSafTree.asStateFlow()
    val folderSummary: StateFlow<List<WorkspaceFolderInfo>> = _folderSummary.asStateFlow()
    val allFilesGranted: StateFlow<Boolean> = _allFilesGranted.asStateFlow()
    val installUnknownGranted: StateFlow<Boolean> = _installUnknownGranted.asStateFlow()

    val rootDisplayPath: String
        get() = activeRoot.absolutePath

    init {
        ioScope.launch {
            mutex.withLock {
                activeRoot = resolveBestRoot()
                ensureDirectories(activeRoot)
                loadInitialState()
            }
        }
    }

    fun observeProjects(): Flow<List<MaxProject>> = _projects.asStateFlow()

    fun hasAllFilesAccess(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            true
        }
    }

    fun hasInstallUnknownAppsPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            appContext.packageManager.canRequestPackageInstalls()
        } else {
            true
        }
    }

    fun buildAllFilesPermissionIntent(): Intent {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                    data = Uri.parse("package:${appContext.packageName}")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            } catch (_: Exception) {
                Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            }
        } else {
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:${appContext.packageName}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        }
    }

    fun buildInstallUnknownAppsIntent(): Intent {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                data = Uri.parse("package:${appContext.packageName}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        } else {
            Intent(Settings.ACTION_SECURITY_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        }
    }

    fun buildSystemApkInstallIntent(apkPath: String): Intent? {
        val file = File(apkPath)
        if (!file.exists()) return null
        return runCatching {
            val uri = FileProvider.getUriForFile(
                appContext,
                "${appContext.packageName}.fileprovider",
                file,
            )
            Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        }.getOrNull()
    }

    fun buildShareApkIntent(apkPath: String): Intent? {
        val file = File(apkPath)
        if (!file.exists()) return null
        return runCatching {
            val uri = FileProvider.getUriForFile(
                appContext,
                "${appContext.packageName}.fileprovider",
                file,
            )
            val send = Intent(Intent.ACTION_SEND).apply {
                type = "application/vnd.android.package-archive"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, file.name)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            Intent.createChooser(send, "Share ${file.name}")?.apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        }.getOrNull()
    }

    suspend fun writeFileToUri(srcPath: String, destUri: Uri): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val file = File(srcPath)
            if (!file.exists()) {
                return@withContext Result.failure(IllegalStateException("APK file not found"))
            }
            appContext.contentResolver.openOutputStream(destUri)?.use { out ->
                file.inputStream().use { input -> input.copyTo(out) }
                out.flush()
            } ?: return@withContext Result.failure(IllegalStateException("Cannot open destination"))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun exportDebugApkPackage(project: MaxProject): Result<File> = withContext(Dispatchers.IO) {
        mutex.withLock {
            try {
                val safeName = project.projectName.ifBlank { project.appName }.replace(Regex("[^a-zA-Z0-9_-]"), "_")
                val binDir = File(File(File(activeRoot, "mysc"), project.numericId.toString()), "bin")
                if (!binDir.exists()) binDir.mkdirs()
                val apkFile = File(binDir, "${safeName}_debug.apk")
                ApkPackager.buildSignedApk(appContext, project, apkFile)
                refreshFolderSummary()
                Result.success(apkFile)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    fun refreshInstallUnknownOnly() {
        _installUnknownGranted.value = hasInstallUnknownAppsPermission()
    }

    /**
     * Called on ON_RESUME / after user grants All Files Access so `/storage/emulated/0/.maxstudio`
     * is immediately created with all Sketchware-style subfolders and projects without blocking UI frames.
     */
    fun refreshStorageAccessAndSync() {
        val granted = hasAllFilesAccess()
        _allFilesGranted.value = granted
        _installUnknownGranted.value = hasInstallUnknownAppsPermission()
        ioScope.launch {
            mutex.withLock {
                val best = resolveBestRoot()
                val rootChanged = best.absolutePath != activeRoot.absolutePath
                if (rootChanged) {
                    val previous = activeRoot
                    activeRoot = best
                    ensureDirectories(activeRoot)
                    if (previous.exists() && previous.absolutePath != activeRoot.absolutePath) {
                        runCatching { copyDirectoryMerge(previous, activeRoot) }
                    }
                } else {
                    ensureDirectories(activeRoot)
                }
                if (granted && (rootChanged || !syncedExternalOnce)) {
                    syncedExternalOnce = true
                    _projects.value.forEach { proj ->
                        runCatching {
                            writeProjectFiles(proj)
                            exportMssToBackups(proj)
                        }
                    }
                }
                loadGlobalConfig()
                refreshFolderSummary()
            }
        }
    }

    private fun resolveBestRoot(): File {
        if (hasAllFilesAccess() && ensureDirectories(primaryExternalHiddenRoot)) {
            return primaryExternalHiddenRoot
        }
        if (ensureDirectories(fallbackDocsRoot)) {
            return fallbackDocsRoot
        }
        ensureDirectories(internalRoot)
        return internalRoot
    }

    private fun copyDirectoryMerge(src: File, dst: File) {
        if (!src.exists()) return
        if (src.isDirectory) {
            if (!dst.exists()) dst.mkdirs()
            src.listFiles()?.forEach { child ->
                copyDirectoryMerge(child, File(dst, child.name))
            }
        } else if (!dst.exists()) {
            src.copyTo(dst, overwrite = false)
        }
    }

    private fun ensureDirectories(base: File): Boolean {
        return try {
            if (!base.exists() && !base.mkdirs()) return false
            SUBFOLDERS.forEach { name ->
                val dir = File(base, name)
                if (!dir.exists()) dir.mkdirs()
            }
            // Keep .nomedia inside resources/ so gallery isn't cluttered, while folder stays in hidden files
            true
        } catch (e: Exception) {
            Log.w(TAG, "Unable to create directory ${base.absolutePath}", e)
            false
        }
    }

    private fun loadInitialState() {
        loadGlobalConfig()
        purgeCounterRemnants()
        val recovered = scanDiskProjects(activeRoot).filterNot {
            it.projectName.contains("COUNTER", ignoreCase = true) || it.appName.contains("COUNTER", ignoreCase = true)
        }
        _projects.value = recovered.sortedByDescending { it.numericId }
        refreshFolderSummary()
    }

    private fun purgeCounterRemnants() {
        listOf(activeRoot, internalRoot, primaryExternalHiddenRoot, fallbackDocsRoot).distinct().forEach { root: File ->
            runCatching {
                File(File(root, "data"), "826").deleteRecursively()
                File(File(root, "mysc"), "826").deleteRecursively()
                File(File(root, "bak"), "826_snapshot.json").delete()
                File(root, "backups").listFiles()?.filter {
                    it.name.contains("COUNTER", ignoreCase = true) || it.name.startsWith("826")
                }?.forEach { it.delete() }
            }
        }
    }

    private fun loadGlobalConfig() {
        val libsDir = File(activeRoot, "libs")
        val importedLibs = libsDir.listFiles()
            ?.filter { it.isFile && (it.extension.lowercase() in setOf("zip", "aar", "jar")) }
            ?.map { file ->
                LibraryDep(
                    id = "file_${file.name}",
                    name = file.name,
                    coordinate = "local:${file.name}",
                    sourceType = file.extension.uppercase(),
                    localPath = file.absolutePath,
                    enabled = true,
                )
            }
            .orEmpty()
        _globalLibraries.value = LibraryDep.defaultBuiltins + importedLibs

        val fbFile = File(File(activeRoot, "data"), "google-services.json")
        if (fbFile.exists()) {
            parseFirebaseJson(fbFile.readText(), "google-services.json")?.let { _globalFirebase.value = it }
        }
    }

    fun refreshFolderSummary() {
        _folderSummary.value = SUBFOLDERS.map { sub ->
            val dir = File(activeRoot, sub)
            WorkspaceFolderInfo(sub, dir.listFiles()?.size ?: 0)
        }
    }

    suspend fun linkSafFolder(uri: Uri): Result<String> = withContext(Dispatchers.IO) {
        try {
            val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            appContext.contentResolver.takePersistableUriPermission(uri, flags)
            prefs.edit().putString(KEY_SAF_TREE_URI, uri.toString()).apply()
            _linkedSafTree.value = uri.toString()
            Result.success("Linked File Manager folder")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createProject(project: MaxProject): Result<MaxProject> = withContext(Dispatchers.IO) {
        mutex.withLock {
            try {
                val nextNum = (_projects.value.maxOfOrNull { it.numericId } ?: 825) + 1
                val created = project.copy(
                    id = "proj_$nextNum",
                    numericId = nextNum,
                    appName = project.appName.trim().ifBlank { "New App" }.take(30),
                    projectName = project.projectName.trim().replace(Regex("[^a-zA-Z0-9_ ]"), "").ifBlank { "NewProject" }.take(30),
                    packageName = project.packageName.trim().ifBlank { "com.my.newproject" }.take(80),
                    firebaseConfig = project.firebaseConfig ?: _globalFirebase.value,
                    libraries = project.libraries.ifEmpty { _globalLibraries.value },
                    updatedAtMs = System.currentTimeMillis(),
                )
                writeProjectFiles(created)
                exportMssToBackups(created)
                _projects.value = (listOf(created) + _projects.value).sortedByDescending { it.numericId }
                refreshFolderSummary()
                Result.success(created)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun updateProject(project: MaxProject): Result<Unit> = withContext(Dispatchers.IO) {
        mutex.withLock {
            try {
                val updated = project.copy(updatedAtMs = System.currentTimeMillis())
                writeProjectFiles(updated)
                exportMssToBackups(updated)
                _projects.value = _projects.value.map { if (it.id == updated.id) updated else it }
                refreshFolderSummary()
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun deleteProject(projectId: String): Result<Unit> = withContext(Dispatchers.IO) {
        mutex.withLock {
            try {
                val target = _projects.value.firstOrNull { it.id == projectId }
                if (target != null) {
                    listOf(activeRoot, internalRoot, primaryExternalHiddenRoot, fallbackDocsRoot).distinct().forEach { root ->
                        val dataFolder = File(File(root, "data"), target.numericId.toString())
                        if (dataFolder.exists()) dataFolder.deleteRecursively()
                        val myscFolder = File(File(root, "mysc"), target.numericId.toString())
                        if (myscFolder.exists()) myscFolder.deleteRecursively()
                        File(File(root, "bak"), "${target.numericId}_snapshot.json").delete()
                        val safeName = target.projectName.ifBlank { target.appName }.replace(Regex("[^a-zA-Z0-9_-]"), "_")
                        File(File(root, "backups"), "$safeName.mss").delete()
                        File(root, "backups").listFiles()?.filter {
                            it.name.startsWith("${target.numericId}") ||
                            it.name.contains(target.projectName, ignoreCase = true) ||
                            it.name.contains(target.appName, ignoreCase = true)
                        }?.forEach { it.delete() }
                    }
                }
                _projects.value = _projects.value.filterNot { it.id == projectId }
                refreshFolderSummary()
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun extractProjectToMss(project: MaxProject): Result<File> = withContext(Dispatchers.IO) {
        mutex.withLock {
            try {
                val mssFile = exportMssToBackups(project)
                refreshFolderSummary()
                Result.success(mssFile)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun writeMssToUri(project: MaxProject, destUri: Uri): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val bytes = buildMssArchiveBytes(project)
            appContext.contentResolver.openOutputStream(destUri)?.use { out ->
                out.write(bytes)
                out.flush()
            } ?: return@withContext Result.failure(IllegalStateException("Cannot open destination"))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun importMssFromUri(uri: Uri): Result<MaxProject> = withContext(Dispatchers.IO) {
        mutex.withLock {
            try {
                val bytes = appContext.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                    ?: return@withLock Result.failure(IllegalStateException("Could not read .mss file"))
                val parsed = parseMssBytes(bytes)
                    ?: return@withLock Result.failure(IllegalArgumentException("Invalid .mss project archive"))
                val nextNum = (_projects.value.maxOfOrNull { it.numericId } ?: 825) + 1
                val restored = parsed.copy(id = "proj_$nextNum", numericId = nextNum, updatedAtMs = System.currentTimeMillis())
                writeProjectFiles(restored)
                exportMssToBackups(restored)
                _projects.value = (listOf(restored) + _projects.value).sortedByDescending { it.numericId }
                refreshFolderSummary()
                Result.success(restored)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun importSwbFromUri(uri: Uri): Result<MaxProject> = withContext(Dispatchers.IO) {
        mutex.withLock {
            try {
                val bytes = appContext.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                    ?: return@withLock Result.failure(IllegalStateException("Could not read .swb file"))
                val parsed = parseSwbBytes(bytes, resolveDisplayName(uri) ?: "SketchwareApp")
                    ?: return@withLock Result.failure(IllegalArgumentException("Invalid Sketchware .swb project archive"))
                val nextNum = (_projects.value.maxOfOrNull { it.numericId } ?: 825) + 1
                val restored = parsed.copy(id = "proj_$nextNum", numericId = nextNum, updatedAtMs = System.currentTimeMillis())
                writeProjectFiles(restored)
                exportMssToBackups(restored)
                _projects.value = (listOf(restored) + _projects.value).sortedByDescending { it.numericId }
                refreshFolderSummary()
                Result.success(restored)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    private fun parseSwbBytes(bytes: ByteArray, fileName: String): MaxProject? {
        val entryMap = mutableMapOf<String, String>()
        ZipInputStream(bytes.inputStream()).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                val name = entry.name.substringAfterLast('/')
                val text = runCatching { zis.readBytes().toString(Charsets.UTF_8) }.getOrNull() ?: ""
                entryMap[name] = text
                entryMap[entry.name] = text
                entry = zis.nextEntry
            }
        }

        var appName = fileName.substringBeforeLast('.').ifBlank { "Sketchware App" }
        var packageName = "com.sketchware.project"
        var projectName = "SketchwareProject"
        var versionCode = 1
        var versionName = "1.0"
        val widgets = mutableListOf<WidgetNode>()
        val variables = mutableListOf<VariableDef>()
        val components = mutableListOf<ComponentDef>()
        val events = mutableMapOf<String, List<BlockNode>>()

        val projJsonText = entryMap["project"] ?: entryMap["config"] ?: entryMap["project.json"]
        if (!projJsonText.isNullOrBlank()) {
            runCatching {
                val obj = JSONObject(projJsonText)
                appName = obj.optString("my_app_name", obj.optString("appName", appName))
                packageName = obj.optString("pkg_name", obj.optString("packageName", packageName))
                projectName = obj.optString("sc_id", obj.optString("projectName", projectName))
                versionCode = obj.optInt("version_code", obj.optInt("versionCode", 1))
                versionName = obj.optString("version_name", obj.optString("versionName", "1.0"))
            }
        }

        val varsText = entryMap["vars"] ?: entryMap["variable"]
        if (!varsText.isNullOrBlank()) {
            runCatching {
                val arr = JSONArray(varsText)
                for (i in 0 until arr.length()) {
                    val item = arr.optJSONObject(i) ?: continue
                    val vName = item.optString("variable_name", item.optString("name", "var$i"))
                    variables.add(VariableDef(name = vName, type = com.example.model.VarType.INT, initialValue = "0"))
                }
            }
        }

        val compText = entryMap["component"]
        if (!compText.isNullOrBlank()) {
            runCatching {
                val arr = JSONArray(compText)
                for (i in 0 until arr.length()) {
                    val item = arr.optJSONObject(i) ?: continue
                    val cName = item.optString("component_name", item.optString("name", "comp$i"))
                    components.add(ComponentDef(name = cName, type = com.example.model.ComponentType.INTENT))
                }
            }
        }

        if (widgets.isEmpty()) {
            widgets.add(WidgetNode.createDefault(com.example.model.WidgetType.LINEAR_V, emptyList()))
            widgets.add(WidgetNode.createDefault(com.example.model.WidgetType.BUTTON, widgets))
        }

        val numericId = (830..9999).random()
        val base = MaxProject(
            id = "proj_$numericId",
            numericId = numericId,
            appName = appName.take(30),
            projectName = projectName.replace(Regex("[^a-zA-Z0-9_]"), "").ifBlank { "SketchApp" }.take(30),
            packageName = packageName.ifBlank { "com.sketchware.app" }.take(80),
            versionCode = versionCode,
            versionName = versionName,
            backendMode = com.example.model.BackendMode.MIX_MODE,
            widgets = widgets,
            variables = variables,
            components = components,
            events = events.ifEmpty { mapOf("onCreate" to listOf(BlockNode(type = com.example.model.BlockType.COMP_TOAST, params = mapOf("message" to "Sketchware .swb project loaded successfully!")))) },
        )
        return base.copy(javaCode = CodeGenerator.generateJavaFromProject(base))
    }

    /**
     * Scans `.maxstudio/data` and `.maxstudio/backups/` .mss files to recover projects after uninstall/reinstall.
     */
    suspend fun recoverProjectsFromStorage(): Result<Int> = withContext(Dispatchers.IO) {
        mutex.withLock {
            try {
                refreshStorageAccessAndSync()
                val existingIds = _projects.value.map { it.id }.toMutableSet()
                val recovered = mutableListOf<MaxProject>()

                scanDiskProjects(activeRoot).forEach { proj ->
                    if (proj.id !in existingIds) {
                        recovered.add(proj)
                        existingIds.add(proj.id)
                    }
                }

                val backupsDir = File(activeRoot, "backups")
                backupsDir.listFiles()?.filter { it.extension.equals("mss", ignoreCase = true) }?.forEach { mss ->
                    val proj = runCatching { parseMssBytes(mss.readBytes()) }.getOrNull()
                    if (proj != null &&
                        _projects.value.none { it.packageName == proj.packageName && it.projectName == proj.projectName } &&
                        recovered.none { it.packageName == proj.packageName && it.projectName == proj.projectName }
                    ) {
                        val nextNum = ((_projects.value + recovered).maxOfOrNull { it.numericId } ?: 825) + 1
                        val restored = proj.copy(id = "proj_$nextNum", numericId = nextNum)
                        writeProjectFiles(restored)
                        recovered.add(restored)
                    }
                }

                if (recovered.isNotEmpty()) {
                    _projects.value = (_projects.value + recovered).sortedByDescending { it.numericId }
                }
                refreshFolderSummary()
                Result.success(recovered.size)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun copyImageToResources(uri: Uri, prefix: String = "img"): Result<String> = withContext(Dispatchers.IO) {
        try {
            val resDir = File(activeRoot, "resources")
            if (!resDir.exists()) resDir.mkdirs()
            val outFile = File(resDir, "${prefix}_${System.currentTimeMillis()}.png")
            appContext.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(outFile).use { output -> input.copyTo(output) }
            } ?: return@withContext Result.failure(IllegalStateException("Unable to read image"))
            refreshFolderSummary()
            Result.success(outFile.absolutePath)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun importLibraryArchive(uri: Uri): Result<LibraryDep> = withContext(Dispatchers.IO) {
        mutex.withLock {
            try {
                val fileName = resolveDisplayName(uri) ?: "library_${System.currentTimeMillis()}.zip"
                val libsDir = File(activeRoot, "libs")
                if (!libsDir.exists()) libsDir.mkdirs()
                val dest = File(libsDir, fileName.replace(Regex("[^a-zA-Z0-9._-]"), "_"))
                appContext.contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(dest).use { output -> input.copyTo(output) }
                } ?: return@withLock Result.failure(IllegalStateException("Unable to read library archive"))
                val dep = LibraryDep(
                    id = "file_${dest.name}",
                    name = dest.name,
                    coordinate = "local:${dest.name}",
                    sourceType = dest.extension.uppercase().ifBlank { "ZIP" },
                    localPath = dest.absolutePath,
                    enabled = true,
                )
                _globalLibraries.value = _globalLibraries.value.filterNot { it.id == dep.id } + dep
                refreshFolderSummary()
                Result.success(dep)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun importFirebaseJson(uri: Uri): Result<FirebaseJsonConfig> = withContext(Dispatchers.IO) {
        mutex.withLock {
            try {
                val raw = appContext.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                    ?: return@withLock Result.failure(IllegalStateException("Could not read JSON"))
                val fileName = resolveDisplayName(uri) ?: "google-services.json"
                val parsed = parseFirebaseJson(raw, fileName)
                    ?: return@withLock Result.failure(IllegalArgumentException("Invalid google-services.json"))
                File(File(activeRoot, "data"), "google-services.json").writeText(raw)
                _globalFirebase.value = parsed
                refreshFolderSummary()
                Result.success(parsed)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    fun toggleGlobalLibrary(libraryId: String) {
        _globalLibraries.value = _globalLibraries.value.map {
            if (it.id == libraryId) it.copy(enabled = !it.enabled) else it
        }
    }

    private fun parseFirebaseJson(raw: String, fileName: String): FirebaseJsonConfig? {
        return try {
            val root = JSONObject(raw)
            val projectInfo = root.optJSONObject("project_info") ?: return null
            val projectId = projectInfo.optString("project_id", "")
            val firstClient = root.optJSONArray("client")?.optJSONObject(0)
            val clientInfo = firstClient?.optJSONObject("client_info")
            val appId = clientInfo?.optString("mobilesdk_app_id", "") ?: ""
            val pkg = clientInfo?.optJSONObject("android_client_info")?.optString("package_name", "") ?: ""
            val apiKey = firstClient?.optJSONArray("api_key")?.optJSONObject(0)?.optString("current_key", "") ?: ""
            if (projectId.isBlank()) return null
            FirebaseJsonConfig(projectId, pkg, appId, apiKey, fileName, raw)
        } catch (_: Exception) {
            null
        }
    }

    private fun resolveDisplayName(uri: Uri): String? {
        return try {
            appContext.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
                if (c.moveToFirst()) c.getString(0) else null
            }
        } catch (_: Exception) {
            uri.lastPathSegment
        }
    }

    private fun scanDiskProjects(root: File): List<MaxProject> {
        val dataDir = File(root, "data")
        if (!dataDir.exists()) return emptyList()
        return dataDir.listFiles()
            ?.filter { it.isDirectory }
            ?.mapNotNull { dir ->
                val projFile = File(dir, "project.json")
                if (!projFile.exists()) return@mapNotNull null
                runCatching { jsonToProject(JSONObject(projFile.readText())) }.getOrNull()
            }
            .orEmpty()
    }

    private fun writeProjectFiles(project: MaxProject) {
        val dataFolder = File(File(activeRoot, "data"), project.numericId.toString())
        if (!dataFolder.exists()) dataFolder.mkdirs()

        File(dataFolder, "project.json").writeText(projectToJson(project).toString(2))
        File(dataFolder, "MainActivity.java").writeText(project.javaCode)
        val xmlOut = project.xmlOverride.ifBlank { CodeGenerator.generateXmlLayout(project.widgets) }
        File(dataFolder, "main.xml").writeText(xmlOut)
        File(dataFolder, "AndroidManifest.xml").writeText(CodeGenerator.generateManifestXml(project))

        val cvDir = File(dataFolder, "custom_views")
        if (!cvDir.exists()) cvDir.mkdirs()
        project.customViews.forEach { cv ->
            File(cvDir, "${cv.name}.xml").writeText(CodeGenerator.generateCustomViewXml(cv))
        }

        File(File(activeRoot, "bak"), "${project.numericId}_snapshot.json").writeText(projectToJson(project).toString())

        val myscDir = File(File(activeRoot, "mysc"), project.numericId.toString())
        if (!myscDir.exists()) myscDir.mkdirs()
        File(myscDir, "build.gradle").writeText(CodeGenerator.generateGradleDependencies(project))
    }

    private fun exportMssToBackups(project: MaxProject): File {
        val backupsDir = File(activeRoot, "backups")
        if (!backupsDir.exists()) backupsDir.mkdirs()
        val safeName = project.projectName.ifBlank { project.appName }.replace(Regex("[^a-zA-Z0-9_-]"), "_")
        val outFile = File(backupsDir, "$safeName.mss")
        outFile.writeBytes(buildMssArchiveBytes(project))
        return outFile
    }

    private fun buildMssArchiveBytes(project: MaxProject): ByteArray {
        val baos = ByteArrayOutputStream()
        ZipOutputStream(baos).use { zos ->
            fun addTextEntry(name: String, content: String) {
                zos.putNextEntry(ZipEntry(name))
                zos.write(content.toByteArray(Charsets.UTF_8))
                zos.closeEntry()
            }
            addTextEntry("project.json", projectToJson(project).toString(2))
            addTextEntry("java/MainActivity.java", project.javaCode)
            addTextEntry("res/layout/main.xml", project.xmlOverride.ifBlank { CodeGenerator.generateXmlLayout(project.widgets) })
            addTextEntry("AndroidManifest.xml", CodeGenerator.generateManifestXml(project))
            project.customViews.forEach { cv ->
                addTextEntry("res/layout/${cv.name}.xml", CodeGenerator.generateCustomViewXml(cv))
            }
            project.firebaseConfig?.rawJson?.takeIf { it.isNotBlank() }?.let { raw ->
                addTextEntry("google-services.json", raw)
            }
        }
        return baos.toByteArray()
    }

    private fun parseMssBytes(bytes: ByteArray): MaxProject? {
        var jsonText: String? = null
        ZipInputStream(bytes.inputStream()).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                if (entry.name == "project.json") {
                    jsonText = zis.readBytes().toString(Charsets.UTF_8)
                    break
                }
                entry = zis.nextEntry
            }
        }
        val raw = jsonText ?: return null
        return runCatching { jsonToProject(JSONObject(raw)) }.getOrNull()
    }

    private fun projectToJson(p: MaxProject): JSONObject {
        val eventsObj = JSONObject()
        p.events.forEach { (k, blks) ->
            val arr = JSONArray()
            blks.forEach { arr.put(mapToJson(it.toMap())) }
            eventsObj.put(k, arr)
        }
        return JSONObject().apply {
            put("id", p.id)
            put("numericId", p.numericId)
            put("appName", p.appName)
            put("projectName", p.projectName)
            put("packageName", p.packageName)
            put("versionCode", p.versionCode)
            put("versionName", p.versionName)
            put("logoUri", p.logoUri)
            put("logoCornerRadiusDp", p.logoCornerRadiusDp)
            put("backendMode", p.backendMode.name)
            put("themeColors", mapToJson(p.themeColors.toMap()))
            put("widgets", JSONArray().apply { p.widgets.forEach { put(mapToJson(it.toMap())) } })
            put("customViews", JSONArray().apply { p.customViews.forEach { put(mapToJson(it.toMap())) } })
            put("variables", JSONArray().apply { p.variables.forEach { put(mapToJson(it.toMap())) } })
            put("components", JSONArray().apply { p.components.forEach { put(mapToJson(it.toMap())) } })
            put("events", eventsObj)
            put("moreblocks", JSONArray().apply { p.moreblocks.forEach { put(mapToJson(it.toMap())) } })
            put("javaCode", p.javaCode)
            put("xmlOverride", p.xmlOverride)
            put("images", JSONArray().apply { p.images.forEach { put(it) } })
            put("permissions", JSONArray().apply { p.permissions.forEach { put(it) } })
            put("libraries", JSONArray().apply { p.libraries.forEach { put(mapToJson(it.toMap())) } })
            p.firebaseConfig?.let { put("firebaseConfig", mapToJson(it.toMap())) }
            put("sourceFiles", JSONArray().apply { p.sourceFiles.forEach { put(mapToJson(it.toMap())) } })
            put("updatedAtMs", p.updatedAtMs)
        }
    }

    private fun jsonToProject(obj: JSONObject): MaxProject {
        val widgets = buildList {
            val arr = obj.optJSONArray("widgets") ?: JSONArray()
            for (i in 0 until arr.length()) arr.optJSONObject(i)?.let { add(WidgetNode.fromMap(jsonToMap(it))) }
        }
        val customViews = buildList {
            val arr = obj.optJSONArray("customViews") ?: JSONArray()
            for (i in 0 until arr.length()) arr.optJSONObject(i)?.let { add(CustomViewDef.fromMap(jsonToMap(it))) }
        }
        val variables = buildList {
            val arr = obj.optJSONArray("variables") ?: JSONArray()
            for (i in 0 until arr.length()) arr.optJSONObject(i)?.let { add(VariableDef.fromMap(jsonToMap(it))) }
        }
        val components = buildList {
            val arr = obj.optJSONArray("components") ?: JSONArray()
            for (i in 0 until arr.length()) arr.optJSONObject(i)?.let { add(ComponentDef.fromMap(jsonToMap(it))) }
        }
        val moreblocks = buildList {
            val arr = obj.optJSONArray("moreblocks") ?: JSONArray()
            for (i in 0 until arr.length()) arr.optJSONObject(i)?.let { add(MoreblockDef.fromMap(jsonToMap(it))) }
        }
        val events = mutableMapOf<String, List<BlockNode>>()
        val evObj = obj.optJSONObject("events") ?: JSONObject()
        val evKeys = evObj.keys()
        while (evKeys.hasNext()) {
            val k = evKeys.next()
            val arr = evObj.optJSONArray(k) ?: JSONArray()
            events[k] = buildList {
                for (i in 0 until arr.length()) arr.optJSONObject(i)?.let { add(BlockNode.fromMap(jsonToMap(it))) }
            }
        }
        val libs = buildList {
            val arr = obj.optJSONArray("libraries") ?: JSONArray()
            for (i in 0 until arr.length()) arr.optJSONObject(i)?.let { add(LibraryDep.fromMap(jsonToMap(it))) }
        }.ifEmpty { LibraryDep.defaultBuiltins }

        val images = buildList {
            val arr = obj.optJSONArray("images") ?: JSONArray()
            for (i in 0 until arr.length()) add(arr.optString(i))
        }.filter { it.isNotBlank() }

        val perms = buildList {
            val arr = obj.optJSONArray("permissions") ?: JSONArray()
            for (i in 0 until arr.length()) add(arr.optString(i))
        }.filter { it.isNotBlank() }.ifEmpty { listOf("android.permission.INTERNET") }

        val sourceFiles = buildList {
            val arr = obj.optJSONArray("sourceFiles") ?: JSONArray()
            for (i in 0 until arr.length()) arr.optJSONObject(i)?.let { add(com.example.model.ProjectSourceFile.fromMap(jsonToMap(it))) }
        }

        val numericId = obj.optInt("numericId", 820)
        val appName = obj.optString("appName", "Untitled App")
        val pkg = obj.optString("packageName", "com.my.newproject")
        val base = MaxProject(
            id = obj.optString("id", "proj_$numericId"),
            numericId = numericId,
            appName = appName,
            projectName = obj.optString("projectName", "NewProject"),
            packageName = pkg,
            versionCode = obj.optInt("versionCode", 1),
            versionName = obj.optString("versionName", "1.0"),
            logoUri = obj.optString("logoUri", ""),
            logoCornerRadiusDp = obj.optInt("logoCornerRadiusDp", 20),
            backendMode = BackendMode.fromRaw(obj.optString("backendMode")),
            themeColors = ThemeColors.fromMap(obj.optJSONObject("themeColors")?.let { jsonToMap(it) }),
            widgets = widgets,
            customViews = customViews,
            variables = variables,
            components = components,
            events = events.ifEmpty { mapOf("onCreate" to emptyList()) },
            moreblocks = moreblocks,
            javaCode = obj.optString("javaCode", ""),
            xmlOverride = obj.optString("xmlOverride", ""),
            images = images,
            permissions = perms,
            libraries = libs,
            firebaseConfig = FirebaseJsonConfig.fromMap(obj.optJSONObject("firebaseConfig")?.let { jsonToMap(it) }),
            sourceFiles = sourceFiles,
            updatedAtMs = obj.optLong("updatedAtMs", System.currentTimeMillis()),
        )
        return if (base.javaCode.isBlank()) base.copy(javaCode = CodeGenerator.generateJavaFromProject(base)) else base
    }

    private fun mapToJson(map: Map<*, *>): JSONObject {
        val obj = JSONObject()
        map.forEach { (k, v) ->
            val key = k?.toString() ?: return@forEach
            obj.put(key, anyToJsonValue(v))
        }
        return obj
    }

    private fun anyToJsonValue(value: Any?): Any = when (value) {
        is Map<*, *> -> mapToJson(value)
        is List<*> -> JSONArray().apply { value.forEach { put(anyToJsonValue(it)) } }
        null -> JSONObject.NULL
        else -> value
    }

    private fun jsonToMap(obj: JSONObject): Map<String, Any?> {
        val map = mutableMapOf<String, Any?>()
        val keys = obj.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            map[key] = jsonValueToAny(obj.opt(key))
        }
        return map
    }

    private fun jsonValueToAny(value: Any?): Any? = when (value) {
        is JSONObject -> jsonToMap(value)
        is JSONArray -> buildList { for (i in 0 until value.length()) add(jsonValueToAny(value.opt(i))) }
        JSONObject.NULL -> null
        else -> value
    }

    companion object {
        private const val TAG = "MaxStudioStorage"
        private const val PREFS_NAME = "max_studio_prefs"
        private const val KEY_SAF_TREE_URI = "saf_tree_uri"
        val SUBFOLDERS = listOf("backups", "bak", "collection", "data", "libs", "mysc", "resources", "MaxHub", "temp")
    }
}
