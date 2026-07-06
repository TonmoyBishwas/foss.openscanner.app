package app.openscanner.android.core.vision

import kotlin.math.abs
import kotlin.math.hypot

/** A 2D point in pixel coordinates. Pure Kotlin so the vision core is unit-testable. */
data class Point2(val x: Float, val y: Float) {
    fun distanceTo(other: Point2): Float = hypot(x - other.x, y - other.y)
}

/**
 * A detected document quadrilateral, corners ordered
 * top-left, top-right, bottom-right, bottom-left.
 */
data class Quad(
    val topLeft: Point2,
    val topRight: Point2,
    val bottomRight: Point2,
    val bottomLeft: Point2
) {
    val corners: List<Point2>
        get() = listOf(topLeft, topRight, bottomRight, bottomLeft)

    /** Shoelace area. */
    fun area(): Float {
        val c = corners
        var sum = 0f
        for (i in c.indices) {
            val j = (i + 1) % c.size
            sum += c[i].x * c[j].y - c[j].x * c[i].y
        }
        return abs(sum) / 2f
    }

    fun scaled(sx: Float, sy: Float): Quad = map { Point2(it.x * sx, it.y * sy) }

    fun map(transform: (Point2) -> Point2): Quad = Quad(
        transform(topLeft), transform(topRight), transform(bottomRight), transform(bottomLeft)
    )

    /** Mean per-corner distance to another quad; used for temporal stability checks. */
    fun distanceTo(other: Quad): Float =
        corners.zip(other.corners).map { (a, b) -> a.distanceTo(b) }.average().toFloat()

    companion object {
        /**
         * Orders four arbitrary corners as TL, TR, BR, BL.
         * TL has the smallest x+y, BR the largest; TR has the largest x-y, BL the smallest.
         */
        fun fromUnordered(points: List<Point2>): Quad {
            require(points.size == 4) { "Quad needs exactly 4 points" }
            val topLeft = points.minBy { it.x + it.y }
            val bottomRight = points.maxBy { it.x + it.y }
            val topRight = points.maxBy { it.x - it.y }
            val bottomLeft = points.minBy { it.x - it.y }
            return Quad(topLeft, topRight, bottomRight, bottomLeft)
        }
    }
}
