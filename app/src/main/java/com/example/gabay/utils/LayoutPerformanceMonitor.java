package com.example.gabay.utils;

import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import androidx.constraintlayout.widget.ConstraintLayout;

/**
 * Utility class for monitoring and analyzing layout performance
 * Helps identify potential performance bottlenecks in layouts
 */
public class LayoutPerformanceMonitor {
    
    private static final String TAG = "LayoutPerformance";
    private static final int MAX_NESTING_DEPTH = 5;
    private static final int MAX_VIEWS_PER_LAYOUT = 50;
    
    /**
     * Analyze a layout for potential performance issues
     */
    public static void analyzeLayout(View rootView) {
        if (rootView == null) return;
        
        int totalViews = countViews(rootView);
        int nestingDepth = getNestingDepth(rootView);
        boolean hasRelativeLayout = hasRelativeLayout(rootView);
        boolean hasLinearLayout = hasLinearLayout(rootView);
        
        Log.d(TAG, "Layout Analysis for: " + rootView.getClass().getSimpleName());
        Log.d(TAG, "Total Views: " + totalViews);
        Log.d(TAG, "Nesting Depth: " + nestingDepth);
        Log.d(TAG, "Has RelativeLayout: " + hasRelativeLayout);
        Log.d(TAG, "Has LinearLayout: " + hasLinearLayout);
        
        // Performance recommendations
        if (totalViews > MAX_VIEWS_PER_LAYOUT) {
            Log.w(TAG, "⚠️ Too many views in layout. Consider using RecyclerView or ViewStub.");
        }
        
        if (nestingDepth > MAX_NESTING_DEPTH) {
            Log.w(TAG, "⚠️ Deep nesting detected. Consider flattening the layout hierarchy.");
        }
        
        if (hasRelativeLayout) {
            Log.w(TAG, "⚠️ RelativeLayout detected. Consider using ConstraintLayout for better performance.");
        }
        
        if (hasLinearLayout && nestingDepth > 3) {
            Log.w(TAG, "⚠️ Deep LinearLayout nesting. Consider using ConstraintLayout.");
        }
        
        // Performance score (0-100, higher is better)
        int performanceScore = calculatePerformanceScore(totalViews, nestingDepth, hasRelativeLayout, hasLinearLayout);
        Log.i(TAG, "Layout Performance Score: " + performanceScore + "/100");
    }
    
    /**
     * Count total views in a layout hierarchy
     */
    private static int countViews(View view) {
        if (view == null) return 0;
        
        int count = 1;
        if (view instanceof ViewGroup) {
            ViewGroup viewGroup = (ViewGroup) view;
            for (int i = 0; i < viewGroup.getChildCount(); i++) {
                count += countViews(viewGroup.getChildAt(i));
            }
        }
        return count;
    }
    
    /**
     * Get the maximum nesting depth of a layout
     */
    private static int getNestingDepth(View view) {
        return getNestingDepth(view, 0);
    }
    
    private static int getNestingDepth(View view, int currentDepth) {
        if (view == null) return currentDepth;
        
        int maxDepth = currentDepth;
        if (view instanceof ViewGroup) {
            ViewGroup viewGroup = (ViewGroup) view;
            for (int i = 0; i < viewGroup.getChildCount(); i++) {
                maxDepth = Math.max(maxDepth, getNestingDepth(viewGroup.getChildAt(i), currentDepth + 1));
            }
        }
        return maxDepth;
    }
    
    /**
     * Check if layout contains RelativeLayout
     */
    private static boolean hasRelativeLayout(View view) {
        if (view == null) return false;
        
        if (view instanceof RelativeLayout) return true;
        
        if (view instanceof ViewGroup) {
            ViewGroup viewGroup = (ViewGroup) view;
            for (int i = 0; i < viewGroup.getChildCount(); i++) {
                if (hasRelativeLayout(viewGroup.getChildAt(i))) {
                    return true;
                }
            }
        }
        return false;
    }
    
    /**
     * Check if layout contains LinearLayout
     */
    private static boolean hasLinearLayout(View view) {
        if (view == null) return false;
        
        if (view instanceof LinearLayout) return true;
        
        if (view instanceof ViewGroup) {
            ViewGroup viewGroup = (ViewGroup) view;
            for (int i = 0; i < viewGroup.getChildCount(); i++) {
                if (hasLinearLayout(viewGroup.getChildAt(i))) {
                    return true;
                }
            }
        }
        return false;
    }
    
    /**
     * Calculate performance score based on various factors
     */
    private static int calculatePerformanceScore(int totalViews, int nestingDepth, boolean hasRelativeLayout, boolean hasLinearLayout) {
        int score = 100;
        
        // Deduct points for too many views
        if (totalViews > MAX_VIEWS_PER_LAYOUT) {
            score -= Math.min(30, (totalViews - MAX_VIEWS_PER_LAYOUT) / 2);
        }
        
        // Deduct points for deep nesting
        if (nestingDepth > MAX_NESTING_DEPTH) {
            score -= Math.min(25, (nestingDepth - MAX_NESTING_DEPTH) * 5);
        }
        
        // Deduct points for RelativeLayout usage
        if (hasRelativeLayout) {
            score -= 15;
        }
        
        // Deduct points for LinearLayout with deep nesting
        if (hasLinearLayout && nestingDepth > 3) {
            score -= 10;
        }
        
        return Math.max(0, score);
    }
    
    /**
     * Get optimization recommendations for a layout
     */
    public static String[] getOptimizationRecommendations(View rootView) {
        if (rootView == null) return new String[0];
        
        int totalViews = countViews(rootView);
        int nestingDepth = getNestingDepth(rootView);
        boolean hasRelativeLayout = hasRelativeLayout(rootView);
        boolean hasLinearLayout = hasLinearLayout(rootView);
        
        java.util.List<String> recommendations = new java.util.ArrayList<>();
        
        if (totalViews > MAX_VIEWS_PER_LAYOUT) {
            recommendations.add("Use RecyclerView for lists with many items");
            recommendations.add("Consider using ViewStub for rarely used views");
        }
        
        if (nestingDepth > MAX_NESTING_DEPTH) {
            recommendations.add("Flatten layout hierarchy using ConstraintLayout");
            recommendations.add("Reduce nested ViewGroups");
        }
        
        if (hasRelativeLayout) {
            recommendations.add("Replace RelativeLayout with ConstraintLayout");
            recommendations.add("ConstraintLayout provides better performance");
        }
        
        if (hasLinearLayout && nestingDepth > 3) {
            recommendations.add("Consider using ConstraintLayout instead of nested LinearLayouts");
        }
        
        return recommendations.toArray(new String[0]);
    }
}

