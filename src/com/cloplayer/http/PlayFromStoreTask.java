package com.cloplayer.http;

import java.io.IOException;

import android.content.SharedPreferences;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.AsyncTask;
import android.util.Log;

import com.cloplayer.CloplayerService;
import com.cloplayer.PlayItem;
import com.cloplayer.utils.ServerConstants;

public class PlayFromStoreTask extends AsyncTask<String, String, String> {

	int currentLine;
	PlayItem currPlayItem;
	boolean isCanceled = false;

	public PlayFromStoreTask() {
		this(0);
	}

	public PlayFromStoreTask(int i) {
		currentLine = i;
	}

	@Override
	protected String doInBackground(String... data) {

		SharedPreferences globalSettings = CloplayerService.getInstace().getSharedPreferences(ServerConstants.CLOPLAYER_GLOBAL_PREFS, 0);
		SharedPreferences.Editor editor = globalSettings.edit();
		int progressMax = globalSettings.getInt("nowPlayingProgressMax", 0);
		int downLoadProgress = globalSettings.getInt("nowPlayingDownloadProgress", 0);

		if (currentLine < 0)
			currentLine = 0;
		if (currentLine > downLoadProgress && downLoadProgress == 0)
			currentLine = 0;
		if (currentLine >= downLoadProgress && downLoadProgress != progressMax)
			currentLine = downLoadProgress - 1;
		if (currentLine > downLoadProgress && downLoadProgress == progressMax)
			currentLine = downLoadProgress - 1;
		if (currentLine == downLoadProgress && downLoadProgress == progressMax)
			currentLine = downLoadProgress;

		while (!isCanceled) {

			CloplayerService.getInstace().sendIntMessageToUI(CloplayerService.MSG_UPDATE_PLAY_PROGRESS, currentLine);

			Log.e("Test", "CurrentLine : Reading" + currentLine);

			editor.putInt("nowPlayingPlayProgress", currentLine);
			editor.commit();

			synchronized (CloplayerService.getInstace().currentStory) {

				try {
					currPlayItem = PlayItem.fromString(globalSettings.getString("nowPlayingPlayItem." + currentLine, null));
				} catch (Exception e) {
					try {
						CloplayerService.getInstace().currentStory.wait();
						currPlayItem = PlayItem.fromString(globalSettings.getString("nowPlayingPlayItem." + currentLine, null));
					} catch (Exception e1) {
						e1.printStackTrace();
					}
				}

			}

			CloplayerService.getInstace().sendStringMessageToUI(CloplayerService.MSG_UPDATE_DETAIL, currPlayItem.getText());
			editor.putString("nowPlayingDetail", currPlayItem.getText());
			editor.commit();

			at.write(currPlayItem.getByteArray(), 0, currPlayItem.getByteArray().length);

			if (!isCanceled) {
				currentLine++;

				CloplayerService.getInstace().sendIntMessageToUI(CloplayerService.MSG_UPDATE_PLAY_PROGRESS, currentLine);
				editor.putInt("nowPlayingPlayProgress", currentLine);
				editor.commit();

				Log.e("Test", "CurrentLine : Reading" + currentLine);
			}

		}

		return "";
	}

	@Override
	protected void onPostExecute(String result) {

	}

	public int getCurrentLine() {
		return currentLine;
	}

	public PlayItem getCurrPlayItem() {
		return currPlayItem;
	}

	int intSize = android.media.AudioTrack.getMinBufferSize(16000, AudioFormat.CHANNEL_CONFIGURATION_MONO, AudioFormat.ENCODING_PCM_16BIT);
	AudioTrack at = new AudioTrack(AudioManager.STREAM_MUSIC, 16000, AudioFormat.CHANNEL_CONFIGURATION_MONO, AudioFormat.ENCODING_PCM_16BIT, intSize, AudioTrack.MODE_STREAM);

	public void start() {
		at.setStereoVolume(AudioTrack.getMaxVolume(), AudioTrack.getMaxVolume());
		at.play();
		execute();
	}

	public void stop() {
		cancel(true);
		isCanceled = true;
		at.stop();
		at.flush();
	}
}
