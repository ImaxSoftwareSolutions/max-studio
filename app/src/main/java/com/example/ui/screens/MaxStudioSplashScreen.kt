package com.example.ui.screens

import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val ArmorNavyDeep = Color(0xFF050914)
private val ArmorNavyMid = Color(0xFF0C152B)
private val ArcReactorCyan = Color(0xFF38BDF8)
private val ArmorGold = Color(0xFFF59E0B)
private val ArmorCrimson = Color(0xFFEF4444)

private data class LogoArmorQuadrants(
    val topLeft: ImageBitmap,
    val topRight: ImageBitmap,
    val bottomLeft: ImageBitmap,
    val bottomRight: ImageBitmap,
)

@Composable
fun MaxStudioSplashScreen(
    onFinished: () -> Unit,
) {
    val context = LocalContext.current
    val density = LocalDensity.current

    // Slice R.drawable.launcher_max_icon off the main thread so frame 1 commits immediately
    val quadrants by produceState<LogoArmorQuadrants?>(initialValue = null, context) {
        value = withContext(Dispatchers.Default) {
            runCatching {
                val drawable = ContextCompat.getDrawable(context, R.drawable.launcher_max_icon)
                    ?: return@runCatching null
                val sizePx = 256
                val full = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
                val canvas = AndroidCanvas(full)
                drawable.setBounds(0, 0, sizePx, sizePx)
                drawable.draw(canvas)
                val half = sizePx / 2
                val result = LogoArmorQuadrants(
                    topLeft = Bitmap.createBitmap(full, 0, 0, half, half).asImageBitmap(),
                    topRight = Bitmap.createBitmap(full, half, 0, half, half).asImageBitmap(),
                    bottomLeft = Bitmap.createBitmap(full, 0, half, half, half).asImageBitmap(),
                    bottomRight = Bitmap.createBitmap(full, half, half, half, half).asImageBitmap(),
                )
                full.recycle()
                result
            }.getOrNull()
        }
    }

    // Individual armor piece fly-in progress (0f = scattered outer corners, 1f = locked into center core)
    val pieceTL = remember { Animatable(0f) }
    val pieceTR = remember { Animatable(0f) }
    val pieceBL = remember { Animatable(0f) }
    val pieceBR = remember { Animatable(0f) }

    // Reactor lock shockwave, HUD alpha, progress bar, and exit fade (read only inside draw/graphicsLayer)
    val shockwaveProgress = remember { Animatable(0f) }
    val hudTextAlpha = remember { Animatable(0f) }
    val bootBarProgress = remember { Animatable(0f) }
    val exitAlpha = remember { Animatable(1f) }

    val infiniteTransition = rememberInfiniteTransition(label = "hud_rings")
    val outerRingAngle = infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 7000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "outer_hud_angle",
    )
    val innerRingAngle = infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "inner_hud_angle",
    )

    val isCoreLocked by remember {
        derivedStateOf { shockwaveProgress.value > 0.25f }
    }
    val showAssembledBorder by remember {
        derivedStateOf {
            pieceTL.value > 0.92f &&
                pieceTR.value > 0.92f &&
                pieceBL.value > 0.92f &&
                pieceBR.value > 0.92f
        }
    }

    LaunchedEffect(Unit) {
        // Yield briefly so the initial window surface completes its first frame cleanly
        delay(90)

        launch {
            hudTextAlpha.animateTo(1f, tween(durationMillis = 420, easing = FastOutSlowInEasing))
        }
        launch {
            bootBarProgress.animateTo(1f, tween(durationMillis = 1950, easing = FastOutSlowInEasing))
        }

        val armorSpring = spring<Float>(
            dampingRatio = 0.68f,
            stiffness = Spring.StiffnessLow,
        )
        launch {
            delay(60)
            pieceTL.animateTo(1f, armorSpring)
        }
        launch {
            delay(170)
            pieceTR.animateTo(1f, armorSpring)
        }
        launch {
            delay(280)
            pieceBL.animateTo(1f, armorSpring)
        }
        launch {
            delay(390)
            pieceBR.animateTo(1f, armorSpring)
        }

        delay(960)
        shockwaveProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 680, easing = FastOutSlowInEasing),
        )

        delay(420)
        exitAlpha.animateTo(0f, tween(durationMillis = 180, easing = FastOutSlowInEasing))
        onFinished()
    }

    val startOffsetPx = with(density) { 190.dp.toPx() }
    val startOffsetYPx = with(density) { 230.dp.toPx() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer { alpha = exitAlpha.value }
            .background(
                Brush.radialGradient(
                    colors = listOf(ArmorNavyMid, ArmorNavyDeep, Color(0xFF02040A)),
                ),
            )
            .statusBarsPadding()
            .navigationBarsPadding()
            .testTag("max_studio_splash_screen"),
    ) {
        // HUD Background & Rotating Arc Reactor Rings + Lock Shockwave (values read strictly in DrawScope)
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height * 0.43f)
            val baseRadius = size.minDimension * 0.25f

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        ArcReactorCyan.copy(alpha = 0.26f),
                        ArmorGold.copy(alpha = 0.08f),
                        Color.Transparent,
                    ),
                    center = center,
                    radius = baseRadius * 2.2f,
                ),
                radius = baseRadius * 2.2f,
                center = center,
            )

            drawLine(
                color = ArcReactorCyan.copy(alpha = 0.14f),
                start = Offset(center.x - baseRadius * 1.9f, center.y),
                end = Offset(center.x + baseRadius * 1.9f, center.y),
                strokeWidth = 1.5f,
            )
            drawLine(
                color = ArcReactorCyan.copy(alpha = 0.14f),
                start = Offset(center.x, center.y - baseRadius * 1.9f),
                end = Offset(center.x, center.y + baseRadius * 1.9f),
                strokeWidth = 1.5f,
            )

            rotate(degrees = outerRingAngle.value, pivot = center) {
                val r = baseRadius * 1.38f
                val topLeft = Offset(center.x - r, center.y - r)
                val arcSize = Size(r * 2f, r * 2f)
                for (i in 0 until 4) {
                    drawArc(
                        color = ArmorGold.copy(alpha = 0.65f),
                        startAngle = i * 90f + 12f,
                        sweepAngle = 56f,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = 4.5f, cap = StrokeCap.Round),
                    )
                }
            }

            rotate(degrees = innerRingAngle.value, pivot = center) {
                val r = baseRadius * 1.12f
                val topLeft = Offset(center.x - r, center.y - r)
                val arcSize = Size(r * 2f, r * 2f)
                for (i in 0 until 6) {
                    drawArc(
                        color = ArcReactorCyan.copy(alpha = 0.78f),
                        startAngle = i * 60f + 8f,
                        sweepAngle = 34f,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = 3.5f, cap = StrokeCap.Round),
                    )
                }
            }

            val sw = shockwaveProgress.value
            if (sw in 0.01f..0.99f) {
                val waveRadius = baseRadius * (0.75f + sw * 1.9f)
                drawCircle(
                    color = ArcReactorCyan.copy(alpha = (1f - sw) * 0.85f),
                    radius = waveRadius,
                    center = center,
                    style = Stroke(width = (1f - sw) * 14f + 2f),
                )
                drawCircle(
                    color = ArmorGold.copy(alpha = (1f - sw) * 0.55f),
                    radius = waveRadius * 0.78f,
                    center = center,
                    style = Stroke(width = (1f - sw) * 7f + 1f),
                )
            }
        }



        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.weight(0.28f))

            Box(
                modifier = Modifier.size(136.dp),
                contentAlignment = Alignment.Center,
            ) {
                val loadedQuadrants = quadrants
                if (loadedQuadrants != null) {
                    val halfDp = 68.dp

                    ArmorLogoPiece(
                        bitmap = loadedQuadrants.topLeft,
                        progress = pieceTL,
                        startOffsetXPx = -startOffsetPx,
                        startOffsetYPx = -startOffsetYPx,
                        startRotation = -52f,
                        alignment = Alignment.TopStart,
                        pieceSizeDp = halfDp,
                        contentDescription = "Max Studio armor logo piece top left",
                    )
                    ArmorLogoPiece(
                        bitmap = loadedQuadrants.topRight,
                        progress = pieceTR,
                        startOffsetXPx = startOffsetPx,
                        startOffsetYPx = -startOffsetYPx,
                        startRotation = 52f,
                        alignment = Alignment.TopEnd,
                        pieceSizeDp = halfDp,
                        contentDescription = "Max Studio armor logo piece top right",
                    )
                    ArmorLogoPiece(
                        bitmap = loadedQuadrants.bottomLeft,
                        progress = pieceBL,
                        startOffsetXPx = -startOffsetPx,
                        startOffsetYPx = startOffsetYPx,
                        startRotation = 46f,
                        alignment = Alignment.BottomStart,
                        pieceSizeDp = halfDp,
                        contentDescription = "Max Studio armor logo piece bottom left",
                    )
                    ArmorLogoPiece(
                        bitmap = loadedQuadrants.bottomRight,
                        progress = pieceBR,
                        startOffsetXPx = startOffsetPx,
                        startOffsetYPx = startOffsetYPx,
                        startRotation = -46f,
                        alignment = Alignment.BottomEnd,
                        pieceSizeDp = halfDp,
                        contentDescription = "Max Studio armor logo piece bottom right",
                    )

                    if (showAssembledBorder) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(26.dp))
                                .border(
                                    width = 2.dp,
                                    brush = Brush.linearGradient(listOf(ArcReactorCyan, ArmorGold)),
                                    shape = RoundedCornerShape(26.dp),
                                ),
                        )
                    }
                } else {
                    Image(
                        painter = painterResource(R.drawable.launcher_max_icon),
                        contentDescription = "MAX STUDIO logo",
                        modifier = Modifier
                            .size(136.dp)
                            .clip(RoundedCornerShape(26.dp))
                            .graphicsLayer { alpha = 0.2f },
                    )
                }
            }

            Spacer(Modifier.height(34.dp))

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.graphicsLayer { alpha = hudTextAlpha.value },
            ) {
                Text(
                    text = "MAX STUDIO",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 4.sp,
                    color = Color.White,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = if (isCoreLocked) {
                        "ARMOR CORE LOCKED • IDE READY"
                    } else {
                        "ASSEMBLING STUDIO MODULES..."
                    },
                    style = MaterialTheme.typography.labelMedium,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.6.sp,
                    color = ArcReactorCyan,
                )

                Spacer(Modifier.height(18.dp))

                LinearProgressIndicator(
                    progress = { bootBarProgress.value },
                    color = ArcReactorCyan,
                    trackColor = Color.White.copy(alpha = 0.12f),
                    strokeCap = StrokeCap.Round,
                    modifier = Modifier
                        .widthIn(max = 240.dp)
                        .fillMaxWidth()
                        .height(5.dp)
                        .clip(RoundedCornerShape(3.dp)),
                )
            }

            Spacer(Modifier.weight(0.36f))

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = ArmorGold.copy(alpha = 0.12f),
                modifier = Modifier
                    .padding(bottom = 26.dp)
                    .graphicsLayer { alpha = hudTextAlpha.value }
                    .border(
                        width = 1.dp,
                        brush = Brush.horizontalGradient(
                            listOf(
                                ArmorGold.copy(alpha = 0.55f),
                                ArmorCrimson.copy(alpha = 0.45f),
                                ArcReactorCyan.copy(alpha = 0.55f),
                            ),
                        ),
                        shape = RoundedCornerShape(16.dp),
                    ),
            ) {
                Text(
                    text = "APP DEVELOPED BY IMAX SOFTWARE SOLUTIONS",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.1.sp,
                    color = Color(0xFFFEF3C7),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                )
            }
        }
    }
}

@Composable
private fun BoxScope.ArmorLogoPiece(
    bitmap: ImageBitmap,
    progress: Animatable<Float, AnimationVector1D>,
    startOffsetXPx: Float,
    startOffsetYPx: Float,
    startRotation: Float,
    alignment: Alignment,
    pieceSizeDp: Dp,
    contentDescription: String,
) {
    Image(
        bitmap = bitmap,
        contentDescription = contentDescription,
        contentScale = ContentScale.FillBounds,
        modifier = Modifier
            .align(alignment)
            .size(pieceSizeDp)
            .graphicsLayer {
                val p = progress.value
                val inv = (1f - p).coerceAtLeast(0f)
                val scale = 0.62f + (0.38f * p)
                translationX = startOffsetXPx * inv
                translationY = startOffsetYPx * inv
                rotationZ = startRotation * inv
                scaleX = scale
                scaleY = scale
                alpha = (p * 1.4f).coerceIn(0f, 1f)
            },
    )
}
