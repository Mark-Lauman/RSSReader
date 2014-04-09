package ca.marklauman.rssreader;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;


import ca.marklauman.rssreader.dialogs.NewFolderDialog;
import ca.marklauman.rssreader.panels.ItemPanel;
import ca.marklauman.rssreader.panels.FolderPanel;
import ca.marklauman.rssreader.panels.FolderPanel.FolderListener;
import ca.marklauman.rssreader.panels.ProgressPanel;
import ca.marklauman.rssreader.panels.PopupPanel;
import ca.marklauman.rssreader.panels.PopupPanel.UndoListener;
import ca.marklauman.rssreader.providers.DBSchema;
import ca.marklauman.rssreader.providers.DBSchema.Folder;
import ca.marklauman.rssreader.providers.DBSchema.Item;
import ca.marklauman.rssreader.providers.DBSchema.Undo;
import ca.marklauman.rssreader.providers.DBService;
import ca.marklauman.rssreader.providers.Sample;
import ca.marklauman.tools.Debug;
import ca.marklauman.tools.Tools;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentManager.OnBackStackChangedListener;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.LoaderManager;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

public class MainActivity extends SherlockFragmentActivity
						  implements LoaderCallbacks<Cursor>,
						  			 OnBackStackChangedListener,
						  			 FolderListener,
						  			 UndoListener,
						  			 Debug {
	
	public static final int LOAD_UNDO = 9001;
	public static final int LOAD_FOLD_PANEL	= 0;
	public static final int LOAD_ITEM_PANEL	= 1;
	public static final int NUM_LOADERS		= 2;
	// TODO: REMOVE
	public static final int LOAD_POKE = 9002;
	
	/** Bundle key for selections. (Used when starting
	 * a loader and when saving last_select)       */
	public static final String KEY_SELECT	= "LOAD_KEY_SELECT";
	/** savedInstanceState key for the loaders' parameters. */
	public static final String KEY_LOADER_PARAMS = "LOADER PARAMS";
	
	/** The last set of parameters passed to each loader */
	private Bundle[] loader_params;
	/** Last folder id selected.
	 *  (used for restore from savedInstanceState) */
	private long last_select;
	
	private FolderPanel folder_panel;
	private ItemPanel feed_panel;
	private ProgressPanel prog_panel;
	View prog_div;
	private PopupPanel popup_panel;
	private DownloadReceiver prog_receiver;
	

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		msg("onCreate()");
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		getSupportFragmentManager()
				.addOnBackStackChangedListener(this);
		
		
		// Load the active loaders from the last instance
		// For a first boot, none of the loaders are active.
		last_select = 0L;
		loader_params = null;
		if(savedInstanceState != null) {
			last_select = savedInstanceState.getLong(KEY_SELECT);
			loader_params = (Bundle[]) savedInstanceState.getParcelableArray(KEY_LOADER_PARAMS);
		}
		if(last_select == 0) {
			loader_params = new Bundle[NUM_LOADERS];
			for(int i = 0; i < NUM_LOADERS; i++) {
				loader_params[i] = null;
			}
		}
		
		IntentFilter filter = new IntentFilter(DBService.BROADCAST_ACT);
        filter.addCategory(Intent.CATEGORY_DEFAULT);
		prog_receiver = new DownloadReceiver();
        registerReceiver(prog_receiver, filter);
	}
	
	@Override
	public void onSaveInstanceState(Bundle outState) {
		outState.putLong(KEY_SELECT, last_select);
		outState.putParcelableArray(KEY_LOADER_PARAMS, loader_params);
		super.onSaveInstanceState(outState);
	}
	
	
	@Override
	public void onStart() {
		msg("onStart()");
		getPanels();
		restartLoaders();
		super.onStart();
	}
	
	@Override
	public void onDestroy() {
		this.unregisterReceiver(prog_receiver);
		super.onDestroy();
	}
	
	
	@Override
	public void onFolderSelected(boolean swap, long... ids) {
		msg("onFolderSelected("
					+ swap + ", " + Tools.arrToStr(ids) + ")");
		
		// remember this selection (if not a multi-select)
		switch(ids.length) {
		case 0: last_select = 0; break;
		case 1: last_select = ids[0]; break;
		}
		
		// create the feed panel if necessary (1 panel views)
		if(feed_panel == null)
			feed_panel = new ItemPanel();
		
		// display the feed panel if necessary (1 panel views)
		FragmentManager fm = getSupportFragmentManager();
		if(fm.findFragmentById(R.id.swap_panel) != null
				&& swap) {
			msg("                 make transaction");
			FragmentTransaction transaction = fm.beginTransaction();
			transaction.replace(R.id.swap_panel, feed_panel, ItemPanel.PANEL_TAG);
			transaction.addToBackStack(null);
			msg("                 commit transaction");
			transaction.commit();
		}
		
		Bundle args = new Bundle();
		args.putLongArray(KEY_SELECT, ids);
		getSupportLoaderManager().restartLoader(LOAD_ITEM_PANEL, args, this);
	}
	
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getSupportMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
	
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		
		// "Back" button on the action bar
		case android.R.id.home:
			getSupportFragmentManager().popBackStack();
            return true;
		
        // Refresh all feeds
		case R.id.action_refresh:
			String start = getResources().getString(R.string.download_start);
			prog_panel.setProgress(start, 0);
			prog_div.setVisibility(View.VISIBLE);
			prog_panel.getView()
					  .setVisibility(View.VISIBLE);
			DBService.refresh(this, null);
			return true;
		
		// Open the new folder dialog
		case R.id.action_new_fold:
			NewFolderDialog dialog = new NewFolderDialog();
			dialog.show(getSupportFragmentManager(), null);
			return true;
		
		// Toggle manage feeds activity
		case R.id.action_mng_feed:
			Intent intent = new Intent(this, ManageFeedsActivity.class);
			startActivity(intent);
			return true;
		
		// Open settings screen
		case R.id.action_settings:
			// TODO: Make this do something
			Toast.makeText(this, "SETTINGS SCREEN\nComing Soon!", Toast.LENGTH_SHORT).show();
			return true;
		
		// TODO: REMOVE SAMPLE GENERATION
		case R.id.action_make_samples:
			Sample.makefolders(this);
			Sample.makeLinkedFeeds(this);
			Sample.makeFeedItems(this);
			Toast.makeText(this, "SAMPLES ADDED", Toast.LENGTH_SHORT).show();
			return true;
			
		case R.id.action_poke_db:
			LoaderManager lm = getSupportLoaderManager();
			lm.initLoader(LOAD_POKE, null, this);
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
	
	


	@Override
	public void onUndo() {
		getSupportLoaderManager()
				.restartLoader(LOAD_UNDO, null, this);
	}
	
	
	/** In the first application run, this function starts
	 *  nothing. In subsequent runs it restarts the loaders
	 *  this application has started, and associates them
	 *  with the new application instance.               */
	public void restartLoaders() {
		msg("restartLoaders()");
		LoaderManager lm = getSupportLoaderManager();
		lm.initLoader(LOAD_FOLD_PANEL, null, this);
	}
	
	
	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
		CursorLoader c = new CursorLoader(this);
		msg("onCreateLoader(" + id + ")");
		switch(id) {
		case LOAD_POKE:
			c.setUri(Uri.parse(DBSchema.URI_STR + "/" + Item.TABLE_NAME + "/before/100"));
			return c;
		case LOAD_UNDO:
			c.setUri(Undo.URI);
			// Undo's parameters are not kept.
			return c;
		case LOAD_FOLD_PANEL:
			c.setUri(Folder.URI);	break;
		case LOAD_ITEM_PANEL:
			c.setUri(Item.URI);
			// Determine the selected feeds.
			long[] select = args.getLongArray(KEY_SELECT);
			if(select.length == 0) {
				c.setSelection(Item._FOLD_ID + "=?");
				c.setSelectionArgs(new String[]{"-1"});
				break;
			}
			String sel_str = "";
			String[] sel_args = new String[select.length];
			for(int i = 0; i < select.length; i++) {
				if(select[i] == 0) {
					sel_str = "";
					break;
				}
				sel_str += " or " + Item._FOLD_ID + "=?";
				sel_args[i] = "" + select[i];
			}
			if(sel_str.length() < 4) break;
			sel_str = sel_str.substring(4);
			c.setSelection(sel_str);
			c.setSelectionArgs(sel_args);
			break;
		}
		loader_params[id] = args;
		return c;
	}
	
	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
		int id = loader.getId();
		msg("onLoadFinished(" + id + ")");
		switch(id) {
			case LOAD_UNDO:
				String msg = getResources().getString(R.string.restored);
				Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
				break;
			case LOAD_FOLD_PANEL:
				if(folder_panel == null) return;
				folder_panel.changeCursor(data);
				int pos = folder_panel.getItemPosition(last_select);
				folder_panel.softSelect(pos);	break;
			case LOAD_ITEM_PANEL:
				if(feed_panel == null) return;
				feed_panel.changeCursor(data);		break;
		}
	}
	
	@Override
	public void onLoaderReset(Loader<Cursor> loader) {
		int id = loader.getId();
		msg("onLoaderReset(" + id + ")");
		switch(id) {
			case LOAD_FOLD_PANEL:
				if(folder_panel == null) return;
				folder_panel.changeCursor(null);	break;
			case LOAD_ITEM_PANEL:
				if(feed_panel == null) return;
				feed_panel.changeCursor(null);		break;
		}
	}
	
	
	/** Gets the left and right panels, if they are present.
	 *  If they are not present, sets up the left panel in
	 *  the swap space. The right panel remains uninitialized
	 *  until it is needed.                                */
	public void getPanels() {
		FragmentManager fm = getSupportFragmentManager();
		folder_panel = FolderPanel.getPanel(fm);
		feed_panel = ItemPanel.getPanel(fm);
		popup_panel = PopupPanel.getPanel(fm);
		prog_panel = ProgressPanel.getPanel(fm);
		prog_div = findViewById(R.id.prog_div);
		prog_div.setVisibility(View.GONE);
		prog_panel.getView()
				  .setVisibility(View.GONE);
		
		if(folder_panel == null) {
			folder_panel = new FolderPanel();
			fm.beginTransaction()
			  .add(R.id.swap_panel, folder_panel, FolderPanel.PANEL_TAG)
			  .commit();
		}
		
		popup_panel.setUndoListener(this);
	}
	
	/** Called whenever a fragment is added to or removed
	 *  from the back stack.                           */
	@Override
	public void onBackStackChanged() {
		FragmentManager fm = getSupportFragmentManager();
		ActionBar bar = getSupportActionBar();
		
		int num_back = fm.getBackStackEntryCount();
		if(num_back < 1) {
			bar.setDisplayHomeAsUpEnabled(false);
			bar.setHomeButtonEnabled(false);
		} else
			bar.setDisplayHomeAsUpEnabled(true);
	}
	
	private class DownloadReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
			if(prog_panel == null) return;
			Bundle bund = intent.getExtras();
			int err = bund.getInt(DBService.PROG_ERR);
			String desc = bund.getString(DBService.PROG_DESC);
			int perc = bund.getInt(DBService.PROG_PERC);
			
			// respond to errors
			switch(err) {
			case 0: break;	// No errors, proceed
			case DBService.ERR_OFFLINE:
				popup_panel.showMsg(desc, false);
				return;
			case DBService.ERR_URL:
				prog_panel.showError(desc);
				return;
			case DBService.ERR_CONN:
				prog_panel.showError(desc);
				return;
			}
			
			// respond to normal returns
			if(perc == 100) {
				prog_div.setVisibility(View.GONE);
				prog_panel.getView()
						  .setVisibility(View.GONE);
				return;
			}
			prog_panel.setProgress(desc, perc);
			prog_div.setVisibility(View.VISIBLE);
			prog_panel.getView()
					  .setVisibility(View.VISIBLE);
		}
	}
	
	@Override
	public void msg(String msg) {
		Log.d("RSSReader.MainActivity", msg);
	}
}