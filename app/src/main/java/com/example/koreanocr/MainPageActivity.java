package com.example.koreanocr;


import android.content.pm.PackageManager;
import android.os.Bundle;
import android.Manifest;

import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

public class MainPageActivity extends FragmentActivity {

    private static final int REQUEST_CODE = 1001;
    private static final String[] REQUESTED_PERMISSIONS = new String[]{Manifest.permission.CAMERA};
    private static final int NUM_PAGES = 2;
    private ViewPager2 viewPager;
    private FragmentStateAdapter pagerAdapter;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_screen_slide);

        // check if all permissions granted
        if (allPermissionsGranted()) {
            // Instantiate a ViewPager2 and a PagerAdapter.
            viewPager = findViewById(R.id.pager);
            pagerAdapter = new ScreenSlidePagerAdapter(this);
            viewPager.setAdapter(pagerAdapter);
            viewPager.setCurrentItem(1);
        } else {
            finish();
        }
    }


    @Override
    public void onBackPressed() {
        if (viewPager.getCurrentItem() == 1) {
            // If the user is currently looking at the first step, allow the system to handle the
            // Back button. This calls finish() on this activity and pops the back stack.
            super.onBackPressed();
        } else {
            // Otherwise, select the previous step.
            viewPager.setCurrentItem(1);
        }
    }

    /**
     * A simple pager adapter that represents 5 ScreenSlidePageFragment objects, in
     * sequence.
     */
    private class ScreenSlidePagerAdapter extends FragmentStateAdapter {
        public ScreenSlidePagerAdapter(FragmentActivity fa) {
            super(fa);
        }
        @Override
        public Fragment createFragment(int position) {
            switch (position) {
                case 0:
                    return new WordDetectFragment();
                case 1:

                    return new MainFragment();
                case 2:
                    return new WordDetectFragment();
            }
            return null;
        }

        @Override
        public int getItemCount() {
            return NUM_PAGES;
        }
    }

    // check permissions are granted
    private boolean allPermissionsGranted(){
        for(String permission : REQUESTED_PERMISSIONS)
            if((ActivityCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED))
                // You can directly ask for the permission.
                requestPermissions(REQUESTED_PERMISSIONS, REQUEST_CODE);
        return true;
    }

}