package com.kikyou.kikoplay;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import com.kikyou.kikoplay.module.AsyncCallBack;
import com.kikyou.kikoplay.module.DanmuPool;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link DanmuFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link DanmuFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class DanmuFragment extends Fragment {

    private OnFragmentInteractionListener mListener;
    private ListView sourcesView;
    private Button updateButton;
    private TextView danmuInfoTextView;
    private DanmuPool danmuPool;
    public DanmuFragment() {
        // Required empty public constructor
    }


    public static DanmuFragment newInstance() {
        return new DanmuFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View root = inflater.inflate(R.layout.fragment_danmu, container, false);
        sourcesView=root.findViewById(R.id.curSourceList);
        updateButton=root.findViewById(R.id.updateDanmu);
        danmuInfoTextView=root.findViewById(R.id.danmuCountView);
        return root;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        sourcesView.setAdapter(danmuPool);
        updateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updateButton.setEnabled(false);
                sourcesView.setEnabled(false);
                updateButton.setText(getText(R.string.danmu_updating));
                danmuPool.updateDanmu(new AsyncCallBack() {
                    @Override
                    public void onResponse(int state) {
                        updateButton.setEnabled(true);
                        sourcesView.setEnabled(true);
                        updateButton.setText(getText(R.string.danmu_update));
                        updateCountInfo();
                        mListener.showUpdateStatus(state);
                    }
                });
            }
        });
    }
    public void setAdapter(DanmuPool danmuPool){
        this.danmuPool=danmuPool;
    }
    public void updateCountInfo(){
        danmuInfoTextView.setText(String.format(getString(R.string.danmu_count_desc), danmuPool.getDanmuCount()));
    }
    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        void showUpdateStatus(int state);
    }
}
