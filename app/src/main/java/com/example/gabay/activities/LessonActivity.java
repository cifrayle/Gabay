package com.example.gabay.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.*;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.gabay.R;

public class LessonActivity extends AppCompatActivity {
    private ImageButton toMainActivity;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_lesson);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);


            //Initialize back button from lesson to main
            toMainActivity = findViewById(R.id.lesson_activity_back_button);
            toMainActivity.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    goToMainActivity();
                }
            });

            return insets;
        });
    }

    public void goToMainActivity(){
        Intent intent = new Intent(this, MainActivity.class);

        startActivity(intent);
    }
}