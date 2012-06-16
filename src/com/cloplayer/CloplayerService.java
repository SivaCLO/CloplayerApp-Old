package com.cloplayer;

import java.util.ArrayList;
import java.util.HashMap;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;

import com.cloplayer.http.AsyncHTTPClient;
import com.cloplayer.sqlite.Story;
import com.cloplayer.sqlite.StoryDataSource;

public class CloplayerService extends Service {

	// private NotificationManager nm;
	private static boolean isRunning = false;

	final Messenger mService = new Messenger(new IncomingHandler());

	ArrayList<Messenger> mClients = new ArrayList<Messenger>();

	public static final int MSG_REGISTER_CLIENT = 1;
	public static final int MSG_UNREGISTER_CLIENT = 2;
	public static final int MSG_PLAY_SOURCE = 3;
	public static final int MSG_PAUSE_PLAYING = 4;
	public static final int MSG_UNPAUSE_PLAYING = 5;
	public static final int MSG_STORE_SOURCE = 6;

	public static final int MSG_NEXT1 = 11;
	public static final int MSG_NEXT5 = 12;
	public static final int MSG_BACK1 = 13;
	public static final int MSG_BACK5 = 14;

	public static final int MSG_UPDATE_STORY = 51;
	public static final int MSG_ADD_STORY = 52;

	public static final int MSG_REFRESH_ARTICLES = 71;
	public static final int MSG_REFRESH_ARTICLES_COMPLETE = 72;

	private static CloplayerService instance;
	public boolean isPaused = false;

	public StoryDataSource datasource = new StoryDataSource(this);
	public Story currentStory;

	public HashMap<String, byte[]> cache = new HashMap<String, byte[]>();

	public static CloplayerService getInstance() {
		return instance;
	}

	@Override
	public void onCreate() {
		super.onCreate();
		instance = this;
		datasource.open();
		isRunning = true;
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
			case MSG_REFRESH_ARTICLES:
				refreshArticles();
				break;
			case MSG_PAUSE_PLAYING:
				pauseReading();
				break;
			case MSG_UNPAUSE_PLAYING:
				unpauseReading();
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

		isPaused = false;

		AsyncHTTPClient client = new AsyncHTTPClient("http://api.cloplayer.com/api/list?userId=4fd920bee4b0d59877649ed6") {

			public void onSuccessResponse(String response) {
				try {

					JSONObject content = new JSONObject(response);
					JSONArray articles = content.getJSONArray("articles");

					for (int i = 0; i < articles.length(); i++) {
						downloadSource(cleanUrl(articles.getString(i)));						
					}

					CloplayerService.getInstance().sendEmptyMessageToUI(CloplayerService.MSG_REFRESH_ARTICLES_COMPLETE);

				} catch (JSONException e) {
					e.printStackTrace();
				}
			}

			public void onErrorResponse(Exception e) {
				Log.e("LoginActivity", "Error", e);
			}
		};

		client.execute();
	}

	private void playSource(Message msg) {

		isPaused = false;

		String sourceUrl = cleanUrl((String) msg.obj);

		if (currentStory == null || !currentStory.getUrl().equals(sourceUrl)) {
			stopReading();
		}

		currentStory = datasource.findStory(sourceUrl);
		if (currentStory == null) {
			currentStory = datasource.addStory(sourceUrl);
		}

		if (currentStory.getState() < Story.STATE_DOWNLOADED) {
			currentStory.download();
		}

		currentStory.play();

	}

	private void storeSource(Message msg) {
		isPaused = false;
		String sourceUrl = cleanUrl((String) msg.obj);
		downloadSource(sourceUrl);
	}

	private void downloadSource(String sourceUrl) {

		Story story = datasource.findStory(sourceUrl);
		if (story == null) {
			story = datasource.addStory(sourceUrl);
		}

		if (story.getState() < Story.STATE_DOWNLOADED) {
			story.download();
		}
	}

	private void stopReading() {
		if (currentStory != null)
			currentStory.stopPlaying();
	}

	private void pauseReading() {
		if (currentStory != null)
			currentStory.stopPlaying();
		isPaused = true;
	}

	private void unpauseReading() {
		if (isPaused) {
			currentStory.resume();
			isPaused = false;
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
		super.onDestroy();

		stopReading();

		datasource.close();

		isRunning = false;
		Log.i("CloplayerService", "Service Stopped.");
	}
}