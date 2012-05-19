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

public class IntroductionActivity extends Activity {

	@Override
	public void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);
		
		SharedPreferences settings = getSharedPreferences(ServerConstants.CLOPLAYER_GLOBAL_PREFS, 0);
		String userId = settings.getString("userId", "");
		if (!"".equals(userId)) {
			Intent intent = new Intent();
			intent.setClass(IntroductionActivity.this,
					DataLoadActivity.class);
			startActivity(intent);
			finish();		
		}		
		
		setContentView(R.layout.main);

		ViewGroup parent = (ViewGroup) findViewById(R.id.root);
		LayoutInflater inflater = LayoutInflater.from(getBaseContext());

		parent.addView(inflater.inflate(R.layout.logo, parent, false));
		parent.addView(inflater.inflate(R.layout.intro, parent, false));

		//final TextView introText = (TextView) findViewById(R.id.introText);
		final Button introContinueButton = (Button) findViewById(R.id.introContinue);

		introContinueButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				Intent intent = new Intent();
				intent.setClass(IntroductionActivity.this,
						LoginActivity.class);
				startActivity(intent);
				finish();
			}
		});
	}
}
