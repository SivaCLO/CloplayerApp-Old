package com.cloreader;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.SharedPreferences;
import android.os.Bundle;

import com.cloreader.utils.ServerConstants;

public class PublicChannelsActivity extends ListActivity {
	
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		SharedPreferences settings = getSharedPreferences(
				ServerConstants.CLOREADER_CHANNEL_PREFS, 0);

		SharedPreferences globalSettings = getSharedPreferences(
				ServerConstants.CLOREADER_GLOBAL_PREFS, 0);
		
		String userId = globalSettings.getString("userId", "");
		
		try {

			JSONArray channelList = new JSONArray(settings.getString(
					"publicChannels", ""));

			List<String> channelIds = new ArrayList<String>();
			HashMap<String, JSONObject> channelDetails = new HashMap<String, JSONObject>();
			HashMap<String, Boolean> favoriteDetails  = new HashMap<String, Boolean>();

			for (int i = 0; i < channelList.length(); i++) {
				String id;
				id = channelList.getString(i);
				channelIds.add(id);
				channelDetails.put(id, new JSONObject(settings.getString(id, "")));
				favoriteDetails.put(id, settings.getBoolean(id + "#favorite", false));
			}

			final ProgressDialog progDailog = new ProgressDialog(this);
			progDailog.setMessage("Please wait...");
			progDailog.setIndeterminate(true);
			
			EditChannelsListAdapter adapter = new EditChannelsListAdapter(this,
					channelIds, channelDetails, favoriteDetails, userId, progDailog, this);
			setListAdapter(adapter);

		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
}
