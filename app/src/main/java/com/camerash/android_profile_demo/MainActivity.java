package com.camerash.android_profile_demo;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.ContextThemeWrapper;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import io.skygear.skygear.AuthResponseHandler;
import io.skygear.skygear.Configuration;
import io.skygear.skygear.Container;
import io.skygear.skygear.Database;
import io.skygear.skygear.Error;
import io.skygear.skygear.Query;
import io.skygear.skygear.Record;
import io.skygear.skygear.RecordQueryResponseHandler;
import io.skygear.skygear.RecordSaveResponseHandler;
import io.skygear.skygear.User;

public class MainActivity extends AppCompatActivity {

    public static Record[] savedRecords;
    public static ProgressDialog progressDialog;
    public static Container skygear;
    public static Database publicDB;
    public Thread skygearSignupThread, skygearSigninThread;
    public static Thread getRecordsThread;
    public Context mInstance;
    public String username, password;
    public RecyclerView recyclerView;
    public LinearLayoutManager linearLayoutManager;
    public static SwipeRefreshLayout swipeRefreshLayout;
    public static User me;
    public static LinearLayout loginScreen;
    public boolean firstStartUp = false;

    public Runnable getRecords = new Runnable() {
        @Override
        public void run() {
            Query noteQuery = new Query("user");

            publicDB.query(noteQuery, new RecordQueryResponseHandler() {
                @Override
                public void onQuerySuccess(Record[] records) {
                    Log.i("Record Query", String.format("Successfully got %d records", records.length));
                    savedRecords = records;
                    prepareProfileList();
                }

                @Override
                public void onQueryError(Error error) {
                    Log.i("Record Query", String.format("Fail with reason:%s", error.getMessage()));
                }
            });
        }
    };
    public Runnable skygearSignin = new Runnable() {
        @Override
        public void run() {
            skygear.loginWithUsername(username, password, new AuthResponseHandler() {
                @Override
                public void onAuthSuccess(User user) {
                    progressDialog.dismiss();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            findViewById(R.id.login).setVisibility(View.GONE);
                            findViewById(R.id.loading_screen).setVisibility(View.GONE);
                            progressDialog.setMessage("Loading......");
                            progressDialog.show();
                        }
                    });
                    Util.saveData(mInstance, "username", username);
                    Util.saveData(mInstance, "password", password);
                    me = user;
                    getRecordsThread = new Thread(getRecords);
                    getRecordsThread.start();
                }

                @Override
                public void onAuthFail(Error error) {
                    progressDialog.dismiss();
                    if(error.getCode().equals(Error.Code.INVALID_CREDENTIALS) || error.getCode().equals(Error.Code.RESOURCE_NOT_FOUND)) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(mInstance, "Email / Password incorrect", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                    else{
                        Handler handler = new Handler();
                        handler.postDelayed(skygearSignin, 2000);
                    }
                }
            });
        }
    };

    public Runnable skygearSignup = new Runnable() {
        @Override
        public void run() {
            skygear.signupWithUsername(username, password, new AuthResponseHandler() {
                @Override
                public void onAuthSuccess(User user) {
                    progressDialog.dismiss();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            findViewById(R.id.login).setVisibility(View.GONE);
                            findViewById(R.id.loading_screen).setVisibility(View.GONE);
                            progressDialog.setMessage("Loading......");
                            progressDialog.show();
                        }
                    });
                    Util.saveData(mInstance, "username", username);
                    Util.saveData(mInstance, "password", password);
                    me = user;
                    Query noteQuery = new Query("user")
                            .equalTo("_id",me.getId());
                    publicDB.query(noteQuery, new RecordQueryResponseHandler() {
                        @Override
                        public void onQuerySuccess(Record[] records) {
                            Record myRecord = records[0];
                            myRecord.set("username", username);
                            publicDB.save(myRecord, new RecordSaveResponseHandler() {
                                @Override
                                public void onSaveSuccess(Record[] records) {
                                    getRecordsThread = new Thread(getRecords);
                                    getRecordsThread.start();
                                }

                                @Override
                                public void onPartiallySaveSuccess(Map<String, Record> successRecords, Map<String, Error> errors) {

                                }

                                @Override
                                public void onSaveFail(Error error) {
                                    getRecordsThread = new Thread(getRecords);
                                    getRecordsThread.start();
                                }
                            });
                        }

                        @Override
                        public void onQueryError(Error error) {

                        }
                    });
                }
                @Override
                public void onAuthFail(Error error) {
                    if (error.getCode().equals(Error.Code.DUPLICATED)) {
                        Toast.makeText(mInstance, "User already exists", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(mInstance, "Signup failed", Toast.LENGTH_SHORT).show();
                    }
                    progressDialog.dismiss();
                    Log.d("error", error.getCode().toString());
                }
            });
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar myToolbar = (Toolbar) findViewById(R.id.my_toolbar);
        setSupportActionBar(myToolbar);

        if (!Util.isNetworkConnected(getApplicationContext())) {
            Toast.makeText(getApplicationContext(), "No network connected", Toast.LENGTH_SHORT).show();
            finish();
        }

        if(Util.getData(this, "first_start_up") == null){
            Util.saveData(this, "first_start_up", "true");
            firstStartUp = true;
        } else {
            firstStartUp = false;
        }

        skygear = Container.defaultContainer(this);
        publicDB = skygear.getPublicDatabase();

        mInstance = this.getApplicationContext();

        progressDialog = new ProgressDialog(this);
        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progressDialog.setIndeterminate(false);

        recyclerView = (RecyclerView) findViewById(R.id.recycler_view);
        linearLayoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(linearLayoutManager);

        swipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.swiperefresh);
        loginScreen = (LinearLayout) findViewById(R.id.login);

        Configuration config = new Configuration.Builder()
                .endPoint(getResources().getString(R.string.skygear_endPoint))
                .apiKey(getResources().getString(R.string.skygear_apiKey))
                .build();

        skygear.configure(config);

        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                getRecordsThread = new Thread(getRecords);
                getRecordsThread.start();
            }
        });

        findViewById(R.id.singin).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                username = ((EditText) findViewById(R.id.username)).getText().toString();
                password = ((EditText) findViewById(R.id.password)).getText().toString();
                progressDialog.setMessage("Signing in......");
                progressDialog.show();
                skygearSigninThread = new Thread(skygearSignin);
                skygearSigninThread.start();
            }
        });
        findViewById(R.id.singup).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                username = ((EditText) findViewById(R.id.username)).getText().toString();
                password = ((EditText) findViewById(R.id.password)).getText().toString();
                progressDialog.setMessage("Signing up......");
                progressDialog.show();
                skygearSignupThread = new Thread(skygearSignup);
                skygearSignupThread.start();
            }
        });

        if (Util.getData(this, "username") == null) {
            findViewById(R.id.loading_screen).setVisibility(View.GONE);
            findViewById(R.id.login).setVisibility(View.VISIBLE);
            return;
        } else {
            findViewById(R.id.login).setVisibility(View.GONE);
            username = Util.getData(this, "username");
            password = Util.getData(this, "password");
            skygearSigninThread = new Thread(skygearSignin);
            skygearSigninThread.start();
        }
        Util.startUpAnimation(MainActivity.this);
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.edit_profile:
                editProfile();
                return true;
            case R.id.logout:
                AlertDialog.Builder mBuilder = new AlertDialog.Builder(new ContextThemeWrapper(this, R.style.myDialog));
                mBuilder.setTitle("Logout");
                mBuilder.setMessage("Are you sure you want to logout?");
                mBuilder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Util.logout(MainActivity.this);
                    }
                });
                mBuilder.setNegativeButton("No", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                });
                AlertDialog dialog = mBuilder.create();
                dialog.show();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void editProfile(){
        getRecordsThread = new Thread(getRecords);
        Intent intent = new Intent(this, EditProfile.class);
        startActivity(intent);
    }

    private void prepareProfileList(){
        ArrayList<String> name = new ArrayList<String>();
        ArrayList<String> single = new ArrayList<String>();
        ArrayList<String> email = new ArrayList<>();
        ArrayList<String> date = new ArrayList<>();
        ArrayList<String> hobby = new ArrayList<>();
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");

        for (Record record : savedRecords) {
            HashMap data = record.getData();
            if(data.containsKey("username")){
                name.add(data.get("username").toString());
                if(data.get("single")!=null){
                    Boolean singleBool = (Boolean) data.get("single");
                    if(singleBool){
                        single.add("yes");
                    } else {
                        single.add("no");
                    }
                } else {
                    single.add("N/A");
                }

                if(data.get("email")!=null){
                    email.add(data.get("email").toString());
                } else {
                    email.add("N/A");
                }

                if(record.getCreatedAt()!=null){
                    date.add(format.format(record.getCreatedAt()));
                } else {
                    date.add("N/A");
                }

                if(data.get("hobbies")!=null){
                    JSONObject hobbies = (JSONObject) data.get("hobbies");
                    try {
                        String s = null;
                        if(!hobbies.getString("1").equals("")){
                            s = hobbies.getString("1");
                        }
                        if(!hobbies.getString("2").equals("")){
                            s += ", "+hobbies.getString("2");
                        }
                        if(!hobbies.getString("3").equals("")){
                            s += ", "+hobbies.getString("3");
                        }
                        hobby.add(s);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                } else {
                    hobby.add("N/A");
                }
            } else {
                name.add("");
                single.add("N/A");
                email.add("N/A");
                date.add("N/A");
                hobby.add("N/A");
            }
        }

        final ProfileAdapter mAdapter = new ProfileAdapter(mInstance, name, single, email, date, hobby);
        recyclerView.setAdapter(mAdapter);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (progressDialog != null) {
                    progressDialog.dismiss();
                    swipeRefreshLayout.setRefreshing(false);
                }
            }
        });
        if(firstStartUp){
            firstStartUp = false;
            Toast.makeText(getApplicationContext(), "Please set up your personal profile", Toast.LENGTH_LONG).show();
            getRecordsThread = new Thread(getRecords);
            Intent intent = new Intent(this, EditProfile.class);
            startActivity(intent);
        }
    }
}
