package com.cloplayer;

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
import android.widget.TextView;
import android.widget.Toast;

import com.cloplayer.LibraryActivity.IncomingHandler;
import com.cloplayer.http.URLHelper;
import com.cloplayer.utils.ServerConstants;

public class PlayNowActivity extends Activity {

	Messenger mService = null;
	final Messenger mClient = new Messenger(new IncomingHandler());
	boolean mIsBound;

	SharedPreferences globalSettings;

	TextView sourceUrlText;
	TextView headlineText;
	TextView detailText;

	String extra_text = URLHelper.home();

	@Override
	public void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);

		// Get the intent that started this activity
		Intent intent = getIntent();

		Bundle extras = intent.getExtras();

		if (extras != null)
			extra_text = extras.getString(Intent.EXTRA_TEXT);

		resumeService();

	}

	private void resumeService() {
		if (!CloplayerService.isRunning()) {
			startService(new Intent(PlayNowActivity.this, CloplayerService.class));
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

			Log.e("PlayNowActivity", "UserId : " + userId);

			if (userId == null) {
				Log.e("PlayNowActivity", "User not logged in");
				Intent intentToGo = new Intent();
				intentToGo.setClass(PlayNowActivity.this, HomeActivity.class);
				startActivity(intentToGo);
				Toast.makeText(PlayNowActivity.this, "Please login to cloplayer and try again", Toast.LENGTH_SHORT).show();
				finish();
			} else {
				Log.e("PlayNowActivity", "User logged in as : " + userId);

				Toast.makeText(PlayNowActivity.this, "Will be played shortly...", Toast.LENGTH_SHORT).show();

				playSource(extra_text);

				/*
				 * Intent intentToGo = new Intent();
				 * intentToGo.setClass(AddToCloplayerActivity.this,
				 * PlayerActivity.class); startActivity(intentToGo);
				 */
				finish();
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
			case CloplayerService.MSG_BOOTSTRAP_COMPLETE:
				break;
			case CloplayerService.MSG_NETWORK_ERROR:
				Toast.makeText(PlayNowActivity.this, "Network Failure. Cloplayer will be in offline mode.", Toast.LENGTH_SHORT).show();
				finish();
				break;
			default:
				super.handleMessage(msg);
			}
		}
	}

}
