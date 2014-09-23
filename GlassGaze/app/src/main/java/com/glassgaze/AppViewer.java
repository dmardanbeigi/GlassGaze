package com.glassgaze;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.FrameLayout;
import android.widget.TextView;


public class AppViewer extends FrameLayout {
    public AppViewer(Context context) {
        this(context, null, 0);
    }
    public AppViewer(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }
    public void start() {
    }
    public AppViewer(Context context, AttributeSet attrs, int style) {
        super(context, attrs, style);
        LayoutInflater.from(context).inflate(R.layout.start,this);
        TextView mTextView = (TextView) findViewById(R.id.hello_view);
        mTextView.setText("GlassGaze");
    }
}