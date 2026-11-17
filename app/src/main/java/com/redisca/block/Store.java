package com.redisca.block;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

final class Store {
    static SharedPreferences prefs(Context c) { return c.getSharedPreferences("redisca", Context.MODE_PRIVATE); }
    static Set<String> selected(Context c) { return new HashSet<>(prefs(c).getStringSet("apps", Collections.emptySet())); }
    static void selected(Context c, Set<String> apps) { prefs(c).edit().putStringSet("apps", new HashSet<>(apps)).apply(); }
    static synchronized void blocked(Context c, String name) {
        SharedPreferences p = prefs(c);
        String key = "count:" + name;
        p.edit().putInt(key, p.getInt(key, 0) + 1).apply();
    }
}
