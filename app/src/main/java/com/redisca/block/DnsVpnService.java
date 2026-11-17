package com.redisca.block;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.VpnService;
import android.os.Build;
import android.os.ParcelFileDescriptor;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;

public class DnsVpnService extends VpnService {
    static final String STOP = "com.redisca.block.STOP";
    static volatile boolean running = false;
    private ParcelFileDescriptor tunnel;
    private Thread worker;
    private ExecutorService pool;
    private final Set<String> blocked = new HashSet<>();
    private final Object writeLock = new Object();

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && STOP.equals(intent.getAction())) { stopSelf(); return START_NOT_STICKY; }
        if (running) return START_NOT_STICKY;
        NotificationManager nm = getSystemService(NotificationManager.class);
        nm.createNotificationChannel(new NotificationChannel("vpn", "Защита DNS", NotificationManager.IMPORTANCE_LOW));
        PendingIntent open = PendingIntent.getActivity(this, 0, new Intent(this, MainActivity.class), PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        Notification n = new Notification.Builder(this, "vpn").setContentTitle("Redisca_block")
                .setContentText("Фильтрация DNS включена").setSmallIcon(android.R.drawable.ic_lock_lock).setContentIntent(open).setOngoing(true).build();
        startForeground(1, n);
        try {
            try (BufferedReader r = new BufferedReader(new InputStreamReader(getAssets().open("blocked_domains.txt"), StandardCharsets.UTF_8))) {
                String s; while ((s = r.readLine()) != null) if (!s.startsWith("#") && !s.trim().isEmpty()) blocked.add(s.trim());
            }
            Builder b = new Builder().setSession("Redisca_block").setMtu(1500).addAddress("10.111.222.2", 32)
                    .addDnsServer("10.111.222.1").addRoute("10.111.222.1", 32);
            Set<String> apps = Store.selected(this);
            int added = 0;
            for (String app : apps) try { b.addAllowedApplication(app); added++; } catch (PackageManager.NameNotFoundException ignored) { }
            // Never silently broaden a stale selection to all apps.
            if (!apps.isEmpty() && added == 0) throw new IllegalStateException("No selected applications are installed");
            tunnel = b.establish();
            if (tunnel == null) throw new IOException("VPN permission missing");
            running = true;
            pool = Executors.newFixedThreadPool(4);
            worker = new Thread(this::readPackets, "dns-vpn");
            worker.start();
        } catch (Exception e) { android.util.Log.e("Redisca", "VPN start failed", e); stopSelf(); }
        return START_NOT_STICKY;
    }
    private void readPackets() {
        try (FileInputStream in = new FileInputStream(tunnel.getFileDescriptor())) {
            byte[] buf = new byte[32767]; int n;
            while (running && (n = in.read(buf)) > 0) {
                byte[] packet = Arrays.copyOf(buf, n);
                try { pool.execute(() -> handle(packet)); } catch (RejectedExecutionException ignored) { break; }
            }
        } catch (IOException ignored) { } finally { if (running) stopSelf(); }
    }
    private void handle(byte[] p) {
        try {
            if (p.length < 40 || (p[0] >> 4 & 15) != 4 || (p[9] & 255) != 17) return;
            int ip = (p[0] & 15) * 4;
            if (ip < 20 || p.length < ip + 20 || u16(p, ip + 2) != 53 || (u16(p, 6) & 0x3fff) != 0) return;
            int udpLength = u16(p, ip + 4);
            if (udpLength < 20 || ip + udpLength > p.length) return;
            byte[] query = Arrays.copyOfRange(p, ip + 8, ip + udpLength);
            String domain = domain(query);
            if (domain == null) return;
            byte[] response;
            if (isBlocked(domain)) {
                response = query.clone();
                response[2] = (byte) ((response[2] | 0x80) & 0xfd);
                response[3] = (byte) ((response[3] & 0x70) | 0x83); // recursion available, NXDOMAIN
                response[6] = response[7] = response[8] = response[9] = response[10] = response[11] = 0;
                Store.blocked(this, owner(p, ip));
            } else {
                try (DatagramSocket socket = new DatagramSocket()) {
                    if (!protect(socket)) return;
                    socket.setSoTimeout(3000);
                    socket.send(new DatagramPacket(query, query.length, InetAddress.getByName("1.1.1.1"), 53));
                    byte[] data = new byte[4096]; DatagramPacket answer = new DatagramPacket(data, data.length);
                    socket.receive(answer);
                    if (!answer.getAddress().getHostAddress().equals("1.1.1.1") || answer.getPort() != 53 || answer.getLength() < 12 || data[0] != query[0] || data[1] != query[1]) return;
                    response = Arrays.copyOf(data, answer.getLength());
                }
            }
            byte[] out = new byte[20 + 8 + response.length];
            out[0] = 0x45; put16(out, 2, out.length); out[8] = 64; out[9] = 17;
            System.arraycopy(p, 16, out, 12, 4); System.arraycopy(p, 12, out, 16, 4);
            put16(out, 10, checksum(out, 0, 20));
            put16(out, 20, 53); put16(out, 22, u16(p, ip)); put16(out, 24, 8 + response.length);
            System.arraycopy(response, 0, out, 28, response.length);
            // An IPv4 UDP checksum of zero means no checksum.
            synchronized (writeLock) { if (running && tunnel != null) new FileOutputStream(tunnel.getFileDescriptor()).write(out); }
        } catch (Exception e) { android.util.Log.w("Redisca", "DNS packet failed", e); }
    }
    private String owner(byte[] p, int ip) {
        try {
            ConnectivityManager cm = getSystemService(ConnectivityManager.class);
            InetAddress src = InetAddress.getByAddress(Arrays.copyOfRange(p, 12, 16));
            InetAddress dst = InetAddress.getByAddress(Arrays.copyOfRange(p, 16, 20));
            int uid = cm.getConnectionOwnerUid(17, new InetSocketAddress(src, u16(p, ip)), new InetSocketAddress(dst, 53));
            String[] packages = getPackageManager().getPackagesForUid(uid);
            if (packages != null && packages.length == 1) return packages[0];
        } catch (Exception ignored) { }
        return "Неизвестное приложение";
    }
    private boolean isBlocked(String d) {
        while (true) { if (blocked.contains(d)) return true; int dot = d.indexOf('.'); if (dot < 0) return false; d = d.substring(dot + 1); }
    }
    private static String domain(byte[] q) {
        if (q.length < 17 || (q[2] & 0x80) != 0 || u16(q, 4) != 1) return null;
        int i = 12; StringBuilder s = new StringBuilder();
        while (i < q.length) {
            int len = q[i++] & 255;
            if (len == 0) return i + 4 <= q.length ? s.toString().toLowerCase(Locale.ROOT) : null;
            if (len > 63 || i + len > q.length || s.length() + len > 253) return null;
            if (s.length() > 0) s.append('.');
            s.append(new String(q, i, len, StandardCharsets.US_ASCII)); i += len;
        }
        return null;
    }
    private static int u16(byte[] a, int i) { return (a[i] & 255) * 256 + (a[i+1] & 255); }
    private static void put16(byte[] a, int i, int v) { a[i] = (byte)(v >> 8); a[i+1] = (byte)v; }
    private static int checksum(byte[] a, int start, int length) {
        long sum = 0; for (int i = start; i < start + length; i += 2) sum += u16(a, i);
        while ((sum >> 16) != 0) sum = (sum & 65535) + (sum >> 16); return (int)(~sum) & 65535;
    }
    @Override public void onRevoke() { stopSelf(); }
    @Override public void onDestroy() {
        running = false;
        if (pool != null) pool.shutdownNow();
        if (tunnel != null) try { tunnel.close(); } catch (IOException ignored) { }
        tunnel = null; stopForeground(true); super.onDestroy();
    }
}
