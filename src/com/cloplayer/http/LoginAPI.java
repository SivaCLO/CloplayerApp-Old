package com.cloplayer.http;

public abstract class LoginAPI extends CloplayerAPIClient {

	public LoginAPI(String email, String password) {
		super.path = "/api/login?email=" + email + "&password=" + password; 
	}
	
}
