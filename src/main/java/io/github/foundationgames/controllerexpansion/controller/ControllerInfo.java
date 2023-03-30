package io.github.foundationgames.controllerexpansion.controller;

import net.minecraft.client.gui.navigation.NavigationDirection;
import org.jetbrains.annotations.Nullable;

import java.util.function.BooleanSupplier;

public class ControllerInfo {
    public static ControllerNavigation INSTANCE = new ControllerNavigation() {
        @Override
        public @Nullable NavigationDirection getMenuNav() {
            return null;
        }

        @Override
        public @Nullable NavigationDirection getTabNav() {
            return null;
        }

        @Override
        public boolean getRequestingBlur() {
            return false;
        }

        @Override
        public boolean getRequestingPress() {
            return false;
        }

        @Override
        public void setVirtualMouse(boolean enabled) {
        }
    };

    public static BooleanSupplier USING_CONTROLLER = () -> false;
}
