package com.cloplayer;

import java.util.HashMap;
import java.util.List;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.database.DataSetObserver;
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
import android.widget.ListView;
import android.widget.TextView;

import com.cloplayer.sqlite.Story;

public class LibraryActivity extends Activity {

	final Messenger mClient = new Messenger(new IncomingHandler());

	Messenger mService = null;
	boolean mIsBound;

	SharedPreferences globalSettings;

	TextView sourceUrlText;
	TextView headlineText;
	TextView detailText;

	LayoutInflater inflater;

	HashMap<Integer, View> storyLocations;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.home);

		Button refreshButton = (Button) findViewById(R.id.refresh_articles);
		refreshButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				triggerRefresh();
			}
		});

		inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	}

	public void onResume() {
		super.onResume();
		resumeService();
	}

	public void onPause() {
		super.onPause();
		doUnbindService();
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
			mService = new Messenger(service);
			sendEmptyMessageToService(CloplayerService.MSG_REGISTER_CLIENT);
			updateUI();

			triggerRefresh();
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
		mIsBound = true;
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
				resetRefreshButton();
				break;
			case CloplayerService.MSG_UPDATE_STORY:
				updateStory(msg.arg1);
				break;
			case CloplayerService.MSG_ADD_STORY:
				// should add dynamically instead of refreshing page.
				updateUI();
				break;
			default:
				super.handleMessage(msg);
			}
		}
	}

	public void updateStory(int id) {
		View rowView = storyLocations.get(id);
		Story story = CloplayerService.getInstance().datasource.getStory(id);

		if (rowView != null && story != null)
			paintStory(rowView, story);
		else
			Log.e("Home", "View not Found : " + id);			
	}

	public void paintStory(View view, Story story) {
		
		//This doesn't update the UI. Need to change.
		
		TextView headlineView = (TextView) view.findViewById(R.id.headline);
		headlineView.setText(story.getPlayProgress() + " > " + story.getHeadline());
		TextView sourceView = (TextView) view.findViewById(R.id.source);
		sourceView.setText(story.getDomain());
	}

	public void updateUI() {
		final List<Story> values = CloplayerService.getInstance().datasource.getAllStories();
		ListView lv = (ListView) findViewById(R.id.list);
		storyLocations = new HashMap<Integer, View>();
		lv.setAdapter(new ArrayAdapter<Story>(LibraryActivity.this, android.R.layout.simple_list_item_1, values) {

			@Override
			public View getView(int position, View convertView, ViewGroup parent) {

				Story story = getItem(position);
				View rowView = inflater.inflate(R.layout.current_story, parent, false);

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
			}
		});

		resetRefreshButton();
	}

	public void resetRefreshButton() {
		final Button refreshButton = (Button) findViewById(R.id.refresh_articles);
		refreshButton.setEnabled(true);
		refreshButton.setText("Refresh");
	}

	public void triggerRefresh() {
		final Button refreshButton = (Button) findViewById(R.id.refresh_articles);
		refreshButton.setEnabled(false);
		refreshButton.setText("Refreshing");
		sendEmptyMessageToService(CloplayerService.MSG_REFRESH_ARTICLES);
	}

}