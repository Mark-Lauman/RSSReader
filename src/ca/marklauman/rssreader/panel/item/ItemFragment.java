package ca.marklauman.rssreader.panel.item;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ListView;
import ca.marklauman.rssreader.R;
import ca.marklauman.rssreader.database.schema.Item;

import com.actionbarsherlock.app.SherlockListFragment;
import com.actionbarsherlock.view.ActionMode;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.ActionMode.Callback;

public class ItemFragment extends SherlockListFragment
						  implements OnItemClickListener,
						  OnItemLongClickListener,
						  Callback,
						  LoaderCallbacks<Cursor> {
	
	/** Key used in the {@code savedInstanceState}
	 *  to store selected items.                */
	public static final String KEY_SELECT = "ItemHandler.selections";
	
	/** Adapter used by the list. */
	private ItemAdapter adapt;
	/** Selections placed in savedInstanceState
	 *  before the application rebooted.
	 *  These will be restored once the loader
	 *  finishes getting the feed list.
	 *  Turns to null after the restoration.    */
	private long[] restore;
	
	
	@Override
	public void onViewStateRestored(Bundle savedInstanceState) {
		/* Called after the view has been created
		 * and before onStart().               */
		super.onViewStateRestored(savedInstanceState);
		
		// basic setup
		ListView list = getListView();
		adapt = new ItemAdapter(getSherlockActivity());
		setListAdapter(adapt);
		list.setOnItemClickListener(this);
		list.setOnItemLongClickListener(this);
		
		// Setup selection restore
		if(savedInstanceState == null) restore = null;
		else restore = savedInstanceState.getLongArray(KEY_SELECT);
	}
	
	
	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		if(ItemAdapter.CHOICE_MODE_MULTIPLE
				!= adapt.getChoiceMode())
			return;
		outState.putLongArray(KEY_SELECT, adapt.getSelections());
	}
	
	
	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		switch(adapt.getChoiceMode()) {
		case ItemAdapter.CHOICE_MODE_NONE:
			setRead(id);
			Uri url = adapt.getURL(position);
			Intent launchBrowser = new Intent(Intent.ACTION_VIEW, url);
			getSherlockActivity().startActivity(launchBrowser);
			return;
		case ItemAdapter.CHOICE_MODE_MULTIPLE:
			adapt.toggleItem(position);
			return;
		}
		return;
	}
	
	
	@Override
	public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
		switch(adapt.getChoiceMode()) {
		case ItemAdapter.CHOICE_MODE_NONE:
			getSherlockActivity().startActionMode(this);
			adapt.setChoiceMode(ItemAdapter.CHOICE_MODE_MULTIPLE);
			onItemClick(parent, view, position, id);
			return true;
		case ItemAdapter.CHOICE_MODE_MULTIPLE:
			onItemClick(parent, view, position, id);
			return true;
		}
		return false;
	}
	
	
	@Override
	public boolean onCreateActionMode(ActionMode mode, Menu menu) {
		MenuInflater inflater = mode.getMenuInflater();
        inflater.inflate(R.menu.context_item, menu);
        return true;
	}
	
	
	@Override
	public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
		return false;
	}
	
	
	@Override
	public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
		switch(item.getItemId()) {
		case R.id.context_toggle_all:
			adapt.toggleAll();
			return true;
		}
		return false;
	}
	
	
	@Override
	public void onDestroyActionMode(ActionMode mode) {
		adapt.setChoiceMode(ItemAdapter.CHOICE_MODE_NONE);
	}
	
	
	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
		CursorLoader c = new CursorLoader(getActivity());
		c.setUri(Item.URI);
		c.setProjection(new String[]{Item._ID,
									 Item._TITLE,
									 Item._BRIEF,
									 Item._URL,
									 Item._READ,
									 "ifnull(" + Item._TIME
									 + ", " + Item._TIME_SAVE
									 + ") AS " + Item._TIME});
		c.setSortOrder(Item._TIME + " DESC, " + Item._TITLE);
		return c;
	}
	
	
	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
		adapt.changeCursor(data);
		
		// Restore selections if needed
		if(restore == null) return;
		getSherlockActivity().startActionMode(this);
		adapt.setChoiceMode(ItemAdapter.CHOICE_MODE_MULTIPLE);
		adapt.setSelections(restore);
		restore = null;
	}
	
	
	@Override
	public void onLoaderReset(Loader<Cursor> loader) {
		adapt.changeCursor(null);
	}
	
	
	private void setRead(long id) {
		ContentResolver cr = getSherlockActivity().getContentResolver();
		ContentValues vals = new ContentValues();
		vals.put(Item._READ, true);
		cr.update(Item.URI, vals,
				  Item._ID + "=?",
				  new String[]{"" + id});
	}
}