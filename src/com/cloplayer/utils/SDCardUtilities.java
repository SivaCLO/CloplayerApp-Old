package com.cloplayer.utils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

import android.content.Context;

import com.cloplayer.R;

public class SDCardUtilities {

	private static final String TAG = "SDCardUtilities";

	public static void copySphinxFilesToSDCard(Context ctx, String rootPath) {
		copyFileToSDCard(ctx, R.raw.hub4dic, rootPath
				+ "/lm/en_US", "hub4.5000.dic");
		copyFileToSDCard(ctx, R.raw.hub4dmp, rootPath
				+ "/lm/en_US", "hub4.5000.DMP");
		
		copyFileToSDCard(ctx, R.raw.feat, rootPath
				+ "/hmm/en_US/hub4wsj_sc_8k", "feat.params");
		copyFileToSDCard(ctx, R.raw.mdef, rootPath
				+ "/hmm/en_US/hub4wsj_sc_8k", "mdef");
		copyFileToSDCard(ctx, R.raw.means, rootPath
				+ "/hmm/en_US/hub4wsj_sc_8k", "means");
		copyFileToSDCard(ctx, R.raw.noisedict, rootPath
				+ "/hmm/en_US/hub4wsj_sc_8k", "noisedict");
		copyFileToSDCard(ctx, R.raw.sendump, rootPath
				+ "/hmm/en_US/hub4wsj_sc_8k", "sendump");
		copyFileToSDCard(ctx, R.raw.transition_matrices, rootPath
				+ "/hmm/en_US/hub4wsj_sc_8k", "transition_matrices");
		copyFileToSDCard(ctx, R.raw.variances, rootPath
				+ "/hmm/en_US/hub4wsj_sc_8k", "variances");		
		
	}

	private static void copyFileToSDCard(Context ctx, int resId, String filePath, String fileName) {
		
		InputStream inputStream = ctx.getResources().openRawResource(resId);
		OutputStream out = null;
		
		try {
			File dir = new File(filePath);
			dir.mkdirs();
			
			File file = new File(filePath + "/" + fileName);
			file.createNewFile();
			
			out = new FileOutputStream(filePath + "/" + fileName);

			int read = 0;
			byte[] bytes = new byte[1024];

			while ((read = inputStream.read(bytes)) != -1) {
				out.write(bytes, 0, read);
			}

		} catch (Exception e) {
			e.printStackTrace();
			
		} finally {
			try {
			inputStream.close();
			out.flush();
			out.close();
			} catch (Exception e){}
		}
	}

}
