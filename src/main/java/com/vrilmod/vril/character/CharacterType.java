package com.vrilmod.vril.character;

import com.vrilmod.vril.VrilMod;
import net.minecraft.ChatFormatting;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * A selectable character. Attribute modifiers, hitbox height, step height and
 * air drain are all data on this record, so adding another character is just
 * a new entry in {@link Characters}.
 */
public record CharacterType(
        String id,
        ResourceLocation icon,
        int iconSize,
        List<Trait> traits,
        List<ModifierSpec> modifiers,
        float extraHeight,
        float stepHeight,
        int extraAirDrain) {

    public static final float DEFAULT_STEP_HEIGHT = 0.6F;

    public enum TraitKind {
        POSITIVE(ChatFormatting.GREEN),
        NEGATIVE(ChatFormatting.RED),
        NEUTRAL(ChatFormatting.YELLOW);

        public final ChatFormatting color;

        TraitKind(ChatFormatting color) {
            this.color = color;
        }
    }

    /** Lang keys: trait.vril.<id>.name and trait.vril.<id>.desc */
    public record Trait(String id, TraitKind kind) {
    }

    public record ModifierSpec(Supplier<Attribute> attribute, String name, double amount,
                               AttributeModifier.Operation operation) {
    }

    public UUID modifierId(ModifierSpec spec) {
        return UUID.nameUUIDFromBytes(
                (VrilMod.MOD_ID + ":" + id + ":" + spec.name()).getBytes(StandardCharsets.UTF_8));
    }
}
