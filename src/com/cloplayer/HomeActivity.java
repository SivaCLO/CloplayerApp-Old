package com.cloplayer;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.text.Spannable;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.cloplayer.PlayerActivity.IncomingHandler;
import com.cloplayer.sqlite.Story;

public class HomeActivity extends Activity {

	final Messenger mClient = new Messenger(new IncomingHandler());

	Messenger mService = null;
	boolean mIsBound;

	SharedPreferences globalSettings;

	TextView sourceUrlText;
	TextView headlineText;
	TextView detailText;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		resumeService();

		setContentView(R.layout.home);
	}

	private void resumeService() {
		if (!CloplayerService.isRunning()) {
			startService(new Intent(HomeActivity.this, CloplayerService.class));
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
			case CloplayerService.MSG_UPDATE_STORY:
				//updateUI();
				break;
			default:
				super.handleMessage(msg);
			}
		}
	}

	public void updateUI() {
		final List<Story> values = CloplayerService.getInstance().datasource.getAllStories();
		ArrayAdapter<Story> adapter = new ArrayAdapter<Story>(HomeActivity.this, android.R.layout.simple_list_item_1, values);
		ListView lv = (ListView) findViewById(R.id.list);
		lv.setAdapter(adapter);
		lv.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int position, long id) {
				Story story = values.get(position);
				playSource(story.getUrl());

				Intent intentToGo = new Intent();
				intentToGo.setClass(HomeActivity.this, PlayerActivity.class);
				startActivity(intentToGo);
			}
		});
	}

}