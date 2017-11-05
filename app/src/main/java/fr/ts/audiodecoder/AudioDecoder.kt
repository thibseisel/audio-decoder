package fr.ts.audiodecoder

import android.content.Context
import android.media.AudioFormat.ENCODING_PCM_16BIT
import android.media.MediaFormat
import android.net.Uri
import java.io.IOException
import java.nio.ShortBuffer

/**
 * Read audio samples at a specific [Uri] as 16-bit PCM audio frames.
 */
interface AudioDecoder {

    /**
     * Configures this decoder to read from the specified [contentUri].
     * This should be called before starting the decoder.
     *
     * @param context will be used to resolve the media content uri
     * @param contentUri the uri pointing to the media to decode
     * @param listener an object to react to various decoder events
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
         * Called when the output audio format has changed.
         * Subsequent calls to [onFramesAvailable] will emit frames of the provided `outputFormat`.
         *
         * @param outputFormat The new output format
         */
        fun onOutputFormatChanged(outputFormat: MediaFormat)

        /**
         * Called when a decoding error occurred.
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