package au.radsoft.appdrawer;

import android.content.Context;

import android.view.MenuItem;

import android.widget.Toast;

class Utils
{
    private Utils()
    {
    }
    
    static void toast(Context context, String msg)
    {
        Toast toast = Toast.makeText(context, msg, Toast.LENGTH_LONG);
        toast.show();
    }
    
    static int[] getState(MenuItem item)
    {
        int count = 0;
        if (item.isEnabled())
            ++count;
        if (item.isCheckable())
            ++count;
        if (item.isChecked())
            ++count;
        int pos = 0;
        int[] state = new int[count];
        if (item.isEnabled())
            state[pos++] = android.R.attr.state_enabled;
        if (item.isCheckable())
            state[pos++] = android.R.attr.state_checkable;
        if (item.isChecked())
            state[pos++] = android.R.attr.state_checked;
        assert pos == count;
        return state;
    }
    
    static void storeObject(java.io.File f, Object o)
    {
        try
        {
            java.io.ObjectOutputStream out = new java.io.ObjectOutputStream(new java.io.FileOutputStream(f));
            out.writeObject(o);
            out.close();
        }
        catch (java.io.IOException e)
        {
            e.printStackTrace();
            //toast(context, "Saving cache: " + e);
        }
    }
    
    static Object loadObject(java.io.File f)
    {
        try
        {
            Object o = null;
            if (f.exists())
            {
                java.io.ObjectInputStream in = new java.io.ObjectInputStream(new java.io.FileInputStream(f));
                o = in.readObject();
                in.close();
            }
            return o;
        }
        catch (ClassNotFoundException e)
        {
            e.printStackTrace();
            //toast(context, "Loading cache: " + e);
            return null;
        }
        catch (java.io.IOException e)
        {
            e.printStackTrace();
            //toast(context, "Loading cache: " + e);
            return null;
        }
    }
}
