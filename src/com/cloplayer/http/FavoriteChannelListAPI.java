package com.cloplayer.http;

public abstract class FavoriteChannelListAPI extends CloplayerAPIClient {

	public FavoriteChannelListAPI(String userId) {
		super.path = "/api/channels/list/favorites?userId=" + userId; 
	}
	
}
