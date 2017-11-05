package fr.ts.audiodecoder

import android.media.MediaCodec
import android.os.Build
import android.support.annotation.RequiresApi
import android.util.Log
import java.nio.ByteOrder

private const val TAG = "AudioDecoderLollipop"

/**
 * AudioDecoder implementation to be used on API 21 and above.
 * This uses the synchronous API of MediaCodec.
 */
@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
internal class AudioDecoderLollipop : AudioDecoderBase() {

    override fun start() {
        val extractor = checkNotNull(mExtractor) { "DataSource is not set" }
        val decoder = checkNotNull(mDecoder) { "DataSource is not set" }

        decoder.start()
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
                val inputBuffer = decoder.getInputBuffer(inputIndex)
                var sampleSize = extractor.readSampleData(inputBuffer, 0)
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
                    val outputBuffer = decoder.getOutputBuffer(outputIndex)
                    val samples = outputBuffer.order(ByteOrder.nativeOrder()).asShortBuffer()

                    // Make those samples available to the client
                    mListener.onFramesAvailable(samples)

                    // Send this buffer back to the decoder
                    decoder.releaseOutputBuffer(outputIndex, false)

                    if (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                        // End of the output stream
                        Log.d(TAG, "End of output")
                        sawEndOfOutput = true
                    }

                } else if (outputIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    val outputFormat = decoder.outputFormat
                    Log.d(TAG, "New output format: $outputFormat")
                    mListener.onOutputFormatChanged(outputFormat)
                } else if (outputIndex == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                    // Ignored deprecated code
                } else {
                    Log.w(TAG, "Unexpected error code: $outputIndex")
                    mListener.onError()
                }
            }
        }

        // Notify clients that the decoding for this track is finished
        mListener.onFinished()
    }
}