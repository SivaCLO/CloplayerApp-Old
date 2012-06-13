package com.cloplayer.sqlite;

import java.util.concurrent.ConcurrentHashMap;

public class StoryFactory {

	ConcurrentHashMap<Long, Story> storyMap = new ConcurrentHashMap<Long, Story>();
	private static StoryFactory instance = new StoryFactory();

	public static StoryFactory getInstance() {
		return instance;
	}

	public Story getStory(long id) {
		Story story = storyMap.get(new Long(id));
		if (story == null) {
			story = new Story();
			storyMap.put(new Long(id), story);
		}

		return story;
	}

}
