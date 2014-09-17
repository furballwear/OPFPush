/*
 * Copyright 2012-2014 One Platform Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.onepf.openpush.gcm;

import android.Manifest;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.provider.Settings;
import android.text.TextUtils;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.gcm.GoogleCloudMessaging;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.onepf.openpush.BasePushProvider;
import org.onepf.openpush.OpenPushException;
import org.onepf.openpush.util.PackageUtils;

import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class GCMProvider extends BasePushProvider {

    public static final String NAME = "com.google.android.gms.gcm.provider";

    private static final String PREF_REGISTRATION_TOKEN = "registration_token";
    private static final String PREF_APP_VERSION = "app_version";
    static final String PREF_ANDROID_ID = "android_id";

    private static final String GOOGLE_ACCOUNT_TYPE = "com.google";
    private static final String ANDROID_RELEASE_4_0_4 = "4.0.4";
    static final String PREFERENCES_NAME = "org.onepf.openpush.gcm";
    private static final String PERMISSION_RECEIVE = "com.google.android.c2dm.permission.RECEIVE";

    private String mRegistrationToken;
    private final String[] mSenderIDs;
    private final SharedPreferences mPreferences;
    private final GoogleCloudMessaging mGoogleCloudMessaging;

    private final ExecutorService mExecutor = Executors.newSingleThreadExecutor();

    public GCMProvider(@NotNull Context context, @NotNull String... senderIDs) {
        super(context, NAME, "com.android.vending");
        mSenderIDs = senderIDs;
        mGoogleCloudMessaging = GoogleCloudMessaging.getInstance(context);
        mPreferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE);
        mRegistrationToken = mPreferences.getString(PREF_REGISTRATION_TOKEN, null);
    }

    public void register() {
        if (isRegistered()) {
            throw new OpenPushException("Google Cloud Messaging already registered.");
        }
        mExecutor.execute(new RegisterTask());
    }

    public void unregister() {
        if (isRegistered()) {
            mExecutor.execute(new UnregisterTask(mRegistrationToken));
        } else {
            throw new OpenPushException("Google Cloud Messaging must" +
                    " be registered before unregister.");
        }
    }

    @Override
    public boolean checkManifest() {
        Context ctx = getContext();
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN
                && !Build.VERSION.RELEASE.equals(ANDROID_RELEASE_4_0_4)
                && !PackageUtils.checkPermission(ctx, Manifest.permission.GET_ACCOUNTS)) {
            return false;
        }

        return super.checkManifest()
                && PackageUtils.checkPermission(ctx, android.Manifest.permission.WAKE_LOCK)
                && PackageUtils.checkPermission(ctx, Manifest.permission.RECEIVE_BOOT_COMPLETED)
                && PackageUtils.checkPermission(ctx, PERMISSION_RECEIVE)
                && PackageUtils.checkPermission(ctx, ctx.getPackageName() + ".permission.C2D_MESSAGE");
    }

    @Override
    @Nullable
    public String getRegistrationId() {
        return mRegistrationToken;
    }

    @Override
    public boolean isAvailable() {
        //Need verify that GCM classes present, because depende
        try {
            Class.forName("com.google.android.gms.gcm.GoogleCloudMessaging");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            return false;
        }

        if (super.isAvailable()) {
            if (GooglePlayServicesUtil.isGooglePlayServicesAvailable(getContext())
                    == ConnectionResult.SUCCESS) {

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN
                        || Build.VERSION.RELEASE.equals(ANDROID_RELEASE_4_0_4)) {
                    return true;
                } else {
                    // On device with version of Android less than "4.0.4"
                    // we need to ensure that the user has at least one google account.
                    Account[] googleAccounts = AccountManager.get(getContext())
                            .getAccountsByType(GOOGLE_ACCOUNT_TYPE);
                    return googleAccounts.length != 0;
                }
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    @Override
    public boolean isRegistered() {
        if (TextUtils.isEmpty(mRegistrationToken)) {
            return false;
        } else {
            if (mPreferences.contains(PREF_APP_VERSION)) {
                int registeredVersion = mPreferences.getInt(PREF_APP_VERSION, Integer.MIN_VALUE);
                return registeredVersion == PackageUtils.getAppVersion(getContext());
            } else {
                return false;
            }
        }
    }

    @Override
    public void close() {
        mGoogleCloudMessaging.close();
        mExecutor.shutdown();
    }

    @NotNull
    @Override
    public String toString() {
        return String.format("%s (senderId: '%s', appVersion: %d)", NAME, Arrays.toString(mSenderIDs),
                mPreferences.getInt(PREF_APP_VERSION, -1));
    }

    @Override
    public void onAppStateChanged() {
        reset();
    }

    @Override
    public void onUnavailable() {
        reset();
    }

    private void reset() {
        mRegistrationToken = null;
        mPreferences.edit().clear().apply();
    }

    private class UnregisterTask implements Runnable {
        private String mOldRegistrationToken;

        private UnregisterTask(String oldRegistrationToken) {
            mOldRegistrationToken = oldRegistrationToken;
        }

        @Override
        public void run() {
            try {
                mGoogleCloudMessaging.unregister();

                reset();

                Intent intent = new Intent(GCMConstants.ACTION_UNREGISTRATION);
                intent.putExtra(GCMConstants.EXTRA_TOKEN, mOldRegistrationToken);
                getContext().sendBroadcast(intent);
            } catch (IOException e) {
                //TODO Send error.
            }
        }
    }

    private class RegisterTask implements Runnable {

        @Override
        public void run() {
            reset();
            try {
                mRegistrationToken = mGoogleCloudMessaging.register(mSenderIDs);
                if (mRegistrationToken != null) {
                    mPreferences.edit()
                            .putString(PREF_ANDROID_ID, Settings.Secure.ANDROID_ID)
                            .putString(PREF_REGISTRATION_TOKEN, mRegistrationToken)
                            .putInt(PREF_APP_VERSION, PackageUtils.getAppVersion(getContext()))
                            .apply();

                    Intent intent = new Intent(GCMConstants.ACTION_REGISTRATION);
                    intent.putExtra(GCMConstants.EXTRA_TOKEN, mRegistrationToken);
                    getContext().sendBroadcast(intent);
                } else {
                    Intent intent = new Intent(GCMConstants.ACTION_ERROR);
                    intent.putExtra(GCMConstants.EXTRA_ERROR_ID,
                            GCMConstants.ERROR_AUTHEFICATION_FAILED);
                    getContext().sendBroadcast(intent);
                }
            } catch (IOException e) {
                Intent intent = new Intent(GCMConstants.ACTION_ERROR);
                if (GoogleCloudMessaging.ERROR_SERVICE_NOT_AVAILABLE.equals(e.getMessage())) {
                    intent.putExtra(GCMConstants.EXTRA_ERROR_ID,
                            GCMConstants.ERROR_SERVICE_NOT_AVAILABLE);
                }
                getContext().sendBroadcast(intent);
            }
        }
    }
}
