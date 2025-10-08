class Camera(
    var position: Vector2 = Vector2.ZERO,
    var zoom: Float = 1f,
    var rotation: Float = 0f
) {
    var target: Player? = null
    var followSpeed = 5f
    var deadZone = Rectangle(0f, 0f, 100f, 60f)
    var bounds: Rectangle? = null
    var lookAhead = Vector2(50f, 0f)
    var lookAheadSpeed = 2f
    
    private var shakeTimer = 0f
    private var shakeIntensity = 0f
    private var shakeOffset = Vector2.ZERO
    
    private var transitionTimer = 0f
    private var transitionDuration = 0f
    private var transitionStart = Vector2.ZERO
    private var transitionEnd = Vector2.ZERO
    
    var isLocked = false
    var lockedPosition = Vector2.ZERO
    
    var perspective2_5D = true
    var perspectiveOffset = Vector2(0f, -20f)
    var perspectiveScale = 0.1f
    
    fun update(deltaTime: Float) {
        if (isLocked) {
            position = lockedPosition.copy()
            return
        }
        
        if (transitionTimer > 0) {
            updateTransition(deltaTime)
            return
        }
        
        target?.let { player ->
            updateFollowTarget(player, deltaTime)
        }
        
        updateShake(deltaTime)
        applyBounds()
        
        if (perspective2_5D) {
            apply2_5DPerspective()
        }
    }
    
    private fun updateFollowTarget(player: Player, deltaTime: Float) {
        val targetPos = player.position.copy()
        
        val lookAheadTarget = if (player.facingRight) lookAhead.x else -lookAhead.x
        val currentLookAhead = position.x - targetPos.x
        val newLookAhead = lerp(currentLookAhead, lookAheadTarget, lookAheadSpeed * deltaTime)
        
        targetPos.x += newLookAhead
        
        val deadZoneWorld = Rectangle(
            position.x - deadZone.width / 2,
            position.y - deadZone.height / 2,
            deadZone.width,
            deadZone.height
        )
        
        var newPosition = position.copy()
        
        if (targetPos.x < deadZoneWorld.x) {
            newPosition.x = targetPos.x + deadZone.width / 2
        } else if (targetPos.x > deadZoneWorld.x + deadZoneWorld.width) {
            newPosition.x = targetPos.x - deadZone.width / 2
        }
        
        if (targetPos.y < deadZoneWorld.y) {
            newPosition.y = targetPos.y + deadZone.height / 2
        } else if (targetPos.y > deadZoneWorld.y + deadZoneWorld.height) {
            newPosition.y = targetPos.y - deadZone.height / 2
        }
        
        position = lerp(position, newPosition, followSpeed * deltaTime)
    }
    
    private fun updateTransition(deltaTime: Float) {
        transitionTimer -= deltaTime
        val progress = 1f - (transitionTimer / transitionDuration).coerceIn(0f, 1f)
        val easedProgress = easeInOutCubic(progress)
        
        position = lerp(transitionStart, transitionEnd, easedProgress)
        
        if (transitionTimer <= 0) {
            position = transitionEnd.copy()
            transitionTimer = 0f
        }
    }
    
    private fun updateShake(deltaTime: Float) {
        if (shakeTimer > 0) {
            shakeTimer -= deltaTime
            
            val shakeAmount = shakeIntensity * (shakeTimer / 0.5f).coerceIn(0f, 1f)
            shakeOffset.x = (Math.random().toFloat() - 0.5f) * 2f * shakeAmount
            shakeOffset.y = (Math.random().toFloat() - 0.5f) * 2f * shakeAmount
        } else {
            shakeOffset = Vector2.ZERO
        }
    }
    
    private fun applyBounds() {
        bounds?.let { bounds ->
            position.x = position.x.coerceIn(
                bounds.x + getViewportWidth() / 2,
                bounds.x + bounds.width - getViewportWidth() / 2
            )
            position.y = position.y.coerceIn(
                bounds.y + getViewportHeight() / 2,
                bounds.y + bounds.height - getViewportHeight() / 2
            )
        }
    }
    
    private fun apply2_5DPerspective() {
        val depthOffset = Vector2(
            perspectiveOffset.x * perspectiveScale,
            perspectiveOffset.y * perspectiveScale
        )
        position.add(depthOffset)
    }
    
    fun shake(intensity: Float, duration: Float = 0.5f) {
        shakeIntensity = intensity
        shakeTimer = duration
    }
    
    fun transitionTo(targetPosition: Vector2, duration: Float) {
        transitionStart = position.copy()
        transitionEnd = targetPosition.copy()
        transitionDuration = duration
        transitionTimer = duration
    }
    
    fun lockAt(position: Vector2) {
        isLocked = true
        lockedPosition = position.copy()
    }
    
    fun unlock() {
        isLocked = false
    }
    
    fun setZoom(newZoom: Float, smooth: Boolean = false) {
        if (smooth) {
        } else {
            zoom = newZoom.coerceIn(0.1f, 5f)
        }
    }
    
    fun getViewMatrix(): Matrix4 {
        val finalPosition = position + shakeOffset
        return Matrix4.createTranslation(-finalPosition.x, -finalPosition.y, 0f)
            .multiply(Matrix4.createScale(zoom, zoom, 1f))
            .multiply(Matrix4.createRotationZ(rotation))
    }
    
    fun screenToWorld(screenPos: Vector2): Vector2 {
        val viewportCenter = Vector2(getViewportWidth() / 2, getViewportHeight() / 2)
        val worldPos = (screenPos - viewportCenter) / zoom
        return worldPos + position + shakeOffset
    }
    
    fun worldToScreen(worldPos: Vector2): Vector2 {
        val viewportCenter = Vector2(getViewportWidth() / 2, getViewportHeight() / 2)
        val screenPos = (worldPos - position - shakeOffset) * zoom
        return screenPos + viewportCenter
    }
    
    fun isVisible(bounds: Rectangle): Boolean {
        val cameraBounds = Rectangle(
            position.x - getViewportWidth() / (2 * zoom),
            position.y - getViewportHeight() / (2 * zoom),
            getViewportWidth() / zoom,
            getViewportHeight() / zoom
        )
        return cameraBounds.intersects(bounds)
    }
    
    fun getViewportWidth(): Float = 1920f
    fun getViewportHeight(): Float = 1080f
    
    fun onPlayerLand() {
        shake(5f, 0.2f)
    }
    
    fun onPlayerDamage() {
        shake(15f, 0.4f)
    }
    
    fun onExplosion(intensity: Float) {
        shake(intensity * 20f, 0.6f)
    }
    
    private fun lerp(start: Vector2, end: Vector2, factor: Float): Vector2 {
        return Vector2(
            start.x + (end.x - start.x) * factor,
            start.y + (end.y - start.y) * factor
        )
    }
    
    private fun lerp(start: Float, end: Float, factor: Float): Float {
        return start + (end - start) * factor
    }
    
    private fun easeInOutCubic(t: Float): Float {
        return if (t < 0.5f) {
            4f * t * t * t
        } else {
            1f - kotlin.math.pow(-2f * t + 2f, 3f) / 2f
        }
    }
}

data class Matrix4(
    val m00: Float = 1f, val m01: Float = 0f, val m02: Float = 0f, val m03: Float = 0f,
    val m10: Float = 0f, val m11: Float = 1f, val m12: Float = 0f, val m13: Float = 0f,
    val m20: Float = 0f, val m21: Float = 0f, val m22: Float = 1f, val m23: Float = 0f,
    val m30: Float = 0f, val m31: Float = 0f, val m32: Float = 0f, val m33: Float = 1f
) {
    companion object {
        fun createTranslation(x: Float, y: Float, z: Float): Matrix4 {
            return Matrix4(m03 = x, m13 = y, m23 = z)
        }
        
        fun createScale(x: Float, y: Float, z: Float): Matrix4 {
            return Matrix4(m00 = x, m11 = y, m22 = z)
        }
        
        fun createRotationZ(angle: Float): Matrix4 {
            val cos = kotlin.math.cos(angle)
            val sin = kotlin.math.sin(angle)
            return Matrix4(
                m00 = cos.toFloat(), m01 = -sin.toFloat(),
                m10 = sin.toFloat(), m11 = cos.toFloat()
            )
        }
    }
    
    fun multiply(other: Matrix4): Matrix4 {
        return Matrix4(
            m00 = m00 * other.m00 + m01 * other.m10 + m02 * other.m20 + m03 * other.m30,
            m01 = m00 * other.m01 + m01 * other.m11 + m02 * other.m21 + m03 * other.m31,
            m02 = m00 * other.m02 + m01 * other.m12 + m02 * other.m22 + m03 * other.m32,
            m03 = m00 * other.m03 + m01 * other.m13 + m02 * other.m23 + m03 * other.m33,
            
            m10 = m10 * other.m00 + m11 * other.m10 + m12 * other.m20 + m13 * other.m30,
            m11 = m10 * other.m01 + m11 * other.m11 + m12 * other.m21 + m13 * other.m31,
            m12 = m10 * other.m02 + m11 * other.m12 + m12 * other.m22 + m13 * other.m32,
            m13 = m10 * other.m03 + m11 * other.m13 + m12 * other.m23 + m13 * other.m33,
            
            m20 = m20 * other.m00 + m21 * other.m10 + m22 * other.m20 + m23 * other.m30,
            m21 = m20 * other.m01 + m21 * other.m11 + m22 * other.m21 + m23 * other.m31,
            m22 = m20 * other.m02 + m21 * other.m12 + m22 * other.m22 + m23 * other.m32,
            m23 = m20 * other.m03 + m21 * other.m13 + m22 * other.m23 + m23 * other.m33,
            
            m30 = m30 * other.m00 + m31 * other.m10 + m32 * other.m20 + m33 * other.m30,
            m31 = m30 * other.m01 + m31 * other.m11 + m32 * other.m21 + m33 * other.m31,
            m32 = m30 * other.m02 + m31 * other.m12 + m32 * other.m22 + m33 * other.m32,
            m33 = m30 * other.m03 + m31 * other.m13 + m32 * other.m23 + m33 * other.m33
        )
    }
}