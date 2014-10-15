package com.asigbe.slidekeyboardpro;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

/**
 * Displays an information dialog to the user.
 * 
 * @author Delali Zigah
 */
public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
	// showDialog(INFORMATION_DIALOG);
	setContentView(R.layout.explanation);
	Button goToMarketButton = (Button) findViewById(R.id.goToMarketButton);
	goToMarketButton.setOnClickListener(new OnClickListener() {

	    @Override
	    public void onClick(View v) {
		Intent intent = new Intent(Intent.ACTION_VIEW);
		intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		intent
		        .setData(Uri
		                .parse("http://market.android.com/search?q=asigbe skin"));
		startActivity(intent);
	    }
	});
	Button settingsButton = (Button) findViewById(R.id.settingsButton);
	settingsButton.setOnClickListener(new OnClickListener() {

	    @Override
	    public void onClick(View v) {
		Intent intent = new Intent();
		intent.setClass(MainActivity.this, LatinIMESettings.class);
		intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		startActivity(intent);
	    }
	});
	super.onCreate(savedInstanceState);
    }
}
