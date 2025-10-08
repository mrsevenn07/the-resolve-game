import java.io.File

class AssetManager {
    private val textures = mutableMapOf<String, Texture>()
    private val sounds = mutableMapOf<String, SoundEffect>()
    private val music = mutableMapOf<String, MusicTrack>()
    private val animations = mutableMapOf<String, Animation>()
    
    var textureDirectory = "assets/textures/"
    var soundDirectory = "assets/sounds/"
    var musicDirectory = "assets/music/"
    var animationDirectory = "assets/animations/"
    
    fun loadAssets() {
        loadTextures()
        loadSounds()
        loadMusic()
        loadAnimations()
    }
    
    private fun loadTextures() {
        val textureDir = File(textureDirectory)
        if (!textureDir.exists()) {
            println("Texture directory not found: $textureDirectory")
            createDefaultTextures()
            return
        }
        
        textureDir.listFiles()?.forEach { file ->
            if (file.isFile && isImageFile(file)) {
                val name = file.nameWithoutExtension
                try {
                    val texture = Texture(file.absolutePath)
                    textures[name] = texture
                    println("Loaded texture: $name")
                } catch (e: Exception) {
                    println("Failed to load texture: ${file.name} - ${e.message}")
                }
            }
        }
        
        if (textures.isEmpty()) {
            createDefaultTextures()
        }
    }
    
    private fun loadSounds() {
        val soundDir = File(soundDirectory)
        if (!soundDir.exists()) {
            println("Sound directory not found: $soundDirectory")
            createDefaultSounds()
            return
        }
        
        soundDir.listFiles()?.forEach { file ->
            if (file.isFile && isAudioFile(file)) {
                val name = file.nameWithoutExtension
                try {
                    val sound = SoundEffect(file.absolutePath)
                    sounds[name] = sound
                    println("Loaded sound: $name")
                } catch (e: Exception) {
                    println("Failed to load sound: ${file.name} - ${e.message}")
                }
            }
        }
        
        if (sounds.isEmpty()) {
            createDefaultSounds()
        }
    }
    
    private fun loadMusic() {
        val musicDir = File(musicDirectory)
        if (!musicDir.exists()) {
            println("Music directory not found: $musicDirectory")
            createDefaultMusic()
            return
        }
        
        musicDir.listFiles()?.forEach { file ->
            if (file.isFile && isAudioFile(file)) {
                val name = file.nameWithoutExtension
                try {
                    val musicTrack = MusicTrack(file.absolutePath)
                    music[name] = musicTrack
                    println("Loaded music: $name")
                } catch (e: Exception) {
                    println("Failed to load music: ${file.name} - ${e.message}")
                }
            }
        }
        
        if (music.isEmpty()) {
            createDefaultMusic()
        }
    }
    
    private fun loadAnimations() {
        val animDir = File(animationDirectory)
        if (!animDir.exists()) {
            println("Animation directory not found: $animationDirectory")
            createDefaultAnimations()
            return
        }
        
        animDir.listFiles()?.forEach { file ->
            if (file.isFile && file.extension == "anim") {
                val name = file.nameWithoutExtension
                try {
                    val animation = loadAnimationFromFile(file)
                    animations[name] = animation
                    println("Loaded animation: $name")
                } catch (e: Exception) {
                    println("Failed to load animation: ${file.name} - ${e.message}")
                }
            }
        }
        
        if (animations.isEmpty()) {
            createDefaultAnimations()
        }
    }
    
    private fun isImageFile(file: File): Boolean {
        val imageExtensions = setOf("png", "jpg", "jpeg", "bmp", "gif")
        return imageExtensions.contains(file.extension.lowercase())
    }
    
    private fun isAudioFile(file: File): Boolean {
        val audioExtensions = setOf("wav", "mp3", "ogg", "m4a")
        return audioExtensions.contains(file.extension.lowercase())
    }
    
    private fun loadAnimationFromFile(file: File): Animation {
        val frames = mutableListOf<AnimationFrame>()
        
        file.readLines().forEach { line ->
            val parts = line.split(",")
            if (parts.size >= 3) {
                val textureName = parts[0].trim()
                val duration = parts[1].trim().toFloatOrNull() ?: 0.1f
                val offsetX = parts.getOrNull(2)?.trim()?.toFloatOrNull() ?: 0f
                val offsetY = parts.getOrNull(3)?.trim()?.toFloatOrNull() ?: 0f
                
                val texture = getTexture(textureName)
                if (texture != null) {
                    frames.add(AnimationFrame(texture, duration, Vector2(offsetX, offsetY)))
                }
            }
        }
        
        return Animation(frames)
    }
    
    private fun createDefaultTextures() {
        textures["player"] = Texture.createDefault(32, 32, Color.BLUE)
        textures["enemy"] = Texture.createDefault(24, 24, Color.RED)
        textures["platform"] = Texture.createDefault(64, 16, Color.BROWN)
        textures["coin"] = Texture.createDefault(16, 16, Color.YELLOW)
        textures["background"] = Texture.createDefault(800, 600, Color.SKY_BLUE)
        textures["particle"] = Texture.createDefault(4, 4, Color.WHITE)
        println("Created default textures")
    }
    
    private fun createDefaultSounds() {
        sounds["jump"] = SoundEffect.createDefault(0.2f, 440f)
        sounds["coin"] = SoundEffect.createDefault(0.1f, 880f)
        sounds["hurt"] = SoundEffect.createDefault(0.3f, 220f)
        sounds["enemy_death"] = SoundEffect.createDefault(0.2f, 330f)
        sounds["footstep"] = SoundEffect.createDefault(0.1f, 200f)
        println("Created default sounds")
    }
    
    private fun createDefaultMusic() {
        music["level1"] = MusicTrack.createDefault("level1", 120f)
        music["menu"] = MusicTrack.createDefault("menu", 100f)
        music["game_over"] = MusicTrack.createDefault("game_over", 80f)
        println("Created default music")
    }
    
    private fun createDefaultAnimations() {
        val playerIdleFrames = listOf(
            AnimationFrame(getTexture("player")!!, 0.5f, Vector2.ZERO)
        )
        animations["player_idle"] = Animation(playerIdleFrames)
        
        val playerRunFrames = listOf(
            AnimationFrame(getTexture("player")!!, 0.1f, Vector2.ZERO),
            AnimationFrame(getTexture("player")!!, 0.1f, Vector2.ZERO)
        )
        animations["player_run"] = Animation(playerRunFrames)
        
        println("Created default animations")
    }
    
    fun getTexture(name: String): Texture? {
        return textures[name] ?: run {
            println("Texture not found: $name")
            textures["player"]
        }
    }
    
    fun getSound(name: String): SoundEffect? {
        return sounds[name] ?: run {
            println("Sound not found: $name")
            null
        }
    }
    
    fun getMusic(name: String): MusicTrack? {
        return music[name] ?: run {
            println("Music not found: $name")
            null
        }
    }
    
    fun getAnimation(name: String): Animation? {
        return animations[name] ?: run {
            println("Animation not found: $name")
            animations["player_idle"]
        }
    }
    
    fun preloadAssets(assetList: List<String>) {
        assetList.forEach { assetName ->
            getTexture(assetName)
            getSound(assetName)
            getMusic(assetName)
            getAnimation(assetName)
        }
    }
    
    fun unloadAssets() {
        textures.values.forEach { it.dispose() }
        sounds.values.forEach { it.dispose() }
        music.values.forEach { it.dispose() }
        
        textures.clear()
        sounds.clear()
        music.clear()
        animations.clear()
        
        println("All assets unloaded")
    }
    
    fun getMemoryUsage(): AssetMemoryInfo {
        val textureMemory = textures.values.sumOf { it.getMemorySize() }
        val soundMemory = sounds.values.sumOf { it.getMemorySize() }
        val musicMemory = music.values.sumOf { it.getMemorySize() }
        
        return AssetMemoryInfo(
            textureCount = textures.size,
            soundCount = sounds.size,
            musicCount = music.size,
            animationCount = animations.size,
            totalMemoryMB = (textureMemory + soundMemory + musicMemory) / (1024 * 1024)
        )
    }
}

data class Texture(
    val path: String,
    val width: Int = 32,
    val height: Int = 32
) {
    fun dispose() {
    }
    
    fun getMemorySize(): Long {
        return (width * height * 4).toLong()
    }
    
    companion object {
        fun createDefault(width: Int, height: Int, color: Color): Texture {
            return Texture("default", width, height)
        }
    }
}

data class SoundEffect(
    val path: String,
    val duration: Float = 1f
) {
    fun play(volume: Float = 1f, pitch: Float = 1f) {
    }
    
    fun dispose() {
    }
    
    fun getMemorySize(): Long {
        return (duration * 44100 * 2).toLong()
    }
    
    companion object {
        fun createDefault(duration: Float, frequency: Float): SoundEffect {
            return SoundEffect("default", duration)
        }
    }
}

data class MusicTrack(
    val path: String,
    val name: String = "",
    val bpm: Float = 120f
) {
    fun play(loop: Boolean = true, volume: Float = 1f) {
    }
    
    fun stop() {
    }
    
    fun pause() {
    }
    
    fun resume() {
    }
    
    fun dispose() {
    }
    
    fun getMemorySize(): Long {
        return 1024 * 1024
    }
    
    companion object {
        fun createDefault(name: String, bpm: Float): MusicTrack {
            return MusicTrack("default", name, bpm)
        }
    }
}

data class Animation(
    val frames: List<AnimationFrame>,
    var currentFrame: Int = 0,
    var timeAccumulator: Float = 0f,
    var isPlaying: Boolean = true,
    var loop: Boolean = true
) {
    fun update(deltaTime: Float) {
        if (!isPlaying || frames.isEmpty()) return
        
        timeAccumulator += deltaTime
        val currentFrameData = frames[currentFrame]
        
        if (timeAccumulator >= currentFrameData.duration) {
            timeAccumulator = 0f
            currentFrame++
            
            if (currentFrame >= frames.size) {
                if (loop) {
                    currentFrame = 0
                } else {
                    currentFrame = frames.size - 1
                    isPlaying = false
                }
            }
        }
    }
    
    fun getCurrentFrame(): AnimationFrame? {
        return if (frames.isNotEmpty() && currentFrame < frames.size) {
            frames[currentFrame]
        } else null
    }
    
    fun reset() {
        currentFrame = 0
        timeAccumulator = 0f
        isPlaying = true
    }
    
    fun play() {
        isPlaying = true
    }
    
    fun pause() {
        isPlaying = false
    }
    
    fun stop() {
        isPlaying = false
        currentFrame = 0
        timeAccumulator = 0f
    }
}

data class AnimationFrame(
    val texture: Texture,
    val duration: Float,
    val offset: Vector2 = Vector2.ZERO
)

data class Color(
    val r: Float,
    val g: Float,
    val b: Float,
    val a: Float = 1f
) {
    companion object {
        val WHITE = Color(1f, 1f, 1f)
        val BLACK = Color(0f, 0f, 0f)
        val RED = Color(1f, 0f, 0f)
        val GREEN = Color(0f, 1f, 0f)
        val BLUE = Color(0f, 0f, 1f)
        val YELLOW = Color(1f, 1f, 0f)
        val BROWN = Color(0.6f, 0.3f, 0.1f)
        val SKY_BLUE = Color(0.5f, 0.8f, 1f)
    }
}

data class AssetMemoryInfo(
    val textureCount: Int,
    val soundCount: Int,
    val musicCount: Int,
    val animationCount: Int,
    val totalMemoryMB: Long
)