package com.asigbe.slidekeyboardpro;

import java.util.ArrayList;
import java.util.List;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.preference.PreferenceManager;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;

import com.asigbe.view.ViewTools;

/**
 * This class displays dialog used to configure the keyboard for the first
 * usage.
 * 
 * @author Delali Zigah
 */
public class FirstUsageConfiguration {

    private final SlideKeyboard      slideKeyboard;
    // private final AlertDialog getDictionariesDialog;
    protected CharSequence           keyboardSkin;
    protected int                    keyboardTypes;
    private final List<CharSequence> packageNames;
    private final List<CharSequence> skinNames;
    private final List<AlertDialog>  dialogs;
    private int                      currentDialogIndex;

    /**
     * Initializes dialogs.
     */
    public FirstUsageConfiguration(final SlideKeyboard slideKeyboard, View view) {
	this.slideKeyboard = slideKeyboard;
	this.packageNames = new ArrayList<CharSequence>();
	this.skinNames = new ArrayList<CharSequence>();
	this.dialogs = new ArrayList<AlertDialog>();

//	View getDictionariesView = ViewTools.inflateView(this.slideKeyboard,
//	        R.layout.get_dictionaries);
	// Button openMarketButton = (Button) getDictionariesView
	// .findViewById(R.id.openMarketButton);
	// openMarketButton.setOnClickListener(new OnClickListener() {
	//
	// @Override
	// public void onClick(View v) {
	// Intent intent = new Intent(Intent.ACTION_VIEW);
	// intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
	// intent
	// .setData(Uri
	// .parse("http://market.android.com/search?q=pname:com.asigbe.dictionaries"));
	// slideKeyboard.startActivity(intent);
	// if (firstUsage) {
	// next();
	// } else {
	// close();
	// }
	// }
	// });
	// Button nextButton = (Button) getDictionariesView
	// .findViewById(R.id.nextButton);
	// nextButton.setOnClickListener(new OnClickListener() {
	//
	// @Override
	// public void onClick(View v) {
	// next();
	// }
	// });
	// AlertDialog.Builder getDictionariesDialogBuilder = new
	// AlertDialog.Builder(
	// this.slideKeyboard);
	// getDictionariesDialogBuilder.setView(getDictionariesView);
	// getDictionariesDialogBuilder.setTitle(R.string.dictionary);
	// this.getDictionariesDialog = getDictionariesDialogBuilder.create();
	// Window getDictionariesDialogWindow = this.getDictionariesDialog
	// .getWindow();
	// WindowManager.LayoutParams getDictionarieslayoutParams =
	// getDictionariesDialogWindow
	// .getAttributes();
	// getDictionarieslayoutParams.token = view.getWindowToken();
	// getDictionarieslayoutParams.type =
	// WindowManager.LayoutParams.TYPE_APPLICATION_ATTACHED_DIALOG;
	// getDictionariesDialogWindow.setAttributes(getDictionarieslayoutParams);
	// getDictionariesDialogWindow
	// .addFlags(WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM);

	View chooseKeyboardStyleView = ViewTools.inflateView(
	        this.slideKeyboard, R.layout.choose_keyboard_skin);
	ListView skinsListView = (ListView) chooseKeyboardStyleView
	        .findViewById(R.id.skinsListView);
	PackageManager packageManager = this.slideKeyboard.getPackageManager();
	List<ResolveInfo> resolveInfos = packageManager.queryIntentActivities(
	        new Intent(SlideKeyboard.SKIN_PACKAGE), 0);
	this.packageNames.add(SlideKeyboard.STANDARD_SKIN);
	this.skinNames.add(this.slideKeyboard.getString(R.string.default_skin));
	if ((resolveInfos != null) && (resolveInfos.size() > 0)) {
	    for (int i = 0; i < resolveInfos.size(); i++) {
		ResolveInfo resolveInfo = resolveInfos.get(i);
		this.packageNames.add(resolveInfo.activityInfo.packageName);
		// this.skinNames.add(resolveInfo.nonLocalizedLabel);
		try {
		    this.skinNames
			    .add(packageManager.getApplicationLabel(packageManager
			            .getApplicationInfo(
			                    resolveInfo.activityInfo.packageName,
			                    0)));
		} catch (NameNotFoundException e) {
		    this.skinNames.add(resolveInfo.activityInfo.packageName);
		}
	    }
	}
	skinsListView.setAdapter(new ArrayAdapter<CharSequence>(
	        this.slideKeyboard, R.layout.skin_layout, R.id.skinTextView,
	        this.skinNames));
	skinsListView.setOnItemClickListener(new OnItemClickListener() {

	    @Override
	    public void onItemClick(AdapterView<?> parent, View view,
		    int position, long id) {
		FirstUsageConfiguration.this.keyboardSkin = FirstUsageConfiguration.this.packageNames
		        .get(position);
		next();
	    }
	});
	AlertDialog.Builder keyboardStylesBuilder = new AlertDialog.Builder(
	        this.slideKeyboard);
	keyboardStylesBuilder.setView(chooseKeyboardStyleView);
	keyboardStylesBuilder.setTitle(R.string.keyboard_skin);
	AlertDialog alertDialog = keyboardStylesBuilder.create();
	this.dialogs.add(alertDialog);
	Window window = alertDialog.getWindow();
	WindowManager.LayoutParams lp = window.getAttributes();
	lp.token = view.getWindowToken();
	lp.type = WindowManager.LayoutParams.TYPE_APPLICATION_ATTACHED_DIALOG;
	window.setAttributes(lp);
	window.addFlags(WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM);

	View chooseKeyboardTypeView = ViewTools.inflateView(this.slideKeyboard,
	        R.layout.choose_keyboard_type);
	Button alphabeticButton = (Button) chooseKeyboardTypeView
	        .findViewById(R.id.alphabeticButton);
	alphabeticButton.setOnClickListener(new OnClickListener() {

	    @Override
	    public void onClick(View v) {
		FirstUsageConfiguration.this.keyboardTypes = SlideKeyboard.MODE_TEXT_ALPHA;
		next();
	    }
	});
	Button azertyButton = (Button) chooseKeyboardTypeView
	        .findViewById(R.id.azertyButton);
	azertyButton.setOnClickListener(new OnClickListener() {

	    @Override
	    public void onClick(View v) {
		FirstUsageConfiguration.this.keyboardTypes = SlideKeyboard.MODE_TEXT_AZERTY;
		next();
	    }
	});
	Button qwertyButton = (Button) chooseKeyboardTypeView
	        .findViewById(R.id.qwertyButton);
	qwertyButton.setOnClickListener(new OnClickListener() {

	    @Override
	    public void onClick(View v) {
		FirstUsageConfiguration.this.keyboardTypes = SlideKeyboard.MODE_TEXT_QWERTY;
		next();
	    }
	});
	Button dvorakButton = (Button) chooseKeyboardTypeView
	        .findViewById(R.id.dvorakButton);
	dvorakButton.setOnClickListener(new OnClickListener() {

	    @Override
	    public void onClick(View v) {
		FirstUsageConfiguration.this.keyboardTypes = SlideKeyboard.MODE_TEXT_DVORAK;
		next();
	    }
	});

	AlertDialog.Builder keyboardTypesDialogBuilder = new AlertDialog.Builder(
	        this.slideKeyboard);
	keyboardTypesDialogBuilder.setView(chooseKeyboardTypeView);
	keyboardTypesDialogBuilder.setTitle(R.string.keyboard_type);
	alertDialog = keyboardTypesDialogBuilder.create();
	this.dialogs.add(alertDialog);
	Window alertDialogWindow = alertDialog.getWindow();
	WindowManager.LayoutParams layoutParams = alertDialogWindow
	        .getAttributes();
	layoutParams.token = view.getWindowToken();
	layoutParams.type = WindowManager.LayoutParams.TYPE_APPLICATION_ATTACHED_DIALOG;
	alertDialogWindow.setAttributes(layoutParams);
	alertDialogWindow
	        .addFlags(WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM);
	
	View howDoesItWorkKeyboardTypeView = ViewTools.inflateView(this.slideKeyboard,
	        R.layout.how_does_it_work);
	Button doneButton = (Button) howDoesItWorkKeyboardTypeView
	        .findViewById(R.id.doneButton);
	doneButton.setOnClickListener(new OnClickListener() {

	    @Override
	    public void onClick(View v) {
		next();
	    }
	});
	
	AlertDialog.Builder howDoesItWorkDialogBuilder = new AlertDialog.Builder(
	        this.slideKeyboard);
	howDoesItWorkDialogBuilder.setView(howDoesItWorkKeyboardTypeView);
	howDoesItWorkDialogBuilder.setTitle(R.string.how_does_it_work);
	alertDialog = howDoesItWorkDialogBuilder.create();
	this.dialogs.add(alertDialog);
	Window howDoesItWorkDialogWindow = alertDialog.getWindow();
	WindowManager.LayoutParams howDoesItWorkLayoutParams = howDoesItWorkDialogWindow
	        .getAttributes();
	howDoesItWorkLayoutParams.token = view.getWindowToken();
	howDoesItWorkLayoutParams.type = WindowManager.LayoutParams.TYPE_APPLICATION_ATTACHED_DIALOG;
	howDoesItWorkDialogWindow.setAttributes(howDoesItWorkLayoutParams);
	howDoesItWorkDialogWindow
	        .addFlags(WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM);
    }

    /**
     * Displays the first dialog used for configuration.
     */
    public void start() {
	this.currentDialogIndex = 0;
	this.dialogs.get(this.currentDialogIndex).show();
    }

    private boolean next() {
	this.dialogs.get(this.currentDialogIndex).dismiss();
	this.currentDialogIndex++;
	if (this.currentDialogIndex < this.dialogs.size()) {
	    this.dialogs.get(this.currentDialogIndex).show();
	} else {
	    close();
	}

	return true;
    }

    /**
     * Closes the last dialog used for configuration.
     */
    private boolean close() {
	saveSettings();
	this.slideKeyboard.onStartInputView(
	        this.slideKeyboard.getCurrentInputEditorInfo(), true);

	return true;
    }

    private void saveSettings() {
	SharedPreferences sp = PreferenceManager
	        .getDefaultSharedPreferences(this.slideKeyboard);
	Editor editor = sp.edit();
	editor.putString(LatinIMESettings.PREF_KEYBOARD_SKIN,
	        this.keyboardSkin.toString());
	editor.putString(LatinIMESettings.PREF_KEYBOARD_TYPE, Integer.toString(this.keyboardTypes));
	editor.putBoolean(LatinIMESettings.PREF_FIRST_USAGE, false);
	editor.commit();
    }
}
