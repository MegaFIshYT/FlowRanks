package gg.flowpvp.client.hud;

import gg.flowpvp.client.FlowPvPClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

public class HudEditorScreen extends Screen {
    private final Screen parent;
    private HudElement dragging;
    private int dragOffsetX, dragOffsetY;

    public HudEditorScreen(Screen parent) {
        super(Component.literal("HUD Editor"));
        this.parent = parent;
    }

    @Override
    public void init() {
        addRenderableWidget(Button.builder(Component.literal("Done"), b -> Minecraft.getInstance().setScreen(parent))
                .pos(this.width / 2 - 50, this.height - 30)
                .size(100, 20)
                .build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        graphics.fill(0, 0, this.width, this.height, 0x88000000);
        graphics.drawCenteredString(this.font,
                Component.literal("Drag to move  •  Right-click to toggle"),
                this.width / 2, 10, 0xFFAAAAAA);

        HudLayout layout = FlowPvPClient.HUD_LAYOUT;
        for (HudElement element : HudElement.values()) {
            HudLayout.ElementState state = layout.get(element);
            int w = this.font.width(element.label) + 8;
            int h = 14;
            int x = state.x;
            int y = state.y;

            boolean visible = state.visible;
            int bgColor = visible ? 0xCC1A4A8A : 0x88333333;
            int borderColor = visible ? 0xFF2A6AC0 : 0xFF555555;
            int textColor = visible ? 0xFFFFFFFF : 0xFF888888;

            graphics.fill(x, y, x + w, y + h, bgColor);
            graphics.renderOutline(x, y, w, h, borderColor);
            graphics.drawString(this.font, element.label, x + 4, y + 3, textColor, false);
        }

        super.render(graphics, mouseX, mouseY, partialTicks);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean bl) {
        int mouseX = (int) event.x();
        int mouseY = (int) event.y();
        HudLayout layout = FlowPvPClient.HUD_LAYOUT;

        for (HudElement element : HudElement.values()) {
            HudLayout.ElementState state = layout.get(element);
            int w = this.font.width(element.label) + 8;
            int h = 14;
            int x = state.x;
            int y = state.y;

            if (mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + h) {
                if (event.button() == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
                    layout.toggleVisible(element);
                    FlowPvPClient.saveHudConfig();
                    return true;
                } else if (event.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
                    dragging = element;
                    dragOffsetX = mouseX - x;
                    dragOffsetY = mouseY - y;
                    return true;
                }
            }
        }

        return super.mouseClicked(event, bl);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (dragging != null) {
            dragging = null;
            FlowPvPClient.saveHudConfig();
            return true;
        }
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double deltaX, double deltaY) {
        if (dragging != null) {
            HudLayout layout = FlowPvPClient.HUD_LAYOUT;
            // Use a generous max width estimate so the element never clips past the right edge
            int w = this.font.width(dragging.label) + 8;
            int h = 14;
            int newX = (int) Math.max(0, Math.min(this.width - w - 4, event.x() - dragOffsetX));
            int newY = (int) Math.max(0, Math.min(this.height - h - 4, event.y() - dragOffsetY));
            layout.set(dragging, newX, newY);
            return true;
        }
        return super.mouseDragged(event, deltaX, deltaY);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
