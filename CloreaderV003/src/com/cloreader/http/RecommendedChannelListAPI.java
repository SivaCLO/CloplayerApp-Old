package com.cloreader.http;

public abstract class RecommendedChannelListAPI extends CloreaderAPIClient {

	public RecommendedChannelListAPI() {
		super.path = "/api/channels/list/recommended"; 
	}
	
}
