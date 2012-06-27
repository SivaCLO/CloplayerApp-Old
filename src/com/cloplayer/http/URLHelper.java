package com.cloplayer.http;

public class URLHelper {

	public static final String BASE_URL = "http://api.cloplayer.com/";
	public static final String HOME_URL = "http://cloplayer.com/";

	public static String home() {
		return HOME_URL;
	}

	public static String api_home() {
		return BASE_URL;
	}

	public static String login(String email, String password) {
		return BASE_URL + "api/login?email=" + email + "&password=" + password;
	}

	public static String list(String userId) {
		return BASE_URL + "api/list?userId=" + userId;
	}

	public static String add(String userId, String url) {
		return BASE_URL + "api/add?userId=" + userId + "&url=" + url;
	}

}
