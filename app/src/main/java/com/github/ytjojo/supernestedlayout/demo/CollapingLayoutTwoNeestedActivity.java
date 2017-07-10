package com.github.ytjojo.supernestedlayout.demo;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import com.github.ytjojo.supernestedlayout.CollapsingLayout;
import com.github.ytjojo.supernestedlayout.SuperNestedLayout;
import com.github.ytjojo.supernestedlayout.OnLoadListener;
import com.github.ytjojo.supernestedlayout.RefreshHeaderBehavior;

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
        final View headerLoadingView =findViewById(R.id.refreshHeader);
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
                },6000);
            }
        });

    }
}
