package com.cloreader.http;

public abstract class RemoveFavoriteAPI extends CloreaderAPIClient {

	public RemoveFavoriteAPI(String userId, String channelId) {
		super.path = "/api/users/" + userId + "/unfavorite?channelId=" + channelId; 
	}
	
}
