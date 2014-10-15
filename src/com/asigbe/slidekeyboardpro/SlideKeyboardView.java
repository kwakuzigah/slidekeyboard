package com.asigbe.slidekeyboardpro;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.content.Context;
import android.content.res.TypedArray;
import android.content.res.XmlResourceParser;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.Region.Op;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.inputmethodservice.Keyboard;
import android.inputmethodservice.KeyboardView;
import android.os.Debug;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.view.WindowManager;
import android.widget.PopupWindow;
import android.widget.TextView;

import com.asigbe.slidekeyboardpro.AsigbeKeyboard.Key;
import com.asigbe.view.ViewTools;

/**
 * A view that renders a virtual {@link Keyboard}. It handles rendering of keys
 * and detecting key presses and touch movements.
 */
public class SlideKeyboardView extends View implements View.OnClickListener {

    /**
     * Listener for virtual keyboard events.
     */
    public interface OnKeyboardActionListener {

	/**
	 * Called when the user presses a key. This is sent before the
	 * {@link #onKey} is called. For keys that repeat, this is only called
	 * once.
	 * 
	 * @param primaryCode
	 *            the unicode of the key being pressed. If the touch is not
	 *            on a valid key, the value will be zero.
	 */
	void onPress(int primaryCode);

	/**
	 * Called when the user releases a key. This is sent after the
	 * {@link #onKey} is called. For keys that repeat, this is only called
	 * once.
	 * 
	 * @param primaryCode
	 *            the code of the key that was released
	 */
	void onRelease(int primaryCode);

	/**
	 * Send a key press to the listener.
	 * 
	 * @param primaryCode
	 *            this is the key that was pressed
	 */
	void onKeyDown(int primaryCode, int[] keyCodes, Object option);

	/**
	 * Send a long key press to the listener.
	 * 
	 * @param primaryCode
	 *            this is the key that was pressed
	 */
	void onLongKeyDown(int primaryCode, int[] keyCodes, Object option);

	/**
	 * Send a key press to the listener.
	 * 
	 * @param primaryCode
	 *            this is the key that was pressed
	 */
	void onKeyUp(int primaryCode, int[] keyCodes, Object option);

	/**
	 * Sends a sequence of characters to the listener.
	 * 
	 * @param text
	 *            the sequence of characters to be displayed.
	 */
	void onText(CharSequence text);

	/**
	 * Called when the user quickly moves the finger from right to left.
	 */
	void swipeLeft();

	/**
	 * Called when the user quickly moves the finger from left to right.
	 */
	void swipeRight();

	/**
	 * Called when the user quickly moves the finger from up to down.
	 */
	void swipeDown();

	/**
	 * Called when the user quickly moves the finger from down to up.
	 */
	void swipeUp();
    }

    private final class CacheKey {

	public final int swipingDirection;
	public final Key key;

	public CacheKey(Key key, int swipingDirection) {
	    this.key = key;
	    this.swipingDirection = swipingDirection;
	}

	@Override
	public int hashCode() {
	    final int prime = 31;
	    int result = 1;
	    result = prime * result + getOuterType().hashCode();
	    result = prime * result
		    + ((this.key == null) ? 0 : this.key.hashCode());
	    result = prime * result + this.swipingDirection;
	    return result;
	}

	@Override
	public boolean equals(Object obj) {
	    if (this == obj) {
		return true;
	    }
	    if (obj == null) {
		return false;
	    }
	    if (getClass() != obj.getClass()) {
		return false;
	    }
	    CacheKey other = (CacheKey) obj;
	    if (!getOuterType().equals(other.getOuterType())) {
		return false;
	    }
	    if (this.key == null) {
		if (other.key != null) {
		    return false;
		}
	    } else if (!this.key.equals(other.key)) {
		return false;
	    }
	    if (this.swipingDirection != other.swipingDirection) {
		return false;
	    }
	    return true;
	}

	private SlideKeyboardView getOuterType() {
	    return SlideKeyboardView.this;
	}

    }

    static final int                 KEYCODE_OPTIONS          = -100;
    static final int                 KEYCODE_SHIFT_LONGPRESS  = -101;

    private static final int         FIGURE_NORMAL            = 0;
    private static final int         FIGURE_OCTOGONAL         = 1;

    private static final int         NO_LINE                  = 0;
    private static final int         LINE_NORMAL              = 1;
    private static final int         LINE_OBLIQUE             = 2;

    private static final int         NO_SWIPING               = 0;
    private static final int         SWIPING_TOP              = 1;
    private static final int         SWIPING_BOTTOM           = 2;
    private static final int         SWIPING_LEFT             = 3;
    private static final int         SWIPING_RIGHT            = 4;

    private static final int         NOT_A_KEY                = -1;
    private static final int[]       LONG_PRESSABLE_STATE_SET = { android.R.attr.state_long_pressable };

    private AsigbeKeyboard           keyboard;
    private int                      labelTextSize;
    private int                      keyTextSize;
    private int                      keyTextColor;
    private float                    shadowRadius;
    private int                      shadowColor;
    private int                      centerTextColor;
    private float                    backgroundDimAmount;

    private TextView                 previewText;
    private PopupWindow              previewPopup;
    private int                      previewTextSizeLarge;
    private int                      previewOffset;
    private int                      previewHeight;
    private int[]                    offsetInWindow;

    private PopupWindow              popupKeyboard;
    private View                     miniKeyboardContainer;
    private SlideKeyboardView        miniKeyboard;
    private boolean                  miniKeyboardOnScreen;
    private View                     popupParent;
    private int                      mMiniKeyboardOffsetX;
    private int                      mMiniKeyboardOffsetY;
    private Map<CacheKey, View>      miniKeyboardCache;
    private int[]                    mWindowOffset;
    private Key[]                    keys;

    /** Listener for {@link OnKeyboardActionListener}. */
    private OnKeyboardActionListener keyboardActionListener;

    private int                      sensitivity              = 15;
    private static final int         MSG_SHOW_PREVIEW         = 1;
    private static final int         MSG_REMOVE_PREVIEW       = 2;
    private static final int         MSG_REPEAT               = 3;
    private static final int         MSG_LONGPRESS            = 4;

    private static final int         DELAY_BEFORE_PREVIEW     = 70;
    private static final int         DELAY_AFTER_PREVIEW      = 60;

    private int                      verticalCorrection;
    private int                      mProximityThreshold;

    private final boolean            mPreviewCentered         = false;
    private boolean                  mShowPreview             = true;
    private int                      mPopupPreviewX;
    private int                      mPopupPreviewY;

    private boolean                  mProximityCorrectOn;

    private final Paint              paint;
    private Rect                     padding;

    private final int[]              mKeyIndices              = new int[12];
    private int                      popupX;
    private int                      popupY;
    private int                      popupLayout;
    private List<Key>                invalidatedKeys;
    private final Rect               clipRegion               = new Rect(0, 0,
	                                                              0, 0);

    private Drawable                 keyBackground;
    private int                      repeatInterval;
    private static final int         REPEAT_START_DELAY       = 400;
    private static final Rect        INTERNAL_PADDING         = new Rect(5, 5,
	                                                              5, 5);
    // Deemed to be too short : ViewConfiguration.getLongPressTimeout();

    private static int               MAX_NEARBY_KEYS          = 12;
    private final int[]              mDistances               = new int[MAX_NEARBY_KEYS];

    /** Whether the keyboard bitmap needs to be redrawn before it's blitted. **/
    private boolean                  mDrawPending;
    /** The dirty region in the keyboard bitmap */
    private final Rect               dirtyRect                = new Rect();
    /** The keyboard bitmap for faster updates */
    private Bitmap                   buffer;
    /** The canvas for the above mutable keyboard bitmap */
    private Canvas                   canvas;

    Handler                          handler                  = new Handler() {
	                                                          @Override
	                                                          public void handleMessage(
	                                                                  Message msg) {
		                                                      switch (msg.what) {
		                                                      case MSG_SHOW_PREVIEW:
		                                                          showKey((TouchInfo) msg.obj);
		                                                          break;
		                                                      case MSG_REMOVE_PREVIEW:
		                                                          SlideKeyboardView.this.previewText
		                                                                  .setVisibility(INVISIBLE);
		                                                          break;
		                                                      case MSG_REPEAT:
		                                                          if (repeatKey((TouchInfo) msg.obj)) {
			                                                      Message repeat = Message
			                                                              .obtain(this,
			                                                                      MSG_REPEAT,
			                                                                      msg.obj);
			                                                      sendMessageDelayed(
			                                                              repeat,
			                                                              SlideKeyboardView.this.repeatInterval);
		                                                          }
		                                                          break;
		                                                      case MSG_LONGPRESS:
		                                                          openPopupIfRequired((TouchInfo) msg.obj);
		                                                          break;
		                                                      }
	                                                          }
	                                                      };

    private final int                displayWidth;
    private int                      textStyle;
    private int                      temporaryBackgroundColor;
    private int                      permanentBackgroundColor;
    private int                      figureStyle;
    private int                      lineStyle;
    private boolean                  displayKeyPreview;
    private final AttributeSet       attrs;
    private boolean                  mustLeaveSpaceWhenNoLetter;

    public SlideKeyboardView(Context context, AttributeSet attrs) {
	this(context, attrs, R.style.SlideKeyboardStyle);

    }

    public SlideKeyboardView(Context context, AttributeSet attrs, int defStyle) {
	super(context, attrs, defStyle);
	this.attrs = attrs;
	this.invalidatedKeys = new ArrayList<AsigbeKeyboard.Key>();

	WindowManager wm = (WindowManager) context
	        .getSystemService(Context.WINDOW_SERVICE);
	final Display display = wm.getDefaultDisplay();
	this.displayWidth = display.getWidth();

	this.paint = new Paint();
	this.paint.setAntiAlias(true);
	this.paint.setTextSize(keyTextSize);
	this.paint.setTextAlign(Align.CENTER);

	this.padding = new Rect(0, 0, 0, 0);
	this.popupParent = this;
	this.touchInfos[0] = new TouchInfo();
	this.touchInfos[1] = new TouchInfo();
	applySkin(context, defStyle);
    }

    public void applySkin(Context context, int defStyle) {

	LayoutInflater inflate = (LayoutInflater) context
	        .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	TypedArray a = context.obtainStyledAttributes(this.attrs,
	        R.styleable.SlideKeyboardView, 0, defStyle);

	int previewLayout = 0;
	int n = a.getIndexCount();
	for (int i = 0; i < n; i++) {
	    int attr = a.getIndex(i);

	    switch (attr) {
	    case R.styleable.SlideKeyboardView_android_keyBackground:
		this.keyBackground = a.getDrawable(attr);
		break;
	    case R.styleable.SlideKeyboardView_android_verticalCorrection:
		this.verticalCorrection = a.getDimensionPixelOffset(attr, 0);
		break;
	    case R.styleable.SlideKeyboardView_android_keyPreviewLayout:
		previewLayout = a.getResourceId(attr, 0);
		break;
	    case R.styleable.SlideKeyboardView_android_keyPreviewOffset:
		this.previewOffset = a.getDimensionPixelOffset(attr, 0);
		break;
	    case R.styleable.SlideKeyboardView_android_keyPreviewHeight:
		this.previewHeight = a.getDimensionPixelSize(attr, 80);
		break;
	    case R.styleable.SlideKeyboardView_android_keyTextSize:
		this.keyTextSize = a.getDimensionPixelSize(attr, 18);
		break;
	    case R.styleable.SlideKeyboardView_android_keyTextColor:
		this.keyTextColor = a.getColor(attr, Color.WHITE);
		break;
	    case R.styleable.SlideKeyboardView_android_labelTextSize:
		this.labelTextSize = a.getDimensionPixelSize(attr, 14);
		break;
	    case R.styleable.SlideKeyboardView_android_popupLayout:
		this.popupLayout = a.getResourceId(attr, 0);
		break;
	    case R.styleable.SlideKeyboardView_android_shadowColor:
		this.shadowColor = a.getColor(attr, 0);
		break;
	    case R.styleable.SlideKeyboardView_android_shadowRadius:
		this.shadowRadius = a.getFloat(attr, 0f);
		break;
	    case R.styleable.SlideKeyboardView_android_textStyle:
		this.textStyle = a.getInt(attr, Typeface.NORMAL);
		break;
	    case R.styleable.SlideKeyboardView_centerTextColor:
		this.centerTextColor = a.getColor(attr, Color.WHITE);
		break;
	    case R.styleable.SlideKeyboardView_temporaryBackgroundColor:
		this.temporaryBackgroundColor = a.getColor(attr, Color.BLUE);
		break;
	    case R.styleable.SlideKeyboardView_permanentBackgroundColor:
		this.permanentBackgroundColor = a.getColor(attr, Color.BLACK);
		break;
	    case R.styleable.SlideKeyboardView_figureStyle:
		this.figureStyle = a.getInt(attr, FIGURE_NORMAL);
		break;
	    case R.styleable.SlideKeyboardView_lineStyle:
		this.lineStyle = a.getInt(attr, LINE_NORMAL);
	    case R.styleable.SlideKeyboardView_leaveSpaceWhenNoLetter:
		this.mustLeaveSpaceWhenNoLetter = a.getBoolean(attr, true);
		break;
	    }

	    switch (this.textStyle) {
	    case Typeface.NORMAL:
		this.textTypeFace = SlideKeyboard.textTypeFace;
		break;
	    case Typeface.BOLD:
		this.textTypeFace = SlideKeyboard.boldTextTypeFace;
		break;
	    case Typeface.ITALIC:
		this.textTypeFace = SlideKeyboard.italicTextTypeFace;
		break;
	    default:
		this.textTypeFace = SlideKeyboard.textTypeFace;
		break;
	    }
	}

	// No theme configured
	// a = getContext().obtainStyledAttributes(android.R.attr.theme);
	// mBackgroundDimAmount = a.getFloat(R.attr.Theme_backgroundDimAmount,
	// 0.5f);

	this.backgroundDimAmount = 0.5f;
	this.previewPopup = new PopupWindow(context);
	if (previewLayout != 0) {
	    this.previewText = (TextView) inflate.inflate(previewLayout, null);
	    this.previewText.setTypeface(this.textTypeFace);
	    this.previewTextSizeLarge = (int) this.previewText.getTextSize();
	    this.previewPopup.setContentView(this.previewText);
	    this.previewPopup.setBackgroundDrawable(null);
	} else {
	    this.mShowPreview = false;
	}

	this.previewPopup.setTouchable(false);

	this.popupKeyboard = new PopupWindow(context);
	this.popupKeyboard.setBackgroundDrawable(null);
	// mPopupKeyboard.setClippingEnabled(false);

	// mPredicting = true;
	this.miniKeyboardCache = new HashMap<CacheKey, View>();
	if (!this.keyBackground.getPadding(this.padding)) {
	    this.padding = INTERNAL_PADDING;
	}
	resetSwiping();
    }

    public void setOnKeyboardActionListener(OnKeyboardActionListener listener) {
	this.keyboardActionListener = listener;
    }

    /**
     * Returns the {@link OnKeyboardActionListener} object.
     * 
     * @return the listener attached to this keyboard
     */
    protected OnKeyboardActionListener getOnKeyboardActionListener() {
	return this.keyboardActionListener;
    }

    /**
     * Attaches a keyboard to this view. The keyboard can be switched at any
     * time and the view will re-layout itself to accommodate the keyboard.
     * 
     * @see Keyboard
     * @see #getKeyboard()
     * @param keyboard
     *            the keyboard to display in this view
     */
    public void setKeyboard(AsigbeKeyboard keyboard) {
	if (this.keyboard != null) {
	    cancelPreview();
	}
	this.keyboard = keyboard;
	List<Key> keys = this.keyboard.getKeys();
	this.keys = keys.toArray(new Key[keys.size()]);
	releaseAllKeys();
	requestLayout();
	// Release buffer, just in case the new keyboard has a different size.
	// It will be reallocated on the next draw.
	this.buffer = null;
	invalidateAll();
	computeProximityThreshold(keyboard);
	this.miniKeyboardCache.clear(); // Not really necessary to do every
	// time,
	// but will free up views
    }

    /**
     * Returns the current keyboard being displayed by this view.
     * 
     * @return the currently attached keyboard
     * @see #setKeyboard(Keyboard)
     */
    public AsigbeKeyboard getKeyboard() {
	return this.keyboard;
    }

    /**
     * Sets the state of the shift key of the keyboard, if any.
     * 
     * @param shifted
     *            whether or not to enable the state of the shift key
     * @return true if the shift key state changed, false if there was no change
     * @see KeyboardView#isShifted()
     */
    public boolean setShifted(boolean shifted, boolean locked) {
	if (this.keyboard != null) {
	    if (this.keyboard.setShifted(shifted, locked)) {
		// The whole keyboard probably needs to be redrawn
		invalidateAll();
		return true;
	    }
	}
	return false;
    }

    /**
     * Returns the state of the shift key of the keyboard, if any.
     * 
     * @return true if the shift is in a pressed state, false otherwise. If
     *         there is no shift key on the keyboard or there is no keyboard
     *         attached, it returns false.
     * @see KeyboardView#setShifted(boolean)
     */
    public boolean isShifted() {
	if (this.keyboard != null) {
	    return this.keyboard.isShifted();
	}
	return false;
    }

    /**
     * Enables or disables the key feedback popup. This is a popup that shows a
     * magnified version of the depressed key. By default the preview is
     * enabled.
     * 
     * @param previewEnabled
     *            whether or not to enable the key feedback popup
     * @see #isPreviewEnabled()
     */
    public void setPreviewEnabled(boolean previewEnabled) {
	this.mShowPreview = previewEnabled;
    }

    /**
     * Returns the enabled state of the key feedback popup.
     * 
     * @return whether or not the key feedback popup is enabled
     * @see #setPreviewEnabled(boolean)
     */
    public boolean isPreviewEnabled() {
	return this.mShowPreview;
    }

    public void setPopupParent(View v) {
	this.popupParent = v;
    }

    public void setPopupOffset(int x, int y) {
	this.mMiniKeyboardOffsetX = x;
	this.mMiniKeyboardOffsetY = y;
	if (this.previewPopup.isShowing()) {
	    this.previewPopup.dismiss();
	}
    }

    /**
     * When enabled, calls to {@link OnKeyboardActionListener#onKey} will
     * include key codes for adjacent keys. When disabled, only the primary key
     * code will be reported.
     * 
     * @param enabled
     *            whether or not the proximity correction is enabled
     */
    public void setProximityCorrectionEnabled(boolean enabled) {
	this.mProximityCorrectOn = enabled;
    }

    /**
     * Returns true if proximity correction is enabled.
     */
    public boolean isProximityCorrectionEnabled() {
	return this.mProximityCorrectOn;
    }

    /**
     * Popup keyboard close button clicked.
     * 
     * @hide
     */
    public void onClick(View v) {
	dismissPopupKeyboard();
    }

    private CharSequence adjustCase(CharSequence label) {
	if (this.keyboard.isShifted() && label != null && label.length() < 3
	        && Character.isLowerCase(label.charAt(0))) {
	    label = label.toString().toUpperCase();
	}
	return label;
    }

    @Override
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
	// Round up a little
	if (this.keyboard == null) {
	    setMeasuredDimension(getPaddingLeft() + getPaddingRight(),
		    getPaddingTop() + getPaddingBottom());
	} else {
	    int width = this.keyboard.getFillScreenWidth() ? this.displayWidth
		    : this.keyboard.getMinWidth() + getPaddingLeft()
		            + getPaddingRight();
	    if (MeasureSpec.getSize(widthMeasureSpec) < width + 10) {
		width = MeasureSpec.getSize(widthMeasureSpec);
	    }
	    setMeasuredDimension(width, this.keyboard.getHeight()
		    + getPaddingTop() + getPaddingBottom());
	}
    }

    /**
     * Compute the average distance between adjacent keys (horizontally and
     * vertically) and square it to get the proximity threshold. We use a square
     * here and in computing the touch distance from a key's center to avoid
     * taking a square root.
     * 
     * @param keyboard
     */
    private void computeProximityThreshold(AsigbeKeyboard keyboard) {
	if (keyboard == null) {
	    return;
	}
	final Key[] keys = this.keys;
	if (keys == null) {
	    return;
	}
	int length = keys.length;
	int dimensionSum = 0;
	for (int i = 0; i < length; i++) {
	    Key key = keys[i];
	    dimensionSum += Math.min(key.width, key.height) + key.gap;
	}
	if ((dimensionSum < 0) || (length == 0)) {
	    return;
	}
	this.mProximityThreshold = (int) (dimensionSum * 1.4f / length);
	this.mProximityThreshold *= this.mProximityThreshold; // Square it
    }

    @Override
    public void onSizeChanged(int w, int h, int oldw, int oldh) {
	super.onSizeChanged(w, h, oldw, oldh);
	// Release the buffer, if any and it will be reallocated on the next
	// draw
	this.buffer = null;
    }

    @Override
    public void onDraw(Canvas canvas) {
	super.onDraw(canvas);
	if (this.mDrawPending || (this.buffer == null)) {
	    onBufferDraw();
	}
	canvas.drawBitmap(this.buffer, 0, 0, null);
    }

    private void onBufferDraw() {
	final Key[] keys = this.keys;
	if (this.buffer == null) {
	    this.buffer = Bitmap.createBitmap(getWidth(), getHeight(),
		    Bitmap.Config.ARGB_8888);
	    this.canvas = new Canvas(this.buffer);
	    this.octagonalPath = new Path();
	    this.octagonalPath.moveTo(keys[0].width / 2, 0);
	    this.octagonalPath.lineTo(getWidth() - keys[0].width / 2, 0);
	    this.octagonalPath.lineTo(getWidth(), keys[0].height / 2);
	    this.octagonalPath.lineTo(getWidth(), getHeight() - keys[0].height
		    / 2);
	    this.octagonalPath.lineTo(getWidth() - keys[0].width / 2,
		    getHeight());
	    this.octagonalPath.lineTo(keys[0].width / 2, getHeight());
	    this.octagonalPath.lineTo(0, getHeight() - keys[0].height / 2);
	    this.octagonalPath.lineTo(0, keys[0].height / 2);

	    this.borderPath = new Path();
	    this.borderPath.moveTo(keys[0].width / 2 + 1, 0);
	    this.borderPath.lineTo(getWidth() - keys[0].width / 2 - 1, 0);
	    this.borderPath.lineTo(getWidth(), keys[0].height / 2 + 1);
	    this.borderPath.lineTo(getWidth(), getHeight() - keys[0].height / 2
		    - 1);
	    this.borderPath.lineTo(getWidth() - keys[0].width / 2 - 1,
		    getHeight());
	    this.borderPath.lineTo(keys[0].width / 2 + 1, getHeight());
	    this.borderPath.lineTo(0, getHeight() - keys[0].height / 2 - 1);
	    this.borderPath.lineTo(0, keys[0].height / 2 + 1);

	    invalidateAll();
	}

	final Canvas canvas = this.canvas;
	canvas.clipRect(this.dirtyRect, Op.REPLACE);

	if (this.keyboard == null) {
	    return;
	}

	int centerOffset = getWidth() - this.keyboard.getMinWidth();
	canvas.translate(centerOffset / 2, 0);
	if (this.figureStyle == FIGURE_OCTOGONAL) {
	    canvas.clipPath(this.octagonalPath);
	    canvas.drawPath(this.borderPath, paint);
	}

	final Paint paint = this.paint;
	final Rect clipRegion = this.clipRegion;
	final Rect padding = this.padding;
	final int kbdPaddingLeft = getPaddingLeft();
	final int kbdPaddingTop = getPaddingTop();

	paint.setAlpha(255);
	paint.setColor(this.keyTextColor);
	boolean drawSingleKey = false;
	for (Key invalidKey : this.invalidatedKeys) {
	    if (canvas.getClipBounds(clipRegion)) {
		// Is clipRegion completely contained within the invalidated
		// key?
		if ((invalidKey.x + kbdPaddingLeft + centerOffset - 1 <= clipRegion.left)
		        && (invalidKey.y + kbdPaddingTop - 1 <= clipRegion.top)
		        && (invalidKey.x + invalidKey.width + kbdPaddingLeft
		                + centerOffset + 1 >= clipRegion.right)
		        && (invalidKey.y + invalidKey.height + kbdPaddingTop
		                + 1 >= clipRegion.bottom)) {
		    drawSingleKey = true;
		}
	    }
	}
	final Drawable keyBackground = this.keyBackground;
	int keyTextSize = this.keyTextSize;
	int centerTextColor = this.centerTextColor;
	int keyTextColor = this.keyTextColor;

	canvas.drawColor(0x00000000, PorterDuff.Mode.CLEAR);
	final int keyCount = keys.length;
	for (int i = 0; i < keyCount; i++) {
	    final Key key = keys[i];
	    if (drawSingleKey) {
		boolean found = false;
		for (int j = 0; j < this.invalidatedKeys.size() && !found; j++) {
		    found = (this.invalidatedKeys.get(j) == key);
		}
		if (!found) {
		    continue;
		}
	    }
	    int[] drawableState = key.getCurrentDrawableState();
	    keyBackground.setState(drawableState);

	    final Rect bounds = keyBackground.getBounds();
	    if ((key.width != bounds.right) || (key.height != bounds.bottom)) {
		keyBackground.setBounds(0, 0, key.width, key.height);
	    }
	    canvas.translate(key.x + kbdPaddingLeft, key.y + kbdPaddingTop);
	    keyBackground.draw(canvas);

	    // initializes the typeface
	    paint.setTextSize(keyTextSize);

	    int keyHeightWithoutPadding = key.height - padding.top
		    - padding.bottom;
	    if (!this.mustLeaveSpaceWhenNoLetter) {
		// we translate if we must not leave space for empty letters

		// right letter is missing
		if ((key.labelLeft != null || key.iconLeft != null)
		        && (key.labelRight == null && key.iconRight == null)) {
		    canvas.translate(
			    (key.width - padding.left - padding.right) / 6, 0);
		}

		// left letter is missing
		if ((key.labelRight != null || key.iconRight != null)
		        && (key.labelLeft == null && key.iconLeft == null)) {
		    canvas.translate(
			    -((key.width - padding.left - padding.right) / 6),
			    0);
		}

		// bottom letter is missing
		if ((key.labelTop != null || key.iconTop != null)
		        && (key.labelBottom == null && key.iconBottom == null)) {
		    canvas.translate(
			    0,
			    keyHeightWithoutPadding / 6 + padding.top
			            + (paint.getTextSize() - paint.descent())
			            / 2);
		}

		// top letter is missing
		if ((key.labelBottom != null || key.iconBottom != null)
		        && (key.labelTop == null && key.iconTop == null)) {
		    canvas.translate(0, -(keyHeightWithoutPadding / 6
			    + padding.top + (paint.getTextSize() - paint
			    .descent()) / 2));
		}
	    }

	    float centerx = (key.width - padding.left - padding.right) / 2
		    + padding.left;
	    float centery = keyHeightWithoutPadding / 2
		    + (paint.getTextSize() - paint.descent()) / 2 + padding.top
		    - 1;

	    // paint on center
	    paint.setColor(centerTextColor);
	    if (key.icon != null) {
		key.icon.setColorFilter(centerTextColor,
		        PorterDuff.Mode.SRC_ATOP);
	    }
	    int iconWidth = key.icon != null ? key.icon.getIntrinsicWidth() : 0;
	    int centerDrawableX = computeCenterDrawableX(padding, key, key.icon);
	    int centerDrawableY = computeCenterDrawableY(padding, key, key.icon);
	    paintLabel(key.label, key.icon, centerx, centery, centerDrawableX,
		    centerDrawableY, key, canvas, paint);

	    // paint on left
	    paint.setColor(keyTextColor);
	    iconWidth = key.iconLeft != null ? key.iconLeft.getIntrinsicWidth()
		    : 0;
	    centerDrawableX = computeCenterDrawableX(padding, key, key.iconLeft);
	    centerDrawableY = computeCenterDrawableY(padding, key, key.iconLeft);
	    paintLabel(key.labelLeft, key.iconLeft,
		    (key.width - padding.left - padding.right) / 6
		            + padding.left, centery,
		    (key.width - padding.left - padding.right) / 6 - iconWidth
		            / 2, centerDrawableY, key, canvas, paint);

	    // paint on right
	    iconWidth = key.iconRight != null ? key.iconRight
		    .getIntrinsicWidth() : 0;
	    centerDrawableX = computeCenterDrawableX(padding, key,
		    key.iconRight);
	    centerDrawableY = computeCenterDrawableY(padding, key,
		    key.iconRight);
	    paintLabel(key.labelRight, key.iconRight, 5
		    * (key.width - padding.left - padding.right) / 6
		    + padding.left, centery, 5
		    * (key.width - padding.left - padding.right) / 6
		    + padding.left - iconWidth / 2, centerDrawableY, key,
		    canvas, paint);

	    // paint on top
	    int iconHeight = key.iconTop != null ? key.iconTop
		    .getIntrinsicHeight() : 0;
	    centerDrawableX = computeCenterDrawableX(padding, key, key.iconTop);
	    centerDrawableY = computeCenterDrawableY(padding, key, key.iconTop);
	    paintLabel(
		    key.labelTop,
		    key.iconTop,
		    centerx,
		    keyHeightWithoutPadding / 6 + padding.top
		            + (paint.getTextSize() - paint.descent()) / 2 - 2,
		    centerDrawableX, keyHeightWithoutPadding / 6 + padding.top
		            - iconHeight / 2, key, canvas, paint);

	    // paint on bottom
	    iconHeight = key.iconBottom != null ? key.iconBottom
		    .getIntrinsicHeight() : 0;
	    centerDrawableX = computeCenterDrawableX(padding, key,
		    key.iconBottom);
	    centerDrawableY = computeCenterDrawableY(padding, key,
		    key.iconBottom);
	    paintLabel(
		    key.labelBottom,
		    key.iconBottom,
		    centerx,
		    5 * keyHeightWithoutPadding / 6 + padding.top
		            + (paint.getTextSize() - paint.descent()) / 2,
		    centerDrawableX, 5 * keyHeightWithoutPadding / 6
		            + padding.top - iconHeight / 2 + 2, key, canvas,
		    paint);

	    if (!this.mustLeaveSpaceWhenNoLetter) {
		// we cancel the translatation if we must not leave space for
		// empty letters
		if ((key.labelLeft != null || key.iconLeft != null)
		        && (key.labelRight == null && key.iconRight == null)) {
		    canvas.translate(
			    -((key.width - padding.left - padding.right) / 6),
			    0);
		}

		if ((key.labelRight != null || key.iconRight != null)
		        && (key.labelLeft == null && key.iconLeft == null)) {
		    canvas.translate(
			    (key.width - padding.left - padding.right) / 6, 0);
		}

		if ((key.labelTop != null || key.iconTop != null)
		        && (key.labelBottom == null && key.iconBottom == null)) {
		    canvas.translate(0, -(keyHeightWithoutPadding / 6
			    + padding.top + (paint.getTextSize() - paint
			    .descent()) / 2));
		}

		if ((key.labelBottom != null || key.iconBottom != null)
		        && (key.labelTop == null && key.iconTop == null)) {
		    canvas.translate(
			    0,
			    keyHeightWithoutPadding / 6 + padding.top
			            + (paint.getTextSize() - paint.descent())
			            / 2);
		}
	    }

	    if (this.lineStyle == LINE_NORMAL) {
		// draw vertical lines
		float offset = padding.left
		        + (key.width - padding.left - padding.right) / 3;
		canvas.drawLine(offset, padding.top, offset, key.height
		        - padding.bottom, paint);
		offset += (key.width - padding.left - padding.right) / 3;
		canvas.drawLine(offset, padding.top, offset, key.height
		        - padding.bottom, paint);

		// draw horizontal lines
		offset = padding.top + keyHeightWithoutPadding / 3;
		canvas.drawLine(padding.left, offset,
		        key.width - padding.right, offset, paint);
		offset += keyHeightWithoutPadding / 3;
		canvas.drawLine(padding.left, offset,
		        key.width - padding.right, offset, paint);
	    } else if (this.lineStyle == LINE_OBLIQUE) {
		paint.setAlpha(123);
		int leftOffset = (key.width - padding.left - padding.right - this.keyTextSize) / 2;
		int rightOffset = key.width - leftOffset;
		int topOffset = (key.height - padding.top - padding.bottom - this.keyTextSize) / 2;
		int bottomOffset = key.height - topOffset;
		if ((key.edgeFlags & Keyboard.EDGE_LEFT) != Keyboard.EDGE_LEFT) {
		    if ((key.edgeFlags & Keyboard.EDGE_TOP) != Keyboard.EDGE_TOP) {
			canvas.drawLine(0, 0, leftOffset, topOffset, paint);
		    }
		    if ((key.edgeFlags & Keyboard.EDGE_BOTTOM) != Keyboard.EDGE_BOTTOM) {
			canvas.drawLine(0, key.height, leftOffset,
			        bottomOffset, paint);
		    }
		    // if ((key.edgeFlags & Keyboard.EDGE_TOP) !=
		    // Keyboard.EDGE_TOP)
		    // {
		    // canvas.drawLine(key.width, 0, rightOffset,
		    // bottomOffset,
		    // paint);
		    // }
		}

		if ((key.edgeFlags & Keyboard.EDGE_RIGHT) != Keyboard.EDGE_RIGHT) {
		    if ((key.edgeFlags & Keyboard.EDGE_TOP) != Keyboard.EDGE_TOP) {
			canvas.drawLine(key.width, 0, rightOffset, topOffset,
			        paint);
		    }
		    if ((key.edgeFlags & Keyboard.EDGE_BOTTOM) != Keyboard.EDGE_BOTTOM) {
			canvas.drawLine(key.width, key.height, rightOffset,
			        bottomOffset, paint);
		    }

		    // if ((key.edgeFlags & Keyboard.EDGE_TOP) !=
		    // Keyboard.EDGE_TOP)
		    // {
		    // canvas.drawLine(key.width, 0, rightOffset,
		    // bottomOffset,
		    // paint);
		    // }
		}

		if (((key.edgeFlags & Keyboard.EDGE_RIGHT) != Keyboard.EDGE_RIGHT)
		        && ((key.edgeFlags & Keyboard.EDGE_LEFT) != Keyboard.EDGE_LEFT)) {
		    if ((key.edgeFlags & Keyboard.EDGE_TOP) == Keyboard.EDGE_TOP) {
			canvas.drawLine(key.width / 2, 0, key.width / 2,
			        topOffset, paint);
		    } else if ((key.edgeFlags & Keyboard.EDGE_BOTTOM) == Keyboard.EDGE_BOTTOM) {
			canvas.drawLine(key.width / 2, key.height,
			        key.width / 2, bottomOffset, paint);
		    }
		} else if ((key.edgeFlags & Keyboard.EDGE_LEFT) == Keyboard.EDGE_LEFT) {
		    if (((key.edgeFlags & Keyboard.EDGE_TOP) != Keyboard.EDGE_TOP)
			    && ((key.edgeFlags & Keyboard.EDGE_BOTTOM) != Keyboard.EDGE_BOTTOM)) {
			canvas.drawLine(0, key.height / 2, leftOffset,
			        key.height / 2, paint);
		    } else if ((key.edgeFlags & Keyboard.EDGE_TOP) == Keyboard.EDGE_TOP) {
			canvas.drawLine(0, 0, leftOffset, topOffset, paint);
		    } else if ((key.edgeFlags & Keyboard.EDGE_BOTTOM) == Keyboard.EDGE_BOTTOM) {
			canvas.drawLine(0, key.height, leftOffset,
			        bottomOffset, paint);
		    }
		} else if ((key.edgeFlags & Keyboard.EDGE_RIGHT) == Keyboard.EDGE_RIGHT) {
		    if (((key.edgeFlags & Keyboard.EDGE_TOP) != Keyboard.EDGE_TOP)
			    && ((key.edgeFlags & Keyboard.EDGE_BOTTOM) != Keyboard.EDGE_BOTTOM)) {
			canvas.drawLine(key.width, key.height / 2, rightOffset,
			        key.height / 2, paint);
		    } else if ((key.edgeFlags & Keyboard.EDGE_TOP) == Keyboard.EDGE_TOP) {
			canvas.drawLine(key.width, 0, rightOffset, topOffset,
			        paint);
		    } else if ((key.edgeFlags & Keyboard.EDGE_BOTTOM) == Keyboard.EDGE_BOTTOM) {
			canvas.drawLine(key.width, key.height, rightOffset,
			        bottomOffset, paint);
		    }
		}

		paint.setAlpha(255);
	    }

	    canvas.translate(-key.x - kbdPaddingLeft, -key.y - kbdPaddingTop);

	}
	this.invalidatedKeys.clear();
	// Overlay a dark rectangle to dim the keyboard
	if (this.miniKeyboardOnScreen) {
	    paint.setColor((int) (this.backgroundDimAmount * 0xFF) << 24);
	    canvas.drawRect(0, 0, getWidth(), getHeight(), paint);
	}

	this.mDrawPending = false;
	this.dirtyRect.setEmpty();
	canvas.translate(-centerOffset / 2, 0);
    }

    private int computeCenterDrawableY(final Rect padding, final Key key,
	    Drawable icon) {
	if (icon == null) {
	    return 0;
	}
	return (key.height - padding.top - padding.bottom - icon
	        .getIntrinsicHeight()) / 2 + padding.top;
    }

    private int computeCenterDrawableX(final Rect padding, final Key key,
	    Drawable icon) {
	if (icon == null) {
	    return 0;
	}
	return (key.width - padding.left - padding.right - icon
	        .getIntrinsicWidth()) / 2 + padding.left;
    }

    private final Rect rect = new Rect();
    private int        longPressTimeOut;
    private boolean    isTemporarySwitch;
    private Typeface   textTypeFace;
    private Path       octagonalPath;
    private Path       borderPath;

    private void paintLabel(CharSequence labelToPaint, Drawable icon, float x,
	    float y, int drawableX, int drawableY, Key key, Canvas canvas,
	    Paint paint) {

	// Switch the character to uppercase if shift is pressed
	String label = labelToPaint == null ? null : adjustCase(labelToPaint)
	        .toString();
	if (label != null) {
	    // For characters, use large font. For labels like "Done", use
	    // small font.
	    if ((label.length() > 1)
		    && ((key.codes != null) && (key.codes.length < 2))) {
		paint.setTextSize(this.labelTextSize);
		paint.getTextBounds(label, 0, label.length(), this.rect);

		// if (this.rect.right - this.rect.left < 20) {
		// paint.setTextSize(this.labelTextSize + 4);
		// }
	    } else {
		paint.setTextSize(this.keyTextSize);
	    }
	    paint.setTypeface(this.textTypeFace);
	    // Draw a drop shadow for the text
	    paint.setShadowLayer(this.shadowRadius, 0, 0, this.shadowColor);
	    canvas.drawText(label, x, y, paint);
	    // Turn off drop shadow
	    paint.setShadowLayer(0, 0, 0, 0);
	} else if (icon != null) {
	    canvas.translate(drawableX, drawableY);
	    icon.setBounds(5, 5, icon.getIntrinsicWidth() - 5,
		    icon.getIntrinsicHeight() - 5);
	    icon.draw(canvas);
	    canvas.translate(-drawableX, -drawableY);
	}
    }

    private int getKeyIndices(int x, int y, int[] allKeys) {
	final Key[] keys = this.keys;
	int primaryIndex = NOT_A_KEY;
	int closestKey = NOT_A_KEY;
	int closestKeyDist = this.mProximityThreshold + 1;
	java.util.Arrays.fill(this.mDistances, Integer.MAX_VALUE);
	int[] nearestKeyIndices = this.keyboard.getNearestKeys(x, y);
	final int keyCount = nearestKeyIndices.length;
	for (int i = 0; i < keyCount; i++) {
	    final Key key = keys[nearestKeyIndices[i]];
	    int dist = 0;
	    boolean isInside = key.isInside(x, y);
	    if (((this.mProximityCorrectOn && ((dist = key.squaredDistanceFrom(
		    x, y)) < this.mProximityThreshold)) || isInside)
		    && ((key.codes != null) && (key.codes[0] > 32))) {
		// Find insertion point
		final int nCodes = key.codes.length;
		if (dist < closestKeyDist) {
		    closestKeyDist = dist;
		    closestKey = nearestKeyIndices[i];
		}

		if (allKeys == null) {
		    continue;
		}

		for (int j = 0; j < this.mDistances.length; j++) {
		    if (this.mDistances[j] > dist) {
			// Make space for nCodes codes
			System.arraycopy(this.mDistances, j, this.mDistances, j
			        + nCodes, this.mDistances.length - j - nCodes);
			System.arraycopy(allKeys, j, allKeys, j + nCodes,
			        allKeys.length - j - nCodes);
			for (int c = 0; c < nCodes; c++) {
			    allKeys[j + c] = key.codes[c];
			    this.mDistances[j + c] = dist;
			}
			break;
		    }
		}
	    }

	    if (isInside) {
		primaryIndex = nearestKeyIndices[i];
	    }
	}
	if (primaryIndex == NOT_A_KEY) {
	    primaryIndex = closestKey;
	}
	return primaryIndex;
    }

    /**
     * Detects the key by using the current slide status.
     */
    private void detectAndSendKey(TouchInfo touchInfo, boolean isDown) {
	if (touchInfo.currentKeyIndex >= 0
	        && touchInfo.currentKeyIndex < this.keys.length) {
	    final Key key = this.keys[touchInfo.currentKeyIndex];
	    CharSequence text = null;
	    Object option = null;
	    switch (touchInfo.currentSwiping) {
	    case NO_SWIPING:
		text = key.text;
		option = key.keyboard;
		break;
	    case SWIPING_TOP:
		text = key.textTop;
		option = key.keyboardTop;
		break;
	    case SWIPING_BOTTOM:
		text = key.textBottom;
		option = key.keyboardBottom;
		break;
	    case SWIPING_LEFT:
		text = key.textLeft;
		option = key.keyboardLeft;
		break;
	    case SWIPING_RIGHT:
		text = key.textRight;
		option = key.keyboardRight;
		break;
	    }

	    if (text == null) {
		text = key.defaultText;
	    }
	    if (text != null) {
		// if the key has a text we write it
		this.keyboardActionListener.onText(text);
		this.keyboardActionListener.onRelease(NOT_A_KEY);
	    } else {
		// stores codes of each direction for alternatives
		ArrayList<Integer> codes = new ArrayList<Integer>(5);
		codes.add(key.codes != null ? key.codes[0] : 0);
		codes.add(key.codesTop != null ? key.codesTop[0] : 0);
		codes.add(key.codesBottom != null ? key.codesBottom[0] : 0);
		codes.add(key.codesLeft != null ? key.codesLeft[0] : 0);
		codes.add(key.codesRight != null ? key.codesRight[0] : 0);
		int swippedCode[];
		switch (touchInfo.currentSwiping) {
		case NO_SWIPING:
		    swippedCode = key.codes;
		    option = key.keyboard;
		    break;
		case SWIPING_TOP:
		    swippedCode = key.codesTop;
		    option = key.keyboardTop;
		    break;
		case SWIPING_BOTTOM:
		    swippedCode = key.codesBottom;
		    option = key.keyboardBottom;
		    break;
		case SWIPING_LEFT:
		    swippedCode = key.codesLeft;
		    option = key.keyboardLeft;
		    break;
		case SWIPING_RIGHT:
		    swippedCode = key.codesRight;
		    option = key.keyboardRight;
		    break;
		default:
		    swippedCode = key.codes;
		    option = key.keyboard;
		    break;
		}
		if (swippedCode == null) {
		    if (key.defaultCodes == null) {
			return;
		    }
		    swippedCode = key.defaultCodes;
		}

		Integer code = swippedCode[0];
		if (code != Integer.MAX_VALUE) {
		    codes.remove(code);
		    int[] alternatesCodes = new int[4];
		    for (int i = 0; i < alternatesCodes.length; i++) {
			alternatesCodes[i] = codes.get(i);
		    }
		    if (isDown) {
			this.keyboardActionListener.onKeyDown(code,
			        alternatesCodes, option);
		    } else {
			this.keyboardActionListener.onKeyUp(code,
			        alternatesCodes, option);
			this.keyboardActionListener.onRelease(code);
		    }
		}
	    }
	}
    }

    /**
     * Handle slide keys by producing the key label for the current slide state.
     */
    private CharSequence getPreviewText(TouchInfo touchInfo, Key key) {
	CharSequence chaine;
	switch (touchInfo.currentSwiping) {
	case NO_SWIPING:
	    chaine = key.label;
	    break;
	case SWIPING_BOTTOM:
	    chaine = key.labelBottom;
	    break;
	case SWIPING_TOP:
	    chaine = key.labelTop;
	    break;
	case SWIPING_LEFT:
	    chaine = key.labelLeft;
	    break;
	case SWIPING_RIGHT:
	    chaine = key.labelRight;
	    break;
	default:
	    chaine = key.label;
	    break;
	}
	if (chaine == null) {
	    chaine = key.label;
	}
	return adjustCase(chaine);
    }

    private void cancelPreview() {
	if (this.displayKeyPreview) {
	    this.handler.removeMessages(MSG_SHOW_PREVIEW);
	    if (previewPopup.isShowing()) {
		this.handler.sendMessageDelayed(
		        this.handler.obtainMessage(MSG_REMOVE_PREVIEW),
		        DELAY_AFTER_PREVIEW);
	    }
	}
    }

    private void showPreview(TouchInfo touchInfo) {

	final PopupWindow previewPopup = this.previewPopup;

	if (this.displayKeyPreview) {
	    // If key changed and preview is on ...
	    if (this.mShowPreview) {
		if (touchInfo.currentKeyIndex != NOT_A_KEY) {
		    if (previewPopup.isShowing()
			    && (this.previewText.getVisibility() == VISIBLE)) {
			// Show right away, if it's already visible and finger
			// is moving around
			showKey(touchInfo);
		    } else {
			Message obtainMessage = this.handler.obtainMessage(
			        MSG_SHOW_PREVIEW, touchInfo);
			this.handler.sendMessageDelayed(obtainMessage,
			        DELAY_BEFORE_PREVIEW);
		    }
		}
	    }
	}
    }

    private void showKey(TouchInfo touchInfo) {
	final PopupWindow previewPopup = this.previewPopup;
	final Key[] keys = this.keys;
	Key key = keys[touchInfo.currentKeyIndex];
	Drawable iconPreview = null;
	switch (touchInfo.currentSwiping) {
	case NO_SWIPING:
	    iconPreview = key.iconPreview;
	    break;
	case SWIPING_TOP:
	    iconPreview = key.iconPreviewTop;
	    break;
	case SWIPING_BOTTOM:
	    iconPreview = key.iconPreviewBottom;
	    break;
	case SWIPING_LEFT:
	    iconPreview = key.iconPreviewLeft;
	    break;
	case SWIPING_RIGHT:
	    iconPreview = key.iconPreviewRight;
	    break;
	}
	if (iconPreview != null) {
	    this.previewText
		    .setCompoundDrawables(null, null, null, iconPreview);
	    this.previewText.setText(null);
	} else {
	    this.previewText.setCompoundDrawables(null, null, null, null);
	    this.previewText.setText(getPreviewText(touchInfo, key));
	    if (((key.label != null) && (key.label.length() > 1))
		    && (key.codes.length < 2)) {
		this.previewText.setTextSize(this.keyTextSize);
		this.previewText.setTypeface(Typeface.DEFAULT_BOLD);
	    } else {
		this.previewText.setTextSize(this.previewTextSizeLarge);
		this.previewText.setTypeface(Typeface.DEFAULT);
	    }
	}
	this.previewText.measure(
	        MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED),
	        MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));
	int popupWidth = Math.max(this.previewText.getMeasuredWidth(),
	        key.width + this.previewText.getPaddingLeft()
	                + this.previewText.getPaddingRight());
	final int popupHeight = this.previewHeight;
	LayoutParams lp = this.previewText.getLayoutParams();
	if (lp != null) {
	    lp.width = popupWidth;
	    lp.height = popupHeight;
	}
	if (!this.mPreviewCentered) {
	    this.mPopupPreviewX = key.x - this.previewText.getPaddingLeft()
		    + getPaddingLeft();
	    this.mPopupPreviewY = key.y - popupHeight + this.previewOffset;
	} else {
	    this.mPopupPreviewX = 160 - this.previewText.getMeasuredWidth() / 2;
	    this.mPopupPreviewY = -this.previewText.getMeasuredHeight();
	}
	this.handler.removeMessages(MSG_REMOVE_PREVIEW);
	if (this.offsetInWindow == null) {
	    this.offsetInWindow = new int[2];
	    getLocationInWindow(this.offsetInWindow);
	    this.offsetInWindow[0] += this.mMiniKeyboardOffsetX; // Offset may
	    // be zero
	    this.offsetInWindow[1] += this.mMiniKeyboardOffsetY; // Offset may
	    // be zero
	}
	// Set the preview background state
	Drawable background = this.previewText.getBackground();
	if (background != null) {
	    int popupResId = 0;
	    switch (touchInfo.currentSwiping) {
	    case NO_SWIPING:
		popupResId = key.popupResId;
		break;
	    case SWIPING_TOP:
		popupResId = key.popupResIdTop;
		break;
	    case SWIPING_BOTTOM:
		popupResId = key.popupResIdBottom;
		break;
	    case SWIPING_LEFT:
		popupResId = key.popupResIdLeft;
		break;
	    case SWIPING_RIGHT:
		popupResId = key.popupResIdRight;
		break;
	    }
	    background.setState(popupResId != 0 ? LONG_PRESSABLE_STATE_SET
		    : EMPTY_STATE_SET);
	}
	if (previewPopup.isShowing()) {
	    previewPopup.update(this.mPopupPreviewX + this.offsetInWindow[0],
		    this.mPopupPreviewY + this.offsetInWindow[1], popupWidth,
		    popupHeight);
	} else {
	    previewPopup.setWidth(popupWidth);
	    previewPopup.setHeight(popupHeight);
	    previewPopup.showAtLocation(this.popupParent, Gravity.NO_GRAVITY,
		    this.mPopupPreviewX + this.offsetInWindow[0],
		    this.mPopupPreviewY + this.offsetInWindow[1]);
	}
	this.previewText.setVisibility(VISIBLE);
    }

    private void invalidateAll() {
	this.dirtyRect.union(0, 0, getWidth(), getHeight());
	this.mDrawPending = true;
	invalidate();
    }

    private void invalidateKey(int keyIndex) {
	if ((keyIndex < 0) || (keyIndex >= this.keys.length)) {
	    return;
	}
	final Key key = this.keys[keyIndex];
	this.invalidatedKeys.add(key);
	int centerOffset = (getWidth() - this.keyboard.getMinWidth()) / 2;
	this.dirtyRect.union(key.x + getPaddingLeft() + centerOffset, key.y
	        + getPaddingTop(), key.x + key.width + getPaddingLeft()
	        + centerOffset, key.y + key.height + getPaddingTop());
	onBufferDraw();
	invalidate(key.x + getPaddingLeft() + centerOffset, key.y
	        + getPaddingTop(), key.x + key.width + getPaddingLeft()
	        + centerOffset, key.y + key.height + getPaddingTop());
    }

    private boolean openPopupIfRequired(TouchInfo touchInfo) {
	// Check if we have a popup layout specified first.
	if (this.popupLayout == 0) {
	    return false;
	}
	// Check if the key really exist
	if ((touchInfo.currentKeyIndex < 0)
	        || (touchInfo.currentKeyIndex >= this.keys.length)) {
	    return false;
	}

	// Retrieve the key
	Key popupKey = this.keys[touchInfo.currentKeyIndex];
	boolean result = onLongPress(touchInfo, popupKey);
	if (result) {
	    cancelPreview();
	}
	return result;
    }

    /**
     * Called when a key is long pressed. By default this will open any popup
     * keyboard associated with this key through the attributes popupLayout and
     * popupCharacters.
     * 
     * @param popupKey
     *            the key that was long pressed
     * @return true if the long press is handled, false otherwise. Subclasses
     *         should call the method on the base class if the subclass doesn't
     *         wish to handle the call.
     */
    protected boolean onLongPress(TouchInfo touchInfo, Key popupKey) {
	int popupKeyboardId = 0;
	int codes[] = popupKey.codes;
	Object option = null;

	switch (touchInfo.currentSwiping) {
	case NO_SWIPING:
	    popupKeyboardId = popupKey.popupResId;
	    codes = popupKey.codes;
	    option = popupKey.keyboard;
	    break;
	case SWIPING_LEFT:
	    popupKeyboardId = popupKey.popupResIdLeft;
	    codes = popupKey.codesLeft;
	    option = popupKey.keyboardLeft;
	    break;
	case SWIPING_RIGHT:
	    popupKeyboardId = popupKey.popupResIdRight;
	    codes = popupKey.codesRight;
	    option = popupKey.keyboardRight;
	    break;
	case SWIPING_BOTTOM:
	    popupKeyboardId = popupKey.popupResIdBottom;
	    codes = popupKey.codesBottom;
	    option = popupKey.keyboardBottom;
	    break;
	case SWIPING_TOP:
	    popupKeyboardId = popupKey.popupResIdTop;
	    codes = popupKey.codesTop;
	    option = popupKey.keyboardTop;
	    break;
	}

	// there is no key on the swiping direction
	if (codes == null) {
	    return false;
	}

	CacheKey cacheKey = new CacheKey(popupKey, popupKeyboardId);

	if (popupKeyboardId != 0) {
	    this.miniKeyboardContainer = this.miniKeyboardCache.get(popupKey);
	    if (this.miniKeyboardContainer == null) {
		this.miniKeyboardContainer = ViewTools.inflateView(
		        getContext(), this.popupLayout);

		this.miniKeyboard = (SlideKeyboardView) this.miniKeyboardContainer
		        .findViewById(R.id.popupKeyboardView);
		View closeButton = this.miniKeyboardContainer
		        .findViewById(R.id.closeButton);
		if (closeButton != null) {
		    closeButton.setOnClickListener(this);
		}
		this.miniKeyboard
		        .setOnKeyboardActionListener(new OnKeyboardActionListener() {
			    public void onKeyUp(int primaryCode,
			            int[] keyCodes, Object option) {
			        SlideKeyboardView.this.keyboardActionListener
			                .onKeyUp(primaryCode, keyCodes, option);
			        dismissPopupKeyboard();
			    }

			    public void onText(CharSequence text) {
			        SlideKeyboardView.this.keyboardActionListener
			                .onText(text);
			        dismissPopupKeyboard();
			    }

			    @Override
			    public void swipeLeft() {
			    }

			    @Override
			    public void swipeRight() {
			    }

			    @Override
			    public void swipeUp() {
			    }

			    @Override
			    public void swipeDown() {
			    }

			    @Override
			    public void onPress(int primaryCode) {
			        SlideKeyboardView.this.keyboardActionListener
			                .onPress(primaryCode);
			    }

			    @Override
			    public void onRelease(int primaryCode) {
			        SlideKeyboardView.this.keyboardActionListener
			                .onRelease(primaryCode);
			    }

			    @Override
			    public void onKeyDown(int primaryCode,
			            int[] keyCodes, Object option) {
			        SlideKeyboardView.this.keyboardActionListener
			                .onKeyDown(primaryCode, keyCodes,
			                        option);

			    }

			    @Override
			    public void onLongKeyDown(int primaryCode,
			            int[] keyCodes, Object option) {
			        SlideKeyboardView.this.keyboardActionListener
			                .onLongKeyDown(primaryCode, keyCodes,
			                        option);

			    }
		        });
		// mInputView.setSuggest(mSuggest);
		AsigbeKeyboard keyboard = new AsigbeKeyboard(getContext(),
		        popupKeyboardId);
		this.miniKeyboard.setKeyboard(keyboard);
		this.miniKeyboard.setPopupParent(this);
		this.miniKeyboardContainer.measure(MeasureSpec.makeMeasureSpec(
		        getWidth(), MeasureSpec.AT_MOST), MeasureSpec
		        .makeMeasureSpec(getHeight(), MeasureSpec.AT_MOST));

		this.miniKeyboardCache
		        .put(cacheKey, this.miniKeyboardContainer);
	    } else {
		this.miniKeyboard = (SlideKeyboardView) this.miniKeyboardContainer
		        .findViewById(R.id.popupKeyboardView);
	    }
	    if (this.mWindowOffset == null) {
		this.mWindowOffset = new int[2];
		getLocationInWindow(this.mWindowOffset);
	    }
	    this.popupX = popupKey.x + getPaddingLeft();
	    this.popupY = popupKey.y + getPaddingTop();
	    this.popupX = this.popupX + popupKey.width
		    - this.miniKeyboardContainer.getMeasuredWidth();
	    this.popupY = this.popupY
		    - this.miniKeyboardContainer.getMeasuredHeight();
	    final int x = this.popupX
		    + this.miniKeyboardContainer.getPaddingRight()
		    + this.mWindowOffset[0];
	    final int y = this.popupY
		    + this.miniKeyboardContainer.getPaddingBottom()
		    + this.mWindowOffset[1];
	    this.miniKeyboard.setPopupOffset(x < 0 ? 0 : x, y);
	    this.miniKeyboard.setShifted(isShifted(), false);
	    this.popupKeyboard.setContentView(this.miniKeyboardContainer);
	    this.popupKeyboard.setWidth(this.miniKeyboardContainer
		    .getMeasuredWidth());
	    this.popupKeyboard.setHeight(this.miniKeyboardContainer
		    .getMeasuredHeight());
	    this.popupKeyboard.showAtLocation(this, Gravity.NO_GRAVITY, x, y);
	    this.miniKeyboardOnScreen = true;
	    // mMiniKeyboard.onTouchEvent(getTranslatedEvent(me));
	    invalidateAll();
	    return true;
	} else {
	    Integer code = codes[0];
	    if (code != Integer.MAX_VALUE) {
		SlideKeyboardView.this.keyboardActionListener.onLongKeyDown(
		        code, null, option);
	    }
	}
	return false;
    }

    private static class TouchInfo {
	private int     lastCodeX;
	private int     lastCodeY;
	private int     lastMoveX;
	private int     lastMoveY;
	private long    downTime;
	private long    lastMoveTime;
	private int     lastKey;
	private int     currentKeyIndex = NOT_A_KEY;
	private long    lastKeyTime;
	private long    currentKeyTime;
	private int     repeatKeyIndex  = NOT_A_KEY;
	private int     repeatSwiping;
	private boolean isRepeating;
	public Object   previousSwiping;
	public int      currentSwiping;

    }

    private final TouchInfo touchInfos[] = new TouchInfo[2];
    private int             pointerId1;
    private int             pointerId2;

    private boolean handleKeyDown(TouchInfo touchInfo, int touchX, int touchY,
	    int keyIndex, long eventTime) {
	// Debug.waitForDebugger();
	touchInfo.lastCodeX = touchX;
	touchInfo.lastCodeY = touchY;
	touchInfo.lastMoveX = touchX;
	touchInfo.lastMoveY = touchY;
	touchInfo.lastKeyTime = 0;
	touchInfo.currentKeyTime = 0;
	touchInfo.lastKey = NOT_A_KEY;
	touchInfo.currentKeyIndex = keyIndex;
	touchInfo.downTime = eventTime;
	touchInfo.lastMoveTime = touchInfo.downTime;
	this.keyboardActionListener
	        .onPress(((touchInfo.currentKeyIndex != NOT_A_KEY) && (this.keys[touchInfo.currentKeyIndex].codes != null)) ? this.keys[keyIndex].codes[0]
	                : 0);
	if (touchInfo.currentKeyIndex != NOT_A_KEY) {
	    Message msg = this.handler.obtainMessage(MSG_LONGPRESS, touchInfo);
	    this.handler.sendMessageDelayed(msg, this.longPressTimeOut);
	    // Release the old key and press the new key
	    final Key[] keys = this.keys;
	    if (keys.length > touchInfo.currentKeyIndex) {
		keys[touchInfo.currentKeyIndex].onPressed();
		invalidateKey(touchInfo.currentKeyIndex);
	    }
	    showPreview(touchInfo);
	} else {
	    return false;
	}
	if ((touchInfo.currentKeyIndex >= 0)
	        && isCurrentKeyRepeatable(touchInfo)) {
	    touchInfo.repeatKeyIndex = touchInfo.currentKeyIndex;
	    touchInfo.repeatSwiping = touchInfo.currentSwiping;
	    touchInfo.isRepeating = false;
	    Message msg = this.handler.obtainMessage(MSG_REPEAT, touchInfo);
	    this.handler.sendMessageDelayed(msg, REPEAT_START_DELAY);
	}
	identifySwipingMove(touchInfo, touchX, touchY);
	detectAndSendKey(touchInfo, true);
	return true;
    }

    private void handleKeyMove(TouchInfo touchInfo, int touchX, int touchY) {

	identifySwipingMove(touchInfo, touchX, touchY);
	if ((Math.abs(touchInfo.lastMoveX - touchX) > 2)
	        || (Math.abs(touchInfo.lastMoveY - touchY) > 2)) {
	    touchInfo.lastMoveX = touchX;
	    touchInfo.lastMoveY = touchY;
	    this.handler.removeMessages(MSG_LONGPRESS);
	    Message msg = this.handler.obtainMessage(MSG_LONGPRESS, touchInfo);
	    this.handler.sendMessageDelayed(msg, this.longPressTimeOut);
	}
    }

    private void handleKeyUp(TouchInfo touchInfo, int touchX, int touchY,
	    int keyIndex, long eventTime) {

	identifySwipingMove(touchInfo, touchX, touchY);
	detectAndSendKey(touchInfo, false);
	touchInfo.repeatKeyIndex = NOT_A_KEY;
	this.handler.removeMessages(MSG_SHOW_PREVIEW);
	this.handler.removeMessages(MSG_REPEAT);
	this.handler.removeMessages(MSG_LONGPRESS);
	if (keyIndex == touchInfo.currentKeyIndex) {
	    touchInfo.currentKeyTime += eventTime - touchInfo.lastMoveTime;
	} else {
	    touchInfo.lastKey = touchInfo.currentKeyIndex;
	    touchInfo.lastKeyTime = touchInfo.currentKeyTime + eventTime
		    - touchInfo.lastMoveTime;
	    touchInfo.currentKeyIndex = keyIndex;
	    touchInfo.currentKeyTime = 0;
	}
	if ((touchInfo.currentKeyTime < touchInfo.lastKeyTime)
	        && (touchInfo.lastKey != NOT_A_KEY)) {
	    touchInfo.currentKeyIndex = touchInfo.lastKey;
	    touchX = touchInfo.lastCodeX;
	    touchY = touchInfo.lastCodeY;
	}
	cancelPreview();
	releaseKey(touchInfo);
	Arrays.fill(this.mKeyIndices, NOT_A_KEY);
	touchInfo.lastKey = NOT_A_KEY;
	touchInfo.currentKeyIndex = NOT_A_KEY;

	invalidateKey(keyIndex);
	resetSwiping();
    }

    private void releaseKey(TouchInfo touchInfo) {
	final Key[] keys = this.keys;
	if (keys.length > touchInfo.currentKeyIndex
	        && touchInfo.currentKeyIndex >= 0) {
	    keys[touchInfo.currentKeyIndex].onReleased();
	    invalidateKey(touchInfo.currentKeyIndex);
	}
    }

    private void releaseAllKeys() {
	final Key[] keys = this.keys;
	for (Key key : keys) {
	    key.onReleased();
	}
    }

    @Override
    public boolean onTouchEvent(MotionEvent me) {

	// Needs to be called after the gesture detector gets a turn, as it may
	// have
	// displayed the mini keyboard
	if (this.miniKeyboardOnScreen) {
	    return true;
	}

	long eventTime = me.getEventTime();
	int touchX = (int) me.getX() - getPaddingLeft()
	        - (getWidth() - this.keyboard.getMinWidth()) / 2;
	int touchY = (int) me.getY() + this.verticalCorrection
	        - getPaddingTop();
	int action = me.getAction();
	int keyIndex = getKeyIndices(touchX, touchY, null);

	switch (action) {
	case MotionEvent.ACTION_DOWN:
	    if (!handleKeyDown(this.touchInfos[0], touchX, touchY, keyIndex,
		    eventTime)) {
		return false;
	    }
	    break;

	case MotionEvent.ACTION_MOVE:
	    handleKeyMove(this.touchInfos[0], touchX, touchY);
	    break;

	case MotionEvent.ACTION_UP:
	    handleKeyUp(this.touchInfos[0], touchX, touchY, keyIndex, eventTime);
	    break;
	}

	// Debug.waitForDebugger();
	/*
	 * int touchX; int touchY; // Extract the index of the pointer that left
	 * the touch sensor // final int pointerId =
	 * me.getPointerId(pointerIndex); int pointerIndex = (action &
	 * MotionEvent.ACTION_POINTER_ID_MASK) >>
	 * MotionEvent.ACTION_POINTER_ID_SHIFT; int pointerId =
	 * me.getPointerId(pointerIndex);
	 * 
	 * int keyIndex; switch (action & MotionEvent.ACTION_MASK) { case
	 * MotionEvent.ACTION_DOWN: this.pointerId1 = pointerId; //
	 * this.pointerId = me.getPointerId(pointerIndex); touchX = (int)
	 * me.getX(pointerIndex) - getPaddingLeft() - (getWidth() -
	 * this.keyboard.getMinWidth()) / 2; touchY = (int)
	 * me.getY(pointerIndex) + this.verticalCorrection - getPaddingTop();
	 * keyIndex = getKeyIndices(touchX, touchY, null); if
	 * (!handleKeyDown(this.touchInfos[pointerIndex], touchX, touchY,
	 * keyIndex, eventTime)) { return false; } break; case
	 * MotionEvent.ACTION_POINTER_DOWN: this.pointerId2 = pointerId; //
	 * this.pointerId = me.getPointerId(pointerIndex); touchX = (int)
	 * me.getX(pointerIndex) - getPaddingLeft() - (getWidth() -
	 * this.keyboard.getMinWidth()) / 2; touchY = (int)
	 * me.getY(pointerIndex) + this.verticalCorrection - getPaddingTop();
	 * keyIndex = getKeyIndices(touchX, touchY, null); if
	 * (!handleKeyDown(this.touchInfos[pointerIndex], touchX, touchY,
	 * keyIndex, eventTime)) { return false; } break;
	 * 
	 * // this.pointerId = me.getPointerId(pointerIndex); // touchX = (int)
	 * me.getX(pointerIndex) - getPaddingLeft() // - (getWidth() -
	 * this.keyboard.getMinWidth()) / 2; // touchY = (int)
	 * me.getY(pointerIndex) + this.verticalCorrection // - getPaddingTop();
	 * // keyIndex = getKeyIndices(touchX, touchY, null); // if
	 * (!handleKeyDown(this.touchInfos[pointerIndex], touchX, touchY, //
	 * keyIndex, eventTime)) { // return false; // } // break;
	 * 
	 * case MotionEvent.ACTION_MOVE: // pointerIndex = (action &
	 * MotionEvent.ACTION_POINTER_ID_MASK) >> //
	 * MotionEvent.ACTION_POINTER_ID_SHIFT; // if
	 * (me.getPointerId(pointerIndex) == this.pointerId) {
	 * 
	 * pointerIndex = me.findPointerIndex(this.pointerId2); if (pointerIndex
	 * != -1) { touchX = (int) me.getX(pointerIndex) - getPaddingLeft() -
	 * (getWidth() - this.keyboard.getMinWidth()) / 2; touchY = (int)
	 * me.getY(pointerIndex) + this.verticalCorrection - getPaddingTop();
	 * 
	 * handleKeyMove(this.touchInfos[pointerIndex], touchX, touchY);
	 * Log.d("test", "x:" + touchX + ",y:" + touchY + ",pointerId:" +
	 * pointerId); // } } break;
	 * 
	 * case MotionEvent.ACTION_POINTER_UP: case MotionEvent.ACTION_UP: //
	 * pointerIndex = me.findPointerIndex(this.pointerId); touchX = (int)
	 * me.getX(pointerIndex) - getPaddingLeft() - (getWidth() -
	 * this.keyboard.getMinWidth()) / 2; touchY = (int)
	 * me.getY(pointerIndex) + this.verticalCorrection - getPaddingTop();
	 * keyIndex = getKeyIndices(touchX, touchY, null);
	 * handleKeyUp(this.touchInfos[pointerIndex], touchX, touchY, keyIndex,
	 * eventTime); break;
	 * 
	 * // pointerIndex = me.findPointerIndex(this.pointerId); // touchX =
	 * (int) me.getX(pointerIndex) - getPaddingLeft() // - (getWidth() -
	 * this.keyboard.getMinWidth()) / 2; // touchY = (int)
	 * me.getY(pointerIndex) + this.verticalCorrection // - getPaddingTop();
	 * // handleKeyUp(this.touchInfos[pointerIndex], touchX, touchY, //
	 * keyIndex, eventTime); // break; }
	 */
	return true;
    }

    private boolean isCurrentKeyRepeatable(TouchInfo touchInfo) {
	if (this.keys[touchInfo.currentKeyIndex] == null) {
	    return false;
	}
	boolean isRepeatable = false;
	switch (touchInfo.currentSwiping) {
	case NO_SWIPING:
	    isRepeatable = this.keys[touchInfo.currentKeyIndex].repeatable;
	    break;
	case SWIPING_TOP:
	    isRepeatable = this.keys[touchInfo.currentKeyIndex].repeatableTop;
	    break;
	case SWIPING_BOTTOM:
	    isRepeatable = this.keys[touchInfo.currentKeyIndex].repeatableBottom;
	    break;
	case SWIPING_LEFT:
	    isRepeatable = this.keys[touchInfo.currentKeyIndex].repeatableLeft;
	    break;
	case SWIPING_RIGHT:
	    isRepeatable = this.keys[touchInfo.currentKeyIndex].repeatableRight;
	    break;
	}
	return isRepeatable;
    }

    public void cancelDetection() {
	if (this.touchInfos[0].currentKeyIndex >= 0
	        || this.touchInfos[1].currentKeyIndex >= 0) {
	    for (TouchInfo touchInfo : this.touchInfos) {
		releaseKey(touchInfo);
	    }
	    cancelPreview();
	    this.handler.removeMessages(MSG_SHOW_PREVIEW);
	    this.handler.removeMessages(MSG_REPEAT);
	    this.handler.removeMessages(MSG_LONGPRESS);
	    resetSwiping();
	    dismissPopupKeyboard();
	}
    }

    private void identifySwipingMove(TouchInfo touchInfo, int touchX, int touchY) {
	final float moveX = touchInfo.lastCodeX - touchX;
	final float moveY = touchInfo.lastCodeY - touchY;

	if (Math.abs(moveX) > Math.abs(moveY)) {
	    if (moveX < -this.sensitivity) {
		swipeRight(touchInfo);
	    } else if (moveX > this.sensitivity) {
		swipeLeft(touchInfo);
	    } else {
		noSwipe(touchInfo);
	    }
	} else {
	    if (moveY > this.sensitivity) {
		swipeUp(touchInfo);
	    } else if (moveY < -this.sensitivity) {
		swipeDown(touchInfo);
	    } else {
		noSwipe(touchInfo);
	    }
	}
    }

    private void resetSwiping() {
	for (TouchInfo touchInfo : this.touchInfos) {
	    touchInfo.currentSwiping = NO_SWIPING;
	    touchInfo.previousSwiping = NO_SWIPING;
	}
    }

    protected void swipeRight(TouchInfo touchInfo) {
	touchInfo.previousSwiping = touchInfo.currentSwiping;
	touchInfo.currentSwiping = SWIPING_RIGHT;
	showPreview(touchInfo);
	this.keyboardActionListener.swipeRight();
    }

    protected void swipeLeft(TouchInfo touchInfo) {
	touchInfo.previousSwiping = touchInfo.currentSwiping;
	touchInfo.currentSwiping = SWIPING_LEFT;
	showPreview(touchInfo);
	this.keyboardActionListener.swipeLeft();
    }

    protected void swipeUp(TouchInfo touchInfo) {
	touchInfo.previousSwiping = touchInfo.currentSwiping;
	touchInfo.currentSwiping = SWIPING_TOP;
	showPreview(touchInfo);
	this.keyboardActionListener.swipeUp();
    }

    protected void swipeDown(TouchInfo touchInfo) {
	touchInfo.previousSwiping = touchInfo.currentSwiping;
	touchInfo.currentSwiping = SWIPING_BOTTOM;
	showPreview(touchInfo);
	this.keyboardActionListener.swipeDown();
    }

    protected void noSwipe(TouchInfo touchInfo) {
	touchInfo.previousSwiping = touchInfo.currentSwiping;
	touchInfo.currentSwiping = NO_SWIPING;
	showPreview(touchInfo);
    }

    public void closing() {
	if (this.previewPopup.isShowing()) {
	    this.previewPopup.dismiss();
	}
	this.handler.removeMessages(MSG_LONGPRESS);
	this.handler.removeMessages(MSG_SHOW_PREVIEW);

	dismissPopupKeyboard();
	this.buffer = null;
	this.canvas = null;
	this.miniKeyboardCache.clear();
    }

    @Override
    public void onDetachedFromWindow() {
	super.onDetachedFromWindow();
	closing();
    }

    private void dismissPopupKeyboard() {
	if (this.popupKeyboard.isShowing()) {
	    this.popupKeyboard.dismiss();
	    this.miniKeyboardOnScreen = false;
	    invalidateAll();
	}
    }

    /**
     * Handles when the back button is pressed on the phone.
     */
    public boolean handleBack() {
	if (this.popupKeyboard.isShowing()) {
	    dismissPopupKeyboard();
	    return true;
	}
	return false;
    }

    private boolean repeatKey(TouchInfo touchInfo) {
	if ((touchInfo.isRepeating || (touchInfo.repeatKeyIndex == touchInfo.currentKeyIndex && touchInfo.repeatSwiping == touchInfo.currentSwiping))
	        && touchInfo.repeatKeyIndex < this.keys.length) {
	    final Key key = this.keys[touchInfo.repeatKeyIndex];
	    int code = key.codes[0];
	    if (code != Integer.MAX_VALUE) {
		this.keyboardActionListener.onKeyUp(code, key.codes,
		        key.keyboard);
		this.keyboardActionListener.onRelease(code);
		touchInfo.isRepeating = true;
		return true;
	    }
	}
	return false;
    }

    /**
     * Sets the sensitivity of the keyboard when swiping.
     */
    public void setSensitivity(int level) {
	this.sensitivity = level * 10;
    }

    /**
     * Sets the time interval between two repetition of a character (en ms).
     */
    public void setRepeatInterval(int repeatInterval) {
	this.repeatInterval = repeatInterval;
    }

    /**
     * Sets the time before the popup keyboard is displayed (en ms).
     */
    public void setLongPressTimeOut(int popupKeyboardTimeOut) {
	this.longPressTimeOut = popupKeyboardTimeOut;
    }

    /**
     * Indicates if the view is temporary.
     */
    public void setTemporarySwitch(boolean isTemporarySwitch) {
	this.isTemporarySwitch = isTemporarySwitch;
	if (this.isTemporarySwitch) {
	    setBackgroundColor(this.temporaryBackgroundColor);
	} else {
	    setBackgroundColor(this.permanentBackgroundColor);
	}
    }

    /**
     * Gets if the view is temporary.
     */
    public boolean isTemporarySwitch() {
	return this.isTemporarySwitch;
    }

    /**
     * Sets the boolean which indicates if the preview popup is displayed.
     */
    public void setDisplayKeyPreview(boolean displayKeyPreview) {
	this.displayKeyPreview = displayKeyPreview;
    }
}
