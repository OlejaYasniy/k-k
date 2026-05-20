package com.example.indoornavigation.ui.map

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.TypedValue
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.view.animation.LinearInterpolator
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
        set(value) { field = value; startRouteAnim(); invalidate() }
    var selectedStartRoom: Room? = null
        set(value) { field = value; invalidate() }
    var selectedEndRoom: Room? = null
        set(value) { field = value; invalidate() }
    var fontScale: Float = 1f
        set(value) { field = value; invalidate() }
    var onRoomClick: ((Room, Float, Float) -> Unit)? = null

    /* ── 2.5D isometric offset ── */
    private val ISO = 5f
    private val ISO_TRANS = 9f
    private val CORRIDOR_W = 6f

    /* ═══════════ ANIMATION STATE ═══════════ */

    // Route "marching ants" phase
    private var routePhase = 0f
    private var routeAnimator: ValueAnimator? = null

    // Marker pulse (0..1 looping)
    private var pulsePhase = 0f
    private var pulseAnimator: ValueAnimator? = null

    // Tap glow: which room is glowing, glow progress 0..1, ripple expand
    private var glowRoomId: Int? = null
    private var glowProgress = 0f
    private var glowAnimator: ValueAnimator? = null
    private var rippleX = 0f   // data-space tap point
    private var rippleY = 0f
    private var ripplePhase = 0f
    private var rippleAnimator: ValueAnimator? = null

    private fun startRouteAnim() {
        routeAnimator?.cancel()
        if (routePath.size < 2) return
        routeAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 1200; repeatCount = ValueAnimator.INFINITE; interpolator = LinearInterpolator()
            addUpdateListener { routePhase = it.animatedValue as Float; invalidate() }
            start()
        }
        // Also start pulse for markers
        if (pulseAnimator == null || pulseAnimator?.isRunning != true) {
            pulseAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
                duration = 1500; repeatCount = ValueAnimator.INFINITE; interpolator = LinearInterpolator()
                addUpdateListener { pulsePhase = it.animatedValue as Float; invalidate() }
                start()
            }
        }
    }

    private fun triggerGlow(roomId: Int, dataX: Float, dataY: Float) {
        glowRoomId = roomId; rippleX = dataX; rippleY = dataY
        glowAnimator?.cancel()
        glowAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 500
            addUpdateListener { glowProgress = it.animatedValue as Float; invalidate() }
            start()
        }
        rippleAnimator?.cancel()
        rippleAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 700
            addUpdateListener { ripplePhase = it.animatedValue as Float; invalidate() }
            start()
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        routeAnimator?.cancel(); pulseAnimator?.cancel()
        glowAnimator?.cancel(); rippleAnimator?.cancel()
    }

    /* ── Reusable paints ── */
    private val fp = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val sp = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val tp = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
    }
    private val routeShadow = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#501976D2"); style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND; strokeJoin = Paint.Join.ROUND
    }
    private val routeLine = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#2196F3"); style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND; strokeJoin = Paint.Join.ROUND
    }
    private val mStart = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#4CAF50"); style = Paint.Style.FILL }
    private val mEnd   = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#F44336"); style = Paint.Style.FILL }
    private val mWhite = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE; style = Paint.Style.FILL }
    private val mBorder= Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE; style = Paint.Style.STROKE }
    private val shadowP= Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(22, 0, 0, 0); style = Paint.Style.FILL }
    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val ripplePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }

    /* ── Theme colors ── */
    private var bgColor = Color.parseColor("#ECEFF1")
    private var surfCol = Color.WHITE
    private var outCol  = Color.parseColor("#B0BEC5")
    private var onSurf  = Color.parseColor("#37474F")
    private var corrCol = Color.parseColor("#CFD8DC")

    init { updateThemeColors() }

    fun updateThemeColors() {
        val tv = TypedValue()
        if (context.theme.resolveAttribute(android.R.attr.colorBackground, tv, true)) bgColor = tv.data
        surfCol = res(com.google.android.material.R.attr.colorSurface, Color.WHITE)
        outCol  = res(com.google.android.material.R.attr.colorOutline, Color.parseColor("#B0BEC5"))
        onSurf  = res(com.google.android.material.R.attr.colorOnSurface, Color.parseColor("#37474F"))
        corrCol = res(com.google.android.material.R.attr.colorSurfaceVariant, Color.parseColor("#CFD8DC"))
        tp.color = onSurf; invalidate()
    }

    private fun res(a: Int, fb: Int): Int { val t = TypedValue(); return if (context.theme.resolveAttribute(a, t, true)) t.data else fb }
    private fun dk(c: Int, f: Float): Int = Color.argb(Color.alpha(c), (Color.red(c)*f).toInt().coerceIn(0,255), (Color.green(c)*f).toInt().coerceIn(0,255), (Color.blue(c)*f).toInt().coerceIn(0,255))

    private fun isTrans(r: Room): Boolean {
        val n = r.name.lowercase()
        return n.contains("лестниц") || n.contains("лифт") || n.contains("эскалатор") ||
                n.contains("stair") || n.contains("elevator") || n.contains("escalat")
    }

    private fun isServiceRoom(r: Room): Boolean {
        val n = r.name.lowercase()
        return isTrans(r)
                || n.contains("туалет") || n.contains("toilet") || n.contains("wc")
                || n.contains("выход") || n.contains("exit")
    }

    /* ── Transform ── */
    private var sf = 1f; private var tx = 0f; private var ty = 0f
    private var lx = 0f; private var ly = 0f; private var dx0 = 0f; private var dy0 = 0f
    private var dMinX = 0f; private var dMinY = 0f; private var dMaxX = 1000f; private var dMaxY = 1000f

    private val sd = ScaleGestureDetector(context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(d: ScaleGestureDetector): Boolean { sf = (sf * d.scaleFactor).coerceIn(0.2f, 15f); invalidate(); return true }
    })

    private fun autoScale() {
        if (rooms.isEmpty()) return
        dMinX = rooms.minOf { it.x }; dMinY = rooms.minOf { it.y }
        dMaxX = rooms.maxOf { it.x + it.width } + ISO_TRANS + 4f
        dMaxY = rooms.maxOf { it.y + it.height } + ISO_TRANS + 4f
        if (width > 0 && height > 0) fit()
    }

    override fun onSizeChanged(w: Int, h: Int, ow: Int, oh: Int) { super.onSizeChanged(w, h, ow, oh); fit() }

    private fun fit() {
        if (width == 0 || height == 0) return
        val p = 60f; val dw = (dMaxX - dMinX).coerceAtLeast(1f); val dh = (dMaxY - dMinY).coerceAtLeast(1f)
        sf = minOf((width - p*2)/dw, (height - p*2)/dh)
        tx = (width - dw*sf)/2f - dMinX*sf; ty = (height - dh*sf)/2f - dMinY*sf
    }

    /* ═══════════════ DRAWING ═══════════════ */

    override fun onDraw(c: Canvas) {
        super.onDraw(c)
        c.drawColor(bgColor)
        c.save(); c.translate(tx, ty); c.scale(sf, sf)

        val nodeMap = nodes.associateBy { it.id }

        // ── Step 1: Draw ALL corridors first (always behind rooms) ──
        for (edge in edges) {
            val n1 = nodeMap[edge.from] ?: continue
            val n2 = nodeMap[edge.to] ?: continue
            drawCorridorClipped(c, n1, n2)
        }

        // ── Step 2: Draw rooms on top with painter's algorithm ──
        val sorted = rooms.sortedWith(compareBy<Room> { it.y }.thenBy { it.x })
        for (room in sorted) drawRoom3D(c, room)

        // ── Step 3: Glow overlay (tapped room) ──
        drawGlowOverlay(c)

        // ── Step 4: Text pass (no overlaps) ──
        val textRects = mutableListOf<RectF>()
        for (room in sorted) drawRoomText(c, room, textRects)

        drawRoute(c); drawMarkers(c)
        c.restore()
    }

    /**
     * Find the point where the line from (ax,ay) to (bx,by) exits the room rect,
     * with an extra [gap] padding outside the room. Returns the clipped point.
     * If (ax,ay) is not inside the room, returns (ax,ay) unchanged.
     */
    private fun clipToRoomEdge(ax: Float, ay: Float, bx: Float, by: Float, room: Room, gap: Float): Pair<Float, Float> {
        val x1 = room.x - gap; val y1 = room.y - gap
        val x2 = room.x + room.width + gap; val y2 = room.y + room.height + gap
        if (ax < x1 || ax > x2 || ay < y1 || ay > y2) return Pair(ax, ay) // not inside expanded rect

        val dx = bx - ax; val dy = by - ay
        var tMin = 1f
        if (dx != 0f) {
            val t1 = (x1 - ax) / dx; val t2 = (x2 - ax) / dx
            val t = if (dx > 0) t2 else t1
            if (t > 0f) tMin = minOf(tMin, t)
        }
        if (dy != 0f) {
            val t1 = (y1 - ay) / dy; val t2 = (y2 - ay) / dy
            val t = if (dy > 0) t2 else t1
            if (t > 0f) tMin = minOf(tMin, t)
        }
        return Pair(ax + dx * tMin, ay + dy * tMin)
    }

    /* ── Simple 2D corridor line ── */
    private fun drawCorridorClipped(c: Canvas, n1: Node, n2: Node) {
        val GAP = 4f   // keep a clean separation from room edges

        // Clip start
        var sx = n1.x; var sy = n1.y
        val r1 = rooms.firstOrNull { n1.x >= it.x && n1.x <= it.x+it.width && n1.y >= it.y && n1.y <= it.y+it.height }
        if (r1 != null) { val p = clipToRoomEdge(n1.x, n1.y, n2.x, n2.y, r1, GAP); sx = p.first; sy = p.second }

        // Clip end
        var ex = n2.x; var ey = n2.y
        val r2 = rooms.firstOrNull { n2.x >= it.x && n2.x <= it.x+it.width && n2.y >= it.y && n2.y <= it.y+it.height }
        if (r2 != null) { val p = clipToRoomEdge(n2.x, n2.y, n1.x, n1.y, r2, GAP); ex = p.first; ey = p.second }

        // Simple 2D line drawing
        val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = corrCol
            style = Paint.Style.STROKE
            strokeWidth = CORRIDOR_W
            strokeCap = Paint.Cap.ROUND
        }
        c.drawLine(sx, sy, ex, ey, linePaint)
    }


    /* ── 3D Room ── */
    private fun drawRoom3D(c: Canvas, room: Room) {
        val isS = room.id == selectedStartRoom?.id; val isE = room.id == selectedEndRoom?.id
        val isT = isTrans(room); val isGlow = room.id == glowRoomId && glowProgress > 0f

        // Glow "pop-up" effect: increase depth when glowing
        val glowLift = if (isGlow) (1f - Math.abs(glowProgress*2f - 1f)) * 4f else 0f
        val dep = (if (isT) ISO_TRANS else ISO) + glowLift
        val cr = 8f // rounded corners for the rooms
        val x1 = room.x; val y1 = room.y - glowLift * 0.3f  // lift up slightly
        val x2 = x1 + room.width; val y2 = y1 + room.height

        val base = when {
            isS -> Color.parseColor("#C8E6C9"); isE -> Color.parseColor("#FFCDD2")
            isT -> Color.parseColor("#C5CAE9"); else -> surfCol
        }
        val border = when {
            isS -> Color.parseColor("#4CAF50"); isE -> Color.parseColor("#F44336")
            isT -> Color.parseColor("#3F51B5"); else -> outCol
        }

        // Shadow (bigger when lifted)
        shadowP.color = Color.argb((20 + glowLift*3).toInt().coerceAtMost(50), 0, 0, 0)
        c.drawRoundRect(RectF(x1+dep+2f, y1+dep+2f, x2+dep+2f, y2+dep+2f), cr, cr, shadowP)

        // Draw the extruded 3D depth by stacking rounded rectangles
        val steps = (dep * 2f).toInt().coerceAtLeast(3)
        for (i in steps downTo 1) {
            val offset = i.toFloat() * (dep / steps)
            val tempR = RectF(x1 + offset, y1 + offset, x2 + offset, y2 + offset)
            fp.color = dk(base, 0.72f - (i.toFloat() / steps) * 0.08f) // subtle 3D side gradient
            c.drawRoundRect(tempR, cr, cr, fp)
        }

        // Draw a border at the bottom step of the extrusion
        sp.color = border
        sp.strokeWidth = 0.8f
        c.drawRoundRect(RectF(x1 + dep, y1 + dep, x2 + dep, y2 + dep), cr, cr, sp)

        // Top face
        val topR = RectF(x1, y1, x2, y2)
        fp.color = base; c.drawRoundRect(topR, cr, cr, fp)

        // Gradient polish
        val gp = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            shader = LinearGradient(x1, y1, x2, y2, Color.argb(15,255,255,255), Color.argb(10,0,0,0), Shader.TileMode.CLAMP)
        }
        c.drawRoundRect(topR, cr, cr, gp)

        // Top border
        sp.color = border; sp.strokeWidth = 1.2f; c.drawRoundRect(topR, cr, cr, sp)

        if (isT) drawTransIcon(c, room, y1)
    }

    /* ── Glow overlay + ripple ── */
    private fun drawGlowOverlay(c: Canvas) {
        val gRoom = glowRoomId ?: return
        if (glowProgress <= 0f) return
        val room = rooms.find { it.id == gRoom } ?: return

        // Neon glow ring around the room
        val glowAlpha = ((1f - glowProgress) * 80).toInt().coerceIn(0, 80)
        val glowRadius = 3f + glowProgress * 6f
        val base = when {
            room.id == selectedStartRoom?.id -> Color.parseColor("#4CAF50")
            room.id == selectedEndRoom?.id -> Color.parseColor("#F44336")
            isTrans(room) -> Color.parseColor("#3F51B5")
            else -> Color.parseColor("#2196F3")
        }

        glowPaint.color = Color.argb(glowAlpha, Color.red(base), Color.green(base), Color.blue(base))
        glowPaint.maskFilter = BlurMaskFilter(glowRadius, BlurMaskFilter.Blur.NORMAL)
        setLayerType(LAYER_TYPE_SOFTWARE, null)
        val inset = -glowRadius
        c.drawRoundRect(RectF(room.x+inset, room.y+inset, room.x+room.width-inset, room.y+room.height-inset), 5f, 5f, glowPaint)
        glowPaint.maskFilter = null

        // Ripple circles expanding from tap point
        if (ripplePhase > 0f && ripplePhase < 1f) {
            for (i in 0..2) {
                val phase = (ripplePhase - i*0.15f).coerceIn(0f, 1f)
                val radius = phase * 40f
                val alpha = ((1f - phase) * 60).toInt().coerceIn(0, 60)
                ripplePaint.color = Color.argb(alpha, Color.red(base), Color.green(base), Color.blue(base))
                ripplePaint.strokeWidth = 1.5f
                c.drawCircle(rippleX, rippleY, radius, ripplePaint)
            }
        }
    }

    private fun drawTransIcon(c: Canvas, room: Room, y1: Float) {
        val cx = room.x + room.width/2f; val cy = y1 + room.height*0.30f
        val sz = (room.height * 0.18f).coerceIn(4f, 14f)
        val n = room.name.lowercase()
        val ip = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#3F51B5"); style = Paint.Style.STROKE; strokeWidth = 1.8f; strokeCap = Paint.Cap.ROUND }
        when {
            n.contains("лестниц") || n.contains("stair") -> {
                val p = Path().apply { moveTo(cx-sz,cy+sz*0.6f); lineTo(cx-sz*0.33f,cy+sz*0.6f); lineTo(cx-sz*0.33f,cy); lineTo(cx+sz*0.33f,cy); lineTo(cx+sz*0.33f,cy-sz*0.6f); lineTo(cx+sz,cy-sz*0.6f) }
                c.drawPath(p, ip)
            }
            n.contains("лифт") || n.contains("elevator") -> {
                c.drawRoundRect(RectF(cx-sz*0.6f,cy-sz*0.7f,cx+sz*0.6f,cy+sz*0.7f), 2f, 2f, ip)
                c.drawPath(Path().apply { moveTo(cx,cy-sz*0.35f); lineTo(cx-sz*0.25f,cy); moveTo(cx,cy-sz*0.35f); lineTo(cx+sz*0.25f,cy) }, ip)
            }
            n.contains("эскалатор") || n.contains("escalat") -> {
                c.drawLine(cx-sz,cy+sz*0.5f, cx+sz,cy-sz*0.5f, ip)
                for (i in -1..1) { val sx=cx+i*sz*0.4f; val sy=cy-i*sz*0.2f; c.drawLine(sx-sz*0.15f,sy,sx+sz*0.15f,sy,ip) }
            }
        }
    }

    /* ── Room text (separate pass to avoid overlaps) ── */
    private fun drawRoomText(c: Canvas, room: Room, drawn: MutableList<RectF>) {
        if (isServiceRoom(room)) return // skip labels for service rooms (stairs, wc, exits, etc.)
        val loc = if (context.resources.configuration.locales[0].language == "en" && !room.nameEn.isNullOrBlank()) room.nameEn else room.name
        val isS = room.id == selectedStartRoom?.id; val isE = room.id == selectedEndRoom?.id
        var ts = (room.height * 0.25f * fontScale).coerceIn(6f, 48f); tp.textSize = ts
        val tw = tp.measureText(loc); if (tw > room.width*0.85f) tp.textSize = ts*(room.width*0.85f/tw)
        val fw = tp.measureText(loc); val th = tp.fontMetrics.descent - tp.fontMetrics.ascent
        val cx = room.x + room.width/2f; val yOff = if (isTrans(room)) room.height*0.15f else 0f
        val cy = if (isS||isE) room.y+room.height/2f+tp.textSize/3f+(18f/sf)+yOff else room.y+room.height/2f+tp.textSize/3f+yOff
        val r = RectF(cx-fw/2f-3f, cy-th/2f-3f, cx+fw/2f+3f, cy+th/2f+3f)
        if (drawn.any { RectF.intersects(it, r) }) {
            tp.textSize = tp.textSize*0.7f; val nw = tp.measureText(loc)
            val nr = RectF(cx-nw/2f-2f, cy-th/2f-2f, cx+nw/2f+2f, cy+th/2f+2f)
            if (!drawn.any { RectF.intersects(it, nr) }) { c.drawText(loc, cx, cy, tp); drawn.add(nr) }
        } else { c.drawText(loc, cx, cy, tp); drawn.add(r) }
    }

    /* ── POI badges ── */
    private fun drawPois(c: Canvas) {
        val r = (12f/sf).coerceIn(6f, 20f)
        val isEn = context.resources.configuration.locales[0].language == "en"
        for (p in pois) {
            val label = when (p.type) {
                "elevator" -> if (isEn) "LIFT" else "ЛИФТ"; "escalator" -> if (isEn) "ESCAL" else "ЭСКАЛ"
                "stairs" -> if (isEn) "STAIRS" else "ЛЕСТН"; "exit" -> if (isEn) "EXIT" else "ВЫХОД"
                "toilet" -> "WC"; else -> "POI"
            }
            val pp = Paint(Paint.ANTI_ALIAS_FLAG).apply { color=Color.parseColor("#37474F"); textAlign=Paint.Align.CENTER; textSize=(r*0.75f).coerceIn(8f,18f); typeface=Typeface.create("sans-serif-bold",Typeface.BOLD) }
            val tw=pp.measureText(label); val pw=(tw+r).coerceAtLeast(r*2.2f); val ph=r*1.8f
            val rect=RectF(p.x-pw/2f,p.y-ph/2f,p.x+pw/2f,p.y+ph/2f)
            val bd = 2f
            fp.color = Color.parseColor("#90A4AE"); c.drawPath(Path().apply { moveTo(p.x-pw/2f,p.y+ph/2f); lineTo(p.x+pw/2f,p.y+ph/2f); lineTo(p.x+pw/2f+bd,p.y+ph/2f+bd); lineTo(p.x-pw/2f+bd,p.y+ph/2f+bd); close() }, fp)
            fp.color = Color.parseColor("#78909C"); c.drawPath(Path().apply { moveTo(p.x+pw/2f,p.y-ph/2f); lineTo(p.x+pw/2f+bd,p.y-ph/2f+bd); lineTo(p.x+pw/2f+bd,p.y+ph/2f+bd); lineTo(p.x+pw/2f,p.y+ph/2f); close() }, fp)
            fp.color = Color.parseColor("#ECEFF1"); c.drawRoundRect(rect, ph/2f, ph/2f, fp)
            sp.color = Color.parseColor("#90A4AE"); sp.strokeWidth = 1f/sf; c.drawRoundRect(rect, ph/2f, ph/2f, sp)
            c.drawText(label, p.x, p.y+pp.textSize/3f, pp)
        }
    }

    /* ── Animated route with marching dots ── */
    private fun drawRoute(c: Canvas) {
        if (routePath.size < 2) return
        val path = Path(); path.moveTo(routePath[0].x, routePath[0].y)
        for (i in 1 until routePath.size) path.lineTo(routePath[i].x, routePath[i].y)

        // Glow shadow
        routeShadow.strokeWidth = 14f/sf; c.drawPath(path, routeShadow)

        // Animated dashes: offset shifts with routePhase
        val dashLen = 12f/sf; val gapLen = 8f/sf
        routeLine.strokeWidth = 5f/sf
        routeLine.pathEffect = DashPathEffect(floatArrayOf(dashLen, gapLen), routePhase * (dashLen + gapLen))
        c.drawPath(path, routeLine)
        routeLine.pathEffect = null

        // Marching dots along the route
        drawMarchingDots(c)
    }

    private fun drawMarchingDots(c: Canvas) {
        if (routePath.size < 2) return
        // Calculate total route length
        val segments = mutableListOf<Float>() // cumulative distances
        var total = 0f; segments.add(0f)
        for (i in 1 until routePath.size) {
            val dx = routePath[i].x - routePath[i-1].x; val dy = routePath[i].y - routePath[i-1].y
            total += Math.sqrt((dx*dx+dy*dy).toDouble()).toFloat(); segments.add(total)
        }
        if (total < 1f) return

        // Place 3-4 dots evenly, animated along the path
        val dotCount = 4
        val dotRadius = 3f / sf
        val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#1565C0"); style = Paint.Style.FILL }
        val dotGlow = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(0x40, 0x90, 0xD0, 0xFF); style = Paint.Style.FILL }

        for (di in 0 until dotCount) {
            val t = ((routePhase + di.toFloat() / dotCount) % 1f) * total
            // Find which segment this falls on
            var seg = 0
            for (s in 1 until segments.size) { if (segments[s] >= t) { seg = s-1; break } }
            val segLen = segments[seg+1] - segments[seg]
            if (segLen < 0.01f) continue
            val frac = (t - segments[seg]) / segLen
            val px = routePath[seg].x + (routePath[seg+1].x - routePath[seg].x) * frac
            val py = routePath[seg].y + (routePath[seg+1].y - routePath[seg].y) * frac
            c.drawCircle(px, py, dotRadius*2.5f, dotGlow)
            c.drawCircle(px, py, dotRadius, dotPaint)
        }
    }

    /* ── Pulsating markers ── */
    private fun drawMarkers(c: Canvas) {
        val mr = (10f/sf).coerceIn(5f, 16f); mBorder.strokeWidth = 3f/sf
        fun pin(cx: Float, cy: Float, p: Paint, isPulse: Boolean) {
            // Pulse ring
            if (isPulse && pulsePhase > 0f) {
                val pulseR = mr + mr * pulsePhase * 1.5f
                val pulseAlpha = ((1f - pulsePhase) * 100).toInt().coerceIn(0, 100)
                val pulsePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.argb(pulseAlpha, Color.red(p.color), Color.green(p.color), Color.blue(p.color))
                    style = Paint.Style.STROKE; strokeWidth = 2.5f / sf
                }
                c.drawCircle(cx, cy, pulseR, pulsePaint)
                // Second ring, slightly delayed
                val phase2 = ((pulsePhase + 0.4f) % 1f)
                val pulseR2 = mr + mr * phase2 * 1.5f
                val alpha2 = ((1f - phase2) * 60).toInt().coerceIn(0, 60)
                pulsePaint.color = Color.argb(alpha2, Color.red(p.color), Color.green(p.color), Color.blue(p.color))
                c.drawCircle(cx, cy, pulseR2, pulsePaint)
            }
            // Drop shadow
            shadowP.color = Color.argb(40, 0, 0, 0)
            c.drawCircle(cx+1.5f, cy+1.5f, mr+2f, shadowP)
            c.drawCircle(cx, cy, mr+mBorder.strokeWidth, mBorder)
            c.drawCircle(cx, cy, mr, p)
            c.drawCircle(cx, cy, mr*0.35f, mWhite)
        }
        val hasRoute = routePath.size >= 2
        selectedStartRoom?.let { pin(it.x+it.width/2f, it.y+it.height/2f, mStart, hasRoute) }
        selectedEndRoom?.let   { pin(it.x+it.width/2f, it.y+it.height/2f, mEnd, hasRoute) }
        if (hasRoute) {
            val sr = selectedStartRoom
            if (sr == null || routePath.first().floorId != sr.floorId) pin(routePath.first().x, routePath.first().y, mStart, true)
            val er = selectedEndRoom
            if (er == null || routePath.last().floorId != er.floorId) pin(routePath.last().x, routePath.last().y, mEnd, true)
        }
    }

    /* ═══════════════ TOUCH ═══════════════ */

    override fun onTouchEvent(event: MotionEvent): Boolean {
        sd.onTouchEvent(event)
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> { dx0=event.x; dy0=event.y; lx=event.x; ly=event.y }
            MotionEvent.ACTION_MOVE -> { if (event.pointerCount==1) { tx+=event.x-lx; ty+=event.y-ly; lx=event.x; ly=event.y; invalidate() } }
            MotionEvent.ACTION_UP -> { val ddx=event.x-dx0; val ddy=event.y-dy0; if (ddx*ddx+ddy*ddy<144f) handleTap(event.x, event.y) }
        }
        return true
    }

    private fun handleTap(sx: Float, sy: Float) {
        val dataX = (sx-tx)/sf; val dataY = (sy-ty)/sf
        for (room in rooms) {
            if (RectF(room.x, room.y, room.x+room.width, room.y+room.height).contains(dataX, dataY)) {
                triggerGlow(room.id, dataX, dataY) // ← fire glow + ripple animation
                onRoomClick?.invoke(room, sx, sy); break
            }
        }
    }
}