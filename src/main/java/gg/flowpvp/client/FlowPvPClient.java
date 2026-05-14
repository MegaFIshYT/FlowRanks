package gg.flowpvp.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import gg.flowpvp.client.api.FlowPvpApi;
import gg.flowpvp.client.api.FlowPvpCache;
import gg.flowpvp.client.hud.FlowHudOverlay;
import gg.flowpvp.client.hud.HudEditorScreen;
import gg.flowpvp.client.hud.HudLayout;
import gg.flowpvp.client.model.Ladder;
import gg.flowpvp.client.screen.FlowPvpScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

public class FlowPvPClient implements ClientModInitializer {
    public static final String MOD_ID = "flowranks";
    private static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(id("category"));

    public static final FlowPvpApi API = new FlowPvpApi();
    public static final FlowPvpCache CACHE = new FlowPvpCache(API);
    public static final HudLayout HUD_LAYOUT = new HudLayout();

    public static final KeyMapping OPEN_GUI_KEY = new KeyMapping("key.flowranks.open", GLFW.GLFW_KEY_G, CATEGORY);
    public static final KeyMapping CYCLE_MODE_KEY = new KeyMapping("key.flowranks.cycle_mode", GLFW.GLFW_KEY_Y, CATEGORY);
    public static final KeyMapping TOGGLE_OVERLAY_KEY = new KeyMapping("key.flowranks.toggle_overlay", GLFW.GLFW_KEY_O, CATEGORY);
    public static final KeyMapping OPEN_HUD_EDITOR_KEY = new KeyMapping("key.flowranks.hud_editor", GLFW.GLFW_KEY_H, CATEGORY);

    private static Ladder selectedLadder = Ladder.SWORD;
    private static boolean overlayEnabled = true;
    private int prefetchTick = 0;

    @Override
    public void onInitializeClient() {
        KeyBindingHelper.registerKeyBinding(OPEN_GUI_KEY);
        KeyBindingHelper.registerKeyBinding(CYCLE_MODE_KEY);
        KeyBindingHelper.registerKeyBinding(TOGGLE_OVERLAY_KEY);
        KeyBindingHelper.registerKeyBinding(OPEN_HUD_EDITOR_KEY);

        loadConfig();
        HudRenderCallback.EVENT.register(FlowHudOverlay::render);

        ClientTickEvents.END_CLIENT_TICK.register(this::onClientTick);
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> {
            try {
                API.close();
            } catch (Exception ignored) {
            }
        });
    }

    private void onClientTick(Minecraft minecraft) {
        while (OPEN_GUI_KEY.consumeClick()) {
            minecraft.setScreen(new FlowPvpScreen(Ladder.GLOBAL));
        }

        while (CYCLE_MODE_KEY.consumeClick()) {
            selectedLadder = selectedLadder.nextForHud();
            saveConfig();
            if (minecraft.player != null) {
                minecraft.player.displayClientMessage(Component.literal("FlowRanks mode: " + selectedLadder.icon + " " + selectedLadder.displayName), true);
            }
        }

        while (TOGGLE_OVERLAY_KEY.consumeClick()) {
            overlayEnabled = !overlayEnabled;
            saveConfig();
            if (minecraft.player != null) {
                minecraft.player.displayClientMessage(Component.literal("FlowRanks name tags: " + (overlayEnabled ? "on" : "off")), true);
            }
        }

        while (OPEN_HUD_EDITOR_KEY.consumeClick()) {
            minecraft.setScreen(new HudEditorScreen(null));
        }

        // Pre-fetch rank data for everyone on the server every ~10 seconds so tab is warm
        if (++prefetchTick >= 200 && minecraft.getConnection() != null) {
            prefetchTick = 0;
            for (var info : minecraft.getConnection().getListedOnlinePlayers()) {
                CACHE.rankedByName(info.getProfile().name());
            }
        }
    }

    private static void loadConfig() {
        try {
            Path path = Minecraft.getInstance().gameDirectory.toPath().resolve("config/flowranks.json");
            if (!Files.exists(path)) return;
            JsonObject obj = JsonParser.parseString(Files.readString(path)).getAsJsonObject();
            if (obj.has("selectedLadder")) {
                try {
                    Ladder l = Ladder.valueOf(obj.get("selectedLadder").getAsString());
                    if (l != Ladder.GLOBAL) selectedLadder = l;
                } catch (IllegalArgumentException ignored) {}
            }
            if (obj.has("overlayEnabled")) {
                overlayEnabled = obj.get("overlayEnabled").getAsBoolean();
            }
            HUD_LAYOUT.fromJson(obj);
        } catch (Exception ignored) {}
    }

    public static void saveConfig() {
        try {
            Path path = Minecraft.getInstance().gameDirectory.toPath().resolve("config/flowranks.json");
            Files.createDirectories(path.getParent());
            JsonObject obj = new JsonObject();
            obj.addProperty("selectedLadder", selectedLadder.name());
            obj.addProperty("overlayEnabled", overlayEnabled);
            HUD_LAYOUT.toJson(obj);
            String json = obj.toString();
            CompletableFuture.runAsync(() -> {
                try { Files.writeString(path, json); } catch (Exception ignored) {}
            });
        } catch (Exception ignored) {}
    }

    public static void saveHudConfig() {
        saveConfig();
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }

    public static Ladder selectedLadder() {
        return selectedLadder;
    }

    public static void setSelectedLadder(Ladder ladder) {
        selectedLadder = ladder == null || ladder == Ladder.GLOBAL ? Ladder.SWORD : ladder;
        saveConfig();
    }

    public static boolean overlayEnabled() {
        return overlayEnabled;
    }
}
