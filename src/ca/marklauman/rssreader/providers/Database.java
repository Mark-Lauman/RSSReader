package ca.marklauman.rssreader.providers;

import java.util.Calendar;
import java.util.Map.Entry;

import ca.marklauman.rssreader.providers.DBSchema.Feed;
import ca.marklauman.rssreader.providers.DBSchema.Item;
import ca.marklauman.rssreader.providers.DBSchema.Folder;
import ca.marklauman.rssreader.providers.DBSchema.Undo;
import ca.marklauman.tools.Debug;
import ca.marklauman.tools.Tools;
import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.util.Log;

public class Database extends ContentProvider
					  implements Debug {
	
	/** Flag for a row retrieval URI.  */
	private static final int TYPE_ROW = 0;
	/** Flag for a "before row" URI.  */
	private static final int TYPE_BEF = 1;
	/** Flag for an "after row" URI.  */
	private static final int TYPE_AFT = 2;
	
	
	/** Direct handle to the database.     */
	private DBHandler dbhandle;
	/** Matches the uri to a particular table */
	private static final UriMatcher table_match;
	/** Matches the uri to a particular query type */
	private static final UriMatcher type_match;
	
	
	static {
		table_match = new UriMatcher(UriMatcher.NO_MATCH);
		table_match.addURI(DBSchema.AUTHORITY, Undo.TABLE_NAME, Undo.FLAG);
		addTable(table_match, Folder.TABLE_NAME, Folder.FLAG);
		addTable(table_match, Feed.TABLE_NAME, Feed.FLAG);
		addTable(table_match, Item.TABLE_NAME, Item.FLAG);
		
		type_match = new UriMatcher(UriMatcher.NO_MATCH);
		type_match.addURI(DBSchema.AUTHORITY, "*/#", TYPE_ROW);
		type_match.addURI(DBSchema.AUTHORITY, "*/before/#", TYPE_BEF);
		type_match.addURI(DBSchema.AUTHORITY, "*/after/#", TYPE_AFT);
	}
	
	
	/** Adds a table to the provided {@link UriMatcher}.
	 *  The matcher will match
	 *  "authority/tablename",
	 *  "authority/tablename/*",
	 *  and "authority/tablename/*&#47;*".
	 *  @param matcher The {@link UriMatcher} to add to.
	 *  @param tablename The name of the table to add.
	 *  @param id The id to return for a match to
	 *  this table.                            */
	private static void addTable(UriMatcher matcher,
			String tablename, int id) {
		matcher.addURI(DBSchema.AUTHORITY, tablename, id);
		matcher.addURI(DBSchema.AUTHORITY, tablename + "/*", id);
		matcher.addURI(DBSchema.AUTHORITY, tablename + "/*/*", id);
	}
	
	
	@Override
	public boolean onCreate() {
		dbhandle = new DBHandler(getContext());
		return true;
	}
	
	
	@Override
	public String getType(Uri uri) {
		String table_mime = "";
		switch(table_match.match(uri)) {
		case Folder.FLAG:
			table_mime = Folder.MIME_TYPE;	break;
		case Feed.FLAG:
			table_mime = Feed.MIME_TYPE;	break;
		case Item.FLAG:
			table_mime = Item.MIME_TYPE;	break;
		default:
			return null;
		}
		
		switch(type_match.match(uri)) {
		case TYPE_ROW:
			return DBSchema.MIME_SINGLE + table_mime;
		default:
			return DBSchema.MIME_GROUP + table_mime;
		}
	}
	
	
	@Override
	public Uri insert(Uri uri, ContentValues values) {
		// Input check
		if(uri == null) throw new IllegalArgumentException("uri parameter is null");
		if(values == null) throw new IllegalArgumentException("values parameter is null");
//		msg("Insert");
//		printValues(values);
		
		// Match uri to table name
		String table;
		switch(table_match.match(uri)) {
			case Folder.FLAG:
				table = Folder.TABLE_NAME;	break;
			case Feed.FLAG:
				table = Feed.TABLE_NAME;	break;
			case Item.FLAG:
				table = Item.TABLE_NAME;
				long now = Calendar.getInstance()
								   .getTimeInMillis();
				values.put(Item._SAVE_TIME, now);
				break;
			default:
				return null;
		}
//		msg("       into " + table + ": " + values);
		
		// do database insertion
		SQLiteDatabase db = dbhandle.getWritableDatabase();
		long id = -1;
		try {
			id = db.insertWithOnConflict(table, null,
							values, SQLiteDatabase.CONFLICT_IGNORE);
		} catch(Exception e) {}
		if(id < 0) return null;
		
		// cleanup
		notifyChange(uri);
//		msg("       id = " + id);
		return ContentUris.withAppendedId(uri, id);
	}
	
	
	@Override
	public int update(Uri uri, ContentValues values,
				String selection, String[] selectionArgs) {
		String table;
		switch(table_match.match(uri)) {
		case Folder.FLAG:
			table = Folder.TABLE_NAME;	break;
		case Feed.FLAG:
			table = Feed.TABLE_NAME;	break;
		default: return 0;
		}
		
		SQLiteDatabase db = dbhandle.getWritableDatabase();
		int res = db.update(table, values, selection, selectionArgs);
		if(res > 0)
			notifyChange(uri);
		return res;
	}
	
	
	@Override
	public Cursor query(Uri uri, String[] projection,
				String selection, String[] selectionArgs,
				String sortOrder) {
		msg("query(" + uri + ",");
		msg("      " + Tools.arrToStr(projection) + ",");
		msg("      " + selection + ", " + Tools.arrToStr(selectionArgs) + ",");
		msg("      " + sortOrder + ")");
		
		
		// match the table, set default sort order
		int table_flag = table_match.match(uri);
		if(table_flag == -1) return null;
		if(table_flag == Undo.FLAG) {
			msg("  table=\"undo\"");
			doUndo();
			return null;
		}
		String table = DBSchema.QUERY_TABLES[table_flag];
		if(sortOrder == null)
			sortOrder = DBSchema.DEF_ORDERS[table_flag];
		
		
		SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
		
		
		// match the type of query
		switch(type_match.match(uri)) {
		case TYPE_ROW:
			msg("  type=row");
			selection = "_id=?";
			selectionArgs = new String[]{uri.getLastPathSegment()};
			break;
		case TYPE_BEF:
			msg("  before row=" + uri.getLastPathSegment());
			projection = new String[]{"T1.*", "T2._id AS t2_id"};
			table =  table + " AS T1 LEFT OUTER JOIN "
					 	  + "(SELECT * FROM "
					 	  + table + " WHERE _id="
					 	  + uri.getLastPathSegment()
					 	  + ") AS T2";
			selection = DBSchema.BEF_SELECTS[table_flag];
			selectionArgs = null;
			sortOrder = DBSchema.DEF_ORDERS[table_flag];
			break;
		case TYPE_AFT:
			msg("  after row=" + uri.getLastPathSegment());
			return null;
//			break;
		}
		msg("  table=\"" + table + "\"");
		qb.setTables(table);
		
		
		// do the query
		SQLiteDatabase db = dbhandle.getReadableDatabase();
		Cursor c = qb.query(db, projection, selection, selectionArgs, null, null, sortOrder);
		c.setNotificationUri(getContext().getContentResolver(), uri);
		msg("  res_size=" + c.getCount());
		msg(DatabaseUtils.dumpCursorToString(c));
		return c;
	}
	
	
	
	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		msg("delete("
				+ uri.toString() + ",");
		msg("       \""
				+ selection + "\", "
				+ Tools.arrToStr(selectionArgs) + ")");
		
		SQLiteDatabase db = dbhandle.getWritableDatabase();
		String table;
		switch(table_match.match(uri)) {
			case Folder.FLAG:
				table = Folder.TABLE_NAME;	break;
			case Feed.FLAG:
				table = Feed.TABLE_NAME;	break;
			default: return 0;
		}
		
		clearUndo(db);
		
		msg("       matches " + table);
		int count = db.delete(table, selection, selectionArgs);
		msg("       done. (" + count + " deleted)");
		if(0 < count)
			notifyChange(uri);
		return count;
	}
	
	
	/** Notify registered observers that a row was
	 *  updated and attempt to sync changes to the network.
	 *  By default, CursorAdapter objects will
	 *  get this notification.
	 *  @param uri  The uri of the table that
	 *  was changed.                                     */
	private void notifyChange(Uri uri) {
		msg("notifyChange(" + uri.toString() + ")");
		getContext().getContentResolver()
					.notifyChange(uri, null);
	}
	
	
	/** Clears the undo log.
	 *  @param db The database containing the Undo table. */
	private void clearUndo(SQLiteDatabase db) {
		db.execSQL(Undo.DROP_TABLE);
		db.execSQL(Undo.CREATE_TABLE);
	}
	
	
	/** Does the undo operation and clears the undo table.
	 *  Affected groups will be notified.               */
	private void doUndo() {
		SQLiteDatabase db = dbhandle.getWritableDatabase();
		
		SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
		qb.setTables(Undo.TABLE_NAME);
		Cursor c = qb.query(db, null, null, null, null, null, Undo.DEF_ORDER);
		if(c == null || c.getCount() == 0)
			return;
		c.moveToFirst();
		String ins;
		do {
			ins = c.getString(c.getColumnIndex(Undo._COMMAND));
			try {
				db.execSQL(ins);
				Uri update = null;
				switch(c.getInt(c.getColumnIndex(Undo._TABLE_ID))) {
					case Folder.FLAG:
						update = Folder.URI;	break;
					case Feed.FLAG:
						update = Feed.URI;		break;
				}
				notifyChange(update);
			} catch(Exception e) {
				Log.e("SQL Error", "" + e.getLocalizedMessage());
			}
		} while(c.moveToNext());
	}
	
	
	/** Handler class for the SQLite Database.
	 *  The {@link Database} class acts as a wrapper to
	 *  this one.
	 *  @author Mark Lauman                           */
	private static class DBHandler extends SQLiteOpenHelper {
		
		/** Create a link to the database.
		 *  @param c The application {@link Context}. */
		public DBHandler(Context c) {
			super(c, DBSchema.DB_NAME, null, DBSchema.DB_VERSION);
		}
		
		@Override
		public void onOpen(SQLiteDatabase db) {
			super.onOpen(db);
			if (!db.isReadOnly())
				db.execSQL("PRAGMA foreign_keys=ON;");
		}
		
		@Override
		public void onCreate(SQLiteDatabase db) {
			// create the tables
			db.execSQL(Undo.CREATE_TABLE);
			db.execSQL(Folder.CREATE_TABLE);
			db.execSQL(Feed.CREATE_TABLE);
			db.execSQL(Item.CREATE_TABLE);
			
			// create the triggers
			db.execSQL(Folder.CREATE_TRIGGER_DEL);
			db.execSQL(Feed.CREATE_TRIGGER_DEL);
			db.execSQL(Item.CREATE_TRIGGER_DEL);
		}
		
		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			// remove the triggers
			db.execSQL(Item.DROP_TRIGGER_DEL);
			db.execSQL(Feed.DROP_TRIGGER_DEL);
			db.execSQL(Folder.DROP_TRIGGER_DEL);
			
			// remove the tables
			db.execSQL(Item.DROP_TABLE);
			db.execSQL(Feed.DROP_TABLE);
			db.execSQL(Folder.DROP_TABLE);
			db.execSQL(Undo.DROP_TABLE);
			
			// recreate everything
			onCreate(db);
		}
	}

	@Override
	public void msg(String msg) {
		Log.d("RSSDatabase", msg);
	}
	
	@Deprecated
	public static void printValues(ContentValues v) {
		Log.d("RSSDatabase", "============================ContentValue===========================");
		for(Entry<String, Object> e : v.valueSet()) {
			Log.d(e.getKey(), "" + e.getValue());
		}
	}
}