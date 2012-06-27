package com.cloplayer.tasks;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.ContentValues;
import android.content.SharedPreferences;
import android.util.Log;

import com.cloplayer.CloplayerService;
import com.cloplayer.http.SyncHTTPClient;
import com.cloplayer.http.URLHelper;
import com.cloplayer.sqlite.MySQLiteHelper;
import com.cloplayer.sqlite.Story;
import com.cloplayer.utils.ServerConstants;

public class BootstrapTask {

	Story story;

	public BootstrapTask(Story story) {
		this.story = story;
	}

	public void execute() {

		if (story.getState() < Story.STATE_BOOTSTRAPPED) {

			Log.e("DownloadTask", "Starting Bootstrap");

			SharedPreferences globalSettings = CloplayerService.getInstance().getSharedPreferences(ServerConstants.CLOPLAYER_GLOBAL_PREFS, 0);
			String userId = globalSettings.getString("userId", null);

			SyncHTTPClient client = new SyncHTTPClient(URLHelper.add(userId, story.getUrl())) {

				public void onSuccessResponse(String response) {
					try {

						JSONObject content = new JSONObject(response);

						ContentValues values = new ContentValues();
						values.put(MySQLiteHelper.COLUMN_HEADLINE, content.getString("title"));
						values.put(MySQLiteHelper.COLUMN_DETAIL, content.getString("cleanedArticleText"));
						values.put(MySQLiteHelper.COLUMN_DOMAIN, content.getString("domain"));
						values.put(MySQLiteHelper.COLUMN_STATE, Story.STATE_BOOTSTRAPPED);
						CloplayerService.getInstance().datasource.updateStory(story, values);

						// CloplayerService.getInstance().showNotification("Downloading : "
						// + story.getHeadline(), "Downloading : " +
						// story.getHeadline());
						CloplayerService.getInstance().sendEmptyMessageToUI(CloplayerService.MSG_BOOTSTRAP_COMPLETE);
						
						CloplayerService.getInstance().mode = CloplayerService.MODE_ONLINE;

					} catch (JSONException e) {
						e.printStackTrace();
					}
				}

				public void onErrorResponse(Exception e) {
					Log.e("BootstrapTask", "Error", e);
					CloplayerService.getInstance().mode = CloplayerService.MODE_OFFLINE;
					CloplayerService.getInstance().sendEmptyMessageToUI(CloplayerService.MSG_NETWORK_ERROR);
					CloplayerService.getInstance().stopForeground(true);
				}
			};

			client.execute();
		}
	}
}
