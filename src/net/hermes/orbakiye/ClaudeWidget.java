package net.hermes.orbakiye;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Build;
import android.view.View;
import android.widget.RemoteViews;

import java.util.Locale;

public class ClaudeWidget extends AppWidgetProvider {
    public static final String ACTION_REFRESH = "net.hermes.orbakiye.CLAUDE_REFRESH";

    static volatile ClaudeApi.Veri son;

    static void guncelle(final Context c, final AppWidgetManager m, final int[] ids, final android.content.BroadcastReceiver.PendingResult pr) {
        gor(c, m, ids, son);
        new Thread(new Runnable() {
            public void run() {
                try {
                    ClaudeApi.Veri v = ClaudeApi.getir(c);
                    if (v.var) son = v;
                    gor(c, m, ids, v.var ? v : (son != null ? son : v));
                } catch (Throwable t) {
                    // sessiz gec
                } finally {
                    if (pr != null) pr.finish();
                }
            }
        }).start();
    }

    @Override
    public void onUpdate(Context c, AppWidgetManager m, int[] ids) {
        RefreshService.baslat(c);
        guncelle(c, m, ids, goAsync());
    }

    @Override
    public void onEnabled(Context c) {
        RefreshService.baslat(c);
    }

    @Override
    public void onReceive(Context c, Intent i) {
        if (ACTION_REFRESH.equals(i.getAction())) {
            AppWidgetManager m = AppWidgetManager.getInstance(c);
            int[] ids = m.getAppWidgetIds(new ComponentName(c, ClaudeWidget.class));
            RefreshService.baslat(c);
            guncelle(c, m, ids, goAsync());
        } else {
            super.onReceive(c, i);
        }
    }

    static int renk(Tema t, double dolu) {
        return dolu < 60 ? t.tamam : (dolu < 85 ? t.orta : t.dusuk);
    }

    static void bar(RemoteViews v, int id, double kalanYuzde, int renk) {
        v.setViewVisibility(id, View.VISIBLE);
        v.setProgressBar(id, 1000, (int) Math.max(8, Math.min(1000, kalanYuzde * 10)), false);
        if (Build.VERSION.SDK_INT >= 31) v.setColorStateList(id, "setProgressTintList", ColorStateList.valueOf(renk));
    }

    static void gor(Context c, AppWidgetManager m, int[] ids, ClaudeApi.Veri d) {
        Tema t = Tema.mevcut(c);
        for (int id : ids) {
            RemoteViews v = new RemoteViews(c.getPackageName(), R.layout.claude_widget);
            v.setInt(R.id.root, "setBackgroundResource", t.bg);
            v.setTextViewText(R.id.l_title, t.mono ? "▌ CLAUDE // LİMİT.DURUMU" : "✦  CLAUDE KULLANIM LİMİTİ");
            v.setTextColor(R.id.l_title, t.baslik);
            v.setTextColor(R.id.l_refresh, t.ayar);
            for (int x : new int[] { R.id.l_sec1, R.id.l_sec2, R.id.l_sec3, R.id.l_sec4, R.id.l_sec5 }) v.setTextColor(x, t.baslik);
            for (int x : new int[] { R.id.l_plan, R.id.l_ana_etiket, R.id.l_uzun_yazi, R.id.l_tempo1, R.id.l_tempo2, R.id.l_ekstra, R.id.l_baglam, R.id.l_not }) v.setTextColor(x, t.aciklama);
            v.setTextColor(R.id.l_alt, t.guncel);

            if (d == null || !d.var) {
                v.setTextViewText(R.id.l_ana, d != null && d.dosyaYok ? "Veri yok" : "⚠");
                v.setTextColor(R.id.l_ana, t.orta);
                v.setTextViewText(R.id.l_plan, d != null && d.hata != null ? d.hata : "Sunucuda Claude limit yayını henüz yok (Usta talimatı: 2026-10-02-claude-limit-usta-talimati.md).");
                for (int x : new int[] { R.id.l_ana_etiket, R.id.l_uzun_buyuk, R.id.l_uzun_yazi, R.id.l_tempo1, R.id.l_tempo2, R.id.l_ekstra, R.id.l_baglam, R.id.l_alt }) v.setTextViewText(x, "");
                for (int x : new int[] { R.id.l_bar1, R.id.l_bar2, R.id.l_bar3, R.id.l_bar4 }) v.setViewVisibility(x, View.GONE);
                v.setTextViewText(R.id.l_not, "Bu widget Claude (Pro) planının 5 saatlik ve haftalık kullanım limitini gösterir. "
                    + "Veri, sunucudaki Claude Code tarafından anahtarsız (yalnız yüzde ve zaman) yayımlanır; PC gerekmez.\n"
                    + "Önizleme için ⚙ ayarlardan 'Claude limit widget'ı' düğmesine dokun.");
            } else {
                long yas = ClaudeApi.yas(d);
                boolean eski = !d.ornek && yas > 3 * 3600;
                boolean kisaYen = ClaudeApi.yenilenmis(d.kisaYenilenme) && !d.ornek;
                boolean uzunYen = ClaudeApi.yenilenmis(d.uzunYenilenme) && !d.ornek;
                v.setTextViewText(R.id.l_plan, "Plan: Claude " + d.plan + (d.ornek ? "  ·  ÖRNEK VERİ" : "") + "  ·  hesap geneli (sunucudan)");

                if (kisaYen) {
                    v.setTextViewText(R.id.l_ana, "yenilendi");
                    v.setTextColor(R.id.l_ana, t.tamam);
                    v.setTextViewText(R.id.l_ana_etiket, "Bu pencere sıfırlanmış olmalı; güncel yüzde için yeni veri bekleniyor.");
                    v.setViewVisibility(R.id.l_bar1, View.GONE);
                    v.setTextViewText(R.id.l_tempo1, "");
                } else {
                    double kalan = d.kisaYuzde >= 0 ? 100 - d.kisaYuzde : -1;
                    int rk = d.kisaYuzde >= 0 ? renk(t, d.kisaYuzde) : t.tamam;
                    v.setTextViewText(R.id.l_ana, d.kisaYuzde >= 0 ? ClaudeApi.yuzde(kalan) + " kaldı" : "-");
                    v.setTextColor(R.id.l_ana, rk);
                    v.setTextViewText(R.id.l_ana_etiket, ClaudeApi.yuzde(d.kisaYuzde) + " kullanıldı  ·  yenilenmeye " + CodexApi.kalan(d.kisaYenilenme)
                        + " var  (" + CodexApi.zamanYaz(d.kisaYenilenme) + ")");
                    if (kalan >= 0) bar(v, R.id.l_bar1, kalan, rk); else v.setViewVisibility(R.id.l_bar1, View.GONE);
                    v.setTextViewText(R.id.l_tempo1, CodexApi.tempo(d.kisaYuzde, d.kisaYenilenme, 18000, "5 saatlik"));
                }

                if (uzunYen) {
                    v.setTextViewText(R.id.l_uzun_buyuk, "yenilendi");
                    v.setTextColor(R.id.l_uzun_buyuk, t.tamam);
                    v.setTextViewText(R.id.l_uzun_yazi, "Haftalık pencere sıfırlanmış olmalı.");
                    v.setViewVisibility(R.id.l_bar2, View.GONE);
                    v.setTextViewText(R.id.l_tempo2, "");
                } else if (d.uzunYuzde >= 0) {
                    double uk = 100 - d.uzunYuzde;
                    int rk2 = renk(t, d.uzunYuzde);
                    v.setTextViewText(R.id.l_uzun_buyuk, ClaudeApi.yuzde(uk) + " kaldı");
                    v.setTextColor(R.id.l_uzun_buyuk, rk2);
                    v.setTextViewText(R.id.l_uzun_yazi, ClaudeApi.yuzde(d.uzunYuzde) + " kullanıldı  ·  yenilenmeye " + CodexApi.kalan(d.uzunYenilenme)
                        + " var  (" + CodexApi.zamanYaz(d.uzunYenilenme) + ")");
                    bar(v, R.id.l_bar2, uk, rk2);
                    v.setTextViewText(R.id.l_tempo2, CodexApi.tempo(d.uzunYuzde, d.uzunYenilenme, 604800, "Haftalık"));
                } else {
                    v.setTextViewText(R.id.l_uzun_buyuk, "");
                    v.setTextViewText(R.id.l_uzun_yazi, "");
                    v.setTextViewText(R.id.l_tempo2, "");
                    v.setViewVisibility(R.id.l_bar2, View.GONE);
                }

                if (d.ekstraAcik && d.ekstraLimit <= 0) {
                    v.setTextViewText(R.id.l_ekstra, "Ek kullanım AÇIK: limit dolsa bile ücretli taşma ile devam edebilirsin (harcanan tutar bu kaynaktan okunamıyor).");
                    v.setViewVisibility(R.id.l_bar3, View.GONE);
                } else if (d.ekstraAcik && d.ekstraLimit > 0) {
                    double oran = Math.max(0, d.ekstraHarcanan) / d.ekstraLimit * 100;
                    v.setTextViewText(R.id.l_ekstra, "Ek kullanım AÇIK  ·  bu ay harcanan " + ClaudeApi.para(Math.max(0, d.ekstraHarcanan), d.para)
                        + " / aylık sınır " + ClaudeApi.para(d.ekstraLimit, d.para)
                        + (d.ekstraHarcanan <= 0 ? "\nHenüz hiç ücretli taşma olmadı: limitler dolsa bile bu sınıra kadar devam edebilirsin." : "\nKalan ek hak: " + ClaudeApi.para(d.ekstraLimit - d.ekstraHarcanan, d.para)));
                    bar(v, R.id.l_bar3, 100 - oran, renk(t, oran));
                } else if (d.ekstraAcik) {
                    v.setTextViewText(R.id.l_ekstra, "Ek kullanım AÇIK (aylık sınır tanımsız).");
                    v.setViewVisibility(R.id.l_bar3, View.GONE);
                } else {
                    v.setTextViewText(R.id.l_ekstra, "Ek kullanım KAPALI: limit dolarsa pencere yenilenene kadar beklemen gerekir.");
                    v.setViewVisibility(R.id.l_bar3, View.GONE);
                }

                if (d.baglamPencere > 0 && d.baglamYuzde >= 0) {
                    double kalanBaglam = 100 - d.baglamYuzde;
                    v.setTextViewText(R.id.l_baglam, "Bu konuşma: " + ClaudeApi.tokenYaz(d.baglamKullanilan) + " / " + ClaudeApi.tokenYaz(d.baglamPencere)
                        + " token  (" + ClaudeApi.yuzde(d.baglamYuzde) + " dolu)\nKendiliğinden özetleme (sıkıştırma) %" + Math.round(d.otomatikSikistirma)
                        + " dolunca başlar" + (d.baglamYuzde >= d.otomatikSikistirma - 8 ? "  →  ⚠ çok yakın" : "") + ".");
                    bar(v, R.id.l_bar4, kalanBaglam, renk(t, d.baglamYuzde));
                } else {
                    v.setViewVisibility(R.id.l_sec4, View.GONE);
                    v.setViewVisibility(R.id.l_baglam, View.GONE);
                    v.setViewVisibility(R.id.l_bar4, View.GONE);
                }

                v.setTextViewText(R.id.l_not, "• Bu limitler Claude uygulaması ve Claude Code için ORTAKTIR (aynı kota).\n"
                    + "• 5 saatlik pencere ilk mesajla başlar; dolarsa yenilenmeyi beklersin.\n"
                    + "• Haftalık limit tüm modeller için tek havuzdur.\n"
                    + "• Kota yoğun işte (araç, çok dosya) hızlı erir; küçük sohbetlerde yavaş.\n"
                    + "• Bildirim: 5 saatlik %90, haftalık %85 dolunca telefona uyarı gelir.");
                String taze = d.ornek ? "ÖRNEK VERİ (gerçek değil)" : (eski ? "⚠ veri eski (" + ClaudeApi.once(d.guncellendi) + ") — yüzdeler güncel olmayabilir" : "veri " + ClaudeApi.once(d.guncellendi));
                v.setTextViewText(R.id.l_alt, taze + "  ·  geri sayımlar canlı  ·  tema: " + t.ad + "  ·  dokun: yenile");
                v.setTextColor(R.id.l_alt, eski ? t.orta : t.guncel);
            }
            v.setOnClickPendingIntent(R.id.root,
                PendingIntent.getBroadcast(c, 40, new Intent(c, ClaudeWidget.class).setAction(ACTION_REFRESH),
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE));
            v.setOnClickPendingIntent(R.id.l_refresh,
                PendingIntent.getBroadcast(c, 41, new Intent(c, ClaudeWidget.class).setAction(ACTION_REFRESH),
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE));
            m.updateAppWidget(id, v);
        }
    }
}
