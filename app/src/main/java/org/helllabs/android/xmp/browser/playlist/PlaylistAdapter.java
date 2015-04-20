package org.helllabs.android.xmp.browser.playlist;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.helllabs.android.xmp.R;
import org.helllabs.android.xmp.util.FileUtils;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Typeface;
import android.os.Build;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class PlaylistAdapter extends ArrayAdapter<PlaylistItem> {
    private final List<PlaylistItem> items;
    private final Context context;
    private final boolean useFilename;
    
    static class ViewHolder {
    	public TextView titleText;
    	public TextView infoText;
    	public ImageView image;
    }

    public PlaylistAdapter(final Context context, final int resource, final int textViewResId, final List<PlaylistItem> items, final boolean useFilename) {
    	super(context, resource, textViewResId, items);
    	this.items = items;
    	this.context = context;
    	this.useFilename = useFilename;
    }
    
    @SuppressLint("InflateParams")
	@Override
    public View getView(final int position, View convertView, final ViewGroup parent) {
    	ViewHolder holder;
    	
    	if (convertView == null) {
    		final LayoutInflater inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    		convertView = inflater.inflate(R.layout.playlist_item, null);
    		holder = new ViewHolder();
    		holder.titleText = (TextView)convertView.findViewById(R.id.plist_title);
    		holder.infoText = (TextView)convertView.findViewById(R.id.plist_info);
    		holder.image = (ImageView)convertView.findViewById(R.id.plist_image);
    		convertView.setTag(holder);
    	} else {
    		holder = (ViewHolder)convertView.getTag();
    	}
    	final PlaylistItem item = getItem(position);
    	           
    	if (item != null) {                		    		
   			holder.titleText.setText(useFilename ? FileUtils.basename(item.getFile().getPath()) : item.getName());
   			holder.infoText.setText(item.getComment());
   			
   			final Typeface typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL);
   			final int imageRes = item.getImageRes();
   			final int type = item.getType();
   			
   			if (type == PlaylistItem.TYPE_DIRECTORY) {
    			holder.infoText.setTypeface(typeface, Typeface.ITALIC);
   			} else {
    			holder.infoText.setTypeface(typeface, Typeface.NORMAL);  			
   			}

   			if (imageRes > 0) {
   				holder.image.setImageResource(imageRes);
   				holder.image.setVisibility(View.VISIBLE);
   			} else {
   				holder.image.setVisibility(View.GONE);
   			}
     	}
            
    	return convertView;
    }
    
    public List<PlaylistItem> getItems() {
		return items;
	}
    
    public String getFilename(final int location) {
    	return items.get(location).getFile().getPath();
    }
    
    public File getFile(final int location) {
    	return items.get(location).getFile();
    }
    
    public List<String> getFilenameList() {
    	final List<String> list = new ArrayList<String>();
    	for (final PlaylistItem item : items) {
    		if (item.getType() == PlaylistItem.TYPE_FILE) {
    			list.add(item.getFile().getPath());
    		}
    	}
    	return list;
    }
    
    public int getDirectoryCount() {
    	int count = 0;
    	for (final PlaylistItem item : items) {
    		if (item.getType() != PlaylistItem.TYPE_DIRECTORY) {
    			break;
    		}
    		count++;
    	}
    	return count;
    }
    
    @TargetApi(11)
    public void addList(final List<PlaylistItem> list) {
    	if (Build.VERSION.SDK_INT >= 11) {
    		addAll(list);
    	} else {
    		for (final PlaylistItem item : list) {
    			add(item);
    		}
    	}
    }
}
