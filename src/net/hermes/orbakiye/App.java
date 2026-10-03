package net.hermes.orbakiye;

/** Her süreç başlangıcında (widget, servis, ekran) uygulama bağlamını Sunucu sınıfına verir. */
public class App extends android.app.Application {
    @Override
    public void onCreate() {
        super.onCreate();
        Sunucu.ctx = getApplicationContext();
    }
}
