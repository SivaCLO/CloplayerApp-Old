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
import org.json.JSONException;
import org.json.JSONObject;

import android.os.AsyncTask;
import android.util.Log;

public abstract class CloplayerAPIClient extends
		AsyncTask<String, String, String> {

	protected String host = "http://api.cloplayer.com";
	protected String path;

	@Override
	protected String doInBackground(String... data) {
		HttpClient httpclient = new DefaultHttpClient();
		HttpResponse response;
		String responseString = null;
		try {
			response = httpclient.execute(new HttpGet(host + path));
			Log.e("CloplayerAPIClient", "Path = " + host+ path);
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
		return responseString;
	}

	@Override
	protected void onPostExecute(String result) {
		super.onPostExecute(result);
		try {
			if (result != null)
				onSuccessResponse(new JSONObject(result));
		} catch (JSONException e) {
			e.printStackTrace();
			onErrorResponse(e);
		}
	}

	public abstract void onSuccessResponse(JSONObject response);

	public abstract void onErrorResponse(Exception e);

}
