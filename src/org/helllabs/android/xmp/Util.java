package org.helllabs.android.xmp;

public class Util {
	static char[] digits = {
		'0', '1', '2', '3', '4', '5', '6', '7', '8', '9'
	};
	static char[] hexDigits = {
		'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'
	};
	
	static void to2d(char[] c, int x) {
		c[0] = x < 10 ? ' ' : digits[x / 10];
		c[1] = digits[x % 10];
	}
	
	static void to02d(char[] c, int x) {
		c[0] = digits[x / 10];
		c[1] = digits[x % 10];
	}
	
	static void to02X(char[] c, int x) {
		c[0] = hexDigits[x >> 4];
		c[1] = hexDigits[x & 0x0f];
	}

}
