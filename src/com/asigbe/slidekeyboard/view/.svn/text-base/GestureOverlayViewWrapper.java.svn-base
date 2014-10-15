package com.asigbe.slidekeyboard.view;

import java.util.ArrayList;

import android.gesture.Gesture;
import android.gesture.GestureOverlayView;
import android.gesture.Prediction;
import android.gesture.GestureOverlayView.OnGesturePerformedListener;
import android.view.View;

import com.asigbe.slidekeyboardpro.AsigbeKeyboard;
import com.asigbe.slidekeyboardpro.SlideKeyboard;
import com.asigbe.slidekeyboardpro.SlideKeyboardView;

/**
 * Wrapper used for retro-compatibility.
 * 
 * @author Delali Zigah
 */
public class GestureOverlayViewWrapper {

    static {
        try {
            Class.forName("android.gesture.GestureOverlayView");
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }
    
    /** Checks availability of the class */
    public static void checkAvailable() {}
    
    
    private final GestureOverlayView gestureOverlayView;

    /**
     * Creates a wrapper for the given gesture overlay view.
     */
    public GestureOverlayViewWrapper(View gestureOverlayView) {
	this.gestureOverlayView = (GestureOverlayView) gestureOverlayView;
    }

    /**
     * Removes all listeners on the gesture overlay view.
     */
    public void removeAllOnGesturePerformedListeners() {
	this.gestureOverlayView.removeAllOnGesturePerformedListeners();
    }

    /**
     * Adds a new gesture listener.
     */
    public void addOnGesturePerformedListener(
	    final SlideKeyboard slideKeyboard,
	    final SlideKeyboardView slideKeyboardView,
	    final GestureLibraryWrapper gestureLibraryWrapper) {
	this.gestureOverlayView
	        .addOnGesturePerformedListener(new OnGesturePerformedListener() {
		    @Override
		    public void onGesturePerformed(GestureOverlayView overlay,
		            Gesture gesture) {
		        ArrayList<Prediction> predictions = gestureLibraryWrapper
		                .recognize(gesture);

		        // We want at least one prediction
		        if (predictions.size() > 0) {
			    Prediction prediction = predictions.get(0);
			    // We want at least some confidence in
			    // the
			    // result
			    if (prediction.score > 1.0) {
			        if (prediction.name
			                .equals(SlideKeyboard.GESTURE_RIGHT)) {
				    // right gesture is for spaces
				    slideKeyboard.onKeyUp(' ', null, null);
			        } else if (prediction.name
			                .equals(SlideKeyboard.GESTURE_LEFT)) {
				    // left gesture is for delete
				    slideKeyboard
				            .onKeyUp(
				                    AsigbeKeyboard.KEYCODE_DELETE,
				                    null, null);
			        } else if (prediction.name
			                .equals(SlideKeyboard.GESTURE_RIGHT_UP)) {
				    // up gesture is for shift
				    slideKeyboard.onKeyUp(
				            AsigbeKeyboard.KEYCODE_SHIFT, null, null);
			        } else if (prediction.name
			                .equals(SlideKeyboard.GESTURE_DOWN_LEFT)) {
				    // down gesture is for enter
				    slideKeyboard.onKeyUp(
				            AsigbeKeyboard.KEYCODE_ENTER, null, null);
			        } else if (prediction.name
			                .equals(SlideKeyboard.GESTURE_LEFT_PARENTHESIS)) {
				    // down gesture is for enter
				    slideKeyboard.onKeyUp('(', null, null);
			        } else if (prediction.name
			                .equals(SlideKeyboard.GESTURE_RIGHT_PARENTHESIS)) {
				    // down gesture is for enter
				    slideKeyboard.onKeyUp(')', null, null);
			        }
			        slideKeyboardView.cancelDetection();
			    }
		        }
		    }
	        });
    }

}
