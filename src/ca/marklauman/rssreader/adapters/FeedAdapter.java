package ca.marklauman.rssreader.adapters;

import java.util.HashSet;

import ca.marklauman.rssreader.R;
import ca.marklauman.rssreader.providers.DBSchema.Feed;
import android.content.Context;
import android.database.Cursor;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class FeedAdapter extends CursorSelAdapter {
	
	/** Items that are the first of their folder */
	private HashSet<Integer> fold_firsts = new HashSet<Integer>();
	
	
	public FeedAdapter(Context context) {
		super(context, R.layout.list_item_feed,
			  new String[]{Feed._FOLD_NAME,
						   Feed._NAME,
						   Feed._URL},
			  new int[]   {R.id.feed_folder,
						   android.R.id.text1,
						   android.R.id.text2});
	}
	
	
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View view = super.getView(position, convertView, parent);
		TextView fold = (TextView) view.findViewById(R.id.feed_folder);
		
		if(fold_firsts.contains(position)) {
			fold.setVisibility(View.VISIBLE);
			if("".equals(fold.getText().toString()))
				fold.setText(
						mContext.getResources()
								.getString(R.string.no_folder));
		} else
			fold.setVisibility(View.GONE);
		return view;
	}
	
	
	@Override
	public void changeCursor(Cursor cursor) {
		fold_firsts.clear();
		super.changeCursor(cursor);
		if(cursor == null || !cursor.moveToFirst())
			return;
		
		int sql_fold_id = cursor.getColumnIndex(Feed._FOLD_ID);
		long last = -2L;
		do {
			long fold_id = cursor.getLong(sql_fold_id);
			if(last != fold_id) {
				fold_firsts.add(cursor.getPosition());
				last = fold_id;
			}
		} while(cursor.moveToNext());
	}
}