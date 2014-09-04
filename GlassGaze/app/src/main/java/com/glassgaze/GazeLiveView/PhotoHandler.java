package com.glassgaze.GazeLiveView;

import com.glassgaze.Constants;
import com.glassgaze.MessageType;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Camera;
import android.hardware.Camera.PictureCallback;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;


public class PhotoHandler implements PictureCallback {
    private final Handler mHandler;
  private final Context context;

  public PhotoHandler(Context context,Handler handler) {
      mHandler = handler;
    this.context = context;
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
