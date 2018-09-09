package anonymous.com.mtcchoir.adapters;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Context;
import android.os.Environment;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageMetadata;
import com.google.firebase.storage.StorageReference;

import java.io.File;
import java.util.List;
import java.util.Objects;

import anonymous.com.mtcchoir.R;
import anonymous.com.mtcchoir.Utils.AppController;
import anonymous.com.mtcchoir.database.DatabaseHelper;
import anonymous.com.mtcchoir.models.Mp3Item;

public class Mp3Adapter extends RecyclerView.Adapter<Mp3Adapter.MyViewHolder> {


    private List<Mp3Item> itemList;
    private Context mContext;
    private Mp3Adapter.Mp3AdapterInteractionListener mp3AdapterInteractionListener;
    private DatabaseHelper myDb;
    private ProgressDialog mProgressDialog;
    private long mFreeSpace = 0;
    private long mFileSize = 0;

    /**
     * View holder class
     */
    public class MyViewHolder extends RecyclerView.ViewHolder {

        TextView mSongName;
        TextView mAddedUser;
        public ImageView mPlayImageView;
        public SeekBar mSeekBar;
        public TextView mSongStart;
        public TextView mSongEnd;
        ImageView mDeleteImageView;
        public ImageView mShareImageView;


        MyViewHolder(View view) {
            super(view);
            mSongName = view.findViewById(R.id.text_song_name_mp3);
            mPlayImageView = view.findViewById(R.id.image_play);
            mSeekBar = view.findViewById(R.id.seekbar_mp3);
            mSongStart = view.findViewById(R.id.text_song_start);
            mSongEnd = view.findViewById(R.id.text_song_end);
            myDb = new DatabaseHelper(view.getContext());
            mAddedUser = view.findViewById(R.id.text_added_user_mp3);
            mDeleteImageView = view.findViewById(R.id.image_delete_mp3);
            mShareImageView = view.findViewById(R.id.image_share);

            mp3AdapterInteractionListener = (Mp3AdapterInteractionListener) view.getContext();
        }
    }

    public Mp3Adapter(List<Mp3Item> itemList, Context mContext) {
        this.itemList = itemList;
        this.mContext = mContext;
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(@NonNull final Mp3Adapter.MyViewHolder holder, @SuppressLint("RecyclerView") final int position) {

        holder.mSongName.setText(String.valueOf(position + 1) + ". " + itemList.get(position).getmSongName());
        if (myDb.IsDownloaded(itemList.get(position).getmSongName()) == 0) {
            holder.mPlayImageView.setImageDrawable(mContext.getResources().getDrawable(R.drawable.ic_download));
            holder.mShareImageView.setVisibility(View.GONE);
        } else {
            holder.mPlayImageView.setImageDrawable(mContext.getResources().getDrawable(R.drawable.ic_play));
        }
        if (itemList.get(position).getmAddedUser().equals(AppController.mUserName) || AppController.mUserName.equals("antonio2904")) {
            holder.mDeleteImageView.setVisibility(View.VISIBLE);
        }
        holder.mDeleteImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mp3AdapterInteractionListener.onDeleteAudio(itemList.get(position).getmSongName());
            }
        });
        holder.mSongStart.setText("00:00");
        holder.mSongEnd.setText("00:00");
        holder.mAddedUser.setText("Added By : " + itemList.get(position).getmAddedUser());
        holder.mPlayImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {

                holder.mSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                    @Override
                    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

                        if (fromUser  && position == AppController.nowPlayingPosition) {
                            mp3AdapterInteractionListener.onSeek(progress, seekBar);
                        }
                    }

                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {

                    }

                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {

                    }
                });

                if (!AppController.isPlaying || position == AppController.nowPlayingPosition) {

                    if (Objects.equals(holder.mPlayImageView.getDrawable().getConstantState(), mContext.getResources().getDrawable(R.drawable.ic_play).getConstantState())) {
                        holder.mPlayImageView.setImageDrawable(mContext.getResources().getDrawable(R.drawable.ic_pause));
                        mp3AdapterInteractionListener.onPlay(holder,position, itemList.get(position).getmSongName());
                    } else if (Objects.equals(holder.mPlayImageView.getDrawable().getConstantState(), mContext.getResources().getDrawable(R.drawable.ic_pause).getConstantState())) {
                        holder.mPlayImageView.setImageDrawable(mContext.getResources().getDrawable(R.drawable.ic_play));
                        mp3AdapterInteractionListener.onPauseClicked();
                    } else {

                        if (AppController.isNetworkAvailable(v.getContext())) {

                            mProgressDialog = new ProgressDialog(v.getContext());
                            mProgressDialog.setCancelable(false);
                            mProgressDialog.setTitle("Downloading");
                            mProgressDialog.setButton(ProgressDialog.BUTTON_NEGATIVE, "CANCEL", new Message());
                            mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);

                            StorageReference storageRef = FirebaseStorage.getInstance().getReference("Mp3/" + itemList.get(position).getmSongName());

                            File localFile1 = new File(Environment.getExternalStorageDirectory(), "MTC_CHOIR");
                            if (!localFile1.exists()) {
                                localFile1.mkdir();
                            }

                            mFreeSpace = localFile1.getFreeSpace();

                            storageRef.getMetadata().addOnSuccessListener(new OnSuccessListener<StorageMetadata>() {
                                @Override
                                public void onSuccess(StorageMetadata storageMetadata) {
                                    mFileSize = storageMetadata.getSizeBytes();
                                }
                            });

                            if (mFreeSpace > mFileSize) {

                                File localFile = new File(Environment.getExternalStorageDirectory(), "/MTC_CHOIR/" + itemList.get(position).getmSongName() + ".mp3");

                                if (!localFile.exists()) {

                                    mProgressDialog.show();

                                    final FileDownloadTask fileDownloadTask = storageRef.getFile(localFile);
                                    fileDownloadTask.addOnSuccessListener(new OnSuccessListener<FileDownloadTask.TaskSnapshot>() {
                                        @Override
                                        public void onSuccess(FileDownloadTask.TaskSnapshot taskSnapshot) {
                                            // Local temp file has been created
                                            mProgressDialog.dismiss();
                                            mProgressDialog = null;
                                            myDb.updateData(itemList.get(position).getmSongName(), 1);
                                            holder.mPlayImageView.setImageDrawable(v.getResources().getDrawable(R.drawable.ic_play));
                                            holder.mShareImageView.setVisibility(View.VISIBLE);
                                        }
                                    }).addOnFailureListener(new OnFailureListener() {
                                        @Override
                                        public void onFailure(@NonNull Exception exception) {
                                            // Handle any errors
                                            Toast.makeText(v.getContext(), "Failed", Toast.LENGTH_SHORT).show();
                                            mProgressDialog.dismiss();
                                        }
                                    }).addOnProgressListener(new OnProgressListener<FileDownloadTask.TaskSnapshot>() {
                                        @Override
                                        public void onProgress(FileDownloadTask.TaskSnapshot taskSnapshot) {
                                            double progress = (double) taskSnapshot.getBytesTransferred() / (double) taskSnapshot.getTotalByteCount() * 100;
                                            mProgressDialog.setProgress((int) progress);
                                        }
                                    });
                                    mProgressDialog.getButton(ProgressDialog.BUTTON_NEGATIVE).setOnClickListener(new View.OnClickListener() {
                                        @Override
                                        public void onClick(View v) {
                                            fileDownloadTask.cancel();
                                            Toast.makeText(v.getContext(), "Download Cancelled", Toast.LENGTH_SHORT).show();
                                            mProgressDialog.dismiss();
                                        }
                                    });
                                }
                                else{
                                    myDb.updateData(itemList.get(position).getmSongName(), 1);
                                    holder.mPlayImageView.setImageDrawable(v.getResources().getDrawable(R.drawable.ic_play));
                                    holder.mShareImageView.setVisibility(View.VISIBLE);
                                }
                            }
                        } else {
                            Toast.makeText(mContext, "Free up some space to continue", Toast.LENGTH_SHORT).show();
                        }

                    }
                }
            }
        });
        holder.mShareImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mp3AdapterInteractionListener.onShareAudio(itemList.get(position).getmSongName());
            }
        });

    }

    @Override
    public int getItemCount() {
        return itemList.size();
    }

    @NonNull
    @Override
    public Mp3Adapter.MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_mp3_fragment, parent, false);


        return new Mp3Adapter.MyViewHolder(v);
    }

    public interface Mp3AdapterInteractionListener {

        void onPlay(Mp3Adapter.MyViewHolder holder, int position, String songName);

        void onPauseClicked();

        void onSeek(int progress, SeekBar seekBar);

        void onDeleteAudio(String songName);

        void onShareAudio(String songName);
    }
}
