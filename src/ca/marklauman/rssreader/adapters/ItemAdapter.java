package ca.marklauman.rssreader.adapters;

import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;

import ca.marklauman.rssreader.R;
import ca.marklauman.rssreader.providers.DBSchema.Item;
import android.content.Context;
import android.database.Cursor;
import android.support.v4.widget.SimpleCursorAdapter.ViewBinder;
import android.text.Html;
import android.view.View;
import android.widget.TextView;

public class ItemAdapter extends CursorSelAdapter
						 implements ViewBinder {
	
	/** Shorthand for acessing
	 *  {@link android.text.format.DateFormat}
	 *  @author Mark Lauman                 */
	private class AndDateFormat
		extends android.text.format.DateFormat {}
	
	/** Milliseconds in 1 day */
	private static final long ONE_DAY = 86400000L;
	
	/** Formatter for displaying the day correctly */
	private DateFormat format_date = null;
	/** Formatter for displaying the hour correctly */
	private DateFormat format_hour = null;
	
	private int col_time = -1;
	private int col_desc = -1;
	
	
	public ItemAdapter(Context context) {
		super(context, R.layout.list_item_item,
				  new String[]{Item._TITLE,
							   Item._DESC,
							   Item._TIME},
				  new int[]   {android.R.id.text1,
							   android.R.id.text2,
							   R.id.item_time});
		this.setViewBinder(this);
		if(format_date != null) return;
		format_date = AndDateFormat.getMediumDateFormat(context);
		format_hour = AndDateFormat.getTimeFormat(context);
	}
	
	
	@Override
	public void changeCursor(Cursor cursor) {
		super.changeCursor(cursor);
		if(cursor != null) {
			col_time = cursor.getColumnIndex(Item._TIME);
			col_desc = cursor.getColumnIndex(Item._DESC);
		}
	}


	@Override
	public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
		if(columnIndex == col_time) {
			long time = cursor.getLong(columnIndex);
			TextView txt = (TextView) view;
			txt.setText(getTimeString(time));
			return true;
		}
		if(columnIndex == col_desc) {
			TextView txt = (TextView) view;
			txt.setText(Html.fromHtml(cursor.getString(columnIndex))
							.toString()
							.replace(""+(char)65532, "")
							.replace("  ", " ")
							.trim());
			return true;
		}
		return false;
	}
	
	
	
	/** Gets the time formatted as a string. Will follow
	 *  system settings for stuff like 24hr time, etc.
	 *  @param time
	 *  @return */
	public String getTimeString(long time) {
		long curTime = Calendar.getInstance()
							   .getTimeInMillis();
		
		if( (curTime - time) < ONE_DAY)
			return format_hour.format(new Date(time));
		return format_date.format(new Date(time));
	}
}
