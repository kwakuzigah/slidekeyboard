package com.asigbe.slidekeyboardpro;

import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.CursorJoiner;
import android.database.MatrixCursor;
import android.graphics.Typeface;
import android.inputmethodservice.InputMethodService;
import android.inputmethodservice.Keyboard;
import android.media.AudioManager;
import android.os.Debug;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.provider.Contacts;
import android.provider.Contacts.People;
import android.provider.UserDictionary;
import android.text.InputType;
import android.text.TextUtils;
import android.util.PrintWriterPrinter;
import android.util.Printer;
import android.view.Display;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.CompletionInfo;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.SlidingDrawer;
import android.widget.SlidingDrawer.OnDrawerCloseListener;
import android.widget.SlidingDrawer.OnDrawerOpenListener;
import android.widget.TextView;

import com.asigbe.inputmethod.latin.Suggest;
import com.asigbe.inputmethod.latin.Suggest.Suggestion;
import com.asigbe.inputmethod.latin.TextEntryState;
import com.asigbe.inputmethod.latin.WordComposer;
import com.asigbe.slidekeyboard.provider.ContactContractsWrapper;
import com.asigbe.slidekeyboard.view.GestureLibraryWrapper;
import com.asigbe.slidekeyboard.view.GestureOverlayViewWrapper;
import com.asigbe.utils.ReportSender;
import com.asigbe.view.CustomGridView;
import com.asigbe.view.CustomGridView.OnItemTouchListener;
import com.asigbe.view.ViewTools;

//import com.flurry.android.FlurryAgent;

/**
 * Input method implementation for Qwerty'ish keyboard.
 */
@SuppressWarnings("deprecation")
public class SlideKeyboard extends InputMethodService implements
        SlideKeyboardView.OnKeyboardActionListener {
    private static final int   DOUBLE_CLICK_TIMEOUT           = 450;
    public static final String DICTIONARY_PACKAGE             = "com.asigbe.dictionary";
    public static final String SKIN_PACKAGE                   = "com.asigbe.slidekeyboard.skin";
    static final boolean       DEBUG                          = false;
    static final boolean       TRACE                          = false;

    static boolean             IS_GESTURE_OVERLAY_SUPPORTED   = false;
    static boolean             IS_CONTACT_CONTRACTS_SUPPORTED = false;

    static {
	try {
	    GestureOverlayViewWrapper.checkAvailable();
	    IS_GESTURE_OVERLAY_SUPPORTED = true;
	} catch (Throwable t) {
	    IS_GESTURE_OVERLAY_SUPPORTED = false;
	}

	try {
	    ContactContractsWrapper.checkAvailable();
	    IS_CONTACT_CONTRACTS_SUPPORTED = true;
	} catch (Throwable t) {
	    IS_CONTACT_CONTRACTS_SUPPORTED = false;
	}
    }

    /**
     * Adapter used to display icons of the toolbar.
     * 
     * @author Delali Zigah
     */
    private static class ToolBarAdapter extends BaseAdapter {
	private final Context           context;

	// references to our images
	public static final Integer[][] TOOLBAR_THUMBS = new Integer[][] {
	                                                       {
	                                                               R.drawable.cut_icon,
	                                                               R.drawable.copy_icon,
	                                                               R.drawable.paste_icon,
	                                                               R.drawable.select_icon,
	                                                               R.drawable.select_all_icon,
	                                                               // R.drawable.contact_icon,
	                                                               // R.drawable.person_icon,
	                                                               R.drawable.phone_icon,
	                                                               // R.drawable.phone_icon_2,
	                                                               R.drawable.email_icon,
	                                                               // R.drawable.house_icon,
	                                                               // R.drawable.house_icon_2,
	                                                               R.drawable.tools_icon },
	                                                       {
	                                                               R.string.cut,
	                                                               R.string.copy,
	                                                               R.string.paste,
	                                                               R.string.select,
	                                                               R.string.select_all,
	                                                               // R.string.contact,
	                                                               // R.string.person,
	                                                               R.string.phone,
	                                                               // R.string.phone_v2,
	                                                               R.string.email,
	                                                               // R.string.address,
	                                                               // R.string.address_v2,
	                                                               R.string.settings } };

	/**
	 * Creates a toolbar adapter with the input method.
	 */
	public ToolBarAdapter(Context c) {
	    this.context = c;
	}

	public int getCount() {
	    return TOOLBAR_THUMBS[0].length;
	}

	public Object getItem(int position) {
	    return null;
	}

	public long getItemId(int position) {
	    return 0;
	}

	// create a new ImageView for each item referenced by the Adapter
	public View getView(int position, View convertView, ViewGroup parent) {
	    if (convertView == null) { // if it's not recycled, initialize some
		// attributes
		convertView = ViewTools.inflateView(this.context,
		        R.layout.labeled_icon);
		convertView.setLayoutParams(new GridView.LayoutParams(67, 67));

	    }
	    ImageView imageView = (ImageView) convertView
		    .findViewById(R.id.iconImageView);
	    imageView.setImageResource(TOOLBAR_THUMBS[0][position]);

	    TextView textView = (TextView) convertView
		    .findViewById(R.id.iconTextView);
	    textView.setText(TOOLBAR_THUMBS[1][position]);

	    return convertView;
	}

    }

    private static final int          MSG_UPDATE_SUGGESTIONS              = 0;
    private static final int          MSG_START_FIRST_USAGE_CONFIGURATION = 1;
    // private static final int MSG_SWITCH_KEYBOARD = 2;

    // How many continuous deletes at which to start deleting at a higher speed.
    private static final int          DELETE_ACCELERATE_AT                = 20;
    // Key events coming any faster than this are long-presses.
    private static final int          QUICK_PRESS                         = 200;

    private static final int          KEYCODE_ENTER                       = 10;
    private static final int          KEYCODE_SPACE                       = ' ';

    // Contextual menu positions
    private static final int          POS_DELETE_WORD                     = 0;

    public static final String        STANDARD_SKIN                       = SlideKeyboard.class
	                                                                          .getPackage()
	                                                                          .getName();

    public static final int           MODE_TEXT_QWERTY                    = 0;
    public static final int           MODE_TEXT_AZERTY                    = 1;
    public static final int           MODE_TEXT_ALPHA                     = 2;
    public static final int           MODE_TEXT_DVORAK                    = 3;
    public static final int           MODE_TEXT_COUNT                     = 4;

    public static final String        GESTURE_RIGHT_UP                    = "right_up";
    public static final String        GESTURE_DOWN_LEFT                   = "down_left";
    public static final String        GESTURE_LEFT                        = "left";
    public static final String        GESTURE_RIGHT                       = "right";
    public static final String        GESTURE_LEFT_PARENTHESIS            = "left_parenthesis";
    public static final String        GESTURE_RIGHT_PARENTHESIS           = "right_parenthesis";
    private static final String[]     KEYBOARD_TYPES                      = {
	    "qwerty", "azerty", "alphabetic", "dvorak"                   };

    // private static final int POS_GET_CONTACT = 0;
    // private static final int POS_GET_PERSON = 1;
    // private static final int POS_GET_PHONE = 2;
    // private static final int POS_GET_PHONE_V2 = 3;
    // private static final int POS_GET_POSTAL_ADDRESS = 4;
    // private static final int POS_GET_POSTAL_ADDRESS_V2 = 5;
    // private static final int POS_METHOD = 1;

    private View                      slideKeyboardView;
    private SlideKeyboardView         inputView;
    private CandidateViewContainer    candidateViewContainer;
    private CandidateView             candidateView;
    private Suggest                   suggest;
    private CompletionInfo[]          completions;
    private FirstUsageConfiguration   firstUsageConfiguration;
    private AlertDialog               contactPhonesDialog;
    private Cursor                    contactPhonesCursor;
    private String                    language                            = new String();
    private boolean                   displayKeyPreview;

    private AlertDialog               optionsDialog;

    KeyboardSwitcher                  keyboardSwitcher;

    // private String mLocale;

    private final StringBuilder       composing                           = new StringBuilder();
    private final WordComposer        word                                = new WordComposer();
    private int                       committedLength;
    private boolean                   predicting;
    private CharSequence              bestWord;
    private boolean                   predictionOn;
    private boolean                   completionOn;
    private boolean                   autoSpace;
    private boolean                   autoCorrectOn;
    private boolean                   isShiftLocked;
    private boolean                   vibrateOn;
    private boolean                   soundOn;
    private boolean                   autoCap;
    private int                       sensitivity;
    private int                       repeatInterval;
    private boolean                   quickFixes;
    private boolean                   showSuggestions;
    private boolean                   autoComplete;
    private int                       correctionMode;
    // Indicates whether the suggestion strip is to be on in landscape
    private boolean                   justAccepted;
    private CharSequence              justRevertedSeparator;
    private int                       deleteCount;
    private long                      lastKeyTime;

    private Vibrator                  vibrator;
    private long                      vibrateDuration;

    private AudioManager              audioManager;
    private final float               FX_VOLUME                           = 1.0f;
    private boolean                   silentMode;

    private String                    wordSeparators;
    private String                    sentenceSeparators;
    private GestureLibraryWrapper     library;
    // private long lastActionPad;

    private boolean                   autoAddDictionary;
    private boolean                   gestureShortcut;
    private boolean                   displayToolbar;
    private int                       keyboardType;
    private String                    keyboardSkinPackage;
    private int                       longPressTimeOut;
    // private MovementDetector mMovementDetector;

    Handler                           handler                             = new Handler() {
	                                                                      @Override
	                                                                      public void handleMessage(
	                                                                              Message msg) {
		                                                                  switch (msg.what) {
		                                                                  case MSG_UPDATE_SUGGESTIONS:
		                                                                      updateSuggestions();
		                                                                      break;
		                                                                  // case
		                                                                  // MSG_SWITCH_KEYBOARD:
		                                                                  // SlideKeyboard.this.keyboardSwitcher
		                                                                  // .setActiveKeyboard((String)
		                                                                  // msg.obj);
		                                                                  // break;
		                                                                  case MSG_START_FIRST_USAGE_CONFIGURATION:
		                                                                      if (SlideKeyboard.this.firstUsageConfiguration == null) {
			                                                                  if (SlideKeyboard.this.inputView
			                                                                          .isShown()) {
			                                                                      SlideKeyboard.this.firstUsageConfiguration = new FirstUsageConfiguration(
			                                                                              SlideKeyboard.this,
			                                                                              SlideKeyboard.this.inputView);
			                                                                      SlideKeyboard.this.firstUsageConfiguration
			                                                                              .start();
			                                                                  } else {
			                                                                      sendMessageDelayed(
			                                                                              obtainMessage(MSG_START_FIRST_USAGE_CONFIGURATION),
			                                                                              100);
			                                                                  }
		                                                                      }
		                                                                      break;
		                                                                  }
	                                                                      }
	                                                                  };
    private int                       inputType;

    private SlidingDrawer             slidingDrawer;
    private GestureOverlayViewWrapper gestureOverlayView;
    private boolean                   firstUsage                          = true;
    private ContactContractsWrapper   contactContractsWrapper;
    private Cursor                    contactEmailsCursor;
    private AlertDialog               contactEmailsDialog;
    private String                    currentLanguage                     = new String();
    private int                       lastVersionCode;
    private String                    lastVersionName;
    private String                    lastKeyboardSkinPackage;
    private static final Typeface     DEFAULT_TYPEFACE                    = Typeface
	                                                                          .defaultFromStyle(Typeface.NORMAL);
    private static final Typeface     DEFAULT_BOLD_TYPEFACE               = Typeface
	                                                                          .defaultFromStyle(Typeface.NORMAL);
    private static final Typeface     DEFAULT_ITALIC_TYPEFACE             = Typeface
	                                                                          .defaultFromStyle(Typeface.NORMAL);
    public static Typeface            textTypeFace                        = DEFAULT_TYPEFACE;
    public static Typeface            boldTextTypeFace                    = DEFAULT_BOLD_TYPEFACE;
    public static Typeface            italicTextTypeFace                  = DEFAULT_ITALIC_TYPEFACE;

    private boolean                   useTemporarySwitch;

    private boolean                   isLongPress;
    private boolean                   orientationHasChanged;

    // private Context skinContext;

    @Override
    public void onCreate() {
	this.keyboardSwitcher = new KeyboardSwitcher(this);

	ReportSender.install(this);
	this.wordSeparators = getResources()
	        .getString(R.string.word_separators);
	if (IS_GESTURE_OVERLAY_SUPPORTED) {

	    this.library = new GestureLibraryWrapper(this);
	    if (!this.library.load()) {
		throw new RuntimeException("Gesture libraries not loaded");
	    }
	    // mMovementDetector = new MovementDetector(getBaseContext(),
	    // new PitchingMovement());
	}

	if (IS_CONTACT_CONTRACTS_SUPPORTED) {
	    this.contactContractsWrapper = new ContactContractsWrapper(this);
	}

	// setStatusIcon(R.drawable.ime_qwerty);

	this.vibrateDuration = getResources().getInteger(
	        R.integer.vibrate_duration_ms);

	// register to receive ringer mode changes for silent mode
	IntentFilter filter = new IntentFilter(
	        AudioManager.RINGER_MODE_CHANGED_ACTION);
	registerReceiver(this.mReceiver, filter);

	super.onCreate();
    }

    protected boolean getFirstUsage() {
	return this.firstUsage;
    }

    private void initSuggest() {
	if (this.currentLanguage.equals(this.language)) {
	    try {
		// verify if the dictionary is available
		getPackageManager().getResourcesForApplication(
		        DICTIONARY_PACKAGE + "." + this.language);
	    } catch (NameNotFoundException e) {
		this.suggest = new Suggest(this);
		this.currentLanguage = "";
	    }
	    return;
	}
	try {
	    Resources resourcesForApplication = getPackageManager()
		    .getResourcesForApplication(
		            DICTIONARY_PACKAGE + "." + this.language);
	    this.currentLanguage = this.language;
	    this.suggest = new Suggest(this,
		    resourcesForApplication.getAssets());
	} catch (NameNotFoundException e) {
	    this.suggest = new Suggest(this);
	    this.currentLanguage = "";
	}
	if (this.suggest != null) {
	    this.suggest.setCorrectionMode(this.correctionMode);
	}
	this.sentenceSeparators = getResources().getString(
	        R.string.sentence_separators);
    }

    @Override
    public void onDestroy() {
	unregisterReceiver(this.mReceiver);
	if (this.contactPhonesCursor != null) {
	    this.contactPhonesCursor.close();
	}
	if (this.contactEmailsCursor != null) {
	    this.contactEmailsCursor.close();
	}
	super.onDestroy();
    }

    @Override
    public View onCreateInputView() {
	int keyboard = (IS_GESTURE_OVERLAY_SUPPORTED) ? R.layout.keyboardview
	        : R.layout.keyboardview_for_old_version;

	this.slideKeyboardView = ViewTools.inflateView(this, keyboard);
	if (IS_GESTURE_OVERLAY_SUPPORTED) {
	    this.gestureOverlayView = new GestureOverlayViewWrapper(
		    this.slideKeyboardView
		            .findViewById(R.id.gestureOverlayView));
	}
	this.slidingDrawer = (SlidingDrawer) this.slideKeyboardView
	        .findViewById(R.id.drawer);
	final ImageView handle = (ImageView) this.slideKeyboardView
	        .findViewById(R.id.handle);
	OnDrawerCloseListener onDrawerCloseListener = new OnDrawerCloseListener() {

	    @Override
	    public void onDrawerClosed() {
		handle.setImageResource(R.drawable.handle_drawer_closed);
	    }
	};
	this.slidingDrawer.setOnDrawerCloseListener(onDrawerCloseListener);
	OnDrawerOpenListener onDrawerOpenListener = new OnDrawerOpenListener() {

	    @Override
	    public void onDrawerOpened() {
		handle.setImageResource(R.drawable.handle_drawer_opened);
	    }
	};
	this.slidingDrawer.setOnDrawerOpenListener(onDrawerOpenListener);

	final CustomGridView gridView = (CustomGridView) this.slideKeyboardView
	        .findViewById(R.id.toolbar);
	ToolBarAdapter adapter = new ToolBarAdapter(this);
	gridView.setAdapter(adapter);
	gridView.setOnItemTouchListener(new OnItemTouchListener() {

	    @Override
	    public void onItemTouch(int position) {

		Intent intent = new Intent(Intent.ACTION_PICK);
		intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		InputConnection currentInputConnection = getCurrentInputConnection();
		if (position != AdapterView.INVALID_POSITION) {
		    // slidingDrawer.close();
		    switch (ToolBarAdapter.TOOLBAR_THUMBS[0][position]) {
		    case R.drawable.cut_icon:
			currentInputConnection
			        .performContextMenuAction(android.R.id.cut);
			break;
		    case R.drawable.copy_icon:
			currentInputConnection
			        .performContextMenuAction(android.R.id.copy);
			break;
		    case R.drawable.paste_icon:
			currentInputConnection
			        .performContextMenuAction(android.R.id.paste);
			break;
		    case R.drawable.select_icon:
			currentInputConnection
			        .performContextMenuAction(android.R.id.startSelectingText);
			break;
		    case R.drawable.select_all_icon:
			currentInputConnection
			        .performContextMenuAction(android.R.id.selectAll);
			break;
		    // case R.drawable.contact_icon:
		    // break;
		    // case R.drawable.person_icon:
		    // break;
		    case R.drawable.phone_icon:
			SlideKeyboard.this.slidingDrawer.close();
			choosePhones();
			break;
		    // case R.drawable.phone_icon_2:
		    // break;
		    case R.drawable.email_icon:
			SlideKeyboard.this.slidingDrawer.close();
			chooseEmails();
			break;
		    // case R.drawable.house_icon:
		    // SlideKeyboard.this.slidingDrawer[index].close();
		    // chooseAddresses();
		    // break;
		    // case R.drawable.house_icon_2:
		    // break;
		    case R.drawable.tools_icon:
			SlideKeyboard.this.slidingDrawer.close();
			launchSettings();
			break;
		    default:
			break;
		    }
		}
	    }
	});
	return this.slideKeyboardView;
    }

    private void choosePhones() {
	// synchronized (SlideKeyboard.this.contactPhonesDialog) {
	if (this.contactPhonesDialog == null) {
	    String[] fields = null;
	    if (IS_CONTACT_CONTRACTS_SUPPORTED) {
		this.contactPhonesCursor = this.contactContractsWrapper
		        .getContactsPhones();
		fields = this.contactContractsWrapper.getContactsPhonesFields();
	    } else {
		this.contactPhonesCursor = getContactsPhones();
		fields = getContactsPhonesFields();
	    }
	    this.contactPhonesDialog = createContactDialog(
		    this.contactPhonesCursor, fields);
	}
	this.contactPhonesDialog.show();
	// }
    }

    private String[] getContactsPhonesFields() {
	return new String[] { People.DISPLAY_NAME, People.NUMBER };
    }

    private Cursor getContactsPhones() {
	return getContentResolver()
	        .query(People.CONTENT_URI,
	                new String[] { People._ID, People.DISPLAY_NAME,
	                        People.NUMBER },
	                People.NUMBER + " NOT NULL AND " + People.NUMBER
	                        + "<> ''", null, People.DISPLAY_NAME + " ASC");
    }

    private void chooseEmails() {
	// synchronized (SlideKeyboard.this.contactEmailsDialog) {
	if (this.contactEmailsDialog == null) {
	    String[] fields = null;
	    if (IS_CONTACT_CONTRACTS_SUPPORTED) {
		this.contactEmailsCursor = this.contactContractsWrapper
		        .getContactsEmails();
		fields = this.contactContractsWrapper.getContactsEmailsFields();
	    } else {
		this.contactEmailsCursor = getContactsEmails();
		fields = getContactsEmailsFields();
	    }
	    this.contactEmailsDialog = createContactDialog(
		    this.contactEmailsCursor, fields);
	}
	this.contactEmailsDialog.show();
	// }
    }

    private String[] getContactsEmailsFields() {
	return new String[] { "name", "_mail" };
    }

    private Cursor getContactsEmails() {
	ContentResolver contentResolver = getContentResolver();
	Cursor peopleCursor = contentResolver.query(
	        Contacts.People.CONTENT_URI, new String[] {
	                Contacts.People._ID, Contacts.People.DISPLAY_NAME },
	        null, null, People.DISPLAY_NAME + " ASC");
	Cursor mailCursor = contentResolver
	        .query(Contacts.ContactMethods.CONTENT_URI, new String[] {
	                Contacts.ContactMethods.PERSON_ID,
	                Contacts.ContactMethods.DATA },
	                Contacts.ContactMethods.KIND + "='"
	                        + Contacts.KIND_EMAIL + "'", null,
	                Contacts.ContactMethods.PERSON_ID);
	CursorJoiner joiner = new CursorJoiner(peopleCursor,
	        new String[] { Contacts.People._ID }, mailCursor,
	        new String[] { Contacts.ContactMethods.PERSON_ID });

	MatrixCursor cursor = new MatrixCursor(new String[] { "_id", "name",
	        "_mail" }, 10);
	for (CursorJoiner.Result joinerResult : joiner) {
	    switch (joinerResult) {
	    case BOTH:
		String id = peopleCursor.getString(0);
		String name = peopleCursor.getString(1);
		String mail = mailCursor.getString(1);
		cursor.addRow(new String[] { id, name, mail });
		break;
	    }
	}

	return cursor;
    }

    // private void chooseAddresses() {
    //
    // if (this.contactAddressesDialog == null) {
    //
    // this.contactAddressesCursor = this.contactContractsWrapper
    // .getContactsAddresses();
    // String[] fields = this.contactContractsWrapper
    // .getContactsAddressesFields();
    // this.contactAddressesDialog = createContactDialog(
    // this.contactEmailsCursor, fields);
    // }
    // this.contactAddressesDialog.show();
    // }

    private AlertDialog createContactDialog(Cursor cursor, String[] fields) {
	final SimpleCursorAdapter adapter = new SimpleCursorAdapter(this,
	        R.layout.contact_layout, cursor, fields, new int[] {
	                R.id.contactNameTextView,
	                R.id.contactInformationTextView });

	ListView contactsListView = new ListView(this);
	contactsListView.setAdapter(adapter);

	AlertDialog.Builder builder = new AlertDialog.Builder(this);
	builder.setView(contactsListView);
	final AlertDialog alertDialog = builder.create();
	Window window = alertDialog.getWindow();
	WindowManager.LayoutParams lp = window.getAttributes();
	lp.token = this.inputView.getWindowToken();
	lp.type = WindowManager.LayoutParams.TYPE_APPLICATION_ATTACHED_DIALOG;
	window.setAttributes(lp);
	window.addFlags(WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM);

	contactsListView.setOnItemClickListener(new OnItemClickListener() {

	    @Override
	    public void onItemClick(AdapterView<?> parent, View view,
		    int position, long id) {
		onText(((TextView) view
		        .findViewById(R.id.contactInformationTextView))
		        .getText());
		alertDialog.dismiss();
	    }
	});
	return alertDialog;
    }

    @Override
    public View onCreateCandidatesView() {
	this.candidateViewContainer = (CandidateViewContainer) getLayoutInflater()
	        .inflate(R.layout.candidates, null);
	this.candidateViewContainer.initViews();
	this.candidateView = (CandidateView) this.candidateViewContainer
	        .findViewById(R.id.candidates);
	this.candidateView.setService(this);
	setCandidatesViewShown(true);
	return this.candidateViewContainer;
    }

    @Override
    public void onStartInputView(EditorInfo attribute, boolean restarting) {
	loadSettings();
	initSuggest();
	chooseSlideKeyboardView();

	// mMovementDetector.resume();

	// In landscape mode, this method gets called without the input view
	// being created.
	if (this.inputView == null) {
	    return;
	}

	this.inputType = attribute.inputType;

	TextEntryState.newSession(this);

	this.predictionOn = false;
	this.completionOn = false;
	this.completions = null;
	this.isShiftLocked = false;
	// switch (attribute.inputType & InputType.TYPE_MASK_CLASS) {
	// case InputType.TYPE_CLASS_DATETIME:
	// case InputType.TYPE_CLASS_NUMBER:
	// this.keyboardSwitcher.setKeyboardMode(AsigbeKeyboard.MODE_NUMBERS,
	// attribute.imeOptions);
	// break;
	// case InputType.TYPE_CLASS_PHONE:
	// this.keyboardSwitcher.setKeyboardMode(AsigbeKeyboard.MODE_PHONE,
	// attribute.imeOptions);
	// break;
	// case InputType.TYPE_CLASS_TEXT:
	// this.keyboardSwitcher.setKeyboardMode(AsigbeKeyboard.MODE_TEXT,
	// attribute.imeOptions);
	// // startPrediction();
	// this.predictionOn = true;
	// // Make sure that passwords are not displayed in candidate view
	// int variation = attribute.inputType & InputType.TYPE_MASK_VARIATION;
	// if ((variation == InputType.TYPE_TEXT_VARIATION_PASSWORD)
	// || (variation == InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD)) {
	// this.predictionOn = false;
	// }
	// if ((variation == InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS)
	// || (variation == InputType.TYPE_TEXT_VARIATION_PERSON_NAME)) {
	// this.autoSpace = false;
	// } else {
	// this.autoSpace = true;
	// }
	// if (variation == InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS) {
	// this.predictionOn = false;
	// this.keyboardSwitcher.setKeyboardMode(
	// AsigbeKeyboard.MODE_EMAIL, attribute.imeOptions);
	// } else if (variation == InputType.TYPE_TEXT_VARIATION_URI) {
	// this.predictionOn = false;
	// this.keyboardSwitcher.setKeyboardMode(AsigbeKeyboard.MODE_URL,
	// attribute.imeOptions);
	// } else if (variation == InputType.TYPE_TEXT_VARIATION_SHORT_MESSAGE)
	// {
	// this.keyboardSwitcher.setKeyboardMode(AsigbeKeyboard.MODE_IM,
	// attribute.imeOptions);
	// } else if (variation == InputType.TYPE_TEXT_VARIATION_FILTER) {
	// this.predictionOn = false;
	// }
	// if ((attribute.inputType & InputType.TYPE_TEXT_FLAG_AUTO_COMPLETE) !=
	// 0) {
	// this.predictionOn = false;
	// this.completionOn = true && isFullscreenMode();
	// }
	// updateShiftKeyState(attribute);
	// break;
	// default:
	// this.keyboardSwitcher.setKeyboardMode(AsigbeKeyboard.MODE_TEXT,
	// attribute.imeOptions);
	// updateShiftKeyState(attribute);
	// }
	switch (attribute.inputType & InputType.TYPE_MASK_CLASS) {
	case InputType.TYPE_CLASS_DATETIME:
	case InputType.TYPE_CLASS_NUMBER:
	case InputType.TYPE_CLASS_PHONE:
	    this.keyboardSwitcher
		    .setActiveKeyboard(KEYBOARD_TYPES[this.keyboardType]
		            + "_keyboard_symbols");
	    break;
	default:
	    this.keyboardSwitcher
		    .setActiveKeyboard(KEYBOARD_TYPES[this.keyboardType]
		            + "_keyboard");
	    break;
	}
	this.inputView.closing();
	this.composing.setLength(0);
	this.predicting = false;
	this.deleteCount = 0;
	setCandidatesViewShown(false);
	if (this.candidateView != null) {
	    this.candidateView.setSuggestions(null, false, false, false);
	}

	if (IS_GESTURE_OVERLAY_SUPPORTED) {
	    this.gestureOverlayView.removeAllOnGesturePerformedListeners();
	    if (this.gestureShortcut) {
		this.gestureOverlayView.addOnGesturePerformedListener(this,
		        this.inputView, this.library);
	    }

	    if (!this.displayToolbar) {
		this.slidingDrawer.setVisibility(View.GONE);
	    } else {
		this.slidingDrawer.setVisibility(View.VISIBLE);
	    }
	}

	if (!this.displayToolbar) {
	    this.slidingDrawer.setVisibility(View.GONE);
	} else {
	    this.slidingDrawer.setVisibility(View.VISIBLE);
	    WindowManager wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
	    Display display = wm.getDefaultDisplay();

	    ImageView handle = (ImageView) slideKeyboardView
		    .findViewById(R.id.handle);
	    this.slidingDrawer.setLayoutParams(new FrameLayout.LayoutParams(
		    display.getWidth(), this.inputView.getKeyboard()
		            .getHeight()
		            + this.inputView.getPaddingTop()
		            + this.inputView.getPaddingBottom()
		            + handle.getDrawable().getIntrinsicHeight()));
	    this.slidingDrawer.invalidate();
	}
	this.inputView.setSensitivity(this.sensitivity);
	this.inputView.setDisplayKeyPreview(this.displayKeyPreview);
	this.inputView.setRepeatInterval(this.repeatInterval);
	this.inputView.setLongPressTimeOut(this.longPressTimeOut);
	this.inputView.setProximityCorrectionEnabled(true);
	if (this.suggest != null) {
	    this.suggest.setCorrectionMode(this.correctionMode);
	}
	this.predictionOn = this.predictionOn && (this.correctionMode > 0);
	this.inputView.setTemporarySwitch(false);

	// mMainView.requestLayout();
	// mMainView.invalidate();

	updateShiftKeyState(getCurrentInputEditorInfo());

	if (this.firstUsage) {
	    this.handler.sendMessageDelayed(this.handler
		    .obtainMessage(MSG_START_FIRST_USAGE_CONFIGURATION), 500);
	}
    }

    // @Override
    // public void onStartInput(EditorInfo attribute, boolean restarting) {
    // super.onStartInput(attribute, restarting);
    // FlurryAgent.onStartSession(this, "5XHEQ1MW5QHAVP9SELFL");
    // }

    private void chooseSlideKeyboardView() {

	boolean mustReinstall = false;
	try {
	    PackageInfo packageInfo = getPackageManager().getPackageInfo(
		    this.keyboardSkinPackage, 0);
	    if (packageInfo.versionCode != this.lastVersionCode
		    || !packageInfo.versionName.equals(this.lastVersionName)
		    || !this.keyboardSkinPackage
		            .equals(this.lastKeyboardSkinPackage)) {
		this.lastVersionCode = packageInfo.versionCode;
		this.lastVersionName = packageInfo.versionName;
		this.lastKeyboardSkinPackage = this.keyboardSkinPackage;
		mustReinstall = true;
	    }
	} catch (NameNotFoundException e) {
	    // TODO Auto-generated catch block
	    e.printStackTrace();
	}
	Context skinContext = ViewTools.getPackageContext(
	        getApplicationContext(), this.keyboardSkinPackage);
	// skin does not exist anymore
	if (skinContext == null) {
	    this.keyboardSkinPackage = STANDARD_SKIN;
	    skinContext = ViewTools.getPackageContext(getApplicationContext(),
		    this.keyboardSkinPackage);
	}
	if (mustReinstall) {
	    // load the new fonts
	    SlideKeyboard.textTypeFace = Typeface.createFromAsset(
		    skinContext.getAssets(), "fonts/font.ttf");
	    SlideKeyboard.boldTextTypeFace = Typeface.create(
		    SlideKeyboard.textTypeFace, Typeface.BOLD);
	    SlideKeyboard.italicTextTypeFace = Typeface.create(
		    SlideKeyboard.textTypeFace, Typeface.ITALIC);
	}

	this.keyboardSwitcher.setKeyboardSkin(skinContext,
	        this.keyboardSkinPackage);
	this.inputView = (SlideKeyboardView) this.slideKeyboardView
	        .findViewById(R.id.keyboardView);
	this.keyboardSwitcher.setInputView(this.inputView);
	this.inputView.setOnKeyboardActionListener(this);
	this.inputView.invalidate();

	// for (Map.Entry<String, View> entry : keyboardViews.entrySet()) {
	// if (entry.getKey().equals(this.keyboardSkin)) {
	// entry.getValue().setVisibility(View.VISIBLE);
	// this.inputView = (SlideKeyboardView) entry.getValue()
	// .findViewById(R.id.keyboardView);
	// this.keyboardSwitcher.setInputView(this.inputView);
	// this.inputView.setOnKeyboardActionListener(this);
	// } else {
	// entry.getValue().setVisibility(View.GONE);
	// }
	// }
    }

    @Override
    public void onFinishInput() {
	super.onFinishInput();

	if (this.inputView != null) {
	    this.inputView.closing();
	}
	// FlurryAgent.onEndSession(this);
    }

    @Override
    public void onUpdateSelection(int oldSelStart, int oldSelEnd,
	    int newSelStart, int newSelEnd, int candidatesStart,
	    int candidatesEnd) {
	super.onUpdateSelection(oldSelStart, oldSelEnd, newSelStart, newSelEnd,
	        candidatesStart, candidatesEnd);
	// If the current selection in the text view changes, we should
	// clear whatever candidate text we have.
	if ((this.composing.length() > 0)
	        && this.predicting
	        && ((newSelStart != candidatesEnd) || (newSelEnd != candidatesEnd))) {
	    this.composing.setLength(0);
	    this.predicting = false;
	    updateSuggestions();
	    TextEntryState.reset();
	    InputConnection ic = getCurrentInputConnection();
	    if (ic != null) {
		ic.finishComposingText();
	    }
	} else if (!this.predicting
	        && !this.justAccepted
	        && (TextEntryState.getState() == TextEntryState.STATE_ACCEPTED_DEFAULT)) {
	    TextEntryState.reset();
	}
	this.justAccepted = false;
    }

    @Override
    public void hideWindow() {
	if ((this.optionsDialog != null) && this.optionsDialog.isShowing()) {
	    this.optionsDialog.dismiss();
	    this.optionsDialog = null;
	}
	super.hideWindow();
	TextEntryState.endSession();
    }

    @Override
    public void onDisplayCompletions(CompletionInfo[] completions) {
	// if (false) {
	// Log.i("foo", "Received completions:");
	// for (int i = 0; i < (completions != null ? completions.length : 0);
	// i++) {
	// Log.i("foo", "  #" + i + ": " + completions[i]);
	// }
	// }
	if (this.completionOn) {
	    this.completions = completions;
	    if (completions == null) {
		this.candidateView.setSuggestions(null, false, false, false);
		return;
	    }

	    List<Suggestion> stringList = new ArrayList<Suggestion>();
	    for (int i = 0; i < (completions != null ? completions.length : 0); i++) {
		CompletionInfo ci = completions[i];
		if (ci != null) {
		    stringList.add(new Suggestion(ci.getText(), false));
		}
	    }
	    // CharSequence typedWord = mWord.getTypedWord();
	    this.candidateView.setSuggestions(stringList, true, true, true);
	    this.bestWord = null;
	    setCandidatesViewShown(isCandidateStripVisible()
		    || this.completionOn);
	}
    }

    @Override
    public void setCandidatesViewShown(boolean shown) {
	// TODO: Remove this if we support candidates with hard keyboard
	if (onEvaluateInputViewShown()) {
	    super.setCandidatesViewShown(shown);
	}
    }

    @Override
    public void onComputeInsets(InputMethodService.Insets outInsets) {
	super.onComputeInsets(outInsets);
	if (!isFullscreenMode()) {
	    outInsets.contentTopInsets = outInsets.visibleTopInsets;
	}
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
	switch (keyCode) {
	case KeyEvent.KEYCODE_BACK:
	    if ((event.getRepeatCount() == 0) && (this.inputView != null)) {
		if (this.inputView.handleBack()) {
		    return true;
		}
	    }
	    break;
	case KeyEvent.KEYCODE_DPAD_DOWN:
	case KeyEvent.KEYCODE_DPAD_UP:
	case KeyEvent.KEYCODE_DPAD_LEFT:
	case KeyEvent.KEYCODE_DPAD_RIGHT:
	    break;
	// case KeyEvent.KEYCODE_DPAD_CENTER:
	// if (this.inputType != InputType.TYPE_NULL) {
	// long now = System.currentTimeMillis();
	// if (now - this.lastActionPad < DOUBLE_CLICK_TIMEOUT) {
	// // we only do a permanent switch when there is a double
	// // click
	// if (this.clickCount == 1) {
	// this.inputView.setTemporarySwitch(false);
	// }
	// this.clickCount++;
	//
	// } else {
	// // we do a temporary switch when there is a simple click,
	// // except if we are already in a temporary switch, in this
	// // case we cancel the switch
	// this.keyboardSwitcher.switchKeyboard();
	// this.inputView.setTemporarySwitch(!this.inputView
	// .isTemporarySwitch());
	// this.clickCount = 1;
	// }
	// this.lastActionPad = now;
	// break;
	// }
	}
	return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
	switch (keyCode) {
	case KeyEvent.KEYCODE_DPAD_DOWN:
	case KeyEvent.KEYCODE_DPAD_UP:
	case KeyEvent.KEYCODE_DPAD_LEFT:
	case KeyEvent.KEYCODE_DPAD_RIGHT:
	    // Enable shift key and DPAD to do selections
	    if ((this.inputView != null) && this.inputView.isShown()
		    && this.inputView.isShifted()) {
		event = new KeyEvent(event.getDownTime(), event.getEventTime(),
		        event.getAction(), event.getKeyCode(),
		        event.getRepeatCount(), event.getDeviceId(),
		        event.getScanCode(), KeyEvent.META_SHIFT_LEFT_ON
		                | KeyEvent.META_SHIFT_ON);
		InputConnection ic = getCurrentInputConnection();
		if (ic != null) {
		    ic.sendKeyEvent(event);
		}
		return true;
	    }
	    break;
	}
	return super.onKeyUp(keyCode, event);
    }

    private void commitTyped(InputConnection inputConnection) {
	if (this.predicting) {
	    this.predicting = false;
	    if (this.composing.length() > 0) {
		if (inputConnection != null) {
		    inputConnection.commitText(this.composing, 1);
		}
		this.committedLength = this.composing.length();
		TextEntryState.acceptedTyped(this.composing);
	    }
	    updateSuggestions();
	}
    }

    /**
     * Switches between shift states.
     */
    public void updateShiftKeyState(EditorInfo attr) {
	InputConnection ic = getCurrentInputConnection();
	if ((attr != null) && (this.inputView != null) && (ic != null)) {
	    int caps = 0;
	    EditorInfo ei = getCurrentInputEditorInfo();
	    if (this.autoCap && (ei != null)
		    && (ei.inputType != InputType.TYPE_NULL)) {
		caps = ic.getCursorCapsMode(attr.inputType);
	    }
	    this.inputView.setShifted(this.isShiftLocked || (caps != 0),
		    this.isShiftLocked);
	}
    }

    private void swapPunctuationAndSpace() {
	final InputConnection ic = getCurrentInputConnection();
	if (ic == null) {
	    return;
	}
	CharSequence lastTwo = ic.getTextBeforeCursor(2, 0);
	if ((lastTwo != null) && (lastTwo.length() == 2)
	        && (lastTwo.charAt(0) == KEYCODE_SPACE)
	        && isSentenceSeparator(lastTwo.charAt(1))) {
	    ic.beginBatchEdit();
	    ic.deleteSurroundingText(2, 0);
	    ic.commitText(lastTwo.charAt(1) + " ", 1);
	    ic.endBatchEdit();
	    updateShiftKeyState(getCurrentInputEditorInfo());
	}
    }

    private void doubleSpace() {
	// if (!mAutoPunctuate) return;
	if (this.correctionMode == Suggest.CORRECTION_NONE) {
	    return;
	}
	final InputConnection ic = getCurrentInputConnection();
	if (ic == null) {
	    return;
	}
	CharSequence lastThree = ic.getTextBeforeCursor(3, 0);
	if ((lastThree != null) && (lastThree.length() == 3)
	        && Character.isLetterOrDigit(lastThree.charAt(0))
	        && (lastThree.charAt(1) == KEYCODE_SPACE)
	        && (lastThree.charAt(2) == KEYCODE_SPACE)) {
	    ic.beginBatchEdit();
	    ic.deleteSurroundingText(2, 0);
	    ic.commitText(". ", 1);
	    ic.endBatchEdit();
	    updateShiftKeyState(getCurrentInputEditorInfo());
	}
    }

    /**
     * Adds a new word to the user dictionary.
     */
    public boolean addWordToDictionary(CharSequence word) {
	try {
	    if ((word != null)
		    && (word.length() != 0)
		    && (this.suggest == null || !this.suggest
		            .containsWord(word))) {
		UserDictionary.Words.addWord(getBaseContext(), word.toString(),
		        255, UserDictionary.Words.LOCALE_TYPE_CURRENT);
	    }
	} catch (IllegalArgumentException e) {

	}

	return true;
    }

    private boolean isAlphabet(int code) {
	if (Character.isLetter(code)) {
	    return true;
	} else {
	    return false;
	}
    }

    // Implementation of KeyboardViewListener

    // case AsigbeKeyboard.KEYCODE_KEYBOARD_SWITCH:
    // // if (this.inputView.isTemporarySwitch()) {
    // // this.keyboardSwitcher.revertActiveKeyboard();
    // // this.inputView.setTemporarySwitch(false);
    // // } else {
    // // if (this.handler.hasMessages(MSG_SWITCH_KEYBOARD)) {
    // // this.inputView.setTemporarySwitch(false);
    // // } else {
    // // this.inputView.setTemporarySwitch(true);
    // // Message obtainMessage = this.handler
    // // .obtainMessage(MSG_SWITCH_KEYBOARD);
    // // obtainMessage.obj = option;
    // // this.handler.sendMessageDelayed(obtainMessage, 200);
    // // }
    // // }
    // // break;
    // this.keyboardSwitcher.setActiveKeyboard((String) option);
    // this.inputView.setTemporarySwitch(true);
    // break;

    public void onKeyDown(int primaryCode, int[] keyCodes, Object option) {
	// for multitouch
	// switch (primaryCode) {
	// case AsigbeKeyboard.KEYCODE_KEYBOARD_SWITCH:
	// this.lastTimeKeyboardSwitch = SystemClock.uptimeMillis();
	// if (this.inputView.isTemporarySwitch()) {
	// this.keyboardSwitcher.revertActiveKeyboard();
	// this.inputView.setTemporarySwitch(false);
	// } else {
	// this.keyboardSwitcher.setActiveKeyboard((String) option);
	// this.inputView.setTemporarySwitch(true);
	// }
	// break;
	// default:
	// break;
	// }
	this.isLongPress = false;
    }

    @Override
    public void onLongKeyDown(int primaryCode, int[] keyCodes, Object option) {
	if (!this.useTemporarySwitch) {
	    return;
	}
	this.isLongPress = true;
	switch (primaryCode) {
	case AsigbeKeyboard.KEYCODE_KEYBOARD_REVERT:
	    this.keyboardSwitcher.revertActiveKeyboard();
	    this.inputView.setTemporarySwitch(false);
	    break;
	case AsigbeKeyboard.KEYCODE_KEYBOARD_SWITCH:
	    this.keyboardSwitcher.setActiveKeyboard((String) option);
	    this.inputView.setTemporarySwitch(false);
	    break;
	case AsigbeKeyboard.KEYCODE_SHIFT_TOGGLE:
	    handleCapsLock();
	    break;
	default:
	    break;
	}
    }

    public void onKeyUp(int primaryCode, int[] keyCodes, Object option) {
	long when = SystemClock.uptimeMillis();
	if ((primaryCode != AsigbeKeyboard.KEYCODE_DELETE)
	        || (when > this.lastKeyTime + QUICK_PRESS)) {
	    this.deleteCount = 0;
	}
	this.lastKeyTime = when;
	switch (primaryCode) {
	case AsigbeKeyboard.KEYCODE_DELETE:
	    handleBackspace();
	    this.deleteCount++;
	    break;
	case AsigbeKeyboard.KEYCODE_SHIFT:
	    handleShift();
	    break;
	case AsigbeKeyboard.KEYCODE_SHIFT_TOGGLE:
	    if (this.useTemporarySwitch) {
		if (!this.isLongPress) {
		    handleShiftToggle();
		}
	    } else {
		handleCapsLock();
	    }
	    break;
	case AsigbeKeyboard.KEYCODE_CAPS_LOCK:
	    handleCapsLock();
	    break;
	case AsigbeKeyboard.KEYCODE_KEYBOARD_REVERT:
	    if (!this.isLongPress) {
		this.keyboardSwitcher.revertActiveKeyboard();
		this.inputView.setTemporarySwitch(false);
	    }
	    break;
	case AsigbeKeyboard.KEYCODE_CANCEL:
	    if ((this.optionsDialog == null) || !this.optionsDialog.isShowing()) {
		handleClose();
	    }
	    break;
	// case SlideKeyboardView.KEYCODE_OPTIONS:
	// showOptionsMenu();
	// break;
	// case SlideKeyboardView.KEYCODE_SHIFT_LONGPRESS:
	// if (mCapsLock) {
	// handleShift();
	// } else {
	// toggleCapsLock();
	// }
	// break;
	// case AsigbeKeyboard.KEYCODE_ALPHABETIC_CHANGE:
	// // Make sure that passwords are not displayed in candidate view
	// EditorInfo attribute = getCurrentInputEditorInfo();
	// int variation = attribute.inputType & InputType.TYPE_MASK_VARIATION;
	// if (variation == InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS) {
	// this.keyboardSwitcher.setKeyboardMode(
	// AsigbeKeyboard.MODE_EMAIL, attribute.imeOptions);
	// } else if (variation == InputType.TYPE_TEXT_VARIATION_URI) {
	// this.keyboardSwitcher.setKeyboardMode(AsigbeKeyboard.MODE_URL,
	// attribute.imeOptions);
	// } else if (variation == InputType.TYPE_TEXT_VARIATION_SHORT_MESSAGE)
	// {
	// this.keyboardSwitcher.setKeyboardMode(AsigbeKeyboard.MODE_IM,
	// attribute.imeOptions);
	// } else {
	// this.keyboardSwitcher.setKeyboardMode(AsigbeKeyboard.MODE_TEXT,
	// attribute.imeOptions);
	// }
	// // if (this.doubleClickSwitch) {
	// // if (when - this.lastAlphabeticChange < DOUBLE_CLICK_TIMEOUT) {
	// // this.inputView.setTemporarySwitch(false);
	// // }
	// // }
	// // this.lastAlphabeticChange = when;
	// break;
	// // case AsigbeKeyboard.KEYCODE_NUMBERS_CHANGE:
	// // mKeyboardSwitcher.setKeyboardMode(KeyboardSwitcher.MODE_NUMBERS);
	// // break;
	// case AsigbeKeyboard.KEYCODE_SYMBOL_CHANGE:
	// this.keyboardSwitcher.setKeyboardMode(AsigbeKeyboard.MODE_SYMBOLS);
	// this.inputView.setTemporarySwitch(false);
	// // this.lastSymbolChange = when;
	// break;

	case AsigbeKeyboard.KEYCODE_KEYBOARD_SWITCH:
	    if (this.useTemporarySwitch) {
		if (!this.isLongPress) {
		    if (this.inputView.isTemporarySwitch()) {
			this.keyboardSwitcher
			        .setActiveKeyboard((String) option);
			this.inputView.setTemporarySwitch(false);
		    } else {
			this.keyboardSwitcher
			        .setActiveKeyboard((String) option);
			this.inputView.setTemporarySwitch(true);
		    }
		}
	    } else {
		this.keyboardSwitcher.setActiveKeyboard((String) option);
	    }
	    break;
	case AsigbeKeyboard.KEYCODE_ENTER:
	    if (this.keyboardSwitcher.getKeyboardMode() == AsigbeKeyboard.MODE_IM) {
		break;
	    }
	default:
	    if (isWordSeparator(primaryCode)) {
		handleSeparator(primaryCode);
	    } else {
		handleCharacter(primaryCode, keyCodes);
	    }
	    // Cancel the just reverted state
	    this.justRevertedSeparator = null;
	    if (this.inputView.isTemporarySwitch()) {
		this.keyboardSwitcher.revertActiveKeyboard();
		this.inputView.setTemporarySwitch(false);
	    }
	    break;
	}
    }

    public void onText(CharSequence text) {
	InputConnection ic = getCurrentInputConnection();
	if (ic == null) {
	    return;
	}
	ic.beginBatchEdit();
	if (this.predicting) {
	    commitTyped(ic);
	}
	ic.commitText(text, 1);
	ic.endBatchEdit();
	updateShiftKeyState(getCurrentInputEditorInfo());
	this.justRevertedSeparator = null;
    }

    private void handleBackspace() {
	boolean deleteChar = false;
	InputConnection ic = getCurrentInputConnection();
	if (ic == null) {
	    return;
	}
	if (this.predicting) {
	    final int length = this.composing.length();
	    if (length > 0) {
		this.composing.delete(length - 1, length);
		this.word.deleteLast();
		ic.setComposingText(this.composing, 1);
		if (this.composing.length() == 0) {
		    this.predicting = false;
		}
		postUpdateSuggestions();
	    } else {
		ic.deleteSurroundingText(1, 0);
	    }
	} else {
	    deleteChar = true;
	}
	updateShiftKeyState(getCurrentInputEditorInfo());
	TextEntryState.backspace();
	if (TextEntryState.getState() == TextEntryState.STATE_UNDO_COMMIT) {
	    revertLastWord(deleteChar);
	    return;
	} else if (deleteChar) {
	    sendDownUpKeyEvents(KeyEvent.KEYCODE_DEL);
	    if (this.deleteCount > DELETE_ACCELERATE_AT) {
		sendDownUpKeyEvents(KeyEvent.KEYCODE_DEL);
	    }
	}
	this.justRevertedSeparator = null;
    }

    private void handleShift() {
	if (this.inputView.isShifted()) {
	    if (this.isShiftLocked) {
		// remove lock on shift
		this.isShiftLocked = false;
		this.inputView.setShifted(false, this.isShiftLocked);
	    } else {
		// set lock on shift
		this.isShiftLocked = true;
		this.inputView.setShifted(true, this.isShiftLocked);
	    }
	} else {
	    // set shift
	    this.isShiftLocked = false;
	    this.inputView.setShifted(true, this.isShiftLocked);
	}
    }

    private void handleShiftToggle() {
	if (this.inputView.isShifted()) {
	    // remove shift
	    this.isShiftLocked = false;
	    this.inputView.setShifted(false, false);
	} else {
	    // set shift
	    this.isShiftLocked = false;
	    this.inputView.setShifted(true, this.isShiftLocked);
	}
    }

    private void handleCapsLock() {
	if (this.isShiftLocked) {
	    this.isShiftLocked = false;
	    this.inputView.setShifted(false, this.isShiftLocked);
	} else {
	    this.isShiftLocked = true;
	    this.inputView.setShifted(true, this.isShiftLocked);
	}
    }

    private void handleCharacter(int primaryCode, int[] keyCodes) {
	if (isAlphabet(primaryCode) && isPredictionOn()
	        && !isCursorTouchingWord()) {
	    if (!this.predicting) {
		this.predicting = true;
		this.composing.setLength(0);
		this.word.reset();
	    }
	}
	if (this.inputView.isShifted()) {
	    primaryCode = Character.toUpperCase(primaryCode);
	}
	if (this.predicting) {
	    if (this.inputView.isShifted() && (this.composing.length() == 0)) {
		this.word.setCapitalized(true);
	    }
	    this.composing.append((char) primaryCode);
	    this.word.add(primaryCode, keyCodes);
	    InputConnection ic = getCurrentInputConnection();
	    if (ic != null) {
		ic.setComposingText(this.composing, 1);
	    }
	    postUpdateSuggestions();
	} else {
	    sendKeyChar((char) primaryCode);
	}
	updateShiftKeyState(getCurrentInputEditorInfo());
	TextEntryState.typedCharacter((char) primaryCode,
	        isWordSeparator(primaryCode));
    }

    private void handleSeparator(int primaryCode) {
	boolean pickedDefault = false;
	// Handle separator
	InputConnection ic = getCurrentInputConnection();
	if (ic != null) {
	    ic.beginBatchEdit();
	}
	if (this.predicting) {
	    // In certain languages where single quote is a separator, it's
	    // better
	    // not to auto correct, but accept the typed word. For instance,
	    // in Italian dov' should not be expanded to dove' because the
	    // elision
	    // requires the last vowel to be removed.
	    if (this.autoCorrectOn
		    && (primaryCode != '\'')
		    && ((this.justRevertedSeparator == null)
		            || (this.justRevertedSeparator.length() == 0) || (this.justRevertedSeparator
		            .charAt(0) != primaryCode))) {
		pickDefaultSuggestion();
		pickedDefault = true;
	    } else {
		commitTyped(ic);
	    }
	}
	sendKeyChar((char) primaryCode);
	TextEntryState.typedCharacter((char) primaryCode, true);
	if ((TextEntryState.getState() == TextEntryState.STATE_PUNCTUATION_AFTER_ACCEPTED)
	        && (primaryCode != KEYCODE_ENTER)) {
	    swapPunctuationAndSpace();
	} else if (isPredictionOn() && (primaryCode == ' ')) {
	    // else if (TextEntryState.STATE_SPACE_AFTER_ACCEPTED) {
	    doubleSpace();
	}
	if (pickedDefault && (this.bestWord != null)) {
	    TextEntryState.acceptedDefault(this.word.getTypedWord(),
		    this.bestWord);
	    if (this.autoAddDictionary) {
		addWordToDictionary(this.bestWord);
	    }
	}

	updateShiftKeyState(getCurrentInputEditorInfo());
	if (ic != null) {
	    ic.endBatchEdit();
	}
    }

    private void handleClose() {
	commitTyped(getCurrentInputConnection());
	requestHideSelf(0);
	this.inputView.closing();
	TextEntryState.endSession();
    }

    // private void checkToggleCapsLock() {
    // if (mInputView.getKeyboard().isShifted()) {
    // toggleCapsLock();
    // }
    // }

    // private void toggleCapsLock() {
    // if (mKeyboardSwitcher.isAlphabetMode()) {
    // ((AsigbeKeyboard) mInputView.getKeyboard())
    // .setShiftLocked(mCapsLock);
    // }
    // }

    private void postUpdateSuggestions() {
	this.handler.removeMessages(MSG_UPDATE_SUGGESTIONS);
	this.handler.sendMessageDelayed(
	        this.handler.obtainMessage(MSG_UPDATE_SUGGESTIONS), 100);
    }

    private boolean isPredictionOn() {
	boolean predictionOn = this.predictionOn;
	// if (isFullscreenMode()) predictionOn &= mPredictionLandscape;
	return predictionOn;
    }

    private boolean isCandidateStripVisible() {
	return isPredictionOn() && this.showSuggestions;
    }

    private void updateSuggestions() {
	// Check if we have a suggestion engine attached.
	if ((this.suggest == null) || !isPredictionOn()) {
	    return;
	}

	if (!this.predicting) {
	    this.candidateView.setSuggestions(null, false, false, false);
	    return;
	}

	List<Suggestion> stringList = this.suggest.getSuggestions(
	        this.inputView, this.word, false);
	boolean correctionAvailable = this.suggest.hasMinimalCorrection();
	// || mCorrectionMode == mSuggest.CORRECTION_FULL;
	CharSequence typedWord = this.word.getTypedWord();
	// If we're in basic correct
	boolean typedWordValid = this.suggest.isValidWord(typedWord);
	if (this.correctionMode == Suggest.CORRECTION_FULL) {
	    correctionAvailable |= typedWordValid;
	}

	this.candidateView.setSuggestions(stringList, false, typedWordValid,
	        correctionAvailable);
	if (stringList.size() > 0) {
	    if (correctionAvailable && !typedWordValid
		    && (stringList.size() > 1)) {
		this.bestWord = stringList.get(1).word;
	    } else {
		this.bestWord = typedWord;
	    }
	} else {
	    this.bestWord = null;
	}
	setCandidatesViewShown(isCandidateStripVisible() || this.completionOn);
    }

    private void pickDefaultSuggestion() {
	// Complete any pending candidate query first
	if (this.handler.hasMessages(MSG_UPDATE_SUGGESTIONS)) {
	    this.handler.removeMessages(MSG_UPDATE_SUGGESTIONS);
	    updateSuggestions();
	}
	if (this.bestWord != null) {
	    TextEntryState.acceptedDefault(this.word.getTypedWord(),
		    this.bestWord);
	    this.justAccepted = true;
	    pickSuggestion(this.bestWord);
	}
    }

    /**
     * Writes the selected suggestion into the text area.
     */
    public void pickSuggestionManually(int index, CharSequence suggestion) {
	if (this.completionOn && (this.completions != null) && (index >= 0)
	        && (index < this.completions.length)) {
	    CompletionInfo ci = this.completions[index];
	    InputConnection ic = getCurrentInputConnection();
	    if (ic != null) {
		ic.commitCompletion(ci);
	    }
	    this.committedLength = suggestion.length();
	    if (this.candidateView != null) {
		this.candidateView.clear();
	    }
	    updateShiftKeyState(getCurrentInputEditorInfo());
	    return;
	}
	pickSuggestion(suggestion);
	TextEntryState
	        .acceptedSuggestion(this.composing.toString(), suggestion);

	addWordToDictionary(suggestion.toString());

	// Follow it with a space
	if (this.autoSpace) {
	    sendSpace();
	}
	// Fool the state watcher so that a subsequent backspace will not do a
	// revert
	TextEntryState.typedCharacter((char) KEYCODE_SPACE, true);
    }

    /**
     * Displays the contextual menu used to delete dictionary words.
     */
    public void showCandidatesContextualMenu(final CharSequence word) {

	AlertDialog.Builder builder = new AlertDialog.Builder(this);
	builder.setCancelable(true);
	// builder.setIcon(R.drawable.ic_dialog_keyboard);
	builder.setNegativeButton(R.string.cancel, null);
	CharSequence deleteString = getString(R.string.delete) + " \"" + word
	        + "\" " + getString(R.string.from_user_dictionary);
	builder.setItems(new CharSequence[] { deleteString },
	        new DialogInterface.OnClickListener() {

		    public void onClick(DialogInterface di, int position) {
		        di.dismiss();
		        switch (position) {
		        case POS_DELETE_WORD:
			    if (SlideKeyboard.this.suggest != null) {
			        SlideKeyboard.this.suggest.deleteWord(word);
			        updateSuggestions();
			    }
			    break;
		        }
		    }
	        });
	builder.setTitle(getString(R.string.app_name));
	this.optionsDialog = builder.create();
	Window window = this.optionsDialog.getWindow();
	WindowManager.LayoutParams lp = window.getAttributes();
	lp.token = this.inputView.getWindowToken();
	lp.type = WindowManager.LayoutParams.TYPE_APPLICATION_ATTACHED_DIALOG;
	window.setAttributes(lp);
	window.addFlags(WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM);
	this.optionsDialog.show();
    }

    // public void showContactsContextualMenu() {
    //
    // AlertDialog.Builder builder = new AlertDialog.Builder(this);
    // builder.setCancelable(true);
    // // builder.setIcon(R.drawable.ic_dialog_keyboard);
    // builder.setNegativeButton(R.string.cancel, null);
    // builder.setItems(new CharSequence[] { getString(R.string.contact),
    // getString(R.string.person), getString(R.string.phone),
    // getString(R.string.phone_v2),
    // getString(R.string.postal_address),
    // getString(R.string.postal_address_v2) },
    // new DialogInterface.OnClickListener() {
    //
    // public void onClick(DialogInterface di, int position) {
    // di.dismiss();
    // switch (position) {
    // case POS_GET_CONTACT:
    // break;
    // case POS_GET_PERSON:
    // break;
    // case POS_GET_PHONE:
    // break;
    // case POS_GET_PHONE_V2:
    // break;
    // case POS_GET_POSTAL_ADDRESS:
    // break;
    // case POS_GET_POSTAL_ADDRESS_V2:
    // break;
    // }
    // }
    // });
    // builder.setTitle(getString(R.string.get_contact));
    // mOptionsDialog = builder.create();
    // Window window = mOptionsDialog.getWindow();
    // WindowManager.LayoutParams lp = window.getAttributes();
    // lp.token = mInputView.getWindowToken();
    // lp.type = WindowManager.LayoutParams.TYPE_APPLICATION_ATTACHED_DIALOG;
    // window.setAttributes(lp);
    // window.addFlags(WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM);
    // mOptionsDialog.show();
    // }

    private void pickSuggestion(CharSequence suggestion) {
	if (this.isShiftLocked) {
	    suggestion = suggestion.toString().toUpperCase();
	} else if (preferCapitalization() && this.inputView.isShifted()) {
	    suggestion = Character.toUpperCase(suggestion.charAt(0))
		    + suggestion.subSequence(1, suggestion.length()).toString();
	}
	InputConnection ic = getCurrentInputConnection();
	if (ic != null) {
	    ic.commitText(suggestion, 1);
	}
	this.predicting = false;
	this.committedLength = suggestion.length();
	if (this.candidateView != null) {
	    this.candidateView.setSuggestions(null, false, false, false);
	}
	updateShiftKeyState(getCurrentInputEditorInfo());
    }

    private boolean isCursorTouchingWord() {
	InputConnection ic = getCurrentInputConnection();
	if (ic == null) {
	    return false;
	}
	CharSequence toLeft = ic.getTextBeforeCursor(1, 0);
	CharSequence toRight = ic.getTextAfterCursor(1, 0);
	if (!TextUtils.isEmpty(toLeft) && !isWordSeparator(toLeft.charAt(0))) {
	    return true;
	}
	if (!TextUtils.isEmpty(toRight) && !isWordSeparator(toRight.charAt(0))) {
	    return true;
	}
	return false;
    }

    /**
     * Reverts the last word which has been written to what the user had typed.
     */
    public void revertLastWord(boolean deleteChar) {
	final int length = this.composing.length();
	if (!this.predicting && (length > 0)) {
	    final InputConnection ic = getCurrentInputConnection();
	    this.predicting = true;
	    ic.beginBatchEdit();
	    this.justRevertedSeparator = ic.getTextBeforeCursor(1, 0);
	    if (deleteChar) {
		ic.deleteSurroundingText(1, 0);
	    }
	    int toDelete = this.committedLength;
	    CharSequence toTheLeft = ic.getTextBeforeCursor(
		    this.committedLength, 0);
	    if ((toTheLeft != null) && (toTheLeft.length() > 0)
		    && isWordSeparator(toTheLeft.charAt(0))) {
		toDelete--;
	    }
	    ic.deleteSurroundingText(toDelete, 0);
	    ic.setComposingText(this.composing, 1);
	    TextEntryState.backspace();
	    ic.endBatchEdit();
	    postUpdateSuggestions();
	} else {
	    sendDownUpKeyEvents(KeyEvent.KEYCODE_DEL);
	    this.justRevertedSeparator = null;
	}
    }

    protected String getWordSeparators() {
	return this.wordSeparators;
    }

    /**
     * Determines if the given code corresponds to a word separator character.
     */
    public boolean isWordSeparator(int code) {
	String separators = getWordSeparators();
	return separators.contains(String.valueOf((char) code));
    }

    /**
     * Determines if the given code corresponds to a sentence separator
     * character.
     */
    public boolean isSentenceSeparator(int code) {
	return this.sentenceSeparators.contains(String.valueOf((char) code));
    }

    private void sendSpace() {
	sendKeyChar((char) KEYCODE_SPACE);
	updateShiftKeyState(getCurrentInputEditorInfo());
	// onKey(KEY_SPACE[0], KEY_SPACE);
    }

    /**
     * Indicates whether or not the user typed a capital letter as the first
     * letter in the word.
     */
    public boolean preferCapitalization() {
	return this.word.isCapitalized();
    }

    public void swipeRight() {

    }

    public void swipeLeft() {
	// handleBackspace();
    }

    public void swipeDown() {
	// handleClose();
    }

    public void swipeUp() {
	// launchSettings();
    }

    public void onPress(int primaryCode) {
	vibrate();
	playKeyClick(primaryCode);
    }

    public void onRelease(int primaryCode) {
	// vibrate();
    }

    // receive ringer mode changes to detect silent mode
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
	                                          @Override
	                                          public void onReceive(
	                                                  Context context,
	                                                  Intent intent) {
		                                      updateRingerMode();
	                                          }
	                                      };

    // update flags for silent mode
    private void updateRingerMode() {
	if (this.audioManager == null) {
	    this.audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
	}
	if (this.audioManager != null) {
	    this.silentMode = (this.audioManager.getRingerMode() != AudioManager.RINGER_MODE_NORMAL);
	}
    }

    private void playKeyClick(int primaryCode) {
	// if mAudioManager is null, we don't have the ringer state yet
	// mAudioManager will be set by updateRingerMode
	if (this.audioManager == null) {
	    if (this.inputView != null) {
		updateRingerMode();
	    }
	}
	if (this.soundOn && !this.silentMode) {
	    // FIXME: Volume and enable should come from UI settings
	    // FIXME: These should be triggered after auto-repeat logic
	    int sound = AudioManager.FX_KEYPRESS_STANDARD;
	    switch (primaryCode) {
	    case Keyboard.KEYCODE_DELETE:
		sound = AudioManager.FX_KEYPRESS_DELETE;
		break;
	    case KEYCODE_ENTER:
		sound = AudioManager.FX_KEYPRESS_RETURN;
		break;
	    case KEYCODE_SPACE:
		sound = AudioManager.FX_KEYPRESS_SPACEBAR;
		break;
	    }
	    this.audioManager.playSoundEffect(sound, this.FX_VOLUME);
	}
    }

    private void vibrate() {
	if (!this.vibrateOn) {
	    return;
	}
	if (this.vibrator == null) {
	    this.vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
	}
	this.vibrator.vibrate(this.vibrateDuration);
    }

    private void launchSettings() {
	handleClose();
	Intent intent = new Intent();
	intent.setClass(SlideKeyboard.this, LatinIMESettings.class);
	intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
	startActivity(intent);
    }

    private void loadSettings() {
	// Get the settings preferences
	SharedPreferences sp = PreferenceManager
	        .getDefaultSharedPreferences(this);
	this.gestureShortcut = sp.getBoolean(
	        LatinIMESettings.PREF_GESTURE_SHORTCUTS, true);
	this.displayToolbar = sp.getBoolean(
	        LatinIMESettings.PREF_DISPLAY_TOOLBAR, true);
	this.displayKeyPreview = sp.getBoolean(
	        LatinIMESettings.PREF_DISPLAY_KEY_PREVIEW, true);
	this.vibrateOn = sp.getBoolean(LatinIMESettings.PREF_VIBRATE_ON, false);
	this.soundOn = sp.getBoolean(LatinIMESettings.PREF_SOUND_ON, false);
	this.autoCap = sp.getBoolean(LatinIMESettings.PREF_AUTO_CAP, true);
	this.sensitivity = Integer.valueOf(sp.getString(
	        LatinIMESettings.PREF_SENSITIVITY, "3"));
	this.repeatInterval = Integer.valueOf(sp.getString(
	        LatinIMESettings.PREF_REPEAT_INTERVAL, "50"));
	this.longPressTimeOut = Integer.valueOf(sp.getString(
	        LatinIMESettings.PREF_LONGPRESS_TIMEOUT, "400"));
	// this.autoAddDictionary = sp.getBoolean(
	// LatinIMESettings.PREF_AUTO_ADD_DICTIONARY, true);
	this.keyboardType = Integer.valueOf(sp.getString(
	        LatinIMESettings.PREF_KEYBOARD_TYPE, "2"));
	this.keyboardSkinPackage = sp.getString(
	        LatinIMESettings.PREF_KEYBOARD_SKIN, STANDARD_SKIN);
	this.useTemporarySwitch = sp.getBoolean(
	        LatinIMESettings.PREF_USE_TEMPORARY_SWITCH, false);

	// If there is no auto text data, then quickfix is forced to "on", so
	// that the other options will continue to work
	// this.quickFixes = sp
	// .getBoolean(LatinIMESettings.PREF_QUICK_FIXES, true);
	// if (AutoText.getSize(slideKeyboardView) < 1) {
	// this.quickFixes = true;
	// }
	// this.showSuggestions = sp.getBoolean(
	// LatinIMESettings.PREF_SHOW_SUGGESTIONS, true) & this.quickFixes;
	// this.autoComplete =
	// sp.getBoolean(LatinIMESettings.PREF_AUTO_COMPLETE,
	// true) & this.showSuggestions;
	// this.autoCorrectOn = (this.suggest != null)
	// && (this.autoComplete || this.quickFixes);
	// this.correctionMode = this.autoComplete ? 2 : (this.quickFixes ? 1 :
	// 0);
	this.firstUsage = sp
	        .getBoolean(LatinIMESettings.PREF_FIRST_USAGE, true);
	// this.firstUsageLanguage = sp.getBoolean(
	// LatinIMESettings.PREF_FIRST_USAGE_LANGUAGE, true);
	// this.language = sp.getString(
	// LatinIMESettings.PREF_LANGUAGE_SETTINGS_KEY, Locale
	// .getDefault().getLanguage());
    }

    @Override
    public String toString() {
	String string = new String();

	// Get the settings preferences
	string += "mGestureShortcut : " + this.gestureShortcut + "\n";
	string += "mVibrateOn : " + this.vibrateOn + "\n";
	string += "mSoundOn : " + this.soundOn + "\n";
	string += "mAutoCap : " + this.autoCap + "\n";
	string += "mSensitivity : " + this.sensitivity + "\n";
	string += "mRepeatInterval : " + this.repeatInterval + "\n";
	string += "mLongPressTimeOut : " + this.longPressTimeOut + "\n";
	string += "mAutoAddDictionary : " + this.autoAddDictionary + "\n";
	string += "mKeyboardType : " + this.keyboardType + "\n";
	string += "mKeyboardLayout : " + this.keyboardSkinPackage + "\n";
	string += "mQuickFixes : " + this.quickFixes + "\n";
	string += "mShowSuggestions : " + this.showSuggestions + "\n";
	string += "mAutoComplete : " + this.autoComplete + "\n";
	string += "mAutoCorrectOn : " + this.autoCorrectOn + "\n";
	string += "mCorrectionMode : " + this.correctionMode + "\n";

	return string;
    }

    @Override
    protected void dump(FileDescriptor fd, PrintWriter fout, String[] args) {
	super.dump(fd, fout, args);

	final Printer p = new PrintWriterPrinter(fout);
	p.println("LatinIME state :");
	p.println("  Keyboard mode = "
	        + this.keyboardSwitcher.getKeyboardMode());
	p.println("  mCapsLock=" + this.isShiftLocked);
	p.println("  mComposing=" + this.composing.toString());
	p.println("  mPredictionOn=" + this.predictionOn);
	p.println("  mCorrectionMode=" + this.correctionMode);
	p.println("  mPredicting=" + this.predicting);
	p.println("  mAutoCorrectOn=" + this.autoCorrectOn);
	p.println("  mAutoSpace=" + this.autoSpace);
	p.println("  mCompletionOn=" + this.completionOn);
	p.println("  TextEntryState.state=" + TextEntryState.getState());
	p.println("  mSoundOn=" + this.soundOn);
	p.println("  mVibrateOn=" + this.vibrateOn);
    }

}
