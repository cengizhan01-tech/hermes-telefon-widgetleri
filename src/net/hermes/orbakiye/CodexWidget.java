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
import android.view.View;
import android.widget.RemoteViews;

public class CodexWidget extends AppWidgetProvider {
    public static final String ACTION_REFRESH = "net.hermes.orbakiye.CODEX_REFRESH";
    static volatile CodexApi.Veri son;

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
            int[] ids = m.getAppWidgetIds(new ComponentName(c, CodexWidget.class));
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
                    CodexApi.ayarla(c);
                    CodexApi.Veri v = CodexApi.getir();
                    if (v.var) son = v;
                    gor(c, m, ids, v.var ? v : (son != null ? son : v), false);
                } catch (Throwable t) {
                    // sessiz geç
                } finally {
                    if (pr != null) pr.finish();
                }
            }
        }).start();
    }

    /** Dolu % büyüdükçe kotanın bitmesi yaklaşır */
    static int renk(Tema t, double doluYuzde) {
        return doluYuzde < 60 ? t.tamam : (doluYuzde < 85 ? t.orta : t.dusuk);
    }

    static void bar(RemoteViews v, int id, double kalanYuzde, int renk) {
        v.setViewVisibility(id, View.VISIBLE);
        v.setProgressBar(id, 1000, (int) Math.max(8, Math.min(1000, kalanYuzde * 10)), false);
        if (Build.VERSION.SDK_INT >= 31) v.setColorStateList(id, "setProgressTintList", ColorStateList.valueOf(renk));
    }

    static void gor(Context c, AppWidgetManager m, int[] ids, CodexApi.Veri d, boolean yukleniyor) {
        Tema t = Tema.mevcut(c);
        for (int id : ids) {
            RemoteViews v = new RemoteViews(c.getPackageName(), R.layout.codex_widget);
            v.setInt(R.id.root, "setBackgroundResource", t.bg);
            v.setTextViewText(R.id.c_title, t.mono ? "▌ CHATGPT // KOTA.DURUMU" : "✦  CHATGPT KOTA (CODEX / HERMES)");
            v.setTextColor(R.id.c_title, t.baslik);
            v.setTextColor(R.id.c_refresh, t.ayar);
            for (int x : new int[] { R.id.c_sec1, R.id.c_sec2, R.id.c_sec3 }) v.setTextColor(x, t.baslik);
            for (int x : new int[] { R.id.c_ana_etiket, R.id.c_uzun_yazi, R.id.c_tempo1, R.id.c_tempo2, R.id.c_not }) v.setTextColor(x, t.aciklama);
            v.setTextColor(R.id.c_alt, t.guncel);
            if (d == null || !d.var) {
                v.setTextViewText(R.id.c_ana, d != null && d.yayinYok ? "Yayın yok" : (d != null && d.hata != null ? "⚠" : "…"));
                v.setTextColor(R.id.c_ana, t.orta);
                v.setTextViewText(R.id.c_ana_etiket, d != null && d.yayinYok ? "Sunucuda kota yayını henüz kurulmamış."
                    : (d != null && d.hata != null ? d.hata : "Yükleniyor…"));
                v.setViewVisibility(R.id.c_bar1, View.GONE);
                v.setViewVisibility(R.id.c_bar2, View.GONE);
                v.setTextViewText(R.id.c_uzun_buyuk, "");
                v.setTextViewText(R.id.c_uzun_yazi, "");
                v.setTextViewText(R.id.c_tempo1, "");
                v.setTextViewText(R.id.c_tempo2, "");
                v.setTextViewText(R.id.c_not, "Bu widget, Hermes'in zaten okuduğu ChatGPT/Codex kullanım kotasını gösterir. "
                    + "Veri sunucudan, ChatGPT oturum anahtarı OLMADAN yayımlanır (yalnız yüzde ve zaman). "
                    + "Kurulum için Usta talimatı: 2026-10-02-chatgpt-kota-usta-talimati.md\n\nÖnizleme: ⚙ ayarlardan 'ChatGPT kota widget'ı' düğmesiyle örnek veri açılır.");
                v.setTextViewText(R.id.c_alt, "");
            } else {
                double kisaKalan = d.kisaYuzde >= 0 ? 100 - d.kisaYuzde : -1;
                double uzunKalan = d.uzunYuzde >= 0 ? 100 - d.uzunYuzde : -1;
                int rk = d.kisaYuzde >= 0 ? renk(t, d.kisaYuzde) : t.tamam;
                v.setTextViewText(R.id.c_ana, d.kisaYuzde >= 0 ? CodexApi.yuzde(kisaKalan) + " kaldı" : "-");
                v.setTextColor(R.id.c_ana, rk);
                v.setTextViewText(R.id.c_ana_etiket, CodexApi.yuzde(d.kisaYuzde) + " kullanıldı  ·  yenilenmeye " + CodexApi.kalan(d.kisaYenilenme)
                    + " var  (" + CodexApi.zamanYaz(d.kisaYenilenme) + ")");
                if (kisaKalan >= 0) bar(v, R.id.c_bar1, kisaKalan, rk); else v.setViewVisibility(R.id.c_bar1, View.GONE);
                v.setTextViewText(R.id.c_tempo1, CodexApi.tempo(d.kisaYuzde, d.kisaYenilenme, 18000, "5 saatlik"));

                if (d.uzunYuzde >= 0) {
                    int rk2 = renk(t, d.uzunYuzde);
                    v.setTextViewText(R.id.c_uzun_buyuk, CodexApi.yuzde(uzunKalan) + " kaldı");
                    v.setTextColor(R.id.c_uzun_buyuk, rk2);
                    v.setTextViewText(R.id.c_uzun_yazi, CodexApi.yuzde(d.uzunYuzde) + " kullanıldı  ·  yenilenmeye " + CodexApi.kalan(d.uzunYenilenme)
                        + " var  (" + CodexApi.zamanYaz(d.uzunYenilenme) + ")");
                    bar(v, R.id.c_bar2, uzunKalan, rk2);
                    v.setTextViewText(R.id.c_tempo2, CodexApi.tempo(d.uzunYuzde, d.uzunYenilenme, 604800, "Haftalık"));
                } else {
                    v.setTextViewText(R.id.c_uzun_buyuk, "");
                    v.setTextViewText(R.id.c_uzun_yazi, "");
                    v.setTextViewText(R.id.c_tempo2, "");
                    v.setViewVisibility(R.id.c_bar2, View.GONE);
                }
                v.setTextViewText(R.id.c_not, "• Bu kota Hermes + Codex (Harita) için ORTAKTIR.\n"
                    + "• 5 saatlik pencere dolarsa sıradaki pencereyi beklemek gerekir; haftalık dolarsa hafta sonuna kadar.\n"
                    + "• Kota bitince Hermes ücretli OpenRouter yedeğine düşebilir (bakiye widget'ına bak).\n"
                    + "• Uyarı: 5 saatlik %90, haftalık %85'te telefona bildirim gelir.");
                String taze = d.ornek ? "ÖRNEK VERİ (gerçek değil)" : (CodexApi.taze(d) ? "veri " + CodexApi.once(d.guncellendi) : "⚠ veri eski (" + CodexApi.once(d.guncellendi) + ")");
                String kr = d.kredi >= 0 ? "Kalan kredi: " + String.format(java.util.Locale.US, "%.2f", d.kredi) + "  ·  " : "";
                if (d.kredi >= 0) v.setTextViewText(R.id.c_title, "✦  CHATGPT KOTA  ·  KREDİ " + String.format(java.util.Locale.US, "%.2f", d.kredi));
                v.setTextViewText(R.id.c_alt, kr + (yukleniyor ? "↻ yenileniyor…  " : "") + taze + "  ·  tema: " + t.ad + "  ·  dokun: yenile");
            }
            v.setOnClickPendingIntent(R.id.root,
                PendingIntent.getActivity(c, 35, new Intent(c, TapActivity.class).putExtra("tur", "codex"), PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE));
            v.setOnClickPendingIntent(R.id.c_refresh,
                PendingIntent.getBroadcast(c, 31, new Intent(c, CodexWidget.class).setAction(ACTION_REFRESH),
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE));
            DetayActivity.yakala = v;
            m.updateAppWidget(id, v);
        }
    }
}
