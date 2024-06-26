package io.esper.android.tflite

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import io.esper.android.files.R
import org.tensorflow.lite.task.vision.detector.Detection
import java.util.LinkedList
import kotlin.math.max

class OverlayView(context: Context?, attrs: AttributeSet?) : View(context, attrs) {

    private var results: List<Detection> = LinkedList()
    private var boxPaint = Paint()
    private var textBackgroundPaint = Paint()
    private var textPaintLabel = Paint()
    private var textPaintScore = Paint()
    private var scaleFactor: Float = 1f
    private var bounds = Rect()

    // Preallocate objects to avoid allocation during onDraw
    private val drawableRect = RectF()
    private val textRect = RectF()
    private val drawableTextBuilder = StringBuilder()

    init {
        initPaints()
    }

    fun clear() {
        textPaintLabel.reset()
        textPaintScore.reset()
        textBackgroundPaint.reset()
        boxPaint.reset()
        invalidate()
        initPaints()
    }

    private fun initPaints() {
        // Set the color of the bounding box and text background to the same value
        val boundingBoxColor = ContextCompat.getColor(context!!, R.color.bounding_box_color)

        boxPaint.apply {
            color = boundingBoxColor
            strokeWidth = 8f
            style = Paint.Style.STROKE
            isAntiAlias = true
        }

        textBackgroundPaint.apply {
            color = boundingBoxColor
            style = Paint.Style.FILL
            isAntiAlias = true
        }

        textPaintLabel.apply {
            color = Color.WHITE
            style = Paint.Style.FILL
            textSize = 50f
        }

        textPaintScore.apply {
            style = Paint.Style.FILL
            textSize = 50f
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        for (result in results) {
            val boundingBox = result.boundingBox

            val left = boundingBox.left * scaleFactor
            val top = boundingBox.top * scaleFactor
            val right = boundingBox.right * scaleFactor
            val bottom = boundingBox.bottom * scaleFactor

            // Draw rounded bounding box around detected objects
            drawableRect.set(left, top, right, bottom)
            canvas.drawRoundRect(drawableRect, BOX_CORNER_RADIUS, BOX_CORNER_RADIUS, boxPaint)

            // Create text to display above detected objects
            drawableTextBuilder.setLength(0)  // Clear the builder
            val label = result.categories[0].label
            val score = String.format("%.2f", result.categories[0].score)

            // Set score text color based on its value
            textPaintScore.color = if (result.categories[0].score > 0.7) {
                Color.GREEN
            } else if (result.categories[0].score > 0.45) {
                Color.YELLOW
            } else {
                Color.RED
            }

            // Calculate text bounds
            textPaintLabel.getTextBounds(label, 0, label.length, bounds)
            val labelWidth = bounds.width()
            val labelHeight = bounds.height()
            textPaintScore.getTextBounds(score, 0, score.length, bounds)
            val scoreWidth = bounds.width()
            val scoreHeight = bounds.height()

            // Draw rect behind display text, extending from the top of the bounding box
            val totalTextWidth = labelWidth + scoreWidth + TEXT_PADDING * 2
            textRect.set(left, top - labelHeight - TEXT_PADDING * 4, left + totalTextWidth + BOUNDING_RECT_TEXT_PADDING * 2, top)
            canvas.drawRect(textRect, textBackgroundPaint)

            // Draw label text
            canvas.drawText(label, left + TEXT_PADDING, top - TEXT_PADDING * 2 - labelHeight / 2, textPaintLabel)

            // Draw score text
            canvas.drawText(score, left + labelWidth + TEXT_PADDING * 2, top - TEXT_PADDING * 2 - scoreHeight / 2, textPaintScore)
        }
    }

    fun setResults(detectionResults: MutableList<Detection>, imageHeight: Int, imageWidth: Int) {
        results = detectionResults
        scaleFactor = max(width * 1f / imageWidth, height * 1f / imageHeight)
    }

    companion object {
        private const val BOUNDING_RECT_TEXT_PADDING = 16
        private const val BOX_CORNER_RADIUS = 20f
        private const val TEXT_PADDING = 10f
    }
}