package com.cloreader;

import android.app.TabActivity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TabHost;

import com.cloreader.utils.ServerConstants;

public class EditFavoritesActivity extends TabActivity {

	@Override
	public void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.channel_tabs);

		//ViewGroup parent = (ViewGroup) findViewById(R.id.root);
		//LayoutInflater inflater = LayoutInflater.from(getBaseContext());

		//parent.addView(inflater.inflate(R.layout.logo, parent, false));
		//parent.addView(inflater.inflate(R.layout.channel_tabs, parent, false));

		Resources res = getResources(); // Resource object to get Drawables
	    TabHost tabHost = getTabHost();  // The activity TabHost
	    TabHost.TabSpec spec;  // Resusable TabSpec for each tab
	    Intent intent;  // Reusable Intent for each tab

	    // Create an Intent to launch an Activity for the tab (to be reused)
	    intent = new Intent().setClass(this, RecommendedChannelsActivity.class);

	    // Initialize a TabSpec for each tab and add it to the TabHost
	    spec = tabHost.newTabSpec("recchannels").setIndicator("Recommended Channels")
	                  .setContent(intent);
	    tabHost.addTab(spec);

	    // Do the same for the other tabs
	    intent = new Intent().setClass(this, UserChannelsActivity.class);
	    spec = tabHost.newTabSpec("yourchannels").setIndicator("Your Channels")
	                  .setContent(intent);
	    tabHost.addTab(spec);

	    intent = new Intent().setClass(this, PublicChannelsActivity.class);
	    spec = tabHost.newTabSpec("publicchannels").setIndicator("Public Channels")
	                  .setContent(intent);
	    tabHost.addTab(spec);

	    tabHost.setCurrentTab(0);
		
		Button logoutButton = (Button) findViewById(R.id.logout);

		logoutButton.setOnClickListener(new OnClickListener() {
			
			public void onClick(View v) {

				SharedPreferences settings = getSharedPreferences(ServerConstants.CLOREADER_GLOBAL_PREFS, 0);
				SharedPreferences.Editor editor = settings.edit();
				editor.clear();
				editor.commit();
				
				Intent intent = new Intent();
		        intent.setClass(EditFavoritesActivity.this, LoginActivity.class);
		        startActivity(intent);
		        finish();				
			}
		});

		
		Button createButton = (Button) findViewById(R.id.create);

		createButton.setOnClickListener(new OnClickListener() {
			
			public void onClick(View v) {

				Intent intent = new Intent();
		        intent.setClass(EditFavoritesActivity.this, CreateChannelActivity.class);
		        startActivity(intent);		        				
			}
		});

		
	}
}
