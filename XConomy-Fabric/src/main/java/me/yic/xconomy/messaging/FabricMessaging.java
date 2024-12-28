package me.yic.xconomy.messaging;

import me.yic.xconomy.XConomyFabric;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;

public class FabricMessaging {
    private static final Identifier CHANNEL = Identifier.of("xconomy_fabric", "sync");
    private static final List<Consumer<byte[]>> messageListeners = new ArrayList<>();
    private final XConomyFabric plugin;
    private final ConcurrentLinkedQueue<byte[]> pendingMessages = new ConcurrentLinkedQueue<>();
    private static final int MAX_RETRY_ATTEMPTS = 3;
    private static final long RETRY_DELAY_TICKS = 20L * 5; // 5 seconds in ticks
    private long ticksUntilNextRetry = 0;
    private int currentAttempt = 0;

    public FabricMessaging(XConomyFabric plugin) {
        this.plugin = plugin;
        init();
    }

    private void init() {
        PayloadTypeRegistry.playS2C().register(PluginMessagePayload.ID, PluginMessagePayload.CODEC);
        PayloadTypeRegistry.playC2S().register(PluginMessagePayload.ID, PluginMessagePayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(PluginMessagePayload.ID, this::receive);
        
        // Register tick event for handling retries
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (!pendingMessages.isEmpty() && ticksUntilNextRetry > 0) {
                ticksUntilNextRetry--;
                if (ticksUntilNextRetry <= 0) {
                    scheduleRetry(currentAttempt);
                }
            }
        });
    }

    private void receive(PluginMessagePayload payload, ServerPlayNetworking.Context context) {
        context.server().execute(() -> {
            for (Consumer<byte[]> listener : messageListeners) {
                listener.accept(payload.data);
            }
        });
    }

    public void sendPluginMessage(byte[] message) {
        if (plugin.getServer() == null) return;

        for (ServerPlayerEntity player : plugin.getServer().getPlayerManager().getPlayerList()) {
            ServerPlayNetworking.send(player, new PluginMessagePayload(message));
        }
    }

    public void sendPluginMessage(ServerPlayerEntity player, byte[] message) {
        if (plugin.getServer() == null || player == null) return;
        ServerPlayNetworking.send(player, new PluginMessagePayload(message));
    }

    public void sendPluginMessage(UUID playerUUID, byte[] message) {
        if (plugin.getServer() == null) return;

        ServerPlayerEntity player = plugin.getServer().getPlayerManager().getPlayer(playerUUID);
        if (player == null) return;

        ServerPlayNetworking.send(player, new PluginMessagePayload(message));
    }

    // Send message to the first available player, retrying if no players are online
    public void sendPluginMessageReliably(byte[] message) {
        if (plugin.getServer() == null) return;

        ServerPlayerEntity player = plugin.getServer().getPlayerManager().getPlayerList().stream().findFirst().orElse(null);
        if (player != null) {
            ServerPlayNetworking.send(player, new PluginMessagePayload(message));
        } else {
            // Add message to pending queue and schedule retry
            pendingMessages.offer(message);
            currentAttempt = 1;
            ticksUntilNextRetry = RETRY_DELAY_TICKS;
            plugin.getLogger().warn("No players online to send plugin message. Message queued for retry.");
        }
    }

    private void scheduleRetry(int attempt) {
        if (attempt > MAX_RETRY_ATTEMPTS) {
            plugin.getLogger().error("Failed to send plugin message after {} attempts. Message will be discarded.", MAX_RETRY_ATTEMPTS);
            pendingMessages.poll(); // Remove the failed message
            currentAttempt = 0;
            return;
        }

        // Attempt to send the message
        plugin.getServer().execute(() -> {
            byte[] message = pendingMessages.peek();
            if (message == null) {
                currentAttempt = 0;
                return;
            }

            ServerPlayerEntity player = plugin.getServer().getPlayerManager().getPlayerList().stream().findFirst().orElse(null);
            if (player != null) {
                ServerPlayNetworking.send(player, new PluginMessagePayload(message));
                pendingMessages.poll(); // Remove the successfully sent message
                plugin.getLogger().info("Successfully sent queued plugin message on attempt {}", attempt);
                currentAttempt = 0;
            } else {
                plugin.getLogger().warn("Retry attempt {} failed. Will retry again in {} seconds.", attempt, RETRY_DELAY_TICKS / 20);
                currentAttempt = attempt + 1;
                ticksUntilNextRetry = RETRY_DELAY_TICKS;
            }
        });
    }

    public void registerMessageListener(Consumer<byte[]> listener) {
        messageListeners.add(listener);
    }

    public void unregisterMessageListener(Consumer<byte[]> listener) {
        messageListeners.remove(listener);
    }

    public void unregisterAll() {
        messageListeners.clear();
        ServerPlayNetworking.unregisterGlobalReceiver(CHANNEL);
        pendingMessages.clear();
        currentAttempt = 0;
        ticksUntilNextRetry = 0;
    }

    public static class PluginMessagePayload implements CustomPayload {
        public static final CustomPayload.Id<PluginMessagePayload> ID = new CustomPayload.Id<>(CHANNEL);
        public static final PacketCodec<RegistryByteBuf, PluginMessagePayload> CODEC = PacketCodec.of(PluginMessagePayload::write, PluginMessagePayload::new).cast();

        private final byte[] data;

        private PluginMessagePayload(byte[] data) {
            this.data = data;
        }

        private PluginMessagePayload(PacketByteBuf buf) {
            this.data = new byte[buf.readableBytes()];
            buf.readBytes(this.data);
        }

        private void write(PacketByteBuf buf) {
            buf.writeBytes(this.data);
        }

        @Override
        public Id<PluginMessagePayload> getId() {
            return ID;
        }
    }
}
