package com.example.gabay.fragments.pages;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.gabay.R;
import com.example.gabay.activities.AuthActivity;
import com.example.gabay.activities.MainActivity;
import com.example.gabay.services.SupabaseJavaService;
import com.example.gabay.services.SupabaseHelper;

import org.json.JSONObject;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;
import java.io.IOException;

public class SignInPage extends Fragment {

    private EditText emailInput, passwordInput;
    private Button signInButton;
    private ImageButton signInGoogle;
    private TextView tvCreateAcc, forgotPass;
    private CheckBox checkBox_rememberMe;

    private SharedPreferences sharedPreferences;
    private static final String PREFS_NAME = "AuthPrefs";
    private static final String KEY_EMAIL = "email";
    private static final String KEY_PASSWORD = "password";
    private static final String KEY_REMEMBER_ME = "rememberMe";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_sign_in, container, false);

        emailInput = view.findViewById(R.id.signIn_email);
        passwordInput = view.findViewById(R.id.signIn_Password);
        signInButton = view.findViewById(R.id.signIn_button);
        tvCreateAcc = view.findViewById(R.id.tv_createAcc);
        signInGoogle = view.findViewById(R.id.signIn_google);
        forgotPass = view.findViewById(R.id.tv_forgotPass);
        checkBox_rememberMe = view.findViewById(R.id.checkBox_rememberMe);

        // Initialize SharedPreferences
        sharedPreferences = requireActivity().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        // Load saved credentials if "Remember Me" was checked
        loadSavedCredentials();

        signInButton.setOnClickListener(v -> handleSignIn());

        // Create account
        tvCreateAcc.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Navigate to SignUpPage
                if (getActivity() instanceof AuthActivity) {
                    ((AuthActivity) getActivity()).loadSignUpFragment();
                }
            }
        });

        // Google sign in
        signInGoogle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Optional: Show loading state
                signInGoogle.setEnabled(false);

                // Trigger Google Sign-In
                if (getActivity() instanceof AuthActivity) {
                    ((AuthActivity) getActivity()).signInWithGoogle();
                }

                // Re-enable after a delay or in onActivityResult
                signInGoogle.postDelayed(() -> signInGoogle.setEnabled(true), 2000);
            }
        });

        // Forgot password
        forgotPass.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handleForgotPassword();
            }
        });

        return view;
    }

    private void loadSavedCredentials() {
        boolean rememberMe = sharedPreferences.getBoolean(KEY_REMEMBER_ME, false);
        if (rememberMe) {
            String savedEmail = sharedPreferences.getString(KEY_EMAIL, "");
            String savedPassword = sharedPreferences.getString(KEY_PASSWORD, "");

            emailInput.setText(savedEmail);
            passwordInput.setText(savedPassword);
            checkBox_rememberMe.setChecked(true);
        }
    }

    private void saveCredentials(String email, String password, boolean rememberMe) {
        SharedPreferences.Editor editor = sharedPreferences.edit();

        if (rememberMe) {
            editor.putString(KEY_EMAIL, email);
            editor.putString(KEY_PASSWORD, password);
            editor.putBoolean(KEY_REMEMBER_ME, true);
        } else {
            editor.remove(KEY_EMAIL);
            editor.remove(KEY_PASSWORD);
            editor.putBoolean(KEY_REMEMBER_ME, false);
        }
        editor.apply();
    }

    private void clearCredentials() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.remove(KEY_EMAIL);
        editor.remove(KEY_PASSWORD);
        editor.remove(KEY_REMEMBER_ME);
        editor.apply();
    }

    private void handleSignIn() {
        String email = emailInput.getText().toString().trim();
        String password = passwordInput.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(getContext(), "Please enter email and password", Toast.LENGTH_SHORT).show();
            return;
        }

        // Show loading state
        signInButton.setEnabled(false);
        signInButton.setText("Signing In...");

        // Call the Supabase signIn function
        SupabaseHelper.signIn(email, password, new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                if (getActivity() == null) return;
                getActivity().runOnUiThread(() -> {
                    signInButton.setEnabled(true);
                    signInButton.setText("Continue");
                    Toast.makeText(getContext(), "Login failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                String responseBody = response.body().string();

                if (getActivity() == null) return;
                getActivity().runOnUiThread(() -> {
                    signInButton.setEnabled(true);
                    signInButton.setText("Continue");

                    if (response.isSuccessful()) {
                        // Save credentials if "Remember Me" is checked
                        boolean rememberMe = checkBox_rememberMe.isChecked();
                        saveCredentials(email, password, rememberMe);

                        // Extract and save auth tokens
                        try {
                            JSONObject jsonResponse = new JSONObject(responseBody);
                            String accessToken = jsonResponse.optString("access_token");
                            String refreshToken = jsonResponse.optString("refresh_token");

                            // IMPORTANT: Extract user ID from the response
                            JSONObject user = jsonResponse.getJSONObject("user");
                            String userId = user.getString("id");

                            // Save tokens to SharedPreferences
                            saveAuthTokens(accessToken, refreshToken);

                            // ⭐ ADD THIS: Set the token in SupabaseService
                            SupabaseJavaService.setAccessToken(accessToken, userId);
                            Log.d("SignIn", "✅ SupabaseService authenticated for user: " + userId);

                            Toast.makeText(getContext(), "Login successful!", Toast.LENGTH_SHORT).show();

                            // Navigate to MainActivity
                            navigateToMainActivity();

                        } catch (Exception e) {
                            e.printStackTrace();
                            Log.e("SignIn", "Error parsing auth response: " + e.getMessage());
                            Toast.makeText(getContext(), "Login error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(getContext(), "Error: " + responseBody, Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }

    private void saveAuthTokens(String accessToken, String refreshToken) {
        SharedPreferences sharedPreferences = requireActivity().getSharedPreferences("AuthPrefs", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("access_token", accessToken);
        editor.putString("refresh_token", refreshToken);
        editor.apply();
    }

    private void navigateToMainActivity() {
        Intent intent = new Intent(getActivity(), MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        if (getActivity() != null) {
            getActivity().finish();
        }
    }

    private void handleForgotPassword() {
        String email = emailInput.getText().toString().trim();

        if (email.isEmpty()) {
            Toast.makeText(getContext(), "Please enter your email address", Toast.LENGTH_SHORT).show();
            return;
        }

        // TODO: Implement Supabase password reset
        // This is a placeholder - you'll need to add this method to SupabaseHelper
        Toast.makeText(getContext(), "Password reset email sent to " + email, Toast.LENGTH_SHORT).show();

        // Example implementation (you'll need to add this to SupabaseHelper):
        // SupabaseHelper.resetPassword(email, new Callback() {
        //     @Override
        //     public void onFailure(@NonNull Call call, @NonNull IOException e) {
        //         // Handle error
        //     }
        //
        //     @Override
        //     public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
        //         // Handle response
        //     }
        // });
    }

    // Optional: Add a method to clear credentials when user signs out
    public static void clearSavedCredentials(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.clear();
        editor.apply();
    }
}