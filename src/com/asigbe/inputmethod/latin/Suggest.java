package com.asigbe.inputmethod.latin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import android.content.Context;
import android.content.res.AssetManager;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.provider.UserDictionary;
import android.text.AutoText;
import android.text.TextUtils;
import android.view.View;

/**
 * This class loads a dictionary and provides a list of suggestions for a given
 * sequence of characters. This includes corrections and completions.
 * 
 * @hide pending API Council Approval
 */
public class Suggest implements Dictionary.WordCallback {

    public final static class Suggestion {
	public final CharSequence word;
	public final boolean      isUserWord;

	public Suggestion(CharSequence word, boolean isUserWord) {
	    this.word = word;
	    this.isUserWord = isUserWord;
	}
    }

    public static final int    CORRECTION_NONE     = 0;
    public static final int    CORRECTION_BASIC    = 1;
    public static final int    CORRECTION_FULL     = 2;

    private Dictionary         mMainDict;

    private int                mPrefMaxSuggestions = 12;

    private int[]              mPriorities         = new int[mPrefMaxSuggestions];
    private List<Suggestion>   mSuggestions        = new ArrayList<Suggestion>();
    private boolean            mIncludeTypedWordIfValid;
    private List<CharSequence> mStringPool         = new ArrayList<CharSequence>();
    private Context            mContext;
    private boolean            mHaveCorrection;
    private CharSequence       mOriginalWord;
    private String             mLowerOriginalWord;

    private int                mCorrectionMode     = CORRECTION_BASIC;

    public Suggest(Context context, AssetManager assetManager) {
	this(context);
	mMainDict = new BinaryDictionary(context, assetManager);
    }

    public Suggest(Context context) {
	mContext = context;
	for (int i = 0; i < mPrefMaxSuggestions; i++) {
	    StringBuilder sb = new StringBuilder(32);
	    mStringPool.add(sb);
	}
    }

    public int getCorrectionMode() {
	return mCorrectionMode;
    }

    public void setCorrectionMode(int mode) {
	mCorrectionMode = mode;
    }

    /**
     * Number of suggestions to generate from the input key sequence. This has
     * to be a number between 1 and 100 (inclusive).
     * 
     * @param maxSuggestions
     * @throws IllegalArgumentException
     *             if the number is out of range
     */
    public void setMaxSuggestions(int maxSuggestions) {
	if (maxSuggestions < 1 || maxSuggestions > 100) {
	    throw new IllegalArgumentException(
		    "maxSuggestions must be between 1 and 100");
	}
	mPrefMaxSuggestions = maxSuggestions;
	mPriorities = new int[mPrefMaxSuggestions];
	collectGarbage();
	while (mStringPool.size() < mPrefMaxSuggestions) {
	    StringBuilder sb = new StringBuilder(32);
	    mStringPool.add(sb);
	}
    }

    private boolean haveSufficientCommonality(String original,
	    CharSequence suggestion) {
	final int len = Math.min(original.length(), suggestion.length());
	if (len <= 2)
	    return true;
	int matching = 0;
	for (int i = 0; i < len; i++) {
	    if (Character.toLowerCase(original.charAt(i)) == Character
		    .toLowerCase(suggestion.charAt(i))) {
		matching++;
	    }
	}
	if (len <= 4) {
	    return matching >= 2;
	} else {
	    return matching > len / 2;
	}
    }

    /**
     * Returns a list of words that match the list of character codes passed in.
     * This list will be overwritten the next time this function is called.
     * 
     * @param a
     *            view for retrieving the context for AutoText
     * @param codes
     *            the list of codes. Each list item contains an array of
     *            character codes in order of probability where the character at
     *            index 0 in the array has the highest probability.
     * @return list of suggestions.
     */
    public List<Suggestion> getSuggestions(View view,
	    WordComposer wordComposer, boolean includeTypedWordIfValid) {
	mHaveCorrection = false;
	collectGarbage();
	Arrays.fill(mPriorities, 0);
	mIncludeTypedWordIfValid = includeTypedWordIfValid;

	// Save a lowercase version of the original word
	mOriginalWord = wordComposer.getTypedWord();
	if (mOriginalWord != null) {
	    mOriginalWord = mOriginalWord.toString();
	    mLowerOriginalWord = mOriginalWord.toString().toLowerCase();
	} else {
	    mLowerOriginalWord = "";
	}
	// Search the dictionary only if there are at least 2 characters
	if (wordComposer.size() > 1) {
	    // mUserDictionary.getWords(wordComposer, this);
	    getUserDictionaryWords(wordComposer.getTypedWord());
	    if (mSuggestions.size() > 0 && isValidWord(mOriginalWord)) {
		mHaveCorrection = true;
	    }
	    if (mMainDict != null) {
		mMainDict.getWords(wordComposer, this);
	    }
	    if (mCorrectionMode == CORRECTION_FULL && mSuggestions.size() > 0) {
		mHaveCorrection = true;
	    }
	}
	if (mOriginalWord != null) {
	    mSuggestions.add(0, new Suggestion(mOriginalWord, false));
	}

	// Check if the first suggestion has a minimum number of characters in
	// common
	if (mCorrectionMode == CORRECTION_FULL && mSuggestions.size() > 1) {
	    if (!haveSufficientCommonality(mLowerOriginalWord, mSuggestions
		    .get(1).word)) {
		mHaveCorrection = false;
	    }
	}

	int i = 0;
	int max = 6;
	// Don't autotext the suggestions from the dictionaries
	if (mCorrectionMode == CORRECTION_BASIC)
	    max = 1;
	while (i < mSuggestions.size() && i < max) {
	    String suggestedWord = mSuggestions.get(i).toString().toLowerCase();
	    CharSequence autoText = AutoText.get(suggestedWord, 0,
		    suggestedWord.length(), view);
	    // Is there an AutoText correction?
	    boolean canAdd = autoText != null;
	    // Is that correction already the current prediction (or original
	    // word)?
	    canAdd &= !TextUtils.equals(autoText, mSuggestions.get(i).word);
	    // Is that correction already the next predicted word?
	    if (canAdd && i + 1 < mSuggestions.size()
		    && mCorrectionMode != CORRECTION_BASIC) {
		canAdd &= !TextUtils.equals(autoText,
		        mSuggestions.get(i + 1).word);
	    }
	    if (canAdd) {
		mHaveCorrection = true;
		mSuggestions.add(i + 1, new Suggestion(autoText, false));
		i++;
	    }
	    i++;
	}

	return mSuggestions;
    }

    public boolean hasMinimalCorrection() {
	return mHaveCorrection;
    }

    private boolean compareCaseInsensitive(final String mLowerOriginalWord,
	    final char[] word, final int offset, final int length) {
	final int originalLength = mLowerOriginalWord.length();
	if (originalLength == length && Character.isUpperCase(word[offset])) {
	    for (int i = 0; i < originalLength; i++) {
		if (mLowerOriginalWord.charAt(i) != Character
		        .toLowerCase(word[offset + i])) {
		    return false;
		}
	    }
	    return true;
	}
	return false;
    }

    public boolean addWord(final char[] word, final int offset,
	    final int length, final int freq, boolean isUserWord) {
	int pos = 0;
	final int[] priorities = mPriorities;
	final int prefMaxSuggestions = mPrefMaxSuggestions;
	// Check if it's the same word, only caps are different
	if (compareCaseInsensitive(mLowerOriginalWord, word, offset, length)) {
	    pos = 0;
	} else {
	    // Check the last one's priority and bail
	    if (priorities[prefMaxSuggestions - 1] >= freq)
		return true;
	    while (pos < prefMaxSuggestions) {
		if (priorities[pos] < freq
		        || (priorities[pos] == freq && length < mSuggestions
		                .get(pos).word.length())) {
		    break;
		}
		pos++;
	    }
	}

	if (pos >= prefMaxSuggestions) {
	    return true;
	}
	System.arraycopy(priorities, pos, priorities, pos + 1,
	        prefMaxSuggestions - pos - 1);
	priorities[pos] = freq;
	int poolSize = mStringPool.size();
	StringBuilder sb = poolSize > 0 ? (StringBuilder) mStringPool
	        .remove(poolSize - 1) : new StringBuilder(32);
	sb.setLength(0);
	sb.append(word, offset, length);
	mSuggestions.add(pos, new Suggestion(sb, isUserWord));
	if (mSuggestions.size() > prefMaxSuggestions) {
	    CharSequence garbage = mSuggestions.remove(prefMaxSuggestions).word;
	    if (garbage instanceof StringBuilder) {
		mStringPool.add(garbage);
	    }
	}
	return true;
    }

    public boolean isValidWord(final CharSequence word) {
	if (word == null || word.length() == 0) {
	    return false;
	}
	return (mCorrectionMode == CORRECTION_FULL && mMainDict != null && mMainDict
	        .isValidWord(word))
	        || (mCorrectionMode > CORRECTION_NONE && (userDictionaryIsValidWord(word)));
    }

    private void collectGarbage() {
	int poolSize = mStringPool.size();
	int garbageSize = mSuggestions.size();
	while (poolSize < mPrefMaxSuggestions && garbageSize > 0) {
	    CharSequence garbage = mSuggestions.get(garbageSize - 1).word;
	    if (garbage != null && garbage instanceof StringBuilder) {
		mStringPool.add(garbage);
		poolSize++;
	    }
	    garbageSize--;
	}
	// if (poolSize == mPrefMaxSuggestions + 1) {
	// Log.w("Suggest", "String pool got too big: " + poolSize);
	// }
	mSuggestions.clear();
    }

    /**
     * Indicates if the dictionary contains the word.
     */
    public boolean containsWord(CharSequence word) {
	return (mMainDict == null ? false : mMainDict.containsWord(word))
	        || userDictionaryContainsWord(word);
    }

    /**
     * Indicates if the user dictionary contains word.
     */
    public boolean userDictionaryContainsWord(CharSequence word) {
	Cursor cursor = mContext.getContentResolver()
	        .query(
	                UserDictionary.Words.CONTENT_URI,
	                null,
	                "Words.WORD=?",
	                new String[] { DatabaseUtils.sqlEscapeString(word
	                        .toString()) }, null);
	boolean isValidWord = cursor != null && cursor.getCount() > 0;
	if (cursor != null) {
	    cursor.close();
	}
	return isValidWord;
    }

    /**
     * Indicates if the user dictionary contains word.
     */
    public boolean userDictionaryIsValidWord(CharSequence word) {
	Cursor cursor = mContext.getContentResolver().query(
	        UserDictionary.Words.CONTENT_URI, null, "Words.WORD like ?",
	        new String[] { word.toString() + "%" }, null);

	boolean isValidWord = cursor != null && cursor.getCount() > 0;
	if (cursor != null) {
	    cursor.close();
	}
	return isValidWord;
    }

    /**
     * Deletes the given word from the user dictionary.
     */
    public void deleteWord(CharSequence word) {
	mContext.getContentResolver().delete(UserDictionary.Words.CONTENT_URI,
	        "Words.WORD = ?", new String[] { word.toString() });
    }

    private final HashSet<String> words = new HashSet<String>();

    private boolean getUserDictionaryWords(CharSequence word) {
	this.words.clear();
	Cursor cursor = mContext.getContentResolver().query(
	        UserDictionary.Words.CONTENT_URI, null, "Words.WORD like ?",
	        new String[] { word.toString() + "%" }, null);

	boolean isValidWord = cursor != null && cursor.getCount() > 0;
	if (cursor != null) {
	    if (cursor.moveToFirst()) {
		String foundWord = null;
		int frequency;
		do {
		    foundWord = cursor.getString(cursor
			    .getColumnIndex(UserDictionary.Words.WORD));
		    if (!this.words.contains(foundWord)) {
			frequency = cursor
			        .getInt(cursor
			                .getColumnIndex(UserDictionary.Words.FREQUENCY));
			addWord(foundWord.toCharArray(), 0, foundWord.length(),
			        frequency, true);
			this.words.add(foundWord);
		    }
		} while (cursor.moveToNext());
	    }
	    if (cursor != null) {
		cursor.close();
	    }
	}
	return isValidWord;
    }
}
