package com.achep.acdisplay.notifications;

import android.support.annotation.NonNull;
import android.util.Log;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

public class ListenerManager {

    private static final String TAG = "ListenerManager";
    private final ArrayList<WeakReference<NotificationPresenter.OnNotificationListChangedListener>> mListenersRefs;

    public ListenerManager() {
        mListenersRefs = new ArrayList<>();
    }

    public void registerListener(@NonNull NotificationPresenter.OnNotificationListChangedListener listener) {
        for (WeakReference<NotificationPresenter.OnNotificationListChangedListener> ref : mListenersRefs) {
            if (ref.get() == listener) {
                Log.w(TAG, "Tried to register already registered listener!");
                return;
            }
        }
        mListenersRefs.add(new WeakReference<>(listener));
    }

    public void unregisterListener(@NonNull NotificationPresenter.OnNotificationListChangedListener listener) {
        for (WeakReference<NotificationPresenter.OnNotificationListChangedListener> ref : mListenersRefs) {
            if (ref.get() == listener) {
                mListenersRefs.remove(ref);
                return;
            }
        }
        Log.w(TAG, "Tried to unregister non-existent listener!");
    }

    public ArrayList<WeakReference<NotificationPresenter.OnNotificationListChangedListener>> getListeners() {
        return mListenersRefs;
    }
}
