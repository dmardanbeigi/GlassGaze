package com.glassgaze.GazeDisplay.Demos.metaio;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.media.AudioManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.os.SystemClock;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

import com.glassgaze.Constants;
import com.glassgaze.GazeDisplay.Calibration;
import com.glassgaze.GazeDisplay.PointerView_display;
import com.glassgaze.MessageType;
import com.glassgaze.R;
import com.glassgaze.Utils;
import com.glassgaze.WifiService;
import com.google.android.glass.media.Sounds;
import com.google.android.glass.touchpad.Gesture;
import com.google.android.glass.touchpad.GestureDetector;
import com.metaio.cloud.plugin.MetaioCloudPlugin;
import com.metaio.sdk.MetaioDebug;
import com.metaio.sdk.jni.IMetaioSDKAndroid;
import com.metaio.tools.SystemInfo;

public class SplashActivity extends Activity
{
    private GestureDetector mGestureDetector = null;
	/**
	 * Progress dialog
	 */
	private ProgressDialog progressDialog;

    PointerView_display mPointerViewDisplay;

    static final int RGT = 1;
    static final int CALIB = 1;

    float x=0.0f;
    float y=0.0f;

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
            Toast.makeText(SplashActivity.this, "Service is disconnected", Toast.LENGTH_SHORT).show();
            mBounded = false;
            mWifiService = null;
        }




    };
    private void init()
    {
        mWifiService.GazeStream(RGT,true);
        mPointerViewDisplay = (PointerView_display) findViewById(R.id.pointerView_display);

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

                    switch (Utils.GetIndicator(readBuf)) {


                        case MessageType.toGLASS_test:
                            Toast.makeText(getApplicationContext(), "Test Msg from Haytham", Toast.LENGTH_SHORT).show();  //  C1S

                            break;


                        case MessageType.toGLASS_GAZE_RGT:

                             x = (float) Utils.GetX(readBuf);
                             y =(float)  Utils.GetY(readBuf);

                            mPointerViewDisplay.GazeEvent((int)x, (int)y, 4);

                            break;
                        default:
                            super.handleMessage(msg);
                    }

                    break;
            }
        }
    }



    /**
     * Load native libs required by the Metaio SDK
     */
    protected void loadNativeLibs() throws UnsatisfiedLinkError, RuntimeException
    {
        IMetaioSDKAndroid.loadNativeLibs();
        MetaioDebug.log(Log.INFO, "MetaioSDK libs loaded for " + SystemInfo.getDeviceABI() + " using "
                + com.metaio.sdk.jni.SystemInfo.getAvailableCPUCores() + " CPU cores");
    }

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
        mGestureDetector = createGestureDetector(this);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

		setContentView(R.layout.junaio_main);


        try {
            loadNativeLibs();
        } catch (Exception e) {
            com.glassgaze.Utils.showErrorForCloudPluginResult(MetaioCloudPlugin.ERROR_CPU_NOT_SUPPORTED, this);
            return;
        }

		// Initialize cloud plugin in an AsyncTask
		CloudPluginInitializerTask task = new CloudPluginInitializerTask();
		task.execute(1);


	}


    //...................................................
    @Override
    protected void onDestroy(){
        mWifiService.GazeStream(RGT, false);
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

        if (progressDialog != null)
        {
            progressDialog.dismiss();
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




	/**
	 * Launch Cloud plugin's live view
	 */
	private void launchLiveView()
	{
		// Set your channel id in /res/values/channelid.xml
		final int myChannelId = getResources().getInteger(R.integer.channelid);

		// if you have set a channel ID, then load it directly
		if (myChannelId != -1)
		{
			startChannel(myChannelId, true);
		}
	}

	public void startARELChannel(View v)
	{
		startChannel(383212, true);// start AREL test
	}

	public void startLocationBasedChannel(View v)
	{
		startChannel(4796, true);// start Wikipedia EN
	}

	public void startChannel(int channelId, boolean andFinishActivity)
	{
		Intent intent = new Intent(SplashActivity.this, MainActivity.class);
		intent.putExtra(getPackageName() + ".CHANNELID", channelId);

        Log.i("ChannelID: ", String.valueOf(channelId));
        Log.i("getPackageName(): ",getPackageName().toString());


        startActivity(intent);

		if (andFinishActivity)
			finish();
	}

	private class CloudPluginInitializerTask extends AsyncTask<Integer, Integer, Integer>
	{

		@Override
		protected void onPreExecute()
		{
			progressDialog = ProgressDialog.show(SplashActivity.this, "Metaio Cloud Plugin", "Starting up...");
		}

		@Override
		protected Integer doInBackground(Integer... params)
		{

			// TODO Set authentication if a private channel is used
			// MetaioCloudPlugin.setAuthentication("username", "password");

			// This will initialize everything the plugin needs
			final int result = MetaioCloudPlugin.initialize(this, getApplicationContext());

			return result;
		}

		@Override
		protected void onProgressUpdate(Integer... progress)
		{

		}

		@Override
		protected void onPostExecute(Integer result)
		{
			if (progressDialog != null)
			{
				progressDialog.cancel();
				progressDialog = null;
			}

			if (result == MetaioCloudPlugin.SUCCESS)
				launchLiveView();
			else
				com.glassgaze.Utils.showErrorForCloudPluginResult(result, SplashActivity.this);
		}

    }


    private void SelectButton(){

//-------------- Diako: simulating touch
        //Here I simulate the action of touching the second button
        //View  view = (View)findViewById(R.layout.junaio_main);
        View  view = getWindow().getDecorView().getRootView();

// Obtain MotionEvent object
        long eventTime = SystemClock.uptimeMillis();

        //float x=70.0f;
        //float y=50.0f;

// List of meta states found here: developer.android.com/reference/android/view/KeyEvent.html#getMetaState()
        int metaState = 0;
        MotionEvent motionEvent_down = MotionEvent.obtain(
                eventTime,
                eventTime+100,
                MotionEvent.ACTION_DOWN,
                x,
                y,
                0

        );


        view.dispatchTouchEvent(motionEvent_down);

        eventTime = SystemClock.uptimeMillis();

        MotionEvent motionEvent_move = MotionEvent.obtain(
                eventTime,
                eventTime+100,
                MotionEvent.ACTION_MOVE,
                x+1.0f,
                y+1.0f,
                metaState
        );
// Dispatch touch event to view

        view.dispatchTouchEvent(motionEvent_move);


        eventTime = SystemClock.uptimeMillis();
        MotionEvent motionEvent_up = MotionEvent.obtain(
                eventTime,
                eventTime+100,
                MotionEvent.ACTION_UP,
                x+1.0f,
                y+1.0f,
                metaState
        );
// Dispatch touch event to view

        view.dispatchTouchEvent(motionEvent_up);



    }

    private GestureDetector createGestureDetector(Context context) {


        GestureDetector gestureDetector = new GestureDetector(context);
        gestureDetector.setBaseListener( new GestureDetector.BaseListener() {
            @Override
            public boolean onGesture(Gesture gesture) {
                AudioManager am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
                if (gesture == Gesture.TAP) {
                    am.playSoundEffect( Sounds.TAP);
SelectButton();

                    return true;
                }
                else if (gesture == Gesture.TWO_TAP) {


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


}
