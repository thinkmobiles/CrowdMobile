package com.crowdmobile.kesapp.fragment;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.MultiSelectListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceGroup;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.util.DisplayMetrics;

import com.crowdmobile.kesapp.R;
import com.crowdmobile.kesapp.SettingsActivity;
import com.crowdmobile.kesapp.widget.DatePreference;
import com.kes.KES;
import com.kes.model.User;

import java.util.Locale;
import java.util.Map;

/**
 * Created by gadza on 2015.06.25..
 */
public class PrefsFragment extends PreferenceFragment {

    SharedPreferences sp;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sp = PreferenceManager.getDefaultSharedPreferences(getActivity());
        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.preferences);
        PreferenceScreen preferenceScreen = (PreferenceScreen)findPreference(getString(R.string.key_preferenceScreen));

        Map<String, ?> map = sp.getAll();
        for (Map.Entry<String, ?> entry : map.entrySet())
        updateValue(entry.getKey());

        //Account group
        PreferenceGroup preferenceGroup = (PreferenceGroup)findPreference(getString(R.string.key_account));
        if (preferenceGroup != null)
        {
            User u  = KES.shared().getAccountManager().getUser();
            if (u != null && u.isRegistered()) {
                Preference account = findPreference(getString(R.string.key_accountname));
                if (account != null)
                    account.setTitle(u.getFullName());
            }
            else
                preferenceScreen.removePreference(preferenceGroup);
        }

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
            if (getString(R.string.key_language).equals(key)) {
                setLocale(getActivity(), true);
                ((SettingsActivity)getActivity()).notifyLanguageChanged();
                //getActivity().recreate();
            }
        }
    };


    public static void setLocale(Context context,boolean force) {
        String language = PreferenceManager.getDefaultSharedPreferences(context)
                .getString(context.getString(R.string.key_language), "0");
        boolean defaultLang = language.equals("0");
        if (defaultLang && !force)
            return;
        Resources res = context.getResources();
        DisplayMetrics dm = res.getDisplayMetrics();
        Configuration conf = res.getConfiguration();
        if (defaultLang)
            conf.locale = Locale.getDefault();
        else {
            String[] locales = language.split("_");
            if (locales.length == 1)
                conf.locale = new Locale(language);
            else
                conf.locale = new Locale(locales[0],locales[1]);
        }
        res.updateConfiguration(conf, dm);
        KES.shared().getAccountManager().setLocale(conf.locale);
        /*
        Intent refresh = new Intent(this, AndroidLocalize.class);
        startActivity(refresh);
        */
    }


}
