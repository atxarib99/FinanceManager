package com.arib.financemanager;

import android.app.ActionBar;
import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.preference.PreferenceManager;
import android.os.Bundle;
import android.widget.CompoundButton;
import android.widget.Switch;

public class SettingsActivity extends Activity {

    //On creation of the activity
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //set the color for the applicatiopn
        ActionBar bar = getActionBar();
        bar.setBackgroundDrawable(new ColorDrawable(Color.parseColor("#4E9455")));
        //set the view
        setContentView(R.layout.activity_settings);

        //get the preferences and set the switches to the proper values
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(SettingsActivity.this);
        Switch readSmsSwitch = (Switch) findViewById(R.id.settings_readSMSSwitch);
        readSmsSwitch.setChecked(prefs.getBoolean(getString(R.string.readsms_key), false));
        readSmsSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                SharedPreferences.Editor editor = prefs.edit();
                editor.putBoolean(getString(R.string.readsms_key), isChecked);
                editor.apply();
            }
        });

        Switch addStarSwitch = (Switch) findViewById(R.id.settings_addStarSwitch);
        addStarSwitch.setChecked(prefs.getBoolean(getString(R.string.addstar_key), false));
        addStarSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                SharedPreferences.Editor editor = prefs.edit();
                editor.putBoolean(getString(R.string.addstar_key), isChecked);
                editor.apply();
            }
        });

//        Switch backupToDriveSwitch = (Switch) findViewById(R.id.settings_backupToDriveSwitch);
//        backupToDriveSwitch.setChecked(prefs.getBoolean(getString(R.string.drive_key), false));
//        backupToDriveSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
//            @Override
//            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
//                SharedPreferences.Editor editor = prefs.edit();
//                editor.putBoolean(getString(R.string.drive_key), isChecked);
//                editor.apply();
//            }
//        });
    }


}
