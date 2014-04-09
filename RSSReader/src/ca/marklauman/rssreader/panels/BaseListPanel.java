package ca.marklauman.rssreader.panels;

import ca.marklauman.rssreader.R;

import com.actionbarsherlock.app.SherlockListFragment;

import android.annotation.SuppressLint;
import android.database.Cursor;
import android.os.Build;
import android.support.v4.widget.SimpleCursorAdapter;

/** The Folder and Feed Panels are based off of this. */
public abstract class BaseListPanel extends SherlockListFragment {
	
	/** Basic 1-line list layout */
	@SuppressLint("InlinedApi")
	public static final int DEFAULT_LAYOUT_1 = Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB ?
            android.R.layout.simple_list_item_activated_1 : android.R.layout.simple_list_item_1;
	/** Basic 2-line list layout */
	@SuppressLint("InlinedApi")
	public static final int DEFAULT_LAYOUT_2 = Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB ?
            android.R.layout.simple_list_item_activated_2 : android.R.layout.simple_list_item_2;
	
	/** The cursor adapter attached to this panel */
	protected SimpleCursorAdapter cursor_adapt;
	
	/** Provide the cursor for the list view.        */
	public void setListAdapter(SimpleCursorAdapter adapter) {
		this.cursor_adapt = adapter;
		super.setListAdapter(adapter);
	}
	
	/** Change the underlying cursor to a new cursor. If
	 *  there is an existing cursor it will be closed.
	 *  @param cursor The new cursor to be used.      */
	public void changeCursor(Cursor cursor) {
		if(cursor_adapt == null) return;
		cursor_adapt.changeCursor(cursor);
	}
	
	/** Checks if the swap panel exists.
	 *  @return {@code true} if it does. */
	public boolean swapExists() {
		return getFragmentManager().findFragmentById(R.id.swap_panel) != null;
	}
}
