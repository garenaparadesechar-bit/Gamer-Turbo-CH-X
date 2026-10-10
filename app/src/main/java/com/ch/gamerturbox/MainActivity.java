package com.ch.gamerturbox;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
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
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import android.net.Uri;
import java.io.OutputStream;
import java.io.IOException;

import rikka.shizuku.Shizuku;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends Activity {
    private static final int SHIZUKU_REQUEST = 7301;
    private final int bg = Color.rgb(9, 7, 18);
    private final int panel = Color.rgb(25, 18, 43);
    private final int purple = Color.rgb(190, 95, 255);
    private final int white = Color.rgb(248, 244, 255);
    private TextView ramText, batteryText, tempText, shizukuText, statusText;
    private EditText commandInput;
    private EditText fileNameInput, fileContentInput;
    private static final int CREATE_TEXT_FILE = 7412;
    private String pendingFileContent = "";
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private final Runnable monitor = new Runnable() {
        @Override public void run() { updateMonitor(); handler.postDelayed(this, 5000); }
    };
    private final Shizuku.OnRequestPermissionResultListener permissionListener = (requestCode, grantResult) -> {
        if (requestCode != SHIZUKU_REQUEST) return;
        if (grantResult == PackageManager.PERMISSION_GRANTED) {
            refreshShizukuStatus();
            Toast.makeText(this, "Permiso de Shizuku concedido", Toast.LENGTH_SHORT).show();
        } else {
            setStatus("Permiso de Shizuku denegado.");
            new AlertDialog.Builder(this).setTitle("Permiso requerido")
                    .setMessage("Concede el permiso de Gamer Turbo CH X dentro de Shizuku y vuelve a intentarlo.")
                    .setPositiveButton("Entendido", null).show();
        }
    };
    private final Shizuku.OnBinderReceivedListener binderReceivedListener = this::refreshShizukuStatus;
    private final Shizuku.OnBinderDeadListener binderDeadListener = () -> runOnUiThread(() -> {
        if (shizukuText != null) shizukuText.setText("Shizuku: servicio detenido");
    });

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(bg);
        getWindow().setNavigationBarColor(bg);
        Shizuku.addRequestPermissionResultListener(permissionListener);
        Shizuku.addBinderReceivedListenerSticky(binderReceivedListener);
        Shizuku.addBinderDeadListener(binderDeadListener);
        buildUi();
        updateMonitor();
        refreshShizukuStatus();
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
        cover.setImageResource(R.drawable.cover_gamer_turbo);
        cover.setScaleType(ImageView.ScaleType.CENTER_CROP);
        page.addView(cover, new LinearLayout.LayoutParams(-1, dp(220)));

        TextView title = text("GAMER TURBO CH X", 25, purple, true);
        title.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams tp = new LinearLayout.LayoutParams(-1, -2);
        tp.topMargin = dp(14);
        page.addView(title, tp);
        TextView sub = text("GAMING TOOLS  •  SHIZUKU  •  CONTROL", 12, white, true);
        sub.setGravity(Gravity.CENTER);
        page.addView(sub);

        TextView section = section(page, "MONITOR EN VIVO");
        LinearLayout stats = new LinearLayout(this);
        stats.setOrientation(LinearLayout.VERTICAL);
        stats.setPadding(dp(14), dp(12), dp(14), dp(12));
        stats.setBackground(round(panel, 20, purple));
        ramText = text("RAM: consultando…", 15, white, false);
        batteryText = text("Batería: consultando…", 15, white, false);
        tempText = text("Temperatura: consultando…", 15, white, false);
        shizukuText = text("Shizuku: comprobando…", 14, purple, true);
        stats.addView(ramText); stats.addView(batteryText); stats.addView(tempText); stats.addView(shizukuText);
        page.addView(stats);

        section(page, "OPTIMIZACIÓN SEGURA");
        addButton(page, "⚡ ACTUALIZAR MONITOR", v -> updateMonitor());
        addButton(page, "🧠 DIAGNÓSTICO DEL TELÉFONO", v -> showDiagnostics());
        addButton(page, "🧹 LIMPIAR CACHÉ DE ESTA APP", v -> cleanOwnCache());
        addButton(page, "🧹 LIMPIAR CACHÉ DEL SISTEMA (SHIZUKU)", v -> clearSystemCache());
        addButton(page, "🎮 MODO JUEGO: ANIMACIONES 0.5x", v -> setAnimationMode(true));
        addButton(page, "↩️ RESTAURAR ANIMACIONES 1x", v -> setAnimationMode(false));
        addButton(page, "🛑 PAUSAR UNA APP (FORCE-STOP)", v -> chooseAppToStop());

        section(page, "CONSOLA SHIZUKU");
        TextView consoleHint = text("Pega un comando de shell y revisa su salida. Cada ejecución pide confirmación. Los comandos pueden cambiar ajustes o borrar archivos: usa solo comandos que entiendas.", 12, Color.rgb(190, 177, 210), false);
        consoleHint.setPadding(dp(2), 0, dp(2), dp(8));
        page.addView(consoleHint);
        commandInput = new EditText(this);
        commandInput.setTextColor(white);
        commandInput.setHintTextColor(Color.rgb(160, 145, 180));
        commandInput.setHint("Ejemplo: id; getprop ro.build.version.release");
        commandInput.setTextSize(13);
        commandInput.setGravity(Gravity.TOP | Gravity.START);
        commandInput.setMinLines(3);
        commandInput.setMaxLines(7);
        commandInput.setPadding(dp(12), dp(10), dp(12), dp(10));
        commandInput.setBackground(round(panel, 14, purple));
        page.addView(commandInput, new LinearLayout.LayoutParams(-1, -2));
        addButton(page, "▶ EJECUTAR COMANDO CON SHIZUKU", v -> executeConsoleCommand());
        addButton(page, "📄 CREAR ARCHIVO DE TEXTO / CONFIGURACIÓN", v -> showCreateTextFile());
        addButton(page, "ℹ️ EJEMPLOS DE COMANDOS SEGUROS", v -> showCommandExamples());

        section(page, "CENTRO DE JUEGOS");
        addButton(page, "🔥 ABRIR FREE FIRE", v -> launchGame("com.dts.freefireth"));
        addButton(page, "💜 ABRIR FREE FIRE MAX", v -> launchGame("com.dts.freefiremax"));
        addButton(page, "🔐 CONECTAR / PEDIR PERMISO SHIZUKU", v -> requestShizuku());
        addButton(page, "🖥️ AJUSTES DE PANTALLA", v -> openDisplaySettings());
        addButton(page, "⚙️ AJUSTES DE ESTA APP", v -> openAppSettings());

        statusText = text("Estado: listo", 13, Color.LTGRAY, false);
        statusText.setPadding(dp(4), dp(14), dp(4), 0);
        page.addView(statusText);
        TextView disclaimer = text("Las operaciones del sistema requieren Shizuku activo y permiso concedido. La limpieza usa el administrador de caché de Android; no borra fotos, cuentas ni datos de juegos. No se promete aumento de FPS ni enfriamiento físico.", 11, Color.rgb(180,165,200), false);
        disclaimer.setPadding(dp(4), dp(14), dp(4), 0);
        page.addView(disclaimer);
    }

    private TextView section(LinearLayout page, String label) {
        TextView t = text(label, 17, white, true);
        t.setPadding(0, dp(22), 0, dp(8)); page.addView(t); return t;
    }

    private void addButton(LinearLayout parent, String label, View.OnClickListener listener) {
        Button b = new Button(this);
        b.setText(label); b.setTextColor(white); b.setAllCaps(false); b.setTextSize(13);
        b.setTypeface(Typeface.DEFAULT, Typeface.BOLD); b.setBackground(round(panel, 16, purple));
        b.setPadding(dp(10), dp(10), dp(10), dp(10));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2); lp.bottomMargin = dp(9); parent.addView(b, lp);
        b.setOnClickListener(v -> { b.setAlpha(0.72f); b.postDelayed(() -> b.setAlpha(1f), 120); listener.onClick(v); });
    }

    private TextView text(String s, int size, int color, boolean bold) {
        TextView t = new TextView(this); t.setText(s); t.setTextSize(size); t.setTextColor(color);
        if (bold) t.setTypeface(Typeface.DEFAULT, Typeface.BOLD); return t;
    }
    private GradientDrawable round(int color, int radius, int strokeColor) {
        GradientDrawable d = new GradientDrawable(); d.setColor(color); d.setCornerRadius(dp(radius)); d.setStroke(dp(1), strokeColor); return d;
    }
    private int dp(float v) { return (int)(v * getResources().getDisplayMetrics().density + 0.5f); }

    private void updateMonitor() {
        try {
            ActivityManager am = (ActivityManager)getSystemService(ACTIVITY_SERVICE);
            ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo(); am.getMemoryInfo(mi);
            ramText.setText(String.format(Locale.getDefault(), "RAM disponible: %.2f GB / %.2f GB", mi.availMem / 1073741824.0, mi.totalMem / 1073741824.0));
            Intent i = registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
            int level = i == null ? -1 : i.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
            int scale = i == null ? -1 : i.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
            int temp = i == null ? -1 : i.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1);
            batteryText.setText("Batería: " + ((level >= 0 && scale > 0) ? (level * 100 / scale) + "%" : "N/D"));
            tempText.setText("Temperatura de batería: " + (temp >= 0 ? String.format(Locale.getDefault(), "%.1f °C", temp / 10f) : "no disponible"));
        } catch (Exception e) { setStatus("No se pudo actualizar el monitor: " + e.getClass().getSimpleName()); }
    }

    private void showDiagnostics() {
        ActivityManager am = (ActivityManager)getSystemService(ACTIVITY_SERVICE);
        ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo(); am.getMemoryInfo(mi);
        Intent i = registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        int temp = i == null ? -1 : i.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1);
        StringBuilder s = new StringBuilder("RAM disponible: ").append(String.format(Locale.getDefault(), "%.2f GB", mi.availMem / 1073741824.0)).append("\n");
        s.append(mi.lowMemory ? "Android indica presión de memoria. Cierra apps que no uses.\n" : "Android no indica presión crítica de memoria.\n");
        if (temp >= 0) { float c = temp / 10f; s.append(String.format(Locale.getDefault(), "Temperatura de batería: %.1f °C\n", c)); if (c >= 42) s.append("Pausa el juego y deja enfriar el teléfono; evita jugar mientras carga.\n"); else if (c >= 38) s.append("Vigila el calentamiento y reduce la carga si aumenta.\n"); else s.append("Sin alerta térmica por el umbral de batería configurado.\n"); }
        else s.append("Android no proporcionó temperatura de batería.\n");
        s.append("\nEl diagnóstico informa; no controla directamente CPU/GPU ni garantiza FPS.");
        new AlertDialog.Builder(this).setTitle("Diagnóstico Gamer Turbo").setMessage(s.toString()).setPositiveButton("OK", null).show();
        setStatus("Diagnóstico ejecutado.");
    }

    private void cleanOwnCache() {
        long before = folderSize(getCacheDir()); boolean ok = deleteContents(getCacheDir()); long freed = Math.max(0, before - folderSize(getCacheDir()));
        String msg = (ok ? "Caché propia limpiada." : "Limpieza parcial.") + "\nEspacio liberado: " + formatSize(freed) + "\nNo se borraron datos ni caché de otros juegos.";
        new AlertDialog.Builder(this).setTitle("Limpieza de esta app").setMessage(msg).setPositiveButton("OK", null).show(); setStatus(msg.replace('\n', ' '));
    }
    private boolean deleteContents(java.io.File dir) { java.io.File[] files = dir.listFiles(); if (files == null) return true; boolean ok = true; for (java.io.File f : files) { if (f.isDirectory()) ok &= deleteContents(f); if (!f.delete() && f.exists()) ok = false; } return ok; }
    private long folderSize(java.io.File dir) { long n = 0; java.io.File[] files = dir.listFiles(); if (files != null) for (java.io.File f : files) n += f.isDirectory() ? folderSize(f) : f.length(); return n; }
    private String formatSize(long n) { if (n < 1024) return n + " B"; if (n < 1048576) return String.format(Locale.getDefault(), "%.1f KB", n / 1024.0); return String.format(Locale.getDefault(), "%.2f MB", n / 1048576.0); }

    private boolean shizukuGranted() {
        try { return Shizuku.pingBinder() && Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED; }
        catch (Exception e) { return false; }
    }
    private void refreshShizukuStatus() {
        if (shizukuText == null) return;
        runOnUiThread(() -> {
            if (!Shizuku.pingBinder()) shizukuText.setText("Shizuku: no conectado (abre Shizuku y activa el servicio)");
            else if (shizukuGranted()) shizukuText.setText("Shizuku: conectado y autorizado ✓");
            else shizukuText.setText("Shizuku: conectado, falta conceder permiso");
        });
    }
    private void requestShizuku() {
        if (!Shizuku.pingBinder()) {
            new AlertDialog.Builder(this).setTitle("Shizuku no está activo")
                    .setMessage("Instala/abre Shizuku, inicia el servicio mediante Depuración inalámbrica o ADB, y vuelve aquí. Gamer Turbo no puede activar Shizuku por sí solo.")
                    .setPositiveButton("Abrir Shizuku", (d, w) -> { try { startActivity(getPackageManager().getLaunchIntentForPackage("moe.shizuku.privileged.api")); } catch (Exception ignored) {} })
                    .setNegativeButton("Cerrar", null).show(); return;
        }
        if (shizukuGranted()) { refreshShizukuStatus(); new AlertDialog.Builder(this).setTitle("Shizuku listo").setMessage("Permiso concedido. Las herramientas del sistema están disponibles.").setPositiveButton("OK", null).show(); return; }
        try { Shizuku.requestPermission(SHIZUKU_REQUEST); } catch (Exception e) { setStatus("No se pudo solicitar permiso Shizuku: " + e.getMessage()); }
    }

    private void runShell(String command, String successTitle, String description) {
        if (!Shizuku.pingBinder()) { requestShizuku(); return; }
        if (!shizukuGranted()) { requestShizuku(); return; }
        setStatus("Ejecutando operación mediante Shizuku…");
        worker.execute(() -> {
            String output = ""; int exit = -1;
            try {
                Process p = Shizuku.newProcess(new String[]{"sh", "-c", command + " 2>&1"}, null, null);
                p.getOutputStream().close();
                StringBuilder b = new StringBuilder();
                // Merge stderr into stdout so a full error pipe cannot deadlock the process.
                BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream())); String line;
                while ((line = reader.readLine()) != null) { if (b.length() < 6000) b.append(line).append('\n'); }
                exit = p.waitFor(); output = b.toString().trim(); p.destroy();
            } catch (Exception e) { output = e.getClass().getSimpleName() + ": " + e.getMessage(); }
            final int result = exit; final String resultText = output;
            runOnUiThread(() -> {
                String msg = description + "\n\n" + (result == 0 ? "Comando completado." : "El comando terminó con código " + result + ".") + (resultText.isEmpty() ? "" : "\n\n" + resultText);
                new AlertDialog.Builder(this).setTitle(successTitle).setMessage(msg).setPositiveButton("OK", null).show();
                setStatus(result == 0 ? successTitle + " completado." : "Operación con resultado " + result + ".");
            });
        });
    }

    private void clearSystemCache() {
        new AlertDialog.Builder(this).setTitle("Limpiar caché de Android")
                .setMessage("Usa el administrador de paquetes de Android para intentar reducir cachés de aplicaciones. No borra fotos, cuentas ni datos de usuario; Android decide qué cachés puede eliminar. Requiere Shizuku activo y autorizado. ¿Continuar?")
                .setNegativeButton("Cancelar", null).setPositiveButton("Limpiar", (d, w) -> runShell("pm trim-caches 1G", "Limpieza de caché del sistema", "Android recibió la solicitud para reducir las cachés. El espacio liberado depende del sistema." )).show();
    }
    private void setAnimationMode(boolean gaming) {
        String value = gaming ? "0.5" : "1.0";
        String command = "settings put global window_animation_scale " + value + " && settings put global transition_animation_scale " + value + " && settings put global animator_duration_scale " + value;
        String desc = gaming ? "Se solicitaron escalas de animación de 0.5x para que las transiciones se sientan más rápidas. Esto no aumenta directamente los FPS." : "Se solicitaron escalas de animación de 1x (predeterminadas).";
        new AlertDialog.Builder(this).setTitle(gaming ? "Modo de animaciones 0.5x" : "Restaurar animaciones")
                .setMessage(desc + "\n\n¿Aplicar mediante Shizuku?").setNegativeButton("Cancelar", null)
                .setPositiveButton("Aplicar", (d, w) -> runShell(command, gaming ? "Modo de juego" : "Animaciones restauradas", desc)).show();
    }

    private void executeConsoleCommand() {
        if (commandInput == null) return;
        String command = commandInput.getText().toString().trim();
        if (command.isEmpty()) { commandInput.setError("Escribe o pega un comando"); return; }
        if (command.length() > 2000) { commandInput.setError("Máximo 2000 caracteres por comando"); return; }
        new AlertDialog.Builder(this).setTitle("Confirmar comando")
                .setMessage("Se ejecutará este comando con los privilegios que Shizuku conceda:\n\n" + command + "\n\nUn comando incorrecto puede cambiar el sistema o eliminar archivos. ¿Ejecutarlo?")
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Ejecutar", (d, w) -> runShell(command, "Consola Shizuku", "Comando ejecutado: " + command))
                .show();
    }

    private void showCommandExamples() {
        String examples = "Comandos informativos:\n" +
                "id\ngetprop ro.build.version.release\ngetprop ro.product.model\n" +
                "df -h /data\nfree\nsettings get global window_animation_scale\n\n" +
                "Para crear carpetas/archivos con shell, las rutas permitidas dependen de Android y de Shizuku. No pegues comandos de desconocidos ni comandos de borrado masivo.";
        new AlertDialog.Builder(this).setTitle("Ejemplos de comandos").setMessage(examples)
                .setPositiveButton("Poner ejemplo", (d, w) -> { if (commandInput != null) commandInput.setText("id; getprop ro.build.version.release; getprop ro.product.model"); })
                .setNegativeButton("Cerrar", null).show();
    }

    private void showCreateTextFile() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(8), dp(4), dp(8), 0);
        fileNameInput = new EditText(this);
        fileNameInput.setSingleLine(true);
        fileNameInput.setHint("Nombre, por ejemplo ajustes.txt");
        fileNameInput.setTextColor(white);
        fileNameInput.setHintTextColor(Color.GRAY);
        fileNameInput.setBackground(round(panel, 12, purple));
        box.addView(fileNameInput);
        fileContentInput = new EditText(this);
        fileContentInput.setHint("Contenido del archivo o configuración");
        fileContentInput.setTextColor(white);
        fileContentInput.setHintTextColor(Color.GRAY);
        fileContentInput.setGravity(Gravity.TOP | Gravity.START);
        fileContentInput.setMinLines(5);
        fileContentInput.setBackground(round(panel, 12, purple));
        LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(-1, -2); cp.topMargin = dp(10); box.addView(fileContentInput, cp);
        new AlertDialog.Builder(this).setTitle("Crear archivo de texto")
                .setMessage("El archivo se guardará donde tú elijas mediante el selector de Android.")
                .setView(box).setNegativeButton("Cancelar", null)
                .setPositiveButton("Elegir ubicación", (d, w) -> {
                    String name = fileNameInput.getText().toString().trim();
                    if (name.isEmpty()) name = "gamer-turbo-config.txt";
                    name = name.replaceAll("[/\\\\:*?\"<>|]", "_");
                    pendingFileContent = fileContentInput.getText().toString();
                    Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
                    intent.addCategory(Intent.CATEGORY_OPENABLE);
                    intent.setType("text/plain");
                    intent.putExtra(Intent.EXTRA_TITLE, name);
                    try { startActivityForResult(intent, CREATE_TEXT_FILE); }
                    catch (Exception e) { setStatus("No se pudo abrir el selector de archivos: " + e.getMessage()); }
                }).show();
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != CREATE_TEXT_FILE || resultCode != RESULT_OK || data == null) return;
        Uri uri = data.getData();
        if (uri == null) { setStatus("No se recibió ubicación para el archivo."); return; }
        try (OutputStream out = getContentResolver().openOutputStream(uri, "wt")) {
            if (out == null) throw new IOException("No se pudo abrir el archivo");
            out.write(pendingFileContent.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            out.flush();
            setStatus("Archivo creado correctamente: " + uri.getLastPathSegment());
            Toast.makeText(this, "Archivo creado", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            setStatus("Error al crear archivo: " + e.getClass().getSimpleName() + ": " + e.getMessage());
            new AlertDialog.Builder(this).setTitle("No se pudo guardar")
                    .setMessage(e.getClass().getSimpleName() + ": " + e.getMessage()).setPositiveButton("OK", null).show();
        }
    }

    private void chooseAppToStop() {
        if (!Shizuku.pingBinder() || !shizukuGranted()) { requestShizuku(); return; }
        List<ApplicationInfo> apps = new ArrayList<>();
        PackageManager pm = getPackageManager();
        for (ApplicationInfo info : pm.getInstalledApplications(0)) {
            if (info.packageName.equals(getPackageName())) continue;
            if (pm.getLaunchIntentForPackage(info.packageName) != null) apps.add(info);
        }
        Collections.sort(apps, Comparator.comparing(a -> pm.getApplicationLabel(a).toString().toLowerCase(Locale.ROOT)));
        if (apps.isEmpty()) { setStatus("No se encontraron apps que se puedan abrir."); return; }
        String[] labels = new String[apps.size()];
        for (int i = 0; i < apps.size(); i++) labels[i] = pm.getApplicationLabel(apps.get(i)) + "\n" + apps.get(i).packageName;
        new AlertDialog.Builder(this).setTitle("Selecciona una app para detener")
                .setItems(labels, (d, which) -> {
                    ApplicationInfo chosen = apps.get(which); String pkg = chosen.packageName;
                    new AlertDialog.Builder(this).setTitle("Detener aplicación")
                            .setMessage("Se forzará la detención de " + pm.getApplicationLabel(chosen) + ". Puede perder trabajo no guardado y las notificaciones se pausarán hasta que la app se abra otra vez. No borra sus datos. ¿Continuar?")
                            .setNegativeButton("Cancelar", null).setPositiveButton("Detener", (dd, ww) -> runShell("am force-stop --user 0 " + pkg, "Detener aplicación", "Solicitud de cierre forzado para " + pkg + ". No se borran datos.")).show();
                }).setNegativeButton("Cancelar", null).show();
    }

    private void launchGame(String pkg) {
        try { Intent launch = getPackageManager().getLaunchIntentForPackage(pkg); if (launch == null) { setStatus("El juego no está instalado o no está visible."); Toast.makeText(this, "No se encontró el juego instalado.", Toast.LENGTH_LONG).show(); return; } startActivity(launch); setStatus("Juego abierto: " + pkg); }
        catch (Exception e) { setStatus("No se pudo abrir el juego: " + e.getClass().getSimpleName()); }
    }
    private void openDisplaySettings() { try { startActivity(new Intent(Settings.ACTION_DISPLAY_SETTINGS)); } catch (Exception e) { setStatus("No se pudieron abrir los ajustes de pantalla."); } }
    private void openAppSettings() { try { Intent i = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS); i.setData(android.net.Uri.parse("package:" + getPackageName())); startActivity(i); } catch (Exception e) { setStatus("No se pudieron abrir los ajustes."); } }
    private void setStatus(String s) { runOnUiThread(() -> { if (statusText != null) statusText.setText("Estado: " + s); }); }

    @Override protected void onResume() { super.onResume(); handler.removeCallbacks(monitor); handler.post(monitor); refreshShizukuStatus(); }
    @Override protected void onPause() { super.onPause(); handler.removeCallbacks(monitor); }
    @Override protected void onDestroy() {
        super.onDestroy(); handler.removeCallbacks(monitor); worker.shutdownNow();
        Shizuku.removeRequestPermissionResultListener(permissionListener);
        Shizuku.removeBinderReceivedListener(binderReceivedListener);
        Shizuku.removeBinderDeadListener(binderDeadListener);
    }
}
