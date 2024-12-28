package me.yic.xconomy.listeners;

import me.yic.xconomy.data.DataCon;
import me.yic.xconomy.data.DataFormat;
import me.yic.xconomy.data.syncdata.PlayerData;
import net.minecraft.server.network.ServerPlayerEntity;

import java.math.BigDecimal;

public class ConnectionListeners {

    public static void onPlayerJoin(ServerPlayerEntity player) {
        if (player == null) {
            return;
        }

        // Load player data
        PlayerData pd = DataCon.getPlayerData(player.getUuid());
        if (pd == null) {
            // Create new player data if it doesn't exist
            pd = new PlayerData(player.getUuid(), player.getName().getString(), DataFormat.formatString("0"));
            DataCon.changeplayerdata("PLAYER_JOIN", player.getUuid(), pd.getBalance(), null, "join", null);
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