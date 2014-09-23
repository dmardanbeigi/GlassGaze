package com.glassgaze.GazeLiveView;

import com.glassgaze.Constants;
import com.glassgaze.MessageType;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Camera;
import android.hardware.Camera.PictureCallback;
import android.os.Handler;

import java.io.ByteArrayOutputStream;


public class PhotoHandler implements PictureCallback {
    private final Handler mHandler;

    public PhotoHandler(Context context,Handler handler) {
      mHandler = handler;
        Context context1 = context;
  }

  @Override
  public void onPictureTaken(byte[] data, Camera camera) {

      Bitmap image = BitmapFactory.decodeByteArray(data , 0, data.length);

      ByteArrayOutputStream compressedImageStream = new ByteArrayOutputStream();
      image.compress(Bitmap.CompressFormat.JPEG, Constants.IMAGE_QUALITY_LOW, compressedImageStream);
      byte[] compressedImage = compressedImageStream.toByteArray();


      mHandler.obtainMessage(MessageType.PHOTO_READY_IMAGE, -1, -1, image).sendToTarget();


      mHandler.obtainMessage(MessageType.PHOTO_READY, -1, -1, compressedImage).sendToTarget();

  }


} 
