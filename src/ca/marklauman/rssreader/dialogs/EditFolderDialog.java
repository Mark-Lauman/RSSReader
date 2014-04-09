package ca.marklauman.rssreader.dialogs;

import ca.marklauman.rssreader.R;
import ca.marklauman.rssreader.providers.DBSchema.Folder;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.widget.EditText;

public class EditFolderDialog extends DialogFragment {
	
	public static final String ARG_FOLD_ID = "FOLDER_ID";
	public static final String ARG_FOLD_NAME = "FOLDER_NAME";
	
	EditText txt;
	long id = Integer.MIN_VALUE;
	String foldername;
	
	public void setFolder(long id, String foldername) {
		this.id = id;
		this.foldername = foldername;
	}
	
	@Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
		if(savedInstanceState != null) {
			id = savedInstanceState.getLong(ARG_FOLD_ID);
			foldername = savedInstanceState.getString(ARG_FOLD_NAME);
		}
		if(id == Integer.MIN_VALUE)
			throw new IllegalArgumentException("Folder has not been set. Call setFolder().");
		
		// Make the contents of the dialog
		txt = new EditText(getActivity());
		txt.setHint(R.string.dialog_fold_name);
		txt.setText(foldername);
		
		// Make the dialog
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		builder.setTitle(R.string.dialog_fold_rename)
				.setPositiveButton(R.string.dialog_save,
								   new OkClick())
				.setNegativeButton(R.string.dialog_cancel,
								   new CancelClick())
				.setView(txt);
		return builder.create();
	}
	
	@Override
	public void onSaveInstanceState(Bundle outState) {
		outState.putLong(ARG_FOLD_ID, id);
		outState.putString(ARG_FOLD_NAME, txt.getText().toString());
	}
	
	private class OkClick implements DialogInterface.OnClickListener {
		@Override
		public void onClick(DialogInterface dialog, int which) {
			foldername = txt.getText().toString().trim();
			if(foldername == null || "".equals(foldername))
				return;
			ContentValues values = new ContentValues();
			values.put(Folder._NAME, foldername);
			getActivity().getContentResolver()
						 .update(Folder.URI, values,
								 Folder._ID + "=?",
								 new String[]{"" + id});
		}
	}
	
	
	private class CancelClick implements DialogInterface.OnClickListener {
		@Override
		public void onClick(DialogInterface dialog, int which) {
			// Do nothing on cancel
		}
	}
}
