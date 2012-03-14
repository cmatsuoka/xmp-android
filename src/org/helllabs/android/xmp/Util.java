package org.helllabs.android.xmp;

public class Util {
	static final char[] digits = {
		'0', '1', '2', '3', '4', '5', '6', '7', '8', '9'
	};
	
	static final char[] hexDigits = {
		'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'
	};
	
	// http://www.rbgrn.net/content/290-light-racer-20-days-32-33-getting-great-game-performance
	static void toString(int val, char[] buf) {
		val %= 100;
		buf[0] = val > 9 ? digits[val / 10] : ' ';
		buf[1] = digits[val % 10];
	}
	
	static void toHex(int val, char[] buf) {
		val %= 256;
		buf[0] = hexDigits[val / 16];
		buf[1] = hexDigits[val % 16];
	}
}
