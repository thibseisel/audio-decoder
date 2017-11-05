package fr.ts.audiodecoder

import android.content.ContentUris
import android.database.Cursor
import android.media.MediaFormat
import android.net.Uri
import android.os.AsyncTask
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore.Audio.Media
import android.support.v4.app.LoaderManager
import android.support.v4.content.CursorLoader
import android.support.v4.content.Loader
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.widget.Button
import android.widget.ListView
import android.widget.Toast
import java.io.IOException
import java.lang.ref.WeakReference
import java.nio.ShortBuffer

class MainActivity : AppCompatActivity(),
        AudioDecoder.Listener,
        LoaderManager.LoaderCallbacks<Cursor> {

    private lateinit var mDecoder: AudioDecoder
    private lateinit var mPlayer: MusicPlayer
    private lateinit var mAdapter: TrackAdapter

    private var mTask: DecoderTask? = null

    private var mPlaying = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mAdapter = TrackAdapter(this, null)

        findViewById<ListView>(R.id.listView).apply {
            adapter = mAdapter
            isFastScrollEnabled = true
            setOnItemClickListener { _, _, _, id ->
                val uri = ContentUris.withAppendedId(Media.EXTERNAL_CONTENT_URI, id)
                startDecoder(uri)
            }
        }

        findViewById<Button>(R.id.buttonStop).setOnClickListener {
            if (mPlaying) {
                mTask?.cancel(true)
                mPlayer.stop()
                mPlaying = false
            }
        }

        supportLoaderManager.initLoader(0, null, this)
    }

    private fun startDecoder(contentUri: Uri) {
        mPlayer = MusicPlayer()
        mDecoder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            AudioDecoderLollipop()
        } else {
            AudioDecoderBase()
        }

        mTask = DecoderTask(this, mDecoder).also {
            it.execute(contentUri)
        }
    }

    override fun onFramesAvailable(frames: ShortBuffer) {
        Log.v(TAG, "Frames available. Size=${frames.capacity()}")
        mPlayer.feed(frames)
    }

    override fun onOutputFormatChanged(outputFormat: MediaFormat) {
        Log.d(TAG, "New outputFormat: $outputFormat")
        val sampleRate = outputFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE)
        mPlayer.configure(sampleRate)
        mPlayer.start()
        mPlaying = true
    }

    override fun onError() {
        Log.e(TAG, "A decoder error occurred.")
    }

    override fun onFinished() {
        Log.i(TAG, "Decoding is finished !")
    }

    override fun onCreateLoader(id: Int, args: Bundle?): Loader<Cursor> {
        return CursorLoader(this, Media.EXTERNAL_CONTENT_URI,
                arrayOf(Media._ID, Media.TITLE, Media.ARTIST),
                "${Media.IS_MUSIC} = 1", null, Media.TITLE_KEY)
    }

    override fun onLoadFinished(loader: Loader<Cursor>, data: Cursor?) {
        mAdapter.swapCursor(data)
        mAdapter.notifyDataSetChanged()
    }

    override fun onLoaderReset(loader: Loader<Cursor>) {
        mAdapter.swapCursor(null)
        mAdapter.notifyDataSetInvalidated()
    }

    override fun onDestroy() {
        mTask?.cancel(true)
        mDecoder.reset()
        mPlayer.release()
        super.onDestroy()
    }

    private class DecoderTask(
            activity: MainActivity,
            private val decoder: AudioDecoder
    ) : AsyncTask<Uri, Unit, Unit>() {

        private val contextRef = WeakReference<MainActivity>(activity)

        override fun doInBackground(vararg params: Uri) {
            contextRef.get()?.let { activity ->
                if (params.isEmpty()) return

                try {
                    decoder.configure(activity, params[0], activity)
                    decoder.start()
                } catch (ioe: IOException) {
                    Log.i(TAG, "Cannot read from media source", ioe)
                    activity.runOnUiThread {
                        Toast.makeText(activity, "Unable to read media", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        override fun onCancelled(result: Unit) {
            // No way to cancel at the current time
        }
    }

    private companion object {
        const val TAG = "MainActivity"
    }
}
