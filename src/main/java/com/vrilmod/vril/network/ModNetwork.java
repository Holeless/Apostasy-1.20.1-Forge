package com.vrilmod.vril.network;

import com.vrilmod.vril.VrilMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public class ModNetwork {
    private static final String PROTOCOL = "1";

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(VrilMod.MOD_ID, "main"),
            () -> PROTOCOL,
            PROTOCOL::equals,
            PROTOCOL::equals);

    public static void register() {
        int id = 0;
        CHANNEL.registerMessage(id++, OpenSelectionPacket.class,
                OpenSelectionPacket::encode, OpenSelectionPacket::decode, OpenSelectionPacket::handle);
        CHANNEL.registerMessage(id++, ChooseCharacterPacket.class,
                ChooseCharacterPacket::encode, ChooseCharacterPacket::decode, ChooseCharacterPacket::handle);
    }

    public static void sendOpenSelection(ServerPlayer player) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new OpenSelectionPacket());
    }
}
