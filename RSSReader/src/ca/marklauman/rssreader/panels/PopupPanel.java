package ca.marklauman.rssreader.panels;

import ca.marklauman.rssreader.R;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewPropertyAnimator;
import android.widget.Button;
import android.widget.TextView;

/** A basic undo bar for undoing large operations.
 *  Heavily based off code provided by Roman Nurik here:
 *  {@link http://code.google.com/p/romannurik-code/source/browse/misc/undobar}
 *  @author Mark Lauman                        */
public class PopupPanel extends Fragment {
	/** Fragment Tag for this panel. */
	public static final String PANEL_TAG = "POPUP";
	/** savedInstanceState key for the message at startup */
	private static final String KEY_MSG = "STARTING MESSAGE";
	
	/** The message on display. */
	private CharSequence message;
	
	/** The view for this {@link PopupPanel}. */
	private View view;
	/** The {@link TextView} for the message. */
	private TextView msgView;
	/** The {@link Button} for undo operations. */
	private Button undoButton;
	/** The listener to call when the undo button is clicked */
	private UndoListener listener;
	
	/** Handles hiding the undo bar after x seconds. */
	private Handler hideHandler = new Handler();
	/** Actually does the hiding for the above handler. */
	private Runnable hideRunnable = new UndoHider();
    /** Provides fancy fade transitions for newer
     *  versions of android.                   */
	private ViewPropertyAnimator hideAnimator;
	
	/** Set the {@link UndoListener} for this bar.
	 *  @param u The new listener.         */
	public void setUndoListener(UndoListener u) {
		listener = u;
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater,
			ViewGroup container, Bundle savedInstanceState) {
		view = inflater.inflate(R.layout.panel_popup, container);
		msgView = (TextView) view.findViewById(R.id.undo_message);
		undoButton = (Button) view.findViewById(R.id.undo_button);
		undoButton.setOnClickListener(new UndoClickListener());
		hide(true);
		
		return view;
	}
	
	@Override
	public void onViewCreated(View v, Bundle savedInstanceState) {
		super.onViewCreated(v, savedInstanceState);
		if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH)
			setupAnimator();
		
		message = null;
		if(savedInstanceState != null)
			message = savedInstanceState.getCharSequence(KEY_MSG);
		if(message != null)
			showUndo(message, true);
	}
	
	public void onSaveInstanceState(Bundle outState) {
		outState.putCharSequence(KEY_MSG, message);
		super.onSaveInstanceState(outState);
	}
	
	@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
	private void setupAnimator() {
		hideAnimator = view.animate();
	}
	
	
	/** Show this {@link PopupPanel} with the undo button
	 *  visible. Display the message passed in.
	 *  @param message The message to display on the bar.
	 *  @param immediate In Android versions > 4.0 (Ice Cream
	 *  Sandwich) this indicates whether the transition should
	 *  be immediate or should fade in slowly. In versions
	 *  earlier than 4.0, this parameter will be ignored.   */
	public void showUndo(CharSequence message, boolean immediate) {
		this.undoButton.setVisibility(View.VISIBLE);
		show(message, immediate);
    }
	
	
	/** Show this {@link PopupPanel} and display the message.
	 *  @param message The message to display on the bar.
	 *  @param immediate In Android versions > 4.0 (Ice Cream
	 *  Sandwich) this indicates whether the transition should
	 *  be immediate or should fade in slowly. In versions
	 *  earlier than 4.0, this parameter will be ignored.   */
	public void showMsg(CharSequence message, boolean immediate) {
		this.undoButton.setVisibility(View.GONE);
		show(message, immediate);
	}
	
	
	/** Show this {@link PopupPanel} and display the message.
	 *  @param message The message to display on the bar.
	 *  @param immediate In Android versions > 4.0 (Ice Cream
	 *  Sandwich) this indicates whether the transition should
	 *  be immediate or should fade in slowly. In versions
	 *  earlier than 4.0, this parameter will be ignored.   */
	private void show(CharSequence message, boolean immediate) {
		this.message = message;
		this.msgView.setText(message);
		
        hideHandler.removeCallbacks(hideRunnable);
        hideHandler.postDelayed(hideRunnable,
                getResources().getInteger(R.integer.undo_hide_delay));
        
        view.setVisibility(View.VISIBLE);
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH)
        	fancyShow(immediate);
	}
	
	
	/** Does fancy fade in effects for versions that support it.
	 *  @param immediate This indicates whether the transition
	 *  should be immediate or should fade in slowly.         */
	@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
	private void fancyShow(boolean immediate) {
		if (! immediate) {
            hideAnimator.cancel();
            hideAnimator
                    .alpha(1)
                    .setDuration(getResources()
                                    .getInteger(android.R.integer.config_shortAnimTime))
                    .setListener(null);
        } else 
            view.setAlpha(1);
	}
	
	
	/** Hide this {@link PopupPanel}.
	 *  @param immediate In Android versions > 4.0 (Ice Cream
	 *  Sandwich) this indicates whether the transition should
	 *  be immediate or should fade out slowly. In versions
	 *  earlier than 4.0, this parameter will be ignored.      */
	public void hide(boolean immediate) {
        hideHandler.removeCallbacks(hideRunnable);
        message = null;
        
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH)
        	fancyHide(immediate);
        else
        	view.setVisibility(View.GONE);
    }
	
	
	/** Does fancy fade out effects for versions that support it.
	 *  @param immediate This indicates whether the transition
	 *  should be immediate or should fade out slowly.         */
	@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
	private void fancyHide(boolean immediate) {
		if(! immediate) {
			hideAnimator.cancel();
            hideAnimator
                    .alpha(0)
                    .setDuration(view.getResources()
                            .getInteger(android.R.integer.config_shortAnimTime))
                    .setListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            view.setVisibility(View.GONE);
                        }
                    });
		} else {
			view.setVisibility(View.GONE);
            view.setAlpha(0);
		}
	}
	
	
	/** Gets the {@link PopupPanel} with the
	 *  {@link #PANEL_TAG} tag.
	 *  @param fm The manager to search for the panel.
	 *  @return The panel if found, or null.        */
	public static PopupPanel getPanel(FragmentManager fm) {
		return (PopupPanel) fm.findFragmentByTag(PANEL_TAG);
	}
	
	
	/** A listener that responds to undo events. */
	public interface UndoListener {
		/** Called when an undo button is pressed on a targeted
		 *  {@link PopupPanel}.                                 */
        void onUndo();
    }
	
	/** Listens to the undo button, and informs this
	 *  {@link PopupPanel}'s listener when it is clicked.        */
	private class UndoClickListener implements OnClickListener {
		@Override
        public void onClick(View view) {
            hide(false);
            if(listener == null) {
            	Log.e("UndoBar", "UndoListener not set. Did you call UndoBar.setListener()?");
            	return;
            }
            listener.onUndo();
        }
	}
	
	/** Hides the {@link PopupPanel} when run.    */
	private class UndoHider implements Runnable {
		@Override
        public void run() {
            hide(false);
        }
	}
}
