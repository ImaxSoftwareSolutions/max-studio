package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.apk.ApkPackager
import com.example.model.StarterTemplates
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string and package from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("MAX STUDIO", appName)
    assertEquals("com.maxstudio.app", context.packageName)
  }

  @Test
  fun `apk packager builds valid binary apk`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val project = StarterTemplates.counterPro().copy(logoCornerRadiusDp = 26)
    val outApk = File("/tmp/maxstudio_verify_test.apk")
    if (outApk.exists()) outApk.delete()
    ApkPackager.buildSignedApk(
      context = context,
      project = project.copy(
        customViews = project.customViews + com.example.model.CustomViewDef(
          name = "home",
          screenType = "Activity",
          widgets = listOf(com.example.model.WidgetNode(id = "textview1", type = com.example.model.WidgetType.TEXT_VIEW, text = "Home")),
        ),
        events = project.events + (
          "onCreate" to listOf(
            com.example.model.BlockNode(
              type = com.example.model.BlockType.COMP_TIMER_AFTER,
              params = mapOf("compName" to "timer", "ms" to "1500"),
              children = listOf(
                com.example.model.BlockNode(
                  type = com.example.model.BlockType.COMP_INTENT_SCREEN,
                  params = mapOf("compName" to "intent", "screen" to "HomeActivity"),
                ),
                com.example.model.BlockNode(
                  type = com.example.model.BlockType.COMP_START_ACTIVITY,
                  params = mapOf("compName" to "intent"),
                ),
              ),
            )
          )
        ),
      ),
      outputApkFile = outApk,
      certStorageDir = context.filesDir,
    )
    assertTrue(outApk.exists())
    assertTrue(outApk.length() > 4096)
    java.util.zip.ZipFile(outApk).use { zf ->
      val specText = zf.getInputStream(zf.getEntry("assets/max_app.json")).bufferedReader().readText()
      assertTrue(specText.contains("COMP_TIMER_AFTER"))
      assertTrue(specText.contains("\"children\""))
      assertTrue(specText.contains("HomeActivity"))
      assertTrue(specText.contains("\"screens\""))
      val manifestBytes = zf.getInputStream(zf.getEntry("AndroidManifest.xml")).readBytes()
      val manifestUtf16 = String(manifestBytes, Charsets.UTF_16LE)
      assertTrue(manifestUtf16.contains("com.maxstudio.generated.ScreenActivity"))
    }
  }
}
