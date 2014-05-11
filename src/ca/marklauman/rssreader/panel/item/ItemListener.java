package ca.marklauman.rssreader.panel.item;

import ca.marklauman.rssreader.MainActivity;
import ca.marklauman.rssreader.R;

import com.actionbarsherlock.view.ActionMode;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.ActionMode.Callback;

import android.content.Intent;
import android.net.Uri;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;

/** This class monitors actions delivered to the
 *  item panel and its context menu. It is also
 *  starts the context menu when required.
 *  @author Mark Lauman                       */
public class ItemListener implements OnItemClickListener,
									 OnItemLongClickListener,
									 Callback {
	private MainActivity activity;
	private ItemAdapter adapt;
	
	
	public ItemListener(MainActivity act, ItemAdapter adapter) {
		activity = act;
		adapt = adapter;
	}
	
	
	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		switch(adapt.getChoiceMode()) {
		case ItemAdapter.CHOICE_MODE_NONE:
			Uri url = adapt.getURL(position);
			Intent launchBrowser = new Intent(Intent.ACTION_VIEW, url);
			activity.startActivity(launchBrowser);
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
			activity.startActionMode(this);
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
		activity.clearActionMode();
	}
}