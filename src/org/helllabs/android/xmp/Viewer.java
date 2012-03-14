package org.helllabs.android.xmp;

import android.content.Context;
import android.view.SurfaceView;

public abstract class Viewer extends SurfaceView {
	
    public class Info {
    	int time;
    	int[] values = new int[7];	// order pattern row num_rows frame speed bpm
    	int[] volumes = new int[64];
    	int[] instruments = new int[64];
    	int[] keys = new int[64];
    };

	public Viewer(Context context) {
		super(context);
		// TODO Auto-generated constructor stub
	}
	

	public void update(ModInterface modPlayer, int modVars[], Info info) {
		
	}
}
