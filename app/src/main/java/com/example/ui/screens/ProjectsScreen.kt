package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SettingsBrightness
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.R
import com.example.data.WorkspaceFolderInfo
import com.example.model.FirebaseJsonConfig
import com.example.model.LibraryDep
import com.example.model.MaxProject
import com.example.ui.theme.EmeraldRun
import com.example.ui.theme.ThemeMode
import com.example.ui.viewmodel.MaxHubViewModel
import com.example.ui.viewmodel.ProjectsViewModel
import com.example.ui.viewmodel.UiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectsScreen(
    viewModel: ProjectsViewModel,
    onOpenProject: (MaxProject) -> Unit,
    onEditProject: (MaxProject) -> Unit,
    onRunProjectDirectly: (MaxProject) -> Unit,
    onOpenNewProject: () -> Unit,
    onRequestAllFilesPermission: () -> Unit,
    onRequestInstallUnknownPermission: () -> Unit,
) {
    val context = LocalContext.current
    val projectsState by viewModel.projectsState.collectAsStateWithLifecycle()
    val globalLibs by viewModel.globalLibraries.collectAsStateWithLifecycle()
    val globalFirebase by viewModel.globalFirebase.collectAsStateWithLifecycle()
    val folderSummary by viewModel.folderSummary.collectAsStateWithLifecycle()
    val linkedSafTree by viewModel.linkedSafTreeUri.collectAsStateWithLifecycle()
    val allFilesGranted by viewModel.allFilesGranted.collectAsStateWithLifecycle()
    val installUnknownGranted by viewModel.installUnknownGranted.collectAsStateWithLifecycle()
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val actionMessage by viewModel.actionMessage.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val maxHubViewModel = remember { MaxHubViewModel(context) }

    var bottomTab by remember { mutableIntStateOf(0) } // 0 = Projects, 1 = Store, 2 = MaxHub, 3 = Support
    var searchQuery by remember { mutableStateOf("") }
    var sortAlpha by remember { mutableStateOf(false) }
    var expandedProjectId by remember { mutableStateOf<String?>(null) }
    var deleteCandidate by remember { mutableStateOf<MaxProject?>(null) }
    var showRestoreSheet by remember { mutableStateOf(false) }
    var pendingSafExportProject by remember { mutableStateOf<MaxProject?>(null) }

    val mssImportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.importMssFromUri(uri)
            showRestoreSheet = false
        }
    }

    val mssSaveSafLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri: Uri? ->
        val proj = pendingSafExportProject
        if (uri != null && proj != null) {
            viewModel.exportMssToSafUri(proj, uri)
        }
        pendingSafExportProject = null
    }

    val safFolderLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        if (uri != null) viewModel.linkSafFolder(uri)
    }

    val libraryZipLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) viewModel.importLibraryArchive(uri)
    }

    val firebaseJsonLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) viewModel.importFirebaseJson(uri)
    }

    LaunchedEffect(actionMessage) {
        val msg = actionMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(msg)
        viewModel.clearActionMessage()
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                MaxStudioDrawerContent(
                    storageRootPath = viewModel.storageRootPath,
                    allFilesGranted = allFilesGranted,
                    installUnknownGranted = installUnknownGranted,
                    linkedSafTree = linkedSafTree,
                    folderSummary = folderSummary,
                    themeMode = themeMode,
                    onThemeModeChange = { viewModel.setThemeMode(it) },
                    onGrantAllFiles = {
                        scope.launch { drawerState.close() }
                        onRequestAllFilesPermission()
                    },
                    onGrantInstallUnknown = {
                        scope.launch { drawerState.close() }
                        onRequestInstallUnknownPermission()
                    },
                    onLinkSafFolder = { safFolderLauncher.launch(null) },
                    onOpenStoreTab = {
                        scope.launch { drawerState.close() }
                        bottomTab = 1
                    },
                    onOpenSupportTab = {
                        scope.launch { drawerState.close() }
                        bottomTab = 2
                    },
                )
            }
        },
    ) {
        Scaffold(
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            snackbarHost = { SnackbarHost(snackbarHostState) },
            floatingActionButton = {
                if (bottomTab == 0) {
                    ExtendedFloatingActionButton(
                        onClick = onOpenNewProject,
                        icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                        text = { Text("New project", fontWeight = FontWeight.SemiBold) },
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.testTag("new_project_fab"),
                    )
                }
            },
            bottomBar = {
                NavigationBar(
                    modifier = Modifier.navigationBarsPadding(),
                    containerColor = MaterialTheme.colorScheme.surface,
                ) {
                    NavigationBarItem(
                        selected = bottomTab == 0,
                        onClick = { bottomTab = 0 },
                        icon = { Icon(Icons.Filled.FormatListBulleted, contentDescription = null) },
                        label = { Text("Projects") },
                        modifier = Modifier.testTag("nav_projects"),
                    )
                    NavigationBarItem(
                        selected = bottomTab == 1,
                        onClick = { bottomTab = 1 },
                        icon = { Icon(Icons.Filled.Store, contentDescription = null) },
                        label = { Text("Store") },
                        modifier = Modifier.testTag("nav_store"),
                    )
                    NavigationBarItem(
                        selected = bottomTab == 2,
                        onClick = { bottomTab = 2 },
                        icon = { Icon(Icons.Filled.Store, contentDescription = null) },
                        label = { Text("MaxHub") },
                        modifier = Modifier.testTag("nav_maxhub"),
                    )
                    NavigationBarItem(
                        selected = bottomTab == 3,
                        onClick = { bottomTab = 3 },
                        icon = { Icon(Icons.Filled.SupportAgent, contentDescription = null) },
                        label = { Text("Support") },
                        modifier = Modifier.testTag("nav_support"),
                    )
                }
            },
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .statusBarsPadding(),
            ) {
                when (bottomTab) {
                    0 -> {
                        DashboardProjectsBody(
                            projectsState = projectsState,
                            allFilesGranted = allFilesGranted,
                            installUnknownGranted = installUnknownGranted,
                            storageRootPath = viewModel.storageRootPath,
                            searchQuery = searchQuery,
                            onSearchQueryChange = { searchQuery = it },
                            sortAlpha = sortAlpha,
                            onToggleSort = { sortAlpha = !sortAlpha },
                            expandedProjectId = expandedProjectId,
                            onToggleExpand = { id ->
                                expandedProjectId = if (expandedProjectId == id) null else id
                            },
                            onRequestAllFiles = onRequestAllFilesPermission,
                            onRequestInstallUnknown = onRequestInstallUnknownPermission,
                            onOpenDrawer = { scope.launch { drawerState.open() } },
                            onOpenRestore = { showRestoreSheet = true },
                            onOpenProject = onOpenProject,
                            onEditProject = onEditProject,
                            onRunProject = onRunProjectDirectly,
                            onExtractMss = { proj ->
                                viewModel.extractProjectToMss(proj) {
                                    pendingSafExportProject = it
                                    mssSaveSafLauncher.launch("${it.projectName.ifBlank { "project" }}.mss")
                                }
                            },
                            onDuplicate = { viewModel.duplicateProject(it) },
                            onDelete = { deleteCandidate = it },
                        )
                    }
                    1 -> {
                        StoreAndLibrariesTab(
                            globalFirebase = globalFirebase,
                            globalLibraries = globalLibs,
                            folderSummary = folderSummary,
                            storageRootPath = viewModel.storageRootPath,
                            onImportFirebaseJson = { firebaseJsonLauncher.launch(arrayOf("application/json", "*/*")) },
                            onImportLibraryZip = { libraryZipLauncher.launch(arrayOf("application/zip", "application/java-archive", "*/*")) },
                            onToggleLibrary = { viewModel.toggleLibrary(it) },
                            onOpenDrawer = { scope.launch { drawerState.open() } },
                        )
                    }
                    2 -> {
                        MaxHubScreen(
                            viewModel = maxHubViewModel,
                            onOpenDrawer = { scope.launch { drawerState.open() } },
                        )
                    }
                    else -> {
                        CustomerSupportTab(
                            onOpenDrawer = { scope.launch { drawerState.open() } },
                            onShowMessage = { msg ->
                                scope.launch { snackbarHostState.showSnackbar(msg) }
                            },
                        )
                    }
                }
            }
        }
    }

    if (showRestoreSheet) {
        ModalBottomSheet(onDismissRequest = { showRestoreSheet = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    "Restore & Recover Projects (.maxstudio / .mss)",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    "Projects are saved locally in ${viewModel.storageRootPath} so they stay safe even if MAX STUDIO is uninstalled.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Button(
                    onClick = {
                        viewModel.recoverProjectsFromStorage()
                        showRestoreSheet = false
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp).testTag("scan_maxstudio_btn"),
                ) {
                    Icon(Icons.Filled.Restore, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Auto-Recover from .maxstudio Folder")
                }

                FilledTonalButton(
                    onClick = { mssImportLauncher.launch(arrayOf("*/*")) },
                    modifier = Modifier.fillMaxWidth().height(48.dp).testTag("import_mss_file_btn"),
                ) {
                    Icon(Icons.Filled.UploadFile, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Import .mss Project File")
                }


                OutlinedButton(
                    onClick = { safFolderLauncher.launch(null) },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                ) {
                    Icon(Icons.Filled.FolderOpen, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Link File Manager Folder")
                }
                Spacer(Modifier.height(12.dp))
            }
        }
    }

    deleteCandidate?.let { target ->
        AlertDialog(
            onDismissRequest = { deleteCandidate = null },
            title = { Text("Delete Project?") },
            text = { Text("Remove \"${target.appName}\" (#${target.numericId})? Its .mss archive stays in .maxstudio/backups/ for recovery.") },
            confirmButton = {
                Button(onClick = {
                    viewModel.deleteProject(target)
                    deleteCandidate = null
                }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { deleteCandidate = null }) { Text("Cancel") }
            },
        )
    }
}

@Composable
private fun DashboardProjectsBody(
    projectsState: UiState<List<MaxProject>>,
    allFilesGranted: Boolean,
    installUnknownGranted: Boolean,
    storageRootPath: String,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    sortAlpha: Boolean,
    onToggleSort: () -> Unit,
    expandedProjectId: String?,
    onToggleExpand: (String) -> Unit,
    onRequestAllFiles: () -> Unit,
    onRequestInstallUnknown: () -> Unit,
    onOpenDrawer: () -> Unit,
    onOpenRestore: () -> Unit,
    onOpenProject: (MaxProject) -> Unit,
    onEditProject: (MaxProject) -> Unit,
    onRunProject: (MaxProject) -> Unit,
    onExtractMss: (MaxProject) -> Unit,
    onDuplicate: (MaxProject) -> Unit,
    onDelete: (MaxProject) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
    ) {
        Spacer(Modifier.height(8.dp))

        // Top Search Pill matching Screenshot 2
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceContainer,
            modifier = Modifier.fillMaxWidth().height(52.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 6.dp),
            ) {
                IconButton(onClick = onOpenDrawer, modifier = Modifier.testTag("drawer_menu_btn")) {
                    Icon(Icons.Filled.Menu, contentDescription = "Open menu")
                }
                Box(modifier = Modifier.weight(1f)) {
                    if (searchQuery.isEmpty()) {
                        Text(
                            "Search for projects...",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    BasicTextField(
                        value = searchQuery,
                        onValueChange = onSearchQueryChange,
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyLarge.copy(
                            color = MaterialTheme.colorScheme.onSurface,
                        ),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        modifier = Modifier.fillMaxWidth().testTag("search_projects_input"),
                    )
                }
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchQueryChange("") }) {
                        Icon(Icons.Filled.Close, contentDescription = "Clear search")
                    }
                } else {
                    IconButton(onClick = { }) {
                        Icon(Icons.Filled.Search, contentDescription = "Search")
                    }
                }
            }
        }

        // All Files Access Permission Banner if not granted yet
        if (!allFilesGranted) {
            Spacer(Modifier.height(10.dp))
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.errorContainer,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .clickable(onClick = onRequestAllFiles)
                    .testTag("grant_all_files_banner"),
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Filled.WarningAmber,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onErrorContainer,
                    )
                    Spacer(Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Allow All Files Access for .maxstudio",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                        )
                        Text(
                            "Tap here to create /storage/emulated/0/.maxstudio in File Manager hidden files",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                        )
                    }
                    Button(onClick = onRequestAllFiles, contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)) {
                        Text("Allow")
                    }
                }
            }
        }

        // Install Unknown Apps Permission Banner if not granted yet
        if (!installUnknownGranted) {
            Spacer(Modifier.height(8.dp))
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.secondaryContainer,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .clickable(onClick = onRequestInstallUnknown)
                    .testTag("grant_install_unknown_banner"),
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Filled.PlayArrow,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                    Spacer(Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Allow Install Unknown Apps",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                        )
                        Text(
                            "Required for automatic APK install popup right after build finishes",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                        )
                    }
                    FilledTonalButton(onClick = onRequestInstallUnknown, contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)) {
                        Text("Allow")
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // Restore Projects Card matching Screenshot 2
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surfaceContainer,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .clickable(onClick = onOpenRestore)
                .testTag("restore_projects_card"),
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Filled.Restore, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                }
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Restore Projects", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(
                        "Get your projects in a couple of clicks",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = "Open restore options",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        // Projects Section Header + Sort button
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                Text("Projects", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                Text(
                    text = storageRootPath,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (allFilesGranted) EmeraldRun else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            IconButton(onClick = onToggleSort, modifier = Modifier.testTag("sort_projects_btn")) {
                Icon(
                    Icons.AutoMirrored.Filled.Sort,
                    contentDescription = if (sortAlpha) "Sort by ID" else "Sort A-Z",
                    tint = if (sortAlpha) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                )
            }
        }

        when (projectsState) {
            is UiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            is UiState.Error -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(projectsState.message, color = MaterialTheme.colorScheme.error)
                }
            }
            is UiState.Success -> {
                val filtered = projectsState.data.filter { p ->
                    searchQuery.isBlank() ||
                        p.appName.contains(searchQuery, ignoreCase = true) ||
                        p.projectName.contains(searchQuery, ignoreCase = true) ||
                        p.packageName.contains(searchQuery, ignoreCase = true) ||
                        p.numericId.toString().contains(searchQuery)
                }.let { list ->
                    if (sortAlpha) list.sortedBy { it.appName.lowercase() }
                    else list.sortedByDescending { it.numericId }
                }

                if (filtered.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Filled.Folder,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(Modifier.height(10.dp))
                            Text("No projects found", style = MaterialTheme.typography.titleMedium)
                            Text(
                                "Tap + New project or Restore Projects above",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(top = 6.dp, bottom = 100.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        items(filtered, key = { it.id }) { project ->
                            DashboardProjectCard(
                                project = project,
                                expanded = expandedProjectId == project.id,
                                onCardClick = { onOpenProject(project) },
                                onLogoClick = { onEditProject(project) },
                                onToggleExpand = { onToggleExpand(project.id) },
                                onRunClick = { onRunProject(project) },
                                onExtractMss = { onExtractMss(project) },
                                onDuplicate = { onDuplicate(project) },
                                onDelete = { onDelete(project) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DashboardProjectCard(
    project: MaxProject,
    expanded: Boolean,
    onCardClick: () -> Unit,
    onLogoClick: () -> Unit,
    onToggleExpand: () -> Unit,
    onRunClick: () -> Unit,
    onExtractMss: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit,
) {
    val logoShape = RoundedCornerShape(((project.logoCornerRadiusDp.coerceIn(0, 44) * 58f) / 86f).dp)
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onCardClick)
            .testTag("project_card_${project.numericId}"),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(58.dp)
                        .clip(logoShape)
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, logoShape)
                        .clickable(onClick = onLogoClick)
                        .testTag("edit_logo_${project.numericId}"),
                    contentAlignment = Alignment.Center,
                ) {
                    val logoExists = remember(project.logoUri) {
                        project.logoUri.isNotBlank() && File(project.logoUri).exists()
                    }
                    if (logoExists) {
                        AsyncImage(
                            model = File(project.logoUri),
                            contentDescription = "Edit ${project.appName} settings",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                        )
                    } else {
                        Image(
                            painter = painterResource(R.drawable.launcher_max_icon),
                            contentDescription = "Edit ${project.appName} settings",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .background(Color.Black.copy(alpha = 0.65f))
                            .padding(vertical = 1.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = project.numericId.toString(),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }

                Spacer(Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = project.appName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false),
                        )
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer,
                        ) {
                            Text(
                                text = project.backendMode.badge,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            )
                        }
                    }
                    Text(
                        text = "${project.projectName} - ${project.versionName} (${project.versionCode})",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = project.packageName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                IconButton(onClick = onToggleExpand, modifier = Modifier.testTag("expand_proj_${project.numericId}")) {
                    Icon(
                        imageVector = if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                        contentDescription = "Project actions",
                    )
                }
            }

            AnimatedVisibility(visible = expanded) {
                Column {
                    Spacer(Modifier.height(10.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Button(
                            onClick = onRunClick,
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.weight(1f),
                        ) {
                            Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Run", maxLines = 1)
                        }
                        FilledTonalButton(
                            onClick = onExtractMss,
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            modifier = Modifier.weight(1.2f),
                        ) {
                            Icon(Icons.Filled.Archive, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Extract .mss", maxLines = 1)
                        }
                        IconButton(onClick = onLogoClick) {
                            Icon(Icons.Filled.Edit, contentDescription = "Edit project name & logo")
                        }
                        IconButton(onClick = onDuplicate) {
                            Icon(Icons.Filled.ContentCopy, contentDescription = "Duplicate project")
                        }
                        IconButton(onClick = onDelete) {
                            Icon(Icons.Filled.DeleteOutline, contentDescription = "Delete project", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StoreAndLibrariesTab(
    globalFirebase: FirebaseJsonConfig?,
    globalLibraries: List<LibraryDep>,
    folderSummary: List<WorkspaceFolderInfo>,
    storageRootPath: String,
    onImportFirebaseJson: () -> Unit,
    onImportLibraryZip: () -> Unit,
    onToggleLibrary: (String) -> Unit,
    onOpenDrawer: () -> Unit,
) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onOpenDrawer) {
                    Icon(Icons.Filled.Menu, contentDescription = "Menu")
                }
                Column {
                    Text("Store, Firebase & Libraries", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(
                        "Import google-services.json & Android Studio / Sketchware .zip libraries",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        // Firebase JSON File Import Placed Explicitly ABOVE Library Section
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.CloudUpload, contentDescription = null, tint = Color(0xFFFFA000))
                        Spacer(Modifier.width(10.dp))
                        Text("Firebase JSON Import (google-services.json)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    }
                    if (globalFirebase != null) {
                        Surface(
                            color = EmeraldRun.copy(alpha = 0.14f),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = EmeraldRun, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    "Connected: ${globalFirebase.projectId} (${globalFirebase.packageName.ifBlank { "default" }})",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = EmeraldRun,
                                    fontWeight = FontWeight.Medium,
                                )
                            }
                        }
                    } else {
                        Text(
                            "Import your project's google-services.json directly into .maxstudio/data/",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Button(
                        onClick = onImportFirebaseJson,
                        modifier = Modifier.fillMaxWidth().height(48.dp).testTag("import_firebase_json_btn"),
                    ) {
                        Icon(Icons.Filled.UploadFile, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(if (globalFirebase == null) "Import Firebase JSON" else "Replace Firebase JSON")
                    }
                }
            }
        }

        // Library Section directly BELOW Firebase JSON Import
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Extension, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(10.dp))
                        Text("Library Manager (ZIP / AAR / JAR)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    }
                    Text(
                        "Upload Android Studio or Sketchware libraries (.zip, .aar, .jar) saved to .maxstudio/libs/",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    FilledTonalButton(
                        onClick = onImportLibraryZip,
                        modifier = Modifier.fillMaxWidth().height(48.dp).testTag("import_library_zip_btn"),
                    ) {
                        Icon(Icons.Filled.Archive, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Import Library (.zip / .aar / .jar)")
                    }
                }
            }
        }

        items(globalLibraries, key = { it.id }) { lib ->
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceContainer,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(lib.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        Text(
                            "${lib.sourceType} • ${lib.coordinate}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Switch(checked = lib.enabled, onCheckedChange = { onToggleLibrary(lib.id) })
                }
            }
        }

        item {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(".maxstudio Storage Structure", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Text(storageRootPath, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(6.dp))
                    folderSummary.forEach { folder ->
                        Text("• ${folder.name}/ (${folder.itemCount} items)", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

@Composable
private fun MaxStudioDrawerContent(
    storageRootPath: String,
    allFilesGranted: Boolean,
    installUnknownGranted: Boolean,
    linkedSafTree: String?,
    folderSummary: List<WorkspaceFolderInfo>,
    themeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit,
    onGrantAllFiles: () -> Unit,
    onGrantInstallUnknown: () -> Unit,
    onLinkSafFolder: () -> Unit,
    onOpenStoreTab: () -> Unit,
    onOpenSupportTab: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(
                painter = painterResource(R.drawable.launcher_max_icon),
                contentDescription = "MAX STUDIO icon",
                modifier = Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)),
            )
            Spacer(Modifier.width(12.dp))
            Column {
                Text("MAX STUDIO", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("IMAX SOFTWARE SOLUTIONS", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        HorizontalDivider()

        Text("Theme (Light / Dark)", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ThemeMode.entries.forEach { mode ->
                val selected = themeMode == mode
                val icon = when (mode) {
                    ThemeMode.SYSTEM -> Icons.Filled.SettingsBrightness
                    ThemeMode.DARK -> Icons.Filled.DarkMode
                    ThemeMode.LIGHT -> Icons.Filled.LightMode
                }
                FilledTonalButton(
                    onClick = { onThemeModeChange(mode) },
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(icon, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = when (mode) {
                            ThemeMode.SYSTEM -> "Auto"
                            ThemeMode.DARK -> "Dark"
                            ThemeMode.LIGHT -> "Light"
                        },
                        maxLines = 1,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                    )
                }
            }
        }

        OutlinedButton(
            onClick = onOpenStoreTab,
            modifier = Modifier.fillMaxWidth().height(48.dp),
        ) {
            Icon(Icons.Filled.Extension, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Firebase JSON & Libraries")
        }

        OutlinedButton(
            onClick = onOpenSupportTab,
            modifier = Modifier.fillMaxWidth().height(48.dp).testTag("drawer_support_btn"),
        ) {
            Icon(Icons.Filled.SupportAgent, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Customer Support")
        }

        HorizontalDivider()

        Text(".maxstudio File Manager Folder", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
        Text(
            storageRootPath,
            style = MaterialTheme.typography.labelSmall,
            color = if (allFilesGranted) EmeraldRun else MaterialTheme.colorScheme.error,
        )
        if (!allFilesGranted) {
            Button(
                onClick = onGrantAllFiles,
                modifier = Modifier.fillMaxWidth().height(44.dp),
            ) {
                Icon(Icons.Filled.WarningAmber, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Grant All Files Access")
            }
        }
        if (!installUnknownGranted) {
            FilledTonalButton(
                onClick = onGrantInstallUnknown,
                modifier = Modifier.fillMaxWidth().height(44.dp),
            ) {
                Icon(Icons.Filled.PlayArrow, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Allow Install Unknown Apps")
            }
        } else {
            Text(
                "Install Unknown Apps: Enabled ✓",
                style = MaterialTheme.typography.labelSmall,
                color = EmeraldRun,
            )
        }
        if (linkedSafTree != null) {
            Text(
                "SAF Linked: ${linkedSafTree.take(42)}...",
                style = MaterialTheme.typography.labelSmall,
                color = EmeraldRun,
            )
        }
        FilledTonalButton(
            onClick = onLinkSafFolder,
            modifier = Modifier.fillMaxWidth().height(44.dp),
        ) {
            Icon(Icons.Filled.FolderOpen, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Link File Manager Folder")
        }

        LazyColumn(modifier = Modifier.weight(1f)) {
            items(folderSummary) { folder ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Filled.Folder, contentDescription = null, tint = AmberFolderColor, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(10.dp))
                    Text(folder.name, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                    Text("${folder.itemCount}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun CustomerSupportTab(
    onOpenDrawer: () -> Unit,
    onShowMessage: (String) -> Unit,
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    var isExportingApk by remember { mutableStateOf(false) }

    val saveSelfApkLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/vnd.android.package-archive")
    ) { destUri: Uri? ->
        if (destUri == null) return@rememberLauncherForActivityResult
        scope.launch {
            isExportingApk = true
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    val srcApk = File(context.applicationInfo.sourceDir)
                    if (!srcApk.exists()) error("Installed APK not found")
                    context.contentResolver.openOutputStream(destUri)?.use { out ->
                        srcApk.inputStream().use { input -> input.copyTo(out) }
                    } ?: error("Unable to open output location")
                }
            }
            isExportingApk = false
            onShowMessage(
                result.fold(
                    onSuccess = { "MAX_STUDIO.apk saved successfully!" },
                    onFailure = { "Export failed: ${it.message}" },
                ),
            )
        }
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onOpenDrawer) {
                    Icon(Icons.Filled.Menu, contentDescription = "Open menu")
                }
                Column {
                    Text(
                        text = "Customer Support",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "IMAX SOFTWARE SOLUTIONS • Help & APK",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        // Studio Identity Card
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Image(
                        painter = painterResource(R.drawable.launcher_max_icon),
                        contentDescription = "MAX STUDIO emblem",
                        modifier = Modifier.size(54.dp).clip(RoundedCornerShape(14.dp)),
                    )
                    Spacer(Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "IMAX SOFTWARE SOLUTIONS",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = "Official MAX STUDIO Support Desk. Direct help for project setup, blocks, XML & APK builds.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        // WhatsApp Support Card
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF25D366).copy(alpha = 0.16f),
                            modifier = Modifier.size(44.dp),
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Filled.Chat,
                                    contentDescription = "WhatsApp icon",
                                    tint = Color(0xFF25D366),
                                )
                            }
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "WhatsApp Support",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                text = "+91 9234825317",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF25D366),
                            )
                        }
                    }

                    Text(
                        text = "Tap below to chat directly on WhatsApp with MAX STUDIO Support.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Button(
                            onClick = {
                                val waUrl = "https://wa.me/919234825317?text=${
                                    Uri.encode("Hello IMAX SOFTWARE SOLUTIONS, I need help with MAX STUDIO.")
                                }"
                                val opened = runCatching {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(waUrl)).apply {
                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    }
                                    context.startActivity(intent)
                                }.isSuccess
                                if (!opened) {
                                    onShowMessage("WhatsApp not found — number copied: 9234825317")
                                    clipboard.setText(AnnotatedString("9234825317"))
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                                .testTag("support_whatsapp_btn"),
                        ) {
                            Icon(Icons.Filled.Chat, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Open WhatsApp")
                        }

                        OutlinedButton(
                            onClick = {
                                clipboard.setText(AnnotatedString("9234825317"))
                                onShowMessage("WhatsApp number copied: 9234825317")
                            },
                            modifier = Modifier
                                .heightIn(min = 48.dp)
                                .testTag("copy_whatsapp_btn"),
                        ) {
                            Icon(Icons.Filled.ContentCopy, contentDescription = "Copy WhatsApp number", modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Copy")
                        }
                    }
                }
            }
        }

        // Telegram Support Card
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF2AABEE).copy(alpha = 0.16f),
                            modifier = Modifier.size(44.dp),
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Send,
                                    contentDescription = "Telegram icon",
                                    tint = Color(0xFF2AABEE),
                                )
                            }
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Telegram Support",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                text = "@IMAXSOFTWARESOLUTIONS",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF2AABEE),
                            )
                        }
                    }

                    Text(
                        text = "Join or message our official Telegram channel/support handle @IMAXSOFTWARESOLUTIONS.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Button(
                            onClick = {
                                val openedTgApp = runCatching {
                                    val tgIntent = Intent(
                                        Intent.ACTION_VIEW,
                                        Uri.parse("tg://resolve?domain=IMAXSOFTWARESOLUTIONS"),
                                    ).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
                                    context.startActivity(tgIntent)
                                }.isSuccess
                                if (!openedTgApp) {
                                    val openedWeb = runCatching {
                                        val webIntent = Intent(
                                            Intent.ACTION_VIEW,
                                            Uri.parse("https://t.me/IMAXSOFTWARESOLUTIONS"),
                                        ).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
                                        context.startActivity(webIntent)
                                    }.isSuccess
                                    if (!openedWeb) {
                                        clipboard.setText(AnnotatedString("@IMAXSOFTWARESOLUTIONS"))
                                        onShowMessage("Telegram username copied: @IMAXSOFTWARESOLUTIONS")
                                    }
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                                .testTag("support_telegram_btn"),
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Open Telegram")
                        }

                        OutlinedButton(
                            onClick = {
                                clipboard.setText(AnnotatedString("@IMAXSOFTWARESOLUTIONS"))
                                onShowMessage("Telegram username copied: @IMAXSOFTWARESOLUTIONS")
                            },
                            modifier = Modifier
                                .heightIn(min = 48.dp)
                                .testTag("copy_telegram_btn"),
                        ) {
                            Icon(Icons.Filled.ContentCopy, contentDescription = "Copy Telegram handle", modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Copy")
                        }
                    }
                }
            }
        }

        // Export / Share Installed MAX STUDIO APK Card
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.Download,
                            contentDescription = "Export MAX STUDIO APK",
                            tint = EmeraldRun,
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = "MAX STUDIO APK Export & Share",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                    Text(
                        text = "Save MAX_STUDIO.apk directly to your Downloads folder or share the installer APK with another device.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        FilledTonalButton(
                            enabled = !isExportingApk,
                            onClick = { saveSelfApkLauncher.launch("MAX_STUDIO.apk") },
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                                .testTag("export_self_apk_btn"),
                        ) {
                            Icon(Icons.Filled.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(if (isExportingApk) "Exporting..." else "Save APK")
                        }

                        OutlinedButton(
                            enabled = !isExportingApk,
                            onClick = {
                                scope.launch {
                                    isExportingApk = true
                                    val shareUriResult = withContext(Dispatchers.IO) {
                                        runCatching {
                                            val srcApk = File(context.applicationInfo.sourceDir)
                                            if (!srcApk.exists()) error("APK source unavailable")
                                            val outFile = File(context.cacheDir, "MAX_STUDIO.apk")
                                            srcApk.copyTo(outFile, overwrite = true)
                                            FileProvider.getUriForFile(
                                                context,
                                                "${context.packageName}.fileprovider",
                                                outFile,
                                            )
                                        }
                                    }
                                    isExportingApk = false
                                    shareUriResult.onSuccess { uri ->
                                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                            type = "application/vnd.android.package-archive"
                                            putExtra(Intent.EXTRA_STREAM, uri)
                                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        }
                                        val chooser = Intent.createChooser(shareIntent, "Share MAX STUDIO APK").apply {
                                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        }
                                        context.startActivity(chooser)
                                    }.onFailure { err ->
                                        onShowMessage("Share failed: ${err.message}")
                                    }
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                                .testTag("share_self_apk_btn"),
                        ) {
                            Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Share APK")
                        }
                    }
                }
            }
        }
    }
}

private val AmberFolderColor = Color(0xFFF59E0B)
