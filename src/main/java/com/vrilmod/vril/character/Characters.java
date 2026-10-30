package com.vrilmod.vril.character;

import com.vrilmod.vril.VrilMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraftforge.common.ForgeMod;

import java.util.List;

public class Characters {

    public static final CharacterType CHARACTER_1 = new CharacterType(
            "character_1",
            new ResourceLocation(VrilMod.MOD_ID, "textures/gui/character_1.png"),
            8,
            List.of(
                    new CharacterType.Trait("swift_strong", CharacterType.TraitKind.POSITIVE),
                    new CharacterType.Trait("poor_swimmer", CharacterType.TraitKind.NEGATIVE),
                    new CharacterType.Trait("short_breath", CharacterType.TraitKind.NEGATIVE),
                    new CharacterType.Trait("towering", CharacterType.TraitKind.POSITIVE)),
            List.of(
                    // The first modifier is also how a character is detected on a player,
                    // and attribute modifiers are synced to every client for free.
                    new CharacterType.ModifierSpec(() -> Attributes.MOVEMENT_SPEED, "movement_speed",
                            0.30, AttributeModifier.Operation.MULTIPLY_BASE),
                    new CharacterType.ModifierSpec(() -> Attributes.ATTACK_DAMAGE, "attack_damage",
                            2.0, AttributeModifier.Operation.ADDITION),
                    new CharacterType.ModifierSpec(ForgeMod.SWIM_SPEED::get, "swim_speed",
                            -0.85, AttributeModifier.Operation.MULTIPLY_TOTAL)),
            1.0F,   // one block taller
            1.1F,   // step height: lets you walk up full blocks (vanilla is 0.6)
            2);     // extra air lost per tick underwater (3x total drain, about 5s of air)

    public static final List<CharacterType> ALL = List.of(CHARACTER_1);

    public static CharacterType byId(String id) {
        for (CharacterType c : ALL) {
            if (c.id().equals(id)) {
                return c;
            }
        }
        return null;
    }
}
