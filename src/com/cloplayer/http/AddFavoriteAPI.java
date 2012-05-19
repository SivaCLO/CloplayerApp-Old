package com.cloplayer.http;

public abstract class AddFavoriteAPI extends CloplayerAPIClient {

	public AddFavoriteAPI(String userId, String channelId) {
		super.path = "/api/users/" + userId + "/favorite?channelId=" + channelId; 
	}
	
}
