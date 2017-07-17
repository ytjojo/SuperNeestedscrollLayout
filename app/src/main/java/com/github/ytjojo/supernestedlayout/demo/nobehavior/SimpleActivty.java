package com.github.ytjojo.supernestedlayout.demo.nobehavior;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import com.github.ytjojo.supernestedlayout.demo.R;

/**
 * Created by Administrator on 2017/7/14 0014.
 */

public class SimpleActivty extends AppCompatActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_simple);
        findViewById(R.id.toolbar).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent= new Intent(SimpleActivty.this,SimpleRefreshHeaderActivity.class);
                startActivity(intent);
            }
        });
    }
}
