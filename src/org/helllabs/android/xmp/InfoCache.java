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
	
	private static boolean myTestModule(String filename, ModInfo info) {
		boolean ret = xmp.testModule(filename, info);
		if (info != null) {
			info.name = info.name.trim();
			if (info.name.isEmpty()) {
				info.name = (new File(filename)).getName();
			}
		}
		return ret;
	}
	
	public static boolean testModule(String filename, ModInfo info) {
		final File file = new File(filename);
		final File cacheFile = new File(Settings.cacheDir, filename + ".cache");
		final File skipFile = new File(Settings.cacheDir, filename + ".skip");

		if (!Settings.cacheDir.isDirectory()) {
			if (Settings.cacheDir.mkdirs() == false) {
				// Can't use cache
				return myTestModule(filename, info);
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
			
			Boolean isMod = myTestModule(filename, info);
			if (!isMod) {
				File dir = skipFile.getParentFile();
				if (!dir.isDirectory())
					dir.mkdirs();
				skipFile.createNewFile();
			}
			
			return isMod;
		} catch (IOException e) {
			return myTestModule(filename, info);
		}	
	}
	
	public static ModInfo getModInfo(String filename) {
		final File file = new File(filename);
		final File cacheFile = new File(Settings.cacheDir, filename + ".cache");

		if (!Settings.cacheDir.isDirectory()) {
			if (Settings.cacheDir.mkdirs() == false) {
				ModInfo info = new ModInfo();
				// Can't use cache
				if (myTestModule(filename, info)) {					
					return info;
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
					in.readLine();		// skip filename
					mi.type = in.readLine();
					
					in.close();
					return mi;
				}
				in.close();
			}

			ModInfo info = new ModInfo();
			
			if ((myTestModule(filename, info))) {
				String[] lines = {
					Long.toString(file.length()),
					info.name,
					filename,
					info.type
				};
				
				File dir = cacheFile.getParentFile();
				if (!dir.isDirectory())
					dir.mkdirs();			
				cacheFile.createNewFile();
				FileUtils.writeToFile(cacheFile, lines);

				return info;
			}
			
			return null;
		} catch (IOException e) {
			ModInfo info = new ModInfo();
			if (myTestModule(filename, info)) {
				return info;
			} else {
				return null;
			}
		}
	}
}
