package com.cloplayer;


public class PlayItem {

	byte[] byteArray;
	String text;

	public PlayItem(String text, byte[] byteArray) {
		this.text = text;
		this.byteArray = byteArray;
	}

	public byte[] getByteArray() {
		return byteArray;
	}

	public void setByteArray(byte[] byteArray) {
		this.byteArray = byteArray;
	}

	public String getText() {
		return text;
	}

	public void setText(String text) {
		this.text = text;
	}
}
