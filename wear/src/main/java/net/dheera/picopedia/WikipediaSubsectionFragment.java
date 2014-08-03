package net.dheera.picopedia;

import android.app.Fragment;
import android.os.Bundle;
import android.support.wearable.view.CardFragment;
import android.support.wearable.view.GridViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class WikipediaSubsectionFragment extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View contentView = inflater.inflate(R.layout.wikipedia_fragment_subsection, container, false);
        Bundle bundle = getArguments();

        TextView mTextView_title = (TextView) contentView.findViewById(R.id.textView_title);
        mTextView_title.setText(bundle.getString("title"));
        TextView mTextView_text = (TextView) contentView.findViewById(R.id.textView_text);
        mTextView_text.setText(bundle.getString("text"));

        contentView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });

        return contentView;
    }

    public static WikipediaSubsectionFragment newInstance(String title, String text) {
        Bundle bundle = new Bundle();
        bundle.putString("title", title);
        bundle.putString("text", text);
        WikipediaSubsectionFragment f = new WikipediaSubsectionFragment();
        f.setArguments(bundle);
        return f;
    }

}
