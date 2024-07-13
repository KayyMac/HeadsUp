package com.achep.acdisplay.notifications;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.util.Log;

import com.achep.base.utils.Operator;

import java.util.ArrayList;

import static com.achep.base.Build.DEBUG;

public class NotificationHandler {

    private static final String TAG = "NotificationHandler";
    private static final int FLAG_DONT_NOTIFY_FOLLOWERS = 1;
    private static final int FLAG_DONT_WAKE_UP = 2;
    private static final int RESULT_SUCCESS = 1;
    private static final int RESULT_SPAM = -1;

    private final NotificationList mGList;
    private final NotificationList mLList;
    private final Handler mHandler;
    private final Formatter mFormatter;
    private final Config mConfig;
    private boolean mFrozen;
    private final ArrayList<NotificationListChange> mFrozenEvents;
    private final ListenerManager listenerManager;

    public NotificationHandler(NotificationList gList, NotificationList lList, Formatter formatter, Config config, ListenerManager listenerManager) {
        this.mGList = gList;
        this.mLList = lList;
        this.mFormatter = formatter;
        this.mConfig = config;
        this.mFrozenEvents = new ArrayList<>();
        this.mHandler = new Handler(Looper.getMainLooper());
        this.listenerManager = listenerManager;
    }

    public void postNotificationFromMain(@NonNull final Context context, @NonNull final OpenNotification n, final int flags) {
        if (DEBUG) Log.d(TAG, "Initially posting " + n + " from '" + Thread.currentThread().getName() + "' thread.");
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                postNotification(context, n, flags);
            }
        });
    }

    void postNotification(@NonNull Context context, @NonNull OpenNotification n, int flags) {
        Check.getInstance().isInMainThread();

        if (isInitNotification(context, n)) {
            NotificationUtils.dismissNotification(n);
            return;
        }

        boolean globalValid = isValidForGlobal(n);
        boolean localValid = false;
        boolean isGroupSummary = false;

        if (globalValid) {
            n.load(context);

            if (n.isGroupSummary()) {
                isGroupSummary = true;

                String groupKey = n.getGroupKey();
                assert groupKey != null;
                mGroupsWithSummaries.add(groupKey);
            }

            Config config = Config.getInstance();
            n.setEmoticonsEnabled(config.isEmoticonsEnabled());

            localValid = isValidForLocal(n);
        }

        boolean flagIgnoreFollowers = Operator.bitAnd(flags, FLAG_DONT_NOTIFY_FOLLOWERS);
        boolean flagWakeUp = !Operator.bitAnd(flags, FLAG_DONT_WAKE_UP);

        freezeListeners();
        if (localValid && isGroupSummary) rebuildLocalList();
        if (KEEP_GLOBAL_LIST) mGList.pushOrRemove(n, globalValid, flagIgnoreFollowers);
        int result = mLList.pushOrRemove(n, localValid, flagIgnoreFollowers);

        if (flagWakeUp && result == RESULT_SUCCESS) {
            // Try start gui
//            mPresenter.tryStartGuiCauseNotification(context, n);
//            TODO:
        }

        meltListeners();
    }

    public void removeNotificationFromMain(@NonNull final OpenNotification n) {
        if (DEBUG) Log.d(TAG, "Initially removing " + n + " from '" + Thread.currentThread().getName() + "' thread.");
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                removeNotification(n);
            }
        });
    }

    @Deprecated
    public void removeNotification(@NonNull OpenNotification n) {
        Check.getInstance().isInMainThread();

        if (KEEP_GLOBAL_LIST) mGList.remove(n);
        mLList.remove(n);

        if (n.isGroupSummary()) {
            String groupKey = n.getGroupKey();
            assert groupKey != null;
            mGroupsWithSummaries.remove(groupKey);
            rebuildLocalList();
        }
    }

    private void rebuildLocalList() {
        ArrayList<NotificationListChange> changes = new ArrayList<>();

        ArrayList<OpenNotification> list = mLList.list();
        for (int i = 0; i < list.size(); i++) {
            OpenNotification n = list.get(i);
            if (!isValidForLocal(n)) {
                list.remove(i--);
                changes.add(new NotificationListChange(EVENT_REMOVED, n));
            }
        }

        for (OpenNotification n : mGList.list()) {
            if (isValidForLocal(n) && mLList.indexOf(n) == -1) {
                list.add(n);
                changes.add(new NotificationListChange(EVENT_POSTED, n));
            }
        }

        int size = changes.size();
        if (size > 4) {
            notifyListeners(null, EVENT_BATH);
        } else if (size > 0) {
            notifyListeners(changes);
        }
    }

    private void freezeListeners() {
        mFrozen = true;
    }

    private void meltListeners() {
        mFrozen = false;
        notifyListeners(mFrozenEvents);
        mFrozenEvents.clear();
    }

    private void notifyListeners(@Nullable OpenNotification n, int event) {
        notifyListeners(n, event, true);
    }

    private void notifyListeners(@Nullable OpenNotification n, int event, boolean isLastEventInSequence) {
        Check.getInstance().isInMainThread();

        if (mFrozen) {
            if (mFrozenEvents.size() >= 1 && mFrozenEvents.get(0).event == EVENT_BATH) return;
            if (event == EVENT_BATH) mFrozenEvents.clear();
            mFrozenEvents.add(new NotificationListChange(event, n));
            return;
        }

        for (int i = listenerManager.getListeners().size() - 1; i >= 0; i--) {
            WeakReference<NotificationPresenter.OnNotificationListChangedListener> ref = listenerManager.getListeners().get(i);
            NotificationPresenter.OnNotificationListChangedListener l = ref.get();

            if (l == null) {
                Log.w(TAG, "Deleting an unused listener!");
                listenerManager.getListeners().remove(i);
            } else {
                l.onNotificationListChanged(this, n, event, isLastEventInSequence);
            }
        }
    }

    private void notifyListeners(@NonNull ArrayList<NotificationListChange> changes) {
        int size = changes.size();
        for (int i = 0; i < size; i++) {
            NotificationListChange change = changes.get(i);
            notifyListeners(change.notification, change.event, i + 1 == size);
        }
    }
}
