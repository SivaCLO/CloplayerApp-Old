package com.cloreader;

import java.lang.reflect.Field;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.cloreader.story.Story;
import com.cloreader.utils.ServerConstants;
import com.cloreader.HomeActivity;
import com.cloreader.CloreaderService;
import com.cloreader.R;

public class HomeActivity extends Activity {

	private View[] storyViews;
	private AlertDialog pauseDialog;

	@Override
	public void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);

		Session.init(this);
		startService(new Intent(this, Session.class));
		
		// Load Pref Page
		Intent i = new Intent(HomeActivity.this, PreferencesManager.class);
		startActivityForResult(i, ServerConstants.ACTIVITY_PREFERENCE);
	}

	public void initializeUI() {

		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage(
				"Welcome back. \n\nYou can control me using the below options :\n\nScroll up / down - Next / Previous story\n\nSay \"Cloreader\" or touch the current news to pause me.\n\nWhen I am paused, I respond to the below voice commands:\n\n\"Next\" - Next Story\n\"Back\" - Previous Story\n\"Contine\" - Continue Reading\n\nYou can also change your preferences using the menu button.")
				.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						dialog.cancel();
						Session.setInitialized(true);
						Session.ttsManager.startReading();
					}
				});
		AlertDialog alert = builder.create();
		alert.show();

		pauseDialog = new ProgressDialog(this);
		// rec_dialog.setTitle(title);
		pauseDialog
				.setMessage("Listening to you Boss! Say Next, Back, Continue or Stop.");
		pauseDialog.setButton("Continue",
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						Session.sphinxManager.stopListening();
						Session.ttsManager.startReading();
						dialog.dismiss();
					}
				});

		pauseDialog
				.setOnDismissListener(new DialogInterface.OnDismissListener() {
					public void onDismiss(DialogInterface dialog) {
						Session.sphinxManager.stopListening();
						Session.ttsManager.startReading();
						dialog.dismiss();
					}
				});

		setContentView(R.layout.main);

		// Loading Main layout
		ViewGroup parent = (ViewGroup) findViewById(R.id.root);
		LayoutInflater inflater = LayoutInflater.from(getBaseContext());

		parent.addView(inflater.inflate(R.layout.logo, parent, false));

		storyViews = new View[ServerConstants.NUMBER_OF_STORIES];

		storyViews[0] = inflater.inflate(R.layout.current_story, parent, false);
		parent.addView(storyViews[0]);

		for (int i = 1; i < ServerConstants.NUMBER_OF_STORIES; i++) {
			storyViews[i] = inflater.inflate(R.layout.story, parent, false);
			parent.addView(storyViews[i]);
		}

		TextView headline = (TextView) storyViews[0]
				.findViewById(R.id.headline);
		headline.setSelected(true);

		//getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

		ActivitySwipeDetector activitySwipeDetector = new ActivitySwipeDetector(
				this);
		LinearLayout root = (LinearLayout) this.findViewById(R.id.root);
		root.setOnTouchListener(activitySwipeDetector);
	}

	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		switch (requestCode) {
		case (ServerConstants.ACTIVITY_DATA_CHECK): {
			// If TTS Present
			if (resultCode == TextToSpeech.Engine.CHECK_VOICE_DATA_PASS) {
				Session.ttsManager.createTTS();
			}

			// If TTS Not Present
			if (resultCode != TextToSpeech.Engine.CHECK_VOICE_DATA_PASS) {
				Session.ttsManager.installTTS();
			}
			break;
		}
		case (ServerConstants.ACTIVITY_PREFERENCE): {
			if (resultCode == Activity.RESULT_OK) {

				initializeUI();

				// Set Preferences
				setPreferences();

				// Check for TTS data
				Session.ttsManager.checkTTS();
			}
			break;
		}
		}
	}

	public void setPreferences() {
		SharedPreferences settings = getSharedPreferences(
				ServerConstants.CLOREADER_CHANNEL_PREFS, 0);

		JSONObject preferences = null;
		try {
			preferences = new JSONObject(settings.getString(getResources()
					.getString(R.string.preferenceResultString), ""));

			JSONObject createResult = Session.apiClient.createUser(settings.getString("email", ""));			
			Session.apiClient.setUserId(createResult.getString("userId"));
			
			JSONObject result = Session.apiClient.topics_set(preferences);
			Log.e(ServerConstants.TAG, result.toString());
		} catch (JSONException ignored) {
		}
	}

	public void loadMainScreen() {
		Session.storyManager.fillQueue();
	}

	// public HashMap<Integ, int> storyMap = new

	public void showStory(Story newStory) {
		int location = newStory.getLocation();

		if (newStory.isNoStories()) {
			setFieldText(storyViews[location], R.id.headline, "No more stories");
			setFieldText(storyViews[location], R.id.source, "");
			setNewsLogo(storyViews[location], 0);
		} else if (newStory.isLoaded()) {
			setFieldText(storyViews[location], R.id.headline,
					newStory.getHeadLine());
			setFieldText(
					storyViews[location],
					R.id.source,
					newStory.getSource() + " · "
							+ getDateString(newStory.getDate()));

			int logoId = findNewsImageId(newStory.getSource().toLowerCase()
					.replaceAll(" ", ""));
			setNewsLogo(storyViews[location], logoId);
		} else {
			setFieldText(storyViews[location], R.id.headline, "Loading...");
			setFieldText(storyViews[location], R.id.source, "");
			setNewsLogo(storyViews[location], 0);
		}

		if (location == 0 && newStory.isLoaded()) {
			Session.ttsManager.startReading();
		}

	}

	int findNewsImageId(String source) {

		Class<R.drawable> res = R.drawable.class;

		try {
			Field field = res.getField("news_logo_" + source);
			return field.getInt(null);
		} catch (Exception e) {
			return 0;
		}
	}

	public String getDateString(String date) {

		long milliseconds1 = Long.parseLong(date);
		long milliseconds2 = new Date().getTime();

		long diff = milliseconds2 - milliseconds1;
		String suffix = " minutes ago";
		diff = diff / (60 * 1000);
		if (diff > 59) {
			diff = diff / 60;
			suffix = " hours ago";
			if (diff > 23) {
				diff = diff / 24;
				suffix = " days ago";
			}
		}

		return diff + suffix;
	}

	private void setFieldText(final View view, final int field,
			final String text) {
		runOnUiThread(new Runnable() {
			public void run() {
				((TextView) view.findViewById(field)).setText(text.replaceAll(
						"\\<.*?>", ""));
			}
		});

	}

	private void setNewsLogo(final View view, final int logoId) {
		runOnUiThread(new Runnable() {
			public void run() {
				if (logoId != 0)
					((ImageView) view.findViewById(R.id.logo))
							.setImageResource(logoId);
				else
					((ImageView) view.findViewById(R.id.logo))
							.setImageResource(R.drawable.news_logo_default);
			}
		});

	}

	String getFieldText(View view, int field) {
		return ((TextView) view.findViewById(field)).getText().toString();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.menu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.preferences:
			Intent i = new Intent(this, PreferencesManager.class);
			i.putExtra("dontIgnore", true);
			startActivityForResult(i, ServerConstants.ACTIVITY_PREFERENCE);
			break;
		}
		return true;
	}

	@Override
	protected void onStop() {
		Session.stop();
		super.onStop();
	}

	@Override
	protected void onStart() {
		super.onStart();

		SharedPreferences settings = getSharedPreferences(
				ServerConstants.CLOREADER_CHANNEL_PREFS, 0);

		boolean isPreferenceChanged = settings.getBoolean(
				"isPreferenceChanged", false);

		if (!isPreferenceChanged) {
			Session.restart();
		} else {
			SharedPreferences.Editor editor = settings.edit();
			editor.putBoolean("isPreferenceChanged", false);
			editor.commit();
		}

	}

	public void onMainStoryClick(View v) {
		Session.ttsManager.toggleReading();
	}

	public void showPauseDialog() {
		pauseDialog.show();
		Session.ttsManager.beep();
	}

	public void cancelPauseDialog() {
		if (pauseDialog != null) {
			pauseDialog.dismiss();
		}
	}
	
	
	
	
	
	
	
	/*
	
	
	
	
    Button btnStart, btnStop, btnBind, btnUnbind, btnUpby1, btnUpby10;
    TextView textStatus, textIntValue, textStrValue;
    Messenger mService = null;
    boolean mIsBound;
    final Messenger mMessenger = new Messenger(new IncomingHandler());

    class IncomingHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case CloreaderService.MSG_SET_INT_VALUE:
                textIntValue.setText("Int Message: " + msg.arg1);
                break;
            case CloreaderService.MSG_SET_STRING_VALUE:
                String str1 = msg.getData().getString("str1");
                textStrValue.setText("Str Message: " + str1);
                break;
            default:
                super.handleMessage(msg);
            }
        }
    }
    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            mService = new Messenger(service);
            textStatus.setText("Attached.");
            try {
                Message msg = Message.obtain(null, CloreaderService.MSG_REGISTER_CLIENT);
                msg.replyTo = mMessenger;
                mService.send(msg);
            } catch (RemoteException e) {
                // In this case the service has crashed before we could even do anything with it
            }
        }

        public void onServiceDisconnected(ComponentName className) {
            // This is called when the connection with the service has been unexpectedly disconnected - process crashed.
            mService = null;
            textStatus.setText("Disconnected.");
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        btnStart = (Button)findViewById(R.id.btnStart);
        btnStop = (Button)findViewById(R.id.btnStop);
        btnBind = (Button)findViewById(R.id.btnBind);
        btnUnbind = (Button)findViewById(R.id.btnUnbind);
        textStatus = (TextView)findViewById(R.id.textStatus);
        textIntValue = (TextView)findViewById(R.id.textIntValue);
        textStrValue = (TextView)findViewById(R.id.textStrValue);
        btnUpby1 = (Button)findViewById(R.id.btnUpby1);
        btnUpby10 = (Button)findViewById(R.id.btnUpby10);

        btnStart.setOnClickListener(btnStartListener);
        btnStop.setOnClickListener(btnStopListener);
        btnBind.setOnClickListener(btnBindListener);
        btnUnbind.setOnClickListener(btnUnbindListener);
        btnUpby1.setOnClickListener(btnUpby1Listener);
        btnUpby10.setOnClickListener(btnUpby10Listener);

        restoreMe(savedInstanceState);

        CheckIfServiceIsRunning();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        Log.e("Test", "Save Instance State");
        outState.putString("textStatus", textStatus.getText().toString());
        outState.putString("textIntValue", textIntValue.getText().toString());
        outState.putString("textStrValue", textStrValue.getText().toString());
    }
    private void restoreMe(Bundle state) {
        if (state!=null) {
            textStatus.setText(state.getString("textStatus"));
            textIntValue.setText(state.getString("textIntValue"));
            textStrValue.setText(state.getString("textStrValue"));
        }
    }
    private void CheckIfServiceIsRunning() {
        //If the service is running when the activity starts, we want to automatically bind to it.
        if (CloreaderService.isRunning()) {
            doBindService();
        }
    }

    private OnClickListener btnStartListener = new OnClickListener() {
        public void onClick(View v){
            startService(new Intent(CloreaderActivity.this, CloreaderService.class));
        }
    };
    private OnClickListener btnStopListener = new OnClickListener() {
        public void onClick(View v){
            doUnbindService();
            stopService(new Intent(CloreaderActivity.this, CloreaderService.class));
        }
    };
    private OnClickListener btnBindListener = new OnClickListener() {
        public void onClick(View v){
            doBindService();
        }
    };
    private OnClickListener btnUnbindListener = new OnClickListener() {
        public void onClick(View v){
            doUnbindService();
        }
    };
    private OnClickListener btnUpby1Listener = new OnClickListener() {
        public void onClick(View v){
            sendMessageToService(1);
        }
    };
    private OnClickListener btnUpby10Listener = new OnClickListener() {
        public void onClick(View v){
            sendMessageToService(10);
        }
    };
    private void sendMessageToService(int intvaluetosend) {
        if (mIsBound) {
            if (mService != null) {
                try {
                    Message msg = Message.obtain(null, CloreaderService.MSG_SET_INT_VALUE, intvaluetosend, 0);
                    msg.replyTo = mMessenger;
                    mService.send(msg);
                } catch (RemoteException e) {
                }
            }
        }
    }


    void doBindService() {
        bindService(new Intent(this, CloreaderService.class), mConnection, Context.BIND_AUTO_CREATE);
        mIsBound = true;
        textStatus.setText("Binding.");
    }
    void doUnbindService() {
        if (mIsBound) {
            // If we have received the service, and hence registered with it, then now is the time to unregister.
            if (mService != null) {
                try {
                    Message msg = Message.obtain(null, CloreaderService.MSG_UNREGISTER_CLIENT);
                    msg.replyTo = mMessenger;
                    mService.send(msg);
                } catch (RemoteException e) {
                    // There is nothing special we need to do if the service has crashed.
                }
            }
            // Detach our existing connection.
            unbindService(mConnection);
            mIsBound = false;
            textStatus.setText("Unbinding.");
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            doUnbindService();
        } catch (Throwable t) {
            Log.e("CloreaderActivity", "Failed to unbind from the service", t);
        }
    }
	
*/	
	
	
}