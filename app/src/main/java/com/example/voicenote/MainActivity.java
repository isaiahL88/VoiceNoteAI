package com.example.voicenote;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class MainActivity extends AppCompatActivity {

    private Button recordButton, myNotesButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        recordButton = findViewById(R.id.recordButton);
        myNotesButton = findViewById(R.id.myNotesButton);
        recordButton.setOnClickListener(this::onRecord);
        myNotesButton.setOnClickListener(this::onMyNotes);
    }

    public void onMyNotes(View v){
        Intent i = new Intent(this, MyNotes.class);
        startActivity(i);
    }

    public void onRecord(View v){
        Intent i = new Intent(this, RecordNotes.class);
        startActivity(i);
    }
}