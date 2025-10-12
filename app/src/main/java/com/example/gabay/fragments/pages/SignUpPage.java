package com.example.gabay.fragments.pages;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.example.gabay.R;
import com.example.gabay.activities.AuthActivity;

import com.example.gabay.R;
import com.example.gabay.utils.SupabaseHelper;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class SignUpPage extends Fragment {
    private TextView tvSignIn;
    private EditText signUp_dispName, signUp_email, signUp_Password, signUp_confirmPassword;
    private Button signUp_button;
    private ImageButton signUpGoogle;
    private CheckBox checkBox_showPass;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_sign_up_page, container, false);

        tvSignIn = view.findViewById(R.id.tv_signIn);
        signUp_dispName = view.findViewById(R.id.signUp_displayName);
        signUp_email = view.findViewById(R.id.signUp_email);
        signUp_Password = view.findViewById(R.id.signUp_Password);
        signUp_confirmPassword = view.findViewById(R.id.signUp_confirmPassword);
        signUp_button = view.findViewById(R.id.signUp_button);
        signUpGoogle = view.findViewById(R.id.signUp_google);
        checkBox_showPass = view.findViewById(R.id.checkBox_showPass);

        tvSignIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Navigate back to SignInPage
                if (getActivity() != null) {
                    getActivity().onBackPressed();
                }
            }
        });

        signUpGoogle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Optional: Show loading state
                signUpGoogle.setEnabled(false);

                // Trigger Google Sign-In
                if (getActivity() instanceof AuthActivity) {
                    ((AuthActivity) getActivity()).signInWithGoogle();
                }

                // Re-enable after a delay or in onActivityResult
                signUpGoogle.postDelayed(() -> signUpGoogle.setEnabled(true), 1500);
            }
        });

        signUp_button.setOnClickListener(v -> handleSignUp());

        setupPasswordVisibility();
        setupPasswordMatching();

        return view;
    }

    // show pass
    private void setupPasswordVisibility() {
        checkBox_showPass.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    // Show passwords
                    signUp_Password.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
                    signUp_confirmPassword.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
                } else {
                    // Hide passwords
                    signUp_Password.setTransformationMethod(PasswordTransformationMethod.getInstance());
                    signUp_confirmPassword.setTransformationMethod(PasswordTransformationMethod.getInstance());
                }

                // Move cursor to the end of text for both fields
                signUp_Password.setSelection(signUp_Password.getText().length());
                signUp_confirmPassword.setSelection(signUp_confirmPassword.getText().length());
            }
        });
    }

    // pass matching
    private void setupPasswordMatching() {
        // Add text watcher to show/hide password mismatch error in real-time
        TextWatcher passwordWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                String password = signUp_Password.getText().toString().trim();
                String confirmPassword = signUp_confirmPassword.getText().toString().trim();

                if (!password.isEmpty() && !confirmPassword.isEmpty() && !password.equals(confirmPassword)) {
                    signUp_confirmPassword.setError("Passwords do not match");
                } else {
                    signUp_confirmPassword.setError(null);
                }
            }
        };

        signUp_Password.addTextChangedListener(passwordWatcher);
        signUp_confirmPassword.addTextChangedListener(passwordWatcher);
    }
    private void handleSignUp() {
        String displayName = signUp_dispName.getText().toString().trim();
        String email = signUp_email.getText().toString().trim();
        String password = signUp_Password.getText().toString().trim();
        String confirmPassword = signUp_confirmPassword.getText().toString().trim();

        // Validation
        if (displayName.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            Toast.makeText(getContext(), "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(getContext(), "Please enter a valid email address", Toast.LENGTH_SHORT).show();
            return;
        }

        if (password.length() < 8) {
            Toast.makeText(getContext(), "Password must be at least 8 characters", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!password.equals(confirmPassword)) {
            Toast.makeText(getContext(), "Passwords do not match", Toast.LENGTH_SHORT).show();
            return;
        }

        // Show loading state
        signUp_button.setEnabled(false);
        signUp_button.setText("Creating Account...");

        SupabaseHelper.signUp(displayName, email, password, new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                if (getActivity() == null) return;
                getActivity().runOnUiThread(() -> {
                    signUp_button.setEnabled(true);
                    signUp_button.setText("Register");
                    Toast.makeText(getContext(), "Network error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                String responseBody = response.body().string();

                if (getActivity() == null) return;
                getActivity().runOnUiThread(() -> {
                    signUp_button.setEnabled(true);
                    signUp_button.setText("Sign Up");

                    if (response.isSuccessful()) {
                        Toast.makeText(getContext(), "Sign-up successful! Please check your email for verification.", Toast.LENGTH_LONG).show();
                        // Optionally navigate back to sign-in page
                        if (getActivity() != null) {
                            getActivity().onBackPressed();
                        }
                    } else {
                        // Parse error message for better user feedback
                        String errorMessage = "Sign-up failed";
                        try {
                            org.json.JSONObject errorJson = new org.json.JSONObject(responseBody);
                            errorMessage = errorJson.optString("message", errorJson.optString("error", responseBody));
                        } catch (Exception e) {
                            errorMessage = responseBody;
                        }
                        Toast.makeText(getContext(), errorMessage, Toast.LENGTH_LONG).show();
                    }
                });
            }
        });
    }
}