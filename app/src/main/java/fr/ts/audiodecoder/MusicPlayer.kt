package fr.ts.audiodecoder

import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import java.nio.ShortBuffer

class MusicPlayer {
    private var audioTrack: AudioTrack? = null

    fun configure(sampleRate: Int) {
        val minBufferSize = AudioTrack.getMinBufferSize(sampleRate,
                AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_16BIT)
        audioTrack = AudioTrack(AudioManager.STREAM_MUSIC,
                sampleRate,
                AudioFormat.CHANNEL_OUT_STEREO,
                AudioFormat.ENCODING_PCM_16BIT,
                minBufferSize,
                AudioTrack.MODE_STREAM)
    }

    fun start() {
        val player = checkNotNull(audioTrack) { "MusicPlayer should be configured" }
        player.play()
    }

    fun stop() {
        val player = checkNotNull(audioTrack) { "MusicPlayer should be configured" }
        player.pause()
        player.flush()
    }

    fun feed(samples: ShortBuffer) {
        val player = checkNotNull(audioTrack)
        val chunk = ShortArray(samples.remaining())
        samples.get(chunk, 0, chunk.size)

        if (chunk.isNotEmpty()) {
            player.write(chunk, 0, chunk.size)
        }
    }

    fun release() {
        audioTrack?.release()
        audioTrack = null
    }
}