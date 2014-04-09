package ca.marklauman.rssreader.dialogs;

import ca.marklauman.rssreader.R;
import ca.marklauman.rssreader.providers.DBSchema;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.widget.EditText;

public class NewFolderDialog extends DialogFragment {
	
	public static final String ARG_FOLD_NAME = "FOLDER_NAME";
	
	EditText txt;
	
	@Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
		String foldername = "";
		if(savedInstanceState != null)
			foldername = savedInstanceState.getString(ARG_FOLD_NAME);
		
		// Make the contents of the dialog
		txt = new EditText(getActivity());
		txt.setHint(R.string.dialog_fold_name);
		txt.setText(foldername);
		
        // Make the dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.action_new_fold)
        	   .setPositiveButton(R.string.dialog_create,
        			   			  new OkClick())
        	   .setNegativeButton(R.string.dialog_cancel,
			   			  		  new CancelClick())
			   .setView(txt);
        return builder.create();
	}
	
	@Override
	public void onSaveInstanceState(Bundle outState) {
		outState.putString(ARG_FOLD_NAME, txt.getText().toString());
		super.onSaveInstanceState(outState);
	}
	
	
	private class OkClick implements DialogInterface.OnClickListener {
		@Override
		public void onClick(DialogInterface dialog, int which) {
			// Get the name of the new folder
			String new_name = txt.getText().toString().trim();
			String all_feed = getActivity().getString(R.string.all_feeds);
			if(all_feed.equalsIgnoreCase(new_name)) return;
			if("".equals(new_name)) return;
			
			// Do the folder insertion
			ContentValues values = new ContentValues();
			values.put(DBSchema.Folder._NAME, new_name);
			ContentResolver cr = getActivity().getContentResolver();
			cr.insert(DBSchema.Folder.URI, values);
		}
	}
	
	private class CancelClick implements DialogInterface.OnClickListener {
		@Override
		public void onClick(DialogInterface dialog, int which) {
			// Do nothing on cancel
		}
	}
}
