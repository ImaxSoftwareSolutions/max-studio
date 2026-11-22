package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.InstallMobile
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.diagnostics.DiagnosticItem
import com.example.diagnostics.DiagnosticSeverity
import com.example.diagnostics.ProjectValidationReport
import com.example.model.MaxProject
import com.example.runtime.rememberBlockRuntimeController
import com.example.service.BuildPipelineController
import com.example.ui.theme.EmeraldRun
import com.example.ui.theme.JetBrainsMonoFontFamily

@Composable
fun RunBuildTab(
    project: MaxProject,
    diagnosticsReport: ProjectValidationReport = ProjectValidationReport(),
    selectedSubMode: Int = 0,
    onSelectSubMode: (Int) -> Unit = {},
    installUnknownGranted: Boolean = true,
    onStartBuild: () -> Unit,
    onExtractMss: () -> Unit,
    onOpenInstallPopup: () -> Unit = {},
    onSaveApk: () -> Unit = {},
    onShareApk: () -> Unit = {},
    onRequestInstallPermission: () -> Unit = {},
) {
    val buildState by BuildPipelineController.state.collectAsStateWithLifecycle()
    val clipboard = LocalClipboardManager.current

    LaunchedEffect(buildState.buildFailed) {
        if (buildState.buildFailed) {
            onSelectSubMode(2)
        }
    }

    val totalErrorLogs = diagnosticsReport.items.size + buildState.runtimeErrors.size
    val hasErrors = diagnosticsReport.errorCount > 0 || buildState.runtimeErrors.isNotEmpty() || buildState.buildFailed

    Column(
        modifier = Modifier.fillMaxSize().padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Primary Build + Extract .mss Action Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Button(
                onClick = onStartBuild,
                enabled = !buildState.isBuilding,
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                modifier = Modifier.weight(1.3f).height(48.dp).testTag("run_build_apk_btn"),
            ) {
                Icon(Icons.Filled.PlayArrow, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text(if (buildState.isBuilding) "Building (${buildState.progressPercent}%)..." else "Build & Run APK")
            }
            FilledTonalButton(
                onClick = onExtractMss,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
                modifier = Modifier.weight(1f).height(48.dp).testTag("extract_mss_btn"),
            ) {
                Icon(Icons.Filled.Archive, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Extract .mss")
            }
        }

        // Toggle between Compiler Terminal, Live App Preview, & Error Logs
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FilterChip(
                selected = selectedSubMode == 0,
                onClick = { onSelectSubMode(0) },
                label = { Text("Compiler Log") },
                modifier = Modifier.testTag("build_tab_compiler"),
            )
            FilterChip(
                selected = selectedSubMode == 1,
                onClick = { onSelectSubMode(1) },
                label = { Text("Live App") },
                modifier = Modifier.testTag("build_tab_live_app"),
            )
            FilterChip(
                selected = selectedSubMode == 2,
                onClick = { onSelectSubMode(2) },
                label = {
                    Text(
                        text = if (totalErrorLogs > 0) "Error Logs ($totalErrorLogs)" else "Error Logs",
                        fontWeight = if (hasErrors) FontWeight.Bold else FontWeight.Medium,
                    )
                },
                leadingIcon = if (hasErrors) {
                    {
                        Icon(
                            imageVector = Icons.Filled.Error,
                            contentDescription = "Errors present",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.error,
                        )
                    }
                } else null,
                colors = if (hasErrors) {
                    FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.errorContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onErrorContainer,
                    )
                } else {
                    FilterChipDefaults.filterChipColors()
                },
                modifier = Modifier.testTag("build_tab_error_logs"),
            )
        }

        when (selectedSubMode) {
            0 -> {
                // Build Progress Card + Console Lines
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (buildState.buildFailed) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.45f)
                        else MaterialTheme.colorScheme.surfaceContainer
                    ),
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Icon(
                                    imageVector = when {
                                        buildState.buildFailed -> Icons.Filled.Error
                                        buildState.completedSuccess -> Icons.Filled.CheckCircle
                                        else -> Icons.Filled.Build
                                    },
                                    contentDescription = null,
                                    tint = when {
                                        buildState.buildFailed -> MaterialTheme.colorScheme.error
                                        buildState.completedSuccess -> EmeraldRun
                                        else -> MaterialTheme.colorScheme.primary
                                    },
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = buildState.stageLabel,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            Text(
                                text = "${buildState.progressPercent}%",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = when {
                                    buildState.buildFailed -> MaterialTheme.colorScheme.error
                                    buildState.completedSuccess -> EmeraldRun
                                    else -> MaterialTheme.colorScheme.primary
                                },
                            )
                        }

                        LinearProgressIndicator(
                            progress = { (buildState.progressPercent / 100f).coerceIn(0f, 1f) },
                            color = if (buildState.buildFailed) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                        )

                        if (buildState.buildFailed || hasErrors) {
                            OutlinedButton(
                                onClick = { onSelectSubMode(2) },
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                                modifier = Modifier.fillMaxWidth().testTag("open_error_logs_from_compiler"),
                            ) {
                                Icon(Icons.Filled.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    "Open Error Logs ($totalErrorLogs issue(s))",
                                    color = MaterialTheme.colorScheme.error,
                                )
                            }
                        }

                        if (buildState.completedSuccess) {
                            Text(
                                "APK packaged: ${buildState.outputApkName} • .mss backed up locally",
                                style = MaterialTheme.typography.bodySmall,
                                color = EmeraldRun,
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                FilledTonalButton(
                                    onClick = onOpenInstallPopup,
                                    modifier = Modifier.weight(1f).testTag("run_install_app_btn"),
                                ) {
                                    Icon(Icons.Filled.InstallMobile, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Install App")
                                }
                                if (!installUnknownGranted) {
                                    OutlinedButton(
                                        onClick = onRequestInstallPermission,
                                        modifier = Modifier.weight(1f),
                                    ) {
                                        Text("Allow Install")
                                    }
                                }
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                OutlinedButton(
                                    onClick = onSaveApk,
                                    modifier = Modifier.weight(1f).testTag("run_save_apk_btn"),
                                ) {
                                    Icon(Icons.Filled.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Save APK")
                                }
                                OutlinedButton(
                                    onClick = onShareApk,
                                    modifier = Modifier.weight(1f).testTag("run_share_apk_btn"),
                                ) {
                                    Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Share APK")
                                }
                            }
                        }
                    }
                }

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLowest,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(14.dp)),
                ) {
                    if (buildState.logs.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize().padding(20.dp), contentAlignment = Alignment.Center) {
                            Text(
                                "Tap \"Build & Run APK\" above to run AAPT2 XML compilation, javac, D8 dexing, and diagnostics.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            items(buildState.logs) { line ->
                                val lineColor = when {
                                    line.contains("[FATAL]") || line.contains("[ERROR]") || line.contains("[BUILD FAILED]") ->
                                        MaterialTheme.colorScheme.error
                                    line.contains("[WARN]") -> Color(0xFFF59E0B)
                                    line.contains("SUCCESS") -> EmeraldRun
                                    else -> MaterialTheme.colorScheme.onSurface
                                }
                                Text(
                                    text = line,
                                    fontFamily = JetBrainsMonoFontFamily,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = lineColor,
                                )
                            }
                        }
                    }
                }
            }

            1 -> {
                // Live App Runtime Execution Preview (Timers + Activity switching + Blocks)
                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    LiveAppRuntimeSurface(
                        project = project,
                        showExitButton = false,
                        onDismiss = {},
                        onRecordError = { BuildPipelineController.recordRuntimeError(it) },
                    )
                }
            }

            else -> {
                // Error Logs Viewer
                ErrorLogsPanel(
                    diagnostics = diagnosticsReport.items,
                    runtimeErrors = buildState.runtimeErrors,
                    onCopyAll = {
                        val combined = buildString {
                            if (buildState.runtimeErrors.isNotEmpty()) {
                                appendLine("=== RUNTIME ERRORS ===")
                                buildState.runtimeErrors.forEachIndexed { idx, e -> appendLine("${idx + 1}. $e") }
                                appendLine()
                            }
                            if (diagnosticsReport.items.isNotEmpty()) {
                                appendLine("=== PROJECT / COMPILE LOGS ===")
                                diagnosticsReport.items.forEachIndexed { idx, d ->
                                    appendLine("${idx + 1}. [${d.severity.name}] ${d.source}: ${d.message}")
                                    if (d.hint.isNotBlank()) appendLine("   Fix: ${d.hint}")
                                }
                            }
                        }.ifBlank { "No errors found." }
                        clipboard.setText(AnnotatedString(combined))
                    },
                    onClearRuntime = { BuildPipelineController.clearRuntimeErrors() },
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
fun LiveAppRuntimeSurface(
    project: MaxProject,
    showExitButton: Boolean,
    onDismiss: () -> Unit,
    onRecordError: (String) -> Unit,
) {
    var runKey by remember { mutableIntStateOf(0) }
    val controller = rememberBlockRuntimeController(
        project = project,
        runKey = runKey,
        onExternalError = onRecordError,
        onExitRequested = onDismiss,
    )

    val widgets = controller.currentWidgets()

    Surface(
        shape = if (showExitButton) RoundedCornerShape(0.dp) else RoundedCornerShape(20.dp),
        color = Color.White,
        modifier = Modifier
            .fillMaxSize()
            .then(
                if (!showExitButton) Modifier.border(1.5.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(20.dp))
                else Modifier
            ),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // App Toolbar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(parseHexOrDefault(project.themeColors.colorPrimary, Color(0xFF1976D2)))
                    .then(if (showExitButton) Modifier.statusBarsPadding() else Modifier)
                    .padding(horizontal = 8.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (controller.isSecondaryScreen() || showExitButton) {
                    IconButton(onClick = { controller.handleBackPress() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                } else {
                    Spacer(Modifier.width(8.dp))
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = controller.currentScreenTitle(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = "${project.packageName} • ${controller.currentScreenName}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.85f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (controller.runtimeErrors.isNotEmpty()) {
                    FilledTonalButton(
                        onClick = { controller.showErrorLogDialog = true },
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.height(40.dp).testTag("live_preview_error_badge"),
                    ) {
                        Text(
                            "ERR (${controller.runtimeErrors.size})",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                    Spacer(Modifier.width(4.dp))
                }
                IconButton(
                    onClick = {
                        runKey++
                    },
                    modifier = Modifier.testTag("live_preview_restart_btn"),
                ) {
                    Icon(Icons.Filled.Refresh, contentDescription = "Restart Preview", tint = Color.White)
                }
            }

            controller.activeBanner?.let { msg ->
                val isErr = msg.startsWith("Error:")
                Surface(
                    color = if (isErr) Color(0xFFFEE2E2) else EmeraldRun.copy(alpha = 0.14f),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(10.dp),
                ) {
                    Text(
                        text = msg,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isErr) Color(0xFFB91C1C) else Color(0xFF065F46),
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(10.dp),
                    )
                }
            }

            if (widgets.isEmpty()) {
                Box(
                    modifier = Modifier.weight(1f).fillMaxWidth().padding(24.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "${controller.currentScreenTitle()} active hai!\nIs screen par widgets add karein.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF475569),
                    )
                }
            } else {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .then(if (showExitButton) Modifier.navigationBarsPadding() else Modifier)
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    widgets.forEach { baseWidget ->
                        val vis = controller.isWidgetVisible(baseWidget.id)
                        if (vis != "GONE") {
                            val resolved = controller.resolvedWidget(baseWidget)
                            val enabled = controller.isWidgetEnabled(baseWidget.id)
                            Box(modifier = Modifier.alpha(if (vis == "INVISIBLE") 0f else if (enabled) 1f else 0.5f)) {
                                RenderWidgetVisualPreview(
                                    widget = resolved,
                                    interactive = enabled,
                                    onButtonClick = { controller.onWidgetClicked(baseWidget.id) },
                                    editValue = controller.widgetText[baseWidget.id] ?: resolved.text,
                                    onEditChange = { controller.onEditTextChanged(baseWidget.id, it) },
                                    checkedValue = controller.widgetChecked[baseWidget.id] ?: resolved.checked,
                                    onCheckedChange = { controller.onCheckedChanged(baseWidget.id, it) },
                                    progressValue = controller.widgetProgress[baseWidget.id] ?: resolved.progress,
                                    onProgressChange = { controller.onProgressChanged(baseWidget.id, it) },
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    controller.activeDialog?.let { (title, message) ->
        AlertDialog(
            onDismissRequest = { controller.dismissDialog() },
            title = { Text(title) },
            text = { Text(message) },
            confirmButton = {
                TextButton(onClick = { controller.dismissDialog() }) { Text("OK") }
            },
        )
    }

    if (controller.showErrorLogDialog) {
        AlertDialog(
            onDismissRequest = { controller.showErrorLogDialog = false },
            title = { Text("Runtime Error Logs (${controller.runtimeErrors.size})") },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    controller.runtimeErrors.forEachIndexed { idx, err ->
                        Text("${idx + 1}. $err", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { controller.showErrorLogDialog = false }) { Text("Close") }
            },
        )
    }
}

@Composable
private fun ErrorLogsPanel(
    diagnostics: List<DiagnosticItem>,
    runtimeErrors: List<String>,
    onCopyAll: () -> Unit,
    onClearRuntime: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedButton(
                onClick = onCopyAll,
                modifier = Modifier.weight(1f).testTag("copy_error_logs_btn"),
            ) {
                Icon(Icons.Filled.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Copy Logs")
            }
            if (runtimeErrors.isNotEmpty()) {
                OutlinedButton(
                    onClick = onClearRuntime,
                    modifier = Modifier.weight(1f).testTag("clear_runtime_errors_btn"),
                ) {
                    Icon(Icons.Filled.DeleteSweep, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Clear Runtime")
                }
            }
        }

        Surface(
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLowest,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(14.dp)),
        ) {
            if (diagnostics.isEmpty() && runtimeErrors.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize().padding(20.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = EmeraldRun, modifier = Modifier.size(38.dp))
                        Text(
                            "Zero Errors Found!",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldRun,
                        )
                        Text(
                            "Project me koi compile, syntax, ya block wiring error nahi mila.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    if (runtimeErrors.isNotEmpty()) {
                        items(runtimeErrors) { errMsg ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFFFFF1F2),
                                border = BorderStroke(1.dp, Color(0xFFFDA4AF)),
                            ) {
                                Column(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(
                                        text = "RUNTIME ERROR",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFBE123C),
                                    )
                                    Text(
                                        text = errMsg,
                                        fontFamily = JetBrainsMonoFontFamily,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFF881337),
                                    )
                                }
                            }
                        }
                    }

                    items(diagnostics, key = { it.id }) { item ->
                        val (bgColor, borderColor, badgeColor) = when (item.severity) {
                            DiagnosticSeverity.FATAL -> Triple(Color(0xFFFEF2F2), Color(0xFFFCA5A5), Color(0xFFB91C1C))
                            DiagnosticSeverity.ERROR -> Triple(Color(0xFFFFF7ED), Color(0xFFFDBA74), Color(0xFFC2410C))
                            DiagnosticSeverity.WARNING -> Triple(Color(0xFFFFFBEB), Color(0xFFFDE68A), Color(0xFFB45309))
                        }
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = bgColor,
                            border = BorderStroke(1.dp, borderColor),
                        ) {
                            Column(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        text = item.severity.name,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = badgeColor,
                                    )
                                    Text(
                                        text = item.source,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFF334155),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                                Text(
                                    text = item.message,
                                    fontFamily = JetBrainsMonoFontFamily,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF0F172A),
                                )
                                if (item.hint.isNotBlank()) {
                                    Text(
                                        text = "Fix: ${item.hint}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFF475569),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

