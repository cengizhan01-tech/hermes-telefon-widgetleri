package net.hermes.orbakiye;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Build;
import android.widget.RemoteViews;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class HermesWidget extends AppWidgetProvider {
    public static final String ACTION_REFRESH = "net.hermes.orbakiye.HERMES_REFRESH";
    static volatile HermesApi.Veri son;

    @Override
    public void onUpdate(final Context c, final AppWidgetManager m, final int[] ids) {
        RefreshService.baslat(c);
        cizVeGuncelle(c, m, ids, goAsync());
    }

    @Override
    public void onEnabled(Context c) {
        RefreshService.baslat(c);
    }

    @Override
    public void onReceive(final Context c, Intent i) {
        if (ACTION_REFRESH.equals(i.getAction())) {
            AppWidgetManager m = AppWidgetManager.getInstance(c);
            int[] ids = m.getAppWidgetIds(new ComponentName(c, HermesWidget.class));
            RefreshService.baslat(c);
            cizVeGuncelle(c, m, ids, goAsync());
        } else {
            super.onReceive(c, i);
        }
    }

    static void cizVeGuncelle(final Context c, final AppWidgetManager m, final int[] ids, final BroadcastReceiver.PendingResult pr) {
        gor(c, m, ids, son, true);
        new Thread(new Runnable() {
            public void run() {
                try {
                    HermesApi.ctx = c.getApplicationContext();
                    HermesApi.Veri v = HermesApi.getir();
                    if (v.ulasildi) son = v;
                    gor(c, m, ids, v.ulasildi ? v : (son != null ? son : v), false);
                } catch (Throwable t) {
                    // sessiz geç
                } finally {
                    if (pr != null) pr.finish();
                }
            }
        }).start();
    }

    static int renkYuzde(Tema t, double y) {
        return y < 60 ? t.tamam : (y < 85 ? t.orta : t.dusuk);
    }

    static void bar(RemoteViews v, int id, double yuzde, int renk) {
        v.setProgressBar(id, 1000, (int) Math.max(8, Math.min(1000, yuzde * 10)), false);
        if (Build.VERSION.SDK_INT >= 31) v.setColorStateList(id, "setProgressTintList", ColorStateList.valueOf(renk));
    }

    static void gor(Context c, AppWidgetManager m, int[] ids, HermesApi.Veri d, boolean yukleniyor) {
        Tema t = Tema.mevcut(c);
        int[] chip = new int[] { R.id.h_s1, R.id.h_s2, R.id.h_s3, R.id.h_s4, R.id.h_s5, R.id.h_s6, R.id.h_s7, R.id.h_s8, R.id.h_s9, R.id.h_s10 };
        for (int id : ids) {
            RemoteViews v = new RemoteViews(c.getPackageName(), R.layout.hermes_widget);
            v.setInt(R.id.root, "setBackgroundResource", t.bg);
            v.setTextViewText(R.id.h_title, t.mono ? "▌ HERMES // SUNUCU.DURUMU" : "✦  HERMES SUNUCU");
            v.setTextColor(R.id.h_title, t.baslik);
            v.setTextColor(R.id.h_refresh, t.ayar);
            for (int x : new int[] { R.id.h_gateway, R.id.h_model, R.id.h_detay, R.id.h_isi, R.id.h_ssd, R.id.h_calisma }) v.setTextColor(x, t.aciklama);
            for (int x : new int[] { R.id.h_sec_isi, R.id.h_sec_ssd, R.id.h_sec_calisma, R.id.h_sec_svc }) v.setTextColor(x, t.baslik);
            v.setTextColor(R.id.h_updated, t.guncel);
            v.setTextColor(R.id.h_cpu_t, t.chipYazi);
            v.setTextColor(R.id.h_ram_t, t.chipYazi);
            v.setTextColor(R.id.h_disk_t, t.chipYazi);
            for (int ch : chip) {
                v.setInt(ch, "setBackgroundResource", t.chip);
                v.setTextColor(ch, t.chipYazi);
            }
            if (d == null || !d.ulasildi) {
                v.setTextViewText(R.id.h_overall, d != null && d.hata != null ? "⚠ ÇEVRİM DIŞI" : "…");
                v.setTextColor(R.id.h_overall, t.dusuk);
                v.setTextViewText(R.id.h_gateway, d != null && d.hata != null ? d.hata : "Yükleniyor…");
                for (int x : new int[] { R.id.h_model, R.id.h_detay, R.id.h_isi, R.id.h_ssd, R.id.h_calisma }) v.setTextViewText(x, "");
                for (int ch : chip) v.setTextViewText(ch, "");
            } else {
                boolean iyi = "ok".equals(d.genel);
                v.setTextViewText(R.id.h_overall, (iyi ? "● HER ŞEY YOLUNDA" : "⚠ DİKKAT: " + d.genel.toUpperCase(new Locale("tr"))));
                v.setTextColor(R.id.h_overall, iyi ? t.tamam : t.dusuk);
                v.setTextViewText(R.id.h_gateway, "Gateway: " + d.gwDurum + (d.gwMesgul ? " (meşgul)" : " (boşta)") + "  ·  Hermes v" + d.surum
                    + "  ·  mod " + d.gwMod + "\n" + d.oturum + " aktif oturum  ·  " + d.ajan + " aktif ajan  ·  profiller: " + d.profiller);
                v.setTextViewText(R.id.h_model, "Aktif model: " + d.model + " (" + d.saglayici + ")  ·  bağlam " + HermesApi.tokenYaz(d.ctx)
                    + (d.yetenek.length() > 0 ? "\nYetenekler: " + d.yetenek : ""));

                double ram = d.ramToplam > 0 ? d.ramKullanilan * 100.0 / d.ramToplam : 0;
                double disk = d.diskYuzde;
                v.setTextViewText(R.id.h_cpu_t, String.format(Locale.US, "CPU %.0f%%", d.cpu));
                v.setTextViewText(R.id.h_ram_t, String.format(Locale.US, "RAM %.0f%%", ram));
                v.setTextViewText(R.id.h_disk_t, String.format(Locale.US, "Disk %.0f%%", disk));
                bar(v, R.id.h_cpu, d.cpu, renkYuzde(t, d.cpu));
                bar(v, R.id.h_ram, ram, renkYuzde(t, ram));
                bar(v, R.id.h_disk, disk, renkYuzde(t, disk));

                BeszelApi.Sonuc b = d.bes;
                boolean besOk = b != null && b.ok;
                StringBuilder kay = new StringBuilder();
                kay.append("RAM ").append(HermesApi.gb(d.ramKullanilan)).append(" / ").append(HermesApi.gb(d.ramToplam))
                   .append("  ·  müsait ").append(HermesApi.gb(d.ramMusait)).append("  ·  önbellek ").append(HermesApi.gb(d.ramOnbellek))
                   .append("  ·  swap ").append(HermesApi.gb(d.swapKullanilan)).append("/").append(HermesApi.gb(d.swapToplam));
                kay.append("\nDisk ").append(HermesApi.gb(d.diskKullanilan)).append(" dolu  ·  ").append(HermesApi.gb(d.diskBos)).append(" boş  ·  toplam ").append(HermesApi.gb(d.diskToplam));
                kay.append("\nİşlemci yükü ").append(besOk && b.yuk.length() > 0 ? b.yuk : String.format(Locale.US, "%.2f", d.load));
                if (besOk && b.ag.length() > 0) kay.append("\nAğ ").append(b.ag);
                if (besOk && b.diskIo.length() > 0) kay.append("   ·   disk ").append(b.diskIo);
                v.setTextViewText(R.id.h_detay, kay.toString());

                StringBuilder isi = new StringBuilder();
                if (besOk && b.sicakliklar.length() > 0) isi.append("🌡 ").append(b.sicakliklar);
                else if (d.sicak >= 0) isi.append("🌡 CPU ").append(Math.round(d.sicak)).append("° (en yüksek ").append(Math.round(d.sicakMax)).append("°) · yonga seti ").append(Math.round(d.sicakChipset)).append("°");
                if (d.cekirdekler.length() > 0 && !(besOk && b.sicakliklar.length() > 0)) isi.append("\nÇekirdekler ").append(d.cekirdekler);
                if (besOk && b.fan.length() > 0) isi.append("\n").append(b.fan);
                if (besOk && b.pil.length() > 0) isi.append("\nSunucu pili: ").append(b.pil);
                v.setTextViewText(R.id.h_isi, isi.toString());
                v.setTextColor(R.id.h_isi, (besOk && b.pilDikkat) || d.sicak >= 70 ? t.dusuk : (d.sicak >= 55 ? t.orta : t.aciklama));

                if (besOk && b.ssd1.length() > 0) {
                    v.setTextViewText(R.id.h_ssd, b.ssd1 + "\n" + b.ssd2 + "\n" + b.ssd3);
                    v.setTextColor(R.id.h_ssd, b.ssdIyi ? t.tamam : t.dusuk);
                } else if (b != null && b.girisYok) {
                    v.setTextViewText(R.id.h_ssd, "SSD verisi için uygulamada Beszel girişi yap (⚙ → Beszel girişi).");
                } else {
                    v.setTextViewText(R.id.h_ssd, b != null && b.hata != null ? "⚠ " + b.hata : "SSD verisi okunamadı");
                }

                StringBuilder cal = new StringBuilder();
                cal.append("⏱ ").append(HermesApi.sure(d.acikSn)).append(" kapanmadan açık  ·  son açılış ").append(HermesApi.acilis(d.acikSn))
                   .append(d.temizAcilis ? " (temiz)" : " (⚠ temiz değil)").append(d.oomSuphesi ? " · bellek şüphesi" : "");
                cal.append("\nGateway bellek ").append(d.gwRssMb).append(" MB");
                if (d.gwGuncel.length() > 0) cal.append("  ·  son sinyal ").append(HermesApi.once(d.gwGuncel));
                if (d.bilesenler.length() > 0) cal.append("\n").append(d.bilesenler.replace("\n", "  ·  "));
                v.setTextViewText(R.id.h_calisma, cal.toString());
                v.setTextColor(R.id.h_calisma, d.temizAcilis && !d.oomSuphesi ? t.aciklama : t.dusuk);

                int acik = 0;
                for (HermesApi.Svc s : d.servisler) if (s.ok) acik++;
                v.setTextViewText(R.id.h_sec_svc, "SERVİSLER  " + acik + " / " + d.servisler.size() + " ÇALIŞIYOR");
                v.setTextColor(R.id.h_sec_svc, acik == d.servisler.size() ? t.tamam : t.dusuk);
                for (int i = 0; i < chip.length; i++) {
                    if (i < d.servisler.size()) {
                        HermesApi.Svc s = d.servisler.get(i);
                        v.setTextViewText(chip[i], (s.ok ? "● " : "○ ") + s.kisa + (s.ok ? "\n" + Math.round(s.ms) + " ms" : "\nKAPALI"));
                        v.setTextColor(chip[i], s.ok ? t.tamam : t.dusuk);
                    } else {
                        v.setTextViewText(chip[i], "");
                    }
                }
            }
            String zaman = d != null && d.zaman > 0 ? new SimpleDateFormat("HH:mm:ss", new Locale("tr")).format(new Date(d.zaman)) : "-";
            v.setTextViewText(R.id.h_updated, (yukleniyor ? "↻ yenileniyor…  " : "") + zaman + "  ·  tema: " + t.ad + "  ·  ↻ yenile  ·  dokun: tam sayfa panel");

            v.setOnClickPendingIntent(R.id.root,
                PendingIntent.getActivity(c, 10, new Intent(c, HermesActivity.class), PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE));
            v.setOnClickPendingIntent(R.id.h_refresh,
                PendingIntent.getBroadcast(c, 11, new Intent(c, HermesWidget.class).setAction(ACTION_REFRESH),
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE));
            m.updateAppWidget(id, v);
        }
    }
}
