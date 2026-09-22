package com.smartcalculator.ai

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Color
import android.util.AttributeSet
import android.view.View
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class GraphView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(35, 50, 75)
        strokeWidth = 1f
        style = Paint.Style.STROKE
    }

    private val axisPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(130, 150, 180)
        strokeWidth = 2f
        style = Paint.Style.STROKE
    }

    private val graphPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(36, 107, 253)
        strokeWidth = 5f
        style = Paint.Style.STROKE
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(180, 195, 215)
        textSize = 28f
        style = Paint.Style.FILL
    }

    private var function: ((Double) -> Double)? = null

    private var xMin = -10.0
    private var xMax = 10.0
    private var yMin = -10.0
    private var yMax = 10.0

    fun setFunction(
        newFunction: (Double) -> Double,
        newXMin: Double = -10.0,
        newXMax: Double = 10.0,
        newYMin: Double = -10.0,
        newYMax: Double = 10.0
    ) {
        function = newFunction
        xMin = newXMin
        xMax = newXMax
        yMin = newYMin
        yMax = newYMax
        invalidate()
    }

    fun clearGraph() {
        function = null
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        canvas.drawColor(Color.rgb(14, 23, 40))

        drawGrid(canvas)
        drawAxes(canvas)
        drawLabels(canvas)
        drawFunction(canvas)
    }

    private fun toScreenX(x: Double): Float {
        return (
            ((x - xMin) / (xMax - xMin)) * width
        ).toFloat()
    }

    private fun toScreenY(y: Double): Float {
        return (
            height -
                ((y - yMin) / (yMax - yMin)) * height
        ).toFloat()
    }

    private fun drawGrid(canvas: Canvas) {

        val gridStep = calculateGridStep()

        var x = kotlin.math.ceil(xMin / gridStep) * gridStep

        while (x <= xMax) {

            val screenX = toScreenX(x)

            canvas.drawLine(
                screenX,
                0f,
                screenX,
                height.toFloat(),
                gridPaint
            )

            x += gridStep
        }

        var y = kotlin.math.ceil(yMin / gridStep) * gridStep

        while (y <= yMax) {

            val screenY = toScreenY(y)

            canvas.drawLine(
                0f,
                screenY,
                width.toFloat(),
                screenY,
                gridPaint
            )

            y += gridStep
        }
    }

    private fun drawAxes(canvas: Canvas) {

        if (xMin <= 0 && xMax >= 0) {

            val zeroX = toScreenX(0.0)

            canvas.drawLine(
                zeroX,
                0f,
                zeroX,
                height.toFloat(),
                axisPaint
            )
        }

        if (yMin <= 0 && yMax >= 0) {

            val zeroY = toScreenY(0.0)

            canvas.drawLine(
                0f,
                zeroY,
                width.toFloat(),
                zeroY,
                axisPaint
            )
        }
    }

    private fun drawLabels(canvas: Canvas) {

        if (xMin <= 0 && xMax >= 0) {

            val zeroX = toScreenX(0.0)

            canvas.drawText(
                "Y",
                zeroX + 10f,
                30f,
                textPaint
            )
        }

        if (yMin <= 0 && yMax >= 0) {

            val zeroY = toScreenY(0.0)

            canvas.drawText(
                "X",
                width - 35f,
                zeroY - 10f,
                textPaint
            )
        }
    }

    private fun drawFunction(canvas: Canvas) {

        val currentFunction = function ?: return

        val path = Path()

        var firstPoint = true

        val samples = max(width, 400)

        for (i in 0..samples) {

            val ratio = i.toDouble() / samples.toDouble()

            val x = xMin + (xMax - xMin) * ratio

            val y = try {
                currentFunction(x)
            } catch (_: Exception) {
                Double.NaN
            }

            if (!y.isFinite()) {
                firstPoint = true
                continue
            }

            if (y < yMin * 5 || y > yMax * 5) {
                firstPoint = true
                continue
            }

            val screenX = toScreenX(x)
            val screenY = toScreenY(y)

            if (firstPoint) {
                path.moveTo(screenX, screenY)
                firstPoint = false
            } else {
                path.lineTo(screenX, screenY)
            }
        }

        canvas.drawPath(path, graphPaint)
    }

    private fun calculateGridStep(): Double {

        val range = max(
            abs(xMax - xMin),
            abs(yMax - yMin)
        )

        return when {
            range <= 10 -> 1.0
            range <= 20 -> 2.0
            range <= 50 -> 5.0
            range <= 100 -> 10.0
            else -> 20.0
        }
    }
}
