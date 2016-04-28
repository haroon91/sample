package com.example.haroon.sample.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.example.haroon.sample.R;
import com.example.haroon.sample.cache.DataManager;

public class MainActivity extends MyBaseActivity implements View.OnClickListener {



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button sampleQuestion = (Button) findViewById(R.id.bt_question);
        Button sampleUser = (Button) findViewById(R.id.bt_user);

        sampleQuestion.setOnClickListener(this);
        sampleUser.setOnClickListener(this);

        DataManager.initCachedDb(this.getApplicationContext());
    }


    @Override
    public void onClick(View v) {
        switch (v.getId()) {

            case R.id.bt_question:
                Intent intent = new Intent(this, QuestionActivity.class);
                startActivity(intent);
                break;

            case R.id.bt_user:
                intent = new Intent(this, UserActivity.class);
                startActivity(intent);
                break;
        }
    }

}
