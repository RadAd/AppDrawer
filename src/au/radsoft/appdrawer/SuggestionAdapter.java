package au.radsoft.appdrawer;

import android.content.Context;

import android.widget.CursorAdapter;
import android.widget.SearchView;
import android.widget.SimpleCursorAdapter;

import android.database.Cursor;
import android.database.MatrixCursor;

class SuggestionAdapter extends SimpleCursorAdapter implements SearchView.OnSuggestionListener
{
    private static final String FIELD_SUGGESTION = "data";
    private static final String[] from_ = { FIELD_SUGGESTION };
    private static final int[] to_ = { android.R.id.text1 };
    private static final String[] columns_ = { android.provider.BaseColumns._ID, FIELD_SUGGESTION };
    
    private final SearchView searchView_;

    SuggestionAdapter(SearchView searchView)
    {
        super(searchView.getContext(),
            android.R.layout.simple_list_item_1,
            null, from_, to_,
            CursorAdapter.FLAG_REGISTER_CONTENT_OBSERVER);
            
        searchView_ = searchView;
        
        searchView_.setSuggestionsAdapter(this);
        searchView_.setOnSuggestionListener(this);
    }
    
    @Override
    public CharSequence convertToString(Cursor cursor)
    {
        int indexColumnSuggestion = cursor.getColumnIndex(FIELD_SUGGESTION);
        return cursor.getString(indexColumnSuggestion);
    }

    @Override // SearchView.OnSuggestionListener
    public boolean onSuggestionClick(int position)
    {
        Cursor c = getCursor();
        c.moveToPosition(position);
        searchView_.setQuery(convertToString(c), true);
        return true;
    }

    @Override // SearchView.OnSuggestionListener
    public boolean onSuggestionSelect(int position)
    {
        return onSuggestionClick(position);
    }

    public void updateSuggestions(java.util.List<CharSequence> suggestions)
    {
        final MatrixCursor c = new MatrixCursor(columns_);
        int i = 0;
        for (CharSequence cs : suggestions)
        {
            c.addRow(new Object[] { i, cs });
            ++i;
        }
        
        changeCursor(c);
    }
}
