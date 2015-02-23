/*
 * Copyright (C) 2013 The Android Open Source Project
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
package com.glassgaze.GazeDisplay;

import com.glassgaze.R;

import android.content.Context;
import android.util.AttributeSet;
import android.view.Gravity;
import android.widget.RelativeLayout;
import android.widget.TextView;

/**
 * Provides a visual representation of the Glass touchpad, with colored and labeled circles that
 * represent the locations_display_Coordinate of the user's fingers when they are on the touchpad.
 */
public class PointerView_display extends RelativeLayout {

    // The size of the finger trace drawn at the location of one of the user's fingers.
    private static  int FINGER_TRACE_SIZE = 50;

    // The drawable ids used to draw the user's three fingers.
    private static final int[] FINGER_RES_IDS = {
            R.drawable.pointer0,
            R.drawable.pointer1,
             R.drawable.pointer2,
             R.drawable.pointer3,
             R.drawable.pointer4,
            R.drawable.pointer5,
            R.drawable.gaze_pointer,


};

    // The duration, in milliseconds, of the animation used to fade out a finger trace when the
    // user's finger is lifted from the touchpad.
    private static final int FADE_OUT_DURATION_MILLIS = 100;

    // The views used to display the location of a finger on the touchpad.
    private final TextView[] mFingerTraceViews = new TextView[FINGER_RES_IDS.length];

    // The horizontal and vertical hardware resolutions of the touchpad. These are used to
    // calculate the aspect ratio of the view when it is measured.
    private float mTouchpadHardwareWidth;
    private float mTouchpadHardwareHeight;

    public PointerView_display(Context context) {
        this(context, null, 0);
    }

    public PointerView_display(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public PointerView_display(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        setFocusable(true);
        //setFocusableInTouchMode(true);
        setClipChildren(false);



        for (int i = 0; i < mFingerTraceViews.length; i++) {

            String text="+";
            if (i==2 || i==5)text="";
            else if (i==6) text="X";

            mFingerTraceViews[i] = createFingerTraceView(i,text);
            addView(mFingerTraceViews[i]);
        }
    }


/*

    */
/**
     * Processes all the pointers that are part of the motion event and displays the finger traces
     * at the proper positions in the view.
     * <p>
     * Since this view is only intended to render motion events and not consume them, we always
     * return false so that the events bubble up to the activity and the gesture detector has a
     * chance to handle them.
     *//*

    @Override
    public boolean onGenericMotionEvent(MotionEvent event) {
        for (int i = 0; i < event.getPointerCount(); i++) {
            int pointerId = event.getPointerId(i);
            float x = event.getX(i);
            float y = event.getY(i);

            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                case MotionEvent.ACTION_POINTER_DOWN:
                case MotionEvent.ACTION_MOVE:
                    moveFingerTrace(pointerId, x, y);
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_POINTER_UP:
                case MotionEvent.ACTION_CANCEL:
                    hideFingerTrace(pointerId);
                    break;
            }
        }

        return false;
    }
*/


    /**
     * Creates a new view that will be used to display the specified pointer id on the touchpad
     * view.
     *
     * @param pointerId the id of the pointer that this finger trace view will represent; used to
     *     determine its color and text
     * @return the {@code TextView} that was created
     */
    private TextView createFingerTraceView(int pointerId, String txt) {
        TextView fingerTraceView = new TextView(getContext());
        fingerTraceView.setBackgroundResource(FINGER_RES_IDS[pointerId]);
        fingerTraceView.setText(txt);
        fingerTraceView.setGravity(Gravity.CENTER);

        fingerTraceView.setTextAppearance(getContext(),
                android.R.style.TextAppearance_DeviceDefault_Small);

        fingerTraceView.setAlpha(0);

        RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(
                FINGER_TRACE_SIZE, FINGER_TRACE_SIZE);
        lp.addRule(RelativeLayout.ALIGN_PARENT_TOP);
        lp.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
        // The right and bottom margin here are required so that the view doesn't get "squished"
        // as it touches the right or bottom side of the touchpad view.
        lp.rightMargin = -2 * FINGER_TRACE_SIZE;
        lp.bottomMargin = -2 * FINGER_TRACE_SIZE;
        fingerTraceView.setLayoutParams(lp);

        return fingerTraceView;
    }

    public void GazeEvent(int x, int y ,int color) {
        setFocusable(true);
        requestFocus();

         moveFingerTrace(color, x, y);
        requestFocus();
        // hideFingerTrace(pointerId);


    }



    /**
     * Moves the finger trace associated with the specified pointer id to a new location in the
     * view.
     *
     * @param pointerId the pointer id of the finger trace to move
     * @param x the new location of the finger trace
     */
    private void moveFingerTrace(int pointerId, float x, float y) {
        TextView fingerTraceView = mFingerTraceViews[pointerId];

        // Cancel any current animations on the view and bring it back to full opacity.
        fingerTraceView.animate().cancel();
        fingerTraceView.setAlpha(1);

        // Reposition the finger trace by updating the layout margins of its view.
        RelativeLayout.LayoutParams lp =
                (RelativeLayout.LayoutParams) fingerTraceView.getLayoutParams();



      //  int viewX = (int) (x / mTouchpadHardwareWidth * getWidth());
      //  int viewY = (int) (y / mTouchpadHardwareHeight * getHeight());

      //  lp.leftMargin = viewX - FINGER_TRACE_SIZE / 2;
      //  lp.topMargin = viewY - FINGER_TRACE_SIZE / 2;


        //x=320;
       // y=150;
        lp.leftMargin =  (int)x - FINGER_TRACE_SIZE / 2;
        lp.topMargin =  (int)y - FINGER_TRACE_SIZE / 2;

        fingerTraceView.setLayoutParams(lp);

        invalidate();//??????????????
    }

    /**
     * Hides the finger trace associated with the specified pointer id. Traces are faded away
     * instead of immediately hidden in order to reduce flickering due to intermittence in the
     * touchpad.
     *
     * @param pointerId the pointer id whose finger trace should be hidden
     */
    public void hideFingerTrace(int pointerId, Boolean fade) {
        TextView fingerTraceView = mFingerTraceViews[pointerId];
        if (fade) fingerTraceView.animate().setDuration(FADE_OUT_DURATION_MILLIS).alpha(0);
        else fingerTraceView.setAlpha(0);
    }
        public void setFINGER_TRACE_SIZE(int FINGER_SIZE, int id) {
        FINGER_TRACE_SIZE = FINGER_SIZE;
        TextView fingerTraceView = mFingerTraceViews[id];
        RelativeLayout.LayoutParams lp =
                (RelativeLayout.LayoutParams) fingerTraceView.getLayoutParams();
        lp.width = FINGER_SIZE;
        lp.height = FINGER_SIZE;
    }
}
