package com.asigbe.slidekeyboardpro;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.os.Debug;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.PreferenceActivity;
import android.view.OrientationEventListener;

public class LatinIMESettings extends PreferenceActivity {

    public static final String         PREF_KEYBOARD_TYPE           = "keyboard_type";
    public static final String         PREF_KEYBOARD_LAYOUT         = "keyboard_layout";
    public static final String         PREF_FIRST_USAGE             = "first_usage";
    public static final String         PREF_VIBRATE_ON              = "vibrate_on";
    public static final String         PREF_SOUND_ON                = "sound_on";
    public static final String         PREF_AUTO_CAP                = "auto_cap";
    public static final String         PREF_SENSITIVITY             = "sensitivity";
    public static final String         PREF_REPEAT_INTERVAL         = "repeat_interval";
    public static final String         PREF_QUICK_FIXES             = "quick_fixes";
    public static final String         PREF_SHOW_SUGGESTIONS        = "show_suggestions";
    public static final String         PREF_AUTO_COMPLETE           = "auto_complete";
    public static final String         PREF_AUTO_ADD_DICTIONARY     = "auto_add_dictionary";
    public static final String         PREF_GESTURE_SHORTCUTS       = "gesture_shortcuts";
    public static final String         PREF_DISPLAY_TOOLBAR         = "display_toolbar";
    public static final String         PREF_DISPLAY_KEY_PREVIEW     = "display_key_preview";
    public static final String         PREF_LONGPRESS_TIMEOUT       = "longpress_timeout";
    public static final String         PREF_PREDICTION_SETTINGS_KEY = "prediction_settings";
    public static final String         PREF_LANGUAGE_SETTINGS_KEY   = "language";
    public static final String         PREF_KEYBOARD_SKIN           = "keyboard_skin";
    public static final String         PREF_USE_TEMPORARY_SWITCH    = "use_temporary_switch";

    private CheckBoxPreference         quickFixes;
    private CheckBoxPreference         showSuggestions;
    private ListPreference             language;
    private ListPreference             skin;
    private static Map<String, Locale> LOCALES                      = new HashMap<String, Locale>();
    private OrientationEventListener   orientationEventListener;

    static {
	Locale locales[] = Locale.getAvailableLocales();
	for (Locale locale : locales) {
	    LOCALES.put(locale.getLanguage(), locale);
	}
    }

    @Override
    protected void onCreate(Bundle icicle) {
	super.onCreate(icicle);
	addPreferencesFromResource(R.xml.prefs);
	this.quickFixes = (CheckBoxPreference) findPreference(PREF_QUICK_FIXES);
	this.showSuggestions = (CheckBoxPreference) findPreference(PREF_SHOW_SUGGESTIONS);
	((CheckBoxPreference) findPreference(PREF_GESTURE_SHORTCUTS))
	        .setEnabled(SlideKeyboard.IS_GESTURE_OVERLAY_SUPPORTED);

	this.language = (ListPreference) findPreference(PREF_LANGUAGE_SETTINGS_KEY);
	this.skin = (ListPreference) findPreference(PREF_KEYBOARD_SKIN);
	this.orientationEventListener = new OrientationEventListener(this) {

	    @Override
	    public void onOrientationChanged(int arg0) {
		configurePreferences();
	    }
	};
	this.orientationEventListener.enable();
    }

    @Override
    protected void onResume() {
	super.onResume();
	// int autoTextSize = AutoText.getSize(getListView());
	// if (autoTextSize < 1) {
	// ((PreferenceGroup) findPreference(PREF_PREDICTION_SETTINGS_KEY))
	// .removePreference(this.quickFixes);
	// } else {
	// this.showSuggestions.setDependency(PREF_QUICK_FIXES);
	// }
	//
	// List<ResolveInfo> resolveInfos = getPackageManager()
	// .queryIntentActivities(
	// new Intent(SlideKeyboard.DICTIONARY_PACKAGE), 0);
	// if ((resolveInfos != null) && (resolveInfos.size() > 0)) {
	// CharSequence entries[] = new CharSequence[resolveInfos.size()];
	// CharSequence entryValues[] = new CharSequence[resolveInfos.size()];
	// for (int i = 0; i < resolveInfos.size(); i++) {
	// ResolveInfo resolveInfo = resolveInfos.get(i);
	// String[] split = resolveInfo.activityInfo.packageName != null ?
	// resolveInfo.activityInfo.packageName
	// .split("\\.")
	// : null;
	// if ((split != null) && (split.length > 0)) {
	// String language = split[split.length - 1];
	// Locale locale = LOCALES.get(language);
	// if (locale != null) {
	// String displayLanguage = locale.getDisplayLanguage();
	// if (displayLanguage != null
	// && displayLanguage.length() > 1) {
	// entryValues[i] = language;
	// displayLanguage = displayLanguage.substring(0, 1)
	// .toUpperCase()
	// + displayLanguage.substring(1);
	// entries[i] = displayLanguage;
	// }
	// }
	//
	// }
	// }
	// this.language.setDefaultValue(Locale.getDefault().getLanguage());
	// this.language.setEntries(entries);
	// this.language.setEntryValues(entryValues);
	// } else {
	// this.language.setEnabled(false);
	// }

	configurePreferences();
    }
    
    private void configurePreferences() {
	PackageManager packageManager = getPackageManager();
	List<ResolveInfo> resolveInfos = packageManager.queryIntentActivities(
	        new Intent(SlideKeyboard.SKIN_PACKAGE), 0);
	CharSequence entries[] = new CharSequence[resolveInfos.size() + 1];
	CharSequence entryValues[] = new CharSequence[resolveInfos.size() + 1];
	entryValues[0] = SlideKeyboard.STANDARD_SKIN;
	entries[0] = getString(R.string.default_skin);
	if ((resolveInfos != null) && (resolveInfos.size() > 0)) {
	    for (int i = 0; i < resolveInfos.size(); i++) {
		ResolveInfo resolveInfo = resolveInfos.get(i);
		try {
		    entryValues[i + 1] = resolveInfo.activityInfo.packageName;
		    entries[i + 1] = packageManager
			    .getApplicationLabel(packageManager
			            .getApplicationInfo(
			                    resolveInfo.activityInfo.packageName,
			                    0));
		} catch (NameNotFoundException e) {
		}
	    }
	} else {
	    this.skin.setEnabled(false);
	}
	this.skin.setDefaultValue(SlideKeyboard.STANDARD_SKIN);
	this.skin.setEntries(entries);
	this.skin.setEntryValues(entryValues);
    }
}
