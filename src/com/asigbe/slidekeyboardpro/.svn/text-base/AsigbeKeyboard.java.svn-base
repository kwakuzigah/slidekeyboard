package com.asigbe.slidekeyboardpro;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.content.res.XmlResourceParser;
import android.graphics.drawable.Drawable;
import android.inputmethodservice.Keyboard;
import android.text.TextUtils;
import android.util.TypedValue;
import android.util.Xml;
import android.view.Display;
import android.view.WindowManager;

/**
 * Loads an XML description of a keyboard and stores the attributes of the keys.
 * A keyboard consists of rows of keys.
 * <p>
 * The layout file for a keyboard contains XML that looks like the following
 * snippet:
 * </p>
 * 
 * <pre>
 * &lt;Keyboard
 *         android:keyWidth=&quot;%10p&quot;
 *         android:keyHeight=&quot;50px&quot;
 *         android:horizontalGap=&quot;2px&quot;
 *         android:verticalGap=&quot;2px&quot; &gt;
 *     &lt;Row android:keyWidth=&quot;32px&quot; &gt;
 *         &lt;Key android:keyLabel=&quot;A&quot; /&gt;
 *         ...
 *     &lt;/Row&gt;
 *     ...
 * &lt;/Keyboard&gt;
 * </pre>
 * 
 * @attr ref android.R.styleable#Keyboard_keyWidth
 * @attr ref android.R.styleable#Keyboard_keyHeight
 * @attr ref android.R.styleable#Keyboard_horizontalGap
 * @attr ref android.R.styleable#Keyboard_verticalGap
 */
public class AsigbeKeyboard {

    private static final int    SHIFT_OFF                 = 0;
    private static final int    SHIFT_ON                  = 1;

    private boolean             isShifted                 = false;

    static final String         TAG                       = "com.asigbe.slidekeyboardpro.SlideKeyboardView";

    // Keyboard XML Tags
    private static final String TAG_KEYBOARD              = "com.asigbe.slidekeyboardpro.SlideKeyboardView";
    private static final String TAG_ROW                   = "Row";
    private static final String TAG_KEY                   = "Key";

    public static final int     MODE_TEXT                 = 1;
    public static final int     MODE_SYMBOLS              = 2;
    public static final int     MODE_PHONE                = 3;
    public static final int     MODE_URL                  = 4;
    public static final int     MODE_EMAIL                = 5;
    public static final int     MODE_IM                   = 6;
    public static final int     MODE_NUMBERS              = 7;

    public static final int     EDGE_LEFT                 = 0x01;
    public static final int     EDGE_RIGHT                = 0x02;
    public static final int     EDGE_TOP                  = 0x04;
    public static final int     EDGE_BOTTOM               = 0x08;

    private static final int    KEY_CENTER                = 0;
    private static final int    KEY_TOP                   = 1;
    private static final int    KEY_BOTTOM                = 2;
    private static final int    KEY_LEFT                  = 3;
    private static final int    KEY_RIGHT                 = 4;

    public static final int     KEYCODE_ENTER             = 10;
    public static final int     KEYCODE_SHIFT             = -1;
    public static final int     KEYCODE_SYMBOL_CHANGE     = -2;
    public static final int     KEYCODE_CANCEL            = -3;
    public static final int     KEYCODE_DONE              = -4;
    public static final int     KEYCODE_DELETE            = -5;
    public static final int     KEYCODE_ALT               = -6;
    // public static final int KEYCODE_NUMBERS_CHANGE = -7;
    public static final int     KEYCODE_ALPHABETIC_CHANGE = -8;
    public static final int     KEYCODE_CAPS_LOCK         = -9;
    public static final int     KEYCODE_SHIFT_TOGGLE      = -10;
    public static final int     KEYCODE_KEYBOARD_SWITCH   = -11;
    public static final int     KEYCODE_KEYBOARD_REVERT   = -12;

    /** Horizontal gap default for all rows */
    private int                 mDefaultHorizontalGap;

    /** Default key width */
    private int                 mDefaultWidth;

    /** Default key height */
    private int                 mDefaultHeight;

    /** Default gap between rows */
    private int                 mDefaultVerticalGap;

    /** Key instance for the shift key, if present */
    private Key                 shiftKey;

    /** Key instance for the enter key, if present */
    private Key                 enterKey;

    /** Total height of the keyboard, including the padding and keys */
    private int                 totalHeight;

    /**
     * Total width of the keyboard, including left side gaps and keys, but not
     * any gaps on the right side.
     */
    private int                 totalWidth;

    /** List of keys in this keyboard */
    private final List<Key>     keys;

    /** List of modifier keys such as Shift & Alt, if any */
    private final List<Key>     modifierKeys;

    /** Width of the screen available to fit the keyboard */
    private final int           displayWidth;

    /** Height of the screen */
    private final int           displayHeight;

    /** Keyboard mode */
    private int                 keyboardMode;

    // Variables for pre-computing nearest keys.

    private static final int    GRID_WIDTH                = 10;
    private static final int    GRID_HEIGHT               = 5;
    private static final int    GRID_SIZE                 = GRID_WIDTH
	                                                          * GRID_HEIGHT;
    private int                 cellWidth;
    private int                 cellHeight;
    private int[][]             gridNeighbors;
    private int                 proximityThreshold;
    /**
     * Number of key widths from current touch point to search for nearest keys.
     */
    private static float        SEARCH_DISTANCE           = 1.4f;

    // private final Drawable shiftLockIcon;
    //
    // private final Drawable shiftLockPreviewIcon;

    // private final Drawable shiftIcon;

    // private final Drawable shiftPreviewIcon;

    private int                 shiftKeyPosition;

    // private Drawable enterKeyIcon;
    //
    // private Drawable enterKeyIconPreview;
    //
    private int                 enterKeyPosition;

    /** Indicates if the keyboard fills the screen width **/
    private boolean             fillScreenWidth;

    /**
     * Container for keys in the keyboard. All keys in a row are at the same
     * Y-coordinate. Some of the key size defaults can be overridden per row
     * from what the {@link Keyboard} defines.
     * 
     * @attr ref android.R.styleable#Keyboard_keyWidth
     * @attr ref android.R.styleable#Keyboard_keyHeight
     * @attr ref android.R.styleable#Keyboard_horizontalGap
     * @attr ref android.R.styleable#Keyboard_verticalGap
     * @attr ref android.R.styleable#Keyboard_Row_rowEdgeFlags
     * @attr ref android.R.styleable#Keyboard_Row_keyboardMode
     */
    public static class Row {
	/** Default width of a key in this row. */
	public int                   defaultWidth;
	/** Default height of a key in this row. */
	public int                   defaultHeight;
	/** Default horizontal gap between keys in this row. */
	public int                   defaultHorizontalGap;
	/** Vertical gap following this row. */
	public int                   verticalGap;
	/**
	 * Edge flags for this row of keys. Possible values that can be assigned
	 * are {@link Keyboard#EDGE_TOP EDGE_TOP} and
	 * {@link Keyboard#EDGE_BOTTOM EDGE_BOTTOM}
	 */
	public int                   rowEdgeFlags;

	/** The keyboard mode for this row */
	public int                   mode;

	private final AsigbeKeyboard parent;
	public int                   width;
	public ArrayList<Key>        keys = new ArrayList<Key>();

	public Row(AsigbeKeyboard parent) {
	    this.parent = parent;
	}

	public Row(Context context, AsigbeKeyboard parent,
	        XmlResourceParser parser) {
	    this.parent = parent;
	    TypedArray a = context.obtainStyledAttributes(
		    Xml.asAttributeSet(parser), R.styleable.SlideKeyboardRow);
	    this.defaultWidth = getDimensionOrFraction(a,
		    R.styleable.SlideKeyboardRow_android_keyWidth,
		    parent.displayWidth, parent.mDefaultWidth);
	    this.defaultHeight = getDimensionOrFraction(a,
		    R.styleable.SlideKeyboardRow_android_keyHeight,
		    parent.displayHeight, parent.mDefaultHeight);
	    this.defaultHorizontalGap = getDimensionOrFraction(a,
		    R.styleable.SlideKeyboardRow_android_horizontalGap,
		    parent.displayWidth, parent.mDefaultHorizontalGap);
	    this.verticalGap = getDimensionOrFraction(a,
		    R.styleable.SlideKeyboardRow_android_verticalGap,
		    parent.displayHeight, parent.mDefaultVerticalGap);
	    a.recycle();
	    a = context.obtainStyledAttributes(Xml.asAttributeSet(parser),
		    R.styleable.SlideKeyboardRow);
	    this.rowEdgeFlags = a.getInt(
		    R.styleable.SlideKeyboardRow_android_rowEdgeFlags, 0);
	    this.mode = a.getResourceId(
		    R.styleable.SlideKeyboardRow_android_keyboardMode, 0);
	    a.recycle();
	}
    }

    /**
     * Class for describing the position and characteristics of a single key in
     * the keyboard.
     * 
     * @attr ref android.R.styleable#Keyboard_keyWidth
     * @attr ref android.R.styleable#Keyboard_keyHeight
     * @attr ref android.R.styleable#Keyboard_horizontalGap
     * @attr ref android.R.styleable#Keyboard_Key_codes
     * @attr ref android.R.styleable#Keyboard_Key_keyIcon
     * @attr ref android.R.styleable#Keyboard_Key_keyLabel
     * @attr ref android.R.styleable#Keyboard_Key_iconPreview
     * @attr ref android.R.styleable#Keyboard_Key_isSticky
     * @attr ref android.R.styleable#Keyboard_Key_isRepeatable
     * @attr ref android.R.styleable#Keyboard_Key_isModifier
     * @attr ref android.R.styleable#Keyboard_Key_popupKeyboard
     * @attr ref android.R.styleable#Keyboard_Key_popupCharacters
     * @attr ref android.R.styleable#Keyboard_Key_keyOutputText
     * @attr ref android.R.styleable#Keyboard_Key_keyEdgeFlags
     */
    public static class Key {
	/**
	 * All the key codes (unicode or custom code) that this key could
	 * generate, zero'th being the most important.
	 */
	public int[]                 codes;

	/** Label to display in the center */
	public CharSequence          label;

	/** Label to display at the top */
	public CharSequence          labelTop;

	/** Label to display at the bottom */
	public CharSequence          labelBottom;

	/** Label to display at the left */
	public CharSequence          labelLeft;

	/** Label to display at the right */
	public CharSequence          labelRight;

	/**
	 * Icon to display instead of a label. Icon takes precedence over a
	 * label
	 */
	public Drawable              icon;
	public Drawable              iconTop;
	public Drawable              iconBottom;
	public Drawable              iconLeft;
	public Drawable              iconRight;
	/** Preview version of the icon, for the preview popup */
	public Drawable              iconPreview;
	public Drawable              iconPreviewTop;
	public Drawable              iconPreviewBottom;
	public Drawable              iconPreviewLeft;
	public Drawable              iconPreviewRight;
	/** Width of the key, not including the gap */
	public int                   width;
	/** Height of the key, not including the gap */
	public int                   height;
	/** The horizontal gap before this key */
	public int                   gap;
	/** Whether this key is sticky, i.e., a toggle key */
	public boolean               sticky;
	/** X coordinate of the key in the keyboard layout */
	public int                   x;
	/** Y coordinate of the key in the keyboard layout */
	public int                   y;
	/** The current pressed state of this key */
	public boolean               pressed;
	/** If this is a sticky key, is it on? */
	public boolean               on;
	/**
	 * Text to output when pressed. This can be multiple characters, like
	 * ".com"
	 */
	public CharSequence          text;
	/** Popup characters */
	public CharSequence          popupCharacters;

	/**
	 * Flags that specify the anchoring to edges of the keyboard for
	 * detecting touch events that are just out of the boundary of the key.
	 * This is a bit mask of {@link Keyboard#EDGE_LEFT},
	 * {@link Keyboard#EDGE_RIGHT}, {@link Keyboard#EDGE_TOP} and
	 * {@link Keyboard#EDGE_BOTTOM}.
	 */
	public int                   edgeFlags;
	/** Whether this is a modifier key, such as Shift or Alt */
	public boolean               modifier;
	/** The keyboard that this key belongs to */
	private final AsigbeKeyboard parentKeyboard;
	/**
	 * If this key pops up a mini keyboard, this is the resource id for the
	 * XML layout for that keyboard.
	 */
	public int                   popupResId;

	public int                   popupResIdTop;

	public int                   popupResIdBottom;

	public int                   popupResIdLeft;

	public int                   popupResIdRight;

	/** Whether this key repeats itself when held down */
	public boolean               repeatable;

	public boolean               repeatableTop;

	public boolean               repeatableBottom;

	public boolean               repeatableLeft;

	public boolean               repeatableRight;

	public int[]                 codesTop;

	public int[]                 codesBottom;

	public int[]                 codesLeft;

	public int[]                 codesRight;

	public int[]                 defaultCodes;

	public CharSequence          textTop;

	public CharSequence          textBottom;

	public CharSequence          textLeft;

	public CharSequence          textRight;

	public CharSequence          defaultText;

	private int                  defaultKey;

	public CharSequence          keyboard;

	public CharSequence          keyboardTop;

	public CharSequence          keyboardBottom;

	public CharSequence          keyboardLeft;

	public CharSequence          keyboardRight;

	private final static int[]   KEY_STATE_NORMAL_ON   = {
	                                                           android.R.attr.state_checkable,
	                                                           android.R.attr.state_checked };

	private final static int[]   KEY_STATE_PRESSED_ON  = {
	                                                           android.R.attr.state_pressed,
	                                                           android.R.attr.state_checkable,
	                                                           android.R.attr.state_checked };

	private final static int[]   KEY_STATE_NORMAL_OFF  = { android.R.attr.state_checkable };

	private final static int[]   KEY_STATE_PRESSED_OFF = {
	                                                           android.R.attr.state_pressed,
	                                                           android.R.attr.state_checkable };

	private final static int[]   KEY_STATE_NORMAL      = {};

	private final static int[]   KEY_STATE_PRESSED     = { android.R.attr.state_pressed };

	/** Create an empty key with no attributes. */
	public Key(Row parent) {
	    this.parentKeyboard = parent.parent;
	}

	/**
	 * Create a key with the given top-left coordinate and extract its
	 * attributes from the XML parser.
	 * 
	 * @param context
	 *            caller's context
	 * @param parent
	 *            the row that this key belongs to. The row must already be
	 *            attached to a {@link Keyboard}.
	 * @param x
	 *            the x coordinate of the top-left
	 * @param y
	 *            the y coordinate of the top-left
	 * @param parser
	 *            the XML parser containing the attributes for this key
	 */
	public Key(Context context, Row parent, int x, int y,
	        XmlResourceParser parser) {
	    this(parent);

	    this.x = x;
	    this.y = y;

	    TypedArray a = context.obtainStyledAttributes(
		    Xml.asAttributeSet(parser), R.styleable.SlideKeyboard);

	    this.width = getDimensionOrFraction(a,
		    R.styleable.SlideKeyboard_android_keyWidth,
		    this.parentKeyboard.displayWidth, parent.defaultWidth);
	    this.height = getDimensionOrFraction(a,
		    R.styleable.SlideKeyboard_android_keyHeight,
		    this.parentKeyboard.displayHeight, parent.defaultHeight);
	    this.gap = getDimensionOrFraction(a,
		    R.styleable.SlideKeyboard_android_horizontalGap,
		    this.parentKeyboard.displayWidth,
		    parent.defaultHorizontalGap);
	    a.recycle();

	    a = context.obtainStyledAttributes(Xml.asAttributeSet(parser),
		    R.styleable.SlideKeyboardKey);
	    this.x += this.gap;

	    this.iconPreview = a
		    .getDrawable(R.styleable.SlideKeyboardKey_android_iconPreview);
	    if (this.iconPreview != null) {
		this.iconPreview.setBounds(0, 0,
		        this.iconPreview.getIntrinsicWidth(),
		        this.iconPreview.getIntrinsicHeight());
	    }
	    this.iconPreviewTop = a
		    .getDrawable(R.styleable.SlideKeyboardKey_iconPreviewTop);
	    if (this.iconPreviewTop != null) {
		this.iconPreviewTop.setBounds(0, 0,
		        this.iconPreviewTop.getIntrinsicWidth(),
		        this.iconPreviewTop.getIntrinsicHeight());
	    }
	    this.iconPreviewBottom = a
		    .getDrawable(R.styleable.SlideKeyboardKey_iconPreviewBottom);
	    if (this.iconPreviewBottom != null) {
		this.iconPreviewBottom.setBounds(0, 0,
		        this.iconPreviewBottom.getIntrinsicWidth(),
		        this.iconPreviewBottom.getIntrinsicHeight());
	    }
	    this.iconPreviewLeft = a
		    .getDrawable(R.styleable.SlideKeyboardKey_iconPreviewLeft);
	    if (this.iconPreviewLeft != null) {
		this.iconPreviewLeft.setBounds(0, 0,
		        this.iconPreviewLeft.getIntrinsicWidth(),
		        this.iconPreviewLeft.getIntrinsicHeight());
	    }
	    this.iconPreviewRight = a
		    .getDrawable(R.styleable.SlideKeyboardKey_iconPreviewRight);
	    if (this.iconPreviewRight != null) {
		this.iconPreviewRight.setBounds(0, 0,
		        this.iconPreviewRight.getIntrinsicWidth(),
		        this.iconPreviewRight.getIntrinsicHeight());
	    }
	    this.popupCharacters = a
		    .getText(R.styleable.SlideKeyboardKey_android_popupCharacters);
	    this.popupResId = a.getResourceId(
		    R.styleable.SlideKeyboardKey_android_popupKeyboard, 0);
	    this.popupResIdTop = a.getResourceId(
		    R.styleable.SlideKeyboardKey_popupKeyboardTop, 0);
	    this.popupResIdBottom = a.getResourceId(
		    R.styleable.SlideKeyboardKey_popupKeyboardBottom, 0);
	    this.popupResIdLeft = a.getResourceId(
		    R.styleable.SlideKeyboardKey_popupKeyboardLeft, 0);
	    this.popupResIdRight = a.getResourceId(
		    R.styleable.SlideKeyboardKey_popupKeyboardRight, 0);
	    this.repeatable = a.getBoolean(
		    R.styleable.SlideKeyboardKey_android_isRepeatable, false);
	    this.repeatableTop = a.getBoolean(
		    R.styleable.SlideKeyboardKey_isRepeatableTop, false);
	    this.repeatableBottom = a.getBoolean(
		    R.styleable.SlideKeyboardKey_isRepeatableBottom, false);
	    this.repeatableLeft = a.getBoolean(
		    R.styleable.SlideKeyboardKey_isRepeatableLeft, false);
	    this.repeatableRight = a.getBoolean(
		    R.styleable.SlideKeyboardKey_isRepeatableRight, false);
	    this.modifier = a.getBoolean(
		    R.styleable.SlideKeyboardKey_android_isModifier, false);
	    this.sticky = a.getBoolean(
		    R.styleable.SlideKeyboardKey_android_isSticky, false);
	    this.edgeFlags = a.getInt(
		    R.styleable.SlideKeyboardKey_android_keyEdgeFlags, 0);
	    this.edgeFlags |= parent.rowEdgeFlags;
	    this.defaultKey = a.getInt(R.styleable.SlideKeyboardKey_defaultKey,
		    KEY_CENTER);

	    this.icon = a
		    .getDrawable(R.styleable.SlideKeyboardKey_android_keyIcon);
	    if (this.icon != null) {
		this.icon.setBounds(0, 0, this.icon.getIntrinsicWidth(),
		        this.icon.getIntrinsicHeight());
	    }
	    this.iconTop = a
		    .getDrawable(R.styleable.SlideKeyboardKey_keyIconTop);
	    if (this.iconTop != null) {
		this.iconTop.setBounds(0, 0, this.iconTop.getIntrinsicWidth(),
		        this.iconTop.getIntrinsicHeight());
	    }

	    this.iconBottom = a
		    .getDrawable(R.styleable.SlideKeyboardKey_keyIconBottom);
	    if (this.iconBottom != null) {
		this.iconBottom.setBounds(0, 0,
		        this.iconBottom.getIntrinsicWidth(),
		        this.iconBottom.getIntrinsicHeight());
	    }

	    this.iconLeft = a
		    .getDrawable(R.styleable.SlideKeyboardKey_keyIconLeft);
	    if (this.iconLeft != null) {
		this.iconLeft.setBounds(0, 0,
		        this.iconLeft.getIntrinsicWidth(),
		        this.iconLeft.getIntrinsicHeight());
	    }

	    this.iconRight = a
		    .getDrawable(R.styleable.SlideKeyboardKey_keyIconRight);
	    if (this.iconRight != null) {
		this.iconRight.setBounds(0, 0,
		        this.iconRight.getIntrinsicWidth(),
		        this.iconRight.getIntrinsicHeight());
	    }
	    this.label = a
		    .getText(R.styleable.SlideKeyboardKey_android_keyLabel);
	    this.labelTop = a.getText(R.styleable.SlideKeyboardKey_keyLabelTop);
	    this.labelBottom = a
		    .getText(R.styleable.SlideKeyboardKey_keyLabelBottom);
	    this.labelLeft = a
		    .getText(R.styleable.SlideKeyboardKey_keyLabelLeft);
	    this.labelRight = a
		    .getText(R.styleable.SlideKeyboardKey_keyLabelRight);
	    this.text = a
		    .getText(R.styleable.SlideKeyboardKey_android_keyOutputText);
	    this.textTop = a
		    .getText(R.styleable.SlideKeyboardKey_keyOutputTextTop);
	    this.textBottom = a
		    .getText(R.styleable.SlideKeyboardKey_keyOutputTextBottom);
	    this.textLeft = a
		    .getText(R.styleable.SlideKeyboardKey_keyOutputTextLeft);
	    this.textRight = a
		    .getText(R.styleable.SlideKeyboardKey_keyOutputTextRight);

	    this.codes = getCodes(a,
		    R.styleable.SlideKeyboardKey_android_codes, this.label);
	    this.codesTop = getCodes(a, R.styleable.SlideKeyboardKey_codesTop,
		    this.labelTop);
	    this.codesBottom = getCodes(a,
		    R.styleable.SlideKeyboardKey_codesBottom, this.labelBottom);
	    this.codesLeft = getCodes(a,
		    R.styleable.SlideKeyboardKey_codesLeft, this.labelLeft);
	    this.codesRight = getCodes(a,
		    R.styleable.SlideKeyboardKey_codesRight, this.labelRight);

	    this.keyboard = a.getText(R.styleable.SlideKeyboardKey_keyboard);
	    this.keyboardTop = a
		    .getText(R.styleable.SlideKeyboardKey_keyboardTop);
	    this.keyboardBottom = a
		    .getText(R.styleable.SlideKeyboardKey_keyboardBottom);
	    this.keyboardLeft = a
		    .getText(R.styleable.SlideKeyboardKey_keyboardLeft);
	    this.keyboardRight = a
		    .getText(R.styleable.SlideKeyboardKey_keyboardRight);

	    switch (this.defaultKey) {
	    case KEY_BOTTOM:
		this.defaultText = this.textBottom;
		this.defaultCodes = this.codesBottom;
		break;
	    case KEY_CENTER:
		this.defaultText = this.text;
		this.defaultCodes = this.codes;
		break;
	    case KEY_LEFT:
		this.defaultText = this.textLeft;
		this.defaultCodes = this.codesLeft;
		break;
	    case KEY_RIGHT:
		this.defaultText = this.textRight;
		this.defaultCodes = this.codesRight;
		break;
	    case KEY_TOP:
		this.defaultText = this.textTop;
		this.defaultCodes = this.codesTop;
		break;
	    }

	    a.recycle();
	}

	private int[] getCodes(TypedArray a, int attr, CharSequence label) {
	    TypedValue codesValue = new TypedValue();
	    a.getValue(attr, codesValue);
	    int codes[] = null;
	    if ((codesValue.type == TypedValue.TYPE_INT_DEC)
		    || (codesValue.type == TypedValue.TYPE_INT_HEX)) {
		codes = new int[] { codesValue.data };
	    } else if (codesValue.type == TypedValue.TYPE_STRING) {
		codes = parseCSV(codesValue.string.toString());
	    }
	    if ((codes == null) && !TextUtils.isEmpty(label)) {
		codes = new int[] { label.charAt(0) };
	    }
	    return codes;
	}

	/**
	 * Informs the key that it has been pressed, in case it needs to change
	 * its appearance or state.
	 * 
	 * @see #onReleased(boolean)
	 */
	public void onPressed() {
	    this.pressed = true;
	}

	/**
	 * Changes the pressed state of the key. If it is a sticky key, it will
	 * also change the toggled state of the key if the finger was release
	 * inside.
	 * 
	 * @param inside
	 *            whether the finger was released inside the key
	 * @see #onPressed()
	 */
	public void onReleased() {
	    this.pressed = false;
	    // if (this.sticky) {
	    // this.on = !this.on;
	    // }
	}

	int[] parseCSV(String value) {
	    int count = 0;
	    int lastIndex = 0;
	    if (value.length() > 0) {
		count++;
		while ((lastIndex = value.indexOf(",", lastIndex + 1)) > 0) {
		    count++;
		}
	    }
	    int[] values = new int[count];
	    count = 0;
	    StringTokenizer st = new StringTokenizer(value, ",");
	    while (st.hasMoreTokens()) {
		try {
		    values[count++] = Integer.parseInt(st.nextToken());
		} catch (NumberFormatException nfe) {
		    // Log.e(TAG, "Error parsing keycodes " + value);
		}
	    }
	    return values;
	}

	/**
	 * Detects if a point falls inside this key.
	 * 
	 * @param x
	 *            the x-coordinate of the point
	 * @param y
	 *            the y-coordinate of the point
	 * @return whether or not the point falls inside the key. If the key is
	 *         attached to an edge, it will assume that all points between
	 *         the key and the edge are considered to be inside the key.
	 */
	public boolean isInside(int x, int y) {
	    boolean leftEdge = (this.edgeFlags & EDGE_LEFT) > 0;
	    boolean rightEdge = (this.edgeFlags & EDGE_RIGHT) > 0;
	    boolean topEdge = (this.edgeFlags & EDGE_TOP) > 0;
	    boolean bottomEdge = (this.edgeFlags & EDGE_BOTTOM) > 0;
	    if (((x >= this.x) || (leftEdge && (x <= this.x + this.width)))
		    && ((x < this.x + this.width) || (rightEdge && (x >= this.x)))
		    && ((y >= this.y) || (topEdge && (y <= this.y + this.height)))
		    && ((y < this.y + this.height) || (bottomEdge && (y >= this.y)))) {
		return true;
	    } else {
		return false;
	    }
	}

	/**
	 * Returns the square of the distance between the center of the key and
	 * the given point.
	 * 
	 * @param x
	 *            the x-coordinate of the point
	 * @param y
	 *            the y-coordinate of the point
	 * @return the square of the distance of the point from the center of
	 *         the key
	 */
	public int squaredDistanceFrom(int x, int y) {
	    int xDist = this.x + this.width / 2 - x;
	    int yDist = this.y + this.height / 2 - y;
	    return xDist * xDist + yDist * yDist;
	}

	/**
	 * Returns the drawable state for the key, based on the current state
	 * and type of the key.
	 * 
	 * @return the drawable state of the key.
	 * @see android.graphics.drawable.StateListDrawable#setState(int[])
	 */
	public int[] getCurrentDrawableState() {
	    int[] states = KEY_STATE_NORMAL;

	    if (this.on) {
		if (this.pressed) {
		    states = KEY_STATE_PRESSED_ON;
		} else {
		    states = KEY_STATE_NORMAL_ON;
		}
	    } else {
		if (this.sticky) {
		    if (this.pressed) {
			states = KEY_STATE_PRESSED_OFF;
		    } else {
			states = KEY_STATE_NORMAL_OFF;
		    }
		} else {
		    if (this.pressed) {
			states = KEY_STATE_PRESSED;
		    }
		}
	    }
	    return states;
	}
    }

    /**
     * Creates a keyboard from the given xml key layout file.
     * 
     * @param context
     *            the application or service context
     * @param xmlLayoutResId
     *            the resource file that contains the keyboard layout and keys.
     */
    public AsigbeKeyboard(Context context, int xmlLayoutResId) {
	this(context, xmlLayoutResId, R.style.KeyboardStyle);
    }

    /**
     * Creates a keyboard from the given xml key layout file. Weeds out rows
     * that have a keyboard mode defined but don't match the specified mode.
     * 
     * @param context
     *            the application or service context
     * @param xmlLayoutResId
     *            the resource file that contains the keyboard layout and keys.
     * @param modeId
     *            keyboard mode identifier
     */
    public AsigbeKeyboard(Context context, int xmlLayoutResId, int modeId) {
	WindowManager wm = (WindowManager) context
	        .getSystemService(Context.WINDOW_SERVICE);
	final Display display = wm.getDefaultDisplay();
	this.displayWidth = display.getWidth();
	this.displayHeight = display.getHeight();
	this.mDefaultHorizontalGap = 0;
	this.mDefaultWidth = this.displayWidth / 5;
	this.mDefaultVerticalGap = 0;
	this.mDefaultHeight = this.mDefaultWidth;
	this.keys = new ArrayList<Key>();
	this.modifierKeys = new ArrayList<Key>();
	// this.shiftLockIcon = context.getResources().getDrawable(
	// R.drawable.symboleshiftlocked);
	// this.shiftLockPreviewIcon = context.getResources().getDrawable(
	// R.drawable.symboleshiftlockedfeedback);
	// this.shiftLockPreviewIcon.setBounds(0, 0,
	// this.shiftLockPreviewIcon.getIntrinsicWidth(),
	// this.shiftLockPreviewIcon.getIntrinsicHeight());
	// this.shiftIcon = context.getResources().getDrawable(
	// R.drawable.symboleshift);
	// this.shiftPreviewIcon = context.getResources().getDrawable(
	// R.drawable.symboleshiftfeedback);
	// this.shiftPreviewIcon.setBounds(0, 0,
	// this.shiftPreviewIcon.getIntrinsicWidth(),
	// this.shiftPreviewIcon.getIntrinsicHeight());

	// this.enterKeyIcon = context.getResources().getDrawable(
	// R.drawable.symbolereturn);
	// this.enterKeyIconPreview = context.getResources().getDrawable(
	// R.drawable.symbolereturnfeedback);
	// this.enterKeyIconPreview.setBounds(0, 0,
	// this.shiftPreviewIcon.getIntrinsicWidth(),
	// this.shiftPreviewIcon.getIntrinsicHeight());

	loadKeyboard(context, context.getResources().getXml(xmlLayoutResId));

	setKeyboardMode(modeId);
    }

    public int getDisplayWidth() {
	return this.displayWidth;
    }

    public void setKeyboardMode(int modeId) {
	this.keyboardMode = modeId;
    }

    /**
     * <p>
     * Creates a blank keyboard from the given resource file and populates it
     * with the specified characters in left-to-right, top-to-bottom fashion,
     * using the specified number of columns.
     * </p>
     * <p>
     * If the specified number of columns is -1, then the keyboard will fit as
     * many keys as possible in each row.
     * </p>
     * 
     * @param context
     *            the application or service context
     * @param layoutTemplateResId
     *            the layout template file, containing no keys.
     * @param characters
     *            the list of characters to display on the keyboard. One key
     *            will be created for each character.
     * @param columns
     *            the number of columns of keys to display. If this number is
     *            greater than the number of keys that can fit in a row, it will
     *            be ignored. If this number is -1, the keyboard will fit as
     *            many keys as possible in each row.
     */
    // unused
    // public AsigbeKeyboard(Context context, int layoutTemplateResId,
    // CharSequence characters, int columns, int horizontalPadding) {
    // this(context, layoutTemplateResId);
    // int x = 0;
    // int y = 0;
    // int column = 0;
    // totalWidth = 0;
    //
    // Row row = new Row(this);
    // row.defaultHeight = mDefaultHeight;
    // row.defaultWidth = mDefaultWidth;
    // row.defaultHorizontalGap = mDefaultHorizontalGap;
    // row.verticalGap = mDefaultVerticalGap;
    // row.rowEdgeFlags = EDGE_TOP | EDGE_BOTTOM;
    // final int maxColumns = columns == -1 ? Integer.MAX_VALUE : columns;
    // for (int i = 0; i < characters.length(); i++) {
    // char c = characters.charAt(i);
    // if (column >= maxColumns
    // || x + mDefaultWidth + horizontalPadding > displayWidth) {
    // x = 0;
    // y += mDefaultVerticalGap + mDefaultHeight;
    // column = 0;
    // }
    // final Key key = new Key(row);
    // key.x = x;
    // key.y = y;
    // key.width = mDefaultWidth;
    // key.height = mDefaultHeight;
    // key.gap = mDefaultHorizontalGap;
    // key.label = String.valueOf(c);
    // key.codes = new int[] { c };
    // column++;
    // x += key.width + key.gap;
    // mKeys.add(key);
    // if (x > totalWidth) {
    // totalWidth = x;
    // }
    // }
    // totalHeight = y + mDefaultHeight;
    // }
    public List<Key> getKeys() {
	return this.keys;
    }

    public List<Key> getModifierKeys() {
	return this.modifierKeys;
    }

    /**
     * Returns the total height of the keyboard
     * 
     * @return the total height of the keyboard
     */
    public int getHeight() {
	return this.totalHeight;
    }

    public int getMinWidth() {
	return this.totalWidth;
    }

    /**
     * Returns the boolean which indicates if the keyboard width must be the
     * same of the screen.
     * 
     * @return <code>true</code> if it must file the screen
     */
    public boolean getFillScreenWidth() {
	return this.fillScreenWidth;
    }

    public boolean setShifted(boolean isShifted, boolean locked) {
	boolean oldShiftState = this.isShifted;
	boolean oldOn = false;
	boolean newOldOn = true;
	this.isShifted = isShifted;
	if (this.shiftKey != null) {
	    oldOn = this.shiftKey.on;
	    this.shiftKey.sticky = isShifted;
	    this.shiftKey.on = locked;
	    // Drawable icon = null;
	    // Drawable iconPreview = null;
	    // if (isShifted) {
	    // this.shiftKey.on = locked;
	    // icon = this.shiftLockIcon;
	    // iconPreview = this.shiftLockPreviewIcon;
	    // } else {
	    // this.shiftKey.on = false;
	    // icon = this.shiftIcon;
	    // iconPreview = this.shiftPreviewIcon;
	    // }
	    newOldOn = this.shiftKey.on;
	    // setShiftKeyIcon(icon, iconPreview);
	}
	return (oldShiftState != this.isShifted || oldOn != newOldOn);
    }

    public boolean isShifted() {
	return this.isShifted;
    }

    void enableShiftLock() {
	// int index = getShiftKeyIndex();
	// if (index >= 0) {
	// mShiftKey = getKeys().get(index);
	// if (mShiftKey instanceof LatinKey) {
	// ((LatinKey)mShiftKey).enableShiftLock();
	// }
	// mOldShiftIcon = mShiftKey.icon;
	// mOldShiftPreviewIcon = mShiftKey.iconPreview;
	// }
    }

    private void setShiftKeyIcon(Drawable newIcon, Drawable newIconPreview) {
	switch (this.shiftKeyPosition) {
	case KEY_CENTER:
	    this.shiftKey.icon = newIcon;
	    this.shiftKey.iconPreview = newIconPreview;
	    break;
	case KEY_TOP:
	    this.shiftKey.iconTop = newIcon;
	    this.shiftKey.iconPreviewTop = newIconPreview;
	    break;
	case KEY_BOTTOM:
	    this.shiftKey.iconBottom = newIcon;
	    this.shiftKey.iconPreviewBottom = newIconPreview;
	    break;
	case KEY_LEFT:
	    this.shiftKey.iconLeft = newIcon;
	    this.shiftKey.iconPreviewLeft = newIconPreview;
	    break;
	case KEY_RIGHT:
	    this.shiftKey.iconRight = newIcon;
	    this.shiftKey.iconPreviewRight = newIconPreview;
	    break;
	}
    }

    private void computeNearestNeighbors() {
	// Round-up so we don't have any pixels outside the grid
	this.cellWidth = (getMinWidth() + GRID_WIDTH - 1) / GRID_WIDTH;
	this.cellHeight = (getHeight() + GRID_HEIGHT - 1) / GRID_HEIGHT;
	this.gridNeighbors = new int[GRID_SIZE][];
	int[] indices = new int[this.keys.size()];
	final int gridWidth = GRID_WIDTH * this.cellWidth;
	final int gridHeight = GRID_HEIGHT * this.cellHeight;
	for (int x = 0; x < gridWidth; x += this.cellWidth) {
	    for (int y = 0; y < gridHeight; y += this.cellHeight) {
		int count = 0;
		for (int i = 0; i < this.keys.size(); i++) {
		    final Key key = this.keys.get(i);
		    if ((key.squaredDistanceFrom(x, y) < this.proximityThreshold)
			    || (key.squaredDistanceFrom(x + this.cellWidth - 1,
			            y) < this.proximityThreshold)
			    || (key.squaredDistanceFrom(x + this.cellWidth - 1,
			            y + this.cellHeight - 1) < this.proximityThreshold)
			    || (key.squaredDistanceFrom(x, y + this.cellHeight
			            - 1) < this.proximityThreshold)) {
			indices[count++] = i;
		    }
		}
		int[] cell = new int[count];
		System.arraycopy(indices, 0, cell, 0, count);
		this.gridNeighbors[(y / this.cellHeight) * GRID_WIDTH
		        + (x / this.cellWidth)] = cell;
	    }
	}
    }

    /**
     * Returns the indices of the keys that are closest to the given point.
     * 
     * @param x
     *            the x-coordinate of the point
     * @param y
     *            the y-coordinate of the point
     * @return the array of integer indices for the nearest keys to the given
     *         point. If the given point is out of range, then an array of size
     *         zero is returned.
     */
    public int[] getNearestKeys(int x, int y) {
	if (this.gridNeighbors == null) {
	    computeNearestNeighbors();
	}
	if ((x >= 0) && (x < getMinWidth()) && (y >= 0) && (y < getHeight())) {
	    int index = (y / this.cellHeight) * GRID_WIDTH
		    + (x / this.cellWidth);
	    if (index < GRID_SIZE) {
		return this.gridNeighbors[index];
	    }
	}
	return new int[0];
    }

    protected Row createRowFromXml(Context context, XmlResourceParser parser) {
	return new Row(context, this, parser);
    }

    protected Key createKeyFromXml(Context context, Row parent, int x, int y,
	    XmlResourceParser parser) {
	return new Key(context, parent, x, y, parser);
    }

    private void loadKeyboard(Context context, XmlResourceParser parser) {
	boolean inKey = false;
	boolean inRow = false;
	int row = 0;
	int x = 0;
	int y = 0;
	Key key = null;
	Row currentRow = null;
	boolean skipRow = false;
	ArrayList<Row> rows = new ArrayList<AsigbeKeyboard.Row>();

	try {
	    int event;
	    while ((event = parser.next()) != XmlPullParser.END_DOCUMENT) {
		if (event == XmlPullParser.START_TAG) {
		    String tag = parser.getName();
		    if (TAG_ROW.equals(tag)) {
			inRow = true;
			x = 0;
			currentRow = createRowFromXml(context, parser);
			rows.add(currentRow);
			skipRow = (currentRow.mode != 0)
			        && (currentRow.mode != this.keyboardMode);
			if (skipRow) {
			    skipToEndOfRow(parser);
			    inRow = false;
			}
		    } else if (TAG_KEY.equals(tag)) {
			inKey = true;
			key = createKeyFromXml(context, currentRow, x, y,
			        parser);
			this.keys.add(key);
			currentRow.keys.add(key);
			detectSpecialKey(key, key.codes, KEY_CENTER);
			detectSpecialKey(key, key.codesBottom, KEY_BOTTOM);
			detectSpecialKey(key, key.codesTop, KEY_TOP);
			detectSpecialKey(key, key.codesLeft, KEY_LEFT);
			detectSpecialKey(key, key.codesRight, KEY_RIGHT);
		    } else if (TAG_KEYBOARD.equals(tag)) {
			parseKeyboardAttributes(context, parser);
		    }
		} else if (event == XmlPullParser.END_TAG) {
		    if (inKey) {
			inKey = false;
			x += key.gap + key.width;
			if (x > this.totalWidth) {
			    this.totalWidth = x;
			}
		    } else if (inRow) {
			inRow = false;
			y += currentRow.verticalGap;
			y += currentRow.defaultHeight;
			currentRow.width = x;
			row++;
		    } else {
			// TODO: error or extend?
		    }
		}
	    }
	} catch (Exception e) {
	    // Log.e(TAG, "Parse error:" + e);
	    // e.printStackTrace();
	}
	int countRows = rows.size();
	for (int i = 0; i < countRows; i++) {
	    currentRow = rows.get(i);
	    if (currentRow.width < this.totalWidth) {
		int shift = (this.totalWidth - currentRow.width)/2;
		ArrayList<Key> keys = currentRow.keys;
		int countKeys = keys.size();
		for (int j = 0; j < countKeys; j++) {
		    keys.get(j).x += shift;
		}
	    }
	}

	this.totalHeight = y - this.mDefaultVerticalGap;
    }

    private void detectSpecialKey(Key key, int code[], int keyPosition) {
	if ((code != null) && (code.length > 0)) {
	    if (code[0] == KEYCODE_SHIFT || code[0] == KEYCODE_SHIFT_TOGGLE) {
		this.shiftKey = key;
		this.shiftKeyPosition = keyPosition;
		this.modifierKeys.add(key);
	    } else if (code[0] == KEYCODE_ALT) {
		this.modifierKeys.add(key);
	    } else if (code[0] == KEYCODE_ENTER) {
		this.enterKey = key;
		this.enterKeyPosition = keyPosition;
	    }
	}
    }

    private void skipToEndOfRow(XmlResourceParser parser)
	    throws XmlPullParserException, IOException {
	int event;
	while ((event = parser.next()) != XmlPullParser.END_DOCUMENT) {
	    if ((event == XmlPullParser.END_TAG)
		    && parser.getName().equals(TAG_ROW)) {
		break;
	    }
	}
    }

    private void parseKeyboardAttributes(Context context,
	    XmlResourceParser parser) {
	TypedArray a = context.obtainStyledAttributes(
	        Xml.asAttributeSet(parser), R.styleable.SlideKeyboard);

	this.mDefaultWidth = getDimensionOrFraction(a,
	        R.styleable.SlideKeyboard_android_keyWidth, this.displayWidth,
	        this.displayWidth / 5);
	this.mDefaultHeight = getDimensionOrFraction(a,
	        R.styleable.SlideKeyboard_android_keyHeight,
	        this.displayHeight, 50);
	this.mDefaultHorizontalGap = getDimensionOrFraction(a,
	        R.styleable.SlideKeyboard_android_horizontalGap,
	        this.displayWidth, 0);
	this.mDefaultVerticalGap = getDimensionOrFraction(a,
	        R.styleable.SlideKeyboard_android_verticalGap,
	        this.displayHeight, 0);
	this.proximityThreshold = (int) (this.mDefaultWidth * SEARCH_DISTANCE);
	this.proximityThreshold = this.proximityThreshold
	        * this.proximityThreshold; // Square
	this.fillScreenWidth = a.getBoolean(
	        R.styleable.SlideKeyboard_fillScreenWidth, true);

	a.recycle();
    }

    static int getDimensionOrFraction(TypedArray a, int index, int base,
	    int defValue) {
	TypedValue value = a.peekValue(index);
	if (value == null) {
	    return defValue;
	}
	if (value.type == TypedValue.TYPE_DIMENSION) {
	    return a.getDimensionPixelOffset(index, defValue);
	} else if (value.type == TypedValue.TYPE_FRACTION) {
	    // Round it to avoid values like 47.9999 from getting truncated
	    return Math.round(a.getFraction(index, base, base, defValue));
	}
	return defValue;
    }

    private void setEnterKey(Key key, int keyPosition, String label,
	    String text, int code, Drawable icon, Drawable iconPreview,
	    int popupResId) {
	switch (keyPosition) {
	case KEY_CENTER:
	    this.enterKey.label = label;
	    this.enterKey.text = text;
	    this.enterKey.icon = icon;
	    this.enterKey.codes[0] = code;
	    this.enterKey.iconPreview = iconPreview;
	    this.enterKey.popupResId = popupResId;
	    break;
	case KEY_BOTTOM:
	    this.enterKey.labelBottom = label;
	    this.enterKey.textBottom = text;
	    this.enterKey.iconBottom = icon;
	    this.enterKey.codesBottom[0] = code;
	    this.enterKey.iconPreviewBottom = iconPreview;
	    this.enterKey.popupResIdBottom = popupResId;
	    break;
	case KEY_TOP:
	    this.enterKey.labelTop = label;
	    this.enterKey.textTop = text;
	    this.enterKey.iconTop = icon;
	    this.enterKey.codesTop[0] = code;
	    this.enterKey.iconPreviewTop = iconPreview;
	    this.enterKey.popupResIdTop = popupResId;
	    break;
	case KEY_LEFT:
	    this.enterKey.labelLeft = label;
	    this.enterKey.textLeft = text;
	    this.enterKey.iconLeft = icon;
	    this.enterKey.codesLeft[0] = code;
	    this.enterKey.iconPreviewLeft = iconPreview;
	    this.enterKey.popupResIdLeft = popupResId;
	    break;
	case KEY_RIGHT:
	    this.enterKey.labelRight = label;
	    this.enterKey.textRight = text;
	    this.enterKey.iconRight = icon;
	    this.enterKey.codesRight[0] = code;
	    this.enterKey.iconPreviewRight = iconPreview;
	    this.enterKey.popupResIdRight = popupResId;
	    break;
	}
    }

    void setImeOptions(Resources res, int mode, int options) {
	// if (mEnterKey != null) {
	// // Reset some of the rarely used attributes.
	// mEnterKey.popupCharacters = null;
	// mEnterKey.popupResId = 0;
	// mEnterKey.text = null;
	// switch (options&(EditorInfo.IME_MASK_ACTION|EditorInfo.
	// IME_FLAG_NO_ENTER_ACTION)) {
	// case EditorInfo.IME_ACTION_GO:
	// mEnterKey.iconPreview = null;
	// mEnterKey.icon = null;
	// mEnterKey.label = res.getText(R.string.label_go_key);
	// break;
	// case EditorInfo.IME_ACTION_NEXT:
	// mEnterKey.iconPreview = null;
	// mEnterKey.icon = null;
	// mEnterKey.label = res.getText(R.string.label_next_key);
	// break;
	// case EditorInfo.IME_ACTION_DONE:
	// mEnterKey.iconPreview = null;
	// mEnterKey.icon = null;
	// mEnterKey.label = res.getText(R.string.label_done_key);
	// break;
	// case EditorInfo.IME_ACTION_SEARCH:
	// mEnterKey.iconPreview = res.getDrawable(
	// R.drawable.sym_keyboard_feedback_search);
	// mEnterKey.icon = res.getDrawable(
	// R.drawable.sym_keyboard_search);
	// mEnterKey.label = null;
	// break;
	// case EditorInfo.IME_ACTION_SEND:
	// mEnterKey.iconPreview = null;
	// mEnterKey.icon = null;
	// mEnterKey.label = res.getText(R.string.label_send_key);
	// break;
	// default:
	// if (mode == KeyboardSwitcher.MODE_IM) {
	// mEnterKey.icon = null;
	// mEnterKey.iconPreview = null;
	// mEnterKey.label = ":-)";
	// mEnterKey.text = ":-) ";
	// mEnterKey.popupResId = R.xml.popup_smileys;
	// } else {
	// mEnterKey.iconPreview = res.getDrawable(
	// R.drawable.sym_keyboard_feedback_return);
	// mEnterKey.icon = res.getDrawable(
	// R.drawable.sym_keyboard_return);
	// mEnterKey.label = null;
	// }
	// break;
	// }
	// // Set the initial size of the preview icon
	// if (mEnterKey.iconPreview != null) {
	// mEnterKey.iconPreview.setBounds(0, 0,
	// mEnterKey.iconPreview.getIntrinsicWidth(),
	// mEnterKey.iconPreview.getIntrinsicHeight());
	// }
	// }
    }

    // /**
    // * Displays the enter key or smileys instead.
    // */
    // public void displayEnterKey(boolean displayEnterKey) {
    // if (this.enterKey != null) {
    // if (!displayEnterKey) {
    // setEnterKey(this.enterKey, this.enterKeyPosition, ":-)", ":-)",
    // KEYCODE_ENTER, null, null, R.xml.popupkeyboard_smiley);
    // } else {
    // setEnterKey(this.enterKey, this.enterKeyPosition, null, null,
    // KEYCODE_ENTER, this.enterKeyIcon,
    // this.enterKeyIconPreview, 0);
    // }
    // }
    // }

}
