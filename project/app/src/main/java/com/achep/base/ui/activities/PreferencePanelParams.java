package com.achep.base.ui.activities;

import android.os.Bundle;
import androidx.fragment.app.Fragment;

public class PreferencePanelParams {
    private String fragmentClass;
    private Bundle args;
    private int titleRes;
    private CharSequence titleText;
    private Fragment resultTo;
    private int resultRequestCode;

    public PreferencePanelParams(String fragmentClass, Bundle args, int titleRes,
                                 CharSequence titleText, Fragment resultTo, int resultRequestCode) {
        this.fragmentClass = fragmentClass;
        this.args = args;
        this.titleRes = titleRes;
        this.titleText = titleText;
        this.resultTo = resultTo;
        this.resultRequestCode = resultRequestCode;
    }

    // Getters
    public String getFragmentClass() { return fragmentClass; }
    public Bundle getArgs() { return args; }
    public int getTitleRes() { return titleRes; }
    public CharSequence getTitleText() { return titleText; }
    public Fragment getResultTo() { return resultTo; }
    public int getResultRequestCode() { return resultRequestCode; }
}
