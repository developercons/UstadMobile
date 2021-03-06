package com.ustadmobile.port.android.view;


import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.github.clans.fab.FloatingActionMenu;
import com.toughra.ustadmobile.R;
import com.ustadmobile.core.model.ListableEntity;

import java.util.List;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link EntityListFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class EntityListFragment extends UstadBaseFragment {

    protected EntityCardAdapter mAdapter;

    protected RecyclerView mRecyclerView;

    protected RecyclerView.LayoutManager mLayoutManager;

    protected FloatingActionMenu floatingActionMenu;

    protected SwipeRefreshLayout refreshLayout;

    private List<? extends ListableEntity> list;


    public EntityListFragment() {
        // Required empty public constructor
    }



    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.fragment_entity_list, container, false);
        mRecyclerView = (RecyclerView)rootView.findViewById(R.id.entity_fragment_recyclerview);
        mLayoutManager = new LinearLayoutManager(getContext());
        mRecyclerView.setLayoutManager(mLayoutManager);
        floatingActionMenu = (FloatingActionMenu)rootView.findViewById(R.id.entity_fragment_floating_action_menu);
        refreshLayout = (SwipeRefreshLayout)rootView.findViewById(R.id.entity_fragment_refreshlayout);
        refreshLayout.setEnabled(false);

        checkListUpdate();
        return rootView;
    }

    public void setEntityList(List<? extends ListableEntity> list)  {
        mAdapter = new EntityCardAdapter(list);
        checkListUpdate();
    }

    private void checkListUpdate() {
        if(mRecyclerView != null && mAdapter != null && getActivity() != null) {
            getActivity().runOnUiThread(new Runnable() {
                public void run() {
                    mRecyclerView.setAdapter(mAdapter);
                }
            });
        }
    }

    /**
     * Gets the floating action menu
     *
     * @return
     */
    protected FloatingActionMenu getFloatingActionMenu() {
        return floatingActionMenu;
    }

    @Override
    public boolean canGoBack() {
        return floatingActionMenu.isOpened();
    }

    @Override
    public void goBack() {
        floatingActionMenu.close(true);
    }
}
