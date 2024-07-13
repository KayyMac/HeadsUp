package com.achep.acdisplay.notifications;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.service.notification.StatusBarNotification;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;

import com.achep.acdisplay.App;
import com.achep.acdisplay.Config;
import com.achep.acdisplay.blacklist.AppConfig;
import com.achep.acdisplay.blacklist.Blacklist;
import com.achep.base.Device;
import com.achep.base.content.ConfigBase;
import com.achep.base.interfaces.ISubscriptable;
import com.achep.base.tests.Check;
import com.achep.base.utils.Operator;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import static com.achep.base.Build.DEBUG;

public class NotificationPresenter implements
        NotificationList.OnNotificationListChangedListener,
        ISubscriptable<NotificationPresenter.OnNotificationListChangedListener> {

    private static final String TAG = "NotificationPresenter";

    private static NotificationPresenter sNotificationPresenter;

    private final ListenerManager listenerManager;
    private final NotificationHandler notificationHandler;
    private final ConfigManager configManager;

    private final Config mConfig;
    private final Blacklist mBlacklist;

    private NotificationPresenter() {
        listenerManager = new ListenerManager();
        mConfig = Config.getInstance();
        mBlacklist = Blacklist.getInstance();

        NotificationList mGList = new NotificationList(null);
        NotificationList mLList = new NotificationList(this);
        Formatter mFormatter = new Formatter();

        notificationHandler = new NotificationHandler(mGList, mLList, mFormatter, mConfig, listenerManager);
        configManager = new ConfigManager(mConfig, mBlacklist, notificationHandler);

        if (!Device.hasJellyBeanMR2Api()) {
            mGList.setMaximumSize(5);
            mLList.setMaximumSize(5);
        }
    }

    @NonNull
    public synchronized static NotificationPresenter getInstance() {
        if (sNotificationPresenter == null) {
            sNotificationPresenter = new NotificationPresenter();
        }
        return sNotificationPresenter;
    }

    @Override
    public void registerListener(@NonNull OnNotificationListChangedListener listener) {
        listenerManager.registerListener(listener);
    }

    @Override
    public void unregisterListener(@NonNull OnNotificationListChangedListener listener) {
        listenerManager.unregisterListener(listener);
    }

    @NonNull
    public Formatter getFormatter() {
        return notificationHandler.getFormatter();
    }

    @NonNull
    public ArrayList<OpenNotification> getList() {
        return notificationHandler.getList();
    }

    public int size() {
        return notificationHandler.size();
    }

    public boolean isEmpty() {
        return notificationHandler.isEmpty();
    }

    @Override
    public int onNotificationAdded(@NonNull OpenNotification n) {
        return notificationHandler.onNotificationAdded(n);
    }

    @Override
    public int onNotificationChanged(@NonNull OpenNotification n, @NonNull OpenNotification old) {
        return notificationHandler.onNotificationChanged(n, old);
    }

    @Override
    public int onNotificationRemoved(@NonNull OpenNotification n) {
        return notificationHandler.onNotificationRemoved(n);
    }
}
