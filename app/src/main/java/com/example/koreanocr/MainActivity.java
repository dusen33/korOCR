package com.example.koreanocr;

import android.content.pm.PackageManager;
import android.os.Bundle;
import android.Manifest;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class MainActivity extends AppCompatActivity {

    private final int REQUEST_CODE = 1001;
    private final String[] REQUESTED_PERMISSIONS = new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE};


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // check if all permissions granted
        if (allPermissionsGranted()) {
            // You can use the API that requires the permission.

        } else {
            this.finish();
        }

    }

    private boolean allPermissionsGranted(){
        for(String permission : REQUESTED_PERMISSIONS){
            if(ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED){

                return true;
            } else {
                // You can directly ask for the permission.
                requestPermissions(REQUESTED_PERMISSIONS, REQUEST_CODE);
            }
        }
        return false;
    }

}