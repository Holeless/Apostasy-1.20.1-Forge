package com.vrilmod.vril.client;

import net.minecraft.client.Minecraft;

public class ClientHooks {
    public static void openSelectionScreen() {
        Minecraft.getInstance().setScreen(new CharacterSelectScreen());
    }
}
