package net.hermes.orbakiye;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

/** Widget dokunuşu: tek dokunuş yeniler, 1 sn içinde ikinci dokunuş tam sayfa ayrıntıyı açar. */
public class TapActivity extends Activity {
    static final long CIFT_MS = 1000;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        String tur = getIntent().getStringExtra("tur");
        if (tur == null) tur = "or";
        SharedPreferences p = getSharedPreferences("tap", 0);
        long simdi = System.currentTimeMillis();
        if (simdi - p.getLong(tur, 0) < CIFT_MS) {
            p.edit().putLong(tur, 0).apply();
            Intent i = "hermes".equals(tur) ? new Intent(this, HermesActivity.class)
                : new Intent(this, DetayActivity.class).putExtra("tur", tur);
            startActivity(i);
        } else {
            p.edit().putLong(tur, simdi).apply();
            Intent r;
            if ("hermes".equals(tur)) r = new Intent(this, HermesWidget.class).setAction(HermesWidget.ACTION_REFRESH);
            else if ("codex".equals(tur)) r = new Intent(this, CodexWidget.class).setAction(CodexWidget.ACTION_REFRESH);
            else if ("claude".equals(tur)) r = new Intent(this, ClaudeWidget.class).setAction(ClaudeWidget.ACTION_REFRESH);
            else r = new Intent(this, BalanceWidget.class).setAction(BalanceWidget.ACTION_REFRESH);
            sendBroadcast(r);
        }
        finish();
        overridePendingTransition(0, 0);
    }
}
