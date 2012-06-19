package com.cloplayer;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;

public class HomeActivity extends Activity {

	public void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);
		setContentView(R.layout.home);
		
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
