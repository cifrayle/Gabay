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
            //new Lesson("Letter T", R.raw.t),
            new Lesson("Letter U", R.raw.u),
            new Lesson("Letter V", R.raw.v),
            new Lesson("Letter W", R.raw.w),
            new Lesson("Letter X", R.raw.x),
            new Lesson("Letter Y", R.raw.y),
            new Lesson("Letter Z", R.raw.z)
    };

    // add other chapters here:
    // public static final Lesson[] CHAPTER2 = { ... };
}
