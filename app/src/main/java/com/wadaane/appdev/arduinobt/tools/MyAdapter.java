package com.wadaane.appdev.arduinobt.tools;

import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.wadaane.appdev.arduinobt.Activity_Sensor;
import com.wadaane.appdev.arduinobt.R;

import java.util.List;

import static android.content.Context.MODE_PRIVATE;

public class MyAdapter extends RecyclerView.Adapter<MyAdapter.ViewHolder> {
    public List<Sensor> list;
    public Context context;
    private MyOnClickListener myOnClickListener;

    public MyAdapter(List<Sensor> sensorList, Context con, MyOnClickListener myOnClickListener1) {
        this.list = sensorList;
        this.context = con;
        this.myOnClickListener = myOnClickListener1;

    }


    //  Create View that hold the view widgets.
    @Override
    public MyAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.itemview, parent, false);

        return new ViewHolder(v);
    }

    // Bind data with the view widget.
    @Override
    public void onBindViewHolder(ViewHolder holder, final int position) {
        holder.mTextView.setText(list.get(position).getName());
        holder.cardView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                final SharedPreferences preferences = context.getSharedPreferences("SETTINGS", MODE_PRIVATE);
                String ADDRESS = context.getSharedPreferences("SETTINGS", MODE_PRIVATE)
                        .getString("DEVICE", "0");
                if (ADDRESS.equals("0")) {
                    Toast.makeText(context, "Please select device first !", Toast.LENGTH_SHORT).show();
                } else {
                    Intent i = new Intent(context, Activity_Sensor.class);
                    i.putExtra("SENSOR", String.valueOf(list.get(position).getType()));
//                    context.startActivity(i);
                    myOnClickListener.myOnClick(i);

                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    // Take the View and assign the view widgets
    public class ViewHolder extends RecyclerView.ViewHolder {
        public TextView mTextView;
        public CardView cardView;

        public ViewHolder(View v) {
            super(v);
            mTextView = (TextView) v.findViewById(R.id.textViewSensorName);
            cardView = (CardView) v.findViewById(R.id.cardView);
        }
    }


}
