package org.helllabs.android.xmp.service.utils;

import java.util.Collections;
import java.util.List;



public class QueueManager {
	private final List<String> list;
	private final RandomIndex ridx;
	private int index;
	private final boolean shuffleMode;
	private final boolean loopListMode;
	private int randomStart;
    
    public QueueManager(final List<String> fileList, int start, final boolean shuffle, final boolean loop, final boolean keepFirst) {
    	if (start >= fileList.size()) {
    		start = fileList.size() - 1;
    	}
    	
    	if (keepFirst) {
    		Collections.swap(fileList, 0, start);
    		start = 0;
    		randomStart = 1;
    	}
    	
    	index = start;
    	list = fileList;
    	ridx = new RandomIndex(randomStart, fileList.size());
    	shuffleMode = shuffle;
    	loopListMode = loop;
    }
    
    public void add(final List<String> fileList) {
    	if (!fileList.isEmpty()) {
    		ridx.extend(fileList.size(), index + 1);
    		list.addAll(fileList);
    	}
    }
    
    public int size() {
    	return list.size();
    }
    
    public boolean next() {
    	index++;
    	if (index >= list.size()) {
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
    			index += list.size();
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
    	return list.get(idx);
    }
}
