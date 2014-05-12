package ca.marklauman.rssreader.database;

import java.util.Calendar;
import java.util.Map.Entry;
import java.util.Set;

import ca.marklauman.rssreader.database.schema.Database;
import ca.marklauman.rssreader.database.schema.Item;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
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
		// do not allow null rows
		if(values == null || values.size() < 1)
			return null;
		
		/* Match the uri to a table and do any
		 * pre-insertion operations.        */
		String table;
		int uri_type = matcher.match(uri);
		switch(uri_type) {
		case ITEM_ID:
			table = Item.TABLE_NAME;
			String content = values.getAsString(Item._CONTENT);
			if(content == null) {
				values.put(Item._CONTENT, "");
				content = "";
			}
			if(!values.containsKey(Item._BRIEF)) {
				String brief = stripHTML(content);
				if(brief.length() > 1000)
					brief = brief.substring(0, 999) + "…";
				values.put(Item._BRIEF, brief);
			}
			if(!values.containsKey(Item._TIME_SAVE))
				values.put(Item._TIME_SAVE,
						   Calendar.getInstance()
								   .getTimeInMillis());
			break;
		default:
			return null;
		}
		
		/* do the insertion */
		SQLiteDatabase db = cache_db.getWritableDatabase();
		long id;
		try {
			/* Insertion will fail if the db is
			 * destroyed in another thread (such
			 * as when the user clears the app data
			 * in the Android settings screen)     */
			id = db.insertWithOnConflict(table,
										 null, values,
										 SQLiteDatabase.CONFLICT_IGNORE);
		} catch (Exception e) {
			// reopen the database and redo the insert
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
		
		/* In the event of a conflict, look for an
		 * existing row with the same values.
		 * If one exists, we'll return it as the
		 * inserted row.                        */
		if(id < 0) {
			values.remove(Item._TIME_SAVE);
			Set<Entry<String, Object>> vals = values.valueSet();
			String sel = "";
			String[] selArgs = new String[vals.size()];
			int i = 0;
			for(Entry<String, Object> entry : vals) {
				sel += " AND "+ entry.getKey() + "=?";
				selArgs[i] = "" + entry.getValue();
				i++;
			}
			if(sel.length() > 0)
				sel = sel.substring(5);
			
			Cursor c = db.query(table,
								new String[]{"_id"},
								sel, selArgs,
								null, null, null);
			
			if(0 < c.getCount()) {
				int id_col = c.getColumnIndex("_id");
				c.moveToFirst();
				id = c.getLong(id_col);
			}
			c = null;
		}
		
		/* Post-insertion operations. */
		if(id < 0) return null;
		switch(uri_type) {
		case ITEM_ID:
			// update the save time of inserted Items
			ContentValues new_time = new ContentValues();
			long now = Calendar.getInstance()
							   .getTimeInMillis();
			new_time.put(Item._TIME_INSERT, now);
			db.update(table, new_time,
					  "_id=?", new String[]{id+""});
		}
		
		/* Return the entry's url. */
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
	
	
	/** Strip all html code from a string.
	 *  @param htmlString A string with html
	 *  formatting.
	 *  @return That string will all such formatting
	 *  removed.                                  */
	private static String stripHTML(String htmlString) {
		return Html.fromHtml(htmlString)
				   .toString()
				   .replace(""+(char)65532, "")
				   .replace("\n", " ")
				   .replace("\r", " ")
				   .replace("  ", " ")
				   .replace("  ", " ")
				   .trim();
	}
}