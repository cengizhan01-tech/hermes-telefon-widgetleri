package net.hermes.orbakiye;

import android.content.Context;

/**
 * Sunucu adresi: derlemede sabit YOK. Uygulamada "Sunucu adresi" kutusuna girilir, yalnız bu telefonda saklanır.
 * Kabul edilen girişler: 100.x.y.z, sunucum.ornek.ts.net, http://100.x.y.z, sunucu.local:3000 (port ve yol yok sayılır).
 */
class Sunucu {
    static final String PREFS = "or";
    static final String ANAHTAR = "sunucu";
    static Context ctx;   // App.onCreate doldurur

    static String ham() {
        return ctx == null ? "" : ctx.getSharedPreferences(PREFS, 0).getString(ANAHTAR, "");
    }

    /** "http://host" (port ve yol yok) ya da adres girilmemişse "" */
    static String ip() {
        String s = ham().trim();
        if (s.length() == 0) return "";
        s = s.replaceFirst("(?i)^https?://", "");
        int i = s.indexOf('/');
        if (i >= 0) s = s.substring(0, i);
        i = s.indexOf(':');
        if (i >= 0) s = s.substring(0, i);
        s = s.trim();
        return s.length() == 0 ? "" : "http://" + s;
    }

    static boolean var() {
        return ip().length() > 0;
    }

    static void kaydet(Context c, String v) {
        c.getSharedPreferences(PREFS, 0).edit().putString(ANAHTAR, v == null ? "" : v.trim()).apply();
    }
}
