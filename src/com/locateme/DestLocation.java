/*******************************************************
 ***** LocateMe - Indoor Localization & navigation *****
 * Made By: Saurabh Agrawal, Karthik NG, Shival Thaker *
 *******************************************************
 ** This class is made by the developers. 2 libraries **
 ** have been used for the app. Tesseract library for **
  OCR engine and Tagin library for Wi-Fi Fingerprinting 
 *******************************************************
*/

package com.locateme;

import java.util.List;
import ca.idrc.tagin.lib.TaginManager;
import ca.idrc.tagin.lib.TaginService;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import ca.idrc.tagin.lib.TaginManager;
import ca.idrc.tagin.lib.TaginService;
import ca.idrc.tagin.lib.TaginUtils;
import ca.idrc.tagin.lib.tags.GetLabelsTask;
import ca.idrc.tagin.lib.tags.GetLabelsTaskListener;
import ca.idrc.tagin.lib.tags.SetLabelTask;
import ca.idrc.tagin.lib.tags.SetLabelTaskListener;

import com.google.api.services.tagin.model.Fingerprint;
import com.google.api.services.tagin.model.FingerprintCollection;

/*
* This activity implements the second part of the application.
* After getting the source location, the destination location can be set here and Wi-Fi fingerprint can be requested.
* The fingerprint is a unique hash which can be associated with room numbers, which in turn can help localizing and navigating.
*/
public class DestLocation extends Activity implements GetLabelsTaskListener, SetLabelTaskListener {

	private Context mContext;
	String source;
	String URN;
	AutoCompleteTextView autocompletetextview;
	private TaginManager mTaginManager;
	TextView mURNTextView;
	TextView mLabelView;
	EditText mLabelText;
	Button mURNRequestButton;
	Button update;
	
	// Sample array holding the room numbers
	String[] arr = { "100", "101", "102", "103", "104", "105", "106", "107", "108", "109", "110",
			 		 "200", "201", "300"  };


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_dest_location);
		mContext = this;
		
		// Set the objects
		mTaginManager = new TaginManager(this);
		mURNTextView = (TextView) findViewById(R.id.urn);
		mLabelView = (TextView) findViewById(R.id.label);
		mLabelText = (EditText) findViewById(R.id.setLabel);
		mURNRequestButton = (Button) findViewById(R.id.requestURN);
		update = (Button) findViewById(R.id.update);
		
		registerReceiver(mReceiver, new IntentFilter(TaginService.ACTION_URN_READY));
//		registerReceiver(mReceiver, new IntentFilter(TaginService.ACTION_NEIGHBOURS_READY));
//		registerReceiver(mReceiver, new IntentFilter(TaginService.ACTION_FINGERPRINTS_READY));

		source = getIntent().getExtras().getString("sourceLoc");
		System.out.println(source);

		// Implement the auto-complete textview, which will show filtered results as the user types the destination location.
		autocompletetextview = (AutoCompleteTextView)             
				findViewById(R.id.autoCompleteTextView1);

		ArrayAdapter adapter = new ArrayAdapter(this,android.R.layout.select_dialog_item, arr);

		autocompletetextview.setThreshold(1);
		autocompletetextview.setInputType(DEFAULT_KEYS_DIALER);
		autocompletetextview.setAdapter(adapter);	

	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		return super.onOptionsItemSelected(item);
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		unregisterReceiver(mReceiver);
	}

	// This method is called when the 'Set' button is pressed in the app
	public void setDest(View view){
		
		String dest = autocompletetextview.getText().toString();
		System.out.println(dest);
		
		int destLoc = Integer.parseInt(dest);

		// Sets the source & dest location and displays on screen
		TextView s = (TextView) findViewById(R.id.sourset);
		TextView d = (TextView) findViewById(R.id.destset);
		s.setText("Source set to: " + source);
		d.setText("Destination set to: " + destLoc);
		d.setVisibility(View.VISIBLE);

		// makes the other part of the screen visible
		mURNRequestButton.setVisibility(View.VISIBLE);
		mURNTextView.setVisibility(View.VISIBLE);
		mLabelView.setVisibility(View.VISIBLE);
		mLabelText.setVisibility(View.VISIBLE);
		update.setVisibility(View.VISIBLE);
	}
	
	// This method is called when 'Request Wi-Fi Fingerprint' is pressed
	public void onRequestURN(View view) {
		mURNTextView.setText("URN");
		mLabelView.setText("Label");
		
		// Calls the api request methofd
		//mURNRequestButton.setText(R.string.requesting_urn);
		mTaginManager.apiRequest(TaginService.REQUEST_URN);
	}
	
	// Receives the response from the api call
	private BroadcastReceiver mReceiver = new BroadcastReceiver() {
		
		@Override
		public void onReceive(Context context, Intent intent) {
			if (intent.getAction().equals(TaginService.ACTION_URN_READY)) {
				String urn = intent.getStringExtra(TaginService.EXTRA_QUERY_RESULT);
				handleURNResponse(urn);
			}
		}
	};
	
	// Handels the response which we get from the api
	private void handleURNResponse(String urn) {
		if (urn != null) {
			URN = urn;
			mURNTextView.setText(urn);
			GetLabelsTask<DestLocation> task = new GetLabelsTask<DestLocation>(this, urn);
			task.execute();
		} else {
			mURNTextView.setText("Please try again!");
		}

	}

	// Gets the TAG for a specifed URN (hash)
	@Override
	public void onGetLabelsTaskComplete(String urn, List<String> labels) {
		mLabelView.setText(labels.toString());
//		mGetLabelButton.setText(R.string.get_label);
	}

	// called when we want to save a tag
	public void onSetLabel(View view) {
//		mSetLabelButton.setText(R.string.saving_tag);
//		String urn = mURNText2.getText().toString();
		String label = mLabelText.getText().toString();
		SetLabelTask<DestLocation> task = new SetLabelTask<DestLocation>(this, URN, label);
		task.execute();
	}

	// Called when TAG setting is done
	@Override
	public void onSetLabelTaskComplete(Boolean isSuccessful) {
//		mURNText2.setText("");
//		mLabelText.setText("");
//		mSetLabelButton.setText(R.string.set_label);
		if (isSuccessful) {
			Toast.makeText(mContext, "TAG saved", Toast.LENGTH_SHORT).show();
		} else {
			Toast.makeText(mContext, "Unable to save TAG", Toast.LENGTH_SHORT).show();
		}
	}
}
