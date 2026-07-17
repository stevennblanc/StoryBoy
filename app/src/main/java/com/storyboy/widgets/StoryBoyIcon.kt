package com.storyboy.widgets

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

enum class StoryBoyIconKind {
    Books,
    Store,
    Gear,
    Sliders,
    Sort,
    Refresh,
    Profile,
    Check,
}

@Composable
fun StoryBoyIcon(
    kind: StoryBoyIconKind,
    color: Color,
    modifier: Modifier = Modifier.size(28.dp),
) {
    Canvas(modifier = modifier) {
        val stroke = Stroke(width = size.minDimension * 0.08f, cap = StrokeCap.Round)
        val thinStroke = Stroke(width = size.minDimension * 0.06f, cap = StrokeCap.Round)
        when (kind) {
            StoryBoyIconKind.Books -> {
                drawRect(color, topLeft = Offset(size.width * 0.18f, size.height * 0.18f), size = Size(size.width * 0.24f, size.height * 0.64f), style = stroke)
                drawRect(color, topLeft = Offset(size.width * 0.48f, size.height * 0.18f), size = Size(size.width * 0.28f, size.height * 0.64f), style = stroke)
                drawLine(color, Offset(size.width * 0.30f, size.height * 0.26f), Offset(size.width * 0.30f, size.height * 0.74f), strokeWidth = stroke.width)
            }

            StoryBoyIconKind.Store -> {
                drawLine(color, Offset(size.width * 0.18f, size.height * 0.38f), Offset(size.width * 0.82f, size.height * 0.38f), strokeWidth = stroke.width, cap = StrokeCap.Round)
                drawRect(color, topLeft = Offset(size.width * 0.25f, size.height * 0.38f), size = Size(size.width * 0.50f, size.height * 0.40f), style = stroke)
                drawLine(color, Offset(size.width * 0.25f, size.height * 0.38f), Offset(size.width * 0.34f, size.height * 0.20f), strokeWidth = stroke.width, cap = StrokeCap.Round)
                drawLine(color, Offset(size.width * 0.75f, size.height * 0.38f), Offset(size.width * 0.66f, size.height * 0.20f), strokeWidth = stroke.width, cap = StrokeCap.Round)
                drawLine(color, Offset(size.width * 0.34f, size.height * 0.20f), Offset(size.width * 0.66f, size.height * 0.20f), strokeWidth = stroke.width, cap = StrokeCap.Round)
            }

            StoryBoyIconKind.Gear -> {
                drawCircle(color, radius = size.minDimension * 0.20f, center = center, style = stroke)
                drawCircle(color, radius = size.minDimension * 0.06f, center = center)
                repeat(8) { index ->
                    val angle = Math.toRadians((index * 45).toDouble())
                    val inner = Offset(
                        x = center.x + kotlin.math.cos(angle).toFloat() * size.minDimension * 0.28f,
                        y = center.y + kotlin.math.sin(angle).toFloat() * size.minDimension * 0.28f,
                    )
                    val outer = Offset(
                        x = center.x + kotlin.math.cos(angle).toFloat() * size.minDimension * 0.40f,
                        y = center.y + kotlin.math.sin(angle).toFloat() * size.minDimension * 0.40f,
                    )
                    drawLine(color, inner, outer, strokeWidth = stroke.width, cap = StrokeCap.Round)
                }
            }

            StoryBoyIconKind.Sliders -> {
                val ys = listOf(0.25f, 0.50f, 0.75f)
                val knobs = listOf(0.68f, 0.35f, 0.56f)
                ys.forEachIndexed { index, y ->
                    drawLine(color, Offset(size.width * 0.18f, size.height * y), Offset(size.width * 0.82f, size.height * y), strokeWidth = thinStroke.width, cap = StrokeCap.Round)
                    drawCircle(color, radius = size.minDimension * 0.07f, center = Offset(size.width * knobs[index], size.height * y))
                }
            }

            StoryBoyIconKind.Sort -> {
                drawLine(color, Offset(size.width * 0.32f, size.height * 0.18f), Offset(size.width * 0.32f, size.height * 0.78f), strokeWidth = stroke.width, cap = StrokeCap.Round)
                drawLine(color, Offset(size.width * 0.20f, size.height * 0.30f), Offset(size.width * 0.32f, size.height * 0.18f), strokeWidth = stroke.width, cap = StrokeCap.Round)
                drawLine(color, Offset(size.width * 0.44f, size.height * 0.30f), Offset(size.width * 0.32f, size.height * 0.18f), strokeWidth = stroke.width, cap = StrokeCap.Round)
                drawLine(color, Offset(size.width * 0.68f, size.height * 0.22f), Offset(size.width * 0.68f, size.height * 0.82f), strokeWidth = stroke.width, cap = StrokeCap.Round)
                drawLine(color, Offset(size.width * 0.56f, size.height * 0.70f), Offset(size.width * 0.68f, size.height * 0.82f), strokeWidth = stroke.width, cap = StrokeCap.Round)
                drawLine(color, Offset(size.width * 0.80f, size.height * 0.70f), Offset(size.width * 0.68f, size.height * 0.82f), strokeWidth = stroke.width, cap = StrokeCap.Round)
            }

            StoryBoyIconKind.Refresh -> {
                drawArc(color, startAngle = 30f, sweepAngle = 280f, useCenter = false, topLeft = Offset(size.width * 0.22f, size.height * 0.22f), size = Size(size.width * 0.56f, size.height * 0.56f), style = stroke)
                drawLine(color, Offset(size.width * 0.74f, size.height * 0.32f), Offset(size.width * 0.78f, size.height * 0.18f), strokeWidth = stroke.width, cap = StrokeCap.Round)
                drawLine(color, Offset(size.width * 0.74f, size.height * 0.32f), Offset(size.width * 0.60f, size.height * 0.30f), strokeWidth = stroke.width, cap = StrokeCap.Round)
            }

            StoryBoyIconKind.Profile -> {
                drawCircle(color, radius = size.minDimension * 0.14f, center = Offset(size.width * 0.50f, size.height * 0.34f), style = stroke)
                drawArc(color, startAngle = 205f, sweepAngle = 130f, useCenter = false, topLeft = Offset(size.width * 0.24f, size.height * 0.46f), size = Size(size.width * 0.52f, size.height * 0.42f), style = stroke)
            }

            StoryBoyIconKind.Check -> {
                drawLine(color, Offset(size.width * 0.22f, size.height * 0.52f), Offset(size.width * 0.42f, size.height * 0.70f), strokeWidth = stroke.width, cap = StrokeCap.Round)
                drawLine(color, Offset(size.width * 0.42f, size.height * 0.70f), Offset(size.width * 0.78f, size.height * 0.30f), strokeWidth = stroke.width, cap = StrokeCap.Round)
            }
        }
    }
}
