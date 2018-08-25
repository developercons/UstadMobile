package com.ustadmobile.port.android.view;

import android.arch.lifecycle.LiveData;
import android.arch.paging.DataSource;
import android.arch.paging.LivePagedListBuilder;
import android.arch.paging.PagedList;
import android.arch.paging.PagedListAdapter;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.util.DiffUtil;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.toughra.ustadmobile.R;
import com.ustadmobile.core.controller.ClazzListPresenter;
import com.ustadmobile.core.db.UmProvider;
import com.ustadmobile.core.view.ClazzListView;
import com.ustadmobile.lib.db.entities.ClazzWithNumStudents;
import com.ustadmobile.port.android.util.UMAndroidUtil;

import java.util.ArrayList;

/**
 * ClazzListFragment Android fragment extends UstadBaseFragment
 */
public class ClazzListFragment extends UstadBaseFragment implements ClazzListView,
        View.OnClickListener, View.OnLongClickListener{

    private View rootContainer;

    //RecyclerView
    private RecyclerView mRecyclerView;
    private RecyclerView.LayoutManager mRecyclerLayoutManager;

    private Toolbar toolbar;

    private ClazzListPresenter mPresenter;


    protected class ClazzListRecyclerAdapter extends PagedListAdapter<ClazzWithNumStudents, ClazzListRecyclerAdapter.ClazzViewHolder> {

        protected class ClazzViewHolder extends RecyclerView.ViewHolder {

            protected ClazzViewHolder(View itemView) {
                super(itemView);
            }
        }

        protected ClazzListRecyclerAdapter(@NonNull DiffUtil.ItemCallback<ClazzWithNumStudents> diffCallback) {
            super(diffCallback);
        }

        @NonNull
        @Override
        public ClazzViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View clazzListItem = LayoutInflater.from(getContext()).inflate(R.layout.item_clazzlist_clazz,
                    parent, false);
            return new ClazzViewHolder(clazzListItem);
        }

        @Override
        public void onBindViewHolder(@NonNull ClazzViewHolder holder, int position) {
            ClazzWithNumStudents clazz = getItem(position);
            ((TextView)holder.itemView.findViewById(R.id.item_clazzlist_clazz_title))
                    .setText(clazz.getClazzName());
            ((TextView)holder.itemView.findViewById(R.id.item_clazzlist_numstudents_text))
                    .setText(clazz.getNumStudents() + " " + getResources()
                            .getText(R.string.students_literal).toString());
            holder.itemView.setOnClickListener((view) -> mPresenter.handleClickClazz(clazz));
        }
    }

    public static final DiffUtil.ItemCallback<ClazzWithNumStudents> DIFF_CALLBACK = new DiffUtil.ItemCallback<ClazzWithNumStudents>() {
        @Override
        public boolean areItemsTheSame(ClazzWithNumStudents oldItem, ClazzWithNumStudents newItem) {
            return oldItem.getClazzUid() == newItem.getClazzUid();
        }

        @Override
        public boolean areContentsTheSame(ClazzWithNumStudents oldItem, ClazzWithNumStudents newItem) {
            return oldItem.equals(newItem);
        }
    };

    /**
     * Generates a new Fragment for a page fragment
     * TODO: Add any args if needed
     *
     * @return A new instance of fragment ClazzListFragment.
     */
    public static ClazzListFragment newInstance() {
        ClazzListFragment fragment = new ClazzListFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    /**
     * On Create of the fragment.
     *
     * @param savedInstanceState
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    /**
     * On Create of the View fragment . Part of Android's Fragment Override
     *
     * @param inflater
     * @param container
     * @param savedInstanceState
     * @return the root container
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        rootContainer = inflater.inflate(R.layout.fragment_class_list, container, false);
        setHasOptionsMenu(true);


        mRecyclerView = rootContainer.findViewById(R.id.fragment_class_list_recyclerview);

        mRecyclerLayoutManager = new LinearLayoutManager(getContext());
        mRecyclerView.setLayoutManager(mRecyclerLayoutManager);
        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(mRecyclerView.getContext(),
                LinearLayoutManager.VERTICAL);


        //return container

        mPresenter = new ClazzListPresenter(this,
                UMAndroidUtil.bundleToHashtable(getArguments()), this);
        mPresenter.onCreate(UMAndroidUtil.bundleToHashtable(savedInstanceState));
        return rootContainer;
    }

    @Override
    public void setClazzListProvider(UmProvider<ClazzWithNumStudents> clazzListProvider) {
        ClazzListRecyclerAdapter recyclerAdapter = new ClazzListRecyclerAdapter(DIFF_CALLBACK);
        DataSource.Factory<Integer, ClazzWithNumStudents> factory =
                (DataSource.Factory<Integer, ClazzWithNumStudents>)clazzListProvider.getProvider();
        LiveData<PagedList<ClazzWithNumStudents>> data = new LivePagedListBuilder<>(factory, 20)
                .build();
        data.observe(this, recyclerAdapter::submitList);
        mRecyclerView.setAdapter(recyclerAdapter);
    }

    // This event is triggered soon after onCreateView().
    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        // Setup any handles to view objects here

    }

    @Override
    public void onClick(View v) {

    }

    @Override
    public boolean onLongClick(View v) {
        return false;
    }

}