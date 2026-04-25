package com.hunterxdk.gymsololeveling.feature.rankings.presentation

import androidx.compose.ui.graphics.Path
import com.hunterxdk.gymsololeveling.core.domain.model.enums.MuscleGroup

/**
 * Normalized muscle paths in a 200×400 viewBox coordinate space.
 * Hit-test by converting tap coordinates to viewBox space before testing bounds.
 */
object MusclePaths {

    data class MusclePathData(
        val muscle: MuscleGroup,
        val path: Path,
        val isFront: Boolean,
    )

    fun buildFrontPaths(): List<MusclePathData> = listOf(
        MusclePathData(MuscleGroup.CHEST, Path().apply {
            moveTo(70f, 110f); lineTo(100f, 115f); lineTo(130f, 110f)
            lineTo(135f, 145f); lineTo(100f, 155f); lineTo(65f, 145f); close()
        }, isFront = true),

        MusclePathData(MuscleGroup.FRONT_SHOULDERS, Path().apply {
            moveTo(55f, 100f); lineTo(70f, 100f); lineTo(72f, 130f)
            lineTo(55f, 125f); close()
        }, isFront = true),
        MusclePathData(MuscleGroup.FRONT_SHOULDERS, Path().apply {
            moveTo(130f, 100f); lineTo(145f, 100f); lineTo(145f, 125f)
            lineTo(128f, 130f); close()
        }, isFront = true),

        MusclePathData(MuscleGroup.BICEPS, Path().apply {
            moveTo(48f, 130f); lineTo(60f, 130f); lineTo(62f, 165f)
            lineTo(48f, 160f); close()
        }, isFront = true),
        MusclePathData(MuscleGroup.BICEPS, Path().apply {
            moveTo(140f, 130f); lineTo(152f, 130f); lineTo(152f, 160f)
            lineTo(138f, 165f); close()
        }, isFront = true),

        MusclePathData(MuscleGroup.ABS, Path().apply {
            moveTo(82f, 155f); lineTo(118f, 155f); lineTo(118f, 210f)
            lineTo(82f, 210f); close()
        }, isFront = true),

        MusclePathData(MuscleGroup.OBLIQUES, Path().apply {
            moveTo(68f, 155f); lineTo(82f, 158f); lineTo(80f, 210f)
            lineTo(65f, 200f); close()
        }, isFront = true),
        MusclePathData(MuscleGroup.OBLIQUES, Path().apply {
            moveTo(118f, 158f); lineTo(132f, 155f); lineTo(135f, 200f)
            lineTo(120f, 210f); close()
        }, isFront = true),

        MusclePathData(MuscleGroup.QUADRICEPS, Path().apply {
            moveTo(72f, 220f); lineTo(98f, 220f); lineTo(95f, 295f)
            lineTo(68f, 290f); close()
        }, isFront = true),
        MusclePathData(MuscleGroup.QUADRICEPS, Path().apply {
            moveTo(102f, 220f); lineTo(128f, 220f); lineTo(132f, 290f)
            lineTo(105f, 295f); close()
        }, isFront = true),

        MusclePathData(MuscleGroup.CALVES, Path().apply {
            moveTo(73f, 305f); lineTo(92f, 305f); lineTo(90f, 365f)
            lineTo(72f, 360f); close()
        }, isFront = true),
        MusclePathData(MuscleGroup.CALVES, Path().apply {
            moveTo(108f, 305f); lineTo(127f, 305f); lineTo(128f, 360f)
            lineTo(110f, 365f); close()
        }, isFront = true),
    )

    fun buildBackPaths(): List<MusclePathData> = listOf(
        MusclePathData(MuscleGroup.BACK_SHOULDERS, Path().apply {
            moveTo(55f, 100f); lineTo(72f, 100f); lineTo(72f, 128f)
            lineTo(55f, 122f); close()
        }, isFront = false),
        MusclePathData(MuscleGroup.BACK_SHOULDERS, Path().apply {
            moveTo(128f, 100f); lineTo(145f, 100f); lineTo(145f, 122f)
            lineTo(128f, 128f); close()
        }, isFront = false),

        MusclePathData(MuscleGroup.TRAPS, Path().apply {
            moveTo(80f, 88f); lineTo(100f, 82f); lineTo(120f, 88f)
            lineTo(128f, 108f); lineTo(100f, 112f); lineTo(72f, 108f); close()
        }, isFront = false),

        MusclePathData(MuscleGroup.UPPER_BACK, Path().apply {
            moveTo(72f, 112f); lineTo(128f, 112f); lineTo(128f, 148f)
            lineTo(72f, 148f); close()
        }, isFront = false),

        MusclePathData(MuscleGroup.LATS, Path().apply {
            moveTo(62f, 125f); lineTo(72f, 125f); lineTo(74f, 175f)
            lineTo(60f, 165f); close()
        }, isFront = false),
        MusclePathData(MuscleGroup.LATS, Path().apply {
            moveTo(128f, 125f); lineTo(138f, 125f); lineTo(140f, 165f)
            lineTo(126f, 175f); close()
        }, isFront = false),

        MusclePathData(MuscleGroup.MIDDLE_BACK, Path().apply {
            moveTo(78f, 148f); lineTo(122f, 148f); lineTo(120f, 175f)
            lineTo(80f, 175f); close()
        }, isFront = false),

        MusclePathData(MuscleGroup.LOWER_BACK, Path().apply {
            moveTo(82f, 175f); lineTo(118f, 175f); lineTo(116f, 205f)
            lineTo(84f, 205f); close()
        }, isFront = false),

        MusclePathData(MuscleGroup.GLUTES, Path().apply {
            moveTo(72f, 205f); lineTo(128f, 205f); lineTo(130f, 240f)
            lineTo(70f, 240f); close()
        }, isFront = false),

        MusclePathData(MuscleGroup.HAMSTRINGS, Path().apply {
            moveTo(72f, 242f); lineTo(97f, 242f); lineTo(94f, 295f)
            lineTo(68f, 290f); close()
        }, isFront = false),
        MusclePathData(MuscleGroup.HAMSTRINGS, Path().apply {
            moveTo(103f, 242f); lineTo(128f, 242f); lineTo(132f, 290f)
            lineTo(106f, 295f); close()
        }, isFront = false),
    )

    /**
     * Convert tap to viewBox space, then test each path's bounding rect.
     * Bounding-box approximation is sufficient for these convex regions.
     */
    fun hitTest(
        paths: List<MusclePathData>,
        tapX: Float,
        tapY: Float,
        canvasWidth: Float,
        canvasHeight: Float,
    ): MuscleGroup? {
        val viewBoxW = 200f
        val viewBoxH = 400f
        val viewX = tapX * viewBoxW / canvasWidth
        val viewY = tapY * viewBoxH / canvasHeight

        // Iterate in reverse so overlapping top-layer paths win
        for (pathData in paths.reversed()) {
            val bounds = pathData.path.getBounds()
            if (viewX in bounds.left..bounds.right && viewY in bounds.top..bounds.bottom) {
                return pathData.muscle
            }
        }
        return null
    }
}
