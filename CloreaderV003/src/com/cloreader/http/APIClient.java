package com.cloreader.http;

import java.io.UnsupportedEncodingException;

import org.json.JSONObject;

import android.content.Context;
import android.util.Log;

import com.cloreader.utils.ServerConstants;

public class APIClient {
	
	private String userId;
	
	public APIClient(Context context) {
		HTTPHelper.init(context);
	}
	
	public JSONObject markStory(String storyId) {
		try {
			Log.e(ServerConstants.TAG, "http://api.cloreader.com/api/"
					+ userId + "/stories/" + storyId + "/mark");
			return HTTPHelper.getUrlContent("http://api.cloreader.com/api/"
					+ userId + "/stories/" + storyId + "/mark");			
		} catch (HTTPHelper.ApiException e) {
			Log.e(ServerConstants.TAG, "Problem making HTTP call request", e);
		}
		
		return null;
	}

	public JSONObject popStory() {
		try {
			return HTTPHelper.getUrlContent("http://api.cloreader.com/api/"
					+ userId + "/stories/pop");
		} catch (HTTPHelper.ApiException e) {
			Log.e(ServerConstants.TAG, "Problem making HTTP call request", e);
		}
		return null;
	}
	
	public JSONObject createUser(String emailId) {
		try {
			Log.e(ServerConstants.TAG, "http://api.cloreader.com/api/"
					+ "create?email=" + emailId);
			return HTTPHelper.getUrlContent("http://api.cloreader.com/api/"
					+ "create?email=" + emailId);
		} catch (HTTPHelper.ApiException e) {
			Log.e(ServerConstants.TAG, "Problem making HTTP call request", e);
		}
		return null;
	}

	public JSONObject topics_set(JSONObject preferenceJSON) {
		try {
			return HTTPHelper.getUrlContent("http://api.cloreader.com/api/"
					+ userId
					+ "/set?preferences="
					+ java.net.URLEncoder.encode(preferenceJSON.toString(),
							"ISO-8859-1"));
		} catch (HTTPHelper.ApiException e) {
			Log.e(ServerConstants.TAG, "Problem making HTTP call request", e);
		} catch (UnsupportedEncodingException ignored) {

		}
		return null;
	}

	public void setUserId(String userId) {
		this.userId = userId;		
	}
}
