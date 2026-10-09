package net.hermes.orbakiye;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;

/**
 * Widget'lari yenileyen ve uyarilari kontrol eden on plan servisi.
 *  - Ekran acik        : Hermes sunucu widget'i 10 sn'de bir; OpenRouter/ChatGPT/Claude limit widget'lari 1 dk'da bir;
 *                        uyari degerlendirmesi 1 dk'da bir (10 sn'lik ani dalgalanmalar yanlis alarm uretmesin)
 *  - Ekran kapali      : her sey 5 dk'da bir (uyarilar kacmasin)
 *  - Sessiz saat 22:00-03:45 : hic sorgu/bildirim yok
 */
public class RefreshService extends Service {
    static final long HERMES_ARALIK = 10000L;       // 10 sn
    static final long LIMIT_ARALIK = 60000L;        // 1 dk (ekran açıkken)
    static final long UYARI_ARALIK = 60000L;        // 1 dk
    static final long EKRAN_KAPALI_ARALIK = 300000L;
    static final String KANAL = "or_yenileme";
    volatile boolean calisiyor = false;
    Thread is;

    public static void baslat(Context c) {
        try {
            Intent i = new Intent(c, RefreshService.class);
            if (Build.VERSION.SDK_INT >= 26) c.startForegroundService(i);
            else c.startService(i);
        } catch (Throwable t) {
            // arka plandan baslatma kisitlanmis olabilir: widget'a/uygulamaya dokununca yeniden denenir
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        bildirimle();
        if (!calisiyor) {
            calisiyor = true;
            is = new Thread(new Runnable() {
                public void run() {
                    long sonHermes = 0, sonLimit = 0, sonUyari = 0;
                    while (calisiyor) {
                        long bekle = 5000L;
                        try {
                            Context c = RefreshService.this;
                            long simdi = System.currentTimeMillis();
                            if (Uyari.sessizMi(c)) {
                                bekle = 300000L;
                            } else {
                                PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
                                boolean ekranAcik = pm == null || pm.isInteractive();
                                long hermesAralik = ekranAcik ? HERMES_ARALIK : EKRAN_KAPALI_ARALIK;
                                AppWidgetManager m = AppWidgetManager.getInstance(c);
                                HermesApi.Veri hv = null;
                                if (simdi - sonHermes >= hermesAralik - 500) {
                                    sonHermes = simdi;
                                    hv = hermes(c, m);
                                }
                                if (simdi - sonLimit >= (ekranAcik ? LIMIT_ARALIK : EKRAN_KAPALI_ARALIK) - 500) {
                                    sonLimit = simdi;
                                    limitler(c, m);
                                }
                                if (hv != null && simdi - sonUyari >= (ekranAcik ? UYARI_ARALIK : EKRAN_KAPALI_ARALIK) - 500) {
                                    sonUyari = simdi;
                                    Uyari.kontrol(c, hv);
                                }
                                bekle = ekranAcik ? 2000L : 30000L;   // dongu uyanma araligi; asil sikligi sayaclar belirler
                            }
                            Thread.sleep(bekle);
                        } catch (InterruptedException e) {
                            return;
                        } catch (Throwable t) {
                            try { Thread.sleep(bekle); } catch (InterruptedException e) { return; }
                        }
                    }
                }
            });
            is.start();
        }
        return START_STICKY;
    }

    /** Hermes sunucu verisi + widget */
    HermesApi.Veri hermes(Context c, AppWidgetManager m) {
        HermesApi.ctx = c.getApplicationContext();
        HermesApi.Veri hv = HermesApi.getir();
        if (hv.ulasildi) HermesWidget.son = hv;
        int[] hids = m.getAppWidgetIds(new ComponentName(c, HermesWidget.class));
        if (hids.length > 0) HermesWidget.gor(c, m, hids, hv.ulasildi ? hv : (HermesWidget.son != null ? HermesWidget.son : hv), false);
        DurumWidget.guncelle(c, m);
        return hv;
    }

    /** OpenRouter + ChatGPT kota + Claude limit (5 dk) */
    void limitler(Context c, AppWidgetManager m) {
        if (Api.getAnahtar(c).length() >= 10) {
            Api.Sonuc s = Api.getir(Api.getAnahtar(c));
            boolean ok = Api.veriVar(s);
            if (ok) { Api.onbellegeYaz(c, s); Uyari.orKontrol(c, s); }
            int[] ids = m.getAppWidgetIds(new ComponentName(c, BalanceWidget.class));
            if (ids.length > 0) BalanceWidget.gor(c, m, ids, ok ? null : s.hata, false);
        }

        CodexApi.ayarla(c);
        CodexApi.Veri cv = CodexApi.getir();
        if (cv.var) CodexWidget.son = cv;
        int[] cids = m.getAppWidgetIds(new ComponentName(c, CodexWidget.class));
        if (cids.length > 0) CodexWidget.gor(c, m, cids, cv.var ? cv : (CodexWidget.son != null ? CodexWidget.son : cv), false);
        Uyari.kotaKontrol(c, cv);

        ClaudeApi.Veri kv = ClaudeApi.getir(c);
        if (kv.var) ClaudeWidget.son = kv;
        int[] kids = m.getAppWidgetIds(new ComponentName(c, ClaudeWidget.class));
        if (kids.length > 0) ClaudeWidget.gor(c, m, kids, kv.var ? kv : (ClaudeWidget.son != null ? ClaudeWidget.son : kv));
        Uyari.claudeKontrol(c, kv);
        DurumWidget.guncelle(c, m);
    }

    void bildirimle() {
        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= 26 && nm != null) {
            NotificationChannel ch = new NotificationChannel(KANAL, "OR Bakiye yenileme", NotificationManager.IMPORTANCE_MIN);
            ch.setShowBadge(false);
            nm.createNotificationChannel(ch);
        }
        Notification.Builder b = Build.VERSION.SDK_INT >= 26 ? new Notification.Builder(this, KANAL) : new Notification.Builder(this);
        b.setContentTitle("Widget'lar canlı").setContentText("Hermes 10 sn · limitler 1 dk · uyarılar açık")
            .setSmallIcon(android.R.drawable.stat_notify_sync).setOngoing(true);
        Notification n = b.build();
        if (Build.VERSION.SDK_INT >= 34) startForeground(1, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
        else startForeground(1, n);
    }

    @Override
    public void onDestroy() {
        calisiyor = false;
        if (is != null) is.interrupt();
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
