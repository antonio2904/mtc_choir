package anonymous.com.mtcchoir.activities;

import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.StrictMode;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.File;
import java.io.InputStream;
import java.security.spec.EncodedKeySpec;
import java.util.HashMap;
import java.util.Map;

import anonymous.com.mtcchoir.R;
import anonymous.com.mtcchoir.Utils.AppController;
import anonymous.com.mtcchoir.models.Mp3Item;

public class SplashActivity extends Activity {

    // Splash screen timer
    private static int SPLASH_TIME_OUT = 3000;
    private int mVersionCode = 0;
    private int latest_verion_code = 0;
    private boolean is_under_maintenance = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);


        new Handler().postDelayed(new Runnable() {

            /*
             * Showing splash screen with a timer. This will be useful when you
             * want to show case your app logo / company
             */

            @Override
            public void run() {
                // This method will be executed once the timer is over
                // Start your app main activity

                File apk = new File(Environment.getExternalStorageDirectory(),"/MTC_CHOIR/mtc_choir.apk");
                try {
                    if (apk.exists()) apk.delete();
                }catch(Exception e){e.printStackTrace();}

                SharedPreferences sharedpreferences = getSharedPreferences("user_preferences", Context.MODE_PRIVATE);
                if (!sharedpreferences.contains("user_name")) {
                    initializeUser(sharedpreferences);
                } else {
                    AppController.mUserName = sharedpreferences.getString("user_name", "");
                    request_permission();
                }
            }
        }, SPLASH_TIME_OUT);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions, @NonNull int[] grantResults) {

        switch (requestCode) {
            case 1:
                if (grantResults.length > 1) {

                    update();

                } else {
                    Toast.makeText(SplashActivity.this, "Please Allow Permissions", Toast.LENGTH_SHORT).show();
                    finish();
                }
                break;
        }
    }

    private void redirectStore(String updateUrl) {
//        final Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(updateUrl));
//        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//        startActivity(intent);
        final File file = new File(Environment.getExternalStorageDirectory(),"MTC_CHOIR");
        if(!file.exists()){
            file.mkdir();
        }
        final File file1 = new File(Environment.getExternalStorageDirectory(),"/MTC_CHOIR/mtc_choir.apk");
        StorageReference storageRef = FirebaseStorage.getInstance().getReference("mtc_choir.apk");
        final ProgressDialog mProgressDialog = new ProgressDialog(SplashActivity.this);
        mProgressDialog.setTitle("Downloading");
        mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        mProgressDialog.setCancelable(false);
        mProgressDialog.show();
        storageRef.getFile(file1).addOnSuccessListener(new OnSuccessListener<FileDownloadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(FileDownloadTask.TaskSnapshot taskSnapshot) {
                mProgressDialog.dismiss();
                StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
                StrictMode.setVmPolicy(builder.build());
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setDataAndType(Uri.fromFile(file1),"application/vnd.android.package-archive");
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.putExtra(Intent.EXTRA_NOT_UNKNOWN_SOURCE,true);
                finish();
                startActivity(intent);
            }
        }).addOnProgressListener(new OnProgressListener<FileDownloadTask.TaskSnapshot>() {
            @Override
            public void onProgress(FileDownloadTask.TaskSnapshot taskSnapshot) {
                double progress = (double) taskSnapshot.getBytesTransferred() / (double) taskSnapshot.getTotalByteCount() * 100;
                mProgressDialog.setProgress((int) progress);
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                mProgressDialog.dismiss();
                Toast.makeText(SplashActivity.this, "Failed", Toast.LENGTH_SHORT).show();
            }
        });
    }
    public void update(){
        final FirebaseRemoteConfig mRemoteConfig = FirebaseRemoteConfig.getInstance();
        mRemoteConfig.fetch(0);
        mRemoteConfig.activateFetched();

        boolean is_update_required = mRemoteConfig.getBoolean("is_update_required");
        is_under_maintenance = mRemoteConfig.getBoolean("is_under_maintenance");
        if(!mRemoteConfig.getString("latest_version").equals("")) {
            latest_verion_code = Integer.valueOf(mRemoteConfig.getString("latest_version"));
        }
        try {
            PackageInfo pInfo = this.getPackageManager().getPackageInfo(getPackageName(), 0);
            mVersionCode = pInfo.versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }


        if (is_update_required && AppController.isNetworkAvailable(SplashActivity.this) && mVersionCode<latest_verion_code) {
            final AlertDialog.Builder alertDialog = new AlertDialog.Builder(SplashActivity.this);
            alertDialog.setTitle("Update Available");
            alertDialog.setMessage("Update to new available version!!");
            alertDialog.setCancelable(false);

            alertDialog.setPositiveButton("UPDATE",
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            String updateUrl = mRemoteConfig.getString("update_url");
                            redirectStore(updateUrl);
                        }
                    });

            alertDialog.setNegativeButton("SKIP",
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.cancel();
                            next_page();
                        }
                    });

            alertDialog.show();
        } else {
            next_page();
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }
    private void initializeUser(final SharedPreferences sharedPreferences) {

        final AlertDialog.Builder alertDialog = new AlertDialog.Builder(SplashActivity.this);
        alertDialog.setTitle("Initialize User");
        alertDialog.setCancelable(false);

        LayoutInflater inflater = this.getLayoutInflater();
        final View dialogView = inflater.inflate(R.layout.layout_initialize_user, null);
        alertDialog.setView(dialogView);

        final AlertDialog alertDialog1 = alertDialog.create();
        alertDialog1.setButton(DialogInterface.BUTTON_POSITIVE,"OK",new Message());
        alertDialog1.show();
        alertDialog1.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                EditText name_et = dialogView.findViewById(R.id.et_username);
                String name = name_et.getText().toString();
                if (!name.equals("")) {

                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.putString("user_name", name);
                    editor.apply();
                    AppController.mUserName = name;
                    DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Users");
                    reference.child(name).setValue(name);
                    request_permission();
                    alertDialog1.dismiss();
                }
                else{
                    Toast.makeText(SplashActivity.this, "Please Enter Name", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    public void request_permission(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(new String[]{
                            Manifest.permission.READ_EXTERNAL_STORAGE,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    1);
        }
        else{
            update();
        }
    }

    public void next_page(){
        Intent intent;
        if(is_under_maintenance){
            intent = new Intent(SplashActivity.this,Maintenance.class);
        }
        else{
            intent = new Intent(SplashActivity.this,HomeActivity.class);
        }
        startActivity(intent);
        finish();
    }
}
