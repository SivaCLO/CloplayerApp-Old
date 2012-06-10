package com.cloplayer;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;


public class PlayItem implements Serializable {

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
	
	/** Read the object from Base64 string. */
	public static PlayItem fromString(String s) throws IOException, ClassNotFoundException {
		byte[] data = s.getBytes();
		ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(data));
		Object o = ois.readObject();
		ois.close();
		return (PlayItem)o;
	}

	/** Write the object to a Base64 string. */
	public static String toString(Serializable o) throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ObjectOutputStream oos = new ObjectOutputStream(baos);
		oos.writeObject(o);
		oos.close();
		return new String(baos.toByteArray());
	}

}
