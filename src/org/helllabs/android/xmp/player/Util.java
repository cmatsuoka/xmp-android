package org.helllabs.android.xmp.player;

public final class Util {
	
	private static char[] digits = {
		'0', '1', '2', '3', '4', '5', '6', '7', '8', '9'
	};

	private static char[] hexDigits = {
		'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'
	};

	private Util() {

	}

	public static void to2d(final char[] res, final int val) {
		res[0] = val < 10 ? ' ' : digits[val / 10];
		res[1] = digits[val % 10];
	}

	public static void to02d(final char[] res, final int val) {
		res[0] = digits[val / 10];
		res[1] = digits[val % 10];
	}

	public static void to02X(final char[] res, final int val) {
		res[0] = hexDigits[val >> 4];
		res[1] = hexDigits[val & 0x0f];
	}
}
