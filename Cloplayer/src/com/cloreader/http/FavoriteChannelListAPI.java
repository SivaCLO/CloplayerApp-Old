package com.cloreader.http;

public abstract class FavoriteChannelListAPI extends CloreaderAPIClient {

	public FavoriteChannelListAPI(String userId) {
		super.path = "/api/channels/list/favorites?userId=" + userId; 
	}
	
}
