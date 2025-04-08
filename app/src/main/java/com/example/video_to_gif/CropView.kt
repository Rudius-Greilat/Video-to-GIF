package com.example.video_to_gif

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View

class CropView(context: Context, attrs: AttributeSet?) : View(context, attrs) {

    // 初始位置
    private var rect = RectF(100f, 100f, 400f, 400f)

    // 绘制属性
    private val maskPaint = Paint().apply {
        color = Color.parseColor("#66000000") // 半透明黑色遮罩
        style = Paint.Style.FILL
    }
    private val borderPaint = Paint().apply {
        color = Color.GRAY
        style = Paint.Style.STROKE
        strokeWidth = 5f
    }
    private val handlePaint = Paint().apply {
        color = Color.WHITE
        style = Paint.Style.FILL
    }

    // 触摸交互属性
    private var isResizing = false
    private var resizeDirection = ResizeDirection.NONE
    private var lastX = 0f
    private var lastY = 0f
    private var imageWidth = 0f
    private var imageHeight = 0f
    private val minSize = 50f

    private enum class ResizeDirection {
        NONE, LEFT, RIGHT, TOP, BOTTOM,
        TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT
    }

    override fun onDraw(canvas: Canvas) {
        // 关键修改：实现裁剪框内部透明
        canvas.save()
        canvas.clipRect(0f, 0f, imageWidth, imageHeight)

        // 创建遮罩路径（全屏区域 - 裁剪框区域）
        val maskPath = Path().apply {
            addRect(0f, 0f, imageWidth, imageHeight, Path.Direction.CW)
            addRect(rect, Path.Direction.CW)
            fillType = Path.FillType.EVEN_ODD
        }

        // 绘制遮罩层
        canvas.drawPath(maskPath, maskPaint)
        canvas.restore()

        // 绘制裁剪框边框
        canvas.drawRect(rect, borderPaint)

        // 绘制拖拽手柄
        drawHandles(canvas)
    }

    private fun drawHandles(canvas: Canvas) {
        val handlePositions = arrayOf(
            PointF(rect.left, rect.top),
            PointF(rect.right, rect.top),
            PointF(rect.left, rect.bottom),
            PointF(rect.right, rect.bottom)
        )

        handlePositions.forEach {
            canvas.drawCircle(it.x, it.y, 15f, handlePaint)
        }
    }

    // 以下保持原有触摸事件处理逻辑不变
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> handleDown(event)
            MotionEvent.ACTION_MOVE -> handleMove(event)
            MotionEvent.ACTION_UP -> resetState()
        }
        return true
    }

    private fun handleDown(event: MotionEvent) {
        lastX = event.x
        lastY = event.y
        if (rect.contains(event.x, event.y)) {
            isResizing = true
            resizeDirection = getResizeDirection(event.x, event.y)
        }
    }

    private fun handleMove(event: MotionEvent) {
        if (isResizing) {
            val dx = event.x - lastX
            val dy = event.y - lastY

            when (resizeDirection) {
                ResizeDirection.LEFT -> adjustLeft(dx)
                ResizeDirection.RIGHT -> adjustRight(dx)
                ResizeDirection.TOP -> adjustTop(dy)
                ResizeDirection.BOTTOM -> adjustBottom(dy)
                ResizeDirection.TOP_LEFT -> {
                    adjustLeft(dx)
                    adjustTop(dy)
                }
                ResizeDirection.TOP_RIGHT -> {
                    adjustRight(dx)
                    adjustTop(dy)
                }
                ResizeDirection.BOTTOM_LEFT -> {
                    adjustLeft(dx)
                    adjustBottom(dy)
                }
                ResizeDirection.BOTTOM_RIGHT -> {
                    adjustRight(dx)
                    adjustBottom(dy)
                }
                else -> moveRect(dx, dy)
            }

            applyBounds()
            lastX = event.x
            lastY = event.y
            invalidate()
        }
    }

    private fun adjustLeft(dx: Float) {
        rect.left = (rect.left + dx).coerceAtLeast(0f)
    }

    private fun adjustRight(dx: Float) {
        rect.right = (rect.right + dx).coerceAtMost(imageWidth)
    }

    private fun adjustTop(dy: Float) {
        rect.top = (rect.top + dy).coerceAtLeast(0f)
    }

    private fun adjustBottom(dy: Float) {
        rect.bottom = (rect.bottom + dy).coerceAtMost(imageHeight)
    }

    private fun moveRect(dx: Float, dy: Float) {
        rect.offset(dx, dy)
        applyBounds()
    }

    private fun applyBounds() {
        rect.apply {
            left = left.coerceIn(0f, imageWidth - minSize)
            right = right.coerceIn(left + minSize, imageWidth)
            top = top.coerceIn(0f, imageHeight - minSize)
            bottom = bottom.coerceIn(top + minSize, imageHeight)
        }
    }

    private fun getResizeDirection(x: Float, y: Float): ResizeDirection {
        val handleSize = 30f
        return when {
            x < rect.left + handleSize && y < rect.top + handleSize -> ResizeDirection.TOP_LEFT
            x > rect.right - handleSize && y < rect.top + handleSize -> ResizeDirection.TOP_RIGHT
            x < rect.left + handleSize && y > rect.bottom - handleSize -> ResizeDirection.BOTTOM_LEFT
            x > rect.right - handleSize && y > rect.bottom - handleSize -> ResizeDirection.BOTTOM_RIGHT
            y < rect.top + handleSize -> ResizeDirection.TOP
            y > rect.bottom - handleSize -> ResizeDirection.BOTTOM
            x < rect.left + handleSize -> ResizeDirection.LEFT
            x > rect.right - handleSize -> ResizeDirection.RIGHT
            else -> ResizeDirection.NONE
        }
    }

    private fun resetState() {
        isResizing = false
        resizeDirection = ResizeDirection.NONE
    }

    fun setImageSize(width: Float, height: Float) {
        imageWidth = width
        imageHeight = height
        rect.set(
            width / 2 - 150f,
            height / 2 - 150f,
            width / 2 + 150f,
            height / 2 + 150f
        )
        invalidate()
    }

    fun getCropRect(): Rect {
        return Rect(
            rect.left.toInt(),
            rect.top.toInt(),
            rect.right.toInt(),
            rect.bottom.toInt()
        )
    }
}