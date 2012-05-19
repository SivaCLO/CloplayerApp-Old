package com.cloreader.http;

public abstract class UserChannelListAPI extends CloreaderAPIClient {

	public UserChannelListAPI(String userId) {
		super.path = "/api/channels/list/user?userId=" + userId; 
	}
	
}
