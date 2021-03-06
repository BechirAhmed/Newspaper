package com.github.ayltai.newspaper.setting;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import android.annotation.SuppressLint;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.support.annotation.NonNull;
import android.support.v7.preference.PreferenceManager;
import android.text.TextUtils;

import com.github.ayltai.newspaper.Configs;
import com.github.ayltai.newspaper.Constants;
import com.github.ayltai.newspaper.R;
import com.github.ayltai.newspaper.util.SuppressFBWarnings;

@SuppressFBWarnings("PMB_POSSIBLE_MEMORY_BLOAT")
public final class Settings {
    //region Constants

    static final String PREF_COMPACT_LAYOUT       = "PREF_COMPACT_LAYOUT";
    static final String PREF_DARK_THEME           = "PREF_DARK_THEME";
    static final String PREF_HEADER_IMAGE_ENABLED = "PREF_HEADER_IMAGE_ENABLED";
    static final String PREF_PANORAMA_ENABLED     = "PREF_PANORAMA_ENABLED";
    static final String PREF_SOURCES              = "PREF_SOURCES";
    static final String PREF_CATEGORIES           = "PREF_CATEGORIES";

    private static final String PREF_USER_ID   = "PREF_USER_ID";
    private static final String PREF_AUTO_PLAY = "PREF_AUTO_PLAY";

    //endregion

    private static final Map<String, Integer> POSITIONS = new HashMap<>();

    private Settings() {
    }

    @SuppressLint("CommitPrefEdits")
    @NonNull
    public static String getUserId(@NonNull final Context context) {
        String userId = PreferenceManager.getDefaultSharedPreferences(context).getString(Settings.PREF_USER_ID, null);

        if (TextUtils.isEmpty(userId)) {
            userId = UUID.randomUUID().toString();

            PreferenceManager.getDefaultSharedPreferences(context).edit().putString(Settings.PREF_USER_ID, userId).commit();
        }

        return userId;
    }

    @Constants.ListViewType
    public static int getListViewType(@NonNull final Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(Settings.PREF_COMPACT_LAYOUT, true) ? Constants.LIST_VIEW_TYPE_COMPACT : Constants.LIST_VIEW_TYPE_COZY;
    }

    public static boolean isDarkTheme(@NonNull final Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(Settings.PREF_DARK_THEME, false);
    }

    @SuppressWarnings("checkstyle:magicnumber")
    @NonNull
    public static Set<String> getSources(@NonNull final Context context) {
        final LinkedHashSet<String> reducedSources = new LinkedHashSet<>(Arrays.asList(context.getResources().getStringArray(R.array.pref_source_items)));
        final List<String>          allSources     = Arrays.asList(context.getResources().getStringArray(R.array.sources));
        final Set<String>           userSources    = PreferenceManager.getDefaultSharedPreferences(context).getStringSet(Settings.PREF_SOURCES, reducedSources);
        final LinkedHashSet<String> sources        = new LinkedHashSet<>();

        for (final String source : reducedSources) {
            if (userSources.contains(source)) {
                sources.add(source);

                if (allSources.get(2).equals(source)) sources.add(allSources.get(3));
                if (allSources.get(6).equals(source)) sources.add(allSources.get(7));
            }
        }

        return sources;
    }

    @NonNull
    public static Set<String> getPreferenceSources(@NonNull final Context context) {
        final LinkedHashSet<String> reducedSources = new LinkedHashSet<>(Arrays.asList(context.getResources().getStringArray(R.array.pref_source_items)));
        final Set<String>           userSources    = PreferenceManager.getDefaultSharedPreferences(context).getStringSet(Settings.PREF_SOURCES, reducedSources);
        final LinkedHashSet<String> sources        = new LinkedHashSet<>();

        for (final String source : reducedSources) {
            if (userSources.contains(source)) sources.add(source);
        }

        return sources;
    }

    @NonNull
    public static Set<String> getPreferenceCategories(@NonNull final Context context) {
        final LinkedHashSet<String> reducedCategories = new LinkedHashSet<>(Arrays.asList(context.getResources().getStringArray(R.array.pref_category_items)));
        final Set<String>           userCategories    = PreferenceManager.getDefaultSharedPreferences(context).getStringSet(Settings.PREF_CATEGORIES, reducedCategories);
        final LinkedHashSet<String> categories        = new LinkedHashSet<>();

        for (final String category : reducedCategories) {
            if (userCategories.contains(category)) categories.add(category);
        }

        return categories;
    }

    public static boolean isAutoPlayEnabled(@NonNull final Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(Settings.PREF_AUTO_PLAY, false);
    }

    public static boolean isHeaderImageEnabled(@NonNull final Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(Settings.PREF_HEADER_IMAGE_ENABLED, Configs.isHeaderImageEnabled());
    }

    @SuppressLint("CommitPrefEdits")
    public static void setHeaderImageEnabled(@NonNull final Context context, final boolean enabled) {
        PreferenceManager.getDefaultSharedPreferences(context).edit().putBoolean(Settings.PREF_HEADER_IMAGE_ENABLED, enabled).commit();
    }

    public static boolean isPanoramaEnabled(@NonNull final Context context) {
        return Settings.canPanoramaBeEnabled(context) && PreferenceManager.getDefaultSharedPreferences(context).getBoolean(Settings.PREF_PANORAMA_ENABLED, Configs.isPanoramaEnabled());
    }

    @SuppressLint("CommitPrefEdits")
    public static void setPanoramaEnabled(@NonNull final Context context, final boolean enabled) {
        PreferenceManager.getDefaultSharedPreferences(context).edit().putBoolean(Settings.PREF_PANORAMA_ENABLED, enabled).commit();
    }

    public static boolean canPanoramaBeEnabled(@NonNull final Context context) {
        return ((SensorManager)context.getSystemService(Context.SENSOR_SERVICE)).getDefaultSensor(Sensor.TYPE_GYROSCOPE) != null;
    }

    public static int getPosition(@NonNull final String category) {
        if (Settings.POSITIONS.containsKey(category)) return Settings.POSITIONS.get(category);

        return 0;
    }

    public static void setPosition(@NonNull final String category, final int position) {
        Settings.POSITIONS.put(category, position);
    }

    public static void resetPosition() {
        Settings.POSITIONS.clear();
    }
}
