package com.example.gabay.fragments.pages;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.gabay.R;

public class DictionaryPage extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        setTheme(R.style.Theme_Gabay);
        return inflater.inflate(R.layout.fragment_dictionary_page, container, false);
    }

    private void setTheme(int themeGabay) {

    }
}


