package anonymous.com.mtcchoir.fragments;

import android.app.ProgressDialog;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.speech.tts.UtteranceProgressListener;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import anonymous.com.mtcchoir.R;
import anonymous.com.mtcchoir.Utils.AppController;
import anonymous.com.mtcchoir.activities.HomeActivity;
import anonymous.com.mtcchoir.adapters.Mp3Adapter;
import anonymous.com.mtcchoir.adapters.YoutubeAdapter;
import anonymous.com.mtcchoir.database.DatabaseHelper;
import anonymous.com.mtcchoir.models.Mp3Item;
import anonymous.com.mtcchoir.models.YoutubeItem;
import static anonymous.com.mtcchoir.activities.HomeActivity.*;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * to handle interaction events.
 * Use the {@link MP3Fragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class MP3Fragment extends Fragment {
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    private DatabaseHelper myDb;

    private List<Mp3Item> mp3Items = new ArrayList<>();

    private Mp3FragmentInteractionListener mp3FragmentInteractionListener;

    private Mp3Adapter mp3Adapter;
    private TextView mNoItemTextView;
    private TextView mNoInternetTextView;
    private ProgressBar mProgressBar;

    public MP3Fragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment MP3Fragment.
     */
    // TODO: Rename and change types and number of parameters
    public static MP3Fragment newInstance(String param1, String param2) {
        MP3Fragment fragment = new MP3Fragment();
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
        final View view = inflater.inflate(R.layout.fragment_mp3, container, false);

        mp3FragmentInteractionListener = (Mp3FragmentInteractionListener) view.getContext();
        myDb = new DatabaseHelper(view.getContext());
        mNoItemTextView = view.findViewById(R.id.text_mp3_no_item);
        mNoInternetTextView = view.findViewById(R.id.text_mp3_no_internet);
        mProgressBar = view.findViewById(R.id.progress_mp3);

        final RecyclerView recyclerView = view.findViewById(R.id.recycler_mp3);
        FloatingActionButton mUploadButton = view.findViewById(R.id.button_upload_mp3);
        mUploadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (AppController.isNetworkAvailable(view.getContext())) {
                    mp3FragmentInteractionListener.onAudioUpload();
                } else {
                    Toast.makeText(view.getContext(), "Please check your internet connection", Toast.LENGTH_SHORT).show();
                }
            }
        });
        LinearLayoutManager llm = new LinearLayoutManager(getActivity());
        llm.setOrientation(LinearLayoutManager.VERTICAL);
        recyclerView.setLayoutManager(llm);
        if (AppController.isNetworkAvailable(view.getContext())) {

            mProgressBar.setVisibility(View.VISIBLE);

            final FirebaseDatabase database = FirebaseDatabase.getInstance();
            DatabaseReference ref = database.getReference("Mp3");

            // Attach a listener to read the data at our posts reference
            ref.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                    for (DataSnapshot dsp : dataSnapshot.getChildren()) {
                        for (DataSnapshot dsp1 : dsp.getChildren()) {
                            Mp3Item mp3Item = dsp1.getValue(Mp3Item.class);
                            mp3Items.add(mp3Item);
                            myDb.insertData(mp3Item.getmSongName(), mp3Item.getmAddedUser(), 0, "");
                        }
                    }

                    for (Mp3Item mp3Item : myDb.getAllData()) {
                        if (!mp3Items.contains(mp3Item)) {

                            if(!myDb.getPath(mp3Item.getmSongName()).equals("")){
                                File file = new File(Uri.parse(myDb.getPath(mp3Item.getmSongName())).getPath());
                                if(file.exists()) file.delete();
                            }
                            myDb.deleteData(mp3Item.getmSongName());
                        }
                    }

                    mp3Items.clear();
                    mp3Items = myDb.getAllData();
                    if (mp3Items.size() > 0) {
                        mp3Adapter = new Mp3Adapter(mp3Items, view.getContext());
                        recyclerView.setAdapter(mp3Adapter);
                        recyclerView.setItemAnimator(new DefaultItemAnimator());
                        mp3Adapter.notifyDataSetChanged();
                    } else {
                        recyclerView.setVisibility(View.GONE);
                        mNoItemTextView.setVisibility(View.VISIBLE);
                        mNoInternetTextView.setVisibility(View.GONE);
                    }
                    mProgressBar.setVisibility(View.GONE);
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    System.out.println("The read failed: " + databaseError.getCode());
                }
            });
            ref.addChildEventListener(new ChildEventListener() {
                @Override
                public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

                }

                @Override
                public void onChildChanged(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

                }

                @Override
                public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) {
                    String songName = dataSnapshot.getKey();
                    if (myDb.getPath(songName).equals("")) {
                        File file = new File(Environment.getExternalStorageDirectory(), "/MTC/" + songName + ".mp3");
                        if (file.exists()) file.delete();
                    }
                    myDb.deleteData(songName);
                }

                @Override
                public void onChildMoved(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
            });
        } else {
            mp3Items = myDb.getAllData();
            if (mp3Items.size() > 0) {
                mp3Adapter = new Mp3Adapter(mp3Items, view.getContext());
                recyclerView.setAdapter(mp3Adapter);
                recyclerView.setItemAnimator(new DefaultItemAnimator());
                mp3Adapter.notifyDataSetChanged();
            } else {
                recyclerView.setVisibility(View.GONE);
                mNoItemTextView.setVisibility(View.VISIBLE);
                mNoInternetTextView.setVisibility(View.GONE);
            }
        }
        return view;
    }

    // TODO: Rename method, update argument and hook method into UI event

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

    }

    @Override
    public void onDetach() {
        super.onDetach();

    }

    /*@Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        if (isVisibleToUser && mp3Adapter!=null) {
            mp3Adapter.notifyDataSetChanged();
        }
    }*/

    @Override
    public void onResume() {

        super.onResume();
//        if(mp3Adapter!=null) mp3Adapter.notifyDataSetChanged();
    }

    public interface Mp3FragmentInteractionListener {

        void onAudioUpload();
    }
}
