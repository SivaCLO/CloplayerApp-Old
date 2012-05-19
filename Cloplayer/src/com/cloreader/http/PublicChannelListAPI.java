package com.cloreader.http;

public abstract class PublicChannelListAPI extends CloreaderAPIClient {

	public PublicChannelListAPI(String userId) {
		super.path = "/api/channels/list/public?userId=" + userId; 
	}
}
