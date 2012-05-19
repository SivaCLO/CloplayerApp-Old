package com.cloplayer;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.cloplayer.http.FavoriteChannelListAPI;
import com.cloplayer.http.PublicChannelListAPI;
import com.cloplayer.http.RecommendedChannelListAPI;
import com.cloplayer.http.SourcesListAllAPI;
import com.cloplayer.http.UserChannelListAPI;
import com.cloplayer.utils.ServerConstants;

public class DataLoadActivity extends Activity {

	@Override
	public void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		ViewGroup parent = (ViewGroup) findViewById(R.id.root);
		LayoutInflater inflater = LayoutInflater.from(getBaseContext());

		parent.addView(inflater.inflate(R.layout.logo, parent, false));
		parent.addView(inflater.inflate(R.layout.dataload, parent, false));

		final Button retryButton = (Button) findViewById(R.id.retry);
		final TextView retryText = (TextView) findViewById(R.id.retrytext);		
		
		final ProgressDialog progDailog = new ProgressDialog(this);
		progDailog.setMessage("Loading Sources...");
		progDailog.setIndeterminate(true);		
		
		SharedPreferences settings = getSharedPreferences(ServerConstants.CLOPLAYER_GLOBAL_PREFS, 0);
		final String userId = settings.getString("userId", "");
		
		final SourcesListAllAPI sourceListApi = new SourcesListAllAPI() {

			@Override
			public void onSuccessResponse(JSONObject response) {				
				
				final JSONObject SourceListResponse = response;
				
				Log.e("DataLoadActivity", "SourceList : " + response.toString());
				
				progDailog.setMessage("Loading User Channels...");
				
				new UserChannelListAPI(userId) {

					@Override
					public void onSuccessResponse(JSONObject response) {
						
						final JSONObject UserChannelListResponse = response;
						
						Log.e("DataLoadActivity", "UserChannelList : " + response.toString());
						
						progDailog.setMessage("Loading User Favorite Channels...");
						
						new FavoriteChannelListAPI(userId) {

							@Override
							public void onSuccessResponse(JSONObject response) {
								
								final JSONObject FavoriteChannelListResponse = response;
								
								Log.e("DataLoadActivity", "FavoriteChannelList : " + response.toString());
								
								progDailog.setMessage("Loading Recommended Channels...");
								
								new RecommendedChannelListAPI() {

									@Override
									public void onSuccessResponse(JSONObject response) {
										
										final JSONObject RecommendedChannelListResponse = response;
										
										Log.e("DataLoadActivity", "RecommendedChannelList : " + response.toString());
										
										progDailog.setMessage("Loading Public Channels...");
										
										new PublicChannelListAPI(userId) {

											@Override
											public void onSuccessResponse(JSONObject response) {
												
												final JSONObject PublicChannelListResponse = response;

												Log.e("DataLoadActivity", "PublicChannelList : " + response.toString());
												
												progDailog.setMessage("Loading Channel Details...");
												
												SharedPreferences settings = getSharedPreferences(ServerConstants.CLOPLAYER_CHANNEL_PREFS, 0);
												SharedPreferences.Editor clearEditor = settings.edit();
												clearEditor.clear();
												clearEditor.commit();												
												
												SharedPreferences.Editor editor = settings.edit();
												
												try {
													
													JSONArray temp = SourceListResponse.getJSONArray("sources");
													JSONArray tempList = new JSONArray();
													
													for (int i = 0; i< temp.length(); i++) {
														JSONObject tempObject = temp.getJSONObject(i);
														String id = tempObject.getString("id");
														tempList.put(id);
														editor.putString(id + "#name", tempObject.getString("name"));
														editor.putString(id + "#url", tempObject.getString("url"));
													}
													
													editor.putString("sourceList", tempList.toString());
													
													
													temp = UserChannelListResponse.getJSONArray("channels");
													tempList = new JSONArray();
													
													for (int i = 0; i< temp.length(); i++) {
														JSONObject tempObject = temp.getJSONObject(i);
														String id = tempObject.getString("id");
														tempList.put(id);
														editor.putString(id, tempObject.toString());														
													}
													
													editor.putString("userChannels", tempList.toString());
													
													
													temp = RecommendedChannelListResponse.getJSONArray("channels");
													tempList = new JSONArray();
													
													for (int i = 0; i< temp.length(); i++) {
														JSONObject tempObject = temp.getJSONObject(i);
														String id = tempObject.getString("id");
														tempList.put(id);
														editor.putString(id, tempObject.toString());														
													}
													
													editor.putString("recommendedChannels", tempList.toString());
													
													
													temp = PublicChannelListResponse.getJSONArray("channels");
													tempList = new JSONArray();
													
													for (int i = 0; i< temp.length(); i++) {
														JSONObject tempObject = temp.getJSONObject(i);
														String id = tempObject.getString("id");
														tempList.put(id);
														editor.putString(id, tempObject.toString());														
													}
													
													editor.putString("publicChannels", tempList.toString());
													
													temp = FavoriteChannelListResponse.getJSONArray("channels");
													tempList = new JSONArray();
													
													for (int i = 0; i< temp.length(); i++) {
														JSONObject tempObject = temp.getJSONObject(i);
														String id = tempObject.getString("id");
														tempList.put(id);
														editor.putBoolean(id + "#favorite", true);														
													}
													
													editor.putString("favoriteChannels", tempList.toString());
													
													Log.e("DataLoadActivity", settings.getAll().toString());
													
													editor.commit();
													
													//Uncomment this after development
													
													//if(tempList.length() == 0) {
														Intent intent = new Intent();
												        intent.setClass(DataLoadActivity.this, EditFavoritesActivity.class);
												        startActivity(intent);
												        finish();
													//} else {													
													//	Intent intent = new Intent();
													//	intent.setClass(DataLoadActivity.this, HomeActivity.class);
													//	startActivity(intent);
													//	finish();
													//}
													
												} catch (JSONException e) {
													// TODO Auto-generated catch block
													e.printStackTrace();
												}
											}

											@Override
											public void onErrorResponse(Exception e) {
												progDailog.dismiss();
												retryButton.setVisibility(View.VISIBLE);
												retryText.setVisibility(View.VISIBLE);
											}
										}.execute();

									}

									@Override
									public void onErrorResponse(Exception e) {
										progDailog.dismiss();
										retryButton.setVisibility(View.VISIBLE);
										retryText.setVisibility(View.VISIBLE);
									}
								}.execute();

							}

							@Override
							public void onErrorResponse(Exception e) {
								progDailog.dismiss();
								retryButton.setVisibility(View.VISIBLE);
								retryText.setVisibility(View.VISIBLE);
							}
						}.execute();
					}

					@Override
					public void onErrorResponse(Exception e) {
						progDailog.dismiss();
						retryButton.setVisibility(View.VISIBLE);
						retryText.setVisibility(View.VISIBLE);
					}
				}.execute();
			}

			@Override
			public void onErrorResponse(Exception e) {
				progDailog.dismiss();
				retryButton.setVisibility(View.VISIBLE);
				retryText.setVisibility(View.VISIBLE);

			}

		};

		progDailog.show();
		sourceListApi.execute();

		retryButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				progDailog.show();
				sourceListApi.execute();
			}
		});
	}
}

	
