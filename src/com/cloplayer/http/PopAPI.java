package com.cloplayer.http;

public abstract class PopAPI extends CloplayerAPIClient {

	public PopAPI(String userId, String sourceId) {
		super.path = "/api/stories/pop?userId=" + userId + "&sourceId=" + sourceId; 
	}
	
}
