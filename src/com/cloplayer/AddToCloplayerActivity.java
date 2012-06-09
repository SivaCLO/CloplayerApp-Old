package com.cloplayer;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

public class AddToCloplayerActivity extends Activity {

	Messenger mService = null;
	boolean mIsBound;

	SharedPreferences globalSettings;

	TextView sourceUrlText;
	TextView headlineText;
	TextView detailText;
	
	String extra_text = "http://cloplayer.com";

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
			startService(new Intent(AddToCloplayerActivity.this, CloplayerService.class));
		}

		doBindService();
	}

	public void playSource(String sourceUrl) {
		sendStringMessageToService(CloplayerService.MSG_PLAY_SOURCE, sourceUrl);
	}

	private ServiceConnection mConnection = new ServiceConnection() {
		public void onServiceConnected(ComponentName className, IBinder service) {
			mService = new Messenger(service);
			
			playSource(extra_text); 

			/*Intent intentToGo = new Intent();
			intentToGo.setClass(AddToCloplayerActivity.this, PlayerActivity.class);
			startActivity(intentToGo);*/
			finish();
		}

		public void onServiceDisconnected(ComponentName className) {
			mService = null;
		}
	};

	void sendIntMessageToService(int messageId, int value) {
		if (mIsBound && mService != null) {
			try {
				Message msg = Message.obtain(null, messageId, value, 0);
				mService.send(msg);
			} catch (RemoteException e) {
			}
		}
	}

	void sendStringMessageToService(int messageId, String value) {
		if (mIsBound && mService != null) {
			try {
				Message msg = Message.obtain(null, messageId, value);
				mService.send(msg);
			} catch (RemoteException e) {
			}
		}
	}

	void sendEmptyMessageToService(int messageId) {
		if (mIsBound && mService != null) {
			try {
				Message msg = Message.obtain(null, messageId);
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

}
