import kotlin.math.*

class AudioManager {
    private val soundEffects = mutableMapOf<String, SoundEffect>()
    private val musicTracks = mutableMapOf<String, MusicTrack>()
    private val audioChannels = mutableListOf<AudioChannel>()
    
    private var masterVolume = 1.0f
    private var soundVolume = 1.0f
    private var musicVolume = 1.0f
    
    private var currentMusic: MusicTrack? = null
    private var musicChannel: AudioChannel? = null
    
    private var listenerPosition = Vector2(0f, 0f)
    
    fun initialize() {
        for (i in 0 until MAX_CHANNELS) {
            audioChannels.add(AudioChannel(i))
        }
        
        loadSoundEffects()
        loadMusicTracks()
    }
    
    fun shutdown() {
        stopAllSounds()
        stopMusic()
        soundEffects.clear()
        musicTracks.clear()
        audioChannels.clear()
    }
    
    fun loadSoundEffect(name: String, filePath: String): Boolean {
        return try {
            val soundEffect = SoundEffect(filePath)
            soundEffects[name] = soundEffect
            true
        } catch (e: Exception) {
            false
        }
    }
    
    fun loadMusicTrack(name: String, filePath: String): Boolean {
        return try {
            val musicTrack = MusicTrack(filePath)
            musicTracks[name] = musicTrack
            true
        } catch (e: Exception) {
            false
        }
    }
    
    fun playSound(name: String, volume: Float = 1.0f, pitch: Float = 1.0f, loop: Boolean = false): Int? {
        val sound = soundEffects[name] ?: return null
        val channel = getAvailableChannel() ?: return null
        
        val finalVolume = volume * soundVolume * masterVolume
        channel.play(sound, finalVolume, pitch, loop)
        
        return channel.id
    }
    
    fun playSoundAt(name: String, position: Vector2, volume: Float = 1.0f, pitch: Float = 1.0f, loop: Boolean = false): Int? {
        val sound = soundEffects[name] ?: return null
        val channel = getAvailableChannel() ?: return null
        
        val distance = listenerPosition.distanceTo(position)
        val attenuatedVolume = calculateVolumeAttenuation(volume, distance)
        val finalVolume = attenuatedVolume * soundVolume * masterVolume
        
        channel.playAt(sound, position, finalVolume, pitch, loop)
        
        return channel.id
    }
    
    fun stopSound(channelId: Int) {
        audioChannels.find { it.id == channelId }?.stop()
    }
    
    fun pauseSound(channelId: Int) {
        audioChannels.find { it.id == channelId }?.pause()
    }
    
    fun resumeSound(channelId: Int) {
        audioChannels.find { it.id == channelId }?.resume()
    }
    
    fun stopAllSounds() {
        audioChannels.forEach { it.stop() }
    }
    
    fun pauseAllSounds() {
        audioChannels.forEach { it.pause() }
    }
    
    fun resumeAllSounds() {
        audioChannels.forEach { it.resume() }
    }
    
    fun playMusic(name: String, volume: Float = 1.0f, loop: Boolean = true, fadeInTime: Float = 0f) {
        val music = musicTracks[name] ?: return
        
        stopMusic()
        
        musicChannel = getAvailableChannel()
        musicChannel?.let { channel ->
            val finalVolume = volume * musicVolume * masterVolume
            channel.play(music, finalVolume, 1.0f, loop)
            currentMusic = music
            
            if (fadeInTime > 0f) {
                fadeInMusic(fadeInTime)
            }
        }
    }
    
    fun stopMusic(fadeOutTime: Float = 0f) {
        if (fadeOutTime > 0f) {
            fadeOutMusic(fadeOutTime)
        } else {
            musicChannel?.stop()
            musicChannel = null
            currentMusic = null
        }
    }
    
    fun pauseMusic() {
        musicChannel?.pause()
    }
    
    fun resumeMusic() {
        musicChannel?.resume()
    }
    
    fun setMasterVolume(volume: Float) {
        masterVolume = volume.coerceIn(0f, 1f)
        updateAllVolumes()
    }
    
    fun setSoundVolume(volume: Float) {
        soundVolume = volume.coerceIn(0f, 1f)
        updateSoundVolumes()
    }
    
    fun setMusicVolume(volume: Float) {
        musicVolume = volume.coerceIn(0f, 1f)
        updateMusicVolume()
    }
    
    fun setListenerPosition(position: Vector2) {
        listenerPosition = position
        update3DAudio()
    }
    
    fun update(deltaTime: Float) {
        audioChannels.forEach { it.update(deltaTime) }
        updateFades(deltaTime)
    }
    
    fun playPlayerJump() = playSound("player_jump", 0.7f)
    fun playPlayerLand() = playSound("player_land", 0.6f)
    fun playPlayerHurt() = playSound("player_hurt", 0.8f)
    fun playPlayerDie() = playSound("player_die", 1.0f)
    fun playPlayerAttack() = playSound("player_attack", 0.7f)
    fun playPlayerPowerUp() = playSound("player_powerup", 0.9f)
    
    fun playEnemyHurt() = playSound("enemy_hurt", 0.6f)
    fun playEnemyDie() = playSound("enemy_die", 0.7f)
    fun playEnemyAttack() = playSound("enemy_attack", 0.6f)
    
    fun playCollectCoin() = playSound("collect_coin", 0.8f)
    fun playCollectPowerUp() = playSound("collect_powerup", 0.9f)
    fun playCollectKey() = playSound("collect_key", 0.7f)
    
    fun playCheckpoint() = playSound("checkpoint", 0.8f)
    fun playLevelComplete() = playSound("level_complete", 1.0f)
    fun playGameOver() = playSound("game_over", 1.0f)
    
    fun playMenuSelect() = playSound("menu_select", 0.6f)
    fun playMenuConfirm() = playSound("menu_confirm", 0.7f)
    fun playMenuCancel() = playSound("menu_cancel", 0.6f)
    
    fun playExplosion(position: Vector2) = playSoundAt("explosion", position, 1.0f)
    fun playFootstep(position: Vector2) = playSoundAt("footstep", position, 0.4f)
    
    private fun loadSoundEffects() {
        val soundFiles = mapOf(
            "player_jump" to "assets/audio/sfx/player_jump.wav",
            "player_land" to "assets/audio/sfx/player_land.wav",
            "player_hurt" to "assets/audio/sfx/player_hurt.wav",
            "player_die" to "assets/audio/sfx/player_die.wav",
            "player_attack" to "assets/audio/sfx/player_attack.wav",
            "player_powerup" to "assets/audio/sfx/player_powerup.wav",
            "enemy_hurt" to "assets/audio/sfx/enemy_hurt.wav",
            "enemy_die" to "assets/audio/sfx/enemy_die.wav",
            "enemy_attack" to "assets/audio/sfx/enemy_attack.wav",
            "collect_coin" to "assets/audio/sfx/collect_coin.wav",
            "collect_powerup" to "assets/audio/sfx/collect_powerup.wav",
            "collect_key" to "assets/audio/sfx/collect_key.wav",
            "checkpoint" to "assets/audio/sfx/checkpoint.wav",
            "level_complete" to "assets/audio/sfx/level_complete.wav",
            "game_over" to "assets/audio/sfx/game_over.wav",
            "menu_select" to "assets/audio/sfx/menu_select.wav",
            "menu_confirm" to "assets/audio/sfx/menu_confirm.wav",
            "menu_cancel" to "assets/audio/sfx/menu_cancel.wav",
            "explosion" to "assets/audio/sfx/explosion.wav",
            "footstep" to "assets/audio/sfx/footstep.wav"
        )
        
        soundFiles.forEach { (name, path) ->
            loadSoundEffect(name, path)
        }
    }
    
    private fun loadMusicTracks() {
        val musicFiles = mapOf(
            "menu_theme" to "assets/audio/music/menu_theme.ogg",
            "level1_theme" to "assets/audio/music/level1_theme.ogg",
            "level2_theme" to "assets/audio/music/level2_theme.ogg",
            "boss_theme" to "assets/audio/music/boss_theme.ogg",
            "victory_theme" to "assets/audio/music/victory_theme.ogg",
            "game_over_theme" to "assets/audio/music/game_over_theme.ogg"
        )
        
        musicFiles.forEach { (name, path) ->
            loadMusicTrack(name, path)
        }
    }
    
    private fun getAvailableChannel(): AudioChannel? {
        return audioChannels.find { !it.isPlaying }
    }
    
    private fun calculateVolumeAttenuation(baseVolume: Float, distance: Float): Float {
        val maxDistance = 500f
        val minDistance = 50f
        
        return when {
            distance <= minDistance -> baseVolume
            distance >= maxDistance -> 0f
            else -> {
                val attenuation = 1f - ((distance - minDistance) / (maxDistance - minDistance))
                baseVolume * attenuation
            }
        }
    }
    
    private fun updateAllVolumes() {
        updateSoundVolumes()
        updateMusicVolume()
    }
    
    private fun updateSoundVolumes() {
        audioChannels.forEach { channel ->
            if (channel.isPlaying && channel != musicChannel) {
                channel.updateVolume(soundVolume * masterVolume)
            }
        }
    }
    
    private fun updateMusicVolume() {
        musicChannel?.updateVolume(musicVolume * masterVolume)
    }
    
    private fun update3DAudio() {
        audioChannels.forEach { channel ->
            if (channel.isPlaying && channel.has3DPosition) {
                val distance = listenerPosition.distanceTo(channel.position)
                val attenuatedVolume = calculateVolumeAttenuation(channel.baseVolume, distance)
                channel.updateVolume(attenuatedVolume * soundVolume * masterVolume)
            }
        }
    }
    
    private fun fadeInMusic(duration: Float) {
    }
    
    private fun fadeOutMusic(duration: Float) {
    }
    
    private fun updateFades(deltaTime: Float) {
    }
    
    companion object {
        private const val MAX_CHANNELS = 32
    }
}

class SoundEffect(private val filePath: String) {
    val duration: Float = 1.0f
    val sampleRate: Int = 44100
    
    fun load(): Boolean {
        return true
    }
    
    fun unload() {
    }
}

class MusicTrack(private val filePath: String) {
    val duration: Float = 120.0f
    val isLooping: Boolean = true
    
    fun load(): Boolean {
        return true
    }
    
    fun unload() {
    }
}

class AudioChannel(val id: Int) {
    var isPlaying: Boolean = false
        private set
    
    var isPaused: Boolean = false
        private set
    
    var volume: Float = 1.0f
        private set
    
    var pitch: Float = 1.0f
        private set
    
    var position: Vector2 = Vector2(0f, 0f)
        private set
    
    var has3DPosition: Boolean = false
        private set
    
    var baseVolume: Float = 1.0f
        private set
    
    private var currentSound: Any? = null
    private var playbackTime: Float = 0f
    
    fun play(sound: Any, volume: Float, pitch: Float, loop: Boolean) {
        this.currentSound = sound
        this.volume = volume
        this.baseVolume = volume
        this.pitch = pitch
        this.isPlaying = true
        this.isPaused = false
        this.has3DPosition = false
        this.playbackTime = 0f
    }
    
    fun playAt(sound: Any, position: Vector2, volume: Float, pitch: Float, loop: Boolean) {
        play(sound, volume, pitch, loop)
        this.position = position
        this.has3DPosition = true
    }
    
    fun stop() {
        isPlaying = false
        isPaused = false
        currentSound = null
        playbackTime = 0f
        has3DPosition = false
    }
    
    fun pause() {
        if (isPlaying) {
            isPaused = true
        }
    }
    
    fun resume() {
        if (isPlaying && isPaused) {
            isPaused = false
        }
    }
    
    fun updateVolume(newVolume: Float) {
        volume = newVolume
    }
    
    fun update(deltaTime: Float) {
        if (isPlaying && !isPaused) {
            playbackTime += deltaTime
        }
    }
}