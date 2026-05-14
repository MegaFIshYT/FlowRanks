package gg.flowpvp.client.hud;

import com.google.gson.JsonObject;
import java.util.EnumMap;
import java.util.Map;

public class HudLayout {

    public static final class ElementState {
        public int x, y;
        public boolean visible;

        public ElementState(int x, int y, boolean visible) {
            this.x = x;
            this.y = y;
            this.visible = visible;
        }
    }

    private final Map<HudElement, ElementState> states = new EnumMap<>(HudElement.class);

    public HudLayout() {
        states.put(HudElement.SELECTED_GAMEMODE, new ElementState(10, 50, true));
        states.put(HudElement.YOUR_ELO,          new ElementState(10, 66, true));
        states.put(HudElement.OPPONENT_ELO,      new ElementState(10, 82, true));
    }

    public ElementState get(HudElement element) {
        return states.get(element);
    }

    public void set(HudElement element, int x, int y) {
        ElementState s = states.get(element);
        s.x = x;
        s.y = y;
    }

    public void toggleVisible(HudElement element) {
        ElementState s = states.get(element);
        s.visible = !s.visible;
    }

    public void toJson(JsonObject root) {
        JsonObject hud = new JsonObject();
        for (HudElement e : HudElement.values()) {
            ElementState s = states.get(e);
            JsonObject el = new JsonObject();
            el.addProperty("x", s.x);
            el.addProperty("y", s.y);
            el.addProperty("visible", s.visible);
            hud.add(e.name(), el);
        }
        root.add("hud", hud);
    }

    public void fromJson(JsonObject root) {
        if (!root.has("hud")) return;
        JsonObject hud = root.getAsJsonObject("hud");
        for (HudElement e : HudElement.values()) {
            if (!hud.has(e.name())) continue;
            JsonObject el = hud.getAsJsonObject(e.name());
            ElementState s = states.get(e);
            if (el.has("x")) s.x = el.get("x").getAsInt();
            if (el.has("y")) s.y = el.get("y").getAsInt();
            if (el.has("visible")) s.visible = el.get("visible").getAsBoolean();
        }
    }
}
