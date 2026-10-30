package com.vrilmod.vril.character;

import com.mojang.brigadier.CommandDispatcher;
import com.vrilmod.vril.VrilMod;
import com.vrilmod.vril.network.ModNetwork;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.effect.MobEffectUtil;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = VrilMod.MOD_ID)
public class CharacterEvents {

    // ---- selection / persistence ----

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        CharacterType saved = CharacterManager.getSaved(player);
        if (saved == null) {
            ModNetwork.sendOpenSelection(player);
        } else {
            CharacterManager.apply(player, saved);
        }
    }

    @SubscribeEvent
    public static void onClone(PlayerEvent.Clone event) {
        String id = CharacterManager.getSavedId(event.getOriginal());
        if (!id.isEmpty()) {
            CharacterManager.saveId(event.getEntity(), id);
        }
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        CharacterType saved = CharacterManager.getSaved(player);
        if (saved != null) {
            CharacterManager.apply(player, saved);
        }
    }

    // ---- hitbox: one block taller ----

    @SubscribeEvent
    public static void onSize(EntityEvent.Size event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        CharacterType c = CharacterManager.get(player);
        if (c == null || c.extraHeight() <= 0.0F || !isTallPose(event.getPose())) {
            return;
        }
        EntityDimensions d = event.getNewSize();
        event.setNewSize(EntityDimensions.scalable(d.width, d.height + c.extraHeight()));
        event.setNewEyeHeight(event.getNewEyeHeight() + c.extraHeight());
    }

    private static boolean isTallPose(Pose pose) {
        return pose == Pose.STANDING || pose == Pose.CROUCHING;
    }

    // ---- per-tick: keep hitbox / step height in sync, drain air ----

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Player player = event.player;
        CharacterType c = CharacterManager.get(player);

        if (c == null) {
            // Character was removed (or never had one): undo anything we changed.
            CharacterManager.resetStepHeight(player);
            return;
        }

        // Remote players on a client only receive attribute modifiers, so detect
        // changes statelessly by comparing the real hitbox to what it should be.
        Pose pose = player.getPose();
        float expected = player.getDimensions(pose).height + (isTallPose(pose) ? c.extraHeight() : 0.0F);
        if (Math.abs(player.getBbHeight() - expected) > 0.001F) {
            player.refreshDimensions();
        }
        if (Math.abs(player.maxUpStep() - c.stepHeight()) > 0.001F) {
            player.setMaxUpStep(c.stepHeight());
        }

        // Short breath: drain extra air while the head is underwater (server only).
        if (!player.level().isClientSide
                && c.extraAirDrain() > 0
                && player.isEyeInFluid(FluidTags.WATER)
                && !MobEffectUtil.hasWaterBreathing(player)
                && !player.getAbilities().invulnerable) {
            int air = player.getAirSupply();
            if (air > c.extraAirDrain()) {
                player.setAirSupply(air - c.extraAirDrain());
            }
        }
    }

    // ---- /vril character (op) re-opens the selection screen ----

    @SubscribeEvent
    public static void onCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        dispatcher.register(Commands.literal("vril")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("character").executes(ctx -> {
                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                    ModNetwork.sendOpenSelection(player);
                    return 1;
                })));
    }
}
