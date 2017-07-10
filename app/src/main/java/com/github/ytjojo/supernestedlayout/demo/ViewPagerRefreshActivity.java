package com.github.ytjojo.supernestedlayout.demo;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.github.ytjojo.supernestedlayout.SuperNestedLayout;
import com.github.ytjojo.supernestedlayout.OnLoadListener;
import com.github.ytjojo.supernestedlayout.RefreshFooterBehavior;
import com.github.ytjojo.supernestedlayout.RefreshHeaderBehavior;

/**
 * Created by Administrator on 2017/5/23 0023.
 */

public class ViewPagerRefreshActivity extends AppCompatActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_viewpager_refresh);
        final View headerLoadingView =findViewById(R.id.refreshHeader);
        final View footererLoadingView =findViewById(R.id.refreshFooter);

        SuperNestedLayout.LayoutParams lp = (SuperNestedLayout.LayoutParams) headerLoadingView.getLayoutParams();
        SuperNestedLayout.LayoutParams footerLp = (SuperNestedLayout.LayoutParams) footererLoadingView.getLayoutParams();
        final RefreshFooterBehavior footerBehavior = (RefreshFooterBehavior) footerLp.getBehavior();
        footerBehavior.setOnLoadListener(new OnLoadListener() {
            @Override
            public void onLoad() {
                footererLoadingView.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        footerBehavior.setRefreshComplete();
                    }
                },10000);
            }
        });
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
        ViewPager viewPager = (ViewPager) findViewById(R.id.viewPager);
        viewPager.setAdapter(new PagerAdapter() {
            @Override
            public int getCount() {
                return 3;
            }

            @Override
            public boolean isViewFromObject(View view, Object object) {
                return view ==object;
            }

            @Override
            public Object instantiateItem(ViewGroup container, int position) {
                ImageView imageView =new ImageView(container.getContext());
                imageView.setImageResource(R.drawable.cheese_3);
                imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
                container.addView(imageView);

                return imageView;
            }

            @Override
            public void destroyItem(ViewGroup container, int position, Object object) {
                container.removeView((View) object);
            }
        });
    }
}
