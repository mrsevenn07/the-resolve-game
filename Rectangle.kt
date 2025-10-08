import kotlin.math.*

data class Rectangle(
    var x: Float = 0f,
    var y: Float = 0f,
    var width: Float = 0f,
    var height: Float = 0f
) {
    
    val left: Float get() = x
    val right: Float get() = x + width
    val top: Float get() = y
    val bottom: Float get() = y + height
    val centerX: Float get() = x + width / 2f
    val centerY: Float get() = y + height / 2f
    val center: Vector2 get() = Vector2(centerX, centerY)
    
    fun copy(): Rectangle = Rectangle(x, y, width, height)
    
    fun set(x: Float, y: Float, width: Float, height: Float): Rectangle {
        this.x = x
        this.y = y
        this.width = width
        this.height = height
        return this
    }
    
    fun set(other: Rectangle): Rectangle {
        return set(other.x, other.y, other.width, other.height)
    }
    
    fun setPosition(x: Float, y: Float): Rectangle {
        this.x = x
        this.y = y
        return this
    }
    
    fun setPosition(position: Vector2): Rectangle {
        return setPosition(position.x, position.y)
    }
    
    fun setSize(width: Float, height: Float): Rectangle {
        this.width = width
        this.height = height
        return this
    }
    
    fun setSize(size: Vector2): Rectangle {
        return setSize(size.x, size.y)
    }
    
    fun setCenter(centerX: Float, centerY: Float): Rectangle {
        this.x = centerX - width / 2f
        this.y = centerY - height / 2f
        return this
    }
    
    fun setCenter(center: Vector2): Rectangle {
        return setCenter(center.x, center.y)
    }
    
    fun translate(dx: Float, dy: Float): Rectangle {
        x += dx
        y += dy
        return this
    }
    
    fun translate(delta: Vector2): Rectangle {
        return translate(delta.x, delta.y)
    }
    
    fun contains(pointX: Float, pointY: Float): Boolean {
        return pointX >= x && pointX <= x + width && pointY >= y && pointY <= y + height
    }
    
    fun contains(point: Vector2): Boolean {
        return contains(point.x, point.y)
    }
    
    fun contains(other: Rectangle): Boolean {
        return other.x >= x && other.y >= y && 
               other.x + other.width <= x + width && 
               other.y + other.height <= y + height
    }
    
    fun overlaps(other: Rectangle): Boolean {
        return x < other.x + other.width && 
               x + width > other.x && 
               y < other.y + other.height && 
               y + height > other.y
    }
    
    fun intersects(other: Rectangle): Boolean = overlaps(other)
    
    fun intersection(other: Rectangle): Rectangle? {
        if (!overlaps(other)) return null
        
        val intersectX = maxOf(x, other.x)
        val intersectY = maxOf(y, other.y)
        val intersectWidth = minOf(x + width, other.x + other.width) - intersectX
        val intersectHeight = minOf(y + height, other.y + other.height) - intersectY
        
        return Rectangle(intersectX, intersectY, intersectWidth, intersectHeight)
    }
    
    fun union(other: Rectangle): Rectangle {
        val unionX = minOf(x, other.x)
        val unionY = minOf(y, other.y)
        val unionWidth = maxOf(x + width, other.x + other.width) - unionX
        val unionHeight = maxOf(y + height, other.y + other.height) - unionY
        
        return Rectangle(unionX, unionY, unionWidth, unionHeight)
    }
    
    fun expand(amount: Float): Rectangle {
        x -= amount
        y -= amount
        width += amount * 2f
        height += amount * 2f
        return this
    }
    
    fun expand(horizontal: Float, vertical: Float): Rectangle {
        x -= horizontal
        y -= vertical
        width += horizontal * 2f
        height += vertical * 2f
        return this
    }
    
    fun contract(amount: Float): Rectangle {
        return expand(-amount)
    }
    
    fun area(): Float = width * height
    
    fun perimeter(): Float = 2f * (width + height)
    
    fun aspectRatio(): Float = if (height != 0f) width / height else 0f
    
    fun clampPoint(point: Vector2): Vector2 {
        return Vector2(
            point.x.coerceIn(x, x + width),
            point.y.coerceIn(y, y + height)
        )
    }
    
    fun distanceToPoint(point: Vector2): Float {
        val clampedPoint = clampPoint(point)
        return point.distanceTo(clampedPoint)
    }
    
    fun isEmpty(): Boolean = width <= 0f || height <= 0f
    
    fun isValid(): Boolean = width >= 0f && height >= 0f
    
    override fun toString(): String {
        return "Rectangle(x=$x, y=$y, width=$width, height=$height)"
    }
    
    companion object {
        val EMPTY = Rectangle(0f, 0f, 0f, 0f)
        
        fun fromCenter(centerX: Float, centerY: Float, width: Float, height: Float): Rectangle {
            return Rectangle(centerX - width / 2f, centerY - height / 2f, width, height)
        }
        
        fun fromCenter(center: Vector2, size: Vector2): Rectangle {
            return fromCenter(center.x, center.y, size.x, size.y)
        }
        
        fun fromCorners(x1: Float, y1: Float, x2: Float, y2: Float): Rectangle {
            val minX = minOf(x1, x2)
            val minY = minOf(y1, y2)
            val maxX = maxOf(x1, x2)
            val maxY = maxOf(y1, y2)
            return Rectangle(minX, minY, maxX - minX, maxY - minY)
        }
        
        fun fromCorners(corner1: Vector2, corner2: Vector2): Rectangle {
            return fromCorners(corner1.x, corner1.y, corner2.x, corner2.y)
        }
    }
}