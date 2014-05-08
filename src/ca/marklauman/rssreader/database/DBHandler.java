package ca.marklauman.rssreader.database;

import ca.marklauman.rssreader.database.schema.Database;
import ca.marklauman.rssreader.database.schema.Item;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/** Handler class for the SQLite Database.
 *  The {@link RSSData} class acts as a wrapper to
 *  this one.
 *  @author Mark Lauman                           */
public class DBHandler extends SQLiteOpenHelper {
	
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
