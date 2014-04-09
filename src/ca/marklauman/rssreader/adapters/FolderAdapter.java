package ca.marklauman.rssreader.adapters;

import java.util.HashMap;

import ca.marklauman.rssreader.R;
import ca.marklauman.rssreader.providers.DBSchema.Folder;
import ca.marklauman.tools.Debug;

import android.content.Context;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.database.MergeCursor;
import android.util.Log;

public class FolderAdapter extends CursorSelAdapter
						   implements Debug {
	
	/** The id of the "All Feeds" list item */
	public final static long ALL_FEEDS_ID = 0;
	
	HashMap<Long, String> names = new HashMap<Long, String>();

	public FolderAdapter(Context context, int layout, String[] from, int[] to) {
		super(context, layout, from, to);
	}
	
	@Override
	public void changeCursor(Cursor cursor) {
		if(cursor != null)
			msg("changeCursor(cursor)");
		else
			msg("changeCursor(null)");
		
		// Add "All Feeds" to the top
		String[] cols = {Folder._ID, Folder._NAME};
		String all_feed = mContext.getResources()
								  .getString(R.string.all_feeds);
		MatrixCursor extras = new MatrixCursor(cols);
		extras.addRow(new String[] {"" + ALL_FEEDS_ID, all_feed});
		Cursor[] cursors = {extras, cursor};
		Cursor combo_cur = new MergeCursor(cursors);
		
		// change the cursor
		super.changeCursor(combo_cur);
		
		// update the names set
		names.clear();
		combo_cur.moveToFirst();
		int col_name = combo_cur.getColumnIndex(Folder._NAME);
		int col_id = combo_cur.getColumnIndex(Folder._ID);
		do {
			long id = combo_cur.getLong(col_id);
			String name = combo_cur.getString(col_name);
			names.put(id, name);
		} while(combo_cur.moveToNext());
	}
	
	
	/** Get the name of the folder with the given id.
	 *  @param id The sql id of the folder (not its position
	 *  in the list).
	 *  @return The title of the folder, or {@code null}
	 *  if the folder does not exist.                     */
	public String getFoldName(long id) {
		return names.get(id);
	}

	@Override
	public void msg(String msg) {
		Log.d("RSSReader.FolderAdapter", msg);
	}
}
