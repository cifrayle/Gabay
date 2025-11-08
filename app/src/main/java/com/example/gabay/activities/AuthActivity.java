package com.example.gabay.activities;

import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentTransaction;

import com.example.gabay.R;
import com.example.gabay.fragments.pages.SignInPage;
import com.example.gabay.fragments.pages.SignUpPage;
import com.example.gabay.services.SupabaseJavaService;
import com.example.gabay.services.SupabaseHelper;
import com.google.android.gms.auth.api.identity.BeginSignInRequest;
import com.google.android.gms.auth.api.identity.BeginSignInResult;
import com.google.android.gms.auth.api.identity.Identity;
import com.google.android.gms.auth.api.identity.SignInClient;
import com.google.android.gms.auth.api.identity.SignInCredential;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;

import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class AuthActivity extends AppCompatActivity {
    private SignInClient oneTapClient;
    private BeginSignInRequest signInRequest;
    private static final int REQ_ONE_TAP = 100;
    private static final String TAG = "AuthActivity";
    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initialize SharedPreferences FIRST
        sharedPreferences = getSharedPreferences("AuthPrefs", MODE_PRIVATE);

        // Check if user is already logged in - THIS MUST BE BEFORE setContentView
        if (isUserLoggedIn()) {
            navigateToMainActivity();
            return; // Important: return here to skip the rest of onCreate
        }

        setContentView(R.layout.activity_auth); // Only call this once!

        loadSignInFragment();

        // google onetap
        oneTapClient = Identity.getSignInClient(this);
        signInRequest = BeginSignInRequest.builder()
                .setGoogleIdTokenRequestOptions(BeginSignInRequest.GoogleIdTokenRequestOptions.builder()
                        .setSupported(true)
                        .setServerClientId("640293016127-s87ko9ho8to4j79accn3i6hrt2pt9cvm.apps.googleusercontent.com") //  WEB CLIENT ID
                        .setFilterByAuthorizedAccounts(false)
                        .build())
                .setAutoSelectEnabled(true)
                .build();
    }

    private boolean isUserLoggedIn() {
        // Check if we have an access token
        String accessToken = sharedPreferences.getString("access_token", null);

        // Optional: You can also check token expiry here
        if (accessToken != null && !accessToken.isEmpty()) {
            Log.d(TAG, "User is already logged in, redirecting to MainActivity");
            return true;
        }

        Log.d(TAG, "No valid login session found");
        return false;
    }

    private void navigateToMainActivity() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish(); // Finish AuthActivity so user can't go back
    }

    public void loadSignInFragment() {
        SignInPage signInFragment = new SignInPage();
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.auth_fragment_container, signInFragment);
        transaction.commit();
    }

    public void loadSignUpFragment() {
        SignUpPage signUpFragment = new SignUpPage();
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.auth_fragment_container, signUpFragment);
        transaction.addToBackStack("signUp");
        transaction.commit();
    }

    // Trigger this when Google button is clicked
    public void signInWithGoogle() {
        oneTapClient.beginSignIn(signInRequest)
                .addOnSuccessListener(this, new OnSuccessListener<BeginSignInResult>() {
                    @Override
                    public void onSuccess(BeginSignInResult result) {
                        try {
                            startIntentSenderForResult(
                                    result.getPendingIntent().getIntentSender(),
                                    REQ_ONE_TAP,
                                    null, 0, 0, 0);
                        } catch (IntentSender.SendIntentException e) {
                            Log.e(TAG, "Couldn't start One Tap UI: " + e.getLocalizedMessage());
                            Toast.makeText(AuthActivity.this, "Sign-in failed to start", Toast.LENGTH_SHORT).show();
                        }
                    }
                })
                .addOnFailureListener(this, new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        // No saved credentials found or other error
                        Log.d(TAG, "One-tap sign-in failed: " + e.getLocalizedMessage());
                        Toast.makeText(AuthActivity.this, "Sign-in failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    public void signOutFromGoogle() {
        if (oneTapClient != null) {
            oneTapClient.signOut().addOnCompleteListener(this, task -> {
                Log.d(TAG, "Google sign-out completed");
            });
        }
        // Also clear any local Google sign-in state
        try {
            GoogleSignInClient googleSignInClient = GoogleSignIn.getClient(this,
                    GoogleSignInOptions.DEFAULT_SIGN_IN);
            googleSignInClient.signOut();
        } catch (Exception e) {
            Log.d(TAG, "Google sign-out cleanup: " + e.getMessage());
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQ_ONE_TAP) {
            try {
                SignInCredential credential = oneTapClient.getSignInCredentialFromIntent(data);
                String idToken = credential.getGoogleIdToken();
                String email = credential.getId();
                String displayName = credential.getDisplayName();

                if (idToken != null) {
                    Log.d(TAG, "Got ID token for email: " + email);
                    exchangeGoogleTokenForSupabaseSession(idToken);
                }
            } catch (ApiException e) {
                Log.e(TAG, "One-tap sign-in failed: " + e.getStatusCode() + ", " + e.getMessage());
                Toast.makeText(this, "Sign-in failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        }
    }

    private void exchangeGoogleTokenForSupabaseSession(String idToken) {
        Log.d(TAG, "Exchanging Google token with Supabase...");
        Log.d(TAG, "ID Token length: " + (idToken != null ? idToken.length() : "null"));

        SupabaseHelper.exchangeGoogleToken(idToken, new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(() -> {
                    Log.e(TAG, "Network error: " + e.getMessage());
                    Toast.makeText(AuthActivity.this, "Network error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                String responseBody = response.body().string();
                runOnUiThread(() -> {
                    Log.d(TAG, "Supabase Response Code: " + response.code());
                    Log.d(TAG, "Supabase Response Body: " + responseBody);

                    if (response.isSuccessful()) {
                        try {
                            JSONObject jsonResponse = new JSONObject(responseBody);
                            String accessToken = jsonResponse.optString("access_token");
                            String refreshToken = jsonResponse.optString("refresh_token");

                            // Extract user ID from the response
                            JSONObject user = jsonResponse.getJSONObject("user");
                            String userId = user.getString("id");

                            // Save tokens to SharedPreferences
                            saveAuthTokens(accessToken, refreshToken);

                            // Set the token in SupabaseService
                            SupabaseJavaService.setAccessToken(accessToken, userId);
                            Log.d(TAG, "SupabaseService authenticated for user: " + userId);


                            createUserProfileIfNeeded(user);
                            Toast.makeText(AuthActivity.this, "Signed in successfully!", Toast.LENGTH_SHORT).show();
                            // Navigate to MainActivity
                            navigateToMainActivity();

                        } catch (Exception e) {
                            e.printStackTrace();
                            Log.e(TAG, "Error parsing response: " + e.getMessage());
                            Toast.makeText(AuthActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        }
                    } else {
                        // Parse the error response for more details
                        try {
                            org.json.JSONObject errorJson = new org.json.JSONObject(responseBody);
                            String errorMessage = errorJson.optString("error_description",
                                    errorJson.optString("message",
                                            errorJson.optString("error", responseBody)));
                            Log.e(TAG, "Supabase error details: " + errorMessage);
                            Toast.makeText(AuthActivity.this, "Supabase error: " + errorMessage, Toast.LENGTH_LONG).show();
                        } catch (Exception e) {
                            Log.e(TAG, "Error parsing response: " + responseBody);
                            Toast.makeText(AuthActivity.this, "Error: " + response.code() + " - " + responseBody, Toast.LENGTH_LONG).show();
                        }
                    }
                });
            }
        });
    }

    // ADD THIS NEW METHOD to AuthActivity.java:
    private void createUserProfileIfNeeded(JSONObject user) {
        new Thread(() -> {
            try {
                String displayName = user.optString("user_metadata", "User");
                if (displayName.equals("User")) {
                    // Try to get name from email
                    String email = user.optString("email", "");
                    if (!email.isEmpty()) {
                        displayName = email.split("@")[0];
                    }
                }

                boolean profileCreated = SupabaseJavaService.createUserProfile(displayName);
                if (profileCreated) {
                    Log.d(TAG, "✅ User profile created successfully");
                } else {
                    Log.d(TAG, "Profile creation skipped (may already exist)");
                }
            } catch (Exception e) {
                Log.e(TAG, "Error creating user profile: " + e.getMessage());
            }
        }).start();
    }

    private void saveAuthTokens(String accessToken, String refreshToken) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("access_token", accessToken);
        editor.putString("refresh_token", refreshToken);
        editor.apply();
        Log.d(TAG, "Auth tokens saved successfully");
    }
}