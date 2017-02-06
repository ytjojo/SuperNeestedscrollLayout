package com.ytjojo.viewlib.nestscrolllayout.demo;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;

import java.util.ArrayList;

/**
 * Created by Administrator on 2016/12/25 0025.
 */
public class ListActivity extends AppCompatActivity {
    ArrayList<String> mList;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list);
        ListView listView = (ListView) findViewById(R.id.listView);
        mList = new ArrayList<>();
        for(int i=0;i<30;i++){
            mList.add(""+i);

        }
        BaseAdapter  baseAdapter = new BaseAdapter() {
            @Override
            public int getCount() {
                return mList.size();
            }

            @Override
            public Object getItem(int position) {
                return mList.get(position);
            }

            @Override
            public long getItemId(int position) {
                return position;
            }

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                if(convertView ==null){
                    convertView  =LayoutInflater.from(parent.getContext()).inflate(R.layout.item_detail,parent,false);
                }
                return convertView;
            }
        };
        listView.setAdapter(baseAdapter);
    }

}
