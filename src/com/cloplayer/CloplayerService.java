package com.cloplayer;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
import android.widget.Toast;

import com.cloplayer.http.PlayTask;
import com.cloplayer.story.CloplayerStory;
import com.cloplayer.utils.ServerConstants;

public class CloplayerService extends Service {

	// private NotificationManager nm;
	private static boolean isRunning = false;

	final Messenger mService = new Messenger(new IncomingHandler());

	ArrayList<Messenger> mClients = new ArrayList<Messenger>();

	public static final int MSG_REGISTER_CLIENT = 1;
	public static final int MSG_UNREGISTER_CLIENT = 2;
	public static final int MSG_PLAY_SOURCE = 3;
	public static final int MSG_STOP_PLAYING = 4;
	public static final int MSG_PAUSE_PLAYING = 5;
	public static final int MSG_UNPAUSE_PLAYING = 6;
	public static final int MSG_STORE_SOURCE = 7;

	public static final int MSG_NEXT1 = 11;
	public static final int MSG_NEXT5 = 12;
	public static final int MSG_BACK1 = 13;
	public static final int MSG_BACK5 = 14;

	public static final int MSG_UPDATE_SOURCE_URL = 51;
	public static final int MSG_UPDATE_HEADLINE = 52;
	public static final int MSG_UPDATE_DETAIL = 53;
	public static final int MSG_UPDATE_PROGRESS_MAX = 54;
	public static final int MSG_UPDATE_PLAY_PROGRESS = 55;
	public static final int MSG_UPDATE_DOWNLOAD_PROGRESS = 56;

	private static CloplayerService instance;
	public boolean isPaused = false;
	public LinkedList<PlayItem> playQueue;
	public PlayTask playTask;
	
	public CloplayerStory currentStory;

	public static CloplayerService getInstace() {
		return instance;
	}

	@Override
	public void onCreate() {
		super.onCreate();

		instance = this;

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
			case MSG_STOP_PLAYING:
				stopSelf();
				break;
			case MSG_PAUSE_PLAYING:
				pauseReading();
				break;
			case MSG_UNPAUSE_PLAYING:
				unpauseReading();
				break;
			case MSG_NEXT1:
				int currentLine1 = playTask.getCurrentLine();
				playTask.stop();
				playTask = new PlayTask(currentLine1 + 1);
				playTask.start();
				break;
			case MSG_NEXT5:
				int currentLine2 = playTask.getCurrentLine();
				playTask.stop();
				playTask = new PlayTask(currentLine2 + 3);
				playTask.start();
				break;
			case MSG_BACK1:
				int currentLine3 = playTask.getCurrentLine();
				playTask.stop();
				playTask = new PlayTask(currentLine3 - 1);
				playTask.start();
				break;
			case MSG_BACK5:
				int currentLine4 = playTask.getCurrentLine();
				playTask.stop();
				playTask = new PlayTask(currentLine4 - 3);
				playTask.start();
				break;
			default:
				super.handleMessage(msg);
			}
		}
	}

	private void playSource(Message msg) {

		isPaused = false;

		String sourceUrl = (String) msg.obj;

		stopReading();

		playQueue = new LinkedList<PlayItem>();
		playTask = new PlayTask();
		playTask.start();

		CloplayerService.getInstace().showNotification("Loading", "Loading");

		currentStory = new CloplayerStory(sourceUrl);
		currentStory.loadAndPlay();
	}

	private void storeSource(Message msg) {

		isPaused = false;

		String sourceUrl = (String) msg.obj;
		
		stopReading();

		playQueue = new LinkedList<PlayItem>();
		playTask = new PlayTask();
		playTask.start();

		CloplayerService.getInstace().showNotification("Loading", "Loading");

		currentStory = new CloplayerStory(sourceUrl);
		currentStory.loadAndStore();
	}

	private void stopReading() {

		if (playTask != null)
			playTask.stop();
		playTask = null;
		playQueue = null;

		SharedPreferences globalSettings = getSharedPreferences(ServerConstants.CLOPLAYER_GLOBAL_PREFS, 0);
		SharedPreferences.Editor editor = globalSettings.edit();
		editor.remove("nowPlayingSource");
		editor.remove("nowPlayingHeadline");
		editor.remove("nowPlayingDetail");
		editor.remove("nowPlayingProgressMax");
		editor.remove("nowPlayingPlayProgress");
		editor.remove("nowPlayingDownloadProgress");
		editor.commit();

	}

	private void pauseReading() {
		playTask.stop();
		isPaused = true;
	}

	private void unpauseReading() {
		if (isPaused) {
			int currentLine = playTask.getCurrentLine();
			playTask = new PlayTask(currentLine);
			playTask.start();
			isPaused = false;
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

		isRunning = false;
		Log.i("CloplayerService", "Service Stopped.");
	}
}