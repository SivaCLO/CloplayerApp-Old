package com.cloplayer;

import java.util.HashMap;
import java.util.List;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.cloplayer.sqlite.Story;
import com.cloplayer.utils.ServerConstants;

public class LibraryActivity extends Activity {

	final Messenger mClient = new Messenger(new IncomingHandler());

	Messenger mService = null;
	boolean mIsBound;
	boolean mIsFirstTime = false;

	SharedPreferences globalSettings;

	TextView sourceUrlText;
	TextView headlineText;
	TextView detailText;

	LayoutInflater inflater;

	int storyCategory = CloplayerService.CATEGORY_UNREAD;

	HashMap<Integer, View> storyLocations;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.library);

		TextView refreshButton = (TextView) findViewById(R.id.refresh_articles);
		refreshButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				triggerRefresh();
			}
		});

		final ImageButton newButton = (ImageButton) findViewById(R.id.new_button);
		newButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				storyCategory = CloplayerService.CATEGORY_UNREAD;
				updateUI();
			}
		});

		final ImageButton favButton = (ImageButton) findViewById(R.id.archive_button);
		favButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				storyCategory = CloplayerService.CATEGORY_READ;
				updateUI();
			}
		});

		final ImageButton playAllButton = (ImageButton) findViewById(R.id.play_all);
		playAllButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				sendIntMessageToService(CloplayerService.MSG_PLAY_ALL, storyCategory);
			}
		});

		inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);

		resumeService();
	}

	public void onResume() {
		super.onResume();

		if (mIsBound) {
			updateUI();
			if (mIsFirstTime) {

				SharedPreferences globalSettings = CloplayerService.getInstance().getSharedPreferences(ServerConstants.CLOPLAYER_GLOBAL_PREFS, 0);
				String userId = globalSettings.getString("userId", null);

				Log.e("LibraryActivity", "UserId : " + userId);

				if (userId != null) {
					triggerRefresh();
					mIsFirstTime = false;
				}
			}
		}

	}

	private void resumeService() {
		if (!CloplayerService.isRunning()) {
			startService(new Intent(LibraryActivity.this, CloplayerService.class));
		}

		doBindService();
	}

	public void playSource(String sourceUrl) {
		sendStringMessageToService(CloplayerService.MSG_PLAY_SOURCE, sourceUrl);
	}

	private ServiceConnection mConnection = new ServiceConnection() {
		public void onServiceConnected(ComponentName className, IBinder service) {
			mIsBound = true;
			mService = new Messenger(service);
			sendEmptyMessageToService(CloplayerService.MSG_REGISTER_CLIENT);

			SharedPreferences globalSettings = CloplayerService.getInstance().getSharedPreferences(ServerConstants.CLOPLAYER_GLOBAL_PREFS, 0);
			String userId = globalSettings.getString("userId", null);

			Log.e("LibraryActivity", "UserId : " + userId);

			if (userId == null) {
				mIsFirstTime = true;
				Log.e("LibraryActivity", "User not logged in");
				Intent intentToGo = new Intent();
				intentToGo.setClass(LibraryActivity.this, HomeActivity.class);
				startActivity(intentToGo);
			} else {
				Log.e("LibraryActivity", "User logged in as : " + userId);
				updateUI();
				triggerRefresh();
			}
		}

		public void onServiceDisconnected(ComponentName className) {
			mService = null;
		}
	};

	void sendIntMessageToService(int messageId, int value) {
		if (mIsBound && mService != null) {
			try {
				Message msg = Message.obtain(null, messageId, value, 0);
				msg.replyTo = mClient;
				mService.send(msg);
			} catch (RemoteException e) {
			}
		}
	}

	void sendStringMessageToService(int messageId, String value) {
		if (mIsBound && mService != null) {
			try {
				Message msg = Message.obtain(null, messageId, value);
				msg.replyTo = mClient;
				mService.send(msg);
			} catch (RemoteException e) {
			}
		}
	}

	void sendEmptyMessageToService(int messageId) {
		if (mIsBound && mService != null) {
			try {
				Message msg = Message.obtain(null, messageId);
				msg.replyTo = mClient;
				mService.send(msg);
			} catch (RemoteException e) {
			}
		}
	}

	void doBindService() {
		bindService(new Intent(this, CloplayerService.class), mConnection, Context.BIND_AUTO_CREATE);
	}

	void doUnbindService() {
		if (mIsBound) {
			unbindService(mConnection);
			mIsBound = false;
		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		try {
			doUnbindService();
		} catch (Throwable t) {
			Log.e("CloplayerActivity", "Failed to unbind from the service", t);
		}
	}

	class IncomingHandler extends Handler {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case CloplayerService.MSG_REFRESH_ARTICLES_COMPLETE:
				updateUI();
				break;
			default:
				super.handleMessage(msg);
			}
		}
	}

	public void paintStory(View view, Story story) {
		TextView headlineView = (TextView) view.findViewById(R.id.headline);
		headlineView.setText(story.getHeadline());
		TextView sourceView = (TextView) view.findViewById(R.id.source);
		sourceView.setText(story.getDomain());
	}

	public void updateUI() {

		SharedPreferences globalSettings = CloplayerService.getInstance().getSharedPreferences(ServerConstants.CLOPLAYER_GLOBAL_PREFS, 0);
		int storyId = globalSettings.getInt("nowPlaying", -1);
		if (storyId != -1) {
			Story story = CloplayerService.getInstance().datasource.getStory(storyId);
			((TextView) findViewById(R.id.now_playing_text)).setText(story.getHeadline());
			((LinearLayout) findViewById(R.id.now_playing)).setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View arg0) {
					Intent intentToGo = new Intent();
					intentToGo.setClass(LibraryActivity.this, PlayerActivity.class);
					startActivity(intentToGo);
					finish();
				}
			});
			((LinearLayout) findViewById(R.id.now_playing)).setVisibility(View.VISIBLE);
		} else {
			((LinearLayout) findViewById(R.id.now_playing)).setVisibility(View.GONE);
		}

		final List<Story> values;

		if (storyCategory == CloplayerService.CATEGORY_UNREAD) {
			((TextView) findViewById(R.id.title)).setText("NEW");
			values = CloplayerService.getInstance().datasource.getUnplayedStories();
			((ImageButton) findViewById(R.id.play_all)).setVisibility(View.VISIBLE);
		} else {
			((TextView) findViewById(R.id.title)).setText("ARCHIVES");
			values = CloplayerService.getInstance().datasource.getplayedStories();
			((ImageButton) findViewById(R.id.play_all)).setVisibility(View.GONE);
		}

		ListView lv = (ListView) findViewById(R.id.list);
		storyLocations = new HashMap<Integer, View>();
		lv.setAdapter(new ArrayAdapter<Story>(LibraryActivity.this, android.R.layout.simple_list_item_1, values) {

			@Override
			public View getView(int position, View convertView, ViewGroup parent) {

				Story story = getItem(position);
				View rowView = inflater.inflate(R.layout.story, parent, false);

				paintStory(rowView, story);

				storyLocations.put((int) story.getId(), rowView);

				return rowView;

			}
		});

		lv.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int position, long id) {
				Story story = values.get(position);
				playSource(story.getUrl());

				Intent intentToGo = new Intent();
				intentToGo.setClass(LibraryActivity.this, PlayerActivity.class);
				startActivity(intentToGo);
				finish();
			}
		});

		boolean refreshing = globalSettings.getBoolean("refreshing", false);
		if (refreshing)
			setRefreshButton();
		else
			resetRefreshButton();
	}

	public void resetRefreshButton() {
		final TextView refreshButton = (TextView) findViewById(R.id.refresh_articles);
		refreshButton.setEnabled(true);
		refreshButton.setText("Refresh");
	}

	public void setRefreshButton() {
		final TextView refreshButton = (TextView) findViewById(R.id.refresh_articles);
		refreshButton.setEnabled(false);
		refreshButton.setText("Refreshing");
	}

	public void triggerRefresh() {
		setRefreshButton();
		sendEmptyMessageToService(CloplayerService.MSG_REFRESH_ARTICLES);
	}

}