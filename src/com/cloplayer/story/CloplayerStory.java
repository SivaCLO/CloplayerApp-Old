package com.cloplayer.story;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.SharedPreferences;
import android.util.Log;

import com.cloplayer.CloplayerService;
import com.cloplayer.http.HTTPClient;
import com.cloplayer.http.MaryTask;
import com.cloplayer.http.StoreStoryTask;
import com.cloplayer.utils.ServerConstants;

public class CloplayerStory {

	private String sourceUrl;
	private String domain;
	private String headLine;
	private String detailText;

	public CloplayerStory(String sourceUrl) {
		this.sourceUrl = sourceUrl;
	}

	public void loadValues(String headline, String detailText, String domain) {
		this.headLine = headline;
		this.detailText = detailText;
		this.domain = domain;
	}

	public void play(String prefix) {

		SharedPreferences globalSettings = CloplayerService.getInstace().getSharedPreferences(ServerConstants.CLOPLAYER_GLOBAL_PREFS, 0);
		SharedPreferences.Editor editor = globalSettings.edit();
		editor.putString("nowPlayingHeadline", headLine);
		editor.putString("nowPlayingDetail", detailText);
		editor.putString("nowPlayingSource", domain);
		editor.commit();

		CloplayerService.getInstace().sendStringMessageToUI(CloplayerService.MSG_UPDATE_HEADLINE, headLine);
		CloplayerService.getInstace().sendStringMessageToUI(CloplayerService.MSG_UPDATE_DETAIL, detailText);
		CloplayerService.getInstace().sendStringMessageToUI(CloplayerService.MSG_UPDATE_SOURCE_URL, domain);

		CloplayerService.getInstace().showNotification(headLine, headLine);

		// CloplayerService.getInstace().ttsManager.readStory(this, prefix);

		String textToRead = headLine + ". " + detailText;

		MaryTask task = new MaryTask(textToRead);
		task.execute();

		// CloplayerService.getInstace().stopForeground(true);

	}

	public void loadAndPlay() {
		HTTPClient client = new HTTPClient("http://api.cloplayer.com/api/parse?url=" + sourceUrl) {

			public void onSuccessResponse(String response) {
				try {
					JSONObject content = new JSONObject(response);
					loadValues(content.getString("title"), content.getString("cleanedArticleText"), content.getString("domain"));
					play("");
				} catch (JSONException e) {
					e.printStackTrace();
				}
			}

			public void onErrorResponse(Exception e) {
				Log.e("LoginActivity", "Error", e);
				loadValues("Error", "Not able to load news due to connectivity problems", "");
				play("");
			}
		};
		client.execute();
	}

	public void loadAndStore() {
		HTTPClient client = new HTTPClient("http://api.cloplayer.com/api/parse?url=" + sourceUrl) {

			public void onSuccessResponse(String response) {
				try {
					JSONObject content = new JSONObject(response);
					loadValues(content.getString("title"), content.getString("cleanedArticleText"), content.getString("domain"));
					store("");
				} catch (JSONException e) {
					e.printStackTrace();
				}
			}

			public void onErrorResponse(Exception e) {
				Log.e("LoginActivity", "Error", e);
				loadValues("Error", "Not able to load news due to connectivity problems", "");
			}
		};
		client.execute();
	}

	public void store(String prefix) {

		SharedPreferences globalSettings = CloplayerService.getInstace().getSharedPreferences(ServerConstants.CLOPLAYER_GLOBAL_PREFS, 0);
		SharedPreferences.Editor editor = globalSettings.edit();
		editor.putString("nowPlayingHeadline", headLine);
		editor.putString("nowPlayingDetail", detailText);
		editor.putString("nowPlayingSource", domain);
		editor.commit();

		CloplayerService.getInstace().sendStringMessageToUI(CloplayerService.MSG_UPDATE_HEADLINE, headLine);
		CloplayerService.getInstace().sendStringMessageToUI(CloplayerService.MSG_UPDATE_DETAIL, detailText);
		CloplayerService.getInstace().sendStringMessageToUI(CloplayerService.MSG_UPDATE_SOURCE_URL, domain);

		CloplayerService.getInstace().showNotification(headLine, headLine);
		
		String textToRead = headLine + ". " + detailText;

		StoreStoryTask task = new StoreStoryTask(textToRead);
		task.execute();

	}
	
}
