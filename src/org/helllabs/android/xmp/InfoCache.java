package org.helllabs.android.xmp;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;


public class InfoCache {
	static Xmp xmp = new Xmp();
	
	public static boolean delete(String filename) {
		final File file = new File(filename);
		final File cacheFile = new File(Settings.cacheDir, filename + ".cache");
		
		if (cacheFile.isFile())
			cacheFile.delete();
		
		return file.delete();
	}
	
	public static boolean fileExists(String filename) {
		final File file = new File(filename);
		final File cacheFile = new File(Settings.cacheDir, filename + ".cache");
		
		if (file.isFile())
			return true;
		
		if (cacheFile.isFile())
			cacheFile.delete();
		
		return false;
	}
	
	public static boolean testModule(String filename, Xmp.TestInfo info) {
		final File file = new File(filename);
		final File cacheFile = new File(Settings.cacheDir, filename + ".cache");
		final File skipFile = new File(Settings.cacheDir, filename + ".skip");

		if (!Settings.cacheDir.isDirectory()) {
			if (Settings.cacheDir.mkdirs() == false) {
				// Can't use cache
				return xmp.testModule(filename, info);
			}
		}
		
		try {
			if (skipFile.isFile())
				return false;
			
			// If cache file exists and size matches, file is mod
			if (cacheFile.isFile()) {
				int size = Integer.parseInt(FileUtils.readFromFile(cacheFile));
				if (size == file.length())
					return true;
			}
			
			Boolean isMod = xmp.testModule(filename, info);
			if (!isMod) {
				File dir = skipFile.getParentFile();
				if (!dir.isDirectory())
					dir.mkdirs();
				skipFile.createNewFile();
			}
			
			return isMod;
		} catch (IOException e) {
			return xmp.testModule(filename, info);
		}	
	}
	
	public static ModInfo getModInfo(String filename) {
		final File file = new File(filename);
		final File cacheFile = new File(Settings.cacheDir, filename + ".cache");

		if (!Settings.cacheDir.isDirectory()) {
			if (Settings.cacheDir.mkdirs() == false) {
				Xmp.TestInfo info = xmp.new TestInfo();
				// Can't use cache
				if (xmp.testModule(filename, info)) {
					return info.toModInfo(filename);
				} else {
					return null;
				}
			}
		}

		try {
			// If cache file exists and size matches, file is mod
			if (cacheFile.isFile()) {
				BufferedReader in = new BufferedReader(new FileReader(cacheFile), 512);			
				int size = Integer.parseInt(in.readLine());
				if (size == file.length()) {
					ModInfo mi = new ModInfo();
					
					mi.name = in.readLine();
					mi.filename = in.readLine();
					mi.type = in.readLine();
					
					in.close();
					return mi;
				}
				in.close();
			}

			Xmp.TestInfo info = xmp.new TestInfo();
			
			if ((xmp.testModule(filename, info))) {

				
				String[] lines = {
					Long.toString(file.length()),
					info.name,
					filename,
					info.type
					/*Integer.toString(mi.chn),
					Integer.toString(mi.pat),
					Integer.toString(mi.ins),
					Integer.toString(mi.trk),
					Integer.toString(mi.smp),
					Integer.toString(mi.len),
					Integer.toString(mi.bpm),
					Integer.toString(mi.tpo),
					Integer.toString(mi.time)*/
				};
				
				File dir = cacheFile.getParentFile();
				if (!dir.isDirectory())
					dir.mkdirs();			
				cacheFile.createNewFile();
				FileUtils.writeToFile(cacheFile, lines);

				return info.toModInfo(filename);
			}
			
			return null;
		} catch (IOException e) {
			Xmp.TestInfo info = xmp.new TestInfo();
			if (xmp.testModule(filename, info)) {
				return info.toModInfo(filename);
			} else {
				return null;
			}
		}
	}
}
