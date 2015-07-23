package net.dheera.attopedia;

import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Context;
import android.support.wearable.view.CardFragment;
import android.support.wearable.view.FragmentGridPagerAdapter;
import android.support.wearable.view.ImageReference;
import android.util.Log;
import android.view.Gravity;

import java.util.ArrayList;

public class SearchAdapter extends FragmentGridPagerAdapter {
    private static final String TAG = "attopedia.WikiAdapter";
    private static final boolean D = false;

    private final Context mContext;
    public final SearchBlankFragment mSearchBlankFragment;

    ArrayList<SearchResult> searchResults = null;

    public SearchAdapter(Context ctx, FragmentManager fm, ArrayList<SearchResult> s) {
        super(fm);
        mContext = ctx;
        mSearchBlankFragment = new SearchBlankFragment();
        searchResults = s;
    }

    @Override
    public int getColumnCount(int arg0) {
        if(searchResults == null) {
            return 1;
        } else {
            return 1;
        }
    }

    @Override
    public int getRowCount() {
        if(searchResults == null) {
            return 1;
        } else {
            return searchResults.size();
        }
    }


    @Override
    public ImageReference getBackground(int rowNum, int colNum) {
        Log.d("blah", String.format("getBackground(%d, %d)", rowNum, colNum));
        if(rowNum == 0)
            return ImageReference.NONE;
        else
            return ImageReference.forDrawable(R.drawable.searchresultback);
    }

    @Override
    public Fragment getFragment(int rowNum, int colNum) {
        Log.d("blah", String.format("getFragment(%d, %d)", rowNum, colNum));
        if(searchResults == null) {
            return mSearchBlankFragment;
        } else {
                SearchResult searchResult = searchResults.get(rowNum);
                SearchResultFragment f = SearchResultFragment.newInstance(searchResult.title, searchResult.summary, searchResult.url);

                f.setCardGravity(Gravity.BOTTOM);
                f.setExpansionEnabled(true);
                if (rowNum == 0) {
                    f.setExpansionFactor(0.73f);
                } else {
                    f.setExpansionFactor(1.0f);
                }
                f.setExpansionDirection(CardFragment.EXPAND_DOWN);

                return f;
        }
    }

    public class SearchResult {
        public String title;
        public String url;
        public String summary;
    }

    public SearchResult newSearchResult() {
        return new SearchResult();
    }
}
