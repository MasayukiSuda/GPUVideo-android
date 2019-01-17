package com.daasuu.gpuvideoandroid;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.List;

public class FilterAdapter extends ArrayAdapter<FilterType> {

    static class ViewHolder {
        public TextView text;
    }

    private final Context context;
    private final List<FilterType> values;
    private boolean isWhite = false;

    public FilterAdapter(Context context, int resource, List<FilterType> objects) {
        super(context, resource, objects);
        this.context = context;
        values = objects;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View rowView = convertView;
        // reuse views
        if (rowView == null) {
            LayoutInflater inflater = (LayoutInflater) context
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            if (isWhite) {
                rowView = inflater.inflate(R.layout.row_white_text, null);
            } else {
                rowView = inflater.inflate(R.layout.row_text, null);
            }
            // configure view holder
            ViewHolder viewHolder = new ViewHolder();
            viewHolder.text = rowView.findViewById(R.id.label);
            rowView.setTag(viewHolder);
        }

        ViewHolder holder = (ViewHolder) rowView.getTag();
        String s = values.get(position).name();
        holder.text.setText(s);

        return rowView;
    }

    public FilterAdapter whiteMode() {
        isWhite = true;
        return this;
    }


}

