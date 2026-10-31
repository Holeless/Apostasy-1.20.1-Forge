package com.vrilmod.vril.client;

import com.vrilmod.vril.VrilMod;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Mod-bus client setup: registers the model layer and builds the renderer. */
@Mod.EventBusSubscriber(modid = VrilMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientModEvents {
    private static Char1Renderer char1Renderer;

    public static Char1Renderer getChar1Renderer() {
        return char1Renderer;
    }

    @SubscribeEvent
    public static void registerLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(Char1Model.LAYER, Char1Model::createBodyLayer);
    }

    @SubscribeEvent
    public static void addLayers(EntityRenderersEvent.AddLayers event) {
        char1Renderer = new Char1Renderer(event.getContext());
    }
}
