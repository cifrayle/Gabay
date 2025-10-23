package com.example.gabay.fragments.pages;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
        setTheme(R.style.Theme_Gabay);
        View view = inflater.inflate(R.layout.fragment_dictionary_page, container, false);

        CardView dictionaryItem1 = view.findViewById(R.id.dictionary_item1);

        // Remove any clickable attributes from child views in your XML
        dictionaryItem1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d("DictionaryPage", "CardView clicked - navigating to content page");
                navigateToDictionaryContentPage();
            }
        });

        return view;
    }
    private void navigateToDictionaryContentPage() {
        try {
            DictionaryContentPage dictionaryContentPage = new DictionaryContentPage();
            FragmentTransaction transaction = requireActivity().getSupportFragmentManager().beginTransaction();

            // Replace the fragment in the container
            transaction.replace(R.id.fragment_container, dictionaryContentPage, "dictionary_content_fragment");

            // Add to back stack so user can go back
            transaction.addToBackStack("dictionary_page");

            transaction.commit();
            Log.d("DictionaryPage", "Fragment transaction committed");

        } catch (Exception e) {
            Log.e("DictionaryPage", "Error navigating to content page: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void setTheme(int themeGabay) {
        // Your theme setting logic here
    }
}