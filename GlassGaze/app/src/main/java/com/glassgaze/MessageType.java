package com.glassgaze;


public class MessageType {
    
    /************************INTERNAL Messages*********************************/
    
    public static final int DATA_SENT_OK = 0x00;
    public static final int READY_FOR_DATA = 0x01;
    public static final int DATA_RECEIVED = 0x02;
    public static final int DATA_PROGRESS_UPDATE = 0x03;
    public static final int SENDING_DATA = 0x04;
    public static final int PHOTO_READY = 0x66;
    public static final int PHOTO_READY_IMAGE = 0x67;

    public static final int EXIT = 0x77;

    public static final int CONNECTED = 0x88;

    public static final int DIGEST_DID_NOT_MATCH = 0x50;
    public static final int COULD_NOT_CONNECT = 0x51;
    public static final int INVALID_HEADER = 0x52;


    // Intent request codes
    public static final int REQUEST_CONNECT_DEVICE = 11;
    public static final int REQUEST_ENABLE_BT = 22;

    // Message types sent from the BluetoothChatService Handler
    public static final int MESSAGE_STATE_CHANGE = 111;
    public static final int MESSAGE_READ = 222;
    public static final int MESSAGE_WRITE = 33;
    public static final int MESSAGE_DEVICE_NAME = 44;
    public static final int MESSAGE_TOAST = 55;


    /************************HAYTHAM Messages*********************************/


    //standard 12 byte int msgs
    //GLASS to HAYTHAM
    public static final int toHAYTHAM_READY =1000;
    public static final int toHAYTHAM_StreamGaze_HMGT_START = 1001;
    public static final int toHAYTHAM_StreamGaze_RGT_START = 1002;
    public static final int toHAYTHAM_StreamGaze_HMGT_STOP = 1003;
    public static final int toHAYTHAM_StreamGaze_RGT_STOP = 1004;
    public static final int toHAYTHAM_Calibrate_Display_4 = 1005;
    public static final int toHAYTHAM_Calibrate_Display_Correct = 1006;
    public static final int toHAYTHAM_Calibrate_Scene_4 =1007;
    public static final int toHAYTHAM_Calibrate_Scene_Correct = 1008;
    public static final int toHAYTHAM_SnapshotComming = 1009;
    public static final int toHAYTHAM_SceneCalibrationReady =1010;
    public  static final int toHAYTHAM_HeadderComming=1011;



    //HAYTHAM to GLASS
    public static final int toGLASS_test =2001 ;
    public static final int toGLASS_GAZE_RGT = 2002;
    public static final int toGLASS_GAZE_HMGT =2003;
    public static final int toGLASS_Calibrate_Display = 2004;
    public static final int toGLASS_Calibrate_Scene = 2005;
    public static final int toGLASS_ERROR_NOTCalibrated =2006;




}
