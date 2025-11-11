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
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.example.gabay.R;

import com.example.gabay.services.DisposableEmailChecker;
import com.example.gabay.services.SupabaseHelper;

import org.json.JSONObject;

import java.io.IOException;
import java.util.regex.Pattern;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class SignUpPage extends Fragment {
    private TextView tvSignIn;
    private EditText signUp_dispName, signUp_email, signUp_Password, signUp_confirmPassword;
    private Button signUp_button;
    private CheckBox checkBox_showPass;
    private TextView signUp_error_message;
    private TextView signUp_displayName_error, signUp_email_error, signUp_password_error, signUp_confirmPassword_error;
    private ProgressBar progressBar;
    private View contentLayout;

    // Password strength requirements
    private static final int MIN_PASSWORD_LENGTH = 8;
    private static final int MAX_PASSWORD_LENGTH = 128;
    private static final Pattern PASSWORD_PATTERN = Pattern.compile(
            "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).+$"
    );

    // Rate limiting
    private long lastSignUpAttempt = 0;
    private static final long SIGN_UP_COOLDOWN = 3000; // 3 seconds
    private int failedAttempts = 0;
    private static final int MAX_FAILED_ATTEMPTS = 5;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        DisposableEmailChecker.initialize(requireContext());
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_sign_up_page, container, false);

        initializeViews(view);
        setupListeners();
        setupPasswordVisibility();
        setupPasswordMatching();
        setupRealTimeValidation();

        return view;
    }

    private void initializeViews(View view) {
        tvSignIn = view.findViewById(R.id.tv_signIn);
        signUp_dispName = view.findViewById(R.id.signUp_displayName);
        signUp_email = view.findViewById(R.id.signUp_email);
        signUp_Password = view.findViewById(R.id.signUp_Password);
        signUp_confirmPassword = view.findViewById(R.id.signUp_confirmPassword);
        signUp_button = view.findViewById(R.id.signUp_button);
        checkBox_showPass = view.findViewById(R.id.checkBox_showPass);
        signUp_error_message = view.findViewById(R.id.signUp_error_message);
        signUp_displayName_error = view.findViewById(R.id.signUp_displayName_error);
        signUp_email_error = view.findViewById(R.id.signUp_email_error);
        signUp_password_error = view.findViewById(R.id.signUp_password_error);
        signUp_confirmPassword_error = view.findViewById(R.id.signUp_confirmPassword_error);

        // Optional: Add these to your layout
        // progressBar = view.findViewById(R.id.progressBar);
        // contentLayout = view.findViewById(R.id.contentLayout);

        signUp_button.setEnabled(false);
    }

    private void setupListeners() {
        tvSignIn.setOnClickListener(v -> {
            if (getActivity() != null) {
                getActivity().onBackPressed();
            }
        });

        signUp_button.setOnClickListener(v -> handleSignUp());
    }

    private void setupPasswordVisibility() {
        checkBox_showPass.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                signUp_Password.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
                signUp_confirmPassword.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
            } else {
                signUp_Password.setTransformationMethod(PasswordTransformationMethod.getInstance());
                signUp_confirmPassword.setTransformationMethod(PasswordTransformationMethod.getInstance());
            }

            signUp_Password.setSelection(signUp_Password.getText().length());
            signUp_confirmPassword.setSelection(signUp_confirmPassword.getText().length());
        });
    }

    private void setupPasswordMatching() {
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
                    showFieldError(signUp_confirmPassword_error, "Passwords do not match");
                } else {
                    hideFieldError(signUp_confirmPassword_error);
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

        signUp_dispName.addTextChangedListener(validationWatcher);
        signUp_email.addTextChangedListener(validationWatcher);
        signUp_Password.addTextChangedListener(validationWatcher);
        signUp_confirmPassword.addTextChangedListener(validationWatcher);
    }

    private void validateFieldsRealTime() {
        String displayName = signUp_dispName.getText().toString().trim();
        String email = signUp_email.getText().toString().trim();
        String password = signUp_Password.getText().toString();
        String confirmPassword = signUp_confirmPassword.getText().toString();

        hideFieldError(signUp_displayName_error);
        hideFieldError(signUp_email_error);
        hideFieldError(signUp_password_error);
        hideFieldError(signUp_confirmPassword_error);
        hideErrorMessage();

        boolean displayNameValid = validateDisplayName(displayName, true);
        boolean emailValid = validateEmail(email, true);
        boolean passwordValid = validatePassword(password, true);
        boolean confirmPasswordValid = validateConfirmPassword(password, confirmPassword, true);

        signUp_button.setEnabled(displayNameValid && emailValid && passwordValid && confirmPasswordValid);
    }

    private boolean validateDisplayName(String displayName, boolean showErrors) {
        if (displayName.isEmpty()) {
            return false;
        }

        if (displayName.length() < 3) {
            if (showErrors) showFieldError(signUp_displayName_error, "Username must be at least 3 characters");
            return false;
        }

        if (displayName.length() > 30) {
            if (showErrors) showFieldError(signUp_displayName_error, "Username must be less than 30 characters");
            return false;
        }

        if (!displayName.matches("^[a-zA-Z0-9_]+$")) {
            if (showErrors) showFieldError(signUp_displayName_error, "Username can only contain letters, numbers, and underscores");
            return false;
        }

        // malicious patterns checker
        String lowerName = displayName.toLowerCase();
        if (lowerName.contains("admin") || lowerName.contains("moderator") ||
                lowerName.contains("support") || lowerName.contains("system")) {
            if (showErrors) showFieldError(signUp_displayName_error, "This username is not allowed");
            return false;
        }

        return true;
    }

    private boolean validateEmail(String email, boolean showErrors) {
        if (email.isEmpty()) {
            return false;
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            if (showErrors) showFieldError(signUp_email_error, "Please enter a valid email address");
            return false;
        }

        if (email.length() > 254) {
            if (showErrors) showFieldError(signUp_email_error, "Email address is too long");
            return false;
        }

        // disposable email checker
        if (DisposableEmailChecker.isDisposable(email)) {
            if (showErrors) showFieldError(signUp_email_error, "Temporary email addresses are not allowed");
            return false;
        }

        // some email security checks
        String[] parts = email.split("@");
        if (parts.length == 2) {
            String localPart = parts[0];
            if (localPart.length() > 64) {
                if (showErrors) showFieldError(signUp_email_error, "Email address is invalid");
                return false;
            }
        }

        return true;
    }

    private boolean validatePassword(String password, boolean showErrors) {
        if (password.isEmpty()) {
            return false;
        }

        if (password.length() < MIN_PASSWORD_LENGTH) {
            if (showErrors) showFieldError(signUp_password_error, "Password must be at least 8 characters");
            return false;
        }

        if (password.length() > MAX_PASSWORD_LENGTH) {
            if (showErrors) showFieldError(signUp_password_error, "Password must be less than 128 characters");
            return false;
        }

        // Enhanced password strength requirements
        if (!PASSWORD_PATTERN.matcher(password).matches()) {
            if (showErrors) showFieldError(signUp_password_error, "Password must contain uppercase, lowercase, and number");
            return false;
        }

        // Check for common weak passwords
        String lowerPassword = password.toLowerCase();
        if (lowerPassword.contains("password") || lowerPassword.contains("12345678") ||
                lowerPassword.contains("qwerty") || lowerPassword.equals(signUp_dispName.getText().toString().toLowerCase())) {
            if (showErrors) showFieldError(signUp_password_error, "Password is too weak");
            return false;
        }

        return true;
    }

    private boolean validateConfirmPassword(String password, String confirmPassword, boolean showErrors) {
        if (confirmPassword.isEmpty()) {
            return false;
        }

        if (!confirmPassword.equals(password)) {
            if (showErrors) showFieldError(signUp_confirmPassword_error, "Passwords do not match");
            return false;
        }

        return true;
    }

    private void showFieldError(TextView errorView, String message) {
        if (errorView != null) {
            errorView.setText(message);
            errorView.setVisibility(View.VISIBLE);
        }
    }

    private void hideFieldError(TextView errorView) {
        if (errorView != null) {
            errorView.setVisibility(View.GONE);
        }
    }

    private void showErrorMessage(String message) {
        if (signUp_error_message != null) {
            signUp_error_message.setText(message);
            signUp_error_message.setVisibility(View.VISIBLE);
        }
    }

    private void hideErrorMessage() {
        if (signUp_error_message != null) {
            signUp_error_message.setVisibility(View.GONE);
        }
    }

    private void handleSignUp() {
        // Rate limiting check
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastSignUpAttempt < SIGN_UP_COOLDOWN) {
            showErrorMessage("Please wait a moment before trying again");
            return;
        }

        // Check for too many failed attempts
        if (failedAttempts >= MAX_FAILED_ATTEMPTS) {
            showErrorMessage("Too many failed attempts. Please try again later.");
            return;
        }

        lastSignUpAttempt = currentTime;

        String displayName = signUp_dispName.getText().toString().trim();
        String email = signUp_email.getText().toString().trim();
        String password = signUp_Password.getText().toString();
        String confirmPassword = signUp_confirmPassword.getText().toString();

        // Clear previous errors
        hideFieldError(signUp_displayName_error);
        hideFieldError(signUp_email_error);
        hideFieldError(signUp_password_error);
        hideFieldError(signUp_confirmPassword_error);
        hideErrorMessage();

        // Comprehensive validation
        boolean hasErrors = false;

        if (!validateDisplayName(displayName, true)) {
            hasErrors = true;
        }

        if (!validateEmail(email, true)) {
            hasErrors = true;
        }

        if (!validatePassword(password, true)) {
            hasErrors = true;
        }

        if (!validateConfirmPassword(password, confirmPassword, true)) {
            hasErrors = true;
        }

        if (hasErrors) {
            showErrorMessage("Please fix the errors above");
            return;
        }

        displayName = sanitizeInput(displayName);
        email = sanitizeInput(email);

        setLoadingState(true);

        SupabaseHelper.signUp(displayName, email, password, new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e("SignUpPage", "Network failure: " + e.getMessage());
                if (getActivity() == null) return;

                getActivity().runOnUiThread(() -> {
                    setLoadingState(false);
                    failedAttempts++;
                    showErrorMessage("Network error. Please check your connection and try again.");
                });
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                String responseBody = response.body().string();
                int responseCode = response.code();

                Log.d("SignUpPage", "Response Code: " + responseCode);

                if (getActivity() == null) return;

                getActivity().runOnUiThread(() -> {
                    setLoadingState(false);

                    if (response.isSuccessful()) {
                        handleSuccessfulSignUp(responseBody);
                    } else {
                        handleSignUpError(responseCode, responseBody);
                    }
                });
            }
        });
    }

    private void handleSuccessfulSignUp(String responseBody) {
        try {
            JSONObject jsonResponse = new JSONObject(responseBody);
            JSONObject user = jsonResponse.optJSONObject("user");

            if (user != null) {
                // Reset failed attempts on success
                failedAttempts = 0;

                showErrorMessage("Account created! Please check your email to verify your account before signing in.");

                clearFields();

                new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                    if (getActivity() != null) {
                        getActivity().onBackPressed();
                    }
                }, 3000);
            } else {
                showErrorMessage("Account created. Please check your email for verification instructions.");
            }
        } catch (Exception e) {
            Log.e("SignUpPage", "Error parsing success response: " + e.getMessage());
            showErrorMessage("Account created. Please check your email for verification instructions.");
        }
    }

    private void handleSignUpError(int responseCode, String responseBody) {
        failedAttempts++;
        String errorMessage = "Sign-up failed. Please try again.";

        try {
            JSONObject errorJson = new JSONObject(responseBody);
            String errorDescription = errorJson.optString("error_description", "");
            String errorMsg = errorJson.optString("msg", "");
            String error = errorJson.optString("error", "");

            String combinedError = (errorDescription + " " + errorMsg + " " + error).toLowerCase();

            if (combinedError.contains("user already registered") ||
                    combinedError.contains("email already exists") ||
                    combinedError.contains("already registered")) {
                showFieldError(signUp_email_error, "Email already registered");
                errorMessage = "An account with this email already exists. Please sign in instead.";
            } else if (combinedError.contains("rate limit")) {
                errorMessage = "Too many requests. Please wait a moment and try again.";
            } else if (combinedError.contains("invalid") && combinedError.contains("email")) {
                showFieldError(signUp_email_error, "Invalid email address");
                errorMessage = "Please provide a valid email address.";
            } else if (!errorDescription.isEmpty()) {
                errorMessage = errorDescription;
            } else if (!errorMsg.isEmpty()) {
                errorMessage = errorMsg;
            } else if (!error.isEmpty()) {
                errorMessage = error;
            }

            Log.e("SignUpPage", "Sign-up error: " + combinedError);
        } catch (Exception e) {
            Log.e("SignUpPage", "Could not parse error: " + responseBody);

            if (responseCode == 429) {
                errorMessage = "Too many requests. Please wait a moment and try again.";
            } else if (responseCode == 400 || responseCode == 422) {
                errorMessage = "Invalid information provided. Please check your entries.";
            } else if (responseCode == 409) {
                showFieldError(signUp_email_error, "Email already registered");
                errorMessage = "An account with this email already exists. Please sign in instead.";
            }
        }

        showErrorMessage(errorMessage);
    }

    private void setLoadingState(boolean isLoading) {
        signUp_button.setEnabled(!isLoading);
        signUp_button.setText(isLoading ? "Creating Account..." : "Register");

        // Disable input fields during loading
        signUp_dispName.setEnabled(!isLoading);
        signUp_email.setEnabled(!isLoading);
        signUp_Password.setEnabled(!isLoading);
        signUp_confirmPassword.setEnabled(!isLoading);
        checkBox_showPass.setEnabled(!isLoading);

        // Optional: Show/hide progress bar
        // if (progressBar != null) {
        //     progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        // }
    }

    private void clearFields() {
        signUp_Password.setText("");
        signUp_confirmPassword.setText("");
    }

    private String sanitizeInput(String input) {
        if (input == null) return "";
        // Remove any potential script tags or SQL injection attempts
        return input.replaceAll("[<>\"';]", "");
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Clear sensitive data
        if (signUp_Password != null) signUp_Password.setText("");
        if (signUp_confirmPassword != null) signUp_confirmPassword.setText("");
    }
}