package com.cloplayer;

import java.lang.reflect.Field;
import java.util.Date;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.cloplayer.story.Story;
import com.cloplayer.utils.ServerConstants;

public class CloplayerService extends Activity {

	private View[] storyViews;
	private AlertDialog pauseDialog;

	@Override
	public void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);

		//Session.init(this);
		startService(new Intent(this, Session.class));
		
		// Load Pref Page
		Intent i = new Intent(CloplayerService.this, PreferencesManager.class);
		startActivityForResult(i, ServerConstants.ACTIVITY_PREFERENCE);
	}

	public void initializeUI() {

		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage(
				"Welcome back. \n\nYou can control me using the below options :\n\nScroll up / down - Next / Previous story\n\nSay \"Cloplayer\" or touch the current news to pause me.\n\nWhen I am paused, I respond to the below voice commands:\n\n\"Next\" - Next Story\n\"Back\" - Previous Story\n\"Contine\" - Continue Reading\n\nYou can also change your preferences using the menu button.")
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

		//ActivitySwipeDetector activitySwipeDetector = new ActivitySwipeDetector(
		//		this);
		//LinearLayout root = (LinearLayout) this.findViewById(R.id.root);
		//root.setOnTouchListener(activitySwipeDetector);
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
				ServerConstants.CLOPLAYER_CHANNEL_PREFS, 0);

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
				ServerConstants.CLOPLAYER_CHANNEL_PREFS, 0);

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
	
    private NotificationManager nm;
    private Timer timer = new Timer();
    private int counter = 0, incrementby = 1;
    private static boolean isRunning = false;

    ArrayList<Messenger> mClients = new ArrayList<Messenger>(); // Keeps track of all current registered clients.
    int mValue = 0; // Holds last value set by a client.
    static final int MSG_REGISTER_CLIENT = 1;
    static final int MSG_UNREGISTER_CLIENT = 2;
    static final int MSG_SET_INT_VALUE = 3;
    static final int MSG_SET_STRING_VALUE = 4;
    final Messenger mMessenger = new Messenger(new IncomingHandler()); // Target we publish for clients to send messages to IncomingHandler.


    @Override
    public IBinder onBind(Intent intent) {
        return mMessenger.getBinder();
    }
    class IncomingHandler extends Handler { // Handler of incoming messages from clients.
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case MSG_REGISTER_CLIENT:
                mClients.add(msg.replyTo);
                break;
            case MSG_UNREGISTER_CLIENT:
                mClients.remove(msg.replyTo);
                break;
            case MSG_SET_INT_VALUE:
                incrementby = msg.arg1;
                break;
            default:
                super.handleMessage(msg);
            }
        }
    }
    private void sendMessageToUI(int intvaluetosend) {
        for (int i=mClients.size()-1; i>=0; i--) {
            try {
                // Send data as an Integer
                mClients.get(i).send(Message.obtain(null, MSG_SET_INT_VALUE, intvaluetosend, 0));

                //Send data as a String
                Bundle b = new Bundle();
                b.putString("str1", "ab" + intvaluetosend + "cd");
                Message msg = Message.obtain(null, MSG_SET_STRING_VALUE);
                msg.setData(b);
                mClients.get(i).send(msg);

            } catch (RemoteException e) {
                // The client is dead. Remove it from the list; we are going through the list from back to front so this is safe to do inside the loop.
                mClients.remove(i);
            }
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i("CloplayerService", "Service Started.");
        showNotification();
        timer.scheduleAtFixedRate(new TimerTask(){ public void run() {onTimerTick();}}, 0, 100L);
        isRunning = true;
    }
    private void showNotification() {
        nm = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        // In this sample, we'll use the same text for the ticker and the expanded notification
        CharSequence text = getText(R.string.service_started);
        // Set the icon, scrolling text and timestamp
        Notification notification = new Notification(R.drawable.ic_launcher, text, System.currentTimeMillis());
        // The PendingIntent to launch our activity if the user selects this notification
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, new Intent(this, CloplayerActivity.class), 0);
        // Set the info for the views that show in the notification panel.
        notification.setLatestEventInfo(this, getText(R.string.service_label), text, contentIntent);
        // Send the notification.
        // We use a layout id because it is a unique number.  We use it later to cancel.
        nm.notify(R.string.service_started, notification);
    }
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i("CloplayerService", "Received start id " + startId + ": " + intent);
        return START_STICKY; // run until explicitly stopped.
    }

    public static boolean isRunning()
    {
        return isRunning;
    }


    private void onTimerTick() {
        Log.i("TimerTick", "Timer doing work." + counter);
        try {
            counter += incrementby;
            sendMessageToUI(counter);

        } catch (Throwable t) { //you should always ultimately catch all exceptions in timer tasks.
            Log.e("TimerTick", "Timer Tick Failed.", t);            
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (timer != null) {timer.cancel();}
        counter=0;
        nm.cancel(R.string.service_started); // Cancel the persistent notification.
        Log.i("CloplayerService", "Service Stopped.");
        isRunning = false;
    }*/
}