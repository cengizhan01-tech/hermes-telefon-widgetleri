package net.hermes.orbakiye;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Html;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/** Hermes sunucusu icin tam sayfa panel (15 sn'de bir yenilenir, temali). */
public class HermesActivity extends Activity {
    LinearLayout kok, icerik;
    TextView durum;
    Handler h = new Handler(Looper.getMainLooper());
    volatile boolean gorunur = false;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        ScrollView sv = new ScrollView(this);
        kok = new LinearLayout(this);
        kok.setOrientation(LinearLayout.VERTICAL);
        int pad = (int) (16 * getResources().getDisplayMetrics().density);
        kok.setPadding(pad, pad * 2, pad, pad * 2);
        icerik = new LinearLayout(this);
        icerik.setOrientation(LinearLayout.VERTICAL);
        kok.addView(icerik);
        sv.addView(kok);
        sv.setFillViewport(true);
        setContentView(sv);
        sv.setBackgroundResource(Tema.mevcut(this).bg);
        durum = yazi("Yükleniyor…", 14, "#A5B4FC", false);
        icerik.addView(durum);
    }

    @Override
    protected void onResume() {
        super.onResume();
        gorunur = true;
        yenile();
    }

    @Override
    protected void onPause() {
        gorunur = false;
        h.removeCallbacksAndMessages(null);
        super.onPause();
    }

    void yenile() {
        new Thread(new Runnable() {
            public void run() {
                HermesApi.ctx = getApplicationContext();
                final HermesApi.Veri v = HermesApi.getir();
                if (v.ulasildi) HermesWidget.son = v;
                runOnUiThread(new Runnable() {
                    public void run() {
                        ciz(v);
                    }
                });
            }
        }).start();
        h.postDelayed(new Runnable() {
            public void run() {
                if (gorunur) yenile();
            }
        }, 15000);
    }

    TextView yazi(String t, int sp, String renk, boolean kalin) {
        TextView v = new TextView(this);
        v.setText(t);
        v.setTextSize(sp);
        v.setTextColor(Color.parseColor(renk));
        if (kalin) v.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        v.setPadding(0, 6, 0, 6);
        return v;
    }

    TextView baslik(Tema t, String s) {
        TextView v = new TextView(this);
        v.setText(s);
        v.setTextSize(13);
        v.setTextColor(t.baslik);
        v.setTypeface(t.mono ? android.graphics.Typeface.MONOSPACE : android.graphics.Typeface.DEFAULT_BOLD, android.graphics.Typeface.BOLD);
        v.setLetterSpacing(0.12f);
        v.setPadding(0, 28, 0, 8);
        return v;
    }

    TextView satir(Tema t, String s, int renk) {
        TextView v = new TextView(this);
        v.setText(s);
        v.setTextSize(15);
        v.setTextColor(renk);
        if (t.mono) v.setTypeface(android.graphics.Typeface.MONOSPACE);
        v.setPadding(0, 5, 0, 5);
        return v;
    }

    View cubuk(Tema t, double yuzde) {
        LinearLayout k = new LinearLayout(this);
        k.setBackgroundResource(R.drawable.bar_track);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, (int) (10 * getResources().getDisplayMetrics().density));
        lp.setMargins(0, 6, 0, 10);
        k.setLayoutParams(lp);
        View dolu = new View(this);
        dolu.setBackgroundColor(HermesWidget.renkYuzde(t, yuzde));
        k.addView(dolu, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, (float) Math.max(2, yuzde)));
        k.addView(new View(this), new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, (float) Math.max(1, 100 - yuzde)));
        return k;
    }

    void ciz(HermesApi.Veri v) {
        Tema t = Tema.mevcut(this);
        ((View) kok.getParent()).setBackgroundResource(t.bg);
        icerik.removeAllViews();

        TextView b = new TextView(this);
        b.setText(t.mono ? "▌ HERMES // SERVER.STATUS" : "✦ HERMES SUNUCU");
        b.setTextSize(22);
        b.setTextColor(t.baslik);
        b.setTypeface(t.mono ? android.graphics.Typeface.MONOSPACE : android.graphics.Typeface.DEFAULT_BOLD, android.graphics.Typeface.BOLD);
        icerik.addView(b);

        if (!v.ulasildi) {
            icerik.addView(satir(t, "⚠ " + v.hata, t.dusuk));
            icerik.addView(satir(t, "Tailscale bağlı olmalı. Sunucu kapalı olabilir.", t.aciklama));
            ekle(t, v);
            return;
        }
        boolean iyi = "ok".equals(v.genel);
        TextView g = new TextView(this);
        g.setText(iyi ? "● HER ŞEY YOLUNDA" : "⚠ DİKKAT: " + v.genel);
        g.setTextSize(28);
        g.setTextColor(iyi ? t.tamam : t.dusuk);
        g.setTypeface(t.mono ? android.graphics.Typeface.MONOSPACE : android.graphics.Typeface.DEFAULT_BOLD, android.graphics.Typeface.BOLD);
        g.setPadding(0, 10, 0, 4);
        icerik.addView(g);

        icerik.addView(baslik(t, "KAYNAKLAR"));
        double ram = v.ramToplam > 0 ? v.ramKullanilan * 100.0 / v.ramToplam : 0;
        icerik.addView(satir(t, String.format(Locale.US, "CPU  %.1f%%   (yük %.2f)", v.cpu, v.load), t.chipYazi));
        icerik.addView(cubuk(t, v.cpu));
        icerik.addView(satir(t, String.format(Locale.US, "RAM  %.0f%%   %s / %s", ram, HermesApi.gb(v.ramKullanilan), HermesApi.gb(v.ramToplam)), t.chipYazi));
        icerik.addView(cubuk(t, ram));
        icerik.addView(satir(t, String.format(Locale.US, "Disk  %.0f%%   %s dolu · %s boş · toplam %s", v.diskYuzde, HermesApi.gb(v.diskKullanilan), HermesApi.gb(v.diskBos), HermesApi.gb(v.diskToplam)), t.chipYazi));
        icerik.addView(cubuk(t, v.diskYuzde));
        icerik.addView(satir(t, "RAM ayrıntı: müsait " + HermesApi.gb(v.ramMusait) + " · önbellek " + HermesApi.gb(v.ramOnbellek)
            + " · tampon " + HermesApi.gb(v.ramTampon), t.aciklama));
        icerik.addView(satir(t, "Swap  " + HermesApi.gb(v.swapKullanilan) + " / " + HermesApi.gb(v.swapToplam)
            + (v.swapKullanilan == 0 ? "  (hiç kullanılmamış)" : ""), t.aciklama));

        icerik.addView(baslik(t, "SICAKLIK"));
        if (v.sicak >= 0) {
            int sr = v.sicak >= 70 ? t.dusuk : (v.sicak >= 55 ? t.orta : t.tamam);
            icerik.addView(satir(t, "🌡  CPU  " + Math.round(v.sicak) + "°C   (en yüksek " + Math.round(v.sicakMax) + "°C)", sr));
            icerik.addView(cubuk(t, Math.min(100, v.sicak)));
            icerik.addView(satir(t, "Çekirdekler  " + v.cekirdekler + (v.sicakChipset >= 0 ? "   ·   yonga seti " + Math.round(v.sicakChipset) + "°" : ""), t.aciklama));
            icerik.addView(satir(t, v.sicak >= 70 ? "⚠ Sıcak! Havalandırmayı kontrol et." : (v.sicak >= 55 ? "Ilkın, normal yük altında." : "Serin ve rahat."), sr));
        } else {
            icerik.addView(satir(t, "Sıcaklık okunamadı.", t.aciklama));
        }
        icerik.addView(baslik(t, "AĞ, DİSK VE SSD (BESZEL)"));
        if (v.bes == null || v.bes.girisYok) {
            icerik.addView(satir(t, "Beszel girişi yapılmamış. Ayar: OR Bakiye uygulaması → 'Beszel girişi'.", t.aciklama));
        } else if (v.bes.hata != null) {
            icerik.addView(satir(t, "⚠ " + v.bes.hata, t.dusuk));
        } else {
            if (v.bes.ag.length() > 0) icerik.addView(satir(t, "Ağ (toplam)   " + v.bes.ag, t.chipYazi));
            if (v.bes.agArayuzler.length() > 0) icerik.addView(satir(t, v.bes.agArayuzler, t.aciklama));
            if (v.bes.diskIo.length() > 0) icerik.addView(satir(t, "Disk okuma/yazma   " + v.bes.diskIo, t.chipYazi));
            if (v.bes.yuk.length() > 0) icerik.addView(satir(t, "İşlemci yükü   " + v.bes.yuk, t.aciklama));
            if (v.bes.pil.length() > 0) icerik.addView(satir(t, "Sunucu pili   " + v.bes.pil, v.bes.pilDikkat ? t.dusuk : t.chipYazi));
            if (v.bes.fan.length() > 0) icerik.addView(satir(t, v.bes.fan, t.aciklama));
            if (v.bes.sicakliklar.length() > 0) icerik.addView(satir(t, "Sıcaklıklar   " + v.bes.sicakliklar, t.chipYazi));
            icerik.addView(baslik(t, "SSD SAĞLIĞI"));
            if (v.bes.smartAyrinti.length() > 0) icerik.addView(satir(t, v.bes.smartAyrinti, v.bes.ssdIyi ? t.tamam : t.chipYazi));
            else icerik.addView(satir(t, v.bes.smart, t.aciklama));
            if (v.bes.ssdNot.length() > 0) icerik.addView(satir(t, v.bes.ssdNot, v.bes.ssdIyi ? t.tamam : t.dusuk));
        }

        icerik.addView(baslik(t, "ÇALIŞMA SÜRESİ"));
        icerik.addView(satir(t, "⏱  " + HermesApi.sure(v.acikSn) + " kapanmadan açık", t.chipYazi));
        icerik.addView(satir(t, "Son açılış  " + HermesApi.acilis(v.acikSn), t.aciklama));
        icerik.addView(satir(t, "Son açılış " + (v.temizAcilis ? "temizdi (ani kapanma yok)" : "⚠ TEMİZ DEĞİLDİ (ani kapanma)")
            + (v.oomSuphesi ? "  ·  ⚠ bellek (OOM) şüphesi" : ""), v.temizAcilis && !v.oomSuphesi ? t.tamam : t.dusuk));

        icerik.addView(baslik(t, "HERMES AGENT"));
        icerik.addView(satir(t, "Sürüm  v" + v.surum, t.chipYazi));
        icerik.addView(satir(t, "Gateway  " + v.gwDurum + (v.gwMesgul ? " (meşgul)" : " (boşta)") + "  ·  mod: " + v.gwMod, t.chipYazi));
        icerik.addView(satir(t, "Aktif oturum  " + v.oturum + "   ·   aktif ajan  " + v.ajan, t.chipYazi));
        icerik.addView(satir(t, "Gateway bellek  " + v.gwRssMb + " MB" + (v.gwGuncel.length() > 0 ? "   ·   son sinyal " + HermesApi.once(v.gwGuncel) : ""), t.chipYazi));
        icerik.addView(satir(t, "Profiller  " + v.profiller, t.chipYazi));
        if (v.bilesenler.length() > 0) icerik.addView(satir(t, v.bilesenler.replace("\n", "  ·  "), t.aciklama));

        icerik.addView(baslik(t, "MODEL"));
        icerik.addView(satir(t, v.model, t.chipYazi));
        icerik.addView(satir(t, "Sağlayıcı: " + v.saglayici + (v.ctx > 0 ? "   ·   bağlam: " + (v.ctx / 1000) + "K token" : ""), t.aciklama));

        icerik.addView(baslik(t, "SERVİSLER (" + v.servisler.size() + ")"));
        int acik = 0;
        for (HermesApi.Svc s : v.servisler) if (s.ok) acik++;
        icerik.addView(satir(t, acik + " / " + v.servisler.size() + " servis çalışıyor", acik == v.servisler.size() ? t.tamam : t.dusuk));
        for (HermesApi.Svc s : v.servisler) {
            icerik.addView(satir(t, (s.ok ? "● " : "○ ") + s.ad + "   " + (s.ok ? Math.round(s.ms) + " ms" : "KAPALI (" + s.kod + ")"), s.ok ? t.tamam : t.dusuk));
        }
        ekle(t, v);
    }

    void ekle(Tema t, HermesApi.Veri v) {
        icerik.addView(baslik(t, "KISAYOLLAR"));
        icerik.addView(dugme("Hermes Dashboard", HermesApi.HD));
        icerik.addView(dugme("Homepage pano", HermesApi.HP));
        icerik.addView(dugme("Uptime Kuma", "http://@SUNUCU_IP@:3001"));
        icerik.addView(dugme("Beszel", "http://@SUNUCU_IP@:8090"));
        icerik.addView(dugme("Paperless", "http://@SUNUCU_IP@:8000"));
        String z = new SimpleDateFormat("HH:mm:ss", new Locale("tr")).format(new Date(v.zaman));
        durum = yazi("Güncellendi " + z + "  ·  15 sn'de bir yenilenir  ·  tema: " + t.ad, 12, "#A5B4FC", false);
        durum.setTextColor(t.guncel);
        icerik.addView(durum);
    }

    Button dugme(final String ad, final String url) {
        Button d = new Button(this);
        d.setText(ad);
        d.setAllCaps(false);
        d.setOnClickListener(new View.OnClickListener() {
            public void onClick(View x) {
                try {
                    Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                    i.setPackage("com.brave.browser");
                    startActivity(i);
                } catch (Throwable e) {
                    try { startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url))); } catch (Throwable e2) { }
                }
            }
        });
        return d;
    }
}
