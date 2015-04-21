package org.helllabs.android.xmp.browser.playlist;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.helllabs.android.xmp.R;
import org.helllabs.android.xmp.util.FileUtils;
import org.helllabs.android.xmp.util.Log;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Typeface;
import android.os.Build;
import android.support.v7.widget.RecyclerView;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class PlaylistAdapter extends RecyclerView.Adapter<PlaylistAdapter.ViewHolder> {
    private static final String TAG = "PlaylistAdapter";
    private final List<PlaylistItem> items;
    private final Context context;
    private final boolean useFilename;
    private int position;
    private OnItemClickListener onItemClickListener;

    public static interface OnItemClickListener {
        void onItemClick(PlaylistAdapter adapter, View view, int position);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        public TextView titleText;
        public TextView infoText;
        public ImageView image;
        private OnItemClickListener onItemClickListener;
        private PlaylistAdapter adapter;

        public ViewHolder(final View itemView, final PlaylistAdapter adapter) {
            super(itemView);
            itemView.setOnClickListener(this);
            titleText = (TextView)itemView.findViewById(R.id.plist_title);
            infoText = (TextView)itemView.findViewById(R.id.plist_info);
            image = (ImageView)itemView.findViewById(R.id.plist_image);
            this.adapter = adapter;
        }

        public void setOnItemClickListener(final OnItemClickListener listener) {
            onItemClickListener = listener;
        }

        @Override
        public void onClick(final View view) {
            if (onItemClickListener != null) {
                onItemClickListener.onItemClick(adapter, view, getPosition());
            }
        }
    }

    public void setOnItemClickListener(final OnItemClickListener listener) {
        onItemClickListener = listener;
    }

    @Override
    public ViewHolder onCreateViewHolder(final ViewGroup parent, final int viewType) {
        final View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.playlist_item, parent, false);
        final ViewHolder holder = new ViewHolder(view, this);
        holder.setOnItemClickListener(onItemClickListener);
        return holder;
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, final int position) {
        final PlaylistItem item = items.get(position);
        final Typeface typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL);
        final int imageRes = item.getImageRes();
        final int type = item.getType();

        if (type == PlaylistItem.TYPE_DIRECTORY) {
            holder.infoText.setTypeface(typeface, Typeface.ITALIC);
        } else {
            holder.infoText.setTypeface(typeface, Typeface.NORMAL);
        }

        holder.titleText.setText(item.getName());
        holder.infoText.setText(item.getComment());

        if (imageRes > 0) {
   			holder.image.setImageResource(imageRes);
   			holder.image.setVisibility(View.VISIBLE);
   		} else {
   			holder.image.setVisibility(View.GONE);
   		}

        // See http://stackoverflow.com/questions/26466877/how-to-create-context-menu-for-recyclerview
        holder.itemView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                setPosition(holder.getPosition());
                return false;
            }
        });
    }

    @Override
    public void onViewRecycled(ViewHolder holder) {
        holder.itemView.setOnLongClickListener(null);
        super.onViewRecycled(holder);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public PlaylistItem getItem(final int num) {
        return items.get(num);
    }

    public void clear() {
        items.clear();
    }

    public void add(final PlaylistItem item) {
        items.add(item);
    }

    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        this.position = position;
    }

    public PlaylistAdapter(final Context context, final int resource, final int textViewResId, final List<PlaylistItem> items, final boolean useFilename) {
    	this.items = items;
    	this.context = context;
    	this.useFilename = useFilename;
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
    		items.addAll(list);
    	} else {
    		for (final PlaylistItem item : list) {
    			items.add(item);
    		}
    	}
    }
}
