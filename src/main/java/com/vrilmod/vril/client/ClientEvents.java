package com.vrilmod.vril.client;

import com.vrilmod.vril.VrilMod;
import com.vrilmod.vril.character.CharacterManager;
import com.vrilmod.vril.character.CharacterType;
import com.vrilmod.vril.character.Characters;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderArmEvent;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = VrilMod.MOD_ID, value = Dist.CLIENT)
public class ClientEvents {

    /** Swaps in Character 1's own model (and scaling) instead of the skin model. */
    @SubscribeEvent
    public static void onRenderPlayer(RenderPlayerEvent.Pre event) {
        Player player = event.getEntity();
        if (CharacterManager.get(player) != Characters.CHARACTER_1) {
            return;
        }
        Char1Renderer renderer = ClientModEvents.getChar1Renderer();
        if (renderer == null || !(player instanceof AbstractClientPlayer p)) {
            return;
        }
        event.setCanceled(true);
        renderer.render(p, p.getYRot(), event.getPartialTick(), event.getPoseStack(),
                event.getMultiBufferSource(), event.getPackedLight());
    }

    /** First-person arm uses the character's texture too. */
    @SubscribeEvent
    public static void onRenderArm(RenderArmEvent event) {
        if (CharacterManager.get(event.getPlayer()) != Characters.CHARACTER_1) {
            return;
        }
        Char1Renderer renderer = ClientModEvents.getChar1Renderer();
        if (renderer == null) {
            return;
        }
        event.setCanceled(true);
        renderer.renderHand(event.getPoseStack(), event.getMultiBufferSource(),
                event.getPackedLight(), event.getPlayer(), event.getArm());
    }
}
