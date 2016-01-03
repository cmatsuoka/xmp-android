package org.helllabs.android.xmp.browser;

import java.io.File;

import android.content.Context;
import android.content.res.AssetManager;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.FileOutputStream;


public final class Examples {
	
	private Examples() {
		
	}
	
	public static int install(final Context context, final String path, final boolean examples) {
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
		
			for (final String a : assets) {
				copyAsset(am.open("mod/" + a), path + "/" + a);
			}
		} catch (IOException e) {
			return -1;
		}
		
		return 0;
	}

	private static int copyAsset(final InputStream in, final String dst) {
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
