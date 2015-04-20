package org.helllabs.android.xmp.service;

interface PlayerCallback {
	void newModCallback();
	void endModCallback();
	void endPlayCallback(int result);
	void pauseCallback();
	void newSequenceCallback();
}