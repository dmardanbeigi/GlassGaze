package com.glassgaze;


import java.net.InetAddress;

public class Constants {
      ///public static final int CHUNK_SIZE = 4192;
      public static final int HEADER_MSB = 0x10;
      public static final int HEADER_LSB = 0x55;
      public static final String NAME = "ANDROID-BTXFR";

      public static final int MSG_SIZE = 12;//byteArray[12]



      public static InetAddress SERVER_IP;

      public static Boolean UDPBroadcasting=false;

      public static Boolean QRcode_Scan=true;

      public static String directServerIP= "10.0.0.16";

    public static final int  display_W=640;
    public static final int  display_H = 360;

      public static final int  SERVER_PORT =  4444;
      public static final int  SERVER_PORT_UDP =  4343;
      public static final int  SERVER_PORT_UDP_GAZE =  4545;



      public static final String TEMP_IMAGE_FILE_NAME = "btimage.jpg";
      public static final int PICTURE_RESULT_CODE = 1234;
      public static final int IMAGE_QUALITY_LOW = 40;
      public static final int IMAGE_QUALITY_HIGH = 100;


    // Key names received
      public static final String DEVICE_NAME = "device_name";
      public static final String TOAST = "toast";
}
