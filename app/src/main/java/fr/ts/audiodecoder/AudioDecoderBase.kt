package fr.ts.audiodecoder

import android.annotation.TargetApi
import android.content.Context
import android.media.AudioFormat
import android.media.AudioTrack
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import android.util.Log

import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.ShortBuffer

private const val TAG = "AudioDecoderBase"

/**
 * An implementation
 */
class AudioDecoderBase : AudioDecoder {

    internal var mExtractor: MediaExtractor? = null
    internal var mDecoder: MediaCodec? = null
    internal var mListener: AudioDecoder.SampleListener = DefaultSampleListener

    override fun configure(context: Context, contentUri: Uri, listener: AudioDecoder.SampleListener) {
        val extractor = MediaExtractor().apply {
            setDataSource(context, contentUri, null)
        }

        if (extractor.trackCount == 0) {
            throw IOException("No track available through this URI.")
        }

        val inputFormat = extractor.getTrackFormat(0)
        val sampleRate = inputFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE)
        val mimeType = inputFormat.getString(MediaFormat.KEY_MIME)
        Log.d(TAG, "Input format: " + inputFormat.toString())

        val decoder = MediaCodec.createDecoderByType(mimeType)
        decoder.configure(inputFormat, null, null, 0)

        val outputFormat = decoder.outputFormat
        Log.d(TAG, "Output format: " + outputFormat.toString())

        val minBufferSize = AudioTrack.getMinBufferSize(sampleRate,
                AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_16BIT)
        Log.d(TAG, "configure: minBufferSize=" + minBufferSize)

        mExtractor = extractor
        mDecoder = decoder
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
                var sampleSize = extractor.readSampleData(inputs[inputIndex], 0)
                Log.d(TAG, "Bytes read from source : " + sampleSize)
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
                    assert(presentationTimeUs != -1L)
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
                    val buffer = outputs[outputIndex]
                    val samples = buffer.order(ByteOrder.nativeOrder()).asShortBuffer()

                    // Make those samples available to the client
                    mListener.onSamplesAvailable(samples)

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
                } else {
                    Log.w(TAG, "Unexpected error code: $outputIndex")
                }
            }
        }

        // Notify clients that the decoding for this track is finished
        mListener.onFinished()
    }

    override fun reset() {
        mExtractor?.release()
        mDecoder?.release()
    }

    internal object DefaultSampleListener : AudioDecoder.SampleListener {
        override fun onSamplesAvailable(data: ShortBuffer) {
            // Do nothing
        }

        override fun onFinished() {
            // Do nothing
        }

    }
}
