package org.helllabs.android.xmp;

import java.util.List;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;

import android.app.Application;

public class XmpApplication extends Application {
	
	private List<String> fileList;
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
	
	public List<String> getFileList() {
		return fileList;
	}
	
	public void setFileList(final List<String> fileList) {
		this.fileList = fileList;
	}
	
	public void clearFileList() {
		fileList = null;
	}
	
	public RequestQueue getRequestQueue() {
        if (mRequestQueue == null) {
            mRequestQueue = Volley.newRequestQueue(getApplicationContext());
        }
 
        return mRequestQueue;
    }

}
