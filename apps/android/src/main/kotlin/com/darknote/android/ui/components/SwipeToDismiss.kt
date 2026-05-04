package com.darknote.android.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlin.math.abs

@Composable
fun SwipeToDismissBox(
    onSwipeLeft: (() -> Unit)? = null,
    onSwipeRight: (() -> Unit)? = null,
    enableSwipeLeft: Boolean = true,
    enableSwipeRight: Boolean = true,
    content: @Composable () -> Unit
) {
    val density = LocalDensity.current
    val coroutineScope = rememberCoroutineScope()
    val offsetX = remember { Animatable(0f) }
    val swipeThresholdPx = with(density) { 150.dp.toPx() }
    val swipeMaxPx = with(density) { 200.dp.toPx() }

    val leftBgColor by animateColorAsState(
        targetValue = if (offsetX.value < -swipeThresholdPx) {
            MaterialTheme.colorScheme.errorContainer
        } else {
            Color.Transparent
        },
        animationSpec = tween(200),
        label = "leftBgColor"
    )

    val rightBgColor by animateColorAsState(
        targetValue = if (offsetX.value > swipeThresholdPx) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            Color.Transparent
        },
        animationSpec = tween(200),
        label = "rightBgColor"
    )

    Box(
        modifier = Modifier
            .clip(MaterialTheme.shapes.medium)
            .fillMaxWidth()
    ) {
        if (enableSwipeLeft) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(leftBgColor)
                    .padding(horizontal = 24.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
        if (enableSwipeRight) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(rightBgColor)
                    .padding(horizontal = 24.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Icon(
                    Icons.Filled.Star,
                    contentDescription = "Favorite",
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        Box(
            modifier = Modifier
                .graphicsLayer {
                    translationX = offsetX.value
                }
                .pointerInput(enableSwipeLeft, enableSwipeRight) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            coroutineScope.launch {
                                if (abs(offsetX.value) > swipeThresholdPx) {
                                    if (offsetX.value < 0 && enableSwipeLeft) {
                                        onSwipeLeft?.invoke()
                                    } else if (offsetX.value > 0 && enableSwipeRight) {
                                        onSwipeRight?.invoke()
                                    }
                                }
                                offsetX.animateTo(0f, tween(200))
                            }
                        },
                        onHorizontalDrag = { _, dragAmount ->
                            coroutineScope.launch {
                                val newOffset = (offsetX.value + dragAmount).coerceIn(
                                    -swipeMaxPx,
                                    swipeMaxPx
                                )
                                offsetX.snapTo(newOffset)
                            }
                        }
                    )
                }
        ) {
            content()
        }
    }
}