package com.cloplayer;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.cloplayer.http.AsyncHTTPClient;
import com.cloplayer.utils.ServerConstants;
import com.facebook.android.AsyncFacebookRunner;
import com.facebook.android.AsyncFacebookRunner.RequestListener;
import com.facebook.android.DialogError;
import com.facebook.android.Facebook;
import com.facebook.android.Facebook.DialogListener;
import com.facebook.android.FacebookError;

public class LoginActivity extends Activity {

	Facebook facebook = new Facebook("298227830273715");
	AsyncFacebookRunner mAsyncRunner = new AsyncFacebookRunner(facebook);

	ProgressDialog progressBar;

	public void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);

		setContentView(R.layout.login);
		
		progressBar = new ProgressDialog(this);
		progressBar.setMessage("Connecting to Facebook");
		progressBar.setIndeterminate(true);
		progressBar.show();

		((Button) findViewById(R.id.login)).setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {

				final String email = ((EditText) findViewById(R.id.email)).getText().toString();
				final String password = ((EditText) findViewById(R.id.password)).getText().toString();

				if (email == null || password == null || email.equals("") || password.equals("")) {
					Toast.makeText(LoginActivity.this, "Please fill all the fields", Toast.LENGTH_SHORT).show();
				} else {
					
					progressBar.setMessage("Logging in...");				
					progressBar.show();

					AsyncHTTPClient client = new AsyncHTTPClient("http://api.cloplayer.com/api/login?email=" + email + "&password=" + password) {

						public void onSuccessResponse(String response) {
							try {

								JSONObject content = new JSONObject(response);
								String userId = content.getString("userId");

								SharedPreferences globalSettings = CloplayerService.getInstance().getSharedPreferences(ServerConstants.CLOPLAYER_GLOBAL_PREFS, 0);
								SharedPreferences.Editor editor = globalSettings.edit();
								editor.putString("userId", userId);
								editor.commit();

								Log.e("LoginActivity", "UserId: " + userId);

								Intent intentToGo = new Intent();
								intentToGo.setClass(LoginActivity.this, LibraryActivity.class);
								startActivity(intentToGo);
								finish();

							} catch (JSONException e) {
								e.printStackTrace();
							}
						}

						public void onErrorResponse(Exception e) {
							Log.e("LoginActivity", "Error", e);
						}
					};

					client.execute();
				}
			}
		});

		facebook.authorize(this, new String[] { "email" }, new DialogListener() {
			@Override
			public void onComplete(Bundle values) {

				mAsyncRunner.request("me", new RequestListener() {

					@Override
					public void onComplete(String response, Object state) {
						Log.e("LoginActivity", "Response: " + response.toString());
						try {
							JSONObject jo = new JSONObject(response);
							final String email = jo.getString("email");
							Log.e("LoginActivity", "Email: " + email);

							runOnUiThread(new Runnable() {
								public void run() {
									((EditText) findViewById(R.id.email)).setText(email);
									((EditText) findViewById(R.id.email)).setEnabled(false);
									((EditText) findViewById(R.id.password)).requestFocus();
									
									progressBar.hide();
								}
							});

						} catch (JSONException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}

					@Override
					public void onIOException(IOException e, Object state) {
						// TODO Auto-generated method stub

					}

					@Override
					public void onFileNotFoundException(FileNotFoundException e, Object state) {
						// TODO Auto-generated method stub

					}

					@Override
					public void onMalformedURLException(MalformedURLException e, Object state) {
						// TODO Auto-generated method stub

					}

					@Override
					public void onFacebookError(FacebookError e, Object state) {
						// TODO Auto-generated method stub

					}

				});

			}

			@Override
			public void onFacebookError(FacebookError error) {
			}

			@Override
			public void onError(DialogError e) {
			}

			@Override
			public void onCancel() {
			}
		});
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		facebook.authorizeCallback(requestCode, resultCode, data);
	}

}
