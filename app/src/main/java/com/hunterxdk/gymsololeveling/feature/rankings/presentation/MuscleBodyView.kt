package com.hunterxdk.gymsololeveling.feature.rankings.presentation

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import com.hunterxdk.gymsololeveling.core.domain.model.MuscleRank
import com.hunterxdk.gymsololeveling.core.domain.model.enums.MuscleGroup
import com.hunterxdk.gymsololeveling.core.domain.model.enums.RankTier
import com.hunterxdk.gymsololeveling.core.service.RankService

@Composable
fun MuscleBodyView(
    muscleRanks: List<MuscleRank>,
    isFrontView: Boolean,
    onMuscleSelected: (MuscleGroup) -> Unit,
    selectedMuscle: MuscleGroup? = null,
    modifier: Modifier = Modifier,
) {
    val rankMap = muscleRanks.associateBy { it.muscleGroup }
    val paths = remember(isFrontView) {
        if (isFrontView) MusclePaths.buildFrontPaths()
        else MusclePaths.buildBackPaths()
    }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(0.5f)
            .pointerInput(isFrontView) {
                detectTapGestures { offset ->
                    val muscle = MusclePaths.hitTest(
                        paths = paths,
                        tapX = offset.x,
                        tapY = offset.y,
                        canvasWidth = size.width.toFloat(),
                        canvasHeight = size.height.toFloat(),
                    )
                    muscle?.let { onMuscleSelected(it) }
                }
            }
    ) {
        val viewBoxW = 200f
        val viewBoxH = 400f
        val scaleX = size.width / viewBoxW
        val scaleY = size.height / viewBoxH

        drawRoundRect(
            color = Color(0xFF2A2A2A),
            size = Size(size.width, size.height),
            cornerRadius = CornerRadius(16f * scaleX),
        )

        paths.forEach { pathData ->
            val rank = rankMap[pathData.muscle]
            val baseColor = RankService.getRankColor(rank?.currentRank ?: RankTier.UNTRAINED)
            val isSelected = selectedMuscle == pathData.muscle

            withTransform({ scale(scaleX, scaleY, pivot = Offset.Zero) }) {
                drawPath(
                    path = pathData.path,
                    color = if (isSelected) baseColor else baseColor.copy(alpha = 0.7f),
                )
                if (isSelected) {
                    drawPath(
                        path = pathData.path,
                        color = Color.White,
                        style = Stroke(width = 2f / scaleX),
                    )
                }
            }
        }
    }
}
