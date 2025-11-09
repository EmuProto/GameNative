package app.gamenative.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import app.gamenative.ui.theme.PluviaTheme
import kotlinx.coroutines.delay
import kotlin.random.Random

@Composable
fun BootingSplash(
    visible: Boolean = true,
    text: String = "Booting...",
    onBootCompleted: () -> Unit = {}
) {
    // Tailwind-style “animate-pulse”: opacity 0.3 → 0.5 → 0.3
    val pulseAlpha by rememberInfiniteTransition(label = "pulse")
        .animateFloat(
            initialValue = 0.25f,
            targetValue = 0.65f,
            animationSpec = infiniteRepeatable(
                animation = tween(3000, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "alpha"
        )

    val tips = remember {
        listOf(
            "Booting may take a few minutes on first launch",
            "Tip: You can view the game files by pressing \"Open Container\" in the game settings.",
            "Tip: Try the Adreno or Snapdragon 8 Elite drivers if you are on a compatible device.",
            "Tip: Turn off \"Show FPS\" to get rid of the mesa overlay.",
            "Tip: Install packages in A:\\_CommonRedist if your game doesn't launch.",
            "Tip: You can enable or disable the onscreen controller with your device's back key.",
            "Tip: You can bring up the keyboard with your device's back key.",
            "Tip: You can tap with two fingers inside the container to right click.",
            "Tip: If you are using the onscreen controller, you can disable the mouse to prevent accidental touches.",
            "Tip: Report issues on Discord so we can fix them.",
            "Tip: Use the Vortek driver if you are on a non-Adreno GPU.",
            "Tip: Lower resolution and use box64 in performance mode to boost FPS.",
            "Tip: If the game is crashing after loading, increase the video memory.",
            "Tip: If you are seeing visual glitches, try setting environment variables MESA_VK_WSI_DEBUG to sw and MESA_VK_WSI_PRESENT_MODE to immediate.",
            "Tip: You can enable touchscreen mode.",
        )
    }

    // Start at a random tip, then rotate while visible
    var tipIndex by remember { mutableStateOf(if (tips.isNotEmpty()) Random.nextInt(tips.size) else 0) }

    LaunchedEffect(visible, tips) {
        while (visible && tips.isNotEmpty()) {
            delay(4000)
            tipIndex = (tipIndex + 1) % tips.size
        }
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(durationMillis = 99)),
        exit = fadeOut()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            // Gradient overlay (drawn first, so it sits behind the text)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(pulseAlpha)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                Color(0xFFA21CAF),   // purple start
                                Color.Black,         // black centre 1
                                Color.Black,         // black centre 2
                                Color.Black,
                                Color.Black,
                                Color(0xFF06B6D4)    // cyan end
                            )
                        )
                    )
            )

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "GameNative",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold,
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.tertiary
                            )
                        )
                    )
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )


                if (tips.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(24.dp))
                    Crossfade(
                        targetState = tipIndex,
                        animationSpec = tween(durationMillis = 600),
                        label = "tipCrossfade"
                    ) { idx ->
                        Text(
                            text = tips[idx],
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 24.dp)
                        )
                    }
                }
            }
        }
    }
}

@Preview(name = "BootingSplash")
@Composable
fun BootingSplashPreview() {
	PluviaTheme {
		BootingSplash(visible = true)
	}
}
