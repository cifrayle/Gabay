package com.example.gabay.data;

import com.example.gabay.R;

public class LessonDescriptionData {

    // MODIFIED: The constructor now accepts integer resource IDs
    public static class LessonDescription {
        public final int titleResId;
        public final int descriptionResId;

        public LessonDescription(int titleResId, int descriptionResId) {
            this.titleResId = titleResId;
            this.descriptionResId = descriptionResId;
        }
    }

    // MODIFIED: All arrays now use R.string references

    // Chapter 1 - FSL alphabet
    public static final LessonDescription[] CHAPTER1 = new LessonDescription[]{
            new LessonDescription(R.string.lesson_letter_a, R.string.lesson_alphabet_desc_a),
            new LessonDescription(R.string.lesson_letter_b, R.string.lesson_alphabet_desc_b),
            new LessonDescription(R.string.lesson_letter_c, R.string.lesson_alphabet_desc_c),
            new LessonDescription(R.string.lesson_letter_d, R.string.lesson_alphabet_desc_d),
            new LessonDescription(R.string.lesson_letter_e, R.string.lesson_alphabet_desc_e),
            new LessonDescription(R.string.lesson_letter_f, R.string.lesson_alphabet_desc_f),
            new LessonDescription(R.string.lesson_letter_g, R.string.lesson_alphabet_desc_g),
            new LessonDescription(R.string.lesson_letter_h, R.string.lesson_alphabet_desc_h),
            new LessonDescription(R.string.lesson_letter_i, R.string.lesson_alphabet_desc_i),
            new LessonDescription(R.string.lesson_letter_j, R.string.lesson_alphabet_desc_j),
            new LessonDescription(R.string.lesson_letter_k, R.string.lesson_alphabet_desc_k),
            new LessonDescription(R.string.lesson_letter_l, R.string.lesson_alphabet_desc_l),
            new LessonDescription(R.string.lesson_letter_m, R.string.lesson_alphabet_desc_m),
            new LessonDescription(R.string.lesson_letter_n, R.string.lesson_alphabet_desc_n),
            new LessonDescription(R.string.lesson_letter_enye, R.string.lesson_alphabet_desc_enye),
            new LessonDescription(R.string.lesson_letter_ng, R.string.lesson_alphabet_desc_ng),
            new LessonDescription(R.string.lesson_letter_o, R.string.lesson_alphabet_desc_o),
            new LessonDescription(R.string.lesson_letter_p, R.string.lesson_alphabet_desc_p),
            new LessonDescription(R.string.lesson_letter_q, R.string.lesson_alphabet_desc_q),
            new LessonDescription(R.string.lesson_letter_r, R.string.lesson_alphabet_desc_r),
            new LessonDescription(R.string.lesson_letter_s, R.string.lesson_alphabet_desc_s),
            new LessonDescription(R.string.lesson_letter_t, R.string.lesson_alphabet_desc_t),
            new LessonDescription(R.string.lesson_letter_u, R.string.lesson_alphabet_desc_u),
            new LessonDescription(R.string.lesson_letter_v, R.string.lesson_alphabet_desc_v),
            new LessonDescription(R.string.lesson_letter_w, R.string.lesson_alphabet_desc_w),
            new LessonDescription(R.string.lesson_letter_x, R.string.lesson_alphabet_desc_x),
            new LessonDescription(R.string.lesson_letter_y, R.string.lesson_alphabet_desc_y),
            new LessonDescription(R.string.lesson_letter_z, R.string.lesson_alphabet_desc_z),
    };

    // Chapter 2 - basic greetings
    public static final LessonDescription[] CHAPTER2 = new LessonDescription[]{
            new LessonDescription(R.string.lesson_greeting_hello, R.string.lesson_greeting_desc_hello),
            new LessonDescription(R.string.lesson_greeting_how_are_you, R.string.lesson_greeting_desc_how_are_you),
            new LessonDescription(R.string.lesson_greeting_sorry, R.string.lesson_greeting_desc_sorry),
            new LessonDescription(R.string.lesson_greeting_thank_you_formal, R.string.lesson_greeting_desc_thank_you_formal),
            new LessonDescription(R.string.lesson_greeting_thank_you_informal, R.string.lesson_greeting_desc_thank_you_informal),
    };

    // Chapter 3 - numbers
    public static final LessonDescription[] CHAPTER3 = new LessonDescription[]{
            new LessonDescription(R.string.lesson_number_1, R.string.lesson_number_desc_1),
            new LessonDescription(R.string.lesson_number_2, R.string.lesson_number_desc_2),
            new LessonDescription(R.string.lesson_number_3, R.string.lesson_number_desc_3),
            new LessonDescription(R.string.lesson_number_4, R.string.lesson_number_desc_4),
            new LessonDescription(R.string.lesson_number_5, R.string.lesson_number_desc_5),
            new LessonDescription(R.string.lesson_number_6, R.string.lesson_number_desc_6),
            new LessonDescription(R.string.lesson_number_7, R.string.lesson_number_desc_7),
            new LessonDescription(R.string.lesson_number_8, R.string.lesson_number_desc_8),
            new LessonDescription(R.string.lesson_number_9, R.string.lesson_number_desc_9),
            new LessonDescription(R.string.lesson_number_10, R.string.lesson_number_desc_10),
    };

    // Chapter 4 - questions
    public static final LessonDescription[] CHAPTER4 = new LessonDescription[]{
            new LessonDescription(R.string.lesson_question_what, R.string.lesson_question_desc_what),
            new LessonDescription(R.string.lesson_question_when, R.string.lesson_question_desc_when),
            new LessonDescription(R.string.lesson_question_where, R.string.lesson_question_desc_where),
            new LessonDescription(R.string.lesson_question_which, R.string.lesson_question_desc_which),
            new LessonDescription(R.string.lesson_question_who, R.string.lesson_question_desc_who),
            new LessonDescription(R.string.lesson_question_why, R.string.lesson_question_desc_why),
            new LessonDescription(R.string.lesson_question_how, R.string.lesson_question_desc_how),
    };

    // Chapter 5 - days of the week
    public static final LessonDescription[] CHAPTER5 = new LessonDescription[]{
            new LessonDescription(R.string.lesson_day_monday, R.string.lesson_day_desc_monday),
            new LessonDescription(R.string.lesson_day_tuesday, R.string.lesson_day_desc_tuesday),
            new LessonDescription(R.string.lesson_day_wednesday, R.string.lesson_day_desc_wednesday),
            new LessonDescription(R.string.lesson_day_thursday, R.string.lesson_day_desc_thursday),
            new LessonDescription(R.string.lesson_day_friday, R.string.lesson_day_desc_friday),
            new LessonDescription(R.string.lesson_day_saturday, R.string.lesson_day_desc_saturday),
            new LessonDescription(R.string.lesson_day_sunday, R.string.lesson_day_desc_sunday),
    };
}
