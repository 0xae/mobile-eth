package com.dk.ethereumwallet;

import android.content.SharedPreferences;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.math.BigDecimal;

public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        SharedPreferences settings = getSharedPreferences(MainActivity.PREFS_NAME, 0);

        String miner = settings.getString("miner", getString(R.string.default_net));
        ((TextView)findViewById(R.id.miner_address))
                .setText(miner);

        findViewById(R.id.save_settings)
        .setOnClickListener(view -> {
            TextView miner_address = (TextView)findViewById(R.id.miner_address);

            String address;
            if (miner_address.getText() != null && !miner_address.getText().toString().isEmpty()) {
                settings.edit()
                        .putString("miner", miner_address.getText().toString())
                        .commit();
            }

            Snackbar.make(view, "Guardado com sucesso", Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show();
        });
    }
}
