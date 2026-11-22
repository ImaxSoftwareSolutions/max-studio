package com.example.codegen

import com.example.model.BlockNode
import com.example.model.BlockType
import com.example.model.CustomViewDef
import com.example.model.MaxProject
import com.example.model.VarType
import com.example.model.WidgetNode
import com.example.model.WidgetType

object CodeGenerator {

    fun generateJavaFromProject(project: MaxProject): String {
        val pkg = project.packageName.ifBlank { "com.my.newproject" }
        val sb = StringBuilder()
        sb.appendLine("package $pkg;")
        sb.appendLine()
        sb.appendLine("import android.app.*;")
        sb.appendLine("import android.content.*;")
        sb.appendLine("import android.graphics.*;")
        sb.appendLine("import android.os.*;")
        sb.appendLine("import android.text.*;")
        sb.appendLine("import android.view.*;")
        sb.appendLine("import android.webkit.*;")
        sb.appendLine("import android.widget.*;")
        sb.appendLine("import androidx.appcompat.app.AppCompatActivity;")
        sb.appendLine("import androidx.cardview.widget.CardView;")
        sb.appendLine("import com.google.android.material.floatingactionbutton.FloatingActionButton;")
        sb.appendLine("import com.google.android.material.snackbar.Snackbar;")
        sb.appendLine("import java.util.*;")
        sb.appendLine()
        sb.appendLine("public class MainActivity extends AppCompatActivity {")
        sb.appendLine()

        if (project.variables.isNotEmpty()) {
            sb.appendLine("    // Variables")
            project.variables.forEach { v ->
                val init = when (v.type) {
                    VarType.INT -> (v.initialValue.toDoubleOrNull()?.toInt() ?: 0).toString()
                    VarType.BOOLEAN -> if (v.initialValue.equals("true", true)) "true" else "false"
                    VarType.STRING -> quoteJava(v.initialValue)
                    VarType.MAP -> "new HashMap<>()"
                    VarType.LIST_NUMBER, VarType.LIST_STRING, VarType.LIST_MAP -> "new ArrayList<>()"
                }
                val jType = if (v.type == VarType.INT) "int" else v.type.javaType
                sb.appendLine("    private $jType ${safeIdent(v.name)} = $init;")
            }
            sb.appendLine()
        }

        if (project.widgets.isNotEmpty()) {
            sb.appendLine("    // Views")
            project.widgets.forEach { w ->
                sb.appendLine("    private ${w.type.javaClassName} ${safeIdent(w.id)};")
            }
            sb.appendLine()
        }

        if (project.components.isNotEmpty()) {
            sb.appendLine("    // Components")
            project.components.forEach { c ->
                sb.appendLine("    private ${c.type.javaClass} ${safeIdent(c.name)};")
            }
            sb.appendLine()
        }

        sb.appendLine("    private final Random _random = new Random();")
        sb.appendLine()
        sb.appendLine("    @Override")
        sb.appendLine("    protected void onCreate(Bundle _savedInstanceState) {")
        sb.appendLine("        super.onCreate(_savedInstanceState);")
        sb.appendLine("        setContentView(R.layout.main);")
        sb.appendLine("        initialize(_savedInstanceState);")
        sb.appendLine("        initializeLogic();")
        sb.appendLine("    }")
        sb.appendLine()
        sb.appendLine("    private void initialize(Bundle _savedInstanceState) {")
        project.widgets.forEach { w ->
            val id = safeIdent(w.id)
            sb.appendLine("        $id = findViewById(R.id.$id);")
        }
        sb.appendLine()
        project.widgets.forEach { w ->
            val id = safeIdent(w.id)
            val clickBlocks = project.events["${w.id}_onClick"].orEmpty()
            if (clickBlocks.isNotEmpty()) {
                sb.appendLine("        $id.setOnClickListener(new View.OnClickListener() {")
                sb.appendLine("            @Override")
                sb.appendLine("            public void onClick(View _view) {")
                emitBlocks(sb, clickBlocks, indent = 4)
                sb.appendLine("            }")
                sb.appendLine("        });")
            }
            val checkedBlocks = project.events["${w.id}_onCheckedChanged"].orEmpty()
            if (checkedBlocks.isNotEmpty()) {
                sb.appendLine("        $id.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {")
                sb.appendLine("            @Override")
                sb.appendLine("            public void onCheckedChanged(CompoundButton _buttonView, boolean _isChecked) {")
                emitBlocks(sb, checkedBlocks, indent = 4)
                sb.appendLine("            }")
                sb.appendLine("        });")
            }
            val progressBlocks = project.events["${w.id}_onProgressChanged"].orEmpty()
            if (progressBlocks.isNotEmpty()) {
                sb.appendLine("        $id.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {")
                sb.appendLine("            @Override public void onProgressChanged(SeekBar _sb, int _progress, boolean _fromUser) {")
                emitBlocks(sb, progressBlocks, indent = 4)
                sb.appendLine("            }")
                sb.appendLine("            @Override public void onStartTrackingTouch(SeekBar _sb) {}")
                sb.appendLine("            @Override public void onStopTrackingTouch(SeekBar _sb) {}")
                sb.appendLine("        });")
            }
            val textBlocks = project.events["${w.id}_onTextChanged"].orEmpty()
            if (textBlocks.isNotEmpty()) {
                sb.appendLine("        $id.addTextChangedListener(new TextWatcher() {")
                sb.appendLine("            @Override public void onTextChanged(CharSequence _charSeq, int _start, int _before, int _count) {")
                emitBlocks(sb, textBlocks, indent = 4)
                sb.appendLine("            }")
                sb.appendLine("            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}")
                sb.appendLine("            @Override public void afterTextChanged(Editable s) {}")
                sb.appendLine("        });")
            }
        }
        sb.appendLine("    }")
        sb.appendLine()
        sb.appendLine("    private void initializeLogic() {")
        val onCreateBlocks = project.events["onCreate"].orEmpty()
        if (onCreateBlocks.isNotEmpty()) {
            emitBlocks(sb, onCreateBlocks, indent = 2)
        }
        sb.appendLine("    }")

        project.moreblocks.forEach { mb ->
            sb.appendLine()
            val ret = if (mb.returnType == "Void") "void" else mb.returnType
            val paramsSig = mb.parameters.joinToString(", ") { "String ${safeIdent(it)}" }
            sb.appendLine("    public $ret ${safeIdent(mb.name)}($paramsSig) {")
            emitBlocks(sb, mb.blocks, indent = 2)
            sb.appendLine("    }")
        }

        sb.appendLine("}")
        return sb.toString()
    }

    /**
     * Mix-mode converter: converts user-written Java lines into visual Sketchware blocks!
     */
    fun parseJavaToBlocks(javaSnippet: String): List<BlockNode> {
        val lines = javaSnippet.lines().map { it.trim() }.filter { it.isNotBlank() && !it.startsWith("//") }
        val result = mutableListOf<BlockNode>()

        for (line in lines) {
            when {
                line.contains("Toast.makeText") -> {
                    val msg = Regex("\"([^\"]*)\"").find(line)?.groupValues?.get(1) ?: "Hello!"
                    result.add(BlockNode(type = BlockType.COMP_TOAST, params = mapOf("message" to msg)))
                }
                line.contains(".setText(") -> {
                    val target = line.substringBefore(".setText(").trim()
                    val arg = Regex("\"([^\"]*)\"").find(line)?.groupValues?.get(1) ?: line.substringAfter(".setText(").substringBeforeLast(")")
                    result.add(BlockNode(type = BlockType.VIEW_SET_TEXT, params = mapOf("widgetId" to target, "text" to arg)))
                }
                line.contains(".setTextColor(") -> {
                    val target = line.substringBefore(".setTextColor(").trim()
                    val hex = Regex("#[0-9A-Fa-f]{6,8}").find(line)?.value ?: "#2196F3"
                    result.add(BlockNode(type = BlockType.VIEW_SET_TEXT_COLOR, params = mapOf("widgetId" to target, "color" to hex)))
                }
                line.contains(".setBackgroundColor(") -> {
                    val target = line.substringBefore(".setBackgroundColor(").trim()
                    val hex = Regex("#[0-9A-Fa-f]{6,8}").find(line)?.value ?: "#1E293B"
                    result.add(BlockNode(type = BlockType.VIEW_SET_BG_COLOR, params = mapOf("widgetId" to target, "color" to hex)))
                }
                line.contains(".setVisibility(") -> {
                    val target = line.substringBefore(".setVisibility(").trim()
                    val vis = if (line.contains("GONE")) "GONE" else "VISIBLE"
                    result.add(BlockNode(type = BlockType.VIEW_SET_VISIBLE, params = mapOf("widgetId" to target, "visibility" to vis)))
                }
                line.contains(".setProgress(") -> {
                    val target = line.substringBefore(".setProgress(").trim()
                    val valStr = line.substringAfter(".setProgress(").substringBefore(")").trim()
                    result.add(BlockNode(type = BlockType.VIEW_SET_PROGRESS, params = mapOf("widgetId" to target, "value" to valStr)))
                }
                line.contains("++") -> {
                    val varName = line.substringBefore("++").trim()
                    result.add(BlockNode(type = BlockType.INCREASE_VAR, params = mapOf("varName" to varName, "amount" to "1")))
                }
                line.contains("--") -> {
                    val varName = line.substringBefore("--").trim()
                    result.add(BlockNode(type = BlockType.DECREASE_VAR, params = mapOf("varName" to varName, "amount" to "1")))
                }
                line.startsWith("if") -> {
                    val cond = line.substringAfter("(").substringBeforeLast(")").trim().ifBlank { "true" }
                    result.add(BlockNode(type = BlockType.IF_THEN, params = mapOf("condition" to cond)))
                }
                line.startsWith("for") -> {
                    val count = Regex("<\\s*(\\d+)").find(line)?.groupValues?.get(1) ?: "5"
                    result.add(BlockNode(type = BlockType.REPEAT, params = mapOf("times" to count)))
                }
                else -> {
                    result.add(BlockNode(type = BlockType.ADD_SOURCE_DIRECTLY, params = mapOf("java" to line)))
                }
            }
        }
        return result
    }

    private fun emitBlocks(sb: StringBuilder, blocks: List<BlockNode>, indent: Int, depth: Int = 0) {
        val pad = "    ".repeat(indent)
        blocks.forEach { b ->
            val p = b.params
            when (b.type) {
                BlockType.SET_VAR_BOOLEAN -> sb.appendLine("${pad}${safeIdent(p["varName"] ?: "flag")} = ${p["value"] ?: "true"};")
                BlockType.SET_VAR_NUMBER -> sb.appendLine("${pad}${safeIdent(p["varName"] ?: "count")} = ${p["value"] ?: "0"};")
                BlockType.SET_VAR_STRING -> sb.appendLine("${pad}${safeIdent(p["varName"] ?: "text")} = ${interpolateJava(p["value"] ?: "")};")
                BlockType.INCREASE_VAR -> sb.appendLine("${pad}${safeIdent(p["varName"] ?: "count")} += ${p["amount"] ?: "1"};")
                BlockType.DECREASE_VAR -> sb.appendLine("${pad}${safeIdent(p["varName"] ?: "count")} -= ${p["amount"] ?: "1"};")
                BlockType.MAP_CREATE_NEW -> sb.appendLine("${pad}${safeIdent(p["mapName"] ?: "map1")} = new HashMap<>();")
                BlockType.MAP_PUT_KEY -> sb.appendLine("${pad}${safeIdent(p["mapName"] ?: "map1")}.put(${quoteJava(p["key"] ?: "k")}, ${interpolateJava(p["value"] ?: "")});")
                BlockType.MAP_CLEAR -> sb.appendLine("${pad}${safeIdent(p["mapName"] ?: "map1")}.clear();")
                BlockType.LIST_ADD -> sb.appendLine("${pad}${safeIdent(p["listName"] ?: "list1")}.add(${interpolateJava(p["value"] ?: "")});")
                BlockType.LIST_INSERT -> sb.appendLine("${pad}${safeIdent(p["listName"] ?: "list1")}.add(${p["index"] ?: "0"}, ${interpolateJava(p["value"] ?: "")});")
                BlockType.LIST_DELETE_AT -> sb.appendLine("${pad}${safeIdent(p["listName"] ?: "list1")}.remove(${p["index"] ?: "0"});")
                BlockType.LIST_CLEAR -> sb.appendLine("${pad}${safeIdent(p["listName"] ?: "list1")}.clear();")
                BlockType.LIST_SORT -> sb.appendLine("${pad}Collections.sort(${safeIdent(p["listName"] ?: "list1")});")
                BlockType.LIST_REVERSE -> sb.appendLine("${pad}Collections.reverse(${safeIdent(p["listName"] ?: "list1")});")
                BlockType.IF_THEN -> {
                    sb.appendLine("${pad}if (${p["condition"] ?: "true"}) {")
                    emitBlocks(sb, b.children, indent + 1, depth + 1)
                    sb.appendLine("${pad}}")
                }
                BlockType.IF_ELSE -> {
                    sb.appendLine("${pad}if (${p["condition"] ?: "true"}) {")
                    emitBlocks(sb, b.children, indent + 1, depth + 1)
                    sb.appendLine("${pad}} else {")
                    emitBlocks(sb, b.elseChildren, indent + 1, depth + 1)
                    sb.appendLine("${pad}}")
                }
                BlockType.REPEAT -> {
                    val idx = "_repeat$depth"
                    sb.appendLine("${pad}for (int $idx = 0; $idx < ${p["times"] ?: "5"}; $idx++) {")
                    emitBlocks(sb, b.children, indent + 1, depth + 1)
                    sb.appendLine("${pad}}")
                }
                BlockType.REPEAT_WHILE -> {
                    sb.appendLine("${pad}while (${p["condition"] ?: "true"}) {")
                    emitBlocks(sb, b.children, indent + 1, depth + 1)
                    sb.appendLine("${pad}}")
                }
                BlockType.FOREVER -> {
                    sb.appendLine("${pad}while (true) {")
                    emitBlocks(sb, b.children, indent + 1, depth + 1)
                    sb.appendLine("${pad}}")
                }
                BlockType.BREAK -> sb.appendLine("${pad}break;")
                BlockType.TRY_CATCH -> {
                    sb.appendLine("${pad}try {")
                    emitBlocks(sb, b.children, indent + 1, depth + 1)
                    sb.appendLine("${pad}} catch (Exception _e) {")
                    if (b.elseChildren.isEmpty()) {
                        sb.appendLine("${pad}    _e.printStackTrace();")
                    } else {
                        emitBlocks(sb, b.elseChildren, indent + 1, depth + 1)
                    }
                    sb.appendLine("${pad}}")
                }
                BlockType.STR_JOIN -> sb.appendLine("${pad}${safeIdent(p["varName"] ?: "text")} = ${interpolateJava(p["a"] ?: "")} + ${interpolateJava(p["b"] ?: "")};")
                BlockType.STR_REPLACE -> sb.appendLine("${pad}${safeIdent(p["varName"] ?: "text")} = ${safeIdent(p["source"] ?: "text")}.replace(${quoteJava(p["old"] ?: "")}, ${quoteJava(p["new"] ?: "")});")
                BlockType.STR_UPPER -> sb.appendLine("${pad}${safeIdent(p["varName"] ?: "text")} = ${safeIdent(p["source"] ?: "text")}.toUpperCase();")
                BlockType.STR_LOWER -> sb.appendLine("${pad}${safeIdent(p["varName"] ?: "text")} = ${safeIdent(p["source"] ?: "text")}.toLowerCase();")
                BlockType.STR_TRIM -> sb.appendLine("${pad}${safeIdent(p["varName"] ?: "text")} = ${safeIdent(p["source"] ?: "text")}.trim();")
                BlockType.STR_TO_NUM -> sb.appendLine("${pad}${safeIdent(p["varName"] ?: "count")} = Integer.parseInt(${safeIdent(p["source"] ?: "text")});")
                BlockType.NUM_TO_STR -> sb.appendLine("${pad}${safeIdent(p["varName"] ?: "text")} = String.valueOf(${safeIdent(p["source"] ?: "count")});")
                BlockType.MATH_RANDOM -> sb.appendLine("${pad}${safeIdent(p["varName"] ?: "count")} = _random.nextInt((${p["max"] ?: "100"}) - (${p["min"] ?: "1"}) + 1) + (${p["min"] ?: "1"});")
                BlockType.MATH_CALC -> sb.appendLine("${pad}${safeIdent(p["varName"] ?: "count")} = ${p["left"] ?: "0"} ${p["op"] ?: "+"} ${p["right"] ?: "1"};")
                BlockType.MATH_ROUND -> sb.appendLine("${pad}${safeIdent(p["varName"] ?: "count")} = (int) Math.round(${p["value"] ?: "0"});")
                BlockType.MATH_SQRT -> sb.appendLine("${pad}${safeIdent(p["varName"] ?: "count")} = (int) Math.sqrt(${p["value"] ?: "16"});")
                BlockType.FILE_WRITE -> sb.appendLine("${pad}FileUtil.writeFile(${quoteJava(p["path"] ?: "")}, ${interpolateJava(p["data"] ?: "")});")
                BlockType.FILE_READ -> sb.appendLine("${pad}${safeIdent(p["varName"] ?: "text")} = FileUtil.readFile(${quoteJava(p["path"] ?: "")});")
                BlockType.FILE_DELETE -> sb.appendLine("${pad}FileUtil.deleteFile(${quoteJava(p["path"] ?: "")});")
                BlockType.FILE_MKDIR -> sb.appendLine("${pad}FileUtil.makeDir(${quoteJava(p["path"] ?: "")});")
                BlockType.VIEW_SET_TEXT -> sb.appendLine("${pad}${safeIdent(p["widgetId"] ?: "textview1")}.setText(${interpolateJava(p["text"] ?: "")});")
                BlockType.VIEW_GET_TEXT -> sb.appendLine("${pad}${safeIdent(p["varName"] ?: "text")} = ${safeIdent(p["widgetId"] ?: "edittext1")}.getText().toString();")
                BlockType.VIEW_SET_TEXT_COLOR -> sb.appendLine("${pad}${safeIdent(p["widgetId"] ?: "textview1")}.setTextColor(Color.parseColor(${quoteJava(p["color"] ?: "#2196F3")}));")
                BlockType.VIEW_SET_BG_COLOR -> sb.appendLine("${pad}${safeIdent(p["widgetId"] ?: "button1")}.setBackgroundColor(Color.parseColor(${quoteJava(p["color"] ?: "#1E293B")}));")
                BlockType.VIEW_SET_VISIBLE -> {
                    val vis = if ((p["visibility"] ?: "VISIBLE").equals("GONE", true)) "View.GONE" else if ((p["visibility"] ?: "VISIBLE").equals("INVISIBLE", true)) "View.INVISIBLE" else "View.VISIBLE"
                    sb.appendLine("${pad}${safeIdent(p["widgetId"] ?: "textview1")}.setVisibility($vis);")
                }
                BlockType.VIEW_SET_ENABLED -> sb.appendLine("${pad}${safeIdent(p["widgetId"] ?: "button1")}.setEnabled(${p["enabled"] ?: "true"});")
                BlockType.VIEW_SET_PROGRESS -> sb.appendLine("${pad}${safeIdent(p["widgetId"] ?: "progress1")}.setProgress(${p["value"]?.removeSurrounding("{", "}") ?: "0"});")
                BlockType.VIEW_SET_CHECKED -> sb.appendLine("${pad}${safeIdent(p["widgetId"] ?: "checkbox1")}.setChecked(${p["checked"] ?: "true"});")
                BlockType.VIEW_LOAD_URL -> sb.appendLine("${pad}${safeIdent(p["widgetId"] ?: "webview1")}.loadUrl(${quoteJava(p["url"] ?: "https://google.com")});")
                BlockType.VIEW_LIST_REFRESH -> sb.appendLine("${pad}((BaseAdapter) ${safeIdent(p["widgetId"] ?: "listview1")}.getAdapter()).notifyDataSetChanged();")
                BlockType.COMP_TOAST -> sb.appendLine("${pad}Toast.makeText(getApplicationContext(), ${interpolateJava(p["message"] ?: "")}, Toast.LENGTH_SHORT).show();")
                BlockType.COMP_SNACKBAR -> sb.appendLine("${pad}Snackbar.make(findViewById(android.R.id.content), ${interpolateJava(p["message"] ?: "")}, Snackbar.LENGTH_SHORT).show();")
                BlockType.COMP_DIALOG_SHOW -> {
                    sb.appendLine("${pad}new AlertDialog.Builder(this).setTitle(${interpolateJava(p["title"] ?: "Info")}).setMessage(${interpolateJava(p["message"] ?: "")}).setPositiveButton(\"OK\", null).show();")
                }
                BlockType.COMP_INTENT_SCREEN -> {
                    val rawScreen = p["screen"]?.trim().orEmpty().ifBlank { "MainActivity" }
                    val cleanCls = if (rawScreen.equals("MainActivity", ignoreCase = true) || rawScreen.equals("main", ignoreCase = true)) {
                        "MainActivity"
                    } else {
                        val ident = safeIdent(rawScreen)
                        if (ident.endsWith("Activity", ignoreCase = true)) ident.replaceFirstChar { it.uppercaseChar() }
                        else "${ident.replaceFirstChar { it.uppercaseChar() }}Activity"
                    }
                    sb.appendLine("${pad}${safeIdent(p["compName"] ?: "intent")}.setClass(getApplicationContext(), ${cleanCls}.class);")
                }
                BlockType.COMP_START_ACTIVITY -> sb.appendLine("${pad}startActivity(${safeIdent(p["compName"] ?: "intent")});")
                BlockType.COMP_FINISH_ACTIVITY -> sb.appendLine("${pad}finish();")
                BlockType.COMP_SHARED_PREF_SET -> sb.appendLine("${pad}${safeIdent(p["compName"] ?: "sp")}.edit().putString(${quoteJava(p["key"] ?: "k")}, ${interpolateJava(p["value"] ?: "")}).apply();")
                BlockType.COMP_TIMER_AFTER -> {
                    sb.appendLine("${pad}new Handler(Looper.getMainLooper()).postDelayed(() -> {")
                    emitBlocks(sb, b.children, indent + 1, depth + 1)
                    sb.appendLine("${pad}}, ${p["ms"] ?: "1000"});")
                }
                BlockType.COMP_VIBRATE -> sb.appendLine("${pad}((Vibrator) getSystemService(VIBRATOR_SERVICE)).vibrate(${p["ms"] ?: "200"});")
                BlockType.COMP_FIREBASE_PUSH -> sb.appendLine("${pad}${safeIdent(p["compName"] ?: "fb")}.push().setValue(${safeIdent(p["mapName"] ?: "map1")});")
                BlockType.COMP_NETWORK_REQUEST -> sb.appendLine("${pad}${safeIdent(p["compName"] ?: "net")}.startRequestNetwork(${quoteJava(p["method"] ?: "GET")}, ${quoteJava(p["url"] ?: "")}, \"req\", _reqListener);")
                BlockType.COMP_TTS_SPEAK -> sb.appendLine("${pad}${safeIdent(p["compName"] ?: "tts")}.speak(${interpolateJava(p["text"] ?: "")}, TextToSpeech.QUEUE_FLUSH, null, null);")
                BlockType.COMP_NOTIFY -> sb.appendLine("${pad}NotificationUtil.show(getApplicationContext(), ${interpolateJava(p["title"] ?: "Alert")}, ${interpolateJava(p["body"] ?: "")});")
                BlockType.XML_SET_STRING -> sb.appendLine("${pad}String _resStr = getString(R.string.${safeIdent(p["key"] ?: "app_name")});")
                BlockType.XML_SET_COLOR -> sb.appendLine("${pad}int _resCol = getColor(R.color.${safeIdent(p["key"] ?: "colorPrimary")});")
                BlockType.CALL_MOREBLOCK -> sb.appendLine("${pad}${safeIdent(p["blockName"] ?: "myHelper")}();")
                BlockType.ADD_SOURCE_DIRECTLY, BlockType.COLLECTION_SNIPPET -> {
                    (p["java"] ?: "// Custom Java").lines().forEach { line -> sb.appendLine("${pad}$line") }
                }
            }
        }
    }

    fun generateXmlLayout(widgets: List<WidgetNode>, backgroundHex: String = "#FFFFFF"): String {
        val sb = StringBuilder()
        sb.appendLine("<?xml version=\"1.0\" encoding=\"utf-8\"?>")
        sb.appendLine("<LinearLayout")
        sb.appendLine("    xmlns:android=\"http://schemas.android.com/apk/res/android\"")
        sb.appendLine("    xmlns:app=\"http://schemas.android.com/apk/res-auto\"")
        sb.appendLine("    android:layout_width=\"match_parent\"")
        sb.appendLine("    android:layout_height=\"match_parent\"")
        sb.appendLine("    android:orientation=\"vertical\"")
        sb.appendLine("    android:background=\"${safeHex(backgroundHex)}\">")
        widgets.forEach { w ->
            sb.appendLine()
            emitWidgetXml(sb, w)
        }
        sb.appendLine()
        sb.appendLine("</LinearLayout>")
        return sb.toString()
    }

    fun generateCustomViewXml(customView: CustomViewDef): String =
        generateXmlLayout(customView.widgets, backgroundHex = "#FFFFFF")

    private fun emitWidgetXml(sb: StringBuilder, w: WidgetNode) {
        val id = safeIdent(w.id)
        val width = if (w.widthMode == "WRAP_CONTENT") "wrap_content" else "match_parent"
        val height = if (w.type == WidgetType.DIVIDER) "1dp" else if (w.heightMode == "MATCH_PARENT") "match_parent" else "wrap_content"
        sb.appendLine("    <${w.type.xmlTag}")
        sb.appendLine("        android:id=\"@+id/$id\"")
        sb.appendLine("        android:layout_width=\"$width\"")
        sb.appendLine("        android:layout_height=\"$height\"")
        sb.appendLine("        android:padding=\"${w.paddingDp}dp\"")
        if (w.type == WidgetType.LINEAR_H) sb.appendLine("        android:orientation=\"horizontal\"")
        if (w.type == WidgetType.LINEAR_V) sb.appendLine("        android:orientation=\"vertical\"")
        if (w.type.supportsText && w.text.isNotEmpty()) sb.appendLine("        android:text=\"${escapeXml(w.text)}\"")
        if (w.type.supportsHint && w.hint.isNotEmpty()) sb.appendLine("        android:hint=\"${escapeXml(w.hint)}\"")
        sb.appendLine("        android:textSize=\"${w.textSize}sp\"")
        sb.appendLine("        android:textColor=\"${safeHex(w.textColor)}\"")
        if (w.backgroundColor != "#00000000") sb.appendLine("        android:background=\"${safeHex(w.backgroundColor)}\"")
        sb.appendLine("        />")
    }

    fun generateManifestXml(project: MaxProject): String {
        val pkg = project.packageName.ifBlank { "com.my.newproject" }
        return buildString {
            appendLine("<?xml version=\"1.0\" encoding=\"utf-8\"?>")
            appendLine("<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\"")
            appendLine("    package=\"$pkg\"")
            appendLine("    android:versionCode=\"${project.versionCode}\"")
            appendLine("    android:versionName=\"${escapeXml(project.versionName)}\">")
            appendLine()
            project.permissions.distinct().forEach { perm ->
                appendLine("    <uses-permission android:name=\"$perm\" />")
            }
            appendLine()
            appendLine("    <application")
            appendLine("        android:allowBackup=\"true\"")
            appendLine("        android:label=\"${escapeXml(project.appName)}\"")
            appendLine("        android:supportsRtl=\"true\"")
            appendLine("        android:theme=\"@style/AppTheme\">")
            appendLine("        <activity android:name=\".MainActivity\" android:exported=\"true\">")
            appendLine("            <intent-filter>")
            appendLine("                <action android:name=\"android.intent.action.MAIN\" />")
            appendLine("                <category android:name=\"android.intent.category.LAUNCHER\" />")
            appendLine("            </intent-filter>")
            appendLine("        </activity>")
            project.customViews.forEach { cv ->
                appendLine("        <activity android:name=\".${cv.activityClassName()}\" android:exported=\"false\" />")
            }
            appendLine("    </application>")
            appendLine("</manifest>")
        }
    }

    fun generateGradleDependencies(project: MaxProject): String = buildString {
        appendLine("dependencies {")
        project.libraries.filter { it.enabled }.forEach { lib ->
            if (lib.sourceType in setOf("ZIP", "AAR", "JAR")) {
                appendLine("    implementation files(\"libs/${lib.name}\")")
            } else {
                appendLine("    implementation \"${lib.coordinate}\"")
            }
        }
        project.firebaseConfig?.let {
            appendLine("    // Firebase Project: ${it.projectId}")
            appendLine("    implementation platform(\"com.google.firebase:firebase-bom:33.5.1\")")
        }
        appendLine("}")
    }

    private fun safeIdent(raw: String): String {
        val cleaned = raw.replace(Regex("[^a-zA-Z0-9_]"), "_")
        return if (cleaned.isEmpty() || cleaned.first().isDigit()) "_$cleaned" else cleaned
    }

    private fun safeHex(raw: String): String =
        if (Regex("^#[0-9A-Fa-f]{6,8}$").matches(raw)) raw else "#2196F3"

    private fun quoteJava(raw: String): String =
        "\"${raw.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n")}\""

    private fun escapeXml(raw: String): String =
        raw.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;")

    private fun interpolateJava(template: String): String {
        val regex = Regex("\\{([a-zA-Z0-9_]+)\\}")
        val matches = regex.findAll(template).toList()
        if (matches.isEmpty()) return quoteJava(template)
        val parts = mutableListOf<String>()
        var cursor = 0
        for (m in matches) {
            if (m.range.first > cursor) parts.add(quoteJava(template.substring(cursor, m.range.first)))
            parts.add("String.valueOf(${safeIdent(m.groupValues[1])})")
            cursor = m.range.last + 1
        }
        if (cursor < template.length) parts.add(quoteJava(template.substring(cursor)))
        return parts.joinToString(" + ")
    }
}
