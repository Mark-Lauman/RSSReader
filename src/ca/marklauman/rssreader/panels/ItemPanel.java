package ca.marklauman.rssreader.panels;

import ca.marklauman.rssreader.adapters.ItemAdapter;
import ca.marklauman.rssreader.providers.DBSchema.Item;
import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.support.v4.app.FragmentManager;
import android.view.View;
import android.widget.ListView;

public class ItemPanel extends BaseListPanel {
	
	public static final String PANEL_TAG = "item_panel";
	private int col_link;
	
	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		setListAdapter(
				new ItemAdapter(activity));
	}
	
	@Override
	public void onListItemClick (ListView l, View v,
			int position, long id) {
		super.onListItemClick(l, v, position, id);
		Cursor c = cursor_adapt.getCursor();
		if(!c.moveToPosition(position))
			return;
		String link = c.getString(col_link);
		Intent i = new Intent(Intent.ACTION_VIEW);
		i.setData(Uri.parse(link));
		startActivity(i);
	}
	
	
	public void changeCursor(Cursor cursor) {
		super.changeCursor(cursor);
		if(cursor != null)
			col_link = cursor.getColumnIndex(Item._LINK);
	}
	
	
	/** Gets the {@link ItemPanel} with the
	 *  {@link #PANEL_TAG} tag.
	 *  @param fm The manager to search for the panel.
	 *  @return The panel if found, or null.        */
	public static ItemPanel getPanel(FragmentManager fm) {
		return (ItemPanel) fm.findFragmentByTag(PANEL_TAG);
	}
}
