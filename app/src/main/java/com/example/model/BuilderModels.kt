package com.example.model

import androidx.compose.ui.graphics.Color
import java.util.UUID
import kotlin.random.Random

enum class BackendMode(
    val title: String,
    val badge: String,
    val description: String,
) {
    BLOCKS_ONLY(
        title = "Blocks System",
        badge = "Visual Blocks",
        description = "Use Sketchware Pro drag-and-drop blocks only. Java code is viewable (read-only).",
    ),
    JAVA_ONLY(
        title = "Java Code System",
        badge = "Pure Java",
        description = "Write and edit MainActivity.java & XML directly without blocks.",
    ),
    MIX_MODE(
        title = "Mix System (Java + Blocks)",
        badge = "Blocks + Java",
        description = "Use visual Blocks AND edit/convert Java code directly into blocks.",
    );

    companion object {
        fun fromRaw(raw: String?): BackendMode =
            entries.firstOrNull { it.name == raw } ?: MIX_MODE
    }
}

enum class WidgetType(
    val displayName: String,
    val idPrefix: String,
    val javaClassName: String,
    val xmlTag: String,
    val category: String,
    val supportsText: Boolean = false,
    val supportsHint: Boolean = false,
    val supportsOnClick: Boolean = true,
) {
    // Layouts (Screenshot 11)
    LINEAR_H("Linear(H)", "linear_h", "LinearLayout", "LinearLayout", "Layouts"),
    LINEAR_V("Linear(V)", "linear_v", "LinearLayout", "LinearLayout", "Layouts"),
    SCROLL_H("Scroll(H)", "hscroll", "HorizontalScrollView", "HorizontalScrollView", "Layouts"),
    SCROLL_V("Scroll(V)", "vscroll", "ScrollView", "ScrollView", "Layouts"),
    RADIO_GROUP("RadioGroup", "radiogroup", "RadioGroup", "RadioGroup", "Layouts"),
    RELATIVE_LAYOUT("RelativeLayout", "relativelayout", "RelativeLayout", "RelativeLayout", "Layouts"),

    // AndroidX (Screenshot 11)
    TAB_LAYOUT("TabLayout", "tablayout", "TabLayout", "com.google.android.material.tabs.TabLayout", "AndroidX"),
    BOTTOM_NAV("BottomNavigationView", "bottomnav", "BottomNavigationView", "com.google.android.material.bottomnavigation.BottomNavigationView", "AndroidX"),
    COLLAPSING_TOOLBAR("CollapsingToolbar", "collapsing", "CollapsingToolbarLayout", "com.google.android.material.appbar.CollapsingToolbarLayout", "AndroidX"),
    CARD_VIEW("CardView", "cardview", "CardView", "androidx.cardview.widget.CardView", "AndroidX", supportsText = true),
    TEXT_INPUT_LAYOUT("TextInputLayout", "textinput", "TextInputLayout", "com.google.android.material.textfield.TextInputLayout", "AndroidX", supportsHint = true),
    SWIPE_REFRESH("SwipeRefreshLayout", "swiperefresh", "SwipeRefreshLayout", "androidx.swiperefreshlayout.widget.SwipeRefreshLayout", "AndroidX"),

    // Widgets (Screenshots 11-12)
    TEXT_VIEW("TextView", "textview", "TextView", "TextView", "Widgets", supportsText = true),
    EDIT_TEXT("EditText", "edittext", "EditText", "EditText", "Widgets", supportsText = true, supportsHint = true, supportsOnClick = false),
    AUTO_COMPLETE("AutoCompleteTextView", "autocomplete", "AutoCompleteTextView", "AutoCompleteTextView", "Widgets", supportsText = true, supportsHint = true),
    MULTI_AUTO_COMPLETE("MultiAutoCompleteTextView", "multiauto", "MultiAutoCompleteTextView", "MultiAutoCompleteTextView", "Widgets", supportsText = true, supportsHint = true),
    BUTTON("Button", "button", "Button", "Button", "Widgets", supportsText = true),
    MATERIAL_BUTTON("MaterialButton", "materialbutton", "MaterialButton", "com.google.android.material.button.MaterialButton", "Widgets", supportsText = true),
    IMAGE_VIEW("ImageView", "imageview", "ImageView", "ImageView", "Widgets"),
    CIRCLE_IMAGE_VIEW("CircleImageView", "circleimageview", "CircleImageView", "de.hdodenhof.circleimageview.CircleImageView", "Widgets"),
    CHECK_BOX("CheckBox", "checkbox", "CheckBox", "CheckBox", "Widgets", supportsText = true),
    RADIO_BUTTON("RadioButton", "radiobutton", "RadioButton", "RadioButton", "Widgets", supportsText = true),
    SWITCH("Switch", "switch", "Switch", "Switch", "Widgets", supportsText = true),
    SEEK_BAR("SeekBar", "seekbar", "SeekBar", "SeekBar", "Widgets", supportsOnClick = false),
    PROGRESS_BAR("ProgressBar", "progressbar", "ProgressBar", "ProgressBar", "Widgets", supportsOnClick = false),
    RATING_BAR("RatingBar", "ratingbar", "RatingBar", "RatingBar", "Widgets", supportsOnClick = false),
    SEARCH_VIEW("SearchView", "searchview", "SearchView", "androidx.appcompat.widget.SearchView", "Widgets", supportsHint = true),
    VIDEO_VIEW("VideoView", "videoview", "VideoView", "VideoView", "Widgets"),
    WEB_VIEW("WebView", "webview", "WebView", "WebView", "Widgets", supportsOnClick = false),

    // List (Screenshot 12)
    LIST_VIEW("ListView", "listview", "ListView", "ListView", "List"),
    GRID_VIEW("GridView", "gridview", "GridView", "GridView", "List"),
    RECYCLER_VIEW("RecyclerView", "recyclerview", "RecyclerView", "androidx.recyclerview.widget.RecyclerView", "List"),
    SPINNER("Spinner", "spinner", "Spinner", "Spinner", "List"),
    VIEW_PAGER("ViewPager", "viewpager", "ViewPager", "androidx.viewpager.widget.ViewPager", "List"),

    // Library (Screenshot 12)
    WAVE_SIDE_BAR("WaveSideBar", "wavesidebar", "WaveSideBar", "com.gjiazhe.wavesidebar.WaveSideBar", "Library"),
    PATTERN_LOCK("PatternLockView", "patternlock", "PatternLockView", "com.andrognito.patternlockview.PatternLockView", "Library"),
    CODE_VIEW("CodeView", "codeview", "CodeView", "io.github.kbiakov.codeview.CodeView", "Library", supportsText = true),
    LOTTIE("LottieAnimation", "lottie", "LottieAnimationView", "com.airbnb.lottie.LottieAnimationView", "Library"),
    OTP_VIEW("OTPView", "otpview", "PinView", "com.chaos.view.PinView", "Library"),

    // Google (Screenshot 12)
    AD_VIEW("AdView", "adview", "AdView", "com.google.android.gms.ads.AdView", "Google"),
    MAP_VIEW("MapView", "mapview", "MapView", "com.google.android.gms.maps.MapView", "Google"),
    SIGN_IN_BUTTON("SignInButton", "signinbutton", "SignInButton", "com.google.android.gms.common.SignInButton", "Google"),
    YOUTUBE_PLAYER("YoutubePlayer", "youtube", "YouTubePlayerView", "com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView", "Google"),

    // Date & Time (Screenshot 12)
    ANALOG_CLOCK("AnalogClock", "analogclock", "AnalogClock", "AnalogClock", "Date & Time"),
    DIGITAL_CLOCK("DigitalClock", "digitalclock", "DigitalClock", "DigitalClock", "Date & Time"),
    TIME_PICKER("TimePicker", "timepicker", "TimePicker", "TimePicker", "Date & Time"),
    DATE_PICKER("DatePicker", "datepicker", "DatePicker", "DatePicker", "Date & Time"),
    CALENDAR_VIEW("CalendarView", "calendarview", "CalendarView", "CalendarView", "Date & Time"),

    // Utility
    FAB("FAB", "fab", "FloatingActionButton", "com.google.android.material.floatingactionbutton.FloatingActionButton", "Widgets"),
    DIVIDER("Divider", "divider", "View", "View", "Widgets", supportsOnClick = false);

    companion object {
        fun fromRaw(raw: String?): WidgetType =
            entries.firstOrNull { it.name == raw } ?: TEXT_VIEW
    }
}

data class WidgetNode(
    val id: String,
    val type: WidgetType,
    val text: String = "",
    val hint: String = "",
    val textSize: Int = 16,
    val textColor: String = "#111827",
    val backgroundColor: String = "#00000000",
    val widthMode: String = "MATCH_PARENT",
    val heightMode: String = "WRAP_CONTENT",
    val paddingDp: Int = 10,
    val marginDp: Int = 6,
    val gravity: String = "START",
    val cornerRadius: Int = 8,
    val progress: Int = 45,
    val checked: Boolean = false,
    val visibility: String = "VISIBLE",
    val itemsCsv: String = "Item 1,Item 2,Item 3",
    val url: String = "https://example.com",
) {
    fun toMap(): Map<String, Any> = mapOf(
        "id" to id,
        "type" to type.name,
        "text" to text,
        "hint" to hint,
        "textSize" to textSize,
        "textColor" to textColor,
        "backgroundColor" to backgroundColor,
        "widthMode" to widthMode,
        "heightMode" to heightMode,
        "paddingDp" to paddingDp,
        "marginDp" to marginDp,
        "gravity" to gravity,
        "cornerRadius" to cornerRadius,
        "progress" to progress,
        "checked" to checked,
        "visibility" to visibility,
        "itemsCsv" to itemsCsv,
        "url" to url,
    )

    companion object {
        fun fromMap(map: Map<*, *>): WidgetNode {
            val type = WidgetType.fromRaw(map["type"] as? String)
            return WidgetNode(
                id = (map["id"] as? String)?.ifBlank { null } ?: "${type.idPrefix}1",
                type = type,
                text = (map["text"] as? String) ?: "",
                hint = (map["hint"] as? String) ?: "",
                textSize = ((map["textSize"] as? Number)?.toInt() ?: 16).coerceIn(10, 48),
                textColor = (map["textColor"] as? String) ?: "#111827",
                backgroundColor = (map["backgroundColor"] as? String) ?: "#00000000",
                widthMode = (map["widthMode"] as? String) ?: "MATCH_PARENT",
                heightMode = (map["heightMode"] as? String) ?: "WRAP_CONTENT",
                paddingDp = ((map["paddingDp"] as? Number)?.toInt() ?: 10).coerceIn(0, 48),
                marginDp = ((map["marginDp"] as? Number)?.toInt() ?: 6).coerceIn(0, 32),
                gravity = (map["gravity"] as? String) ?: "START",
                cornerRadius = ((map["cornerRadius"] as? Number)?.toInt() ?: 8).coerceIn(0, 32),
                progress = ((map["progress"] as? Number)?.toInt() ?: 45).coerceIn(0, 100),
                checked = (map["checked"] as? Boolean) ?: false,
                visibility = (map["visibility"] as? String) ?: "VISIBLE",
                itemsCsv = (map["itemsCsv"] as? String) ?: "Item 1,Item 2,Item 3",
                url = (map["url"] as? String) ?: "https://example.com",
            )
        }

        fun createDefault(type: WidgetType, existingWidgets: List<WidgetNode>): WidgetNode {
            var index = 1
            val existingIds = existingWidgets.map { it.id }.toSet()
            while ("${type.idPrefix}$index" in existingIds) index++
            val newId = "${type.idPrefix}$index"
            return when (type) {
                WidgetType.LINEAR_H, WidgetType.LINEAR_V, WidgetType.SCROLL_H,
                WidgetType.SCROLL_V, WidgetType.RADIO_GROUP, WidgetType.RELATIVE_LAYOUT -> WidgetNode(
                    id = newId,
                    type = type,
                    text = type.displayName,
                    textColor = "#1F2937",
                    backgroundColor = "#E5E7EB",
                    paddingDp = 12,
                )
                WidgetType.BUTTON, WidgetType.MATERIAL_BUTTON -> WidgetNode(
                    id = newId,
                    type = type,
                    text = "${type.displayName} $index",
                    textSize = 15,
                    textColor = "#FFFFFF",
                    backgroundColor = "#2196F3",
                    cornerRadius = 8,
                    gravity = "CENTER",
                )
                WidgetType.TEXT_VIEW -> WidgetNode(
                    id = newId,
                    type = type,
                    text = "TextView $index",
                    textSize = 16,
                    textColor = "#111827",
                )
                WidgetType.EDIT_TEXT, WidgetType.AUTO_COMPLETE, WidgetType.MULTI_AUTO_COMPLETE, WidgetType.TEXT_INPUT_LAYOUT -> WidgetNode(
                    id = newId,
                    type = type,
                    text = "",
                    hint = "Enter text...",
                    textSize = 15,
                    textColor = "#111827",
                    backgroundColor = "#F3F4F6",
                )
                WidgetType.CARD_VIEW -> WidgetNode(
                    id = newId,
                    type = type,
                    text = "CardView $index",
                    textColor = "#111827",
                    backgroundColor = "#FFFFFF",
                    cornerRadius = 12,
                    paddingDp = 14,
                )
                WidgetType.FAB -> WidgetNode(
                    id = newId,
                    type = type,
                    text = "+",
                    textColor = "#FFFFFF",
                    backgroundColor = "#2196F3",
                    widthMode = "WRAP_CONTENT",
                    cornerRadius = 28,
                    gravity = "END",
                )
                WidgetType.DIVIDER -> WidgetNode(
                    id = newId,
                    type = type,
                    backgroundColor = "#CBD5E1",
                    paddingDp = 2,
                )
                else -> WidgetNode(
                    id = newId,
                    type = type,
                    text = type.displayName,
                    textColor = "#111827",
                    backgroundColor = "#F8FAFC",
                )
            }
        }
    }
}

// Variables matching Screenshot 8 (Add Variable Type)
enum class VarType(val javaType: String, val label: String) {
    BOOLEAN("boolean", "Boolean"),
    INT("double", "Number"),
    STRING("String", "String"),
    MAP("HashMap<String, Object>", "Map"),
    LIST_NUMBER("ArrayList<Double>", "List Number"),
    LIST_STRING("ArrayList<String>", "List String"),
    LIST_MAP("ArrayList<HashMap<String, Object>>", "List Map");

    companion object {
        fun fromRaw(raw: String?): VarType =
            entries.firstOrNull { it.name == raw } ?: INT
    }
}

data class VariableDef(
    val name: String,
    val type: VarType = VarType.INT,
    val initialValue: String = "0",
) {
    fun toMap(): Map<String, Any> = mapOf(
        "name" to name,
        "type" to type.name,
        "initialValue" to initialValue,
    )

    companion object {
        fun fromMap(map: Map<*, *>): VariableDef = VariableDef(
            name = (map["name"] as? String)?.ifBlank { null } ?: "var1",
            type = VarType.fromRaw(map["type"] as? String),
            initialValue = (map["initialValue"] as? String) ?: "0",
        )
    }
}

// Components matching Screenshots 14 & 15
enum class ComponentType(val label: String, val javaClass: String) {
    INTENT("Intent", "Intent"),
    SHARED_PREFS("SharedPreferences", "SharedPreferences"),
    CALENDAR("Calendar", "Calendar"),
    VIBRATOR("Vibrator", "Vibrator"),
    TIMER("Timer", "TimerTask"),
    DIALOG("Dialog", "AlertDialog.Builder"),
    MEDIA_PLAYER("MediaPlayer", "MediaPlayer"),
    SOUND_POOL("SoundPool", "SoundPool"),
    OBJECT_ANIMATOR("ObjectAnimator", "ObjectAnimator"),
    FIREBASE_DB("Firebase DB", "DatabaseReference"),
    FIREBASE_AUTH("Firebase Auth", "FirebaseAuth"),
    FIREBASE_STORAGE("FirebaseStorage", "StorageReference"),
    CAMERA("Camera", "Camera"),
    FILE_PICKER("FilePicker", "Intent"),
    REQUEST_NETWORK("RequestNetwork", "RequestNetwork"),
    TEXT_TO_SPEECH("TextToSpeech", "TextToSpeech"),
    SPEECH_TO_TEXT("SpeechToText", "SpeechRecognizer"),
    LOCATION_MANAGER("LocationManager", "LocationManager"),
    VIDEO_AD("VideoAd", "RewardedAd"),
    PROGRESS_DIALOG("ProgressDialog", "ProgressDialog"),
    TIME_PICKER_DIALOG("TimePickerDialog", "TimePickerDialog"),
    NOTIFICATION("Notification", "NotificationManager");

    companion object {
        fun fromRaw(raw: String?): ComponentType =
            entries.firstOrNull { it.name == raw } ?: INTENT
    }
}

data class ComponentDef(
    val id: String = UUID.randomUUID().toString().take(8),
    val name: String,
    val type: ComponentType,
    val extra: String = "",
) {
    fun toMap(): Map<String, Any> = mapOf(
        "id" to id,
        "name" to name,
        "type" to type.name,
        "extra" to extra,
    )

    companion object {
        fun fromMap(map: Map<*, *>): ComponentDef = ComponentDef(
            id = (map["id"] as? String)?.ifBlank { null } ?: UUID.randomUUID().toString().take(8),
            name = (map["name"] as? String)?.ifBlank { null } ?: "comp1",
            type = ComponentType.fromRaw(map["type"] as? String),
            extra = (map["extra"] as? String) ?: "",
        )
    }
}

// Sketchware Pro Block Categories (Screenshot 100859 + uploaded palette.json ESPORTS)
enum class BlockCategory(val label: String, val color: Color) {
    VARIABLE("Variable", Color(0xFFEE7D16)),
    LIST("List", Color(0xFFCC5B22)),
    CONTROL("Control", Color(0xFFE1A91A)),
    OPERATOR("Operator", Color(0xFF5CB712)),
    MATH("Math", Color(0xFF00897B)),
    FILE("File", Color(0xFFA1887F)),
    VIEW("View", Color(0xFF4A6CD4)),
    COMPONENT("Component", Color(0xFF2CA5E2)),
    XML_STRINGS("XML Strings", Color(0xFF3F51B5)),
    MOREBLOCK("Moreblock", Color(0xFF8E2EC2)),
    JAVA_SOURCE("Java Code", Color(0xFF475569)),
    COLLECTION("My Block Collection", Color(0xFFE57373)),
}

object SketchwareMenus {
    val staticMenus: Map<String, List<String>> = mapOf(
        "til_box_mode" to listOf("NONE", "OUTLINE", "FILLED"),
        "fabsize" to listOf("AUTO", "MINI", "NORMAL"),
        "fabvisible" to listOf("hide", "show"),
        "visibility" to listOf("VISIBLE", "INVISIBLE", "GONE"),
        "menuitem" to listOf("menuitem1", "menuitem2", "menuitem3", "menuitem4", "menuitem5"),
        "submenu" to listOf("submenu1", "submenu2", "submenu3", "submenu4", "submenu5"),
        "menuaction" to listOf("SHOW_AS_ACTION_ALWAYS", "SHOW_AS_ACTION_COLLAPSE_ACTION_VIEW", "SHOW_AS_ACTION_IF_ROOM", "SHOW_AS_ACTION_NEVER", "SHOW_AS_ACTION_WITH_TEXT"),
        "inputstream" to listOf("inputstream1", "inputstream2", "inputstream3", "inputstream4", "inputstream5"),
        "porterduff" to listOf("MULTIPLY", "SCREEN", "ADD", "SRC_IN", "SRC_ATOP", "SRC_OVER"),
        "transcriptmode" to listOf("TRANSCRIPT_MODE_NORMAL", "TRANSCRIPT_MODE_DISABLED", "TRANSCRIPT_MODE_ALWAYS_SCROLL"),
        "listscrollparam" to listOf("SCROLL_STATE_IDLE", "SCROLL_STATE_FLING", "SCROLL_STATE_TOUCH_SCROLL"),
        "recyclerscrollparam" to listOf("SCROLL_STATE_IDLE", "SCROLL_STATE_SETTLING", "SCROLL_STATE_DRAGGING"),
        "pagerscrollparam" to listOf("SCROLL_STATE_IDLE", "SCROLL_STATE_SETTLING", "SCROLL_STATE_DRAGGING"),
        "gridstretchmode" to listOf("STRETCH_COLUMN_WIDTH", "STRETCH_SPACING_UNIFORM", "STRETCH_SPACING"),
        "gravity_v" to listOf("TOP", "BOTTOM", "CENTER_VERTICAL"),
        "gravity_h" to listOf("LEFT", "RIGHT", "CENTER_HORIZONTAL"),
        "gravity_t" to listOf("TOP", "CENTER", "BOTTOM"),
        "patternviewmode" to listOf("CORRECT", "WRONG", "AUTO_DRAW"),
        "ime_action" to listOf("IME_ACTION_NONE", "IME_ACTION_GO", "IME_ACTION_SEARCH", "IME_ACTION_SEND", "IME_ACTION_NEXT", "IME_ACTION_DONE", "IME_ACTION_PREVIOUS", "IME_ACTION_UNSPECIFIED"),
        "styleprogress" to listOf("STYLE_HORIZONTAL", "STYLE_SPINNER"),
        "typeview" to listOf("View", "ViewGroup", "LinearLayout", "RelativeLayout", "ScrollView", "HorizontalScrollView", "TextView", "EditText", "Button", "RadioButton", "CheckBox", "Switch", "ImageView", "SeekBar", "ListView", "Spinner", "WebView", "MapView", "ProgressBar", "TextInputLayout", "MaterialButton", "VideoView", "androidMaster"),
        "cv_theme" to listOf("AGATE", "ANDROIDSTUDIO", "ARDUINO_LIGHT", "ATOM_ONE_DARK", "ATOM_ONE_LIGHT", "DARCULA", "DARK", "DRACULA", "GITHUB", "GRUVBOX_DARK", "MONOKAI", "MONOKAI_SUBLIME", "OBSIDIAN", "SOLARIZED_DARK", "SOLARIZED_LIGHT", "VS2015", "XCODE", "ZENBURN"),
        "cv_language" to listOf("AUTO", "JAVA", "KOTLIN", "XML", "JSON", "HTML", "CSS", "JAVASCRIPT", "PYTHON", "PHP", "CPP", "C_SHARP", "SQL", "BASH", "YAML"),
        "import" to listOf("android.content.pm.*", "android.database.sqlite.*", "android.provider.*", "android.view.inputmethod.*", "java.io.*", "java.util.zip.*", "androidx.documentfile.provider.DocumentFile", "android.provider.DocumentsContract"),
        "color" to listOf("#2196F3", "#1976D2", "#4CAF50", "#F44336", "#FF9800", "#9C27B0", "#000000", "#FFFFFF", "#1E293B", "#00897B"),
        "mathOp" to listOf("+", "-", "*", "/", "%"),
        "method" to listOf("GET", "POST", "PUT", "DELETE"),
    )
}

enum class BlockType(
    val category: BlockCategory,
    val spec: String,
    val spec2: String = "",
    val paramKeys: List<String> = emptyList(),
    val isContainer: Boolean = false,
    val hasElseBranch: Boolean = false,
    val blockColor: Color = category.color,
    val defaultParams: Map<String, String> = emptyMap(),
) {
    // Variable (Screenshot 100859 exact specs)
    SET_VAR_BOOLEAN(
        BlockCategory.VARIABLE,
        "set Boolean: %m.varBool to %b",
        paramKeys = listOf("varName", "value"),
        defaultParams = mapOf("varName" to "flag", "value" to "true"),
    ),
    SET_VAR_NUMBER(
        BlockCategory.VARIABLE,
        "set Number: %m.varInt to %d",
        paramKeys = listOf("varName", "value"),
        defaultParams = mapOf("varName" to "count", "value" to "0"),
    ),
    INCREASE_VAR(
        BlockCategory.VARIABLE,
        "Number: %m.varInt increase %d",
        paramKeys = listOf("varName", "amount"),
        defaultParams = mapOf("varName" to "count", "amount" to "1"),
    ),
    DECREASE_VAR(
        BlockCategory.VARIABLE,
        "Number: %m.varInt decrease %d",
        paramKeys = listOf("varName", "amount"),
        defaultParams = mapOf("varName" to "count", "amount" to "1"),
    ),
    SET_VAR_STRING(
        BlockCategory.VARIABLE,
        "set String: %m.varStr to %s",
        paramKeys = listOf("varName", "value"),
        defaultParams = mapOf("varName" to "text", "value" to ""),
    ),
    MAP_CREATE_NEW(
        BlockCategory.VARIABLE,
        "Map: %m.varMap create new map",
        paramKeys = listOf("mapName"),
        defaultParams = mapOf("mapName" to "map1"),
    ),
    MAP_PUT_KEY(
        BlockCategory.VARIABLE,
        "Map: %m.varMap put key %s value %s",
        paramKeys = listOf("mapName", "key", "value"),
        defaultParams = mapOf("mapName" to "map1", "key" to "", "value" to ""),
    ),
    MAP_CLEAR(
        BlockCategory.VARIABLE,
        "Map: %m.varMap clear",
        paramKeys = listOf("mapName"),
        defaultParams = mapOf("mapName" to "map1"),
    ),

    // List
    LIST_ADD(
        BlockCategory.LIST,
        "add %s to %m.varList",
        paramKeys = listOf("value", "listName"),
        defaultParams = mapOf("listName" to "list1", "value" to ""),
    ),
    LIST_INSERT(
        BlockCategory.LIST,
        "insert %s at %d to %m.varList",
        paramKeys = listOf("value", "index", "listName"),
        defaultParams = mapOf("listName" to "list1", "index" to "0", "value" to ""),
    ),
    LIST_DELETE_AT(
        BlockCategory.LIST,
        "delete at %d of %m.varList",
        paramKeys = listOf("index", "listName"),
        defaultParams = mapOf("listName" to "list1", "index" to "0"),
    ),
    LIST_CLEAR(
        BlockCategory.LIST,
        "clear %m.varList",
        paramKeys = listOf("listName"),
        defaultParams = mapOf("listName" to "list1"),
    ),
    LIST_SORT(
        BlockCategory.LIST,
        "sort %m.varList",
        paramKeys = listOf("listName"),
        defaultParams = mapOf("listName" to "list1"),
    ),
    LIST_REVERSE(
        BlockCategory.LIST,
        "reverse %m.varList",
        paramKeys = listOf("listName"),
        defaultParams = mapOf("listName" to "list1"),
    ),

    // Control
    IF_THEN(
        BlockCategory.CONTROL,
        "if %b then",
        paramKeys = listOf("condition"),
        isContainer = true,
        defaultParams = mapOf("condition" to "true"),
    ),
    IF_ELSE(
        BlockCategory.CONTROL,
        "if %b then",
        spec2 = "else",
        paramKeys = listOf("condition"),
        isContainer = true,
        hasElseBranch = true,
        defaultParams = mapOf("condition" to "true"),
    ),
    REPEAT(
        BlockCategory.CONTROL,
        "repeat %d",
        paramKeys = listOf("times"),
        isContainer = true,
        defaultParams = mapOf("times" to "5"),
    ),
    REPEAT_WHILE(
        BlockCategory.CONTROL,
        "repeat while %b",
        paramKeys = listOf("condition"),
        isContainer = true,
        defaultParams = mapOf("condition" to "true"),
    ),
    FOREVER(
        BlockCategory.CONTROL,
        "forever",
        isContainer = true,
    ),
    BREAK(
        BlockCategory.CONTROL,
        "break",
    ),
    TRY_CATCH(
        BlockCategory.CONTROL,
        "Try",
        spec2 = "Catch",
        isContainer = true,
        hasElseBranch = true,
        blockColor = Color(0xFF3F51B5),
    ),

    // Operator
    STR_JOIN(
        BlockCategory.OPERATOR,
        "%m.varStr join %s and %s",
        paramKeys = listOf("varName", "a", "b"),
        defaultParams = mapOf("varName" to "text", "a" to "", "b" to ""),
    ),
    STR_REPLACE(
        BlockCategory.OPERATOR,
        "%m.varStr replace %s all %s with %s",
        paramKeys = listOf("varName", "source", "old", "new"),
        defaultParams = mapOf("varName" to "text", "source" to "text", "old" to "", "new" to ""),
    ),
    STR_UPPER(
        BlockCategory.OPERATOR,
        "%m.varStr toUpperCase %s",
        paramKeys = listOf("varName", "source"),
        defaultParams = mapOf("varName" to "text", "source" to "text"),
    ),
    STR_LOWER(
        BlockCategory.OPERATOR,
        "%m.varStr toLowerCase %s",
        paramKeys = listOf("varName", "source"),
        defaultParams = mapOf("varName" to "text", "source" to "text"),
    ),
    STR_TRIM(
        BlockCategory.OPERATOR,
        "%m.varStr trim %s",
        paramKeys = listOf("varName", "source"),
        defaultParams = mapOf("varName" to "text", "source" to "text"),
    ),
    STR_TO_NUM(
        BlockCategory.OPERATOR,
        "%m.varInt toNumber %s",
        paramKeys = listOf("varName", "source"),
        defaultParams = mapOf("varName" to "count", "source" to "text"),
    ),
    NUM_TO_STR(
        BlockCategory.OPERATOR,
        "%m.varStr toString %d",
        paramKeys = listOf("varName", "source"),
        defaultParams = mapOf("varName" to "text", "source" to "count"),
    ),

    // Math
    MATH_RANDOM(
        BlockCategory.MATH,
        "%m.varInt pick random %d to %d",
        paramKeys = listOf("varName", "min", "max"),
        defaultParams = mapOf("varName" to "count", "min" to "1", "max" to "100"),
    ),
    MATH_CALC(
        BlockCategory.MATH,
        "%m.varInt = %d %m.mathOp %d",
        paramKeys = listOf("varName", "left", "op", "right"),
        defaultParams = mapOf("varName" to "count", "left" to "0", "op" to "+", "right" to "1"),
    ),
    MATH_ROUND(
        BlockCategory.MATH,
        "%m.varInt Math.round %d",
        paramKeys = listOf("varName", "value"),
        defaultParams = mapOf("varName" to "count", "value" to "0"),
    ),
    MATH_SQRT(
        BlockCategory.MATH,
        "%m.varInt Math.sqrt %d",
        paramKeys = listOf("varName", "value"),
        defaultParams = mapOf("varName" to "count", "value" to "16"),
    ),

    // File
    FILE_WRITE(
        BlockCategory.FILE,
        "write string %s to file path %s",
        paramKeys = listOf("data", "path"),
        defaultParams = mapOf("data" to "", "path" to ""),
    ),
    FILE_READ(
        BlockCategory.FILE,
        "%m.varStr read file from path %s",
        paramKeys = listOf("varName", "path"),
        defaultParams = mapOf("varName" to "text", "path" to ""),
    ),
    FILE_DELETE(
        BlockCategory.FILE,
        "delete file %s",
        paramKeys = listOf("path"),
        defaultParams = mapOf("path" to ""),
    ),
    FILE_MKDIR(
        BlockCategory.FILE,
        "make directory %s",
        paramKeys = listOf("path"),
        defaultParams = mapOf("path" to ""),
    ),

    // View
    VIEW_SET_TEXT(
        BlockCategory.VIEW,
        "%m.view setText %s",
        paramKeys = listOf("widgetId", "text"),
        defaultParams = mapOf("widgetId" to "textview1", "text" to ""),
    ),
    VIEW_GET_TEXT(
        BlockCategory.VIEW,
        "%m.varStr = %m.view getText",
        paramKeys = listOf("varName", "widgetId"),
        defaultParams = mapOf("varName" to "text", "widgetId" to "edittext1"),
    ),
    VIEW_SET_TEXT_COLOR(
        BlockCategory.VIEW,
        "%m.view setTextColor %m.color",
        paramKeys = listOf("widgetId", "color"),
        defaultParams = mapOf("widgetId" to "textview1", "color" to "#2196F3"),
    ),
    VIEW_SET_BG_COLOR(
        BlockCategory.VIEW,
        "%m.view setBackgroundColor %m.color",
        paramKeys = listOf("widgetId", "color"),
        defaultParams = mapOf("widgetId" to "linear_h1", "color" to "#1E293B"),
    ),
    VIEW_SET_VISIBLE(
        BlockCategory.VIEW,
        "%m.view setVisible %m.visibility",
        paramKeys = listOf("widgetId", "visibility"),
        defaultParams = mapOf("widgetId" to "textview1", "visibility" to "VISIBLE"),
    ),
    VIEW_SET_ENABLED(
        BlockCategory.VIEW,
        "%m.view setEnabled %b",
        paramKeys = listOf("widgetId", "enabled"),
        defaultParams = mapOf("widgetId" to "button1", "enabled" to "true"),
    ),
    VIEW_SET_PROGRESS(
        BlockCategory.VIEW,
        "%m.view setProgress %d",
        paramKeys = listOf("widgetId", "value"),
        defaultParams = mapOf("widgetId" to "progressbar1", "value" to "0"),
    ),
    VIEW_SET_CHECKED(
        BlockCategory.VIEW,
        "%m.view setChecked %b",
        paramKeys = listOf("widgetId", "checked"),
        defaultParams = mapOf("widgetId" to "checkbox1", "checked" to "true"),
    ),
    VIEW_LOAD_URL(
        BlockCategory.VIEW,
        "%m.view loadUrl %s",
        paramKeys = listOf("widgetId", "url"),
        defaultParams = mapOf("widgetId" to "webview1", "url" to ""),
    ),
    VIEW_LIST_REFRESH(
        BlockCategory.VIEW,
        "%m.view setListCustomViewData %m.varList",
        paramKeys = listOf("widgetId", "listName"),
        defaultParams = mapOf("widgetId" to "listview1", "listName" to "list1"),
    ),

    // Component
    COMP_TOAST(
        BlockCategory.COMPONENT,
        "Toast %s",
        paramKeys = listOf("message"),
        defaultParams = mapOf("message" to ""),
    ),
    COMP_SNACKBAR(
        BlockCategory.COMPONENT,
        "Snackbar %s",
        paramKeys = listOf("message"),
        defaultParams = mapOf("message" to ""),
    ),
    COMP_DIALOG_SHOW(
        BlockCategory.COMPONENT,
        "Dialog %m.dialog title %s message %s",
        paramKeys = listOf("compName", "title", "message"),
        defaultParams = mapOf("compName" to "dialog", "title" to "", "message" to ""),
    ),
    COMP_INTENT_SCREEN(
        BlockCategory.COMPONENT,
        "Intent %m.intent setScreen %m.activity",
        paramKeys = listOf("compName", "screen"),
        defaultParams = mapOf("compName" to "intent", "screen" to "MainActivity"),
    ),
    COMP_START_ACTIVITY(
        BlockCategory.COMPONENT,
        "startActivity %m.intent",
        paramKeys = listOf("compName"),
        defaultParams = mapOf("compName" to "intent"),
    ),
    COMP_FINISH_ACTIVITY(
        BlockCategory.COMPONENT,
        "finishActivity",
    ),
    COMP_SHARED_PREF_SET(
        BlockCategory.COMPONENT,
        "SP %m.sharedpref set key %s value %s",
        paramKeys = listOf("compName", "key", "value"),
        defaultParams = mapOf("compName" to "sp", "key" to "", "value" to ""),
    ),
    COMP_TIMER_AFTER(
        BlockCategory.COMPONENT,
        "Timer %m.timer after %d ms",
        paramKeys = listOf("compName", "ms"),
        isContainer = true,
        defaultParams = mapOf("compName" to "timer", "ms" to "1000"),
    ),
    COMP_VIBRATE(
        BlockCategory.COMPONENT,
        "Vibrator %m.vibrator for %d ms",
        paramKeys = listOf("compName", "ms"),
        defaultParams = mapOf("compName" to "vib", "ms" to "200"),
    ),
    COMP_FIREBASE_PUSH(
        BlockCategory.COMPONENT,
        "FirebaseDB %m.firebase push %m.varMap",
        paramKeys = listOf("compName", "mapName"),
        defaultParams = mapOf("compName" to "fb", "mapName" to "map1"),
    ),
    COMP_NETWORK_REQUEST(
        BlockCategory.COMPONENT,
        "RequestNetwork %m.requestnetwork %m.method URL %s",
        paramKeys = listOf("compName", "method", "url"),
        defaultParams = mapOf("compName" to "net", "method" to "GET", "url" to ""),
    ),
    COMP_TTS_SPEAK(
        BlockCategory.COMPONENT,
        "TTS %m.tts speak %s",
        paramKeys = listOf("compName", "text"),
        defaultParams = mapOf("compName" to "tts", "text" to ""),
    ),
    COMP_NOTIFY(
        BlockCategory.COMPONENT,
        "Notification title %s body %s",
        paramKeys = listOf("title", "body"),
        defaultParams = mapOf("title" to "", "body" to ""),
    ),

    // XML Strings
    XML_SET_STRING(
        BlockCategory.XML_STRINGS,
        "getString R.string %s.inputOnly",
        paramKeys = listOf("key"),
        defaultParams = mapOf("key" to "app_name"),
    ),
    XML_SET_COLOR(
        BlockCategory.XML_STRINGS,
        "getColor R.color %s.inputOnly",
        paramKeys = listOf("key"),
        defaultParams = mapOf("key" to "colorPrimary"),
    ),

    // Moreblock Call
    CALL_MOREBLOCK(
        BlockCategory.MOREBLOCK,
        "call %m.moreblock",
        paramKeys = listOf("blockName"),
        defaultParams = mapOf("blockName" to "myHelper"),
    ),

    // Java Direct Block (Sketchware add source directly + Mix Mode!)
    ADD_SOURCE_DIRECTLY(
        BlockCategory.JAVA_SOURCE,
        "add source directly %s.inputOnly",
        paramKeys = listOf("java"),
        defaultParams = mapOf("java" to ""),
    ),

    // My Block Collection
    COLLECTION_SNIPPET(
        BlockCategory.COLLECTION,
        "collection snippet %s",
        paramKeys = listOf("label"),
        defaultParams = mapOf("label" to "Custom Ripple Effect", "java" to "// Saved snippet"),
    );

    companion object {
        fun fromRaw(raw: String?): BlockType =
            entries.firstOrNull { it.name == raw } ?: COMP_TOAST
    }
}

data class BlockNode(
    val id: String = UUID.randomUUID().toString().take(8),
    val type: BlockType,
    val params: Map<String, String> = type.defaultParams,
    val children: List<BlockNode> = emptyList(),
    val elseChildren: List<BlockNode> = emptyList(),
) {
    fun toMap(): Map<String, Any> = mapOf(
        "id" to id,
        "type" to type.name,
        "params" to params,
        "children" to children.map { it.toMap() },
        "elseChildren" to elseChildren.map { it.toMap() },
    )

    companion object {
        fun fromMap(map: Map<*, *>): BlockNode {
            val type = BlockType.fromRaw(map["type"] as? String)
            val rawParams = (map["params"] as? Map<*, *>)?.entries
                ?.mapNotNull { (k, v) ->
                    val key = k as? String ?: return@mapNotNull null
                    key to (v?.toString() ?: "")
                }?.toMap() ?: type.defaultParams
            val rawChildren = (map["children"] as? List<*>)
                ?.mapNotNull { it as? Map<*, *> }
                ?.map { fromMap(it) }
                ?: emptyList()
            val rawElse = (map["elseChildren"] as? List<*>)
                ?.mapNotNull { it as? Map<*, *> }
                ?.map { fromMap(it) }
                ?: emptyList()
            return BlockNode(
                id = (map["id"] as? String)?.ifBlank { null } ?: UUID.randomUUID().toString().take(8),
                type = type,
                params = type.defaultParams + rawParams,
                children = rawChildren,
                elseChildren = rawElse,
            )
        }
    }
}

data class MoreblockDef(
    val id: String = UUID.randomUUID().toString().take(8),
    val name: String,
    val returnType: String = "Void",
    val parameters: List<String> = emptyList(),
    val blocks: List<BlockNode> = emptyList(),
) {
    fun toMap(): Map<String, Any> = mapOf(
        "id" to id,
        "name" to name,
        "returnType" to returnType,
        "parameters" to parameters,
        "blocks" to blocks.map { it.toMap() },
    )

    companion object {
        fun fromMap(map: Map<*, *>): MoreblockDef {
            val params = (map["parameters"] as? List<*>)?.mapNotNull { it as? String }.orEmpty()
            val blks = (map["blocks"] as? List<*>)?.mapNotNull { it as? Map<*, *> }?.map { BlockNode.fromMap(it) }.orEmpty()
            return MoreblockDef(
                id = (map["id"] as? String)?.ifBlank { null } ?: UUID.randomUUID().toString().take(8),
                name = (map["name"] as? String)?.ifBlank { null } ?: "customBlock",
                returnType = (map["returnType"] as? String) ?: "Void",
                parameters = params,
                blocks = blks,
            )
        }
    }
}

data class CustomViewDef(
    val id: String = UUID.randomUUID().toString().take(8),
    val name: String,
    val screenType: String = "Custom View", // Activity, Fragment, DialogFragment, BottomSheetDialogFragment, Custom View
    val orientation: String = "Portrait",
    val keyboard: String = "Unspecified",
    val hasStatusBar: Boolean = true,
    val hasToolbar: Boolean = true,
    val hasDrawer: Boolean = false,
    val hasFab: Boolean = false,
    val widgets: List<WidgetNode> = emptyList(),
) {
    fun activityClassName(): String {
        val parts = name.trim().split(Regex("[^a-zA-Z0-9]+")).filter { it.isNotBlank() }
        val base = parts.joinToString("") { p ->
            p.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        }.ifBlank { "Screen" }
        return if (base.endsWith("Activity", ignoreCase = true)) base else "${base}Activity"
    }

    fun toMap(): Map<String, Any> = mapOf(
        "id" to id,
        "name" to name,
        "screenType" to screenType,
        "orientation" to orientation,
        "keyboard" to keyboard,
        "hasStatusBar" to hasStatusBar,
        "hasToolbar" to hasToolbar,
        "hasDrawer" to hasDrawer,
        "hasFab" to hasFab,
        "widgets" to widgets.map { it.toMap() },
    )

    companion object {
        fun fromMap(map: Map<*, *>): CustomViewDef {
            val rawWidgets = (map["widgets"] as? List<*>)
                ?.mapNotNull { it as? Map<*, *> }
                ?.map { WidgetNode.fromMap(it) }
                ?: emptyList()
            return CustomViewDef(
                id = (map["id"] as? String)?.ifBlank { null } ?: UUID.randomUUID().toString().take(8),
                name = (map["name"] as? String)?.ifBlank { null } ?: "custom_view",
                screenType = (map["screenType"] as? String) ?: "Custom View",
                orientation = (map["orientation"] as? String) ?: "Portrait",
                keyboard = (map["keyboard"] as? String) ?: "Unspecified",
                hasStatusBar = (map["hasStatusBar"] as? Boolean) ?: true,
                hasToolbar = (map["hasToolbar"] as? Boolean) ?: true,
                hasDrawer = (map["hasDrawer"] as? Boolean) ?: false,
                hasFab = (map["hasFab"] as? Boolean) ?: false,
                widgets = rawWidgets,
            )
        }
    }
}

data class ThemeColors(
    val colorAccent: String = "#5B84B1",
    val colorPrimary: String = "#5B84B1",
    val colorPrimaryDark: String = "#4B729E",
    val colorControlHighlight: String = "#DCE8F7",
    val colorControlNormal: String = "#5B84B1",
) {
    fun toMap(): Map<String, Any> = mapOf(
        "colorAccent" to colorAccent,
        "colorPrimary" to colorPrimary,
        "colorPrimaryDark" to colorPrimaryDark,
        "colorControlHighlight" to colorControlHighlight,
        "colorControlNormal" to colorControlNormal,
    )

    companion object {
        fun fromMap(map: Map<*, *>?): ThemeColors {
            if (map == null) return ThemeColors()
            return ThemeColors(
                colorAccent = (map["colorAccent"] as? String) ?: "#5B84B1",
                colorPrimary = (map["colorPrimary"] as? String) ?: "#5B84B1",
                colorPrimaryDark = (map["colorPrimaryDark"] as? String) ?: "#4B729E",
                colorControlHighlight = (map["colorControlHighlight"] as? String) ?: "#DCE8F7",
                colorControlNormal = (map["colorControlNormal"] as? String) ?: "#5B84B1",
            )
        }

        fun random(): ThemeColors {
            val palettes = listOf(
                ThemeColors("#6200EE", "#6200EE", "#3700B3", "#E9D8FD", "#6200EE"),
                ThemeColors("#1976D2", "#2196F3", "#1565C0", "#BBDEFB", "#1976D2"),
                ThemeColors("#2E7D32", "#4CAF50", "#1B5E20", "#C8E6C9", "#2E7D32"),
                ThemeColors("#C62828", "#E53935", "#B71C1C", "#FFCDD2", "#C62828"),
                ThemeColors("#00897B", "#26A69A", "#00695C", "#B2DFDB", "#00897B"),
            )
            return palettes[Random.nextInt(palettes.size)]
        }
    }
}

data class ThemePreset(
    val name: String,
    val swatches: List<String>,
    val colors: ThemeColors,
)

object ThemePresets {
    val all = listOf(
        ThemePreset("Material Purple", listOf("#00E5FF", "#6200EE", "#3700B3", "#FFFFFF", "#D1D5DB"), ThemeColors("#00E5FF", "#6200EE", "#3700B3", "#E9D8FD", "#6200EE")),
        ThemePreset("Material Blue", listOf("#00E5FF", "#1976D2", "#0D47A1", "#E3F2FD", "#D1D5DB"), ThemeColors("#00E5FF", "#1976D2", "#0D47A1", "#BBDEFB", "#1976D2")),
        ThemePreset("Material Green", listOf("#00E5FF", "#43A047", "#1B5E20", "#E8F5E9", "#D1D5DB"), ThemeColors("#00E5FF", "#43A047", "#1B5E20", "#C8E6C9", "#43A047")),
        ThemePreset("Material Red", listOf("#00E5FF", "#E53935", "#B71C1C", "#FFEBEE", "#D1D5DB"), ThemeColors("#00E5FF", "#E53935", "#B71C1C", "#FFCDD2", "#E53935")),
    )
}

data class LibraryDep(
    val id: String,
    val name: String,
    val coordinate: String,
    val sourceType: String = "BUILTIN",
    val localPath: String = "",
    val enabled: Boolean = true,
) {
    fun toMap(): Map<String, Any> = mapOf(
        "id" to id,
        "name" to name,
        "coordinate" to coordinate,
        "sourceType" to sourceType,
        "localPath" to localPath,
        "enabled" to enabled,
    )

    companion object {
        fun fromMap(map: Map<*, *>): LibraryDep = LibraryDep(
            id = (map["id"] as? String) ?: UUID.randomUUID().toString().take(8),
            name = (map["name"] as? String) ?: "Library",
            coordinate = (map["coordinate"] as? String) ?: "",
            sourceType = (map["sourceType"] as? String) ?: "BUILTIN",
            localPath = (map["localPath"] as? String) ?: "",
            enabled = (map["enabled"] as? Boolean) ?: true,
        )

        val defaultBuiltins = listOf(
            LibraryDep("appcompat", "AppCompat v1.7", "androidx.appcompat:appcompat:1.7.0", "BUILTIN", enabled = true),
            LibraryDep("material", "Material Components", "com.google.android.material:material:1.12.0", "BUILTIN", enabled = true),
            LibraryDep("cardview", "AndroidX CardView", "androidx.cardview:cardview:1.0.0", "BUILTIN", enabled = true),
            LibraryDep("gson", "Google Gson", "com.google.code.gson:gson:2.11.0", "BUILTIN", enabled = false),
            LibraryDep("glide", "Glide Image Loader", "com.github.bumptech.glide:glide:4.16.0", "BUILTIN", enabled = false),
            LibraryDep("okhttp", "OkHttp Networking", "com.squareup.okhttp3:okhttp:4.12.0", "BUILTIN", enabled = false),
            LibraryDep("firebase_db", "Firebase Realtime DB", "com.google.firebase:firebase-database", "BUILTIN", enabled = false),
        )
    }
}

data class FirebaseJsonConfig(
    val projectId: String = "",
    val packageName: String = "",
    val appId: String = "",
    val apiKey: String = "",
    val fileName: String = "google-services.json",
    val rawJson: String = "",
) {
    fun toMap(): Map<String, Any> = mapOf(
        "projectId" to projectId,
        "packageName" to packageName,
        "appId" to appId,
        "apiKey" to apiKey,
        "fileName" to fileName,
        "rawJson" to rawJson,
    )

    companion object {
        fun fromMap(map: Map<*, *>?): FirebaseJsonConfig? {
            if (map == null) return null
            val projectId = (map["projectId"] as? String).orEmpty()
            val raw = (map["rawJson"] as? String).orEmpty()
            if (projectId.isBlank() && raw.isBlank()) return null
            return FirebaseJsonConfig(
                projectId = projectId,
                packageName = (map["packageName"] as? String).orEmpty(),
                appId = (map["appId"] as? String).orEmpty(),
                apiKey = (map["apiKey"] as? String).orEmpty(),
                fileName = (map["fileName"] as? String).orEmpty().ifBlank { "google-services.json" },
                rawJson = raw,
            )
        }
    }
}

data class EventSlot(
    val key: String,
    val label: String,
    val signature: String,
    val category: String, // Activity, View, Component, Drawer, Moreblock
)

data class MaxProject(
    val id: String = "",
    val numericId: Int = 820,
    val appName: String = "My App",
    val projectName: String = "NewProject",
    val packageName: String = "com.my.newproject",
    val versionCode: Int = 1,
    val versionName: String = "1.0",
    val logoUri: String = "",
    val logoCornerRadiusDp: Int = 20,
    val backendMode: BackendMode = BackendMode.MIX_MODE,
    val themeColors: ThemeColors = ThemeColors(),
    val widgets: List<WidgetNode> = emptyList(),
    val customViews: List<CustomViewDef> = emptyList(),
    val variables: List<VariableDef> = listOf(
        VariableDef("count", VarType.INT, "0"),
        VariableDef("text", VarType.STRING, ""),
    ),
    val components: List<ComponentDef> = emptyList(),
    val events: Map<String, List<BlockNode>> = mapOf("onCreate" to emptyList()),
    val moreblocks: List<MoreblockDef> = emptyList(),
    val javaCode: String = "",
    val xmlOverride: String = "",
    val images: List<String> = emptyList(),
    val permissions: List<String> = listOf("android.permission.INTERNET"),
    val libraries: List<LibraryDep> = LibraryDep.defaultBuiltins,
    val firebaseConfig: FirebaseJsonConfig? = null,
    val sourceFiles: List<ProjectSourceFile> = emptyList(),
    val updatedAtMs: Long = System.currentTimeMillis(),
) {
    fun availableEvents(): List<EventSlot> {
        val list = mutableListOf(
            EventSlot("onCreate", "onCreate", "On activity create", "Activity"),
            EventSlot("onStart", "onStart", "On activity start", "Activity"),
            EventSlot("onBackPressed", "onBackPressed", "On back pressed", "Activity"),
        )
        widgets.forEach { w ->
            when {
                w.type == WidgetType.BUTTON || w.type == WidgetType.MATERIAL_BUTTON ||
                    w.type == WidgetType.TEXT_VIEW || w.type == WidgetType.CARD_VIEW ||
                    w.type == WidgetType.IMAGE_VIEW || w.type == WidgetType.FAB -> {
                    list.add(EventSlot("${w.id}_onClick", "${w.id} onClick", "When ${w.id} clicked", "View"))
                }
                w.type == WidgetType.EDIT_TEXT || w.type == WidgetType.AUTO_COMPLETE -> {
                    list.add(EventSlot("${w.id}_onTextChanged", "${w.id} onTextChanged", "When ${w.id} text changes", "View"))
                }
                w.type == WidgetType.CHECK_BOX || w.type == WidgetType.SWITCH || w.type == WidgetType.RADIO_BUTTON -> {
                    list.add(EventSlot("${w.id}_onCheckedChanged", "${w.id} onCheckedChange", "When ${w.id} toggled", "View"))
                }
                w.type == WidgetType.SEEK_BAR -> {
                    list.add(EventSlot("${w.id}_onProgressChanged", "${w.id} onProgressChanged", "When ${w.id} moved", "View"))
                }
                w.type == WidgetType.LIST_VIEW || w.type == WidgetType.SPINNER -> {
                    list.add(EventSlot("${w.id}_onItemClick", "${w.id} onItemClicked", "When ${w.id} item clicked", "View"))
                }
            }
        }
        customViews.forEach { cv ->
            val cls = cv.activityClassName()
            list.add(EventSlot("screen_${cv.name}_onCreate", "${cv.name}.xml : onCreate", "On $cls create", "Activity"))
            list.add(EventSlot("screen_${cv.name}_onStart", "${cv.name}.xml : onStart", "On $cls start", "Activity"))
            cv.widgets.forEach { w ->
                when {
                    w.type == WidgetType.BUTTON || w.type == WidgetType.MATERIAL_BUTTON ||
                        w.type == WidgetType.TEXT_VIEW || w.type == WidgetType.CARD_VIEW ||
                        w.type == WidgetType.IMAGE_VIEW || w.type == WidgetType.FAB -> {
                        list.add(EventSlot("screen_${cv.name}_${w.id}_onClick", "${cv.name}/${w.id} onClick", "When ${cv.name}.${w.id} clicked", "View"))
                    }
                    w.type == WidgetType.EDIT_TEXT || w.type == WidgetType.AUTO_COMPLETE -> {
                        list.add(EventSlot("screen_${cv.name}_${w.id}_onTextChanged", "${cv.name}/${w.id} onTextChanged", "When ${cv.name}.${w.id} text changes", "View"))
                    }
                    w.type == WidgetType.CHECK_BOX || w.type == WidgetType.SWITCH || w.type == WidgetType.RADIO_BUTTON -> {
                        list.add(EventSlot("screen_${cv.name}_${w.id}_onCheckedChanged", "${cv.name}/${w.id} onCheckedChange", "When ${cv.name}.${w.id} toggled", "View"))
                    }
                    w.type == WidgetType.SEEK_BAR -> {
                        list.add(EventSlot("screen_${cv.name}_${w.id}_onProgressChanged", "${cv.name}/${w.id} onProgressChanged", "When ${cv.name}.${w.id} moved", "View"))
                    }
                    w.type == WidgetType.LIST_VIEW || w.type == WidgetType.SPINNER -> {
                        list.add(EventSlot("screen_${cv.name}_${w.id}_onItemClick", "${cv.name}/${w.id} onItemClicked", "When ${cv.name}.${w.id} item clicked", "View"))
                    }
                }
            }
        }
        components.forEach { comp ->
            list.add(EventSlot("comp_${comp.name}_onResult", "${comp.name} : ${comp.type.label}", "Component event", "Component"))
        }
        moreblocks.forEach { mb ->
            list.add(EventSlot("mb_${mb.name}", mb.name, "Moreblock (${mb.returnType})", "Moreblock"))
        }
        return list
    }
}

object StarterTemplates {
    fun blankProject(
        appName: String = "My App",
        projectName: String = "NewProject",
        packageName: String = "com.my.newproject",
        backendMode: BackendMode = BackendMode.MIX_MODE,
        themeColors: ThemeColors = ThemeColors(),
        versionCode: Int = 1,
        versionName: String = "1.0",
        logoUri: String = "",
        logoCornerRadiusDp: Int = 20,
    ): MaxProject {
        val proj = MaxProject(
            appName = appName,
            projectName = projectName,
            packageName = packageName,
            versionCode = versionCode,
            versionName = versionName,
            logoUri = logoUri,
            logoCornerRadiusDp = logoCornerRadiusDp,
            backendMode = backendMode,
            themeColors = themeColors,
            widgets = emptyList(),
            customViews = emptyList(),
            events = mapOf("onCreate" to emptyList()),
        )
        return proj.copy(javaCode = com.example.codegen.CodeGenerator.generateJavaFromProject(proj))
    }

    fun counterPro(): MaxProject {
        val theme = ThemePresets.all[1].colors
        val widgets = listOf(
            WidgetNode(id = "textview_count", type = WidgetType.TEXT_VIEW, text = "Count: 0", textSize = 26, textColor = "#111827", gravity = "CENTER"),
            WidgetNode(id = "progress1", type = WidgetType.PROGRESS_BAR, progress = 0),
            WidgetNode(id = "btn_plus", type = WidgetType.BUTTON, text = "+1 Increment", textColor = "#FFFFFF", backgroundColor = "#1976D2", gravity = "CENTER"),
            WidgetNode(id = "btn_reset", type = WidgetType.BUTTON, text = "Reset", textColor = "#FFFFFF", backgroundColor = "#374151", gravity = "CENTER"),
        )
        val proj = MaxProject(
            numericId = 826,
            appName = "MAX COUNTER",
            projectName = "COUNTER_PRO",
            packageName = "com.maxstudio.counterpro",
            backendMode = BackendMode.MIX_MODE,
            themeColors = theme,
            widgets = widgets,
            customViews = listOf(CustomViewDef(name = "history_item", widgets = listOf(WidgetNode(id = "txt_step", type = WidgetType.TEXT_VIEW, text = "Tap #1")))),
            events = mapOf(
                "onCreate" to listOf(
                    BlockNode(type = BlockType.COMP_TOAST, params = mapOf("message" to "Counter Ready!")),
                ),
                "btn_plus_onClick" to listOf(
                    BlockNode(type = BlockType.INCREASE_VAR, params = mapOf("varName" to "count", "amount" to "1")),
                    BlockNode(type = BlockType.VIEW_SET_TEXT, params = mapOf("widgetId" to "textview_count", "text" to "Count: {count}")),
                    BlockNode(type = BlockType.VIEW_SET_PROGRESS, params = mapOf("widgetId" to "progress1", "value" to "{count}")),
                ),
                "btn_reset_onClick" to listOf(
                    BlockNode(type = BlockType.SET_VAR_NUMBER, params = mapOf("varName" to "count", "value" to "0")),
                    BlockNode(type = BlockType.VIEW_SET_TEXT, params = mapOf("widgetId" to "textview_count", "text" to "Count: 0")),
                    BlockNode(type = BlockType.VIEW_SET_PROGRESS, params = mapOf("widgetId" to "progress1", "value" to "0")),
                ),
            ),
        )
        return proj.copy(javaCode = com.example.codegen.CodeGenerator.generateJavaFromProject(proj))
    }
}

data class ProjectSourceFile(
    val path: String,
    val content: String,
    val language: String = "JAVA",
) {
    fun toMap(): Map<String, Any> = mapOf(
        "path" to path,
        "content" to content,
        "language" to language,
    )

    companion object {
        fun fromMap(map: Map<*, *>): ProjectSourceFile = ProjectSourceFile(
            path = (map["path"] as? String).orEmpty(),
            content = (map["content"] as? String).orEmpty(),
            language = (map["language"] as? String) ?: "JAVA",
        )
    }
}

