package net.hermes.orbakiye;

import android.content.Context;
import android.graphics.Color;

/** Widget temalari. Otomatik modda her DEGISIM_MS'de bir sonraki temaya gecer. */
public class Tema {
    static final long DEGISIM_MS = 120000L; // artik kullanilmiyor (tema saate gore)

    final String ad;
    final int bg, chip;
    final int baslik, aciklama, chipYazi, guncel, tamam, orta, dusuk, ayar;
    final boolean mono;
    final String baslikMetin;

    Tema(String ad, int bg, int chip, String baslikMetin, String baslik, String aciklama, String chipYazi,
         String guncel, String tamam, String orta, String dusuk, boolean mono) {
        this.ad = ad;
        this.bg = bg;
        this.chip = chip;
        this.baslikMetin = baslikMetin;
        this.baslik = Color.parseColor(baslik);
        this.ayar = Color.parseColor(baslik);
        this.aciklama = Color.parseColor(aciklama);
        this.chipYazi = Color.parseColor(chipYazi);
        this.guncel = Color.parseColor(guncel);
        this.tamam = Color.parseColor(tamam);
        this.orta = Color.parseColor(orta);
        this.dusuk = Color.parseColor(dusuk);
        this.mono = mono;
    }

    static final Tema[] TEMALAR = new Tema[] {
        new Tema("Aurora", R.drawable.bg_aurora, R.drawable.chip_bg, "◉  OPENROUTER",
            "#C4B5FD", "#E0E7FF", "#FFFFFF", "#A5B4FC", "#34D399", "#FBBF24", "#F87171", false),
        new Tema("Cyberpunk", R.drawable.bg_cyber, R.drawable.chip_cyber, "▌ NIGHT CITY // OR.BAL",
            "#FCEE0A", "#00F0FF", "#FCEE0A", "#FF2A6D", "#00F0FF", "#FCEE0A", "#FF2A6D", true),
        new Tema("Okyanus", R.drawable.bg_ocean, R.drawable.chip_bg, "≋  OPENROUTER",
            "#5EEAD4", "#CCFBF1", "#FFFFFF", "#99F6E4", "#5EEAD4", "#FDE047", "#FB7185", false),
        new Tema("Gün Batımı", R.drawable.bg_sunset, R.drawable.chip_bg, "☀  OPENROUTER",
            "#FED7AA", "#FFEDD5", "#FFFFFF", "#FDBA74", "#A7F3D0", "#FDE68A", "#FCA5A5", false),
        new Tema("Matrix", R.drawable.bg_matrix, R.drawable.chip_matrix, "> OPENROUTER_",
            "#00FF41", "#4ADE80", "#00FF41", "#15A340", "#00FF41", "#A3E635", "#FF3B30", true),
        new Tema("Buz", R.drawable.bg_ice, R.drawable.chip_ice, "❄  OPENROUTER",
            "#075985", "#0C4A6E", "#0C4A6E", "#0369A1", "#047857", "#B45309", "#B91C1C", false),
        new Tema("Grafit", R.drawable.bg_grafit, R.drawable.chip_grafit, "OPENROUTER",
            "#7DD3FC", "#E5E7EB", "#FFFFFF", "#9CA3AF", "#4ADE80", "#FACC15", "#F87171", false),
        new Tema("Obsidyen", R.drawable.bg_obsidyen, R.drawable.chip_obsidyen, "OPENROUTER",
            "#FBBF24", "#E2E8F0", "#FFFFFF", "#94A3B8", "#34D399", "#FBBF24", "#F87171", false)
    };

    /** -1 = otomatik, 0..N-1 = sabit tema */
    static int mod(Context c) {
        return c.getSharedPreferences(Api.PREFS, 0).getInt("tema_mod", -1);
    }

    static void modAyarla(Context c, int m) {
        c.getSharedPreferences(Api.PREFS, 0).edit().putInt("tema_mod", m).apply();
    }

    static int sonrakiMod(Context c) {
        int m = mod(c) + 1;
        if (m >= TEMALAR.length) m = -1;
        modAyarla(c, m);
        return m;
    }

    static Tema mevcut(Context c) {
        int m = mod(c);
        if (m >= 0 && m < TEMALAR.length) return TEMALAR[m];
        // iki koyu tema, saatte bir sırayla: çift saat Grafit, tek saat Obsidyen
        int h = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY);
        return TEMALAR[h % 2 == 0 ? 6 : 7];
    }

    static String modAdi(Context c) {
        int m = mod(c);
        return m < 0 ? "Otomatik (saatte bir Grafit / Obsidyen)" : TEMALAR[m].ad;
    }
}
