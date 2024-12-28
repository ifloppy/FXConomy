package me.yic.xconomy;

import me.yic.xconomy.adapter.FabricAdapter;
import me.yic.xconomy.adapter.comp.CConfig;
import me.yic.xconomy.commands.CommandManager;
import me.yic.xconomy.info.DefaultConfig;
import me.yic.xconomy.info.DataBaseConfig;
import me.yic.xconomy.lang.MessagesManager;
import me.yic.xconomy.listeners.ConnectionListeners;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.MinecraftServer;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class XConomyFabric extends XConomy implements ModInitializer {
    private static final Logger LOGGER = LoggerFactory.getLogger("xconomy");
    private FabricAdapter adapter;
    private MinecraftServer server;
    public static String PVersion;

    public XConomyFabric() {
        instance = this;
        version = "Fabric";
    }

    @Override
    public void onInitialize() {
        try {
            PVersion = FabricLoader.getInstance()
                    .getModContainer("xconomy")
                    .map(container -> container.getMetadata().getVersion().getFriendlyString())
                    .orElse("unknown");

            // Initialize adapter
            this.adapter = new FabricAdapter(this);
            AdapterManager.PLUGIN = adapter;

            // Load configuration using Core project's system
            DefaultConfig.config = new CConfig(adapter.getConfigFile("config.yml"));
            DataBaseConfig.config = new CConfig(adapter.getConfigFile("database.yml"));

            XConomyLoad.LoadConfig();

            if (XConomyLoad.Config.ISOLDCONFIG) {
                LOGGER.error("==================================================");
                LOGGER.error("Please regenerate all configuration files");
                LOGGER.error("==================================================");
                return;
            }

            // Initialize database tables
            me.yic.xconomy.data.sql.SQL.createTable();

            // Initialize server reference and complete initialization
            ServerLifecycleEvents.SERVER_STARTING.register(server -> {
                this.server = server;
                try {
                    // Complete initialization after server is available
                    XConomyLoad.Initial();
                } catch (Exception e) {
                    LOGGER.error("Failed to complete initialization", e);
                }
            });

            // Register player join/quit events
            ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> 
                ConnectionListeners.onPlayerJoin(handler.getPlayer()));
            ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> 
                ConnectionListeners.onPlayerQuit(handler.getPlayer()));

            // Register commands
            CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> 
                CommandManager.registerCommands(dispatcher, registryAccess));

            // Register server lifecycle events
            ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
                if (adapter != null) {
                    adapter.disable();
                }
            });

            LOGGER.info("XConomy Fabric initialized!");
        } catch (Exception e) {
            LOGGER.error("Failed to initialize XConomy", e);
        }
    }

    @Override
    public void logger(String tag, int type, String message) {
        String mess = message;
        if (tag != null) {
            if (message == null) {
                mess = MessagesManager.systemMessage(tag);
            } else {
                if (message.startsWith("<#>")) {
                    mess = message.substring(3) + MessagesManager.systemMessage(tag);
                } else {
                    mess = MessagesManager.systemMessage(tag) + message;
                }
            }
        }
        if (type == 1) {
            LOGGER.warn(mess);
        } else {
            LOGGER.info(mess);
        }
    }

    @Override
    public File getDataFolder() {
        File dataFolder = new File(FabricLoader.getInstance().getGameDir().toFile(), "config/xconomy");
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }
        return dataFolder;
    }

    @Override
    public File getPDataFolder() {
        File dataFolder = new File(FabricLoader.getInstance().getGameDir().toFile(), "config/xconomy/playerdata");
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }
        return dataFolder;
    }

    public Logger getLogger() {
        return LOGGER;
    }

    public MinecraftServer getServer() {
        return server;
    }

    public FabricAdapter getAdapter() {
        return adapter;
    }
}