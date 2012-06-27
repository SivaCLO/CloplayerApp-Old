package com.cloplayer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;

import com.cloplayer.http.AsyncHTTPClient;
import com.cloplayer.http.URLHelper;
import com.cloplayer.sqlite.Story;
import com.cloplayer.sqlite.StoryDataSource;
import com.cloplayer.utils.ServerConstants;

public class CloplayerService extends Service {

	// private NotificationManager nm;
	private static boolean isRunning = false;

	final Messenger mService = new Messenger(new IncomingHandler());

	ArrayList<Messenger> mClients = new ArrayList<Messenger>();

	public static final int MSG_REGISTER_CLIENT = 1;
	public static final int MSG_UNREGISTER_CLIENT = 2;
	public static final int MSG_PLAY_SOURCE = 3;
	public static final int MSG_PAUSE_UNPAUSE_PLAYING = 4;
	public static final int MSG_PLAY_ALL = 5;
	public static final int MSG_STORE_SOURCE = 6;
	public static final int MSG_PLAY_NEXT = 7;

	public static final int MSG_NEXT1 = 11;
	public static final int MSG_NEXT5 = 12;
	public static final int MSG_BACK1 = 13;
	public static final int MSG_BACK5 = 14;

	public static final int MSG_UPDATE_STORY = 51;
	public static final int MSG_ADD_STORY = 52;

	public static final int MSG_REFRESH_ARTICLES = 71;
	public static final int MSG_REFRESH_ARTICLES_COMPLETE = 72;
	public static final int MSG_BOOTSTRAP_COMPLETE = 73;

	public static final int MSG_NETWORK_ERROR = 80;

	public static final int CATEGORY_UNREAD = 0;
	public static final int CATEGORY_READ = 1;

	private static CloplayerService instance;
	public boolean isPaused = false;

	public StoryDataSource datasource = new StoryDataSource(this);
	public Story currentStory;

	List<Story> playlist;

	public HashMap<String, byte[]> cache = new HashMap<String, byte[]>();

	public static CloplayerService getInstance() {
		return instance;
	}

	public static final int MODE_START = 0;
	public static final int MODE_ONLINE = 1;
	public static final int MODE_OFFLINE = -1;

	public int mode = MODE_ONLINE;

	@Override
	public void onCreate() {
		super.onCreate();
		instance = this;
		datasource.open();
		isRunning = true;

		SharedPreferences globalSettings = CloplayerService.getInstance().getSharedPreferences(ServerConstants.CLOPLAYER_GLOBAL_PREFS, 0);
		SharedPreferences.Editor editor = globalSettings.edit();
		editor.remove("nowPlaying");
		editor.remove("nextPlaying");
		editor.remove("refreshing");
		editor.commit();

		Log.i("CloplayerService", "Service Started.");
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		return START_STICKY;
	}

	@Override
	public IBinder onBind(Intent intent) {
		return mService.getBinder();
	}

	class IncomingHandler extends Handler {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case MSG_REGISTER_CLIENT:
				mClients.add(msg.replyTo);
				break;
			case MSG_UNREGISTER_CLIENT:
				mClients.remove(msg.replyTo);
				break;
			case MSG_STORE_SOURCE:
				storeSource(msg);
				break;
			case MSG_PLAY_SOURCE:
				playSource(msg);
				break;
			case MSG_PLAY_ALL:
				playAll(msg.arg1);
				break;
			case MSG_PLAY_NEXT:
				playNext();
				break;
			case MSG_REFRESH_ARTICLES:
				refreshArticles();
				break;
			case MSG_PAUSE_UNPAUSE_PLAYING:
				pauseReading();
				break;
			case MSG_NEXT1:
				currentStory.scroll(1);
				break;
			case MSG_NEXT5:
				currentStory.scroll(5);
				break;
			case MSG_BACK1:
				currentStory.scroll(-1);
				break;
			case MSG_BACK5:
				currentStory.scroll(-5);
				break;
			default:
				super.handleMessage(msg);
			}
		}
	}

	public void playAll(int category) {

		switch (category) {
		case CATEGORY_READ:
			playlist = datasource.getPlayedStories();
			break;
		case CATEGORY_UNREAD:
			playlist = datasource.getUnplayedStories();
			break;
		}

		playNext();
	}

	public void playNext() {
		if (playlist != null && playlist.size() > 0)
			playStory(playlist.remove(0));
	}

	public Story getNext() {
		if (playlist != null && playlist.size() > 0)
			return playlist.get(0);
		return null;
	}

	public String cleanUrl(String url) {

		String result = url;

		int index = url.indexOf('?');

		if (index >= 0)
			result = url.substring(0, index);

		index = result.indexOf('&');

		if (index >= 0)
			result = result.substring(0, index);

		return result;
	}

	private void refreshArticles() {

		SharedPreferences globalSettings = CloplayerService.getInstance().getSharedPreferences(ServerConstants.CLOPLAYER_GLOBAL_PREFS, 0);
		final SharedPreferences.Editor editor = globalSettings.edit();
		editor.putBoolean("refreshing", true);
		editor.commit();

		String userId = globalSettings.getString("userId", null);

		AsyncHTTPClient client = new AsyncHTTPClient(URLHelper.list(userId)) {

			public void onSuccessResponse(String response) {
				try {

					JSONObject content = new JSONObject(response);
					JSONArray articles = content.getJSONArray("articles");

					for (int i = 0; i < articles.length(); i++) {
						downloadSource(cleanUrl(articles.getString(i)));
					}

					editor.putBoolean("refreshing", false);
					editor.commit();

					CloplayerService.getInstance().sendEmptyMessageToUI(CloplayerService.MSG_REFRESH_ARTICLES_COMPLETE);
					mode = MODE_ONLINE;

				} catch (JSONException e) {
					e.printStackTrace();
				}
			}

			public void onErrorResponse(Exception e) {
				Log.e("CloplayerService", "Error", e);

				editor.putBoolean("refreshing", false);
				editor.commit();

				CloplayerService.getInstance().sendEmptyMessageToUI(CloplayerService.MSG_NETWORK_ERROR);
				mode = MODE_OFFLINE;
			}
		};

		client.execute();
	}

	private void playStory(Story story) {

		if (story != null) {
			isPaused = false;

			if (currentStory == null || currentStory != story) {
				stopReading();
			}

			currentStory = story;

			if (currentStory.getState() < Story.STATE_BOOTSTRAPPED) {
				currentStory.bootstrap();
			}

			if (mode != MODE_OFFLINE) {

				if (currentStory.getState() < Story.STATE_DOWNLOADED) {
					currentStory.download();
				}
				currentStory.play();
			}
		}

		refreshArticles();

	}

	private void playSource(Message msg) {

		String sourceUrl = cleanUrl((String) msg.obj);

		Story story = datasource.findStory(sourceUrl);
		if (story == null) {
			story = datasource.addStory(sourceUrl);
		}

		playlist = new ArrayList<Story>();

		playStory(story);
	}

	private void storeSource(Message msg) {
		isPaused = false;
		String sourceUrl = cleanUrl((String) msg.obj);
		downloadSource(sourceUrl);

		refreshArticles();
	}

	private void downloadSource(String sourceUrl) {

		Story story = datasource.findStory(sourceUrl);
		if (story == null) {
			story = datasource.addStory(sourceUrl);
		}

		if (story.getState() < Story.STATE_BOOTSTRAPPED) {
			story.bootstrap();
		}

		if (story.getState() < Story.STATE_DOWNLOADED && mode != MODE_OFFLINE) {
			story.download();
		}
	}

	private void stopReading() {
		if (currentStory != null)
			currentStory.stopPlaying();
	}

	private void pauseReading() {

		if (currentStory != null) {
			if (isPaused) {
				currentStory.resume();
				isPaused = false;
			} else {
				currentStory.stopPlaying();
				isPaused = true;
			}
		}

	}

	public void sendEmptyMessageToUI(int messageId) {
		for (int i = mClients.size() - 1; i >= 0; i--) {
			try {
				mClients.get(i).send(Message.obtain(null, messageId));
			} catch (RemoteException e) {
				mClients.remove(i);
			}
		}
	}

	public void sendIntMessageToUI(int messageId, int value) {
		for (int i = mClients.size() - 1; i >= 0; i--) {
			try {
				mClients.get(i).send(Message.obtain(null, messageId, value, 0));
			} catch (RemoteException e) {
				mClients.remove(i);
			}
		}
	}

	public void sendStringMessageToUI(int messageId, String value) {
		for (int i = mClients.size() - 1; i >= 0; i--) {
			try {
				mClients.get(i).send(Message.obtain(null, messageId, value));
			} catch (RemoteException e) {
				mClients.remove(i);
			}
		}
	}

	public void showNotification(String tickerText, String contentText) {
		// nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		Notification notification = new Notification(R.drawable.logo, tickerText, System.currentTimeMillis());
		PendingIntent contentIntent = PendingIntent.getActivity(this, 0, new Intent(this, PlayerActivity.class), 0);
		notification.setLatestEventInfo(this, getText(R.string.cloplayer_notification), contentText, contentIntent);
		startForeground(R.string.cloplayer_notification, notification);
	}

	public static boolean isRunning() {
		return isRunning;
	}

	@Override
	public void onDestroy() {

		SharedPreferences globalSettings = CloplayerService.getInstance().getSharedPreferences(ServerConstants.CLOPLAYER_GLOBAL_PREFS, 0);
		SharedPreferences.Editor editor = globalSettings.edit();
		editor.remove("nowPlaying");
		editor.remove("nextPlaying");
		editor.remove("refreshing");
		editor.commit();

		super.onDestroy();

		stopReading();

		datasource.close();

		isRunning = false;
		Log.i("CloplayerService", "Service Stopped.");
	}
}