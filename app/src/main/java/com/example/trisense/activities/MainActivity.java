package com.example.trisense.activities;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.example.trisense.R;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class MainActivity extends AppCompatActivity {

    DatabaseReference database;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        database = FirebaseDatabase.getInstance().getReference();

        database.child("messaggio").setValue("Ciao Firebase!");
    }
}
