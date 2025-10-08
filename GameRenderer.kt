import kotlin.math.*

class GameRenderer {
    private var graphics: Graphics? = null
    private val renderLayers = mutableListOf<RenderLayer>()
    private val uiElements = mutableListOf<UIElement>()
    private val postEffects = mutableListOf<PostEffect>()
    
    private var screenWidth = 1920
    private var screenHeight = 1080
    private var cameraX = 0f
    private var cameraY = 0f
    
    private var frameCount = 0
    private var lastFpsTime = 0L
    private var fps = 0
    
    fun initialize(graphics: Graphics, width: Int, height: Int) {
        this.graphics = graphics
        this.screenWidth = width
        this.screenHeight = height
        setupRenderLayers()
    }
    
    fun beginFrame() {
        graphics?.clear(Color(0.2f, 0.3f, 0.8f, 1.0f))
        uiElements.clear()
    }
    
    fun endFrame() {
        renderAllLayers()
        renderUI()
        applyPostEffects()
        
        updateFPS()
    }
    
    fun setCamera(x: Float, y: Float) {
        cameraX = x
        cameraY = y
    }
    
    fun renderLevel(level: Level) {
        renderBackground(level)
        renderPlatforms(level)
        renderObstacles(level)
        renderCollectibles(level)
        renderEnemies(level)
        renderCheckpoints(level)
        renderTriggers(level)
    }
    
    fun renderPlayer(player: Player) {
        val sprite = Sprite(
            "player_${player.currentAnimation}",
            player.position.x - cameraX,
            player.position.y - cameraY,
            player.size.x,
            player.size.y,
            player.facingRight
        )
        addToLayer("PLAYER", sprite)
    }
    
    fun renderParticles(particles: List<ParticleEffect>) {
        particles.forEach { particle ->
            val sprite = Sprite(
                particle.textureName,
                particle.position.x - cameraX,
                particle.position.y - cameraY,
                particle.size.x,
                particle.size.y
            )
            addToLayer("PARTICLES", sprite)
        }
    }
    
    fun renderUI(score: Int, lives: Int, time: Float) {
        addUIText("Score: $score", 20f, 20f, Color.WHITE)
        addUIText("Lives: $lives", 20f, 50f, Color.WHITE)
        addUIText("Time: ${time.toInt()}", 20f, 80f, Color.WHITE)
        
        if (fps > 0) {
            addUIText("FPS: $fps", screenWidth - 100f, 20f, Color.YELLOW)
        }
    }
    
    fun renderMenu(menuItems: List<String>, selectedIndex: Int) {
        val startY = screenHeight / 2f - (menuItems.size * 30f) / 2f
        
        menuItems.forEachIndexed { index, item ->
            val color = if (index == selectedIndex) Color.YELLOW else Color.WHITE
            val y = startY + index * 40f
            addUIText(item, screenWidth / 2f - 100f, y, color)
        }
    }
    
    fun renderGameOverScreen(score: Int, highScore: Int) {
        addUIRectangle(0f, 0f, screenWidth.toFloat(), screenHeight.toFloat(), Color(0f, 0f, 0f, 0.7f))
        
        addUIText("GAME OVER", screenWidth / 2f - 100f, screenHeight / 2f - 100f, Color.RED, 48f)
        addUIText("Score: $score", screenWidth / 2f - 80f, screenHeight / 2f - 50f, Color.WHITE)
        addUIText("High Score: $highScore", screenWidth / 2f - 100f, screenHeight / 2f - 20f, Color.YELLOW)
        addUIText("Press ENTER to restart", screenWidth / 2f - 120f, screenHeight / 2f + 20f, Color.WHITE)
    }
    
    fun renderPauseScreen() {
        addUIRectangle(0f, 0f, screenWidth.toFloat(), screenHeight.toFloat(), Color(0f, 0f, 0f, 0.5f))
        addUIText("PAUSED", screenWidth / 2f - 60f, screenHeight / 2f, Color.WHITE, 36f)
        addUIText("Press P to resume", screenWidth / 2f - 80f, screenHeight / 2f + 40f, Color.WHITE)
    }
    
    fun renderLevelCompleteScreen(score: Int, timeBonus: Int, totalScore: Int) {
        addUIRectangle(0f, 0f, screenWidth.toFloat(), screenHeight.toFloat(), Color(0f, 0f, 0f, 0.7f))
        
        addUIText("LEVEL COMPLETE!", screenWidth / 2f - 120f, screenHeight / 2f - 100f, Color.GREEN, 36f)
        addUIText("Score: $score", screenWidth / 2f - 60f, screenHeight / 2f - 50f, Color.WHITE)
        addUIText("Time Bonus: $timeBonus", screenWidth / 2f - 80f, screenHeight / 2f - 20f, Color.YELLOW)
        addUIText("Total: $totalScore", screenWidth / 2f - 60f, screenHeight / 2f + 10f, Color.GREEN)
        addUIText("Press ENTER to continue", screenWidth / 2f - 100f, screenHeight / 2f + 50f, Color.WHITE)
    }
    
    fun addScreenShake(intensity: Float, duration: Float) {
        postEffects.add(PostEffect.ScreenShake(intensity, duration))
    }
    
    fun addFadeEffect(color: Color, duration: Float, fadeIn: Boolean) {
        postEffects.add(PostEffect.Fade(color, duration, fadeIn))
    }
    
    private fun setupRenderLayers() {
        renderLayers.add(RenderLayer("BACKGROUND", 0))
        renderLayers.add(RenderLayer("PLATFORMS", 1))
        renderLayers.add(RenderLayer("OBSTACLES", 2))
        renderLayers.add(RenderLayer("COLLECTIBLES", 3))
        renderLayers.add(RenderLayer("ENEMIES", 4))
        renderLayers.add(RenderLayer("PLAYER", 5))
        renderLayers.add(RenderLayer("PARTICLES", 6))
        renderLayers.add(RenderLayer("UI", 7))
    }
    
    private fun addToLayer(layerName: String, renderObject: RenderObject) {
        val layer = renderLayers.find { it.name == layerName }
        layer?.objects?.add(renderObject)
    }
    
    private fun renderBackground(level: Level) {
        level.backgroundLayers.forEach { bgLayer ->
            val parallaxX = cameraX * bgLayer.parallaxFactor
            val parallaxY = cameraY * bgLayer.parallaxFactor
            
            val sprite = Sprite(
                bgLayer.textureName,
                -parallaxX,
                -parallaxY,
                level.width,
                level.height
            )
            addToLayer("BACKGROUND", sprite)
        }
    }
    
    private fun renderPlatforms(level: Level) {
        level.platforms.forEach { platform ->
            val rect = Rectangle(
                platform.bounds.x - cameraX,
                platform.bounds.y - cameraY,
                platform.bounds.width,
                platform.bounds.height,
                platform.color
            )
            addToLayer("PLATFORMS", rect)
        }
    }
    
    private fun renderObstacles(level: Level) {
        level.obstacles.forEach { obstacle ->
            val sprite = Sprite(
                "obstacle_${obstacle.type}",
                obstacle.position.x - cameraX,
                obstacle.position.y - cameraY,
                obstacle.size.x,
                obstacle.size.y
            )
            addToLayer("OBSTACLES", sprite)
        }
    }
    
    private fun renderCollectibles(level: Level) {
        level.collectibles.forEach { collectible ->
            if (!collectible.collected) {
                val sprite = Sprite(
                    "collectible_${collectible.type}",
                    collectible.position.x - cameraX,
                    collectible.position.y - cameraY,
                    collectible.size.x,
                    collectible.size.y
                )
                addToLayer("COLLECTIBLES", sprite)
            }
        }
    }
    
    private fun renderEnemies(level: Level) {
        level.enemies.forEach { enemy ->
            if (enemy.health > 0) {
                val sprite = Sprite(
                    "enemy_${enemy.type}_${enemy.currentAnimation}",
                    enemy.position.x - cameraX,
                    enemy.position.y - cameraY,
                    enemy.size.x,
                    enemy.size.y,
                    enemy.facingRight
                )
                addToLayer("ENEMIES", sprite)
            }
        }
    }
    
    private fun renderCheckpoints(level: Level) {
        level.checkpoints.forEach { checkpoint ->
            val color = if (checkpoint.activated) Color.GREEN else Color.BLUE
            val rect = Rectangle(
                checkpoint.position.x - cameraX,
                checkpoint.position.y - cameraY,
                checkpoint.size.x,
                checkpoint.size.y,
                color
            )
            addToLayer("PLATFORMS", rect)
        }
    }
    
    private fun renderTriggers(level: Level) {
        level.triggers.forEach { trigger ->
            if (trigger.visible) {
                val rect = Rectangle(
                    trigger.bounds.x - cameraX,
                    trigger.bounds.y - cameraY,
                    trigger.bounds.width,
                    trigger.bounds.height,
                    Color(1f, 1f, 0f, 0.3f)
                )
                addToLayer("PLATFORMS", rect)
            }
        }
    }
    
    private fun renderAllLayers() {
        renderLayers.sortedBy { it.depth }.forEach { layer ->
            layer.objects.forEach { obj ->
                when (obj) {
                    is Sprite -> graphics?.drawSprite(obj)
                    is Rectangle -> graphics?.drawRectangle(obj)
                    is Line -> graphics?.drawLine(obj)
                }
            }
            layer.objects.clear()
        }
    }
    
    private fun renderUI() {
        uiElements.forEach { element ->
            when (element) {
                is UIElement.Text -> graphics?.drawText(element)
                is UIElement.Rectangle -> graphics?.drawUIRectangle(element)
                is UIElement.Sprite -> graphics?.drawUISprite(element)
            }
        }
    }
    
    private fun applyPostEffects() {
        postEffects.removeAll { effect ->
            when (effect) {
                is PostEffect.ScreenShake -> {
                    graphics?.applyScreenShake(effect.intensity)
                    effect.duration -= 1f/60f
                    effect.duration <= 0f
                }
                is PostEffect.Fade -> {
                    graphics?.applyFade(effect.color, effect.alpha)
                    effect.duration -= 1f/60f
                    effect.duration <= 0f
                }
            }
        }
    }
    
    private fun addUIText(text: String, x: Float, y: Float, color: Color, size: Float = 24f) {
        uiElements.add(UIElement.Text(text, x, y, color, size))
    }
    
    private fun addUIRectangle(x: Float, y: Float, width: Float, height: Float, color: Color) {
        uiElements.add(UIElement.Rectangle(x, y, width, height, color))
    }
    
    private fun addUISprite(textureName: String, x: Float, y: Float, width: Float, height: Float) {
        uiElements.add(UIElement.Sprite(textureName, x, y, width, height))
    }
    
    private fun updateFPS() {
        frameCount++
        val currentTime = System.currentTimeMillis()
        
        if (currentTime - lastFpsTime >= 1000) {
            fps = frameCount
            frameCount = 0
            lastFpsTime = currentTime
        }
    }
}

data class RenderLayer(
    val name: String,
    val depth: Int,
    val objects: MutableList<RenderObject> = mutableListOf()
)

sealed class RenderObject {
    data class Sprite(
        val textureName: String,
        val x: Float,
        val y: Float,
        val width: Float,
        val height: Float,
        val flipX: Boolean = false,
        val flipY: Boolean = false,
        val rotation: Float = 0f,
        val color: Color = Color.WHITE
    ) : RenderObject()
    
    data class Rectangle(
        val x: Float,
        val y: Float,
        val width: Float,
        val height: Float,
        val color: Color
    ) : RenderObject()
    
    data class Line(
        val x1: Float,
        val y1: Float,
        val x2: Float,
        val y2: Float,
        val color: Color,
        val thickness: Float = 1f
    ) : RenderObject()
}

sealed class UIElement {
    data class Text(
        val text: String,
        val x: Float,
        val y: Float,
        val color: Color,
        val size: Float = 24f,
        val font: String = "default"
    ) : UIElement()
    
    data class Rectangle(
        val x: Float,
        val y: Float,
        val width: Float,
        val height: Float,
        val color: Color
    ) : UIElement()
    
    data class Sprite(
        val textureName: String,
        val x: Float,
        val y: Float,
        val width: Float,
        val height: Float
    ) : UIElement()
}

sealed class PostEffect {
    data class ScreenShake(
        val intensity: Float,
        var duration: Float
    ) : PostEffect()
    
    data class Fade(
        val color: Color,
        var duration: Float,
        val fadeIn: Boolean,
        var alpha: Float = if (fadeIn) 0f else 1f
    ) : PostEffect()
}

class Graphics {
    fun clear(color: Color) {
    }
    
    fun drawSprite(sprite: RenderObject.Sprite) {
    }
    
    fun drawRectangle(rectangle: RenderObject.Rectangle) {
    }
    
    fun drawLine(line: RenderObject.Line) {
    }
    
    fun drawText(text: UIElement.Text) {
    }
    
    fun drawUIRectangle(rectangle: UIElement.Rectangle) {
    }
    
    fun drawUISprite(sprite: UIElement.Sprite) {
    }
    
    fun applyScreenShake(intensity: Float) {
    }
    
    fun applyFade(color: Color, alpha: Float) {
    }
}

data class Color(
    val r: Float,
    val g: Float,
    val b: Float,
    val a: Float = 1f
) {
    companion object {
        val WHITE = Color(1f, 1f, 1f, 1f)
        val BLACK = Color(0f, 0f, 0f, 1f)
        val RED = Color(1f, 0f, 0f, 1f)
        val GREEN = Color(0f, 1f, 0f, 1f)
        val BLUE = Color(0f, 0f, 1f, 1f)
        val YELLOW = Color(1f, 1f, 0f, 1f)
        val TRANSPARENT = Color(0f, 0f, 0f, 0f)
    }
}