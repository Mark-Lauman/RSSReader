package ca.marklauman.rssreader.adapters;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import ca.marklauman.rssreader.R;
import ca.marklauman.rssreader.database.Item;
import android.content.Context;
import android.database.Cursor;
import android.support.v4.widget.SimpleCursorAdapter.ViewBinder;
import android.view.View;
import android.widget.TextView;

/** Adapter for cursors from the {@link Item} table
 *  of {@link RSSData}.
 *  @author Mark Lauman                          */
public class ItemAdapter extends CursorSelAdapter
						 implements ViewBinder {
	
	// index of the time column
	private int col_time;
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
						   Item._BRIEF},
				 new int[]{R.id.title,
						   R.id.time,
						   R.id.brief});
		setViewBinder(this);
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
	}

	@Override
	public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
		if(columnIndex == col_time) {
			TextView txt = (TextView) view;
			txt.setText(getTimeString(cursor.getLong(columnIndex)));
			return true;
		}
		return false;
	}
	
	/** Converts the provided time to a String.
	 *  Follows system settings for time formatting.
	 *  @param time The time to convert (UNIX format)
	 *  @return That time as a string.             */
	public String getTimeString(long time) {
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