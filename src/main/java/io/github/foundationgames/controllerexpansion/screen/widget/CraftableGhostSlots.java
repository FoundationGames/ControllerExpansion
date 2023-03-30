package io.github.foundationgames.controllerexpansion.screen.widget;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.recipebook.RecipeBookGhostSlots;
import net.minecraft.client.util.math.MatrixStack;

public class CraftableGhostSlots extends RecipeBookGhostSlots {
    @Override
    public void draw(MatrixStack matrices, MinecraftClient client, int x, int y, boolean notInventory, float tickDelta) {
        for(int i = 0; i < this.getSlotCount(); ++i) {
            var slot = this.getSlot(i);
            int slotX = slot.getX() + x;
            int slotY = slot.getY() + y;

            var stack = slot.getCurrentItemStack();
            client.getItemRenderer().renderInGui(matrices, stack, slotX, slotY);
            if (i == 0) {
                client.getItemRenderer().renderGuiItemOverlay(matrices, client.textRenderer, stack, slotX, slotY);
            }
        }
    }
}
