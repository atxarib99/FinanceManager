package com.example.arib.financemanager;

import android.app.ActionBar;
import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.CompoundButton;
import android.widget.Switch;

public class SettingsActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActionBar bar = getActionBar();
        bar.setBackgroundDrawable(new ColorDrawable(Color.parseColor("#4E9455")));
        setContentView(R.layout.activity_settings);
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
    }


}
