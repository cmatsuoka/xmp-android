package org.helllabs.android.xmp;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;


public class InfoCache {

	public static boolean delete(String filename) {
		final File file = new File(filename);
		final File cacheFile = new File(Settings.cacheDir, filename + ".cache");
		final File skipFile = new File(Settings.cacheDir, filename + ".skip");

		if (cacheFile.isFile())
			cacheFile.delete();
		
		if (skipFile.isFile())
			skipFile.delete();

		return file.delete();
	}

	public static boolean fileExists(String filename) {
		final File file = new File(filename);
		final File cacheFile = new File(Settings.cacheDir, filename + ".cache");
		final File skipFile = new File(Settings.cacheDir, filename + ".skip");

		if (file.isFile())
			return true;

		if (cacheFile.isFile())
			cacheFile.delete();
		
		if (skipFile.isFile())
			skipFile.delete();

		return false;
	}
	
	public static boolean testModuleForceIfInvalid(String filename) {
		final File skipFile = new File(Settings.cacheDir, filename + ".skip");
		
		if (skipFile.isFile())
			skipFile.delete();
		
		return testModule(filename);
	}
	
	public static boolean testModule(String filename) {
		ModInfo modInfo = new ModInfo();
		return testModule(filename, modInfo);
	}

	public static boolean testModule(String filename, ModInfo info) {
		final File file = new File(filename);
		final File cacheFile = new File(Settings.cacheDir, filename + ".cache");
		final File skipFile = new File(Settings.cacheDir, filename + ".skip");

		if (!Settings.cacheDir.isDirectory()) {
			if (Settings.cacheDir.mkdirs() == false) {
				// Can't use cache
				return Xmp.testModule(filename, info);
			}
		}

		try {
			// If cache file exists and size matches, file is mod
			if (cacheFile.isFile()) {
				if (skipFile.isFile())
					skipFile.delete();
				
				BufferedReader in = new BufferedReader(new FileReader(cacheFile), 512);
				int size = Integer.parseInt(in.readLine());
				if (size == file.length()) {
					info.name = in.readLine();
					in.readLine();				/* skip filename */
					info.type = in.readLine();
					in.close();
					return true;
				}
				in.close();
				
				cacheFile.delete();		// Invalid or outdated cache file
			}
			
			if (skipFile.isFile())
				return false;

			Boolean isMod = Xmp.testModule(filename, info);
			if (!isMod) {
				File dir = skipFile.getParentFile();
				if (!dir.isDirectory())
					dir.mkdirs();
				skipFile.createNewFile();
			} else {
				final String[] lines = {
						Long.toString(file.length()),
						info.name,
						filename,
						info.type,
				};

				File dir = cacheFile.getParentFile();
				if (!dir.isDirectory())
					dir.mkdirs();			
				cacheFile.createNewFile();
				FileUtils.writeToFile(cacheFile, lines);
			}

			return isMod;
		} catch (IOException e) {
			return Xmp.testModule(filename, info);
		}	
	}
};
