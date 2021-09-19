package com.github.onetimepass;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

/**
 * Simple activity to give us a splash background when the app is first launched.
 */
public class SplashActivity extends AppCompatActivity {

    /**
     * Activity constructor method. Simply hand-off the extras and data to MainController.
     *
     * @param savedInstanceState
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = new Intent(this, MainController.class);
        Bundle extras = getIntent().getExtras();
        if (extras != null)
            intent.putExtras(extras);
        Uri data = getIntent().getData();
        if (data != null)
            intent.setData(data);
        startActivity(intent);
        finish();
    }

}
