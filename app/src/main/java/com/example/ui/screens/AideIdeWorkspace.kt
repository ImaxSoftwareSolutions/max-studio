package com.example.ui.screens

import android.content.Intent
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.codegen.CodeGenerator
import com.example.model.MaxProject
import com.example.model.ProjectSourceFile
import com.example.ui.theme.JetBrainsMonoFontFamily
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AideIdeWorkspace(
    project: MaxProject,
    activeFilePath: String,
    openTabs: List<ProjectSourceFile>,
    onOpenFile: (String) -> Unit,
    onCloseTab: (String) -> Unit,
    onUpdateFileContent: (String, String) -> Unit,
    onCreateNewFile: (String, String, Boolean) -> Unit, // path, content, isJava
    onDeleteFile: (String) -> Unit,
    onImportImage: (android.net.Uri) -> Unit,
    onRemoveImage: (String) -> Unit,
    onNotify: (String) -> Unit,
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current

    var showFileTreeDrawer by remember { mutableStateOf(false) }
    var showNewFileDialog by remember { mutableStateOf(false) }
    var showImageManagerSheet by remember { mutableStateOf(false) }
    var isXmlPreviewMode by remember { mutableStateOf(false) }

    val currentFile = openTabs.firstOrNull { it.path == activeFilePath } ?: openTabs.firstOrNull() ?: ProjectSourceFile(
        path = "app/src/main/java/com/my/newproject/MainActivity.java",
        content = project.javaCode.ifBlank { CodeGenerator.generateJavaFromProject(project) },
        language = "JAVA",
    )

    val imagePickerLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) onImportImage(uri as Any as android.net.Uri)
    }

    val symbolButtons = listOf("{", "}", "(", ")", ";", "\"", "=", ".", "<", ">", "/", "+", "-")

    val javaSnippets = listOf(
        "OnClick" to "\nbutton1.setOnClickListener(v -> {\n    Toast.makeText(getApplicationContext(), \"Clicked!\", Toast.LENGTH_SHORT).show();\n});\n",
        "Toast" to "\nToast.makeText(getApplicationContext(), \"Message\", Toast.LENGTH_SHORT).show();\n",
        "AlertDialog" to "\nnew AlertDialog.Builder(this).setTitle(\"Info\").setMessage(\"Hello from AIDE\").setPositiveButton(\"OK\", null).show();\n",
        "Intent" to "\nIntent i = new Intent(getApplicationContext(), MainActivity.class);\nstartActivity(i);\n",
        "Handler Timer" to "\nnew Handler(Looper.getMainLooper()).postDelayed(() -> {\n    // Timer action\n}, 1000);\n",
    )

    Row(modifier = Modifier.fillMaxSize()) {
        // Desktop / Wide screen persistent File Tree Sidebar
        BoxWithConstraints(modifier = Modifier.fillMaxHeight()) {
            if (maxWidth >= 720.dp) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    modifier = Modifier.width(260.dp).fillMaxHeight(),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                ) {
                    FileTreeContent(
                        project = project,
                        activeFilePath = activeFilePath,
                        onOpenFile = onOpenFile,
                        onNewFileClick = { showNewFileDialog = true },
                        onImageManagerClick = { showImageManagerSheet = true },
                        onDeleteFile = onDeleteFile,
                    )
                }
            }
        }

        // Main Editor Area
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .background(MaterialTheme.colorScheme.surface),
        ) {
            // Top Toolbar: File Tree Toggle (on phone) + Open File Tabs Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                IconButton(
                    onClick = { showFileTreeDrawer = true },
                    modifier = Modifier.size(38.dp).testTag("open_file_tree_btn"),
                ) {
                    Icon(Icons.Filled.FolderOpen, contentDescription = "Open File Explorer", tint = MaterialTheme.colorScheme.primary)
                }

                // Open File Tabs
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    openTabs.forEach { tab ->
                        val isActive = tab.path == activeFilePath
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isActive) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    onOpenFile(tab.path)
                                    isXmlPreviewMode = false
                                },
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Icon(
                                    imageVector = when {
                                        tab.path.endsWith(".java") -> Icons.Filled.Code
                                        tab.path.endsWith(".xml") -> Icons.Filled.Dashboard
                                        else -> Icons.Filled.Description
                                    },
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                    tint = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Text(
                                    text = File(tab.path).name,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isActive) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                                )
                                if (openTabs.size > 1) {
                                    IconButton(
                                        onClick = { onCloseTab(tab.path) },
                                        modifier = Modifier.size(16.dp),
                                    ) {
                                        Icon(Icons.Filled.Close, contentDescription = "Close tab", modifier = Modifier.size(12.dp))
                                    }
                                }
                            }
                        }
                    }
                }

                // XML Preview Toggle if viewing an XML layout
                if (currentFile.path.endsWith(".xml") && currentFile.path.contains("res/layout")) {
                    FilterChip(
                        selected = isXmlPreviewMode,
                        onClick = { isXmlPreviewMode = !isXmlPreviewMode },
                        label = { Text("XML Preview") },
                        leadingIcon = { Icon(Icons.Filled.Visibility, contentDescription = null, modifier = Modifier.size(14.dp)) },
                    )
                }
            }

            // Symbol Accessory Bar for Coding ({ } ( ) ; =)
            if (!isXmlPreviewMode) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        items(symbolButtons) { symbol ->
                            OutlinedButton(
                                onClick = {
                                    onUpdateFileContent(currentFile.path, currentFile.content + symbol)
                                },
                                contentPadding = PaddingValues(0.dp),
                                modifier = Modifier.size(34.dp).testTag("sym_$symbol"),
                            ) {
                                Text(symbol, fontFamily = JetBrainsMonoFontFamily, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }
                    }
                }

                // Quick Java / XML Snippets bar
                if (currentFile.path.endsWith(".java")) {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        items(javaSnippets) { (label, snippet) ->
                            AssistChip(
                                onClick = { onUpdateFileContent(currentFile.path, currentFile.content + snippet) },
                                label = { Text("+ $label", style = MaterialTheme.typography.labelSmall) },
                            )
                        }
                    }
                }
            }

            // Editor / XML Preview Content Surface
            Surface(
                shape = RoundedCornerShape(0.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLowest,
                modifier = Modifier.weight(1f).fillMaxWidth(),
            ) {
                if (isXmlPreviewMode && currentFile.path.endsWith(".xml")) {
                    // Read-only Live XML Layout Preview
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Text(
                            text = "AIDE Live Layout Preview (${File(currentFile.path).name})",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        HorizontalDivider()
                        // Render project widgets
                        project.widgets.forEach { w ->
                            RenderWidgetVisualPreview(widget = w, interactive = true)
                        }
                    }
                } else {
                    // Code Editor
                    OutlinedTextField(
                        value = currentFile.content,
                        onValueChange = { newText ->
                            onUpdateFileContent(currentFile.path, newText)
                        },
                        textStyle = TextStyle(
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 13.sp,
                            lineHeight = 19.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                        ),
                        modifier = Modifier.fillMaxSize().testTag("aide_code_editor"),
                    )
                }
            }
        }
    }

    // Modal Drawer for File Explorer on Phones
    if (showFileTreeDrawer) {
        ModalBottomSheet(onDismissRequest = { showFileTreeDrawer = false }) {
            Box(modifier = Modifier.fillMaxWidth().height(480.dp)) {
                FileTreeContent(
                    project = project,
                    activeFilePath = activeFilePath,
                    onOpenFile = { path ->
                        onOpenFile(path)
                        showFileTreeDrawer = false
                        isXmlPreviewMode = false
                    },
                    onNewFileClick = { showNewFileDialog = true },
                    onImageManagerClick = { showImageManagerSheet = true },
                    onDeleteFile = onDeleteFile,
                )
            }
        }
    }

    // New File Dialog
    if (showNewFileDialog) {
        var newFileName by remember { mutableStateOf("") }
        var isJavaClass by remember { mutableStateOf(true) }
        AlertDialog(
            onDismissRequest = { showNewFileDialog = false },
            title = { Text(if (isJavaClass) "Create New Java Activity / Class" else "Create New XML Layout") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = newFileName,
                        onValueChange = { newFileName = it },
                        placeholder = { Text(if (isJavaClass) "SecondActivity" else "activity_second") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("new_file_name_input"),
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(selected = isJavaClass, onClick = { isJavaClass = true }, label = { Text("Java Class") })
                        FilterChip(selected = !isJavaClass, onClick = { isJavaClass = false }, label = { Text("XML Layout") })
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val clean = newFileName.trim().replace(Regex("[^a-zA-Z0-9_]"), "")
                        if (clean.isNotBlank()) {
                            val path = if (isJavaClass) {
                                val className = if (clean.endsWith("Activity", true)) clean else "${clean}Activity"
                                "app/src/main/java/${project.packageName.replace('.', '/')}/$className.java"
                            } else {
                                val layoutName = clean.removeSuffix(".xml").lowercase()
                                "app/src/main/res/layout/$layoutName.xml"
                            }
                            val initialContent = if (isJavaClass) {
                                "package ${project.packageName};\n\nimport android.os.Bundle;\nimport androidx.appcompat.app.AppCompatActivity;\n\npublic class ${File(path).nameWithoutExtension} extends AppCompatActivity {\n    @Override\n    protected void onCreate(Bundle savedInstanceState) {\n        super.onCreate(savedInstanceState);\n        setContentView(R.layout.main);\n    }\n}\n"
                            } else {
                                "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n<LinearLayout\n    xmlns:android=\"http://schemas.android.com/apk/res/android\"\n    android:layout_width=\"match_parent\"\n    android:layout_height=\"match_parent\"\n    android:orientation=\"vertical\">\n\n</LinearLayout>\n"
                            }
                            onCreateNewFile(path, initialContent, isJavaClass)
                            showNewFileDialog = false
                            showFileTreeDrawer = false
                        }
                    },
                    modifier = Modifier.testTag("create_new_file_confirm_btn"),
                ) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(onClick = { showNewFileDialog = false }) { Text("Cancel") }
            },
        )
    }

    // Image Manager Sheet
    if (showImageManagerSheet) {
        ModalBottomSheet(onDismissRequest = { showImageManagerSheet = false }) {
            Column(
                modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text("Image Manager (res/drawable)", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Button(
                    onClick = {
                        imagePickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp).testTag("import_image_picker_btn"),
                ) {
                    Icon(Icons.Filled.Image, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Import Image / Icon")
                }
                if (project.images.isEmpty()) {
                    Text("No images imported yet.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    LazyColumn(modifier = Modifier.height(240.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(project.images) { imgPath ->
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                                AsyncImage(
                                    model = File(imgPath),
                                    contentDescription = "Project image",
                                    modifier = Modifier.size(46.dp).clip(RoundedCornerShape(8.dp)),
                                )
                                Spacer(Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(File(imgPath).name, style = MaterialTheme.typography.bodyMedium, maxLines = 1)
                                    Text("@drawable/${File(imgPath).nameWithoutExtension}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                }
                                IconButton(onClick = { onRemoveImage(imgPath) }) {
                                    Icon(Icons.Filled.DeleteOutline, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FileTreeContent(
    project: MaxProject,
    activeFilePath: String,
    onOpenFile: (String) -> Unit,
    onNewFileClick: () -> Unit,
    onImageManagerClick: () -> Unit,
    onDeleteFile: (String) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Android IDE Workspace", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            IconButton(onClick = onNewFileClick, modifier = Modifier.testTag("add_file_tree_btn")) {
                Icon(Icons.Filled.Add, contentDescription = "New File")
            }
        }

        HorizontalDivider()

        // java package folder
        Text("java / ${project.packageName}", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        FileTreeItem(
            name = "MainActivity.java",
            path = "app/src/main/java/${project.packageName.replace('.', '/')}/MainActivity.java",
            activeFilePath = activeFilePath,
            onOpenFile = onOpenFile,
            canDelete = false,
            onDelete = {},
        )
        project.customViews.forEach { cv ->
            FileTreeItem(
                name = "${cv.activityClassName()}.java",
                path = "app/src/main/java/${project.packageName.replace('.', '/')}/${cv.activityClassName()}.java",
                activeFilePath = activeFilePath,
                onOpenFile = onOpenFile,
                canDelete = true,
                onDelete = { onDeleteFile(it) },
            )
        }

        Spacer(Modifier.height(4.dp))
        Text("res / layout", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        FileTreeItem(
            name = "activity_main.xml",
            path = "app/src/main/res/layout/activity_main.xml",
            activeFilePath = activeFilePath,
            onOpenFile = onOpenFile,
            canDelete = false,
            onDelete = {},
        )
        project.customViews.forEach { cv ->
            FileTreeItem(
                name = "${cv.name.lowercase()}.xml",
                path = "app/src/main/res/layout/${cv.name.lowercase()}.xml",
                activeFilePath = activeFilePath,
                onOpenFile = onOpenFile,
                canDelete = true,
                onDelete = { onDeleteFile(it) },
            )
        }

        Spacer(Modifier.height(4.dp))
        Text("res / drawable (Image Manager)", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.secondaryContainer,
            modifier = Modifier.fillMaxWidth().clickable(onClick = onImageManagerClick).testTag("file_tree_image_manager"),
        ) {
            Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Image, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text("Open Image Manager (${project.images.size} items)", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSecondaryContainer)
            }
        }

        Spacer(Modifier.height(4.dp))
        Text("res / values", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        FileTreeItem(
            name = "strings.xml",
            path = "app/src/main/res/values/strings.xml",
            activeFilePath = activeFilePath,
            onOpenFile = onOpenFile,
            canDelete = false,
            onDelete = {},
        )
        FileTreeItem(
            name = "colors.xml",
            path = "app/src/main/res/values/colors.xml",
            activeFilePath = activeFilePath,
            onOpenFile = onOpenFile,
            canDelete = false,
            onDelete = {},
        )

        Spacer(Modifier.height(4.dp))
        Text("manifests & build", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        FileTreeItem(
            name = "AndroidManifest.xml",
            path = "app/src/main/AndroidManifest.xml",
            activeFilePath = activeFilePath,
            onOpenFile = onOpenFile,
            canDelete = false,
            onDelete = {},
        )
        FileTreeItem(
            name = "build.gradle.kts",
            path = "app/build.gradle.kts",
            activeFilePath = activeFilePath,
            onOpenFile = onOpenFile,
            canDelete = false,
            onDelete = {},
        )
    }
}

@Composable
private fun FileTreeItem(
    name: String,
    path: String,
    activeFilePath: String,
    onOpenFile: (String) -> Unit,
    canDelete: Boolean,
    onDelete: (String) -> Unit,
) {
    val isActive = path == activeFilePath
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = if (isActive) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f) else Color.Transparent,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable { onOpenFile(path) },
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Icon(
                    imageVector = if (name.endsWith(".java")) Icons.Filled.Code else Icons.Filled.Description,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = name,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                    color = if (isActive) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                )
            }
            if (canDelete) {
                IconButton(onClick = { onDelete(path) }, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Filled.DeleteOutline, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(14.dp))
                }
            }
        }
    }
}
