package org.helllabs.android.xmp.service.utils;

import java.util.Date;
import java.util.Random;


public class RandomIndex {
	private int[] idx;
	private final Random random;
	
	public RandomIndex(final int start, final int size) {
		idx = new int[size];
		
		random = new Random();
		final Date date = new Date();
		random.setSeed(date.getTime());
		
		for (int i = 0; i < size; i++) {
			idx[i] = i;
		}
		
		randomize(start, size - start);
	}

	public void randomize(final int start, final int length) {
		final int end = start + length;
		for (int i = start; i < end; i++) {				
			final int num = start + random.nextInt(length);
			final int temp = idx[i];
			idx[i] = idx[num];
			idx[num] = temp;
		}
	}
	
	public void randomize() {
		randomize(0, idx.length);
	}
	
	public void extend(final int amount, final int index) {
		final int length = idx.length;
		int[] newIdx = new int[length + amount];
		System.arraycopy(idx, 0, newIdx, 0, length);
		for (int i = length; i < length + amount; i++) {
			newIdx[i] = i;
		}
		idx = newIdx;
		randomize(index, idx.length - index);
	}
	
	public int getIndex(final int num) {
		return idx[num];
	}
}
