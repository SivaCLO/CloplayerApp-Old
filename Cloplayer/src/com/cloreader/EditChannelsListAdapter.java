package com.cloreader;

import java.util.HashMap;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.TextView;

import com.cloreader.http.AddFavoriteAPI;
import com.cloreader.http.RemoveFavoriteAPI;

public class EditChannelsListAdapter extends ArrayAdapter<String> {

	private final Context context;
	private final List<String> channelIds;
	private final HashMap<String, JSONObject> channelDetails;
	private final HashMap<String, Boolean> favoriteDetails;
	private final String userId;
	private final ProgressDialog progDialog;
	private final Activity activity;

	public EditChannelsListAdapter(Context context, List<String> channelIds,
			HashMap<String, JSONObject> channelDetails,
			HashMap<String, Boolean> favoriteDetails, String userId,
			ProgressDialog progDialog, Activity activity) {
		super(context, R.layout.edit_row_layout, channelIds);
		this.context = context;
		this.channelIds = channelIds;
		this.channelDetails = channelDetails;
		this.favoriteDetails = favoriteDetails;
		this.userId = userId;
		this.progDialog = progDialog;
		this.activity = activity;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {

		LayoutInflater inflater = (LayoutInflater) context
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View rowView = inflater
				.inflate(R.layout.edit_row_layout, parent, false);

		TextView textView = (TextView) rowView.findViewById(R.id.label);
		final Button favButton = (Button) rowView
				.findViewById(R.id.favoriteButton);

		final String id = channelIds.get(position);

		favButton.setOnClickListener(new OnClickListener() {

			public void onClick(View v) {

				if (favButton.getText().equals("Remove Favorite")) {
					
					activity.runOnUiThread(new Runnable() {
						public void run() {
							progDialog.show();
						}
					});

					new RemoveFavoriteAPI(userId, id) {

						public void onSuccessResponse(JSONObject response) {
							Log.e("LoginActivity", response.toString());
							activity.runOnUiThread(new Runnable() {
								public void run() {
									favButton.setText("Add Favorite");
									progDialog.dismiss();
								}
							});
						}

						public void onErrorResponse(Exception e) {
							Log.e("LoginActivity", "Error", e);
							activity.runOnUiThread(new Runnable() {
								public void run() {
									progDialog.dismiss();
								}
							});
						}
					}.execute();
				} else { 
					
					activity.runOnUiThread(new Runnable() {
						public void run() {
							progDialog.show();
						}
					});

					new AddFavoriteAPI(userId, id) {

						public void onSuccessResponse(JSONObject response) {
							Log.e("LoginActivity", response.toString());
							activity.runOnUiThread(new Runnable() {
								public void run() {
									favButton.setText("Remove Favorite");
									progDialog.dismiss();
								}
							});
						}

						public void onErrorResponse(Exception e) {
							Log.e("LoginActivity", "Error", e);
							activity.runOnUiThread(new Runnable() {
								public void run() {
									progDialog.dismiss();
								}
							});
						}
					}.execute();
				}
			}
		});

		try {
			textView.setText(channelDetails.get(id).getString("name"));
			if (favoriteDetails.get(id)) {
				favButton.setText("Remove Favorite");
			} else {
				favButton.setText("Add Favorite");
			}
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return rowView;
	}
}
