package com.cloplayer;

import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

public class ActivitySwipeDetector implements View.OnTouchListener {

	static final String logTag = "ActivitySwipeDetector";
	private HomeActivity activity;
	static final int MIN_DISTANCE = 120;
	private float downX, downY, upX, upY;
	
	public ActivitySwipeDetector(HomeActivity activity) {
		this.activity = activity;
	}

	public void onTopToBottomSwipe() {
		Log.i(logTag, "onTopToBottomSwipe!");
		Session.ttsManager.stopReading(false);
		Session.storyManager.fetchReadStory();
	}

	public void onBottomToTopSwipe() {
		Log.i(logTag, "onBottomToTopSwipe!");
		Session.ttsManager.stopReading(false);
		Session.storyManager.promoteStory(1, 0);
	}
	
	public boolean onTouch(View v, MotionEvent event) {
		switch (event.getAction()) {
		case MotionEvent.ACTION_DOWN: {
			downX = event.getX();
			downY = event.getY();
			return true;
		}
		case MotionEvent.ACTION_UP: {
			upX = event.getX();
			upY = event.getY();

			float deltaX = downX - upX;
			float deltaY = downY - upY;

			if (Math.abs(deltaY) > MIN_DISTANCE) {
				// top or down
				if (deltaY < 0) {
					this.onTopToBottomSwipe();
					return true;
				}
				if (deltaY > 0) {
					this.onBottomToTopSwipe();
					return true;
				}
			} else {
				//this.onNotSwipe();
				return false; // We don't consume the event
			}

			return true;
		}
		}
		return false;
	}

}