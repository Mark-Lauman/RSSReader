package ca.marklauman.rssreader.dialogs;

import ca.marklauman.rssreader.R;
import ca.marklauman.rssreader.panels.PopupPanel;
import ca.marklauman.rssreader.providers.DBSchema.Feed;
import ca.marklauman.rssreader.providers.DBSchema.Folder;
import ca.marklauman.tools.Debug;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.database.MergeCursor;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.SimpleCursorAdapter;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.EditText;
import android.widget.Spinner;

public class NewFeedDialog extends DialogFragment
						   implements LoaderCallbacks<Cursor>,
						   			  TextWatcher,
									  Debug {
	
	public static final int LOADER_FEED_DIALOG = 60;
	
	EditText url;
	EditText name;
	Spinner fold;
	SimpleCursorAdapter adapt;
	int url_def_col;
	PopupPanel popup;
	
	@Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
		// Make the view and get links to its elements
		View v = View.inflate(getActivity(),
							  R.layout.dialog_feed,
							  null);
		url = (EditText) v.findViewById(R.id.dialog_feed_url);
		url_def_col = url.getCurrentTextColor();
		url.addTextChangedListener(this);
		name = (EditText) v.findViewById(R.id.dialog_feed_name);
		fold = (Spinner) v.findViewById(R.id.dialog_feed_fold_sel);
		adapt = new SimpleCursorAdapter(
				getActivity(),
				android.R.layout.simple_spinner_item,
				null,
				new String[]{Folder._NAME},
				new int[]{android.R.id.text1}, 0);
		adapt.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		fold.setAdapter(adapt);
		
		// Load the spinner data
		LoaderManager lm = getActivity().getSupportLoaderManager();
		lm.initLoader(LOADER_FEED_DIALOG, null, this);
		
		// Make the dialog
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		builder.setTitle(R.string.action_new_feed)
			   .setPositiveButton(R.string.dialog_create,
        			   			  new OkClick())
        	   .setNegativeButton(R.string.dialog_cancel,
			   			  		  new CancelClick())
			   .setView(v);
		return builder.create();
	}
	
	
	public void setPopupPanel(PopupPanel p) {
		popup = p;
	}
	
	
	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
		// There is only one loader, so this is easy
		adapt.changeCursor(null);
		CursorLoader c = new CursorLoader(getActivity());
		c.setUri(Folder.URI);
		return c;
	}


	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
		String[] cols = {Folder._ID, Folder._NAME};
		MatrixCursor extras = new MatrixCursor(cols);
		String no_fold = getResources().getString(R.string.dialog_feed_no_fold);
		extras.addRow(new String[] {"0", no_fold});
		Cursor[] cursors = {extras, data};
		adapt.changeCursor(new MergeCursor(cursors));
	}
	
	
	@Override
	public void onLoaderReset(Loader<Cursor> loader) {
		adapt.changeCursor(null);
	}
	
	
	@Override
	public void afterTextChanged(Editable s) {
		// Do a sanity check on the url field
		// turn it red for an invalid url
		String url_str = s.toString();
		if(!Patterns.WEB_URL
					.matcher(url_str)
					.find()) {
			url_str = "http://" + url_str;
			if(!Patterns.WEB_URL
						.matcher(url_str)
						.find()) {
				url.setTextColor(Color.RED);
				return;
			}
		}
		url.setTextColor(url_def_col);
	}
	
	@Override
	public void beforeTextChanged(CharSequence s,
			int start, int count, int after) {}
	
	@Override
	public void onTextChanged(CharSequence s,
			int start, int before, int count) {}
	
	
	private class OkClick implements DialogInterface.OnClickListener {
		@Override
		public void onClick(DialogInterface dialog, int which) {
			String url_str = url.getText().toString().trim();
			String name_str = name.getText().toString().trim();
			long id = fold.getSelectedItemId();
			
			// URL sanity check
			if(!Patterns.WEB_URL
						.matcher(url_str)
						.find()) {
				url_str = "http://" + url_str;
				if(!Patterns.WEB_URL
							.matcher(url_str)
							.find()) {
					if(popup != null)
						popup.showMsg("Bad URL", false);
					return;
				}
			}
			
			ContentValues values = new ContentValues();
			if(url_str == null || "".equals(url_str))
				return;
			values.put(Feed._URL, url_str);
			if(name_str != null && !"".equals(name_str))
				values.put(Feed._NAME, name_str);
			if(id != 0)
				values.put(Feed._FOLD_ID, id);
			else
				values.putNull(Feed._FOLD_ID);
			
			ContentResolver cr = getActivity().getContentResolver();
			cr.insert(Feed.URI, values);
		}
	}
	
	
	private class CancelClick implements DialogInterface.OnClickListener {
		@Override
		public void onClick(DialogInterface dialog, int which) {
			// Do nothing on cancel
		}
	}


	@Override
	public void msg(String msg) {
		Log.d("RSSReader.NewFeedDialog", msg);
	}
}