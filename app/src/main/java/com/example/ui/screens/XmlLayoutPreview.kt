package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.WidgetNode
import com.example.model.WidgetType
import kotlin.math.roundToInt

@Composable
fun RenderWidgetVisualPreview(
    widget: WidgetNode,
    interactive: Boolean = false,
    onButtonClick: () -> Unit = {},
    editValue: String = widget.text,
    onEditChange: (String) -> Unit = {},
    checkedValue: Boolean = widget.checked,
    onCheckedChange: (Boolean) -> Unit = {},
    progressValue: Int = widget.progress,
    onProgressChange: (Int) -> Unit = {},
) {
    val bgColor = if (widget.backgroundColor == "#00000000") Color.Transparent
    else parseWidgetHexColor(widget.backgroundColor, Color(0xFFE5E7EB))
    val txtColor = parseWidgetHexColor(widget.textColor, Color(0xFF111827))
    val align = when (widget.gravity) {
        "CENTER" -> Alignment.CenterHorizontally
        "END" -> Alignment.End
        else -> Alignment.Start
    }

    when (widget.type) {
        WidgetType.LINEAR_V, WidgetType.LINEAR_H, WidgetType.SCROLL_V, WidgetType.SCROLL_H,
        WidgetType.RADIO_GROUP, WidgetType.RELATIVE_LAYOUT, WidgetType.SWIPE_REFRESH, WidgetType.COLLAPSING_TOOLBAR -> {
            val containerBg = if (bgColor == Color.Transparent) Color(0xFFE5E7EB) else bgColor
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 50.dp)
                    .clip(RoundedCornerShape(widget.cornerRadius.dp))
                    .background(containerBg)
                    .border(1.dp, Color(0xFFCBD5E1), RoundedCornerShape(widget.cornerRadius.dp))
                    .padding(widget.paddingDp.dp),
            ) {
                Text(
                    text = widget.text.ifBlank { "${widget.type.displayName} (${widget.id})" },
                    color = txtColor,
                    fontSize = 12.sp,
                )
            }
        }
        WidgetType.TAB_LAYOUT, WidgetType.BOTTOM_NAV -> {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (bgColor == Color.Transparent) Color(0xFF1976D2) else bgColor)
                    .padding(10.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                Text("Tab 1", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Text("Tab 2", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                Text("Tab 3", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
            }
        }
        WidgetType.CARD_VIEW -> {
            Card(
                shape = RoundedCornerShape(widget.cornerRadius.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (bgColor == Color.Transparent) Color.White else bgColor,
                ),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = widget.text.ifBlank { "CardView Banner" },
                    color = txtColor,
                    fontSize = widget.textSize.sp,
                    modifier = Modifier.padding(widget.paddingDp.dp),
                )
            }
        }
        WidgetType.TEXT_VIEW -> {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(widget.cornerRadius.dp))
                    .background(bgColor)
                    .padding(widget.paddingDp.dp),
                horizontalAlignment = align,
            ) {
                Text(
                    text = widget.text.ifBlank { "TextView" },
                    color = txtColor,
                    fontSize = widget.textSize.sp,
                )
            }
        }
        WidgetType.BUTTON, WidgetType.MATERIAL_BUTTON, WidgetType.SIGN_IN_BUTTON -> {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(widget.cornerRadius.dp))
                    .background(if (bgColor == Color.Transparent) Color(0xFF2196F3) else bgColor)
                    .then(if (interactive) Modifier.clickable(onClick = onButtonClick) else Modifier)
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = widget.text.ifBlank { widget.type.displayName },
                    color = txtColor,
                    fontSize = widget.textSize.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
        WidgetType.EDIT_TEXT, WidgetType.AUTO_COMPLETE, WidgetType.MULTI_AUTO_COMPLETE,
        WidgetType.TEXT_INPUT_LAYOUT, WidgetType.SEARCH_VIEW -> {
            OutlinedTextField(
                value = editValue,
                onValueChange = { if (interactive) onEditChange(it) },
                readOnly = !interactive,
                placeholder = { Text(widget.hint.ifBlank { widget.type.displayName }) },
                singleLine = true,
                shape = RoundedCornerShape(widget.cornerRadius.dp),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        WidgetType.IMAGE_VIEW, WidgetType.CIRCLE_IMAGE_VIEW -> {
            val shape = if (widget.type == WidgetType.CIRCLE_IMAGE_VIEW) CircleShape else RoundedCornerShape(widget.cornerRadius.dp)
            Box(
                modifier = Modifier
                    .then(if (widget.type == WidgetType.CIRCLE_IMAGE_VIEW) Modifier.size(84.dp) else Modifier.fillMaxWidth().height(92.dp))
                    .clip(shape)
                    .background(if (bgColor == Color.Transparent) Color(0xFFE2E8F0) else bgColor),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Filled.Image, contentDescription = null, modifier = Modifier.size(30.dp), tint = Color(0xFF2196F3))
                    Text(widget.text.ifBlank { widget.id }, fontSize = 11.sp, color = Color(0xFF334155))
                }
            }
        }
        WidgetType.CHECK_BOX -> {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(2.dp)) {
                Checkbox(checked = checkedValue, onCheckedChange = { if (interactive) onCheckedChange(it) })
                Text(widget.text.ifBlank { "CheckBox" }, color = txtColor, fontSize = widget.textSize.sp)
            }
        }
        WidgetType.RADIO_BUTTON -> {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(2.dp)) {
                RadioButton(selected = checkedValue, onClick = { if (interactive) onCheckedChange(!checkedValue) })
                Text(widget.text.ifBlank { "RadioButton" }, color = txtColor, fontSize = widget.textSize.sp)
            }
        }
        WidgetType.SWITCH -> {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 2.dp),
            ) {
                Text(widget.text.ifBlank { "Switch" }, color = txtColor, fontSize = widget.textSize.sp)
                Switch(checked = checkedValue, onCheckedChange = { if (interactive) onCheckedChange(it) })
            }
        }
        WidgetType.SEEK_BAR, WidgetType.RATING_BAR -> {
            Slider(
                value = progressValue / 100f,
                onValueChange = { if (interactive) onProgressChange((it * 100).roundToInt()) },
                modifier = Modifier.fillMaxWidth(),
            )
        }
        WidgetType.PROGRESS_BAR -> {
            LinearProgressIndicator(
                progress = { progressValue / 100f },
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
            )
        }
        WidgetType.SPINNER -> {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (bgColor == Color.Transparent) Color(0xFFF1F5F9) else bgColor,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(widget.itemsCsv.split(",").firstOrNull() ?: "Select Item", color = txtColor, fontSize = 13.sp)
                    Icon(Icons.AutoMirrored.Filled.List, contentDescription = null, tint = Color(0xFF475569))
                }
            }
        }
        WidgetType.LIST_VIEW, WidgetType.GRID_VIEW, WidgetType.RECYCLER_VIEW, WidgetType.VIEW_PAGER -> {
            val rows = widget.itemsCsv.split(",").map { it.trim() }.filter { it.isNotEmpty() }
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = if (bgColor == Color.Transparent) Color(0xFFF8FAFC) else bgColor,
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(widget.type.displayName, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2196F3))
                    rows.take(3).forEach { row ->
                        Text("• $row", color = txtColor, fontSize = 12.sp)
                        HorizontalDivider(color = Color(0xFFE2E8F0))
                    }
                }
            }
        }
        WidgetType.WEB_VIEW, WidgetType.VIDEO_VIEW, WidgetType.MAP_VIEW, WidgetType.YOUTUBE_PLAYER, WidgetType.AD_VIEW -> {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = Color(0xFFF1F5F9),
                border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                modifier = Modifier.fillMaxWidth().height(84.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(iconForWidgetType(widget.type), contentDescription = null, tint = Color(0xFF2196F3))
                        Text(
                            text = if (widget.type == WidgetType.WEB_VIEW) widget.url else widget.type.displayName,
                            fontSize = 11.sp,
                            color = Color(0xFF334155),
                        )
                    }
                }
            }
        }
        WidgetType.FAB -> {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                FloatingActionButton(
                    onClick = { if (interactive) onButtonClick() },
                    containerColor = if (bgColor == Color.Transparent) Color(0xFF2196F3) else bgColor,
                    contentColor = txtColor,
                    modifier = Modifier.size(46.dp),
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "FAB")
                }
            }
        }
        WidgetType.DIVIDER -> {
            HorizontalDivider(
                thickness = 2.dp,
                color = if (bgColor == Color.Transparent) Color(0xFFCBD5E1) else bgColor,
                modifier = Modifier.padding(vertical = 4.dp),
            )
        }
        else -> {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = if (bgColor == Color.Transparent) Color(0xFFF1F5F9) else bgColor,
                border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(iconForWidgetType(widget.type), contentDescription = null, tint = Color(0xFF1976D2), modifier = Modifier.size(18.dp))
                    Text("${widget.type.displayName} (${widget.id})", color = txtColor, fontSize = 13.sp)
                }
            }
        }
    }
}

private fun parseWidgetHexColor(hex: String, default: Color): Color {
    return runCatching {
        val clean = hex.trim().removePrefix("#")
        val colorLong = when (clean.length) {
            6 -> "FF$clean".toLong(16)
            8 -> clean.toLong(16)
            else -> return default
        }
        Color(colorLong)
    }.getOrDefault(default)
}

private fun iconForWidgetType(type: WidgetType): ImageVector = when (type) {
    WidgetType.LINEAR_H, WidgetType.SCROLL_H -> Icons.Filled.ViewColumn
    WidgetType.LINEAR_V, WidgetType.SCROLL_V, WidgetType.RELATIVE_LAYOUT -> Icons.Filled.ViewStream
    WidgetType.TAB_LAYOUT, WidgetType.BOTTOM_NAV -> Icons.Filled.Tab
    WidgetType.CARD_VIEW, WidgetType.COLLAPSING_TOOLBAR -> Icons.Filled.ViewAgenda
    WidgetType.SWIPE_REFRESH -> Icons.Filled.Refresh
    WidgetType.TEXT_VIEW -> Icons.Filled.TextFields
    WidgetType.BUTTON, WidgetType.MATERIAL_BUTTON -> Icons.Filled.SmartButton
    WidgetType.EDIT_TEXT, WidgetType.AUTO_COMPLETE, WidgetType.MULTI_AUTO_COMPLETE, WidgetType.TEXT_INPUT_LAYOUT -> Icons.Filled.Edit
    WidgetType.IMAGE_VIEW, WidgetType.CIRCLE_IMAGE_VIEW -> Icons.Filled.Image
    WidgetType.CHECK_BOX -> Icons.Filled.CheckBox
    WidgetType.RADIO_BUTTON, WidgetType.RADIO_GROUP -> Icons.Filled.RadioButtonChecked
    WidgetType.SWITCH -> Icons.Filled.ToggleOn
    WidgetType.SEEK_BAR, WidgetType.PROGRESS_BAR -> Icons.Filled.Tune
    WidgetType.RATING_BAR -> Icons.Filled.Star
    WidgetType.SEARCH_VIEW -> Icons.Filled.Search
    WidgetType.VIDEO_VIEW, WidgetType.YOUTUBE_PLAYER -> Icons.Filled.OndemandVideo
    WidgetType.WEB_VIEW -> Icons.Filled.Language
    WidgetType.LIST_VIEW, WidgetType.SPINNER, WidgetType.VIEW_PAGER -> Icons.AutoMirrored.Filled.List
    WidgetType.GRID_VIEW, WidgetType.RECYCLER_VIEW -> Icons.Filled.GridView
    WidgetType.WAVE_SIDE_BAR, WidgetType.CODE_VIEW -> Icons.Filled.Code
    WidgetType.PATTERN_LOCK -> Icons.Filled.Lock
    WidgetType.LOTTIE -> Icons.Filled.Animation
    WidgetType.OTP_VIEW -> Icons.Filled.Password
    WidgetType.AD_VIEW -> Icons.Filled.Notifications
    WidgetType.MAP_VIEW -> Icons.Filled.Map
    WidgetType.SIGN_IN_BUTTON -> Icons.Filled.AccountCircle
    WidgetType.ANALOG_CLOCK, WidgetType.DIGITAL_CLOCK, WidgetType.TIME_PICKER -> Icons.Filled.AccessTime
    WidgetType.DATE_PICKER, WidgetType.CALENDAR_VIEW -> Icons.Filled.CalendarToday
    WidgetType.FAB -> Icons.Filled.Add
    WidgetType.DIVIDER -> Icons.Filled.SpaceBar
}
