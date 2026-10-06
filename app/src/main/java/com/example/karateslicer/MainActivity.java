package com.example.karateslicer;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

/**
 * Legacy entry point — redirects to GameActivity (spec §12: GameActivity replaces MainActivity).
 * Kept so any saved launcher shortcuts continue to work.
 */
public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        startActivity(new Intent(this, GameActivity.class));
        finish();
    }
}
