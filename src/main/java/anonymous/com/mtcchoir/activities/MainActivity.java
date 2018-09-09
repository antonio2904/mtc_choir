package anonymous.com.mtcchoir.activities;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCanceledListener;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import anonymous.com.mtcchoir.R;
import anonymous.com.mtcchoir.Utils.AppController;
import anonymous.com.mtcchoir.adapters.Mp3Adapter;
import anonymous.com.mtcchoir.adapters.YoutubeAdapter;
import anonymous.com.mtcchoir.database.DatabaseHelper;
import anonymous.com.mtcchoir.fragments.HomeFragment;
import anonymous.com.mtcchoir.fragments.MP3Fragment;
import anonymous.com.mtcchoir.fragments.YoutubeFragment;
import anonymous.com.mtcchoir.models.Mp3Item;
import anonymous.com.mtcchoir.models.YoutubeItem;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, YoutubeAdapter.YoutubeAdapterInteractionListener, Mp3Adapter.Mp3AdapterInteractionListener, HomeFragment.OnFragmentInteractionListener, YoutubeFragment.YoutubeFragmentListener, MP3Fragment.Mp3FragmentInteractionListener {

    private ProgressDialog mProgressDialog;
    private DatabaseHelper myDb;
    private Handler mHandler;
    private Runnable mRunnable;
    private MediaPlayer mMediaPlayer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        myDb = new DatabaseHelper(this);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        Intent intent = getIntent();
        String action = intent.getAction();
        String type = intent.getType();

        if (Intent.ACTION_SEND.equals(action) && type != null) {
            if ("text/plain".equals(type)) {
                initUi(1); // Handle text being sent
                onUpload(intent.getStringExtra(Intent.EXTRA_TEXT));
            } else if (type.equals("audio/mpeg")) {
                initUi(0);
                upload((Uri) intent.getParcelableExtra(Intent.EXTRA_STREAM));
            }
        } else {
            initUi(0);
        }
    }

    private void initUi(int page) {

        getSupportFragmentManager().beginTransaction().replace(R.id.container_fragment, HomeFragment.newInstance("", "", page))
                .commit();
    }

    boolean doubleBackToExitPressedOnce = false;

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {

            if (doubleBackToExitPressedOnce) {
                if (mMediaPlayer != null && mMediaPlayer.isPlaying()) {
                    mMediaPlayer.stop();
                    mMediaPlayer.release();
                    mMediaPlayer = null;
                }
                super.onBackPressed();
                return;
            }

            this.doubleBackToExitPressedOnce = true;
            Toast.makeText(this, "Please click BACK again to exit", Toast.LENGTH_SHORT).show();

            new Handler().postDelayed(new Runnable() {

                @Override
                public void run() {
                    doubleBackToExitPressedOnce = false;
                }
            }, 2000);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        if (item.getItemId() == R.id.item_edit_username) {
            if (!AppController.isPlaying) initializeUser();
            else
                Toast.makeText(this, "Stop the player to change the name", Toast.LENGTH_SHORT).show();
        }
        return true;
    }

    private void initializeUser() {

        final AlertDialog.Builder alertDialog = new AlertDialog.Builder(MainActivity.this);
        alertDialog.setTitle("Change Name");
        alertDialog.setCancelable(false);

        LayoutInflater inflater = this.getLayoutInflater();
        final View dialogView = inflater.inflate(R.layout.layout_initialize_user, null);
        alertDialog.setView(dialogView);
        EditText et = dialogView.findViewById(R.id.et_username);
        et.setText(AppController.mUserName);
        alertDialog.setPositiveButton("OK",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        EditText name_et = dialogView.findViewById(R.id.et_username);
                        String name = name_et.getText().toString();
                        if (!name.equals("")) {
                            SharedPreferences sharedpreferences = getSharedPreferences("user_preferences", Context.MODE_PRIVATE);
                            SharedPreferences.Editor editor = sharedpreferences.edit();
                            editor.putString("user_name", name);
                            editor.apply();
                            AppController.mUserName = name;
                            AppController.hideKeyboardFrom(MainActivity.this, dialogView);
                            if (mMediaPlayer != null && mMediaPlayer.isPlaying()) {
                                mMediaPlayer.stop();
                                mMediaPlayer.release();
                                mMediaPlayer = null;
                            }
                            MainActivity.this.finish();
                            startActivity(new Intent(MainActivity.this, MainActivity.class));

                        } else {
                            Toast.makeText(MainActivity.this, "Please Enter a valid Name", Toast.LENGTH_SHORT).show();
                        }
                    }
                });

        alertDialog.show();
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_camera) {
            // Handle the camera action
        } else if (id == R.id.nav_gallery) {

        } else if (id == R.id.nav_slideshow) {

        } else if (id == R.id.nav_manage) {

        } else if (id == R.id.nav_share) {

        } else if (id == R.id.nav_send) {

        }

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    public void onFragmentInteraction(Uri uri) {

    }

    @Override
    public void onAudioUpload() {

        if (!AppController.isPlaying) {
            if (mHandler != null) mHandler.removeCallbacks(mRunnable);
            Intent intent;
            intent = new Intent();
            intent.setAction(Intent.ACTION_GET_CONTENT);
            intent.setType("audio/mpeg");
            startActivityForResult(intent, 1);
        } else {
            Toast.makeText(this, "Stop the player to upload", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1 && resultCode == Activity.RESULT_OK) {
            if ((data != null) && (data.getData() != null)) {

                upload(data.getData());
            }
        }
    }

    @Override
    public void onRefresh() {

    }

    @Override
    public void onUpload(String link) {
        final AlertDialog.Builder alertDialog = new AlertDialog.Builder(MainActivity.this);
        alertDialog.setTitle("Enter Details");
        alertDialog.setCancelable(false);

        LayoutInflater inflater = this.getLayoutInflater();
        final View dialogView = inflater.inflate(R.layout.layout_upload_youtube, null);
        alertDialog.setView(dialogView);
        if (!link.equals("")) {
            EditText et_link = dialogView.findViewById(R.id.et_link_youtube);
            et_link.setText(link);
            et_link.setEnabled(false);
        }

        alertDialog.setPositiveButton("UPLOAD",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        EditText name_et = dialogView.findViewById(R.id.et_name_youtube);
                        EditText link_et = dialogView.findViewById(R.id.et_link_youtube);
                        String name = name_et.getText().toString();
                        String link = link_et.getText().toString();
                        if (!name.equals("") && !link.equals("")) {

                            FirebaseDatabase database = FirebaseDatabase.getInstance();
                            DatabaseReference ref = database.getReference("youtube_link");

                            Map<String, YoutubeItem> youtubeItemMap = new HashMap<>();
                            YoutubeItem youtubeItem = new YoutubeItem();
                            youtubeItem.setmSongName(name);
                            youtubeItem.setmLink(link);
                            youtubeItem.setmAddedUser(AppController.mUserName);
                            youtubeItemMap.put(name, youtubeItem);

                            ref.child(name).setValue(youtubeItemMap);
                        }
                    }
                });

        alertDialog.setNegativeButton("CANCEL",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });

        alertDialog.show();
    }

    @Override
    public void onPlay(final Mp3Adapter.MyViewHolder holder, int position, String songName) {
        if (position != AppController.nowPlayingPosition) {

            mMediaPlayer = null;
            holder.mSeekBar.setProgress(0);
        }

        if (mMediaPlayer == null) {

            Uri uri;

            if (!myDb.getPath(songName).equals("")) {
                File file = new File("storage/emulated/0/" + myDb.getPath(songName));
                if (file.exists()) {
                    uri = Uri.parse("storage/emulated/0/" + myDb.getPath(songName));
                } else if (new File("storage/emulated/1/" + myDb.getPath(songName)).exists()) {
                    uri = Uri.parse("storage/emulated/1/" + myDb.getPath(songName));
                } else {
                    uri = Uri.parse("");
                }
            } else {
                uri = Uri.parse(new File(Environment.getExternalStorageDirectory() + "/MTC_CHOIR/" + songName + ".mp3").toString());
            }
            mMediaPlayer = MediaPlayer.create(MainActivity.this, uri);

        }
        mHandler = new Handler();
        mRunnable = new Runnable() {
            @Override
            public void run() {
                if (mMediaPlayer != null && mMediaPlayer.isPlaying()) {

                    holder.mSeekBar.setProgress(mMediaPlayer.getCurrentPosition());
                    holder.mSongStart.setText(AppController.formatMilliseconds(mMediaPlayer.getCurrentPosition()));

                }
                mHandler.postDelayed(this, 1000);

            }
        };
        try {

            mMediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) {
                    holder.mSeekBar.setVisibility(View.GONE);
                    holder.mSongEnd.setVisibility(View.GONE);
                    holder.mSongStart.setText("00:00");
                    AppController.isPlaying = false;
                    holder.mPlayImageView.setImageDrawable(getResources().getDrawable(R.drawable.ic_play));
                    if (mMediaPlayer != null) mMediaPlayer.release();
                    mMediaPlayer = null;
                    mHandler.removeCallbacks(mRunnable);
                    mRunnable = null;
                    mHandler = null;
                }
            });

            holder.mSongEnd.setVisibility(View.VISIBLE);
            holder.mSeekBar.setVisibility(View.VISIBLE);
            holder.mSeekBar.setMax(mMediaPlayer.getDuration());
            holder.mSongEnd.setText(AppController.formatMilliseconds(mMediaPlayer.getDuration()));

//        mMediaPlayer.seekTo(mp3Song.getmSeekBar().getProgress());
            mMediaPlayer.start();
            AppController.isPlaying = true;
            AppController.nowPlayingPosition = position;
            MainActivity.this.runOnUiThread(mRunnable);
        } catch (Exception e) {
            Toast.makeText(this, "Error", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onPauseClicked() {
        if (mMediaPlayer != null && mMediaPlayer.isPlaying()) {
            mMediaPlayer.pause();
            AppController.isPlaying = false;
            mHandler.removeCallbacks(mRunnable);
        }
    }

    @Override
    public void onSeek(int progress, SeekBar seekBar) {
        if (mMediaPlayer != null) {
            mMediaPlayer.seekTo(progress);
        }
    }

    @Override
    public void onDeleteAudio(final String songName) {
        if (AppController.isNetworkAvailable(MainActivity.this)) {

            if (!AppController.isPlaying) {

                final AlertDialog.Builder alertDialog = new AlertDialog.Builder(MainActivity.this);
                alertDialog.setMessage("Do you want to delete " + songName + " ?");
                alertDialog.setCancelable(false);

                alertDialog.setPositiveButton("DELETE",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {

                                delete(songName);

                            }
                        });

                alertDialog.setNegativeButton("CANCEL",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.cancel();
                            }
                        });

                alertDialog.show();
            } else {
                Toast.makeText(this, "Stop the player to delete", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(MainActivity.this, "Please check your internet connection", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onShareAudio(String songName) {
        if (!AppController.isPlaying) {
            Uri uri;
            if (!myDb.getPath(songName).equals("")) uri = Uri.parse(myDb.getPath(songName));
            else
                uri = Uri.parse(new File(Environment.getExternalStorageDirectory() + "/MTC_CHOIR/" + songName + ".mp3").toString());
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("audio/mpeg");
            intent.putExtra(Intent.EXTRA_STREAM, uri);
            startActivity(intent);
        } else {
            Toast.makeText(this, "Stop the player to share", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onLinkClick(String link) {
        if (!AppController.isPlaying) {

            AppController.watchYoutubeVideo(MainActivity.this, AppController.extractYTId(link));
        } else {

            Toast.makeText(this, "Please stop the player to continue", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onDelete(String songName) {
        FirebaseDatabase firebaseDatabase = FirebaseDatabase.getInstance();
        DatabaseReference ref = firebaseDatabase.getReference("youtube_link");
        ref.child(songName).setValue(null);
    }

    public void upload(final Uri data) {

        final AlertDialog.Builder alertDialog = new AlertDialog.Builder(MainActivity.this);
        alertDialog.setTitle("Enter Details");
        alertDialog.setCancelable(false);

        LayoutInflater inflater = this.getLayoutInflater();
        final View dialogView = inflater.inflate(R.layout.layout_upload_youtube, null);
        dialogView.findViewById(R.id.et_link_youtube).setVisibility(View.GONE);
        alertDialog.setView(dialogView);

        alertDialog.setPositiveButton("UPLOAD",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {

                        EditText name_et = dialogView.findViewById(R.id.et_name_youtube);
                        AppController.hideKeyboardFrom(MainActivity.this, dialogView);
                        mProgressDialog = new ProgressDialog(MainActivity.this);
                        mProgressDialog.setCancelable(false);
                        mProgressDialog.setTitle("Uploading");
                        mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                        mProgressDialog.setButton(DialogInterface.BUTTON_NEGATIVE, "Cancel", new Message());
                        mProgressDialog.show();
                        final String name = name_et.getText().toString();
                        if (!name.equals("")) {

                            // Now you can use that Uri to get the file path, or upload it, ...
                            FirebaseStorage firebaseStorage = FirebaseStorage.getInstance();
                            final StorageReference myRef = firebaseStorage.getReference("Mp3/" + name);

                            try {
                                InputStream iStream = getContentResolver().openInputStream(data);
                                byte[] inputData = getBytes(iStream);


                                final UploadTask uploadTask = myRef.putBytes(inputData);
                                uploadTask.addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                                    @Override
                                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                                        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference("Mp3");
                                        Map<String, Mp3Item> mp3ItemMap = new HashMap<>();
                                        Mp3Item mp3Item = new Mp3Item();
                                        mp3Item.setmAddedUser(AppController.mUserName);
                                        mp3Item.setmPath("");
                                        mp3Item.setmSongName(name);
                                        mp3ItemMap.put(name, mp3Item);
                                        databaseReference.child(name).setValue(mp3ItemMap);
                                        File file = new File(data.getPath());//create path from uri
                                        final String[] split = file.getPath().split(":");//split the path.
                                        String filePath;
                                        if (split.length > 1)
                                            filePath = split[1];//assign it to a string(your choice).
                                        else filePath = split[0];

                                        myDb.insertData(name, AppController.mUserName, 1, filePath);

                                        mProgressDialog.dismiss();
                                        mProgressDialog = null;
                                    }
                                }).addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
                                    @Override
                                    public void onProgress(UploadTask.TaskSnapshot taskSnapshot) {
                                        double progress = (double) taskSnapshot.getBytesTransferred() / (double) taskSnapshot.getTotalByteCount() * 100;
                                        mProgressDialog.setProgress((int) progress);
                                    }
                                }).addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        mProgressDialog.dismiss();
                                        Toast.makeText(MainActivity.this, "Failed", Toast.LENGTH_SHORT).show();
                                    }
                                }).addOnCanceledListener(new OnCanceledListener() {
                                    @Override
                                    public void onCanceled() {
                                        mProgressDialog.dismiss();
                                    }
                                });
                                mProgressDialog.getButton(DialogInterface.BUTTON_NEGATIVE).setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {
                                        uploadTask.cancel();
                                        Toast.makeText(MainActivity.this, "Upload Cancelled", Toast.LENGTH_SHORT).show();
                                        mProgressDialog.dismiss();
                                    }
                                });

                            } catch (Exception ignored) {
                            }
                        }
                    }
                });

        alertDialog.setNegativeButton("CANCEL",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });

        alertDialog.show();
    }

    public byte[] getBytes(InputStream inputStream) throws IOException {
        ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();
        int bufferSize = 1024;
        byte[] buffer = new byte[bufferSize];

        int len;
        while ((len = inputStream.read(buffer)) != -1) {
            byteBuffer.write(buffer, 0, len);
        }
        return byteBuffer.toByteArray();
    }

    public void delete(String songName) {

        DatabaseReference myRef = FirebaseDatabase.getInstance().getReference("Mp3");
        myRef.child(songName).setValue(null);

        mProgressDialog = new ProgressDialog(MainActivity.this);

        mProgressDialog.setTitle("Deleting");
        mProgressDialog.setMessage("Please wait");
        mProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        mProgressDialog.setCancelable(false);
        mProgressDialog.show();

        StorageReference storageReference = FirebaseStorage.getInstance().getReference("Mp3/" + songName);
        storageReference.delete().addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                mProgressDialog.dismiss();
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                mProgressDialog.dismiss();
                Toast.makeText(MainActivity.this, "Failed", Toast.LENGTH_SHORT).show();
            }
        });

        File file = new File(Environment.getExternalStorageDirectory() + "/MTC_CHOIR/" + songName + ".mp3");
        if (file.exists()) file.delete();
    }
}
