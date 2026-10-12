package io.github.antonschmitz.newgameplus.client;

import io.github.antonschmitz.newgameplus.ImportData;
import io.github.antonschmitz.newgameplus.NewGamePlus;
import io.github.antonschmitz.newgameplus.SourceReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.level.storage.LevelSummary;

/** Pick the world whose player comes along, and which parts of it. */
public class ImportPlayerScreen extends Screen {
    private static final int PER_PAGE = 6;
    private static final int WHITE = 0xFFFFFFFF;
    private static final int GRAY = 0xFFA0A0A0;
    private static final int RED = 0xFFFF5555;

    private final CreateWorldScreen parent;
    private List<LevelSummary> worlds;
    private int page;
    private boolean items = true;
    private boolean xp = true;
    private boolean enderChest;
    private Component status = Component.empty();
    private int statusColor = GRAY;

    public ImportPlayerScreen(CreateWorldScreen parent) {
        super(Component.translatable("newgameplus.screen.title"));
        this.parent = parent;
        ImportData current = NewGamePlusClient.selection(parent);
        if (current != null) {
            items = current.items();
            xp = current.xp();
            enderChest = current.enderChest();
        }
    }

    @Override
    protected void init() {
        if (worlds == null) {
            worlds = List.of();
            status = Component.translatable("newgameplus.screen.loading");
            LevelStorageSource source = minecraft.getLevelSource();
            try {
                source.loadLevelSummaries(source.findLevelCandidates()).thenAcceptAsync(found -> {
                    List<LevelSummary> sorted = new ArrayList<>(found);
                    sorted.removeIf(summary -> summary.isLocked() || summary.requiresManualConversion());
                    sorted.sort(null);
                    worlds = sorted;
                    status = sorted.isEmpty() ? Component.translatable("newgameplus.screen.no_worlds") : Component.empty();
                    rebuildWidgets();
                }, minecraft);
            } catch (Exception e) {
                NewGamePlus.LOGGER.error("New Game Plus: could not list worlds", e);
                status = Component.translatable("newgameplus.screen.no_worlds");
            }
        }

        int centerX = width / 2;
        int top = 40;
        int pages = Math.max(1, (worlds.size() + PER_PAGE - 1) / PER_PAGE);
        page = Math.min(page, pages - 1);
        for (int i = 0; i < PER_PAGE; i++) {
            int index = page * PER_PAGE + i;
            if (index >= worlds.size()) {
                break;
            }
            LevelSummary summary = worlds.get(index);
            addRenderableWidget(Button.builder(Component.literal(summary.getLevelName()), button -> pick(summary))
                    .bounds(centerX - 100, top + i * 22, 200, 20)
                    .build());
        }
        int pagerY = top + PER_PAGE * 22 + 2;
        if (pages > 1) {
            Button previous = addRenderableWidget(Button.builder(Component.literal("<"), button -> {
                page--;
                rebuildWidgets();
            }).bounds(centerX - 100, pagerY, 20, 20).build());
            previous.active = page > 0;
            Button next = addRenderableWidget(Button.builder(Component.literal(">"), button -> {
                page++;
                rebuildWidgets();
            }).bounds(centerX + 80, pagerY, 20, 20).build());
            next.active = page < pages - 1;
        }

        int optionsY = pagerY + 26;
        addRenderableWidget(Checkbox.builder(Component.translatable("newgameplus.option.items"), font)
                .pos(centerX - 150, optionsY).selected(items).onValueChange((box, value) -> items = value).build());
        addRenderableWidget(Checkbox.builder(Component.translatable("newgameplus.option.xp"), font)
                .pos(centerX - 50, optionsY).selected(xp).onValueChange((box, value) -> xp = value).build());
        addRenderableWidget(Checkbox.builder(Component.translatable("newgameplus.option.ender_chest"), font)
                .pos(centerX + 30, optionsY).selected(enderChest).onValueChange((box, value) -> enderChest = value).build());

        int footerY = height - 28;
        addRenderableWidget(Button.builder(Component.translatable("newgameplus.screen.none"), button -> {
            NewGamePlusClient.select(parent, null);
            onClose();
        }).bounds(centerX - 154, footerY, 150, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.cancel"), button -> onClose())
                .bounds(centerX + 4, footerY, 150, 20).build());
    }

    private void pick(LevelSummary summary) {
        try {
            Optional<CompoundTag> player = SourceReader.readPlayer(minecraft.getLevelSource().getLevelPath(summary.getLevelId()),
                    minecraft.getUser().getProfileId());
            if (player.isEmpty()) {
                setStatus(Component.translatable("newgameplus.screen.no_player", summary.getLevelName()), RED);
                return;
            }
            NewGamePlusClient.select(parent, new ImportData(summary.getLevelName(), player.get(), items, xp, enderChest));
            onClose();
        } catch (Exception e) {
            NewGamePlus.LOGGER.error("New Game Plus: could not read the player from {}", summary.getLevelId(), e);
            setStatus(Component.translatable("newgameplus.screen.read_failed", summary.getLevelName()), RED);
        }
    }

    private void setStatus(Component message, int color) {
        status = message;
        statusColor = color;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        graphics.centeredText(font, title, width / 2, 15, WHITE);
        graphics.centeredText(font, Component.translatable("newgameplus.screen.hint"), width / 2, 27, GRAY);
        graphics.centeredText(font, status, width / 2, height - 44, statusColor);
    }

    @Override
    public void onClose() {
        minecraft.gui.setScreen(parent);
    }
}
