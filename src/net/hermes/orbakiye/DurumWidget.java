package net.hermes.orbakiye;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

import java.util.Locale;

/** Tek satirlik durum seridi: Hermes, Claude ve ChatGPT onbellegini okur, agdan veri cekmez. */
public class DurumWidget extends AppWidgetProvider {
    static final double UYARI_YUZDE = 85;

    @Override
    public void onUpdate(Context c, AppWidgetManager m, int[] ids) {
        RefreshService.baslat(c);
        gor(c, m, ids);
    }

    @Override
    public void onEnabled(Context c) {
        RefreshService.baslat(c);
    }

    /** RefreshService her turdan sonra cagirir. */
    static void guncelle(Context c, AppWidgetManager m) {
        int[] ids = m.getAppWidgetIds(new ComponentName(c, DurumWidget.class));
        if (ids.length > 0) gor(c, m, ids);
    }

    static String yz(double y) {
        return y < 0 ? "-" : String.format(Locale.US, "%%%.0f", y);
    }

    static void gor(Context c, AppWidgetManager m, int[] ids) {
        Tema t = Tema.mevcut(c);
        HermesApi.Veri h = HermesWidget.son;
        ClaudeApi.Veri k = ClaudeWidget.son;
        CodexApi.Veri x = CodexWidget.son;

        String sorun = null;
        if (h == null) sorun = "Hermes verisi bekleniyor";
        else if (!"ok".equals(h.genel)) sorun = "Hermes: " + h.genel;
        else if (!h.temizAcilis || h.oomSuphesi) sorun = "Hermes temiz açılmadı";
        if (sorun == null && k != null && k.var && Math.max(k.kisaYuzde, k.uzunYuzde) >= UYARI_YUZDE) {
            sorun = "Claude limiti " + yz(Math.max(k.kisaYuzde, k.uzunYuzde)) + " dolu";
        }
        if (sorun == null && x != null && x.var && Math.max(x.kisaYuzde, x.uzunYuzde) >= UYARI_YUZDE) {
            sorun = "ChatGPT kotası " + yz(Math.max(x.kisaYuzde, x.uzunYuzde)) + " dolu";
        }

        StringBuilder s = new StringBuilder();
        s.append("Claude 5s ").append(k != null && k.var ? yz(k.kisaYuzde) : "-")
         .append(" · hafta ").append(k != null && k.var ? yz(k.uzunYuzde) : "-")
         .append("   ChatGPT 5s ").append(x != null && x.var ? yz(x.kisaYuzde) : "-")
         .append(" · hafta ").append(x != null && x.var ? yz(x.uzunYuzde) : "-");
        if (h != null) s.append("   CPU ").append(String.format(Locale.US, "%.0f%%", h.cpu));

        for (int id : ids) {
            RemoteViews v = new RemoteViews(c.getPackageName(), R.layout.durum_widget);
            v.setInt(R.id.root, "setBackgroundResource", t.bg);
            v.setTextViewText(R.id.d_ana, sorun == null ? "● HER ŞEY YOLUNDA" : "⚠ " + sorun);
            v.setTextColor(R.id.d_ana, sorun == null ? t.tamam : t.dusuk);
            v.setTextViewText(R.id.d_alt, s.toString());
            v.setTextColor(R.id.d_alt, t.aciklama);
            v.setOnClickPendingIntent(R.id.root,
                PendingIntent.getActivity(c, 31, new Intent(c, TapActivity.class).putExtra("tur", "hermes"),
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE));
            m.updateAppWidget(id, v);
        }
    }
}
