package com.cloplayer;

import android.app.Activity;
import android.app.ProgressDialog;
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
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.cloplayer.utils.ServerConstants;

public class PlayerActivity extends Activity {

	final Messenger mClient = new Messenger(new IncomingHandler());
	Messenger mService = null;
	boolean mIsBound;

	SharedPreferences globalSettings;

	TextView sourceUrlText;
	TextView headlineText;
	TextView detailText;
	ProgressBar progress;

	ProgressDialog progDailog;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		ViewGroup parent = (ViewGroup) findViewById(R.id.root);
		LayoutInflater inflater = LayoutInflater.from(getBaseContext());

		parent.addView(inflater.inflate(R.layout.logo, parent, false));
		parent.addView(inflater.inflate(R.layout.player, parent, false));

		sourceUrlText = (TextView) findViewById(R.id.sourceUrl);
		headlineText = (TextView) findViewById(R.id.headline);
		detailText = (TextView) findViewById(R.id.detail);
		progress = (ProgressBar) findViewById(R.id.progressBar);

		detailText.setMovementMethod(ScrollingMovementMethod.getInstance());

		globalSettings = getSharedPreferences(ServerConstants.CLOPLAYER_GLOBAL_PREFS, 0);

		Button stopButton = (Button) findViewById(R.id.stop_button);
		stopButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				sendEmptyMessageToService(CloplayerService.MSG_STOP_PLAYING);
				finish();
			}
		});

		Button homeButton = (Button) findViewById(R.id.minimize_button);
		homeButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				finish();
			}
		});

		Button pauseButton = (Button) findViewById(R.id.pause_button);
		pauseButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				sendEmptyMessageToService(CloplayerService.MSG_PAUSE_PLAYING);
			}
		});

		Button unpauseButton = (Button) findViewById(R.id.unpause_button);
		unpauseButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				sendEmptyMessageToService(CloplayerService.MSG_UNPAUSE_PLAYING);
			}
		});

		Button next1Button = (Button) findViewById(R.id.next1_button);
		next1Button.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				sendEmptyMessageToService(CloplayerService.MSG_NEXT1);
			}
		});

		Button next5Button = (Button) findViewById(R.id.next5_button);
		next5Button.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				sendEmptyMessageToService(CloplayerService.MSG_NEXT5);
			}
		});

		Button back1Button = (Button) findViewById(R.id.back1_button);
		back1Button.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				sendEmptyMessageToService(CloplayerService.MSG_BACK1);
			}
		});

		Button back5Button = (Button) findViewById(R.id.back5_button);
		back5Button.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				sendEmptyMessageToService(CloplayerService.MSG_BACK5);
			}
		});

		progDailog = new ProgressDialog(this);
		progDailog.setMessage("Loading...");
		progDailog.setIndeterminate(true);

		resumeService();

	}

	private void resumeService() {
		if (CloplayerService.isRunning()) {
			String headline = globalSettings.getString("nowPlayingHeadline", "");
			String detail = globalSettings.getString("nowPlayingDetail", "");
			String sourceUrl = globalSettings.getString("nowPlayingSource", "");
			int progressMax = globalSettings.getInt("nowPlayingProgressMax", 1);
			int playProgress = globalSettings.getInt("nowPlayingPlayProgress", 0);
			int downloadProgress = globalSettings.getInt("nowPlayingDownloadProgress", 0);

			sourceUrlText.setText(sourceUrl);
			headlineText.setText(headline);
			detailText.setText(detail);

			progress.setMax(progressMax);
			progress.setProgress(playProgress);
			progress.setSecondaryProgress(downloadProgress);
		} else {
			startService(new Intent(PlayerActivity.this, CloplayerService.class));
		}

		doBindService();

		if (progress.getProgress() == progress.getSecondaryProgress() && progress.getProgress() != progress.getMax()) {
			progDailog.show();
		}
	}

	private ServiceConnection mConnection = new ServiceConnection() {
		public void onServiceConnected(ComponentName className, IBinder service) {
			mService = new Messenger(service);
			sendEmptyMessageToService(CloplayerService.MSG_REGISTER_CLIENT);
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
			if (mService != null) {
				sendEmptyMessageToService(CloplayerService.MSG_UNREGISTER_CLIENT);
			}
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
			case CloplayerService.MSG_UPDATE_SOURCE_URL:
				sourceUrlText.setText((String) msg.obj);
				break;
			case CloplayerService.MSG_UPDATE_HEADLINE:
				headlineText.setText((String) msg.obj);
				break;
			case CloplayerService.MSG_UPDATE_DETAIL:
				detailText.setText((String) msg.obj);
				break;
			case CloplayerService.MSG_UPDATE_PLAY_PROGRESS:
				progress.setProgress(msg.arg1);
				if (progress.getProgress() == progress.getSecondaryProgress() && progress.getProgress() != progress.getMax()) {
					progDailog.show();
				}
				break;
			case CloplayerService.MSG_UPDATE_DOWNLOAD_PROGRESS:
				progress.setSecondaryProgress(msg.arg1);
				progDailog.hide();
				break;
			case CloplayerService.MSG_UPDATE_PROGRESS_MAX:
				progress.setMax(msg.arg1);
				break;
			default:
				super.handleMessage(msg);
			}
		}
	}
}