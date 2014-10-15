package com.asigbe.slidekeyboard.view;

import java.util.ArrayList;

import android.content.Context;
import android.gesture.Gesture;
import android.gesture.GestureLibraries;
import android.gesture.GestureLibrary;
import android.gesture.Prediction;

import com.asigbe.slidekeyboardpro.R;

/**
 * Wrapper used for retro-compatibility.
 * 
 * @author Delali Zigah
 */
public class GestureLibraryWrapper {

    private final GestureLibrary gestureLibrary;

    /**
     * Creates a wrapper for the gesture library.
     */
    public GestureLibraryWrapper(Context context) {
	this.gestureLibrary = GestureLibraries.fromRawResource(context,
		    R.raw.gestures);
    }

    /**
     * Loads gestures.
     */
    public boolean load() {
	return this.gestureLibrary.load();
    }

    /**
     * Identifies a gesture.
     */
    public ArrayList<Prediction> recognize(Gesture gesture) {
	return this.gestureLibrary.recognize(gesture);
    }

}
