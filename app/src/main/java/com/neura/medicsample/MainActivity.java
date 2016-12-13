package com.neura.medicsample;

import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

import com.neura.medicationaddon.NeuraManager;
import com.neura.standalonesdk.util.SDKUtils;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final View loginButton = findViewById(R.id.login_with_neura);
        final View simulateEventButton = findViewById(R.id.simulate_event);

        NeuraManager.getInstance().initNeuraConnection(getApplicationContext(),
                getString(R.string.app_uid), getString(R.string.app_secret));

        if (SDKUtils.isConnected(this, NeuraManager.getInstance().getNeuraClient())) {
            loginButton.setVisibility(View.GONE);
        } else {
            simulateEventButton.setVisibility(View.GONE);
        }

        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                NeuraManager.getInstance().authenticateWithNeura(MainActivity.this, new Handler.Callback() {
                    @Override
                    public boolean handleMessage(Message message) {
                        if (message.arg1 == 1) {
                            loginButton.setVisibility(View.GONE);
                            simulateEventButton.setVisibility(View.VISIBLE);
                        }
                        return false;
                    }
                });
            }
        });

        simulateEventButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                NeuraManager.getInstance().getNeuraClient().simulateAnEvent();
            }
        });
    }


}

