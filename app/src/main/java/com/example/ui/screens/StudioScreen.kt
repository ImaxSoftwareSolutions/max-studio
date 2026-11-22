package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.codegen.CodeGenerator
import com.example.model.MaxProject
import com.example.service.BuildPipelineController
import com.example.ui.theme.EmeraldRun
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.viewmodel.SaveStatus
import com.example.ui.viewmodel.StudioViewModel
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudioScreen(
    viewModel: StudioViewModel,
    onBackToProjects: () -> Unit,
) {
    val context = LocalContext.current
    val project by viewModel.project.collectAsStateWithLifecycle()
    val activeFilePath by viewModel.activeFilePath.collectAsStateWithLifecycle()
    val openTabs by viewModel.openTabs.collectAsStateWithLifecycle()
    val saveStatus by viewModel.saveStatus.collectAsStateWithLifecycle()
    val canUndo by viewModel.canUndo.collectAsStateWithLifecycle()
    val canRedo by viewModel.canRedo.collectAsStateWithLifecycle()
    val showBuildSheet by viewModel.showBuildSheet.collectAsStateWithLifecycle()
    val toastMsg by viewModel.toastMessage.collectAsStateWithLifecycle()
    val installUnknownGranted by viewModel.installUnknownGranted.collectAsStateWithLifecycle()
    val diagnosticsReport by viewModel.diagnosticsReport.collectAsStateWithLifecycle()
    val requestedBuildSubMode by viewModel.requestedBuildSubMode.collectAsStateWithLifecycle()
    val buildState by BuildPipelineController.state.collectAsStateWithLifecycle()

    val currentProject = project ?: return
    val snackbarHostState = remember { SnackbarHostState() }
    val lifecycleOwner = LocalLifecycleOwner.current

    var showConfigDrawer by remember { mutableStateOf(false) }
    var showLibsSheet by remember { mutableStateOf(false) }
    var showPermissionManagerSheet by remember { mutableStateOf(false) }
    var showManifestDialog by remember { mutableStateOf(false) }
    var showRunChevronMenu by remember { mutableStateOf(false) }

    var lastHandledInstallTriggerId by remember { mutableIntStateOf(buildState.installPromptTriggerId) }
    var showAutoInstallDialog by remember { mutableStateOf(false) }
    var showInstalledLiveAppDialog by remember { mutableStateOf(false) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshInstallPermission()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(buildState.installPromptTriggerId, buildState.completedSuccess) {
        if (buildState.installPromptTriggerId > lastHandledInstallTriggerId && buildState.completedSuccess) {
            lastHandledInstallTriggerId = buildState.installPromptTriggerId
            viewModel.refreshInstallPermission()
            viewModel.setShowBuildSheet(false)
            kotlinx.coroutines.delay(350)
            showAutoInstallDialog = true
        }
    }

    BackHandler {
        when {
            showInstalledLiveAppDialog -> showInstalledLiveAppDialog = false
            showAutoInstallDialog -> showAutoInstallDialog = false
            showConfigDrawer -> showConfigDrawer = false
            showBuildSheet -> viewModel.setShowBuildSheet(false)
            else -> onBackToProjects()
        }
    }

    val mssSaveLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri: Uri? ->
        if (uri != null) viewModel.writeCurrentMssToUri(uri)
    }

    val apkSaveLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/vnd.android.package-archive")
    ) { uri: Uri? ->
        if (uri != null && buildState.outputApkPath.isNotBlank()) {
            viewModel.saveApkToUri(buildState.outputApkPath, uri)
        }
    }

    val firebaseJsonLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) viewModel.importFirebaseJsonIntoProject(uri)
    }

    val libraryZipLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) viewModel.importLibraryIntoProject(uri)
    }

    LaunchedEffect(toastMsg) {
        val msg = toastMsg ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(msg)
        viewModel.clearToast()
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = currentProject.projectName.ifBlank { currentProject.appName },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        val saveLabel = when (saveStatus) {
                            SaveStatus.SAVED -> "${currentProject.numericId} • Android IDE"
                            SaveStatus.SAVING -> "${currentProject.numericId} • Saving..."
                            SaveStatus.UNSAVED -> "${currentProject.numericId} • Unsaved"
                            SaveStatus.ERROR -> "${currentProject.numericId} • Save Error"
                        }
                        Text(
                            text = saveLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackToProjects, modifier = Modifier.testTag("studio_back_btn")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to projects")
                    }
                },
                actions = {
                    val issueCount = diagnosticsReport.errorCount + buildState.runtimeErrors.size
                    if (issueCount > 0) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.errorContainer,
                            modifier = Modifier
                                .padding(end = 2.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .clickable { viewModel.setShowBuildSheet(true, subMode = 2) }
                                .testTag("studio_errors_badge"),
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                Icon(
                                    Icons.Filled.Warning,
                                    contentDescription = "Errors present",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(16.dp),
                                )
                                Text(
                                    text = "$issueCount",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                )
                            }
                        }
                    }
                    IconButton(
                        onClick = { viewModel.undo() },
                        enabled = canUndo,
                        modifier = Modifier.testTag("studio_undo_btn"),
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = "Undo")
                    }
                    IconButton(
                        onClick = { viewModel.redo() },
                        enabled = canRedo,
                        modifier = Modifier.testTag("studio_redo_btn"),
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Redo, contentDescription = "Redo")
                    }
                    IconButton(
                        onClick = { viewModel.saveNow() },
                        modifier = Modifier.testTag("studio_save_btn"),
                    ) {
                        Icon(Icons.Filled.Save, contentDescription = "Save Project")
                    }
                    IconButton(
                        onClick = { showConfigDrawer = true },
                        modifier = Modifier.testTag("studio_more_menu_btn"),
                    ) {
                        Icon(Icons.Filled.MoreVert, contentDescription = "Configuration Drawer")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
            )
        },
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 4.dp,
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = File(activeFilePath).name.ifBlank { "AIDE IDE" },
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        modifier = Modifier.weight(1f),
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Button(
                            onClick = { viewModel.triggerApkBuild(context) },
                            shape = RoundedCornerShape(topStart = 24.dp, bottomStart = 24.dp, topEnd = 6.dp, bottomEnd = 6.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            ),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                            modifier = Modifier.height(44.dp).testTag("studio_bottom_run_btn"),
                        ) {
                            Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Run APK", fontWeight = FontWeight.Bold)
                        }
                        FilledTonalButton(
                            onClick = { showRunChevronMenu = true },
                            shape = RoundedCornerShape(topStart = 6.dp, bottomStart = 6.dp, topEnd = 24.dp, bottomEnd = 24.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp),
                            modifier = Modifier.height(44.dp).testTag("studio_run_chevron_btn"),
                        ) {
                            Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "Build Options")
                        }
                    }

                    DropdownMenu(
                        expanded = showRunChevronMenu,
                        onDismissRequest = { showRunChevronMenu = false },
                    ) {
                        DropdownMenuItem(
                            text = { Text("Build Settings & Libraries") },
                            onClick = {
                                showRunChevronMenu = false
                                showLibsSheet = true
                            },
                        )
                        DropdownMenuItem(
                            text = { Text("Show Compiler & Error Logs") },
                            onClick = {
                                showRunChevronMenu = false
                                viewModel.setShowBuildSheet(true, subMode = 2)
                            },
                        )
                        DropdownMenuItem(
                            text = { Text("AndroidManifest.xml") },
                            onClick = {
                                showRunChevronMenu = false
                                showManifestDialog = true
                            },
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            AideIdeWorkspace(
                project = currentProject,
                activeFilePath = activeFilePath,
                openTabs = openTabs,
                onOpenFile = { viewModel.openFile(it) },
                onCloseTab = { viewModel.closeTab(it) },
                onUpdateFileContent = { path, content -> viewModel.updateSourceFileContent(path, content) },
                onCreateNewFile = { path, content, _ -> viewModel.createNewFile(path, content) },
                onDeleteFile = { viewModel.deleteSourceFile(it) },
                onImportImage = { viewModel.importImageIntoProject(it) },
                onRemoveImage = { viewModel.removeProjectImage(it) },
                onNotify = { viewModel.showToast(it) },
            )
        }
    }

    // Configuration Drawer Sheet
    if (showConfigDrawer) {
        ModalBottomSheet(onDismissRequest = { showConfigDrawer = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 10.dp),
            ) {
                Text(
                    "Project Settings & Tools",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                )
                HorizontalDivider()
                DrawerActionRow(
                    icon = Icons.Filled.Extension,
                    title = "Library Manager & Firebase",
                    subtitle = "Import .zip, .aar, .jar libraries & google-services.json",
                    onClick = {
                        showConfigDrawer = false
                        showLibsSheet = true
                    },
                )
                DrawerActionRow(
                    icon = Icons.Filled.Security,
                    title = "Permission Manager",
                    subtitle = "Configure Android Manifest permissions (Internet, Camera, etc.)",
                    onClick = {
                        showConfigDrawer = false
                        showPermissionManagerSheet = true
                    },
                )
                DrawerActionRow(
                    icon = Icons.Filled.Description,
                    title = "View AndroidManifest.xml",
                    subtitle = "Inspect generated binary manifest",
                    onClick = {
                        showConfigDrawer = false
                        showManifestDialog = true
                    },
                )
                DrawerActionRow(
                    icon = Icons.Filled.Archive,
                    title = "Extract ${currentProject.projectName}.mss",
                    subtitle = "Export full project archive to backups/",
                    onClick = {
                        showConfigDrawer = false
                        viewModel.extractCurrentProjectToMss()
                        mssSaveLauncher.launch("${currentProject.projectName.ifBlank { "project" }}.mss")
                    },
                )
            }
        }
    }

    // Run / Build Modal Sheet
    if (showBuildSheet) {
        ModalBottomSheet(onDismissRequest = { viewModel.setShowBuildSheet(false) }) {
            Box(modifier = Modifier.fillMaxWidth().height(560.dp).navigationBarsPadding()) {
                RunBuildTab(
                    project = currentProject,
                    diagnosticsReport = diagnosticsReport,
                    selectedSubMode = requestedBuildSubMode,
                    onSelectSubMode = { viewModel.selectBuildSubMode(it) },
                    installUnknownGranted = installUnknownGranted,
                    onStartBuild = { viewModel.triggerApkBuild(context) },
                    onExtractMss = {
                        viewModel.extractCurrentProjectToMss()
                        mssSaveLauncher.launch("${currentProject.projectName.ifBlank { "project" }}.mss")
                    },
                    onOpenInstallPopup = {
                        viewModel.setShowBuildSheet(false)
                        showAutoInstallDialog = true
                    },
                    onSaveApk = {
                        val defaultName = buildState.outputApkName.ifBlank {
                            "${currentProject.projectName.ifBlank { "app" }}_debug.apk"
                        }
                        apkSaveLauncher.launch(defaultName)
                    },
                    onShareApk = {
                        val intent = viewModel.buildShareApkIntent(buildState.outputApkPath)
                        if (intent == null) {
                            viewModel.showToast("APK file unavailable")
                        } else {
                            runCatching { context.startActivity(intent) }
                                .onFailure { viewModel.showToast("No share target found") }
                        }
                    },
                    onRequestInstallPermission = {
                        runCatching { context.startActivity(viewModel.buildInstallUnknownAppsIntent()) }
                    },
                )
            }
        }
    }

    // Library Manager + Firebase Sheet
    if (showLibsSheet) {
        ModalBottomSheet(onDismissRequest = { showLibsSheet = false }) {
            Column(
                modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 20.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text("Firebase JSON & Libraries", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

                Button(
                    onClick = { firebaseJsonLauncher.launch(arrayOf("application/json", "*/*")) },
                    modifier = Modifier.fillMaxWidth().height(48.dp).testTag("studio_firebase_json_btn"),
                ) {
                    Icon(Icons.Filled.CloudUpload, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        if (currentProject.firebaseConfig == null) "Import Firebase JSON (google-services.json)"
                        else "Firebase Connected: ${currentProject.firebaseConfig.projectId}",
                    )
                }

                FilledTonalButton(
                    onClick = { libraryZipLauncher.launch(arrayOf("application/zip", "application/java-archive", "*/*")) },
                    modifier = Modifier.fillMaxWidth().height(48.dp).testTag("studio_import_library_btn"),
                ) {
                    Icon(Icons.Filled.Extension, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Import Library (.zip / .aar / .jar)")
                }

                HorizontalDivider()
                LazyColumn(modifier = Modifier.height(240.dp)) {
                    items(currentProject.libraries, key = { it.id }) { lib ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(lib.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                                Text(lib.coordinate, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Switch(checked = lib.enabled, onCheckedChange = { viewModel.toggleProjectLibrary(lib.id) })
                        }
                    }
                }
            }
        }
    }

    // Permission Manager Sheet
    if (showPermissionManagerSheet) {
        val commonPerms = listOf(
            "android.permission.INTERNET",
            "android.permission.ACCESS_NETWORK_STATE",
            "android.permission.VIBRATE",
            "android.permission.CAMERA",
            "android.permission.RECORD_AUDIO",
            "android.permission.ACCESS_FINE_LOCATION",
            "android.permission.POST_NOTIFICATIONS",
        )
        ModalBottomSheet(onDismissRequest = { showPermissionManagerSheet = false }) {
            Column(
                modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text("Permission Manager", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                commonPerms.forEach { perm ->
                    val enabled = perm in currentProject.permissions
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().clickable { viewModel.togglePermission(perm) },
                    ) {
                        Checkbox(checked = enabled, onCheckedChange = { viewModel.togglePermission(perm) })
                        Text(perm.removePrefix("android.permission."), style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }

    if (showManifestDialog) {
        val manifest = remember(currentProject) { CodeGenerator.generateManifestXml(currentProject) }
        AlertDialog(
            onDismissRequest = { showManifestDialog = false },
            title = { Text("AndroidManifest.xml") },
            text = {
                Box(modifier = Modifier.height(320.dp).verticalScroll(rememberScrollState())) {
                    Text(manifest, fontFamily = JetBrainsMonoFontFamily, style = MaterialTheme.typography.bodySmall)
                }
            },
            confirmButton = {
                OutlinedButton(onClick = { showManifestDialog = false }) { Text("Close") }
            },
        )
    }

    if (showAutoInstallDialog) {
        AutoInstallApkDialog(
            project = currentProject,
            outputApkName = buildState.outputApkName.ifBlank { "${currentProject.projectName.ifBlank { "app" }}_debug.apk" },
            outputApkPath = buildState.outputApkPath,
            installUnknownGranted = installUnknownGranted,
            onDismiss = { showAutoInstallDialog = false },
            onAllowInstallUnknown = {
                runCatching { context.startActivity(viewModel.buildInstallUnknownAppsIntent()) }
            },
            onInstallAndRun = {
                showAutoInstallDialog = false
                showInstalledLiveAppDialog = true
            },
            onOpenSystemInstaller = {
                if (!viewModel.hasInstallUnknownAppsPermission()) {
                    viewModel.showToast("Please enable Install Unknown Apps permission first")
                    runCatching { context.startActivity(viewModel.buildInstallUnknownAppsIntent()) }
                } else {
                    val intent = viewModel.buildSystemApkInstallIntent(buildState.outputApkPath)
                    if (intent == null) {
                        viewModel.showToast("APK file path unavailable")
                    } else {
                        runCatching {
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            context.startActivity(intent)
                        }.onFailure {
                            viewModel.showToast("System installer unavailable; use Install & Open App")
                        }
                    }
                }
            },
            onSaveApk = {
                val defaultName = buildState.outputApkName.ifBlank {
                    "${currentProject.projectName.ifBlank { "app" }}_debug.apk"
                }
                apkSaveLauncher.launch(defaultName)
            },
            onShareApk = {
                val intent = viewModel.buildShareApkIntent(buildState.outputApkPath)
                if (intent == null) {
                    viewModel.showToast("APK file path unavailable")
                } else {
                    runCatching { context.startActivity(intent) }
                        .onFailure { viewModel.showToast("No share target available") }
                }
            },
        )
    }

    if (showInstalledLiveAppDialog) {
        InstalledLiveAppDialog(
            project = currentProject,
            onDismiss = { showInstalledLiveAppDialog = false },
        )
    }
}

@Composable
private fun AutoInstallApkDialog(
    project: MaxProject,
    outputApkName: String,
    outputApkPath: String,
    installUnknownGranted: Boolean,
    onDismiss: () -> Unit,
    onAllowInstallUnknown: () -> Unit,
    onInstallAndRun: () -> Unit,
    onOpenSystemInstaller: () -> Unit,
    onSaveApk: () -> Unit,
    onShareApk: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            val iconShape = RoundedCornerShape(((project.logoCornerRadiusDp.coerceIn(0, 44) * 54f) / 86f).dp)
            if (project.logoUri.isNotBlank()) {
                val model: Any = if (project.logoUri.startsWith("/")) File(project.logoUri) else Uri.parse(project.logoUri)
                AsyncImage(
                    model = model,
                    contentDescription = project.appName,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(54.dp).clip(iconShape),
                )
            } else {
                Surface(
                    shape = iconShape,
                    color = EmeraldRun.copy(alpha = 0.16f),
                    modifier = Modifier.size(54.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Filled.InstallMobile,
                            contentDescription = "Install APK",
                            tint = EmeraldRun,
                            modifier = Modifier.size(30.dp),
                        )
                    }
                }
            }
        },
        title = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Do you want to install this app?",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "${project.appName} • v${project.versionName} (${project.versionCode})",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(outputApkName, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                        Text(project.packageName, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = "${project.permissions.size} permission(s) • Android IDE",
                            style = MaterialTheme.typography.labelSmall,
                            color = EmeraldRun,
                        )
                        if (outputApkPath.isNotBlank()) {
                            Text(
                                text = outputApkPath,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }

                if (!installUnknownGranted) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.errorContainer,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Filled.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.size(18.dp))
                                Text(
                                    text = "Install Unknown Apps permission off hai",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                )
                            }
                            OutlinedButton(
                                onClick = onAllowInstallUnknown,
                                modifier = Modifier.fillMaxWidth().testTag("allow_unknown_apps_btn"),
                            ) {
                                Text("Allow Install Unknown Apps")
                            }
                        }
                    }
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = EmeraldRun, modifier = Modifier.size(16.dp))
                        Text(
                            text = "Signed APK ready • Install Unknown Apps granted",
                            style = MaterialTheme.typography.labelMedium,
                            color = EmeraldRun,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }

                OutlinedButton(
                    onClick = onInstallAndRun,
                    modifier = Modifier.fillMaxWidth().testTag("install_and_open_app_btn"),
                ) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Run Instant Live Preview")
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedButton(
                        onClick = onSaveApk,
                        modifier = Modifier.weight(1f).testTag("dialog_save_apk_btn"),
                    ) {
                        Icon(Icons.Filled.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Save APK")
                    }
                    OutlinedButton(
                        onClick = onShareApk,
                        modifier = Modifier.weight(1f).testTag("dialog_share_apk_btn"),
                    ) {
                        Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Share APK")
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onOpenSystemInstaller,
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldRun, contentColor = Color.White),
                modifier = Modifier.testTag("system_apk_installer_btn"),
            ) {
                Icon(Icons.Filled.Android, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text("Install APK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}

@Composable
private fun InstalledLiveAppDialog(
    project: MaxProject,
    onDismiss: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        LiveAppRuntimeSurface(
            project = project,
            showExitButton = true,
            onDismiss = onDismiss,
            onRecordError = { BuildPipelineController.recordRuntimeError(it) },
        )
    }
}

@Composable
private fun DrawerActionRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = title, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
