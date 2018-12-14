package com.example.eventbusdemo;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.example.eventbusdemo.event.Subscribe;
import com.example.eventbusdemo.event.ThreadMode;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(){

    }
}
