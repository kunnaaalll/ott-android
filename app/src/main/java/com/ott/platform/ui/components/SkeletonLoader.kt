package com.ott.platform.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Pre-built skeleton layouts for common loading states.
 * These replace blank screens while Firestore data loads.
 */

@Composable
fun SkeletonHero(modifier: Modifier = Modifier) {
    ShimmerEffect(
        modifier = modifier.fillMaxWidth(),
        height = 420.dp,
        shape = RoundedCornerShape(0.dp),
    )
}

@Composable
fun SkeletonContentRow(modifier: Modifier = Modifier) {
    Column(modifier = modifier.padding(start = 16.dp)) {
        ShimmerEffect(
            modifier = Modifier.width(140.dp),
            height = 20.dp,
        )
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            repeat(4) {
                ShimmerEffect(
                    modifier = Modifier.width(120.dp),
                    height = 170.dp,
                    shape = RoundedCornerShape(8.dp),
                )
            }
        }
    }
}

@Composable
fun SkeletonHomeScreen(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        SkeletonHero()
        SkeletonContentRow()
        SkeletonContentRow()
        SkeletonContentRow()
    }
}

@Composable
fun SkeletonGrid(modifier: Modifier = Modifier, columns: Int = 3, rows: Int = 4) {
    Column(
        modifier = modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        repeat(rows) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                repeat(columns) {
                    ShimmerEffect(
                        modifier = Modifier.weight(1f),
                        height = 170.dp,
                        shape = RoundedCornerShape(8.dp),
                    )
                }
            }
        }
    }
}
