package com.crowdmobile.kesapp.fragment;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.MultiSelectListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;

import com.crowdmobile.kesapp.R;
import com.crowdmobile.kesapp.widget.DatePreference;
import com.kes.KES;
import com.kes.model.User;

import java.util.Map;

/**
 * Created by gadza on 2015.06.25..
 */
public class PrefsFragment extends PreferenceFragment {

    Preference accountName;
    SharedPreferences sp;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sp = PreferenceManager.getDefaultSharedPreferences(getActivity());
        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.preferences);
        Map<String, ?> map = sp.getAll();
        for (Map.Entry<String, ?> entry : map.entrySet())
        updateValue(entry.getKey());
        accountName = findPreference(getString(R.string.key_accountname));
        User u  = KES.shared().getAccountManager().getUser();
        accountName.setTitle(u.getFullName());
    }

    @Override
    public void onStart() {
        super.onStart();
        sp.registerOnSharedPreferenceChangeListener(preferenceChangeListener);
    }

    @Override
    public void onStop() {
        super.onStop();
        sp.unregisterOnSharedPreferenceChangeListener(preferenceChangeListener);
    }

    private void updateValue(String key)
    {
        Preference p = findPreference(key);
        if (p == null)
            return;

        if (p instanceof ListPreference) {
            ListPreference listPref = (ListPreference) p;
            p.setSummary(listPref.getEntry());
        }
        if (p instanceof EditTextPreference) {
            EditTextPreference editTextPref = (EditTextPreference) p;
            if (p.getTitle().toString().contains("assword"))
            {
                p.setSummary("******");
            } else {
                p.setSummary(editTextPref.getText());
            }
        }
        if (p instanceof MultiSelectListPreference) {
            EditTextPreference editTextPref = (EditTextPreference) p;
            p.setSummary(editTextPref.getText());
        }
        if (p instanceof DatePreference) {
            DatePreference datePref = (DatePreference) p;
            p.setSummary(datePref.getText());
        }
    }

    SharedPreferences.OnSharedPreferenceChangeListener preferenceChangeListener = new SharedPreferences.OnSharedPreferenceChangeListener() {

        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            updateValue(key);
        }
    };


}
