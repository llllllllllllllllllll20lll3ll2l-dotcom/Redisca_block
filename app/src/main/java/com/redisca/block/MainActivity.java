package com.redisca.block;

import android.app.*;
import android.content.*;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
import android.net.VpnService;
import android.os.*;
import android.view.*;
import android.widget.*;
import java.util.*;

public class MainActivity extends Activity {
    private static final int VPN_REQUEST = 7;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private LinearLayout root, stats;
    private TextView status, total, selection;
    private Button power;
    private final Runnable refresh = new Runnable() { public void run() { update(); handler.postDelayed(this, 2000); } };
    private final int text = Color.rgb(225, 232, 245), muted = Color.rgb(145, 160, 182), accent = Color.rgb(113, 150, 188);
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(Color.rgb(8, 13, 26));
        getWindow().setNavigationBarColor(Color.rgb(8, 13, 26));
        build();
    }
    private int dp(float n) { return (int)(getResources().getDisplayMetrics().density * n + .5f); }
    private TextView label(String s, int size, int color) { TextView t = new TextView(this); t.setText(s); t.setTextSize(size); t.setTextColor(color); return t; }
    private GradientDrawable background(int color, int stroke) { GradientDrawable d = new GradientDrawable(); d.setColor(color); d.setCornerRadius(dp(18)); if (stroke != 0) d.setStroke(dp(1), stroke); return d; }
    private Button button(String s) {
        Button b = new Button(this); b.setText(s); b.setTextColor(text); b.setAllCaps(false); b.setTextSize(15);
        b.setBackground(background(Color.rgb(28, 43, 64), Color.rgb(65, 86, 110))); return b;
    }
    private LinearLayout card() {
        LinearLayout c = new LinearLayout(this); c.setOrientation(LinearLayout.VERTICAL); c.setPadding(dp(18), dp(16), dp(18), dp(16));
        c.setBackground(background(Color.rgb(17, 25, 40), Color.rgb(37, 51, 70)));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2); lp.bottomMargin = dp(12); root.addView(c, lp); return c;
    }
    private void build() {
        ScrollView scroll = new ScrollView(this); scroll.setFillViewport(true);
        root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setPadding(dp(20), dp(24), dp(20), dp(24));
        root.setBackground(new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, new int[]{Color.rgb(14, 25, 44), Color.rgb(7, 12, 23)}));
        scroll.addView(root); setContentView(scroll);
        TextView title = label("✦   Redisca_block", 24, text); title.setTypeface(null, 1); root.addView(title);
        TextView subtitle = label("ТИХАЯ ЗАЩИТА · ANDROID", 11, muted); subtitle.setPadding(0, dp(5), 0, dp(22)); root.addView(subtitle);
        LinearLayout hero = card(); hero.setGravity(Gravity.CENTER);
        TextView shield = label("✧", 56, accent); shield.setGravity(Gravity.CENTER); hero.addView(shield);
        status = label("", 20, text); status.setGravity(Gravity.CENTER); status.setTypeface(null, 1); hero.addView(status);
        TextView note = label("Локальная DNS-фильтрация", 13, muted); note.setPadding(0, dp(5), 0, dp(14)); hero.addView(note);
        power = button(""); hero.addView(power, new LinearLayout.LayoutParams(-1, dp(48)));
        power.setOnClickListener(v -> { if (DnsVpnService.running) stopService(new Intent(this, DnsVpnService.class)); else requestVpn(); handler.postDelayed(this::update, 400); });
        LinearLayout counter = card(); TextView countTitle = label("ЗАБЛОКИРОВАННЫЕ ЗАПРОСЫ", 11, muted); counter.addView(countTitle);
        total = label("0", 30, text); total.setTypeface(null, 1); counter.addView(total);
        TextView countInfo = label("За всё время · это не число показов рекламы", 12, muted); counter.addView(countInfo);
        LinearLayout rankings = card(); TextView rankTitle = label("Где больше рекламы", 18, text); rankTitle.setTypeface(null, 1); rankings.addView(rankTitle);
        TextView rankNote = label("Обнаруженные заблокированные DNS-запросы", 12, muted); rankNote.setPadding(0, dp(5), 0, dp(10)); rankings.addView(rankNote);
        stats = new LinearLayout(this); stats.setOrientation(LinearLayout.VERTICAL); rankings.addView(stats);
        Button clear = button("Сбросить статистику"); LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(-1, dp(42)); cp.topMargin = dp(12); rankings.addView(clear, cp);
        clear.setOnClickListener(v -> new AlertDialog.Builder(this).setMessage("Удалить всю статистику?").setNegativeButton("Отмена", null).setPositiveButton("Удалить", (d, w) -> {
            android.content.SharedPreferences p = Store.prefs(this); android.content.SharedPreferences.Editor e = p.edit();
            for (String key : p.getAll().keySet()) if (key.startsWith("count:")) e.remove(key); e.apply(); update();
        }).show());
        LinearLayout apps = card(); TextView appTitle = label("Блокировка в приложениях", 18, text); appTitle.setTypeface(null, 1); apps.addView(appTitle);
        selection = label("", 13, muted); selection.setPadding(0, dp(7), 0, dp(14)); apps.addView(selection);
        Button choose = button("Выбрать приложения"); apps.addView(choose, new LinearLayout.LayoutParams(-1, dp(48))); choose.setOnClickListener(v -> chooseApps());
        TextView foot = label("Часть рекламы может обходить DNS-фильтр. Статистика по приложениям не всегда доступна.", 12, muted); foot.setPadding(dp(2), dp(4), dp(2), 0); root.addView(foot);
    }
    private void requestVpn() {
        Intent prepare = VpnService.prepare(this);
        if (prepare != null) startActivityForResult(prepare, VPN_REQUEST); else startProtection();
    }
    @Override protected void onActivityResult(int request, int result, Intent data) {
        super.onActivityResult(request, result, data);
        if (request == VPN_REQUEST && result == RESULT_OK) startProtection();
        else if (request == VPN_REQUEST) Toast.makeText(this, "Для защиты требуется разрешение VPN", Toast.LENGTH_LONG).show();
    }
    private void startProtection() {
        try { startForegroundService(new Intent(this, DnsVpnService.class)); handler.postDelayed(this::update, 500); }
        catch (Exception e) { Toast.makeText(this, "Не удалось запустить VPN: " + e.getMessage(), Toast.LENGTH_LONG).show(); }
    }
    private void chooseApps() {
        PackageManager pm = getPackageManager();
        List<ApplicationInfo> all = pm.getInstalledApplications(0); List<ApplicationInfo> apps = new ArrayList<>();
        for (ApplicationInfo a : all) if (!a.packageName.equals(getPackageName()) && pm.getLaunchIntentForPackage(a.packageName) != null) apps.add(a);
        apps.sort(Comparator.comparing(a -> a.loadLabel(pm).toString().toLowerCase(Locale.ROOT)));
        String[] names = new String[apps.size()]; boolean[] checked = new boolean[apps.size()]; Set<String> chosen = Store.selected(this);
        for (int i = 0; i < apps.size(); i++) { names[i] = apps.get(i).loadLabel(pm).toString() + "\n" + apps.get(i).packageName; checked[i] = chosen.contains(apps.get(i).packageName); }
        new AlertDialog.Builder(this).setTitle("Выберите приложения (пустой список — все)")
            .setMultiChoiceItems(names, checked, (d, which, on) -> { if (on) chosen.add(apps.get(which).packageName); else chosen.remove(apps.get(which).packageName); })
            .setNegativeButton("Отмена", null).setPositiveButton("Сохранить", (d, w) -> {
                Store.selected(this, chosen);
                if (DnsVpnService.running) { stopService(new Intent(this, DnsVpnService.class)); handler.postDelayed(this::requestVpn, 500); }
                update();
            }).show();
    }
    private void update() {
        if (status == null) return;
        boolean on = DnsVpnService.running;
        status.setText(on ? "Защита включена" : "Защита выключена"); power.setText(on ? "Выключить защиту" : "Включить защиту");
        int n = Store.selected(this).size(); selection.setText(n == 0 ? "Все приложения" : "Выбрано приложений: " + n);
        List<Map.Entry<String, Integer>> counts = new ArrayList<>(); int sum = 0;
        for (Map.Entry<String, ?> e : Store.prefs(this).getAll().entrySet()) if (e.getKey().startsWith("count:") && e.getValue() instanceof Integer) {
            int value = (Integer)e.getValue(); sum += value; counts.add(new AbstractMap.SimpleEntry<>(e.getKey().substring(6), value));
        }
        total.setText(String.valueOf(sum)); counts.sort((a, b) -> Integer.compare(b.getValue(), a.getValue())); stats.removeAllViews();
        if (counts.isEmpty()) stats.addView(label("Пока нет заблокированных запросов", 14, muted));
        for (int i = 0; i < Math.min(5, counts.size()); i++) {
            Map.Entry<String, Integer> e = counts.get(i); String name = e.getKey();
            try { name = getPackageManager().getApplicationLabel(getPackageManager().getApplicationInfo(name, 0)).toString(); } catch (PackageManager.NameNotFoundException ignored) { }
            TextView row = label(name + "   ·   " + e.getValue(), 14, text); row.setPadding(0, dp(6), 0, dp(6)); stats.addView(row);
        }
    }
    @Override protected void onResume() { super.onResume(); handler.post(refresh); }
    @Override protected void onPause() { handler.removeCallbacks(refresh); super.onPause(); }
}
