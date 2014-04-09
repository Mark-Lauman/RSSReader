package ca.marklauman.rssreader.panels;

import com.actionbarsherlock.view.ActionMode;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;

import ca.marklauman.rssreader.R;
import ca.marklauman.rssreader.adapters.FolderAdapter;
import ca.marklauman.rssreader.dialogs.EditFolderDialog;
import ca.marklauman.rssreader.providers.DBSchema;
import ca.marklauman.rssreader.providers.DBSchema.Folder;
import ca.marklauman.tools.Debug;
import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.Toast;

public class FolderPanel extends BaseListPanel
						 implements OnItemLongClickListener,
						 			ActionMode.Callback,
						 			Debug {
	

	/** The tag for this fragment. */
	public static final String PANEL_TAG = "folder_panel";
	/** The fragment tag for the edit folder dialog */
	public final static String EDIT_DIALOG_TAG = "EDIT_FOLDER_DIALOG";
	/** The id of the "All Feeds" list item */
	public final static long ALL_FEEDS_ID = 0;
	
	/** The activity listening for click events */
	private FolderListener listener;
	/** A handle to the context mode itself. Null if
	 *  not in context mode.                      */
	ActionMode context_handle = null;
	
	FolderAdapter adapter;
	
	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		try {
			listener = (FolderListener) activity;
		} catch (ClassCastException e) {
			throw new ClassCastException(activity.toString()
	                    + " must implement FolderListener");
		}
		adapter = new FolderAdapter(activity,
							R.layout.list_item_folder,
							new String[] {Folder._NAME},
							new int[] {android.R.id.text1});
		setListAdapter(adapter);
	}
	
	
	@Override
	public void onStart() {
		super.onStart();
		defaultSelectMode(getListView());
		getListView().setOnItemLongClickListener(this);
	}
	
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View res;
		if(!swapExists())
			res = inflater.inflate(R.layout.panel_folder, container, false);
		else
			res = super.onCreateView(inflater, container, savedInstanceState);
		ListView lv = (ListView) res.findViewById(android.R.id.list);
		defaultSelectMode(lv);
		
		return res;
	}
	
	
	@Override
	public void onListItemClick (ListView l, View v,
								 int position, long id) {
		super.onListItemClick(l, v, position, id);
		
		msg("onListItemClick(" + id + ")");
		if(context_handle == null)
			msg("                context active");
		else
			msg("                context inactive");
		
		if(context_handle == null) {
			adapter.selectItem(position);
			listener.onFolderSelected(true, id);
			return;
		}
		
		adapter.toggleItem(position);
		context_handle.invalidate();
		listener.onFolderSelected(false,
								  adapter.getSelections());
	}
	
	
	@Override
	public boolean onItemLongClick(AdapterView<?> parent, View view,
			   int position, long id) {
		msg("onItemLongClick(" + position + ")");
		
		if(context_handle == null) {
	    	getListView().setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
	    	adapter.setChoiceMode(FolderAdapter.CHOICE_MODE_MULTIPLE);
	    	context_handle = getSherlockActivity().startActionMode(this);
		}
		onListItemClick((ListView) parent, view, position, id);
		
		return true;
	}
	
	
	private void defaultSelectMode(ListView lv) {
		if(swapExists()) {
			lv.setChoiceMode(ListView.CHOICE_MODE_NONE);
			adapter.setChoiceMode(FolderAdapter.CHOICE_MODE_NONE);
			return;
		}
		adapter.setChoiceMode(FolderAdapter.CHOICE_MODE_SINGLE);
		lv.setChoiceMode(ListView.CHOICE_MODE_NONE);
	}
	
	
	/** Get the position of the item with this id.
	 *  @param id The sql id of the item.
	 *  (from the "_id" column)
	 *  @return The position of that item in the list,
	 *  or -1 if no items exist with that id.       */
	public int getItemPosition(long id) {
		return adapter.getPosition(id);
	}
	
	
	/** Set the selection to the indicated position.
	 *  Will not call for a screen transition on small screens.
	 *  @param position The position of the new selection.
	 *  @return {@link true} if successful. Will only fail if
	 *  this fragment is not on display.                   */
	public boolean softSelect(int position) {
		msg("softSelect" + position + ")");
		try {
			defaultSelectMode(getListView());
	    	adapter.selectItem(position);
	    	listener.onFolderSelected(false, ALL_FEEDS_ID);
	    	listener.onFolderSelected(false,
					  				  adapter.getSelections());
		} catch(Exception e) {
			return false;
		} return true;
	}
	
	
	public interface FolderListener {
		/** Called when a item is clicked on the {@link FolderPanel}.
		 *  @param position The position of the item in the list.
		 *  @param swap {@code true} if the feed panel should
		 *  be swapped into the foreground. {@code false}
		 *  if it should be left where it is.
		 *  @param ids The ids of the selected folders.
		 *  Usually of length 1, but may be longer.
		 *  An id of {@link FolderPanel#ALL_FEEDS_ID}
		 *  corresponds to "all feeds".                   */
		public void onFolderSelected(boolean swap, long... ids);
	}


	/** Gets the {@link FolderPanel} with the
	 *  {@link #PANEL_TAG} tag.
	 *  @param fm The manager to search for the panel.
	 *  @return The panel if found, or null.        */
	public static FolderPanel getPanel(FragmentManager fm) {
		return (FolderPanel) fm.findFragmentByTag(PANEL_TAG);
	}


	@Override
	public boolean onCreateActionMode(ActionMode mode, Menu menu) {
		mode.getMenuInflater().inflate(R.menu.context_folder, menu);
		return true;
	}


	@Override
	public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
		// get the selected items
		long[] selections = adapter.getSelections();
		// get the menu icons
		MenuItem rename = menu.findItem(R.id.context_edit);
		MenuItem merge = menu.findItem(R.id.context_merge);
		MenuItem delete = menu.findItem(R.id.context_del);
		
		// hide or show icons based off of # of selections
		switch(selections.length) {
		case 0: // no folders selected
			rename.setVisible(false);
			merge.setVisible(false);
			delete.setVisible(false);
			return true;
		case 1: // 1 folder selected
			rename.setVisible(true);
			merge.setVisible(false);
			delete.setVisible(true);
			break;
		default: // more than 1 selected
			rename.setVisible(false);
			merge.setVisible(true);
			delete.setVisible(true);
		}
		
		// special case for if "all feeds" is selected
		boolean all_feeds = false;
		for(long sel : selections) {
			all_feeds = sel == 0;
			if(all_feeds) break;
		}
		if(all_feeds) {
			rename.setVisible(false);
			merge.setVisible(false);
			delete.setVisible(false);
		}
		
		return true;
	}


	@Override
	public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
		// Called when the user selects a contextual menu item
		switch (item.getItemId()) {
		
		// Rename 1 folder
		case R.id.context_edit:
			long id = adapter.getSelections()[0];
			EditFolderDialog dialog = new EditFolderDialog();
			dialog.setFolder(id, adapter.getFoldName(id));
			dialog.show(getFragmentManager(), EDIT_DIALOG_TAG);
			context_handle.finish();
			return true;
		
		// Merge 2+ folders together
		case R.id.context_merge:
			// TODO: Make this do something
			Toast.makeText(getActivity(), "MERGE FOLDERS\nComing Soon!", Toast.LENGTH_SHORT).show();
			context_handle.finish();
			return true;
		
		// Delete 1+ folders
		case R.id.context_del:
			// the selected feeds
			long[] selections = adapter.getSelections();
			// sql WHERE string to match a folder's id
			String id_match = " or " + Folder._ID + "=?";
			// resulting sql WHERE query.
			String where = "";
			// resulting sql WHERE parameters.
			String[] params = new String[selections.length];
			
			// poulate the sql query
			for(int i = 0; i < params.length; i++) {
				where += id_match;
				params[i] = "" + selections[i];
			}
			// remove the first " or " from the string
			where = where.substring(4);
			
			// do the delete
			int deleted = getActivity().getContentResolver()
					 			.delete(DBSchema.Folder.URI,
					 					where, params);
			
			// get the undo panel
			FragmentManager fm = getFragmentManager();
			PopupPanel undo = PopupPanel.getPanel(fm);
			String msg = null;
			
			if(deleted < 1) {
				msg = getResources().getString(R.string.del_fold_0);
				Toast.makeText(getActivity(),
							   msg, Toast.LENGTH_SHORT)
					 .show();
				context_handle.finish();
				return true;
			} else if(deleted == 1)
				msg = getResources().getString(R.string.del_fold_1);
			else
				msg = getResources().getString(R.string.del_fold_many);
			
			undo.showUndo(deleted + " " + msg, false);
			
			context_handle.finish();
			return true;
		}
		return false;
	}


	@Override
	public void onDestroyActionMode(ActionMode mode) {
		// Called when the user exits the action mode
		context_handle = null;
		getListView().setChoiceMode(ListView.CHOICE_MODE_SINGLE);
		softSelect(0);
	}

	@Override
	public void msg(String msg) {
		Log.d("RSSReader.FolderPanel", msg);
	}
}
