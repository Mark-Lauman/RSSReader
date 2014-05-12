package ca.marklauman.rssreader.panel.item;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import ca.marklauman.rssreader.R;
import ca.marklauman.rssreader.database.schema.Item;
import ca.marklauman.tools.CursorSelAdapter;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.view.View;
import android.widget.TextView;

/** Adapter for cursors from the {@link Item} table
 *  of {@link RSSData}.
 *  @author Mark Lauman                          */
public class ItemAdapter extends CursorSelAdapter {
	
	/** index of the time column */
	private int col_time;
	/** index of the url column */
	private int col_url;
	/** index of the read column */
	private int col_read;
	/** Formatter for displaying the year correctly */
	private DateFormat format_year;
	/** Formatter for displaying the month correctly */
	private DateFormat format_month;
	/** Formatter for displaying the hour correctly */
	private DateFormat format_hour;
	
	
	public ItemAdapter(Context context) {
		super(context, R.layout.list_item,
			  new String[]{Item._TITLE,
						   Item._TIME,
						   Item._READ,
						   Item._BRIEF},
				 new int[]{R.id.title,
						   R.id.time,
						   R.id.layout,
						   R.id.brief});
		setViewBinder(new Binder());
		format_year = android.text.format.DateFormat
							 .getDateFormat(context);
		format_month = new SimpleDateFormat("MMM d", Locale.getDefault());
		format_hour = android.text.format.DateFormat
							 .getTimeFormat(context);
	}
	
	
	@Override
	public void changeCursor(Cursor cursor) {
		super.changeCursor(cursor);
		if(cursor == null) return;
		col_time = cursor.getColumnIndex(Item._TIME);
		col_url = cursor.getColumnIndex(Item._URL);
		col_read = cursor.getColumnIndex(Item._READ);
	}
	
	
	/** Get the URL of the item at the specified
	 *  position.
	 *  @param position The position of the item
	 *  in the adapter.
	 *  (not its row id in the SQL database)
	 *  @return The URL of that item, or {@code null}
	 *  if no URL exists there.                    */
	public Uri getURL(int position) {
		if(mCursor == null || !mCursor.moveToPosition(position))
			return null;
		return Uri.parse(mCursor.getString(col_url));
	}
	
	
	/** Class used to interpret parts of the
	 *  {@link Item} that are not stored in legible
	 *  formats. Transforms them into a legible format
	 *  before displaying them.
	 *  @author Mark Lauman                    */
	private class Binder implements ViewBinder {
		@Override
		public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
			if(columnIndex == col_time) {
				TextView txt = (TextView) view;
				txt.setText(getTimeString(cursor.getLong(columnIndex)));
				return true;
			} else if(columnIndex == col_read) {
				TextView title = (TextView) view.findViewById(R.id.title);
				TextView brief = (TextView) view.findViewById(R.id.brief);
				TextView time = (TextView) view.findViewById(R.id.time);
				int color;
				if(0 == cursor.getInt(columnIndex))
					color = mContext.getResources()
									.getColor(R.color.unread_feed);
				else
					color = mContext.getResources()
									.getColor(R.color.read_feed);
				title.setTextColor(color);
				brief.setTextColor(color);
				time.setTextColor(color);
				return true;
			}
			return false;
		}
		
		
		/** Converts the provided time to a String.
		 *  Follows system settings for time formatting.
		 *  @param time The time to convert (UNIX format)
		 *  @return That time as a string.             */
		private String getTimeString(long time) {
			Calendar now = Calendar.getInstance();
			Calendar then = Calendar.getInstance();
			then.setTimeInMillis(time);
			
			if(then.get(Calendar.YEAR) == now.get(Calendar.YEAR)) {
				if(then.get(Calendar.MONTH) == now.get(Calendar.MONTH)
				   && then.get(Calendar.DATE) == now.get(Calendar.DATE))
					return format_hour.format(new Date(time));
				return format_month.format(new Date(time));
			}
			return format_year.format(new Date(time));
		}
	}
}