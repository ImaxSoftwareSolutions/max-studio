package com.example.ui.screens

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Transform
import androidx.compose.material3.AssistChip
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.codegen.CodeGenerator
import com.example.model.BackendMode
import com.example.model.MaxProject
import com.example.ui.theme.JetBrainsMonoFontFamily

@Composable
fun JavaEditorTab(
    project: MaxProject,
    initialFileIndex: Int = 0,
    onJavaCodeChange: (String) -> Unit,
    onXmlOverrideChange: (String) -> Unit,
    onRegenerateFromBlocks: () -> Unit,
    onOpenJavaToBlocks: (() -> Unit)? = null,
    onNotify: (String) -> Unit,
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current

    var selectedFileIndex by remember(initialFileIndex) { mutableIntStateOf(initialFileIndex) }
    val isJavaReadOnly = project.backendMode == BackendMode.BLOCKS_ONLY

    val files = listOf(
        "MainActivity.java",
        "main.xml",
        "AndroidManifest.xml",
        "build.gradle",
    )

    val displayedContent = remember(project, selectedFileIndex) {
        when (selectedFileIndex) {
            1 -> project.xmlOverride.ifBlank { CodeGenerator.generateXmlLayout(project.widgets) }
            2 -> CodeGenerator.generateManifestXml(project)
            3 -> CodeGenerator.generateGradleDependencies(project)
            else -> project.javaCode
        }
    }

    val javaSnippets = listOf(
        "OnClick" to "\n// Click Listener\nbutton1.setOnClickListener(v -> {\n    Toast.makeText(getApplicationContext(), \"Clicked!\", Toast.LENGTH_SHORT).show();\n});\n",
        "Toast" to "\nToast.makeText(getApplicationContext(), \"Message\", Toast.LENGTH_SHORT).show();\n",
        "AlertDialog" to "\nnew AlertDialog.Builder(this).setTitle(\"Info\").setMessage(\"Hello from MAX STUDIO\").setPositiveButton(\"OK\", null).show();\n",
        "Intent" to "\nIntent i = new Intent(getApplicationContext(), MainActivity.class);\nstartActivity(i);\n",
        "SharedPrefs" to "\nSharedPreferences sp = getSharedPreferences(\"app\", MODE_PRIVATE);\nsp.edit().putString(\"key\", \"value\").apply();\n",
    )

    Column(
        modifier = Modifier.fillMaxSize().padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // File Tabs
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(files.indices.toList()) { idx ->
                FilterChip(
                    selected = selectedFileIndex == idx,
                    onClick = { selectedFileIndex = idx },
                    label = { Text(files[idx], style = MaterialTheme.typography.labelMedium) },
                )
            }
        }

        // Status / Actions Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (selectedFileIndex == 0 && isJavaReadOnly) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer,
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(Icons.Filled.Lock, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Blocks Mode (Read-Only Java)", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
                        }
                    }
                } else if (selectedFileIndex == 0 && project.backendMode == BackendMode.MIX_MODE && onOpenJavaToBlocks != null) {
                    OutlinedButton(
                        onClick = onOpenJavaToBlocks,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.height(36.dp).testTag("java_to_blocks_btn"),
                    ) {
                        Icon(Icons.Filled.Transform, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Java → Blocks", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
            Row {
                if (selectedFileIndex == 0) {
                    IconButton(onClick = onRegenerateFromBlocks) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Sync Java from Views & Blocks")
                    }
                }
                IconButton(onClick = {
                    clipboard.setText(AnnotatedString(displayedContent))
                    onNotify("Copied ${files[selectedFileIndex]}")
                }) {
                    Icon(Icons.Filled.ContentCopy, contentDescription = "Copy code")
                }
                IconButton(onClick = {
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_SUBJECT, "${project.appName} - ${files[selectedFileIndex]}")
                        putExtra(Intent.EXTRA_TEXT, displayedContent)
                    }
                    context.startActivity(Intent.createChooser(shareIntent, "Share ${files[selectedFileIndex]}"))
                }) {
                    Icon(Icons.Filled.Share, contentDescription = "Share code")
                }
            }
        }

        // Quick Java Snippets when Java is editable
        if (selectedFileIndex == 0 && !isJavaReadOnly) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(javaSnippets) { (label, snippet) ->
                    AssistChip(
                        onClick = { onJavaCodeChange(project.javaCode + snippet) },
                        label = { Text("+ $label", style = MaterialTheme.typography.labelSmall) },
                    )
                }
            }
        }

        // Code Editor / Viewer Surface
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLowest,
            modifier = Modifier.weight(1f).fillMaxWidth(),
        ) {
            when (selectedFileIndex) {
                0 -> {
                    if (isJavaReadOnly) {
                        Text(
                            text = project.javaCode,
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 12.5.sp,
                            lineHeight = 18.sp,
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(12.dp)
                                .testTag("java_code_readonly_viewer"),
                        )
                    } else {
                        OutlinedTextField(
                            value = project.javaCode,
                            onValueChange = onJavaCodeChange,
                            textStyle = TextStyle(
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 12.5.sp,
                                lineHeight = 18.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                            ),
                            modifier = Modifier.fillMaxSize().testTag("java_code_editor"),
                        )
                    }
                }
                1 -> {
                    OutlinedTextField(
                        value = displayedContent,
                        onValueChange = onXmlOverrideChange,
                        textStyle = TextStyle(
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 12.5.sp,
                            lineHeight = 18.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                        ),
                        modifier = Modifier.fillMaxSize().testTag("direct_xml_editor"),
                    )
                }
                else -> {
                    Text(
                        text = displayedContent,
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 12.5.sp,
                        lineHeight = 18.sp,
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(12.dp),
                    )
                }
            }
        }
    }
}
