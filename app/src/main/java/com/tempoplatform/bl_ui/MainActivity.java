package com.tempoplatform.bl_ui;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import android.widget.Button;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {
    private Button mainButton;
    private TextView mainLabel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        SetupUIElements();
    }

    private void SetupUIElements() {

        mainButton = findViewById(R.id.mainButton);
        mainButton.setOnClickListener(view -> Utils.Shout("Can read SDK: " + Constants.SomeString1));
    }
}