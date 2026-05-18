package com.example.indoornavigation.ui.map

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.TypedValue
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import com.example.indoornavigation.data.model.*

class FloorCanvasView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    var rooms: List<Room> = emptyList()
        set(value) { field = value; autoScale(); invalidate() }
    var nodes: List<Node> = emptyList()
        set(value) { field = value; invalidate() }
    var edges: List<Edge> = emptyList()
        set(value) { field = value; invalidate() }
    var pois: List<Poi> = emptyList()
        set(value) { field = value; invalidate() }
    var routePath: List<Node> = emptyList()
        set(value) { field = value; invalidate() }
    var selectedStartRoom: Room? = null
        set(value) { field = value; invalidate() }
    var selectedEndRoom: Room? = null
        set(value) { field = value; invalidate() }

    
    var fontScale: Float = 1f
        set(value) { field = value; invalidate() }

    
    var onRoomClick: ((Room, Float, Float) -> Unit)? = null

    

    private val roomPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val roomStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
    }
    private val roomStartPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#E8F5E9"); style = Paint.Style.FILL
    }
    private val roomEndPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FFEBEE"); style = Paint.Style.FILL
    }
    private val roomStartStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#4CAF50"); style = Paint.Style.STROKE
    }
    private val roomEndStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#F44336"); style = Paint.Style.STROKE
    }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
    }
    private val routeShadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#401976D2"); style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND; strokeJoin = Paint.Join.ROUND
    }
    private val routePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#2196F3"); style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND; strokeJoin = Paint.Join.ROUND
    }
    private val markerStartPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#4CAF50"); style = Paint.Style.FILL
    }
    private val markerEndPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#F44336"); style = Paint.Style.FILL
    }
    private val markerWhitePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE; style = Paint.Style.FILL
    }
    private val markerBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE; style = Paint.Style.STROKE
    }
    private val poiBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FBBC04"); style = Paint.Style.STROKE
    }
    private val poiTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
    }

    
    private var canvasBgColor = Color.parseColor("#F5F5F5")

    init {
        updateThemeColors()
    }

    
    fun updateThemeColors() {
        val tv = TypedValue()
        val theme = context.theme

        
        if (theme.resolveAttribute(android.R.attr.colorBackground, tv, true)) {
            canvasBgColor = tv.data
        }

        
        val surface = resolveAttr(com.google.android.material.R.attr.colorSurface, Color.WHITE)
        val outline = resolveAttr(com.google.android.material.R.attr.colorOutline, Color.parseColor("#B0BEC5"))
        roomPaint.color = surface
        roomStroke.color = outline

        
        val onSurface = resolveAttr(com.google.android.material.R.attr.colorOnSurface, Color.parseColor("#37474F"))
        textPaint.color = onSurface

        invalidate()
    }

    private fun resolveAttr(attr: Int, fallback: Int): Int {
        val tv = TypedValue()
        return if (context.theme.resolveAttribute(attr, tv, true)) tv.data else fallback
    }

    
    private var scaleFactor = 1f
    private var translateX = 0f
    private var translateY = 0f
    private var lastTouchX = 0f
    private var lastTouchY = 0f
    private var touchDownX = 0f
    private var touchDownY = 0f

    private var dataMinX = 0f
    private var dataMinY = 0f
    private var dataMaxX = 1000f
    private var dataMaxY = 1000f

    private val scaleDetector = ScaleGestureDetector(context,
        object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(detector: ScaleGestureDetector): Boolean {
                scaleFactor = (scaleFactor * detector.scaleFactor).coerceIn(0.2f, 15f)
                invalidate()
                return true
            }
        })

    
    private fun autoScale() {
        if (rooms.isEmpty()) return
        dataMinX = rooms.minOf { it.x }
        dataMinY = rooms.minOf { it.y }
        dataMaxX = rooms.maxOf { it.x + it.width }
        dataMaxY = rooms.maxOf { it.y + it.height }
        if (width > 0 && height > 0) fitToScreen()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        fitToScreen()
    }

    private fun fitToScreen() {
        if (width == 0 || height == 0) return
        val pad = 60f
        val dw = (dataMaxX - dataMinX).coerceAtLeast(1f)
        val dh = (dataMaxY - dataMinY).coerceAtLeast(1f)
        scaleFactor = minOf((width - pad * 2) / dw, (height - pad * 2) / dh)
        translateX = (width - dw * scaleFactor) / 2f - dataMinX * scaleFactor
        translateY = (height - dh * scaleFactor) / 2f - dataMinY * scaleFactor
    }

    
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawColor(canvasBgColor)
        canvas.save()
        canvas.translate(translateX, translateY)
        canvas.scale(scaleFactor, scaleFactor)
        
        
        
        
        drawRooms(canvas)
        drawRoute(canvas)
        drawPois(canvas)
        drawMarkers(canvas)
        
        canvas.restore()
    }

    private fun drawRooms(canvas: Canvas) {
        val cornerRadius = 6f 

        for (room in rooms) {
            val isStart = room.id == selectedStartRoom?.id
            val isEnd   = room.id == selectedEndRoom?.id
            val fill = when { isStart -> roomStartPaint; isEnd -> roomEndPaint; else -> roomPaint }
            val stroke = when { isStart -> roomStartStroke; isEnd -> roomEndStroke; else -> roomStroke }

            val rect = RectF(room.x, room.y, room.x + room.width, room.y + room.height)
            
            
            canvas.drawRoundRect(rect, cornerRadius, cornerRadius, fill)
            
            
            val strokeWidth = if (isStart || isEnd) 4f / scaleFactor else 2f / scaleFactor
            stroke.strokeWidth = strokeWidth
            canvas.drawRoundRect(rect, cornerRadius, cornerRadius, stroke)

            
            var textSize = (room.height * 0.25f * fontScale).coerceIn(6f, 48f)
            textPaint.textSize = textSize
            
            
            val textWidth = textPaint.measureText(room.name)
            if (textWidth > room.width * 0.9f) {
                textPaint.textSize = textSize * ((room.width * 0.9f) / textWidth)
            }

            canvas.drawText(
                room.name,
                room.x + room.width / 2f,
                room.y + room.height / 2f + textPaint.textSize / 3f,
                textPaint
            )
        }
    }

    private fun drawRoute(canvas: Canvas) {
        if (routePath.size < 2) return
        val path = Path()
        path.moveTo(routePath[0].x, routePath[0].y)
        for (i in 1 until routePath.size) path.lineTo(routePath[i].x, routePath[i].y)
        
        routeShadowPaint.strokeWidth = 14f / scaleFactor
        canvas.drawPath(path, routeShadowPaint)
        
        routePaint.strokeWidth = 8f / scaleFactor
        canvas.drawPath(path, routePaint)
    }

    private fun drawPois(canvas: Canvas) {
        val r = (12f / scaleFactor).coerceIn(6f, 20f)
        poiBorderPaint.strokeWidth = 2f / scaleFactor
        
        for (p in pois) {
            
            val emoji = when (p.type) {
                "elevator" -> "🛗"
                "stairs" -> "🪜"
                "exit" -> "🚪"
                "toilet" -> "🚻"
                else -> "📍"
            }
            
            
            canvas.drawCircle(p.x, p.y, r, markerWhitePaint)
            canvas.drawCircle(p.x, p.y, r, poiBorderPaint)
            
            poiTextPaint.textSize = r * 1.3f
            canvas.drawText(emoji, p.x, p.y + poiTextPaint.textSize / 3f, poiTextPaint)
        }
    }

    private fun drawMarkers(canvas: Canvas) {
        val mr = (10f / scaleFactor).coerceIn(5f, 16f)
        markerBorderPaint.strokeWidth = 3f / scaleFactor
        
        fun drawPin(cx: Float, cy: Float, paint: Paint) {
            canvas.drawCircle(cx, cy, mr + markerBorderPaint.strokeWidth, markerBorderPaint)
            canvas.drawCircle(cx, cy, mr, paint)
            canvas.drawCircle(cx, cy, mr * 0.35f, markerWhitePaint)
        }
        
        selectedStartRoom?.let { r -> drawPin(r.x + r.width / 2f, r.y + r.height / 2f, markerStartPaint) }
        selectedEndRoom?.let   { r -> drawPin(r.x + r.width / 2f, r.y + r.height / 2f, markerEndPaint)   }
        if (routePath.size >= 2) {
            drawPin(routePath.first().x, routePath.first().y, markerStartPaint)
            drawPin(routePath.last().x,  routePath.last().y,  markerEndPaint)
        }
    }

    
    override fun onTouchEvent(event: MotionEvent): Boolean {
        scaleDetector.onTouchEvent(event)
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                touchDownX = event.x; touchDownY = event.y
                lastTouchX = event.x; lastTouchY = event.y
            }
            MotionEvent.ACTION_MOVE -> {
                if (event.pointerCount == 1) {
                    translateX += event.x - lastTouchX
                    translateY += event.y - lastTouchY
                    lastTouchX = event.x
                    lastTouchY = event.y
                    invalidate()
                }
            }
            MotionEvent.ACTION_UP -> {
                val dx = event.x - touchDownX
                val dy = event.y - touchDownY
                if (dx * dx + dy * dy < 144f) handleTap(event.x, event.y)
            }
        }
        return true
    }

    private fun handleTap(sx: Float, sy: Float) {
        val dataX = (sx - translateX) / scaleFactor
        val dataY = (sy - translateY) / scaleFactor
        for (room in rooms) {
            val rect = RectF(room.x, room.y, room.x + room.width, room.y + room.height)
            if (rect.contains(dataX, dataY)) {
                onRoomClick?.invoke(room, sx, sy)
                break
            }
        }
    }

}