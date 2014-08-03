package net.dheera.picopedia;

import android.app.Fragment;
import android.os.Bundle;
import android.support.wearable.view.CardFragment;
import android.support.wearable.view.GridViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class WikipediaSectionFragment extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View contentView = inflater.inflate(R.layout.wikipedia_fragment_section, container, false);
        Bundle bundle = getArguments();

        TextView mTextView = (TextView) contentView.findViewById(R.id.textView);
        mTextView.setText(bundle.getString("title"));

        contentView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });

        return contentView;
    }

    public static WikipediaSectionFragment newInstance(String title) {
        Bundle bundle = new Bundle();
        bundle.putString("title", title);
        WikipediaSectionFragment f = new WikipediaSectionFragment();
        f.setArguments(bundle);
        return f;
    }

}
