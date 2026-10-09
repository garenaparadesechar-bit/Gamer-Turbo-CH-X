package com.ch.gamerturbox;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.Locale;

public class MainActivity extends Activity {
    private final int bg = Color.rgb(10, 8, 20);
    private final int panel = Color.rgb(25, 18, 43);
    private final int purple = Color.rgb(190, 95, 255);
    private final int white = Color.rgb(248, 244, 255);
    private TextView ramText, batteryText, tempText, statusText;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable monitor = new Runnable() {
        @Override public void run() {
            updateMonitor();
            handler.postDelayed(this, 5000);
        }
    };

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(bg);
        getWindow().setNavigationBarColor(bg);
        buildUi();
        updateMonitor();
    }

    private void buildUi() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(dp(16), dp(14), dp(16), dp(28));
        page.setBackgroundColor(bg);
        scroll.addView(page);
        setContentView(scroll);

        ImageView cover = new ImageView(this);
        cover.setImageResource(com.ch.gamerturbox.R.drawable.cover_gamer_turbo);
        cover.setScaleType(ImageView.ScaleType.CENTER_CROP);
        page.addView(cover, new LinearLayout.LayoutParams(-1, dp(235)));

        TextView title = text("GAMER TURBO CH X", 25, purple, true);
        title.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams tp = new LinearLayout.LayoutParams(-1, -2);
        tp.topMargin = dp(14);
        page.addView(title, tp);
        TextView sub = text("CONTROL  •  MONITOR  •  JUEGA", 12, white, true);
        sub.setGravity(Gravity.CENTER);
        page.addView(sub);

        TextView section = text("MONITOR DEL TELÉFONO", 17, white, true);
        section.setPadding(0, dp(22), 0, dp(8));
        page.addView(section);

        LinearLayout stats = new LinearLayout(this);
        stats.setOrientation(LinearLayout.VERTICAL);
        stats.setPadding(dp(14), dp(12), dp(14), dp(12));
        stats.setBackground(round(panel, 20, purple));
        ramText = text("RAM disponible: consultando…", 15, white, false);
        batteryText = text("Batería: consultando…", 15, white, false);
        tempText = text("Temperatura batería: consultando…", 15, white, false);
        stats.addView(ramText); stats.addView(batteryText); stats.addView(tempText);
        page.addView(stats);

        section = text("HERRAMIENTAS", 17, white, true);
        section.setPadding(0, dp(22), 0, dp(8));
        page.addView(section);
        addButton(page, "⚡ ACTUALIZAR MONITOR", v -> updateMonitor());
        addButton(page, "🧠 DIAGNÓSTICO DEL TELÉFONO", v -> showDiagnostics());
        addButton(page, "🧹 LIMPIAR CACHÉ DE ESTA APP", v -> cleanOwnCache());
        addButton(page, "🎮 ABRIR FREE FIRE", v -> launchGame("com.dts.freefireth"));
        addButton(page, "💜 ABRIR FREE FIRE MAX", v -> launchGame("com.dts.freefiremax"));
        addButton(page, "🔐 ESTADO DE SHIZUKU", v -> showShizukuStatus());
        addButton(page, "⚙️ AJUSTES DE ESTA APP", v -> openAppSettings());

        statusText = text("Listo. Las acciones mostrarán su resultado.", 13, Color.LTGRAY, false);
        statusText.setPadding(dp(4), dp(14), dp(4), 0);
        page.addView(statusText);
        TextView disclaimer = text("Las funciones respetan los permisos de Android. No se garantiza un aumento de FPS ni control directo del procesador.", 11, Color.rgb(180,165,200), false);
        disclaimer.setPadding(dp(4), dp(14), dp(4), 0);
        page.addView(disclaimer);
    }

    private void addButton(LinearLayout parent, String label, View.OnClickListener listener) {
        Button b = new Button(this);
        b.setText(label);
        b.setTextColor(white);
        b.setAllCaps(false);
        b.setTextSize(14);
        b.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        b.setBackground(round(panel, 16, purple));
        b.setPadding(dp(10), dp(10), dp(10), dp(10));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.bottomMargin = dp(9);
        parent.addView(b, lp);
        b.setOnClickListener(v -> {
            b.setAlpha(0.72f);
            b.postDelayed(() -> b.setAlpha(1f), 120);
            listener.onClick(v);
        });
    }

    private TextView text(String s, int size, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(s); t.setTextSize(size); t.setTextColor(color);
        if (bold) t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return t;
    }

    private GradientDrawable round(int color, int radius, int strokeColor) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color); d.setCornerRadius(dp(radius));
        d.setStroke(dp(1), strokeColor);
        return d;
    }

    private int dp(float v) { return (int)(v * getResources().getDisplayMetrics().density + 0.5f); }

    private void updateMonitor() {
        try {
            ActivityManager am = (ActivityManager)getSystemService(ACTIVITY_SERVICE);
            ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
            am.getMemoryInfo(mi);
            ramText.setText(String.format(Locale.getDefault(), "RAM disponible: %.2f GB de %.2f GB",
                    mi.availMem / 1073741824.0, mi.totalMem / 1073741824.0));

            Intent i = registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
            int level = i == null ? -1 : i.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
            int scale = i == null ? -1 : i.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
            int t = i == null ? -1 : i.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1);
            batteryText.setText("Batería: " + ((level >= 0 && scale > 0) ? (level * 100 / scale) + "%" : "N/D"));
            tempText.setText("Temperatura de batería: " + (t >= 0 ? String.format(Locale.getDefault(), "%.1f °C", t / 10f) : "no disponible"));
            setStatus("Monitor actualizado.");
        } catch (Exception e) {
            setStatus("No se pudo actualizar el monitor: " + e.getClass().getSimpleName());
        }
    }

    private void showDiagnostics() {
        ActivityManager am = (ActivityManager)getSystemService(ACTIVITY_SERVICE);
        ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
        am.getMemoryInfo(mi);
        Intent i = registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        int t = i == null ? -1 : i.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1);
        StringBuilder s = new StringBuilder();
        s.append("DIAGNÓSTICO\n\nRAM disponible: ").append(String.format(Locale.getDefault(), "%.2f GB", mi.availMem / 1073741824.0)).append("\n");
        s.append(mi.lowMemory ? "Android indica presión de memoria. Cierra apps que no uses.\n" : "Android no indica presión crítica de memoria.\n");
        if (t >= 0) {
            float c = t / 10f;
            s.append(String.format(Locale.getDefault(), "Temperatura de batería: %.1f °C\n", c));
            if (c >= 42) s.append("Aviso: pausa el juego y deja enfriar el teléfono; evita jugar mientras carga.\n");
            else if (c >= 38) s.append("Consejo: vigila el calentamiento y reduce la carga del teléfono si aumenta.\n");
            else s.append("No se activa la alerta térmica configurada.\n");
        } else s.append("Android no proporcionó la temperatura de batería.\n");
        s.append("\nEste diagnóstico no controla la CPU/GPU ni promete más FPS.");
        new AlertDialog.Builder(this).setTitle("Gamer Turbo CH X").setMessage(s.toString()).setPositiveButton("Entendido", null).show();
        setStatus("Diagnóstico ejecutado.");
    }

    private void cleanOwnCache() {
        long before = folderSize(getCacheDir());
        boolean ok = deleteContents(getCacheDir());
        long after = folderSize(getCacheDir());
        long freed = Math.max(0, before - after);
        String msg = ok ? "Limpieza de caché propia completada.\nEspacio liberado: " + formatSize(freed)
                : "Limpieza parcial: Android o algún archivo impidió borrar parte de la caché.\nEspacio liberado: " + formatSize(freed);
        new AlertDialog.Builder(this).setTitle("Limpieza segura").setMessage(msg + "\n\nNo se tocaron los datos de Free Fire.").setPositiveButton("OK", null).show();
        setStatus(msg);
    }

    private boolean deleteContents(File dir) {
        File[] files = dir.listFiles();
        if (files == null) return true;
        boolean ok = true;
        for (File f : files) {
            if (f.isDirectory()) ok &= deleteContents(f);
            if (!f.delete() && f.exists()) ok = false;
        }
        return ok;
    }

    private long folderSize(File dir) {
        long total = 0;
        File[] files = dir.listFiles();
        if (files == null) return 0;
        for (File f : files) total += f.isDirectory() ? folderSize(f) : f.length();
        return total;
    }

    private String formatSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1048576) return String.format(Locale.getDefault(), "%.1f KB", bytes / 1024.0);
        return String.format(Locale.getDefault(), "%.2f MB", bytes / 1048576.0);
    }

    private void launchGame(String pkg) {
        try {
            Intent launch = getPackageManager().getLaunchIntentForPackage(pkg);
            if (launch == null) {
                setStatus("El juego no está instalado o Android no permite abrirlo.");
                Toast.makeText(this, "No se encontró el juego instalado.", Toast.LENGTH_LONG).show();
                return;
            }
            startActivity(launch);
            setStatus("Juego abierto: " + pkg);
        } catch (Exception e) {
            setStatus("No se pudo abrir el juego: " + e.getClass().getSimpleName());
        }
    }

    private void showShizukuStatus() {
        boolean installed;
        try {
            getPackageManager().getPackageInfo("moe.shizuku.privileged.api", 0);
            installed = true;
        } catch (PackageManager.NameNotFoundException e) {
            installed = false;
        }
        String message = installed
                ? "Shizuku está instalado. Esta versión beta todavía no ejecuta operaciones privilegiadas mediante su API; no se afirmará que tiene autorización sin comprobarla."
                : "Shizuku no está instalado. Puedes instalarlo por separado, pero esta beta no realiza operaciones privilegiadas.";
        new AlertDialog.Builder(this).setTitle("Integración Shizuku").setMessage(message).setPositiveButton("OK", null).show();
        setStatus(installed ? "Shizuku detectado; API privilegiada aún pendiente." : "Shizuku no detectado.");
    }

    private void openAppSettings() {
        try {
            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            intent.setData(android.net.Uri.parse("package:" + getPackageName()));
            startActivity(intent);
        } catch (Exception e) {
            setStatus("No se pudieron abrir los ajustes.");
        }
    }

    private void setStatus(String s) {
        if (statusText != null) statusText.setText("Estado: " + s);
    }

    @Override protected void onResume() { super.onResume(); handler.post(monitor); }
    @Override protected void onPause() { super.onPause(); handler.removeCallbacks(monitor); }
}
