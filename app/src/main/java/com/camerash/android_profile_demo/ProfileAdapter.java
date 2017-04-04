package com.camerash.android_profile_demo;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;

/**
 * Created by Esmond on 11-Mar-17.
 */

public class ProfileAdapter extends RecyclerView.Adapter<ProfileAdapter.MyHolder> {
    private ArrayList<String> name, single, email, date, hobby;
    private Context mContext;

    public ProfileAdapter(Context context, ArrayList<String> name, ArrayList<String> single, ArrayList<String> email, ArrayList<String> date, ArrayList<String> hobby) {
        this.mContext = context;
        this.name = name;
        this.single = single;
        this.email = email;
        this.date = date;
        this.hobby = hobby;
    }

    @Override
    public MyHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View inflatedView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.profile_item, parent, false);
        return new MyHolder(inflatedView);
    }

    @Override
    public void onBindViewHolder(final MyHolder holder, final int position) {
        holder.bindProfile(name.get(position), single.get(position), email.get(position), date.get(position), hobby.get(position));
    }

    public long getItemId(int position) {
        return position;
    }

    @Override
    public int getItemCount() {
        return name.size();
    }

    public static class MyHolder extends RecyclerView.ViewHolder {

        private TextView name, single, email, date, hobby;
        private LinearLayout layout;

        public MyHolder(View v) {
            super(v);
            name = (TextView) v.findViewById(R.id.name);
            single = (TextView) v.findViewById(R.id.single);
            email = (TextView) v.findViewById(R.id.email);
            date = (TextView) v.findViewById(R.id.registered_date);
            hobby = (TextView) v.findViewById(R.id.hobby);
            layout = (LinearLayout) v.findViewById(R.id.layout);
        }

        public void bindProfile(String nameStr, String singleStr, String emailStr, String dateStr, String hobbyStr) {
            if (!nameStr.equals("")) {
                name.setText(nameStr);
                single.setText("Single: " + singleStr);
                email.setText("Email: " + emailStr);
                date.setText("Registered date: " + dateStr);
                hobby.setText("Hobbies: " + hobbyStr);
            } else {
                layout.setVisibility(View.GONE);
            }
        }
    }
}
