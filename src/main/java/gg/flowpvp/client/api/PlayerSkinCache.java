package gg.flowpvp.client.api;

import com.mojang.blaze3d.platform.NativeImage;
import gg.flowpvp.client.FlowPvPClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.core.ClientAsset;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.PlayerSkin;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public final class PlayerSkinCache {
    private static final Map<String, Entry> ENTRIES = new ConcurrentHashMap<>();
    private static volatile Path cacheDir;

    private PlayerSkinCache() {
    }

    public static PlayerSkin skin(String uuid) {
        return skin(uuid, null);
    }

    public static PlayerSkin skin(String uuid, String username) {
        UUID parsedUuid = parseUuid(uuid);
        PlayerSkin fallback = DefaultPlayerSkin.get(parsedUuid);
        String normalized = normalize(uuid);
        if (normalized.isBlank()) {
            return fallback;
        }

        Entry entry = ENTRIES.computeIfAbsent(normalized, key -> {
            Entry created = new Entry(
                    FlowPvPClient.id("skin_assets/" + key),
                    FlowPvPClient.id("skins/" + key),
                    fallback
            );
            load(normalized, username, created);
            return created;
        });

        if (!entry.ready) {
            return entry.fallback;
        }

        return PlayerSkin.insecure(new ClientAsset.ResourceTexture(entry.assetId, entry.textureId), entry.fallback.cape(), entry.fallback.elytra(), entry.fallback.model());
    }

    private static void load(String normalizedUuid, String username, Entry entry) {
        CompletableFuture.runAsync(() -> {
            try {
                NativeImage image = readSkinCached(normalizedUuid, username);
                if (image == null) {
                    entry.failed = true;
                    return;
                }

                Minecraft.getInstance().execute(() -> {
                    DynamicTexture texture = new DynamicTexture(() -> "FlowRanks player skin", image);
                    Minecraft.getInstance().getTextureManager().register(entry.textureId, texture);
                    entry.ready = true;
                    entry.failed = false;
                });
            } catch (Exception ignored) {
                entry.failed = true;
            }
        });
    }

    private static NativeImage readSkinCached(String normalizedKey, String username) {
        // Try disk cache first
        try {
            Path file = cacheDir().resolve(normalizedKey + ".png");
            if (Files.exists(file)) {
                try (InputStream in = Files.newInputStream(file)) {
                    return NativeImage.read(in);
                }
            }
        } catch (Exception ignored) {}

        // Download by UUID, fall back to username
        byte[] bytes = downloadSkin(normalizedKey);
        if (bytes == null && username != null && !username.isBlank()) {
            bytes = downloadSkin(username.trim());
        }
        if (bytes == null) return null;

        // Save to disk cache
        try { Files.write(cacheDir().resolve(normalizedKey + ".png"), bytes); } catch (Exception ignored) {}

        try {
            return NativeImage.read(new ByteArrayInputStream(bytes));
        } catch (Exception ignored) {
            return null;
        }
    }

    private static byte[] downloadSkin(String playerId) {
        try {
            HttpURLConnection conn = (HttpURLConnection) URI.create(FlowPvpApi.PLAYER_SKIN_URL.formatted(playerId)).toURL().openConnection();
            conn.setRequestProperty("User-Agent", "FlowPvP-Client-Mod/1.0");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(8000);
            try (InputStream in = conn.getInputStream()) {
                return in.readAllBytes();
            }
        } catch (Exception ignored) {
            return null;
        }
    }

    private static Path cacheDir() {
        Path dir = cacheDir;
        if (dir == null) {
            dir = Minecraft.getInstance().gameDirectory.toPath().resolve("cache/flowranks/skins");
            try { Files.createDirectories(dir); } catch (Exception ignored) {}
            cacheDir = dir;
        }
        return dir;
    }

    private static UUID parseUuid(String value) {
        String normalized = normalize(value);
        if (normalized.matches("[0-9a-f]{32}")) {
            String dashed = normalized.replaceFirst(
                    "([0-9a-f]{8})([0-9a-f]{4})([0-9a-f]{4})([0-9a-f]{4})([0-9a-f]{12})",
                    "$1-$2-$3-$4-$5"
            );
            return UUID.fromString(dashed);
        }

        return UUID.nameUUIDFromBytes(normalized.getBytes(StandardCharsets.UTF_8));
    }

    private static String normalize(String uuid) {
        return uuid == null ? "" : uuid.replace("-", "").toLowerCase(Locale.ROOT);
    }

    private static class Entry {
        private final Identifier assetId;
        private final Identifier textureId;
        private final PlayerSkin fallback;
        private volatile boolean ready;
        private volatile boolean failed;

        private Entry(Identifier assetId, Identifier textureId, PlayerSkin fallback) {
            this.assetId = assetId;
            this.textureId = textureId;
            this.fallback = fallback;
        }
    }
}

