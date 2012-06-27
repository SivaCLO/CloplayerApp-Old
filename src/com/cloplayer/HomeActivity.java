package com.cloplayer;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.TextView;

import com.cloplayer.http.AsyncHTTPClient;
import com.cloplayer.http.URLHelper;

public class HomeActivity extends Activity {

	ProgressDialog progressBar;

	public void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);
		setContentView(R.layout.home);

		progressBar = new ProgressDialog(this);
		progressBar.setMessage("Checking Internet connection...");
		progressBar.setIndeterminate(true);
		progressBar.show();

		AsyncHTTPClient client = new AsyncHTTPClient(URLHelper.api_home()) {

			public void onSuccessResponse(String response) {
				progressBar.hide();
			}

			public void onErrorResponse(Exception e) {
				runOnUiThread(new Runnable() {
					public void run() {
						((ImageView) findViewById(R.id.fb_login)).setVisibility(View.GONE);
						((TextView) findViewById(R.id.no_internet)).setVisibility(View.VISIBLE);
					}
				});

			}
		};

		client.execute();

		((ImageView) findViewById(R.id.fb_login)).setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				Intent intentToGo = new Intent();
				intentToGo.setClass(HomeActivity.this, LoginActivity.class);
				startActivity(intentToGo);
				finish();
			}
		});

	}
}
