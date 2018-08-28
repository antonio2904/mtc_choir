package anonymous.com.mtcchoir.adapters;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

import anonymous.com.mtcchoir.fragments.KaroakeFragment;
import anonymous.com.mtcchoir.fragments.MP3Fragment;
import anonymous.com.mtcchoir.fragments.YoutubeFragment;

public class ViewPagerAdapter extends FragmentPagerAdapter {

    public ViewPagerAdapter(FragmentManager fm) {
        super(fm);
    }

    @Override
    public Fragment getItem(int position) {
        Fragment fragment = null;
        if (position == 0)
        {
            fragment = new MP3Fragment();
        }
        else if (position == 1)
        {
            fragment = new YoutubeFragment();
        }
        else if(position == 2)
        {
            fragment = new KaroakeFragment();
        }
        return fragment;
    }

    @Override
    public int getCount() {
        return 3;
    }

    @Override
    public CharSequence getPageTitle(int position) {
        String title = null;
        if (position == 0)
        {
            title = "Mp3";
        }
        else if (position == 1)
        {
            title = "Youtube";
        }
        else if (position == 2)
        {
            title = "Karaoke";
        }
        return title;
    }
}
