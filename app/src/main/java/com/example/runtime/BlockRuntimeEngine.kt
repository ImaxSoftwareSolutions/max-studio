package com.example.runtime

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.example.diagnostics.ProjectDiagnostics
import com.example.model.BlockNode
import com.example.model.BlockType
import com.example.model.CustomViewDef
import com.example.model.MaxProject
import com.example.model.WidgetNode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import kotlin.math.sqrt
import kotlin.random.Random

@Stable
class BlockRuntimeController(
    private val project: MaxProject,
    private val scope: CoroutineScope,
    private val onExternalError: (String) -> Unit,
    private val onExitRequested: () -> Unit,
) {
    var currentScreenName by mutableStateOf("MainActivity")
        private set

    private val screenStack = mutableListOf("MainActivity")

    val widgetText = mutableStateMapOf<String, String>()
    val widgetTextColor = mutableStateMapOf<String, String>()
    val widgetBgColor = mutableStateMapOf<String, String>()
    val widgetVisibility = mutableStateMapOf<String, String>() // VISIBLE, INVISIBLE, GONE
    val widgetEnabled = mutableStateMapOf<String, Boolean>()
    val widgetProgress = mutableStateMapOf<String, Int>()
    val widgetChecked = mutableStateMapOf<String, Boolean>()

    val vars = mutableStateMapOf<String, String>()
    private val lists = mutableMapOf<String, MutableList<String>>()
    private val maps = mutableMapOf<String, MutableMap<String, String>>()
    private val sharedPrefs = mutableMapOf<String, String>()
    private val sandboxFiles = mutableMapOf<String, String>()
    private val intentScreens = mutableMapOf<String, String>()
    private val activeJobs = mutableListOf<Job>()

    var activeBanner by mutableStateOf<String?>(null)
        private set

    var activeDialog by mutableStateOf<Pair<String, String>?>(null)
        private set

    val runtimeErrors = mutableStateListOf<String>()
    var showErrorLogDialog by mutableStateOf(false)

    private var started = false
    private var disposed = false

    init {
        resetState()
    }

    private fun resetState() {
        vars.clear()
        project.variables.forEach { v ->
            vars[v.name] = v.initialValue
        }
        widgetText.clear()
        widgetTextColor.clear()
        widgetBgColor.clear()
        widgetVisibility.clear()
        widgetEnabled.clear()
        widgetProgress.clear()
        widgetChecked.clear()
    }

    fun start() {
        if (started) return
        started = true
        disposed = false
        currentScreenName = "MainActivity"
        screenStack.clear()
        screenStack.add("MainActivity")
        runScreenLifecycle("MainActivity")
    }

    fun restart() {
        cancelTimers()
        started = false
        runtimeErrors.clear()
        activeBanner = null
        activeDialog = null
        resetState()
        start()
    }

    fun dispose() {
        disposed = true
        cancelTimers()
    }

    private fun cancelTimers() {
        activeJobs.forEach { it.cancel() }
        activeJobs.clear()
    }

    fun clearBanner() {
        activeBanner = null
    }

    fun dismissDialog() {
        activeDialog = null
    }

    fun currentWidgets(): List<WidgetNode> {
        if (currentScreenName == "MainActivity") return project.widgets
        return project.customViews.firstOrNull { it.name == currentScreenName }?.widgets.orEmpty()
    }

    fun currentScreenTitle(): String {
        if (currentScreenName == "MainActivity") return project.appName.ifBlank { "MAX STUDIO App" }
        val cv = project.customViews.firstOrNull { it.name == currentScreenName }
        return cv?.activityClassName() ?: currentScreenName
    }

    fun isSecondaryScreen(): Boolean = currentScreenName != "MainActivity"

    fun resolvedWidget(base: WidgetNode): WidgetNode = base.copy(
        text = widgetText[base.id] ?: base.text,
        textColor = widgetTextColor[base.id] ?: base.textColor,
        backgroundColor = widgetBgColor[base.id] ?: base.backgroundColor,
        progress = widgetProgress[base.id] ?: base.progress,
        checked = widgetChecked[base.id] ?: base.checked,
    )

    fun isWidgetVisible(id: String): String = widgetVisibility[id] ?: "VISIBLE"

    fun isWidgetEnabled(id: String): Boolean = widgetEnabled[id] ?: true

    private fun runScreenLifecycle(screen: String) {
        if (screen == "MainActivity") {
            executeEvent("onCreate")
            executeEvent("onStart")
        } else {
            executeEvent("screen_${screen}_onCreate")
            executeEvent("screen_${screen}_onStart")
        }
    }

    fun onWidgetClicked(widgetId: String) {
        val scopedKey = if (currentScreenName != "MainActivity") "screen_${currentScreenName}_${widgetId}_onClick" else null
        if (scopedKey != null && !project.events[scopedKey].isNullOrEmpty()) {
            executeEvent(scopedKey)
        } else {
            val globalKey = "${widgetId}_onClick"
            if (!project.events[globalKey].isNullOrEmpty()) {
                executeEvent(globalKey)
            } else {
                val w = currentWidgets().firstOrNull { it.id == widgetId }
                activeBanner = "${w?.text?.ifBlank { widgetId } ?: widgetId} clicked"
            }
        }
    }

    fun onEditTextChanged(widgetId: String, value: String) {
        widgetText[widgetId] = value
        val scopedKey = if (currentScreenName != "MainActivity") "screen_${currentScreenName}_${widgetId}_onTextChanged" else null
        if (scopedKey != null && !project.events[scopedKey].isNullOrEmpty()) {
            executeEvent(scopedKey)
        } else if (!project.events["${widgetId}_onTextChanged"].isNullOrEmpty()) {
            executeEvent("${widgetId}_onTextChanged")
        }
    }

    fun onCheckedChanged(widgetId: String, checked: Boolean) {
        widgetChecked[widgetId] = checked
        val scopedKey = if (currentScreenName != "MainActivity") "screen_${currentScreenName}_${widgetId}_onCheckedChanged" else null
        if (scopedKey != null && !project.events[scopedKey].isNullOrEmpty()) {
            executeEvent(scopedKey)
        } else if (!project.events["${widgetId}_onCheckedChanged"].isNullOrEmpty()) {
            executeEvent("${widgetId}_onCheckedChanged")
        } else {
            activeBanner = "$widgetId -> $checked"
        }
    }

    fun onProgressChanged(widgetId: String, progress: Int) {
        widgetProgress[widgetId] = progress
        val scopedKey = if (currentScreenName != "MainActivity") "screen_${currentScreenName}_${widgetId}_onProgressChanged" else null
        if (scopedKey != null && !project.events[scopedKey].isNullOrEmpty()) {
            executeEvent(scopedKey)
        } else if (!project.events["${widgetId}_onProgressChanged"].isNullOrEmpty()) {
            executeEvent("${widgetId}_onProgressChanged")
        }
    }

    fun handleBackPress() {
        if (currentScreenName == "MainActivity") {
            val backBlocks = project.events["onBackPressed"].orEmpty()
            if (backBlocks.isNotEmpty()) {
                executeBlockList(backBlocks)
            } else {
                onExitRequested()
            }
        } else {
            if (screenStack.size > 1) {
                screenStack.removeAt(screenStack.lastIndex)
                currentScreenName = screenStack.last()
            } else {
                onExitRequested()
            }
        }
    }

    fun executeEvent(eventKey: String) {
        val list = project.events[eventKey].orEmpty()
        if (list.isNotEmpty()) executeBlockList(list)
    }

    private fun executeBlockList(blocks: List<BlockNode>) {
        for (block in blocks) {
            if (disposed) return
            val shouldContinue = executeBlock(block)
            if (!shouldContinue) break
        }
    }

    private fun executeBlock(block: BlockNode): Boolean {
        val p = block.params
        return try {
            when (block.type) {
                BlockType.BREAK -> false

                BlockType.COMP_TOAST,
                BlockType.COMP_SNACKBAR -> {
                    activeBanner = interpolate(p["message"].orEmpty())
                    true
                }

                BlockType.COMP_DIALOG_SHOW -> {
                    val title = interpolate(p["title"].orEmpty().ifBlank { "Info" })
                    val msg = interpolate(p["message"].orEmpty())
                    activeDialog = title to msg
                    true
                }

                BlockType.COMP_NOTIFY -> {
                    val title = interpolate(p["title"].orEmpty().ifBlank { "Notification" })
                    val body = interpolate(p["body"].orEmpty())
                    activeBanner = "[$title] $body"
                    true
                }

                BlockType.COMP_TIMER_AFTER -> {
                    val compName = p["compName"].orEmpty().ifBlank { "timer" }
                    val rawMs = interpolate(p["ms"].orEmpty().ifBlank { "1000" }).trim()
                    val delayMs = rawMs.toLongOrNull()
                    if (delayMs == null || delayMs < 0L) {
                        recordRuntimeError("Timer '$compName' invalid delay: '$rawMs' ms")
                    } else if (block.children.isEmpty()) {
                        recordRuntimeError("Timer '$compName' has no inner blocks to run")
                    } else {
                        val job = scope.launch {
                            delay(delayMs)
                            if (!disposed) {
                                executeBlockList(block.children)
                            }
                        }
                        activeJobs.add(job)
                    }
                    true
                }

                BlockType.COMP_INTENT_SCREEN -> {
                    val compName = p["compName"].orEmpty().ifBlank { "intent" }
                    val screen = p["screen"].orEmpty().ifBlank { "MainActivity" }
                    intentScreens[compName] = screen
                    true
                }

                BlockType.COMP_START_ACTIVITY -> {
                    val compName = p["compName"].orEmpty().ifBlank { "intent" }
                    val target = intentScreens[compName]
                    if (target.isNullOrBlank()) {
                        recordRuntimeError("startActivity '$compName' failed: Intent setScreen block not set")
                    } else if (target.equals("MainActivity", true) || target.equals("main", true)) {
                        currentScreenName = "MainActivity"
                        if (screenStack.lastOrNull() != "MainActivity") screenStack.add("MainActivity")
                        runScreenLifecycle("MainActivity")
                    } else {
                        val resolved: CustomViewDef? = ProjectDiagnostics.resolveCustomView(target, project.customViews)
                        if (resolved == null) {
                            recordRuntimeError("Target Activity '$target' not found! View tab -> '+' se '${target.removeSuffix("Activity").lowercase()}' screen banayein.")
                        } else {
                            currentScreenName = resolved.name
                            screenStack.add(resolved.name)
                            runScreenLifecycle(resolved.name)
                        }
                    }
                    true
                }

                BlockType.COMP_FINISH_ACTIVITY -> {
                    handleBackPress()
                    false
                }

                BlockType.COMP_SHARED_PREF_SET -> {
                    val comp = p["compName"].orEmpty().ifBlank { "sp" }
                    val key = p["key"].orEmpty().ifBlank { "k" }
                    sharedPrefs["$comp:$key"] = interpolate(p["value"].orEmpty())
                    true
                }

                BlockType.COMP_VIBRATE -> {
                    val ms = interpolate(p["ms"].orEmpty().ifBlank { "200" })
                    activeBanner = "Vibrated ${ms}ms"
                    true
                }

                BlockType.VIEW_SET_TEXT -> {
                    val id = p["widgetId"].orEmpty()
                    if (!hasWidget(id)) {
                        recordRuntimeError("setText failed: View '$id' not found on $currentScreenName")
                    } else {
                        widgetText[id] = interpolate(p["text"].orEmpty())
                    }
                    true
                }

                BlockType.VIEW_GET_TEXT -> {
                    val id = p["widgetId"].orEmpty()
                    val varName = p["varName"].orEmpty().ifBlank { "text" }
                    val widget = currentWidgets().firstOrNull { it.id == id }
                    if (widget == null) {
                        recordRuntimeError("getText failed: View '$id' not found on $currentScreenName")
                    } else {
                        vars[varName] = widgetText[id] ?: widget.text
                    }
                    true
                }

                BlockType.VIEW_SET_TEXT_COLOR -> {
                    val id = p["widgetId"].orEmpty()
                    val hex = interpolate(p["color"].orEmpty().ifBlank { "#2196F3" })
                    if (!hasWidget(id)) {
                        recordRuntimeError("setTextColor failed: View '$id' not found")
                    } else if (!Regex("^#[0-9A-Fa-f]{6,8}$").matches(hex)) {
                        recordRuntimeError("setTextColor invalid hex color: '$hex'")
                    } else {
                        widgetTextColor[id] = hex
                    }
                    true
                }

                BlockType.VIEW_SET_BG_COLOR -> {
                    val id = p["widgetId"].orEmpty()
                    val hex = interpolate(p["color"].orEmpty().ifBlank { "#1E293B" })
                    if (!hasWidget(id)) {
                        recordRuntimeError("setBgColor failed: View '$id' not found")
                    } else if (!Regex("^#[0-9A-Fa-f]{6,8}$").matches(hex)) {
                        recordRuntimeError("setBgColor invalid hex color: '$hex'")
                    } else {
                        widgetBgColor[id] = hex
                    }
                    true
                }

                BlockType.VIEW_SET_VISIBLE -> {
                    val id = p["widgetId"].orEmpty()
                    val vis = p["visibility"].orEmpty().uppercase().ifBlank { "VISIBLE" }
                    if (!hasWidget(id)) {
                        recordRuntimeError("setVisibility failed: View '$id' not found")
                    } else {
                        widgetVisibility[id] = vis
                    }
                    true
                }

                BlockType.VIEW_SET_ENABLED -> {
                    val id = p["widgetId"].orEmpty()
                    if (!hasWidget(id)) {
                        recordRuntimeError("setEnabled failed: View '$id' not found")
                    } else {
                        widgetEnabled[id] = p["enabled"].equals("true", ignoreCase = true)
                    }
                    true
                }

                BlockType.VIEW_SET_PROGRESS -> {
                    val id = p["widgetId"].orEmpty()
                    val valNum = interpolate(p["value"].orEmpty().ifBlank { "0" }).trim().toIntOrNull() ?: 0
                    if (!hasWidget(id)) {
                        recordRuntimeError("setProgress failed: View '$id' not found")
                    } else {
                        widgetProgress[id] = valNum.coerceIn(0, 100)
                    }
                    true
                }

                BlockType.VIEW_SET_CHECKED -> {
                    val id = p["widgetId"].orEmpty()
                    if (!hasWidget(id)) {
                        recordRuntimeError("setChecked failed: View '$id' not found")
                    } else {
                        widgetChecked[id] = p["checked"].equals("true", ignoreCase = true)
                    }
                    true
                }

                BlockType.VIEW_LOAD_URL -> {
                    val id = p["widgetId"].orEmpty()
                    val url = interpolate(p["url"].orEmpty())
                    if (!hasWidget(id)) {
                        recordRuntimeError("loadUrl failed: WebView '$id' not found")
                    } else {
                        widgetText[id] = url
                        activeBanner = "Loading $url"
                    }
                    true
                }

                BlockType.SET_VAR_BOOLEAN -> {
                    vars[p["varName"].orEmpty().ifBlank { "flag" }] =
                        if (interpolate(p["value"].orEmpty()).equals("true", true)) "true" else "false"
                    true
                }

                BlockType.SET_VAR_NUMBER -> {
                    val name = p["varName"].orEmpty().ifBlank { "count" }
                    val v = interpolate(p["value"].orEmpty().ifBlank { "0" }).trim().toIntOrNull() ?: 0
                    vars[name] = v.toString()
                    true
                }

                BlockType.SET_VAR_STRING -> {
                    val name = p["varName"].orEmpty().ifBlank { "text" }
                    vars[name] = interpolate(p["value"].orEmpty())
                    true
                }

                BlockType.INCREASE_VAR -> {
                    val name = p["varName"].orEmpty().ifBlank { "count" }
                    val cur = vars[name]?.toIntOrNull() ?: 0
                    val amt = interpolate(p["amount"].orEmpty().ifBlank { "1" }).trim().toIntOrNull() ?: 1
                    vars[name] = (cur + amt).toString()
                    true
                }

                BlockType.DECREASE_VAR -> {
                    val name = p["varName"].orEmpty().ifBlank { "count" }
                    val cur = vars[name]?.toIntOrNull() ?: 0
                    val amt = interpolate(p["amount"].orEmpty().ifBlank { "1" }).trim().toIntOrNull() ?: 1
                    vars[name] = (cur - amt).toString()
                    true
                }

                BlockType.MAP_CREATE_NEW -> {
                    maps[p["mapName"].orEmpty().ifBlank { "map1" }] = mutableMapOf()
                    true
                }

                BlockType.MAP_PUT_KEY -> {
                    val name = p["mapName"].orEmpty().ifBlank { "map1" }
                    val m = maps.getOrPut(name) { mutableMapOf() }
                    m[p["key"].orEmpty()] = interpolate(p["value"].orEmpty())
                    true
                }

                BlockType.MAP_CLEAR -> {
                    maps[p["mapName"].orEmpty().ifBlank { "map1" }]?.clear()
                    true
                }

                BlockType.LIST_ADD -> {
                    val name = p["listName"].orEmpty().ifBlank { "list1" }
                    lists.getOrPut(name) { mutableListOf() }.add(interpolate(p["value"].orEmpty()))
                    true
                }

                BlockType.LIST_INSERT -> {
                    val name = p["listName"].orEmpty().ifBlank { "list1" }
                    val l = lists.getOrPut(name) { mutableListOf() }
                    val idx = (p["index"].orEmpty().toIntOrNull() ?: 0).coerceIn(0, l.size)
                    l.add(idx, interpolate(p["value"].orEmpty()))
                    true
                }

                BlockType.LIST_DELETE_AT -> {
                    val name = p["listName"].orEmpty().ifBlank { "list1" }
                    val l = lists[name]
                    val idx = p["index"].orEmpty().toIntOrNull() ?: -1
                    if (l != null && idx in l.indices) l.removeAt(idx)
                    true
                }

                BlockType.LIST_CLEAR -> {
                    lists[p["listName"].orEmpty().ifBlank { "list1" }]?.clear()
                    true
                }

                BlockType.LIST_SORT -> {
                    lists[p["listName"].orEmpty().ifBlank { "list1" }]?.sort()
                    true
                }

                BlockType.LIST_REVERSE -> {
                    lists[p["listName"].orEmpty().ifBlank { "list1" }]?.reverse()
                    true
                }

                BlockType.STR_JOIN -> {
                    val name = p["varName"].orEmpty().ifBlank { "text" }
                    vars[name] = interpolate(p["a"].orEmpty()) + interpolate(p["b"].orEmpty())
                    true
                }

                BlockType.STR_REPLACE -> {
                    val name = p["varName"].orEmpty().ifBlank { "text" }
                    val src = vars[p["source"].orEmpty().ifBlank { "text" }].orEmpty()
                    vars[name] = src.replace(p["old"].orEmpty(), p["new"].orEmpty())
                    true
                }

                BlockType.STR_UPPER -> {
                    val name = p["varName"].orEmpty().ifBlank { "text" }
                    val src = vars[p["source"].orEmpty().ifBlank { "text" }].orEmpty()
                    vars[name] = src.uppercase()
                    true
                }

                BlockType.STR_LOWER -> {
                    val name = p["varName"].orEmpty().ifBlank { "text" }
                    val src = vars[p["source"].orEmpty().ifBlank { "text" }].orEmpty()
                    vars[name] = src.lowercase()
                    true
                }

                BlockType.STR_TRIM -> {
                    val name = p["varName"].orEmpty().ifBlank { "text" }
                    val src = vars[p["source"].orEmpty().ifBlank { "text" }].orEmpty()
                    vars[name] = src.trim()
                    true
                }

                BlockType.STR_TO_NUM -> {
                    val name = p["varName"].orEmpty().ifBlank { "count" }
                    val src = vars[p["source"].orEmpty().ifBlank { "text" }].orEmpty().trim()
                    val num = src.toIntOrNull()
                    if (num == null) {
                        recordRuntimeError("toNumber failed on '$src'")
                    } else {
                        vars[name] = num.toString()
                    }
                    true
                }

                BlockType.NUM_TO_STR -> {
                    val name = p["varName"].orEmpty().ifBlank { "text" }
                    vars[name] = vars[p["source"].orEmpty().ifBlank { "count" }] ?: "0"
                    true
                }

                BlockType.MATH_RANDOM -> {
                    val name = p["varName"].orEmpty().ifBlank { "count" }
                    val min = interpolate(p["min"].orEmpty().ifBlank { "1" }).trim().toIntOrNull() ?: 1
                    val max = interpolate(p["max"].orEmpty().ifBlank { "100" }).trim().toIntOrNull() ?: 100
                    val lo = minOf(min, max)
                    val hi = maxOf(min, max)
                    vars[name] = Random.nextInt(lo, hi + 1).toString()
                    true
                }

                BlockType.MATH_CALC -> {
                    val name = p["varName"].orEmpty().ifBlank { "count" }
                    val left = resolveOperand(p["left"].orEmpty())
                    val right = resolveOperand(p["right"].orEmpty())
                    val op = p["op"].orEmpty().trim().ifBlank { "+" }
                    val res = when (op) {
                        "+" -> left + right
                        "-" -> left - right
                        "*" -> left * right
                        "/" -> {
                            if (right == 0) {
                                recordRuntimeError("Math division by zero ($left / 0)")
                                0
                            } else left / right
                        }
                        "%" -> {
                            if (right == 0) {
                                recordRuntimeError("Math modulo by zero ($left % 0)")
                                0
                            } else left % right
                        }
                        else -> left + right
                    }
                    vars[name] = res.toString()
                    true
                }

                BlockType.MATH_ROUND -> {
                    val name = p["varName"].orEmpty().ifBlank { "count" }
                    val v = interpolate(p["value"].orEmpty().ifBlank { "0" }).trim().toDoubleOrNull() ?: 0.0
                    vars[name] = v.roundToInt().toString()
                    true
                }

                BlockType.MATH_SQRT -> {
                    val name = p["varName"].orEmpty().ifBlank { "count" }
                    val v = interpolate(p["value"].orEmpty().ifBlank { "16" }).trim().toDoubleOrNull() ?: 16.0
                    vars[name] = sqrt(v.coerceAtLeast(0.0)).toInt().toString()
                    true
                }

                BlockType.FILE_WRITE -> {
                    val path = interpolate(p["path"].orEmpty())
                    sandboxFiles[path] = interpolate(p["data"].orEmpty())
                    true
                }

                BlockType.FILE_READ -> {
                    val name = p["varName"].orEmpty().ifBlank { "text" }
                    val path = interpolate(p["path"].orEmpty())
                    vars[name] = sandboxFiles[path].orEmpty()
                    true
                }

                BlockType.FILE_DELETE -> {
                    sandboxFiles.remove(interpolate(p["path"].orEmpty()))
                    true
                }

                BlockType.IF_THEN -> {
                    if (evalCondition(p["condition"].orEmpty())) {
                        executeBlockList(block.children)
                    }
                    true
                }

                BlockType.IF_ELSE -> {
                    if (evalCondition(p["condition"].orEmpty())) {
                        executeBlockList(block.children)
                    } else {
                        executeBlockList(block.elseChildren)
                    }
                    true
                }

                BlockType.REPEAT -> {
                    val count = (interpolate(p["times"].orEmpty().ifBlank { "5" }).trim().toIntOrNull() ?: 5).coerceIn(0, 500)
                    for (i in 0 until count) {
                        if (disposed) break
                        executeBlockList(block.children)
                    }
                    true
                }

                BlockType.REPEAT_WHILE -> {
                    var guard = 0
                    while (!disposed && evalCondition(p["condition"].orEmpty()) && guard < 200) {
                        guard++
                        executeBlockList(block.children)
                    }
                    true
                }

                BlockType.TRY_CATCH -> {
                    try {
                        executeBlockList(block.children)
                    } catch (t: Throwable) {
                        recordRuntimeError(t.message ?: "Caught exception")
                        if (block.elseChildren.isNotEmpty()) executeBlockList(block.elseChildren)
                    }
                    true
                }

                BlockType.CALL_MOREBLOCK -> {
                    val mbName = p["blockName"].orEmpty()
                    val mb = project.moreblocks.firstOrNull { it.name == mbName }
                    if (mb == null) {
                        recordRuntimeError("Moreblock '$mbName' not found")
                    } else {
                        executeBlockList(mb.blocks)
                    }
                    true
                }

                else -> true
            }
        } catch (t: Throwable) {
            recordRuntimeError("${block.type.name}: ${t.message ?: "Runtime error"}")
            true
        }
    }

    private fun hasWidget(id: String): Boolean =
        id.isNotBlank() && currentWidgets().any { it.id == id }

    private fun recordRuntimeError(msg: String) {
        if (msg !in runtimeErrors) {
            runtimeErrors.add(msg)
        }
        activeBanner = "Error: $msg"
        onExternalError("[${currentScreenName}] $msg")
    }

    private fun resolveOperand(raw: String): Int {
        val clean = raw.trim().removeSurrounding("{", "}")
        vars[clean]?.toIntOrNull()?.let { return it }
        return interpolate(raw).trim().toIntOrNull() ?: 0
    }

    private fun interpolate(template: String): String {
        if (!template.contains("{")) return template
        return Regex("\\{([a-zA-Z0-9_]+)\\}").replace(template) { m ->
            vars[m.groupValues[1]] ?: ""
        }
    }

    private fun evalCondition(cond: String): Boolean {
        val s = interpolate(cond).trim()
        if (s.equals("true", ignoreCase = true)) return true
        if (s.equals("false", ignoreCase = true) || s.isEmpty()) return false
        return when {
            s.contains("==") -> {
                val (a, b) = s.split("==", limit = 2)
                a.trim() == b.trim()
            }
            s.contains("!=") -> {
                val (a, b) = s.split("!=", limit = 2)
                a.trim() != b.trim()
            }
            s.contains(">=") -> {
                val (a, b) = s.split(">=", limit = 2)
                resolveOperand(a) >= resolveOperand(b)
            }
            s.contains("<=") -> {
                val (a, b) = s.split("<=", limit = 2)
                resolveOperand(a) <= resolveOperand(b)
            }
            s.contains(">") -> {
                val (a, b) = s.split(">", limit = 2)
                resolveOperand(a) > resolveOperand(b)
            }
            s.contains("<") -> {
                val (a, b) = s.split("<", limit = 2)
                resolveOperand(a) < resolveOperand(b)
            }
            else -> vars[s].equals("true", ignoreCase = true)
        }
    }
}

@Composable
fun rememberBlockRuntimeController(
    project: MaxProject,
    runKey: Int = 0,
    onExternalError: (String) -> Unit = {},
    onExitRequested: () -> Unit = {},
): BlockRuntimeController {
    val scope = rememberCoroutineScope()
    val controller = remember(project.id, project.events, project.widgets, project.customViews, project.variables, project.moreblocks, runKey) {
        BlockRuntimeController(
            project = project,
            scope = scope,
            onExternalError = onExternalError,
            onExitRequested = onExitRequested,
        )
    }
    LaunchedEffect(controller) {
        controller.start()
    }
    DisposableEffect(controller) {
        onDispose { controller.dispose() }
    }
    return controller
}
