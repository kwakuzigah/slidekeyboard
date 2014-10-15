package com.asigbe.slidekeyboardpro;

import java.util.HashMap;
import java.util.Map;

import com.asigbe.view.ViewTools;

import android.content.Context;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources.NotFoundException;
import android.graphics.Typeface;
import android.os.Debug;

/**
 * This class manages switches between keyboards.
 * 
 * @author Delali Zigah
 */
public class KeyboardSwitcher {

    private final class Skin {

	private final String                          xmlKeyboards[][];
	private final HashMap<String, AsigbeKeyboard> asigbeKeyboards;
//	private boolean                               displayEnterKey;

	public Skin(String[][] xmlKeyboards) {
	    this.xmlKeyboards = xmlKeyboards;
	    this.asigbeKeyboards = new HashMap<String, AsigbeKeyboard>();
	}
	
	public void clear() {
	    this.asigbeKeyboards.clear();
	}

//	public AsigbeKeyboard getKeyboard() {
//	    AsigbeKeyboard keyboard = KeyboardSwitcher.this.inputView
//		    .getKeyboard();
//	    int keyboardId = -1;
//
//	    switch (KeyboardSwitcher.this.mode) {
//	    case AsigbeKeyboard.MODE_TEXT:
//	    case AsigbeKeyboard.MODE_URL:
//		keyboardId = KEYBOARD_STANDARD;
//		this.displayEnterKey = true;
//		break;
//	    case AsigbeKeyboard.MODE_IM:
//		keyboardId = KEYBOARD_STANDARD;
//		this.displayEnterKey = false;
//		break;
//	    case AsigbeKeyboard.MODE_PHONE:
//	    case AsigbeKeyboard.MODE_NUMBERS:
//	    case AsigbeKeyboard.MODE_SYMBOLS:
//		keyboardId = KEYBOARD_SYMBOLS;
//		break;
//	    case AsigbeKeyboard.MODE_EMAIL:
//		keyboardId = KEYBOARD_EMAILS;
//		break;
//	    }
//
//	    String nameKeyboard = this.xmlKeyboards[keyboardId][KeyboardSwitcher.this.textMode];
//	    keyboard = this.asigbeKeyboards.get(nameKeyboard);
//	    if (keyboard == null) {
//		try {
//		    int idKeyboard = KeyboardSwitcher.this.context
//			    .getPackageManager()
//			    .getResourcesForApplication(
//			            KeyboardSwitcher.this.keyboardPackage)
//			    .getIdentifier(nameKeyboard, "xml",
//			            KeyboardSwitcher.this.keyboardPackage);
//		    keyboard = new AsigbeKeyboard(
//			    KeyboardSwitcher.this.context, idKeyboard,
//			    KeyboardSwitcher.this.mode);
//
//		    keyboard.enableShiftLock();
//		    this.asigbeKeyboards.put(nameKeyboard, keyboard);
//
//		    keyboard.setKeyboardMode(KeyboardSwitcher.this.mode);
//		    keyboard.displayEnterKey(this.displayEnterKey);
//		    keyboard.setShifted(false, false);
//		    keyboard.setImeOptions(
//			    KeyboardSwitcher.this.context.getResources(),
//			    KeyboardSwitcher.this.mode,
//			    KeyboardSwitcher.this.imeOptions);
//		} catch (NameNotFoundException e1) {
//		}
//	    }
//	    return keyboard;
//	}
	
	public AsigbeKeyboard getKeyboard(String nameKeyboard) {
	   

	    AsigbeKeyboard keyboard = this.asigbeKeyboards.get(nameKeyboard);
	    if (keyboard == null) {
		try {
		    int idKeyboard = KeyboardSwitcher.this.context
			    .getPackageManager()
			    .getResourcesForApplication(
			            KeyboardSwitcher.this.keyboardPackage)
			    .getIdentifier(nameKeyboard, "xml",
			            KeyboardSwitcher.this.keyboardPackage);
		    keyboard = new AsigbeKeyboard(
			    KeyboardSwitcher.this.context, idKeyboard,
			    KeyboardSwitcher.this.mode);

		    keyboard.enableShiftLock();
		    this.asigbeKeyboards.put(nameKeyboard, keyboard);

		    keyboard.setKeyboardMode(KeyboardSwitcher.this.mode);
//		    keyboard.displayEnterKey(this.displayEnterKey);
		    keyboard.setShifted(false, false);
		    keyboard.setImeOptions(
			    KeyboardSwitcher.this.context.getResources(),
			    KeyboardSwitcher.this.mode,
			    KeyboardSwitcher.this.imeOptions);
		} catch (NameNotFoundException e1) {
		}
	    }
	    return keyboard;
	}
    }

    private static final int        KEYBOARD_STANDARD = 0;
    private static final int        KEYBOARD_SYMBOLS  = 1;
    private static final int        KEYBOARD_EMAILS   = 2;
    private static final int        KEYBOARD_COUNT    = 3;

    private SlideKeyboardView       inputView;

    // private AsigbeKeyboard mPhoneKeyboard;
    // private AsigbeKeyboard mPhoneSymbolsKeyboard;

    private int                     mode;
    private int                     imeOptions;
    private int                     textMode          = SlideKeyboard.MODE_TEXT_ALPHA;

    private String                  keyboardPackage   = SlideKeyboard.STANDARD_SKIN;
    private final Map<String, Skin> keyboardskins;
    private Context                 context;
    private AsigbeKeyboard oldKeyboard;

    KeyboardSwitcher(Context context) {
	this.context = context;
	this.keyboardskins = new HashMap<String, Skin>();
	this.keyboardskins.put(SlideKeyboard.STANDARD_SKIN, new Skin(
	        new String[][] {
	                { "qwerty_keyboard", "azerty_keyboard",
	                        "alphabetic_keyboard", "dvorak_keyboard" },
	                { "qwerty_keyboard_symbols", "qwerty_keyboard_symbols",
	                        "alphabetic_keyboard_symbols",
	                        "qwerty_keyboard_symbols" },
	                { "qwerty_keyboard", "azerty_keyboard",
	                        "alphabetic_keyboard", "dvorak_keyboard" } }));
    }

    void setInputView(SlideKeyboardView inputView) {
	this.inputView = inputView;
	this.inputView.applySkin(this.context, ViewTools.getResourceId(
	        this.context, this.keyboardPackage, "SlideKeyboardStyle",
	        "style"));
    }

//    public void setKeyboardMode(int mode) {
//	setKeyboardMode(mode, this.imeOptions);
//    }

//    public void setKeyboardMode(int mode, int imeOptions) {
//	this.mode = mode;
//	this.imeOptions = imeOptions;
//	this.inputView.setPreviewEnabled(true);
//	this.inputView.setKeyboard(getCurrentKeyboardSkin().getKeyboard());
//    }

    private Skin getCurrentKeyboardSkin() {
	Skin currentKeyboardSkin = this.keyboardskins.get(this.keyboardPackage);
	if (currentKeyboardSkin == null) {
	    currentKeyboardSkin = new Skin(new String[][] {
		    { "qwerty_keyboard", "azerty_keyboard",
		            "alphabetic_keyboard", "dvorak_keyboard" },
		    { "qwerty_keyboard_symbols", "qwerty_keyboard_symbols",
		            "alphabetic_keyboard_symbols",
		            "qwerty_keyboard_symbols" },
		    { "qwerty_keyboard", "azerty_keyboard",
		            "alphabetic_keyboard", "dvorak_keyboard" } });
	    this.keyboardskins.put(this.keyboardPackage, currentKeyboardSkin);
	}
	return currentKeyboardSkin;
    }

    int getKeyboardMode() {
	return this.mode;
    }

    int getTextMode() {
	return this.textMode;
    }

//    void setTextMode(int textMode) {
//	if ((textMode < SlideKeyboard.MODE_TEXT_COUNT) && (textMode >= 0)) {
//	    this.textMode = textMode;
//	}
//
//	// reinitializes display
//	setKeyboardMode(this.mode, this.imeOptions);
//    }

    int getTextModeCount() {
	return SlideKeyboard.MODE_TEXT_COUNT;
    }

    /**
     * Display the other keyboard.
     */
//    public void switchKeyboard() {
//	switch (this.mode) {
//	case AsigbeKeyboard.MODE_TEXT:
//	case AsigbeKeyboard.MODE_IM:
//	case AsigbeKeyboard.MODE_URL:
//	    setKeyboardMode(AsigbeKeyboard.MODE_SYMBOLS);
//	    break;
//	case AsigbeKeyboard.MODE_PHONE:
//	case AsigbeKeyboard.MODE_NUMBERS:
//	case AsigbeKeyboard.MODE_SYMBOLS:
//	    setKeyboardMode(AsigbeKeyboard.MODE_TEXT);
//	    break;
//	default:
//	    break;
//	}
//    }

    /**
     * Sets the keyboard skin.
     */
    public void setKeyboardSkin(Context context, String keyboardPackage) {
	this.keyboardPackage = keyboardPackage;
	Skin skin = this.keyboardskins.get(this.keyboardPackage);
	if (skin != null) {
	    skin.clear();
	}
	this.context = context;
    }
    
    public void setActiveKeyboard(String keyboard) {
	if (keyboard == null || keyboard.length() == 0) {
	    return;
	}
	this.oldKeyboard = this.inputView.getKeyboard();
	this.inputView.setKeyboard(getCurrentKeyboardSkin().getKeyboard(keyboard));
    }

    public void revertActiveKeyboard() {
	this.inputView.setKeyboard(this.oldKeyboard);
    }
}
