package net.dheera.picopedia;

import android.app.Application;
import android.app.Fragment;
import android.os.Bundle;
import android.support.wearable.view.GridViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class BrowserFragment extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View contentView = inflater.inflate(R.layout.browser, container, false);

        SearchAdapter mSearchAdapter = new SearchAdapter(MainActivity.instance(), getFragmentManager(), null);
        GridViewPager mGridViewPager = (GridViewPager) contentView.findViewById(R.id.pager);
        mGridViewPager.setAdapter(mSearchAdapter);

        contentView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });
        return contentView;
    }

}
