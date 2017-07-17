package com.github.ytjojo.supernestedlayout.demo;

import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.GridLayout;

import com.github.ytjojo.supernestedlayout.BottomSheetDialog;

/**
 * Created by Administrator on 2017/7/12 0012.
 */

public class BottomSheetDialogActivity extends AppCompatActivity {
    BottomSheetDialog bottomSheetDialog;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bottomsheetdialog);
        WebView webView = (WebView) findViewById(R.id.webview);
        findViewById(R.id.toolbar).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(bottomSheetDialog !=null){
                    bottomSheetDialog.getBehavior().getState();
                }
            }
        });
        webView.setWebViewClient(new WebViewClient(){
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                view.loadUrl(url);
                return true;
            }
        });
//        webView.loadUrl("https://github.com/jeasonlzy/okhttp-OkGo");
        webView.loadUrl("http://www.jianshu.com/p/7caa5f4f49bd");
        GridLayout gridLayout = (GridLayout) findViewById(R.id.gridlayout);
        gridLayout.getChildAt(0).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                BottomSheetDialog dialog =  new BottomSheetDialog(BottomSheetDialogActivity.this);
                dialog.setContentView(R.layout.dialog_collapsinglayout);
                dialog.show();
            }
        });
        gridLayout.getChildAt(1).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                BottomSheetDialog dialog =  new BottomSheetDialog(BottomSheetDialogActivity.this);
                dialog.setContentView(R.layout.dialog_bottomsheet);
                dialog.show();
            }
        });
        gridLayout.getChildAt(2).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                BottomSheetDialog dialog =  new BottomSheetDialog(BottomSheetDialogActivity.this);
                dialog.setContentView(R.layout.dialog_header_scroll_bottomsheet);
                dialog.show();
            }
        });
        gridLayout.getChildAt(3).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                BottomSheetDialog dialog =  new BottomSheetDialog(BottomSheetDialogActivity.this);
                dialog.setContentView(R.layout.dialog_header_scroll_bottomsheet_1);
                dialog.show();
            }
        });
        gridLayout.getChildAt(4).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                BottomSheetDialog dialog =  new BottomSheetDialog(BottomSheetDialogActivity.this);
                dialog.setContentView(R.layout.dialog_header_scroll_bottomsheet_2);
                dialog.show();
            }
        });
        gridLayout.getChildAt(5).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                BottomSheetDialog dialog =  new BottomSheetDialog(BottomSheetDialogActivity.this);
                dialog.setContentView(R.layout.dialog_header_scroll_bottomsheet_3);
                dialog.show();
            }
        });
        gridLayout.getChildAt(6).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                BottomSheetDialog dialog =  new BottomSheetDialog(BottomSheetDialogActivity.this);
                dialog.setContentView(R.layout.dialog_header_scroll_bottomsheet_4);
                dialog.show();
            }
        });
        gridLayout.getChildAt(7).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final BottomSheetDialog dialog =  new BottomSheetDialog(BottomSheetDialogActivity.this);
                dialog.setContentView(R.layout.dialog_header_scroll_bottomsheet_5);
                dialog.show();
                bottomSheetDialog = dialog;
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        dialog.findViewById(R.id.rootview).setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                if(bottomSheetDialog !=null){
                                    bottomSheetDialog.getBehavior().getState();
                                }
                            }
                        });
                    }
                },1000);
            }
        });
        gridLayout.getChildAt(8).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                BottomSheetDialog dialog =  new BottomSheetDialog(BottomSheetDialogActivity.this);
                dialog.setContentView(R.layout.dialog_header_scroll_bottomsheet_6);
                dialog.show();
            }
        });


    }
}
