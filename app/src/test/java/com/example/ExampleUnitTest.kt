package com.example

import com.example.codegen.CodeGenerator
import com.example.model.BackendMode
import com.example.model.BlockNode
import com.example.model.BlockType
import com.example.model.MaxProject
import com.example.model.StarterTemplates
import com.example.model.WidgetNode
import com.example.model.WidgetType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun blankProject_startsWithEmptyCanvasAndCleanOnCreate() {
        val proj = StarterTemplates.blankProject(
            appName = "MyCleanApp",
            projectName = "CleanProj",
            packageName = "com.example.clean",
            backendMode = BackendMode.MIX_MODE,
        )
        assertEquals("MyCleanApp", proj.appName)
        assertTrue(proj.widgets.isEmpty())
        assertTrue(proj.customViews.isEmpty())
        assertTrue(proj.events["onCreate"].orEmpty().isEmpty())
    }

    @Test
    fun parseJavaToBlocks_convertsCommonJavaStatements() {
        val snippet = """
            Toast.makeText(getApplicationContext(), "Saved!", Toast.LENGTH_SHORT).show();
            textview1.setText("Hello Max Studio");
            textview1.setTextColor(Color.parseColor("#FF5722"));
            count++;
            System.out.println("raw");
        """.trimIndent()

        val blocks = CodeGenerator.parseJavaToBlocks(snippet)
        assertEquals(5, blocks.size)
        assertEquals(BlockType.COMP_TOAST, blocks[0].type)
        assertEquals("Saved!", blocks[0].params["message"])
        assertEquals(BlockType.VIEW_SET_TEXT, blocks[1].type)
        assertEquals("textview1", blocks[1].params["widgetId"])
        assertEquals(BlockType.VIEW_SET_TEXT_COLOR, blocks[2].type)
        assertEquals("#FF5722", blocks[2].params["color"])
        assertEquals(BlockType.INCREASE_VAR, blocks[3].type)
        assertEquals(BlockType.ADD_SOURCE_DIRECTLY, blocks[4].type)
    }

    @Test
    fun generateJavaAndXml_producesValidStructureForAllBackendModes() {
        assertEquals(3, BackendMode.entries.size)
        val project = MaxProject(
            id = "p1",
            numericId = 801,
            appName = "Test App",
            packageName = "com.test.max",
            backendMode = BackendMode.MIX_MODE,
            widgets = listOf(
                WidgetNode(id = "button1", type = WidgetType.BUTTON, text = "Click", backgroundColor = "#2196F3"),
            ),
            events = mapOf(
                "button1_onClick" to listOf(
                    BlockNode(type = BlockType.COMP_TOAST, params = mapOf("message" to "Hi"))
                )
            ),
        )
        val java = CodeGenerator.generateJavaFromProject(project)
        val xml = CodeGenerator.generateXmlLayout(project.widgets)
        assertTrue(java.contains("package com.test.max;"))
        assertTrue(java.contains("button1.setOnClickListener"))
        assertTrue(java.contains("Toast.makeText"))
        assertTrue(xml.contains("<Button"))
        assertTrue(xml.contains("@+id/button1"))
    }

    @Test
    fun generateJava_emitsTryCatchElseBranch() {
        val project = MaxProject(
            id = "p2",
            numericId = 802,
            appName = "TryCatch Demo",
            packageName = "com.maxstudio.demo",
            backendMode = BackendMode.BLOCKS_ONLY,
            events = mapOf(
                "onCreate" to listOf(
                    BlockNode(
                        type = BlockType.TRY_CATCH,
                        children = listOf(
                            BlockNode(
                                type = BlockType.VIEW_SET_TEXT,
                                params = mapOf("widgetId" to "textview1", "text" to "OK"),
                            ),
                        ),
                        elseChildren = listOf(
                            BlockNode(
                                type = BlockType.COMP_TOAST,
                                params = mapOf("message" to "Recovered"),
                            ),
                        ),
                    ),
                ),
            ),
        )
        val java = CodeGenerator.generateJavaFromProject(project)
        assertTrue(java.contains("textview1.setText(\"OK\")"))
        assertTrue(java.contains("catch (Exception _e)"))
        assertTrue(java.contains("Toast.makeText"))
    }

    @Test
    fun projectDiagnostics_catchesTimerActivityAndSyntaxErrors() {
        val buggy = MaxProject(
            id = "p3",
            numericId = 803,
            appName = "Buggy",
            packageName = "invalidpkg",
            backendMode = BackendMode.MIX_MODE,
            widgets = listOf(WidgetNode(id = "button1", type = WidgetType.BUTTON, text = "Go")),
            events = mapOf(
                "onCreate" to listOf(
                    BlockNode(
                        type = BlockType.COMP_TIMER_AFTER,
                        params = mapOf("compName" to "t1", "ms" to "-5"),
                        children = emptyList(),
                    ),
                    BlockNode(
                        type = BlockType.COMP_INTENT_SCREEN,
                        params = mapOf("compName" to "i1", "screen" to "MissingScreenActivity"),
                    ),
                    BlockNode(
                        type = BlockType.COMP_START_ACTIVITY,
                        params = mapOf("compName" to "unconfigured_intent"),
                    ),
                    BlockNode(
                        type = BlockType.VIEW_SET_TEXT,
                        params = mapOf("widgetId" to "ghost_view", "text" to "Hi"),
                    ),
                )
            ),
            javaCode = "public class MainActivity { void f() { Toast.makeText(\"unclosed); }",
        )

        val report = com.example.diagnostics.ProjectDiagnostics.analyze(buggy)
        assertTrue(report.hasFatalBlocker)
        assertTrue(report.errorCount >= 4)
        assertTrue(report.items.any { it.message.contains("Invalid package name") })
        assertTrue(report.items.any { it.message.contains("MissingScreenActivity") })
        assertTrue(report.items.any { it.message.contains("unconfigured_intent") })
        assertTrue(report.items.any { it.message.contains("Timer 't1'") })
        assertTrue(report.items.any { it.message.contains("ghost_view") })
    }

    @Test
    fun projectDiagnostics_passesValidTimerToCustomActivity() {
        val valid = MaxProject(
            id = "p4",
            numericId = 804,
            appName = "Splash Demo",
            packageName = "com.maxstudio.splashdemo",
            backendMode = BackendMode.BLOCKS_ONLY,
            customViews = listOf(
                com.example.model.CustomViewDef(
                    name = "home",
                    screenType = "Activity",
                    widgets = listOf(WidgetNode(id = "textview1", type = WidgetType.TEXT_VIEW, text = "Welcome")),
                )
            ),
            events = mapOf(
                "onCreate" to listOf(
                    BlockNode(
                        type = BlockType.COMP_TIMER_AFTER,
                        params = mapOf("compName" to "timer", "ms" to "1500"),
                        children = listOf(
                            BlockNode(
                                type = BlockType.COMP_INTENT_SCREEN,
                                params = mapOf("compName" to "intent", "screen" to "HomeActivity"),
                            ),
                            BlockNode(
                                type = BlockType.COMP_START_ACTIVITY,
                                params = mapOf("compName" to "intent"),
                            ),
                        ),
                    )
                )
            ),
        )
        val report = com.example.diagnostics.ProjectDiagnostics.analyze(valid)
        assertEquals(0, report.errorCount)
        assertEquals(0, report.warningCount)
    }
}

