package com.example.gabay.data;

import com.example.gabay.R;
public class LessonData {

    // A simple model for each lesson
    public static class Lesson {
        public final String title;
        public final int videoRes;

        public Lesson(String title, int videoRes) {
            this.title = title;
            this.videoRes = videoRes;
        }
    }

    // Chapter 1 - Filipino Alphabet
    public static final Lesson[] CHAPTER1 = new Lesson[]{
            new Lesson("Letter A", R.raw.a),
            new Lesson("Letter B", R.raw.b),
            new Lesson("Letter C", R.raw.c),
            new Lesson("Letter D", R.raw.d),
            new Lesson("Letter E", R.raw.e),
            new Lesson("Letter F", R.raw.f),
            new Lesson("Letter G", R.raw.g),
            new Lesson("Letter H", R.raw.h),
            new Lesson("Letter I", R.raw.i),
            new Lesson("Letter J", R.raw.j),
            new Lesson("Letter K", R.raw.k),
            new Lesson("Letter L", R.raw.l),
            new Lesson("Letter M", R.raw.m),
            new Lesson("Letter N", R.raw.n),
            new Lesson("Letter Ñ", R.raw.enye),
            new Lesson("Letter NG", R.raw.ng),
            new Lesson("Letter O", R.raw.o),
            new Lesson("Letter P", R.raw.p),
            new Lesson("Letter Q", R.raw.q),
            new Lesson("Letter R", R.raw.r),
            new Lesson("Letter S", R.raw.s),
            new Lesson("Letter T", R.raw.t),
            new Lesson("Letter U", R.raw.u),
            new Lesson("Letter V", R.raw.v),
            new Lesson("Letter W", R.raw.w),
            new Lesson("Letter X", R.raw.x),
            new Lesson("Letter Y", R.raw.y),
            new Lesson("Letter Z", R.raw.z)
    };

    // chapter2  - basic greetings
    public static final Lesson[] CHAPTER2 = new Lesson[]{
            new Lesson("Hello", R.raw.hello),
            new Lesson("How are you?", R.raw.howareyou),
            new Lesson("Sorry", R.raw.sorry),
            new Lesson("Thank you (Formal)", R.raw.thankyouf), // thank you (formal)
            new Lesson("Thank you (Informal)", R.raw.thankyouin), // thank you (informal)

    };

    // chapter3  - Numbers
    public static final Lesson[] CHAPTER3 = new Lesson[]{
            new Lesson("Number 1", R.raw.one),
            new Lesson("Number 2", R.raw.two),
            new Lesson("Number 3", R.raw.three),
            new Lesson("Number 4", R.raw.four),
            new Lesson("Number 5", R.raw.five),
            new Lesson("Number 6", R.raw.six),
            new Lesson("Number 7", R.raw.seven),
            new Lesson("Number 8", R.raw.eight),
            new Lesson("Number 9", R.raw.nine),
            new Lesson("Number 10", R.raw.ten)
    };

    // chapter4 - questions
    public static final Lesson[] CHAPTER4 = new Lesson[]{
            new Lesson("What", R.raw.what),
            new Lesson("When", R.raw.when),
            new Lesson("Where", R.raw.where),
            new Lesson("Which", R.raw.which),
            new Lesson("Who", R.raw.who),
            new Lesson("Why", R.raw.why),
            new Lesson("How", R.raw.how),

    };

    // chapter5 - days of the week
    public static final Lesson[] CHAPTER5 = new Lesson[]{
            new Lesson("Monday", R.raw.monday),
            new Lesson("Tuesday", R.raw.tuesday),
            new Lesson("Wednesday", R.raw.wednesday),
            new Lesson("Thursday", R.raw.thursday),
            new Lesson("Friday", R.raw.friday),
            new Lesson("Saturday", R.raw.saturday),
            new Lesson("Sunday", R.raw.sunday),

    };
}
