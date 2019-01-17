package com.daasuu.gpuvideoandroid.compose;

import android.content.Context;
import androidx.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.daasuu.gpuvideoandroid.R;

import java.util.List;

public class VideoListAdapter extends ArrayAdapter<VideoItem> {

    private LayoutInflater layoutInflater;

    public VideoListAdapter(Context context, int resource, List<VideoItem> objects) {
        super(context, resource, objects);
        layoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        VideoItem data = getItem(position);

        if (null == convertView) {
            convertView = layoutInflater.inflate(R.layout.row_video_list, null);
        }

        ImageView imageView = convertView.findViewById(R.id.image);
        TextView textView = convertView.findViewById(R.id.txt_image_name);

        Glide.with(getContext().getApplicationContext())
                .load(data.getPath())
                .into(imageView);

        textView.setText(data.getPath());

        return convertView;
    }

}

