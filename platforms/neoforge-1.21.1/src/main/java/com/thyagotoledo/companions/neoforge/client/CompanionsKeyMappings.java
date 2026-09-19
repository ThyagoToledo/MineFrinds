package com.thyagotoledo.companions.neoforge.client;

import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;

/**
 * Mapeamento de teclas de atalho do mod Companions no lado cliente.
 */
public class CompanionsKeyMappings {

    public static final String CATEGORY_COMPANIONS = "key.categories.companions";
    public static final String KEY_OPEN_GUI_NAME = "key.companions.open_gui";

    public static final KeyMapping KEY_OPEN_GUI = new KeyMapping(
            KEY_OPEN_GUI_NAME,
            GLFW.GLFW_KEY_C,
            CATEGORY_COMPANIONS
    );
}
