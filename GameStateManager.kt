import kotlin.math.*

class GameStateManager {
    private var currentState = GameState.MENU
    private var previousState = GameState.MENU
    private val stateStack = mutableListOf<GameState>()
    
    private val stateEnterCallbacks = mutableMapOf<GameState, () -> Unit>()
    private val stateExitCallbacks = mutableMapOf<GameState, () -> Unit>()
    private val stateUpdateCallbacks = mutableMapOf<GameState, (Float) -> Unit>()
    
    var gameData = GameData()
    private var levelManager: LevelManager? = null
    
    fun initialize(levelManager: LevelManager) {
        this.levelManager = levelManager
        setupStateCallbacks()
        changeState(GameState.MENU)
    }
    
    fun update(deltaTime: Float) {
        stateUpdateCallbacks[currentState]?.invoke(deltaTime)
    }
    
    fun changeState(newState: GameState) {
        if (newState == currentState) return
        
        stateExitCallbacks[currentState]?.invoke()
        
        previousState = currentState
        currentState = newState
        
        stateEnterCallbacks[currentState]?.invoke()
    }
    
    fun pushState(state: GameState) {
        stateStack.add(currentState)
        changeState(state)
    }
    
    fun popState() {
        if (stateStack.isNotEmpty()) {
            val previousState = stateStack.removeAt(stateStack.size - 1)
            changeState(previousState)
        }
    }
    
    fun getCurrentState(): GameState = currentState
    
    fun getPreviousState(): GameState = previousState
    
    fun isState(state: GameState): Boolean = currentState == state
    
    fun getGameData(): GameData = gameData
    
    private fun setupStateCallbacks() {
        stateEnterCallbacks[GameState.MENU] = {
        }
        
        stateUpdateCallbacks[GameState.MENU] = { deltaTime ->
        }
        
        stateExitCallbacks[GameState.MENU] = {
        }
        
        stateEnterCallbacks[GameState.PLAYING] = {
            if (gameData.currentLevel == null) {
                startNewGame()
            }
        }
        
        stateUpdateCallbacks[GameState.PLAYING] = { deltaTime ->
            updateGameplay(deltaTime)
        }
        
        stateExitCallbacks[GameState.PLAYING] = {
        }
        
        stateEnterCallbacks[GameState.PAUSED] = {
        }
        
        stateUpdateCallbacks[GameState.PAUSED] = { deltaTime ->
        }
        
        stateExitCallbacks[GameState.PAUSED] = {
        }
        
        stateEnterCallbacks[GameState.GAME_OVER] = {
            gameData.isGameOver = true
        }
        
        stateUpdateCallbacks[GameState.GAME_OVER] = { deltaTime ->
        }
        
        stateExitCallbacks[GameState.GAME_OVER] = {
        }
        
        stateEnterCallbacks[GameState.LEVEL_COMPLETE] = {
            calculateLevelScore()
        }
        
        stateUpdateCallbacks[GameState.LEVEL_COMPLETE] = { deltaTime ->
        }
        
        stateExitCallbacks[GameState.LEVEL_COMPLETE] = {
        }
        
        stateEnterCallbacks[GameState.LOADING] = {
        }
        
        stateUpdateCallbacks[GameState.LOADING] = { deltaTime ->
        }
        
        stateExitCallbacks[GameState.LOADING] = {
        }
        
        stateEnterCallbacks[GameState.SETTINGS] = {
        }
        
        stateUpdateCallbacks[GameState.SETTINGS] = { deltaTime ->
        }
        
        stateExitCallbacks[GameState.SETTINGS] = {
        }
    }
    
    fun startNewGame() {
        gameData.reset()
        gameData.currentLevel = levelManager?.loadLevel("level1")
        gameData.playerLives = 3
        gameData.score = 0
        changeState(GameState.PLAYING)
    }
    
    fun continueGame() {
        if (gameData.hasSaveData()) {
            gameData.loadSaveData()
            changeState(GameState.PLAYING)
        } else {
            startNewGame()
        }
    }
    
    fun pauseGame() {
        if (currentState == GameState.PLAYING) {
            pushState(GameState.PAUSED)
        }
    }
    
    fun resumeGame() {
        if (currentState == GameState.PAUSED) {
            popState()
        }
    }
    
    fun gameOver() {
        gameData.playerLives--
        if (gameData.playerLives <= 0) {
            changeState(GameState.GAME_OVER)
        } else {
            respawnPlayer()
        }
    }
    
    fun completeLevel() {
        changeState(GameState.LEVEL_COMPLETE)
    }
    
    fun nextLevel() {
        val nextLevelId = getNextLevelId()
        if (nextLevelId != null) {
            gameData.currentLevel = levelManager?.loadLevel(nextLevelId)
            changeState(GameState.PLAYING)
        } else {
            changeState(GameState.GAME_COMPLETE)
        }
    }
    
    fun restartLevel() {
        gameData.currentLevel?.reset()
        changeState(GameState.PLAYING)
    }
    
    fun returnToMenu() {
        gameData.saveGameData()
        changeState(GameState.MENU)
    }
    
    private fun updateGameplay(deltaTime: Float) {
        gameData.playTime += deltaTime
        
        gameData.currentLevel?.let { level ->
            if (level.isComplete()) {
                completeLevel()
            }
        }
    }
    
    private fun calculateLevelScore() {
        val level = gameData.currentLevel ?: return
        
        val baseScore = 1000
        val timeBonus = maxOf(0, 300 - gameData.levelTime.toInt()) * 10
        val completionBonus = (level.getCompletionPercentage() * 500).toInt()
        val livesBonus = gameData.playerLives * 100
        
        val levelScore = baseScore + timeBonus + completionBonus + livesBonus
        gameData.score += levelScore
        gameData.lastLevelScore = levelScore
    }
    
    private fun respawnPlayer() {
    }
    
    private fun getNextLevelId(): String? {
        val currentLevelId = gameData.currentLevel?.levelId ?: return null
        val levelNumber = currentLevelId.removePrefix("level").toIntOrNull() ?: return null
        val nextLevelNumber = levelNumber + 1
        
        return if (nextLevelNumber <= 10) {
            "level$nextLevelNumber"
        } else {
            null
        }
    }
    
    fun handleInput(inputHandler: InputHandler) {
        when (currentState) {
            GameState.MENU -> handleMenuInput(inputHandler)
            GameState.PLAYING -> handleGameplayInput(inputHandler)
            GameState.PAUSED -> handlePauseInput(inputHandler)
            GameState.GAME_OVER -> handleGameOverInput(inputHandler)
            GameState.LEVEL_COMPLETE -> handleLevelCompleteInput(inputHandler)
            GameState.SETTINGS -> handleSettingsInput(inputHandler)
            else -> {}
        }
    }
    
    private fun handleMenuInput(inputHandler: InputHandler) {
        if (inputHandler.isActionPressed(InputAction.CONFIRM)) {
            startNewGame()
        }
        if (inputHandler.isActionPressed(InputAction.MENU)) {
            changeState(GameState.SETTINGS)
        }
    }
    
    private fun handleGameplayInput(inputHandler: InputHandler) {
        if (inputHandler.isActionPressed(InputAction.PAUSE)) {
            pauseGame()
        }
    }
    
    private fun handlePauseInput(inputHandler: InputHandler) {
        if (inputHandler.isActionPressed(InputAction.PAUSE) || 
            inputHandler.isActionPressed(InputAction.CONFIRM)) {
            resumeGame()
        }
        if (inputHandler.isActionPressed(InputAction.MENU)) {
            returnToMenu()
        }
    }
    
    private fun handleGameOverInput(inputHandler: InputHandler) {
        if (inputHandler.isActionPressed(InputAction.CONFIRM)) {
            startNewGame()
        }
        if (inputHandler.isActionPressed(InputAction.MENU)) {
            returnToMenu()
        }
    }
    
    private fun handleLevelCompleteInput(inputHandler: InputHandler) {
        if (inputHandler.isActionPressed(InputAction.CONFIRM)) {
            nextLevel()
        }
        if (inputHandler.isActionPressed(InputAction.MENU)) {
            returnToMenu()
        }
    }
    
    private fun handleSettingsInput(inputHandler: InputHandler) {
        if (inputHandler.isActionPressed(InputAction.MENU) || 
            inputHandler.isActionPressed(InputAction.CANCEL)) {
            changeState(previousState)
        }
    }
}

enum class GameState {
    MENU,
    PLAYING,
    PAUSED,
    GAME_OVER,
    LEVEL_COMPLETE,
    GAME_COMPLETE,
    LOADING,
    SETTINGS,
    CREDITS
}

class GameData {
    var score = 0
    var playerLives = 3
    var currentLevel: Level? = null
    var playTime = 0f
    var levelTime = 0f
    var lastLevelScore = 0
    var isGameOver = false
    var highScore = 0
    
    var levelsCompleted = 0
    var totalCoinsCollected = 0
    var totalEnemiesKilled = 0
    var totalDeaths = 0
    
    var musicVolume = 1f
    var soundVolume = 1f
    var masterVolume = 1f
    var fullscreen = false
    
    fun reset() {
        score = 0
        playerLives = 3
        currentLevel = null
        playTime = 0f
        levelTime = 0f
        lastLevelScore = 0
        isGameOver = false
    }
    
    fun hasSaveData(): Boolean {
        return false
    }
    
    fun loadSaveData() {
    }
    
    fun saveGameData() {
        if (score > highScore) {
            highScore = score
        }
    }
    
    fun addScore(points: Int) {
        score += points
    }
    
    fun addCoins(coins: Int) {
        totalCoinsCollected += coins
        addScore(coins * 10)
    }
    
    fun addEnemyKilled(enemy: Enemy) {
        totalEnemiesKilled++
        addScore(enemy.scoreValue)
    }
    
    fun playerDied() {
        totalDeaths++
    }
    
    fun levelCompleted() {
        levelsCompleted++
    }
}

class LevelManager {
    private val levels = mutableMapOf<String, Level>()
    
    fun loadLevel(levelId: String): Level? {
        return levels[levelId] ?: createLevel(levelId)
    }
    
    private fun createLevel(levelId: String): Level? {
        return when (levelId) {
            "level1" -> {
                val level = Level(levelId, "Forest Path", 1600f, 600f)
                level.initialize()
                levels[levelId] = level
                level
            }
            "level2" -> {
                val level = Level(levelId, "Mountain Climb", 1800f, 800f)
                level.initialize()
                levels[levelId] = level
                level
            }
            else -> null
        }
    }
    
    fun getAvailableLevels(): List<String> {
        return listOf("level1", "level2", "level3", "level4", "level5")
    }
}

enum class InputAction {
    MOVE_LEFT,
    MOVE_RIGHT,
    JUMP,
    ATTACK,
    CROUCH,
    RUN,
    PAUSE,
    MENU,
    CONFIRM,
    CANCEL,
    UP,
    DOWN
}