import kotlin.math.*

class Player(
    startPosition: Vector2,
    private val inputHandler: InputHandler,
    private val assetManager: AssetManager,
    private val audioManager: AudioManager
) : PhysicsEntity(
    position = startPosition,
    bounds = Rectangle(startPosition.x, startPosition.y, 32f, 32f)
) {
    
    var health = 100
    var maxHealth = 100
    var lives = 3
    var score = 0
    
    var moveSpeed = 200f
    var jumpForce = 400f
    var maxFallSpeed = 600f
    var acceleration = 800f
    var friction = 600f
    var airResistance = 200f
    
    var coyoteTime = 0.1f
    var jumpBufferTime = 0.1f
    var wallJumpForce = Vector2(300f, 350f)
    
    private var coyoteTimer = 0f
    private var jumpBufferTimer = 0f
    private var lastGroundedTime = 0f
    private var wallJumpTimer = 0f
    private var dashTimer = 0f
    private var invulnerabilityTimer = 0f
    
    var canDoubleJump = false
    var hasDoubleJumped = false
    var canWallJump = true
    var canDash = true
    var dashCooldown = 1f
    var dashDistance = 150f
    var dashDuration = 0.2f
    
    var facingRight = true
    var isRunning = false
    var isJumping = false
    var isFalling = false
    var isWallSliding = false
    var isDashing = false
    var isAttacking = false
    var isInvulnerable = false
    
    private var currentAnimation = PlayerAnimation.IDLE
    private var animationTimer = 0f
    private var attackTimer = 0f
    private var footstepTimer = 0f
    
    var powerUps = mutableSetOf<PowerUpType>()
    var powerUpTimers = mutableMapOf<PowerUpType, Float>()
    
    var stats = PlayerStats()
    
    fun update(deltaTime: Float, level: Level) {
        updateTimers(deltaTime)
        handleInput(deltaTime)
        updatePhysics(deltaTime, level)
        updateAnimation(deltaTime)
        updatePowerUps(deltaTime)
        updateStats(deltaTime)
        
        checkLevelBounds(level)
        handleCollectibles(level)
    }
    
    private fun updateTimers(deltaTime: Float) {
        if (coyoteTimer > 0) coyoteTimer -= deltaTime
        if (jumpBufferTimer > 0) jumpBufferTimer -= deltaTime
        if (wallJumpTimer > 0) wallJumpTimer -= deltaTime
        if (dashTimer > 0) dashTimer -= deltaTime
        if (invulnerabilityTimer > 0) invulnerabilityTimer -= deltaTime
        if (attackTimer > 0) attackTimer -= deltaTime
        if (footstepTimer > 0) footstepTimer -= deltaTime
        
        isInvulnerable = invulnerabilityTimer > 0
        isDashing = dashTimer > 0
        isAttacking = attackTimer > 0
    }
    
    private fun handleInput(deltaTime: Float) {
        val movement = inputHandler.getMovementInput()
        
        if (movement.x != 0f) {
            facingRight = movement.x > 0
            isRunning = true
            
            if (isGrounded && footstepTimer <= 0) {
                audioManager.playSound("footstep", 0.3f)
                footstepTimer = 0.3f
            }
        } else {
            isRunning = false
        }
        
        if (!isDashing && !isAttacking) {
            velocity.x += movement.x * acceleration * deltaTime
        }
        
        if (inputHandler.isActionPressed(GameAction.JUMP)) {
            jumpBufferTimer = jumpBufferTime
        }
        
        if (jumpBufferTimer > 0 && canJump()) {
            jump()
            jumpBufferTimer = 0f
        }
        
        if (inputHandler.isActionPressed(GameAction.ATTACK) && !isAttacking) {
            attack()
        }
        
        if (inputHandler.isActionPressed(GameAction.DASH) && canDash && dashTimer <= 0) {
            dash()
        }
    }
    
    private fun updatePhysics(deltaTime: Float, level: Level) {
        val wasGrounded = isGrounded
        
        super.update(deltaTime, level)
        
        if (isGrounded && !wasGrounded) {
            onLand()
        }
        
        if (!isGrounded && wasGrounded) {
            coyoteTimer = coyoteTime
        }
        
        if (!isDashing) {
            if (isGrounded) {
                velocity.x *= (1f - friction * deltaTime).coerceAtLeast(0f)
            } else {
                velocity.x *= (1f - airResistance * deltaTime).coerceAtLeast(0f)
            }
        }
        
        velocity.x = velocity.x.coerceIn(-moveSpeed, moveSpeed)
        velocity.y = velocity.y.coerceAtLeast(-maxFallSpeed)
        
        updateMovementState()
        checkWallSliding(level)
    }
    
    private fun updateMovementState() {
        isJumping = velocity.y < -50f
        isFalling = velocity.y > 50f && !isGrounded
    }
    
    private fun checkWallSliding(level: Level) {
        if (!isGrounded && velocity.y > 0) {
            val wallOnRight = level.checkCollision(Rectangle(
                position.x + bounds.width,
                position.y,
                5f,
                bounds.height
            ))
            val wallOnLeft = level.checkCollision(Rectangle(
                position.x - 5f,
                position.y,
                5f,
                bounds.height
            ))
            
            isWallSliding = (wallOnRight && facingRight) || (wallOnLeft && !facingRight)
            
            if (isWallSliding) {
                velocity.y = velocity.y.coerceAtMost(100f)
            }
        } else {
            isWallSliding = false
        }
    }
    
    private fun canJump(): Boolean {
        return (isGrounded || coyoteTimer > 0) || 
               (canDoubleJump && !hasDoubleJumped && !isGrounded) ||
               (canWallJump && isWallSliding)
    }
    
    fun jump() {
        when {
            isGrounded || coyoteTimer > 0 -> {
                velocity.y = -jumpForce
                hasDoubleJumped = false
                audioManager.playSound("jump")
            }
            canDoubleJump && !hasDoubleJumped -> {
                velocity.y = -jumpForce * 0.8f
                hasDoubleJumped = true
                audioManager.playSound("jump", 0.8f, 1.2f)
            }
            canWallJump && isWallSliding -> {
                val jumpDirection = if (facingRight) -1f else 1f
                velocity.x = wallJumpForce.x * jumpDirection
                velocity.y = -wallJumpForce.y
                facingRight = !facingRight
                wallJumpTimer = 0.2f
                audioManager.playSound("jump", 0.9f, 0.9f)
            }
        }
        
        coyoteTimer = 0f
        stats.jumps++
    }
    
    private fun attack() {
        attackTimer = 0.3f
        audioManager.playSound("attack")
        stats.attacks++
    }
    
    private fun dash() {
        val dashDirection = if (facingRight) 1f else -1f
        velocity.x = dashDirection * dashDistance / dashDuration
        velocity.y = 0f
        dashTimer = dashDuration
        audioManager.playSound("dash")
        stats.dashes++
    }
    
    private fun onLand() {
        hasDoubleJumped = false
        audioManager.playSound("land", 0.5f)
    }
    
    private fun updateAnimation(deltaTime: Float) {
        val newAnimation = when {
            isDashing -> PlayerAnimation.DASH
            isAttacking -> PlayerAnimation.ATTACK
            !isGrounded && isJumping -> PlayerAnimation.JUMP
            !isGrounded && isFalling -> PlayerAnimation.FALL
            isWallSliding -> PlayerAnimation.WALL_SLIDE
            isRunning && isGrounded -> PlayerAnimation.RUN
            else -> PlayerAnimation.IDLE
        }
        
        if (newAnimation != currentAnimation) {
            currentAnimation = newAnimation
            animationTimer = 0f
        }
        
        animationTimer += deltaTime
    }
    
    private fun updatePowerUps(deltaTime: Float) {
        val expiredPowerUps = mutableListOf<PowerUpType>()
        
        powerUpTimers.forEach { (powerUp, timeLeft) ->
            val newTime = timeLeft - deltaTime
            if (newTime <= 0) {
                expiredPowerUps.add(powerUp)
            } else {
                powerUpTimers[powerUp] = newTime
            }
        }
        
        expiredPowerUps.forEach { removePowerUp(it) }
    }
    
    private fun updateStats(deltaTime: Float) {
        stats.playTime += deltaTime
        
        if (isRunning) {
            stats.distanceTraveled += kotlin.math.abs(velocity.x) * deltaTime / 100f
        }
    }
    
    private fun checkLevelBounds(level: Level) {
        if (position.y > level.bounds.height + 100) {
            takeDamage(health, "fall")
        }
    }
    
    private fun handleCollectibles(level: Level) {
        level.collectibles.removeAll { collectible ->
            if (bounds.intersects(collectible.bounds)) {
                when (collectible.type) {
                    CollectibleType.COIN -> {
                        score += collectible.value
                        audioManager.playSound("coin")
                        stats.coinsCollected++
                    }
                    CollectibleType.HEALTH -> {
                        heal(collectible.value)
                        audioManager.playSound("heal")
                    }
                    CollectibleType.POWER_UP -> {
                        addPowerUp(PowerUpType.SPEED_BOOST, 10f)
                        audioManager.playSound("powerup")
                    }
                }
                true
            } else false
        }
    }
    
    fun takeDamage(damage: Int, source: String = "unknown") {
        if (isInvulnerable) return
        
        health = (health - damage).coerceAtLeast(0)
        invulnerabilityTimer = 1f
        
        audioManager.playSound("hurt")
        stats.damageTaken += damage
        
        if (health <= 0) {
            die()
        }
    }
    
    fun heal(amount: Int) {
        health = (health + amount).coerceAtMost(maxHealth)
        stats.healthHealed += amount
    }
    
    private fun die() {
        lives--
        audioManager.playSound("death")
        stats.deaths++
        
        if (lives > 0) {
            respawn()
        } else {
            gameOver()
        }
    }
    
    private fun respawn() {
        health = maxHealth
        velocity = Vector2.ZERO
        isInvulnerable = false
        invulnerabilityTimer = 2f
        powerUps.clear()
        powerUpTimers.clear()
    }
    
    private fun gameOver() {
    }
    
    fun addPowerUp(type: PowerUpType, duration: Float) {
        powerUps.add(type)
        powerUpTimers[type] = duration
        
        when (type) {
            PowerUpType.SPEED_BOOST -> moveSpeed *= 1.5f
            PowerUpType.JUMP_BOOST -> jumpForce *= 1.3f
            PowerUpType.DOUBLE_JUMP -> canDoubleJump = true
            PowerUpType.INVINCIBILITY -> {
                isInvulnerable = true
                invulnerabilityTimer = duration
            }
        }
    }
    
    private fun removePowerUp(type: PowerUpType) {
        powerUps.remove(type)
        powerUpTimers.remove(type)
        
        when (type) {
            PowerUpType.SPEED_BOOST -> moveSpeed /= 1.5f
            PowerUpType.JUMP_BOOST -> jumpForce /= 1.3f
            PowerUpType.DOUBLE_JUMP -> canDoubleJump = false
            PowerUpType.INVINCIBILITY -> {
                if (invulnerabilityTimer <= 0) {
                    isInvulnerable = false
                }
            }
        }
    }
    
    fun getCurrentTexture(): Texture? {
        val animationName = when (currentAnimation) {
            PlayerAnimation.IDLE -> "player_idle"
            PlayerAnimation.RUN -> "player_run"
            PlayerAnimation.JUMP -> "player_jump"
            PlayerAnimation.FALL -> "player_fall"
            PlayerAnimation.ATTACK -> "player_attack"
            PlayerAnimation.DASH -> "player_dash"
            PlayerAnimation.WALL_SLIDE -> "player_wall_slide"
        }
        
        val animation = assetManager.getAnimation(animationName)
        return animation?.getCurrentFrame()?.texture ?: assetManager.getTexture("player")
    }
    
    fun reset() {
        health = maxHealth
        lives = 3
        score = 0
        velocity = Vector2.ZERO
        powerUps.clear()
        powerUpTimers.clear()
        isInvulnerable = false
        stats = PlayerStats()
        
        coyoteTimer = 0f
        jumpBufferTimer = 0f
        dashTimer = 0f
        invulnerabilityTimer = 0f
        attackTimer = 0f
        
        hasDoubleJumped = false
        isDashing = false
        isAttacking = false
        isWallSliding = false
        facingRight = true
    }
}

enum class PlayerAnimation {
    IDLE,
    RUN,
    JUMP,
    FALL,
    ATTACK,
    DASH,
    WALL_SLIDE
}

enum class PowerUpType {
    SPEED_BOOST,
    JUMP_BOOST,
    DOUBLE_JUMP,
    INVINCIBILITY
}

data class PlayerStats(
    var playTime: Float = 0f,
    var distanceTraveled: Float = 0f,
    var jumps: Int = 0,
    var attacks: Int = 0,
    var dashes: Int = 0,
    var coinsCollected: Int = 0,
    var enemiesDefeated: Int = 0,
    var damageTaken: Int = 0,
    var healthHealed: Int = 0,
    var deaths: Int = 0,
    var levelsCompleted: Int = 0
)