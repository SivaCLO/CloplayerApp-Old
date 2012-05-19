package com.cloreader.http;

public abstract class AddFavoriteAPI extends CloreaderAPIClient {

	public AddFavoriteAPI(String userId, String channelId) {
		super.path = "/api/users/" + userId + "/favorite?channelId=" + channelId; 
	}
	
}
