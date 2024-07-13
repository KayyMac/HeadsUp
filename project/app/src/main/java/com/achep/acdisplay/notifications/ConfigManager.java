package com.achep.acdisplay.notifications;

import android.support.annotation.NonNull;

import com.achep.acdisplay.Config;
import com.achep.acdisplay.blacklist.AppConfig;
import com.achep.acdisplay.blacklist.Blacklist;
import com.achep.base.content.ConfigBase;

public class ConfigManager {

    private final Config mConfig;
    private final Blacklist mBlacklist;
    private final ConfigListener mConfigListener;
    private final BlacklistListener mBlacklistListener;
    private final NotificationHandler notificationHandler;

    public ConfigManager(Config config, Blacklist blacklist, NotificationHandler notificationHandler) {
        this.mConfig = config;
        this.mBlacklist = blacklist;
        this.notificationHandler = notificationHandler;
        this.mConfigListener = new ConfigListener(config);
        this.mConfig.registerListener(mConfigListener);

        this.mBlacklistListener = new BlacklistListener();
        this.mBlacklist.registerListener(mBlacklistListener);
    }

    private class ConfigListener implements ConfigBase.OnConfigChangedListener {

        private int mMinPriority;
        private int mMaxPriority;

        public ConfigListener(@NonNull Config config) {
            mMinPriority = config.getNotifyMinPriority();
            mMaxPriority = config.getNotifyMaxPriority();
        }

        @Override
        public void onConfigChanged(@NonNull ConfigBase configBase, @NonNull String key, @NonNull Object value) {
            switch (key) {
                case Config.KEY_NOTIFY_MIN_PRIORITY:
                    handleNotifyPriorityChanged((int) value, mMinPriority);
                    mMinPriority = (int) value;
                    break;
                case Config.KEY_NOTIFY_MAX_PRIORITY:
                    handleNotifyPriorityChanged((int) value, mMaxPriority);
                    mMaxPriority = (int) value;
                    break;
                case Config.KEY_UI_EMOTICONS:
                    boolean b = (boolean) value;
                    for (OpenNotification n : mConfig.getGList().list()) {
                        n.setEmoticonsEnabled(b);
                    }
                    break;
                case Config.KEY_PRIVACY:
                    mFormatter.setPrivacyMode((int) value);
                    break;
            }
        }

        private void handleNotifyPriorityChanged(int a, int b) {
            if (a > b) {
                a -= b *= -1;
                a -= b += a;
            }

            final int lower = a, higher = b;
            notificationHandler.rebuildLocalList(new NotificationHandler.Comparator() {
                @Override
                public boolean needsRebuild(@NonNull OpenNotification osbn) {
                    int priority = osbn.getNotification().priority;
                    return priority >= lower && priority <= higher;
                }
            });
        }
    }

    private class BlacklistListener extends Blacklist.OnBlacklistChangedListener {

        @Override
        public void onBlacklistChanged(@NonNull AppConfig configNew, @NonNull AppConfig configOld, int diff) {
            boolean hiddenNew = configNew.isHidden();
            boolean hiddenOld = configOld.isHidden();
            boolean nonClearableEnabledNew = configNew.isNonClearableEnabled();
            boolean nonClearableEnabledOld = configOld.isNonClearableEnabled();

            if (hiddenNew != hiddenOld || nonClearableEnabledNew != nonClearableEnabledOld) {
                handlePackageVisibilityChanged(configNew.packageName);
            }
        }

        private void handlePackageVisibilityChanged(@NonNull final String packageName) {
            notificationHandler.rebuildLocalList(new NotificationHandler.Comparator() {
                @Override
                public boolean needsRebuild(@NonNull OpenNotification osbn) {
                    return osbn.getPackageName().equals(packageName);
                }
            });
        }
    }
}
