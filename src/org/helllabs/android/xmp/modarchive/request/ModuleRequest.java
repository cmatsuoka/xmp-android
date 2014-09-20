package org.helllabs.android.xmp.modarchive.request;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

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

		try {
			final XmlPullParserFactory xmlFactoryObject = XmlPullParserFactory.newInstance();
			final XmlPullParser myparser = xmlFactoryObject.newPullParser();
			final InputStream stream = new ByteArrayInputStream(result.getBytes());
			myparser.setInput(stream, null);
			
			int event = myparser.getEventType();
			String text = "";
			while (event != XmlPullParser.END_DOCUMENT)	{
				switch (event){
				case XmlPullParser.START_TAG:
					if (myparser.getName().equals("module")) {
						module = new Module(); // NOPMD
					}
					break;
				case XmlPullParser.TEXT:
					text = myparser.getText();
					break;
				case XmlPullParser.END_TAG:
					final String name = myparser.getName();
					//Log.d(TAG, "name=" + name + " text=" + text);
					if (name.equals("filename")) {
						module.setFilename(text);
					} else if (name.equals("format")) {
						module.setFormat(text);
					} else if (name.equals("url")) {
						module.setUrl(text);
					} else if (name.equals("bytes")) {
						module.setBytes(Integer.parseInt(text));
					} else if (name.equals("songtitle")) {
						module.setSongTitle(text);
					} else if (name.equals("alias")) {
						module.setArtist(text);
					} else if (name.equals("title")) {
						module.setLicense(text);
					} else if (name.equals("instruments")) {
						module.setInstruments(text);
					} else if (name.equals("id")) {
						module.setId(Long.parseLong(text));
					} else if (name.equals("module")) {
						moduleList.add(module);
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
