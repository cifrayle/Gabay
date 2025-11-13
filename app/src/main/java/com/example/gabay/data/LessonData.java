// In LessonData.java

package com.example.gabay.data;

import com.example.gabay.R;

public class LessonData {

    // MODIFIED: The model now holds an integer resource ID for the title
    public static class Lesson {
        public final int titleResId; // Changed from String to int
        public final int videoResId;

        public Lesson(int titleResId, int videoResId) {
            this.titleResId = titleResId;
            this.videoResId = videoResId;
        }
    }

    // MODIFIED: All chapters now use R.string references

    // Chapter 1 - Filipino Alphabet
    public static final Lesson[] CHAPTER1 = new Lesson[]{
            new Lesson(R.string.lesson_letter_a, R.raw.a),
            new Lesson(R.string.lesson_letter_b, R.raw.b),
            new Lesson(R.string.lesson_letter_c, R.raw.c),
            new Lesson(R.string.lesson_letter_d, R.raw.d),
            new Lesson(R.string.lesson_letter_e, R.raw.e),
            new Lesson(R.string.lesson_letter_f, R.raw.f),
            new Lesson(R.string.lesson_letter_g, R.raw.g),
            new Lesson(R.string.lesson_letter_h, R.raw.h),
            new Lesson(R.string.lesson_letter_i, R.raw.i),
            new Lesson(R.string.lesson_letter_j, R.raw.j),
            new Lesson(R.string.lesson_letter_k, R.raw.k),
            new Lesson(R.string.lesson_letter_l, R.raw.l),
            new Lesson(R.string.lesson_letter_m, R.raw.m),
            new Lesson(R.string.lesson_letter_n, R.raw.n),
            new Lesson(R.string.lesson_letter_enye, R.raw.enye),
            new Lesson(R.string.lesson_letter_ng, R.raw.ng),
            new Lesson(R.string.lesson_letter_o, R.raw.o),
            new Lesson(R.string.lesson_letter_p, R.raw.p),
            new Lesson(R.string.lesson_letter_q, R.raw.q),
            new Lesson(R.string.lesson_letter_r, R.raw.r),
            new Lesson(R.string.lesson_letter_s, R.raw.s),
            new Lesson(R.string.lesson_letter_t, R.raw.t),
            new Lesson(R.string.lesson_letter_u, R.raw.u),
            new Lesson(R.string.lesson_letter_v, R.raw.v),
            new Lesson(R.string.lesson_letter_w, R.raw.w),
            new Lesson(R.string.lesson_letter_x, R.raw.x),
            new Lesson(R.string.lesson_letter_y, R.raw.y),
            new Lesson(R.string.lesson_letter_z, R.raw.z)
    };

    // chapter2  - basic greetings
    public static final Lesson[] CHAPTER2 = new Lesson[]{
            new Lesson(R.string.lesson_greeting_hello, R.raw.hello),
            new Lesson(R.string.lesson_greeting_how_are_you, R.raw.howareyou),
            new Lesson(R.string.lesson_greeting_sorry, R.raw.sorry),
            new Lesson(R.string.lesson_greeting_thank_you_formal, R.raw.thankyouf),
            new Lesson(R.string.lesson_greeting_thank_you_informal, R.raw.thankyouin),
    };

    // chapter3  - numbers
    public static final Lesson[] CHAPTER3 = new Lesson[]{
            new Lesson(R.string.lesson_number_1, R.raw.one),
            new Lesson(R.string.lesson_number_2, R.raw.two),
            new Lesson(R.string.lesson_number_3, R.raw.three),
            new Lesson(R.string.lesson_number_4, R.raw.four),
            new Lesson(R.string.lesson_number_5, R.raw.five),
            new Lesson(R.string.lesson_number_6, R.raw.six),
            new Lesson(R.string.lesson_number_7, R.raw.seven),
            new Lesson(R.string.lesson_number_8, R.raw.eight),
            new Lesson(R.string.lesson_number_9, R.raw.nine),
            new Lesson(R.string.lesson_number_10, R.raw.ten)
    };

    // chapter4 - questions
    public static final Lesson[] CHAPTER4 = new Lesson[]{
            new Lesson(R.string.lesson_question_what, R.raw.what),
            new Lesson(R.string.lesson_question_when, R.raw.when),
            new Lesson(R.string.lesson_question_where, R.raw.where),
            new Lesson(R.string.lesson_question_which, R.raw.which),
            new Lesson(R.string.lesson_question_who, R.raw.who),
            new Lesson(R.string.lesson_question_why, R.raw.why),
            new Lesson(R.string.lesson_question_how, R.raw.how),
    };

    // chapter5 - days of the week
    public static final Lesson[] CHAPTER5 = new Lesson[]{
            new Lesson(R.string.lesson_day_monday, R.raw.monday),
            new Lesson(R.string.lesson_day_tuesday, R.raw.tuesday),
            new Lesson(R.string.lesson_day_wednesday, R.raw.wednesday),
            new Lesson(R.string.lesson_day_thursday, R.raw.thursday),
            new Lesson(R.string.lesson_day_friday, R.raw.friday),
            new Lesson(R.string.lesson_day_saturday, R.raw.saturday),
            new Lesson(R.string.lesson_day_sunday, R.raw.sunday),
    };
}
