package uk.co.section9.zotdroid;


import android.annotation.TargetApi;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.support.v7.app.ActionBar;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.support.v7.app.AlertDialog;
import android.util.DisplayMetrics;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.List;

import uk.co.section9.zotdroid.webdav.ZoteroDownload;
import uk.co.section9.zotdroid.webdav.ZoteroWebDavCallback;

/**
 * A {@link PreferenceActivity} that presents a set of application settings. On
 * handset devices, settings are presented as a single list. On tablets,
 * settings are split by category, with category headers shown to the left of
 * the list of settings.
 * <p>
 * See <a href="http://developer.android.com/design/patterns/settings.html">
 * Android Design: Settings</a> for design guidelines and the <a
 * href="http://developer.android.com/guide/topics/ui/settings.html">Settings
 * API Guide</a> for more information on developing a Settings UI.
 */
public class SettingsActivity extends AppCompatPreferenceActivity  implements ZoteroWebDavCallback{

    private static final String TAG = "zotdroid.Settings";
    private static Context  _context; // Used later for popup messages
    private static Dialog   _webdav_dialog;
    private static Button   _webdav_button;

    private ZoteroDownload _zotero_download = new ZoteroDownload();
    /**
     * A preference value change listener that updates the preference's summary
     * to reflect its new value.
     */
    private static Preference.OnPreferenceChangeListener sBindPreferenceSummaryToValueListener = new Preference.OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object value) {
            String stringValue = value.toString();

            if (preference instanceof ListPreference) {
                // For list preferences, look up the correct display value in
                // the preference's 'entries' list.
                ListPreference listPreference = (ListPreference) preference;
                int index = listPreference.findIndexOfValue(stringValue);

                // Set the summary to reflect the new value.
                preference.setSummary(
                        index >= 0
                                ? listPreference.getEntries()[index]
                                : null);

            } else {
                // For all other preferences, set the summary to the value's
                // simple string representation.
                preference.setSummary(stringValue);
            }
            return true;
        }
    };

    /**
     * Helper method to determine if the device has an extra-large screen. For
     * example, 10" tablets are extra-large.
     */
    private static boolean isXLargeTablet(Context context) {
        return (context.getResources().getConfiguration().screenLayout
                & Configuration.SCREENLAYOUT_SIZE_MASK) >= Configuration.SCREENLAYOUT_SIZE_XLARGE;
    }

    /**
     * Binds a preference's summary to its value. More specifically, when the
     * preference's value is changed, its summary (line of text below the
     * preference title) is updated to reflect the value. The summary is also
     * immediately updated upon calling this method. The exact display format is
     * dependent on the type of preference.
     *
     * @see #sBindPreferenceSummaryToValueListener
     */
    private static void bindPreferenceSummaryToValue(Preference preference) {
        // Set the listener to watch for value changes.
        preference.setOnPreferenceChangeListener(sBindPreferenceSummaryToValueListener);

        // Trigger the listener immediately with the preference's
        // current value.
        sBindPreferenceSummaryToValueListener.onPreferenceChange(preference,
                PreferenceManager
                        .getDefaultSharedPreferences(preference.getContext())
                        .getString(preference.getKey(), ""));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        _context = SettingsActivity.this;
        setupActionBar();
        // Add a button to the header list.
        _webdav_button = new Button(this);
        _webdav_button.setText("Test WebDav Settings");
        setListFooter(_webdav_button);
        final SettingsActivity _pp = this;

        _webdav_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                _webdav_button.setClickable(false);
                _webdav_dialog = launchLoadingDialog();
                String status_message = "Testing Webdav Connection.";
                TextView messageView = (TextView) _webdav_dialog.findViewById(R.id.textViewLoading);
                messageView.setText(status_message);

                _zotero_download.testWebDav(_pp,_pp);

                //_zotdroid_user_ops.testWebDav();
            }
        });
    }

    private Dialog launchLoadingDialog() {
        final Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.fragment_loading);
        dialog.setCanceledOnTouchOutside(false);
        dialog.setCancelable(true);

        ProgressBar pb = (ProgressBar) dialog.findViewById(R.id.progressBarLoading);
        pb.setVisibility(View.VISIBLE);

        DisplayMetrics displayMetrics = this.getResources().getDisplayMetrics();
        int dialogWidth = (int)(displayMetrics.widthPixels * 0.85);
        int dialogHeight = (int)(displayMetrics.heightPixels * 0.85);
        dialog.getWindow().setLayout(dialogWidth, dialogHeight);

        Button cancelButton = (Button) dialog.findViewById(R.id.buttonCancelLoading);

        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //_zotdroid_sync_ops.stop();
                dialog.dismiss();
            }
        });

        dialog.show();
        return dialog;
    }

    /**
     * Set up the {@link android.app.ActionBar}, if the API is available.
     */
    private void setupActionBar() {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            // Show the Up button in the action bar.
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onIsMultiPane() {
        return isXLargeTablet(this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public void onBuildHeaders(List<Header> target) {
        loadHeadersFromResource(R.xml.pref_headers, target);
    }

    /**
     * This method stops fragment injection in malicious applications.
     * Make sure to deny any unknown fragments here.
     */
    protected boolean isValidFragment(String fragmentName) {
        return PreferenceFragment.class.getName().equals(fragmentName)
                || GeneralPreferenceFragment.class.getName().equals(fragmentName)
                || WebDavPreferenceFragment.class.getName().equals(fragmentName);
    }

    @Override
    public void onWebDavProgess(boolean result, String message) {}

    @Override
    public void onWebDavComplete(boolean result, String message) {
        String status_message = "Connection Failed: " + message;
        if (result) {
            status_message = "Connection succeded";
        }
        TextView messageView = (TextView) _webdav_dialog.findViewById(R.id.textViewLoading);
        messageView.setText(status_message);
        Button button = (Button) _webdav_dialog.findViewById(R.id.buttonCancelLoading);
        button.setText("Dismiss");

        ProgressBar pb = (ProgressBar) _webdav_dialog.findViewById(R.id.progressBarLoading);
        pb.setVisibility(View.INVISIBLE);
        _webdav_button.setClickable(true);
    }

    /**
     * This fragment shows general preferences only. It is used when the
     * activity is showing a two-pane settings UI.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class GeneralPreferenceFragment extends PreferenceFragment {

        /**
         * Another listener that I've made to post messages for certain changes
         * It creates a popup dialog to warn the user to restart ZotDroid
         */
        private Preference.OnPreferenceChangeListener _messenger_db_location;
        private Preference.OnPreferenceChangeListener _messenger_font_size;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_general);
            setHasOptionsMenu(true);

            // Bind the summaries of EditText/List/Dialog/Ringtone preferences
            // to their values. When their values change, their summaries are
            // updated to reflect the new value, per the Android Design
            // guidelines.
            bindPreferenceSummaryToValue(findPreference("settings_user_id"));
            bindPreferenceSummaryToValue(findPreference("settings_user_secret"));
            bindPreferenceSummaryToValue(findPreference("settings_user_key"));

            _messenger_db_location = new Preference.OnPreferenceChangeListener() {
                AlertDialog.Builder builder  = new AlertDialog.Builder(_context, R.style.ZotDroidAlertDialogStyle);
                @Override
                public boolean onPreferenceChange(Preference preference, Object o) {
                    if (preference == findPreference("settings_db_location")){
                        builder.setTitle("Changed Database location.")
                                .setMessage("You will need to close and restart ZotDroid for this change to take effect. Once restarted you will need to resync.")
                                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {}
                                })
                                .setIcon(android.R.drawable.ic_dialog_alert)
                                .show();
                    }
                    return true;
                }
            };

            _messenger_font_size = new Preference.OnPreferenceChangeListener() {
                AlertDialog.Builder builder  = new AlertDialog.Builder(_context, R.style.ZotDroidAlertDialogStyle);
                @Override
                public boolean onPreferenceChange(Preference preference, Object o) {
                    if (preference == findPreference("settings_font_size")){
                        // TODO - change the major text elements here
                        Intent fontChangeIntent = new Intent();
                        String value = o.toString();
                        fontChangeIntent.setAction("FONT_SIZE_PREFERENCE_CHANGED");
                        fontChangeIntent.putExtra("fontsize",value);
                        getActivity().sendBroadcast(fontChangeIntent);
                    }
                    return true;
                }
            };

            findPreference("settings_db_location").setOnPreferenceChangeListener(_messenger_db_location);
            findPreference("settings_font_size").setOnPreferenceChangeListener(_messenger_font_size);
        }

        @Override
        public boolean onOptionsItemSelected(MenuItem item) {
            int id = item.getItemId();
            if (id == android.R.id.home) {
                //startActivity(new Intent(getActivity(), SettingsActivity.class));
                getActivity().finish();
                return true;
            }
            return super.onOptionsItemSelected(item);
        }
    }


    /**
     * This fragment shows data and sync preferences only. It is used when the
     * activity is showing a two-pane settings UI.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class DataSyncPreferenceFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setHasOptionsMenu(true);
        }

        @Override
        public boolean onOptionsItemSelected(MenuItem item) {
            int id = item.getItemId();
            if (id == android.R.id.home) {
                startActivity(new Intent(getActivity(), SettingsActivity.class));
                return true;
            }
            return super.onOptionsItemSelected(item);
        }
    }

    /**
     * This fragment shows webdav preferences only. It is used when the
     * activity is showing a two-pane settings UI.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class WebDavPreferenceFragment extends PreferenceFragment {

        private Preference.OnPreferenceChangeListener webdav_settings_verifier;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_webdav);
            setHasOptionsMenu(true);

            // Bind the summaries of EditText/List/Dialog/Ringtone preferences
            // to their values. When their values change, their summaries are
            // updated to reflect the new value, per the Android Design
            // guidelines.
            bindPreferenceSummaryToValue(findPreference("settings_webdav_address"));
            bindPreferenceSummaryToValue(findPreference("settings_webdav_username"));
            Preference settings_webdav_password = findPreference("settings_webdav_password");
            settings_webdav_password.setSummary("hidden");

            webdav_settings_verifier = new Preference.OnPreferenceChangeListener() {
                AlertDialog.Builder builder  = new AlertDialog.Builder(_context, R.style.ZotDroidAlertDialogStyle);
                @Override
                public boolean onPreferenceChange(Preference preference, Object o) {
                    if (preference == findPreference("settings_webdav_address")){
                        // TODO - Make sure its https in here - warn user otherwise!
                    }
                    return true;
                }
            };
        }

        @Override
        public boolean onOptionsItemSelected(MenuItem item) {
            int id = item.getItemId();
            if (id == android.R.id.home) {
                getActivity().finish();
                return true;
            }
            return super.onOptionsItemSelected(item);
        }
    }




}
