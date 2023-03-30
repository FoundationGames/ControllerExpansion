package io.github.foundationgames.controllerexpansion.util;

import io.github.foundationgames.controllerexpansion.ControllerExpansion;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.sound.SoundManager;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;

import java.util.Random;

public enum CEUtil {;
    private static final Random random = new Random();

    public static Identifier id(String path) {
        return new Identifier(ControllerExpansion.NAMESPACE, path);
    }

    public static boolean useControllerScreen() {
        // TODO: Config
        return true;
    }

    public static void playUI(SoundManager soundManager, SoundEvent sound, float pitch) {
        soundManager.play(PositionedSoundInstance.master(sound, pitch));
    }

    public static void playSelect(SoundManager soundManager) {
        soundManager.play(PositionedSoundInstance.master(ControllerExpansion.UI_SELECT, 0.975f + (random.nextFloat() * 0.05f)));
    }
}
