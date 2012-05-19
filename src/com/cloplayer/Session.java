package com.cloplayer;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.view.Display;

import com.cloplayer.http.APIClient;
import com.cloplayer.sphinx.SphinxManager;
import com.cloplayer.story.StoryManager;
import com.cloplayer.tts.TTSManager;
import com.cloplayer.utils.ServerConstants;

public class Session extends Service{

	public static HomeActivity mainActivity;
	public static StoryManager storyManager;
	public static TTSManager ttsManager;
	public static SphinxManager sphinxManager;
	public static APIClient apiClient;
	public static boolean isInitialized;

	public static void init(HomeActivity activity) {
		mainActivity = activity;

		Display display = mainActivity.getWindowManager().getDefaultDisplay();		
		ServerConstants.NUMBER_OF_STORIES = (display.getHeight() - ServerConstants.LOGO_HEIGHT) / ServerConstants.STORY_HEIGHT;
		
		storyManager = new StoryManager(ServerConstants.NUMBER_OF_STORIES);
		ttsManager = new TTSManager();
		sphinxManager = new SphinxManager();
		apiClient = new APIClient(mainActivity);
		isInitialized = false;
	}

	public static void setInitialized(boolean init) {
		isInitialized = init;
		sphinxManager.init_sphinx();
	}

	public static void stop() {
		if (isInitialized) {
			ttsManager.stopReading(false);
			sphinxManager.stop();
		}
	}

	public static void restart() {
		if (isInitialized) {
			ttsManager.startReading();
			sphinxManager.restart();
		}
	}

	public static boolean isInitialized() {
		return isInitialized;
	}

	@Override
	public IBinder onBind(Intent intent) {
		// TODO Auto-generated method stub
		return null;
	}
}
