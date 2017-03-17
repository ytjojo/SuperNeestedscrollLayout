package com.ytjojo.viewlib.nestedscrolllayout.demo;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;

import com.ytjojo.viewlib.nestedsrolllayout.CollapsingLayout;

/**
 * Created by Administrator on 2017/3/4 0004.
 */

public class CollapingLayoutTwoNeestedActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_collapinglayout_two_nest);
        CollapsingLayout layout = (CollapsingLayout) findViewById(R.id.collapsingLayout);
        layout.setTitle("这是详情");

    }
}
