package com.cibernet.splatcraft;

import com.cibernet.splatcraft.data.SplatcraftData;
import com.cibernet.splatcraft.entities.InkSquidEntity;
import com.cibernet.splatcraft.handlers.SplatcraftCommonHandler;
import com.cibernet.splatcraft.handlers.WeaponHandler;
import com.cibernet.splatcraft.handlers.client.PlayerMovementHandler;
import com.cibernet.splatcraft.handlers.client.SplatcraftKeyHandler;
import com.cibernet.splatcraft.network.SplatcraftPacketHandler;
import com.cibernet.splatcraft.registries.*;
import net.minecraft.block.Block;
import net.minecraft.entity.ai.attributes.GlobalEntityTypeAttributes;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.DeferredWorkQueue;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.server.FMLServerStartingEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(Splatcraft.MODID)
public class Splatcraft
{
	// Directly reference a log4j logger.
	private static final Logger LOGGER = LogManager.getLogger();
	public static final String MODID = "splatcraft";
	public static final String MODNAME = "Splatcraft";
	public static final String SHORT = "SC";
	public static final String VERSION = "2.3.0";
	
	public Splatcraft()
	{
		FMLJavaModLoadingContext.get().getModEventBus().addListener(this::commonSetup);
		FMLJavaModLoadingContext.get().getModEventBus().addListener(this::clientSetup);
		
		SplatcraftTileEntitites.init();
		
		MinecraftForge.EVENT_BUS.register(this);
		//MinecraftForge.EVENT_BUS.register(new SplatcraftData());
		MinecraftForge.EVENT_BUS.register(new SplatcraftCapabilities());
		MinecraftForge.EVENT_BUS.register(new SplatcraftCommonHandler());
		MinecraftForge.EVENT_BUS.register(new WeaponHandler());
		//MinecraftForge.EVENT_BUS.register(new PlayerMovementHandler());
		//MinecraftForge.EVENT_BUS.register(new SplatcraftKeyHandler());
		MinecraftForge.EVENT_BUS.register(FMLJavaModLoadingContext.get().getModEventBus());
	}
	
	private void commonSetup(final FMLCommonSetupEvent event)
	{
		SplatcraftCapabilities.registerCapabilities();
		SplatcraftPacketHandler.registerMessages();
		
		DeferredWorkQueue.runLater(() -> {
			GlobalEntityTypeAttributes.put(SplatcraftEntities.INK_SQUID, InkSquidEntity.setCustomAttributes().create());
		});
	}
	
	private void clientSetup(final FMLClientSetupEvent event)
	{
		SplatcraftEntities.bindRenderers();
		SplatcraftKeyHandler.registerKeys();
	}
	
	@SubscribeEvent
	public void onServerStarting(FMLServerStartingEvent event)
	{
	
	}
	
	@Mod.EventBusSubscriber(bus=Mod.EventBusSubscriber.Bus.MOD)
	public static class RegistryEvents
	{
		@SubscribeEvent
		public static void onBlocksRegistry(final RegistryEvent.Register<Block> blockRegistryEvent)
		{
		}
	}
}
