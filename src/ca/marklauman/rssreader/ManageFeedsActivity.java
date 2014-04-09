package ca.marklauman.rssreader;

import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.app.NavUtils;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.Toast;

import ca.marklauman.rssreader.adapters.FeedAdapter;
import ca.marklauman.rssreader.dialogs.NewFeedDialog;
import ca.marklauman.rssreader.panels.PopupPanel;
import ca.marklauman.rssreader.panels.PopupPanel.UndoListener;
import ca.marklauman.rssreader.providers.DBSchema;
import ca.marklauman.rssreader.providers.DBSchema.Feed;
import ca.marklauman.rssreader.providers.DBSchema.Undo;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.ActionMode;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;

public class ManageFeedsActivity extends SherlockFragmentActivity
								 implements OnItemClickListener,
								 			OnItemLongClickListener,
								 			LoaderCallbacks<Cursor>,
								 			UndoListener,
								 			ActionMode.Callback {
	
	/** Id for the undo loader */
	public static final int LOAD_UNDO = 9001;
	/** Id for the feed loader */
	public static final int LOAD_FEED = 28;
	
	/** The list displaying the feeds. */
	private ListView list;
	/** The adapter for the list. */
	private FeedAdapter adapter;
	/** The popup panel. */
	private PopupPanel popup;
	/** The context menu is stored here if it is active. */
	ActionMode context_menu = null;
	
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_feed_mng);
		getSupportActionBar()
			.setDisplayHomeAsUpEnabled(true);
		
		list = (ListView) findViewById(R.id.feed_list);
		list.setOnItemClickListener(this);
		list.setOnItemLongClickListener(this);
		
		popup = PopupPanel.getPanel(getSupportFragmentManager());
		popup.setUndoListener(this);
		adapter = new FeedAdapter(this);
		list.setAdapter(adapter);
		
		LoaderManager lm = getSupportLoaderManager();
		lm.initLoader(LOAD_FEED, null, this);
	}
	
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getSupportMenuInflater().inflate(R.menu.manage_feeds, menu);
		return true;
	}
	
	
	@Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        // android "back" button on the action bar
        case android.R.id.home:
            NavUtils.navigateUpFromSameTask(this);
            return true;
            
        case R.id.action_new_feed:
        	NewFeedDialog dialog = new NewFeedDialog();
        	dialog.setPopupPanel(popup);
        	dialog.show(getSupportFragmentManager(), null);
			return true;
        }
        
        return super.onOptionsItemSelected(item);
	}
	
	
	@Override
	public void onItemClick(AdapterView<?> parent,
			View view, int position, long id) {
		if(context_menu == null) {
			// TODO: Make this do something
			Toast.makeText(this, "EDIT FEED #" + position + "\nComing Soon!", Toast.LENGTH_SHORT).show();
			return;
		}
		
		adapter.toggleItem(position);
		context_menu.invalidate();
	}
	
	
	@Override
	public boolean onItemLongClick(AdapterView<?> parent,
			View view, int position, long id) {
		if(context_menu == null) {
			list.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
			adapter.setChoiceMode(FeedAdapter.CHOICE_MODE_MULTIPLE);
			context_menu = startActionMode(this);
		}
		onItemClick((ListView) parent, view, position, id);
		return true;
	}
	
	
	@Override
	public void onUndo() {
		getSupportLoaderManager()
			.restartLoader(LOAD_UNDO, null, this);
	}
	
	
	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
		CursorLoader c = new CursorLoader(this);
		
		switch(id) {
		// Triggers an undo operation
		case LOAD_UNDO:
			c.setUri(Undo.URI);
			return c;
			
		case LOAD_FEED:
			c.setUri(Feed.URI);
			adapter.changeCursor(null);
			return c;
		}
		return null;
	}
	
	
	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
		switch(loader.getId()) {
		case LOAD_FEED:
			adapter.changeCursor(data);
		}
	}
	
	
	@Override
	public void onLoaderReset(Loader<Cursor> loader) {
		if(loader.getId() == LOAD_FEED)
			adapter.changeCursor(null);
	}
	
	
	@Override
	public boolean onCreateActionMode(ActionMode mode, Menu menu) {
		mode.getMenuInflater().inflate(R.menu.context_feed, menu);
		return true;
	}
	
	
	@Override
	public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
		// get the selected items
		long[] selections = adapter.getSelections();
		// get the menu icons
		MenuItem move = menu.findItem(R.id.context_move_feed);
		MenuItem delete = menu.findItem(R.id.context_del);
				
		// hide or show icons based off of # of selections
		switch(selections.length) {
		case 0: // no feeds selected
			move.setVisible(false);
			delete.setVisible(false);
			break;
		case 1: // 1 feed selected
			move.setVisible(true);
			delete.setVisible(true);
			break;
		default: // many feeds selected
			move.setVisible(true);
			delete.setVisible(true);
			break;
		}
		return true;
	}
	
	
	@Override
	public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
		switch (item.getItemId()) {
			
		// Move 1+ feeds to a folder
		case R.id.context_move_feed:
			// TODO: Make this do something
			Toast.makeText(this, "TRANSFER TO FOLDER\nComing Soon!", Toast.LENGTH_SHORT).show();
			context_menu.finish();
			return true;
		
		// Delete 1+ feeds
		case R.id.context_del:
			// setup variables
			long[] selections = adapter.getSelections();
			String where_1feed = " or " + Feed._ID + "=?";
			// SQL where query, with a "?" where the
			// parameters should be.
			String where = "";
			String[] params = new String[selections.length];
			
			// poulate the sql where statement
			for(int i = 0; i < params.length; i++) {
				where += where_1feed;
				params[i] = "" + selections[i];
			}
			where = where.substring(4);
			
			// do the delete
			int deleted = getContentResolver()
								.delete(DBSchema.Feed.URI,
										where, params);
			
			// cleanup, display result message
			String msg;
			if(deleted < 1) {
				// Deletion failed
				msg = getResources().getString(R.string.del_feed_0);
				Toast.makeText(this, msg, Toast.LENGTH_SHORT)
					 .show();
				context_menu.finish();
				return true;
			} else if(deleted == 1)
				msg = getResources().getString(R.string.del_feed_1);
			else
				msg = getResources().getString(R.string.del_feed_many);
			popup.showUndo(deleted + " " + msg, false);
			context_menu.finish();
			return true;
		}
		return false;
	}
	
	
	@Override
	public void onDestroyActionMode(ActionMode mode) {
		context_menu = null;
		list.setChoiceMode(ListView.CHOICE_MODE_NONE);
		adapter.setChoiceMode(FeedAdapter.CHOICE_MODE_NONE);
	}
}