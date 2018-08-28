
package anonymous.com.mtcchoir.fragments;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.DragAndDropPermissions;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import anonymous.com.mtcchoir.R;
import anonymous.com.mtcchoir.Utils.AppController;
import anonymous.com.mtcchoir.adapters.YoutubeAdapter;
import anonymous.com.mtcchoir.models.YoutubeItem;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * to handle interaction events.
 * Use the {@link YoutubeFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class YoutubeFragment extends Fragment {
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    private List<YoutubeItem> youtubeItems = new ArrayList<>();

    private YoutubeFragmentListener mYoutubeFragmentListener;
    private YoutubeAdapter youtubeAdapter;


    public YoutubeFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment YoutubeFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static YoutubeFragment newInstance(String param1, String param2) {
        YoutubeFragment fragment = new YoutubeFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        final View view = inflater.inflate(R.layout.fragment_youtube, container, false);
        final SwipeRefreshLayout swipeRefreshLayout = view.findViewById(R.id.layout_swipeRefresh_youtube);
        final FloatingActionButton floatingActionButton = view.findViewById(R.id.button_upload);
        final TextView mNoItemTextView = view.findViewById(R.id.text_youtube_no_item);
        final TextView mNoInternetTextView = view.findViewById(R.id.text_youtube_no_internet);
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                mYoutubeFragmentListener.onRefresh();
                if (swipeRefreshLayout.isRefreshing()) {

                    swipeRefreshLayout.setRefreshing(false);

                }
            }
        });

        if (AppController.isNetworkAvailable(view.getContext())) {

            floatingActionButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (AppController.isNetworkAvailable(view.getContext())) {
                        mYoutubeFragmentListener.onUpload("");
                    } else {

                        Toast.makeText(view.getContext(), "Please check your internet connection", Toast.LENGTH_SHORT).show();

                    }
                }
            });

            final RecyclerView recyclerView = view.findViewById(R.id.recycler_youtube);
            LinearLayoutManager llm = new LinearLayoutManager(getActivity());
            llm.setOrientation(LinearLayoutManager.VERTICAL);
            recyclerView.setLayoutManager(llm);

            final FirebaseDatabase database = FirebaseDatabase.getInstance();
            DatabaseReference ref = database.getReference("youtube_link");

            // Attach a listener to read the data at our posts reference
            ref.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                    youtubeItems.clear();
                    for (DataSnapshot dsp : dataSnapshot.getChildren()) {
                        for (DataSnapshot dsp1 : dsp.getChildren()) {
                            Log.d("Youtube", dsp1.getValue().toString());
                            youtubeItems.add(dsp1.getValue(YoutubeItem.class));
                        }
                    }
                    if (youtubeItems.size() > 0) {
                        youtubeAdapter = new YoutubeAdapter(youtubeItems);
                        recyclerView.setAdapter(youtubeAdapter);
                        recyclerView.setItemAnimator(new DefaultItemAnimator());
                        youtubeAdapter.notifyDataSetChanged();
                    } else {
                        recyclerView.setVisibility(View.GONE);
                        mNoItemTextView.setVisibility(View.VISIBLE);
                        mNoInternetTextView.setVisibility(View.GONE);
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    System.out.println("The read failed: " + databaseError.getCode());
                }
            });
        } else {
            mNoInternetTextView.setVisibility(View.VISIBLE);
        }

        return view;
    }

    // TODO: Rename method, update argument and hook method into UI event

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mYoutubeFragmentListener = (YoutubeFragmentListener) context;
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    public interface YoutubeFragmentListener {

        void onRefresh();

        void onUpload(String link);

    }
}
