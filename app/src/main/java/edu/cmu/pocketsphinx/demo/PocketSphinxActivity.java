package edu.cmu.pocketsphinx.demo;

import android.Manifest;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.telephony.SmsManager;
import android.util.Log;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.GoogleMap;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.text.DateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import edu.cmu.pocketsphinx.Assets;
import edu.cmu.pocketsphinx.Hypothesis;
import edu.cmu.pocketsphinx.RecognitionListener;
import edu.cmu.pocketsphinx.SpeechRecognizer;
import edu.cmu.pocketsphinx.SpeechRecognizerSetup;
import es.dmoral.toasty.Toasty;

public class PocketSphinxActivity extends AppCompatActivity implements
        RecognitionListener,
        com.google.android.gms.location.LocationListener,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {

    private static final String TAG = "PocketSphinxActivity";

    //CMU SPHINX
    private static final String KWS_SEARCH = "wakeup";
    private static final String MENU_SEARCH = "menu";

    //KEYPHRASES TO TRIGGER THE APPLICATION
    private static final String KEYPHRASE_1 = "";
    private static final String KEYPHRASE_HELP = "start";

    // USED TO HANDLE PERMISSION REQUEST
    private static final int PERMISSIONS_REQUEST_RECORD_AUDIO = 1;
    private SpeechRecognizer recognizer;
    private HashMap<String, Integer> captions;
    private TextView caption_text, result_text;

    //CALL
    private static final int REQUEST_CONTACT_CODE = 1;
    private ImageButton imgbCallButton;

    //MAP
    private static final int ERROR_DIALOG_REQUEST = 9001;
    private Boolean mLocationPermissionGranted = false;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1234;
    private static final float DEFAULT_ZOOM = 15f;
    private FusedLocationProviderClient mFusedLocationProviderClient;
    private LocationManager locationManager;
    private LocationListener locationListener;
    private GoogleMap mGoogleMap;
    private TextView tvGPSCoordinates;

    //MESSAGE
    private EditText etMessage;
    private EditText etMessageLocation;
    private ImageButton sendBtn;
    private EditText smsMessage;
    private TextView smsStatus;
    private ImageButton btnMain;

    public static final String SMS_SENT_ACTION = "com.andriodgifts.gift.SMS_SENT_ACTION";
    public static final String SMS_DELIVERED_ACTION = "com.andriodgifts.gift.SMS_DELIVERED_ACTION";

    //CONTACTS
    private static final int PICK_CONTACT = 1000;
    TextView tvName1, tvNumber1, tvName2, tvNumber2, tvName3, tvNumber3;
    ImageButton imgbPickContact_One;
    ImageButton imgbPickContact_Two;
    ImageButton imgbPickContact_Three;

    private static final int RESULT_PICK_CONTACT = 1;
    private static int PICK_COUNTER = 0;

    //UPDATE GPS
    private static final long INTERVAL = 5000;
    private static final long FASTEST_INTERVAL = 5000;
    Button btnFusedLocation;
    TextView tvLocation;
    LocationRequest mLocationRequest;
    GoogleApiClient mGoogleApiClient;
    Location mCurrentLocation;
    String mLastUpdateTime;

    //SAVED
    private ImageButton imgbSave;

    //OBJECTIVES
    private ImageButton imb_objective;

    //ABOUT
    private ImageButton imb_about;

    //SHARED PREFERENCES
    SharedPreferences sharedPreferences;

    protected void createLocationRequest() {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(INTERVAL);
        mLocationRequest.setFastestInterval(FASTEST_INTERVAL);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.main);

        //Prepare the data for UI
        captions = new HashMap<>();
        captions.put(KWS_SEARCH, R.string.kws_caption);
        captions.put(MENU_SEARCH, R.string.menu_caption);

        //CMU SPHINX
        caption_text = (TextView) findViewById(R.id.caption_text);
        result_text = (TextView) findViewById(R.id.result_text);

        tvGPSCoordinates = (TextView) findViewById(R.id.tvGPSCoordinates);

        etMessage = (EditText) findViewById(R.id.etMessage);
        smsMessage = (EditText) findViewById(R.id.etMessage);
        smsStatus = (TextView) findViewById(R.id.message_status);
        etMessageLocation = (EditText) findViewById(R.id.etMessageLocation);

        tvName1 = (TextView) findViewById(R.id.contact_name_one);
        tvNumber1 = (TextView) findViewById(R.id.contact_number_one);
        tvName2 = (TextView) findViewById(R.id.contact_name_two);
        tvNumber2 = (TextView) findViewById(R.id.contact_number_two);
        tvName3 = (TextView) findViewById(R.id.contact_name_three);
        tvNumber3 = (TextView) findViewById(R.id.contact_number_three);

        sendBtn = (ImageButton) findViewById(R.id.imbSend);
        btnMain = (ImageButton) findViewById(R.id.main_button);

        imgbPickContact_One = (ImageButton) findViewById(R.id.imPickAContact1);
        imgbPickContact_Two = (ImageButton) findViewById(R.id.imPickAContact2);
        imgbPickContact_Three = (ImageButton) findViewById(R.id.imPickAContact3);

        imgbCallButton = (ImageButton) findViewById(R.id.callButton);

        caption_text.setText("Preparing the recognizer");

        Toasty.normal(this, "Preparing the recognizer", Toast.LENGTH_SHORT,
                ContextCompat.getDrawable(this, R.drawable.ic_if_spinner_126578)).show();

        //CALLS THE updateUI() method //UPDATES THE GPS AND MAP
        updateUI();

        /*
         * MyService Class, dito kung saan mag ru-run on background
         * yung application natin para sa location and speech recognition
         */
        startService(new Intent(this, MyService.class)); //SERVICE STARTS WHEN THE APPLICATION IS OPEN

        //WELCOME MESSAGE
        Toasty.success(this, "Welcome!", Toast.LENGTH_SHORT, false).show();

        //SEND MESSAGE
        sendBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //CHECKS IF USER HAS SELECTED A CONTACT THEN SEND IF PHONE NUMBER !=null
                checkPhoneNumber();
            }
        });

        //CALL BUTTON
        imgbCallButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                callContactNumber();
            }
        });

        //show error dialog if GoolglePlayServices not available
        if (!isGooglePlayServicesAvailable()) {
            finish();
        }
        createLocationRequest();
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

        //NAKA PRIVATE YUNG MODE NG SHARED PREFERENCE PARA DI SIYA O YUNG SAVED DATA
        //MA-ACCESS NG IBANG APPLICATION (SHARED PREFERENCES PERO AYAW MAG SHARE)
        sharedPreferences = getPreferences(MODE_PRIVATE);

        //LOADS THE SHARED PREFS
        loadPrefs();

        //CHECKS IF USER HAS GIVEN THE PERMISSION TO USE AUDIO
        int permissionCheck = ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.RECORD_AUDIO);
        if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, PERMISSIONS_REQUEST_RECORD_AUDIO);
            return;
        }
        // Recognizer initialization is a time-consuming and it involves IO,
        // so we execute it in async task
        new SetupTask(this).execute();

    }

    // ============================================================================================================================================
    // =================== THIS IS WHERE THE LOGIC STARTS # THIS IS WHERE THE LOGIC STARTS # THIS IS WHERE THE LOGIC STARTS =======================
    // ============================================================================================================================================

    /******************************************************** GPS ON BACKGROUND *******************************************************/
    @Override
    public void onStart() {
        super.onStart();
        Log.d(TAG, "onStart fired ..............");
        mGoogleApiClient.connect();
    }

    @Override
    public void onStop() {
        super.onStop();
        Log.d(TAG, "onStop fired ..............");
        mGoogleApiClient.disconnect();
        Log.d(TAG, "isConnected ...............: " + mGoogleApiClient.isConnected());
    }

    private boolean isGooglePlayServicesAvailable() {
        int status = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        if (ConnectionResult.SUCCESS == status) {
            return true;
        } else {
            GooglePlayServicesUtil.getErrorDialog(status, this, 0).show();
            return false;
        }
    }

    @Override
    public void onConnected(Bundle bundle) {
        Log.d(TAG, "onConnected - isConnected ...............: " + mGoogleApiClient.isConnected());
        startLocationUpdates();
    }

    protected void startLocationUpdates() {

        if (ActivityCompat.checkSelfPermission(this,
                android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this,
                android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            return;
        }
        PendingResult<Status> pendingResult = LocationServices.FusedLocationApi.requestLocationUpdates(
                mGoogleApiClient, mLocationRequest, this);
        Log.d(TAG, "Location update started ...: ");
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.d(TAG, "Connection failed: " + connectionResult.toString());
    }

    @Override
    public void onLocationChanged(Location location) {
        Log.d(TAG, "Firing onLocationChanged...");
        mCurrentLocation = location;
        //KINUKUHA LANG YUNG TAMANG ORAS SA CP'S SYSTEM
//        mLastUpdateTime = DateFormat.getTimeInstance().format(new Date());
        //CALLS THE METHOD updateUI()
        updateUI();
    }

    //GPS, MESSAGE UPDATES

    private void updateUI() {
        Log.d(TAG, "UI update initiated .............");
        if (null != mCurrentLocation) {
            String lat = String.valueOf(mCurrentLocation.getLatitude());
            String lng = String.valueOf(mCurrentLocation.getLongitude());
            mLastUpdateTime = DateFormat.getTimeInstance().format(new Date());

//            "Time Happened: " + mLastUpdateTime + "\n" +
//
                    etMessageLocation.setText("Time Happened: " + mLastUpdateTime + "\n" +
                    "Streetview:\nhttps://www.google.com/maps/@?api=1&map_action=pano&viewpoint="
                    + lat + "," + lng + "&heading=-45&pitch=38&fov=80");

//            tvGPSCoordinates.setText("https://www.google.com/maps/@?api=1&map_action=pano&viewpoint="
//                    + lat + "," + lng + "&heading=-45&pitch=38&fov=80");

            tvGPSCoordinates.setText("Coordinates: " + lat + "," + lng);

//            + "https://www.google.com.ph/maps/search/nearest+police+station+to+me"
//            tvLocation.append("At Time: " + mLastUpdateTime + "\n" +
//                    "Latitude: " + lat + "\n" +
//                    "Longitude: " + lng + "\n" +
//                    "Accuracy: " + mCurrentLocation.getAccuracy() + "\n" +
//                    "Provider:" + mCurrentLocation.getProvider());

        } else {
            Log.d(TAG, "location is null.............");
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopLocationUpdates();
    }

    protected void stopLocationUpdates() {
        LocationServices.FusedLocationApi.removeLocationUpdates(
                mGoogleApiClient, this);
        Log.d(TAG, "Location update stopped .......................");
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mGoogleApiClient.isConnected()) {
            startLocationUpdates();
            Log.d(TAG, "Location update resumed..........");
        }
    }

    //ASK USER KUNG PWEDE GAMITIN YUNG AUDIO PARA MAGAMIT YUNG SPEECH RECOGNITION (CMU SPHONX)
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSIONS_REQUEST_RECORD_AUDIO) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Recognizer initialization is a time-consuming and it involves IO,
                // so we execute it in async task
                new SetupTask(this).execute();
            } else {
                finish();
            }
        }
    }

    private static class SetupTask extends AsyncTask<Void, Void, Exception> {
        WeakReference<PocketSphinxActivity> activityReference;

        SetupTask(PocketSphinxActivity activity) {
            this.activityReference = new WeakReference<>(activity);
        }

        @Override
        protected Exception doInBackground(Void... params) {
            try {
                Assets assets = new Assets(activityReference.get());
                File assetDir = assets.syncAssets();
                activityReference.get().setupRecognizer(assetDir);
            } catch (IOException e) {
                return e;
            }
            return null;
        }

        @Override
        protected void onPostExecute(Exception result) {
            if (result != null) {
                ((TextView) activityReference.get().findViewById(R.id.caption_text))
                        .setText("Failed to init recognizer " + result);
            } else {
                activityReference.get().switchSearch(KWS_SEARCH);
            }
        }

    }

    //ETO YUNG ALTERNATE BUTTON IN CASE SI USER DI MAKAPAG SALITA DEPENDE SA SITWASYON
    public void btnTriggerApp(View view) {
        //MEANS TINATAWAG YUNG checkPhoneNumber() method
        //YUNG EXPLANATION NANDUN SA PART NA YAN
        checkPhoneNumber();
        //            callContactNumber(); //ETO NAMAN YUNG PAG CALL SA CONTACT.

    }

    //CHOOSING CONTACTS

    //TO DO WHEN BUTTON IS CLICKED
    public void PickContact(View view) {
        if (view.getId() == R.id.imPickAContact1) {
            Intent i = new Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI);
            startActivityForResult(i, RESULT_PICK_CONTACT);
            PICK_COUNTER = 1;
        } else if (view.getId() == R.id.imPickAContact2) {
            Intent i = new Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI);
            startActivityForResult(i, RESULT_PICK_CONTACT);
            PICK_COUNTER = 2;
        } else if (view.getId() == R.id.imPickAContact3) {
            Intent i = new Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI);
            startActivityForResult(i, RESULT_PICK_CONTACT);
            PICK_COUNTER = 3;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case RESULT_PICK_CONTACT:
                    contactPicked(data);
                    break;
            }
        } else {
            Log.e("MainActivity", "Failed to pick contact");
        }
    }

    private void contactPicked(Intent data) {
        Cursor cursor = null;
        try {
            Uri uri = data.getData();
            cursor = getContentResolver().query(uri, null, null, null, null);
            cursor.moveToFirst();
            int phoneIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);
            int nameIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME);

            String phoneNo = cursor.getString(phoneIndex);
            String name = cursor.getString(nameIndex);

            if (PICK_COUNTER == 1) {
                tvNumber1.setText(phoneNo);
                tvName1.setText(name);

                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putString("keyName", name);
                editor.putString("keyNumber", phoneNo);
                editor.apply();

            } else if (PICK_COUNTER == 2) {
                tvNumber2.setText(phoneNo);
                tvName2.setText(name);

                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putString("keyName2", name);
                editor.putString("keyNumber2", phoneNo);
                editor.apply();

            } else if (PICK_COUNTER == 3) {
                tvNumber3.setText(phoneNo);
                tvName3.setText(name);

                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putString("keyName3", name);
                editor.putString("keyNumber3", phoneNo);
                editor.apply();
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            cursor.close();
        }
    }

    //CALLS THE LOAD SHARED PREFS
    public void loadPrefs() {
        Log.d(TAG, "loadPrefs: contact save");
        String savedName = sharedPreferences.getString("keyName", "");
        String saveNumber = sharedPreferences.getString("keyNumber", "");
        String savedName2 = sharedPreferences.getString("keyName2", "");
        String savedNumber2 = sharedPreferences.getString("keyNumber2", "");
        String savedName3 = sharedPreferences.getString("keyName3", "");
        String savedNumber3 = sharedPreferences.getString("keyNumber3", "");

        tvName1.setText(savedName);
        tvNumber1.setText(saveNumber);

        tvName2.setText(savedName2);
        tvNumber2.setText(savedNumber2);

        tvName3.setText(savedName3);
        tvNumber3.setText(savedNumber3);

        String savedMessage = sharedPreferences.getString("keyMessage", "");
        etMessage.setText(savedMessage);
    }

    public void onSaveMessage() {
        String message = etMessage.getText().toString();

        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("keyMessage", message);
        editor.apply();

        Toasty.success(this, "Saved!", Toast.LENGTH_SHORT, true).show();
    }

    /****************************************************** SENDING MESSAGE *****************************************************/

    //CHECKS IF THE CONTACT NUMBER IS NULL
    public void checkPhoneNumber() {
        String phoneNum = tvNumber1.getText().toString();
        String phoneNum2 = tvNumber2.getText().toString();
        String phoneNum3 = tvNumber3.getText().toString();
        String smsBody = smsMessage.getText().toString();
        String smsBody2 = etMessageLocation.getText().toString();

        //Check if the phoneNumber is empty
        if (phoneNum.isEmpty()) {
            Toasty.info(this, "Please Enter a Phone Number!", Toast.LENGTH_SHORT, true).show();
        } else {
            //If Phone Number is not equal to null send message
            sendSMS(phoneNum, smsBody); //First recipient
            sendSMS(phoneNum, smsBody2);
            sendSMS(phoneNum2, smsBody); //Second recipient
            sendSMS(phoneNum2, smsBody2);
            sendSMS(phoneNum3, smsBody); //Third recipient
            sendSMS(phoneNum3, smsBody2);
        }

        Toasty.custom(this, "Sending...", getResources().getDrawable(R.drawable.ic_send_white_24dp)
                , Color.BLUE, Toast.LENGTH_SHORT, true, true).show();

        //SENT RECEIVER
        //DITO MALALAMAN YUNG STATUS NG SMS OR NG NETWORK
        registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String message = null;
                switch (getResultCode()) {
                    case Activity.RESULT_OK: //KUNG NASEND YUNG MESSAGE
                        message = "Message Sent Successfully!";
                        break;
                    case SmsManager.RESULT_ERROR_GENERIC_FAILURE: // KUNG WALANG LOAD
                        message = "Error. Seems you don't have load.";
//                        Toast.makeText(getApplicationContext(), "No load detected", Toast.LENGTH_SHORT).show();
                        break;
                    case SmsManager.RESULT_ERROR_NO_SERVICE: //KUNG WALANG SIGNAL
                        message = "Error: No service.";
//                    Toasty.error(getApplicationContext(), "No service detected.", Toast.LENGTH_SHORT, true).show();
                        break;
                    case SmsManager.RESULT_ERROR_NULL_PDU: //KUNG WALANG CONTACT
                        message = "Error: Null PDU.";
//                        Toasty.error(getApplicationContext(), "No recipient detected.", Toast.LENGTH_SHORT, true).show();
                        break;
                    case SmsManager.RESULT_ERROR_RADIO_OFF: //KUNG NAKA AIRPLANE MODE YUNG PHONE
                        message = "Error: Radio off.";
//                        Toasty.error(getApplicationContext(), "Please disable airplane mode.", Toast.LENGTH_SHORT, true).show();
                        break;
                }
                smsStatus.setText(message); //THEN DITO NA LALABAS YUNG MESSAGE
            }
        }, new IntentFilter(SMS_SENT_ACTION));
    }

    //SENDS THE MESSAGE
    public void sendSMS(String contactNumber, String smsMessage) {
        SmsManager sms = SmsManager.getDefault();
        List<String> messages = sms.divideMessage(smsMessage);
        for (String message : messages) {

            /*
            * sendTextMessage (String destinationAddress, String scAddress, String text, PendingIntent sentIntent, PendingIntent deliveryIntent)
            *
            * Sent Intent: fired when the message is sent and indicates if it's successfully sent or not
            *
            * Delivery Intent: fired when the message is sent and delivered
            * */

            sms.sendTextMessage(contactNumber, null, message, PendingIntent.getBroadcast(
                    this, 0, new Intent(SMS_SENT_ACTION), 0),
                    PendingIntent.getBroadcast(this, 0, new Intent(SMS_DELIVERED_ACTION), 0));

        }
    }


    //cmu sphinx speech recognition
    @Override
    public void onDestroy() {
        super.onDestroy();

        if (recognizer != null) {
            recognizer.cancel();
            recognizer.shutdown();
        }
    }

    /**
     * In partial result we get quick updates about current hypothesis. In
     * keyword spotting mode we can react here, in other modes we need to wait
     * for final result in onResult.
     */

    @Override
    public void onPartialResult(Hypothesis hypothesis) {
        if (hypothesis == null)
            return;

        String text = hypothesis.getHypstr();
        String lat = String.valueOf(mCurrentLocation.getLatitude());
        String lng = String.valueOf(mCurrentLocation.getLongitude());
        result_text.setText(text);

        if (text.equals(KEYPHRASE_HELP)) {
            switchSearch(KWS_SEARCH);

            Toasty.normal(this, "You said: " + KEYPHRASE_HELP).show();

            checkPhoneNumber(); //ETO NA YUNG PROCESS NG PAG SEND NG MESSAGE. TINAWAG YUNG METHOD. YUNG NASA TAAS KANINA.

        }
    }

    /**
     * This callback is called when we stop the recognizer.
     */
    @Override
    public void onResult(Hypothesis hypothesis) {
        result_text.setText("");
        if (hypothesis != null) {
            String text = hypothesis.getHypstr();
        }
    }

    @Override
    public void onBeginningOfSpeech() {
        Log.d(TAG, "onBeginningOfSpeech: ............. ");
    }

    /**
     * We stop recognizer here to get a final result
     */
    @Override
    public void onEndOfSpeech() {
        if (!recognizer.getSearchName().equals(KWS_SEARCH))
            switchSearch(KWS_SEARCH);
    }

    private void switchSearch(String searchName) {
        recognizer.stop();

        // If we are not spotting, start listening with timeout (99999 ms or 1 day).
        if (searchName.equals(KWS_SEARCH))
            recognizer.startListening(searchName);
        else
            recognizer.startListening(searchName, 99999);

        String caption = getResources().getString(captions.get(searchName));
        caption_text.setText(caption);
    }

    private void setupRecognizer(File assetsDir) throws IOException {
        // The recognizer can be configured to perform multiple searches
        // of different kind and switch between them

        recognizer = SpeechRecognizerSetup.defaultSetup()
                .setAcousticModel(new File(assetsDir, "en-us-ptm"))
                .setDictionary(new File(assetsDir, "cmudict-en-us.dict"))

                .setRawLogDir(assetsDir) // To disable logging of raw audio comment out this call (takes a lot of space on the device)

                .getRecognizer();
        recognizer.addListener(this);

        /* In your application you might not need to add all those searches.
          They are added here for demonstration. You can leave just one.
         */

        // Create keyword-activation search.
        recognizer.addKeyphraseSearch(KWS_SEARCH, KEYPHRASE_1);
        recognizer.addKeyphraseSearch(KWS_SEARCH, KEYPHRASE_HELP);
//        recognizer.addKeyphraseSearch(KWS_SEARCH, FIRE);

        // Create grammar-based search for selection between demos
        File menuGrammar = new File(assetsDir, "menu.gram");
        recognizer.addGrammarSearch(MENU_SEARCH, menuGrammar);

    }

    @Override
    public void onError(Exception error) {
        caption_text.setText(error.getMessage());
    }

    @Override
    public void onTimeout() {
        switchSearch(KWS_SEARCH);
    }

    //CALL METHOD

    //CALL METHOD
    private void callContactNumber() {
        String number = tvNumber1.getText().toString();

        if (number.trim().length() > 0) {

            if (ContextCompat.checkSelfPermission(PocketSphinxActivity.this,
                    Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(PocketSphinxActivity.this,
                        new String[]{Manifest.permission.CALL_PHONE}, REQUEST_CONTACT_CODE);
            } else {
                String dial = "tel:" + number;
                startActivity(new Intent(Intent.ACTION_CALL, Uri.parse(dial)));
                Toasty.custom(this, "Dialing...", getResources().getDrawable(R.drawable.ic_call_black_24dp),
                        Color.RED, Toast.LENGTH_SHORT, true, true).show();
            }

        } else {
            Toasty.error(getApplicationContext(), "No recipient detected.", Toast.LENGTH_SHORT, true).show();
        }

    }


    //menu
    @Override
    public boolean onCreateOptionsMenu(android.view.Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.set_menu, menu);
        return true;

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.objective_menu:
                Intent intent = new Intent(PocketSphinxActivity.this, ObjectivesActivity.class);
                startActivity(intent);
                return true;

            case R.id.about_menu:
                Intent intent2 = new Intent(PocketSphinxActivity.this, AboutDevActivity.class);
                startActivity(intent2);
                return true;

            case R.id.save_menu:
                //SAVE BUTTON FOR DEFAULT MESSAGE CUSTOMIZATION
                onSaveMessage();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

}
