package com.glassgaze.GazeDisplay.Demos.SimpleGazeList;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.glassgaze.R;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by diako on 9/9/2014.
 */
public class GazeListAdapter extends BaseAdapter {

    public List<String> list_names = new ArrayList<String>();
    public List<String> list_IDs = new ArrayList<String>();
    Context mContext;

    public GazeListAdapter(Context context, List<String> names, List<String> ids) {
        mContext = context;
        list_names = names;
        list_IDs = ids;

    }

    @Override
    public int getCount() {
        return list_names.size();
    }

    @Override
    public String getItem(int position) {
        return list_names.get(position);
    }

    @Override
    public long getItemId(int position) {
        return list_names.get(position).hashCode();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup container) {
        if (convertView == null) {

            convertView = LayoutInflater.from(mContext).inflate(R.layout.gazelist_item, container, false);
        }

        ((TextView) convertView.findViewById(android.R.id.text1))
                .setText(getItem(position));
        return convertView;
    }
}


