package com.crowdmobile.kesapp;

import android.app.Activity;
import android.os.Bundle;

import com.crowdmobile.kesapp.fragment.PrefsFragment;

public class SettingsActivity extends Activity {

    private boolean languageChanged = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // TODO Auto-generated method stub
        super.onCreate(savedInstanceState);

        getFragmentManager().beginTransaction().replace(android.R.id.content,
                new PrefsFragment()).commit();
    }

    public void notifyLanguageChanged()
    {
        languageChanged = true;
        getFragmentManager().beginTransaction().replace(android.R.id.content,
                new PrefsFragment()).commit();
    }

    @Override
    public void onBackPressed() {
        if (!languageChanged)
            super.onBackPressed();
        else
            MainActivity.open(this);
    }


}
