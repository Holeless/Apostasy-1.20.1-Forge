package com.vrilmod.vril.network;

import com.vrilmod.vril.character.CharacterManager;
import com.vrilmod.vril.character.CharacterType;
import com.vrilmod.vril.character.Characters;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Client to server: the player picked a character. */
public class ChooseCharacterPacket {
    private final String id;

    public ChooseCharacterPacket(String id) {
        this.id = id;
    }

    public static void encode(ChooseCharacterPacket msg, FriendlyByteBuf buf) {
        buf.writeUtf(msg.id, 64);
    }

    public static ChooseCharacterPacket decode(FriendlyByteBuf buf) {
        return new ChooseCharacterPacket(buf.readUtf(64));
    }

    public static void handle(ChooseCharacterPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) {
                return;
            }
            CharacterType chosen = Characters.byId(msg.id);
            if (chosen == null) {
                return;
            }
            // Players pick once; only ops (via /vril character) may change afterwards.
            boolean alreadyChose = CharacterManager.getSaved(player) != null;
            if (alreadyChose && !player.hasPermissions(2)) {
                return;
            }
            CharacterManager.choose(player, chosen);
        });
        ctx.get().setPacketHandled(true);
    }
}
