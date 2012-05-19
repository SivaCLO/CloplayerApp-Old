package com.spa;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.speech.tts.TextToSpeech.OnInitListener;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import edu.cmu.sphinx.frontend.util.AudioFileDataSource;
import edu.cmu.sphinx.linguist.language.grammar.NoSkipGrammar;
import edu.cmu.sphinx.recognizer.Recognizer;
import edu.cmu.sphinx.result.Result;
import edu.cmu.sphinx.util.props.ConfigurationManager;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

public class SpeakingAndroid extends Activity implements OnClickListener,
		OnInitListener, TextToSpeech.OnUtteranceCompletedListener {

	// TTS object
	private TextToSpeech myTTS;
	// status check code
	private int MY_DATA_CHECK_CODE = 0;
	private ExtAudioRecorder extAudioRecorder;

	// create the Activity
	public void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		// get a reference to the button element listed in the XML layout
		Button speakButton = (Button) findViewById(R.id.speak);
		// listen for clicks
		speakButton.setOnClickListener(this);

		// check for TTS data
		Intent checkTTSIntent = new Intent();
		checkTTSIntent.setAction(TextToSpeech.Engine.ACTION_CHECK_TTS_DATA);
		startActivityForResult(checkTTSIntent, MY_DATA_CHECK_CODE);

		// Start recording
		// extAudioRecorder = ExtAudioRecorder.getInstanse(true); // Compressed
		// recording (AMR)
		extAudioRecorder = ExtAudioRecorder.getInstanse(false); // Uncompressed
																// recording
																// (WAV)

		extAudioRecorder.setOutputFile("/sdcard/sample1.wav");
		extAudioRecorder.prepare();
		extAudioRecorder.start();

		new Timer().scheduleAtFixedRate(new CloreaderTask(this), 10000, 10000);
	}

	class CloreaderTask extends TimerTask {

		public Context context;

		public CloreaderTask(Context context) {
			this.context = context;
		}

		int counter = 1;

		@Override
		public void run() {
			extAudioRecorder.stop();
			extAudioRecorder.release();

			ConfigurationManager cm = new ConfigurationManager(
					"/sdcard/config.xml");
			Recognizer recognizer = (Recognizer) cm.lookup("recognizer");
			NoSkipGrammar grammar = (NoSkipGrammar) cm.lookup("NoSkipGrammar");

			grammar.addKeyword("testing");

            AudioFileDataSource dataSource = (AudioFileDataSource) cm
            			.lookup("audioFileDataSource");

			try {
				dataSource.setAudioFile(new URL("/sdcard/sample" + counter
						+ ".wav"), null);
			} catch (MalformedURLException e) {
				e.printStackTrace();
			}

			recognizer.allocate();
			Result result = recognizer.recognize();
			Toast.makeText(context, result.getTimedBestResult(false, true),
					Toast.LENGTH_SHORT).show();

			counter++;

			if (counter <= 5) {
				extAudioRecorder = ExtAudioRecorder.getInstanse(false);
				extAudioRecorder.setOutputFile("/sdcard/sample" + counter
						+ ".wav");
				extAudioRecorder.prepare();
				extAudioRecorder.start();
			} else {
				this.cancel();
			}
		}
	}

	// respond to button clicks
	public void onClick(View v) {

		// get the text entered
		EditText enteredText = (EditText) findViewById(R.id.enter);
		String words = enteredText.getText().toString();
		speakWords(words);
	}

	// speak the user text
	private void speakWords(String speech) {

		// speak straight away
		HashMap<String, String> params = new HashMap<String, String>();
		params.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "stringId");
		myTTS.speak(speech, TextToSpeech.QUEUE_FLUSH, params);
	}

	// act on result of TTS data check
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {

		if (requestCode == MY_DATA_CHECK_CODE) {
			if (resultCode == TextToSpeech.Engine.CHECK_VOICE_DATA_PASS) {
				// the user has the necessary data - create the TTS
				myTTS = new TextToSpeech(this, this);
			} else {
				// no data - install it now
				Intent installTTSIntent = new Intent();
				installTTSIntent
						.setAction(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA);
				startActivity(installTTSIntent);
			}
		}
	}

	// setup TTS
	public void onInit(int initStatus) {

		// check for successful instantiation
		if (initStatus == TextToSpeech.SUCCESS) {
			if (myTTS.isLanguageAvailable(Locale.US) == TextToSpeech.LANG_AVAILABLE)
				myTTS.setLanguage(Locale.US);
		} else if (initStatus == TextToSpeech.ERROR) {
			Toast.makeText(this, "Sorry! Text To Speech failed...",
					Toast.LENGTH_LONG).show();
		}

		myTTS.setOnUtteranceCompletedListener(this);
	}

	public void onUtteranceCompleted(String s) {
		runOnUiThread(new Runnable() {
			public void run() {
				return;
			}
		});
	}
}
