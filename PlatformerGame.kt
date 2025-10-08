import java.awt.*
import java.awt.event.*
import java.awt.image.BufferedImage
import javax.swing.*
import kotlin.math.*

/**
 * Main game class that orchestrates all game systems and manages the game loop.
 * This is the entry point for the 2.5D platformer game.
 */
class PlatformerGame : JPanel(), KeyListener, ActionListener {
    companion object {
        const val WINDOW_WIDTH = 1280
        const val WINDOW_HEIGHT = 720
        const val TARGET_FPS = 60
        const val FIXED_TIMESTEP = 1.0f / TARGET_FPS
        
        @JvmStatic
        fun main(args: Array<String>) {
            SwingUtilities.invokeLater {
                val game = PlatformerGame()
                game.initialize()
            }
        }
    }
    
    // Core game systems
    private lateinit var gameStateManager: GameStateManager
    private lateinit var renderer: GameRenderer
    private lateinit var inputHandler: InputHandler
    private lateinit var physicsEngine: PhysicsEngine
    private lateinit var assetManager: AssetManager
    private lateinit var camera: Camera
    private lateinit var levelManager: LevelManager
    private lateinit var audioManager: AudioManager
    
    // Game entities
    private lateinit var player: Player
    private val enemies = mutableListOf<Enemy>()
    
    // Game loop variables
    private var gameTimer: Timer? = null
    private var lastUpdateTime = System.nanoTime()
    private var accumulator = 0.0f
    private var isRunning = false
    
    // Scoring and progress
    private var score = 0
    private var lives = 3
    private var currentLevel = 1
    
    /**
     * Initialize all game systems and components
     */
    fun initialize() {
        println("Initializing 2.5D Platformer Game...")
        
        // Set up the window
        setupWindow()
        
        // Initialize core systems
        assetManager = AssetManager()
        assetManager.loadAssets()
        
        inputHandler = InputHandler()
        physicsEngine = PhysicsEngine()
        camera = Camera(WINDOW_WIDTH.toFloat(), WINDOW_HEIGHT.toFloat())
        renderer = GameRenderer(camera)
        levelManager = LevelManager()
        audioManager = AudioManager()
        gameStateManager = GameStateManager()
        
        // Initialize game entities
        player = Player(100f, 100f)
        
        // Load the first level
        levelManager.loadLevel(currentLevel)
        
        // Set initial camera position
        camera.setTarget(player.position)
        
        // Set initial game state
        gameStateManager.setState(GameState.MENU)
        
        // Start the game loop
        startGameLoop()
        
        println("Game initialized successfully!")
    }
    
    /**
     * Set up the game window and display
     */
    private fun setupWindow() {
        val frame = JFrame("2.5D Platformer Game")
        frame.defaultCloseOperation = JFrame.EXIT_ON_CLOSE
        frame.setSize(WINDOW_WIDTH, WINDOW_HEIGHT)
        frame.isResizable = false
        frame.add(this)
        frame.addKeyListener(this)
        
        // Center the window
        frame.setLocationRelativeTo(null)
        frame.isVisible = true
        
        // Request focus for key events
        this.isFocusable = true
        this.requestFocusInWindow()
        this.addKeyListener(this)
        
        // Set up double buffering
        this.isDoubleBuffered = true
    }
    
    /**
     * Start the main game loop with fixed timestep
     */
    private fun startGameLoop() {
        isRunning = true
        gameTimer = Timer((1000 / TARGET_FPS), this)
        gameTimer?.start()
        lastUpdateTime = System.nanoTime()
    }
    
    /**
     * Main game loop - called by Swing Timer
     */
    override fun actionPerformed(e: ActionEvent?) {
        if (!isRunning) return
        
        val currentTime = System.nanoTime()
        val deltaTime = (currentTime - lastUpdateTime) / 1_000_000_000.0f
        lastUpdateTime = currentTime
        
        // Accumulate time for fixed timestep updates
        accumulator += deltaTime
        
        // Process input
        inputHandler.update()
        
        // Fixed timestep updates for physics and game logic
        while (accumulator >= FIXED_TIMESTEP) {
            update(FIXED_TIMESTEP)
            accumulator -= FIXED_TIMESTEP
        }
        
        // Render the frame
        repaint()
    }
    
    /**
     * Update game logic with fixed timestep
     */
    private fun update(deltaTime: Float) {
        when (gameStateManager.currentState) {
            GameState.MENU -> updateMenu(deltaTime)
            GameState.PLAYING -> updateGameplay(deltaTime)
            GameState.PAUSED -> updatePaused(deltaTime)
            GameState.GAME_OVER -> updateGameOver(deltaTime)
            GameState.LEVEL_COMPLETE -> updateLevelComplete(deltaTime)
        }
    }
    
    /**
     * Update menu state
     */
    private fun updateMenu(deltaTime: Float) {
        if (inputHandler.isKeyPressed(KeyEvent.VK_SPACE) || inputHandler.isKeyPressed(KeyEvent.VK_ENTER)) {
            gameStateManager.setState(GameState.PLAYING)
            audioManager.playSound("menu_select")
        }
    }
    
    /**
     * Update gameplay state
     */
    private fun updateGameplay(deltaTime: Float) {
        // Handle pause
        if (inputHandler.isKeyPressed(KeyEvent.VK_ESCAPE)) {
            gameStateManager.setState(GameState.PAUSED)
            return
        }
        
        // Update player
        player.handleInput(inputHandler)
        player.update(deltaTime)
        
        // Update enemies
        enemies.forEach { enemy ->
            enemy.update(deltaTime, player.position)
        }
        
        // Update physics
        physicsEngine.update(deltaTime, player, enemies, levelManager.currentLevel)
        
        // Update camera to follow player
        camera.update(deltaTime, player.position)
        
        // Check for level completion
        if (levelManager.isLevelComplete(player.position)) {
            completeLevel()
        }
        
        // Check for player death
        if (player.position.y > levelManager.currentLevel.deathY || player.health <= 0) {
            playerDied()
        }
    }
    
    /**
     * Update paused state
     */
    private fun updatePaused(deltaTime: Float) {
        if (inputHandler.isKeyPressed(KeyEvent.VK_ESCAPE)) {
            gameStateManager.setState(GameState.PLAYING)
        }
    }
    
    /**
     * Update game over state
     */
    private fun updateGameOver(deltaTime: Float) {
        if (inputHandler.isKeyPressed(KeyEvent.VK_R)) {
            restartGame()
        } else if (inputHandler.isKeyPressed(KeyEvent.VK_ESCAPE)) {
            gameStateManager.setState(GameState.MENU)
        }
    }
    
    /**
     * Update level complete state
     */
    private fun updateLevelComplete(deltaTime: Float) {
        if (inputHandler.isKeyPressed(KeyEvent.VK_SPACE)) {
            nextLevel()
        }
    }
    
    /**
     * Handle level completion
     */
    private fun completeLevel() {
        score += 1000 * currentLevel
        audioManager.playSound("level_complete")
        gameStateManager.setState(GameState.LEVEL_COMPLETE)
    }
    
    /**
     * Handle player death
     */
    private fun playerDied() {
        lives--
        audioManager.playSound("player_death")
        
        if (lives <= 0) {
            gameStateManager.setState(GameState.GAME_OVER)
        } else {
            // Respawn player
            player.respawn(levelManager.currentLevel.spawnPoint)
        }
    }
    
    /**
     * Advance to next level
     */
    private fun nextLevel() {
        currentLevel++
        if (levelManager.hasLevel(currentLevel)) {
            levelManager.loadLevel(currentLevel)
            player.respawn(levelManager.currentLevel.spawnPoint)
            enemies.clear()
            levelManager.spawnEnemies(enemies)
            gameStateManager.setState(GameState.PLAYING)
        } else {
            // Game completed
            gameStateManager.setState(GameState.GAME_OVER)
        }
    }
    
    /**
     * Restart the game
     */
    private fun restartGame() {
        score = 0
        lives = 3
        currentLevel = 1
        levelManager.loadLevel(currentLevel)
        player.respawn(levelManager.currentLevel.spawnPoint)
        enemies.clear()
        levelManager.spawnEnemies(enemies)
        gameStateManager.setState(GameState.PLAYING)
    }
    
    /**
     * Render the game
     */
    override fun paintComponent(g: Graphics?) {
        super.paintComponent(g)
        val g2d = g as Graphics2D
        
        // Enable anti-aliasing for smoother graphics
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
        
        when (gameStateManager.currentState) {
            GameState.MENU -> renderer.renderMenu(g2d, WINDOW_WIDTH, WINDOW_HEIGHT)
            GameState.PLAYING -> renderer.renderGame(g2d, player, enemies, levelManager.currentLevel, score, lives)
            GameState.PAUSED -> {
                renderer.renderGame(g2d, player, enemies, levelManager.currentLevel, score, lives)
                renderer.renderPauseOverlay(g2d, WINDOW_WIDTH, WINDOW_HEIGHT)
            }
            GameState.GAME_OVER -> renderer.renderGameOver(g2d, WINDOW_WIDTH, WINDOW_HEIGHT, score)
            GameState.LEVEL_COMPLETE -> renderer.renderLevelComplete(g2d, WINDOW_WIDTH, WINDOW_HEIGHT, currentLevel, score)
        }
    }
    
    /**
     * Shutdown the game and clean up resources
     */
    fun shutdown() {
        isRunning = false
        gameTimer?.stop()
        audioManager.cleanup()
        assetManager.cleanup()
        println("Game shutdown complete.")
    }
    
    // Key event handlers
    override fun keyPressed(e: KeyEvent?) {
        e?.let { inputHandler.keyPressed(it) }
    }
    
    override fun keyReleased(e: KeyEvent?) {
        e?.let { inputHandler.keyReleased(it) }
    }
    
    override fun keyTyped(e: KeyEvent?) {
        // Not used
    }
}