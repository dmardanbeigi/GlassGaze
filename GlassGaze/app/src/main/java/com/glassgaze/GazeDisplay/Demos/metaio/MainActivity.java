package com.glassgaze.GazeDisplay.Demos.metaio;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.os.SystemClock;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout.LayoutParams;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.glassgaze.BuildConfig;
import com.glassgaze.Constants;
import com.glassgaze.GazeDisplay.PointerView_display;
import com.glassgaze.MessageType;
import com.glassgaze.R;
import com.glassgaze.Utils;
import com.glassgaze.WifiService;
import com.google.android.glass.media.Sounds;
import com.google.android.glass.touchpad.Gesture;
import com.google.android.glass.touchpad.GestureDetector;
import com.metaio.cloud.plugin.MetaioCloudPlugin;
import com.metaio.cloud.plugin.util.MetaioCloudUtils;
import com.metaio.cloud.plugin.view.ARMetaioCloudPluginManager;
import com.metaio.sdk.MetaioDebug;
import com.metaio.sdk.MetaioWorldPOIManagerCallback;
import com.metaio.sdk.fragments.ShareScreenshotFragment;
import com.metaio.sdk.jni.DataSourceEvent;
import com.metaio.sdk.jni.IGeometry;
import com.metaio.sdk.jni.IMetaioSDKAndroid;
import com.metaio.sdk.jni.TrackingValuesVector;
import com.metaio.sdk.jni.Vector3d;
import com.metaio.tools.SystemInfo;

import java.io.File;
import java.util.Date;

public class MainActivity extends MetaioCloudPluginBaseActivity implements DialogInterface.OnDismissListener
{
    private GestureDetector mGestureDetector = null;


    static final int RGT = 1;
    static final int CALIB = 1;

    float x=0.0f;
    float y=0.0f;
    PointerView_display mPointerViewDisplay;

    boolean setGaze=false;

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
            Toast.makeText(MainActivity.this, "Service is disconnected", Toast.LENGTH_SHORT).show();
            mBounded = false;
            mWifiService = null;
        }




    };
    private void init()
    {
        mWifiService.GazeStream(RGT,false);


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

                            //I don't know if this is a good idea. I basically get one (x,y) and then turn the streaming off.
                            //There might be a delay between sending and applying the (RGT,false) msg, and we may get more than one
                            //(x,y). In this case we will be clicking several times!!



                            break;
                        default:
                            super.handleMessage(msg);
                    }

                    break;
            }
        }
    }


    /**
	 * GUI overlay
	 */
	private RelativeLayout mGUIView;

	/**
	 * Progress bar viewMetaioCloudARViewTestActivity
	 */
	private ProgressBar progressView;

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
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);


        mGestureDetector = createGestureDetector(this);


		MetaioCloudPlugin.isDebuggable = BuildConfig.DEBUG;



        // TODO Set authentication if a private channel is used
		// MetaioCloudPlugin.setAuthentication("username", "password");

		// Ensure that initialize is called when this activity is started/recreated to initialize
		// all Cloud Plugin related settings, including setting the correct application identifier.
		// Call synchronously because super.onCreate may already have Cloud Plugin logic. Note
		// that startJunaio is already called in SplashActivity, but in case the application gets
		// restarted, or memory is low, or this activity is started directly without opening
		// SplashActivity, we have to make sure this is always called.
        try {
            loadNativeLibs();
        } catch (Exception e) {
            com.glassgaze.Utils.showErrorForCloudPluginResult(MetaioCloudPlugin.ERROR_CPU_NOT_SUPPORTED, this);
            return;
        }
		int result = MetaioCloudPlugin.initialize(null, getApplicationContext());

		// Window managed wake lock (no permissions, no accidentally kept on)
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

		// Optionaly add GUI
		if (mGUIView==null)
			mGUIView = (RelativeLayout)getLayoutInflater().inflate(R.layout.junaio_arview, null);


		progressView = (ProgressBar)mGUIView.findViewById(R.id.progressBar);

		// Init the AREL webview. Pass a container if you want to use a ViewPager or Horizontal
		// Scroll View over the camera preview or the root view.
		mMetaioCloudPluginManager.initARELWebView(mGUIView);

        mMetaioCloudPluginManager.mIsInLiveMode = true;

        if (result!= MetaioCloudPlugin.SUCCESS)
			com.glassgaze.Utils.showErrorForCloudPluginResult(result, this);




    }

	@Override
	protected void onStart()
	{
		super.onStart();
        Intent intent= new Intent(this, WifiService.class);
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);

		// add GUI layout
		if (mGUIView!=null)
			addContentView(mGUIView, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));

		// comes from splash activity
		final int channelID = getIntent().getIntExtra(getPackageName()+".CHANNELID", -1);
		if (channelID>0)
		{
			// Clear the intent extra before proceeding
			getIntent().removeExtra(getPackageName()+".CHANNELID");
			mMetaioCloudPluginManager.setChannel(channelID);
		}

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

	public void onScreenshot(Bitmap bitmap, boolean saveToGalleryWithoutDialog)
	{
		// this is triggered by calling takeScreenshot() or through AREL
		String filename = "junaio-"+DateFormat.format("yyyyMMdd-hhmmss", new Date())+".jpg";

		try
		{
			boolean result =
					MetaioCloudUtils.writeToFile(bitmap, CompressFormat.JPEG, 100, MetaioCloudPlugin.mCacheDir,
                            filename, false);

			if (result)
			{
				if (!saveToGalleryWithoutDialog) // meaning share it
				{
					final String path = new File(MetaioCloudPlugin.mCacheDir, filename).getAbsolutePath();

					// create a new instance of the screenshot dialog, you can provide custom text
					// for the buttons
					ShareScreenshotFragment fragment =
							ShareScreenshotFragment.newInstance(path, "Share screenshot",
                                    getString(R.string.BTN_SAVE_SCREENSHOT), "Share screenshot");

					// optionally set a notification to tell the user a screenshot was taken
					fragment.setNotification(R.drawable.icon_placeholder, getString(R.string.MSGI_IMAGE_SAVED),
							getString(R.string.BTN_VIEW_IMAGE));

					// optionally add some text to the sharing intent
					Intent sharingIntent = new Intent(Intent.ACTION_SEND);
					sharingIntent.putExtra(Intent.EXTRA_TEXT, "This was taken with Metaio SDK");
					sharingIntent.putExtra(Intent.EXTRA_SUBJECT, "Check this cool AR!!");

					fragment.setSharingIntent(sharingIntent);

					// display the fragment
					fragment.show(getSupportFragmentManager(), "share_screenshot");
				}
			}
		}
		catch (Exception e)
		{
			MetaioDebug.log(Log.ERROR, "onScreenshot: Error formatting date");
			MetaioDebug.printStackTrace(Log.ERROR, e);
		}
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig)
	{
		super.onConfigurationChanged(newConfig);
	}

	@Override
	public MetaioWorldPOIManagerCallback getMetaioWorldPOIManagerCallback()
	{
		if (mMetaioWorldPOIManagerCallback==null)
			mMetaioWorldPOIManagerCallback = new MetaioPOIManager(this);
		return mMetaioWorldPOIManagerCallback;
	}

	@Override
	public ARMetaioCloudPluginManager getARMetaioCloudPluginManagerInstance()
	{
		if (mMetaioCloudPluginManager==null)
			mMetaioCloudPluginManager = new MyCloudPlugin(this);


        return mMetaioCloudPluginManager;
	}

	/**
	 * Override {@link com.metaio.sdk.MetaioWorldPOIManagerCallback} to provide your own implementation of some of
	 * the methods
	 */
	class MetaioPOIManager extends MetaioWorldPOIManagerCallback
	{

		public MetaioPOIManager(Activity activity)
		{
			super(activity);
		}

		@Override
		public void onRadarPicked()
		{
			super.onRadarPicked();
		}

		@Override
		public void onTrackingEvent(TrackingValuesVector trackingValues)
		{
			// TODO Auto-generated method stub
			super.onTrackingEvent(trackingValues);
			Log.i("TRACKING",trackingValues.toString());
		}

		@Override
		protected void onSaveScreenshot(Bitmap screenshot, boolean saveToGalleryWithoutDialog)
		{
			MainActivity.this.onScreenshot(screenshot, saveToGalleryWithoutDialog);
		}
	}

	class MyCloudPlugin extends ARMetaioCloudPluginManager
	{

		public MyCloudPlugin(Activity activity)
		{
			super(activity);
		}

		@Override
		public MetaioWorldPOIManagerCallback getMetaioWorldPOIManagerCallback()
		{
			return MainActivity.this.getMetaioWorldPOIManagerCallback();
		}


		@Override
		public void showProgress(final boolean show)
		{

			progressView.post(new Runnable()
			{

				@Override
				public void run()
				{
					progressView.setIndeterminate(true);
					progressView.setVisibility(show?View.VISIBLE:View.INVISIBLE);
				}
			});
		}

		@Override
		public void showProgressBar(final float progress, final boolean show)
		{

			progressView.post(new Runnable()
			{

				@Override
				public void run()
				{
					progressView.setIndeterminate(false);
					progressView.setProgress((int)progress);
					progressView.setVisibility(show?View.VISIBLE:View.INVISIBLE);

				}
			});
		}

		@Override
		public void onSceneReady()
		{
			super.onSceneReady();
		}

		// called by the plugin
		@Override
		public void onSurfaceChanged(int width, int height)
		{
			com.glassgaze.Utils.log(getClass().getSimpleName() + ".onSurfaceChanged");
			super.onSurfaceChanged(width, height);

			// get radar margins from the resources (this will make the values density independant)
			float marginTop = getResources().getDimension(R.dimen.radarTop);
			float marginRight = getResources().getDimension(R.dimen.radarRight);
			float radarScale = getResources().getDimension(R.dimen.radarScale);
			// set the radar to the top right corner and add some margin, scale to 1
			mMetaioCloudPluginManager.setRadarProperties(IGeometry.ANCHOR_TOP| IGeometry.ANCHOR_RIGHT, new Vector3d(
					-marginRight, -marginTop, 0f), new Vector3d(radarScale, radarScale, 1f));

          //  mMetaioCloudPluginManager.setSeeThrough(true);


        }

		@Override
		public void onSurfaceCreated()
		{
			super.onSurfaceCreated();
		}

		@Override
		public void onSurfaceDestroyed()
		{
			super.onSurfaceDestroyed();
		}

		@Override
		public void onServerEvent(DataSourceEvent event)
		{
			switch (event)
			{
				case DataSourceEventNoPoisReturned:
					MetaioCloudUtils.showToast(MainActivity.this, getString(R.string.MSGI_POIS_NOT_FOUND));
					break;
				case DataSourceEventServerError:
					MetaioCloudUtils.showToast(MainActivity.this, getString(R.string.MSGE_TRY_AGAIN));
					break;
				case DataSourceEventServerNotReachable:
				case DataSourceEventCouldNotResolveServer:
					MetaioCloudUtils.showToast(MainActivity.this, getString(R.string.MSGW_SERVER_UNREACHABLE));
					break;
				default:
					break;
			}
		}
	}
	

	@Override
	public void onDismiss(DialogInterface dialog)
	{
		onActivityResult(MetaioWorldPOIManagerCallback.REQUEST_POI_CONTEXT, RESULT_OK, null);
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

        /*eventTime = SystemClock.uptimeMillis();

        MotionEvent motionEvent_move = MotionEvent.obtain(
                eventTime,
                eventTime+100,
                MotionEvent.ACTION_MOVE,
                x+0.0f,
                y+0.0f,
                metaState
        );
// Dispatch touch event to view

        view.dispatchTouchEvent(motionEvent_move);
        */

        eventTime = SystemClock.uptimeMillis();
        MotionEvent motionEvent_up = MotionEvent.obtain(
                eventTime,
                eventTime+50,
                MotionEvent.ACTION_UP,
                x+0.0f,
                y+0.0f,
                metaState
        );
// Dispatch touch event to view

        view.dispatchTouchEvent(motionEvent_up);



    }

    private GestureDetector createGestureDetector(Context context) {
        GestureDetector gestureDetector = new GestureDetector(context);
        gestureDetector.setBaseListener( new GestureDetector.BaseListener() {
            AudioManager am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);


            @Override
            public boolean onGesture(Gesture gesture) {
                if (gesture == Gesture.TAP) {

                    am.playSoundEffect( Sounds.TAP);

                    ////Turn the gaze stream on and wait for one gaze coordinates
                    //mWifiService.GazeStream(RGT,true);
                    //setGaze=true;

                    //mWifiService.GazeStream(RGT,false);
                    if (setGaze) {
                       // setGaze=false;
                        SelectButton();
                        AudioManager am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
                        am.playSoundEffect(Sounds.SELECTED);
                    }else
                    {
                        setGaze=true;
                        mWifiService.GazeStream(RGT,true);


                    }


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
