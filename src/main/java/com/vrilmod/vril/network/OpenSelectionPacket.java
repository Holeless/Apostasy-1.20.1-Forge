package com.vrilmod.vril.network;

import com.vrilmod.vril.client.ClientHooks;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Server to client: open the character selection screen. */
public class OpenSelectionPacket {

    public static void encode(OpenSelectionPacket msg, FriendlyByteBuf buf) {
    }

    public static OpenSelectionPacket decode(FriendlyByteBuf buf) {
        return new OpenSelectionPacket();
    }

    public static void handle(OpenSelectionPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() ->
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientHooks.openSelectionScreen()));
        ctx.get().setPacketHandled(true);
    }
}
