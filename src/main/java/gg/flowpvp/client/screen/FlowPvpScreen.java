package gg.flowpvp.client.screen;

import gg.flowpvp.client.FlowPvPClient;
import gg.flowpvp.client.api.SkinHeadCache;
import gg.flowpvp.client.hud.HudEditorScreen;
import gg.flowpvp.client.model.Ladder;
import gg.flowpvp.client.model.LeaderboardEntry;
import gg.flowpvp.client.model.RankedTier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public class FlowPvpScreen extends net.minecraft.client.gui.screens.Screen {
    private Ladder activeLadder;
    private EditBox searchBox;
    private LeaderboardList leaderboardList;
    private final List<LeaderboardEntry> entries = new ArrayList<>();
    private int page = 1;
    private boolean loading;
    private String status = "Loading leaderboard...";

    public FlowPvpScreen(Ladder initialLadder) {
        super(Component.literal("FlowRanks"));
        this.activeLadder = initialLadder == null ? Ladder.GLOBAL : initialLadder;
    }

    @Override
    public void init() {
        this.clearWidgets();

        int left = Math.max(16, this.width / 2 - 250);
        int right = Math.min(this.width - 16, this.width / 2 + 250);
        int x = left;
        int y = 34;

        for (Ladder ladder : Ladder.values()) {
            int buttonWidth = Math.max(54, Math.min(84, this.font.width(ladder.displayName) + 18));
            if (x + buttonWidth > right) {
                x = left;
                y += 22;
            }

            Button button = Button.builder(modeLabel(ladder), selected -> selectLadder(ladder))
                    .pos(x, y)
                    .size(buttonWidth, 20)
                    .build();
            button.active = ladder != activeLadder;
            addRenderableWidget(button);
            x += buttonWidth + 4;
        }

        int searchY = y + 28;
        this.searchBox = new EditBox(this.font, left, searchY, right - left - 74, 18, Component.literal(""));
        this.searchBox.setMaxLength(32);
        this.searchBox.setHint(Component.literal("Search player..."));
        addRenderableWidget(this.searchBox);

        addRenderableWidget(Button.builder(Component.literal("Search"), button -> searchPlayer())
                .pos(right - 68, searchY)
                .size(68, 20)
                .build());

        int listY = searchY + 30;
        int listHeight = Math.max(96, this.height - listY - 38);
        this.leaderboardList = new LeaderboardList(right - left, listHeight, listY);
        addRenderableWidget(this.leaderboardList);

        addRenderableWidget(Button.builder(Component.literal("More"), button -> loadLeaderboard(false))
                .pos(right - 68, this.height - 28)
                .size(68, 20)
                .build());

        addRenderableWidget(Button.builder(Component.literal("HUD"), button -> Minecraft.getInstance().setScreen(new HudEditorScreen(this)))
                .pos(left, this.height - 28)
                .size(46, 20)
                .build());

        if (entries.isEmpty()) {
            loadLeaderboard(true);
        } else {
            this.leaderboardList.updateEntries();
        }
    }

    private Component modeLabel(Ladder ladder) {
        MutableComponent icon = Component.literal(ladder.iconGlyph)
                .withStyle(style -> style.withFont(new FontDescription.Resource(FlowPvPClient.id("icons"))).withColor(0xFFFFFF));
        return Component.empty().append(icon).append(Component.literal(" " + ladder.displayName));
    }

    private void selectLadder(Ladder ladder) {
        this.activeLadder = ladder;
        if (ladder != Ladder.GLOBAL) {
            FlowPvPClient.setSelectedLadder(ladder);
        }
        loadLeaderboard(true);
        this.init();
    }

    private void loadLeaderboard(boolean reset) {
        if (loading) {
            return;
        }

        if (reset) {
            page = 1;
            entries.clear();
            if (leaderboardList != null) {
                leaderboardList.updateEntries();
            }
        }

        loading = true;
        int targetPage = reset ? 1 : page + 1;
        status = "Loading " + activeLadder.displayName + " page " + targetPage + "...";

        FlowPvPClient.CACHE.leaderboard(activeLadder, targetPage).whenComplete((result, throwable) -> Minecraft.getInstance().execute(() -> {
            loading = false;
            if (throwable != null) {
                status = "Could not load FlowRanks leaderboard.";
                return;
            }

            if (reset) {
                entries.clear();
            }

            entries.addAll(result);
            for (LeaderboardEntry entry : result) {
                SkinHeadCache.texture(entry.uuid());
            }
            page = targetPage;
            status = result.isEmpty() ? "No more players on this leaderboard." : "Showing " + entries.size() + " players.";
            if (leaderboardList != null) {
                leaderboardList.updateEntries();
            }
        }));
    }

    private void searchPlayer() {
        String query = searchBox == null ? "" : searchBox.getValue().trim();
        if (query.length() < 2) {
            status = "Type at least 2 characters to search.";
            return;
        }

        status = "Searching for " + query + "...";
        FlowPvPClient.CACHE.rankedByName(query).whenComplete((profile, throwable) -> Minecraft.getInstance().execute(() -> {
            if (throwable != null || profile == null) {
                status = "No FlowRanks player found for " + query + ".";
                return;
            }
            Minecraft.getInstance().setScreen(new PlayerStatsScreen(profile.uuid(), profile.lastKnownName(), activeLadder, this));
        }));
    }

    @Override
    public boolean keyPressed(KeyEvent keyEvent) {
        if (keyEvent.key() == GLFW.GLFW_KEY_ENTER || keyEvent.key() == GLFW.GLFW_KEY_KP_ENTER) {
            searchPlayer();
            return true;
        }

        return super.keyPressed(keyEvent);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean bl) {
        if (searchBox != null && searchBox.mouseClicked(event, bl)) {
            this.setFocused(searchBox);
        }

        return super.mouseClicked(event, bl);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        graphics.fill(0, 0, this.width, this.height, 0x66000000);
        graphics.drawCenteredString(this.font, Component.literal("FlowRanks Leaderboards"), this.width / 2, 14, 0xFFFFFFFF);
        graphics.drawString(this.font, Component.literal(status), Math.max(16, this.width / 2 - 250), this.height - 24, loading ? 0xFFFFFF55 : 0xFFB8D8FF);
        super.render(graphics, mouseX, mouseY, partialTicks);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    class LeaderboardList extends ObjectSelectionList<LeaderboardList.Entry> {
        private final int rowWidth;

        LeaderboardList(int width, int height, int y) {
            super(FlowPvpScreen.this.minecraft, width, height, y, 28);
            this.rowWidth = width - 18;
            this.setX(FlowPvpScreen.this.width / 2 - width / 2);
            updateEntries();
        }

        @Override
        public int getRowWidth() {
            return rowWidth;
        }

        @Override
        protected int scrollBarX() {
            return this.getX() + this.getRowWidth() + 8;
        }

        void updateEntries() {
            this.clearEntries();
            for (LeaderboardEntry entry : entries) {
                this.addEntry(new Entry(entry));
            }
        }

        class Entry extends ObjectSelectionList.Entry<Entry> {
            private final LeaderboardEntry entry;

            Entry(LeaderboardEntry entry) {
                this.entry = entry;
            }

            @Override
            public void renderContent(GuiGraphics graphics, int mouseX, int mouseY, boolean hovering, float partialTicks) {
                Font font = Minecraft.getInstance().font;
                int x = this.getContentX();
                int y = this.getContentY();
                int color = hovering ? 0x332A9DF4 : 0x22000000;
                graphics.fill(x - 3, y - 2, x + this.getWidth() - 2, y + this.getHeight() - 2, color);

                Identifier head = SkinHeadCache.texture(entry.uuid());
                if (head != null) {
                    graphics.pose().pushMatrix();
                    graphics.pose().translate((float) (x + 34), (float) (y + 3));
                    float scale = 18 / 64.0f;
                    graphics.pose().scale(scale, scale);
                    graphics.blit(RenderPipelines.GUI_TEXTURED, head, 0, 0, 0, 0, 64, 64, 64, 64);
                    graphics.pose().popMatrix();
                } else {
                    graphics.fill(x + 34, y + 3, x + 52, y + 21, 0xFF2A3244);
                    graphics.fill(x + 38, y + 7, x + 48, y + 17, 0xFF364156);
                }

                RankedTier tier = RankedTier.fromElo(entry.elo());
                String rank = entry.position() == 1 ? "Grandmaster" : tier.displayName;
                int rankColor = entry.position() == 1 ? RankedTier.GRANDMASTER.color : tier.color;

                graphics.drawString(font, Component.literal("#" + entry.position()), x + 4, y + 8, rankColor);
                graphics.drawString(font, Component.literal(entry.name()), x + 60, y + 4, 0xFFFFFFFF);
                graphics.drawString(font, Component.literal(entry.uuid()), x + 60, y + 15, 0xFF888888);
                graphics.drawString(font, Component.literal(rank), x + this.getWidth() - 168, y + 4, rankColor);
                graphics.drawString(font, Component.literal(entry.elo() + " elo"), x + this.getWidth() - 76, y + 8, 0xFFFFFFFF);
            }

            @Override
            public boolean mouseClicked(MouseButtonEvent event, boolean bl) {
                Minecraft.getInstance().setScreen(new PlayerStatsScreen(entry.uuid(), entry.name(), activeLadder, FlowPvpScreen.this));
                return true;
            }

            @Override
            public Component getNarration() {
                return Component.literal(entry.name());
            }
        }
    }
}

