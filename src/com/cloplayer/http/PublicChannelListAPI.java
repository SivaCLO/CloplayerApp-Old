package com.cloplayer.http;

public abstract class PublicChannelListAPI extends CloplayerAPIClient {

	public PublicChannelListAPI(String userId) {
		super.path = "/api/channels/list/public?userId=" + userId; 
	}
}
