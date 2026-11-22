package com.example.apk

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.drawable.BitmapDrawable
import androidx.core.content.ContextCompat
import com.example.R
import com.example.model.MaxProject
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.math.BigInteger
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.security.KeyFactory
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.MessageDigest
import java.security.PrivateKey
import java.security.PublicKey
import java.security.Signature
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import java.util.zip.CRC32
import java.util.zip.Deflater

/**
 * Builds an installable, APK Signature Scheme v2 signed Android .apk archive
 * containing binary AndroidManifest.xml, 4-byte aligned resources.arsc,
 * rounded launcher icon PNG, runtime_classes.dex, and assets/max_app.json.
 */
object ApkPackager {

    private const val LAUNCHER_ICON_RES_ID = 0x7F010000
    private const val ANDROID_THEME_LIGHT_NO_ACTION_BAR = 0x01030241
    private const val MAIN_ACTIVITY_CLASS = "com.maxstudio.generated.MainActivity"
    private const val SCREEN_ACTIVITY_CLASS = "com.maxstudio.generated.ScreenActivity"
    private const val SIG_ALG_RSA_PKCS1_SHA256 = 0x0103
    private const val APK_SIG_BLOCK_V2_ID = 0x7109871a
    private val APK_SIG_MAGIC = "APK Sig Block 42".toByteArray(Charsets.US_ASCII)

    fun buildSignedApk(
        context: Context?,
        project: MaxProject,
        outputApkFile: File,
        certStorageDir: File? = context?.filesDir,
    ): File {
        outputApkFile.parentFile?.mkdirs()

        val safePkg = safeAndroidPackage(project.packageName)
        val manifestBytes = buildBinaryManifest(project, safePkg)
        val resourcesBytes = buildResourcesArsc(safePkg)
        val iconPngBytes = buildRoundedIconPng(context, project)
        val dexBytes = loadRuntimeDexBytes(context)
        val appSpecBytes = buildAppSpecJson(project).toByteArray(Charsets.UTF_8)

        val xmlLayoutStr = project.xmlOverride.ifBlank { com.example.codegen.CodeGenerator.generateXmlLayout(project.widgets) }
        val entries = listOf(
            ApkZipEntry("AndroidManifest.xml", manifestBytes, compress = true),
            ApkZipEntry("resources.arsc", resourcesBytes, compress = false, align4 = true),
            ApkZipEntry("res/drawable/ic_launcher.png", iconPngBytes, compress = false, align4 = true),
            ApkZipEntry("classes.dex", dexBytes, compress = true),
            ApkZipEntry("assets/max_app.json", appSpecBytes, compress = true),
            ApkZipEntry("res/layout/main.xml", xmlLayoutStr.toByteArray(Charsets.UTF_8), compress = true),
            ApkZipEntry("src/main/java/com/maxstudio/generated/MainActivity.java", project.javaCode.toByteArray(Charsets.UTF_8), compress = true),
        )

        val identity = loadOrCreateDebugIdentity(certStorageDir)
        val signedApkBytes = assembleAndSignV2Apk(entries, identity)
        outputApkFile.writeBytes(signedApkBytes)
        return outputApkFile
    }

    private fun loadRuntimeDexBytes(context: Context?): ByteArray {
        if (context != null) {
            runCatching {
                context.assets.open("runtime_classes.dex").use { return it.readBytes() }
            }
        }
        val assetFile = File("app/src/main/assets/runtime_classes.dex")
        if (assetFile.exists()) return assetFile.readBytes()
        val relAssetFile = File("src/main/assets/runtime_classes.dex")
        if (relAssetFile.exists()) return relAssetFile.readBytes()
        throw IllegalStateException("runtime_classes.dex missing from assets")
    }

    private fun blockToJson(b: com.example.model.BlockNode): JSONObject = JSONObject().apply {
        put("type", b.type.name)
        put("params", JSONObject().apply {
            b.params.forEach { (pk, pv) -> put(pk, pv) }
        })
        if (b.children.isNotEmpty()) {
            val childArr = JSONArray()
            b.children.forEach { childArr.put(blockToJson(it)) }
            put("children", childArr)
        }
        if (b.elseChildren.isNotEmpty()) {
            val elseArr = JSONArray()
            b.elseChildren.forEach { elseArr.put(blockToJson(it)) }
            put("elseChildren", elseArr)
        }
    }

    private fun widgetToJson(w: com.example.model.WidgetNode): JSONObject = JSONObject().apply {
        put("id", w.id)
        put("type", w.type.name)
        put("text", w.text)
        put("hint", w.hint)
        put("textSize", w.textSize)
        put("textColor", w.textColor)
        put("backgroundColor", w.backgroundColor)
        put("cornerRadius", w.cornerRadius)
        put("paddingDp", w.paddingDp)
        put("gravity", w.gravity)
        put("progress", w.progress)
        put("checked", w.checked)
    }

    private fun buildAppSpecJson(project: MaxProject): String {
        val varsObj = JSONObject()
        project.variables.forEach { varsObj.put(it.name, it.initialValue) }

        val eventsObj = JSONObject()
        project.events.forEach { (key, list) ->
            val arr = JSONArray()
            list.forEach { b -> arr.put(blockToJson(b)) }
            eventsObj.put(key, arr)
        }

        val widgetsArr = JSONArray()
        project.widgets.forEach { w -> widgetsArr.put(widgetToJson(w)) }

        val screensArr = JSONArray()
        project.customViews.forEach { cv ->
            val cvWidgets = JSONArray()
            cv.widgets.forEach { w -> cvWidgets.put(widgetToJson(w)) }
            screensArr.put(JSONObject().apply {
                put("id", cv.id)
                put("name", cv.name)
                put("screenType", cv.screenType)
                put("hasToolbar", cv.hasToolbar)
                put("widgets", cvWidgets)
            })
        }

        val moreblocksObj = JSONObject()
        project.moreblocks.forEach { mb ->
            val mbArr = JSONArray()
            mb.blocks.forEach { b -> mbArr.put(blockToJson(b)) }
            moreblocksObj.put(mb.name, mbArr)
        }

        return JSONObject().apply {
            put("appName", project.appName.ifBlank { "MAX STUDIO App" })
            put("packageName", project.packageName)
            put("colorPrimary", project.themeColors.colorPrimary)
            put("colorPrimaryDark", project.themeColors.colorPrimaryDark)
            put("colorAccent", project.themeColors.colorAccent)
            put("variables", varsObj)
            put("events", eventsObj)
            put("widgets", widgetsArr)
            put("screens", screensArr)
            put("moreblocks", moreblocksObj)
            put("javaCode", project.javaCode)
            put("xmlOverride", project.xmlOverride)
        }.toString()
    }

    private fun buildRoundedIconPng(context: Context?, project: MaxProject): ByteArray {
        val size = 192
        val rawBitmap: Bitmap? = runCatching {
            if (project.logoUri.isNotBlank()) {
                val f = File(project.logoUri)
                if (f.exists()) BitmapFactory.decodeFile(f.absolutePath) else null
            } else null
        }.getOrNull() ?: runCatching {
            if (context != null) {
                val d = ContextCompat.getDrawable(context, R.drawable.launcher_max_icon)
                when (d) {
                    is BitmapDrawable -> d.bitmap
                    null -> null
                    else -> {
                        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
                        val c = Canvas(bmp)
                        d.setBounds(0, 0, size, size)
                        d.draw(c)
                        bmp
                    }
                }
            } else null
        }.getOrNull()

        val source = if (rawBitmap != null) {
            Bitmap.createScaledBitmap(rawBitmap, size, size, true)
        } else {
            // Pure fallback bitmap for unit tests
            Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888).apply {
                Canvas(this).drawColor(Color.parseColor("#1976D2"))
            }
        }

        val out = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        val radiusPx = (project.logoCornerRadiusDp.coerceIn(0, 44) / 44f) * (size / 2f)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val rect = Rect(0, 0, size, size)
        val rectF = RectF(0f, 0f, size.toFloat(), size.toFloat())
        canvas.drawRoundRect(rectF, radiusPx, radiusPx, paint)
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        canvas.drawBitmap(source, rect, rect, paint)

        val baos = ByteArrayOutputStream()
        out.compress(Bitmap.CompressFormat.PNG, 100, baos)
        return baos.toByteArray()
    }

    // -------------------------------------------------------------------------
    // Binary AXML Writer for AndroidManifest.xml
    // -------------------------------------------------------------------------

    private data class AxmlAttr(
        val nsUri: String?,
        val name: String,
        val type: Int,
        val dataInt: Int = 0,
        val dataStr: String? = null,
    )

    private fun safeAndroidPackage(raw: String): String {
        val segments = raw.trim().lowercase()
            .split('.')
            .map { seg ->
                val cleaned = seg.replace(Regex("[^a-z0-9_]"), "")
                when {
                    cleaned.isEmpty() -> "app"
                    cleaned.first().isDigit() -> "a$cleaned"
                    else -> cleaned
                }
            }
            .filter { it.isNotBlank() }
        return when {
            segments.isEmpty() -> "com.maxstudio.app.generated"
            segments.size == 1 -> "com.maxstudio.${segments[0]}"
            else -> segments.joinToString(".")
        }
    }

    private fun buildBinaryManifest(project: MaxProject, pkg: String): ByteArray {
        val appLabel = project.appName.trim().ifBlank { "MAX STUDIO App" }
        val verCode = project.versionCode.coerceAtLeast(1)
        val verName = project.versionName.trim().ifBlank { "1.0" }
        val perms = project.permissions.distinct().filter { it.startsWith("android.permission.") }

        val androidNs = "http://schemas.android.com/apk/res/android"
        val resAttrEntries = listOf(
            "theme" to 0x01010000,
            "label" to 0x01010001,
            "icon" to 0x01010002,
            "name" to 0x01010003,
            "exported" to 0x01010010,
            "minSdkVersion" to 0x0101020c,
            "versionCode" to 0x0101021b,
            "versionName" to 0x0101021c,
            "targetSdkVersion" to 0x01010270,
        )

        val strings = mutableListOf<String>()
        resAttrEntries.forEach { strings.add(it.first) }
        fun strIdx(s: String): Int {
            val existing = strings.indexOf(s)
            if (existing >= 0) return existing
            strings.add(s)
            return strings.lastIndex
        }

        // Register all strings up front before encoding chunks
        val nsIdx = strIdx(androidNs)
        val prefixIdx = strIdx("android")
        val elemNames = listOf("manifest", "uses-sdk", "uses-permission", "application", "activity", "intent-filter", "action", "category")
        elemNames.forEach { strIdx(it) }
        strIdx("package")
        strIdx(pkg)
        strIdx(verName)
        strIdx(appLabel)
        strIdx(MAIN_ACTIVITY_CLASS)
        strIdx(SCREEN_ACTIVITY_CLASS)
        strIdx("android.intent.action.MAIN")
        strIdx("android.intent.category.LAUNCHER")
        perms.forEach { strIdx(it) }

        val xmlChunks = ByteArrayOutputStream()

        fun writeStartNs() {
            val b = ByteBuffer.allocate(24).order(ByteOrder.LITTLE_ENDIAN)
            b.putShort(0x0100) // RES_XML_START_NAMESPACE_TYPE
            b.putShort(16)
            b.putInt(24)
            b.putInt(1) // lineNumber
            b.putInt(-1) // comment
            b.putInt(prefixIdx)
            b.putInt(nsIdx)
            xmlChunks.write(b.array())
        }

        fun writeEndNs() {
            val b = ByteBuffer.allocate(24).order(ByteOrder.LITTLE_ENDIAN)
            b.putShort(0x0101) // RES_XML_END_NAMESPACE_TYPE
            b.putShort(16)
            b.putInt(24)
            b.putInt(1)
            b.putInt(-1)
            b.putInt(prefixIdx)
            b.putInt(nsIdx)
            xmlChunks.write(b.array())
        }

        fun writeStartElem(tag: String, attrs: List<AxmlAttr>, line: Int) {
            val chunkSize = 16 + 20 + attrs.size * 20
            val b = ByteBuffer.allocate(chunkSize).order(ByteOrder.LITTLE_ENDIAN)
            b.putShort(0x0102) // RES_XML_START_ELEMENT_TYPE
            b.putShort(16)
            b.putInt(chunkSize)
            b.putInt(line)
            b.putInt(-1)
            b.putInt(-1) // ns
            b.putInt(strIdx(tag))
            b.putShort(0x0014) // attributeStart
            b.putShort(0x0014) // attributeSize
            b.putShort(attrs.size.toShort())
            b.putShort(0) // idIndex
            b.putShort(0) // classIndex
            b.putShort(0) // styleIndex
            attrs.forEach { attr ->
                b.putInt(if (attr.nsUri != null) strIdx(attr.nsUri) else -1)
                b.putInt(strIdx(attr.name))
                val valStrIdx = if (attr.dataStr != null) strIdx(attr.dataStr) else -1
                b.putInt(valStrIdx)
                b.putShort(8) // Res_value size
                b.put(0) // res0
                b.put(attr.type.toByte())
                b.putInt(if (attr.type == 0x03) valStrIdx else attr.dataInt)
            }
            xmlChunks.write(b.array())
        }

        fun writeEndElem(tag: String, line: Int) {
            val b = ByteBuffer.allocate(24).order(ByteOrder.LITTLE_ENDIAN)
            b.putShort(0x0103) // RES_XML_END_ELEMENT_TYPE
            b.putShort(16)
            b.putInt(24)
            b.putInt(line)
            b.putInt(-1)
            b.putInt(-1)
            b.putInt(strIdx(tag))
            xmlChunks.write(b.array())
        }

        var line = 1
        writeStartNs()
        writeStartElem(
            "manifest",
            listOf(
                AxmlAttr(androidNs, "versionCode", 0x10, dataInt = verCode),
                AxmlAttr(androidNs, "versionName", 0x03, dataStr = verName),
                AxmlAttr(null, "package", 0x03, dataStr = pkg),
            ),
            line++,
        )
        writeStartElem(
            "uses-sdk",
            listOf(
                AxmlAttr(androidNs, "minSdkVersion", 0x10, dataInt = 24),
                AxmlAttr(androidNs, "targetSdkVersion", 0x10, dataInt = 34),
            ),
            line++,
        )
        writeEndElem("uses-sdk", line++)

        perms.forEach { p ->
            writeStartElem("uses-permission", listOf(AxmlAttr(androidNs, "name", 0x03, dataStr = p)), line++)
            writeEndElem("uses-permission", line++)
        }

        writeStartElem(
            "application",
            listOf(
                AxmlAttr(androidNs, "theme", 0x01, dataInt = ANDROID_THEME_LIGHT_NO_ACTION_BAR),
                AxmlAttr(androidNs, "label", 0x03, dataStr = appLabel),
                AxmlAttr(androidNs, "icon", 0x01, dataInt = LAUNCHER_ICON_RES_ID),
            ),
            line++,
        )
        writeStartElem(
            "activity",
            listOf(
                AxmlAttr(androidNs, "label", 0x03, dataStr = appLabel),
                AxmlAttr(androidNs, "name", 0x03, dataStr = MAIN_ACTIVITY_CLASS),
                AxmlAttr(androidNs, "exported", 0x12, dataInt = -1),
            ),
            line++,
        )
        writeStartElem("intent-filter", emptyList(), line++)
        writeStartElem("action", listOf(AxmlAttr(androidNs, "name", 0x03, dataStr = "android.intent.action.MAIN")), line++)
        writeEndElem("action", line++)
        writeStartElem("category", listOf(AxmlAttr(androidNs, "name", 0x03, dataStr = "android.intent.category.LAUNCHER")), line++)
        writeEndElem("category", line++)
        writeEndElem("intent-filter", line++)
        writeEndElem("activity", line++)
        writeStartElem(
            "activity",
            listOf(
                AxmlAttr(androidNs, "name", 0x03, dataStr = SCREEN_ACTIVITY_CLASS),
                AxmlAttr(androidNs, "exported", 0x12, dataInt = 0),
            ),
            line++,
        )
        writeEndElem("activity", line++)
        writeEndElem("application", line++)
        writeEndElem("manifest", line)
        writeEndNs()

        val poolChunk = buildStringPoolChunk(strings, utf8 = false)
        val resMapSize = 8 + resAttrEntries.size * 4
        val resMapBuf = ByteBuffer.allocate(resMapSize).order(ByteOrder.LITTLE_ENDIAN)
        resMapBuf.putShort(0x0180) // RES_XML_RESOURCE_MAP_TYPE
        resMapBuf.putShort(8)
        resMapBuf.putInt(resMapSize)
        resAttrEntries.forEach { resMapBuf.putInt(it.second) }

        val xmlBody = xmlChunks.toByteArray()
        val totalSize = 8 + poolChunk.size + resMapSize + xmlBody.size
        val out = ByteBuffer.allocate(totalSize).order(ByteOrder.LITTLE_ENDIAN)
        out.putShort(0x0003) // RES_XML_TYPE
        out.putShort(8)
        out.putInt(totalSize)
        out.put(poolChunk)
        out.put(resMapBuf.array())
        out.put(xmlBody)
        return out.array()
    }

    private fun buildStringPoolChunk(strings: List<String>, utf8: Boolean): ByteArray {
        val offsets = IntArray(strings.size)
        val strData = ByteArrayOutputStream()
        strings.forEachIndexed { idx, s ->
            offsets[idx] = strData.size()
            if (utf8) {
                val utfBytes = s.toByteArray(Charsets.UTF_8)
                writeLen8(strData, s.length)
                writeLen8(strData, utfBytes.size)
                strData.write(utfBytes)
                strData.write(0)
            } else {
                val chars = s.toCharArray()
                writeLen16(strData, chars.size)
                chars.forEach { ch ->
                    strData.write(ch.code and 0xFF)
                    strData.write((ch.code ushr 8) and 0xFF)
                }
                strData.write(0)
                strData.write(0)
            }
        }
        while (strData.size() % 4 != 0) strData.write(0)
        val rawStrBytes = strData.toByteArray()
        val headerSize = 28
        val stringsStart = headerSize + strings.size * 4
        val totalSize = stringsStart + rawStrBytes.size
        val buf = ByteBuffer.allocate(totalSize).order(ByteOrder.LITTLE_ENDIAN)
        buf.putShort(0x0001) // RES_STRING_POOL_TYPE
        buf.putShort(headerSize.toShort())
        buf.putInt(totalSize)
        buf.putInt(strings.size)
        buf.putInt(0) // styleCount
        buf.putInt(if (utf8) 0x100 else 0) // flags
        buf.putInt(stringsStart)
        buf.putInt(0) // stylesStart
        offsets.forEach { buf.putInt(it) }
        buf.put(rawStrBytes)
        return buf.array()
    }

    private fun writeLen8(out: ByteArrayOutputStream, len: Int) {
        if (len >= 0x80) {
            out.write(((len shr 8) and 0x7F) or 0x80)
            out.write(len and 0xFF)
        } else {
            out.write(len and 0x7F)
        }
    }

    private fun writeLen16(out: ByteArrayOutputStream, len: Int) {
        if (len >= 0x8000) {
            val high = ((len shr 16) and 0x7FFF) or 0x8000
            val low = len and 0xFFFF
            out.write(high and 0xFF)
            out.write((high ushr 8) and 0xFF)
            out.write(low and 0xFF)
            out.write((low ushr 8) and 0xFF)
        } else {
            out.write(len and 0xFF)
            out.write((len ushr 8) and 0xFF)
        }
    }

    // -------------------------------------------------------------------------
    // Minimal Binary resources.arsc Writer (defines @drawable/ic_launcher = 0x7F010000)
    // -------------------------------------------------------------------------

    private fun buildResourcesArsc(packageName: String): ByteArray {
        val globalPool = buildStringPoolChunk(listOf("res/drawable/ic_launcher.png"), utf8 = true)
        val typePool = buildStringPoolChunk(listOf("drawable"), utf8 = true)
        val keyPool = buildStringPoolChunk(listOf("ic_launcher"), utf8 = true)

        // ResTable_typeSpec (type = 0x0202, header = 16B, 1 entry flag = 4B -> 20B)
        val typeSpecBuf = ByteBuffer.allocate(20).order(ByteOrder.LITTLE_ENDIAN)
        typeSpecBuf.putShort(0x0202)
        typeSpecBuf.putShort(16)
        typeSpecBuf.putInt(20)
        typeSpecBuf.put(1) // type id = 1 ("drawable")
        typeSpecBuf.put(0) // res0
        typeSpecBuf.putShort(0) // res1
        typeSpecBuf.putInt(1) // entryCount = 1
        typeSpecBuf.putInt(0) // config flags for entry 0

        // ResTable_type (type = 0x0201)
        // headerSize = 20 (fixed) + 64 (ResTable_config) = 84
        val configSize = 64
        val typeHeaderSize = 20 + configSize
        val entryAndValueSize = 16 // ResTable_entry(8B) + Res_value(8B)
        val entriesStart = typeHeaderSize + 4 // + 4B offset array for 1 entry
        val typeTotalSize = entriesStart + entryAndValueSize
        val typeBuf = ByteBuffer.allocate(typeTotalSize).order(ByteOrder.LITTLE_ENDIAN)
        typeBuf.putShort(0x0201)
        typeBuf.putShort(typeHeaderSize.toShort())
        typeBuf.putInt(typeTotalSize)
        typeBuf.put(1) // type id = 1
        typeBuf.put(0) // flags
        typeBuf.putShort(0) // reserved
        typeBuf.putInt(1) // entryCount = 1
        typeBuf.putInt(entriesStart)
        // ResTable_config (64 bytes, first int = 64)
        typeBuf.putInt(configSize)
        typeBuf.put(ByteArray(configSize - 4))
        // entry offset[0] = 0
        typeBuf.putInt(0)
        // ResTable_entry (8B): size=8, flags=0, key.index=0
        typeBuf.putShort(8)
        typeBuf.putShort(0)
        typeBuf.putInt(0)
        // Res_value (8B): size=8, res0=0, dataType=TYPE_STRING(3), data=0
        typeBuf.putShort(8)
        typeBuf.put(0)
        typeBuf.put(3)
        typeBuf.putInt(0)

        // ResTable_package header = 288 bytes
        val pkgHeaderSize = 288
        val typeStringsOffset = pkgHeaderSize
        val keyStringsOffset = pkgHeaderSize + typePool.size
        val pkgTotalSize = pkgHeaderSize + typePool.size + keyPool.size + typeSpecBuf.capacity() + typeTotalSize
        val pkgBuf = ByteBuffer.allocate(pkgTotalSize).order(ByteOrder.LITTLE_ENDIAN)
        pkgBuf.putShort(0x0200)
        pkgBuf.putShort(pkgHeaderSize.toShort())
        pkgBuf.putInt(pkgTotalSize)
        pkgBuf.putInt(0x7F) // app package id
        val safePkg = packageName.take(127)
        val pkgChars = ByteArray(256)
        safePkg.forEachIndexed { idx, ch ->
            pkgChars[idx * 2] = (ch.code and 0xFF).toByte()
            pkgChars[idx * 2 + 1] = ((ch.code ushr 8) and 0xFF).toByte()
        }
        pkgBuf.put(pkgChars)
        pkgBuf.putInt(typeStringsOffset)
        pkgBuf.putInt(1) // lastPublicType
        pkgBuf.putInt(keyStringsOffset)
        pkgBuf.putInt(1) // lastPublicKey
        pkgBuf.putInt(0) // typeIdOffset
        pkgBuf.put(typePool)
        pkgBuf.put(keyPool)
        pkgBuf.put(typeSpecBuf.array())
        pkgBuf.put(typeBuf.array())

        val tableTotalSize = 12 + globalPool.size + pkgTotalSize
        val tableBuf = ByteBuffer.allocate(tableTotalSize).order(ByteOrder.LITTLE_ENDIAN)
        tableBuf.putShort(0x0002) // RES_TABLE_TYPE
        tableBuf.putShort(12)
        tableBuf.putInt(tableTotalSize)
        tableBuf.putInt(1) // packageCount
        tableBuf.put(globalPool)
        tableBuf.put(pkgBuf.array())
        return tableBuf.array()
    }

    // -------------------------------------------------------------------------
    // 4-Byte Aligned ZIP Builder + APK Signature Scheme v2 Signer
    // -------------------------------------------------------------------------

    private data class ApkZipEntry(
        val name: String,
        val rawData: ByteArray,
        val compress: Boolean,
        val align4: Boolean = false,
    )

    private data class WrittenZipRecord(
        val nameBytes: ByteArray,
        val method: Int,
        val crc: Long,
        val compressedSize: Int,
        val uncompressedSize: Int,
        val localHeaderOffset: Int,
    )

    private fun assembleAndSignV2Apk(entries: List<ApkZipEntry>, identity: SigningIdentity): ByteArray {
        val entriesOut = ByteArrayOutputStream()
        val records = mutableListOf<WrittenZipRecord>()

        entries.forEach { entry ->
            val nameBytes = entry.name.toByteArray(Charsets.UTF_8)
            val crc32 = CRC32().apply { update(entry.rawData) }.value
            val compressedBytes: ByteArray
            val method: Int
            if (entry.compress) {
                compressedBytes = deflateRaw(entry.rawData)
                method = 8 // DEFLATED
            } else {
                compressedBytes = entry.rawData
                method = 0 // STORED
            }

            val localOffset = entriesOut.size()
            val extraLen = if (entry.align4 && !entry.compress) {
                val mod = (localOffset + 30 + nameBytes.size) % 4
                if (mod == 0) 0 else 4 - mod
            } else 0

            val lfh = ByteBuffer.allocate(30 + nameBytes.size + extraLen).order(ByteOrder.LITTLE_ENDIAN)
            lfh.putInt(0x04034b50) // Local file header signature
            lfh.putShort(20) // version needed
            lfh.putShort(0) // flags
            lfh.putShort(method.toShort())
            lfh.putShort(0) // mod time
            lfh.putShort(0x5421) // mod date
            lfh.putInt(crc32.toInt())
            lfh.putInt(compressedBytes.size)
            lfh.putInt(entry.rawData.size)
            lfh.putShort(nameBytes.size.toShort())
            lfh.putShort(extraLen.toShort())
            lfh.put(nameBytes)
            if (extraLen > 0) lfh.put(ByteArray(extraLen))

            entriesOut.write(lfh.array())
            entriesOut.write(compressedBytes)

            records.add(
                WrittenZipRecord(
                    nameBytes = nameBytes,
                    method = method,
                    crc = crc32,
                    compressedSize = compressedBytes.size,
                    uncompressedSize = entry.rawData.size,
                    localHeaderOffset = localOffset,
                )
            )
        }

        val zipContentsBytes = entriesOut.toByteArray()
        val cdOut = ByteArrayOutputStream()
        records.forEach { r ->
            val cdh = ByteBuffer.allocate(46 + r.nameBytes.size).order(ByteOrder.LITTLE_ENDIAN)
            cdh.putInt(0x02014b50) // Central directory file header signature
            cdh.putShort(20) // version made by
            cdh.putShort(20) // version needed
            cdh.putShort(0) // flags
            cdh.putShort(r.method.toShort())
            cdh.putShort(0)
            cdh.putShort(0x5421)
            cdh.putInt(r.crc.toInt())
            cdh.putInt(r.compressedSize)
            cdh.putInt(r.uncompressedSize)
            cdh.putShort(r.nameBytes.size.toShort())
            cdh.putShort(0) // extra len
            cdh.putShort(0) // comment len
            cdh.putShort(0) // disk start
            cdh.putShort(0) // internal attr
            cdh.putInt(0) // external attr
            cdh.putInt(r.localHeaderOffset)
            cdh.put(r.nameBytes)
            cdOut.write(cdh.array())
        }
        val cdBytes = cdOut.toByteArray()

        // EOCD used for digest computation MUST have CD offset pointing to zipContentsBytes.size
        val eocdForDigest = buildEocd(records.size, cdBytes.size, zipContentsBytes.size)
        val contentDigest = computeApkV2ContentDigest(zipContentsBytes, cdBytes, eocdForDigest)

        val signingBlock = buildApkSigningBlockV2(contentDigest, identity)
        val finalCdOffset = zipContentsBytes.size + signingBlock.size
        val finalEocd = buildEocd(records.size, cdBytes.size, finalCdOffset)

        val finalOut = ByteArrayOutputStream(finalCdOffset + cdBytes.size + finalEocd.size)
        finalOut.write(zipContentsBytes)
        finalOut.write(signingBlock)
        finalOut.write(cdBytes)
        finalOut.write(finalEocd)
        return finalOut.toByteArray()
    }

    private fun buildEocd(entryCount: Int, cdSize: Int, cdOffset: Int): ByteArray {
        val eocd = ByteBuffer.allocate(22).order(ByteOrder.LITTLE_ENDIAN)
        eocd.putInt(0x06054b50)
        eocd.putShort(0) // disk num
        eocd.putShort(0) // start disk
        eocd.putShort(entryCount.toShort())
        eocd.putShort(entryCount.toShort())
        eocd.putInt(cdSize)
        eocd.putInt(cdOffset)
        eocd.putShort(0) // comment len
        return eocd.array()
    }

    private fun deflateRaw(input: ByteArray): ByteArray {
        val deflater = Deflater(Deflater.DEFAULT_COMPRESSION, true)
        deflater.setInput(input)
        deflater.finish()
        val out = ByteArrayOutputStream(input.size)
        val buf = ByteArray(4096)
        while (!deflater.finished()) {
            val n = deflater.deflate(buf)
            if (n > 0) out.write(buf, 0, n)
        }
        deflater.end()
        return out.toByteArray()
    }

    private fun computeApkV2ContentDigest(section1: ByteArray, section2: ByteArray, section3: ByteArray): ByteArray {
        val chunkSize = 1024 * 1024
        val chunkDigests = mutableListOf<ByteArray>()
        listOf(section1, section2, section3).forEach { section ->
            if (section.isEmpty()) {
                val md = MessageDigest.getInstance("SHA-256")
                md.update(0xa5.toByte())
                md.update(int32Le(0))
                chunkDigests.add(md.digest())
            } else {
                var offset = 0
                while (offset < section.size) {
                    val len = minOf(chunkSize, section.size - offset)
                    val md = MessageDigest.getInstance("SHA-256")
                    md.update(0xa5.toByte())
                    md.update(int32Le(len))
                    md.update(section, offset, len)
                    chunkDigests.add(md.digest())
                    offset += len
                }
            }
        }
        val top = MessageDigest.getInstance("SHA-256")
        top.update(0x5a.toByte())
        top.update(int32Le(chunkDigests.size))
        chunkDigests.forEach { top.update(it) }
        return top.digest()
    }

    private fun buildApkSigningBlockV2(contentDigest: ByteArray, identity: SigningIdentity): ByteArray {
        val digestsBlock = lenPrefixed(
            lenPrefixed(
                int32Le(SIG_ALG_RSA_PKCS1_SHA256) + lenPrefixed(contentDigest)
            )
        )
        val certsBlock = lenPrefixed(lenPrefixed(identity.certDer))
        val attrsBlock = int32Le(0)
        val additionalFlagsZero = int32Le(0)
        val signedData = digestsBlock + certsBlock + attrsBlock + additionalFlagsZero

        val sig = Signature.getInstance("SHA256withRSA").apply {
            initSign(identity.privateKey)
            update(signedData)
        }.sign()

        val signaturesBlock = lenPrefixed(
            lenPrefixed(
                int32Le(SIG_ALG_RSA_PKCS1_SHA256) + lenPrefixed(sig)
            )
        )
        val publicKeyBytes = identity.publicKey.encoded

        val signer = lenPrefixed(signedData) + signaturesBlock + lenPrefixed(publicKeyBytes)
        val v2SchemeBlock = lenPrefixed(lenPrefixed(signer))

        val pairBytes = ByteBuffer.allocate(8 + 4 + v2SchemeBlock.size).order(ByteOrder.LITTLE_ENDIAN)
        pairBytes.putLong((4 + v2SchemeBlock.size).toLong())
        pairBytes.putInt(APK_SIG_BLOCK_V2_ID)
        pairBytes.put(v2SchemeBlock)
        val pairArr = pairBytes.array()

        // sizeOfBlock = pairs + 8 (size2) + 16 (magic)
        val blockSizeAfterFirst8 = (pairArr.size + 8 + 16).toLong()
        val total = ByteBuffer.allocate(8 + pairArr.size + 8 + 16).order(ByteOrder.LITTLE_ENDIAN)
        total.putLong(blockSizeAfterFirst8)
        total.put(pairArr)
        total.putLong(blockSizeAfterFirst8)
        total.put(APK_SIG_MAGIC)
        return total.array()
    }

    private fun lenPrefixed(data: ByteArray): ByteArray = int32Le(data.size) + data

    private fun int32Le(v: Int): ByteArray = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(v).array()

    // -------------------------------------------------------------------------
    // Ephemeral / Cached Pure-Kotlin RSA-2048 + X.509 DER Certificate Generator
    // -------------------------------------------------------------------------

    private data class SigningIdentity(
        val privateKey: PrivateKey,
        val publicKey: PublicKey,
        val certDer: ByteArray,
    )

    private fun loadOrCreateDebugIdentity(certDir: File?): SigningIdentity {
        if (certDir != null) {
            val privFile = File(certDir, "maxstudio_debug.pk8")
            val pubFile = File(certDir, "maxstudio_debug.x509.pub")
            val certFile = File(certDir, "maxstudio_debug.der")
            if (privFile.exists() && pubFile.exists() && certFile.exists()) {
                val loaded = runCatching {
                    val kf = KeyFactory.getInstance("RSA")
                    val priv = kf.generatePrivate(PKCS8EncodedKeySpec(privFile.readBytes()))
                    val pub = kf.generatePublic(X509EncodedKeySpec(pubFile.readBytes()))
                    SigningIdentity(priv, pub, certFile.readBytes())
                }.getOrNull()
                if (loaded != null) return loaded
            }
        }

        val kpg = KeyPairGenerator.getInstance("RSA").apply { initialize(2048) }
        val pair: KeyPair = kpg.generateKeyPair()
        val certDer = buildSelfSignedX509Der(pair)

        if (certDir != null) {
            runCatching {
                certDir.mkdirs()
                File(certDir, "maxstudio_debug.pk8").writeBytes(pair.private.encoded)
                File(certDir, "maxstudio_debug.x509.pub").writeBytes(pair.public.encoded)
                File(certDir, "maxstudio_debug.der").writeBytes(certDer)
            }
        }
        return SigningIdentity(pair.private, pair.public, certDer)
    }

    private fun buildSelfSignedX509Der(pair: KeyPair): ByteArray {
        val sha256WithRsaAlgId = derSeq(
            derOid(1, 2, 840, 113549, 1, 1, 11) + byteArrayOf(0x05, 0x00)
        )
        val dn = derSeq(
            derSet(
                derSeq(
                    derOid(2, 5, 4, 3) + derUtf8("MAX STUDIO Debug")
                )
            )
        )
        val validity = derSeq(
            derUtcTime("240101000000Z") + derUtcTime("491231235959Z")
        )
        val version = derTag(0xA0, derInt(BigInteger.valueOf(2))) // v3
        val serial = derInt(BigInteger.valueOf(System.currentTimeMillis().coerceAtLeast(1L)))
        val tbsCert = derSeq(
            version + serial + sha256WithRsaAlgId + dn + validity + dn + pair.public.encoded
        )
        val sig = Signature.getInstance("SHA256withRSA").apply {
            initSign(pair.private)
            update(tbsCert)
        }.sign()
        return derSeq(tbsCert + sha256WithRsaAlgId + derBitString(sig))
    }

    private fun derSeq(content: ByteArray) = derTag(0x30, content)
    private fun derSet(content: ByteArray) = derTag(0x31, content)
    private fun derInt(v: BigInteger) = derTag(0x02, v.toByteArray())
    private fun derUtf8(s: String) = derTag(0x0C, s.toByteArray(Charsets.UTF_8))
    private fun derUtcTime(s: String) = derTag(0x17, s.toByteArray(Charsets.US_ASCII))
    private fun derBitString(content: ByteArray) = derTag(0x03, byteArrayOf(0x00) + content)

    private fun derOid(vararg arcs: Int): ByteArray {
        val out = ByteArrayOutputStream()
        out.write(arcs[0] * 40 + arcs[1])
        for (i in 2 until arcs.size) {
            var v = arcs[i]
            val stack = ArrayList<Int>()
            stack.add(v and 0x7F)
            v = v ushr 7
            while (v > 0) {
                stack.add((v and 0x7F) or 0x80)
                v = v ushr 7
            }
            for (j in stack.lastIndex downTo 0) out.write(stack[j])
        }
        return derTag(0x06, out.toByteArray())
    }

    private fun derTag(tag: Int, content: ByteArray): ByteArray {
        val len = content.size
        val lenBytes = when {
            len < 128 -> byteArrayOf(len.toByte())
            len <= 0xFF -> byteArrayOf(0x81.toByte(), len.toByte())
            len <= 0xFFFF -> byteArrayOf(0x82.toByte(), (len shr 8).toByte(), len.toByte())
            else -> byteArrayOf(0x83.toByte(), (len shr 16).toByte(), (len shr 8).toByte(), len.toByte())
        }
        return byteArrayOf(tag.toByte()) + lenBytes + content
    }
}
