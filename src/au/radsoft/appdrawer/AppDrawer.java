package au.radsoft.appdrawer;

import android.app.Activity;

import android.os.Bundle;

import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MenuInflater;
import android.view.View;

import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.SearchView;
import android.widget.Toast;

public class AppDrawer extends Activity implements AdapterView.OnItemClickListener, SearchView.OnQueryTextListener
{
    private static String[] suggestions_ = { "google", "microsoft", "samsung", "#disabled", "#new", "#updated" };
    
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
    
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        enableActionBar();
        
        setContentView(R.layout.main);
        
        super.onCreate(savedInstanceState);
        
        GridView list = (GridView) findViewById(R.id.list);
        View progress = findViewById(R.id.progress);
        
        adapterAppList_ = new AppListAdapter(getPackageManager(), getLayoutInflater(), progress, getCacheDir());
        list.setAdapter(adapterAppList_);
		list.setOnItemClickListener(this);
        registerForContextMenu(list);
		
		setFinishOnTouchOutside(true);
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
        }
        
        return super.onCreateOptionsMenu(menu);
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId())
        {
        //case R.id.action_add:
            //adapterAppList_.add();
            //break;
            
        default:
            return false;
        }
        
        //return true;
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
        java.util.List<CharSequence> suggestions = new java.util.ArrayList<CharSequence>();
        
        //adapterAppList_.getSuggestions(newText, suggestions);
        
        String query = newText.toLowerCase();
        for (int i = 0; i < suggestions_.length; i++)
        {
            if (suggestions_[i].startsWith(query))
                suggestions.add(suggestions_[i]);
        }
        
        adapterSuggestion_.updateSuggestions(suggestions);
        
        //if (newText.length() < 3)
            //newText = "";
        if (!searchText_.equalsIgnoreCase(newText))
        {
            adapterAppList_.filter(newText);
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
    
    void toast(String msg)
    {
        Toast toast = Toast.makeText(this, msg, Toast.LENGTH_LONG);
        toast.show();
    }
}
