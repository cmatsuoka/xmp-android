package org.helllabs.android.xmp.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import org.helllabs.android.xmp.Xmp;
import org.helllabs.android.xmp.preferences.Preferences;


public final class InfoCache {
	
	private InfoCache() {
		
	}

	public static boolean clearCache(final String filename) {
		final File cacheFile = new File(Preferences.CACHE_DIR, filename + ".cache");
		final File skipFile = new File(Preferences.CACHE_DIR, filename + ".skip");
		boolean ret = false;

		if (cacheFile.isFile()) {
			cacheFile.delete();
			ret = true;
		}

		if (skipFile.isFile()) {
			skipFile.delete();
			ret = true;
		}

		return ret;
	}

	public static boolean delete(final String filename) {
		final File file = new File(filename);

		clearCache(filename);

		return file.delete();
	}

	public static boolean fileExists(final String filename) {
		final File file = new File(filename);

		if (file.isFile()) {
			return true;
		}

		clearCache(filename);

		return false;
	}

	public static boolean testModuleForceIfInvalid(final String filename) {
		final File skipFile = new File(Preferences.CACHE_DIR, filename + ".skip");

		if (skipFile.isFile()) {
			skipFile.delete();
		}

		return testModule(filename);
	}

	public static boolean testModule(final String filename) {
		return testModule(filename, new ModInfo());
	}
	
	private static boolean checkIfCacheValid(final File file, final File cacheFile, final ModInfo info) throws IOException {
		boolean ret = false;
		final BufferedReader reader = new BufferedReader(new FileReader(cacheFile), 512);

		final String line = reader.readLine();
		if (line != null) {
			try {
				final int size = Integer.parseInt(line);

				if (size == file.length()) {
					info.name = reader.readLine();
					if (info.name != null) {
						reader.readLine();				// skip filename
						info.type = reader.readLine();
						if (info.type != null) {
							ret = true;
						}
					}
				}
			} catch (NumberFormatException e) {
				// Someone had binary contents in the cache file, breaking parseInt()
				ret = false;
			}
		}

		reader.close();
		return ret;
	}

	public static boolean testModule(final String filename, final ModInfo info) {
		if (!Preferences.CACHE_DIR.isDirectory() && !Preferences.CACHE_DIR.mkdirs()) {
			// Can't use cache
			return Xmp.testModule(filename, info);
		}

		final File file = new File(filename);
		final File cacheFile = new File(Preferences.CACHE_DIR, filename + ".cache");
		final File skipFile = new File(Preferences.CACHE_DIR, filename + ".skip");

		try {
			// If cache file exists and size matches, file is mod
			if (cacheFile.isFile()) {

				// If we have cache and skip, delete skip
				if (skipFile.isFile()) {
					skipFile.delete();
				}

				// Check if our cache data is good
				if (checkIfCacheValid(file, cacheFile, info)) {
					return true;
				}
				
				cacheFile.delete();		// Invalid or outdated cache file
			}

			if (skipFile.isFile()) {
				return false;
			}

			final boolean isMod = Xmp.testModule(filename, info);
			if (isMod) {
				final String[] lines = {
						Long.toString(file.length()),
						info.name,
						filename,
						info.type,
				};

				final File dir = cacheFile.getParentFile();
				if (!dir.isDirectory()) {
					dir.mkdirs();
				}
				cacheFile.createNewFile();
				FileUtils.writeToFile(cacheFile, lines);
			} else {
				final File dir = skipFile.getParentFile();
				if (!dir.isDirectory()) {
					dir.mkdirs();
				}
				skipFile.createNewFile();
			}

			return isMod;
		} catch (IOException e) {
			return Xmp.testModule(filename, info);
		}	
	}
};
