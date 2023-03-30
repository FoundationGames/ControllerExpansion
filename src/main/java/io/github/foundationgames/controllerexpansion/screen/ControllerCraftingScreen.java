package io.github.foundationgames.controllerexpansion.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import io.github.foundationgames.controllerexpansion.ControllerExpansion;
import io.github.foundationgames.controllerexpansion.controller.ControllerInfo;
import io.github.foundationgames.controllerexpansion.controller.ControllerNavigation;
import io.github.foundationgames.controllerexpansion.screen.widget.ItemCategoryButtonWidget;
import io.github.foundationgames.controllerexpansion.screen.widget.RecipeCatalogWidget;
import io.github.foundationgames.controllerexpansion.util.CEUtil;
import io.github.foundationgames.controllerexpansion.util.SlotPositionRemapper;
import io.github.foundationgames.controllerexpansion.util.crafting.ItemCategory;
import net.minecraft.client.gui.navigation.GuiNavigation;
import net.minecraft.client.gui.navigation.GuiNavigationPath;
import net.minecraft.client.gui.navigation.NavigationDirection;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.CraftingInventory;
import net.minecraft.inventory.CraftingResultInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.screen.AbstractRecipeScreenHandler;
import net.minecraft.screen.CraftingScreenHandler;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

public abstract class ControllerCraftingScreen<I extends Inventory, T extends AbstractRecipeScreenHandler<I>> extends HandledScreen<T> {
    public static final Identifier TEXTURE = CEUtil.id("textures/gui/container/crafting.png");
    protected final int craftingAreaWidth;

    protected final ControllerNavigation controller = ControllerInfo.INSTANCE;
    protected int controllerMenuNavTimer = 0;
    protected NavigationDirection controllerTabNav = null;
    protected NavigationDirection controllerMenuNav = null;
    protected boolean controllerRequestingCraft = true;

    protected final @Nullable Screen parent;
    protected RecipeCatalogWidget recipeCatalog;
    private SlotPositionRemapper slotPositions;

    public ControllerCraftingScreen(T handler, PlayerInventory inventory, Text title, @Nullable Screen parent, int craftingAreaWidth) {
        super(handler, inventory, title);
        this.parent = parent;

        this.craftingAreaWidth = craftingAreaWidth;

        this.backgroundWidth = 13 + craftingAreaWidth + 10 + 162 + 7;
        this.backgroundHeight = 159;
    }

    @Override
    protected void init() {
        super.init();

        this.clearChildren();

        if (slotPositions == null && this.handler.slots.size() > 0) {
            var slots = new Slot[this.handler.slots.size()];
            this.handler.slots.toArray(slots);
            this.slotPositions = new SlotPositionRemapper(slots);

            while (this.slotPositions.next()) {
                this.remapSlotPosition(this.slotPositions);
            }
        }

        boolean needsRecipeCatalog = this.recipeCatalog == null;
        if (needsRecipeCatalog) {
            this.recipeCatalog = new RecipeCatalogWidget(this.client);
        }
        this.recipeCatalog.init(this.x + 8, this.y + 8, this.backgroundWidth - 14, this.handler);
        if (needsRecipeCatalog) {
            this.recipeCatalog.open(ItemCategory.CONSTRUCTION, true);
        }

        this.addSelectableChild(this.recipeCatalog);

        int buttonX = this.x + (this.backgroundWidth - ItemCategory.CATEGORIES.size() * 33) / 2;
        for (var category : ItemCategory.CATEGORIES) {
            this.addDrawableChild(new ItemCategoryButtonWidget(buttonX, this.y - 24, category, this.recipeCatalog));
            buttonX += 33;
        }
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        this.renderBackground(matrices);
        super.render(matrices, mouseX, mouseY, delta);
        this.recipeCatalog.render(matrices, mouseX, mouseY, delta);
        this.recipeCatalog.getGhostSlots().draw(matrices, this.client, this.x, this.y, true, delta);

        this.drawMouseoverTooltip(matrices, mouseX, mouseY);
    }

    @Override
    protected void drawBackground(MatrixStack matrices, float delta, int mouseX, int mouseY) {
        RenderSystem.setShaderTexture(0, TEXTURE);
        drawTexture(matrices, (this.width - this.backgroundWidth) / 2, (this.height - this.backgroundHeight) / 2,
                0, 0,
                this.backgroundWidth / 2, this.backgroundHeight);
        drawTexture(matrices, this.width / 2, (this.height - this.backgroundHeight) / 2,
                256 - (this.backgroundWidth / 2), 0,
                this.backgroundWidth / 2, this.backgroundHeight);

        this.drawInventory(matrices,
                ((this.width + this.backgroundWidth) / 2) - 170, ((this.height + this.backgroundHeight) / 2) - 84);

        this.drawCraftingBackground(matrices, delta);
    }

    @Override
    protected void drawForeground(MatrixStack matrices, int mouseX, int mouseY) {
        this.textRenderer.draw(matrices, this.title, (float)this.titleX, (float)this.titleY, 4210752);
    }

    protected void drawInventory(MatrixStack matrices, int x, int y) {
        RenderSystem.setShaderTexture(0, TEXTURE);

        for (int i = 0; i < 3; i++) {
            drawTexture(matrices, x + (54 * i), y, 108, 180, 54, 76);
        }
    }

    protected abstract void drawCraftingBackground(MatrixStack matrices, float delta);

    protected void remapSlotPosition(SlotPositionRemapper slotInfo) {
    }

    private void controllerTick() {
        if (this.recipeCatalog.isFocused() && this.controller.getRequestingBlur()) {
            this.recipeCatalog.setFocused(false);
        }

        var sounds = this.client.getSoundManager();
        if (!this.controllerRequestingCraft && this.controller.getRequestingPress()) {
            if (this.recipeCatalog.craft()) {
                CEUtil.playUI(sounds, SoundEvents.UI_BUTTON_CLICK.value(), 1);
                CEUtil.playUI(sounds, ControllerExpansion.UI_CRAFT, 1);
            } else {
                CEUtil.playUI(sounds, ControllerExpansion.UI_DENY, 1);
            }
        }
        this.controllerRequestingCraft = this.controller.getRequestingPress();

        var tabNav = this.controller.getTabNav();
        if (this.controllerTabNav != tabNav && tabNav != null) {
            this.recipeCatalog.changeCategory(switch (tabNav) {
                case LEFT -> -1;
                case RIGHT -> 1;
                default -> 0;
            });
            CEUtil.playSelect(sounds);
        }
        this.controllerTabNav = tabNav;

        var menuNav = this.controller.getMenuNav();
        if (menuNav == null || this.controllerMenuNav != menuNav) {
            this.controllerMenuNavTimer = 0;
        } else {
            if (this.controllerMenuNavTimer == 0 || this.controllerMenuNavTimer > 5) {
                if (this.recipeCatalog.navigate(this.controller.getMenuNav())) {
                    CEUtil.playUI(sounds, ControllerExpansion.UI_SCROLL, 1);
                }
            }

            this.controllerMenuNavTimer++;
        }
        this.controllerMenuNav = menuNav;

        this.controller.setVirtualMouse(!this.recipeCatalog.isFocused());
    }

    @Override
    protected void handledScreenTick() {
        super.handledScreenTick();

        this.controllerTick();
        this.recipeCatalog.tick();
    }

    @Override
    public void close() {
        if (this.slotPositions != null) {
            this.slotPositions.rewind();
        }

        if (this.parent != null) {
            this.client.setScreen(this.parent);
        } else {
            this.client.player.closeHandledScreen();
            this.client.setScreen(null);
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.recipeCatalog != null && this.recipeCatalog.keyPressed(keyCode, scanCode, modifiers)) {
            return true;
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Nullable
    @Override
    public GuiNavigationPath getNavigationPath(GuiNavigation navigation) {
        return null;
    }

    public static class InventoryCrafting extends ControllerCraftingScreen<CraftingInventory, PlayerScreenHandler> {
        public InventoryCrafting(PlayerScreenHandler handler, PlayerInventory inventory, Text title, Screen parent) {
            super(handler, inventory, title, parent, 90);

            this.titleX = 14;
            this.titleY = 81;
        }

        @Override
        protected void drawCraftingBackground(MatrixStack matrices, float delta) {
            RenderSystem.setShaderTexture(0, TEXTURE);
            drawTexture(matrices,
                    ((this.width - this.backgroundWidth) / 2) + 13,
                    ((this.height - this.backgroundHeight) / 2) + 95,
                    0, 166, 90, 36);
        }

        @Override
        protected void remapSlotPosition(SlotPositionRemapper slotInfo) {
            Inventory inv = slotInfo.slotInv();

            if (inv instanceof PlayerInventory && slotInfo.slotId() > 8 && slotInfo.slotId() < 45) {
                slotInfo.move((this.backgroundWidth - 170) - 7, 76 - 84);
            } else if (inv instanceof CraftingInventory) {
                slotInfo.move(-84, 78);
            } else if (inv instanceof CraftingResultInventory) {
                slotInfo.move(-72, 77);
            } else {
                slotInfo.set(-99999, -99999);
            }
        }
    }

    public static class CraftingTable extends ControllerCraftingScreen<CraftingInventory, CraftingScreenHandler> {
        public CraftingTable(CraftingScreenHandler handler, PlayerInventory inventory, Text title, Screen parent) {
            super(handler, inventory, title, parent, 108);

            this.titleX = 14;
            this.titleY = 74;
        }

        @Override
        protected void drawCraftingBackground(MatrixStack matrices, float delta) {
            RenderSystem.setShaderTexture(0, TEXTURE);
            drawTexture(matrices,
                    ((this.width - this.backgroundWidth) / 2) + 13,
                    ((this.height - this.backgroundHeight) / 2) + 87,
                    0, 202, 108, 54);
        }

        @Override
        protected void remapSlotPosition(SlotPositionRemapper slotInfo) {
            Inventory inv = slotInfo.slotInv();

            if (inv instanceof PlayerInventory) {
                slotInfo.move((this.backgroundWidth - 170) - 7, 76 - 84);
            } else if (inv instanceof CraftingInventory) {
                slotInfo.move(-16, 71);
            } else if (inv instanceof CraftingResultInventory) {
                slotInfo.move(-24, 71);
            }
        }
    }
}
