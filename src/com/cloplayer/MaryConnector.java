package com.cloplayer;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import marytts.client.MaryClient;
import marytts.client.http.Address;

public class MaryConnector {

	public static byte[] getAudio(String text) {

		byte[] byteArray = null;

		try {
			String serverHost = System.getProperty("server.host", "cling.dfki.uni-sb.de");
			int serverPort = Integer.getInteger("server.port", 59125).intValue();
			MaryClient mary;
			mary = MaryClient.getMaryClient(new Address(serverHost, serverPort));

			String locale = "en-US"; // or US English (en-US), Telugu (te),
										// Turkish (tr), ...
			String inputType = "TEXT";
			String outputType = "AUDIO";
			String audioType = "WAVE";

			/*
			 * String effects = "Volume+"; effects += "TractScaler+"; effects +=
			 * "F0Scale+"; effects += "F0Add+"; effects += "Rate+"; effects +=
			 * "Robot+"; effects += "Whisper+"; effects += "Stadium+"; effects
			 * += "Chorus+"; effects += "FIRFilter+"; effects += "JetPilot";
			 */

			//String effects = "";
			//effects += "Volume(amount:5.0;)+";
			// effects += "TractScaler(amount:1.5;)+";
			// effects += "F0Scale(f0Scale:2.0;)+";
			// effects += "F0Add(f0Add:50.0;)+";
			// effects += "Rate(durScale:1.5;)+";
			// effects += "Robot(amount:100.0;)+";
			// effects += "Whisper(amount:100.0;)+";
			// effects += "Stadium(amount:100.0)+";
			// effects +=
			// "Chorus(delay1:466;amp1:0.54;delay2:600;amp2:-0.10;delay3:250;amp3:0.30)+";
			// effects += "FIRFilter(type:3;fc1:500.0;fc2:2000.0)+";
			// effects += "JetPilot";

			String effects = null;
			String defaultVoiceName = "cmu-rms-hsmm";

			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			mary.process(text, inputType, outputType, locale, audioType, defaultVoiceName, null, effects, null, baos);

			byteArray = baos.toByteArray();

			File dstFile = new File("/sdcard/download/dst.wav");
			DataOutputStream outFile = new DataOutputStream(new FileOutputStream(dstFile));
			outFile.write(byteArray, 0, byteArray.length);
			outFile.close();

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return byteArray;
	}

}