package com.zte.blackmusic.fragment;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.zte.blackmusic.R;
import com.zte.blackmusic.activity.ModelActivity;
import com.zte.blackmusic.adapter.FolderAdapter;
import com.zte.blackmusic.database.DBManager;
import com.zte.blackmusic.entity.FolderInfo;
import com.zte.blackmusic.util.MyMusicUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by zte on 2017/3/9.
 */

public class FolderFragment extends Fragment {
    
    private static final String TAG = "FolderFragment";
    private RecyclerView recyclerView;
    private FolderAdapter adapter;
    private List<FolderInfo> folderInfoList = new ArrayList<>();
    private DBManager dbManager;
    private Context mContext;


    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        this.mContext = context;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View view = inflater.inflate(R.layout.fragment_singer,container,false);
        dbManager = DBManager.getInstance(getContext());
        recyclerView = (RecyclerView)view.findViewById(R.id.singer_recycler_view);
        adapter = new FolderAdapter(getContext(),folderInfoList);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getContext());
        linearLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        recyclerView.setLayoutManager(linearLayoutManager);
        recyclerView.setAdapter(adapter);
        adapter.setOnItemClickListener(new FolderAdapter.OnItemClickListener() {
            @Override
            public void onDeleteMenuClick(View content, int position) {

            }

            @Override
            public void onContentClick(View content, int position) {
                Intent intent = new Intent(mContext,ModelActivity.class);
                intent.putExtra(ModelActivity.KEY_TITLE,folderInfoList.get(position).getName());
                intent.putExtra(ModelActivity.KEY_TYPE, ModelActivity.FOLDER_TYPE);
                intent.putExtra(ModelActivity.KEY_PATH,folderInfoList.get(position).getPath());
                mContext.startActivity(intent);
            }
        });
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        folderInfoList.clear();
        folderInfoList.addAll(MyMusicUtil.groupByFolder((ArrayList)dbManager.getAllMusicFromMusicTable()));
        Log.d(TAG, "onResume: folderInfoList.size() = "+folderInfoList.size());
        adapter.notifyDataSetChanged();
    }
}
