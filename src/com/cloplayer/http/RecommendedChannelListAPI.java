package com.cloplayer.http;

public abstract class RecommendedChannelListAPI extends CloplayerAPIClient {

	public RecommendedChannelListAPI() {
		super.path = "/api/channels/list/recommended"; 
	}
	
}
