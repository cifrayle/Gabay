package com.example.gabay.data;

import android.os.Bundle;

import androidx.fragment.app.Fragment;

import com.example.gabay.R;
import com.example.gabay.fragments.quiz.MatchingQuizFragment;
import com.example.gabay.fragments.quiz.QuizFragment;

public final class QuizRegistry {
    public enum QuizType { MULTIPLE_CHOICE, MATCHING }

    public static Fragment buildFragmentForChapter(int chapter, int level) {
        switch (chapter) {
            case 1: // Multiple choice
                return buildMCQ(chapter, level,
                        new int[]{  // images
                                R.drawable.img_letter_c, R.drawable.img_letter_a,
                                R.drawable.img_letter_s, R.drawable.img_letter_h,
                                R.drawable.img_letter_g, R.drawable.img_letter_j,
                                R.drawable.img_letter_i, R.drawable.img_letter_u,
                                R.drawable.img_letter_t, R.drawable.img_letter_f
                        },
                        new String[]{ // choices (4 per image)
                                "HELLO/KUMUSTA","C","NO/HINDI","O",
                                "A","ONE/ISA","I","YOU/IKAW",
                                "FIST/KAMAO","S","ME/AKO","A",
                                "H","LEFT/KALIWA","Y","L",
                                "X","G","TWO/DALAWA","L",
                                "I","ONE/ISA","J","B",
                                "I","R","J","ONE/ISA",
                                "T","D","F","U",
                                "D","T","Y","L",
                                "THREE/TATLO","F","OK","K",
                        },
                        new String[]{ "C","A","S","H","G","J","I","U","T","F" }
                );

            case 2: // Matching
                return buildMatching(chapter, level,
                        new int[]{
                                //image resources
                                R.drawable.img_sorry,
                                R.drawable.img_letter_r,
                                R.drawable.img_letter_y,
                                R.drawable.img_hello,
                                R.drawable.img_letter_q, },
                        new String[]{ "Sorry", "R", "Y", "Hello", "Q"}); //answers

            case 3: // Matching - questions
                return buildMatching(chapter, level,
                        new int[]{
                                R.drawable.img_number_4,
                                R.drawable.img_number_6,
                                R.drawable.img_number_8,
                                R.drawable.img_number_9
                        },
                        new String[]{ "4", "6", "8", "9" });

            case 4: // Multiple choice
                return buildMCQ(chapter, level,
                        new int[]{
                                R.drawable.img_when,
                                R.drawable.img_where,
                                R.drawable.img_what,
                                R.drawable.img_which,
                                R.drawable.img_number_2 },

                        new String[]{
                                "WHERE/SAAN","WHICH/ALIN","WHAT/ANO","WHEN/KAILAN",
                                "WHERE/SAAN","WHICH/ALIN","WHAT/ANO","WHEN/KAILAN",
                                "WHERE/SAAN","WHICH/ALIN","WHAT/ANO","WHEN/KAILAN",
                                "WHERE/SAAN","WHICH/ALIN","WHAT/ANO","WHEN/KAILAN",
                                "THREE/TATLO","V","TWO/DALAWA","W" },

                        new String[]{ "WHEN/KAILAN","WHERE/SAAN","WHAT/ANO","WHICH/ALIN","TWO/DALAWA" });

            case 5: // Matching
                return buildMatching(chapter, level,
                        new int[]{
                                R.drawable.img_sunday,
                                R.drawable.img_monday,
                                R.drawable.img_wednesday,
                                R.drawable.img_tuesday
                        },
                        new String[]{ "Sunday", "Monday", "Wednesday", "Tuesday" });

            default:
                // Safe fallback: show MCQ with no data (fragments will use defaults)
                return QuizFragment.newInstance(chapter, level);
        }
    }

    private static Fragment buildMCQ(int chapter, int level,
                                     int[] images, String[] choices, String[] answers) {
        Bundle args = new Bundle();
        args.putInt("chapter", chapter);
        args.putInt("level", level);
        args.putIntArray("mcq_images", images);
        args.putStringArray("mcq_choices", choices);
        args.putStringArray("mcq_answers", answers);

        Fragment f = QuizFragment.newInstance(chapter, level);
        f.setArguments(args);
        return f;
    }

    private static Fragment buildMatching(int chapter, int level,
                                          int[] images, String[] labels) {
        Bundle args = new Bundle();
        args.putInt("chapter", chapter);
        args.putInt("level", level);
        args.putIntArray("match_images", images);
        args.putStringArray("match_labels", labels);

        Fragment f = MatchingQuizFragment.newInstance(chapter, level);
        f.setArguments(args);
        return f;
    }
}
