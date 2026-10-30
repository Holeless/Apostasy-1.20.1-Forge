package com.vrilmod.vril.item;

import com.vrilmod.vril.VrilMod;
import com.vrilmod.vril.block.ModBlocks;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, VrilMod.MOD_ID);

    public static final RegistryObject<Item> VRIL = ITEMS.register("vril",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> VRIL_ORE_ITEM = ITEMS.register("vril_ore",
            () -> new BlockItem(ModBlocks.VRIL_ORE.get(), new Item.Properties()));

    public static final RegistryObject<Item> DEEPSLATE_VRIL_ORE_ITEM = ITEMS.register("deepslate_vril_ore",
            () -> new BlockItem(ModBlocks.DEEPSLATE_VRIL_ORE.get(), new Item.Properties()));
}
