import kotlin.math.*

data class PhysicsEntity(
    var position: Vector2 = Vector2(),
    var velocity: Vector2 = Vector2(),
    var acceleration: Vector2 = Vector2(),
    var bounds: Rectangle = Rectangle(),
    var mass: Float = 1f,
    var friction: Float = 0.8f,
    var restitution: Float = 0.3f,
    var isStatic: Boolean = false,
    var isGrounded: Boolean = false,
    var onGround: Boolean = false,
    var wasOnGround: Boolean = false,
    var groundNormal: Vector2 = Vector2(0f, -1f),
    var maxVelocity: Vector2 = Vector2(1000f, 1000f),
    var drag: Float = 0.98f
) {
    fun updateBounds() {
        bounds.setPosition(position)
    }
    
    fun applyForce(force: Vector2) {
        if (!isStatic) {
            acceleration = acceleration.add(force.divide(mass))
        }
    }
    
    fun applyImpulse(impulse: Vector2) {
        if (!isStatic) {
            velocity = velocity.add(impulse.divide(mass))
        }
    }
    
    fun setVelocity(newVelocity: Vector2) {
        velocity = newVelocity.clamp(-maxVelocity.x, maxVelocity.x, -maxVelocity.y, maxVelocity.y)
    }
}

data class CollisionInfo(
    val entity1: PhysicsEntity,
    val entity2: PhysicsEntity,
    val normal: Vector2,
    val penetration: Float,
    val contactPoint: Vector2,
    val relativeVelocity: Vector2,
    val separatingVelocity: Float
)

class PhysicsEngine(
    var gravity: Vector2 = Vector2(0f, 980f),
    var airResistance: Float = 0.99f,
    var groundFriction: Float = 0.8f,
    var maxVelocity: Float = 2000f,
    var timeStep: Float = 1f / 60f,
    var velocityIterations: Int = 8,
    var positionIterations: Int = 3
) {
    
    private val entities = mutableListOf<PhysicsEntity>()
    private val staticBodies = mutableListOf<Rectangle>()
    private val collisions = mutableListOf<CollisionInfo>()
    
    fun addEntity(entity: PhysicsEntity) {
        entities.add(entity)
    }
    
    fun removeEntity(entity: PhysicsEntity) {
        entities.remove(entity)
    }
    
    fun addStaticBody(bounds: Rectangle) {
        staticBodies.add(bounds)
    }
    
    fun clearStaticBodies() {
        staticBodies.clear()
    }
    
    fun update(deltaTime: Float) {
        val steps = (deltaTime / timeStep).toInt().coerceAtLeast(1)
        val stepDelta = deltaTime / steps
        
        repeat(steps) {
            updatePhysics(stepDelta)
        }
    }
    
    private fun updatePhysics(deltaTime: Float) {
        collisions.clear()
        
        for (entity in entities) {
            if (entity.isStatic) continue
            
            entity.wasOnGround = entity.onGround
            entity.onGround = false
            
            entity.applyForce(gravity.multiply(entity.mass))
            
            entity.velocity = entity.velocity.add(entity.acceleration.multiply(deltaTime))
            entity.acceleration = Vector2.ZERO
            
            entity.velocity = entity.velocity.multiply(airResistance)
            
            if (entity.onGround) {
                entity.velocity = Vector2(
                    entity.velocity.x * groundFriction,
                    entity.velocity.y
                )
            }
            
            entity.velocity = Vector2(
                entity.velocity.x.coerceIn(-maxVelocity, maxVelocity),
                entity.velocity.y.coerceIn(-maxVelocity, maxVelocity)
            )
            
            val movement = entity.velocity.multiply(deltaTime)
            entity.position = entity.position.add(movement)
            entity.updateBounds()
            
            checkCollisions(entity)
        }
        
        resolveCollisions()
    }
    
    private fun checkCollisions(entity: PhysicsEntity) {
        for (staticBody in staticBodies) {
            if (entity.bounds.overlaps(staticBody)) {
                val collision = calculateCollision(entity, staticBody)
                if (collision != null) {
                    collisions.add(collision)
                }
            }
        }
        
        for (other in entities) {
            if (other != entity && !other.isStatic && entity.bounds.overlaps(other.bounds)) {
                val collision = calculateEntityCollision(entity, other)
                if (collision != null) {
                    collisions.add(collision)
                }
            }
        }
    }
    
    private fun calculateCollision(entity: PhysicsEntity, staticBody: Rectangle): CollisionInfo? {
        val intersection = entity.bounds.intersection(staticBody) ?: return null
        
        val normal: Vector2
        val penetration: Float
        
        if (intersection.width < intersection.height) {
            penetration = intersection.width
            normal = if (entity.bounds.centerX < staticBody.centerX) {
                Vector2(-1f, 0f)
            } else {
                Vector2(1f, 0f)
            }
        } else {
            penetration = intersection.height
            normal = if (entity.bounds.centerY < staticBody.centerY) {
                Vector2(0f, -1f)
            } else {
                Vector2(0f, 1f)
            }
        }
        
        val contactPoint = Vector2(
            intersection.centerX,
            intersection.centerY
        )
        
        val relativeVelocity = entity.velocity
        val separatingVelocity = relativeVelocity.dot(normal)
        
        val staticEntity = PhysicsEntity(
            position = Vector2(staticBody.x, staticBody.y),
            bounds = staticBody,
            isStatic = true
        )
        
        return CollisionInfo(
            entity, staticEntity, normal, penetration, contactPoint, relativeVelocity, separatingVelocity
        )
    }
    
    private fun calculateEntityCollision(entity1: PhysicsEntity, entity2: PhysicsEntity): CollisionInfo? {
        val intersection = entity1.bounds.intersection(entity2.bounds) ?: return null
        
        val normal: Vector2
        val penetration: Float
        
        if (intersection.width < intersection.height) {
            penetration = intersection.width
            normal = if (entity1.bounds.centerX < entity2.bounds.centerX) {
                Vector2(-1f, 0f)
            } else {
                Vector2(1f, 0f)
            }
        } else {
            penetration = intersection.height
            normal = if (entity1.bounds.centerY < entity2.bounds.centerY) {
                Vector2(0f, -1f)
            } else {
                Vector2(0f, 1f)
            }
        }
        
        val contactPoint = Vector2(
            intersection.centerX,
            intersection.centerY
        )
        
        val relativeVelocity = entity1.velocity.subtract(entity2.velocity)
        val separatingVelocity = relativeVelocity.dot(normal)
        
        return CollisionInfo(
            entity1, entity2, normal, penetration, contactPoint, relativeVelocity, separatingVelocity
        )
    }
    
    private fun resolveCollisions() {
        repeat(positionIterations) {
            for (collision in collisions) {
                resolvePositionalCollision(collision)
            }
        }
        
        repeat(velocityIterations) {
            for (collision in collisions) {
                resolveVelocityCollision(collision)
            }
        }
    }
    
    private fun resolvePositionalCollision(collision: CollisionInfo) {
        val entity1 = collision.entity1
        val entity2 = collision.entity2
        
        if (entity1.isStatic && entity2.isStatic) return
        
        val totalMass = entity1.mass + entity2.mass
        val percent = 0.8f
        val slop = 0.01f
        
        val correction = collision.normal.multiply(
            maxOf(collision.penetration - slop, 0f) / totalMass * percent
        )
        
        if (!entity1.isStatic) {
            entity1.position = entity1.position.subtract(correction.multiply(entity2.mass))
            entity1.updateBounds()
        }
        
        if (!entity2.isStatic) {
            entity2.position = entity2.position.add(correction.multiply(entity1.mass))
            entity2.updateBounds()
        }
        
        if (collision.normal.y < -0.5f && !entity1.isStatic) {
            entity1.onGround = true
            entity1.groundNormal = collision.normal
        }
        
        if (collision.normal.y > 0.5f && !entity2.isStatic) {
            entity2.onGround = true
            entity2.groundNormal = collision.normal.multiply(-1f)
        }
    }
    
    private fun resolveVelocityCollision(collision: CollisionInfo) {
        val entity1 = collision.entity1
        val entity2 = collision.entity2
        
        if (entity1.isStatic && entity2.isStatic) return
        
        val relativeVelocity = entity1.velocity.subtract(entity2.velocity)
        val separatingVelocity = relativeVelocity.dot(collision.normal)
        
        if (separatingVelocity > 0) return
        
        val restitution = minOf(entity1.restitution, entity2.restitution)
        val newSeparatingVelocity = -separatingVelocity * restitution
        val deltaVelocity = newSeparatingVelocity - separatingVelocity
        
        val totalInverseMass = (if (entity1.isStatic) 0f else 1f / entity1.mass) + 
                              (if (entity2.isStatic) 0f else 1f / entity2.mass)
        
        if (totalInverseMass <= 0f) return
        
        val impulse = deltaVelocity / totalInverseMass
        val impulseVector = collision.normal.multiply(impulse)
        
        if (!entity1.isStatic) {
            entity1.velocity = entity1.velocity.add(impulseVector.divide(entity1.mass))
        }
        
        if (!entity2.isStatic) {
            entity2.velocity = entity2.velocity.subtract(impulseVector.divide(entity2.mass))
        }
    }
    
    fun raycast(start: Vector2, direction: Vector2, maxDistance: Float): RaycastHit? {
        val normalizedDirection = direction.normalize()
        val end = start.add(normalizedDirection.multiply(maxDistance))
        
        var closestHit: RaycastHit? = null
        var closestDistance = maxDistance
        
        for (staticBody in staticBodies) {
            val hit = raycastRectangle(start, end, staticBody)
            if (hit != null && hit.distance < closestDistance) {
                closestDistance = hit.distance
                closestHit = hit
            }
        }
        
        for (entity in entities) {
            if (entity.isStatic) {
                val hit = raycastRectangle(start, end, entity.bounds)
                if (hit != null && hit.distance < closestDistance) {
                    closestDistance = hit.distance
                    closestHit = hit
                }
            }
        }
        
        return closestHit
    }
    
    private fun raycastRectangle(start: Vector2, end: Vector2, rect: Rectangle): RaycastHit? {
        val direction = end.subtract(start)
        val invDir = Vector2(
            if (direction.x != 0f) 1f / direction.x else Float.MAX_VALUE,
            if (direction.y != 0f) 1f / direction.y else Float.MAX_VALUE
        )
        
        val t1 = (rect.left - start.x) * invDir.x
        val t2 = (rect.right - start.x) * invDir.x
        val t3 = (rect.top - start.y) * invDir.y
        val t4 = (rect.bottom - start.y) * invDir.y
        
        val tmin = maxOf(minOf(t1, t2), minOf(t3, t4))
        val tmax = minOf(maxOf(t1, t2), maxOf(t3, t4))
        
        if (tmax < 0 || tmin > tmax || tmin > 1f) return null
        
        val t = if (tmin >= 0) tmin else tmax
        val hitPoint = start.add(direction.multiply(t))
        val distance = start.distanceTo(hitPoint)
        
        val normal = when {
            abs(hitPoint.x - rect.left) < 0.001f -> Vector2(-1f, 0f)
            abs(hitPoint.x - rect.right) < 0.001f -> Vector2(1f, 0f)
            abs(hitPoint.y - rect.top) < 0.001f -> Vector2(0f, -1f)
            abs(hitPoint.y - rect.bottom) < 0.001f -> Vector2(0f, 1f)
            else -> Vector2(0f, -1f)
        }
        
        return RaycastHit(hitPoint, normal, distance, rect)
    }
    
    fun getEntitiesInArea(area: Rectangle): List<PhysicsEntity> {
        return entities.filter { it.bounds.overlaps(area) }
    }
    
    fun getStaticBodiesInArea(area: Rectangle): List<Rectangle> {
        return staticBodies.filter { it.overlaps(area) }
    }
    
    fun clear() {
        entities.clear()
        staticBodies.clear()
        collisions.clear()
    }
}

data class RaycastHit(
    val point: Vector2,
    val normal: Vector2,
    val distance: Float,
    val collider: Rectangle
)