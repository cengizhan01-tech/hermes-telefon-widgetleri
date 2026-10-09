package net.hermes.orbakiye;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.RemoteViews;
import android.widget.ScrollView;
import android.widget.TextView;

/** Tam sayfa ayrıntı: widget'ın kendi düzenini çizer, widget'ta gizli bölümleri açar. */
public class DetayActivity extends Activity {
    static volatile RemoteViews yakala;
    ScrollView sv;
    String tur;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        tur = getIntent().getStringExtra("tur");
        if (tur == null) tur = "or";
        sv = new ScrollView(this);
        sv.setFillViewport(true);
        setContentView(sv);
    }

    @Override
    protected void onResume() {
        super.onResume();
        final Context c = getApplicationContext();
        new Thread(new Runnable() {
            public void run() {
                try {
                    AppWidgetManager m = AppWidgetManager.getInstance(c);
                    Class<?> k = "codex".equals(tur) ? CodexWidget.class : "claude".equals(tur) ? ClaudeWidget.class : BalanceWidget.class;
                    int[] ids = m.getAppWidgetIds(new ComponentName(c, k));
                    if (ids.length == 0) return;
                    int[] bir = new int[] { ids[0] };
                    if ("codex".equals(tur)) {
                        CodexApi.ayarla(c);
                        CodexApi.Veri d = CodexApi.getir();
                        if (d.var) CodexWidget.son = d;
                        CodexWidget.gor(c, m, bir, d.var ? d : (CodexWidget.son != null ? CodexWidget.son : d), false);
                    } else if ("claude".equals(tur)) {
                        ClaudeApi.Veri d = ClaudeApi.getir(c);
                        if (d.var) ClaudeWidget.son = d;
                        ClaudeWidget.gor(c, m, bir, d.var ? d : (ClaudeWidget.son != null ? ClaudeWidget.son : d));
                    } else {
                        String hata = null;
                        if (Api.getAnahtar(c).length() >= 10) {
                            Api.Sonuc s = Api.getir(Api.getAnahtar(c));
                            if (Api.veriVar(s)) Api.onbellegeYaz(c, s); else hata = s.hata;
                        }
                        BalanceWidget.gor(c, m, bir, hata, false);
                    }
                } catch (Throwable t) {
                    // son yakalanan görünüm gösterilir
                }
                runOnUiThread(new Runnable() { public void run() { ciz(); } });
            }
        }).start();
    }

    void ciz() {
        RemoteViews v = yakala;
        if (v == null) return;
        View kok = v.apply(this, sv);
        kok.setOnClickListener(null);
        kok.setClickable(false);
        ayikla(kok);
        sv.removeAllViews();
        sv.addView(kok, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
    }

    /** Gizli ama dolu metinleri aç; yazıyı büyüt. */
    void ayikla(View x) {
        if (x instanceof ViewGroup) {
            ViewGroup g = (ViewGroup) x;
            for (int i = 0; i < g.getChildCount(); i++) ayikla(g.getChildAt(i));
        } else if (x instanceof TextView && !(x instanceof android.widget.Button)) {
            TextView t = (TextView) x;
            t.setTextSize(TypedValue.COMPLEX_UNIT_PX, t.getTextSize() * 1.15f);
            String s = t.getText().toString().trim();
            if (t.getVisibility() == View.GONE && s.length() > 0 && !s.equals("…") && !s.equals("-")) t.setVisibility(View.VISIBLE);
        }
        // not: gizli ProgressBar'lar (veri yok) gizli kalır
    }
}
