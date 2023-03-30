package io.github.foundationgames.controllerexpansion.controller;

import dev.isxander.controlify.Controlify;
import dev.isxander.controlify.InputMode;
import dev.isxander.controlify.api.ControlifyApi;
import dev.isxander.controlify.bindings.ControllerBindings;
import net.minecraft.client.gui.navigation.NavigationDirection;
import org.jetbrains.annotations.Nullable;

public class ControlifyCompat {
    public static void initialize() {
        ControllerInfo.INSTANCE = new ControlifyNavigation();
        ControllerInfo.USING_CONTROLLER = () -> ControlifyApi.get().currentInputMode() == InputMode.CONTROLLER;
    }

    public static class ControlifyNavigation implements ControllerNavigation {
        private static ControllerBindings<?> bindings() {
            return ControlifyApi.get().currentController().bindings();
        }

        @Override
        public @Nullable NavigationDirection getMenuNav() {
            if (bindings().GUI_NAVI_UP.held()) {
                return NavigationDirection.UP;
            } else if (bindings().GUI_NAVI_DOWN.held()) {
                return NavigationDirection.DOWN;
            } else if (bindings().GUI_NAVI_LEFT.held()) {
                return NavigationDirection.LEFT;
            } else if (bindings().GUI_NAVI_RIGHT.held()) {
                return NavigationDirection.RIGHT;
            }
            return null;
        }

        @Override
        public @Nullable NavigationDirection getTabNav() {
            if (bindings().GUI_PREV_TAB.held()) {
                return NavigationDirection.LEFT;
            } else if (bindings().GUI_NEXT_TAB.held()) {
                return NavigationDirection.RIGHT;
            }
            return null;
        }

        @Override
        public boolean getRequestingBlur() {
            return bindings().GUI_BACK.held();
        }

        @Override
        public boolean getRequestingPress() {
            return bindings().GUI_PRESS.held();
        }

        @Override
        public void setVirtualMouse(boolean enabled) {
            // FIXME: Referencing non-API classes, extremely likely to break
            var handler = Controlify.instance().virtualMouseHandler();
            if (enabled) {
                handler.enableVirtualMouse();
            } else {
                handler.disableVirtualMouse();
            }
        }
    }
}
