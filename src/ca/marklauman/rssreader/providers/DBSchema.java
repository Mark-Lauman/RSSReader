package ca.marklauman.rssreader.providers;

import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.provider.BaseColumns;

/** Schemas for the Database. Defines basic properties of
 *  the database
 *  @author Mark Lauman                                */
public final class DBSchema {
	/** Empty constructor. No instances. */
	DBSchema(){}
	
	/** The authority used by the ContentProvider. */
	public static final String AUTHORITY = "ca.marklauman.rssdata";
	/** String prefix for URIs of this provider. */
	public static final String URI_STR = "content://" + AUTHORITY;
	/** MIME prefix for single items. */
	public static final String MIME_SINGLE = "vnd.android.cursor.item/";
	/** MIME prefix for multiple items. */
	public static final String MIME_GROUP  = "vnd.android.cursor.dir/";
	/** Name of the database file. */
	public static final String DB_NAME = "database.db";
	/** Database version number. */
	public static final int DB_VERSION = 1;
	
	
	/** An array of all the query tables in the database.
	 *  The index of each table is equal to its flag
	 *  value
	 *  ({@code TABLE_NAMES[}{@link Undo#FLAG}{@code ] == }
	 *  {@link Undo#TABLE_NAME}). */
	public static final String[] QUERY_TABLES =
		{Undo.TABLE_NAME, Folder.TABLE_NAME,
		 Feed.QUERY_TABLE, Item.QUERY_TABLE};
	
	/** An array of all the default sort orders for the
	 *  database tables. The index of each table is
	 *  equal to its flag value
	 *  ({@code DEF_ORDERS[}{@link Undo#FLAG}{@code ] == }
	 *  {@link Undo#DEF_ORDER}). */
	public static final String[] DEF_ORDERS =
		{Undo.DEF_ORDER, Folder.DEF_ORDER,
		 Feed.DEF_ORDER, Item.DEF_ORDER};
	
	/** Specifies the sqlite WHERE clauses used in
	 *  "{@code content://rssdata/table/before/#}" queries.
	 *  The Undo table has no before query, and thus is
	 *  {@code null}.                                */
	public static final String[] BEF_SELECTS =
		{null, Folder.BEF_SELECT,
		 Feed.BEF_SELECT, Item.BEF_SELECT};
	
	
	/** Schema for the undo table.
	 *  @author Mark Lauman     */
	public final static class Undo implements BaseColumns {
		/** Empty constructor. No instances. */
		Undo(){}
		
		/** Internal name of this table. */
		public static final String TABLE_NAME = "undo";
		/** String representing this table's uri. */
		public static final String URI_STR = DBSchema.URI_STR + "/" + TABLE_NAME;
		/** URI for this table. */
		public static final Uri URI = Uri.parse(URI_STR);
		/** Flag used to represent this table internally. */
		public static final int FLAG = 0;
		
		/** Name of this column in the table. */
		public static final String _COMMAND = "command";
		/** Name of this column in the table. */
		public static final String _TABLE_ID = "table_id";
		/** Name of this column in the table. */
		public static final String _ITEM_ID = "item_id";
		
		/** Table creation statement. */
		public static final String CREATE_TABLE
			= "CREATE TABLE " + TABLE_NAME + " ("
				+ _ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
				+ _TABLE_ID + " INTEGER NOT NULL, "
				+ _ITEM_ID + " INTEGER, "
				+ _COMMAND + " TEXT NOT NULL);";
		
		/** Drop table statement. */
		public static final String DROP_TABLE =
				"DROP TABLE IF EXISTS " + TABLE_NAME;
		
		/** Default undo execution order. */
		public static final String DEF_ORDER =
				_TABLE_ID + ", " + _ID;
	}
	
	
	/** Schema for the folder table.
	 *  @author Mark Lauman     */
	public final static class Folder implements BaseColumns {
		/** Empty constructor. No instances. */
		Folder(){}
		
		/** Internal name of this table. */
		public static final String TABLE_NAME = "folder";
		/** MIME type of items in this table. */
		public static final String MIME_TYPE =
				AUTHORITY + "." + TABLE_NAME;
		/** String representing this table's uri. */
		public static final String URI_STR = DBSchema.URI_STR + "/" + TABLE_NAME;
		/** URI for this table. */
		public static final Uri URI = Uri.parse(URI_STR);
		/** Flag used to represent this table internally. */
		public static final int FLAG = 1;
		
		/** Name of this column in the table. */
		public static final String _NAME = "name";
		
		/** Table creation statement. */
		public static final String CREATE_TABLE =
				"CREATE TABLE " + TABLE_NAME + " ("
				+ _ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
				+ _NAME + " TEXT NOT NULL UNIQUE);";
		
		/** Drop table statement. */
		public static final String DROP_TABLE
					= "DROP TABLE IF EXISTS " + TABLE_NAME;
		
		/** Deletion trigger statement. (Called on item delete). */
		public static final String CREATE_TRIGGER_DEL =
				"create trigger del_" + TABLE_NAME
				+ " before delete on " + TABLE_NAME
				+ " begin insert into " + Undo.TABLE_NAME
				+ " values(null, 1, null, "
				+ "'insert into " + TABLE_NAME
				+ " values(' || quote(old." + _ID
				+ ") || ', ' || quote(old." + _NAME
				+ ") || ')'); end;";
		
		/** Statement that removes the deletion trigger. */
		public static final String DROP_TRIGGER_DEL =
				"DROP TRIGGER IF EXISTS del_" + TABLE_NAME;
		
		/** Default sort order for queries. */
		public static final String DEF_ORDER =
				_NAME + ", " + _ID;
		
		/** Specifies the sqlite WHERE clause used in
		 *  "{@code content://rssdata/table/before/#}"
		 *  queries.                                */
		public static final String BEF_SELECT =
				"T1." + _NAME + " < T2." + _NAME
				+ " OR (T1." + _NAME + " = T2." + _NAME
				+ " AND T1." + _ID + " < T2." + _ID + ")";
	}
	
	
	/** Schema for the feed table.
	 *  @author Mark Lauman     */
	public final static class Feed implements BaseColumns {
		/** Empty constructor. No instances. */
		Feed(){}
		
		/** Internal name of this table. */
		public static final String TABLE_NAME = "feed";
		/** MIME type of items in this table. */
		public static final String MIME_TYPE =
				AUTHORITY + "." + TABLE_NAME;
		/** URI for this table. */
		public static final Uri URI = Uri.parse(DBSchema.URI_STR + "/" + TABLE_NAME);
		/** Flag used to represent this table internally. */
		public static final int FLAG = 2;
		
		/** Name of this column in the table. */
		public static final String _NAME = "name";
		/** Name of this column in the table. */
		public static final String _URL = "url";
		// TODO: Thumbnail
		/** Name of this column in the table. */
		public static final String _FOLD_ID = "fold_id";
		/** <p>Name of this column in the table.</p>
		 *  <p><i>Column from the query table only,
		 *  not the actual table.</i></p>         */
		public static final String _FOLD_NAME = "fold_name";
		
		/** Table creation statement. */
		public static final String CREATE_TABLE
			= "create table " + TABLE_NAME + " ("
					+ _ID + " integer primary key autoincrement, "
					+ _URL + " text not null, "
					+ _NAME + " text, "
					+ _FOLD_ID + " integer, "
					+ "foreign key(" + _FOLD_ID
					+ ") references " + Folder.TABLE_NAME
						+ "(" + Folder._ID + ") "
					+ "on delete cascade);";
		
		/** Drop table statement. */
		public static final String DROP_TABLE
					= "DROP TABLE IF EXISTS " + TABLE_NAME;
		
		/** Delete trigger statement. (Called on item delete). */
		public static final String CREATE_TRIGGER_DEL =
				"create trigger del_" + TABLE_NAME
				+ " before delete on " + TABLE_NAME
				+ " begin insert into " + Undo.TABLE_NAME
				+ " values(null, 2, null, "
				+ "'insert into " + TABLE_NAME
				+ " values(' || quote(old." + _ID
				+ ") || ', ' || quote(old." + _URL
				+ ") || ', ' || quote(old." + _NAME
				+ ") || ', ' || quote(old." + _FOLD_ID
				+ ") || ')'); end;";
		
		/** Statement that removes the deletion trigger. */
		public static final String DROP_TRIGGER_DEL =
				"DROP TRIGGER IF EXISTS del_" + TABLE_NAME;
		
		/** Table used for query statements. */
		public static final String QUERY_TABLE =
				"(SELECT "
				+ TABLE_NAME + "." + _ID + " AS " + _ID + ", "
				+ TABLE_NAME + "." + _NAME + " AS " + _NAME + ", "
				+ TABLE_NAME + "." + _URL + " AS " + _URL + ", "
				+ TABLE_NAME + "." + _FOLD_ID + " AS " + _FOLD_ID + ", "
				+ Folder.TABLE_NAME + "." + Folder._NAME
					+ " AS " + Feed._FOLD_NAME
				+ " FROM " + TABLE_NAME
					+ " LEFT OUTER JOIN " + Folder.TABLE_NAME
				+ " ON " + TABLE_NAME + "." + _FOLD_ID
				+ " = " + Folder.TABLE_NAME + "." + Folder._ID
				+ ")";
		
		/** Default sort order for queries. */
		public static final String DEF_ORDER =
				_FOLD_NAME + ", " + _FOLD_ID + ", " + _NAME;
		
		/** Specifies the sqlite WHERE clause used in
		 *  "{@code content://rssdata/table/before/#}"
		 *  queries.                                */
		public static final String BEF_SELECT =
				"T1." + _FOLD_NAME + " < T2." + _FOLD_NAME
				+ " OR (T1." + _FOLD_NAME + " = T2." + _FOLD_NAME
				+ " AND (T1." + _FOLD_ID + " < T2." + _FOLD_ID
					+ " OR (T1." + _FOLD_ID + " = T2." + _FOLD_ID
					+ " AND T1." + _NAME + " < T2." + _NAME
				+ ")))";
	}
	
	
	/** Schema for the item table.
	 *  @author Mark Lauman     */
	public final static class Item implements BaseColumns {
		/** Empty constructor. No instances. */
		Item(){}
		
		/** Internal name of this table. */
		public static final String TABLE_NAME = "item";
		/** MIME type of items in this table. */
		public static final String MIME_TYPE =
				AUTHORITY + "." + TABLE_NAME;
		/** URI for this table. */
		public static final Uri URI =
				Uri.parse(DBSchema.URI_STR + "/" + TABLE_NAME);
		/** Flag used to represent this table internally. */
		public static final int FLAG = 3;
		
		/** Name of this column in the table. */
		public static final String _TITLE = "title";
		/** Name of this column in the table. */
		public static final String _DESC = "desc";
		/** Name of this column in the table. */
		public static final String _LINK = "link";
		/** Name of this column in the table. */
		public static final String _TIME = "time";
		/** Name of this column in the table. */
		public static final String _SAVE_TIME = "save_time";
		/** Name of this column in the table. */
		public static final String _FEED_ID = "feed_id";
		/** Name of this column in the table. */
		public static final String _FOLD_ID = "fold_id";
		// TODO: Thumbnail
		/** <p>Name of this column in the table.</p>
		 *  <p><i>Column from the query table only,
		 *  not the actual table.</i></p>         */
		public static final String _FEED_NAME = "feed_name";
		
		/** Table creation statement. */
		public static final String CREATE_TABLE =
				"create table " + TABLE_NAME + " ("
				+ _ID + " integer primary key autoincrement, "
				+ _TITLE + " text, "
				+ _DESC + " text, "
				+ _LINK + " text, "
				+ _TIME + " integer, "
				+ _SAVE_TIME + ", "
				+ _FEED_ID + " integer not null, "
				+ "foreign key(" + _FEED_ID
				+ ") references " + Feed.TABLE_NAME
					+ "(" + Feed._ID + ") "
				+ "on delete cascade, "
				+ "unique(" + _TITLE + ", "
				+ _DESC + ", " + _LINK + ", "
				+ _FEED_ID
				+ ") on conflict ignore);";
		
		/** Drop table statement. */
		public static final String DROP_TABLE =
				"DROP TABLE IF EXISTS " + TABLE_NAME;
		
		/** Deletion trigger statement. (Called on item delete). */
		public static final String CREATE_TRIGGER_DEL =
				"create trigger del_" + TABLE_NAME
				+ " before delete on " + TABLE_NAME
				+ " begin insert into " + Undo.TABLE_NAME
				+ " values(null, 3, null, "
				+ "'insert into " + TABLE_NAME
				+ " values(' || quote(old." + _ID
				+ ") || ', ' || quote(old." + _TITLE
				+ ") || ', ' || quote(old." + _DESC
				+ ") || ', ' || quote(old." + _LINK
				+ ") || ', ' || quote(old." + _TIME
				+ ") || ', ' || quote(old." + _SAVE_TIME
				+ ") || ', ' || quote(old." + _FEED_ID
				+ ") || ')'); end;";
		
		/** Statement that removes the deletion trigger. */
		public static final String DROP_TRIGGER_DEL =
				"DROP TRIGGER IF EXISTS del_" + TABLE_NAME;
		
		/** Table used for query statements. */
		public static final String QUERY_TABLE =
				"(SELECT "
				+ TABLE_NAME + "." + _ID + " AS " + _ID + ", "
				+ TABLE_NAME + "." + _TITLE + " AS " + _TITLE + ", "
				+ TABLE_NAME + "." + _DESC + " AS " + _DESC + ", "
				+ TABLE_NAME + "." + _LINK + " AS " + _LINK + ", "
				+ "coalesce("
					+ TABLE_NAME + "." + _TIME + ", "
					+ TABLE_NAME + "." + _SAVE_TIME
				+ ") AS " + _TIME + ", "
				+ TABLE_NAME + "." + _FEED_ID + " AS " + _FEED_ID + ", "
				+ Feed.TABLE_NAME + "." + Feed._NAME + " AS " + _FEED_NAME + ", "
				+ Feed.TABLE_NAME + "." + Feed._FOLD_ID + " AS " + _FOLD_ID
				+ " FROM " + TABLE_NAME
					+ " JOIN " + Feed.TABLE_NAME
				+ " ON " + TABLE_NAME + "." + _FEED_ID
				+ " = " + Feed.TABLE_NAME + "." + Feed._ID
				+ ")";
		
		/** Default sort order for queries. */
		public static final String DEF_ORDER =
				Item._TIME + " desc, " + Item._TITLE;
		
		/** Specifies the sqlite WHERE clause used in
		 *  "{@code content://rssdata/table/before/#}"
		 *  queries.                                */
		public static final String BEF_SELECT =
				
				"T1." + _TIME + " > T2." + _TIME
				+ " OR (T1." + _TIME + " = T2." + _TIME
				+ " AND T1." + _TITLE + " < T2." + _TITLE + ")";
	}
}