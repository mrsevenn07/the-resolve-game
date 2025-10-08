import kotlin.math.*

data class Vector2(var x: Float = 0f, var y: Float = 0f) {
    
    fun copy(): Vector2 = Vector2(x, y)
    
    fun set(x: Float, y: Float): Vector2 {
        this.x = x
        this.y = y
        return this
    }
    
    fun set(other: Vector2): Vector2 {
        this.x = other.x
        this.y = other.y
        return this
    }
    
    fun add(other: Vector2): Vector2 {
        x += other.x
        y += other.y
        return this
    }
    
    fun add(x: Float, y: Float): Vector2 {
        this.x += x
        this.y += y
        return this
    }
    
    fun subtract(other: Vector2): Vector2 {
        x -= other.x
        y -= other.y
        return this
    }
    
    fun multiply(scalar: Float): Vector2 {
        x *= scalar
        y *= scalar
        return this
    }
    
    fun multiply(other: Vector2): Vector2 {
        x *= other.x
        y *= other.y
        return this
    }
    
    fun divide(scalar: Float): Vector2 {
        if (scalar != 0f) {
            x /= scalar
            y /= scalar
        }
        return this
    }
    
    fun length(): Float = sqrt(x * x + y * y)
    
    fun lengthSquared(): Float = x * x + y * y
    
    fun normalize(): Vector2 {
        val len = length()
        if (len != 0f) {
            divide(len)
        }
        return this
    }
    
    fun normalized(): Vector2 {
        val copy = copy()
        return copy.normalize()
    }
    
    fun distanceTo(other: Vector2): Float {
        val dx = x - other.x
        val dy = y - other.y
        return sqrt(dx * dx + dy * dy)
    }
    
    fun distanceSquaredTo(other: Vector2): Float {
        val dx = x - other.x
        val dy = y - other.y
        return dx * dx + dy * dy
    }
    
    fun dot(other: Vector2): Float = x * other.x + y * other.y
    
    fun cross(other: Vector2): Float = x * other.y - y * other.x
    
    fun lerp(other: Vector2, t: Float): Vector2 {
        x += (other.x - x) * t
        y += (other.y - y) * t
        return this
    }
    
    fun clamp(min: Float, max: Float): Vector2 {
        x = x.coerceIn(min, max)
        y = y.coerceIn(min, max)
        return this
    }
    
    fun clamp(minX: Float, maxX: Float, minY: Float, maxY: Float): Vector2 {
        x = x.coerceIn(minX, maxX)
        y = y.coerceIn(minY, maxY)
        return this
    }
    
    fun clampLength(minLength: Float, maxLength: Float): Vector2 {
        val len = length()
        if (len > 0f) {
            val clampedLength = len.coerceIn(minLength, maxLength)
            multiply(clampedLength / len)
        }
        return this
    }
    
    fun angle(): Float = atan2(y, x)
    
    fun rotate(angleRadians: Float): Vector2 {
        val cos = cos(angleRadians)
        val sin = sin(angleRadians)
        val newX = x * cos - y * sin
        val newY = x * sin + y * cos
        x = newX
        y = newY
        return this
    }
    
    fun isApproximately(other: Vector2, epsilon: Float = 0.001f): Boolean {
        return abs(x - other.x) < epsilon && abs(y - other.y) < epsilon
    }
    
    fun zero(): Vector2 {
        x = 0f
        y = 0f
        return this
    }
    
    fun isZero(): Boolean = x == 0f && y == 0f
    
    operator fun plus(other: Vector2): Vector2 = Vector2(x + other.x, y + other.y)
    operator fun minus(other: Vector2): Vector2 = Vector2(x - other.x, y - other.y)
    operator fun times(scalar: Float): Vector2 = Vector2(x * scalar, y * scalar)
    operator fun div(scalar: Float): Vector2 = Vector2(x / scalar, y / scalar)
    operator fun unaryMinus(): Vector2 = Vector2(-x, -y)
    
    companion object {
        val ZERO = Vector2(0f, 0f)
        val ONE = Vector2(1f, 1f)
        val UP = Vector2(0f, -1f)
        val DOWN = Vector2(0f, 1f)
        val LEFT = Vector2(-1f, 0f)
        val RIGHT = Vector2(1f, 0f)
        
        fun fromAngle(angleRadians: Float, magnitude: Float = 1f): Vector2 {
            return Vector2(cos(angleRadians) * magnitude, sin(angleRadians) * magnitude)
        }
        
        fun lerp(a: Vector2, b: Vector2, t: Float): Vector2 {
            return Vector2(
                a.x + (b.x - a.x) * t,
                a.y + (b.y - a.y) * t
            )
        }
        
        fun distance(a: Vector2, b: Vector2): Float {
            val dx = a.x - b.x
            val dy = a.y - b.y
            return sqrt(dx * dx + dy * dy)
        }
        
        fun dot(a: Vector2, b: Vector2): Float = a.x * b.x + a.y * b.y
        
        fun cross(a: Vector2, b: Vector2): Float = a.x * b.y - a.y * b.x
    }
    
    override fun toString(): String = "Vector2($x, $y)"
}