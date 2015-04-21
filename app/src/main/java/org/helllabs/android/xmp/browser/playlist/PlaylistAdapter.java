package org.helllabs.android.xmp.browser.playlist;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Typeface;
import android.os.Build;
import android.support.v4.view.ViewCompat;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.h6ah4i.android.widget.advrecyclerview.draggable.DraggableItemAdapter;
import com.h6ah4i.android.widget.advrecyclerview.draggable.ItemDraggableRange;
import com.h6ah4i.android.widget.advrecyclerview.utils.AbstractDraggableItemViewHolder;

import org.helllabs.android.xmp.R;
import org.helllabs.android.xmp.util.Log;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class PlaylistAdapter extends RecyclerView.Adapter<PlaylistAdapter.ViewHolder>  implements DraggableItemAdapter<PlaylistAdapter.ViewHolder> {
    private static final String TAG = "PlaylistAdapter";
    private final List<PlaylistItem> items;
    private final Context context;
    private final boolean useFilename;
    private int position;
    private OnItemClickListener onItemClickListener;

    public static interface OnItemClickListener {
        void onItemClick(PlaylistAdapter adapter, View view, int position);
    }

    public static class ViewHolder extends AbstractDraggableItemViewHolder implements View.OnClickListener {
        public View container;
        public TextView titleText;
        public TextView infoText;
        public ImageView image;
        private OnItemClickListener onItemClickListener;
        private PlaylistAdapter adapter;

        public ViewHolder(final View itemView, final PlaylistAdapter adapter) {
            super(itemView);
            itemView.setOnClickListener(this);
            container = itemView.findViewById(R.id.plist_container);
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

    private static boolean hitTest(final View v, final int x, final int y) {
        final int tx = (int) (ViewCompat.getTranslationX(v) + 0.5f);
        final int ty = (int) (ViewCompat.getTranslationY(v) + 0.5f);
        final int left = v.getLeft() + tx;
        final int right = v.getRight() + tx;
        final int top = v.getTop() + ty;
        final int bottom = v.getBottom() + ty;

        return (x >= left) && (x <= right) && (y >= top) && (y <= bottom);
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

        // Advanced RecyclerView
        // set background resource (target view ID: container)
        /*
        final int dragState = holder.getDragStateFlags();

        if (((dragState & RecyclerViewDragDropManager.STATE_FLAG_IS_UPDATED) != 0)) {
            int bgResId;

            if ((dragState & RecyclerViewDragDropManager.STATE_FLAG_IS_ACTIVE) != 0) {
                bgResId = R.drawable.bg_item_dragging_active_state;
            } else if ((dragState & RecyclerViewDragDropManager.STATE_FLAG_DRAGGING) != 0) {
                bgResId = R.drawable.bg_item_dragging_state;
            } else {
                bgResId = R.drawable.bg_item_normal_state;
            }

            holder.mContainer.setBackgroundResource(bgResId);
        }*/


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

    @Override
    public void onMoveItem(final int fromPosition, final int toPosition) {
        Log.d(TAG, "onMoveItem(fromPosition = " + fromPosition + ", toPosition = " + toPosition + ")");

        if (fromPosition == toPosition) {
            return;
        }

        final PlaylistItem item = items.get(fromPosition);
        items.remove(item);
        items.add(toPosition, item);
        //playlist.setListChanged(true);

        notifyItemMoved(fromPosition, toPosition);
    }

    @Override
    public boolean onCheckCanStartDrag(final ViewHolder holder, final int x, final int y) {
        // x, y --- relative from the itemView's top-left
        final View containerView = holder.container;
        final View dragHandleView = holder.image;

        final int offsetX = containerView.getLeft() + (int) (ViewCompat.getTranslationX(containerView) + 0.5f);
        final int offsetY = containerView.getTop() + (int) (ViewCompat.getTranslationY(containerView) + 0.5f);

        return hitTest(dragHandleView, x - offsetX, y - offsetY);
    }

    @Override
    public ItemDraggableRange onGetItemDraggableRange(ViewHolder holder) {
        // no drag-sortable range specified
        return null;
    }
}