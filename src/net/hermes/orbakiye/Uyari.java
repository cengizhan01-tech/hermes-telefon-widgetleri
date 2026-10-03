package net.hermes.orbakiye;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;

import java.util.Calendar;
import java.util.Locale;

/**
 * Sunucu uyari motoru. Yanlis alarmi onlemek icin sorun ardisik N kontrolde surerse bildirim atar,
 * ayni konuyu en fazla saatte bir tekrarlar, duzelince "Duzeldi" bildirimi gonderir.
 * Sessiz saatlerde (22:00-03:45) hic sorgu/bildirim yapilmaz.
 */
public class Uyari {
    static final String PREFS = "uyari";
    static final String KANAL_UYARI = "or_uyari";
    static final String KANAL_BILGI = "or_bilgi";
    static final long TEKRAR_MS = 3600000L;

    static final int SESSIZ_BAS = 22 * 60;        // 22:00
    static final int SESSIZ_BIT = 3 * 60 + 45;    // 03:45

    // esikler
    static final double SSD_SICAK = 60;
    static final double CPU_SICAK = 80;
    static final double DISK_YUZDE = 90;
    static final double RAM_YUZDE = 92;
    static final double SSD_ASINMA = 80;

    static SharedPreferences p(Context c) {
        return c.getSharedPreferences(PREFS, 0);
    }

    static boolean acik(Context c) {
        return p(c).getBoolean("acik", true);
    }

    static void acikAyarla(Context c, boolean v) {
        p(c).edit().putBoolean("acik", v).apply();
    }

    static boolean sessizSaatAcik(Context c) {
        return p(c).getBoolean("sessiz", true);
    }

    static void sessizAyarla(Context c, boolean v) {
        p(c).edit().putBoolean("sessiz", v).apply();
    }

    /** Su an sessiz saat mi (22:00-03:45)? */
    static boolean sessizMi(Context c) {
        if (!sessizSaatAcik(c)) return false;
        Calendar k = Calendar.getInstance();
        int dk = k.get(Calendar.HOUR_OF_DAY) * 60 + k.get(Calendar.MINUTE);
        return dk >= SESSIZ_BAS || dk < SESSIZ_BIT;
    }

    static void kanallar(Context c) {
        if (Build.VERSION.SDK_INT < 26) return;
        NotificationManager nm = (NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm == null) return;
        NotificationChannel u = new NotificationChannel(KANAL_UYARI, "Sunucu uyarıları", NotificationManager.IMPORTANCE_HIGH);
        u.setDescription("Servis kapandı, SSD sıcak, elektrik kesildi gibi önemli uyarılar");
        nm.createNotificationChannel(u);
        NotificationChannel b = new NotificationChannel(KANAL_BILGI, "Sunucu: düzeldi bilgileri", NotificationManager.IMPORTANCE_LOW);
        nm.createNotificationChannel(b);
    }

    static void bildir(Context c, int id, String baslik, String metin, boolean uyari) {
        kanallar(c);
        NotificationManager nm = (NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm == null) return;
        Notification.Builder b = Build.VERSION.SDK_INT >= 26
            ? new Notification.Builder(c, uyari ? KANAL_UYARI : KANAL_BILGI)
            : new Notification.Builder(c);
        b.setContentTitle(baslik)
         .setContentText(metin)
         .setStyle(new Notification.BigTextStyle().bigText(metin))
         .setSmallIcon(uyari ? android.R.drawable.stat_sys_warning : android.R.drawable.stat_notify_sync_noanim)
         .setAutoCancel(true)
         .setContentIntent(PendingIntent.getActivity(c, 20, new Intent(c, HermesActivity.class),
             PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE));
        nm.notify(id, b.build());
    }

    /** Elle test: bildirim gercekten geliyor mu? */
    static void test(Context c) {
        bildir(c, 9999, "Test: sunucu uyarısı çalışıyor ✓", "Bu bir deneme bildirimidir. Gerçek uyarılar böyle görünecek.", true);
    }

    /**
     * Tek konu degerlendirme.
     * @param gereken kac ardisik kontrolde sorun surerse bildirim atilsin
     */
    static void degerlendir(Context c, String anahtar, boolean sorun, int gereken, String baslik, String metin) {
        SharedPreferences sp = p(c);
        int sayac = sp.getInt("c_" + anahtar, 0);
        sayac = sorun ? sayac + 1 : 0;
        SharedPreferences.Editor e = sp.edit();
        e.putInt("c_" + anahtar, sayac);
        boolean aktif = sp.getBoolean("a_" + anahtar, false);
        long simdi = System.currentTimeMillis();
        long son = sp.getLong("n_" + anahtar, 0);
        int id = 1000 + Math.abs(anahtar.hashCode() % 5000);
        if (sorun && sayac >= gereken) {
            if (!aktif || simdi - son > TEKRAR_MS) {
                bildir(c, id, "⚠ " + baslik, metin, true);
                e.putLong("n_" + anahtar, simdi);
            }
            e.putBoolean("a_" + anahtar, true);
        } else if (!sorun && aktif) {
            bildir(c, id, "✓ Düzeldi: " + baslik, "Sorun geçti, her şey normale döndü.", false);
            e.putBoolean("a_" + anahtar, false);
        }
        e.apply();
    }

    /** ChatGPT/Codex kotasi: 5 saatlik >= %90 ya da haftalik >= %85 ise uyar. Ornek veri uyari uretmez. */
    static void kotaKontrol(Context c, CodexApi.Veri k) {
        if (!acik(c) || k == null || !k.var || k.ornek) return;
        degerlendir(c, "kota_kisa", k.kisaYuzde >= 90, 1, "ChatGPT 5 saatlik kotan bitmek üzere",
            String.format(Locale.US, "5 saatlik pencere %%%.0f dolu. Yenilenme: %s. Biterse Hermes ücretli OpenRouter yedeğine düşebilir.", k.kisaYuzde, CodexApi.kalan(k.kisaYenilenme)));
        degerlendir(c, "kota_uzun", k.uzunYuzde >= 85, 1, "ChatGPT haftalık kotan bitmek üzere",
            String.format(Locale.US, "Haftalık pencere %%%.0f dolu. Yenilenme: %s.", k.uzunYuzde, CodexApi.kalan(k.uzunYenilenme)));
    }

    /** Claude plan limiti: 5 saatlik >= %90, haftalik >= %85. Eski/ornek veri uyari uretmez. */
    static void claudeKontrol(Context c, ClaudeApi.Veri k) {
        if (!acik(c) || k == null || !k.var || k.ornek) return;
        if (ClaudeApi.yas(k) > 3 * 3600) return;
        boolean kisaGecerli = !ClaudeApi.yenilenmis(k.kisaYenilenme);
        boolean uzunGecerli = !ClaudeApi.yenilenmis(k.uzunYenilenme);
        degerlendir(c, "claude_kisa", kisaGecerli && k.kisaYuzde >= 90, 1, "Claude 5 saatlik limitin bitmek üzere",
            String.format(Locale.US, "5 saatlik pencere %%%.0f dolu. Yenilenme: %s.", k.kisaYuzde, CodexApi.kalan(k.kisaYenilenme)));
        degerlendir(c, "claude_uzun", uzunGecerli && k.uzunYuzde >= 85, 1, "Claude haftalık limitin bitmek üzere",
            String.format(Locale.US, "Haftalık pencere %%%.0f dolu. Yenilenme: %s.", k.uzunYuzde, CodexApi.kalan(k.uzunYenilenme)));
    }

    /** Her sorgudan sonra cagrilir. */
    static void kontrol(Context c, HermesApi.Veri v) {
        if (!acik(c)) return;
        // sunucuya ulasilamiyor: 3 ardisik kontrol (ekran acikken ~1,5 dk; kapaliyken ~15 dk)
        degerlendir(c, "sunucu", !v.ulasildi, 3, "Sunucuya ulaşılamıyor",
            "Hermes sunucusu yanıt vermiyor. Tailscale kapalı ya da sunucu/İnternet kapalı olabilir.");
        if (!v.ulasildi) return;

        // servisler
        for (HermesApi.Svc s : v.servisler) {
            degerlendir(c, "svc_" + s.kisa, !s.ok, 2, s.ad + " kapandı",
                s.ad + " yanıt vermiyor (kod " + s.kod + "). Sunucuda servis durmuş olabilir.");
        }

        // sicakliklar
        degerlendir(c, "cpu_sicak", v.sicak >= CPU_SICAK, 2, "CPU çok sıcak",
            String.format(Locale.US, "CPU %.0f°C. Havalandırmayı ve sunucunun çevresini kontrol et.", v.sicak));
        if (v.bes != null && v.bes.ok) {
            degerlendir(c, "ssd_sicak", v.bes.ssdSicak >= SSD_SICAK, 2, "SSD çok sıcak",
                String.format(Locale.US, "SSD %.0f°C (sınır %.0f°C).", v.bes.ssdSicak, SSD_SICAK));
            degerlendir(c, "ssd_saglik", !v.bes.ssdGecti || (v.bes.ssdAsinma >= SSD_ASINMA), 1, "SSD sağlığı bozuldu",
                "SSD sağlık testi başarısız ya da yıpranma yüksek (%" + Math.round(v.bes.ssdAsinma) + "). Yedeklerin güncel mi kontrol et.");
            degerlendir(c, "pil", v.bes.pilDikkat, 2, "Sunucu pille çalışıyor",
                "Sunucu dizüstünün fişi çekilmiş ya da elektrik kesilmiş. Pil: " + v.bes.pil);
        }

        // doluluk
        double ram = v.ramToplam > 0 ? v.ramKullanilan * 100.0 / v.ramToplam : 0;
        degerlendir(c, "disk", v.diskYuzde >= DISK_YUZDE, 2, "Disk dolmak üzere",
            String.format(Locale.US, "Sunucu diski %%%.0f dolu (%s boş).", v.diskYuzde, HermesApi.gb(v.diskBos)));
        degerlendir(c, "ram", ram >= RAM_YUZDE, 3, "RAM çok dolu",
            String.format(Locale.US, "RAM %%%.0f kullanımda (müsait %s).", ram, HermesApi.gb(v.ramMusait)));

        // Hermes genel durum
        degerlendir(c, "hermes", !"ok".equals(v.genel) && !"-".equals(v.genel), 2, "Hermes sağlık durumu bozuldu",
            "Hermes “genel durum”: " + v.genel + ". Gateway: " + v.gwDurum + ".");
    }

    /** OpenRouter: kredi azaldı ya da hızla eriyor */
    static void orKontrol(Context c, Api.Sonuc s) {
        if (!acik(c) || s == null || !Api.veriVar(s)) return;
        SharedPreferences pr = c.getSharedPreferences(Api.PREFS, 0);
        double hiz = pr.getFloat("hiz", -1f);
        Double kalan = s.kalanDeger;
        if (kalan == null) return;
        degerlendir(c, "or_kalan", kalan < 3, 1, "OpenRouter kredin azaldı",
            String.format(Locale.US, "Kalan kredi $%.2f. Biterse Hermes’in OpenRouter’a bağlı modelleri çalışmaz.", kalan));
        boolean hizliBitis = hiz > 0.5 && kalan / hiz < 3;
        degerlendir(c, "or_hiz", hizliBitis, 2, "OpenRouter kredisi hızla eriyor",
            String.format(Locale.US, "Son saatte $%.2f/saat harcanıyor; kalan $%.2f bu hızla ~%s sonra biter.", hiz, kalan, Api.sureYaz(kalan / Math.max(hiz, 0.01))));
    }
}
