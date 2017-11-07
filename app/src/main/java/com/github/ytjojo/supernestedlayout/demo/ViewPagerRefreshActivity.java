package com.github.ytjojo.supernestedlayout.demo;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.github.ytjojo.supernestedlayout.OnLoadListener;
import com.github.ytjojo.supernestedlayout.RefreshFooterBehavior;
import com.github.ytjojo.supernestedlayout.RefreshHeaderBehavior;
import com.github.ytjojo.supernestedlayout.SuperNestedLayout;

/**
 * Created by Administrator on 2017/5/23 0023.
 */

public class ViewPagerRefreshActivity extends AppCompatActivity {
    TextView mTvPosition;
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
                },2000);
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
                },2000);
            }
        });
        mTvPosition = (TextView) findViewById(R.id.tv_position);
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
        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                mTvPosition.setText("positoon"+position);
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });
    }
}
