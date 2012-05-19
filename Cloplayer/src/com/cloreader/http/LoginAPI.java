package com.cloreader.http;

public abstract class LoginAPI extends CloreaderAPIClient {

	public LoginAPI(String email, String password) {
		super.path = "/api/login?email=" + email + "&password=" + password; 
	}
	
}
