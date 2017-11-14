package com.dockysoft.alias;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

public class HelpActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_help);
        getSupportActionBar().setTitle("Welcome to Alias");

        findViewById(R.id.continueButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                gotoChatRooms();
            }
        });
    }

    private void gotoChatRooms(){
        Intent intent = new Intent(this, RoomsActivity.class);
        startActivity(intent);
    }
}
