package me.yic.xconomy.adapter;

import me.yic.xconomy.XConomyFabric;
import me.yic.xconomy.adapter.comp.CPlayer;
import me.yic.xconomy.adapter.comp.CPlugin;
import me.yic.xconomy.data.syncdata.PlayerData;
import me.yic.xconomy.messaging.FabricMessaging;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class FabricAdapter extends CPlugin {
    private final XConomyFabric plugin;
    private final FabricMessaging messaging;
    private final ScheduledExecutorService scheduler;
    private final Path configDir;

    public FabricAdapter(XConomyFabric plugin) {
        this.plugin = plugin;
        this.messaging = new FabricMessaging(plugin);
        this.scheduler = Executors.newScheduledThreadPool(1);
        this.configDir = FabricLoader.getInstance().getConfigDir().resolve("xconomy");
        createDefaultFiles();
    }

    private void createDefaultFiles() {
        try {
            Files.createDirectories(configDir);
            createDefaultFile("config.yml");
            createDefaultFile("database.yml");
            createDefaultFile("message.yml");
        } catch (IOException e) {
            plugin.getLogger().error("Failed to create default configuration files", e);
        }
    }

    private void createDefaultFile(String filename) throws IOException {
        Path filePath = configDir.resolve(filename);
        if (!Files.exists(filePath)) {
            try (InputStream in = getClass().getClassLoader().getResourceAsStream(filename)) {
                if (in != null) {
                    Files.copy(in, filePath);
                }
            }
        }
    }

    public File getConfigFile(String filename) {
        return configDir.resolve(filename).toFile();
    }

    private final List<Runnable> pendingSyncTasks = new ArrayList<>();

    private synchronized void addSyncTask(Runnable task) {
        pendingSyncTasks.add(task);
    }

    private synchronized void processSyncTasks() {
        for (Runnable task : pendingSyncTasks) {
            task.run();
        }
        pendingSyncTasks.clear();
    }

    @Override
    public CPlayer getplayer(PlayerData pd) {
        ServerPlayerEntity player = plugin.getServer().getPlayerManager().getPlayer(pd.getUniqueId());
        return new CPlayer(pd.getUniqueId()) {
            @Override
            public String getName() {
                return player != null ? player.getName().getString() : pd.getName();
            }

            @Override
            public boolean hasPermission(String permission) {
                return player != null && player.hasPermissionLevel(4); // TODO: Implement proper permission system
            }

            @Override
            public void sendMessage(String message) {
                if (player != null) {
                    player.sendMessage(Text.literal(message));
                }
            }

            @Override
            public boolean isOnline() {
                return player != null && !player.isDisconnected();
            }

            @Override
            public void kickPlayer(String message) {
                if (player != null) {
                    player.networkHandler.disconnect(Text.literal(message));
                }
            }
        };
    }

    @Override
    public boolean getOnlinePlayersisEmpty() {
        return plugin.getServer().getPlayerManager().getPlayerList().isEmpty();
    }

    @Override
    public int getOnlinePlayerSize() {
        return plugin.getServer().getPlayerManager().getPlayerList().size();
    }

    @Override
    public List<UUID> getOnlinePlayersUUIDs() {
        List<UUID> uuids = new ArrayList<>();
        plugin.getServer().getPlayerManager().getPlayerList().forEach(player -> uuids.add(player.getUuid()));
        return uuids;
    }

    @Override
    public void broadcastMessage(String message) {
        plugin.getServer().getPlayerManager().broadcast(Text.literal(message), false);
    }

    @Override
    public UUID NameToUUID(String name) {
        ServerPlayerEntity player = plugin.getServer().getPlayerManager().getPlayer(name);
        return player != null ? player.getUuid() : null;
    }

    @Override
    public boolean isSync() {
        return !plugin.getServer().isOnThread();
    }

    @Override
    public void runTaskAsynchronously(Runnable runnable) {
        scheduler.execute(runnable);
    }

    @Override
    public void runTaskLaterAsynchronously(Runnable runnable, long delay) {
        scheduler.schedule(runnable, delay * 50, TimeUnit.MILLISECONDS);
    }

    @Override
    public void sendPluginMessage(String channel, ByteArrayOutputStream stream) {
        if (plugin.getServer() == null) return;
        ServerPlayerEntity player = plugin.getServer().getPlayerManager().getPlayerList().stream().findFirst().orElse(null);
        if (player != null) {
            messaging.sendPluginMessage(player, stream.toByteArray());
        }
    }

    @Override
    public void registerIncomingPluginChannel(String channel, String classname) {
        // No-op for Fabric
    }

    @Override
    public void registerOutgoingPluginChannel(String channel) {
        // No-op for Fabric
    }

    @Override
    public void unregisterIncomingPluginChannel(String channel, String classname) {
        // No-op for Fabric
    }

    @Override
    public void unregisterOutgoingPluginChannel(String channel) {
        // No-op for Fabric
    }

    public void disable() {
        try {
            scheduler.shutdown();
            scheduler.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
        }
    }

    public void logger(String message, int type, String prefix) {
        String logMessage = prefix != null ? prefix + " " + message : message;
        switch (type) {
            case 0:
                plugin.getLogger().info(logMessage);
                break;
            case 1:
                plugin.getLogger().error(logMessage);
                break;
            case 2:
                plugin.getLogger().warn(logMessage);
                break;
            default:
                plugin.getLogger().info(logMessage);
        }
    }
} 