package fr.ts.audiodecoder

import android.content.Context
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
     * @throws IOException if no track can be found at the specified uri
     */
    @Throws(IOException::class)
    fun configure(context: Context, contentUri: Uri, listener: SampleListener)

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
     *
     */
    interface SampleListener {

        /**
         *
         */
        fun onSamplesAvailable(data: ShortBuffer)

        /**
         *
         */
        fun onFinished()
    }
}
