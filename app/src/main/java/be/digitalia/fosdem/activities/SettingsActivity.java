package be.digitalia.fosdem.activities;

import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.preference.TwoStatePreference;
import android.support.annotation.RequiresApi;
import android.view.MenuItem;

import be.digitalia.fosdem.R;
import be.digitalia.fosdem.services.AlarmIntentService;

public class SettingsActivity extends AppCompatPreferenceActivity implements SharedPreferences.OnSharedPreferenceChangeListener {

	public static final String KEY_PREF_NOTIFICATIONS_ENABLED = "notifications_enabled";
	// Android >= O only
	public static final String KEY_PREF_NOTIFICATIONS_CHANNEL = "notifications_channel";
	// Android < O only
	public static final String KEY_PREF_NOTIFICATIONS_VIBRATE = "notifications_vibrate";
	// Android < O only
	public static final String KEY_PREF_NOTIFICATIONS_LED = "notifications_led";
	public static final String KEY_PREF_NOTIFICATIONS_DELAY = "notifications_delay";

	@SuppressWarnings("deprecation")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		getSupportActionBar().setDisplayHomeAsUpEnabled(true);

		addPreferencesFromResource(R.xml.settings);
		updateNotificationsEnabled();
		updateNotificationsDelaySummary();
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			setupNotificationsChannel();
		}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case android.R.id.home:
				onBackPressed();
				return true;
		}
		return false;
	}

	@Override
	public void onBackPressed() {
		super.onBackPressed();
		overridePendingTransition(R.anim.partial_zoom_in, R.anim.slide_out_right);
	}

	@Override
	protected void onResume() {
		super.onResume();
		PreferenceManager.getDefaultSharedPreferences(this).registerOnSharedPreferenceChangeListener(this);
	}

	@Override
	protected void onPause() {
		PreferenceManager.getDefaultSharedPreferences(this).unregisterOnSharedPreferenceChangeListener(this);
		super.onPause();
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		if (KEY_PREF_NOTIFICATIONS_ENABLED.equals(key)) {
			updateNotificationsEnabled();
		} else if (KEY_PREF_NOTIFICATIONS_DELAY.equals(key)) {
			updateNotificationsDelaySummary();
		}
	}

	@SuppressWarnings("deprecation")
	private void updateNotificationsEnabled() {
		boolean notificationsEnabled = ((TwoStatePreference) findPreference(KEY_PREF_NOTIFICATIONS_ENABLED)).isChecked();
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			findPreference(KEY_PREF_NOTIFICATIONS_CHANNEL).setEnabled(notificationsEnabled);
		} else {
			findPreference(KEY_PREF_NOTIFICATIONS_VIBRATE).setEnabled(notificationsEnabled);
			findPreference(KEY_PREF_NOTIFICATIONS_LED).setEnabled(notificationsEnabled);
		}
		findPreference(KEY_PREF_NOTIFICATIONS_DELAY).setEnabled(notificationsEnabled);
	}

	@SuppressWarnings("deprecation")
	private void updateNotificationsDelaySummary() {
		ListPreference notificationsDelayPreference = (ListPreference) findPreference(KEY_PREF_NOTIFICATIONS_DELAY);
		notificationsDelayPreference.setSummary(notificationsDelayPreference.getEntry());
	}

	@RequiresApi(api = Build.VERSION_CODES.O)
	private void setupNotificationsChannel() {
		findPreference(KEY_PREF_NOTIFICATIONS_CHANNEL).setOnPreferenceClickListener(
				new Preference.OnPreferenceClickListener() {
					@Override
					public boolean onPreferenceClick(Preference preference) {
						AlarmIntentService.startChannelNotificationSettingsActivity(SettingsActivity.this);
						return true;
					}
				});
	}
}
