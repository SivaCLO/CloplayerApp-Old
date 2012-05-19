package com.cloplayer;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import com.cloplayer.utils.ServerConstants;

public class CreateChannelActivity extends Activity {

	@Override
	public void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		ViewGroup parent = (ViewGroup) findViewById(R.id.root);
		LayoutInflater inflater = LayoutInflater.from(getBaseContext());

		parent.addView(inflater.inflate(R.layout.logo, parent, false));
		parent.addView(inflater.inflate(R.layout.create_channel, parent, false));

		SharedPreferences settings = getSharedPreferences(
				ServerConstants.CLOPLAYER_CHANNEL_PREFS, 0);
		JSONArray sourcesJSON;
		List<String> sources = new ArrayList<String>();
		
		try {
			sourcesJSON = new JSONArray(settings.getString("sourceList", ""));

			for (int i = 0; i < sourcesJSON.length(); i++) {
				sources.add(settings.getString(sourcesJSON.getString(i) + "#name", ""));
			}

		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		ArrayAdapter adapter = new ArrayAdapter(this,
				android.R.layout.simple_spinner_item, sources);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

		Spinner spinner = (Spinner) findViewById(R.id.spinner1);
		spinner.setAdapter(adapter);

		spinner = (Spinner) findViewById(R.id.spinner2);
		spinner.setAdapter(adapter);

		spinner = (Spinner) findViewById(R.id.spinner3);
		spinner.setAdapter(adapter);

		/*
		 * final TextView loginText = (TextView) findViewById(R.id.loginText);
		 * final Button loginButton = (Button) findViewById(R.id.login);
		 * 
		 * final ProgressDialog progDialog = new ProgressDialog(this);
		 * progDialog.setMessage("Logging in...");
		 * progDialog.setIndeterminate(true);
		 * 
		 * loginButton.setOnClickListener(new View.OnClickListener() {
		 * 
		 * public void onClick(View v) {
		 * 
		 * progDialog.show();
		 * 
		 * final String email = ((EditText) findViewById(R.id.login_email))
		 * .getText().toString(); final String password = ((EditText)
		 * findViewById(R.id.login_password)) .getText().toString();
		 * 
		 * new LoginAPI(email, password) {
		 * 
		 * @Override public void onSuccessResponse(JSONObject response) {
		 * Log.e("LoginActivity", response.toString()); progDialog.dismiss();
		 * 
		 * if (response.optBoolean("result")) {
		 * 
		 * SharedPreferences settings =
		 * getSharedPreferences(ServerConstants.CLOPLAYER_GLOBAL_PREFS, 0);
		 * SharedPreferences.Editor editor = settings.edit();
		 * editor.putString("userId", response.optString("userId"));
		 * editor.commit();
		 * 
		 * Intent intent = new Intent();
		 * intent.setClass(CreateChannelActivity.this, DataLoadActivity.class);
		 * startActivity(intent); finish(); } else {
		 * loginText.setText(response.optString("message","Login Error")); }
		 * 
		 * }
		 * 
		 * @Override public void onErrorResponse(Exception e) {
		 * Log.e("LoginActivity", "Error", e); runOnUiThread(new Runnable() {
		 * public void run() { loginText.setText("Connection Problems"); } });
		 * progDialog.dismiss(); }
		 * 
		 * }.execute(); } });
		 */
	}
}
