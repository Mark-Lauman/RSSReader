package ca.marklauman.rssreader.database;

import java.util.Calendar;

import ca.marklauman.rssreader.database.schema.Database;
import ca.marklauman.rssreader.database.schema.Item;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.text.Html;

public class RSSData extends ContentProvider {
	
	/** Resource ID for the item table */
	private static final int ITEM_ID = 1;
	/** Matches uri's to their resource ids */
	private UriMatcher matcher;
	/** Database in the cache directory.
	 *  (contains the Items table)    */
	DBHandler cache_db;
	
	
	@Override
	public boolean onCreate() {
		matcher = new UriMatcher(UriMatcher.NO_MATCH);
		matcher.addURI(Database.AUTHORITY,
					   Item.TABLE_NAME,
					   ITEM_ID);
		Context c = getContext();
		String db_name = c.getCacheDir().getPath() + "/" + Database.NAME;
		cache_db = new DBHandler(c, db_name);
		return true;
	}
	
	
	@Override
	public String getType(Uri uri) {
		switch(matcher.match(uri)) {
		case ITEM_ID:
			return Database.MIME_GROUP + Item.MIME_TYPE;
		}
		return null;
	}
	
	
	@Override
	public Uri insert(Uri uri, ContentValues values) {
		String table;
		switch(matcher.match(uri)) {
		case ITEM_ID:
			table = Item.TABLE_NAME;
			long now = Calendar.getInstance()
					   		   .getTimeInMillis();
			values.put(Item._TIME_SAVE, now);
			String brief = Html.fromHtml(values.getAsString(Item._CONTENT))
					   	   .toString()
					   	   .replace(""+(char)65532, "")
					   	   .replace("\n", " ")
					   	   .replace("\r", " ")
					   	   .replace("  ", " ")
					   	   .replace("  ", " ")
					   	   .trim();
			if(brief.length() > 1000)
				brief = brief.substring(0, 999) + "…";
			values.put(Item._BRIEF, brief);
			break;
		default:
			return null;
		}
		
		// do the insertion
		SQLiteDatabase db = cache_db.getWritableDatabase();
		long id;
		try {
			id = db.insertWithOnConflict(table,
										 null, values,
										 SQLiteDatabase.CONFLICT_IGNORE);
		} catch (Exception e) {
			/* Insertion will fail if the db is
			 * destroyed in another thread (such
			 * as when the user clears the cache data
			 * in the Android Settings screen)     */
			try{cache_db.close();
			} catch(Exception e2) {}
			Context c = getContext();
			String db_name = c.getCacheDir().getPath() + "/" + Database.NAME;
			cache_db = new DBHandler(c, db_name);
			db = cache_db.getWritableDatabase();
			id = db.insertWithOnConflict(table,
										 null, values,
										 SQLiteDatabase.CONFLICT_IGNORE);
		}
		if(id < 0) return null;
		getContext().getContentResolver()
					.notifyChange(uri, null);
		return Uri.withAppendedPath(uri, "" + id);
	}
	
	
	@Override
	public int update(Uri uri, ContentValues values,
			String selection, String[] selectionArgs) {
		String table;
		switch(matcher.match(uri)) {
		case ITEM_ID:
			table = Item.TABLE_NAME;
			break;
		default:
			return 0;
		}
		
		SQLiteDatabase db = cache_db.getWritableDatabase();
		int rows = db.update(table, values,
							 selection, selectionArgs);
		if(0 < rows)
			getContext().getContentResolver()
						.notifyChange(uri, null);
		return rows;
	}
	
	
	@Override
	public Cursor query(Uri uri, String[] projection,
			String selection, String[] selectionArgs,
			String sortOrder) {
		SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
		switch(matcher.match(uri)) {
		case ITEM_ID:
			qb.setTables(Item.TABLE_NAME); break;
		default:
			return null;
		}
		
		SQLiteDatabase db = cache_db.getReadableDatabase();
		Cursor c = qb.query(db, projection,
							selection, selectionArgs,
							null, null, sortOrder);
		c.setNotificationUri(getContext().getContentResolver(), uri);
		return c;
	}
	
	
	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		String table;
		switch(matcher.match(uri)) {
		case ITEM_ID:
			table = Item.TABLE_NAME;
			break;
		default:
			return 0;
		}
		
		SQLiteDatabase db = cache_db.getWritableDatabase();
		int rows = db.delete(table, selection, selectionArgs);
		if(0 < rows)
			getContext().getContentResolver()
						.notifyChange(uri, null);
		return rows;
	}
	
	
	/** Handler class for the SQLite Database.
	 *  The {@link RSSData} class acts as a wrapper to
	 *  this one.
	 *  @author Mark Lauman                           */
	private static class DBHandler extends SQLiteOpenHelper {
		
		/** Create a link to the database.
		 *  @param c The application {@link Context}.
		 *  @param filename The file to use for the db */
		public DBHandler(Context c, String filename) {
			super(c, filename, null, Database.VERSION);
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
			db.execSQL(Item.CREATE_TABLE);
		}
		
		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			// remove the tables
			db.execSQL(Item.DROP_TABLE);
			
			// recreate everything
			onCreate(db);
		}
	}
}