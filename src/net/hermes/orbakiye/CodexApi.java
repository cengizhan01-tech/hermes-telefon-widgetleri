package net.hermes.orbakiye;

import android.content.Context;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Locale;

/**
 * ChatGPT/Codex kota verisi. Sunucu, jeton icermeyen arindirilmis JSON yayimlar:
 * {"guncellendi":epoch,"kisa":{"kullanilan":%,"yenilenme":epoch},"uzun":{...},"hata":""}
 * Telefon yalniz bu JSON'u okur; ChatGPT oturum jetonu telefona ASLA alinmaz.
 */
public class CodexApi {
    static String urlJson() {
        return Sunucu.ip() + ":3000/images/codex-kota.json";
    }
    static final String PREFS = "or";
    static Context ctx;

    static class Veri {
        boolean var;            // veri okundu mu
        boolean yayinYok;       // sunucuda JSON henuz yok
        String hata;
        double kisaYuzde = -1, uzunYuzde = -1;
        long kisaYenilenme, uzunYenilenme;   // epoch sn
        long guncellendi;                    // sunucu epoch sn
        long okunma;                         // telefon ms
        boolean ornek;
    }

    static void ayarla(Context c) {
        if (c != null) ctx = c.getApplicationContext();
    }

    static boolean ornekMod() {
        return ctx != null && ctx.getSharedPreferences(PREFS, 0).getBoolean("kota_ornek", false);
    }

    static void ornekAyarla(Context c, boolean v) {
        c.getSharedPreferences(PREFS, 0).edit().putBoolean("kota_ornek", v).apply();
    }

    static Veri getir() {
        Veri v = new Veri();
        v.okunma = System.currentTimeMillis();
        if (ornekMod()) {
            long simdi = v.okunma / 1000;
            v.var = true;
            v.ornek = true;
            v.kisaYuzde = 37;
            v.kisaYenilenme = simdi + 2 * 3600 + 13 * 60;
            v.uzunYuzde = 61;
            v.uzunYenilenme = simdi + 3 * 86400 + 5 * 3600;
            v.guncellendi = simdi;
            return v;
        }
        if (!Sunucu.var()) {
            v.hata = "Sunucu adresi girilmemiş (uygulamayı aç)";
            return v;
        }
        try {
            HttpURLConnection h = (HttpURLConnection) new URL(urlJson()).openConnection();
            h.setConnectTimeout(4000);
            h.setReadTimeout(5000);
            int kod = h.getResponseCode();
            if (kod == 404) {
                v.yayinYok = true;
                return v;
            }
            if (kod >= 400) throw new Exception("HTTP " + kod);
            BufferedReader r = new BufferedReader(new InputStreamReader(h.getInputStream(), "UTF-8"));
            StringBuilder sb = new StringBuilder();
            String s;
            while ((s = r.readLine()) != null) sb.append(s);
            r.close();
            JSONObject j = new JSONObject(sb.toString());
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
            v.guncellendi = j.optLong("guncellendi", 0);
            String hata = j.optString("hata", "");
            if (hata.length() > 0) v.hata = "sunucu: " + hata;
            v.var = v.kisaYuzde >= 0 || v.uzunYuzde >= 0;
        } catch (Exception e) {
            v.hata = "Okunamadı (" + e.getMessage() + ")";
        }
        return v;
    }

    /** Yenilenmeye kalan sure: "2 sa 13 dk" / "3 gün 5 sa" */
    static String kalan(long yenilenmeEpochSn) {
        if (yenilenmeEpochSn <= 0) return "-";
        long sn = yenilenmeEpochSn - System.currentTimeMillis() / 1000;
        if (sn <= 0) return "yenilendi";
        long g = sn / 86400, sa = (sn % 86400) / 3600, d = (sn % 3600) / 60;
        if (g > 0) return g + " gün " + sa + " sa";
        if (sa > 0) return sa + " sa " + d + " dk";
        return d + " dk";
    }

    /** Verinin tazeligi */
    static String once(long guncellendiEpochSn) {
        if (guncellendiEpochSn <= 0) return "-";
        long sn = Math.max(0, System.currentTimeMillis() / 1000 - guncellendiEpochSn);
        if (sn < 90) return sn + " sn önce";
        if (sn < 5400) return (sn / 60) + " dk önce";
        return (sn / 3600) + " sa önce";
    }

    static String yuzde(double y) {
        return y < 0 ? "-" : String.format(Locale.US, "%%%.0f", y);
    }

    /** Doluluk rengi icin: dolu % -> kalan oran */
    static boolean taze(Veri v) {
        return v.guncellendi > 0 && System.currentTimeMillis() / 1000 - v.guncellendi < 1800;
    }

    /** epoch sn -> "02.10 23:41" */
    static String zamanYaz(long epochSn) {
        if (epochSn <= 0) return "-";
        return new java.text.SimpleDateFormat("dd.MM HH:mm", new java.util.Locale("tr")).format(new java.util.Date(epochSn * 1000));
    }

    static String sureYaz(long sn) {
        if (sn < 0) return "-";
        long g = sn / 86400, sa = (sn % 86400) / 3600, d = (sn % 3600) / 60;
        if (g > 0) return g + " gün " + sa + " sa";
        if (sa > 0) return sa + " sa " + d + " dk";
        return d + " dk";
    }

    /**
     * Tempo analizi: pencerenin ne kadarı geçti, kota ne kadar harcandı, bu hızla pencere sonunda nerede olunur.
     * @param pencereSn pencere süresi (5 saat = 18000, hafta = 604800)
     */
    static String tempo(double kullanilanYuzde, long yenilenmeEpochSn, long pencereSn, String ad) {
        if (kullanilanYuzde < 0 || yenilenmeEpochSn <= 0) return "";
        long simdi = System.currentTimeMillis() / 1000;
        long bas = yenilenmeEpochSn - pencereSn;
        long gecen = Math.max(0, Math.min(pencereSn, simdi - bas));
        double gecenOran = gecen / (double) pencereSn;
        StringBuilder sb = new StringBuilder();
        sb.append(ad).append(" pencerenin %").append(Math.round(gecenOran * 100)).append("'i geçti (").append(sureYaz(gecen)).append("), kotanın %")
          .append(Math.round(kullanilanYuzde)).append("'i kullanıldı.");
        double beklenen = gecenOran * 100;
        double fark = kullanilanYuzde - beklenen;
        String tempoYazi = fark > 15 ? "tempo HIZLI (zamanın önünde harcıyorsun)" : (fark < -15 ? "tempo YAVAŞ (rahat durumdasın)" : "tempo normal");
        sb.append("\n→ ").append(tempoYazi).append(".");
        if (gecenOran > 0.04 && kullanilanYuzde > 0) {
            double sonda = Math.min(999, kullanilanYuzde / gecenOran);
            sb.append("\n→ Bu hızla pencere sonunda ≈ %").append(Math.round(sonda)).append(" olur");
            if (sonda >= 100) {
                double hizSn = kullanilanYuzde / gecen;   // % / sn
                long bitisSn = (long) ((100 - kullanilanYuzde) / hizSn);
                sb.append(" — kota yaklaşık ").append(sureYaz(bitisSn)).append(" sonra BİTER (yenilenmeden önce)");
            } else {
                sb.append(" — yenilenmeden bitmez");
            }
            sb.append(".");
        }
        return sb.toString();
    }
}
