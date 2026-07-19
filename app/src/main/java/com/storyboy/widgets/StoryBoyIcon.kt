package com.storyboy.widgets

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
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
                val pageSize = Size(size.width * 0.30f, size.height * 0.46f)
                val corner = CornerRadius(size.minDimension * 0.06f)
                rotate(degrees = -7f, pivot = Offset(size.width * 0.50f, size.height * 0.26f)) {
                    drawRoundRect(color, topLeft = Offset(size.width * 0.18f, size.height * 0.26f), size = pageSize, cornerRadius = corner, style = stroke)
                }
                rotate(degrees = 7f, pivot = Offset(size.width * 0.52f, size.height * 0.26f)) {
                    drawRoundRect(color, topLeft = Offset(size.width * 0.52f, size.height * 0.26f), size = pageSize, cornerRadius = corner, style = stroke)
                }
            }

            StoryBoyIconKind.Store -> {
                drawRoundRect(
                    color,
                    topLeft = Offset(size.width * 0.22f, size.height * 0.34f),
                    size = Size(size.width * 0.56f, size.height * 0.46f),
                    cornerRadius = CornerRadius(size.minDimension * 0.08f),
                    style = stroke,
                )
                drawArc(
                    color,
                    startAngle = 180f,
                    sweepAngle = 180f,
                    useCenter = false,
                    topLeft = Offset(size.width * 0.35f, size.height * 0.18f),
                    size = Size(size.width * 0.30f, size.height * 0.32f),
                    style = stroke,
                )
            }

            StoryBoyIconKind.Gear -> {
                drawCircle(color, radius = size.minDimension * 0.16f, center = center, style = stroke)
                repeat(6) { index ->
                    val angle = Math.toRadians((index * 60 + 30).toDouble())
                    val inner = Offset(
                        x = center.x + kotlin.math.cos(angle).toFloat() * size.minDimension * 0.26f,
                        y = center.y + kotlin.math.sin(angle).toFloat() * size.minDimension * 0.26f,
                    )
                    val outer = Offset(
                        x = center.x + kotlin.math.cos(angle).toFloat() * size.minDimension * 0.40f,
                        y = center.y + kotlin.math.sin(angle).toFloat() * size.minDimension * 0.40f,
                    )
                    drawLine(color, inner, outer, strokeWidth = stroke.width * 1.3f, cap = StrokeCap.Round)
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
