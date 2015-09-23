package au.radsoft.appdrawer;

import android.widget.ImageView;
import android.widget.TextView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
//import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
//import android.util.Log;

import au.radsoft.utils.CharSequenceUtils;

public class AppListAdapter extends android.widget.BaseAdapter
{
    private static final String LOG_TAG = AppListAdapter.class.getSimpleName();
    
    private static String TAG_ALL = "#all";
    private static String TAG_DISABLED = "#disabled";
    private static String TAG_NEW = "#new";
    private static String TAG_NO_TAGS = "#notags";
    private static String TAG_UPDATED = "#updated";
    private static final String[] specialtags_ = { TAG_DISABLED, TAG_NEW, TAG_NO_TAGS, TAG_UPDATED };
    
    private static final class Info implements java.io.Serializable
    {
        private static final long serialVersionUID = 5L;
        
        private final boolean enabled_;
        private final CharSequence label_;
        private final String uri_;
        private final long firstInstallTime_;
        private final long lastUpdateTime_;
        
        Info(boolean enabled, CharSequence label, String uri, long firstInstallTime, long lastUpdateTime)
        {
            enabled_ = enabled;
            label_ = label;
            uri_ = uri;
            firstInstallTime_ = firstInstallTime;
            lastUpdateTime_ = lastUpdateTime;
        }
    }
    
    private static final class App
    {
        App(ApplicationInfo ai, Info info, Intent intent)
        {
            ai_ = ai;
            info_ = info;
            intent_ = intent;
        }
        
        void loadDrawable(PackageManager pm)
        {
            if (img_ == null && intent_ != null)
            {
                try
                {
                    img_ = pm.getActivityIcon(intent_);
                }
                catch (PackageManager.NameNotFoundException e)
                {
                    // ignore
                }
            }
            if (img_ == null)
            {
                img_ = pm.getApplicationIcon(ai_);  // Note: returns default image if none is found
            }
        }
        
        boolean isEnabled()
        {
            return ai_.enabled;
        }
        
        String getPackageName()
        {
            return ai_.packageName;
        }
        
        boolean canLaunch()
        {
            return intent_ != null;
        }
        
        private final ApplicationInfo ai_;
        private final Info info_;
        private Intent intent_;
        private Drawable img_ = null;
    }
    
    private static final class LabelComparator implements java.util.Comparator<App>
    {
        @Override
        public int compare(App p1, App p2)
        {
            return CharSequenceUtils.compareIgnoreCase(p1.info_.label_, p2.info_.label_);
        }

        @Override
        public boolean equals(Object p1)
        {
            return this == p1;
        }
    }
    
    private final class FilterAsyncTask extends AsyncTask<String, Integer, List<App>>
    {
        FilterAsyncTask()
        {
            if (filterAsyncTask_ != null)
                filterAsyncTask_.cancel(false);
            filterAsyncTask_ = this;
        }

        @Override
        protected List<App> doInBackground(String... texts)
        {
            return filterApps(texts, this);
        }
        
        @Override
        protected void onPostExecute(List<App> result)
        {
            super.onPostExecute(result);
            if (filterAsyncTask_ == this)
                filterAsyncTask_ = null;
            apps_ = result;
            notifyDataSetChanged();
        }
    }
    
    interface Progress
    {
        void startProgress(int count);
        void incrementProgress(int i);
    }

    void loadApps(Progress p)
    {
        if (all_ != null)
            return;
            
        {
            Set<String> favs = (Set<String>) Utils.loadObject(favsFile_);
            if (favs != null)
                favs_ = favs;
            else
            {
                favs_.add("google");
                favs_.add("microsoft");
                favs_.add("samsung");
                favs_.add("games");
                favs_.add("utilities");
            }
        }
        
        Map<String, Set<String>> tags = (Map<String, Set<String>>) Utils.loadObject(tagsFile_);
        if (tags == null)
            tags = new java.util.HashMap();
        Map<String, Set<String>> tagsNew = new java.util.HashMap();
        
        Map<String, Info> infoCache = (Map<String, Info>) Utils.loadObject(infoFile_);
        if (infoCache == null)
            infoCache = new java.util.HashMap();
        Map<String, Info> infoCacheNew = new java.util.HashMap();

        boolean showProgress = infoCache.isEmpty();
        boolean dirty = false;
        
        List<App> apps = new java.util.ArrayList();

        List<PackageInfo> installed = pm_.getInstalledPackages(0);
        if (showProgress)
            p.startProgress(installed.size());
        for (PackageInfo pi : installed)
        {
            Info info = infoCache.get(pi.packageName);
            Intent intent = null;

            if (info == null
                || pi.applicationInfo.enabled != info.enabled_
                || pi.lastUpdateTime > info.lastUpdateTime_)
            {
                intent = pi.applicationInfo.enabled ? pm_.getLaunchIntentForPackage(pi.applicationInfo.packageName) : null;
                ResolveInfo ri = intent == null ? null : pm_.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY);
                CharSequence label = ri != null ? ri.loadLabel(pm_) : pm_.getApplicationLabel(pi.applicationInfo);
                String uri = intent == null ? null : intent.toURI().toString();
                info = new Info(pi.applicationInfo.enabled, label, uri, pi.firstInstallTime, pi.lastUpdateTime);
                dirty = true;
            }
            else
            {
                if (info.uri_ != null && !info.uri_.isEmpty())
                {
                    try
                    {
                        intent = Intent.parseUri(info.uri_, 0);
                    }
                    catch (java.net.URISyntaxException e)
                    {
                        // ignore
                    }
                }
            }
            
            App app = new App(pi.applicationInfo, info, intent);
            apps.add(app);
            infoCacheNew.put(pi.packageName, info);
            Set<String> thisTags = tags.get(pi.packageName);
            if (thisTags != null)
                tagsNew.put(pi.packageName, thisTags);
            
            if (showProgress)
                p.incrementProgress(1);
        }
        
        if (dirty || infoCacheNew.size() != infoCache.size())
            Utils.storeObject(infoFile_, infoCacheNew);
        if (tagsNew.size() != tags.size())
            Utils.storeObject(tagsFile_, tagsNew);
            
        all_ = apps;
        tags_ = tagsNew;
    }
    
    void setImageDrawable(ImageView v, App app)
    {
        AsyncTask oldTask = (AsyncTask) v.getTag();
        v.setTag(null);
        if (oldTask != null)
            oldTask.cancel(false);
            
        if (app.img_ == null)
        {
            v.setImageDrawable(null);
            DrawableLoaderAsyncTask dlat = new DrawableLoaderAsyncTask(v);
            v.setTag(dlat);
            dlat.execute(app);
        }
        else
        {
            v.setImageDrawable(app.img_);
        }
    }
    
    private final class DrawableLoaderAsyncTask extends AsyncTask<App, Void, Drawable>
    {
        private final java.lang.ref.WeakReference<ImageView> v_;
        
        DrawableLoaderAsyncTask(ImageView v)
        {
            v_ = new java.lang.ref.WeakReference<ImageView>(v);
        }
        
        @Override
        protected Drawable doInBackground(App ... apps)
        {
            apps[0].loadDrawable(pm_);
            return apps[0].img_;
        }
        
        @Override
        protected void onPostExecute(Drawable result)
        {
            ImageView v = v_.get();
            if (v != null && v.getTag() == this)
            {
                v.setTag(null);
                v.setImageDrawable(result);
            }
        }
    }
    
    private List<App> filterApps(String[] text, AsyncTask task)
    {
        List<App> apps = new java.util.ArrayList<App>();
        
        final long time = System.currentTimeMillis() - 1000 * 60 * 60 * 24 * 7;
        boolean testEnabled = true;
        boolean testDisabled = false;
        boolean testLaunch = true;
        boolean testInstallTime = false;
        boolean testNoTags = false;
        boolean testUpdateTime = false;
        
        for (String t : text)
        {
            if (!t.isEmpty() && t.charAt(0) == '#')
            {
                if (TAG_ALL.equalsIgnoreCase(t))
                {
                    testEnabled = false;
                    testDisabled = false;
                    testLaunch = false;
                }
                else if (TAG_DISABLED.equalsIgnoreCase(t))
                {
                    testEnabled = false;
                    testDisabled = true;
                    testLaunch = false;
                }
                else if (TAG_NEW.equalsIgnoreCase(t))
                {
                    testInstallTime = true;
                }
                else if (TAG_NO_TAGS.equalsIgnoreCase(t))
                {
                    testNoTags = true;
                }
                else if (TAG_UPDATED.equalsIgnoreCase(t))
                {
                    testUpdateTime = true;
                }
            }
        }
        
        for (App app : all_)
        {
            if (   (!testEnabled     || app.isEnabled())
                && (!testDisabled    || !app.isEnabled())
                && (!testLaunch      || app.canLaunch())
                && (!testInstallTime || app.info_.firstInstallTime_ > time)
                && (!testNoTags      || tags_.get(app.getPackageName()) == null)
                && (!testUpdateTime  || app.info_.lastUpdateTime_ > time))
            {
                boolean add = false;
                
                CharSequence[] strs = { app.getPackageName(), app.info_.label_ };
                if (findAny(strs, text))
                {
                    add = true;
                }
                
                final Set<String> thistags = tags_.get(app.getPackageName());
                if (thistags != null && add == false)
                {
                    for (String t : text)
                    {
                        if (thistags.contains(t))   // TODO Should this be a partial match?
                        {
                            add = true;
                            break;
                        }
                    }
                }
                
                if (add)
                    apps.add(app);
            }
            
            if (task.isCancelled())
                break;
        }
        
        // NOTE LabelComparator assumes that labels have been loaded
        if (!task.isCancelled())
            Collections.sort(apps, new LabelComparator());
        
        return apps;
    }
    
    static List<CharSequence> getSuggestions(String[] qs)
    {
        List<CharSequence> suggestions = new java.util.ArrayList<CharSequence>();
        
        for (String fav : favs_)
        {
            String[] fs = fav.split(" ");
            
            boolean add = false;
            for (String q : qs)
            {
                add = false;
                for (String f : fs)
                {
                    if (f.startsWith(q))
                    {
                        add = true;
                        break;
                    }
                }
                if (!add)
                    break;
            }
            
            if (add)
                suggestions.add(fav);
        }
        for (String s : specialtags_)
        {
            for (String q : qs)
            {
                if (s.startsWith(q))
                {
                    suggestions.add(s);
                    break;
                }
            }
        }
        
        return suggestions;
    }
    
    void updateFav(Context context, String text, boolean add)
    {
        if (add)
            favs_.add(text);
        else
        {
            int count = 0;
            for (Set<String> thisTags : tags_.values())
            {
                if (thisTags.contains(text))
                    ++count;
            }
            if (count <= 0)
                favs_.remove(text);
            else
            {
                android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(context);
                builder
                    .setTitle("Delete favourite?")
                    .setMessage("This tag will be removed from " + count + " apps.")
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener()
                        {
                            private String text_;
                            
                            DialogInterface.OnClickListener init(String text)
                            {
                                text_ = text;
                                return this;
                            }
                            
                            @Override
                            public void onClick(DialogInterface dialog, int which)
                            {
                                for (Set<String> thisTags : tags_.values())
                                {
                                    thisTags.remove(text_);
                                }
                                favs_.remove(text_);
                            }
                        }.init(text))
                    .setNegativeButton(android.R.string.cancel, null)
                    .show();
            }
        }
        
        Utils.storeObject(favsFile_, favs_);
    }
    
    Set<String> getFavs()
    {
        return favs_;
    }
    
    static boolean findAny(CharSequence[] str, String[] text)
    {
        boolean ret = true;
        for (String t : text)
        {
            // Essentially ignore empty strings and strings beginning with '#' ie they always match
            if (!t.isEmpty() && t.charAt(0) != '#')
            {
                ret = false;
                for (CharSequence u : str)
                {
                    if (CharSequenceUtils.findIgnoreCase(u, 0, t) >= 0)
                        return true;
                }
            }
        }
        return ret; // An empty array should match all
    }
    
    private static List<App> all_= null;
    private static Set<String> favs_ = new java.util.TreeSet();
    private static Map<String, Set<String>> tags_ = new java.util.HashMap();    // map package name to tags
    
    private final LayoutInflater layoutInflater_;
    private final PackageManager pm_;
    private final java.io.File favsFile_;
    private final java.io.File tagsFile_;
    private final java.io.File infoFile_;
    
    private List<App> apps_ = new java.util.ArrayList();
    private FilterAsyncTask filterAsyncTask_ = null;

    public AppListAdapter(PackageManager pm, LayoutInflater layoutInflater, java.io.File dataDir, java.io.File cacheDir)
    {
        layoutInflater_ = layoutInflater;
        pm_ = pm;
        favsFile_ = new java.io.File(dataDir, "favourites");
        tagsFile_ = new java.io.File(dataDir, "tags");
        infoFile_ = new java.io.File(cacheDir, "info");
    }
    
    public void filter(String[] text)
    {
        if (all_ != null)
        {
            new FilterAsyncTask().execute(text);
        }
        else
        {
            apps_.clear();
            notifyDataSetChanged();
        }
    }

    @Override
    public int getCount()
    {
        return apps_.size();
    }

    @Override
    public Object getItem(int position)
    {
        return null;
    }

    @Override
    public long getItemId(int position)
    {
        //return apps_.get(position).hashCode();
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent)
    {
        final App app = apps_.get(position);
        
        View v = convertView;
        if (v == null)
            v = layoutInflater_.inflate(R.layout.item, parent, false);

        TextView labelv = (TextView) v.findViewById(R.id.label);
        if (labelv != null)
        {
            labelv.setText(app.info_.label_);
            //labelv.setText(app.ai_.name);
        }
        
        ImageView iconv = (ImageView) v.findViewById(R.id.icon);
        if (iconv != null)
        {
            setImageDrawable(iconv, app);
        }
        
        return v;
    }
    
    private static final class TagsDialogData implements DialogInterface.OnMultiChoiceClickListener
    {
        final CharSequence[] items_;
        final boolean[] selected_;
        boolean dirty_ = false;
        
        TagsDialogData(Set<String> alltags, Set<String> apptags)
        {
            items_ = new CharSequence[alltags.size()];
            alltags.toArray(items_);
            
            selected_ = new boolean[items_.length];
            if (apptags != null)
            {
                for (String s : apptags)
                {
                    int i = java.util.Arrays.binarySearch(items_, s);
                    if (i >= 0)
                        selected_[i] = true;
                }
            }
        }
        
        Set<String> getTags()
        {
            Set<String> apptags = new java.util.HashSet();
            for (int i = 0; i < items_.length; ++i)
            {
                if (selected_[i])
                    apptags.add(items_[i].toString());
            }
            if (apptags.isEmpty())
                apptags = null;
            return apptags;
        }
        
        @Override
        public void onClick(DialogInterface dialog, int which, boolean isChecked)
        {
            selected_[which] = isChecked;
            dirty_ = true;
        }
    }
    
    public boolean doAction(Context context, int action, int position)
    {
        final App app = apps_.get(position);
        try
        {
            //Utils.toast(context, "Do: " + Integer.toHexString(app.ai_.flags));
            switch (action)
            {
            case R.id.action_open:
                open(context, app.intent_);
                break;

            case R.id.action_info:
                openInfo(context, app.getPackageName());
                break;

            case R.id.action_appstore:
                openAppStore(context, app.getPackageName());
                break;

            case R.id.action_tags:
                {
                    final TagsDialogData data = new TagsDialogData(favs_, tags_.get(app.getPackageName()));
                    
                    android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(context);
                    android.app.AlertDialog dlg = builder
                        .setTitle("Tags for " + app.info_.label_)
                        .setMultiChoiceItems(data.items_, data.selected_, data)
                        //.setPositiveButton(R.string.ok, null)
                        //.setNegativeButton(R.string.cancel, null)
                        .show();
                    dlg.setOnDismissListener(new DialogInterface.OnDismissListener()
                        {
                            @Override
                            public void onDismiss(DialogInterface dialog)
                            {
                                if (data.dirty_)
                                {
                                    tags_.put(app.getPackageName(), data.getTags());
                                    
                                    Utils.storeObject(tagsFile_, tags_);
                                }
                            }
                        });
                }
                break;

            default:
                return false;
            }

            return true;
        }
        catch (android.content.ActivityNotFoundException e)
        {
            e.printStackTrace();
            Utils.toast(context, "Unable to launch activity: " + app.getPackageName());
            return false;
        }
        catch (Exception e)
        {
            e.printStackTrace();
            Utils.toast(context, "Unknown exception: " + e);
            return false;
        }
    }
    
    void open(Context context, Intent i)
    {
        if (i != null)
            context.startActivity(i);
        else
            Utils.toast(context, "No launch activity.");
    }
    
    void openInfo(Context context, String packageName)
    {
        Intent i = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        i.addCategory(Intent.CATEGORY_DEFAULT);
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        i.setData(android.net.Uri.parse("package:" + packageName));
        context.startActivity(i);
    }
    
    void openAppStore(Context context, String packageName)
    {
        final String installer = pm_.getInstallerPackageName(packageName);
        String url = null;
        if (installer == null || installer.isEmpty())
            Utils.toast(context, "No installer registered for this app.");
        else if ("com.android.vending".equals(installer))
            url = "market://details?id=" + packageName;
        else if ("com.amazon.venezia".equals(installer))
            url = "amzn://apps/android?p=" + packageName;
        else
            Utils.toast(context, "Unknown installer: " + installer);
        if (url != null)
        {
            Intent i = new Intent(Intent.ACTION_VIEW);
            i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            i.setData(android.net.Uri.parse(url));
            context.startActivity(i);
        }
    }
}
