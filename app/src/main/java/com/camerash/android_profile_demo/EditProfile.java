package com.camerash.android_profile_demo;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

import io.skygear.skygear.Error;
import io.skygear.skygear.Query;
import io.skygear.skygear.Record;
import io.skygear.skygear.RecordQueryResponseHandler;
import io.skygear.skygear.RecordSaveResponseHandler;

public class EditProfile extends AppCompatActivity {

    private Record myRecord;
    private EditText email, h1, h2, h3;
    private CheckBox single;
    private Button save;
    public static ProgressDialog progressDialog;
    public Thread getMyRecordThread;

    public Runnable getMyRecords = new Runnable() {
        @Override
        public void run() {
            Query noteQuery = new Query("user")
                .equalTo("_id",MainActivity.me.getId());

            MainActivity.publicDB.query(noteQuery, new RecordQueryResponseHandler() {
                @Override
                public void onQuerySuccess(Record[] records) {
                    myRecord = records[0];
                    prepareMyProfile();
                }

                @Override
                public void onQueryError(Error error) {

                }
            });
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);
        Toolbar myToolbar = (Toolbar) findViewById(R.id.my_toolbar);
        setSupportActionBar(myToolbar);

        email = (EditText) findViewById(R.id.email);
        h1 = (EditText) findViewById(R.id.h1);
        h2 = (EditText) findViewById(R.id.h2);
        h3 = (EditText) findViewById(R.id.h3);

        single = (CheckBox) findViewById(R.id.single);
        save = (Button) findViewById(R.id.save);

        save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveMyProfile();
            }
        });

        progressDialog = new ProgressDialog(this);
        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progressDialog.setIndeterminate(false);
        progressDialog.setMessage("Loading......");
        progressDialog.show();

        getMyRecordThread = new Thread(getMyRecords);
        getMyRecordThread.start();
    }

    private void prepareMyProfile(){
        HashMap data = myRecord.getData();
        if(data.get("email")!=null){
            email.setText(data.get("email").toString());
        }
        if(data.get("single")!=null){
            single.setChecked((Boolean) data.get("single"));
        }
        if(data.get("hobbies")!=null){
            JSONObject hobbies = (JSONObject) data.get("hobbies");
            try {
                h1.setText(hobbies.getString("1"));
                h2.setText(hobbies.getString("2"));
                h3.setText(hobbies.getString("3"));
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (progressDialog != null) {
                    progressDialog.dismiss();
                }
            }
        });
    }

    private void saveMyProfile(){
        progressDialog.setMessage("Saving......");
        progressDialog.show();

        myRecord.set("email",email.getText().toString());
        myRecord.set("single",single.isChecked());
        JSONObject hobbies = new JSONObject();
        try {
            hobbies.put("1",h1.getText().toString());
            hobbies.put("2",h2.getText().toString());
            hobbies.put("3",h3.getText().toString());
            myRecord.set("hobbies",hobbies);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        MainActivity.publicDB.save(myRecord, new RecordSaveResponseHandler() {
            @Override
            public void onSaveSuccess(Record[] records) {
                Toast.makeText(EditProfile.this,"Saved successfully",Toast.LENGTH_SHORT).show();
                progressDialog.dismiss();
                MainActivity.getRecordsThread.start();
                MainActivity.swipeRefreshLayout.setRefreshing(true);
                finish();
            }

            @Override
            public void onPartiallySaveSuccess(Map<String, Record> successRecords, Map<String, Error> errors) {

            }

            @Override
            public void onSaveFail(Error error) {
                Log.d("error",error.getCode().toString());
                Toast.makeText(EditProfile.this,"Saved failed",Toast.LENGTH_SHORT).show();
                progressDialog.dismiss();
            }
        });
    }
}
