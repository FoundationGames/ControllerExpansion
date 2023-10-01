package io.github.foundationgames.controllerexpansion.screen.widget;

import com.mojang.blaze3d.systems.RenderSystem;
import io.github.foundationgames.controllerexpansion.screen.ControllerCraftingScreen;
import io.github.foundationgames.controllerexpansion.util.menu.ItemCategory;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class ItemCategoryButtonWidget extends ClickableWidget {
    private static final Identifier TEXTURE = ControllerCraftingScreen.TEXTURE;

    private final ItemCategory itemCategory;
    private final RecipeCatalogWidget recipeCatalog;

    public ItemCategoryButtonWidget(int x, int y, ItemCategory itemCategory, RecipeCatalogWidget recipeCatalog) {
        super(x, y, 32, 27, Text.empty());
        this.itemCategory = itemCategory;
        this.recipeCatalog = recipeCatalog;
    }

    @Override
    public void renderButton(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        RenderSystem.setShaderTexture(0, TEXTURE);

        boolean selected = this.itemCategory.equals(this.recipeCatalog.getCurrentCategory());
        drawTexture(matrices, this.getX(), this.getY(), selected ? 194 : 162, 159, this.width, this.height);

        this.itemCategory.drawIcon(matrices, MinecraftClient.getInstance(), this.getX() + 8, this.getY() + 6);
    }

    @Override
    public void onClick(double mouseX, double mouseY) {
        this.recipeCatalog.open(this.itemCategory, true);
    }

    @Override
    protected void appendClickableNarrations(NarrationMessageBuilder builder) {
        // TODO
    }
}
