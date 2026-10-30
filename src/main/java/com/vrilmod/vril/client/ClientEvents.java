package com.vrilmod.vril.client;

import com.vrilmod.vril.VrilMod;
import com.vrilmod.vril.character.CharacterManager;
import com.vrilmod.vril.character.CharacterType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = VrilMod.MOD_ID, value = Dist.CLIENT)
public class ClientEvents {
    private static final float VANILLA_PLAYER_HEIGHT = 1.8F;

    /** Scales the player model so it visually matches the taller hitbox. */
    @SubscribeEvent
    public static void onRenderPlayer(RenderPlayerEvent.Pre event) {
        Player player = event.getEntity();
        CharacterType c = CharacterManager.get(player);
        if (c == null || c.extraHeight() <= 0.0F) {
            return;
        }
        Pose pose = player.getPose();
        if (pose != Pose.STANDING && pose != Pose.CROUCHING) {
            return;
        }
        float scale = (VANILLA_PLAYER_HEIGHT + c.extraHeight()) / VANILLA_PLAYER_HEIGHT;
        event.getPoseStack().scale(scale, scale, scale);
    }
}
