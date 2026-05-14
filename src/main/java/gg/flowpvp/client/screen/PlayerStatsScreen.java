package gg.flowpvp.client.screen;

import gg.flowpvp.client.FlowPvPClient;
import gg.flowpvp.client.api.PlayerSkinCache;
import gg.flowpvp.client.model.EloHistoryPoint;
import gg.flowpvp.client.model.Ladder;
import gg.flowpvp.client.model.MatchSummary;
import gg.flowpvp.client.model.RankedPlayerData;
import gg.flowpvp.client.model.RankedStats;
import gg.flowpvp.client.model.RankedTier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.PlayerSkinWidget;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.PlayerSkin;

import java.util.Comparator;
import java.util.List;
import java.util.function.Supplier;

public class PlayerStatsScreen extends net.minecraft.client.gui.screens.Screen {
    private static final int PANEL = 0xB51A212E;
    private static final int CARD = 0xD01F2734;
    private static final int LINE = 0x664D5B70;
    private static final int TEXT = 0xFFE8F0FF;
    private static final int MUTED = 0xFFB8C5D6;
    private static final int DIM = 0xFF7E8797;

    private final String uuid;
    private final String fallbackName;
    private final FlowPvpScreen parent;
    private Ladder selectedLadder;
    private RankedPlayerData profile;
    private List<EloHistoryPoint> history = List.of();
    private List<MatchSummary> matches = List.of();
    private String status = "Loading profile...";
    private PlayerSkinWidget skinWidget;
    private int statsScrollRow = 0;

    public PlayerStatsScreen(String uuid, String fallbackName, Ladder selectedLadder, FlowPvpScreen parent) {
        super(Component.literal("FlowRanks Player"));
        this.uuid = uuid;
        this.fallbackName = fallbackName;
        this.selectedLadder = selectedLadder == null ? Ladder.GLOBAL : selectedLadder;
        this.parent = parent;
    }

    @Override
    public void init() {
        this.clearWidgets();

        Layout layout = layout();
        int buttonY = layout.bottom() - 22;
        addRenderableWidget(Button.builder(Component.literal("Back"), button -> Minecraft.getInstance().setScreen(parent))
                .pos(layout.left(), buttonY)
                .size(58, 20)
                .build());
        addRenderableWidget(Button.builder(Component.literal("Refresh"), button -> loadAll())
                .pos(layout.left() + 64, buttonY)
                .size(70, 20)
                .build());
        int previewTop = layout.top() + 82;
        int previewHeight = Math.max(96, Math.min(188, buttonY - previewTop - 8));
        int previewWidth = Math.max(64, Math.min(layout.leftWidth() - 44, 132));
        Supplier<PlayerSkin> skin = () -> PlayerSkinCache.skin(uuid, safeName());
        this.skinWidget = new PlayerSkinWidget(previewWidth, previewHeight, Minecraft.getInstance().getEntityModels(), skin);
        this.skinWidget.setX(layout.left() + layout.leftWidth() / 2 - previewWidth / 2);
        this.skinWidget.setY(previewTop);
        addRenderableWidget(this.skinWidget);

        if (profile == null) {
            loadAll();
        }
    }

    private void loadAll() {
        status = "Loading profile...";
        FlowPvPClient.CACHE.rankedByUuid(uuid, fallbackName).whenComplete((loadedProfile, throwable) -> Minecraft.getInstance().execute(() -> {
            if (throwable != null || loadedProfile == null) {
                status = "Could not load FlowRanks stats.";
                return;
            }

            profile = loadedProfile;
            status = "";
        }));

        loadHistory();
        FlowPvPClient.CACHE.matches(uuid).whenComplete((loadedMatches, throwable) -> Minecraft.getInstance().execute(() -> {
            if (throwable == null) {
                matches = loadedMatches;
            }
        }));
    }

    private void loadHistory() {
        history = List.of();
        FlowPvPClient.CACHE.history(uuid, selectedLadder).whenComplete((loadedHistory, throwable) -> Minecraft.getInstance().execute(() -> {
            if (throwable == null) {
                history = loadedHistory.stream()
                        .sorted(Comparator.comparingLong(EloHistoryPoint::timestamp))
                        .toList();
            }
        }));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        graphics.fill(0, 0, this.width, this.height, 0x66000000);

        Layout layout = layout();
        drawIdentity(graphics, layout);
        drawHistoryDuels(graphics, layout.right(), layout.top(), layout.rightWidth(), layout.historyHeight());
        drawRankEverywhere(graphics, layout.right(), layout.top() + layout.historyHeight() + 14, layout.rightWidth(), layout.bottom() - (layout.top() + layout.historyHeight() + 14));
        if (!status.isBlank()) {
            graphics.drawString(this.font, Component.literal(status), layout.left(), this.height - 13, 0xFFB8D8FF);
        }

        super.render(graphics, mouseX, mouseY, partialTicks);
    }

    private void drawIdentity(GuiGraphics graphics, Layout layout) {
        String name = profile == null ? safeName() : profile.lastKnownName();
        int x = layout.left();
        int y = layout.top();
        int width = layout.leftWidth();
        int height = layout.bottom() - layout.top();

        panel(graphics, x, y, width, height, "Player", 0xFF68DFFF);
        graphics.drawString(this.font, Component.literal(name), x + 14, y + 24, TEXT);

        int globalPosition = profile == null ? -1 : profile.globalPosition();
        int globalElo = profile == null ? RankedStats.UNRANKED_ELO : profile.globalElo();
        graphics.drawString(this.font, Component.literal("Position"), x + 14, y + 42, MUTED);
        graphics.drawString(this.font, Component.literal(globalPosition > 0 ? "#" + globalPosition : "Unplaced"), x + 88, y + 42, 0xFF68DFFF);
        graphics.drawString(this.font, Component.literal("Global"), x + 14, y + 56, MUTED);
        graphics.drawString(this.font, Component.literal(globalElo + " SR"), x + 88, y + 56, TEXT);

        RankedStats selected = profile == null ? null : profile.statsFor(selectedLadder);
        if (selectedLadder.hasRankedStats()) {
            String detail = selected == null
                    ? "Unranked / " + RankedStats.UNRANKED_ELO + " SR"
                    : "#" + selected.position() + " / " + selected.tier().displayName;
            int detailColor = selected == null ? RankedTier.fromElo(RankedStats.UNRANKED_ELO).color : selected.tier().color;
            graphics.drawString(this.font, Component.literal(selectedLadder.displayName), x + 14, y + 70, selectedLadder.color);
            graphics.drawString(this.font, Component.literal(detail), x + 88, y + 70, detailColor);
        }

        int trendY = height > 300 ? layout.bottom() - 54 : layout.bottom() - 42;
        drawTrend(graphics, x + 14, trendY, width - 28);
    }

    private void drawTrend(GuiGraphics graphics, int x, int y, int width) {
        graphics.drawString(this.font, Component.literal(selectedLadder.displayName + " ELO history"), x, y, MUTED);
        if (history.isEmpty()) {
            graphics.drawString(this.font, Component.literal("No history yet"), x, y + 13, DIM);
            return;
        }

        EloHistoryPoint first = history.get(0);
        EloHistoryPoint last = history.get(history.size() - 1);
        int change = last.elo() - first.elo();
        int color = change >= 0 ? 0xFF7CE38B : 0xFFFF7777;
        graphics.drawString(this.font, Component.literal(last.elo() + " SR"), x, y + 13, selectedLadder.color);
        graphics.drawString(this.font, Component.literal((change >= 0 ? "+" : "") + change), x + width - 42, y + 13, color);
    }

    private void drawHistoryDuels(GuiGraphics graphics, int x, int y, int width, int height) {
        panel(graphics, x, y, width, height, "History Duels", 0xFF68DFFF);
        if (matches.isEmpty()) {
            graphics.drawString(this.font, Component.literal("No recent ranked duels."), x + 14, y + 28, DIM);
            return;
        }

        int rowY = y + 24;
        int rowHeight = 20;
        int count = Math.min(Math.max(1, (height - 32) / rowHeight), matches.size());
        for (int i = 0; i < count; i++) {
            MatchSummary match = matches.get(i);
            int resultColor = match.won() ? 0xFF43D05F : 0xFFE65353;
            String opponents = match.opponents().isEmpty() ? "Unknown" : String.join(", ", match.opponents());
            String matchup = safeName() + " vs " + opponents;
            String time = relativeTime(match.endedAt());
            int rowRight = x + width - 12;

            graphics.fill(x + 12, rowY, rowRight, rowY + rowHeight - 1, 0xB2161D27);
            graphics.fill(x + 12, rowY, rowRight, rowY + 1, 0x66425261);
            graphics.fill(x + 12, rowY + rowHeight - 2, rowRight, rowY + rowHeight - 1, 0x55323B4B);
            graphics.fill(x + 16, rowY + 4, x + 31, rowY + 17, resultColor);
            graphics.drawCenteredString(this.font, Component.literal(match.won() ? "W" : "L"), x + 23, rowY + 7, 0xFFFFFFFF);
            graphics.drawString(this.font, Component.literal(trim(match.ladderName(), 12)), x + 38, rowY + 6, TEXT);
            graphics.drawString(this.font, Component.literal(trim(matchup, width > 420 ? 36 : 24)), x + 138, rowY + 6, 0xFF9FB7D7);
            graphics.drawString(this.font, Component.literal(time), rowRight - Math.max(34, this.font.width(time)), rowY + 6, 0xFFD9E5F4);
            rowY += rowHeight;
        }
    }

    private void drawRankEverywhere(GuiGraphics graphics, int x, int y, int width, int height) {
        panel(graphics, x, y, width, height, "Stats", 0xFF68DFFF);
        int cardsTop = y + 23;
        int gap = 4;
        int columns = width >= 460 ? 4 : width >= 330 ? 3 : 2;
        int cardWidth = (width - 28 - (columns - 1) * gap) / columns;
        int cardHeight = 52;
        int rowStride = cardHeight + gap;

        int ladderCount = 0;
        for (Ladder l : Ladder.values()) { if (l != Ladder.GLOBAL) ladderCount++; }
        int totalRows = (int) Math.ceil((double) ladderCount / columns);
        int visibleRows = Math.max(1, (height - 31) / rowStride);
        int maxScroll = Math.max(0, totalRows - visibleRows);
        statsScrollRow = Math.min(statsScrollRow, maxScroll);

        // Scroll arrows
        if (statsScrollRow > 0) {
            graphics.drawCenteredString(this.font, Component.literal("â–²"), x + width / 2, y + 14, 0xFF8899BB);
        }
        if (statsScrollRow < maxScroll) {
            graphics.drawCenteredString(this.font, Component.literal("â–¼"), x + width / 2, y + height - 10, 0xFF8899BB);
        }

        int index = 0;
        for (Ladder ladder : Ladder.values()) {
            if (ladder == Ladder.GLOBAL) continue;

            int col = index % columns;
            int row = index / columns;
            index++;

            if (row < statsScrollRow) continue;

            int displayRow = row - statsScrollRow;
            int cardX = x + 14 + col * (cardWidth + gap);
            int cardY = cardsTop + displayRow * rowStride;
            if (cardY + cardHeight > y + height - 12) break;

            drawRankCard(graphics, cardX, cardY, cardWidth, cardHeight, ladder);
        }
    }

    private void drawRankCard(GuiGraphics graphics, int x, int y, int width, int height, Ladder ladder) {
        RankedStats stats = profile == null ? null : profile.statsFor(ladder);
        RankedTier tier = stats == null ? null : stats.tier();
        int accent = tier == null ? 0xFF4D5B70 : tier.color;

        graphics.fill(x, y, x + width, y + height, CARD);
        outline(graphics, x, y, width, height, accent);
        drawIcon(graphics, ladder, x + 7, y + 7, 6);

        String title = ladder.displayName.toUpperCase();
        String position = stats != null && stats.position() > 0 ? " (#" + stats.position() + ")" : "";
        graphics.drawString(this.font, Component.literal(trim(title + position, 15)), x + 17, y + 5, MUTED);

        if (stats == null) {
            RankedTier unrankedTier = RankedTier.fromElo(RankedStats.UNRANKED_ELO);
            graphics.drawString(this.font, Component.literal(RankedStats.UNRANKED_ELO + " SR  Unranked"), x + 8, y + 19, unrankedTier.color);
            compactProgress(graphics, x + 8, y + height - 10, width - 16, tierProgress(RankedStats.UNRANKED_ELO, unrankedTier), unrankedTier.color);
            return;
        }

        graphics.drawString(this.font, Component.literal(stats.totalRating() + " SR  " + trim(tier.displayName, 10)), x + 8, y + 19, accent);
        compactProgress(graphics, x + 8, y + height - 10, width - 16, tierProgress(stats.totalRating(), tier), accent);
    }

    private void drawIcon(GuiGraphics graphics, Ladder ladder, int x, int y, int size) {
        Identifier texture = FlowPvPClient.id("textures/icons/" + ladder.iconTexture + ".png");
        graphics.pose().pushMatrix();
        graphics.pose().translate((float) x, (float) y);
        float scale = size / 32.0f;
        graphics.pose().scale(scale, scale);
        graphics.blit(RenderPipelines.GUI_TEXTURED, texture, 0, 0, 0, 0, 32, 32, 32, 32);
        graphics.pose().popMatrix();
    }

    private void panel(GuiGraphics graphics, int x, int y, int width, int height, String title, int color) {
        graphics.fill(x, y, x + width, y + height, PANEL);
        outline(graphics, x, y, width, height, LINE);
        graphics.drawString(this.font, Component.literal(title), x + 12, y + 9, color);
    }

    private void outline(GuiGraphics graphics, int x, int y, int width, int height, int color) {
        graphics.fill(x, y, x + width, y + 2, color);
        graphics.fill(x, y + height - 2, x + width, y + height, color);
        graphics.fill(x, y, x + 2, y + height, color);
        graphics.fill(x + width - 2, y, x + width, y + height, color);
    }

    private void progress(GuiGraphics graphics, int x, int y, int width, double value, int color) {
        progress(graphics, x, y, width, value, color, true);
    }

    private void progress(GuiGraphics graphics, int x, int y, int width, double value, int color, boolean showLabel) {
        graphics.fill(x, y, x + width, y + 4, 0xFF303746);
        graphics.fill(x, y, x + Math.max(2, (int) (width * Math.max(0, Math.min(1, value)))), y + 4, color);
        if (showLabel) {
            graphics.drawString(this.font, Component.literal(Math.round(value * 1000.0) / 10.0 + "%"), x + width - 38, y + 7, TEXT);
        }
    }

    private void compactProgress(GuiGraphics graphics, int x, int y, int width, double value, int color) {
        int labelWidth = 38;
        int barWidth = Math.max(18, width - labelWidth - 4);
        double clamped = Math.max(0, Math.min(1, value));
        String label = Math.round(clamped * 1000.0) / 10.0 + "%";
        graphics.fill(x, y, x + barWidth, y + 4, 0xFF303746);
        graphics.fill(x, y, x + Math.max(2, (int) (barWidth * clamped)), y + 4, color);
        graphics.drawString(this.font, Component.literal(label), x + width - this.font.width(label), y - 2, TEXT);
    }

    private double tierProgress(int rating, RankedTier tier) {
        RankedTier next = nextTier(tier);
        if (next == null || next.threshold <= tier.threshold) {
            return 1.0;
        }
        return (rating - tier.threshold) / (double) (next.threshold - tier.threshold);
    }

    private RankedTier nextTier(RankedTier tier) {
        RankedTier[] values = RankedTier.values();
        for (int i = 0; i < values.length - 1; i++) {
            if (values[i] == tier) {
                return values[i + 1] == RankedTier.GRANDMASTER ? null : values[i + 1];
            }
        }
        return null;
    }

    private Layout layout() {
        int totalWidth = Math.min(this.width - 32, 900);
        int left = this.width / 2 - totalWidth / 2;
        int top = 26;
        int bottom = this.height - 14;
        int gap = 16;
        int leftWidth = Math.max(230, Math.min(270, totalWidth / 3));
        int right = left + leftWidth + gap;
        int rightWidth = totalWidth - leftWidth - gap;
        int historyHeight = Math.max(118, Math.min(150, (bottom - top) / 3));
        return new Layout(left, right, top, bottom, leftWidth, rightWidth, historyHeight);
    }

    private static String trim(String value, int maxLength) {
        if (value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength - 1) + ".";
    }

    private static String relativeTime(long timestamp) {
        if (timestamp <= 0L) {
            return "now";
        }

        long seconds = Math.max(0L, (System.currentTimeMillis() - timestamp) / 1000L);
        if (seconds < 60L) {
            return seconds + "s ago";
        }

        long minutes = seconds / 60L;
        if (minutes < 60L) {
            return minutes + "m ago";
        }

        long hours = minutes / 60L;
        if (hours < 24L) {
            return hours + "h ago";
        }

        return hours / 24L + "d ago";
    }

    private String safeName() {
        return fallbackName == null || fallbackName.isBlank() ? "Player" : fallbackName;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        Layout layout = layout();
        int statsY = layout.top() + layout.historyHeight() + 14;
        if (mouseX >= layout.right() && mouseY >= statsY) {
            statsScrollRow = Math.max(0, statsScrollRow - (int) Math.signum(scrollY));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private record Layout(int left, int right, int top, int bottom, int leftWidth, int rightWidth, int historyHeight) {
    }
}

