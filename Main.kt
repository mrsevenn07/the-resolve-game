/**
 * 2.5D Platformer Game
 * 
 * - Scoring/progress tracking system
 * - Sound and music integration
 */

fun main() {
    println("Starting 2.5D Platformer Game...")
    
    try {
        // Create and initialize the game
        val game = PlatformerGame(
            screenWidth = 1280,
            screenHeight = 720,
            title = "2.5D Platformer"
        )
        
        // Initialize all game systems
        game.initialize()
        
        // Start the main game loop
        game.run()
        
    } catch (e: Exception) {
        println("Error starting game: ${e.message}")
        e.printStackTrace()
    }
}

/**
 * Game configuration constants
 */
object GameConfig {
    // Display settings
    const val SCREEN_WIDTH = 1280
    const val SCREEN_HEIGHT = 720
    const val TARGET_FPS = 60
    const val VSYNC_ENABLED = true
    
    // Game settings
    const val GAME_TITLE = "2.5D Platformer"
    const val GAME_VERSION = "1.0.0"
    
    // Physics settings
    const val GRAVITY = 980f // pixels per second squared
    const val TERMINAL_VELOCITY = 500f
    const val PHYSICS_TIMESTEP = 1f / 60f // Fixed timestep for consistent physics
    
    // Player settings
    const val PLAYER_SPEED = 200f
    const val PLAYER_JUMP_FORCE = 400f
    const val PLAYER_MAX_HEALTH = 100
    const val PLAYER_STARTING_LIVES = 3
    
    // Camera settings
    const val CAMERA_FOLLOW_SPEED = 5f
    const val CAMERA_DEAD_ZONE_WIDTH = 100f
    const val CAMERA_DEAD_ZONE_HEIGHT = 50f
    const val CAMERA_LOOK_AHEAD_DISTANCE = 150f
    
    // Audio settings
    const val MASTER_VOLUME = 1.0f
    const val SFX_VOLUME = 0.8f
    const val MUSIC_VOLUME = 0.6f
    
    // Level settings
    const val TOTAL_LEVELS = 5
    const val COINS_PER_LEVEL = 10
    const val ENEMIES_PER_LEVEL = 5
    
    // Asset paths
    const val ASSETS_ROOT = "assets"
    const val SPRITES_PATH = "$ASSETS_ROOT/sprites"
    const val SOUNDS_PATH = "$ASSETS_ROOT/sounds"
    const val MUSIC_PATH = "$ASSETS_ROOT/music"
    const val LEVELS_PATH = "$ASSETS_ROOT/levels"
    
    // Debug settings
    const val DEBUG_MODE = false
    const val SHOW_FPS = true
    const val SHOW_COLLISION_BOXES = false
    const val SHOW_CAMERA_BOUNDS = false
}

/**
 * Game statistics and metrics
 */
object GameStats {
    var totalPlayTime: Long = 0
    var totalJumps: Int = 0
    var totalCoinsCollected: Int = 0
    var totalEnemiesDefeated: Int = 0
    var totalDeaths: Int = 0
    var levelsCompleted: Int = 0
    var highScore: Int = 0
    var bestTime: Long = Long.MAX_VALUE
    
    fun reset() {
        totalPlayTime = 0
        totalJumps = 0
        totalCoinsCollected = 0
        totalEnemiesDefeated = 0
        totalDeaths = 0
        levelsCompleted = 0
        // Don't reset high score and best time
    }
    
    fun saveStats() {
        // In a real implementation, this would save to a file or database
        println("Saving game statistics...")
        println("Total Play Time: ${totalPlayTime / 1000}s")
        println("Total Jumps: $totalJumps")
        println("Total Coins: $totalCoinsCollected")
        println("Total Enemies Defeated: $totalEnemiesDefeated")
        println("Total Deaths: $totalDeaths")
        println("Levels Completed: $levelsCompleted")
        println("High Score: $highScore")
        println("Best Time: ${if (bestTime != Long.MAX_VALUE) bestTime / 1000 else "N/A"}s")
    }
    
    fun loadStats() {
        // In a real implementation, this would load from a file or database
        println("Loading game statistics...")
    }
}

/**
 * Performance monitor for tracking game performance
 */
class PerformanceMonitor {
    private var frameCount = 0
    private var lastFpsTime = 0L
    private var currentFps = 0
    private var frameTimeHistory = mutableListOf<Long>()
    private val maxHistorySize = 60
    
    private var updateTimeMs = 0L
    private var renderTimeMs = 0L
    private var totalFrameTimeMs = 0L
    
    fun startFrame() {
        totalFrameTimeMs = System.currentTimeMillis()
    }
    
    fun startUpdate() {
        updateTimeMs = System.currentTimeMillis()
    }
    
    fun endUpdate() {
        updateTimeMs = System.currentTimeMillis() - updateTimeMs
    }
    
    fun startRender() {
        renderTimeMs = System.currentTimeMillis()
    }
    
    fun endRender() {
        renderTimeMs = System.currentTimeMillis() - renderTimeMs
    }
    
    fun endFrame() {
        totalFrameTimeMs = System.currentTimeMillis() - totalFrameTimeMs
        
        frameTimeHistory.add(totalFrameTimeMs)
        if (frameTimeHistory.size > maxHistorySize) {
            frameTimeHistory.removeAt(0)
        }
        
        frameCount++
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastFpsTime >= 1000) {
            currentFps = frameCount
            frameCount = 0
            lastFpsTime = currentTime
            
            if (GameConfig.DEBUG_MODE) {
                printPerformanceStats()
            }
        }
    }
    
    fun getCurrentFps(): Int = currentFps
    
    fun getAverageFrameTime(): Float {
        return if (frameTimeHistory.isNotEmpty()) {
            frameTimeHistory.average().toFloat()
        } else 0f
    }
    
    fun getUpdateTime(): Long = updateTimeMs
    fun getRenderTime(): Long = renderTimeMs
    fun getTotalFrameTime(): Long = totalFrameTimeMs
    
    private fun printPerformanceStats() {
        val avgFrameTime = getAverageFrameTime()
        println("Performance Stats:")
        println("  FPS: $currentFps")
        println("  Avg Frame Time: ${String.format("%.2f", avgFrameTime)}ms")
        println("  Update Time: ${updateTimeMs}ms")
        println("  Render Time: ${renderTimeMs}ms")
        println("  Total Frame Time: ${totalFrameTimeMs}ms")
    }
}

/**
 * Game launcher with error handling and initialization
 */
class GameLauncher {
    private val performanceMonitor = PerformanceMonitor()
    
    fun launch() {
        try {
            // Initialize game statistics
            GameStats.loadStats()
            
            // Create the game instance
            val game = createGame()
            
            // Run the game with performance monitoring
            runGameWithMonitoring(game)
            
        } catch (e: Exception) {
            handleGameError(e)
        } finally {
            cleanup()
        }
    }
    
    private fun createGame(): PlatformerGame {
        println("Initializing 2.5D Platformer Game v${GameConfig.GAME_VERSION}")
        println("Screen Resolution: ${GameConfig.SCREEN_WIDTH}x${GameConfig.SCREEN_HEIGHT}")
        println("Target FPS: ${GameConfig.TARGET_FPS}")
        
        return PlatformerGame(
            screenWidth = GameConfig.SCREEN_WIDTH,
            screenHeight = GameConfig.SCREEN_HEIGHT,
            title = GameConfig.GAME_TITLE
        )
    }
    
    private fun runGameWithMonitoring(game: PlatformerGame) {
        game.initialize()
        
        println("Game initialized successfully!")
        println("Starting main game loop...")
        
        // Main game loop with performance monitoring
        var running = true
        var lastTime = System.nanoTime()
        var accumulator = 0.0
        val targetFrameTime = 1.0 / GameConfig.TARGET_FPS
        
        while (running) {
            performanceMonitor.startFrame()
            
            val currentTime = System.nanoTime()
            val deltaTime = (currentTime - lastTime) / 1_000_000_000.0
            lastTime = currentTime
            
            accumulator += deltaTime
            
            // Fixed timestep update loop
            performanceMonitor.startUpdate()
            while (accumulator >= targetFrameTime) {
                running = game.update(targetFrameTime.toFloat())
                accumulator -= targetFrameTime
            }
            performanceMonitor.endUpdate()
            
            // Render with interpolation
            performanceMonitor.startRender()
            val interpolation = (accumulator / targetFrameTime).toFloat()
            game.render(interpolation)
            performanceMonitor.endRender()
            
            performanceMonitor.endFrame()
            
            // Handle input
            running = running && game.handleInput()
            
            // Limit frame rate if needed
            if (!GameConfig.VSYNC_ENABLED) {
                limitFrameRate(targetFrameTime)
            }
        }
        
        game.shutdown()
    }
    
    private fun limitFrameRate(targetFrameTime: Double) {
        val frameTime = performanceMonitor.getTotalFrameTime() / 1000.0
        val sleepTime = targetFrameTime - frameTime
        
        if (sleepTime > 0) {
            Thread.sleep((sleepTime * 1000).toLong())
        }
    }
    
    private fun handleGameError(e: Exception) {
        println("Fatal game error occurred:")
        println("Error: ${e.message}")
        e.printStackTrace()
        
        // Log error details
        println("\nGame State Information:")
        println("Performance Stats:")
        println("  Last FPS: ${performanceMonitor.getCurrentFps()}")
        println("  Avg Frame Time: ${performanceMonitor.getAverageFrameTime()}ms")
        
        // Save crash report
        saveCrashReport(e)
    }
    
    private fun saveCrashReport(e: Exception) {
        try {
            val timestamp = System.currentTimeMillis()
            val crashReport = buildString {
                appendLine("2.5D Platformer Crash Report")
                appendLine("Timestamp: $timestamp")
                appendLine("Game Version: ${GameConfig.GAME_VERSION}")
                appendLine("Error: ${e.message}")
                appendLine("Stack Trace:")
                e.stackTrace.forEach { appendLine("  $it") }
                appendLine("\nGame Statistics:")
                appendLine("  Total Play Time: ${GameStats.totalPlayTime}ms")
                appendLine("  Current Level: ${GameStats.levelsCompleted + 1}")
                appendLine("  Score: ${GameStats.highScore}")
            }
            
            // In a real implementation, this would write to a file
            println("Crash report generated:")
            println(crashReport)
            
        } catch (reportError: Exception) {
            println("Failed to generate crash report: ${reportError.message}")
        }
    }
    
    private fun cleanup() {
        try {
            GameStats.saveStats()
            println("Game cleanup completed.")
        } catch (e: Exception) {
            println("Error during cleanup: ${e.message}")
        }
    }
}

/**
 * Alternative main function using the game launcher
 */
fun mainWithLauncher() {
    val launcher = GameLauncher()
    launcher.launch()
}