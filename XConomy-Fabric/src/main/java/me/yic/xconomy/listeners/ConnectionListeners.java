package me.yic.xconomy.listeners;

import me.yic.xconomy.data.DataCon;
import me.yic.xconomy.data.DataFormat;
import me.yic.xconomy.data.syncdata.PlayerData;
import me.yic.xconomy.data.sql.SQLCreateNewAccount;
import me.yic.xconomy.adapter.comp.CPlayer;
import net.minecraft.server.network.ServerPlayerEntity;

public class ConnectionListeners {

    public static void onPlayerJoin(ServerPlayerEntity player) {
        if (player == null) {
            return;
        }

        // Create CPlayer wrapper
        CPlayer cPlayer = new CPlayer(player.getUuid()) {
            @Override
            public String getName() {
                return player.getName().getString();
            }

            @Override
            public boolean hasPermission(String permission) {
                return player.hasPermissionLevel(4);
            }

            @Override
            public void sendMessage(String message) {
                player.sendMessage(net.minecraft.text.Text.literal(message));
            }

            @Override
            public boolean isOnline() {
                return !player.isDisconnected();
            }

            @Override
            public void kickPlayer(String message) {
                player.networkHandler.disconnect(net.minecraft.text.Text.literal(message));
            }
        };

        // Create or load player account
        if (!SQLCreateNewAccount.newPlayer(player.getUuid(), player.getName().getString(), cPlayer)) {
            return;
        }

        // Load player data
        PlayerData pd = DataCon.getPlayerData(player.getUuid());
        if (pd == null) {
            pd = new PlayerData(player.getUuid(), player.getName().getString(), DataFormat.formatString("0"));
        }
    }

    public static void onPlayerQuit(ServerPlayerEntity player) {
        if (player == null) {
            return;
        }

        // Clean up player data from cache
        DataCon.deletedatafromcache(player.getUuid());
    }
} 