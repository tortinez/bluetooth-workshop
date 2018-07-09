package edu.upc.mcia.androidpracticabluetooth.fragment;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

public class SectionsPagerAdapter extends FragmentPagerAdapter {

    // Constants
    private static final int NUM_FRAGMENTS = 2;
    public static final int TAB_BITS = 0;
    public static final int TAB_BYTES = 1;

    // Fragment cache
    private Fragment[] fragmentCache;

    public SectionsPagerAdapter(FragmentManager fragmentManager) {
        super(fragmentManager);
        fragmentCache = new Fragment[NUM_FRAGMENTS];
    }

    @Override
    public Fragment getItem(int position) {
        switch (position) {
            case TAB_BITS:
                if (fragmentCache[0] == null) {
                    fragmentCache[0] = BitsFragment.newInstance();
                }
                return fragmentCache[0];
            case TAB_BYTES:
                if (fragmentCache[1] == null) {
                    fragmentCache[1] = BytesFragment.newInstance();
                }
                return fragmentCache[1];
        }
        return null;
    }

    @Override
    public int getCount() {
        return NUM_FRAGMENTS;
    }

    @Override
    public CharSequence getPageTitle(int position) {
        switch (position) {
            case TAB_BITS:
                return "Bits";
            case TAB_BYTES:
                return "Bytes";
            default:
                return null;
        }
    }

}
