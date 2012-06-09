package com.cloplayer.http;

public abstract class MarkAPI extends CloplayerAPIClient {

	public MarkAPI(String userId, String storyId) {
		super.path = "/api/stories/" + storyId + "/mark?userId=" + userId;
	}
}
