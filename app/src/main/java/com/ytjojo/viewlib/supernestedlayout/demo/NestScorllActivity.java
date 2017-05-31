package com.ytjojo.viewlib.supernestedlayout.demo;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;

import com.ytjojo.viewlib.supernestedlayout.SuperNestedLayout;
import com.ytjojo.viewlib.supernestedlayout.OnLoadListener;
import com.ytjojo.viewlib.supernestedlayout.RefreshHeaderBehavior;

/**
 * Created by Administrator on 2016/12/25 0025.
 */
public class NestScorllActivity extends AppCompatActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nestedsrollview);
        final RefreshHeaderLoadingView headerLoadingView = (RefreshHeaderLoadingView) findViewById(R.id.refreshHeader);
        SuperNestedLayout.LayoutParams lp = (SuperNestedLayout.LayoutParams) headerLoadingView.getLayoutParams();
        final RefreshHeaderBehavior headerBehavior = (RefreshHeaderBehavior) lp.getBehavior();
        headerBehavior.setOnLoadListener(new OnLoadListener() {
            @Override
            public void onLoad() {
                headerLoadingView.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        headerBehavior.setRefreshComplete();
                    }
                },40000);
            }
        });




    }
}