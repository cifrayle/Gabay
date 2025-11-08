package com.example.gabay.data;

import com.example.gabay.R;

public class LessonDescriptionData {

    // A simple model for each lesson description
    public static class LessonDescription {
        public final String title;
        public final String description;

        public LessonDescription(String title, String description) {
            this.title = title;
            this.description = description;
        }
    }

    // Chapter 1 - FSL alphabet
    public static final LessonDescription[] CHAPTER1 = new LessonDescription[]{
            new LessonDescription("Letter A", "Make a fist. Keep your thumb on the side of your fist, pointing to the side, " +
                    "not across the front of your other fingers."),
            new LessonDescription("Letter B", "Keep your fingers straight and together, palm facing forward.Tuck your thumb across your palm."),
            new LessonDescription("Letter C", "Curve your hand into a “C” shape, with your fingers and thumb forming a half circle."),
            new LessonDescription("Letter D", "Touch your thumb and middle, ring, and pinky fingertips together while raising your index finger straight up."),
            new LessonDescription("Letter E", "Curl your fingers toward your palm and place your thumb across them, " +
                    "forming a small curved shape like the lowercase."),
            new LessonDescription("Letter F", "Form an “OK” sign — connect your thumb and index finger to make a circle while the other three fingers are raised."),
            new LessonDescription("Letter G", "Hold your hand sideways with your thumb and index finger pointing outward, " +
                    "as if showing a small distance between them."),
            new LessonDescription("Letter H", "Hold out your index and middle finger together, pointing to the side, " +
                    "while the rest are closed — like a sideways peace sign."),
            new LessonDescription("Letter I", "Raise your pinky finger while keeping the rest closed in a fist."),
            new LessonDescription("Letter J", "Start with the “I” sign, then draw a “J” in the air using your pinky finger."),
            new LessonDescription("Letter K", "Raise your index and middle fingers apart (like a “V”), then place your thumb between them."),
            new LessonDescription("Letter L", "Extend your index finger upward and your thumb sideways to form an “L” shape."),
            new LessonDescription("Letter M", "Place your thumb under your first three fingers (index, middle, ring), " +
                    "with your pinky out or folded — representing the three bumps of “M”."),
            new LessonDescription("Letter N", "Similar to “M” but only place your thumb under two fingers (index and middle) — forming the two bumps of “N”."),
            new LessonDescription("Letter Ñ", "Start with the “N” sign, then make a small waving motion (side to side)."),
            new LessonDescription("Letter NG","Start with the “N” sign, then make a “G” sign."),
            new LessonDescription("Letter O", "Form a full “O” with your fingers and thumb touching each other to make a round circle."),
            new LessonDescription("Letter P", "Make a “K” shape, then tilt your wrist downward — the index finger points forward."),
            new LessonDescription("Letter Q", "Form the same shape as “G”, but point it slightly downward."),
            new LessonDescription("Letter R", "Cross your index and middle fingers."),
            new LessonDescription("Letter S", "Similar to “A” sign, but make a fist with your thumb crossing in front of your fingers"),
            new LessonDescription("Letter T", "Make a “thumbs up” gesture, and place your index finger on top of your thumb."),
            new LessonDescription("Letter U", "Raise your index and middle fingers together, touching side by side."),
            new LessonDescription("Letter V", "Raise your index and middle fingers apart to form a “V” sign."),
            new LessonDescription("Letter W", "Raise your index, middle, and ring fingers to form a “W”."),
            new LessonDescription("Letter X", "Bend your index finger to form a hook while keeping the rest closed."),
            new LessonDescription("Letter Y", "Extend your thumb and pinky finger out, keeping the other fingers closed."),
            new LessonDescription("Letter Z", "Use your index finger to draw a “Z” shape in the air."),
            // ... add all others following your LessonData order
    };

    // Chapter 2 - basic greetings
    public static final LessonDescription[] CHAPTER2 = new LessonDescription[]{
            new LessonDescription("Hello", "Raise your dominant hand near your temple, like giving a casual salute. " +
                    "Move your hand outward slightly as if greeting someone from a distance."
            ),
            new LessonDescription("How are you?", "Place both hands together in front of your chest, palms facing downward. " +
                    "Then, rotate your hands outward so the palms face up, as if showing or presenting something. " +
                    "And point your index finger forward."
            ),
            new LessonDescription("Sorry", "Form a fist with your dominant hand and place it over your chest. " +
                    "Move your fist in a small circular motion a few times."
            ),
            new LessonDescription("Thank you (formal)", "Place both hands near your lips with fingertips touching the mouth."),
            new LessonDescription("Thank you (informal)", "Touch your lips lightly with the fingertips of one hand, then move your hand forward."),
    };

    // Chapter 3 - numbers
    public static final LessonDescription[] CHAPTER3 = new LessonDescription[]{
            new LessonDescription(
                    "Number 1",
                    "Raise your index finger while keeping the other fingers closed into your palm. " +
                            "Make sure your palm faces outward. This simple handshape clearly represents the number one."
            ),
            new LessonDescription(
                    "Number 2",
                    "Extend your index and middle fingers together while keeping the other fingers folded down. " +
                            "Your palm should face outward, similar to a peace sign, representing the number two."
            ),
            new LessonDescription(
                    "Number 3",
                    "Raise your thumb, index finger, and middle finger while keeping your ring and pinky fingers folded down. " +
                            "Hold your palm facing outward — this represent the number three."
            ),
            new LessonDescription(
                    "Number 4",
                    "Extend all four fingers upward — index, middle, ring, and pinky — with your thumb tucked against your palm. " +
                            "This clearly represents the number four."
            ),
            new LessonDescription(
                    "Number 5",
                    "Spread out all five fingers with your palm facing forward, as if showing your whole hand. " +
                            "This open-hand gesture symbolizes the number five."
            ),
            new LessonDescription(
                    "Number 6",
                    "Touch the tip of your thumb to the tip of your pinky finger, forming a small circle. " +
                            "Keep your other three fingers extended upward. " +
                            "This gesture visually connects the smallest and largest fingers to represent the number six."
            ),
            new LessonDescription(
                    "Number 7",
                    "Touch the tip of your thumb to the tip of your ring finger while keeping the other fingers extended. " +
                            "This creates a shape that symbolizes the number seven."
            ),
            new LessonDescription(
                    "Number 8",
                    "Touch the tip of your thumb to your middle finger, with your other fingers extended upward. " +
                            "This shape is used to represent the number eight."
            ),
            new LessonDescription(
                    "Number 9",
                    "Touch the tip of your thumb to your index finger to form a small circle, keeping the rest of your fingers extended. " +
                            "It’s similar to an 'OK' sign — symbolizing the number nine."
            ),
            new LessonDescription(
                    "Number 10",
                    "Make a closed fist and slightly shake it side to side, or extend your thumb upward like a thumbs-up sign. " +
                            "This gesture is used to represent the number ten."
            )
    };

    // Chapter 4 - questions
    public static final LessonDescription[] CHAPTER4 = new LessonDescription[]{
            new LessonDescription(
                    "What",
                    "Hold one hand open in front of your chest with your palm facing upward. " +
                            "Move your hand slightly side to side while raising your eyebrows. "
            ),
            new LessonDescription(
                    "When",
                    "Raise your index finger of one hand and use the index finger of your other hand to trace a small circular motion around it. " +
                            "Then, point your finger forward while slightly raising your eyebrows. "
            ),
            new LessonDescription(
                    "Where",
                    "Raise both of your hands and move it side to side gently, with your palm facing outward. " +
                            "Tilt your head slightly and raise your eyebrows to show you’re asking a question. "
            ),
            new LessonDescription(
                    "Which",
                    "Hold both hands in front of your chest with thumbs extended upward and other fingers closed. " +
                            "Move each hand alternately up and down, as if comparing two choices."
            ),
            new LessonDescription(
                    "Who",
                    "Form a slightly open hand near your chin. " +
                            "Gently close it up until you form a shape that looks like you are grabbing something tiny with all of your fingers. " +
                            "Use a curious facial expression to show you’re asking, 'Who?'"
            ),
            new LessonDescription(
                    "Why",
                    "Touch your forehead with your fingertips, then move your hand outward and downward while changing it into a 'Y' sign (thumb and pinky extended). "
            ),
            new LessonDescription(
                    "How",
                    "Place both hands together in front of your chest, palms facing downward. " +
                            "Then, rotate your hands outward so the palms face up, as if showing or presenting something. "
            )
    };

    // Chapter 2 - days of the week
    public static final LessonDescription[] CHAPTER5 = new LessonDescription[]{
            new LessonDescription(
                    "Monday",
                    "Form the letter 'M' by placing your thumb under your first three fingers. " +
                            "Then make a small circular motion in front of your chest. "
            ),
            new LessonDescription(
                    "Tuesday",
                    "Make the letter 'T' by placing your index finger on top of your thumb. " +
                            "Move your hand in a small circle in front of your chest. "
            ),
            new LessonDescription(
                    "Wednesday",
                    "Form the letter 'W' using your three middle fingers raised, with your thumb and pinky down. " +
                            "Move your hand slightly in a circular motion in front of your chest. "
            ),
            new LessonDescription(
                    "Thursday",
                    "Combine the letters 'T' and 'H' in sequence. " +
                            "First, form the letter 'T' with your index finger on top of your thumb, " +
                            "then slide your hand into the 'H' shape (index and middle fingers extended together, palm facing sideways). "
            ),
            new LessonDescription(
                    "Friday",
                    "Make the letter 'F' by touching your index finger and thumb together to form a small circle, " +
                            "while the other fingers stay extended. " +
                            "Move the hand slightly in front of your chest. "
            ),
            new LessonDescription(
                    "Saturday",
                    "Form the letter 'S' by making a closed fist with your thumb placed over your fingers. " +
                            "Move both of your hand in a small circular motion in front of your chest. "
            ),
            new LessonDescription(
                    "Sunday",
                    "Hold both open hands with palms facing forward. Then move them slightly in a circular motion together. "
            )
    };
}
