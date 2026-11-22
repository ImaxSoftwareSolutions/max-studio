package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.MaxStudioStorage
import com.example.ui.screens.CreateProjectScreen
import com.example.ui.screens.MaxStudioSplashScreen
import com.example.ui.screens.ProjectsScreen
import com.example.ui.screens.StudioScreen
import com.example.ui.theme.MaxStudioTheme
import com.example.ui.viewmodel.ProjectsViewModel
import com.example.ui.viewmodel.StudioViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaxStudioRoot()
        }
    }
}

@Composable
fun MaxStudioRoot() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val storage = remember(context.applicationContext) {
        MaxStudioStorage(context.applicationContext)
    }

    val projectsViewModel: ProjectsViewModel = viewModel(
        key = "max_studio_projects",
        factory = viewModelFactory {
            initializer { ProjectsViewModel(storage) }
        },
    )
    val studioViewModel: StudioViewModel = viewModel(
        key = "max_studio_editor",
        factory = viewModelFactory {
            initializer { StudioViewModel(storage) }
        },
    )

    val themeMode by projectsViewModel.themeMode.collectAsStateWithLifecycle()
    val allFilesGranted by projectsViewModel.allFilesGranted.collectAsStateWithLifecycle()
    val installUnknownGranted by projectsViewModel.installUnknownGranted.collectAsStateWithLifecycle()
    val activeProject by studioViewModel.project.collectAsStateWithLifecycle()
    val isCreating by projectsViewModel.isCreating.collectAsStateWithLifecycle()

    var showSplash by remember { mutableStateOf(true) }
    var showCreateProjectPage by remember { mutableStateOf(false) }
    var editingProject by remember { mutableStateOf<com.example.model.MaxProject?>(null) }
    var showAllFilesPromptOnLaunch by remember { mutableStateOf(false) }
    var didCheckAllFilesOnLaunch by remember { mutableStateOf(false) }
    var didCheckNotifPerm by remember { mutableStateOf(false) }

    val notificationPermLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ -> }

    // Sync /storage/emulated/0/.maxstudio and Install Unknown Apps status whenever user returns from Settings
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                projectsViewModel.refreshStorageAccessAndSync()
                if (projectsViewModel.hasAllFilesAccess()) {
                    showAllFilesPromptOnLaunch = false
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(showSplash) {
        if (!showSplash && !didCheckAllFilesOnLaunch) {
            kotlinx.coroutines.delay(200)
            if (!projectsViewModel.hasAllFilesAccess()) {
                showAllFilesPromptOnLaunch = true
            }
            didCheckAllFilesOnLaunch = true
        }
    }

    LaunchedEffect(showSplash, didCheckAllFilesOnLaunch, showAllFilesPromptOnLaunch) {
        if (!showSplash && didCheckAllFilesOnLaunch && !showAllFilesPromptOnLaunch && !didCheckNotifPerm) {
            didCheckNotifPerm = true
            kotlinx.coroutines.delay(300)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val granted = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS,
                ) == PackageManager.PERMISSION_GRANTED
                if (!granted) {
                    notificationPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        }
    }

    fun launchAllFilesSettings() {
        runCatching {
            context.startActivity(projectsViewModel.buildAllFilesPermissionIntent())
        }
    }

    fun launchInstallUnknownSettings() {
        runCatching {
            context.startActivity(projectsViewModel.buildInstallUnknownAppsIntent())
        }
    }

    MaxStudioTheme(themeMode = themeMode) {
        if (showSplash) {
            MaxStudioSplashScreen(
                onFinished = { showSplash = false },
            )
        } else {
            when {
                activeProject != null -> {
                        StudioScreen(
                            viewModel = studioViewModel,
                            onBackToProjects = {
                                studioViewModel.closeProject()
                            },
                        )
                    }
            showCreateProjectPage -> {
                CreateProjectScreen(
                    isCreating = isCreating,
                    initialProject = editingProject,
                    onPickLogoUri = { uri, onSaved ->
                        projectsViewModel.copyPickedLogo(uri, onSaved)
                    },
                    onCancel = {
                        showCreateProjectPage = false
                        editingProject = null
                    },
                    onCreate = { appName, projectName, packageName, backendMode, themeColors, versionCode, versionName, logoUri, logoCornerRadiusDp ->
                        val targetEdit = editingProject
                        if (targetEdit != null) {
                            projectsViewModel.updateProjectDetails(
                                existing = targetEdit,
                                appName = appName,
                                projectName = projectName,
                                packageName = packageName,
                                backendMode = backendMode,
                                themeColors = themeColors,
                                versionCode = versionCode,
                                versionName = versionName,
                                logoUri = logoUri,
                                logoCornerRadiusDp = logoCornerRadiusDp,
                            ) {
                                showCreateProjectPage = false
                                editingProject = null
                            }
                        } else {
                            projectsViewModel.createProject(
                                appName = appName,
                                projectName = projectName,
                                packageName = packageName,
                                backendMode = backendMode,
                                themeColors = themeColors,
                                versionCode = versionCode,
                                versionName = versionName,
                                logoUri = logoUri,
                                logoCornerRadiusDp = logoCornerRadiusDp,
                            ) { created ->
                                showCreateProjectPage = false
                                studioViewModel.openProject(created)
                            }
                        }
                    },
                )
            }
            else -> {
                ProjectsScreen(
                    viewModel = projectsViewModel,
                    onOpenProject = { selected ->
                        studioViewModel.openProject(selected)
                    },
                    onEditProject = { selected ->
                        editingProject = selected
                        showCreateProjectPage = true
                    },
                    onRunProjectDirectly = { selected ->
                        studioViewModel.openProject(selected)
                        studioViewModel.triggerApkBuild(context)
                    },
                    onOpenNewProject = {
                        editingProject = null
                        showCreateProjectPage = true
                    },
                    onRequestAllFilesPermission = { launchAllFilesSettings() },
                    onRequestInstallUnknownPermission = { launchInstallUnknownSettings() },
                )
            }
        }
        }

        // Prompt right after splash finishes for All Files Access to create /storage/emulated/0/.maxstudio
        if (!showSplash && showAllFilesPromptOnLaunch && !allFilesGranted) {
            AlertDialog(
                onDismissRequest = { showAllFilesPromptOnLaunch = false },
                title = { Text("Allow App Permissions") },
                text = {
                    Text(
                        "MAX STUDIO needs:\n" +
                            "1. All Files Access — to save & recover projects in /storage/emulated/0/.maxstudio.\n" +
                            "2. Install Unknown Apps — so built APKs trigger the Install popup right after compiling.",
                    )
                },
                confirmButton = {
                    Button(onClick = {
                        showAllFilesPromptOnLaunch = false
                        launchAllFilesSettings()
                    }) {
                        Text("Allow Storage")
                    }
                },
                dismissButton = {
                    if (!installUnknownGranted) {
                        TextButton(onClick = {
                            showAllFilesPromptOnLaunch = false
                            launchInstallUnknownSettings()
                        }) {
                            Text("Allow Install Apps")
                        }
                    } else {
                        TextButton(onClick = { showAllFilesPromptOnLaunch = false }) {
                            Text("Later")
                        }
                    }
                },
            )
        }
    }
}
