package org.helllabs.android.xmp.modarchive.request;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.helllabs.android.xmp.modarchive.model.Artist;
import org.helllabs.android.xmp.modarchive.model.Module;
import org.helllabs.android.xmp.util.Log;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

public class ModuleRequest extends ModArchiveRequest<List<Module>> {

	private static final String TAG = "ModuleRequest";

	public ModuleRequest(final String key, final String request) {
		super(key, request);
	}

	@Override
	protected List<Module> xmlParse(final String result) {
		final List<Module> moduleList = new ArrayList<Module>();
		Module module = null;
		boolean inArtistInfo = false;

		try {
			final XmlPullParserFactory xmlFactoryObject = XmlPullParserFactory.newInstance();
			final XmlPullParser myparser = xmlFactoryObject.newPullParser();
			final InputStream stream = new ByteArrayInputStream(result.getBytes());
			myparser.setInput(stream, null);

			int event = myparser.getEventType();
			String text = "";
			while (event != XmlPullParser.END_DOCUMENT)	{
				switch (event){		// NOPMD
				case XmlPullParser.START_TAG:
					final String start = myparser.getName();
					if (start.equals("module")) {
						module = new Module(); // NOPMD
					} else if (start.equals("artist_info")) {
						inArtistInfo = true;
					}
					break;
				case XmlPullParser.TEXT:
					text = myparser.getText().trim();
					break;
				case XmlPullParser.END_TAG:
					final String end = myparser.getName();
					//Log.d(TAG, "name=" + name + " text=" + text);
					if (end.equals("filename")) {
						module.setFilename(text);
					} else if (end.equals("format")) {
						module.setFormat(text);
					} else if (end.equals("url")) {
						module.setUrl(text);
					} else if (end.equals("bytes")) {
						module.setBytes(Integer.parseInt(text));
					} else if (end.equals("songtitle")) {
						module.setSongTitle(text);
					} else if (end.equals("alias")) {
						// Use non-guessed artist if available
						if (module.getArtist().equals(Artist.UNKNOWN)) {
							module.setArtist(text);
						}
					} else if (end.equals("title")) {
						module.setLicense(text);
					} else if (end.equals("instruments")) {
						module.setInstruments(text);
					} else if (end.equals("id")) {
						if (!inArtistInfo) {
							module.setId(Long.parseLong(text));
						}
					} else if (end.equals("hash")) {
						module.setHash(text);
					} else if (end.equals("artist_info")) {
						inArtistInfo = false;
					} else if (end.equals("module")) {
						if (!module.getFormat().equals("AHX")) {
							moduleList.add(module);
						}
					}
					break;
				default:
					break;
				}		 
				event = myparser.next(); 					
			}
		} catch (XmlPullParserException e) {
			Log.e(TAG, "XmlPullParserException: " + e.getMessage());
			return null;
		} catch (IOException e) {
			Log.e(TAG, "IOException: " + e.getMessage());
			return null;
		}

		return moduleList;
	}
}
