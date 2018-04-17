/**
 * Created by diako on 8/27/2014.
 */
package com.glassgaze.EXPERIMENT;


import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.speech.RecognizerIntent;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.Toast;

import com.glassgaze.Constants;
import com.glassgaze.GazeDisplay.Calibration;
import com.glassgaze.GazeDisplay.PointerView_display;
import com.glassgaze.GazeLiveView.Calibration_offset;
import com.glassgaze.MessageType;
import com.glassgaze.MyJsonClass;
import com.glassgaze.R;
import com.glassgaze.Utils;
import com.glassgaze.WifiService;
import com.google.android.glass.media.Sounds;
import com.google.android.glass.touchpad.Gesture;
import com.google.android.glass.touchpad.GestureDetector;
import com.google.android.glass.view.WindowUtils;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.List;

public class Activity_CalibEX extends Activity{


    MyJsonClass simpleJsonObject=new MyJsonClass();
    int questions=1;

    private GestureDetector mGestureDetector = null;

    static final int RGT = 1;
    static final int HMGT = 0;

    static final int CALIB_NONE=0;
    static final int CALIB_Display = 1;
    static final int CALIB_Scene = 2;
    static final int SAMPLING_Scene = 3;
    private static final int VOICE_RECOGNITION_REQUEST_CODE = 4;

    int running_Test=CALIB_NONE;

    PointerView_display mPointerViewDisplay;

    FrameLayout preview;
    private boolean mVoiceMenuEnabled = true;
    /**
     * Messenger used for receiving responses from service.
     * Activity target published for clients to send messages to IncomingHandler.
     */
    final Messenger mMessenger = new Messenger(new IncomingHandler());

    //.......................WIFI SERVICE


    /**
     * Messenger used for communicating with service.
     */
    Messenger mService = null;
    private WifiService mWifiService;
    private boolean mBounded;



    private ServiceConnection mConnection = new ServiceConnection() {

        public void onServiceConnected(ComponentName className,IBinder binder) {

            Log.d("MessengerActivity", "Connected to service. Registering our Messenger in the Service...");

            WifiService.MyBinder b = (WifiService.MyBinder) binder;
            mWifiService = b.getService();
            mService =  mWifiService.mMessenger;
            //mService = new Messenger(binder);
            mBounded = true;

            // Register our messenger also on Service side:
            Message msg = Message.obtain(null, WifiService.MESSAGE_TYPE_REGISTER);
            msg.replyTo = mMessenger;
            try {
                mService.send(msg);
            } catch (RemoteException e) {
                // We always have to trap RemoteException (DeadObjectException
                // is thrown if the target Handler no longer exists)
                e.printStackTrace();
            }
            init();


        }
        public void onServiceDisconnected(ComponentName className) {
            Toast.makeText(Activity_CalibEX.this, "Service is disconnected", Toast.LENGTH_SHORT).show();
            mBounded = false;
            mWifiService = null;
        }




    };

private void init()
{
    mWifiService.GazeStream(RGT,true);
    mPointerViewDisplay = (PointerView_display) findViewById(R.id.pointerView_display);
  //  mPointerViewDisplay.setBackgroundColor(mWifiService.backgroundColor);




}
    //......................WIFI SERVICE


// protected abstract void setAdapter(CardScrollView view);




/**
 * Activity Handler of incoming messages
 */
class IncomingHandler extends Handler {
    @Override
    public void handleMessage(Message msg) {


        switch (msg.what) {

            case WifiService.MESSAGE_TYPE_TEXT: {
                Bundle b = msg.getData();
                CharSequence text = null;
                if (b != null) {
                    text = b.getCharSequence("data");
                } else {
                    text = "Service responded with empty message";
                }
                Log.d("MessengerActivity", "Response: " + text);
            }
            break;


            case MessageType.MESSAGE_STATE_CHANGE:
                switch (msg.arg1) {

                    case WifiService.STATE_CONNECTED:

                        //mTitle.setText(R.string.title_connected);

                        break;
                    case WifiService.STATE_DISCONNECTED:


                        break;
                    case WifiService.STATE_CONNECTING:


                        // mTitle.setText(R.string.title_connecting);
                        break;
                }
                break;


            case MessageType.MESSAGE_TOAST:
                Toast.makeText(getApplicationContext(), msg.getData().getString(Constants.TOAST),Toast.LENGTH_SHORT).show();
                break;

            case MessageType.MESSAGE_READ:

                byte[] readBuf = (byte[]) msg.obj;

                //debug:
                Log.i("Activity_CalibEX.... ", String.valueOf(Utils.GetIndicator(readBuf)) );



                switch (Utils.GetIndicator(readBuf)) {

                    case MessageType.toGLASS_DataReceived:
                    {
                        mVoiceMenuEnabled = false;
                        getWindow().invalidatePanelMenu(WindowUtils.FEATURE_VOICE_COMMANDS);



                        mWifiService.Speek("Follow the white circle!");
                        mWifiService.write(MessageType.toHAYTHAM_Experiment_display_Start);
                        running_Test=CALIB_Display;

                        mPointerViewDisplay.hideFingerTrace(5,true);


                    }

                    case MessageType.toGLASS_LetsCorrectOffset:

                        mWifiService.GazeStream(RGT, false);
                        mWifiService.write(MessageType.toHAYTHAM_Calibrate_Display_Correct);


                        break;
                    case MessageType.toGLASS_Experiment_Display:

                        AudioManager am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);


                        int x0 = Utils.GetX(readBuf);
                        int y0 = Utils.GetY(readBuf);


                        if ((x0==-2 && y0==-2)||(x0==-3 && y0==-4))
                        {
                            // Toast.makeText(getApplicationContext(), "Calibration finished!", Toast.LENGTH_SHORT).show(); //"M1



                            mPointerViewDisplay.hideFingerTrace(1,true);

                            running_Test=SAMPLING_Scene;
                            // showPointer=true;

                            am.playSoundEffect(Sounds.SUCCESS);

                            //Starting the second test (gaze in the scene)

                            Intent i = new Intent(Activity_CalibEX.this, Sampling_Scene.class);
                            startActivityForResult(i, SAMPLING_Scene);




                        }
                        else
                        {

                            am.playSoundEffect(Sounds.SELECTED);

                            mPointerViewDisplay.GazeEvent(x0, y0, 1);
                            mWifiService.write(MessageType.toHAYTHAM_READY);
                        }
                        break;
                    case MessageType.toGLASS_Calibrate_Display:


                         x0 = Utils.GetX(readBuf);
                         y0 = Utils.GetY(readBuf);

                        if ((x0==-1 && y0==-1)||(x0==-3 && y0==-3) )//calibrate or correct offset
                        {


                            Intent i = new Intent(Activity_CalibEX.this, Calibration.class);
                            startActivityForResult(i,CALIB_Display);


                        }

                        break;
                    case MessageType.toGLASS_Calibrate_Scene:


                        int x1 = Utils.GetX(readBuf);
                        int y1 = Utils.GetY(readBuf);

                        if (x1 == -1 && y1 == -1)
                        {

                            Intent i = new Intent(Activity_CalibEX.this, com.glassgaze.GazeLiveView.Calibration.class);
                            startActivityForResult(i, CALIB_Scene);


                        }

break;

                    case MessageType.toGLASS_test:
                        Toast.makeText(getApplicationContext(), "Test Msg from Haytham", Toast.LENGTH_SHORT).show();  //  C1S

                        break;
                    case MessageType.toGLASS_ERROR_NOTCalibrated:
                        mWifiService.Speek("Calibrate first!");
                        break;


                    case MessageType.toGLASS_GAZE_RGT:

                        int x = Utils.GetX(readBuf);
                        int y = Utils.GetY(readBuf);


                                mPointerViewDisplay.GazeEvent(x, y, 5);
                                mPointerViewDisplay.postInvalidate();

                        break;

                    default:
                        super.handleMessage(msg);
                }

                break;
        }
    }
}


    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        AudioManager audio = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
        if (requestCode == VOICE_RECOGNITION_REQUEST_CODE)
        {
            //If Voice recognition is successful then it returns RESULT_OK
            if(resultCode == RESULT_OK) {

                ArrayList<String> textMatchList = data
                        .getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);

                if (!textMatchList.isEmpty()) {
                    // If first Match contains the 'search' word
                    // Then start web search.
                    if (textMatchList.get(0).contains("search")) {

/*                        String searchQuery = textMatchList.get(0);
                        searchQuery = searchQuery.replace("search","");
                        Intent search = new Intent(Intent.ACTION_WEB_SEARCH);
                        search.putExtra(SearchManager.QUERY, searchQuery);
                        startActivity(search);*/
                    } else {
                        SetTXT(textMatchList.get(0));
                    }

                }else{ SetTXT("No Text!");}


                //Result code for various error.
            }else{
                if(resultCode == RecognizerIntent.RESULT_AUDIO_ERROR){
                    showToastMessage("Audio Error");
                }else if(resultCode == RecognizerIntent.RESULT_CLIENT_ERROR){
                    showToastMessage("Client Error");
                }else if(resultCode == RecognizerIntent.RESULT_NETWORK_ERROR){
                    showToastMessage("Network Error");
                }else if(resultCode == RecognizerIntent.RESULT_NO_MATCH){
                    showToastMessage("No Match");
                }else if(resultCode == RecognizerIntent.RESULT_SERVER_ERROR){
                    showToastMessage("Server Error");
                }
                SetTXT("No Text!");
            }
        }
        else if (requestCode == CALIB_Scene) {

            if(resultCode == RESULT_OK){
                //String result=data.getStringExtra("result");

            }
            if (resultCode == RESULT_CANCELED) {
                //Write your code if there's no result

            }
            new Handler().postDelayed(new Runnable()
            {
                public void run()
                {

                    mWifiService.GazeStream(RGT, true);
                }
            }, 2000);
        }
        else if (requestCode == CALIB_Display) {

            if(resultCode == RESULT_OK){

                //String result=data.getStringExtra("result");

            }
            if (resultCode == RESULT_CANCELED) {
                //Write your code if there's no result

            }
            new Handler().postDelayed(new Runnable()
            {
                public void run()
                {

                    mWifiService.GazeStream(RGT, true);
                }
            }, 2000);
        }
       else if (requestCode == SAMPLING_Scene) {
            if(resultCode == RESULT_OK){
                //String result=data.getStringExtra("result");
                AudioManager am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

                am.playSoundEffect(Sounds.SUCCESS);

                finish();

            }
            if (resultCode == RESULT_CANCELED) {
                //Write your code if there's no result
            }

        }
    }//onActivityResult






    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);

        // Requests a voice menu on this activity. As for any other window feature,
        // be sure to request this before setContentView() is called
        getWindow().requestFeature(WindowUtils.FEATURE_VOICE_COMMANDS);

        // Ensure screen stays on during demo.
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        mGestureDetector = createGestureDetector(this);

        setContentView(R.layout.display_blank);




        //***********VoiceRecognition
        checkVoiceRecognition();
        simpleJsonObject.name="";


        mWifiService.Speek("Thank you for participating in our experiment! What is your name?");

        new Handler().postDelayed(new Runnable()
        {
            public void run()
            {
                speak();

            }
        }, 4000);



        //********************************

    }

    //...................................................
    @Override
    protected void onDestroy(){
        mWifiService. GazeStream(RGT, false);
        if(mBounded) {
            unbindService(mConnection);
            mBounded = false;
        }


        super.onDestroy();
    }

    @Override
    protected void onStart() {
        Intent intent= new Intent(this, WifiService.class);
        bindService(intent, mConnection,Context.BIND_AUTO_CREATE);

        super.onStart();



    }

    @Override
    protected void onResume() {

        Intent intent= new Intent(this, WifiService.class);
        bindService(intent, mConnection,Context.BIND_AUTO_CREATE);

        super.onResume();
    }

    @Override
    protected void onStop() {
        if(mBounded) {
            unbindService(mConnection);
            mBounded = false;
        }
        super.onStop();

    }
    @Override
    protected void onPause() {

        if(mBounded) {
            unbindService(mConnection);
            mBounded = false;
        }
        super.onPause();
    }
    //..................................VoiceMenu

    @Override
    public boolean onCreatePanelMenu(int featureId, Menu menu) {
        if (featureId == WindowUtils.FEATURE_VOICE_COMMANDS) {
            getMenuInflater().inflate(R.menu.voice_menu_experiment_calib, menu);
            return true;
        }
        // Good practice to pass through, for options menu.
        return super.onCreatePanelMenu(featureId, menu);
    }

    @Override
    public boolean onPreparePanel(int featureId, View view, Menu menu) {
        if (featureId == WindowUtils.FEATURE_VOICE_COMMANDS) {
            // Dynamically decides between enabling/disabling voice menu.
            return mVoiceMenuEnabled;
        }
        // Good practice to pass through, for options menu.
        return super.onPreparePanel(featureId, view, menu);
    }

    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        if (featureId == WindowUtils.FEATURE_VOICE_COMMANDS) {
            switch (item.getItemId()) {
                case R.id.menu_exp_calibration_display: {
                    mWifiService.GazeStream(RGT, false);
                    mWifiService.Speek("Wait!");
                    mWifiService.write(MessageType.toHAYTHAM_Calibrate_Display);
                } break;
                case R.id.menu_exp_calibration_scene: {
                    mWifiService.GazeStream(RGT, false);
                    mWifiService.Speek("Wait!");
                    mWifiService.write(MessageType.toHAYTHAM_Calibrate_Scene);
                } break;
                case R.id.menu_calibration_reuse:  {
                    mWifiService.GazeStream(RGT, false);
                    mWifiService.Speek("Wait!");
                    mWifiService.write(MessageType.toHAYTHAM_Calibrate_ReUse);

                } break;
                case R.id.menu_exp_start:  {
                    mWifiService.GazeStream(RGT, false);




                    //I couldn't find a better place to send the Json!
                    Gson gson = new Gson();
                    String jsonObj = gson.toJson(simpleJsonObject);
                    mWifiService.sendJson(jsonObj, MessageType.toHAYTHAM_JsonComming);


                    //Send the start msg to Haytham after MessageType.toGLASS_DataReceived




                } break;


                default: return true;  // No change.
            }




            return true;
        }
        return super.onMenuItemSelected(featureId, item);
    }

    private GestureDetector createGestureDetector(Context context) {
        GestureDetector gestureDetector = new GestureDetector(context);
        gestureDetector.setBaseListener( new GestureDetector.BaseListener() {
            @Override
            public boolean onGesture(Gesture gesture) {

             if (gesture==Gesture.TAP)
             {



             }

             else  if (gesture == Gesture.TWO_TAP) {



                   // Plays sound.
                   AudioManager am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
                   am.playSoundEffect(Sounds.TAP);
                   // Toggles voice menu. Invalidates menu to flag change.
                   mVoiceMenuEnabled = !mVoiceMenuEnabled;
                   getWindow().invalidatePanelMenu(WindowUtils.FEATURE_VOICE_COMMANDS);

                   return true;
               }
                return false;
            }
        });
        return gestureDetector;
    }


    @Override
    public boolean onGenericMotionEvent(MotionEvent event)
    {
        if (mGestureDetector != null)
        {
            return mGestureDetector.onMotionEvent(event);
        }

        return false;
    }


    public void speak() {

        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);

        // Specify the calling package to identify your application
        intent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, getClass()
                .getPackage().getName());


        // Given an hint to the recognizer about what the user is going to say
        //There are two form of language model available
        //1.LANGUAGE_MODEL_WEB_SEARCH : For short phrases
        //2.LANGUAGE_MODEL_FREE_FORM  : If not sure about the words or phrases and its domain.
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_WEB_SEARCH);


        //Start the Voice recognizer activity for the result.
        startActivityForResult(intent, VOICE_RECOGNITION_REQUEST_CODE);



    }
    public void checkVoiceRecognition() {
        // Check if voice recognition is present
        PackageManager pm = getPackageManager();
        List<ResolveInfo> activities = pm.queryIntentActivities(new Intent(
                RecognizerIntent.ACTION_RECOGNIZE_SPEECH), 0);
        if (activities.size() == 0) {

            Toast.makeText(this, "Voice recognizer not present",
                    Toast.LENGTH_SHORT).show();
        }
    }


    private void SetTXT(String txt){


        //******************


        if(questions==1)//name
        {
            simpleJsonObject.name=String.valueOf(txt);
            simpleJsonObject.texts.add("Name:" + String.valueOf(txt));

            //next question
            questions=questions+1;
            mWifiService.Speek("Cool! How old are you " + String.valueOf(txt));
            new Handler().postDelayed(new Runnable()
            {
                public void run()
                {
                    speak();

                }
            }, 2000);

        }
       else if(questions==2)//age
        {
            questions=questions+1;
            simpleJsonObject.texts.add("Age:" + String.valueOf(txt));

        }




    }
    /**
     * Helper method to show the toast message
     **/
    void showToastMessage(String message){
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }


}
