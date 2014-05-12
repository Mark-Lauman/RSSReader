package ca.marklauman.rssreader.database.schema;

import android.net.Uri;
import android.provider.BaseColumns;

/** Schema class for the Item table */
public class Item implements BaseColumns {
	
	Item() {}
	
	/** Name of this table. */
	public static final String TABLE_NAME = "item";
	/** MIME type of items in this table. */
	public static final String MIME_TYPE =
			Database.AUTHORITY + "." + TABLE_NAME;
	/** Uri to access this table. */
	public static final Uri URI = 
			Uri.withAppendedPath(Database.URI, TABLE_NAME);
	
	/** <b>String</b> - Title of the feed item. */
	public static final String _TITLE = "title";
	/** <b>String</b> - Brief version of the {@link #CONTENT}. */
	public static final String _BRIEF = "brief";
	/** <b>String</b> - Content of the feed item. */
	public static final String _CONTENT = "content";
	/** <b>String</b> - URL provided for this item. */
	public static final String _URL = "url";
	/** <b>Boolean</b> - True if this item has been read. */
	public static final String _READ = "read";
	/** <b>Long</b> - Time this item was posted (according to the feed). */
	public static final String _TIME = "time";
	/** <b>Long</b> - First time this item was inserted. */
	public static final String _TIME_SAVE = "time_save";
	/** <b>Long</b> - Last time this item was inserted. */
	public static final String _TIME_INSERT = "time_insert";
	
	public static final String CREATE_TABLE =
			"CREATE TABLE " + TABLE_NAME +" ("
			+ _ID + " integer primary key autoincrement, "
			+ _TITLE + " text, "
			+ _BRIEF + " text, "
			+ _CONTENT + " text, "
			+ _URL + " text, "
			+ _READ + " integer DEFAULT 0, "
			+ _TIME + " integer, "
			+ _TIME_SAVE + " integer, "
			+ _TIME_INSERT + " integer, "
			+ "CONSTRAINT unique_item UNIQUE ("
			+ _TITLE + ", "
			+ _CONTENT + ", "
			+ _URL + ") ON CONFLICT ABORT"
			+ ");";
	
	public static final String DROP_TABLE =
			"DROP TABLE IF EXISTS " + TABLE_NAME;
}