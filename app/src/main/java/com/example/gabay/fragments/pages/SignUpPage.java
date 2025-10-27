package com.example.gabay.fragments.pages;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.util.Log;
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

import com.example.gabay.services.DisposableEmailChecker;
import com.example.gabay.services.SupabaseHelper;

import org.json.JSONObject;

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

        DisposableEmailChecker.initialize(requireContext());
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

        // validations
        setupPasswordVisibility();
        setupPasswordMatching();
        setupRealTimeValidation();


        return view;
    }

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

    private void setupRealTimeValidation() {
        TextWatcher validationWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                validateFieldsRealTime();
            }
        };

        // Apply to all input fields
        signUp_dispName.addTextChangedListener(validationWatcher);
        signUp_email.addTextChangedListener(validationWatcher);
        signUp_Password.addTextChangedListener(validationWatcher);
        signUp_confirmPassword.addTextChangedListener(validationWatcher);
    }

    private void validateFieldsRealTime() {
        String displayName = signUp_dispName.getText().toString().trim();
        String email = signUp_email.getText().toString().trim();
        String password = signUp_Password.getText().toString().trim();
        String confirmPassword = signUp_confirmPassword.getText().toString().trim();

        // email validation
        if (email.length() > 0) {
            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                signUp_email.setError("Please enter a valid email address");
            } else if (DisposableEmailChecker.isDisposable(email)) {
                signUp_email.setError("Temporary email addresses are not allowed");
            } else {
                signUp_email.setError(null);
            }
        }

        // password validations
        if (password.length() > 0) {
            if (password.length() < 8) {
                signUp_Password.setError("Password must be at least 8 characters");
            } else if (!containsUppercase(password)) {
                signUp_Password.setError("Password must contain at least 1 uppercase letter");
            } else {
                signUp_Password.setError(null);
            }
        } else {
            signUp_Password.setError(null);
        }

        if (displayName.isEmpty()){
            signUp_dispName.setError("Please enter your name");
        } else {
            signUp_dispName.setError(null);
        }

        if (confirmPassword.length() > 0 && !confirmPassword.equals(password)) {
            signUp_confirmPassword.setError("Passwords do not match");
        } else {
            signUp_confirmPassword.setError(null);
        }

        // enable/disable signup button in real-time
        boolean allValid = !displayName.isEmpty() &&
                !email.isEmpty() &&
                !password.isEmpty() &&
                !confirmPassword.isEmpty() &&
                android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() &&
                !DisposableEmailChecker.isDisposable(email) &&
                password.length() >= 8 &&
                containsUppercase(password) &&
                password.equals(confirmPassword);

        signUp_button.setEnabled(allValid);
    }

    // helper method to check for uppercase letters
    private boolean containsUppercase(String password) {
        for (char c : password.toCharArray()) {
            if (Character.isUpperCase(c)) {
                return true;
            }
        }
        return false;
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

        if (DisposableEmailChecker.isDisposable(email)) {
            Toast.makeText(getContext(),
                    "Temporary email addresses are not allowed. Please use a permanent email address.",
                    Toast.LENGTH_LONG).show();
            return;
        }

            if (password.length() < 8) {
            Toast.makeText(getContext(), "Password must be at least 8 characters", Toast.LENGTH_SHORT).show();
            return;
        }


        // Show loading state
        signUp_button.setEnabled(false);
        signUp_button.setText("Creating Account...");

        SupabaseHelper.signUp(displayName, email, password, new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e("SignUpPage", "Network failure: " + e.getMessage());
                if (getActivity() == null) return;
                getActivity().runOnUiThread(() -> {
                    signUp_button.setEnabled(true);
                    signUp_button.setText("Sign Up");
                    Toast.makeText(getContext(), "Network error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                String responseBody = response.body().string();
                int responseCode = response.code();

                // Log the full response for debugging
                Log.d("SignUpPage", "Response Code: " + responseCode);
                Log.d("SignUpPage", "Response Body: " + responseBody);

                if (getActivity() == null) return;
                getActivity().runOnUiThread(() -> {
                    signUp_button.setEnabled(true);
                    signUp_button.setText("Sign Up");

                    if (response.isSuccessful()) {
                        // Check if user needs to verify email
                        try {
                            JSONObject jsonResponse = new JSONObject(responseBody);
                            JSONObject user = jsonResponse.optJSONObject("user");

                            if (user != null) {
                                String confirmedAt = user.optString("confirmed_at", "");

                                if (confirmedAt.isEmpty() || confirmedAt.equals("null")) {
                                    // Email confirmation required
                                    Toast.makeText(getContext(),
                                            "Account created! Please check your email to verify your account.",
                                            Toast.LENGTH_LONG).show();
                                } else {
                                    // Account is ready
                                    Toast.makeText(getContext(),
                                            "Account created successfully!",
                                            Toast.LENGTH_LONG).show();
                                }

                                // Navigate back to sign-in page
                                if (getActivity() != null) {
                                    getActivity().onBackPressed();
                                }
                            } else {
                                Toast.makeText(getContext(),
                                        "Account created but response format unexpected. Check your email.",
                                        Toast.LENGTH_LONG).show();
                            }
                        } catch (Exception e) {
                            Log.e("SignUpPage", "Error parsing success response: " + e.getMessage());
                            Toast.makeText(getContext(),
                                    "Account might be created. Please check your email.",
                                    Toast.LENGTH_LONG).show();
                        }
                    } else {
                        // Parse error message
                        String errorMessage = "Sign-up failed";
                        try {
                            JSONObject errorJson = new JSONObject(responseBody);

                            // Try different error message fields
                            if (errorJson.has("msg")) {
                                errorMessage = errorJson.getString("msg");
                            } else if (errorJson.has("message")) {
                                errorMessage = errorJson.getString("message");
                            } else if (errorJson.has("error_description")) {
                                errorMessage = errorJson.getString("error_description");
                            } else if (errorJson.has("error")) {
                                errorMessage = errorJson.getString("error");
                            }

                            Log.e("SignUpPage", "Error response: " + errorMessage);
                        } catch (Exception e) {
                            Log.e("SignUpPage", "Could not parse error: " + responseBody);
                            errorMessage = "Sign-up failed: " + responseBody;
                        }

                        Toast.makeText(getContext(), errorMessage, Toast.LENGTH_LONG).show();
                    }
                });
            }
        });
    }
}