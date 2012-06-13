package com.cloplayer.http;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import android.os.AsyncTask;
import android.util.Log;

public abstract class SyncHTTPClient {

	protected String url;

	public SyncHTTPClient(String url) {
		this.url = url;
	}

	public void execute() {
		HttpClient httpclient = new DefaultHttpClient();
		HttpResponse response;
		String responseString = null;
		try {
			response = httpclient.execute(new HttpGet(url));
			Log.e("HTTPClient", "Path = " + url);
			StatusLine statusLine = response.getStatusLine();
			if (statusLine.getStatusCode() == HttpStatus.SC_OK) {
				ByteArrayOutputStream out = new ByteArrayOutputStream();
				response.getEntity().writeTo(out);
				out.close();
				responseString = out.toString();
			} else {
				// Closes the connection.
				response.getEntity().getContent().close();
				throw new IOException(statusLine.getReasonPhrase());
			}
		} catch (ClientProtocolException e) {
			e.printStackTrace();
			onErrorResponse(e);
		} catch (IOException e) {
			e.printStackTrace();
			onErrorResponse(e);
		}
		
		onSuccessResponse(responseString);
	}

	public abstract void onSuccessResponse(String response);

	public abstract void onErrorResponse(Exception e);

}
