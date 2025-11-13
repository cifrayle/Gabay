# Gabay Analytics Integration

This document describes the new analytics integration added to the Gabay app, which tracks user progress, app ratings, and chapter completion in Supabase.

## 🎯 Features Implemented

### 1. **App Rating System**
- Custom rating dialog with 1-5 star rating
- Optional comment field for user feedback
- Automatic Play Store redirection for high ratings (4-5 stars)
- Ratings stored in Supabase for analytics

### 2. **User Progress Tracking**
- Session duration tracking
- Total sessions count
- Chapter completion analytics
- Learning streaks (foundation for future features)

### 3. **Chapter Completion Analytics**
- Tracks when users complete entire chapters
- Records favorite chapter based on most recent completion
- Integrates with existing progress system

## 📊 Supabase Table Structure

Create this table in your Supabase dashboard:

```sql
-- User Analytics Table
CREATE TABLE user_analytics (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    user_id UUID REFERENCES auth.users(id) ON DELETE CASCADE,
    
    -- App Rating
    app_rating INTEGER CHECK (app_rating >= 1 AND app_rating <= 5),
    rating_comment TEXT,
    rated_at TIMESTAMP WITH TIME ZONE,
    
    -- Session Analytics
    total_sessions INTEGER DEFAULT 0,
    total_time_spent_minutes INTEGER DEFAULT 0,
    last_session_at TIMESTAMP WITH TIME ZONE,
    
    -- Chapter Completion Analytics
    chapters_completed INTEGER DEFAULT 0,
    favorite_chapter INTEGER,
    
    -- Learning Streaks
    current_streak_days INTEGER DEFAULT 0,
    longest_streak_days INTEGER DEFAULT 0,
    last_activity_date DATE,
    
    -- Timestamps
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    
    UNIQUE(user_id)
);

-- Enable RLS
ALTER TABLE user_analytics ENABLE ROW LEVEL SECURITY;

-- RLS Policies
CREATE POLICY "Users can view own analytics" ON user_analytics
    FOR SELECT USING (auth.uid() = user_id);

CREATE POLICY "Users can insert own analytics" ON user_analytics
    FOR INSERT WITH CHECK (auth.uid() = user_id);

CREATE POLICY "Users can update own analytics" ON user_analytics
    FOR UPDATE USING (auth.uid() = user_id);
```

## 🔧 Implementation Details

### New Files Created

1. **`UserAnalyticsService.java`** - Main service for analytics operations
2. **`RatingDialog.java`** - Custom rating dialog component
3. **`SessionTracker.java`** - Session duration tracking utility
4. **`AnalyticsTestHelper.java`** - Testing utilities
5. **`dialog_rating.xml`** - Rating dialog layout
6. **Drawable resources** - Dialog backgrounds and buttons

### Modified Files

1. **`SettingsPage.java`** - Integrated rating dialog
2. **`Chapter5.java`** - Added chapter completion tracking
3. **`MainActivity.java`** - Added session tracking
4. **`colors.xml`** - Added new color resources

## 🚀 Usage Examples

### Rating Dialog
```java
// Show rating dialog in SettingsPage
RatingDialog ratingDialog = new RatingDialog(requireContext());
ratingDialog.show(new RatingDialog.RatingCallback() {
    @Override
    public void onRatingSubmitted(int rating, String comment) {
        Log.d("Rating", "User rated: " + rating + " stars");
    }
    
    @Override
    public void onRatingCancelled() {
        Log.d("Rating", "User cancelled rating");
    }
});
```

### Session Tracking
```java
// Automatic session tracking in MainActivity
SessionTracker tracker = SessionTracker.getInstance(this);
tracker.startSession();  // Called in onCreate()
tracker.endSession();    // Called in onPause()/onDestroy()
```

### Chapter Completion
```java
// Track chapter completion in Chapter fragments
UserAnalyticsService.trackChapterCompletion(chapterNumber);
```

### Analytics Data Retrieval
```java
// Get user analytics data
new Thread(() -> {
    JSONObject analytics = UserAnalyticsService.getUserAnalytics();
    if (analytics != null) {
        int totalSessions = analytics.optInt("total_sessions", 0);
        int rating = analytics.optInt("app_rating", 0);
        int chaptersCompleted = analytics.optInt("chapters_completed", 0);
    }
}).start();
```

## 🧪 Testing

Use the `AnalyticsTestHelper` class to test the integration:

```java
// Test all analytics features
AnalyticsTestHelper.runAllTests(context);

// Test specific features
AnalyticsTestHelper.testAppRating(context);
AnalyticsTestHelper.testChapterCompletion(context, 1);
AnalyticsTestHelper.testSessionTracking(context);

// View current analytics data
AnalyticsTestHelper.printAnalyticsData(context);
```

## 🔒 Security & Privacy

- All analytics data is tied to authenticated users only
- Row Level Security (RLS) ensures users can only access their own data
- No sensitive personal information is stored
- Users can rate the app without being forced to
- Session tracking respects user privacy

## 📈 Analytics Data Points

The system tracks:

- **App Rating**: 1-5 stars with optional comments
- **Session Data**: Total sessions, time spent, last session
- **Chapter Progress**: Completed chapters, favorite chapter
- **User Engagement**: Session duration, activity patterns

## 🔄 Integration with Existing Code

The implementation is designed to **NOT affect existing functionality**:

- Uses separate `UserAnalyticsService` alongside existing `SupabaseJavaService`
- Rating dialog replaces direct Play Store redirect in SettingsPage
- Chapter completion tracking is additive to existing progress system
- Session tracking runs independently in MainActivity lifecycle

## 🛠 Maintenance

### Adding New Analytics
To track new user actions:

1. Add new fields to `user_analytics` table
2. Create methods in `UserAnalyticsService`
3. Call tracking methods where appropriate
4. Add tests in `AnalyticsTestHelper`

### Monitoring
Check Supabase dashboard for:
- User analytics table data
- API usage statistics
- Error logs in application logs

## 🎉 Benefits

1. **User Insights**: Understand how users interact with the app
2. **Feedback Collection**: Gather ratings and comments for improvements
3. **Engagement Metrics**: Track session duration and frequency
4. **Progress Analytics**: See which chapters are most/least completed
5. **Data-Driven Decisions**: Make informed product decisions

## 📝 Notes

- All analytics operations run in background threads to avoid UI blocking
- Failed analytics operations are logged but don't affect app functionality
- The system gracefully handles offline scenarios
- Analytics data is automatically created when users first interact with tracked features

---

**Implementation completed without affecting existing code functionality.**
