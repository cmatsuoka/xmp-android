package org.helllabs.android.xmp.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class QueueManager {
	private final List<String> array;
	private final RandomIndex ridx;
	private int index;
	private final boolean shuffleMode;
	private final boolean loopListMode;
    
    public QueueManager(final String[] files, int start, final boolean shuffle, final boolean loop) {
    	if (start >= files.length) {
    		start = files.length - 1;
    	}
    	index = start;
    	array = new ArrayList<String>(Arrays.asList(files));
    	ridx = new RandomIndex(files.length);
    	shuffleMode = shuffle;
    	loopListMode = loop;
    }
    
    public void add(final String[] files) {
    	if (files.length > 0) {
    		ridx.extend(files.length, index + 1);
    		for (final String name : files) {
    			array.add(name);
    		}
    	}
    }
    
    public int size() {
    	return array.size();
    }
    
    public boolean next() {
    	index++;
    	if (index >= array.size()) {
    		if (loopListMode) {
    			ridx.randomize();
    			index = 0;
    		} else {
    			return false;
    		}
    	}
    	return true;
    }
    
    public void previous() {
    	index -= 2;
    	if (index < -1) {
    		if (loopListMode) {
    			index += array.size();
    		} else {
    			index = -1;
    		}
    	}
    }
    
    public void restart() {
    	index = -1;
    }
    
    public int getIndex() {
    	return index;
    }
    
    public void setIndex(final int num) {
    	index = num;
    }
    
    public String getFilename() {
    	final int idx = shuffleMode ? ridx.getIndex(index) : index;
    	return array.get(idx);
    }
}
