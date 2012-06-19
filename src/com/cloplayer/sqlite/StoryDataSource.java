package com.cloplayer.sqlite;

import java.util.ArrayList;
import java.util.List;

import com.cloplayer.CloplayerService;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

public class StoryDataSource {

	// Database fields
	private SQLiteDatabase database;
	private MySQLiteHelper dbHelper;
	private String[] allColumns = { MySQLiteHelper.COLUMN_ID, MySQLiteHelper.COLUMN_URL, MySQLiteHelper.COLUMN_DOMAIN, MySQLiteHelper.COLUMN_HEADLINE, MySQLiteHelper.COLUMN_DETAIL, MySQLiteHelper.COLUMN_ITEM_COUNT, MySQLiteHelper.COLUMN_DOWNLOAD_PROGRESS, MySQLiteHelper.COLUMN_PLAY_PROGRESS, MySQLiteHelper.COLUMN_STATE };

	public StoryDataSource(Context context) {
		dbHelper = new MySQLiteHelper(context);
	}

	public void open() throws SQLException {
		database = dbHelper.getWritableDatabase();
	}

	public void close() {
		dbHelper.close();
	}

	public Story addStory(String url) {

		Log.e("CloplayerService", "Adding Article :" + url);

		ContentValues values = new ContentValues();
		values.put(MySQLiteHelper.COLUMN_URL, url);
		values.put(MySQLiteHelper.COLUMN_ITEM_COUNT, 0);
		values.put(MySQLiteHelper.COLUMN_DOWNLOAD_PROGRESS, 0);
		values.put(MySQLiteHelper.COLUMN_PLAY_PROGRESS, 0);
		values.put(MySQLiteHelper.COLUMN_STATE, 0);

		long insertId = database.insert(MySQLiteHelper.TABLE_STORIES, null, values);
		Cursor cursor = database.query(MySQLiteHelper.TABLE_STORIES, allColumns, MySQLiteHelper.COLUMN_ID + " = " + insertId, null, null, null, null);
		cursor.moveToFirst();
		Story newStory = cursorToStory(cursor, StoryFactory.getInstance().getStory(cursor.getLong(0)));
		cursor.close();
		
		CloplayerService.getInstance().sendIntMessageToUI(CloplayerService.MSG_ADD_STORY, (int) newStory.getId());

		return newStory;
	}

	public void updateStory(Story story, ContentValues values) {

		database.update(MySQLiteHelper.TABLE_STORIES, values, MySQLiteHelper.COLUMN_ID + " = " + story.getId(), null);
		Cursor cursor = database.query(MySQLiteHelper.TABLE_STORIES, allColumns, MySQLiteHelper.COLUMN_ID + " = " + story.getId(), null, null, null, null);
		cursor.moveToFirst();
		cursorToStory(cursor, story);
		cursor.close();

		CloplayerService.getInstance().sendIntMessageToUI(CloplayerService.MSG_UPDATE_STORY, (int) story.getId());
	}

	public Story findStory(String url) {
		Cursor cursor = database.query(MySQLiteHelper.TABLE_STORIES, allColumns, MySQLiteHelper.COLUMN_URL + " = \"" + url + "\"", null, null, null, null);
		cursor.moveToFirst();
		if (cursor.getCount() > 0) {
			Story newStory = cursorToStory(cursor, StoryFactory.getInstance().getStory(cursor.getLong(0)));
			cursor.close();
			return newStory;
		} else
			return null;
	}

	public Story getStory(int storyId) {
		Cursor cursor = database.query(MySQLiteHelper.TABLE_STORIES, allColumns, MySQLiteHelper.COLUMN_ID + " = " + storyId, null, null, null, null);
		cursor.moveToFirst();
		Story newStory = cursorToStory(cursor, StoryFactory.getInstance().getStory(cursor.getLong(0)));
		cursor.close();
		return newStory;
	}

	public void deleteStory(Story story) {
		long id = story.getId();
		database.delete(MySQLiteHelper.TABLE_STORIES, MySQLiteHelper.COLUMN_ID + " = " + id, null);
	}

	public List<Story> getAllStories() {
		List<Story> stories = new ArrayList<Story>();

		Cursor cursor = database.query(MySQLiteHelper.TABLE_STORIES, allColumns, null, null, null, null, null);

		cursor.moveToFirst();
		while (!cursor.isAfterLast()) {
			Story story = cursorToStory(cursor, StoryFactory.getInstance().getStory(cursor.getLong(0)));
			stories.add(story);
			cursor.moveToNext();
		}
		// Make sure to close the cursor
		cursor.close();
		return stories;
	}
	
	public List<Story> getUnplayedStories() {
		List<Story> stories = new ArrayList<Story>();

		Cursor cursor = database.query(MySQLiteHelper.TABLE_STORIES, allColumns, MySQLiteHelper.COLUMN_STATE + " < " + Story.STATE_PLAYED, null, null, null, null);

		cursor.moveToFirst();
		while (!cursor.isAfterLast()) {
			Story story = cursorToStory(cursor, StoryFactory.getInstance().getStory(cursor.getLong(0)));
			stories.add(story);
			cursor.moveToNext();
		}
		// Make sure to close the cursor
		cursor.close();
		return stories;
	}
	
	public List<Story> getplayedStories() {
		List<Story> stories = new ArrayList<Story>();

		Cursor cursor = database.query(MySQLiteHelper.TABLE_STORIES, allColumns, MySQLiteHelper.COLUMN_STATE + " = " + Story.STATE_PLAYED, null, null, null, null);

		cursor.moveToFirst();
		while (!cursor.isAfterLast()) {
			Story story = cursorToStory(cursor, StoryFactory.getInstance().getStory(cursor.getLong(0)));
			stories.add(story);
			cursor.moveToNext();
		}
		// Make sure to close the cursor
		cursor.close();
		return stories;
	}

	private Story cursorToStory(Cursor cursor, Story story) {
		story.setId(cursor.getLong(0));
		story.setUrl(cursor.getString(1));
		story.setDomain(cursor.getString(2));
		story.setHeadline(cursor.getString(3));
		story.setDetail(cursor.getString(4));
		story.setItemCount(cursor.getInt(5));
		story.setDownloadProgress(cursor.getInt(6));
		story.setPlayProgress(cursor.getInt(7));
		story.setState(cursor.getInt(8));
		return story;
	}
}