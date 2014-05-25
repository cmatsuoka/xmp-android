package org.helllabs.android.xmp;

import android.app.Application;

public class XmpApplication extends Application {
	
	private String[] fileArray;
	
	public String[] getFileArray() {
		return fileArray;
	}
	
	public void setFileArray(final String[] fileArray) {
		this.fileArray = fileArray;
	}
	
	public void clearFileArray() {
		fileArray = null;
	}

}
