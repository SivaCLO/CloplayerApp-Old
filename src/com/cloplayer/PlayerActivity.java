package com.cloplayer;

import android.app.Activity;
import android.app.ProgressDialog;
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
import android.text.method.ScrollingMovementMethod;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.cloplayer.sqlite.Story;
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

		//parent.addView(inflater.inflate(R.layout.logo, parent, false));
		parent.addView(inflater.inflate(R.layout.player, parent, false));

		sourceUrlText = (TextView) findViewById(R.id.sourceUrl);
		headlineText = (TextView) findViewById(R.id.headline);
		progress = (ProgressBar) findViewById(R.id.progress_bar);
		detailText = (TextView) findViewById(R.id.detail_text);
		

		detailText.setMovementMethod(ScrollingMovementMethod.getInstance());

		globalSettings = getSharedPreferences(ServerConstants.CLOPLAYER_GLOBAL_PREFS, 0);

		ImageButton stopButton = (ImageButton) findViewById(R.id.stop_button);
		stopButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				Intent intentToGo = new Intent();
				intentToGo.setClass(PlayerActivity.this, LibraryActivity.class);
				startActivity(intentToGo);
				finish();
			}
		});

		ImageButton homeButton = (ImageButton) findViewById(R.id.minimize_button);
		homeButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				finish();
			}
		});

		ImageButton unpauseButton = (ImageButton) findViewById(R.id.pause_button);
		unpauseButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				sendEmptyMessageToService(CloplayerService.MSG_PAUSE_UNPAUSE_PLAYING);
			}
		});

		ImageButton next1Button = (ImageButton) findViewById(R.id.next1_button);
		next1Button.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				sendEmptyMessageToService(CloplayerService.MSG_NEXT1);
			}
		});

		ImageButton next5Button = (ImageButton) findViewById(R.id.next5_button);
		next5Button.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				sendEmptyMessageToService(CloplayerService.MSG_NEXT5);
			}
		});

		ImageButton back1Button = (ImageButton) findViewById(R.id.back1_button);
		back1Button.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				sendEmptyMessageToService(CloplayerService.MSG_BACK1);
			}
		});

		ImageButton back5Button = (ImageButton) findViewById(R.id.back5_button);
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
		if (!CloplayerService.isRunning()) {
			startService(new Intent(PlayerActivity.this, CloplayerService.class));
		}

		doBindService();
	}

	private ServiceConnection mConnection = new ServiceConnection() {
		public void onServiceConnected(ComponentName className, IBinder service) {
			mService = new Messenger(service);
			sendEmptyMessageToService(CloplayerService.MSG_REGISTER_CLIENT);

			int storyId = globalSettings.getInt("nowPlaying", -1);
			if (storyId != -1) {
				Story story = CloplayerService.getInstance().datasource.getStory(storyId);
				updateUI(true, story);
			} else {
				headlineText.setText("Loading");
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
			case CloplayerService.MSG_UPDATE_STORY:
				Story story = CloplayerService.getInstance().datasource.getStory(msg.arg1);
				if (story.getId() == CloplayerService.getInstance().currentStory.getId())
					updateUI(false, story);
				break;
			default:
				super.handleMessage(msg);
			}
		}
	}

	public void updateUI(boolean first, Story story) {
		sourceUrlText.setText(story.getDomain());
		headlineText.setText(story.getHeadline());
		String text = globalSettings.getString(story.getId() + "." + story.getPlayProgress() + ".text", "");
		detailText.setText(story.getDetail(), TextView.BufferType.SPANNABLE);

		if (text != null && text.length() != 0 && story.getDetail().indexOf(text) != -1) {
			Spannable WordtoSpan = (Spannable) detailText.getText();
			WordtoSpan.setSpan(new ForegroundColorSpan(Color.BLACK), story.getDetail().indexOf(text), story.getDetail().indexOf(text) + text.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
			detailText.setText(WordtoSpan);
		}

		progress.setMax(story.getItemCount());
		progress.setProgress(story.getPlayProgress());
		progress.setSecondaryProgress(story.getDownloadProgress());

		if (progress.getProgress() == progress.getSecondaryProgress() && progress.getProgress() != progress.getMax()) {
			progDailog.show();
		} else {
			progDailog.hide();
		}

		if (!first && progress.getProgress() == progress.getMax()) {
			finish();
		}
		
		ImageButton pauseButton = (ImageButton) findViewById(R.id.pause_button);
		if(story.isPlaying()) {			
			pauseButton.setImageResource(R.drawable.pause);
		} else {
			pauseButton.setImageResource(R.drawable.play);
		}
	}
}