package com.cloplayer;

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
import android.widget.EditText;
import android.widget.TextView;

import com.cloplayer.http.LoginAPI;
import com.cloplayer.utils.ServerConstants;

public class LoginActivity extends Activity {

	@Override
	public void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		ViewGroup parent = (ViewGroup) findViewById(R.id.root);
		LayoutInflater inflater = LayoutInflater.from(getBaseContext());

		parent.addView(inflater.inflate(R.layout.logo, parent, false));
		parent.addView(inflater.inflate(R.layout.login, parent, false));
		
		final TextView loginText = (TextView) findViewById(R.id.loginText);		
		final Button loginButton = (Button) findViewById(R.id.login);

		final ProgressDialog progDialog = new ProgressDialog(this);
		progDialog.setMessage("Logging in...");
		progDialog.setIndeterminate(true);

		loginButton.setOnClickListener(new View.OnClickListener() {
		
			public void onClick(View v) {
				
				progDialog.show();
				
				final String email = ((EditText) findViewById(R.id.login_email))
						.getText().toString();
				final String password = ((EditText) findViewById(R.id.login_password))
						.getText().toString();
				
				new LoginAPI(email, password) {

					@Override
					public void onSuccessResponse(JSONObject response) {
						Log.e("LoginActivity", response.toString());
						progDialog.dismiss();

						if (response.optBoolean("result")) {
							
							SharedPreferences settings = getSharedPreferences(ServerConstants.CLOPLAYER_GLOBAL_PREFS, 0);
							SharedPreferences.Editor editor = settings.edit();
							editor.putString("userId", response.optString("userId"));
							editor.commit();
							
							Intent intent = new Intent();
							intent.setClass(LoginActivity.this,
									DataLoadActivity.class);
							startActivity(intent);
							finish();
						} else {
							loginText.setText(response.optString("message","Login Error"));
						}

					}

					@Override
					public void onErrorResponse(Exception e) {
						Log.e("LoginActivity", "Error", e);
						runOnUiThread(new Runnable() {
							public void run() {
								loginText.setText("Connection Problems");
							}
						});
						progDialog.dismiss();
					}

				}.execute();
			}
		});
	}
}
