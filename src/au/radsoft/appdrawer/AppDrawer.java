package au.radsoft.appdrawer;

import android.content.SharedPreferences;

import android.preference.PreferenceManager;

import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MenuInflater;
import android.view.View;

import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.SearchView;

import android.graphics.drawable.StateListDrawable;

// TODO
// Tags
// Show recent apps (deprecated in android lollipop)
// Listen to broadcasts for app install/uninstall to update list ie ACTION_PACKAGE_ADDED / ACTION_PACKAGE_REMOVED
// finish after launching app
// apply search when apps finally loaded

public class AppDrawer extends android.app.Activity implements AdapterView.OnItemClickListener, SearchView.OnQueryTextListener
{
    static void setThreshold(SearchView searchView, int i)
    {
        int autoCompleteTextViewID = searchView.getContext().getResources().getIdentifier("android:id/search_src_text", null, null);
        android.widget.AutoCompleteTextView searchAutoCompleteTextView = (android.widget.AutoCompleteTextView) searchView.findViewById(autoCompleteTextViewID);
        if (searchAutoCompleteTextView != null)
            searchAutoCompleteTextView.setThreshold(i);
    }
    
    private String searchText_ = "";
    private AppListAdapter adapterAppList_;
    private SuggestionAdapter adapterSuggestion_;
    private MenuItem menuFav_ = null;
    private StateListDrawable stateFav_ = null;
    
    @Override
    public void onCreate(android.os.Bundle savedInstanceState)
    {
        enableActionBar();
        
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
        if (Preferences.isLightTheme(this))
            setTheme(R.style.AppThemeActionBarDialog_Light);
        
        setContentView(R.layout.main);
        
        super.onCreate(savedInstanceState);
        
        GridView list = (GridView) findViewById(R.id.list);
        View progress = findViewById(R.id.progress);
        
        adapterAppList_ = new AppListAdapter(getPackageManager(), getLayoutInflater(), progress, getDir("data", MODE_PRIVATE), getCacheDir());
        list.setAdapter(adapterAppList_);
		list.setOnItemClickListener(this);
        registerForContextMenu(list);
		
		setFinishOnTouchOutside(true);
    }
    
    private void updateFavIcon()
    {
        if (stateFav_ != null)
        {
            stateFav_.setState(Utils.getState(menuFav_));
            menuFav_.setIcon(stateFav_.getCurrent());
        }
    }

    private void enableActionBar()
    {
        requestWindowFeature(android.view.Window.FEATURE_ACTION_BAR);
        
        android.util.DisplayMetrics metrics = new android.util.DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        
        android.view.WindowManager.LayoutParams params = getWindow().getAttributes(); 
        params.width = metrics.widthPixels;
        if (metrics.heightPixels < metrics.widthPixels)
            params.width = metrics.heightPixels;
        params.height = android.view.WindowManager.LayoutParams.MATCH_PARENT;
        getWindow().setAttributes(params);
        //getWindow().setFlags(android.view.WindowManager.LayoutParams.FLAG_DIM_BEHIND, android.view.WindowManager.LayoutParams.FLAG_DIM_BEHIND);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.options, menu);
        
        final MenuItem menuSearch = menu.findItem(R.id.action_search);
        SearchView searchView = (SearchView) menuSearch.getActionView();
        if (searchView != null)
        {
            searchView.setQueryHint(menuSearch.getTitle());
            setThreshold(searchView, 1);
            searchView.setOnQueryTextListener(this);
            adapterSuggestion_ = new SuggestionAdapter(searchView);
            
            SharedPreferences sharedPref = getDefaultSharedPreferences();
            if (sharedPref.getBoolean(Preferences.PREF_SHOW_SEARCH_ON_STARTUP, false))
            {
                menuSearch.expandActionView();
            }
        }
        
        menuFav_ = menu.findItem(R.id.action_favourite);
        if (menuFav_ != null)
        {
            stateFav_ = (StateListDrawable) menuFav_.getIcon();
            //menuFav_.setChecked(true);
        }
        
        return super.onCreateOptionsMenu(menu);
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId())
        {
        case R.id.action_favourite:
            {
                if (searchText_.isEmpty())
                {
                    // TODO if string is empty popup explanation message
                }
                else
                {
                    item.setChecked(!item.isChecked());
                    updateFavIcon();
                    adapterAppList_.updateFav(searchText_.toLowerCase(), item.isChecked());
                }
            }
            break;
            
        case R.id.action_preferences:
            {
                Preferences.show(this);
            }
            break;
            
        default:
            return false;
        }
        
        return true;
    }
    
    @Override
    public void onCreateContextMenu(ContextMenu menu, View view, ContextMenu.ContextMenuInfo menuInfo)
    {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.item, menu);

        super.onCreateContextMenu(menu, view, menuInfo);
    }
    
    @Override
    public boolean onContextItemSelected(MenuItem item)
    {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        return adapterAppList_.doAction(this, item.getItemId(), info.position);
    }
    
    @Override // AdapterView.OnItemClickListener
    public void onItemClick(AdapterView<?> parent, View view, int position, long id)
    {
        adapterAppList_.doAction(this, R.id.action_open, position);
    }
    
    @Override // SearchView.OnQueryTextListener
    public boolean onQueryTextChange(String newText)
    {
        //if (newText.length() < 3)
            //newText = "";
        if (!searchText_.equalsIgnoreCase(newText))
        {
            String query = newText.toLowerCase();
            String[] qs = query.split(" ");
            
            adapterSuggestion_.updateSuggestions(adapterAppList_.getSuggestions(qs));
            
            menuFav_.setChecked(adapterAppList_.getFavs().contains(query));
            updateFavIcon();
            
            adapterAppList_.filter(qs);
            searchText_ = newText;
        }
        return false;
    }

    @Override // SearchView.OnQueryTextListener
    public boolean onQueryTextSubmit(String p1)
    {
        if (adapterAppList_.getCount() == 1)
            adapterAppList_.doAction(this, R.id.action_open, 0);
        return false;
    }
    
    SharedPreferences getDefaultSharedPreferences()
    {
        return PreferenceManager.getDefaultSharedPreferences(this);
    }
    
    void toast(String msg)
    {
        Utils.toast(this, msg);
    }
}
