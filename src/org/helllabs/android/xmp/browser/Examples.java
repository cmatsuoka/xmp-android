package org.helllabs.android.xmp.browser;

import java.io.File;

import android.content.Context;
import android.content.res.AssetManager;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.FileOutputStream;


public class Examples {
	private final Context context;
	
	public Examples(final Context context) {
		this.context = context;
	}
	
	public int install(final String path, final boolean examples) {
		final File dir = new File(path);
		
		if (dir.isDirectory()) {
			return 0;
		}

		if (!dir.mkdirs()) {
			return -1;
		}
		
		final AssetManager am = context.getResources().getAssets();
		String assets[];
		
		try {
			assets = am.list("mod");
			
			if (!examples || assets == null) {
				return 0;
			}
		
			for (int i = 0; i < assets.length; i++) {
				copyAsset(am.open("mod/" + assets[i]), path + "/" + assets[i]);
			}
		} catch (IOException e) {
			return -1;
		}
		
		return 0;
	}

	
	private int copyAsset(final InputStream in, final String dst) {
		final byte[] buf = new byte[1024];
		int len;
		
		try{			
		      final OutputStream out = new FileOutputStream(new File(dst));
		      while ((len = in.read(buf)) > 0) {
		    	  out.write(buf, 0, len);
		      }
		      in.close();
		      out.close();
		} catch (FileNotFoundException e) {
			return -1;
		} catch (IOException e) {
			return -1;
		}
		
		return 0;
	}
}
