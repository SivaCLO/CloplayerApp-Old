package com.cloplayer.http;

public abstract class UserChannelListAPI extends CloplayerAPIClient {

	public UserChannelListAPI(String userId) {
		super.path = "/api/channels/list/user?userId=" + userId; 
	}
	
}
