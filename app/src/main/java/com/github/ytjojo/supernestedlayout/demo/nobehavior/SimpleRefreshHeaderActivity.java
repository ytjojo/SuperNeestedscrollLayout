package com.github.ytjojo.supernestedlayout.demo.nobehavior;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import com.github.ytjojo.supernestedlayout.OnLoadListener;
import com.github.ytjojo.supernestedlayout.RefreshHeaderBehavior;
import com.github.ytjojo.supernestedlayout.SuperNestedLayout;
import com.github.ytjojo.supernestedlayout.demo.R;

/**
 * Created by Administrator on 2017/7/14 0014.
 */

public class SimpleRefreshHeaderActivity extends AppCompatActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_simple_refreshheader);
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
                },3000);
            }
        });;
    }
}
