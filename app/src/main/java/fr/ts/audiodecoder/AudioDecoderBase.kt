package fr.ts.audiodecoder

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import android.util.Log
import java.io.IOException
import java.nio.ByteOrder

private const val TAG = "AudioDecoderBase"

/**
 * Base implementation of AudioDecoder that should be used on versions earlier than API 21.
 * This uses the synchronous API of MediaCodec with array of buffers.
 */
internal open class AudioDecoderBase : AudioDecoder {

    internal var mExtractor: MediaExtractor? = null
    internal var mDecoder: MediaCodec? = null
    internal var mListener: AudioDecoder.Listener = AudioDecoder.DefaultListener

    override fun configure(context: Context, contentUri: Uri, listener: AudioDecoder.Listener) {
        val extractor = MediaExtractor().apply {
            setDataSource(context, contentUri, null)
        }

        if (extractor.trackCount == 0) {
            throw IOException("No track available through this URI.")
        }

        val inputFormat = extractor.getTrackFormat(0)
        val mimeType = inputFormat.getString(MediaFormat.KEY_MIME)
        Log.d(TAG, "Input format: " + inputFormat.toString())

        val decoder = MediaCodec.createDecoderByType(mimeType)
        decoder.configure(inputFormat, null, null, 0)

        mExtractor = extractor
        mDecoder = decoder
        mListener = listener
    }

    @Suppress("DEPRECATION")
    override fun start() {
        val extractor = checkNotNull(mExtractor) { "DataSource is not set" }
        val decoder = checkNotNull(mDecoder) { "DataSource is not set" }

        decoder.start()
        val inputs = decoder.inputBuffers
        var outputs = decoder.outputBuffers
        val info = MediaCodec.BufferInfo()

        extractor.selectTrack(0)
        var sawEndOfInput = false
        var sawEndOfOutput = false

        val timeoutUs = 10000L

        // Loop until we have no more data to output
        while (!sawEndOfOutput) {

            // Get the input buffer id
            val inputIndex = decoder.dequeueInputBuffer(timeoutUs)
            if (inputIndex >= 0) {
                // An input buffer is available (unavailable = -1)
                inputs[inputIndex].clear()
                var sampleSize = extractor.readSampleData(inputs[inputIndex], 0)
                Log.d(TAG, "Bytes read from source : $sampleSize")
                var presentationTimeUs: Long = 0

                if (sampleSize < 0) {
                    // No more samples are available
                    Log.d(TAG, "End of input.")
                    sawEndOfInput = true
                    sampleSize = 0
                } else {
                    // The position of this sample in the track, in microseconds
                    // Since samples are available, this could never be -1
                    presentationTimeUs = extractor.sampleTime

                    // Manually sets the limit and position to point to the data just read
                    with(inputs[inputIndex]) {
                        position(0)
                        limit(sampleSize)
                    }
                }

                // Send the filled buffer back to the decoder
                decoder.queueInputBuffer(inputIndex, 0, sampleSize, presentationTimeUs,
                        if (sawEndOfInput) MediaCodec.BUFFER_FLAG_END_OF_STREAM else 0)

                if (!sawEndOfInput) {
                    // Move to the next sample
                    extractor.advance()
                }

                // Get decoded samples
                val outputIndex = decoder.dequeueOutputBuffer(info, timeoutUs)
                if (outputIndex >= 0) {
                    val buffer = outputs[outputIndex].apply {
                        position(0)
                        limit(info.size)
                    }

                    val samples = buffer.order(ByteOrder.nativeOrder()).asShortBuffer()

                    // Make those samples available to the client
                    mListener.onFramesAvailable(samples)

                    // Send this buffer back to the decoder
                    decoder.releaseOutputBuffer(outputIndex, false)

                    if (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                        // End of the output stream
                        Log.d(TAG, "End of output")
                        sawEndOfOutput = true
                    }

                } else if (outputIndex == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                    Log.d(TAG, "Output buffers have changed")
                    outputs = decoder.outputBuffers
                } else if (outputIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    val outputFormat = decoder.outputFormat
                    Log.d(TAG, "New output format: $outputFormat")
                    mListener.onOutputFormatChanged(outputFormat)
                } else {
                    Log.w(TAG, "Unexpected error code: $outputIndex")
                    mListener.onError()
                }
            }
        }

        // Notify clients that the decoding for this track is finished
        decoder.stop()
        mListener.onFinished()
    }

    override fun reset() {
        mDecoder?.let {
            it.stop()
            it.release()
        }
        mDecoder?.release()
        mExtractor?.release()
    }

}
