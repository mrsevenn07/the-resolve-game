import kotlin.math.*

abstract class Enemy(
    position: Vector2,
    val enemyType: EnemyType,
    val maxHealth: Int = 3,
    val damage: Int = 1,
    val moveSpeed: Float = 100f
) : PhysicsEntity(position, Vector2(32f, 32f)) {
    
    var health = maxHealth
    var scoreValue = 100
    
    var aiState = EnemyAIState.PATROL
    private var stateTimer = 0f
    private var lastPlayerPosition = Vector2()
    private var playerDetected = false
    private var detectionRange = 150f
    private var attackRange = 40f
    
    protected var jumpForce = 300f
    protected var facingRight = true
    
    private var patrolStartX = position.x
    private var patrolDistance = 100f
    private var patrolDirection = 1f
    
    private var attackCooldown = 0f
    private val attackCooldownDuration = 1.5f
    private var isAttacking = false
    private var attackTimer = 0f
    private val attackDuration = 0.5f
    
    private var invulnerable = false
    private var invulnerabilityTimer = 0f
    private val invulnerabilityDuration = 0.3f
    private var knockbackVelocity = Vector2()
    
    private var currentAnimation = EnemyAnimation.IDLE
    private var animationTimer = 0f
    
    fun update(deltaTime: Float, player: Player, level: Level) {
        updateTimers(deltaTime)
        updateAI(deltaTime, player, level)
        updateAnimation(deltaTime)
        updateBounds()
        
        if (knockbackVelocity.length() > 0) {
            velocity.x += knockbackVelocity.x
            velocity.y += knockbackVelocity.y
            knockbackVelocity.scale(0.9f)
            
            if (knockbackVelocity.length() < 10f) {
                knockbackVelocity.zero()
            }
        }
        
        isGrounded = false
    }
    
    private fun updateTimers(deltaTime: Float) {
        stateTimer += deltaTime
        animationTimer += deltaTime
        
        if (attackCooldown > 0) {
            attackCooldown -= deltaTime
        }
        
        if (attackTimer > 0) {
            attackTimer -= deltaTime
            if (attackTimer <= 0) {
                isAttacking = false
            }
        }
        
        if (invulnerabilityTimer > 0) {
            invulnerabilityTimer -= deltaTime
            if (invulnerabilityTimer <= 0) {
                invulnerable = false
            }
        }
    }
    
    private fun updateAI(deltaTime: Float, player: Player, level: Level) {
        val distanceToPlayer = Vector2.distance(position, player.position)
        val playerInRange = distanceToPlayer <= detectionRange
        
        if (playerInRange && canSeePlayer(player, level)) {
            playerDetected = true
            lastPlayerPosition.set(player.position)
        } else if (distanceToPlayer > detectionRange * 1.5f) {
            playerDetected = false
        }
        
        when (aiState) {
            EnemyAIState.PATROL -> updatePatrolState(deltaTime, player)
            EnemyAIState.CHASE -> updateChaseState(deltaTime, player, level)
            EnemyAIState.ATTACK -> updateAttackState(deltaTime, player)
            EnemyAIState.STUNNED -> updateStunnedState(deltaTime)
            EnemyAIState.DEAD -> updateDeadState(deltaTime)
        }
        
        if (velocity.x > 0) {
            facingRight = true
        } else if (velocity.x < 0) {
            facingRight = false
        }
    }
    
    private fun updatePatrolState(deltaTime: Float, player: Player) {
        if (playerDetected) {
            changeState(EnemyAIState.CHASE)
            return
        }
        
        val targetX = patrolStartX + (patrolDirection * patrolDistance)
        val distanceToTarget = abs(position.x - targetX)
        
        if (distanceToTarget < 10f || !isGrounded) {
            patrolDirection *= -1f
        }
        
        velocity.x = patrolDirection * moveSpeed * 0.5f
        currentAnimation = EnemyAnimation.WALK
    }
    
    private fun updateChaseState(deltaTime: Float, player: Player, level: Level) {
        val distanceToPlayer = Vector2.distance(position, player.position)
        
        if (!playerDetected && stateTimer > 3f) {
            changeState(EnemyAIState.PATROL)
            return
        }
        
        if (distanceToPlayer <= attackRange && attackCooldown <= 0) {
            changeState(EnemyAIState.ATTACK)
            return
        }
        
        val directionToPlayer = if (player.position.x > position.x) 1f else -1f
        velocity.x = directionToPlayer * moveSpeed
        
        if (isGrounded && shouldJump(level, directionToPlayer)) {
            jump()
        }
        
        currentAnimation = EnemyAnimation.RUN
    }
    
    private fun updateAttackState(deltaTime: Float, player: Player) {
        if (!isAttacking) {
            startAttack()
        }
        
        if (attackTimer <= 0) {
            attackCooldown = attackCooldownDuration
            changeState(EnemyAIState.CHASE)
        }
        
        velocity.x = 0f
        currentAnimation = EnemyAnimation.ATTACK
    }
    
    private fun updateStunnedState(deltaTime: Float) {
        velocity.x *= 0.9f
        
        if (stateTimer > 1f) {
            changeState(EnemyAIState.PATROL)
        }
        
        currentAnimation = EnemyAnimation.STUNNED
    }
    
    private fun updateDeadState(deltaTime: Float) {
        velocity.x = 0f
        currentAnimation = EnemyAnimation.DEAD
    }
    
    private fun changeState(newState: EnemyAIState) {
        aiState = newState
        stateTimer = 0f
    }
    
    private fun canSeePlayer(player: Player, level: Level): Boolean {
        val direction = Vector2(
            player.position.x - position.x,
            player.position.y - position.y
        ).normalized()
        
        val hit = level.raycast(position, direction, detectionRange)
        return hit == null || hit.distance > Vector2.distance(position, player.position)
    }
    
    private fun shouldJump(level: Level, direction: Float): Boolean {
        val checkDistance = 40f
        val checkX = position.x + (direction * checkDistance)
        val checkY = position.y + 10f
        
        val wallAhead = level.isPointInSolid(checkX, checkY)
        val groundAhead = level.getGroundY(checkX)
        val gapAhead = groundAhead > position.y + 50f
        
        return wallAhead || gapAhead
    }
    
    private fun jump() {
        if (isGrounded) {
            velocity.y = -jumpForce
            isGrounded = false
        }
    }
    
    private fun startAttack() {
        isAttacking = true
        attackTimer = attackDuration
    }
    
    fun takeDamage(damage: Int, knockbackDirection: Vector2 = Vector2()) {
        if (invulnerable || aiState == EnemyAIState.DEAD) return
        
        health -= damage
        invulnerable = true
        invulnerabilityTimer = invulnerabilityDuration
        
        if (knockbackDirection.length() > 0) {
            knockbackVelocity.set(knockbackDirection.normalized() * 200f)
        }
        
        if (health <= 0) {
            die()
        } else {
            changeState(EnemyAIState.STUNNED)
        }
    }
    
    private fun die() {
        health = 0
        changeState(EnemyAIState.DEAD)
        velocity.x = 0f
    }
    
    fun canDamagePlayer(): Boolean {
        return aiState != EnemyAIState.DEAD && !invulnerable
    }
    
    fun getAttackBounds(): Rectangle? {
        if (!isAttacking) return null
        
        val attackWidth = 50f
        val attackHeight = bounds.height
        val attackX = if (facingRight) {
            bounds.x + bounds.width
        } else {
            bounds.x - attackWidth
        }
        
        return Rectangle(attackX, bounds.y, attackWidth, attackHeight)
    }
    
    private fun updateAnimation(deltaTime: Float) {
    }
    
    fun isAlive(): Boolean = aiState != EnemyAIState.DEAD
    
    fun getCurrentAnimation(): EnemyAnimation = currentAnimation
    
    fun isFacingRight(): Boolean = facingRight
    
    fun reset() {
        health = maxHealth
        aiState = EnemyAIState.PATROL
        position.set(patrolStartX, position.y)
        velocity.zero()
        invulnerable = false
        playerDetected = false
    }
}

class WalkerEnemy(position: Vector2) : Enemy(position, EnemyType.WALKER, 2, 1, 60f) {
    init {
        bounds = Rectangle(position.x, position.y, 24f, 24f)
        scoreValue = 100
    }
}

class FlyerEnemy(position: Vector2) : Enemy(position, EnemyType.FLYER, 1, 1, 100f) {
    private var hoverHeight = position.y
    private var swoopTarget = Vector2()
    private var isSwooping = false
    
    init {
        bounds = Rectangle(position.x, position.y, 28f, 20f)
        scoreValue = 150
    }
    
    override fun updateBounds() {
        super.updateBounds()
        
        if (!isSwooping) {
            position.y = hoverHeight + sin(animationTimer * 2f) * 10f
        }
    }
}

class JumperEnemy(position: Vector2) : Enemy(position, EnemyType.JUMPER, 3, 2, 40f) {
    private var jumpCooldown = 0f
    private val jumpCooldownDuration = 2f
    
    init {
        bounds = Rectangle(position.x, position.y, 30f, 30f)
        jumpForce = 400f
        scoreValue = 200
    }
    
    private fun updateJumperAI(deltaTime: Float, player: Player) {
        if (jumpCooldown > 0) {
            jumpCooldown -= deltaTime
        }
        
        if (playerDetected && jumpCooldown <= 0 && isGrounded) {
            val directionToPlayer = Vector2(
                player.position.x - position.x,
                player.position.y - position.y
            ).normalized()
            
            velocity.x = directionToPlayer.x * moveSpeed * 2f
            velocity.y = -jumpForce
            jumpCooldown = jumpCooldownDuration
            isGrounded = false
        }
    }
}

enum class EnemyType {
    WALKER,
    FLYER,
    JUMPER,
    SHOOTER,
    BOSS
}

enum class EnemyAIState {
    PATROL,
    CHASE,
    ATTACK,
    STUNNED,
    DEAD
}

enum class EnemyAnimation {
    IDLE,
    WALK,
    RUN,
    ATTACK,
    STUNNED,
    DEAD
}

object EnemyFactory {
    fun createEnemy(type: EnemyType, position: Vector2): Enemy {
        return when (type) {
            EnemyType.WALKER -> WalkerEnemy(position)
            EnemyType.FLYER -> FlyerEnemy(position)
            EnemyType.JUMPER -> JumperEnemy(position)
            else -> WalkerEnemy(position)
        }
    }
}