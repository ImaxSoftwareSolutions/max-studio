package com.example.diagnostics

import com.example.model.BackendMode
import com.example.model.BlockNode
import com.example.model.BlockType
import com.example.model.CustomViewDef
import com.example.model.MaxProject
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.StringReader

enum class DiagnosticSeverity {
    FATAL,
    ERROR,
    WARNING,
}

data class DiagnosticItem(
    val id: String,
    val severity: DiagnosticSeverity,
    val source: String,
    val message: String,
    val hint: String = "",
)

data class ProjectValidationReport(
    val items: List<DiagnosticItem> = emptyList(),
) {
    val errorCount: Int
        get() = items.count { it.severity == DiagnosticSeverity.FATAL || it.severity == DiagnosticSeverity.ERROR }

    val warningCount: Int
        get() = items.count { it.severity == DiagnosticSeverity.WARNING }

    val hasFatalBlocker: Boolean
        get() = items.any { it.severity == DiagnosticSeverity.FATAL }
}

object ProjectDiagnostics {

    private val PACKAGE_REGEX = Regex("^[a-zA-Z][a-zA-Z0-9_]*(\\.[a-zA-Z][a-zA-Z0-9_]*)+$")
    private val HEX_COLOR_REGEX = Regex("^#[0-9A-Fa-f]{6,8}$")

    fun analyze(project: MaxProject): ProjectValidationReport {
        val items = mutableListOf<DiagnosticItem>()
        var seq = 0
        fun add(severity: DiagnosticSeverity, source: String, message: String, hint: String = "") {
            items.add(
                DiagnosticItem(
                    id = "diag_${seq++}",
                    severity = severity,
                    source = source,
                    message = message,
                    hint = hint,
                )
            )
        }

        // 1. Metadata & AndroidManifest checks
        val pkg = project.packageName.trim()
        if (pkg.isBlank() || !PACKAGE_REGEX.matches(pkg)) {
            add(
                DiagnosticSeverity.FATAL,
                "AndroidManifest.xml",
                "Invalid package name: '${project.packageName}'",
                "Package name me kam se kam 2 dot-separated segments hone chahiye (e.g. com.maxstudio.myapp).",
            )
        }
        if (project.appName.isBlank()) {
            add(
                DiagnosticSeverity.WARNING,
                "AndroidManifest.xml",
                "App Name khali hai (default fallback use hoga)",
                "Project settings se App Name set karein.",
            )
        }
        if (project.versionCode < 1) {
            add(
                DiagnosticSeverity.ERROR,
                "AndroidManifest.xml",
                "Invalid versionCode: ${project.versionCode}",
                "versionCode kam se kam 1 hona chahiye.",
            )
        }

        // 2. Direct XML override check
        if (project.xmlOverride.isNotBlank()) {
            validateXmlOverride(project.xmlOverride)?.let { xmlErr ->
                add(
                    DiagnosticSeverity.FATAL,
                    "XML Layout (main.xml)",
                    xmlErr,
                    "Direct XML editor me unclosed tags ya invalid attributes check karein.",
                )
            }
        }

        // 3. Block logic checks (unless pure Java mode)
        if (project.backendMode != BackendMode.JAVA_ONLY) {
            val allWidgetIds = (project.widgets + project.customViews.flatMap { it.widgets })
                .map { it.id }
                .toSet()
            val moreblockNames = project.moreblocks.map { it.name }.toSet()

            // Collect all Intent setScreen calls across the project so startActivity validation knows if setScreen exists
            val allIntentSetScreens = mutableSetOf<String>()
            fun collectIntentTargets(blocks: List<BlockNode>) {
                blocks.forEach { b ->
                    if (b.type == BlockType.COMP_INTENT_SCREEN) {
                        allIntentSetScreens.add(b.params["compName"].orEmpty().ifBlank { "intent" })
                    }
                    collectIntentTargets(b.children)
                    collectIntentTargets(b.elseChildren)
                }
            }
            project.events.values.forEach(::collectIntentTargets)
            project.moreblocks.forEach { collectIntentTargets(it.blocks) }

            fun validateBlockList(eventLabel: String, blocks: List<BlockNode>) {
                val localIntentConfigured = mutableSetOf<String>()
                blocks.forEachIndexed { idx, block ->
                    val p = block.params
                    val loc = "$eventLabel • #${idx + 1} (${blockDisplayLabel(block.type)})"

                    when (block.type) {
                        BlockType.COMP_INTENT_SCREEN -> {
                            val compName = p["compName"].orEmpty().ifBlank { "intent" }
                            localIntentConfigured.add(compName)
                            val targetScreen = p["screen"].orEmpty().trim()
                            if (targetScreen.isBlank()) {
                                add(
                                    DiagnosticSeverity.ERROR,
                                    loc,
                                    "Intent '$compName' me target screen empty hai",
                                    "Screen parameter me 'MainActivity' ya apni banayi Activity select karein.",
                                )
                            } else if (!screenExists(targetScreen, project.customViews)) {
                                add(
                                    DiagnosticSeverity.ERROR,
                                    loc,
                                    "Target Activity '$targetScreen' project me nahi mili",
                                    "View tab -> Bottom pill -> Custom View -> '+ Create new view' se '${targetScreen.removeSuffix("Activity").lowercase()}' screen create karein.",
                                )
                            }
                        }

                        BlockType.COMP_START_ACTIVITY -> {
                            val compName = p["compName"].orEmpty().ifBlank { "intent" }
                            if (compName !in localIntentConfigured && compName !in allIntentSetScreens) {
                                add(
                                    DiagnosticSeverity.ERROR,
                                    loc,
                                    "startActivity '$compName' ke liye 'Intent setScreen' block nahi lagaya gaya",
                                    "startActivity se pehle 'Intent setScreen' block lagayein.",
                                )
                            }
                        }

                        BlockType.COMP_TIMER_AFTER -> {
                            val compName = p["compName"].orEmpty().ifBlank { "timer" }
                            val msRaw = p["ms"].orEmpty().trim()
                            if (!msRaw.contains("{")) {
                                val msVal = msRaw.toLongOrNull()
                                if (msVal == null || msVal <= 0L) {
                                    add(
                                        DiagnosticSeverity.ERROR,
                                        loc,
                                        "Timer '$compName' ka delay invalid hai: '$msRaw' ms",
                                        "Valid positive milliseconds dalein (e.g. 1000 ya 2000).",
                                    )
                                }
                            }
                            if (block.children.isEmpty()) {
                                add(
                                    DiagnosticSeverity.WARNING,
                                    loc,
                                    "Timer '$compName' ke andar koi block nahi hai",
                                    "Delay complete hone par chalne wale blocks (jaise Intent setScreen + startActivity) timer container ke andar add karein.",
                                )
                            }
                        }

                        BlockType.VIEW_SET_TEXT,
                        BlockType.VIEW_GET_TEXT,
                        BlockType.VIEW_SET_VISIBLE,
                        BlockType.VIEW_SET_ENABLED,
                        BlockType.VIEW_SET_PROGRESS,
                        BlockType.VIEW_SET_CHECKED,
                        BlockType.VIEW_LIST_REFRESH -> {
                            val wid = p["widgetId"].orEmpty().trim()
                            if (wid.isBlank() || wid !in allWidgetIds) {
                                add(
                                    DiagnosticSeverity.ERROR,
                                    loc,
                                    "View '$wid' kisi bhi screen layout me exist nahi karta",
                                    "View tab me '$wid' add karein ya block me sahi widget ID select karein.",
                                )
                            }
                        }

                        BlockType.VIEW_SET_TEXT_COLOR,
                        BlockType.VIEW_SET_BG_COLOR -> {
                            val wid = p["widgetId"].orEmpty().trim()
                            val hex = p["color"].orEmpty().trim()
                            if (wid.isBlank() || wid !in allWidgetIds) {
                                add(
                                    DiagnosticSeverity.ERROR,
                                    loc,
                                    "View '$wid' layout me nahi mila",
                                    "Valid widget ID choose karein.",
                                )
                            }
                            if (!hex.contains("{") && !HEX_COLOR_REGEX.matches(hex)) {
                                add(
                                    DiagnosticSeverity.ERROR,
                                    loc,
                                    "Invalid hex color '$hex'",
                                    "6 ya 8 digit hex color dalein (e.g. #2196F3).",
                                )
                            }
                        }

                        BlockType.VIEW_LOAD_URL -> {
                            val wid = p["widgetId"].orEmpty().trim()
                            val url = p["url"].orEmpty().trim()
                            if (wid.isBlank() || wid !in allWidgetIds) {
                                add(DiagnosticSeverity.ERROR, loc, "WebView '$wid' layout me nahi mila")
                            }
                            if (url.isBlank()) {
                                add(DiagnosticSeverity.ERROR, loc, "loadUrl me URL empty hai")
                            }
                            if ("android.permission.INTERNET" !in project.permissions) {
                                add(
                                    DiagnosticSeverity.WARNING,
                                    loc,
                                    "INTERNET permission missing hai",
                                    "Drawer -> Permission Manager me INTERNET enable karein.",
                                )
                            }
                        }

                        BlockType.COMP_NETWORK_REQUEST -> {
                            if (p["url"].orEmpty().trim().isBlank()) {
                                add(DiagnosticSeverity.ERROR, loc, "RequestNetwork URL khali hai")
                            }
                            if ("android.permission.INTERNET" !in project.permissions) {
                                add(
                                    DiagnosticSeverity.WARNING,
                                    loc,
                                    "INTERNET permission disabled hai",
                                    "Permission Manager se INTERNET allow karein.",
                                )
                            }
                        }

                        BlockType.COMP_VIBRATE -> {
                            if ("android.permission.VIBRATE" !in project.permissions) {
                                add(
                                    DiagnosticSeverity.WARNING,
                                    loc,
                                    "VIBRATE permission missing hai",
                                    "Drawer -> Permission Manager me VIBRATE add karein.",
                                )
                            }
                        }

                        BlockType.MATH_CALC -> {
                            val op = p["op"].orEmpty().trim()
                            val right = p["right"].orEmpty().trim()
                            if ((op == "/" || op == "%") && right == "0") {
                                add(
                                    DiagnosticSeverity.ERROR,
                                    loc,
                                    "Division by zero (0 se divide nahi kar sakte)",
                                    "Right operand me non-zero number dalein.",
                                )
                            }
                        }

                        BlockType.IF_THEN,
                        BlockType.IF_ELSE,
                        BlockType.REPEAT_WHILE -> {
                            if (p["condition"].orEmpty().isBlank()) {
                                add(DiagnosticSeverity.ERROR, loc, "Condition empty hai")
                            }
                        }

                        BlockType.REPEAT -> {
                            val times = p["times"].orEmpty().trim()
                            if (!times.contains("{") && (times.toIntOrNull() ?: -1) < 0) {
                                add(DiagnosticSeverity.ERROR, loc, "Repeat count invalid hai: '$times'")
                            }
                        }

                        BlockType.CALL_MOREBLOCK -> {
                            val name = p["blockName"].orEmpty().trim()
                            if (name.isBlank() || name !in moreblockNames) {
                                add(
                                    DiagnosticSeverity.ERROR,
                                    loc,
                                    "Moreblock '$name' define nahi kiya gaya",
                                    "Event -> Moreblock rail me '$name' create karein.",
                                )
                            }
                        }

                        BlockType.ADD_SOURCE_DIRECTLY,
                        BlockType.COLLECTION_SNIPPET -> {
                            val code = p["java"].orEmpty()
                            checkBalance(code)?.let { balErr ->
                                add(DiagnosticSeverity.ERROR, loc, "Custom Java snippet: $balErr")
                            }
                        }

                        else -> Unit
                    }

                    if (block.children.isNotEmpty()) {
                        validateBlockList("$eventLabel -> ${blockDisplayLabel(block.type)}", block.children)
                    }
                    if (block.elseChildren.isNotEmpty()) {
                        validateBlockList("$eventLabel -> ${blockDisplayLabel(block.type)} [else]", block.elseChildren)
                    }
                }
            }

            project.events.forEach { (eventKey, list) ->
                if (list.isNotEmpty()) validateBlockList("Event: $eventKey", list)
            }
            project.moreblocks.forEach { mb ->
                if (mb.blocks.isNotEmpty()) validateBlockList("Moreblock: ${mb.name}", mb.blocks)
            }
        }

        // 4. Java source validation (strict fatal in JAVA_ONLY, error/warning in MIX_MODE)
        if (project.backendMode != BackendMode.BLOCKS_ONLY && project.javaCode.isNotBlank()) {
            val strictSeverity = if (project.backendMode == BackendMode.JAVA_ONLY) {
                DiagnosticSeverity.FATAL
            } else {
                DiagnosticSeverity.ERROR
            }
            validateJavaSource(project.javaCode, strictSeverity).forEach { err ->
                items.add(
                    DiagnosticItem(
                        id = "diag_${seq++}",
                        severity = err.first,
                        source = "MainActivity.java",
                        message = err.second,
                        hint = err.third,
                    )
                )
            }
        }

        return ProjectValidationReport(items)
    }

    fun screenExists(targetName: String, customViews: List<CustomViewDef>): Boolean {
        val clean = targetName.trim()
        if (clean.equals("MainActivity", ignoreCase = true) || clean.equals("main", ignoreCase = true)) {
            return true
        }
        val stripped = clean.lowercase()
            .removeSuffix("activity")
            .trimEnd('_')
            .replace("_", "")
        return customViews.any { cv ->
            val cvClean = cv.name.trim().lowercase().replace("_", "")
            cv.name.equals(clean, ignoreCase = true) ||
                cv.activityClassName().equals(clean, ignoreCase = true) ||
                (stripped.isNotEmpty() && cvClean == stripped)
        }
    }

    fun resolveCustomView(targetName: String, customViews: List<CustomViewDef>): CustomViewDef? {
        val clean = targetName.trim()
        val stripped = clean.lowercase()
            .removeSuffix("activity")
            .trimEnd('_')
            .replace("_", "")
        return customViews.firstOrNull { cv ->
            val cvClean = cv.name.trim().lowercase().replace("_", "")
            cv.name.equals(clean, ignoreCase = true) ||
                cv.activityClassName().equals(clean, ignoreCase = true) ||
                (stripped.isNotEmpty() && cvClean == stripped)
        }
    }

    private fun validateXmlOverride(xml: String): String? {
        return try {
            val factory = XmlPullParserFactory.newInstance()
            val parser = factory.newPullParser()
            parser.setInput(StringReader(xml))
            var eventType = parser.eventType
            while (eventType != XmlPullParser.END_DOCUMENT) {
                eventType = parser.next()
            }
            null
        } catch (e: Exception) {
            "XML syntax error: ${e.message ?: "Malformed layout XML"}"
        }
    }

    private fun checkBalance(code: String): String? {
        var brace = 0
        var paren = 0
        var inString = false
        var escaped = false
        for (ch in code) {
            if (escaped) {
                escaped = false
                continue
            }
            if (ch == '\\' && inString) {
                escaped = true
                continue
            }
            if (ch == '"') {
                inString = !inString
                continue
            }
            if (inString) continue
            when (ch) {
                '{' -> brace++
                '}' -> {
                    brace--
                    if (brace < 0) return "Extra closing brace '}'"
                }
                '(' -> paren++
                ')' -> {
                    paren--
                    if (paren < 0) return "Extra closing parenthesis ')'"
                }
            }
        }
        return when {
            inString -> "Unclosed string literal '\"'"
            brace != 0 -> "Unbalanced curly braces '{' / '}' (diff: $brace)"
            paren != 0 -> "Unbalanced parentheses '(' / ')' (diff: $paren)"
            else -> null
        }
    }

    private fun validateJavaSource(
        code: String,
        strictSeverity: DiagnosticSeverity,
    ): List<Triple<DiagnosticSeverity, String, String>> {
        val issues = mutableListOf<Triple<DiagnosticSeverity, String, String>>()
        if (!code.contains("package ")) {
            issues.add(Triple(DiagnosticSeverity.WARNING, "Missing 'package ...;' declaration", "Top par package declaration add karein."))
        }
        if (!Regex("\\bclass\\s+[A-Za-z_][A-Za-z0-9_]*").containsMatchIn(code)) {
            issues.add(Triple(strictSeverity, "Missing Java class declaration", "'public class MainActivity ...' add karein."))
        }

        var brace = 0
        var paren = 0
        val lines = code.lines()
        lines.forEachIndexed { index, raw ->
            val lineNum = index + 1
            val line = raw.trim()
            if (line.isEmpty() || line.startsWith("//") || line.startsWith("/*") || line.startsWith("*")) {
                return@forEachIndexed
            }
            var inStr = false
            var escaped = false
            for (ch in line) {
                if (escaped) {
                    escaped = false
                    continue
                }
                if (ch == '\\' && inStr) {
                    escaped = true
                    continue
                }
                if (ch == '"') {
                    inStr = !inStr
                    continue
                }
                if (inStr) continue
                when (ch) {
                    '{' -> brace++
                    '}' -> brace--
                    '(' -> paren++
                    ')' -> paren--
                }
            }
            if (inStr) {
                issues.add(
                    Triple(
                        strictSeverity,
                        "Line $lineNum: Unclosed string literal '\"'",
                        "Line $lineNum par missing closing quote add karein.",
                    )
                )
            }
            if (brace < 0) {
                issues.add(Triple(strictSeverity, "Line $lineNum: Unexpected '}'", "Extra closing brace remove karein."))
                brace = 0
            }
            if (paren < 0) {
                issues.add(Triple(strictSeverity, "Line $lineNum: Unexpected ')'", "Extra closing parenthesis remove karein."))
                paren = 0
            }
        }
        if (brace != 0) {
            issues.add(Triple(strictSeverity, "Unclosed curly brace '{' ($brace remaining)", "Missing '}' add karein."))
        }
        if (paren != 0) {
            issues.add(Triple(strictSeverity, "Unclosed parenthesis '(' ($paren remaining)", "Missing ')' add karein."))
        }
        return issues
    }

    private fun blockDisplayLabel(type: BlockType): String =
        type.spec.replace(Regex("%[a-zA-Z0-9_.]+"), "_").replace(Regex("\\s+"), " ").trim()
            .ifBlank { type.name }
}
