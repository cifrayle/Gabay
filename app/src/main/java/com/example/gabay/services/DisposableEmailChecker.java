package com.example.gabay.services;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.regex.Pattern;

/**
 * Service for detecting disposable/temporary email addresses.
 * Thread-safe implementation with caching and multiple detection strategies.
 */
public class DisposableEmailChecker {
    private static final String TAG = "DisposableEmailChecker";
    private static final String PREFS_NAME = "DisposableEmailPrefs";
    private static final String KEY_DOMAIN_COUNT = "domain_count";
    private static final String KEY_LAST_UPDATED = "last_updated";

    private static Set<String> disposableEmailDomains;
    private static Set<String> disposablePatterns;
    private static boolean isInitialized = false;
    private static Context applicationContext;

    // Thread-safety
    private static final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    // Common patterns for disposable email services
    private static final String[] SUSPICIOUS_PATTERNS = {
            "temp", "disposable", "throwaway", "fake", "trash", "spam",
            "guerrilla", "mailinator", "yopmail", "10minute", "minute",
            "tempmail", "trashmail", "fakeinbox", "burner", "temporary"
    };

    // Additional validation patterns
    private static final Pattern VALID_DOMAIN_PATTERN = Pattern.compile(
            "^[a-zA-Z0-9]([a-zA-Z0-9-]*[a-zA-Z0-9])?(\\.[a-zA-Z0-9]([a-zA-Z0-9-]*[a-zA-Z0-9])?)*$"
    );

    /**
     * Initialize the checker with application context.
     * This method is thread-safe and can be called multiple times.
     */
    public static void initialize(@NonNull Context context) {
        lock.writeLock().lock();
        try {
            if (isInitialized) {
                Log.d(TAG, "Already initialized, skipping");
                return;
            }

            applicationContext = context.getApplicationContext();
            disposableEmailDomains = new HashSet<>();
            disposablePatterns = new HashSet<>();

            boolean success = loadDomainsFromAssets();

            if (!success) {
                Log.w(TAG, "Asset loading failed, using fallback domains");
                loadFallbackDomains();
            }

            loadSuspiciousPatterns();
            cacheMetadata();

            isInitialized = true;
            Log.i(TAG, "Initialized with " + disposableEmailDomains.size() + " disposable domains");

        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Load domains from assets file with error handling.
     */
    private static boolean loadDomainsFromAssets() {
        if (applicationContext == null) {
            Log.e(TAG, "Context is null, cannot load assets");
            return false;
        }

        InputStream inputStream = null;
        BufferedReader reader = null;

        try {
            inputStream = applicationContext.getAssets().open("disposable_email_blocklist.txt");
            reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));

            String line;
            int lineNumber = 0;
            int validDomains = 0;

            while ((line = reader.readLine()) != null) {
                lineNumber++;
                line = line.trim().toLowerCase();

                // Skip empty lines and comments
                if (line.isEmpty() || line.startsWith("#") || line.startsWith("//")) {
                    continue;
                }

                // Validate domain format
                if (isValidDomain(line)) {
                    disposableEmailDomains.add(line);
                    validDomains++;
                } else {
                    Log.w(TAG, "Invalid domain format at line " + lineNumber + ": " + line);
                }
            }

            Log.d(TAG, "Loaded " + validDomains + " valid domains from " + lineNumber + " lines");
            return validDomains > 0;

        } catch (IOException e) {
            Log.e(TAG, "Error reading disposable email blocklist", e);
            return false;
        } finally {
            closeQuietly(reader);
            closeQuietly(inputStream);
        }
    }

    /**
     * Load fallback domains as a safety net.
     */
    private static void loadFallbackDomains() {
        String[] fallbackDomains = {
                // Popular temporary email services
                "tempmail.com", "temp-mail.org", "temp-mail.io", "tempmail.net",
                "guerrillamail.com", "guerrillamail.net", "guerrillamail.org", "guerrillamail.biz",
                "mailinator.com", "mailinator.net", "mailinator2.com",
                "10minutemail.com", "10minutemail.net", "10minutemail.org",
                "yopmail.com", "yopmail.net", "yopmail.fr",
                "throwawaymail.com", "throwaway.email",
                "fakeinbox.com", "fakeinbox.net",
                "trashmail.com", "trashmail.net", "trashmail.org",
                "disposableemail.com", "disposable.com",
                "getnada.com", "getairmail.com",
                "tempail.com", "tmpmail.org", "tmpmail.net",
                "sharklasers.com", "grr.la", "guerrillamail.de",
                "spam4.me", "maildrop.cc", "mohmal.com",
                "filipx.com", "emailondeck.com", "spamgourmet.com",
                "mailnesia.com", "mintemail.com", "mytrashmail.com",
                "dispostable.com", "emailondeck.com", "throwam.com",
                "hidemail.de", "spambog.com", "mt2014.com",
                "anonymbox.com", "binkmail.com", "bobmail.info",
                "correo.blogos.net", "dodgeit.com", "dontreg.com",
                "emailsensei.com", "emeil.in", "emeil.ir",
                "fastmail.fm", "filzmail.com", "garliclife.com",
                "getonemail.com", "harakirimail.com", "hatespam.org",
                "ihateyoualot.info", "jetable.com", "kasmail.com",
                "koszmail.pl", "lroid.com", "mailcatch.com",
                "mailexpire.com", "mailin8r.com", "mailmetrash.com",
                "mailmoat.com", "mailslite.com", "meltmail.com",
                "mintemail.com", "mypartyclip.de", "nepwk.com",
                "nobulk.com", "no-spam.ws", "nospam.ze.tc",
                "nospamfor.us", "nowmymail.com", "objectmail.com",
                "obobbo.com", "oneoffemail.com", "ordinaryamerican.net",
                "pookmail.com", "proxymail.eu", "prtnx.com",
                "putthisinyourspamdatabase.com", "quickinbox.com", "rcpt.at",
                "recode.me", "recursor.net", "regbypass.com",
                "rmqkr.net", "safetymail.info", "sandelf.de",
                "saynotospams.com", "selfdestructingmail.com", "sendspamhere.com",
                "shiftmail.com", "slaskpost.se", "slopsbox.com",
                "smellfear.com", "snakemail.com", "sneakemail.com",
                "sofort-mail.de", "solvemail.info", "spam.la",
                "spamavert.com", "spambob.com", "spambog.ru",
                "spambox.us", "spamcannon.com", "spamcero.com",
                "spamcon.org", "spamcorptastic.com", "spamcowboy.com",
                "spamday.com", "spamex.com", "spamfree24.com",
                "spamfree24.de", "spamfree24.org", "spamgourmet.com",
                "spamhole.com", "spamify.com", "spaminator.de",
                "spamkill.info", "spaml.com", "spaml.de",
                "spammotel.com", "spamobox.com", "spamoff.de",
                "spamslicer.com", "spamspot.com", "spamthis.co.uk",
                "spamtrail.com", "speed.1s.fr", "supergreatmail.com",
                "teewars.org", "teleworm.com", "teleworm.us",
                "tempalias.com", "tempe-mail.com", "tempemail.biz",
                "tempemail.com", "tempemail.net", "tempinbox.co.uk",
                "tempinbox.com", "tempmail.eu", "tempmaildemo.com",
                "tempmailer.com", "tempmailer.de", "tempomail.fr",
                "temporarily.de", "temporarioemail.com.br", "temporaryemail.net",
                "temporaryforwarding.com", "temporaryinbox.com", "thanksnospam.info",
                "thankyou2010.com", "thisisnotmyrealemail.com", "throwawayemailaddress.com",
                "tilien.com", "tmailinator.com", "tradermail.info",
                "trash2009.com", "trash-amil.com", "trash-mail.at",
                "trash-mail.com", "trash-mail.de", "trashdevil.com",
                "trashemail.de", "trashymail.com", "trashymail.net",
                "trillianpro.com", "twinmail.de", "uggsrock.com",
                "wegwerfadresse.de", "wegwerfemail.de", "wegwerfmail.de",
                "wegwerfmail.net", "wegwerfmail.org", "wh4f.org",
                "whyspam.me", "willselfdestruct.com", "winemaven.info",
                "wronghead.com", "wuzup.net", "wuzupmail.net",
                "www.e4ward.com", "www.gishpuppy.com", "www.mailinator.com",
                "xagloo.com", "xemaps.com", "xents.com",
                "xmaily.com", "xoxy.net", "yopmail.fr",
                "you-spam.com", "yugasandrika.com", "yuurok.com",
                "zehnminuten.de", "zehnminutenmail.de", "zippymail.info",
                "zoaxe.com", "zoemail.org", "zomg.info"
        };

        for (String domain : fallbackDomains) {
            disposableEmailDomains.add(domain.toLowerCase());
        }

        Log.i(TAG, "Loaded " + fallbackDomains.length + " fallback domains");
    }

    /**
     * Load suspicious patterns for additional heuristic checking.
     */
    private static void loadSuspiciousPatterns() {
        for (String pattern : SUSPICIOUS_PATTERNS) {
            disposablePatterns.add(pattern.toLowerCase());
        }
    }

    /**
     * Cache metadata for diagnostics.
     */
    private static void cacheMetadata() {
        if (applicationContext == null) return;

        try {
            SharedPreferences prefs = applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();
            editor.putInt(KEY_DOMAIN_COUNT, disposableEmailDomains.size());
            editor.putLong(KEY_LAST_UPDATED, System.currentTimeMillis());
            editor.apply();
        } catch (Exception e) {
            Log.w(TAG, "Failed to cache metadata", e);
        }
    }

    /**
     * Check if an email address uses a disposable domain.
     * Thread-safe and handles null/invalid input gracefully.
     */
    public static boolean isDisposable(@Nullable String email) {
        lock.readLock().lock();
        try {
            if (!isInitialized) {
                Log.w(TAG, "Checker not initialized, returning false");
                return false;
            }

            if (email == null || email.trim().isEmpty()) {
                return false;
            }

            email = email.trim().toLowerCase();

            // Basic email format validation
            if (!email.contains("@")) {
                return false;
            }

            String domain = extractDomain(email);
            if (domain == null || domain.isEmpty()) {
                return false;
            }

            // Check exact match
            if (disposableEmailDomains.contains(domain)) {
                Log.d(TAG, "Disposable email detected (exact match): " + domain);
                return true;
            }

            // Check subdomain matches (e.g., user@test.tempmail.com)
            if (checkSubdomainMatch(domain)) {
                Log.d(TAG, "Disposable email detected (subdomain match): " + domain);
                return true;
            }

            // Heuristic check for suspicious patterns
            if (checkSuspiciousPatterns(domain)) {
                Log.d(TAG, "Potentially disposable email detected (pattern match): " + domain);
                return true;
            }

            return false;

        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Extract domain from email address with validation.
     */
    @Nullable
    private static String extractDomain(@NonNull String email) {
        try {
            int atIndex = email.lastIndexOf('@');
            if (atIndex < 1 || atIndex >= email.length() - 1) {
                return null;
            }

            String domain = email.substring(atIndex + 1).trim();

            // Remove any trailing dots or invalid characters
            domain = domain.replaceAll("[^a-zA-Z0-9.-]", "");

            return domain.isEmpty() ? null : domain;
        } catch (Exception e) {
            Log.w(TAG, "Error extracting domain from: " + email, e);
            return null;
        }
    }

    /**
     * Check if domain or its parent domain is disposable.
     * Example: mail.tempmail.com matches tempmail.com
     */
    private static boolean checkSubdomainMatch(@NonNull String domain) {
        String[] parts = domain.split("\\.");

        // Check all possible parent domains
        for (int i = 0; i < parts.length - 1; i++) {
            StringBuilder parentDomain = new StringBuilder();
            for (int j = i; j < parts.length; j++) {
                if (j > i) parentDomain.append(".");
                parentDomain.append(parts[j]);
            }

            if (disposableEmailDomains.contains(parentDomain.toString())) {
                return true;
            }
        }

        return false;
    }

    /**
     * Check for suspicious patterns that might indicate disposable email.
     */
    private static boolean checkSuspiciousPatterns(@NonNull String domain) {
        String lowerDomain = domain.toLowerCase();

        for (String pattern : disposablePatterns) {
            if (lowerDomain.contains(pattern)) {
                // Additional validation to reduce false positives
                if (domain.length() < 30 && lowerDomain.matches(".*" + pattern + ".*mail.*|.*mail.*" + pattern + ".*")) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Validate domain format.
     */
    private static boolean isValidDomain(@NonNull String domain) {
        if (domain.length() > 255 || domain.length() < 3) {
            return false;
        }

        if (domain.startsWith(".") || domain.endsWith(".") || domain.contains("..")) {
            return false;
        }

        return VALID_DOMAIN_PATTERN.matcher(domain).matches();
    }

    /**
     * Get the count of loaded disposable domains.
     */
    public static int getDomainCount() {
        lock.readLock().lock();
        try {
            return disposableEmailDomains != null ? disposableEmailDomains.size() : 0;
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Check if the service is initialized.
     */
    public static boolean isInitialized() {
        lock.readLock().lock();
        try {
            return isInitialized;
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Force re-initialization (useful for testing or updates).
     */
    public static void reinitialize(@NonNull Context context) {
        lock.writeLock().lock();
        try {
            isInitialized = false;
            disposableEmailDomains = null;
            disposablePatterns = null;
            initialize(context);
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Get diagnostic information.
     */
    @NonNull
    public static String getDiagnostics() {
        lock.readLock().lock();
        try {
            StringBuilder sb = new StringBuilder();
            sb.append("Initialized: ").append(isInitialized).append("\n");
            sb.append("Domain Count: ").append(getDomainCount()).append("\n");
            sb.append("Pattern Count: ").append(disposablePatterns != null ? disposablePatterns.size() : 0).append("\n");

            if (applicationContext != null) {
                try {
                    SharedPreferences prefs = applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
                    long lastUpdated = prefs.getLong(KEY_LAST_UPDATED, 0);
                    if (lastUpdated > 0) {
                        sb.append("Last Updated: ").append(new java.util.Date(lastUpdated)).append("\n");
                    }
                } catch (Exception e) {
                    // Ignore
                }
            }

            return sb.toString();
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Safely close a Closeable resource.
     */
    private static void closeQuietly(@Nullable java.io.Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (IOException e) {
                // Ignore
            }
        }
    }

    /**
     * Clear all cached data (for testing or memory management).
     */
    public static void clear() {
        lock.writeLock().lock();
        try {
            if (disposableEmailDomains != null) {
                disposableEmailDomains.clear();
            }
            if (disposablePatterns != null) {
                disposablePatterns.clear();
            }
            isInitialized = false;
            applicationContext = null;
            Log.d(TAG, "Cleared all cached data");
        } finally {
            lock.writeLock().unlock();
        }
    }
}