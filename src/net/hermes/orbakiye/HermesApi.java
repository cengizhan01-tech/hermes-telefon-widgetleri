package net.hermes.orbakiye;

import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * Hermes sunucusunun kimlik gerektirmeyen salt okunur uc noktalari:
 * Homepage (:3000) kaynak API'leri, Hermes dashboard (:9119) /api/status + /api/model/info
 * ve her servisin kendi portuna dogrudan saglik sorgusu. Hicbir sifre/anahtar kullanilmaz.
 */
public class HermesApi {
    static String ip() { return Sunucu.ip(); }
    static String hp() { return ip() + ":3000"; }
    static String hd() { return ip() + ":9119"; }
    static android.content.Context ctx;

    static final String[][] SERVISLER = new String[][] {
        { "Hermes Dashboard", "9119", "Dashboard" },
        { "Uptime Kuma", "3001", "Kuma" },
        { "Beszel", "8090", "Beszel" },
        { "Backrest", "9898", "Backrest" },
        { "changedetection.io", "5000", "Change" },
        { "Paperless-ngx", "8000", "Paperless" },
        { "Activepieces", "8080", "Pieces" },
        { "Stirling-PDF", "8081", "Stirling" },
        { "Homepage", "3000", "Homepage" }
    };

    static class Svc {
        String ad, kisa;
        int kod;
        double ms;
        boolean ok;
    }

    static class Veri {
        boolean ulasildi;
        String hata;
        double cpu, load;
        long ramToplam, ramKullanilan, ramMusait, ramOnbellek, ramTampon, swapToplam, swapKullanilan;
        long diskToplam, diskKullanilan, diskBos;
        double diskYuzde;
        double acikSn;
        double sicak = -1, sicakMax = -1, sicakChipset = -1;
        String cekirdekler = "";
        String surum = "-", genel = "-", gwDurum = "-", gwMod = "-", model = "-", saglayici = "-";
        int oturum, ajan, gwRssMb;
        long ctx;
        String yetenek = "", modelAile = "";
        long maxCikti;
        boolean gwMesgul, temizAcilis = true, oomSuphesi;
        String profiller = "-";
        String bilesenler = "";
        String gwGuncel = "";
        List<Svc> servisler = new ArrayList<Svc>();
        long zaman;
        BeszelApi.Sonuc bes;
    }

    static String get(String url, int ms) throws Exception {
        HttpURLConnection h = (HttpURLConnection) new URL(url).openConnection();
        h.setConnectTimeout(ms);
        h.setReadTimeout(ms);
        h.setRequestProperty("Accept", "application/json");
        int kod = h.getResponseCode();
        if (kod >= 400) throw new Exception("HTTP " + kod);
        BufferedReader r = new BufferedReader(new InputStreamReader(h.getInputStream(), "UTF-8"));
        StringBuilder sb = new StringBuilder();
        String s;
        while ((s = r.readLine()) != null) sb.append(s);
        r.close();
        return sb.toString();
    }

    static Svc sorgula(String ad, String port, String kisa) {
        Svc s = new Svc();
        s.ad = ad;
        s.kisa = kisa;
        long t0 = System.nanoTime();
        try {
            HttpURLConnection h = (HttpURLConnection) new URL(ip() + ":" + port + "/").openConnection();
            h.setConnectTimeout(3500);
            h.setReadTimeout(3500);
            h.setInstanceFollowRedirects(false);
            s.kod = h.getResponseCode();
            s.ms = (System.nanoTime() - t0) / 1e6;
            // 2xx, 3xx (giris sayfasina yonlendirme) ve 401/403 = servis ayakta
            s.ok = (s.kod >= 200 && s.kod < 400) || s.kod == 401 || s.kod == 403;
            h.disconnect();
        } catch (Exception e) {
            s.ok = false;
            s.kod = 0;
        }
        return s;
    }

    static Veri getir() {
        final Veri v = new Veri();
        v.zaman = System.currentTimeMillis();
        if (!Sunucu.var()) {
            v.hata = "Sunucu adresi girilmemiş (uygulamayı aç)";
            return v;
        }
        try {
            JSONObject cpu = new JSONObject(get(hp() + "/api/widgets/resources?type=cpu", 4000)).getJSONObject("cpu");
            v.cpu = cpu.optDouble("usage", 0);
            v.load = cpu.optDouble("load", 0);
            v.ulasildi = true;
        } catch (Exception e) {
            v.hata = "Sunucuya ulaşılamadı (Tailscale açık mı?)";
            return v;
        }
        try {
            JSONObject m = new JSONObject(get(hp() + "/api/widgets/resources?type=memory", 4000)).getJSONObject("memory");
            v.ramToplam = m.optLong("total");
            v.ramMusait = m.optLong("available");
            v.ramKullanilan = v.ramToplam - v.ramMusait;
            v.ramOnbellek = m.optLong("cached");
            v.ramTampon = m.optLong("buffers");
            v.swapToplam = m.optLong("swaptotal");
            v.swapKullanilan = m.optLong("swapused");
        } catch (Exception e) { }
        try {
            JSONObject d = new JSONObject(get(hp() + "/api/widgets/resources?type=disk&target=/", 4000)).getJSONObject("drive");
            v.diskToplam = d.optLong("size");
            v.diskKullanilan = d.optLong("used");
            v.diskBos = d.optLong("available");
            v.diskYuzde = d.optDouble("use", 0);
        } catch (Exception e) { }
        try {
            v.acikSn = new JSONObject(get(hp() + "/api/widgets/resources?type=uptime", 4000)).optDouble("uptime", 0);
        } catch (Exception e) { }
        try {
            JSONObject t = new JSONObject(get(hp() + "/api/widgets/resources?type=cputemp", 4000)).getJSONObject("cputemp");
            v.sicak = t.optDouble("main", -1);
            v.sicakMax = t.optDouble("max", -1);
            v.sicakChipset = t.optDouble("chipset", -1);
            JSONArray c = t.optJSONArray("cores");
            if (c != null) {
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < c.length(); i++) {
                    if (i > 0) sb.append(" · ");
                    sb.append(Math.round(c.optDouble(i))).append("°");
                }
                v.cekirdekler = sb.toString();
            }
        } catch (Exception e) { }
        try {
            JSONObject s = new JSONObject(get(hd() + "/api/status", 4000));
            v.surum = s.optString("version", "-");
            v.genel = s.optString("overall", "-");
            v.gwDurum = s.optString("gateway_state", "-");
            v.gwMod = s.optString("gateway_mode", "-");
            v.oturum = s.optInt("active_sessions", 0);
            v.ajan = s.optInt("active_agents", 0);
            v.gwMesgul = s.optBoolean("gateway_busy", false);
            v.gwGuncel = s.optString("gateway_updated_at", "");
            JSONObject mem = s.optJSONObject("memory");
            if (mem != null) {
                v.gwRssMb = mem.optInt("gateway_rss_mb", 0);
                v.temizAcilis = !mem.optBoolean("last_boot_unclean", false);
                v.oomSuphesi = mem.optBoolean("last_boot_suspected_oom", false);
            }
            JSONArray pr = s.optJSONArray("profiles");
            if (pr != null) {
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < pr.length(); i++) {
                    if (i > 0) sb.append(", ");
                    sb.append(pr.optString(i));
                }
                v.profiller = sb.toString();
            }
            JSONObject comp = s.optJSONObject("components");
            if (comp != null) {
                StringBuilder sb = new StringBuilder();
                java.util.Iterator<String> it = comp.keys();
                while (it.hasNext()) {
                    String k = it.next();
                    JSONObject o = comp.optJSONObject(k);
                    if (o != null) sb.append(k).append(": ").append(o.optString("status", "?")).append("\n");
                }
                v.bilesenler = sb.toString().trim();
            }
        } catch (Exception e) { }
        try {
            JSONObject mi = new JSONObject(get(hd() + "/api/model/info", 4000));
            String mad = mi.optString("model", "-");
            int ix = mad.lastIndexOf('/');
            v.model = ix >= 0 ? mad.substring(ix + 1) : mad;
            v.saglayici = mi.optString("provider", "-");
            v.ctx = mi.optLong("effective_context_length", 0);
            JSONObject cap = mi.optJSONObject("capabilities");
            if (cap != null) {
                StringBuilder y = new StringBuilder();
                if (cap.optBoolean("supports_tools")) y.append("araç kullanımı");
                if (cap.optBoolean("supports_vision")) y.append(y.length() > 0 ? " · " : "").append("görsel");
                if (cap.optBoolean("supports_reasoning")) y.append(y.length() > 0 ? " · " : "").append("akıl yürütme");
                v.yetenek = y.toString();
                v.modelAile = cap.optString("model_family", "");
                v.maxCikti = cap.optLong("max_output_tokens", 0);
            }
        } catch (Exception e) { }
        try {
            ExecutorService ex = Executors.newFixedThreadPool(10);
            List<Future<Svc>> isler = new ArrayList<Future<Svc>>();
            for (final String[] s : SERVISLER) {
                isler.add(ex.submit(new Callable<Svc>() {
                    public Svc call() {
                        return sorgula(s[0], s[1], s[2]);
                    }
                }));
            }
            for (Future<Svc> f : isler) {
                try { v.servisler.add(f.get(6, TimeUnit.SECONDS)); } catch (Exception e) { }
            }
            ex.shutdownNow();
        } catch (Exception e) { }
        BeszelApi.ayarla(ctx);
        v.bes = BeszelApi.getir();
        return v;
    }

    static String gb(long bayt) {
        return String.format(java.util.Locale.US, "%.1f GB", bayt / 1073741824.0);
    }

    static String sure(double sn) {
        long s = (long) sn;
        long g = s / 86400, sa = (s % 86400) / 3600, d = (s % 3600) / 60;
        if (g > 0) return g + " gün " + sa + " sa";
        if (sa > 0) return sa + " sa " + d + " dk";
        return d + " dk";
    }

    static String acilis(double acikSn) {
        long t = System.currentTimeMillis() - (long) (acikSn * 1000);
        return new java.text.SimpleDateFormat("dd.MM HH:mm", new java.util.Locale("tr")).format(new java.util.Date(t));
    }

    /** gateway_updated_at (ISO, UTC) -> "x sn önce" */
    static String once(String iso) {
        try {
            if (iso == null || iso.length() < 19) return "-";
            java.text.SimpleDateFormat f = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.US);
            f.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
            long t = f.parse(iso.substring(0, 19)).getTime();
            long sn = Math.max(0, (System.currentTimeMillis() - t) / 1000);
            if (sn < 90) return sn + " sn önce";
            if (sn < 5400) return (sn / 60) + " dk önce";
            return (sn / 3600) + " sa önce";
        } catch (Exception e) {
            return "-";
        }
    }

    /** Aktif modeli dashboard'dan çekip OpenRouter widget'ı için önbelleğe yazar (60 sn önbellekli). */
    static void modelCek(android.content.Context c) {
        android.content.SharedPreferences p = c.getSharedPreferences("or", 0);
        if (System.currentTimeMillis() - p.getLong("m_zaman", 0) < 60000L) return;
        android.content.SharedPreferences.Editor e = p.edit();
        try {
            JSONObject mi = new JSONObject(get(hd() + "/api/model/info", 4000));
            String tam = mi.optString("model", "-");
            e.putString("m_tam", tam);
            e.putString("m_sag", mi.optString("provider", "-"));
            e.putLong("m_ctx", mi.optLong("effective_context_length", 0));
            JSONObject cap = mi.optJSONObject("capabilities");
            if (cap != null) {
                StringBuilder y = new StringBuilder();
                if (cap.optBoolean("supports_tools")) y.append("araç kullanımı");
                if (cap.optBoolean("supports_vision")) y.append(y.length() > 0 ? " · " : "").append("görsel");
                if (cap.optBoolean("supports_reasoning")) y.append(y.length() > 0 ? " · " : "").append("akıl yürütme");
                e.putString("m_yet", y.toString());
                e.putString("m_aile", cap.optString("model_family", ""));
                e.putLong("m_cikti", cap.optLong("max_output_tokens", 0));
            }
            e.putBoolean("m_ok", true);
        } catch (Exception x) {
            e.putBoolean("m_ok", false);
        }
        e.putLong("m_zaman", System.currentTimeMillis());
        e.apply();
    }

    /** "1048576" -> "1M token", "131072" -> "131K token" */
    static String tokenYaz(long n) {
        if (n <= 0) return "-";
        if (n >= 1000000) return String.format(java.util.Locale.US, "%.1fM token", n / 1048576.0);
        return (n / 1000) + "K token";
    }
}
