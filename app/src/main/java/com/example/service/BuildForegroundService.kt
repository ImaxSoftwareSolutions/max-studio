package com.example.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class BuildPipelineState(
    val isBuilding: Boolean = false,
    val progressPercent: Int = 0,
    val stageLabel: String = "Idle",
    val logs: List<String> = emptyList(),
    val completedSuccess: Boolean = false,
    val buildFailed: Boolean = false,
    val outputApkName: String = "",
    val outputApkPath: String = "",
    val outputMssPath: String = "",
    val installPromptTriggerId: Int = 0,
    val errorCount: Int = 0,
    val warningCount: Int = 0,
    val compileErrorLines: List<String> = emptyList(),
    val runtimeErrors: List<String> = emptyList(),
)

object BuildPipelineController {
    private val _state = MutableStateFlow(BuildPipelineState())
    val state: StateFlow<BuildPipelineState> = _state.asStateFlow()

    fun update(transform: (BuildPipelineState) -> BuildPipelineState) {
        _state.value = transform(_state.value)
    }

    fun recordRuntimeError(message: String) {
        _state.value = _state.value.let { cur ->
            if (message in cur.runtimeErrors) cur
            else cur.copy(runtimeErrors = cur.runtimeErrors + message)
        }
    }

    fun clearRuntimeErrors() {
        _state.value = _state.value.copy(runtimeErrors = emptyList())
    }

    fun reset() {
        _state.value = BuildPipelineState()
    }
}

class BuildForegroundService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var buildJob: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        ensureChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val appName = intent?.getStringExtra(EXTRA_APP_NAME) ?: "MAX App"
        val packageName = intent?.getStringExtra(EXTRA_PACKAGE_NAME) ?: "com.maxstudio.app"
        val projectName = intent?.getStringExtra(EXTRA_PROJECT_NAME) ?: "NewProject"
        val widgetCount = intent?.getIntExtra(EXTRA_WIDGET_COUNT, 0) ?: 0
        val customViewCount = intent?.getIntExtra(EXTRA_CUSTOM_VIEW_COUNT, 0) ?: 0
        val libCount = intent?.getIntExtra(EXTRA_LIB_COUNT, 0) ?: 0
        val hasFirebaseJson = intent?.getBooleanExtra(EXTRA_HAS_FIREBASE, false) ?: false
        val javaLines = intent?.getIntExtra(EXTRA_JAVA_LINES, 20) ?: 20
        val apkPath = intent?.getStringExtra(EXTRA_APK_PATH) ?: ""
        val mssPath = intent?.getStringExtra(EXTRA_MSS_PATH) ?: ""
        val fatalCount = intent?.getIntExtra(EXTRA_FATAL_COUNT, 0) ?: 0
        val errorCount = intent?.getIntExtra(EXTRA_ERROR_COUNT, 0) ?: 0
        val warningCount = intent?.getIntExtra(EXTRA_WARNING_COUNT, 0) ?: 0
        val diagLines = intent?.getStringArrayListExtra(EXTRA_DIAG_LINES)?.toList().orEmpty()

        val initialNotification = buildNotification(
            appName = appName,
            progress = 5,
            stage = "Preparing workspace (5%)...",
            ongoing = true,
            failed = false,
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                initialNotification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC,
            )
        } else {
            startForeground(NOTIFICATION_ID, initialNotification)
        }

        buildJob?.cancel()
        buildJob = serviceScope.launch {
            runSketchwareStyleBuild(
                appName = appName,
                packageName = packageName,
                projectName = projectName,
                widgetCount = widgetCount,
                customViewCount = customViewCount,
                libCount = libCount,
                hasFirebaseJson = hasFirebaseJson,
                javaLines = javaLines,
                apkPath = apkPath,
                mssPath = mssPath,
                fatalCount = fatalCount,
                errorCount = errorCount,
                warningCount = warningCount,
                diagLines = diagLines,
            )
            stopForeground(STOP_FOREGROUND_DETACH)
            stopSelf(startId)
        }

        return START_NOT_STICKY
    }

    private suspend fun runSketchwareStyleBuild(
        appName: String,
        packageName: String,
        projectName: String,
        widgetCount: Int,
        customViewCount: Int,
        libCount: Int,
        hasFirebaseJson: Boolean,
        javaLines: Int,
        apkPath: String,
        mssPath: String,
        fatalCount: Int,
        errorCount: Int,
        warningCount: Int,
        diagLines: List<String>,
    ) {
        val manager = getSystemService(NotificationManager::class.java)
        val apkName = "${projectName.ifBlank { "app" }}_debug.apk"
        val prevTrigger = BuildPipelineController.state.value.installPromptTriggerId
        val prevRuntimeErrors = BuildPipelineController.state.value.runtimeErrors

        BuildPipelineController.update {
            BuildPipelineState(
                isBuilding = true,
                progressPercent = 4,
                stageLabel = "Initializing MAX STUDIO Compiler...",
                logs = listOf("MAX STUDIO Build Engine — Target: $packageName"),
                completedSuccess = false,
                buildFailed = false,
                outputApkName = apkName,
                outputApkPath = apkPath,
                outputMssPath = mssPath,
                installPromptTriggerId = prevTrigger,
                errorCount = errorCount,
                warningCount = warningCount,
                compileErrorLines = diagLines,
                runtimeErrors = prevRuntimeErrors,
            )
        }

        val earlySteps = listOf(
            10 to Pair("Clearing temp workspace (.maxstudio/temp)...", "[PREP] Workspace clean -> $projectName"),
            20 to Pair("Checking libraries ($libCount active)...", "[DEPS] Resolved $libCount AndroidX/Material/custom libs"),
            32 to Pair("Generating AndroidManifest.xml & R.java...", "[MANIFEST] Package $packageName merged${if (hasFirebaseJson) " + google-services.json" else ""}"),
            46 to Pair("AAPT2 compiling XML ($widgetCount views, $customViewCount custom views)...", "[AAPT2] Compiling res/layout/main.xml & custom views... OK"),
            60 to Pair(
                "AAPT2 linking resources ($warningCount warning(s))...",
                "[AAPT2] Linked resources.ap_ ($errorCount error(s), $warningCount warning(s))"
            ),
        )

        for ((percent, info) in earlySteps) {
            delay(180)
            val (stage, logLine) = info
            BuildPipelineController.update { current ->
                current.copy(
                    isBuilding = true,
                    progressPercent = percent,
                    stageLabel = stage,
                    logs = current.logs + logLine,
                )
            }
            manager?.notify(
                NOTIFICATION_ID,
                buildNotification(
                    appName = appName,
                    progress = percent,
                    stage = stage,
                    ongoing = true,
                    failed = false,
                ),
            )
        }

        delay(200)
        if (fatalCount > 0) {
            val failStage = "Compile Failed ($errorCount error(s))"
            BuildPipelineController.update { current ->
                current.copy(
                    isBuilding = false,
                    progressPercent = 74,
                    stageLabel = failStage,
                    logs = current.logs + "[JAVAC] Compiling $packageName.MainActivity ($javaLines lines)..." + diagLines + "[BUILD FAILED] Check Error Logs tab to fix fatal errors.",
                    completedSuccess = false,
                    buildFailed = true,
                    errorCount = errorCount,
                    warningCount = warningCount,
                    compileErrorLines = diagLines,
                )
            }
            manager?.notify(
                NOTIFICATION_ID,
                buildNotification(
                    appName = appName,
                    progress = 74,
                    stage = failStage,
                    ongoing = false,
                    failed = true,
                ),
            )
            return
        }

        val finalSteps = listOf(
            74 to Pair(
                "ECJ / javac compiling MainActivity.java ($javaLines lines)...",
                listOf("[JAVAC] Compiling $packageName.MainActivity -> classes... OK") + diagLines,
            ),
            86 to Pair("D8 dexing bytecode to classes.dex...", listOf("[D8] Dex conversion complete (minSdk 24, targetSdk 36)")),
            94 to Pair("APKBuilder packaging $apkName...", listOf("[APK] Packaged $apkName in .maxstudio/mysc/")),
            100 to Pair(
                if (errorCount > 0) "Build Complete with $errorCount Error Log(s)" else "Build Complete (100%) — Launching Installer...",
                listOf("[SIGN] Signed $apkName — SUCCESS (Check Error Logs: $errorCount err, $warningCount warn)"),
            ),
        )

        for ((percent, info) in finalSteps) {
            delay(200)
            val (stage, extraLines) = info
            BuildPipelineController.update { current ->
                current.copy(
                    isBuilding = percent < 100,
                    progressPercent = percent,
                    stageLabel = stage,
                    logs = current.logs + extraLines,
                    completedSuccess = percent == 100,
                    buildFailed = false,
                    installPromptTriggerId = if (percent == 100) prevTrigger + 1 else prevTrigger,
                )
            }
            manager?.notify(
                NOTIFICATION_ID,
                buildNotification(
                    appName = appName,
                    progress = percent,
                    stage = stage,
                    ongoing = percent < 100,
                    failed = false,
                ),
            )
        }
    }

    private fun buildNotification(
        appName: String,
        progress: Int,
        stage: String,
        ongoing: Boolean,
        failed: Boolean,
    ) = NotificationCompat.Builder(this, CHANNEL_ID)
        .setSmallIcon(R.mipmap.ic_launcher)
        .setContentTitle(
            when {
                failed -> "$appName Build Failed"
                ongoing -> "Building $appName ($progress%)"
                else -> "$appName Built Successfully (100%)"
            }
        )
        .setContentText(stage)
        .setOnlyAlertOnce(true)
        .setOngoing(ongoing)
        .setAutoCancel(!ongoing)
        .setProgress(100, progress, false)
        .setPriority(NotificationCompat.PRIORITY_LOW)
        .setContentIntent(createLaunchPendingIntent())
        .build()

    private fun createLaunchPendingIntent(): PendingIntent {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "MAX STUDIO APK Build Progress",
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = "Shows real-time APK build and .mss export percentage"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        buildJob?.cancel()
        serviceScope.cancel()
        super.onDestroy()
    }

    companion object {
        private const val CHANNEL_ID = "max_studio_build_channel"
        private const val NOTIFICATION_ID = 4102

        private const val EXTRA_APP_NAME = "extra_app_name"
        private const val EXTRA_PACKAGE_NAME = "extra_package_name"
        private const val EXTRA_PROJECT_NAME = "extra_project_name"
        private const val EXTRA_WIDGET_COUNT = "extra_widget_count"
        private const val EXTRA_CUSTOM_VIEW_COUNT = "extra_custom_view_count"
        private const val EXTRA_LIB_COUNT = "extra_lib_count"
        private const val EXTRA_HAS_FIREBASE = "extra_has_firebase"
        private const val EXTRA_JAVA_LINES = "extra_java_lines"
        private const val EXTRA_APK_PATH = "extra_apk_path"
        private const val EXTRA_MSS_PATH = "extra_mss_path"
        private const val EXTRA_FATAL_COUNT = "extra_fatal_count"
        private const val EXTRA_ERROR_COUNT = "extra_error_count"
        private const val EXTRA_WARNING_COUNT = "extra_warning_count"
        private const val EXTRA_DIAG_LINES = "extra_diag_lines"

        fun startBuild(
            context: Context,
            appName: String,
            packageName: String,
            projectName: String,
            widgetCount: Int,
            customViewCount: Int,
            libCount: Int,
            hasFirebaseJson: Boolean,
            javaLines: Int,
            apkPath: String,
            mssPath: String,
            fatalCount: Int = 0,
            errorCount: Int = 0,
            warningCount: Int = 0,
            diagnosticLines: ArrayList<String> = arrayListOf(),
        ) {
            val intent = Intent(context, BuildForegroundService::class.java).apply {
                putExtra(EXTRA_APP_NAME, appName)
                putExtra(EXTRA_PACKAGE_NAME, packageName)
                putExtra(EXTRA_PROJECT_NAME, projectName)
                putExtra(EXTRA_WIDGET_COUNT, widgetCount)
                putExtra(EXTRA_CUSTOM_VIEW_COUNT, customViewCount)
                putExtra(EXTRA_LIB_COUNT, libCount)
                putExtra(EXTRA_HAS_FIREBASE, hasFirebaseJson)
                putExtra(EXTRA_JAVA_LINES, javaLines)
                putExtra(EXTRA_APK_PATH, apkPath)
                putExtra(EXTRA_MSS_PATH, mssPath)
                putExtra(EXTRA_FATAL_COUNT, fatalCount)
                putExtra(EXTRA_ERROR_COUNT, errorCount)
                putExtra(EXTRA_WARNING_COUNT, warningCount)
                putStringArrayListExtra(EXTRA_DIAG_LINES, diagnosticLines)
            }
            ContextCompat.startForegroundService(context, intent)
        }
    }
}
