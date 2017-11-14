package com.dockysoft.alias;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;

import java.util.ArrayList;

/**
 * Created by isaac on 6/2/2017.
 */

public class RoomListAdapter extends BaseAdapter implements Filterable {
    private Activity activity;
    private ArrayList<Room> originalData;
    private ArrayList<Room> data;
    private LayoutInflater inflater = null;
    private ValueFilter valueFilter;
    Context context;

    public RoomListAdapter(Activity a, ArrayList<Room> d){
        context = a.getBaseContext();
        activity = a;
        originalData = d;
        data = d;
        inflater = (LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    public int getCount(){
        return data.size();
    }

    public Object getItem(int position){
        return position;
    }

    public long getItemId(int position){
        return position;
    }

    @SuppressLint("InflateParams")
    public View getView(int position, View convertView, ViewGroup parent){
        View view = convertView;

        if(convertView == null)
            view = inflater.inflate(R.layout.room_entry, null);

        TextView name = (TextView) view.findViewById(R.id.roomNameTextView);
        TextView desc = (TextView) view.findViewById(R.id.roomDescTextView);

        Room room = data.get(position);

        name.setText(room.getName());
        desc.setText(room.getDesc());

        return view;
    }

    @Override
    public Filter getFilter() {
        if(valueFilter == null) {
            valueFilter = new ValueFilter();
        }

        return valueFilter;
    }

    public ArrayList<Room> getData() {
        return data;
    }

    private class ValueFilter extends Filter {
        // Invoked in a worker thread to filter the data according to the constraint
        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            FilterResults results = new FilterResults();

            if(constraint != null && constraint.length() > 0){
                ArrayList<Room> filterList = new ArrayList();

                for(int i = 0; i < originalData.size(); i++){
                    if( (originalData.get(i).getName().toUpperCase()).contains(constraint.toString().toUpperCase()) ) {
                        Room room = new Room(originalData.get(i).getName(), originalData.get(i).getDesc(), originalData.get(i).getAuthor());
                        filterList.add(room);
                    }
                }

                results.count = filterList.size();
                results.values = filterList;
            } else {
                results.count = originalData.size();
                results.values = originalData;
            }
            return results;
        }


        // Invoked in the UI thread to publish the filtering results in the user interface
        @SuppressWarnings("unchecked")
        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {
            data = (ArrayList<Room>) results.values;
            notifyDataSetChanged();
        }
    }
}
