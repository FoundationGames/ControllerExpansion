package io.github.foundationgames.controllerexpansion.mixin;

import io.github.foundationgames.controllerexpansion.screen.ControllerCraftingScreen;
import io.github.foundationgames.controllerexpansion.util.CEUtil;
import net.minecraft.client.gui.screen.ingame.CraftingScreen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.recipebook.RecipeBookWidget;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.CraftingScreenHandler;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CraftingScreen.class)
public abstract class CraftingScreenMixin extends HandledScreen<CraftingScreenHandler> {
    @Shadow @Final private RecipeBookWidget recipeBook;

    public CraftingScreenMixin(CraftingScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
    }

    @Inject(method = "init", at = @At("HEAD"), cancellable = true)
    private void controllerex$replaceCraftingScreen(CallbackInfo ci) {
        if (CEUtil.useControllerScreen()) {
            this.client.setScreen(new ControllerCraftingScreen.CraftingTable(this.handler, this.client.player.getInventory(), this.title, null));
            ci.cancel();
        }
    }
}
