package com.cloplayer;

import java.util.HashMap;
import java.util.Locale;

import android.content.Context;
import android.media.AudioManager;
import android.speech.tts.TextToSpeech;

import com.cloplayer.story.CloplayerStory;

public class TTSManager { //implements TextToSpeech.OnUtteranceCompletedListener, TextToSpeech.OnInitListener {

	/*private TextToSpeech myTTS;
	private boolean isReading = false;

	public TTSManager(Context c) {
		myTTS = new TextToSpeech(c, this);
	}

	public void onInit(int initStatus) {
		if (initStatus == TextToSpeech.SUCCESS) {
			if (myTTS.isLanguageAvailable(Locale.US) == TextToSpeech.LANG_AVAILABLE)
				myTTS.setLanguage(Locale.US);
			//myTTS.setSpeechRate(0.90F);
			//myTTS.setPitch(0.90F);
			myTTS.setOnUtteranceCompletedListener(this);
		} else if (initStatus == TextToSpeech.ERROR) {
			return;
		}
	}

	public void onUtteranceCompleted(final String s) {
		CloplayerService.getInstace().stopForeground(true);
	}

	public void readMessage(String message, MessageType messageType) {
		if (!message.equals("")) {
			HashMap<String, String> params = new HashMap<String, String>();
			params.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, messageType.name());
			params.put(TextToSpeech.Engine.KEY_PARAM_STREAM, String.valueOf(AudioManager.STREAM_NOTIFICATION));
			myTTS.speak(message, TextToSpeech.QUEUE_FLUSH, params);
			isReading = true;
		}
	}

	public void stopReading() {
		isReading = false;
		if (myTTS != null)
			myTTS.stop();
	}

	public void readStory(final CloplayerStory story, String prefix) {
		readMessage(prefix + ".." + story.getHeadLine() + "..." + story.getDetailText(), MessageType.NEWS_ITEM);
	}

	public String cleanText(String text) {
		return text.replaceAll("\\<.*?>", "").replace("&", " and ").replace(";", ".").replace("-", " ").replace("'", "").replace("\"", "").replace("[", " ").replace("]", " ");
	}*/
}
