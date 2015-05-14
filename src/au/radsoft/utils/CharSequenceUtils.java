package au.radsoft.utils;

public class CharSequenceUtils
{
    private CharSequenceUtils() { }
    
    public static int find(CharSequence cs, int o, char c)
    {
        while (o < cs.length())
        {
            if (cs.charAt(o) == c)
                return o;
            ++o;
        }
        return -1;
    }
    
    public static int findIgnoreCase(CharSequence cs, int o, char c)
    {
        c = Character.toUpperCase(c);
        while (o < cs.length())
        {
            if (Character.toUpperCase(cs.charAt(o)) == c)
                return o;
            ++o;
        }
        return -1;
    }
    
    public static int find(CharSequence cs, int o, String search)
    {
        if (search.length() == 0)
            return -1;
        while ((o = find(cs, o, search.charAt(0))) != -1)
        {
            if ((o + search.length()) > cs.length())
                return -1;
            CharSequence sub = cs.subSequence(o, o + search.length());
            if (search.contentEquals(sub))
                return o;
            ++o;
        }
        return -1;
    }
    
    public static int findIgnoreCase(CharSequence cs, int o, String search)
    {
        if (search.length() == 0)
            return -1;
        while ((o = findIgnoreCase(cs, o, search.charAt(0))) != -1)
        {
            if ((o + search.length()) > cs.length())
                return -1;
            CharSequence sub = cs.subSequence(o, o + search.length());
            if (compareIgnoreCase(search, sub) == 0)
                return o;
            ++o;
        }
        return -1;
    }
    
    public static int compare(CharSequence l, CharSequence r)
    {
        // Note: compares numerically, ignores locale
        int d = 0;
        if (l == r)
        {
            d = 0;
        }
        else if (l == null)
        {
            d = -1;
        }
        else if (r == null)
        {
            d = 1;
        }
        else
        {
            for (int i = 0; d == 0; ++i)
            {
                if (i == l.length() || i == r.length())
                {
                    d = l.length() - r.length();
                    break;
                }
                else
                    d = l.charAt(i) - r.charAt(i);
            }
        }
        return d;
    }
    
    public static int compareIgnoreCase(CharSequence l, CharSequence r)
    {
        // Note: compares numerically, ignores locale
        int d = 0;
        if (l == r)
        {
            d = 0;
        }
        else if (l == null)
        {
            d = -1;
        }
        else if (r == null)
        {
            d = 1;
        }
        else
        {
            for (int i = 0; d == 0; ++i)
            {
                if (i == l.length() || i == r.length())
                {
                    d = l.length() - r.length();
                    break;
                }
                else
                    d = Character.toUpperCase(l.charAt(i)) - Character.toUpperCase(r.charAt(i));
            }
        }
        return d;
    }
    
    public static class Comparator implements java.util.Comparator<CharSequence>
    {
        @Override
        public int compare(CharSequence o1, CharSequence o2)
        {
            return CharSequenceUtils.compare(o1, o2);
        }
    }
    
    public static class ComparatorIgnoreCase implements java.util.Comparator<CharSequence>
    {
        @Override
        public int compare(CharSequence o1, CharSequence o2)
        {
            return CharSequenceUtils.compareIgnoreCase(o1, o2);
        }
    }
}
