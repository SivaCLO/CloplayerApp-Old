package com.cloreader.tts;

import java.util.HashMap;
import java.util.Locale;

import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.speech.tts.TextToSpeech;
import android.util.Log;

import com.cloreader.Session;
import com.cloreader.story.Story;
import com.cloreader.utils.MessageType;
import com.cloreader.utils.ServerConstants;

public class TTSManager implements TextToSpeech.OnUtteranceCompletedListener,
		TextToSpeech.OnInitListener {

	private TextToSpeech myTTS;
	private boolean isReading = false;

	// Called When TTS is created
	public void onInit(int initStatus) {

		// If TTS Present
		if (initStatus == TextToSpeech.SUCCESS) {
			initializeTTS();
		} else if (initStatus == TextToSpeech.ERROR) {
			Log.e(ServerConstants.TAG, "Sorry! Text To Speech failed...");
			return;
		}

		// TTS Check Successful. Load Main Screen
		Session.mainActivity.loadMainScreen();

	}

	public void onUtteranceCompleted(final String s) {

		final MessageType messageType = MessageType.valueOf(s);

		if (messageType == MessageType.NEWS_ITEM && isReading) {
			isReading = false;			
			Session.storyManager.promoteStory(1, 0);
		}
	}

	public void stopReading(boolean dialog) {

		if (dialog) {
			Session.mainActivity.showPauseDialog();			
		}

		isReading = false;
		if (myTTS != null)
			myTTS.stop();
	}

	public void startReading() {

		final Story story = Session.storyManager.getCurrentStory();
		if (!Session.isInitialized || isReading || story == null
				|| !story.isLoaded() || story.isNoStories())
			return;
		readMessage(
				cleanText(".." + story.getHeadLine()) + "..."
						+ cleanText(story.getDetailText()),
				MessageType.NEWS_ITEM);

		Thread t = new Thread(new Runnable() {
			public void run() {
				if (!story.isRead())
					Session.apiClient.markStory(story.getStoryId());
			}
		});
		t.start();
	}

	public String cleanText(String text) {
		return text.replaceAll("\\<.*?>", "").replace("&", " and ")
				.replace(";", ".").replace("-", " ").replace("'", "")
				.replace("\"", "").replace("[", " ").replace("]", " ");
	}

	private void readMessage(String message, MessageType messageType) {
		if (!message.equals("")) {
			HashMap<String, String> params = new HashMap<String, String>();
			params.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID,
					messageType.name());
			params.put(TextToSpeech.Engine.KEY_PARAM_STREAM,
					String.valueOf(AudioManager.STREAM_NOTIFICATION));
			myTTS.speak(message, TextToSpeech.QUEUE_FLUSH, params);

			isReading = true;
			Session.mainActivity.cancelPauseDialog();
		}
	}

	public boolean isReading() {
		return isReading;
	}

	public void setReading(boolean isReading) {
		this.isReading = isReading;
	}

	public void checkTTS() {
		Intent checkTTSIntent = new Intent();
		checkTTSIntent.setAction(TextToSpeech.Engine.ACTION_CHECK_TTS_DATA);
		Session.mainActivity.startActivityForResult(checkTTSIntent,
				ServerConstants.ACTIVITY_DATA_CHECK);
	}

	private void initializeTTS() {
		if (myTTS.isLanguageAvailable(Locale.US) == TextToSpeech.LANG_AVAILABLE)
			myTTS.setLanguage(Locale.US);
		myTTS.setSpeechRate(0.90F);
		myTTS.setPitch(0.90F);
		myTTS.setOnUtteranceCompletedListener(this);
	}

	public void createTTS() {
		myTTS = new TextToSpeech(Session.mainActivity, this);
	}

	public void installTTS() {
		Intent installTTSIntent = new Intent();
		installTTSIntent.setAction(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA);
		Session.mainActivity.startActivity(installTTSIntent);
	}

	public void toggleReading() {
		if (isReading) {
			stopReading(true);
		} else {
			startReading();
		}
	}

	public void beep() {
		try {
			Uri soundUri = RingtoneManager
					.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
			MediaPlayer mMediaPlayer = new MediaPlayer();
			mMediaPlayer.setDataSource(Session.mainActivity, soundUri);
			final AudioManager audioManager = (AudioManager) Session.mainActivity
					.getSystemService(Context.AUDIO_SERVICE);
			if (audioManager.getStreamVolume(AudioManager.STREAM_ALARM) != 0) {
				mMediaPlayer.setAudioStreamType(AudioManager.STREAM_ALARM);				
				mMediaPlayer.prepare();
				mMediaPlayer.start();
			}
		} catch (Exception e) {

		}
	}
}
