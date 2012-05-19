package com.cloreader.story;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

import android.util.Log;

import com.cloreader.Session;
import com.cloreader.utils.ServerConstants;

public class StoryManager {

	private Story[] nextStories;
	private List<Story> readStories = new LinkedList<Story>();
	private int storyCapacity = 5;
	private ReentrantLock lock = new ReentrantLock();
	private int readStoryPointer = readStories.size() - 1;
	private Story nextBackupStory = null;

	public StoryManager(int storyCapacity) {
		super();
		this.storyCapacity = storyCapacity;
		nextStories = new Story[storyCapacity];
	}

	public Story getCurrentStory() {
		if(nextStories != null)
			return nextStories[0];
		return null;
	}

	public void fillQueue() {

		int location = 0;
		while (location < storyCapacity) {
			addNewStory(location);
			location++;
		}

		Thread t = new Thread(new Runnable() {
			public void run() {
				int location = 0;
				while (location < storyCapacity) {
					Story story = nextStories[location];
					loadStory(story);
					location++;
				}
			}
		});
		t.start();
	}

	public Story addNewStory(int location) {
		final Story newStory;
		if (nextBackupStory == null) {
			newStory = new Story(location);
		} else {
			newStory = nextBackupStory;
			nextBackupStory.setLocation(location);
			nextBackupStory = null;
		}
		nextStories[location] = newStory;
		Session.mainActivity.showStory(newStory);
		return newStory;
	}

	public void loadStory(final Story newStory) {
		if (!newStory.isLoaded())
			newStory.load();
		Session.mainActivity.showStory(newStory);
	}

	public void promoteStory(int location, int newLocation) {

		// Discarding existing story
		if (newLocation >= location)
			return;

		boolean result = lock.tryLock();

		if (!result)
			return;

		Story oldStory = nextStories[newLocation];

		if (location >= storyCapacity) {
			Log.e(ServerConstants.TAG,
					"promoteStory : lastLocation : location : " + location
							+ " : newLocation : " + newLocation);
			final Story newStory = addNewStory(newLocation);
			Thread t = new Thread(new Runnable() {
				public void run() {
					loadStory(newStory);
				}
			});
			t.start();
		} else {
			Log.e(ServerConstants.TAG,
					"promoteStory : otherLocations : location : " + location
							+ " : newLocation : " + newLocation);
			Story story = nextStories[location];
			nextStories[newLocation] = story;
			story.setLocation(newLocation);
			Session.mainActivity.showStory(story);
			promoteStory(location + 1, location);
		}

		if (newLocation == 0 && !oldStory.isRead()) {
			readStories.add(readStories.size(), oldStory);
			oldStory.markRead();
		}

		readStoryPointer = readStories.size() - 1;

		lock.unlock();
	}

	public void demoteStory(int location, int newLocation) {

		// Discarding existing story
		if (location >= newLocation || location < 0)
			return;

		boolean result = lock.tryLock();

		if (!result)
			return;

		if (newLocation >= storyCapacity) {
			nextBackupStory = nextStories[location];
			demoteStory(location - 1, location);
		} else {
			Log.e(ServerConstants.TAG,
					"demoteStory : allLocations : location : " + location
							+ " : newLocation : " + newLocation);
			Story story = nextStories[location];
			nextStories[newLocation] = story;
			story.setLocation(newLocation);
			Session.mainActivity.showStory(story);
			demoteStory(location - 1, location);
		}

		lock.unlock();
	}

	public void fetchReadStory() {
		// Discarding existing story
		if (readStoryPointer < 0) {
			return;
		}

		boolean result = lock.tryLock();

		if (!result)
			return;

		Story story = readStories.get(readStoryPointer);
		story.setLocation(0);
		Story oldStory = nextStories[0];
		if (!oldStory.isRead())
			demoteStory(nextStories.length - 1, nextStories.length);
		nextStories[0] = story;
		Session.mainActivity.showStory(story);
		readStoryPointer--;

		lock.unlock();
	}
}
