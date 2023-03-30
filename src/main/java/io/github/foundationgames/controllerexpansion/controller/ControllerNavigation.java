package io.github.foundationgames.controllerexpansion.controller;

import net.minecraft.client.gui.navigation.NavigationDirection;
import org.jetbrains.annotations.Nullable;

public interface ControllerNavigation {
    @Nullable NavigationDirection getMenuNav();
    @Nullable NavigationDirection getTabNav();
    boolean getRequestingBlur();
    boolean getRequestingPress();
    void setVirtualMouse(boolean enabled);
}
