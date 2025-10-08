import java.awt.event.KeyEvent
import java.awt.event.KeyListener

import kotlin.math.*

class InputHandler : KeyListener {
    private val keyStates = mutableMapOf<Int, KeyState>()
    private val actionMappings = mutableMapOf<GameAction, List<Int>>()
    private var gamepadState = GamepadState()
    
    init {
        setupDefaultKeyMappings()
    }
    
    private fun setupDefaultKeyMappings() {
        actionMappings[GameAction.MOVE_LEFT] = listOf(KeyEvent.VK_A, KeyEvent.VK_LEFT)
        actionMappings[GameAction.MOVE_RIGHT] = listOf(KeyEvent.VK_D, KeyEvent.VK_RIGHT)
        actionMappings[GameAction.JUMP] = listOf(KeyEvent.VK_SPACE, KeyEvent.VK_W, KeyEvent.VK_UP)
        actionMappings[GameAction.ATTACK] = listOf(KeyEvent.VK_X, KeyEvent.VK_ENTER)
        actionMappings[GameAction.DASH] = listOf(KeyEvent.VK_Z, KeyEvent.VK_SHIFT)
        actionMappings[GameAction.PAUSE] = listOf(KeyEvent.VK_ESCAPE, KeyEvent.VK_P)
        actionMappings[GameAction.INTERACT] = listOf(KeyEvent.VK_E, KeyEvent.VK_F)
        actionMappings[GameAction.MENU] = listOf(KeyEvent.VK_TAB, KeyEvent.VK_M)
    }
    
    fun update() {
        keyStates.values.forEach { it.update() }
        updateGamepadState()
    }
    
    override fun keyPressed(e: KeyEvent) {
        val keyCode = e.keyCode
        if (!keyStates.containsKey(keyCode)) {
            keyStates[keyCode] = KeyState()
        }
        keyStates[keyCode]?.press()
    }
    
    override fun keyReleased(e: KeyEvent) {
        val keyCode = e.keyCode
        keyStates[keyCode]?.release()
    }
    
    override fun keyTyped(e: KeyEvent) {
    }
    
    fun isKeyPressed(keyCode: Int): Boolean {
        return keyStates[keyCode]?.isPressed ?: false
    }
    
    fun isKeyDown(keyCode: Int): Boolean {
        return keyStates[keyCode]?.isDown ?: false
    }
    
    fun isKeyReleased(keyCode: Int): Boolean {
        return keyStates[keyCode]?.isReleased ?: false
    }
    
    fun isActionPressed(action: GameAction): Boolean {
        return actionMappings[action]?.any { isKeyPressed(it) } ?: false ||
               isGamepadActionPressed(action)
    }
    
    fun isActionDown(action: GameAction): Boolean {
        return actionMappings[action]?.any { isKeyDown(it) } ?: false ||
               isGamepadActionDown(action)
    }
    
    fun isActionReleased(action: GameAction): Boolean {
        return actionMappings[action]?.any { isKeyReleased(it) } ?: false ||
               isGamepadActionReleased(action)
    }
    
    fun getMovementInput(): Vector2 {
        var movement = Vector2.ZERO
        
        if (isActionDown(GameAction.MOVE_LEFT)) {
            movement = movement.copy(x = movement.x - 1f)
        }
        if (isActionDown(GameAction.MOVE_RIGHT)) {
            movement = movement.copy(x = movement.x + 1f)
        }
        
        movement = movement.add(gamepadState.leftStick)
        
        return movement.normalized()
    }
    
    fun setKeyMapping(action: GameAction, keyCodes: List<Int>) {
        actionMappings[action] = keyCodes
    }
    
    fun addKeyMapping(action: GameAction, keyCode: Int) {
        val currentMappings = actionMappings[action]?.toMutableList() ?: mutableListOf()
        if (!currentMappings.contains(keyCode)) {
            currentMappings.add(keyCode)
            actionMappings[action] = currentMappings
        }
    }
    
    fun removeKeyMapping(action: GameAction, keyCode: Int) {
        val currentMappings = actionMappings[action]?.toMutableList()
        currentMappings?.remove(keyCode)
        if (currentMappings != null) {
            actionMappings[action] = currentMappings
        }
    }
    
    fun clearKeyMappings(action: GameAction) {
        actionMappings[action] = emptyList()
    }
    
    fun getKeyMappings(action: GameAction): List<Int> {
        return actionMappings[action] ?: emptyList()
    }
    
    fun getAllKeyMappings(): Map<GameAction, List<Int>> {
        return actionMappings.toMap()
    }
    
    private fun updateGamepadState() {
    }
    
    private fun isGamepadActionPressed(action: GameAction): Boolean {
        return when (action) {
            GameAction.JUMP -> gamepadState.aButtonPressed
            GameAction.ATTACK -> gamepadState.xButtonPressed
            GameAction.DASH -> gamepadState.bButtonPressed
            GameAction.PAUSE -> gamepadState.startButtonPressed
            GameAction.INTERACT -> gamepadState.yButtonPressed
            GameAction.MENU -> gamepadState.selectButtonPressed
            else -> false
        }
    }
    
    private fun isGamepadActionDown(action: GameAction): Boolean {
        return when (action) {
            GameAction.JUMP -> gamepadState.aButtonDown
            GameAction.ATTACK -> gamepadState.xButtonDown
            GameAction.DASH -> gamepadState.bButtonDown
            GameAction.PAUSE -> gamepadState.startButtonDown
            GameAction.INTERACT -> gamepadState.yButtonDown
            GameAction.MENU -> gamepadState.selectButtonDown
            else -> false
        }
    }
    
    private fun isGamepadActionReleased(action: GameAction): Boolean {
        return when (action) {
            GameAction.JUMP -> gamepadState.aButtonReleased
            GameAction.ATTACK -> gamepadState.xButtonReleased
            GameAction.DASH -> gamepadState.bButtonReleased
            GameAction.PAUSE -> gamepadState.startButtonReleased
            GameAction.INTERACT -> gamepadState.yButtonReleased
            GameAction.MENU -> gamepadState.selectButtonReleased
            else -> false
        }
    }
    
    fun reset() {
        keyStates.clear()
        gamepadState = GamepadState()
    }
    
    fun getDebugInfo(): InputDebugInfo {
        val pressedKeys = keyStates.filter { it.value.isDown }.keys.toList()
        val activeActions = GameAction.values().filter { isActionDown(it) }
        
        return InputDebugInfo(
            pressedKeys = pressedKeys,
            activeActions = activeActions,
            gamepadConnected = gamepadState.isConnected,
            leftStick = gamepadState.leftStick,
            rightStick = gamepadState.rightStick
        )
    }
}

class KeyState {
    var isDown = false
        private set
    var isPressed = false
        private set
    var isReleased = false
        private set
    
    fun press() {
        if (!isDown) {
            isPressed = true
            isDown = true
        }
    }
    
    fun release() {
        if (isDown) {
            isReleased = true
            isDown = false
        }
    }
    
    fun update() {
        isPressed = false
        isReleased = false
    }
}

data class GamepadState(
    var isConnected: Boolean = false,
    var leftStick: Vector2 = Vector2.ZERO,
    var rightStick: Vector2 = Vector2.ZERO,
    var leftTrigger: Float = 0f,
    var rightTrigger: Float = 0f,
    var aButtonDown: Boolean = false,
    var bButtonDown: Boolean = false,
    var xButtonDown: Boolean = false,
    var yButtonDown: Boolean = false,
    var startButtonDown: Boolean = false,
    var selectButtonDown: Boolean = false,
    var leftBumperDown: Boolean = false,
    var rightBumperDown: Boolean = false,
    var dPadUp: Boolean = false,
    var dPadDown: Boolean = false,
    var dPadLeft: Boolean = false,
    var dPadRight: Boolean = false,
    var aButtonPressed: Boolean = false,
    var bButtonPressed: Boolean = false,
    var xButtonPressed: Boolean = false,
    var yButtonPressed: Boolean = false,
    var startButtonPressed: Boolean = false,
    var selectButtonPressed: Boolean = false,
    var leftBumperPressed: Boolean = false,
    var rightBumperPressed: Boolean = false,
    var aButtonReleased: Boolean = false,
    var bButtonReleased: Boolean = false,
    var xButtonReleased: Boolean = false,
    var yButtonReleased: Boolean = false,
    var startButtonReleased: Boolean = false,
    var selectButtonReleased: Boolean = false,
    var leftBumperReleased: Boolean = false,
    var rightBumperReleased: Boolean = false
)

enum class GameAction {
    MOVE_LEFT,
    MOVE_RIGHT,
    JUMP,
    ATTACK,
    DASH,
    PAUSE,
    INTERACT,
    MENU
}

data class InputDebugInfo(
    val pressedKeys: List<Int>,
    val activeActions: List<GameAction>,
    val gamepadConnected: Boolean,
    val leftStick: Vector2,
    val rightStick: Vector2
)