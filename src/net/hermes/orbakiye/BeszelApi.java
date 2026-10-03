package net.hermes.orbakiye;

import android.content.Context;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Iterator;
import java.util.Locale;

/**
 * Beszel (PocketBase) salt okunur kullanici ile giris yapip sistem istatistiklerini okur ve
 * anlasilir Turkce metne cevirir. E-posta/parola yalniz uygulamanin ozel alaninda tutulur.
 */
public class BeszelApi {
    static final String PREFS = "or";
    static Context ctx;
    static String jeton = null;
    static long jetonZaman = 0;

    static class Sonuc {
        boolean girisYok, ok;
        String hata;
        String ag = "";            // "↑ 22 KB/s   ↓ 15 KB/s"
        String agArayuzler = "";   // arayuz basina satirlar
        String diskIo = "";        // "okuma 0 KB/s   yazma 129 KB/s"
        String yuk = "";           // yuk ortalamasi
        String pil = "";           // sunucu dizustu pili
        boolean pilDikkat;         // pille calisiyorsa true
        String fan = "";
        String sicakliklar = "";   // Turkce adlarla
        String smart = "";         // tek satir ozet
        String smartAyrinti = "";  // cok satirli ayrinti
        String ssdNot = "";        // "Cok saglikli" gibi yorum
        boolean ssdIyi;
        double ssdSicak = -1, ssdAsinma = -1;  // uyari motoru icin sayisal degerler
        boolean ssdGecti = true;
        String ssd1 = "", ssd2 = "", ssd3 = "";   // widget için kısa SSD satırları
    }

    static void ayarla(Context c) {
        if (c != null) ctx = c.getApplicationContext();
    }

    static String mail() {
        return ctx == null ? "" : ctx.getSharedPreferences(PREFS, 0).getString("bes_mail", "");
    }

    static String parola() {
        return ctx == null ? "" : ctx.getSharedPreferences(PREFS, 0).getString("bes_pass", "");
    }

    static void kaydet(Context c, String m, String p) {
        c.getSharedPreferences(PREFS, 0).edit().putString("bes_mail", m.trim()).putString("bes_pass", p).apply();
        jeton = null;
    }

    static void sil(Context c) {
        c.getSharedPreferences(PREFS, 0).edit().remove("bes_mail").remove("bes_pass").apply();
        jeton = null;
    }

    static String istek(String yol, String govde, String yetki) throws Exception {
        HttpURLConnection h = (HttpURLConnection) new URL(Sunucu.ip() + ":8090" + yol).openConnection();
        h.setConnectTimeout(4000);
        h.setReadTimeout(6000);
        h.setRequestProperty("Accept", "application/json");
        if (yetki != null) h.setRequestProperty("Authorization", yetki);
        if (govde != null) {
            h.setRequestMethod("POST");
            h.setDoOutput(true);
            h.setRequestProperty("Content-Type", "application/json");
            OutputStream o = h.getOutputStream();
            o.write(govde.getBytes("UTF-8"));
            o.close();
        }
        int kod = h.getResponseCode();
        if (kod >= 400) throw new Exception("HTTP " + kod);
        BufferedReader r = new BufferedReader(new InputStreamReader(h.getInputStream(), "UTF-8"));
        StringBuilder sb = new StringBuilder();
        String s;
        while ((s = r.readLine()) != null) sb.append(s);
        r.close();
        return sb.toString();
    }

    static synchronized String giris() throws Exception {
        long simdi = System.currentTimeMillis();
        if (jeton != null && simdi - jetonZaman < 600000L) return jeton;
        JSONObject b = new JSONObject();
        b.put("identity", mail());
        b.put("password", parola());
        String cevap = istek("/api/collections/users/auth-with-password", b.toString(), null);
        jeton = new JSONObject(cevap).getString("token");
        jetonZaman = simdi;
        return jeton;
    }

    /** bayt/sn -> okunur hiz */
    static String hizB(double bayt) {
        if (bayt < 1024) return String.format(Locale.US, "%.0f B/s", bayt);
        if (bayt < 1048576) return String.format(Locale.US, "%.0f KB/s", bayt / 1024);
        return String.format(Locale.US, "%.1f MB/s", bayt / 1048576);
    }

    /** bayt -> okunur boyut */
    static String boyut(double bayt) {
        if (bayt < 1048576) return String.format(Locale.US, "%.0f KB", bayt / 1024);
        if (bayt < 1073741824.0) return String.format(Locale.US, "%.0f MB", bayt / 1048576);
        if (bayt < 1099511627776.0) return String.format(Locale.US, "%.1f GB", bayt / 1073741824.0);
        return String.format(Locale.US, "%.1f TB", bayt / 1099511627776.0);
    }

    static String kisalt(String s, int n) {
        return s.length() > n ? s.substring(0, n) + "…" : s;
    }

    /** Sensor adlarini anlasilir Turkceye cevirir */
    static String sensorAdi(String ad) {
        if (ad.startsWith("coretemp_core_")) return "CPU çekirdek " + ad.substring("coretemp_core_".length());
        if (ad.startsWith("coretemp_package")) return "CPU genel";
        if (ad.startsWith("nvme")) return "SSD";
        if (ad.startsWith("acpitz")) return "anakart";
        if (ad.startsWith("pch")) return "yonga seti";
        return ad;
    }

    static String arayuzAdi(String ad) {
        if (ad.startsWith("wl")) return "Wi-Fi (" + ad + ")";
        if (ad.startsWith("tailscale")) return "Tailscale";
        if (ad.startsWith("docker") || ad.startsWith("br-") || ad.startsWith("veth")) return "Docker (" + kisalt(ad, 8) + ")";
        if (ad.startsWith("en") || ad.startsWith("eth")) return "Kablolu (" + ad + ")";
        if (ad.equals("lo")) return "yerel";
        return ad;
    }

    static double rv(JSONArray attr, String ad) {
        if (attr == null) return -1;
        for (int i = 0; i < attr.length(); i++) {
            JSONObject a = attr.optJSONObject(i);
            if (a != null && ad.equals(a.optString("n"))) return a.optDouble("rv", -1);
        }
        return -1;
    }

    static Sonuc getir() {
        Sonuc s = new Sonuc();
        if (ctx == null || mail().length() < 3 || parola().length() < 3) {
            s.girisYok = true;
            return s;
        }
        if (!Sunucu.var()) {
            s.hata = "Sunucu adresi girilmemiş (uygulamayı aç)";
            return s;
        }
        String t;
        try {
            t = giris();
        } catch (Exception e) {
            s.hata = "Beszel girişi başarısız (" + e.getMessage() + "). E-posta/parolayı kontrol et.";
            jeton = null;
            return s;
        }
        try {
            JSONObject sis = new JSONObject(istek("/api/collections/systems/records?perPage=5", null, t));
            JSONArray it = sis.optJSONArray("items");
            if (it == null || it.length() == 0) {
                s.hata = "Bu kullanıcıya sistem atanmamış (PocketBase: systems → users alanı)";
                return s;
            }
            s.ok = true;
        } catch (Exception e) {
            s.hata = "Beszel sistem sorgusu: " + e.getMessage();
            return s;
        }
        try {
            JSONObject st = new JSONObject(istek("/api/collections/system_stats/records?perPage=1&sort=-created&filter=(type%3D'1m')", null, t));
            JSONArray it = st.optJSONArray("items");
            if (it != null && it.length() > 0) {
                JSONObject x = it.getJSONObject(0).optJSONObject("stats");
                if (x != null) {
                    // genel ag (bayt/sn): b = [giden, gelen]
                    JSONArray b = x.optJSONArray("b");
                    if (b != null && b.length() >= 2) s.ag = "↑ " + hizB(b.optDouble(0)) + "   ↓ " + hizB(b.optDouble(1));
                    // arayuz basina: ni = { ad: [giden/sn, gelen/sn, toplam giden, toplam gelen] }
                    JSONObject ni = x.optJSONObject("ni");
                    if (ni != null) {
                        StringBuilder sb = new StringBuilder();
                        Iterator<String> k = ni.keys();
                        while (k.hasNext()) {
                            String ad = k.next();
                            JSONArray a = ni.optJSONArray(ad);
                            if (a == null || a.length() < 4) continue;
                            if (sb.length() > 0) sb.append("\n");
                            sb.append(arayuzAdi(ad)).append(":  ↑ ").append(hizB(a.optDouble(0))).append("  ↓ ").append(hizB(a.optDouble(1)))
                              .append("   (açılıştan beri ↑ ").append(boyut(a.optDouble(2))).append(" ↓ ").append(boyut(a.optDouble(3))).append(")");
                        }
                        s.agArayuzler = sb.toString();
                    }
                    // disk G/C (bayt/sn): dio = [okuma, yazma]
                    JSONArray dio = x.optJSONArray("dio");
                    if (dio != null && dio.length() >= 2) s.diskIo = "okuma " + hizB(dio.optDouble(0)) + "   yazma " + hizB(dio.optDouble(1));
                    else if (x.has("dw")) s.diskIo = "okuma " + hizB(x.optDouble("dr", 0) * 1048576) + "   yazma " + hizB(x.optDouble("dw", 0) * 1048576);
                    // yuk ortalamasi
                    JSONArray la = x.optJSONArray("la");
                    if (la != null && la.length() >= 3)
                        s.yuk = String.format(Locale.US, "%.2f (1 dk)  %.2f (5 dk)  %.2f (15 dk)", la.optDouble(0), la.optDouble(1), la.optDouble(2));
                    // pil: bat = [yuzde, durum]  (durum: 3 sarj, 4 pille, 5 prizde bekliyor, 2 dolu)
                    JSONArray bat = x.optJSONArray("bat");
                    if (bat != null && bat.length() >= 2) {
                        int d = bat.optInt(1);
                        String ds = d == 3 ? "şarj oluyor" : (d == 4 ? "⚠ PRİZDE DEĞİL, pille çalışıyor" : (d == 2 || d == 5 ? "prizde" : "durum bilinmiyor"));
                        s.pil = "%" + bat.optInt(0) + " · " + ds;
                        s.pilDikkat = d == 4;
                    }
                    // fan
                    JSONObject f = x.optJSONObject("f");
                    if (f != null) {
                        StringBuilder sb = new StringBuilder();
                        Iterator<String> k = f.keys();
                        while (k.hasNext()) {
                            String ad = k.next();
                            int rpm = f.optInt(ad);
                            if (sb.length() > 0) sb.append(" · ");
                            sb.append(rpm == 0 ? "fan durgun (0 devir, serin olduğu için normal)" : "fan " + rpm + " devir/dk");
                        }
                        s.fan = sb.toString();
                    }
                    // sicakliklar
                    JSONObject tm = x.optJSONObject("t");
                    if (tm != null) {
                        StringBuilder sb = new StringBuilder();
                        Iterator<String> k = tm.keys();
                        while (k.hasNext()) {
                            String ad = k.next();
                            if (sb.length() > 0) sb.append("  ·  ");
                            sb.append(sensorAdi(ad)).append(" ").append(Math.round(tm.optDouble(ad))).append("°");
                        }
                        s.sicakliklar = sb.toString();
                    }
                }
            }
        } catch (Exception e) {
            s.ag = "";
        }
        try {
            JSONObject sm = new JSONObject(istek("/api/collections/smart_devices/records?perPage=5", null, t));
            JSONArray it = sm.optJSONArray("items");
            if (it != null && it.length() > 0) {
                StringBuilder kisa = new StringBuilder();
                StringBuilder ayr = new StringBuilder();
                for (int i = 0; i < it.length(); i++) {
                    JSONObject d = it.getJSONObject(i);
                    String model = d.optString("model", d.optString("name", "disk"));
                    String durum = d.optString("state", d.optString("status", "?"));
                    boolean gecti = "PASSED".equalsIgnoreCase(durum) || "OK".equalsIgnoreCase(durum);
                    JSONArray at = d.optJSONArray("attributes");
                    double asinma = rv(at, "PercentageUsed");
                    double yedek = rv(at, "AvailableSpare");
                    double yedekEsik = rv(at, "AvailableSpareThreshold");
                    double kritik = rv(at, "CriticalWarning");
                    double yazilan = rv(at, "DataUnitsWritten");
                    double okunan = rv(at, "DataUnitsRead");
                    int sicak = d.has("temp") ? d.optInt("temp") : (int) rv(at, "Temperature");
                    int saat = d.has("hours") ? d.optInt("hours") : (int) rv(at, "PowerOnHours");
                    int cevrim = d.has("cycles") ? d.optInt("cycles") : (int) rv(at, "PowerCycles");
                    long kap = d.optLong("capacity", 0);

                    String ad = kisalt(model, 24) + (kap > 0 ? " (" + boyut(kap) + ")" : "");
                    if (i > 0) { kisa.append("\n"); ayr.append("\n\n"); }
                    kisa.append(gecti ? "SAĞLIKLI" : "⚠ " + durum).append(" · ").append(sicak).append("°").append(asinma >= 0 ? " · yıpranma %" + Math.round(asinma) : "");
                    ayr.append(ad).append("\nGenel sağlık testi: ").append(gecti ? "GEÇTİ (SAĞLIKLI)" : durum);
                    ayr.append("\nSıcaklık: ").append(sicak).append("°C");
                    if (asinma >= 0) ayr.append("\nYıpranma: %").append(Math.round(asinma)).append(" (100'e yaklaşırsa disk ömrünü doldurmuş demektir)");
                    if (yedek >= 0) ayr.append("\nYedek hücre: %").append(Math.round(yedek)).append(yedekEsik >= 0 ? " (alt sınır %" + Math.round(yedekEsik) + ")" : "");
                    if (yazilan >= 0) ayr.append("\nToplam yazılan veri: ").append(boyut(yazilan * 512000.0));
                    if (okunan >= 0) ayr.append("\nToplam okunan veri: ").append(boyut(okunan * 512000.0));
                    if (saat > 0) ayr.append("\nÇalışma süresi: ").append(saat).append(" saat (yaklaşık ").append(saat / 24).append(" gün)");
                    if (cevrim > 0) ayr.append("\nAçılıp kapanma: ").append(cevrim).append(" kez");
                    if (kritik >= 0) ayr.append("\nKritik uyarı: ").append(kritik == 0 ? "yok" : "VAR (" + Math.round(kritik) + ")");
                    boolean iyi = gecti && (asinma < 0 || asinma < 50) && (yedek < 0 || yedekEsik < 0 || yedek > yedekEsik + 20) && kritik <= 0 && sicak < 65;
                    if (i == 0) {
                        s.ssdIyi = iyi;
                        s.ssdSicak = sicak;
                        s.ssdAsinma = asinma;
                        s.ssdGecti = gecti;
                        s.ssd1 = ad + ": " + (gecti ? "SAĞLIKLI" : "⚠ " + durum) + " · " + sicak + "°";
                        s.ssd2 = (asinma >= 0 ? "Yıpranma %" + Math.round(asinma) : "") + (yedek >= 0 ? " · yedek hücre %" + Math.round(yedek) : "")
                            + (yazilan >= 0 ? " · yazılan " + boyut(yazilan * 512000.0) : "") + (okunan >= 0 ? " · okunan " + boyut(okunan * 512000.0) : "");
                        s.ssd3 = (saat > 0 ? saat + " sa (" + (saat / 24) + " gün) çalışmış" : "") + (cevrim > 0 ? " · " + cevrim + " açılış" : "")
                            + (kritik >= 0 ? " · kritik uyarı " + (kritik == 0 ? "yok" : "VAR") : "");
                        s.ssdNot = iyi ? "Disk çok sağlıklı: testten geçti, yıpranma düşük, yedek hücre dolu, sıcaklık normal."
                            : "⚠ Diske dikkat: yıpranma, yedek hücre, sıcaklık ya da kritik uyarıyı kontrol et.";
                    }
                }
                s.smart = kisa.toString();
                s.smartAyrinti = ayr.toString();
            } else {
                s.smart = "SMART kaydı yok";
            }
        } catch (Exception e) {
            s.smart = "SMART okunamadı (" + e.getMessage() + ")";
        }
        return s;
    }
}
