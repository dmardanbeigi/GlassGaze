
/**
 * OpenGlass Voice Example 
 * Github - https://github.com/jaredsburrows/OpenQuartz
 * @author Jared Burrows
 * 
 * Copyright (C) 2013 OpenQuartz
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

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.util.Log;

import java.io.IOException;
import android.view.TextureView;

public class CameraView extends  TextureView implements
        TextureView.SurfaceTextureListener
{

	public Camera camera = null;




    /** A safe way to get an instance of the Camera object. */
    public static Camera getCameraInstance(){
        Camera c = null;
        try {
            c = Camera.open(); // attempt to get a Camera instance
        }
        catch (Exception e){
            // Camera is not available (in use or does not exist)
        }
        return c; // returns null if camera is unavailable
    }

    @SuppressWarnings("deprecation")
	public CameraView(Context context)
	{
		super(context);
        camera=getCameraInstance();

        init();

	}


    private void init() {
        setSurfaceTextureListener(this);

    }
    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        /*
 * onSurfaceTextureAvailable does not get called if it is already available.
 */
        try {
            this.setCameraParameters(camera);


            camera.setPreviewTexture(surface);
            CameraStart();
        } catch (IOException ioe) {
            // Something bad happened
        }

    }





    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
        // Ignored, Camera does all the work for us
    }
    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {
        // Invoked every time there's a new Camera preview frame
    }
    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        //this.releaseCamera();
        return true;
    }

    public void CameraStart() {

        try{
            camera.startPreview();
        } catch (Exception e){
            Log.e("............", "Error starting the camera: " + e.getMessage());
        }



    }

    public void CameraStop() {
        try{


        // camera.setPreviewCallback(null);//???????????????
            camera.stopPreview();

        } catch (Exception e){
            Log.e("............", "Error stopping the camera: " + e.getMessage());
        }
    }
    /**
     * Release the camera from use
     */
    public void releaseCamera()
    {
        //just to make sure that I can still release the camera even when I call stopPreview more than once
        try{
            if (camera != null)
            {

                camera.stopPreview();


            }
        } catch (Exception e){
            Log.e("............", "Error Stopping the camera: " + e.getMessage());
        }

        try{
            if (camera != null)
            {

                     // camera.setPreviewCallback(null);//this should be after stopPreview and before release otherwise you get error
                camera.release();
                camera = null;

            }
        } catch (Exception e){
            Log.e("............", "Error releasing the camera: " + e.getMessage());
        }
    }
	/**
	 * Important HotFix for Google Glass (post-XE11) update
	 * @param camera Object
	 */
	public void setCameraParameters(Camera camera) {
        try {
            if (camera != null) {
                Parameters parameters = camera.getParameters();
                parameters.setPreviewFpsRange(30000, 30000);
                camera.setParameters(parameters);
            }

        } catch (Exception e) {
            Log.e("............", "Error camera setCameraParameters " + e.getMessage());
        }
    }



	
}