package com.example.traductor.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.traductor.R
import com.example.traductor.ui.theme.ElectricBlue
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SplashScreen(onSplashFinished: () -> Unit) {
    val scale = remember { Animatable(0.3f) }
    val alpha = remember { Animatable(0f) }

    LaunchedEffect(key1 = true) {
        launch {
            scale.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
            )
        }
        launch {
            alpha.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 800)
            )
        }
        delay(1800)
        onSplashFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF090F24), Color(0xFF0E1A38), Color(0xFF090F24))
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .scale(scale.value)
                    .graphicsLayer(alpha = alpha.value)
                    .clip(CircleShape)
                    .background(Color(0xFF1D4ED8)),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.mipmap.ic_traslate_foreground),
                    contentDescription = "Logo JP Translator",
                    modifier = Modifier.size(100.dp)
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
            Row(
                modifier = Modifier.graphicsLayer(alpha = alpha.value),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("JP ", fontWeight = FontWeight.Bold, fontSize = 28.sp, color = ElectricBlue)
                Text("Translator", fontWeight = FontWeight.Bold, fontSize = 28.sp, color = Color.White)
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                "✦ Traduce sin límites",
                fontSize = 13.sp,
                color = Color(0xFF94A3B8),
                modifier = Modifier.graphicsLayer(alpha = alpha.value)
            )
        }
    }
}
