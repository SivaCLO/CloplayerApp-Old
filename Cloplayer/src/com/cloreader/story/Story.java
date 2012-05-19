package com.cloreader.story;

import org.json.JSONException;
import org.json.JSONObject;

import com.cloreader.Session;

public class Story {

	private String storyId;
	private String topicName;
	private int location;
	private String headLine;
	private String detailText;
	private String source;
	private String date;
	private boolean isLoaded;
	private boolean isRead = false;
	private boolean isNoStories = false;

	private String url;

	public Story(int location) {
		this.location = location;
		this.isLoaded = false;
	}

	public void load() {

		JSONObject storyJSON = Session.apiClient.popStory();

		try {
			this.storyId = storyJSON.getString("storyId");			
			if (!storyId.equals("0")) {
				this.topicName = storyJSON.getString("topicName");
				this.headLine = storyJSON.getString("headline");
				this.detailText = storyJSON.getString("detail");
				this.source = storyJSON.getString("sourceName");
				this.date = storyJSON.getString("timestamp");
				this.url = storyJSON.getString("url");
				this.isLoaded = true;
			} else { 
				this.isNoStories = true;
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}

	public void setLocation(int location) {
		this.location = location;
	}
	
	public void markRead() {
		this.isRead= true;
	}
	
	public boolean isRead() {
		return isRead;
	}

	public boolean isLoaded() {
		return isLoaded;
	}

	public boolean isNoStories() {
		return isNoStories;
	}

	public String getStoryId() {
		return storyId;
	}

	public String getTopicId() {
		return topicName;
	}

	public int getLocation() {
		return location;
	}

	public String getHeadLine() {
		return headLine;
	}

	public String getDetailText() {
		return detailText;
	}

	public String getSource() {
		return source;
	}

	public String getDate() {
		return date;
	}

	public String getUrl() {
		return url;
	}

}
