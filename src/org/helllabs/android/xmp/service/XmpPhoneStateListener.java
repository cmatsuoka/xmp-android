package org.helllabs.android.xmp.service;


import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;


public class XmpPhoneStateListener extends PhoneStateListener {
	PlayerService service;
	
	public XmpPhoneStateListener(PlayerService service) {
		this.service = service;
	}
	
	@Override
	public void onCallStateChanged(int state, String incomingNumber) {
		Log.i("Xmp Listener", "Call state changed: " + state);
		service.autoPause(state != TelephonyManager.CALL_STATE_IDLE);
	}
}



