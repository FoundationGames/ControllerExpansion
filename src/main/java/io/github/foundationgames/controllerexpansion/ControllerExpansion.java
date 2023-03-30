package io.github.foundationgames.controllerexpansion;

import io.github.foundationgames.controllerexpansion.controller.ControlifyCompat;
import io.github.foundationgames.controllerexpansion.util.CEUtil;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.ResourcePackActivationType;
import net.fabricmc.fabric.impl.resource.loader.ModResourcePackUtil;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;

public class ControllerExpansion implements ClientModInitializer {
    public static final String NAMESPACE = "controllerexpansion";
    public static final String MOD_ID = "controllerexpansion";

    public static final SoundEvent UI_SELECT = soundEvent(CEUtil.id("ui.select"));
    public static final SoundEvent UI_SCROLL = soundEvent(CEUtil.id("ui.scroll"));
    public static final SoundEvent UI_BACK = soundEvent(CEUtil.id("ui.back"));
    public static final SoundEvent UI_DENY = soundEvent(CEUtil.id("ui.deny"));
    public static final SoundEvent UI_CRAFT = soundEvent(CEUtil.id("ui.craft"));

    @Override
    public void onInitializeClient() {
        if (FabricLoader.getInstance().isModLoaded("controlify")) {
            ControlifyCompat.initialize();
        }

        FabricLoader.getInstance().getModContainer(MOD_ID).ifPresent(mod -> {
            ResourceManagerHelper.registerBuiltinResourcePack(CEUtil.id("legacy_ui_sounds"), mod, ResourcePackActivationType.NORMAL);
        });
    }

    public static SoundEvent soundEvent(Identifier id) {
        return Registry.register(Registries.SOUND_EVENT, id, SoundEvent.of(id));
    }
}
