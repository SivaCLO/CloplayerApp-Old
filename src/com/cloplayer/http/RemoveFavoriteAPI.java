package com.cloplayer.http;

public abstract class RemoveFavoriteAPI extends CloplayerAPIClient {

	public RemoveFavoriteAPI(String userId, String channelId) {
		super.path = "/api/users/" + userId + "/unfavorite?channelId=" + channelId; 
	}
	
}
