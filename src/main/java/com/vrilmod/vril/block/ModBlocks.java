package com.vrilmod.vril.block;

import com.vrilmod.vril.VrilMod;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DropExperienceBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, VrilMod.MOD_ID);

    public static final RegistryObject<Block> VRIL_ORE = BLOCKS.register("vril_ore",
            () -> new DropExperienceBlock(
                    BlockBehaviour.Properties.copy(Blocks.IRON_ORE),
                    UniformInt.of(1, 3)));

    public static final RegistryObject<Block> DEEPSLATE_VRIL_ORE = BLOCKS.register("deepslate_vril_ore",
            () -> new DropExperienceBlock(
                    BlockBehaviour.Properties.copy(Blocks.DEEPSLATE_IRON_ORE),
                    UniformInt.of(1, 3)));
}
