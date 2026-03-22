package com.cargohub.mobile.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.cargohub.mobile.R;

public class PlaceholderSectionFragment extends Fragment {

    private static final String ARG_TITLE = "arg_title";
    private static final String ARG_MESSAGE = "arg_message";

    public static PlaceholderSectionFragment newInstance(String title, String message) {
        PlaceholderSectionFragment fragment = new PlaceholderSectionFragment();
        Bundle args = new Bundle();
        args.putString(ARG_TITLE, title);
        args.putString(ARG_MESSAGE, message);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_placeholder_section, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        TextView titleText = view.findViewById(R.id.sectionTitleText);
        TextView bodyText = view.findViewById(R.id.sectionBodyText);

        Bundle args = getArguments();
        String title = args != null ? args.getString(ARG_TITLE) : getString(R.string.section_placeholder_default_title);
        String message = args != null ? args.getString(ARG_MESSAGE) : getString(R.string.section_placeholder_default_message);

        titleText.setText(title);
        bodyText.setText(message);
    }
}
