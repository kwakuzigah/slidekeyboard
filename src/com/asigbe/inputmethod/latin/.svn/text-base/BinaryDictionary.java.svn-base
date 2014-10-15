package com.asigbe.inputmethod.latin;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;

import com.asigbe.slidekeyboardpro.R;

/**
 * Implements a static, compacted, binary dictionary of standard words.
 */
public class BinaryDictionary extends Dictionary {

    private static final byte               LETTER_OFFSET    = 0;
    private static final byte               FREQUENCY_OFFSET = 2;
    private static final byte               COUNT_OFFSET     = 4;
    private static final byte               CHILD_OFFSET     = 6;
    private static final byte               DATA_SIZE        = 10;

    private static final int                MAX_WORD_LENGTH  = 48;
    private static final int                MAX_ALTERNATIVES = 4;
    private static final int                MAX_WORDS        = 16;

    private final int[]                     inputCodes       = new int[MAX_WORD_LENGTH
	                                                             * MAX_ALTERNATIVES];
    private final char[]                    outputChars      = new char[MAX_WORD_LENGTH
	                                                             * MAX_WORDS];
    private final int[]                     mFrequencies     = new int[MAX_WORDS];
    private final Map<Character, Character> alternatesCharacter;

    /**
     * Create a dictionary from a raw resource file
     * 
     * @param context
     *            application context for reading resources
     * @param resId
     *            the resource containing the raw binary dictionary
     */
    public BinaryDictionary(Context context, AssetManager assetManager) {
	this.alternatesCharacter = new HashMap<Character, Character>();
	Map<Character, String> alternates = new HashMap<Character, String>();
	alternates.put('a', context.getResources().getString(
	        R.string.alternates_for_a));
	alternates.put('c', context.getResources().getString(
	        R.string.alternates_for_c));
	alternates.put('e', context.getResources().getString(
	        R.string.alternates_for_e));
	alternates.put('i', context.getResources().getString(
	        R.string.alternates_for_i));
	alternates.put('n', context.getResources().getString(
	        R.string.alternates_for_n));
	alternates.put('o', context.getResources().getString(
	        R.string.alternates_for_o));
	alternates.put('s', context.getResources().getString(
	        R.string.alternates_for_s));
	alternates.put('u', context.getResources().getString(
	        R.string.alternates_for_u));
	alternates.put('y', context.getResources().getString(
	        R.string.alternates_for_y));
	alternates.put('z', context.getResources().getString(
	        R.string.alternates_for_z));

	for (Map.Entry<Character, String> entrySet : alternates.entrySet()) {
	    Character character = entrySet.getKey();
	    String alternate = entrySet.getValue();
	    for (int i = 0; i < alternate.length(); i++) {
		char charAt = alternate.charAt(i);
		this.alternatesCharacter.put(charAt, character);
	    }
	}

	if (assetManager != null) {
	    loadDictionary(context, assetManager);
	}
    }

    private byte[] datas;

    private final void loadDictionary(Context context, AssetManager assetManager) {
 
	AssetFileDescriptor afd;

	FileInputStream inputStream = null;
	this.datas = null;
	try {
	    afd = assetManager.openNonAssetFd("assets/raw/main.mp3");
	    inputStream = afd.createInputStream();
	    this.datas = new byte[(int) afd.getLength()];
	    inputStream.read(this.datas);
	    inputStream.close();
	    afd.close();
	} catch (IOException e) {
	    // TODO Auto-generated catch block
	    e.printStackTrace();
	}
    }

    protected static char bytesToChar(byte[] bytes) {
	char value = 0;

	for (byte b : bytes) {
	    value = (char) (value << 8);
	    value += b & 0xff;
	}

	return value;
    }

    protected static int bytesToInt(byte[] bytes) {
	int value = 0;

	for (byte b : bytes) {
	    value = value << 8;
	    value += b & 0xff;
	}

	return value;
    }

    protected static short bytesToShort(byte[] bytes) {
	short value = 0;

	for (byte b : bytes) {
	    value = (short) (value << 8);
	    value += b & 0xff;
	}

	return value;
    }

    private char convertChar(char character) {

	char lowerCaseCharacter = Character.toLowerCase(character);
	Character foundCharacter = this.alternatesCharacter
	        .get(lowerCaseCharacter);
	if (foundCharacter != null) {
	    return foundCharacter;
	}

	return lowerCaseCharacter;
    }

    /**
     * Finds the list of words corresponding to the first letters.
     * 
     * @param offset
     *            the offset of the current letter on the data
     * @param words
     *            the words found by the search
     * @param frequencies
     *            the frequencies of the word found by the search
     * @param firstLetters
     *            the first letters of the word we want to found
     * @param word
     *            the current word
     * @param alternates
     *            the alternates key of the word
     * @param charIndex
     *            the index of the letter being currently search for
     */
    private final byte frequencyData[]   = new byte[2];
    private final byte childCountData[]  = new byte[2];
    private final byte childOffsetData[] = new byte[4];
    private final byte letterData[]      = new byte[2];

    public void getWords(int offset, List<String> words,
	    List<Short> frequencies, String firstLetters, String word,
	    int alternates[], int charIndex, boolean isAlternate) {
	if (words.size() >= MAX_WORDS) {
	    return;
	}

	if (firstLetters.length() == 0) {
	    System.arraycopy(this.datas, offset + FREQUENCY_OFFSET,
		    this.frequencyData, 0, 2);
	    short frequency = bytesToShort(this.frequencyData);
	    int wordLength = word.length();
	    if ((frequency != 0) && (wordLength < MAX_WORD_LENGTH)) {
		words.add(word);
		frequencies.add(frequency);
	    }

	    System.arraycopy(this.datas, offset + COUNT_OFFSET,
		    this.childCountData, 0, 2);
	    short childCount = bytesToShort(this.childCountData);

	    System.arraycopy(this.datas, offset + CHILD_OFFSET,
		    this.childOffsetData, 0, 4);
	    int childOffset = bytesToInt(this.childOffsetData);
	    for (int i = 0; i < childCount; i++) {
		int currentOffset = childOffset + DATA_SIZE * i;

		System.arraycopy(this.datas, currentOffset + LETTER_OFFSET,
		        this.letterData, 0, 2);
		char convertChar = bytesToChar(this.letterData);
		getWords(currentOffset, words, frequencies, firstLetters, word
		        .concat(Character.toString(convertChar)), alternates,
		        ++charIndex, isAlternate);
	    }
	} else {
	    char firstLetter = firstLetters.charAt(0);
	    int foundOffset = findOffsetForCharacter(offset, firstLetter);

	    int nextCharIndex = charIndex + 1;
	    if (foundOffset != 0) {
		// the character has been found
		getWords(foundOffset, words, frequencies, firstLetters
		        .substring(1), word.concat(Character
		        .toString(firstLetter)), alternates, nextCharIndex,
		        isAlternate);
	    }

	    // if the character has not been found or no word has been found
	    if ((foundOffset == 0) || (words.size() < MAX_WORDS)) {
		// the character has not been found, we are going to search for
		// alternates
		int begin = charIndex * MAX_ALTERNATIVES;
		int end = charIndex * MAX_ALTERNATIVES + MAX_ALTERNATIVES;
		for (int i = begin; i < end; i++) {
		    foundOffset = findOffsetForCharacter(offset, alternates[i]);
		    if (foundOffset != 0) {
			// the character has been found
			getWords(foundOffset, words, frequencies, firstLetters
			        .substring(1), word.concat(Character
			        .toString((char) alternates[i])), alternates,
			        nextCharIndex, true);
		    }
		}
	    }
	}
    }

    /**
     * Finds the list of words corresponding to the given word.
     * 
     * @param offset
     *            the offset of the current letter on the data
     * @param frequencies
     *            the frequencies of the word found by the search
     * @param firstLetters
     *            the first letters of the word we want to found
     * @param charIndex
     *            the index of the letter being currently search for
     */

    private final byte frequencyDataWord[] = new byte[2];

    private boolean containsWord(int offset, String firstLetters, String word,
	    int charIndex) {

	if (firstLetters.length() == 0) {
	    System.arraycopy(this.datas, offset + FREQUENCY_OFFSET,
		    this.frequencyDataWord, 0, 2);
	    short frequency = bytesToShort(this.frequencyDataWord);
	    int wordLength = word.length();
	    if ((frequency != 0) && (wordLength < MAX_WORD_LENGTH)) {
		// the word has been found
		return true;
	    }
	} else {
	    char firstLetter = firstLetters.charAt(0);
	    int foundOffset = findOffsetForCharacter(offset, firstLetter);

	    int nextCharIndex = charIndex + 1;
	    if (foundOffset != 0) {
		// the character has been found
		return containsWord(foundOffset, firstLetters.substring(1),
		        word.concat(Character.toString(firstLetter)),
		        nextCharIndex);
	    }
	}
	return false;
    }

    private final byte childCountDataForCharacter[]  = new byte[2];
    private final byte letterDataForCharacter[]      = new byte[2];
    private final byte childOffsetDataForCharacter[] = new byte[4];

    public int findOffsetForCharacter(int offset, int letter) {
	System.arraycopy(this.datas, offset + COUNT_OFFSET,
	        this.childCountDataForCharacter, 0, 2);
	short childCount = bytesToShort(this.childCountDataForCharacter);
	System.arraycopy(this.datas, offset + CHILD_OFFSET,
	        this.childOffsetDataForCharacter, 0, 4);
	int childOffset = bytesToInt(this.childOffsetDataForCharacter);
	int foundOffset = 0;
	for (int i = 0; (i < childCount) && (foundOffset == 0); i++) {
	    int currentOffset = childOffset + DATA_SIZE * i;

	    System.arraycopy(this.datas, currentOffset + LETTER_OFFSET,
		    this.letterDataForCharacter, 0, 2);
	    char convertChar = bytesToChar(this.letterDataForCharacter);
	    if (Character.toLowerCase(convertChar) == Character
		    .toLowerCase(letter)) {
		foundOffset = currentOffset;
	    }
	}
	return foundOffset;
    }

    private final List<String> words       = new ArrayList<String>();
    private final List<Short>  frequencies = new ArrayList<Short>();

    public int getSuggestionsNative(CharSequence typedWord) {
	this.words.clear();
	this.frequencies.clear();
	String typedWordString = typedWord.toString();
	getWords(0, this.words, this.frequencies, typedWordString, "",
	        this.inputCodes, 0, false);

	int index = this.words.indexOf(typedWordString);
	if (index != -1) {
	    this.words.remove(index);
	    this.frequencies.remove(index);
	}
	int wordOffset = 0;
	for (int i = 0; i < this.words.size(); i++) {
	    String word = this.words.get(i);
	    short frequency = this.frequencies.get(i);
	    int wordLength = word.length();
	    System.arraycopy(word.toCharArray(), 0, this.outputChars,
		    wordOffset, wordLength);
	    wordOffset += MAX_WORD_LENGTH;
	    this.mFrequencies[i] = frequency;
	}
	return this.words.size();
    }

    @Override
    public void getWords(final WordComposer wordComposer,
	    final WordCallback callback) {
	final int codesSize = wordComposer.size();
	// Wont deal with really long words.
	if (codesSize > MAX_WORD_LENGTH - 1) {
	    return;
	}

	Arrays.fill(this.inputCodes, -1);
	for (int i = 0; i < codesSize; i++) {
	    int[] alternatives = wordComposer.getCodesAt(i);
	    System.arraycopy(alternatives, 0, this.inputCodes, i
		    * MAX_ALTERNATIVES, Math.min(alternatives.length,
		    MAX_ALTERNATIVES));
	}
	Arrays.fill(this.outputChars, (char) 0);

	int count = getSuggestionsNative(wordComposer.getTypedWord());

	for (int j = 0; j < count; j++) {
	    if (this.mFrequencies[j] < 1) {
		break;
	    }
	    int start = j * MAX_WORD_LENGTH;
	    int len = 0;
	    while (this.outputChars[start + len] != 0) {
		len++;
	    }
	    if (len > 0) {
		callback.addWord(this.outputChars, start, len,
		        this.mFrequencies[j], false);
	    }
	}
    }

    public boolean isValidWord(int offset, String firstLetters) {

	if (firstLetters.length() == 0) {
	    // the word has been found
	    return true;
	}

	byte childCountData[] = new byte[2];
	System.arraycopy(this.datas, offset + COUNT_OFFSET, childCountData, 0,
	        2);
	short childCount = bytesToShort(childCountData);

	byte childOffsetData[] = new byte[4];
	System.arraycopy(this.datas, offset + CHILD_OFFSET, childOffsetData, 0,
	        4);
	int childOffset = bytesToInt(childOffsetData);

	char firstLetter = firstLetters.charAt(0);
	int foundOffset = 0;
	for (int i = 0; (i < childCount) && (foundOffset == 0); i++) {
	    int currentOffset = childOffset + DATA_SIZE * i;

	    byte letterData[] = new byte[2];
	    System.arraycopy(this.datas, currentOffset + LETTER_OFFSET,
		    letterData, 0, 2);
	    char convertChar = bytesToChar(letterData);
	    if (Character.toLowerCase(convertChar) == Character
		    .toLowerCase(firstLetter)) {
		foundOffset = currentOffset;
	    }
	}

	if (foundOffset != 0) {
	    return isValidWord(foundOffset, firstLetters.substring(1));
	}

	return false;
    }

    @Override
    public boolean isValidWord(CharSequence word) {
	if (word == null) {
	    return false;
	}
	char[] chars = word.toString().toLowerCase().toCharArray();
	return isValidWord(0, new String(chars));
    }

    @Override
    public boolean containsWord(CharSequence word) {
	return containsWord(0, word.toString(), "", 0);
    }

    // unused
    // public synchronized void close() {
    // if (mNativeDict != 0) {
    // closeNative(mNativeDict);
    // mNativeDict = 0;
    // }
    // this.am.close();
    // }

    // @Override
    // protected void finalize() throws Throwable {
    // close();
    // super.finalize();
    // }
}
