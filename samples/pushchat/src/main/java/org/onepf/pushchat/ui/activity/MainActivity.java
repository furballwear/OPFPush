/*
 * Copyright 2012-2015 One Platform Foundation
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

package org.onepf.pushchat.ui.activity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ProgressBar;
import com.google.android.gms.common.GooglePlayServicesUtil;
import org.onepf.pushchat.PushChatApplication;
import org.onepf.pushchat.R;
import org.onepf.pushchat.ui.fragment.NavigationDrawerFragment;
import org.onepf.pushchat.ui.fragment.content.BaseContentFragment;
import org.onepf.pushchat.ui.fragment.content.MessagesFragment;
import org.onepf.pushchat.ui.fragment.content.StateFragment;
import org.onepf.pushchat.utils.FragmentUtils;
import org.onepf.pushchat.utils.StateController;

import static android.content.Intent.ACTION_SEND;
import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static org.onepf.pushchat.model.PushState.REGISTERED;
import static org.onepf.pushchat.ui.activity.MainActivity.MainActivityReceiver.*;
import static org.onepf.pushchat.ui.fragment.NavigationDrawerFragment.MESSAGES_POSITION;


public class MainActivity extends ActionBarActivity {

    public static final String OPEN_MESSAGES_FRAGMENT_ACTION = "OPEN_MESSAGES_FRAGMENT_ACTION";

    private static final String TOOLBAR_TITLE_KEY = "TOOLBAR_TITLE_KEY";

    private static final String IS_SHARE_MENU_ITEM_VISIBLE_KEY = "IS_SHARE_MENU_ITEM_VISIBLE_KEY";

    private Toolbar toolbar;

    private DrawerLayout drawerLayout;

    private ActionBarDrawerToggle drawerToggle;

    private ProgressBar progressBar;

    private boolean isShareMenuItemVisible;

    private String title;

    private MainActivityReceiver receiver;

    @Override
    protected void onCreate(@Nullable final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        progressBar = (ProgressBar) findViewById(R.id.progress_bar);

        if (savedInstanceState == null) {
            isShareMenuItemVisible = false;

            final BaseContentFragment fragment;
            if (StateController.getState(this) == REGISTERED) {
                title = getString(R.string.title_messages_fragment);
                fragment = MessagesFragment.newInstance();
            } else {
                title = getString(R.string.title_state_fragment);
                fragment = StateFragment.newInstance();
            }

            FragmentUtils.add(
                    getSupportFragmentManager(),
                    fragment,
                    fragment.getClass().getName()
            );
        } else {
            title = savedInstanceState.getString(TOOLBAR_TITLE_KEY);
            isShareMenuItemVisible = savedInstanceState.getBoolean(IS_SHARE_MENU_ITEM_VISIBLE_KEY, false);
        }
        setUpNavigationDrawer();
    }

    @Override
    protected void onPostCreate(@Nullable final Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        drawerToggle.syncState();
        setToolbarTitle(title);
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver();
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (OPEN_MESSAGES_FRAGMENT_ACTION.equals(intent.getAction())) {
            ((NavigationDrawerFragment) getSupportFragmentManager().findFragmentById(R.id.navigation_drawer))
                    .selectItem(MESSAGES_POSITION);
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull final Bundle outState) {
        super.onSaveInstanceState(outState);
        if (toolbar != null) {
            outState.putString(TOOLBAR_TITLE_KEY, title);
        }
        outState.putBoolean(IS_SHARE_MENU_ITEM_VISIBLE_KEY, isShareMenuItemVisible);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull final MenuItem item) {
        if (drawerToggle.onOptionsItemSelected(item)) {
            return true;
        } else if (item.getItemId() == R.id.action_share) {
            onShareClickListener();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onPrepareOptionsMenu(@NonNull final Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        final MenuItem shareMenuItem = menu.findItem(R.id.action_share);
        shareMenuItem.setVisible(isShareMenuItemVisible);
        return true;
    }

    @Override
    public void onConfigurationChanged(final Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        drawerToggle.onConfigurationChanged(newConfig);
    }

    public void setToolbarTitle(@NonNull final String title) {
        this.title = title;
        if (toolbar != null) {
            toolbar.setTitle(title);
        }
    }

    public void closeDrawer() {
        if (drawerLayout != null) {
            drawerLayout.closeDrawers();
        }
    }

    public void showProgressbar() {
        if (progressBar != null) {
            progressBar.setVisibility(VISIBLE);
        }
    }

    public void hideProgressBar() {
        if (progressBar != null) {
            progressBar.setVisibility(GONE);
        }
    }

    public void showShareButton() {
        isShareMenuItemVisible = true;
        invalidateOptionsMenu();
    }

    public void hideShareButton() {
        isShareMenuItemVisible = false;
        invalidateOptionsMenu();
    }

    public PushChatApplication getPushChatApplication() {
        return (PushChatApplication) getApplication();
    }

    private void registerReceiver() {
        if (receiver == null) {
            receiver = new MainActivityReceiver();
            final IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction(SHOW_PROGRESS_BAR_ACTION);
            intentFilter.addAction(HIDE_PROGRESS_BAR_ACTION);
            intentFilter.addAction(SHOW_GCM_ERROR_DIALOG_ACTION);
            registerReceiver(receiver, intentFilter);
        }
    }

    private void unregisterReceiver() {
        if (receiver != null) {
            unregisterReceiver(receiver);
            receiver = null;
        }
    }

    private void setUpNavigationDrawer() {
        toolbar = (Toolbar) findViewById(R.id.tool_bar);
        setSupportActionBar(toolbar);

        drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawerLayout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);

        drawerToggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar, 0, 0);
        drawerLayout.setDrawerListener(drawerToggle);
    }

    private void showGcmErrorDialog(final int errorCode) {
        if (errorCode != -1) {
            GooglePlayServicesUtil.showErrorDialogFragment(errorCode, this, 0);
        }
    }

    private void onShareClickListener() {
        final Intent intent = new Intent(ACTION_SEND);
        intent.putExtra(Intent.EXTRA_TEXT,
                getString(R.string.uuid_fmt, getPushChatApplication().getUUID()));
        intent.setType("text/plain");
        startActivity(Intent.createChooser(intent, getString(R.string.share_intent_chooser_title)));
    }

    public class MainActivityReceiver extends BroadcastReceiver {

        public static final String SHOW_PROGRESS_BAR_ACTION = "SHOW_PROGRESS_BAR_ACTION";
        public static final String HIDE_PROGRESS_BAR_ACTION = "HIDE_PROGRESS_BAR_ACTION";
        public static final String SHOW_GCM_ERROR_DIALOG_ACTION = "SHOW_GCM_ERROR_DIALOG_ACTION";

        public static final String GCM_ERROR_CODE_EXTRA_KEY = "GCM_ERROR_CODE_EXTRA_KEY";

        @Override
        public void onReceive(@NonNull final Context context, @NonNull final Intent intent) {
            final String action = intent.getAction();
            switch (action) {
                case SHOW_PROGRESS_BAR_ACTION:
                    showProgressbar();
                    break;
                case HIDE_PROGRESS_BAR_ACTION:
                    hideProgressBar();
                    break;
                case SHOW_GCM_ERROR_DIALOG_ACTION:
                    showGcmErrorDialog(intent.getIntExtra(GCM_ERROR_CODE_EXTRA_KEY, -1));
                    break;
            }
        }
    }
}
