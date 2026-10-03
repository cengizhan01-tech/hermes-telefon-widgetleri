package net.hermes.orbakiye;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * OpenRouter bakiye/limit/harcama sorgusu. Anahtar yalnız uygulamanın özel alanında (SharedPreferences) tutulur.
 * Ayrıntılar (harcama hızı, geçmiş, model bilgisi) özetlenip önbelleğe yazılır; widget oradan okur.
 */
public class Api {
    static final String PREFS = "or";
    static Context ctx;

    static class Sonuc {
        String hata;
        Double toplamKredi, toplamKullanim;
        Double anahtarKullanim, anahtarLimit, anahtarKalan;
        Double gunluk, haftalik, aylik, byok;
        boolean ucretsiz, yonetimAnahtari, saglayiciAnahtar, byokLimitte;
        String etiket = "", limitSifirlama = "", bitis = "", hizLimit = "";
        Double kalanDeger;   // büyük bakiye için
    }

    static String getAnahtar(Context c) {
        ctx = c.getApplicationContext();
        return c.getSharedPreferences(PREFS, 0).getString("key", "");
    }

    static void setAnahtar(Context c, String k) {
        c.getSharedPreferences(PREFS, 0).edit().putString("key", k.trim()).apply();
    }

    static String http(String url, String key) throws Exception {
        HttpURLConnection h = (HttpURLConnection) new URL(url).openConnection();
        h.setConnectTimeout(10000);
        h.setReadTimeout(15000);
        h.setRequestProperty("Authorization", "Bearer " + key);
        h.setRequestProperty("Accept", "application/json");
        int kod = h.getResponseCode();
        BufferedReader r = new BufferedReader(new InputStreamReader(kod >= 400 ? h.getErrorStream() : h.getInputStream(), "UTF-8"));
        StringBuilder sb = new StringBuilder();
        String s;
        while ((s = r.readLine()) != null) sb.append(s);
        r.close();
        if (kod >= 400) throw new Exception("HTTP " + kod);
        return sb.toString();
    }

    static Double d(JSONObject o, String ad) {
        if (o == null || o.isNull(ad) || !o.has(ad)) return null;
        return o.optDouble(ad);
    }

    static Sonuc getir(String key) {
        Sonuc s = new Sonuc();
        if (key == null || key.length() < 10) {
            s.hata = "Anahtar yok";
            return s;
        }
        try {
            JSONObject k = new JSONObject(http("https://openrouter.ai/api/v1/key", key)).optJSONObject("data");
            s.etiket = k.optString("label", "");
            s.anahtarKullanim = d(k, "usage");
            s.anahtarLimit = d(k, "limit");
            s.anahtarKalan = d(k, "limit_remaining");
            s.gunluk = d(k, "usage_daily");
            s.haftalik = d(k, "usage_weekly");
            s.aylik = d(k, "usage_monthly");
            s.byok = d(k, "byok_usage");
            s.ucretsiz = k.optBoolean("is_free_tier", false);
            s.yonetimAnahtari = k.optBoolean("is_management_key", false) || k.optBoolean("is_provisioning_key", false);
            s.byokLimitte = k.optBoolean("include_byok_in_limit", false);
            String sif = k.optString("limit_reset", "");
            s.limitSifirlama = sif == null || "null".equals(sif) ? "" : sif;
            String bit = k.optString("expires_at", "");
            s.bitis = bit == null || "null".equals(bit) ? "" : bit;
            JSONObject rl = k.optJSONObject("rate_limit");
            if (rl != null) s.hizLimit = rl.optInt("requests", 0) + " istek / " + rl.optString("interval", "?");
        } catch (Exception e) {
            s.hata = "Anahtar sorgusu: " + e.getMessage();
        }
        try {
            JSONObject c = new JSONObject(http("https://openrouter.ai/api/v1/credits", key)).optJSONObject("data");
            s.toplamKredi = d(c, "total_credits");
            s.toplamKullanim = d(c, "total_usage");
        } catch (Exception e) {
            // yönetim anahtarı gerekebilir: sessizce geç
        }
        if (s.toplamKredi != null && s.toplamKullanim != null) s.kalanDeger = s.toplamKredi - s.toplamKullanim;
        else if (s.anahtarKalan != null) s.kalanDeger = s.anahtarKalan;
        if (s.toplamKredi == null && s.anahtarKullanim == null && s.hata == null) s.hata = "Veri alınamadı";
        return s;
    }

    static String para(Double v) {
        if (v == null) return "-";
        if (Math.abs(v) < 10) return String.format(Locale.US, "$%.2f", v);
        return String.format(Locale.US, "$%.1f", v);
    }

    static boolean veriVar(Sonuc s) {
        return s.toplamKredi != null || s.anahtarKullanim != null;
    }

    static String ana(Sonuc s) {
        if (!veriVar(s)) return "—";
        if (s.kalanDeger != null) return para(s.kalanDeger);
        return para(s.anahtarKullanim);
    }

    static String etiket(Sonuc s) {
        if (!veriVar(s)) return "";
        if (s.toplamKredi != null && s.toplamKullanim != null) return "kalan kredi";
        if (s.anahtarKalan != null) return "limitten kalan";
        return "toplam kullanım";
    }

    static float oran(Sonuc s) {
        if (s.toplamKredi != null && s.toplamKullanim != null && s.toplamKredi > 0)
            return (float) Math.max(0, Math.min(1, (s.toplamKredi - s.toplamKullanim) / s.toplamKredi));
        if (s.anahtarLimit != null && s.anahtarKalan != null && s.anahtarLimit > 0)
            return (float) Math.max(0, Math.min(1, s.anahtarKalan / s.anahtarLimit));
        return -1f;
    }

    static String saat() {
        return new SimpleDateFormat("HH:mm", new Locale("tr")).format(new Date());
    }

    // ---------------------------------------------------------------- geçmiş / hız
    static List<double[]> gecmis(SharedPreferences p) {
        List<double[]> l = new ArrayList<double[]>();
        String h = p.getString("hist", "");
        if (h.length() == 0) return l;
        for (String e : h.split(";")) {
            String[] a = e.split(":");
            if (a.length != 2) continue;
            try {
                l.add(new double[] { Double.parseDouble(a[0]), Double.parseDouble(a[1]) });
            } catch (Exception x) { }
        }
        return l;
    }

    static void gecmisEkle(SharedPreferences p, double usage) {
        List<double[]> l = gecmis(p);
        long dk = System.currentTimeMillis() / 60000;
        if (!l.isEmpty() && dk - (long) l.get(l.size() - 1)[0] < 4) return;
        l.add(new double[] { dk, usage });
        StringBuilder sb = new StringBuilder();
        for (double[] e : l) {
            if (dk - e[0] > 26 * 60) continue;
            if (sb.length() > 0) sb.append(";");
            sb.append((long) e[0]).append(":").append(String.format(Locale.US, "%.4f", e[1]));
        }
        p.edit().putString("hist", sb.toString()).apply();
    }

    /** $/saat: son ~60 dakikanın harcama hızı (en az 10 dk veri gerekir), yoksa -1 */
    static double hizSaat(SharedPreferences p, double usageNow) {
        List<double[]> l = gecmis(p);
        long dk = System.currentTimeMillis() / 60000;
        double[] sec = null;
        for (double[] e : l) {
            double yas = dk - e[0];
            if (yas >= 10 && yas <= 75) {
                if (sec == null || Math.abs(yas - 60) < Math.abs((dk - sec[0]) - 60)) sec = e;
            }
        }
        if (sec == null) return -1;
        double saatFark = (dk - sec[0]) / 60.0;
        return Math.max(0, (usageNow - sec[1]) / saatFark);
    }

    /** Kümülatif kullanımın verilen dakikadaki değeri (en yakın önceki örnek), yoksa NaN */
    static double kumulatifDk(List<double[]> l, long dk) {
        double[] en = null;
        for (double[] e : l) if (e[0] <= dk && (en == null || e[0] > en[0])) en = e;
        return (en == null || dk - en[0] > 30) ? Double.NaN : en[1];
    }

    /** Son 12 saatin saatlik harcama çizgisi (▁▂▃▄▅▆▇█) */
    static String cizgi(SharedPreferences p) {
        List<double[]> l = gecmis(p);
        if (l.size() < 3) return "";
        long dk = System.currentTimeMillis() / 60000;
        double[] saatlik = new double[12];
        double maks = 0;
        int dolu = 0;
        for (int i = 0; i < 12; i++) {
            long bit = dk - 60L * (11 - i);
            long bas = bit - 60;
            double ub = kumulatifDk(l, bit), ua = kumulatifDk(l, bas);
            if (Double.isNaN(ub) || Double.isNaN(ua)) { saatlik[i] = -1; continue; }
            saatlik[i] = Math.max(0, ub - ua);
            maks = Math.max(maks, saatlik[i]);
            dolu++;
        }
        if (dolu < 2) return "";
        String blok = "▁▂▃▄▅▆▇█";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 12; i++) {
            if (saatlik[i] < 0) { sb.append("·"); continue; }
            int ix = maks <= 0 ? 0 : (int) Math.round((saatlik[i] / maks) * 7);
            sb.append(blok.charAt(Math.max(0, Math.min(7, ix))));
        }
        return sb.toString() + String.format(Locale.US, "   (en yoğun saat %s)", para(maks));
    }

    static String sureYaz(double saat) {
        if (saat < 0) return "-";
        long dk = Math.round(saat * 60);
        if (dk < 60) return dk + " dk";
        if (dk < 48 * 60) return (dk / 60) + " sa " + (dk % 60) + " dk";
        return (dk / 1440) + " gün";
    }

    // ---------------------------------------------------------------- aktivite (yönetim anahtarı gerekir)
    static String aktiviteOzet(Context c, String key) {
        SharedPreferences p = c.getSharedPreferences(PREFS, 0);
        long son = p.getLong("act_zaman", 0);
        if (System.currentTimeMillis() - son < 600000L) return p.getString("act_txt", "");
        String sonuc;
        try {
            JSONObject o = new JSONObject(http("https://openrouter.ai/api/v1/activity", key));
            JSONArray data = o.optJSONArray("data");
            Map<String, double[]> m = new HashMap<String, double[]>();
            if (data != null) {
                for (int i = 0; i < data.length(); i++) {
                    JSONObject x = data.optJSONObject(i);
                    if (x == null) continue;
                    String ad = x.optString("model", x.optString("model_permaslug", "?"));
                    int ix = ad.lastIndexOf('/');
                    if (ix >= 0) ad = ad.substring(ix + 1);
                    double[] a = m.get(ad);
                    if (a == null) { a = new double[2]; m.put(ad, a); }
                    a[0] += x.optDouble("usage", 0);
                    a[1] += x.optDouble("requests", 0);
                }
            }
            List<Map.Entry<String, double[]>> liste = new ArrayList<Map.Entry<String, double[]>>(m.entrySet());
            java.util.Collections.sort(liste, new java.util.Comparator<Map.Entry<String, double[]>>() {
                public int compare(Map.Entry<String, double[]> a, Map.Entry<String, double[]> b) {
                    return Double.compare(b.getValue()[0], a.getValue()[0]);
                }
            });
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < Math.min(4, liste.size()); i++) {
                if (i > 0) sb.append("\n");
                sb.append("• ").append(liste.get(i).getKey()).append("  ").append(para(liste.get(i).getValue()[0]))
                  .append("  (").append((long) liste.get(i).getValue()[1]).append(" istek)");
            }
            sonuc = sb.toString();
        } catch (Exception e) {
            sonuc = "";   // normal anahtar: etkinlik dökümü yok
        }
        p.edit().putString("act_txt", sonuc).putLong("act_zaman", System.currentTimeMillis()).apply();
        return sonuc;
    }

    // ---------------------------------------------------------------- önbelleğe yaz
    static void onbellegeYaz(Context c, Sonuc s) {
        SharedPreferences p = c.getSharedPreferences(PREFS, 0);
        String key = getAnahtar(c);
        double kullanim = s.anahtarKullanim != null ? s.anahtarKullanim : (s.toplamKullanim != null ? s.toplamKullanim : -1);
        if (kullanim >= 0) gecmisEkle(p, kullanim);
        double hiz = kullanim >= 0 ? hizSaat(p, kullanim) : -1;
        HermesApi.modelCek(c);   // aktif model bilgisini tazele (önbellekli)
        String act = aktiviteOzet(c, key);

        SharedPreferences.Editor e = p.edit();
        e.putString("ana", ana(s));
        e.putString("etiket", etiket(s));
        e.putFloat("oran", oran(s));
        e.putString("gun", para(s.gunluk));
        e.putString("hafta", para(s.haftalik));
        e.putString("ay", para(s.aylik));
        e.putString("limit", s.anahtarLimit != null ? "limit " + para(s.anahtarLimit) : (s.ucretsiz ? "ücretsiz katman" : ""));
        e.putString("zaman", saat());
        e.putFloat("kalan", s.kalanDeger != null ? s.kalanDeger.floatValue() : -1f);
        e.putFloat("hiz", (float) hiz);

        // harcama hızı bölümü
        StringBuilder hz = new StringBuilder();
        if (hiz >= 0) {
            hz.append("Son 1 saatte: ").append(para(hiz)).append(" / saat");
            if (s.kalanDeger != null && hiz > 0.02) {
                double saatKaldi = s.kalanDeger / hiz;
                hz.append("\nBu hızla kalan kredi ").append(sureYaz(saatKaldi)).append(" sonra biter");
                if (s.anahtarKalan != null && s.anahtarKalan < s.kalanDeger) hz.append(" (anahtar limiti ").append(sureYaz(s.anahtarKalan / hiz)).append(" sonra)");
            } else if (hiz <= 0.02) {
                hz.append("\nHarcama durgun: kredi şu an erimiyor");
            }
        } else {
            hz.append("Harcama hızı için veri birikiyor (10 dk sonra görünür)");
        }
        if (s.gunluk != null) {
            java.util.Calendar k = java.util.Calendar.getInstance();
            double gecen = k.get(java.util.Calendar.HOUR_OF_DAY) + k.get(java.util.Calendar.MINUTE) / 60.0;
            if (gecen >= 1) hz.append("\nBugünkü ortalama: ").append(para(s.gunluk / gecen)).append(" / saat (günün ").append((int) gecen).append(". saati)");
        }
        e.putString("hiz_txt", hz.toString());
        e.putString("cizgi", cizgi(p));

        // anahtar bölümü
        StringBuilder an = new StringBuilder();
        if (s.etiket.length() > 0) an.append("Anahtar: ").append(s.etiket.length() > 22 ? s.etiket.substring(0, 22) + "…" : s.etiket).append("\n");
        if (s.anahtarLimit != null) {
            an.append("Anahtar limiti: ").append(para(s.anahtarLimit));
            if (s.anahtarKalan != null) an.append("  ·  kalan ").append(para(s.anahtarKalan));
            an.append("\n");
        }
        if (s.limitSifirlama.length() > 0) an.append("Limit sıfırlanma: ").append(s.limitSifirlama).append("\n");
        an.append("Tür: ").append(s.ucretsiz ? "ücretsiz katman" : "ücretli").append(s.yonetimAnahtari ? " · yönetim anahtarı" : " · normal anahtar");
        if (s.hizLimit.length() > 0) an.append("\nİstek sınırı: ").append(s.hizLimit);
        if (s.bitis.length() > 0) an.append("\nAnahtar bitişi: ").append(s.bitis);
        e.putString("anahtar_txt", an.toString());

        // hesap bölümü
        StringBuilder hs = new StringBuilder();
        if (s.toplamKredi != null) hs.append("Toplam yüklenen kredi: ").append(para(s.toplamKredi)).append("\n");
        if (s.toplamKullanim != null) hs.append("Toplam harcanan: ").append(para(s.toplamKullanim)).append("\n");
        if (s.anahtarKullanim != null) hs.append("Bu anahtarın toplam harcaması: ").append(para(s.anahtarKullanim)).append("\n");
        if (s.byok != null && s.byok > 0) hs.append("Kendi anahtarınla (BYOK) harcama: ").append(para(s.byok)).append("\n");
        e.putString("hesap_txt", hs.toString().trim());
        e.putString("act_goster", act);
        e.apply();
    }
}
