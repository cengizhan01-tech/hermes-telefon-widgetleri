package net.hermes.orbakiye;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.os.Build;
import android.view.View;
import android.widget.RemoteViews;

public class BalanceWidget extends AppWidgetProvider {
    public static final String ACTION_REFRESH = "net.hermes.orbakiye.REFRESH";

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
            int[] ids = m.getAppWidgetIds(new ComponentName(c, BalanceWidget.class));
            RefreshService.baslat(c);
            cizVeGuncelle(c, m, ids, goAsync());
        } else {
            super.onReceive(c, i);
        }
    }

    /** Önce önbellekteki değeri hemen göster, sonra ağdan çekip yenile. */
    static void cizVeGuncelle(final Context c, final AppWidgetManager m, final int[] ids, final BroadcastReceiver.PendingResult pr) {
        gor(c, m, ids, null, true);
        new Thread(new Runnable() {
            public void run() {
                try {
                    Api.Sonuc s = Api.getir(Api.getAnahtar(c));
                    boolean ok = Api.veriVar(s);
                    if (ok) Api.onbellegeYaz(c, s);
                    gor(c, m, ids, ok ? null : s.hata, false);
                } catch (Throwable t) {
                    gor(c, m, ids, "Hata", false);
                } finally {
                    if (pr != null) pr.finish();
                }
            }
        }).start();
    }

    static void baslik(RemoteViews v, int id, Tema t) {
        v.setTextColor(id, t.baslik);
    }

    static void gor(Context c, AppWidgetManager m, int[] ids, String hata, boolean yukleniyor) {
        SharedPreferences p = c.getSharedPreferences(Api.PREFS, 0);
        boolean anahtarVar = Api.getAnahtar(c).length() >= 10;
        Tema t = Tema.mevcut(c);
        for (int id : ids) {
            RemoteViews v = new RemoteViews(c.getPackageName(), R.layout.widget);
            v.setInt(R.id.root, "setBackgroundResource", t.bg);
            v.setTextViewText(R.id.title, t.mono ? "▌ OPENROUTER // BAKİYE" : "◉  OPENROUTER BAKİYE");
            v.setTextColor(R.id.title, t.baslik);
            v.setTextColor(R.id.settings, t.ayar);
            v.setTextColor(R.id.caption, t.aciklama);
            v.setTextColor(R.id.updated, t.guncel);
            for (int sec : new int[] { R.id.sec_hiz, R.id.sec_model, R.id.sec_act, R.id.sec_anahtar, R.id.sec_hesap }) baslik(v, sec, t);
            for (int tx : new int[] { R.id.hiz, R.id.model, R.id.act, R.id.anahtar, R.id.hesap }) v.setTextColor(tx, t.aciklama);
            v.setTextColor(R.id.spark, t.tamam);
            int[] chipler = new int[] { R.id.chip_saat, R.id.chip_gun, R.id.chip_hafta, R.id.chip_ay };
            for (int ch : chipler) {
                v.setInt(ch, "setBackgroundResource", t.chip);
                v.setTextColor(ch, t.chipYazi);
            }
            int bal = t.mono ? R.id.balance_mono : R.id.balance;
            v.setViewVisibility(R.id.balance, t.mono ? View.GONE : View.VISIBLE);
            v.setViewVisibility(R.id.balance_mono, t.mono ? View.VISIBLE : View.GONE);

            if (!anahtarVar) {
                v.setTextViewText(bal, "Anahtar yok");
                v.setTextColor(bal, t.tamam);
                v.setTextViewText(R.id.caption, "⚙ dokun, OpenRouter anahtarını yapıştır");
                v.setViewVisibility(R.id.bar, View.GONE);
                v.setViewVisibility(R.id.chips, View.GONE);
                v.setTextViewText(R.id.updated, "");
            } else {
                float oran = p.getFloat("oran", -1f);
                int rk = oran < 0 ? t.tamam : (oran > 0.5f ? t.tamam : (oran > 0.2f ? t.orta : t.dusuk));
                v.setTextViewText(bal, p.getString("ana", "…"));
                v.setTextColor(bal, rk);
                String cap = p.getString("etiket", "");
                String lim = p.getString("limit", "");
                if (lim.length() > 0) cap = cap + "  ·  " + lim;
                if (hata != null) cap = "⚠ " + hata;
                v.setTextViewText(R.id.caption, cap);
                v.setViewVisibility(R.id.chips, View.VISIBLE);
                float hiz = p.getFloat("hiz", -1f);
                v.setTextViewText(R.id.chip_saat, "⏱ " + (hiz >= 0 ? Api.para((double) hiz) : "-") + "\nson saat");
                v.setTextColor(R.id.chip_saat, hiz > 2 ? t.dusuk : (hiz > 0.8 ? t.orta : t.chipYazi));
                v.setTextViewText(R.id.chip_gun, "☀ " + p.getString("gun", "-") + "\nbugün");
                v.setTextViewText(R.id.chip_hafta, "◐ " + p.getString("hafta", "-") + "\nhafta");
                v.setTextViewText(R.id.chip_ay, "☾ " + p.getString("ay", "-") + "\nay");
                if (oran >= 0) {
                    v.setViewVisibility(R.id.bar, View.VISIBLE);
                    v.setProgressBar(R.id.bar, 1000, Math.round(oran * 1000), false);
                    if (Build.VERSION.SDK_INT >= 31) v.setColorStateList(R.id.bar, "setProgressTintList", ColorStateList.valueOf(rk));
                } else {
                    v.setViewVisibility(R.id.bar, View.GONE);
                }
                v.setTextViewText(R.id.hiz, p.getString("hiz_txt", ""));
                String cz = p.getString("cizgi", "");
                v.setTextViewText(R.id.spark, cz.length() > 0 ? "son 12 saat  " + cz : "");
                v.setTextViewText(R.id.model, modelYazi(p));
                String act = p.getString("act_goster", "");
                boolean actVar = act.length() > 0;
                v.setViewVisibility(R.id.sec_act, actVar ? View.VISIBLE : View.GONE);
                v.setViewVisibility(R.id.act, actVar ? View.VISIBLE : View.GONE);
                v.setTextViewText(R.id.act, act);
                v.setTextViewText(R.id.anahtar, p.getString("anahtar_txt", ""));
                v.setTextViewText(R.id.hesap, p.getString("hesap_txt", ""));
                v.setTextViewText(R.id.updated, (yukleniyor ? "↻ yenileniyor…  " : "") + "güncellendi " + p.getString("zaman", "-")
                    + "  ·  tema: " + t.ad + "  ·  dokun: yenile  ·  ⚙: ayarlar");
            }
            Intent yenile = new Intent(c, BalanceWidget.class).setAction(ACTION_REFRESH);
            v.setOnClickPendingIntent(R.id.root,
                PendingIntent.getBroadcast(c, 0, yenile, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE));
            Intent ayar = new Intent(c, MainActivity.class);
            v.setOnClickPendingIntent(R.id.settings,
                PendingIntent.getActivity(c, 1, ayar, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE));
            m.updateAppWidget(id, v);
        }
    }

    /** Hermes'in şu an kullandığı (aktif) model — dashboard'dan önbellekli */
    static String modelYazi(SharedPreferences p) {
        if (!p.getBoolean("m_ok", false) && p.getString("m_tam", "").length() == 0) return "Hermes dashboard'a ulaşılamadı (Tailscale açık mı?)";
        String tam = p.getString("m_tam", "-");
        int ix = tam.lastIndexOf('/');
        String kisa = ix >= 0 ? tam.substring(ix + 1) : tam;
        StringBuilder sb = new StringBuilder();
        sb.append("● ").append(kisa).append("   (şu an aktif)\n");
        sb.append("Tam ad: ").append(tam).append("\n");
        sb.append("Sağlayıcı: ").append(p.getString("m_sag", "-"));
        String aile = p.getString("m_aile", "");
        if (aile.length() > 0) sb.append(" · aile: ").append(aile);
        sb.append("\nBağlam: ").append(HermesApi.tokenYaz(p.getLong("m_ctx", 0)));
        long cikti = p.getLong("m_cikti", 0);
        if (cikti > 0) sb.append(" · azami çıktı: ").append(HermesApi.tokenYaz(cikti));
        String yet = p.getString("m_yet", "");
        if (yet.length() > 0) sb.append("\nYetenekler: ").append(yet);
        return sb.toString();
    }
}
