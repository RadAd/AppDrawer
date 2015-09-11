package au.radsoft.appdrawer;

import android.preference.PreferenceManager;
import android.content.Context;
import android.content.SharedPreferences;
//import android.util.Log;

public class Preferences extends android.preference.PreferenceActivity
{
    //private static final String LOG_TAG = Preferences.class.getSimpleName();
    
    public static final String PREF_THEME = "pref_theme";
    public static final String PREF_THEME_DARK = "dark";
    public static final String PREF_THEME_LIGHT = "light";
    public static final String PREF_SHOW_SEARCH_ON_STARTUP = "pref_show_search_on_startup";
    
    static void show(Context context)
    {
        android.content.Intent myIntent = new android.content.Intent(context, Preferences.class);
        context.startActivity(myIntent);
    }
    
    static boolean isLightTheme(Context context)
    {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        //setTheme(sharedPref.getInt("pref_theme", R.style.AppThemeActionBarDialogDark));
        String theme = sharedPref.getString(PREF_THEME, PREF_THEME_DARK);
        return PREF_THEME_LIGHT.equals(theme);
    }
    
    @Override
    public void onCreate(android.os.Bundle savedInstanceState)
    {
        if (isLightTheme(this))
            setTheme(R.style.AppThemeDialog_Light);
        
        super.onCreate(savedInstanceState);
        
        addPreferencesFromResource(R.xml.preferences);
    }
}
