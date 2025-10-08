import kotlin.math.*

class Level(
    val id: String,
    val name: String,
    val width: Float,
    val height: Float
) {
    val platforms = mutableListOf<Platform>()
    val obstacles = mutableListOf<Obstacle>()
    val collectibles = mutableListOf<Collectible>()
    val enemies = mutableListOf<Enemy>()
    val checkpoints = mutableListOf<Checkpoint>()
    val triggers = mutableListOf<Trigger>()
    val backgroundLayers = mutableListOf<BackgroundLayer>()
    
    var gravity = Vector2(0f, -980f)
    var windForce = Vector2.ZERO
    var ambientLight = Color(0.8f, 0.8f, 0.9f, 1f)
    
    var spawnPoint = Vector2(100f, 100f)
    var exitPoint = Vector2(width - 100f, 100f)
    
    var timeLimit = 300f
    var targetScore = 1000
    
    private val particleEffects = mutableListOf<ParticleEffect>()
    
    fun initialize() {
        createDefaultLevel()
    }
    
    private fun createDefaultLevel() {
        addPlatform(0f, 0f, width, 50f, PlatformType.SOLID)
        addPlatform(200f, 150f, 200f, 20f, PlatformType.SOLID)
        addPlatform(500f, 250f, 150f, 20f, PlatformType.JUMP_THROUGH)
        addPlatform(800f, 350f, 200f, 20f, PlatformType.MOVING)
        
        addObstacle(300f, 50f, 30f, 100f, ObstacleType.SPIKE)
        addObstacle(600f, 270f, 40f, 40f, ObstacleType.SAW)
        
        addCollectible(250f, 200f, CollectibleType.COIN, 100)
        addCollectible(550f, 300f, CollectibleType.POWER_UP, 0)
        addCollectible(850f, 400f, CollectibleType.KEY, 0)
        
        addCheckpoint(400f, 170f)
        addCheckpoint(700f, 370f)
        
        addTrigger(900f, 50f, 100f, 200f, TriggerAction.LEVEL_COMPLETE)
        
        addBackgroundLayer("sky", 0f, 0.1f)
        addBackgroundLayer("mountains", 0f, 0.3f)
        addBackgroundLayer("trees", 0f, 0.6f)
    }
    
    fun addPlatform(x: Float, y: Float, width: Float, height: Float, type: PlatformType) {
        platforms.add(Platform(
            bounds = Rectangle(x, y, width, height),
            type = type,
            material = "stone"
        ))
    }
    
    fun addObstacle(x: Float, y: Float, width: Float, height: Float, type: ObstacleType) {
        obstacles.add(Obstacle(
            bounds = Rectangle(x, y, width, height),
            type = type,
            damage = when(type) {
                ObstacleType.SPIKE -> 1
                ObstacleType.SAW -> 2
                ObstacleType.LAVA -> 3
                ObstacleType.CRUSHER -> 5
            }
        ))
    }
    
    fun addCollectible(x: Float, y: Float, type: CollectibleType, value: Int) {
        collectibles.add(Collectible(
            position = Vector2(x, y),
            type = type,
            value = value,
            isCollected = false
        ))
    }
    
    fun addEnemy(enemy: Enemy) {
        enemies.add(enemy)
    }
    
    fun addCheckpoint(x: Float, y: Float) {
        checkpoints.add(Checkpoint(
            position = Vector2(x, y),
            isActivated = false
        ))
    }
    
    fun addTrigger(x: Float, y: Float, width: Float, height: Float, action: TriggerAction) {
        triggers.add(Trigger(
            bounds = Rectangle(x, y, width, height),
            action = action,
            isTriggered = false
        ))
    }
    
    fun addBackgroundLayer(textureName: String, offsetY: Float, parallaxFactor: Float) {
        backgroundLayers.add(BackgroundLayer(
            textureName = textureName,
            offsetY = offsetY,
            parallaxFactor = parallaxFactor
        ))
    }
    
    fun update(deltaTime: Float, player: Player) {
        updateMovingPlatforms(deltaTime)
        updateObstacles(deltaTime)
        updateCollectibles(deltaTime, player)
        updateEnemies(deltaTime, player)
        updateCheckpoints(player)
        updateTriggers(player)
        updateParticleEffects(deltaTime)
    }
    
    private fun updateMovingPlatforms(deltaTime: Float) {
        platforms.filter { it.type == PlatformType.MOVING }.forEach { platform ->
            platform.velocity?.let { velocity ->
                platform.bounds.x += velocity.x * deltaTime
                platform.bounds.y += velocity.y * deltaTime
                
                platform.movementBounds?.let { bounds ->
                    if (platform.bounds.x <= bounds.x || platform.bounds.x + platform.bounds.width >= bounds.x + bounds.width) {
                        velocity.x = -velocity.x
                    }
                    if (platform.bounds.y <= bounds.y || platform.bounds.y + platform.bounds.height >= bounds.y + bounds.height) {
                        velocity.y = -velocity.y
                    }
                }
            }
        }
    }
    
    private fun updateObstacles(deltaTime: Float) {
        obstacles.forEach { obstacle ->
            when (obstacle.type) {
                ObstacleType.SAW -> {
                    obstacle.rotation += 180f * deltaTime
                }
                ObstacleType.CRUSHER -> {
                    obstacle.velocity?.let { velocity ->
                        obstacle.bounds.y += velocity.y * deltaTime
                        if (obstacle.bounds.y <= obstacle.originalY - 100f || obstacle.bounds.y >= obstacle.originalY) {
                            velocity.y = -velocity.y
                        }
                    }
                }
                else -> {}
            }
        }
    }
    
    private fun updateCollectibles(deltaTime: Float, player: Player) {
        collectibles.filter { !it.isCollected }.forEach { collectible ->
            val distance = Vector2.distance(collectible.position, player.position)
            if (distance < 30f) {
                collectible.isCollected = true
                onCollectibleCollected(collectible, player)
            }
            
            collectible.bobOffset += deltaTime * 3f
            collectible.rotation += deltaTime * 90f
        }
    }
    
    private fun updateEnemies(deltaTime: Float, player: Player) {
        enemies.forEach { enemy ->
            enemy.update(deltaTime, player, this)
        }
    }
    
    private fun updateCheckpoints(player: Player) {
        checkpoints.filter { !it.isActivated }.forEach { checkpoint ->
            val distance = Vector2.distance(checkpoint.position, player.position)
            if (distance < 50f) {
                checkpoint.isActivated = true
                player.lastCheckpoint = checkpoint.position.copy()
                onCheckpointActivated(checkpoint)
            }
        }
    }
    
    private fun updateTriggers(player: Player) {
        triggers.filter { !it.isTriggered }.forEach { trigger ->
            if (trigger.bounds.contains(player.position)) {
                trigger.isTriggered = true
                onTriggerActivated(trigger, player)
            }
        }
    }
    
    private fun updateParticleEffects(deltaTime: Float) {
        particleEffects.removeAll { effect ->
            effect.update(deltaTime)
            effect.isFinished()
        }
    }
    
    private fun onCollectibleCollected(collectible: Collectible, player: Player) {
        when (collectible.type) {
            CollectibleType.COIN -> {
                player.score += collectible.value
            }
            CollectibleType.POWER_UP -> {
                player.applyPowerUp(PowerUpType.SPEED_BOOST)
            }
            CollectibleType.KEY -> {
                player.keys++
            }
            CollectibleType.HEALTH -> {
                player.heal(1)
            }
        }
    }
    
    private fun onCheckpointActivated(checkpoint: Checkpoint) {
        addParticleEffect(checkpoint.position, "checkpoint_activated")
    }
    
    private fun onTriggerActivated(trigger: Trigger, player: Player) {
        when (trigger.action) {
            TriggerAction.LEVEL_COMPLETE -> {
            }
            TriggerAction.SPAWN_ENEMY -> {
            }
            TriggerAction.ACTIVATE_PLATFORM -> {
            }
            TriggerAction.PLAY_CUTSCENE -> {
            }
        }
    }
    
    private fun addParticleEffect(position: Vector2, effectType: String) {
    }
    
    fun raycast(start: Vector2, direction: Vector2, maxDistance: Float): RaycastHit? {
        val end = start + (direction.normalized() * maxDistance)
        var closestHit: RaycastHit? = null
        var closestDistance = maxDistance
        
        platforms.forEach { platform ->
            val hit = raycastRectangle(start, end, platform.bounds)
            hit?.let {
                val distance = Vector2.distance(start, it.point)
                if (distance < closestDistance) {
                    closestDistance = distance
                    closestHit = RaycastHit(
                        point = it.point,
                        normal = it.normal,
                        distance = distance,
                        hitObject = platform
                    )
                }
            }
        }
        
        return closestHit
    }
    
    private fun raycastRectangle(start: Vector2, end: Vector2, rect: Rectangle): RaycastHit? {
        val direction = (end - start).normalized()
        val invDir = Vector2(1f / direction.x, 1f / direction.y)
        
        val t1 = (rect.x - start.x) * invDir.x
        val t2 = (rect.x + rect.width - start.x) * invDir.x
        val t3 = (rect.y - start.y) * invDir.y
        val t4 = (rect.y + rect.height - start.y) * invDir.y
        
        val tmin = maxOf(minOf(t1, t2), minOf(t3, t4))
        val tmax = minOf(maxOf(t1, t2), maxOf(t3, t4))
        
        if (tmax < 0 || tmin > tmax) return null
        
        val t = if (tmin < 0) tmax else tmin
        val hitPoint = start + (direction * t)
        
        val normal = when {
            hitPoint.x <= rect.x + 1f -> Vector2(-1f, 0f)
            hitPoint.x >= rect.x + rect.width - 1f -> Vector2(1f, 0f)
            hitPoint.y <= rect.y + 1f -> Vector2(0f, -1f)
            else -> Vector2(0f, 1f)
        }
        
        return RaycastHit(hitPoint, normal, t, null)
    }
    
    fun getWallSide(position: Vector2, bounds: Rectangle): WallSide? {
        platforms.forEach { platform ->
            if (platform.bounds.intersects(bounds)) {
                val overlapLeft = (bounds.x + bounds.width) - platform.bounds.x
                val overlapRight = (platform.bounds.x + platform.bounds.width) - bounds.x
                val overlapTop = (bounds.y + bounds.height) - platform.bounds.y
                val overlapBottom = (platform.bounds.y + platform.bounds.height) - bounds.y
                
                val minOverlap = minOf(overlapLeft, overlapRight, overlapTop, overlapBottom)
                
                return when (minOverlap) {
                    overlapLeft -> WallSide.LEFT
                    overlapRight -> WallSide.RIGHT
                    overlapTop -> WallSide.TOP
                    else -> WallSide.BOTTOM
                }
            }
        }
        return null
    }
    
    fun reset() {
        collectibles.forEach { it.isCollected = false }
        checkpoints.forEach { it.isActivated = false }
        triggers.forEach { it.isTriggered = false }
        enemies.forEach { it.reset() }
        particleEffects.clear()
    }
}

data class Platform(
    val bounds: Rectangle,
    val type: PlatformType,
    val material: String,
    var velocity: Vector2? = null,
    var movementBounds: Rectangle? = null
)

data class Obstacle(
    val bounds: Rectangle,
    val type: ObstacleType,
    val damage: Int,
    var rotation: Float = 0f,
    var velocity: Vector2? = null,
    val originalY: Float = bounds.y
)

data class Collectible(
    val position: Vector2,
    val type: CollectibleType,
    val value: Int,
    var isCollected: Boolean,
    var bobOffset: Float = 0f,
    var rotation: Float = 0f
)

data class Checkpoint(
    val position: Vector2,
    var isActivated: Boolean
)

data class Trigger(
    val bounds: Rectangle,
    val action: TriggerAction,
    var isTriggered: Boolean
)

data class BackgroundLayer(
    val textureName: String,
    val offsetY: Float,
    val parallaxFactor: Float
)

data class RaycastHit(
    val point: Vector2,
    val normal: Vector2,
    val distance: Float,
    val hitObject: Any?
)

data class Color(
    val r: Float,
    val g: Float,
    val b: Float,
    val a: Float
)

enum class PlatformType {
    SOLID,
    JUMP_THROUGH,
    MOVING,
    BREAKABLE,
    ICE,
    BOUNCY
}

enum class ObstacleType {
    SPIKE,
    SAW,
    LAVA,
    CRUSHER
}

enum class CollectibleType {
    COIN,
    POWER_UP,
    KEY,
    HEALTH
}

enum class TriggerAction {
    LEVEL_COMPLETE,
    SPAWN_ENEMY,
    ACTIVATE_PLATFORM,
    PLAY_CUTSCENE
}

enum class WallSide {
    LEFT,
    RIGHT,
    TOP,
    BOTTOM
}

class ParticleEffect {
    fun update(deltaTime: Float): Boolean {
        return true
    }
    
    fun isFinished(): Boolean {
        return false
    }
}