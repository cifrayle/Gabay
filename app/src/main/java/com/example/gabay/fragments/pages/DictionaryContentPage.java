package com.example.gabay.fragments.pages;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.example.gabay.R;
import com.example.gabay.activities.MainActivity;

public class DictionaryContentPage extends Fragment {

    private static final String TAG = "DictionaryContentPage";

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_dictionary_content_page, container, false);

        // Hide the MainActivity's action bar and bottom navigation
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).hideActionBar();
            ((MainActivity) getActivity()).hideBottomNav();
        }

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupFragmentToolbar(view);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Restore MainActivity's UI
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).showActionBar();
            ((MainActivity) getActivity()).showBottomNav();
        }
    }

    private void setupFragmentToolbar(View view) {
        String wordTitle = getArguments() != null ? getArguments().getString("word_title", "Word Details") : "Word Details";

        // Find views in the fragment's layout
        View toolbar = view.findViewById(R.id.toolbar);
        ImageButton backButton = toolbar.findViewById(R.id.levels_back_button);
        TextView titleTextView = toolbar.findViewById(R.id.action_bar_title);

        Log.d(TAG, "Fragment toolbar: " + (toolbar != null));
        Log.d(TAG, "Fragment back button: " + (backButton != null));
        Log.d(TAG, "Fragment title view: " + (titleTextView != null));

        if (titleTextView != null) {
            titleTextView.setText(wordTitle);
        }

        if (backButton != null) {
            backButton.setVisibility(View.VISIBLE);
            backButton.setOnClickListener(v -> {
                Log.d(TAG, "Fragment back button clicked");
                requireActivity().getSupportFragmentManager().popBackStack();
            });
        }
    }
}