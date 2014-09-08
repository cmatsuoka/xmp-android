package org.helllabs.android.xmp;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;

import android.app.Application;

public class XmpApplication extends Application {
	
	private String[] fileArray;
	private RequestQueue mRequestQueue;
	private static XmpApplication mInstance;
	
	@Override
    public void onCreate() {
        super.onCreate();
        setInstance(this);
	}
	
	private static void setInstance(final XmpApplication instance) {
        mInstance = instance;
    }
	
	public static synchronized XmpApplication getInstance() {
        return mInstance;
    }
	
	public String[] getFileArray() {
		return fileArray;
	}
	
	public void setFileArray(final String[] fileArray) {
		this.fileArray = fileArray;
	}
	
	public void clearFileArray() {
		fileArray = null;
	}
	
	public RequestQueue getRequestQueue() {
        if (mRequestQueue == null) {
            mRequestQueue = Volley.newRequestQueue(getApplicationContext());
        }
 
        return mRequestQueue;
    }

}
