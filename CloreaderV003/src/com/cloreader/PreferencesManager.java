/*
 * Copyright (C) 2007 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.cloreader;

import java.util.Iterator;
import java.util.Map;
import java.util.Random;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Environment;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.util.Log;
import android.widget.Toast;

import com.cloreader.http.HTTPHelper;
import com.cloreader.utils.SDCardUtilities;
import com.cloreader.utils.ServerConstants;

public class PreferencesManager extends PreferenceActivity implements
		SharedPreferences.OnSharedPreferenceChangeListener,
		Preference.OnPreferenceClickListener {

	private int userId;
	private JSONObject allTopics;
	public String sphinxDir = Environment.getExternalStorageDirectory()
			.getAbsolutePath() + "/Android/data/edu.cmu.pocketsphinx";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		Bundle extras = getIntent().getExtras();
		boolean dontIgnore = extras != null
				&& extras.getBoolean("dontIgnore", false);

		SharedPreferences settings = getSharedPreferences(ServerConstants.CLOREADER_CHANNEL_PREFS, 0);
		boolean isFirstTime = settings.getBoolean("isFirstTime", true);
		if (!isFirstTime && !dontIgnore) {
			Intent resultIntent = new Intent();
			setResult(Activity.RESULT_OK, resultIntent);
			finish();
			return;
		}

		Log.e(ServerConstants.TAG, sphinxDir);

		if (isFirstTime) {
			Log.e(ServerConstants.TAG, sphinxDir);
			SDCardUtilities.copySphinxFilesToSDCard(this, sphinxDir);

			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setMessage(
					"Hi, My name is Cloreader. I am your news reading assistant. Please tell me your email address and the topics you are interested in.")
					.setPositiveButton("Ok",
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int id) {
									dialog.cancel();
								}
							});
			AlertDialog alert = builder.create();
			alert.show();
		}

		setPreferenceScreen(createPreferenceHierarchy());
	}

	private PreferenceScreen createPreferenceHierarchy() {

		SharedPreferences sharedPrefs = PreferenceManager
				.getDefaultSharedPreferences(this);
		String[] topicOptions = getResources().getStringArray(
				R.array.topicOptions);

		PreferenceScreen root = getPreferenceManager().createPreferenceScreen(
				this);

		PreferenceCategory personalCat = new PreferenceCategory(this);
		personalCat.setTitle("Personal Information");
		root.addPreference(personalCat);

		EditTextPreference emailPref = new EditTextPreference(this);
		emailPref.setKey("email");
		emailPref.setTitle("Email Address");
		emailPref.setSummary(sharedPrefs.getString("email", ""));
		// emailPref.setSummary("Save");
		personalCat.addPreference(emailPref);

		PreferenceCategory dialogBasedPrefCat = new PreferenceCategory(this);
		dialogBasedPrefCat.setTitle("What are you interested in ?");
		root.addPreference(dialogBasedPrefCat);

		allTopics = all_topics();
		Iterator topics = allTopics.keys();

		while (topics.hasNext()) {
			String topicName = (String) topics.next();
			ListPreference listPref = new ListPreference(this);
			listPref.setEntries(R.array.topicOptions);
			listPref.setEntryValues(R.array.topicOptionValues);
			listPref.setDialogTitle("Topic Options");
			listPref.setKey(topicName);
			listPref.setTitle(topicName);
			listPref.setSummary(topicOptions[Integer.parseInt(sharedPrefs
					.getString(topicName, "0"))]);
			dialogBasedPrefCat.addPreference(listPref);
		}

		PreferenceCategory saveCat = new PreferenceCategory(this);
		saveCat.setTitle("Save");
		root.addPreference(saveCat);

		PreferenceScreen screenPref = getPreferenceManager()
				.createPreferenceScreen(this);
		screenPref.setKey("Save");
		screenPref.setTitle("Save");
		screenPref.setSummary("Save");
		screenPref.setOnPreferenceClickListener(this);
		saveCat.addPreference(screenPref);

		return root;
	}

	public void onResume() {
		super.onResume();
		getPreferenceScreen().getSharedPreferences()
				.registerOnSharedPreferenceChangeListener(this);
	}

	public JSONObject all_topics() {
		try {
			return HTTPHelper
					.getUrlContent("http://api.cloreader.com/api/topics/all");
		} catch (HTTPHelper.ApiException e) {
			Log.e(ServerConstants.TAG, "Problem making HTTP call request", e);
		}
		return null;
	}

	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
			String key) {
		if (key.equals("Save")) {
		} else if (key.equals("email")) {
			EditTextPreference emailPref = (EditTextPreference) getPreferenceScreen()
					.findPreference(key);
			emailPref.setSummary(sharedPreferences.getString("email", ""));
		} else {
			String value = sharedPreferences.getString(key, "");
			ListPreference pref = (ListPreference) getPreferenceScreen()
					.findPreference(key);
			String[] topicOptions = getResources().getStringArray(
					R.array.topicOptions);
			pref.setSummary(topicOptions[Integer.parseInt(value)]);
		}
	}

	public boolean onPreferenceClick(Preference preference) {
		if (preference.getKey().equals("Save")) {
			JSONArray preferenceJSON = new JSONArray();
			SharedPreferences sharedPrefs = PreferenceManager
					.getDefaultSharedPreferences(this);
			Map<String, ?> prefs = (Map<String, ?>) sharedPrefs.getAll();
			boolean atLeastOne = false;
			for (String pref : prefs.keySet()) {
				String value = sharedPrefs.getString(pref, "");
				Log.e(ServerConstants.TAG, pref + " : " + value);

				if (!pref.equals("email") && Integer.parseInt(value) > 0) {
					try {
						atLeastOne = true;
						JSONObject tempJSON = new JSONObject();
						tempJSON.put("volume", Integer.parseInt(value));
						tempJSON.put("muted", false);
						tempJSON.put("id", allTopics.getString(pref));
						preferenceJSON.put(tempJSON);
					} catch (JSONException e) {

					}
				}
			}
			
			JSONObject responseJSON = new JSONObject();
			
			try {
				responseJSON.put("topics", preferenceJSON);
			} catch (JSONException e) {
				e.printStackTrace();
			}

			boolean emailPreference = false;
			String email = sharedPrefs.getString("email", "");
			if (email.contains("@") && email.length() > 5
					&& email.contains(".")) {
				emailPreference = true;
			}

			if (!atLeastOne) {
				Toast.makeText(this, "Please select atleast one topic",
						Toast.LENGTH_LONG).show();
			} else if (!emailPreference) {
				Toast.makeText(this, "Please enter a valid email address",
						Toast.LENGTH_LONG).show();
			} else {
				SharedPreferences settings = getSharedPreferences(ServerConstants.CLOREADER_CHANNEL_PREFS, 0);
				SharedPreferences.Editor editor = settings.edit();
				editor.putBoolean("isFirstTime", false);
				editor.putBoolean("isPreferenceChanged", true);
				editor.putString("email", email);
				editor.putInt(getResources().getString(R.string.userId), userId);
				editor.putString(
						getResources().getString(
								R.string.preferenceResultString),
						responseJSON.toString());
				editor.commit();

				Intent resultIntent = new Intent();
				setResult(Activity.RESULT_OK, resultIntent);
				finish();
			}
		}
		return true;
	}
}
