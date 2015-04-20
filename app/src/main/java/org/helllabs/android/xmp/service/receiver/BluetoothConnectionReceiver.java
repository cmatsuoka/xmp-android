package org.helllabs.android.xmp.service.receiver;

import org.helllabs.android.xmp.util.Log;

import android.bluetooth.BluetoothA2dp;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

public class BluetoothConnectionReceiver extends BroadcastReceiver {
	private static final String TAG = "BluetoothConnectionReceiver";
	public static final int DISCONNECTED = 0;
	public static final int CONNECTED = 1;
	public static final int NO_STATE = -1;
	private static int state = NO_STATE;

	@Override
	public void onReceive(final Context context, final Intent intent) {
		final String action = intent.getAction();
		Log.i(TAG, "Action " + action);
		
		if (Build.VERSION.SDK_INT >= 11 && intent.getAction().equals(BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED)) {			
			final int bluetoothState = intent.getIntExtra(BluetoothA2dp.EXTRA_STATE, -1);
			Log.i(TAG, "Extra state = " + bluetoothState);
			if (bluetoothState == BluetoothProfile.STATE_DISCONNECTING || bluetoothState == BluetoothProfile.STATE_DISCONNECTED) {
				Log.i(TAG, "Bluetooth state changed to disconnected");
				state = DISCONNECTED;
			} else if (bluetoothState == BluetoothProfile.STATE_CONNECTED) {
				Log.i(TAG, "Bluetooth state changed to connected");
				state = CONNECTED;
			}
		} else if (intent.getAction().equals(BluetoothDevice.ACTION_ACL_CONNECTED)) {
			Log.i(TAG, "Bluetooth connected");
			//state = CONNECTED;
		} else if (intent.getAction().equals(BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED)) {
			Log.i(TAG, "Bluetooth disconnect requested");
			state = DISCONNECTED;
		} else if (intent.getAction().equals(BluetoothDevice.ACTION_ACL_DISCONNECTED)) {
			Log.i(TAG, "Bluetooth disconnected");
			state = DISCONNECTED;
		}
	}
	
	public static int getState() {
		return state;
	}
	
	public static void setState(final int state) {
		BluetoothConnectionReceiver.state = state;
	}

}
