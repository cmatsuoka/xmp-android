package org.helllabs.android.xmp.service;

interface PlayerCallback {
	void newModCallback();
	void endModCallback();
	void endPlayCallback();
	void pauseCallback();
	void newSequenceCallback();
}