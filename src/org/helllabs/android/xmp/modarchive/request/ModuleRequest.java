package org.helllabs.android.xmp.modarchive.request;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;

import org.helllabs.android.xmp.modarchive.model.Artist;
import org.helllabs.android.xmp.modarchive.model.Module;
import org.helllabs.android.xmp.modarchive.response.HardErrorResponse;
import org.helllabs.android.xmp.modarchive.response.ModArchiveResponse;
import org.helllabs.android.xmp.modarchive.response.ModuleResponse;
import org.helllabs.android.xmp.modarchive.response.SoftErrorResponse;
import org.helllabs.android.xmp.util.Log;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

public class ModuleRequest extends ModArchiveRequest {

	private static final String TAG = "ModuleRequest";

	public ModuleRequest(final String key, final String request) {
		super(key, request);
	}
	
	public ModuleRequest(final String key, final String request, final String parameter) throws UnsupportedEncodingException {
		super(key, request, parameter);
	}
	
	public ModuleRequest(final String key, final String request, final long parameter) throws UnsupportedEncodingException {
		this(key, request, String.valueOf(parameter));
	}

	@Override
	protected ModArchiveResponse xmlParse(final String result) {
		final ModuleResponse moduleList = new ModuleResponse();
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
					if (end.equals("error")) {
						return new SoftErrorResponse(text);
					} else if (end.equals("filename")) {
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
					} else if (end.equals("description")) {
						module.setLicenseDescription(text);
					} else if (end.equals("legalurl")) {
						module.setLegalUrl(text);
					} else if (end.equals("instruments")) {
						module.setInstruments(text);
					} else if (end.equals("id")) {
						if (!inArtistInfo) {
							module.setId(Long.parseLong(text));
						}
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
			return new HardErrorResponse(e);
		} catch (IOException e) {
			Log.e(TAG, "IOException: " + e.getMessage());
			return new HardErrorResponse(e);
		}

		return moduleList;
	}
}
