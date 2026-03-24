package com.lulan.shincolle;

import com.lulan.shincolle.handler.*;
import com.lulan.shincolle.init.*;
import com.lulan.shincolle.proxy.CommonProxy;
import com.lulan.shincolle.proxy.IProxy;
import com.lulan.shincolle.proxy.ServerProxy;
import com.lulan.shincolle.utility.LogHelper;
import net.minecraftforge.common.ForgeChunkManager;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.*;
import net.minecraftforge.fml.common.network.NetworkRegistry;

@Mod(modid="shincolle", name="Shinkaiseikan Collection", version="1.12.2.7.1", /*dependencies="required-after:forge@[14.23.5.2859,)",*/ guiFactory="com.lulan.shincolle.config.ConfigGuiFactory", acceptedMinecraftVersions="[1.12.2]")
public class ShinColle {
    @Mod.Instance(value="shincolle")
    public static ShinColle instance;
    @SidedProxy(clientSide="com.lulan.shincolle.proxy.ClientProxy", serverSide="com.lulan.shincolle.proxy.ServerProxy")
    public static IProxy proxy;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) throws Exception {
        ConfigHandler.init(event);
        ModEntity.init();
        ModSounds.init();
        proxy.registerRender();
        proxy.registerChannel();
        proxy.registerCapability();
        LogHelper.info("INFO: Pre-Init completed.");
    }

    @Mod.EventHandler
    public void Init(FMLInitializationEvent event) {
        NetworkRegistry.INSTANCE.registerGuiHandler(this, new GuiHandler());
        MinecraftForge.EVENT_BUS.register(new NameTagRenderHandler());
        ModOres.oreDictRegister();
        ModEvents.init();
        ModRecipes.init();
        LogHelper.info("INFO: Init completed.");
    }

    @Mod.EventHandler
    public void postInit(FMLPostInitializationEvent event) {
        ModWorldGen.init();
        ForgeChunkManager.setForcedChunkLoadingCallback(instance, new ChunkLoaderHandler());
        ConfigHandler.checkChange(ConfigHandler.config);
        CommonProxy.checkModLoaded();
        LogHelper.info("INFO: Post-Init completed.");
    }

    @Mod.EventHandler
    public void onServerAboutToStart(FMLServerAboutToStartEvent event) {
        LogHelper.info("INFO: server about to start: is MP server? " + event.getSide().isServer());
        ServerProxy.initServerFile = true;
        ServerProxy.saveServerFile = false;
        CommonProxy.isMultiplayer = event.getSide().isServer();
    }

    @Mod.EventHandler
    public void onServerStarting(FMLServerStartingEvent event) {
        LogHelper.info("INFO: Server starting event: is MP server? " + event.getSide().isServer());
        CommandHandler.init(event);
    }

    @Mod.EventHandler
    public void onServerStopping(FMLServerStoppingEvent event) {
        LogHelper.info("INFO: Server stopping event");
        ServerProxy.initServerFile = false;
        ServerProxy.saveServerFile = true;
    }
}

