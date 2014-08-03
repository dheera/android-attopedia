package net.dheera.picopedia;

import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Context;
import android.support.wearable.view.CardFragment;
import android.support.wearable.view.FragmentGridPagerAdapter;
import android.support.wearable.view.ImageReference;
import android.util.Log;

import org.json.JSONObject;


public class WikipediaAdapter extends FragmentGridPagerAdapter {
    private static final String TAG = "picopedia.WikipediaAdapter";
    private static final boolean D = true;

    private final Context mContext;
    // public final WikipediaFragment mWikipediaFragment;
    public final JSONObject page;

    public WikipediaAdapter(Context ctx, FragmentManager fm, JSONObject page) {
        super(fm);
        mContext = ctx;
        // mWikipediaFragment = new WikipediaFragment();
        this.page = page;
    }

    @Override
    public int getColumnCount(int arg0) {
        if(page == null) {
            return 1;
        }
        return 1;
    }

    @Override
    public int getRowCount() {
        if(page == null) {
            return 1;
        }
        return 1;
    }

    @Override
    public Fragment getFragment(int rowNum, int colNum) {
        Log.d("blah", String.format("getFragment(%d, %d)", rowNum, colNum));
        if(page == null) {
            return CardFragment.create("miao", "haha");
        }
        return null;
    }
}
