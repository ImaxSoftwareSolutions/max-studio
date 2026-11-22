package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.model.BackendMode
import com.example.model.MaxProject
import com.example.model.ThemeColors
import com.example.model.ThemePresets
import java.io.File
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateProjectScreen(
    isCreating: Boolean,
    initialProject: MaxProject? = null,
    onPickLogoUri: (android.net.Uri, (String) -> Unit) -> Unit,
    onCancel: () -> Unit,
    onCreate: (
        appName: String,
        projectName: String,
        packageName: String,
        backendMode: BackendMode,
        themeColors: ThemeColors,
        versionCode: Int,
        versionName: String,
        logoUri: String,
        logoCornerRadiusDp: Int,
    ) -> Unit,
) {
    BackHandler { onCancel() }
    val isEditing = initialProject != null

    var appName by remember(initialProject?.id) { mutableStateOf(initialProject?.appName ?: "") }
    var packageName by remember(initialProject?.id) { mutableStateOf(initialProject?.packageName ?: "com.my.newproject") }
    var packageEditedManually by remember(initialProject?.id) { mutableStateOf(isEditing) }
    var projectName by remember(initialProject?.id) { mutableStateOf(initialProject?.projectName ?: "NewProject") }
    var projectEditedManually by remember(initialProject?.id) { mutableStateOf(isEditing) }
    val backendMode = BackendMode.JAVA_ONLY
    var themeColors by remember(initialProject?.id) { mutableStateOf(initialProject?.themeColors ?: ThemeColors()) }
    var versionCode by remember(initialProject?.id) { mutableIntStateOf(initialProject?.versionCode ?: 1) }
    var versionName by remember(initialProject?.id) { mutableStateOf(initialProject?.versionName ?: "1.0") }
    var logoPath by remember(initialProject?.id) { mutableStateOf(initialProject?.logoUri ?: "") }
    var logoCornerRadiusDp by remember(initialProject?.id) { mutableIntStateOf(initialProject?.logoCornerRadiusDp ?: 20) }

    var editingSwatchKey by remember { mutableStateOf<String?>(null) }
    var showVersionDialog by remember { mutableStateOf(false) }
    var showThemeHelp by remember { mutableStateOf(false) }

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            onPickLogoUri(uri) { savedPath ->
                logoPath = savedPath
            }
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (isEditing) "Edit Project (${initialProject?.numericId})" else "New Project",
                        fontWeight = FontWeight.SemiBold,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onCancel, modifier = Modifier.testTag("new_project_back")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.background,
                tonalElevation = 2.dp,
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    FilledTonalButton(
                        onClick = onCancel,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("cancel_create_project"),
                    ) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = {
                            onCreate(
                                appName.ifBlank { "My App" },
                                projectName.ifBlank { "NewProject" },
                                packageName.ifBlank { "com.my.newproject" },
                                backendMode,
                                themeColors,
                                versionCode,
                                versionName,
                                logoPath,
                                logoCornerRadiusDp,
                            )
                        },
                        enabled = !isCreating,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("confirm_create_project"),
                    ) {
                        Text(
                            when {
                                isCreating && isEditing -> "Saving..."
                                isCreating -> "Creating..."
                                isEditing -> "Save"
                                else -> "Create"
                            }
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // App Logo Preview with adjustable Corner Radius
            val iconShape = RoundedCornerShape(logoCornerRadiusDp.coerceIn(0, 44).dp)
            Box(
                modifier = Modifier.size(92.dp),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .size(86.dp)
                        .clip(iconShape)
                        .background(Color(0xFFF1F5F9))
                        .border(2.dp, MaterialTheme.colorScheme.outline, iconShape)
                        .clickable {
                            photoPicker.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        }
                        .testTag("pick_project_logo_btn"),
                    contentAlignment = Alignment.Center,
                ) {
                    if (logoPath.isNotBlank() && File(logoPath).exists()) {
                        AsyncImage(
                            model = File(logoPath),
                            contentDescription = "Uploaded app logo",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Filled.Android,
                            contentDescription = "Tap to upload app icon",
                            tint = Color(0xFFA4C639),
                            modifier = Modifier.size(48.dp),
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Filled.PhotoCamera,
                        contentDescription = "Upload logo",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(15.dp),
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = if (logoPath.isNotBlank()) "Logo uploaded! Tap to change" else "Tap to upload or change icon",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.height(10.dp))

            // Home-Screen Logo Corner Radius Control
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceContainer,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "Logo Corner Radius (Home Screen)",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = "${logoCornerRadiusDp}dp",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    Slider(
                        value = logoCornerRadiusDp.toFloat(),
                        onValueChange = { logoCornerRadiusDp = it.roundToInt().coerceIn(0, 44) },
                        valueRange = 0f..44f,
                        modifier = Modifier.testTag("logo_corner_radius_slider"),
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        listOf("Square" to 0, "Rounded" to 16, "Squircle" to 26, "Circle" to 44).forEach { (label, rad) ->
                            val selected = logoCornerRadiusDp == rad
                            OutlinedButton(
                                onClick = { logoCornerRadiusDp = rad },
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                border = BorderStroke(
                                    width = if (selected) 1.5.dp else 1.dp,
                                    color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                                ),
                                modifier = Modifier.weight(1f).height(32.dp),
                            ) {
                                Text(label, style = MaterialTheme.typography.labelSmall, maxLines = 1)
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = appName,
                onValueChange = { input ->
                    appName = input
                    val cleanSlug = input.lowercase().replace(Regex("[^a-z0-9]"), "")
                    val cleanProj = input.replace(Regex("[^a-zA-Z0-9_]"), "")
                    if (!packageEditedManually && cleanSlug.isNotEmpty()) {
                        packageName = "com.my.$cleanSlug"
                    }
                    if (!projectEditedManually && cleanProj.isNotEmpty()) {
                        projectName = cleanProj
                    }
                },
                placeholder = { Text("Enter application name") },
                leadingIcon = { Icon(Icons.Filled.Android, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth().testTag("input_app_name"),
            )

            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = packageName,
                onValueChange = {
                    packageName = it
                    packageEditedManually = true
                },
                label = { Text("Package name") },
                leadingIcon = { Icon(Icons.Filled.ViewInAr, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth().testTag("input_package_name"),
            )

            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = projectName,
                onValueChange = {
                    projectName = it
                    projectEditedManually = true
                },
                label = { Text("Project name (.mss)") },
                leadingIcon = { Icon(Icons.Filled.Category, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth().testTag("input_project_name"),
            )

            Spacer(Modifier.height(14.dp))

            // 5 Color Swatches Bar
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceContainer,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    val swatches = listOf(
                        "colorAccent" to themeColors.colorAccent,
                        "colorPrimary" to themeColors.colorPrimary,
                        "colorPrimaryDark" to themeColors.colorPrimaryDark,
                        "colorControlHighlight" to themeColors.colorControlHighlight,
                        "colorControlNormal" to themeColors.colorControlNormal,
                    )
                    swatches.forEach { (key, hex) ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { editingSwatchKey = key }
                                .padding(vertical = 4.dp),
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(parseHexOrDefault(hex, Color(0xFF5B84B1)))
                                    .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = key,
                                style = MaterialTheme.typography.labelSmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    IconButton(onClick = { showThemeHelp = true }, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Filled.HelpOutline, contentDescription = "Theme color help")
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            // Theme Presets Card
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surfaceContainer,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Palette, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(10.dp))
                            Text("Theme Presets", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        }
                        FilledTonalButton(
                            onClick = { themeColors = ThemeColors.random() },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        ) {
                            Text("Generate Random", style = MaterialTheme.typography.labelMedium)
                        }
                    }

                    Spacer(Modifier.height(10.dp))
                    Text(
                        "Choose from predefined themes or generate a random one",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        ThemePresets.all.forEach { preset ->
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { themeColors = preset.colors }
                                    .padding(4.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(42.dp)
                                        .clip(RoundedCornerShape(20.dp))
                                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(20.dp)),
                                ) {
                                    preset.swatches.forEach { swatchHex ->
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .fillMaxSize()
                                                .background(parseHexOrDefault(swatchHex, Color.Gray))
                                        )
                                    }
                                }
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    text = preset.name,
                                    style = MaterialTheme.typography.labelSmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            // Version Code / Version Name Card
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surfaceContainer,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .clickable { showVersionDialog = true }
                    .testTag("edit_version_card"),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(versionCode.toString(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text("Version code", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Icon(Icons.Filled.Hub, contentDescription = "Edit version", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(versionName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text("Version name", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }

    if (editingSwatchKey != null) {
        val key = editingSwatchKey!!
        val currentHex = when (key) {
            "colorAccent" -> themeColors.colorAccent
            "colorPrimary" -> themeColors.colorPrimary
            "colorPrimaryDark" -> themeColors.colorPrimaryDark
            "colorControlHighlight" -> themeColors.colorControlHighlight
            else -> themeColors.colorControlNormal
        }
        var hexInput by remember(key) { mutableStateOf(currentHex) }
        val palette = listOf("#3B82F6", "#6200EE", "#00E5FF", "#10B981", "#E53935", "#F59E0B", "#8B5CF6", "#111827")

        AlertDialog(
            onDismissRequest = { editingSwatchKey = null },
            title = { Text("Edit $key") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        palette.forEach { hex ->
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(parseHexOrDefault(hex, Color.Blue))
                                    .border(1.5.dp, MaterialTheme.colorScheme.outline, CircleShape)
                                    .clickable { hexInput = hex }
                            )
                        }
                    }
                    OutlinedTextField(
                        value = hexInput,
                        onValueChange = { hexInput = it },
                        label = { Text("Hex Color (#RRGGBB)") },
                        singleLine = true,
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    val safe = if (Regex("^#[0-9A-Fa-f]{6,8}$").matches(hexInput)) hexInput else currentHex
                    themeColors = when (key) {
                        "colorAccent" -> themeColors.copy(colorAccent = safe)
                        "colorPrimary" -> themeColors.copy(colorPrimary = safe)
                        "colorPrimaryDark" -> themeColors.copy(colorPrimaryDark = safe)
                        "colorControlHighlight" -> themeColors.copy(colorControlHighlight = safe)
                        else -> themeColors.copy(colorControlNormal = safe)
                    }
                    editingSwatchKey = null
                }) { Text("Apply") }
            },
            dismissButton = {
                TextButton(onClick = { editingSwatchKey = null }) { Text("Cancel") }
            },
        )
    }

    if (showVersionDialog) {
        var codeStr by remember { mutableStateOf(versionCode.toString()) }
        var nameStr by remember { mutableStateOf(versionName) }
        AlertDialog(
            onDismissRequest = { showVersionDialog = false },
            title = { Text("App Version Info") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = codeStr,
                        onValueChange = { codeStr = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Version Code") },
                        singleLine = true,
                    )
                    OutlinedTextField(
                        value = nameStr,
                        onValueChange = { nameStr = it },
                        label = { Text("Version Name") },
                        singleLine = true,
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    versionCode = codeStr.toIntOrNull()?.coerceAtLeast(1) ?: 1
                    versionName = nameStr.trim().ifBlank { "1.0" }
                    showVersionDialog = false
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showVersionDialog = false }) { Text("Cancel") }
            },
        )
    }

    if (showThemeHelp) {
        AlertDialog(
            onDismissRequest = { showThemeHelp = false },
            title = { Text("App Theme Colors") },
            text = { Text("Tap any of the 5 color swatches to customize your Android app's colors.xml attributes, or select a Material Preset below.") },
            confirmButton = {
                OutlinedButton(onClick = { showThemeHelp = false }) { Text("Got it") }
            },
        )
    }
}

fun parseHexOrDefault(hex: String, fallback: Color): Color {
    return try {
        Color(android.graphics.Color.parseColor(hex))
    } catch (_: Exception) {
        fallback
    }
}
