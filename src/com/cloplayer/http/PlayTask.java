package com.cloplayer.http;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.UnsupportedEncodingException;

import android.content.ContentValues;
import android.content.SharedPreferences;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;

import com.cloplayer.CloplayerService;
import com.cloplayer.sqlite.MySQLiteHelper;
import com.cloplayer.sqlite.Story;
import com.cloplayer.utils.ServerConstants;

public class PlayTask extends AsyncTask<String, String, String> {

	int currentLine;
	boolean isCanceled = false;
	Story story;

	public PlayTask(Story story) {
		this(story, 0);
	}

	public PlayTask(Story story, int i) {
		currentLine = i;
		this.story = story;
	}

	@Override
	protected String doInBackground(String... data) {

		synchronized (story) {
			while (story.getState() < Story.STATE_BOOTSTRAPPED) {
				try {
					Log.e("PlayTask", "Waiting for bootstrap");
					CloplayerService.getInstance().showNotification("Loading", "Loading");
					story.wait();
					Log.e("PlayTask", "Waiting for bootstrap Complete");
				} catch (InterruptedException e1) {
					e1.printStackTrace();
				}
			}
		}

		int progressMax = story.getItemCount();
		int downloadProgress = story.getDownloadProgress();

		if (currentLine < 0)
			currentLine = 0;
		if (currentLine > downloadProgress && downloadProgress == 0)
			currentLine = 0;
		if (currentLine >= downloadProgress && downloadProgress != progressMax)
			currentLine = downloadProgress - 1;
		if (currentLine >= downloadProgress && downloadProgress == progressMax)
			currentLine = downloadProgress - 1;

		SharedPreferences globalSettings = CloplayerService.getInstance().getSharedPreferences(ServerConstants.CLOPLAYER_GLOBAL_PREFS, 0);
		SharedPreferences.Editor editor = globalSettings.edit();
		editor.putInt("nowPlaying", (int) story.getId());
		editor.commit();

		CloplayerService.getInstance().showNotification(story.getHeadline(), story.getHeadline());

		while (!isCanceled && currentLine != story.getItemCount()) {

			Log.e("PlayTask", "CurrentLine : " + currentLine);
			Log.e("PlayTask", "Download Progress : " + story.getDownloadProgress());

			ContentValues values = new ContentValues();
			values.put(MySQLiteHelper.COLUMN_PLAY_PROGRESS, currentLine);
			CloplayerService.getInstance().datasource.updateStory(story, values);

			synchronized (story) {
				if (currentLine >= story.getDownloadProgress()) {
					try {
						Log.e("PlayTask", "Waiting");
						story.wait();
						Log.e("PlayTask", "Waiting Complete");
					} catch (InterruptedException e1) {
						e1.printStackTrace();
					}
				}
			}

			Log.e("PlayTask", "Going to Play : " + globalSettings.getString(story.getId() + "." + currentLine + ".text", ""));

			byte[] audio = null;
			int audioLength = globalSettings.getInt(story.getId() + "." + currentLine + ".audio", 0);

			Log.e("PlayTask", "Going to Play Audio Length : " + audioLength);

			audio = new byte[audioLength];

			String sdcardPath = Environment.getExternalStorageDirectory().getAbsolutePath();

			File dstDir = new File(sdcardPath + "/cloplayer");
			dstDir.mkdirs();

			File dstFile = new File(sdcardPath + "/cloplayer/" + story.getId() + "." + currentLine + ".audio.wav");
			DataInputStream inFile;
			try {
				inFile = new DataInputStream(new FileInputStream(dstFile));
				inFile.readFully(audio);
				inFile.close();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			CloplayerService.getInstance().sendIntMessageToUI(CloplayerService.MSG_UPDATE_STORY, (int) story.getId());

			at.write(audio, 0, audio.length);

			if (!isCanceled) {
				currentLine++;

				Log.e("PlayTask", "PlayProgress : " + currentLine);

				values = new ContentValues();
				values.put(MySQLiteHelper.COLUMN_PLAY_PROGRESS, currentLine);
				CloplayerService.getInstance().datasource.updateStory(story, values);
			}

		}

		ContentValues values = new ContentValues();
		values = new ContentValues();
		values.put(MySQLiteHelper.COLUMN_STATE, Story.STATE_PLAYED);
		CloplayerService.getInstance().datasource.updateStory(story, values);

		// CloplayerService.getInstance().stopForeground(true);

		return "";
	}

	@Override
	protected void onPostExecute(String result) {
		cleanup(true);
		story.playTask = null;
	}

	public int getCurrentLine() {
		return currentLine;
	}

	int intSize = android.media.AudioTrack.getMinBufferSize(16000, AudioFormat.CHANNEL_CONFIGURATION_MONO, AudioFormat.ENCODING_PCM_16BIT);
	AudioTrack at = new AudioTrack(AudioManager.STREAM_MUSIC, 16000, AudioFormat.CHANNEL_CONFIGURATION_MONO, AudioFormat.ENCODING_PCM_16BIT, intSize, AudioTrack.MODE_STREAM);

	public void start() {
		at.setStereoVolume(AudioTrack.getMaxVolume(), AudioTrack.getMaxVolume());
		at.play();
		execute();
	}

	public void stop(boolean closeNotification) {
		cancel(true);
		isCanceled = true;
		cleanup(closeNotification);
	}
	
	public void cleanup(boolean closeNotification) {
		at.stop();
		at.flush();

		// CloplayerService.getInstance().currentStory = null;

		SharedPreferences globalSettings = CloplayerService.getInstance().getSharedPreferences(ServerConstants.CLOPLAYER_GLOBAL_PREFS, 0);
		SharedPreferences.Editor editor = globalSettings.edit();
		editor.remove("nowPlaying");
		editor.commit();

		if (closeNotification)
			CloplayerService.getInstance().stopForeground(true);
	}
}
