package com.vrilmod.vril.character;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;

import java.util.UUID;

public class CharacterManager {
    private static final String KEY = "vril_character";

    // ---- persistence (server) ----

    public static String getSavedId(Player player) {
        CompoundTag root = player.getPersistentData();
        if (!root.contains(Player.PERSISTED_NBT_TAG, 10)) {
            return "";
        }
        return root.getCompound(Player.PERSISTED_NBT_TAG).getString(KEY);
    }

    public static void saveId(Player player, String id) {
        CompoundTag root = player.getPersistentData();
        CompoundTag persisted = root.getCompound(Player.PERSISTED_NBT_TAG);
        persisted.putString(KEY, id);
        root.put(Player.PERSISTED_NBT_TAG, persisted);
    }

    public static CharacterType getSaved(Player player) {
        return Characters.byId(getSavedId(player));
    }

    // ---- detection (client + server) ----

    /** Returns the character a player currently has, based on synced attribute modifiers. */
    public static CharacterType get(Player player) {
        for (CharacterType c : Characters.ALL) {
            CharacterType.ModifierSpec first = c.modifiers().get(0);
            AttributeInstance inst = player.getAttribute(first.attribute().get());
            if (inst != null && inst.getModifier(c.modifierId(first)) != null) {
                return c;
            }
        }
        return null;
    }

    // ---- applying (server) ----

    public static void apply(Player player, CharacterType c) {
        clear(player);
        for (CharacterType.ModifierSpec spec : c.modifiers()) {
            AttributeInstance inst = player.getAttribute(spec.attribute().get());
            if (inst == null) {
                continue;
            }
            UUID id = c.modifierId(spec);
            inst.removeModifier(id);
            inst.addTransientModifier(new AttributeModifier(
                    id, c.id() + "_" + spec.name(), spec.amount(), spec.operation()));
        }
        player.setMaxUpStep(c.stepHeight());
        player.refreshDimensions();
    }

    public static void clear(Player player) {
        for (CharacterType c : Characters.ALL) {
            for (CharacterType.ModifierSpec spec : c.modifiers()) {
                AttributeInstance inst = player.getAttribute(spec.attribute().get());
                if (inst != null) {
                    inst.removeModifier(c.modifierId(spec));
                }
            }
        }
        resetStepHeight(player);
        player.refreshDimensions();
    }

    /** Puts step height back to vanilla if it is currently one of our character values. */
    public static void resetStepHeight(Player player) {
        for (CharacterType c : Characters.ALL) {
            if (c.stepHeight() != CharacterType.DEFAULT_STEP_HEIGHT
                    && Math.abs(player.maxUpStep() - c.stepHeight()) < 0.001F) {
                player.setMaxUpStep(CharacterType.DEFAULT_STEP_HEIGHT);
                return;
            }
        }
    }

    public static void choose(ServerPlayer player, CharacterType c) {
        saveId(player, c.id());
        apply(player, c);
        player.sendSystemMessage(Component.translatable("message.vril.character_chosen",
                Component.translatable("character.vril." + c.id())));
    }
}
