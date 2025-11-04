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
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.example.gabay.R;

public class DictionaryPage extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_dictionary_page, container, false);


        CardView dictionaryItem1 = view.findViewById(R.id.dictionary_item1);

        dictionaryItem1.setOnClickListener(v -> {
            Log.d("DictionaryPage", "CardView clicked - navigating to content page");
            navigateToDictionaryContentPage("Hello / Kumusta"); // pass the word name here
        });

        return view;
    }

    private void navigateToDictionaryContentPage(String wordTitle) {
        try {
            DictionaryContentPage dictionaryContentPage = new DictionaryContentPage();

            // ✅ Pass the word title using a Bundle
            Bundle args = new Bundle();
            args.putString("word_title", wordTitle);
            dictionaryContentPage.setArguments(args);

            FragmentTransaction transaction = requireActivity().getSupportFragmentManager().beginTransaction();
            transaction.replace(R.id.fragment_container, dictionaryContentPage, "dictionary_content_fragment");
            transaction.addToBackStack("dictionary_page");
            transaction.commit();

            Log.d("DictionaryPage", "Navigated to DictionaryContentPage with title: " + wordTitle);
        } catch (Exception e) {
            Log.e("DictionaryPage", "Error navigating to content page: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
