package fr.ts.audiodecoder

import android.content.Context
import android.net.Uri

import java.io.IOException
import java.nio.ShortBuffer

import android.media.AudioFormat.ENCODING_PCM_16BIT
import android.media.MediaFormat

/**
 * Read audio samples at a specific [Uri] as 16-bit PCM audio frames.
 */
interface AudioDecoder {

    /**
     * Configures this decoder to read from the specified [contentUri].
     * This should be called before starting the decoder.
     *
     * @throws IOException if no track can be found at the specified uri
     */
    @Throws(IOException::class)
    fun configure(context: Context, contentUri: Uri, listener: Listener)

    /**
     * Starts the decoding process.
     *
     * @throws IllegalStateException if no source has been configured,
     * or the decoder has been released.
     */
    fun start()

    /**
     * Reset this decoder, releasing any resources it may hold.
     * This decoder could be reused by associating a new data source.
     */
    fun reset()

    /**
     * Notify clients about events that occur during the decoding of media.
     */
    interface Listener {

        /**
         * Called when raw audio frames are available.
         * A frame is one sample for each channel in channel order.
         *
         * Each sample is a 16-bit signed integer in native byte order
         * as per [ENCODING_PCM_16BIT].
         *
         * The provided buffer should not be used after this method returns.
         *
         * @param frames A buffer containing PCM raw audio data as 16-bit signed integers
         */
        fun onFramesAvailable(frames: ShortBuffer)

        /**
         *
         */
        fun onOutputFormatChanged(outputFormat: MediaFormat)

        /**
         *
         */
        fun onError()

        /**
         * Called when the decoding of audio is finished.
         * The [onFramesAvailable] will no longer be called.
         */
        fun onFinished()
    }

    /**
     * A [AudioDecoder.Listener] instance that does nothing.
     */
    object DefaultListener : AudioDecoder.Listener {
        override fun onFramesAvailable(frames: ShortBuffer) {
            // Do nothing
        }

        override fun onOutputFormatChanged(outputFormat: MediaFormat) {
            // Do nothing
        }

        override fun onError() {
            // Do nothing
        }

        override fun onFinished() {
            // Do nothing
        }
    }
}