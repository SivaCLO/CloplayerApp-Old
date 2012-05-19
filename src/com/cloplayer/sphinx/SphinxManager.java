package com.cloplayer.sphinx;

import java.util.Timer;
import java.util.TimerTask;

import android.os.Bundle;
import android.os.Environment;
import android.util.Log;

import com.cloplayer.Session;
import com.cloplayer.utils.ServerConstants;

public class SphinxManager implements RecognitionListener {

	private RecognizerTask rec;
	private Thread rec_thread;
	private Timer t;
	private boolean paused = false;
	private String sphinxDir = Environment.getExternalStorageDirectory()
			.getAbsolutePath() + "/Android/data/edu.cmu.pocketsphinx";

	static {
		System.loadLibrary("pocketsphinx_jni");
	}

	public void stop() {

		paused = true;

		if (rec != null) {
			listening = false;
			rec.stop();
		}

		if (t != null)
			t.cancel();
	}

	public void restart() {

		paused = false;

		// readItem();
		if (rec != null) {
			rec.start();
			listening = true;
		}

		if (t != null)
			t.cancel();

		t = new Timer();
		t.scheduleAtFixedRate(new TimerTask() {
			public void run() {
				listening = false;

				rec.stop();
			}
		}, 30000, 30000);

	}

	public void init_sphinx() {
		this.rec = new RecognizerTask(sphinxDir);
		this.rec_thread = new Thread(this.rec);
		this.rec.setRecognitionListener(this);
		this.rec_thread.start();
		this.rec.start();
		listening = true;
	}

	private boolean listening;

	/** Called when partial results are generated. */
	public void onPartialResults(Bundle b) {
		final String hyp = b.getString("hyp");
		Session.mainActivity.runOnUiThread(new Runnable() {

			public void run() {
				if (listening) {

					if (Session.ttsManager.isReading() && hyp.contains("full")) {
						Session.ttsManager.stopReading(true);
						Log.d(ServerConstants.TAG, "Hello Detected");
						return;
					} 
					
					if (!Session.ttsManager.isReading()) {
						if (hyp.contains("next")) {
							stopListening();
							Session.storyManager.promoteStory(1, 0);
							Session.mainActivity.cancelPauseDialog();
						} else if (hyp.contains("back")) {
							stopListening();
							Session.storyManager.fetchReadStory();
							Session.mainActivity.cancelPauseDialog();
						} else if (hyp.contains("stop")) {
							stopListening();
							Session.mainActivity.cancelPauseDialog();
						} else if (hyp.contains("continue")) {
							stopListening();
							Session.ttsManager.startReading();
							Session.mainActivity.cancelPauseDialog();
						}
						return;
					}
				}
			}
		});

	}

	public void stopListening() {
		listening = false;
		rec.stop();
	}

	public void onResults(Bundle b) {
		if (paused == false) {
			rec.start();
			listening = true;
		}
	}

	public void onError(int err) {
		if (paused == false) {
			rec.start();
			listening = true;
		}
	}
}