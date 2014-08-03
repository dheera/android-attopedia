package net.dheera.picopedia;

import android.os.Bundle;
import android.support.wearable.view.CardFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

/**
 * Created by dheera on 8/2/14.
 */
public class SearchResultFragment extends CardFragment {

    private static String title;
    private static String summary;
    private static String url;

    ViewGroup mRootView;
    @Override
    public View onCreateContentView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mRootView = (ViewGroup) inflater.inflate(R.layout.searchresult, null);
        Bundle bundle = getArguments();
        TextView textTitle = (TextView) mRootView.findViewById(R.id.textTitle);
        textTitle.setText(bundle.getString(CardFragment.KEY_TITLE));
        TextView textSummary = (TextView) mRootView.findViewById(R.id.textSummary);
        textSummary.setText(bundle.getString(CardFragment.KEY_TEXT));
        return mRootView;
    }

    public static SearchResultFragment newInstance(String title, String summary, String url) {
        Bundle bundle = new Bundle();
        bundle.putString(CardFragment.KEY_TITLE, title);
        bundle.putString(CardFragment.KEY_TEXT, summary);
        // bundle.putString(CardFragment.KEY_ICON_RESOURCE, icon);
        SearchResultFragment f = new SearchResultFragment();
        f.setArguments(bundle);
        return f;
    }

}
