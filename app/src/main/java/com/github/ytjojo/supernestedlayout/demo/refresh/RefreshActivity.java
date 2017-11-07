package com.github.ytjojo.supernestedlayout.demo.refresh;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;

/**
 * Created by Administrator on 2017/11/7 0007.
 */

public class RefreshActivity extends AppCompatActivity{


    public static void startActivity(Context context,int type){
        Intent intent =new Intent(context,RefreshActivity.class);
        intent.putExtra("type",type);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        int type = getIntent().getIntExtra("type",0);

    }
}
