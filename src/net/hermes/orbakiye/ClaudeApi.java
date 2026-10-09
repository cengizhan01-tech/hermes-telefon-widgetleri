package net.hermes.orbakiye;

import android.content.Context;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.Locale;

/**
 * Claude (Pusula) plan limitleri. Veri, Claude Code oturumundan alınıp telefona
 * uygulamanın kendi klasörüne (claude-limit.json) yazılır. Telefon yalnız bu dosyayı okur;
 * hiçbir Anthropic anahtarı/oturum bilgisi telefona alınmaz.
 */
public class ClaudeApi {
    static final String PREFS = "or";

    static class Veri {
        boolean var, dosyaYok;
        String hata, plan = "Pro";
        double kisaYuzde = -1, uzunYuzde = -1;
        long kisaYenilenme, uzunYenilenme, guncellendi;
        boolean ekstraAcik;
        double ekstraHarcanan = -1, ekstraLimit = -1;
        double kredi = -1;   // elle girilen kalan kullanim kredisi (claude.ai > Kullanim)
        String para = "EUR";
        long baglamKullanilan, baglamPencere;
        double baglamYuzde = -1, otomatikSikistirma = 97;
        boolean ornek;
    }

    static boolean ornekMod(Context c) {
        return c.getSharedPreferences(PREFS, 0).getBoolean("claude_ornek", false);
    }

    static void ornekAyarla(Context c, boolean v) {
        c.getSharedPreferences(PREFS, 0).edit().putBoolean("claude_ornek", v).apply();
    }

    static File dosya(Context c) {
        File d = c.getExternalFilesDir(null);
        return d == null ? null : new File(d, "claude-limit.json");
    }

    static String urlJson() {
        return Sunucu.ip() + ":3000/images/claude-limit.json";
    }

    /** Once sunucudaki (arindirilmis) JSON, olmazsa yerel dosya; ikisi de yoksa dosyaYok. */
    static Veri getir(Context c) {
        Veri v = getirIc(c);
        return v;
    }

    static String uzaktan() {
        if (!Sunucu.var()) return null;
        try {
            java.net.HttpURLConnection h = (java.net.HttpURLConnection) new java.net.URL(urlJson()).openConnection();
            h.setConnectTimeout(4000);
            h.setReadTimeout(5000);
            if (h.getResponseCode() >= 400) return null;
            BufferedReader r = new BufferedReader(new InputStreamReader(h.getInputStream(), "UTF-8"));
            StringBuilder sb = new StringBuilder();
            String s;
            while ((s = r.readLine()) != null) sb.append(s);
            r.close();
            return sb.toString();
        } catch (Exception e) {
            return null;
        }
    }

    static Veri getirIc(Context c) {
        Veri v = new Veri();
        if (ornekMod(c)) {
            long simdi = System.currentTimeMillis() / 1000;
            v.var = true;
            v.ornek = true;
            v.kisaYuzde = 66;
            v.kisaYenilenme = simdi + 2 * 3600 + 42 * 60;
            v.uzunYuzde = 32;
            v.uzunYenilenme = simdi + 3 * 86400 + 6 * 3600;
            v.ekstraAcik = true;
            v.ekstraHarcanan = 0;
            v.ekstraLimit = 30;
            v.baglamKullanilan = 838641;
            v.baglamPencere = 1000000;
            v.baglamYuzde = 84;
            v.guncellendi = simdi;
            return v;
        }
        try {
            String metin = uzaktan();
            if (metin == null) {
                File f = dosya(c);
                if (f == null || !f.exists()) {
                    v.dosyaYok = true;
                    return v;
                }
                BufferedReader r = new BufferedReader(new InputStreamReader(new FileInputStream(f), "UTF-8"));
                StringBuilder sb = new StringBuilder();
                String s;
                while ((s = r.readLine()) != null) sb.append(s);
                r.close();
                metin = sb.toString();
            }
            JSONObject j = new JSONObject(metin);
            v.plan = j.optString("plan", "Pro");
            v.guncellendi = j.optLong("guncellendi", 0);
            JSONObject k = j.optJSONObject("kisa");
            JSONObject u = j.optJSONObject("uzun");
            if (k != null) {
                v.kisaYuzde = k.optDouble("kullanilan", -1);
                v.kisaYenilenme = k.optLong("yenilenme", 0);
            }
            if (u != null) {
                v.uzunYuzde = u.optDouble("kullanilan", -1);
                v.uzunYenilenme = u.optLong("yenilenme", 0);
            }
            JSONObject e = j.optJSONObject("ekstra");
            if (e != null) {
                v.ekstraAcik = e.optBoolean("acik", false);
                v.ekstraHarcanan = e.optDouble("harcanan", -1);
                v.ekstraLimit = e.optDouble("limit", -1);
                v.para = e.optString("para", "EUR");
            }
            if (v.ekstraLimit <= 0) {
                android.content.SharedPreferences ep = c.getSharedPreferences("or", 0);
                double el = ep.getFloat("claude_ek_limit", -1f);
                if (el > 0) {
                    v.ekstraAcik = true;
                    v.ekstraLimit = el;
                    v.ekstraHarcanan = Math.max(0, ep.getFloat("claude_ek_harcanan", 0f));
                }
            }
            float kr = c.getSharedPreferences("or", 0).getFloat("claude_kredi", -1f);
            if (kr >= 0) v.kredi = kr;
            JSONObject b = j.optJSONObject("baglam");
            if (b != null) {
                v.baglamKullanilan = b.optLong("kullanilan", 0);
                v.baglamPencere = b.optLong("pencere", 0);
                v.baglamYuzde = b.optDouble("yuzde", -1);
                v.otomatikSikistirma = b.optDouble("otomatik", 97);
            }
            v.var = v.kisaYuzde >= 0 || v.uzunYuzde >= 0;
        } catch (Exception e) {
            v.hata = "Okunamadı (" + e.getMessage() + ")";
        }
        return v;
    }

    /** Verinin yaşı (sn) */
    static long yas(Veri v) {
        return v.guncellendi <= 0 ? -1 : Math.max(0, System.currentTimeMillis() / 1000 - v.guncellendi);
    }

    static String once(long guncellendi) {
        if (guncellendi <= 0) return "-";
        long sn = Math.max(0, System.currentTimeMillis() / 1000 - guncellendi);
        if (sn < 90) return sn + " sn önce";
        if (sn < 5400) return (sn / 60) + " dk önce";
        if (sn < 172800) return (sn / 3600) + " sa önce";
        return (sn / 86400) + " gün önce";
    }

    /** Pencere yenilendiyse eski yüzde artık geçersizdir */
    static boolean yenilenmis(long yenilenme) {
        return yenilenme > 0 && yenilenme <= System.currentTimeMillis() / 1000;
    }

    static String yuzde(double y) {
        return y < 0 ? "-" : String.format(Locale.US, "%%%.0f", y);
    }

    static String para(double d, String birim) {
        String sembol = "EUR".equals(birim) ? "€" : ("USD".equals(birim) ? "$" : birim + " ");
        return String.format(Locale.US, "%s%.2f", sembol, d);
    }

    static String tokenYaz(long n) {
        if (n >= 1000000) return String.format(Locale.US, "%.2fM", n / 1000000.0);
        if (n >= 1000) return (n / 1000) + "K";
        return String.valueOf(n);
    }
}
