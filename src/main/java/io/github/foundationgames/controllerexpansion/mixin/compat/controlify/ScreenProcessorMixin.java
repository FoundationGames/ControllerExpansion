package io.github.foundationgames.controllerexpansion.mixin.compat.controlify;

import dev.isxander.controlify.controller.Controller;
import dev.isxander.controlify.screenop.ScreenProcessor;
import io.github.foundationgames.controllerexpansion.ControllerExpansion;
import io.github.foundationgames.controllerexpansion.util.CEUtil;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ScreenProcessor.class)
public class ScreenProcessorMixin {
    @Inject(method = "handleButtons", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screen/Screen;close()V", ordinal = 0))
    private void controllerex$playBackSound(Controller<?, ?> controller, CallbackInfo ci) {
        CEUtil.playUI(MinecraftClient.getInstance().getSoundManager(), ControllerExpansion.UI_BACK, 1);
    }

    @Inject(method = "handleComponentNavigation", at = @At(value = "INVOKE", target = "Ldev/isxander/controlify/mixins/feature/screenop/vanilla/ScreenAccessor;invokeChangeFocus(Lnet/minecraft/client/gui/navigation/GuiNavigationPath;)V", ordinal = 0))
    private void controllerex$playSelectSound(Controller<?, ?> controller, CallbackInfo ci) {
        CEUtil.playSelect(MinecraftClient.getInstance().getSoundManager());
    }
}
