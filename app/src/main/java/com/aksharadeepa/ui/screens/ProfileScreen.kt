package com.aksharadeepa.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aksharadeepa.viewmodel.MainViewModel
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun ProfileScreen(viewModel: MainViewModel) {
    val subjects by viewModel.subjects.collectAsState()
    
    val totalChapters = subjects.sumOf { it.chapters.size }
    val completedChapters = subjects.sumOf { it.chapters.count { ch -> ch.isCompleted } }
    val overallProgress = if (totalChapters > 0) completedChapters.toFloat() / totalChapters else 0f

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Header
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.primary,
            shape = RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp)
        ) {
            Column(
                modifier = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    modifier = Modifier.size(100.dp),
                    shape = CircleShape,
                    color = Color.White.copy(alpha = 0.2f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.AccountCircle,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(80.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text("Akshara Scholar", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Text("Class 10 Student", fontSize = 14.sp, color = Color.White.copy(alpha = 0.8f))
            }
        }

        Column(modifier = Modifier.padding(24.dp)) {
            // Stats Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                StatCard(
                    title = "Finished",
                    value = "$completedChapters/$totalChapters",
                    icon = Icons.Default.EmojiEvents,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    title = "Mastery",
                    value = "${(overallProgress * 100).toInt()}%",
                    icon = Icons.Default.Star,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text("Performance Radar", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))
            
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    if (subjects.isNotEmpty()) {
                        RadarChart(
                            labels = subjects.map { it.subject.name },
                            data = subjects.map { it.subject.masteryLevel.toFloat() },
                            modifier = Modifier.fillMaxWidth().height(220.dp)
                        )
                    } else {
                        Text("Start learning to see your radar chart!")
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Settings / Other
            ProfileOption(icon = Icons.Default.Settings, title = "Study Settings")
            ProfileOption(icon = Icons.Default.EmojiEvents, title = "Certificates & Achievements")
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun StatCard(title: String, value: String, icon: ImageVector, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(value, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text(title, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun ProfileOption(icon: ImageVector, title: String) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable { },
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(16.dp))
            Text(title, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
            Icon(Icons.Default.Star, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
fun RadarChart(labels: List<String>, data: List<Float>, modifier: Modifier = Modifier) {
    val primaryColor = MaterialTheme.colorScheme.primary
    
    Canvas(modifier = modifier) {
        val center = Offset(size.width / 2, size.height / 2)
        val radius = (minOf(size.width, size.height) / 2) * 0.7f
        val numAxes = data.size
        
        if (numAxes < 3) return@Canvas

        val getOffset = { value: Float, i: Int ->
            val angle = (Math.PI * 2 * i) / numAxes - Math.PI / 2
            Offset(
                x = center.x + radius * (value / 100f) * cos(angle).toFloat(),
                y = center.y + radius * (value / 100f) * sin(angle).toFloat()
            )
        }

        // Draw Rings
        listOf(20f, 40f, 60f, 80f, 100f).forEach { level ->
            val path = Path()
            for (i in 0 until numAxes) {
                val p = getOffset(level, i)
                if (i == 0) path.moveTo(p.x, p.y) else path.lineTo(p.x, p.y)
            }
            path.close()
            drawPath(path, color = Color.LightGray.copy(alpha = 0.5f), style = Stroke(1f))
        }

        // Draw Axes & Labels
        for (i in 0 until numAxes) {
            val p = getOffset(100f, i)
            drawLine(color = Color.LightGray.copy(alpha = 0.5f), start = center, end = p, strokeWidth = 1f)
            
            // Labels
            drawContext.canvas.nativeCanvas.drawText(
                labels[i].take(3),
                p.x,
                p.y,
                android.graphics.Paint().apply {
                    color = android.graphics.Color.GRAY
                    textSize = 30f
                    textAlign = android.graphics.Paint.Align.CENTER
                }
            )
        }

        // Draw Data Polygon
        val dataPath = Path()
        for (i in 0 until numAxes) {
            val p = getOffset(data[i], i)
            if (i == 0) dataPath.moveTo(p.x, p.y) else dataPath.lineTo(p.x, p.y)
        }
        dataPath.close()
        drawPath(dataPath, color = primaryColor.copy(alpha = 0.3f))
        drawPath(dataPath, color = primaryColor, style = Stroke(3f))
        
        // Data Points
        for (i in 0 until numAxes) {
            drawCircle(color = primaryColor, radius = 6f, center = getOffset(data[i], i))
        }
    }
}
