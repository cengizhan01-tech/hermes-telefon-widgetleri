package net.hermes.orbakiye;

import android.app.Activity;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

public class MainActivity extends Activity {
    EditText anahtar, yonetim, ekLimit, ekHarcanan, ekKredi;
    TextView sonuc;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        int pad = (int) (20 * getResources().getDisplayMetrics().density);
        LinearLayout k = new LinearLayout(this);
        k.setOrientation(LinearLayout.VERTICAL);
        k.setBackgroundColor(Color.parseColor("#101820"));
        k.setPadding(pad, pad * 2, pad, pad);

        TextView baslik = yazi("OpenRouter Bakiye", 24, "#FFFFFF");
        TextView ipucu = yazi("API anahtarını bir kez yapıştır. Anahtar yalnız bu telefonda, uygulamanın özel alanında saklanır.", 14, "#9FB3C8");
        anahtar = new EditText(this);
        anahtar.setHint("sk-or-v1-...");
        anahtar.setHintTextColor(Color.parseColor("#6B7C8F"));
        anahtar.setTextColor(Color.WHITE);
        anahtar.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        anahtar.setSingleLine(true);
        if (Api.getAnahtar(this).length() >= 10) anahtar.setHint("Anahtar kayıtlı (değiştirmek için yapıştır)");

        Button yapistir = new Button(this);
        yapistir.setText("Panodan yapıştır");
        yapistir.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                if (cm != null && cm.hasPrimaryClip() && cm.getPrimaryClip().getItemCount() > 0) {
                    CharSequence t = cm.getPrimaryClip().getItemAt(0).coerceToText(MainActivity.this);
                    if (t != null) anahtar.setText(t.toString().trim());
                }
            }
        });
        Button kaydet = new Button(this);
        kaydet.setText("Kaydet ve yenile");
        kaydet.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                kaydetYenile();
            }
        });
        Button sil = new Button(this);
        sil.setText("Anahtarı telefondan sil");
        sil.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Api.setAnahtar(MainActivity.this, "");
                anahtar.setText("");
                sonuc.setText("Anahtar silindi.");
                widgetYenile();
            }
        });
        final Button temaDugme = new Button(this);
        temaDugme.setText("Tema: " + Tema.modAdi(this) + "  (değiştirmek için dokun)");
        temaDugme.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Tema.sonrakiMod(MainActivity.this);
                temaDugme.setText("Tema: " + Tema.modAdi(MainActivity.this) + "  (değiştirmek için dokun)");
                widgetYenile();
            }
        });
        final Button uyariDugme = new Button(this);
        uyariDugme.setText(uyariYazi());
        uyariDugme.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Uyari.acikAyarla(MainActivity.this, !Uyari.acik(MainActivity.this));
                uyariDugme.setText(uyariYazi());
            }
        });
        final Button sessizDugme = new Button(this);
        sessizDugme.setText(sessizYazi());
        sessizDugme.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Uyari.sessizAyarla(MainActivity.this, !Uyari.sessizSaatAcik(MainActivity.this));
                sessizDugme.setText(sessizYazi());
            }
        });
        CodexApi.ayarla(this);
        final Button kotaOrnek = new Button(this);
        kotaOrnek.setText(kotaOrnekYazi());
        kotaOrnek.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                CodexApi.ornekAyarla(MainActivity.this, !CodexApi.ornekMod());
                kotaOrnek.setText(kotaOrnekYazi());
                sendBroadcast(new Intent(MainActivity.this, CodexWidget.class).setAction(CodexWidget.ACTION_REFRESH));
            }
        });
        final Button claudeOrnek = new Button(this);
        claudeOrnek.setText(claudeOrnekYazi());
        claudeOrnek.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                ClaudeApi.ornekAyarla(MainActivity.this, !ClaudeApi.ornekMod(MainActivity.this));
                claudeOrnek.setText(claudeOrnekYazi());
                sendBroadcast(new Intent(MainActivity.this, ClaudeWidget.class).setAction(ClaudeWidget.ACTION_REFRESH));
            }
        });
        Button testDugme = new Button(this);
        testDugme.setText("Test bildirimi gönder");
        testDugme.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Uyari.test(MainActivity.this);
            }
        });
        Button widgetEkle = new Button(this);
        widgetEkle.setText("Widget'ı ana ekrana ekle");
        widgetEkle.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                widgetSabitle();
            }
        });
        TextView sunBaslik = yazi("Sunucu adresi (Hermes sunucu, ChatGPT kotası, Claude limiti widget'ları için)", 14, "#9FB3C8");
        final EditText sunAdres = new EditText(this);
        sunAdres.setHint("100.x.y.z veya sunucu adı");
        sunAdres.setHintTextColor(Color.parseColor("#6B7C8F"));
        sunAdres.setTextColor(Color.WHITE);
        sunAdres.setSingleLine(true);
        sunAdres.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
        if (Sunucu.ham().length() > 0) sunAdres.setText(Sunucu.ham());
        Button sunKaydet = new Button(this);
        sunKaydet.setText("Sunucu adresini kaydet");
        sunKaydet.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                String a = sunAdres.getText().toString().trim();
                Sunucu.kaydet(MainActivity.this, a);
                sonuc.setText(Sunucu.var() ? "Sunucu adresi kaydedildi: " + Sunucu.ip() + "\nWidget'lar birkaç saniye içinde güncellenir." : "Sunucu adresi silindi.");
                widgetYenile();
                sendBroadcast(new Intent(MainActivity.this, HermesWidget.class).setAction(HermesWidget.ACTION_REFRESH));
                sendBroadcast(new Intent(MainActivity.this, CodexWidget.class).setAction(CodexWidget.ACTION_REFRESH));
                sendBroadcast(new Intent(MainActivity.this, ClaudeWidget.class).setAction(ClaudeWidget.ACTION_REFRESH));
            }
        });
        TextView besBaslik = yazi("Beszel girişi (SSD / ağ hızı için, salt okunur kullanıcı)", 14, "#9FB3C8");
        final EditText besMail = new EditText(this);
        besMail.setHint("Beszel e-postası");
        besMail.setHintTextColor(Color.parseColor("#6B7C8F"));
        besMail.setTextColor(Color.WHITE);
        besMail.setSingleLine(true);
        besMail.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        final EditText besPass = new EditText(this);
        besPass.setHint("Beszel parolası");
        besPass.setHintTextColor(Color.parseColor("#6B7C8F"));
        besPass.setTextColor(Color.WHITE);
        besPass.setSingleLine(true);
        besPass.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        BeszelApi.ayarla(this);
        if (BeszelApi.mail().length() > 0) besMail.setHint("Kayıtlı: " + BeszelApi.mail() + " (değiştirmek için yaz)");
        Button besKaydet = new Button(this);
        besKaydet.setText("Beszel girişini kaydet ve dene");
        besKaydet.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                String m = besMail.getText().toString().trim();
                String p = besPass.getText().toString();
                if (m.length() < 3) m = BeszelApi.mail();
                if (p.length() < 3) p = BeszelApi.parola();
                BeszelApi.kaydet(MainActivity.this, m, p);
                besPass.setText("");
                sonuc.setText("Beszel deneniyor…");
                new Thread(new Runnable() {
                    public void run() {
                        final BeszelApi.Sonuc b = BeszelApi.getir();
                        runOnUiThread(new Runnable() {
                            public void run() {
                                if (b.ok) sonuc.setText("Beszel tamam ✓\n" + b.ag + "\n" + b.diskIo + "\n" + b.smart);
                                else sonuc.setText("Beszel: " + (b.hata != null ? b.hata : "giriş bilgisi eksik"));
                                widgetYenile();
                                sendBroadcast(new Intent(MainActivity.this, HermesWidget.class).setAction(HermesWidget.ACTION_REFRESH));
                            }
                        });
                    }
                }).start();
            }
        });
        Button besSil = new Button(this);
        besSil.setText("Beszel girişini telefondan sil");
        besSil.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                BeszelApi.sil(MainActivity.this);
                besMail.setText("");
                besPass.setText("");
                sonuc.setText("Beszel girişi silindi.");
                sendBroadcast(new Intent(MainActivity.this, HermesWidget.class).setAction(HermesWidget.ACTION_REFRESH));
            }
        });
        sonuc = yazi("", 18, "#2ECC71");
        sonuc.setGravity(Gravity.START);

        k.addView(baslik);
        k.addView(temaDugme);
        k.addView(uyariDugme);
        k.addView(sessizDugme);
        k.addView(testDugme);
        k.addView(kotaOrnek);
        k.addView(claudeOrnek);
        k.addView(widgetEkle);
        k.addView(ipucu);
        k.addView(anahtar);
        yonetim = new EditText(this);
        yonetim.setHint(Api.getYonetim(this).length() >= 10 ? "Yönetim anahtarı kayıtlı (değiştirmek için yapıştır)" : "Yönetim anahtarı (isteğe bağlı: model dökümü)");
        yonetim.setHintTextColor(Color.parseColor("#6B7C8F"));
        yonetim.setTextColor(Color.WHITE);
        yonetim.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        yonetim.setSingleLine(true);
        k.addView(yonetim);
        android.content.SharedPreferences cp = getSharedPreferences("or", 0);
        ekLimit = new EditText(this);
        ekLimit.setHint("Claude ek kullanım aylık limiti (örn. 30)" + (cp.getFloat("claude_ek_limit", -1f) > 0 ? "  [kayıtlı: " + cp.getFloat("claude_ek_limit", 0f) + "]" : ""));
        ekLimit.setHintTextColor(Color.parseColor("#6B7C8F"));
        ekLimit.setTextColor(Color.WHITE);
        ekLimit.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        ekLimit.setSingleLine(true);
        k.addView(ekLimit);
        ekHarcanan = new EditText(this);
        ekHarcanan.setHint("Claude ek kullanım bu ay harcanan (örn. 0)" + (cp.contains("claude_ek_harcanan") ? "  [kayıtlı: " + cp.getFloat("claude_ek_harcanan", 0f) + "]" : ""));
        ekHarcanan.setHintTextColor(Color.parseColor("#6B7C8F"));
        ekHarcanan.setTextColor(Color.WHITE);
        ekHarcanan.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        ekHarcanan.setSingleLine(true);
        k.addView(ekHarcanan);
        ekKredi = new EditText(this);
        ekKredi.setHint("Claude kalan kullanım kredisi (örn. 13.94)" + (cp.getFloat("claude_kredi", -1f) >= 0 ? "  [kayıtlı: " + cp.getFloat("claude_kredi", 0f) + "]" : ""));
        ekKredi.setHintTextColor(Color.parseColor("#6B7C8F"));
        ekKredi.setTextColor(Color.WHITE);
        ekKredi.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        ekKredi.setSingleLine(true);
        k.addView(ekKredi);
        k.addView(yapistir);
        k.addView(kaydet);
        k.addView(sil);
        k.addView(sunBaslik);
        k.addView(sunAdres);
        k.addView(sunKaydet);
        k.addView(besBaslik);
        k.addView(besMail);
        k.addView(besPass);
        k.addView(besKaydet);
        k.addView(besSil);
        k.addView(sonuc);
        android.widget.ScrollView kaydirma = new android.widget.ScrollView(this);
        kaydirma.setBackgroundColor(Color.parseColor("#101820"));
        kaydirma.addView(k);
        setContentView(kaydirma);
        RefreshService.baslat(this);
        if (Api.getAnahtar(this).length() >= 10) yenile();
        if (getIntent() != null && getIntent().getBooleanExtra("pin", false)) widgetSabitle();
    }

    String claudeOrnekYazi() {
        return "Claude limit widget'ı: " + (ClaudeApi.ornekMod(this) ? "ÖRNEK VERİ gösteriliyor" : "gerçek veri") + "  (önizleme için dokun)";
    }

    String kotaOrnekYazi() {
        return "ChatGPT kota widget'ı: " + (CodexApi.ornekMod() ? "ÖRNEK VERİ gösteriliyor" : "gerçek veri") + "  (önizleme için dokun)";
    }

    String uyariYazi() {
        return "Sunucu uyarı bildirimleri: " + (Uyari.acik(this) ? "AÇIK" : "KAPALI") + "  (değiştirmek için dokun)";
    }

    String sessizYazi() {
        return "Sessiz saatler 22:00–03:45: " + (Uyari.sessizSaatAcik(this) ? "AÇIK (sorgu/bildirim yok)" : "KAPALI") + "  (değiştirmek için dokun)";
    }

    void widgetSabitle() {
        android.appwidget.AppWidgetManager m = android.appwidget.AppWidgetManager.getInstance(this);
        if (m.isRequestPinAppWidgetSupported()) {
            m.requestPinAppWidget(new android.content.ComponentName(this, BalanceWidget.class), null, null);
        } else {
            sonuc.setText("Bu ana ekran widget sabitlemeyi desteklemiyor: ana ekranda boş yere basılı tut > Widget'lar > OR Bakiye.");
        }
    }

    TextView yazi(String t, int sp, String renk) {
        TextView v = new TextView(this);
        v.setText(t);
        v.setTextSize(sp);
        v.setTextColor(Color.parseColor(renk));
        v.setPadding(0, 12, 0, 12);
        return v;
    }

    void kaydetYenile() {
        String a = anahtar.getText().toString().trim();
        if (a.length() >= 10) Api.setAnahtar(this, a);
        String y = yonetim.getText().toString().trim();
        if (y.length() >= 10) Api.setYonetim(this, y);
        yonetim.setText("");
        try {
            android.content.SharedPreferences.Editor ed = getSharedPreferences("or", 0).edit();
            String el = ekLimit.getText().toString().trim().replace(',', '.');
            String eh = ekHarcanan.getText().toString().trim().replace(',', '.');
            if (el.length() > 0) ed.putFloat("claude_ek_limit", Float.parseFloat(el));
            if (eh.length() > 0) ed.putFloat("claude_ek_harcanan", Float.parseFloat(eh));
            String kk = ekKredi.getText().toString().trim().replace(',', '.');
            if (kk.length() > 0) ed.putFloat("claude_kredi", Float.parseFloat(kk));
            ed.apply();
        } catch (NumberFormatException e) {
            // geçersiz sayı yok sayılır
        }
        ekLimit.setText("");
        ekHarcanan.setText("");
        ekKredi.setText("");
        anahtar.setText("");
        yenile();
    }

    void yenile() {
        sonuc.setText("Sorgulanıyor...");
        new Thread(new Runnable() {
            public void run() {
                final Api.Sonuc s = Api.getir(Api.getAnahtar(MainActivity.this));
                if (s.toplamKredi != null || s.anahtarKullanim != null) Api.onbellegeYaz(MainActivity.this, s);
                runOnUiThread(new Runnable() {
                    public void run() {
                        StringBuilder sb = new StringBuilder();
                        sb.append("Bakiye: ").append(Api.ana(s)).append("  (").append(Api.etiket(s)).append(")");
                        sb.append("\nBugün ").append(Api.para(s.gunluk)).append("  ·  Hafta ").append(Api.para(s.haftalik)).append("  ·  Ay ").append(Api.para(s.aylik));
                        if (s.anahtarLimit != null) sb.append("\nAnahtar limiti: ").append(Api.para(s.anahtarLimit));
                        if (s.hata != null) sb.append("\n⚠ ").append(s.hata);
                        sonuc.setText(sb.toString());
                        widgetYenile();
                    }
                });
            }
        }).start();
    }

    void widgetYenile() {
        sendBroadcast(new Intent(this, BalanceWidget.class).setAction(BalanceWidget.ACTION_REFRESH));
    }
}
