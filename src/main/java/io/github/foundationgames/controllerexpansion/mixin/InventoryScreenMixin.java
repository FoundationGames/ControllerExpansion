package io.github.foundationgames.controllerexpansion.mixin;

import io.github.foundationgames.controllerexpansion.screen.ControllerCraftingScreen;
import io.github.foundationgames.controllerexpansion.util.CEUtil;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.gui.screen.recipebook.RecipeBookWidget;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InventoryScreen.class)
public abstract class InventoryScreenMixin extends HandledScreen<PlayerScreenHandler> {
    @Shadow @Final private RecipeBookWidget recipeBook;

    public InventoryScreenMixin(PlayerScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
    }

    @Inject(method = "method_19891(Lnet/minecraft/client/gui/widget/ButtonWidget;)V", at = @At("HEAD"), cancellable = true)
    private void controllerex$replaceInvRecipeBook(ButtonWidget button, CallbackInfo ci) {
        if (CEUtil.useControllerScreen()) {
            if (this.recipeBook.isOpen()) {
                this.recipeBook.toggleOpen();
            }
            this.client.setScreen(new ControllerCraftingScreen.InventoryCrafting(this.handler, this.client.player.getInventory(), this.title, this));
            ci.cancel();
        }
    }
}
