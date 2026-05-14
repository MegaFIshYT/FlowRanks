package gg.flowpvp.client.api;

import com.mojang.blaze3d.platform.NativeImage;
import gg.flowpvp.client.FlowPvPClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public final class SkinHeadCache {
    private static final Map<String, Entry> ENTRIES = new ConcurrentHashMap<>();
    private static volatile Path cacheDir;

    private SkinHeadCache() {
    }

    public static Identifier texture(String uuid) {
        String normalized = uuid == null ? "" : uuid.replace("-", "").toLowerCase(Locale.ROOT);
        if (normalized.isBlank()) {
            return null;
        }

        Entry entry = ENTRIES.computeIfAbsent(normalized, key -> {
            Entry created = new Entry(FlowPvPClient.id("heads/" + key));
            load(key, uuid, created);
            return created;
        });

        if (entry.ready) {
            return entry.id;
        }
        if (entry.failedAt > 0 && System.currentTimeMillis() - entry.failedAt > 30_000L) {
            ENTRIES.remove(normalized, entry);
        }
        return null;
    }

    private static void load(String normalizedKey, String uuid, Entry entry) {
        CompletableFuture.runAsync(() -> {
            try {
                NativeImage image = loadFromDisk(normalizedKey);
                if (image == null) {
                    image = downloadAndCache(normalizedKey, uuid);
                }
                if (image == null) {
                    entry.failedAt = System.currentTimeMillis();
                    return;
                }
                NativeImage finalImage = image;
                Minecraft.getInstance().execute(() -> {
                    DynamicTexture texture = new DynamicTexture(() -> "FlowRanks player head", finalImage);
                    Minecraft.getInstance().getTextureManager().register(entry.id, texture);
                    entry.ready = true;
                });
            } catch (Exception ignored) {
                entry.failedAt = System.currentTimeMillis();
            }
        });
    }

    private static NativeImage loadFromDisk(String normalizedKey) {
        try {
            Path file = cacheDir().resolve(normalizedKey + ".png");
            if (!Files.exists(file)) return null;
            try (InputStream in = Files.newInputStream(file)) {
                return NativeImage.read(in);
            }
        } catch (Exception ignored) {
            return null;
        }
    }

    private static NativeImage downloadAndCache(String normalizedKey, String uuid) {
        try {
            HttpURLConnection conn = (HttpURLConnection) URI.create(FlowPvpApi.AVATAR_URL.formatted(uuid)).toURL().openConnection();
            conn.setRequestProperty("User-Agent", "FlowPvP-Client-Mod/1.0");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(8000);
            byte[] bytes;
            try (InputStream in = conn.getInputStream()) {
                bytes = in.readAllBytes();
            }
            try { Files.write(cacheDir().resolve(normalizedKey + ".png"), bytes); } catch (Exception ignored) {}
            return NativeImage.read(new ByteArrayInputStream(bytes));
        } catch (Exception ignored) {
            return null;
        }
    }

    private static Path cacheDir() {
        Path dir = cacheDir;
        if (dir == null) {
            dir = Minecraft.getInstance().gameDirectory.toPath().resolve("cache/flowranks/heads");
            try { Files.createDirectories(dir); } catch (Exception ignored) {}
            cacheDir = dir;
        }
        return dir;
    }

    private static class Entry {
        private final Identifier id;
        private volatile boolean ready;
        private volatile long failedAt;

        private Entry(Identifier id) {
            this.id = id;
        }
    }
}

