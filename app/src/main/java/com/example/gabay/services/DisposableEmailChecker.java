package com.example.gabay.services;

import android.content.Context;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;
public class DisposableEmailChecker {
    private static final String TAG = "DisposableEmailChecker";
    private static Set<String> disposableEmailDomains;
    private static boolean isInitialized = false;

    public static void initialize(Context context) {
        if (isInitialized) return;

        disposableEmailDomains = new HashSet<>();
        try {
            loadDomainsFromAssets(context);
            isInitialized = true;
            Log.d(TAG, "Disposable email domains loaded: " + disposableEmailDomains.size());
        } catch (IOException ex) {
            Log.e(TAG, "Failed to load list of disposable email domains.", ex);
            // Load fallback domains
            loadFallbackDomains();
            isInitialized = true;
        }
    }
    private static void loadDomainsFromAssets(Context context) throws IOException {
        InputStream inputStream = context.getAssets().open("disposable_email_blocklist.conf");
        BufferedReader reader = new BufferedReader(
                new InputStreamReader(inputStream, StandardCharsets.UTF_8));

        String line;
        while ((line = reader.readLine()) != null) {
            line = line.trim();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            disposableEmailDomains.add(line.toLowerCase());
        }

        reader.close();
        inputStream.close();
    }

    private static void loadFallbackDomains() {
        // Basic fallback in case the file can't be loaded
        String[] fallbackDomains = {
                "tempmail.com", "guerrillamail.com", "mailinator.com", "10minutemail.com",
                "yopmail.com", "throwawaymail.com", "fakeinbox.com", "trashmail.com",
                "disposableemail.com", "temp-mail.org", "getnada.com", "tempail.com",
                "tmpmail.org", "sharklasers.com", "guerrillamail.net", "guerrillamail.org",
                "spam4.me", "maildrop.cc", "getairmail.com", "mohmal.com", "filipx.com",
        };

        for (String domain : fallbackDomains) {
            disposableEmailDomains.add(domain);
        }
        Log.w(TAG, "Using fallback disposable domains list");
    }

    public static boolean isDisposable(String email) {
        if (!isInitialized) {
            Log.w(TAG, "Checker not initialized yet");
            return false;
        }

        if (email == null || !email.contains("@")) {
            return false;
        }

        String domain = email.substring(email.indexOf('@') + 1).toLowerCase().trim();
        return disposableEmailDomains.contains(domain);
    }

    public static boolean isInitialized() {
        return isInitialized;
    }
}

