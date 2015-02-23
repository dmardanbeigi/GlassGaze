/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.glassgaze.GazeLiveView;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.FrameLayout;
import android.widget.Toast;

import com.glassgaze.Constants;
import com.glassgaze.GazeDisplay.*;
import com.glassgaze.GazeLiveView.Demos.SnapShot;
import com.glassgaze.MessageType;
import com.glassgaze.R;
import com.glassgaze.Utils;
import com.glassgaze.WifiService;
import com.glassgaze.card.CardAdapter;
import com.google.android.glass.app.Card;
import com.google.android.glass.media.Sounds;
import com.google.android.glass.touchpad.GestureDetector;
import com.google.android.glass.view.WindowUtils;
import com.google.android.glass.widget.CardScrollView;

import java.util.ArrayList;
import java.util.List;

/**
 * An activity that demonstrates the voice menu API.
 */
public final class LiveViewActivity extends Activity {

    private static final String TAG = DisplayActivity.class.getSimpleName();

    private int mState;
    // Constants that indicate the current state (what card is selected?!)
    //******numbers MUST indicate the index in the card menu *******
    //


    public static final int SNAPSHOT = 1; //gazeshow card
    public static final int MOREDEMOS = 2;


    private int mState_SNAPSHOT;
    // Constants that indicate the current state within each card
    public static final int STATE_SNAPSHOT_SOMETHING = 0;

    private int mState_APP1;
    // Constants that indicate the current state within each card
    public static final int STATE_APP1_SOMETHING = 0;


    private GestureDetector mGestureDetector = null;


    private CardScrollView mCardScroller;

    private boolean mVoiceMenuEnabled = true;
    private boolean mVoiceMenuEnabled_beforeCalib = mVoiceMenuEnabled;


    private CameraView cameraView = null;

    public boolean waitForYesNo = false;
    private CountDownTimer cdTimer;
    private long total = 4000;
    private long tempTotal = 5000;


    private ProgressDialog progressDialog;
    int currentPhoto = 0;
    int totalPhotosNeeded;

    PointerView_liveView mPointerView;

    FrameLayout preview;
    private Boolean showPointer = true;

    private Boolean gazeStream_HMGT = false;


    static final int HMGT = 0;

    protected static ProgressData progressData = new ProgressData();
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
            Toast.makeText(LiveViewActivity .this, "Service is disconnected", Toast.LENGTH_SHORT).show();
            mBounded = false;
            mWifiService = null;
        }




    };
    private void init()
    {
        mWifiService. GazeStream(HMGT,false);
    }

    //......................WIFI SERVICE

    /**
     * Activity Handler of incoming messages from service.
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
                    Toast.makeText(getApplicationContext(), msg.getData().getString(Constants.TOAST),
                            Toast.LENGTH_SHORT).show();
                    break;
                case MessageType.MESSAGE_READ:

                    byte[] readBuf = (byte[]) msg.obj;


                    switch (Utils.GetIndicator(readBuf)) {
                        case MessageType.toGLASS_Calibrate_Scene:


                            int x1 = Utils.GetX(readBuf);
                            int y1 = Utils.GetY(readBuf);

                            if (x1 == -1 && y1 == -1)
                            {

                                Intent i = new Intent(LiveViewActivity.this, com.glassgaze.GazeLiveView.Calibration.class);
                                startActivityForResult(i, 1);


                            }
                            else if (x1 == -6 && y1 == -6)
                            {

                                Intent i = new Intent(LiveViewActivity.this, Calibration_offset.class);
                                startActivityForResult(i, 1);


                            }
                            break;
                        case MessageType.toGLASS_test:
                            Toast.makeText(getApplicationContext(), "Test Msg from Haytham", Toast.LENGTH_SHORT).show();  //  C1S
                        case MessageType.toGLASS_GAZE_HMGT:
                            int x = Utils.GetX(readBuf);
                            int y = Utils.GetY(readBuf);


                            break;
                        default:
                            super.handleMessage(msg);
                    }

                    break;
            }
        }
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 1) {
            if(resultCode == RESULT_OK){
                //String result=data.getStringExtra("result");
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



        // Sets up a singleton card scroller as content of this activity. Clicking
        // on the card toggles the voice menu on and off.
        mCardScroller = new CardScrollView(this) { };
        mCardScroller.setAdapter(new CardAdapter(createCards(this)));
        setCardScrollerListener();

        mCardScroller.setHorizontalScrollBarEnabled(true);

        mCardScroller.activate();
        setContentView(mCardScroller);


    }


    @Override
    protected void onDestroy() {
        mWifiService.GazeStream(HMGT, false);


        try {

        } catch (Exception e) {
        }

        if (mBounded) {
            unbindService(mConnection);
            mBounded = false;
        }

        super.onDestroy();
    }

    @Override
    protected void onStart() {
        Intent intent = new Intent(this, WifiService.class);
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);

        super.onStart();

    }

    @Override
    protected void onResume() {
        mCardScroller.activate();
        Intent intent = new Intent(this, WifiService.class);
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);

        super.onResume();
    }

    @Override
    protected void onStop() {
        if (mBounded) {
            unbindService(mConnection);
            mBounded = false;
        }
        super.onStop();

    }

    @Override
    protected void onPause() {

        if (mBounded) {
            unbindService(mConnection);
            mBounded = false;
        }

        super.onPause();
    }

    @Override
    public boolean onCreatePanelMenu(int featureId, Menu menu) {

        if (featureId == WindowUtils.FEATURE_VOICE_COMMANDS) {
            getMenuInflater().inflate(R.menu.voice_menu_scene_main, menu);
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

                case R.id.menu_liveview_calibration_calibrate: {
                    mWifiService.GazeStream(HMGT, false);
                    mWifiService.Speek("Wait!");
                    mWifiService.write(MessageType.toHAYTHAM_Calibrate_Scene_4);
                } break;
                case R.id.menu_liveview_calibration_correctOffset:  {
                    mWifiService.GazeStream(HMGT, false);
                    mWifiService.Speek("Wait!");
                    mWifiService.write(MessageType.toHAYTHAM_Calibrate_Scene_Correct);

                } break;
                case R.id.menu_liveview_calibration_create:  {
                    mWifiService.GazeStream(HMGT, false);
                    mWifiService.Speek("Wait!");
                    mWifiService.write(MessageType.toHAYTHAM_Calibrate_Scene_Master);

                } break;


                default:
                    return true;  // No change.
            }
           // mCardScroller.setAdapter(new CardAdapter(createCards(this)));
            return true;
        }
        return super.onMenuItemSelected(featureId, item);
    }

    /**
     * Creates a singleton card list to display as activity content.
     */
    private List<Card> createCards(Context context) {
        ArrayList<Card> cards = new ArrayList<Card>();


        cards.add(0, new Card(context)
                .setText(R.string.text_displayGaze_main));

        cards.add(SNAPSHOT, new Card(context)
                .addImage(getImageResource(SNAPSHOT))
                .setImageLayout(Card.ImageLayout.LEFT)
                .setText(R.string.text_liveViewGaze_blankDemo));

        cards.add(MOREDEMOS,new Card(context).setText(R.string.text_MOREDEMOS));


        return cards;
    }


    /**
     * Returns current image resource.
     */
    public static int getImageResource(int i) {
        switch (i) {
            //case CALIBRATION:  return R.drawable.rm;
            case SNAPSHOT:
                return R.drawable.snapshot;

            default:
                return 0;
        }
    }


    /**
     * Different type of activities can be shown, when tapped on a card.
     */
    private void setCardScrollerListener() {
        mCardScroller.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                AudioManager am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
                Log.d(TAG, "Clicked view at position " + position + ", row-id " + id);

                switch (position) {
                    case SNAPSHOT:
                        am.playSoundEffect( Sounds.TAP);
                        startActivity(new Intent(LiveViewActivity.this, SnapShot.class));
                        break;

                    case MOREDEMOS:
                        break;
                    default:

                        Log.d(TAG, "Don't show anything");
                }

            }
        });

        mCardScroller.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position != 0) {
                    mVoiceMenuEnabled = false;
                    getWindow().invalidatePanelMenu(WindowUtils.FEATURE_VOICE_COMMANDS);

                } else {
                    mVoiceMenuEnabled = true;
                    getWindow().invalidatePanelMenu(WindowUtils.FEATURE_VOICE_COMMANDS);

                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }


}
