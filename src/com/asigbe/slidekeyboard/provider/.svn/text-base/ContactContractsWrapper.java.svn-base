package com.asigbe.slidekeyboard.provider;

import android.content.Context;
import android.database.Cursor;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.CommonDataKinds.Email;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.CommonDataKinds.StructuredPostal;

/**
 * Wrapper used for retro-compatibility.
 * 
 * @author Delali Zigah
 */
public class ContactContractsWrapper {

    private final Context context;

    static {
	try {
	    Class.forName("android.provider.ContactsContract");
	} catch (Exception ex) {
	    throw new RuntimeException(ex);
	}
    }

    /** Checks availability of the class */
    public static void checkAvailable() {
    }

    /**
     * Creates a wrapper for the contact manager.
     */
    public ContactContractsWrapper(Context context) {
	this.context = context;
    }

    /**
     * Get the list of phones contacts.
     */
    public Cursor getContactsPhones() {

	return this.context.getContentResolver().query(
	        Data.CONTENT_URI,
	        new String[] { Contacts.DISPLAY_NAME,
	                Data._ID, Phone.NUMBER },
	        Data.MIMETYPE + "='" + Phone.CONTENT_ITEM_TYPE + "'",
	        null,
	        Contacts.DISPLAY_NAME
	                + " COLLATE LOCALIZED ASC");
    }

    /**
     * Get the phone contacts field to display.
     */
    public String[] getContactsPhonesFields() {
	return new String[] { Data.DISPLAY_NAME, Phone.NUMBER };
    }
    
    /**
     * Get the list of emails contacts.
     */
    public Cursor getContactsEmails() {

	return this.context.getContentResolver().query(
	        Data.CONTENT_URI,
	        new String[] { Contacts.DISPLAY_NAME,
	                Data._ID, Email.DATA },
	        Data.MIMETYPE + "='" + Email.CONTENT_ITEM_TYPE + "'",
	        null,
	        Contacts.DISPLAY_NAME
	                + " COLLATE LOCALIZED ASC");
    }

    /**
     * Get the email contacts field to display.
     */
    public String[] getContactsEmailsFields() {
	return new String[] { Data.DISPLAY_NAME, Email.DATA };
    }
    
    /**
     * Get the list of addresses contacts.
     */
    public Cursor getContactsAddresses() {

	return this.context.getContentResolver().query(
	        Data.CONTENT_URI,
	        new String[] { Contacts.DISPLAY_NAME,
	                Data._ID, StructuredPostal.FORMATTED_ADDRESS },
	        Data.MIMETYPE + "='" + StructuredPostal.CONTENT_ITEM_TYPE + "'",
	        null,
	        Contacts.DISPLAY_NAME
	                + " COLLATE LOCALIZED ASC");
    }

    /**
     * Get the email addresses field to display.
     */
    public String[] getContactsAddressesFields() {
	return new String[] { Data.DISPLAY_NAME, StructuredPostal.FORMATTED_ADDRESS };
    }

}
