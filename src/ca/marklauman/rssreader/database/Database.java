package ca.marklauman.rssreader.database;

import android.net.Uri;

/** Schema class for the database as a whole.
 *  @author Mark Lauman                    */
public final class Database {
	
	Database() {}

	/** Database version number. */
	public static final int VERSION = 2;
	
	/** Name of the database file */
	public final static String NAME = "rssdata.db";
	/** The authority used by the ContentProvider. */
	public static final String AUTHORITY = "ca.marklauman.rssdata";
	/** MIME prefix for single items. */
	public static final String MIME_SINGLE = "vnd.android.cursor.item/";
	/** MIME prefix for multiple items. */
	public static final String MIME_GROUP  = "vnd.android.cursor.dir/";
	/** Uri of this database. */
	public static final Uri URI = Uri.parse("content://" + AUTHORITY);
	
}
