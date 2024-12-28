package me.yic.xconomy.utils;

import me.yic.xconomy.data.DataCon;
import me.yic.xconomy.data.DataFormat;
import me.yic.xconomy.data.syncdata.PlayerData;
import me.yic.xconomy.info.PermissionINFO;
import me.yic.xconomy.XConomyLoad;

import java.math.BigDecimal;
import java.util.UUID;

public class FabricEconomyCore {
    public static boolean pay(UUID from, UUID to, double amount) {
        PlayerData fromData = DataCon.getPlayerData(from);
        if (fromData == null) {
            return false;
        }

        BigDecimal balance = fromData.getBalance();
        BigDecimal amountBD = DataFormat.formatdouble(amount);

        if (balance.compareTo(amountBD) < 0) {
            return false;
        }

        DataCon.changeplayerdata("PLAYER_COMMAND", from, amountBD, false, "pay", null);
        DataCon.changeplayerdata("PLAYER_COMMAND", to, amountBD, true, "pay", null);
        return true;
    }

    public static UUID getUUIDByName(String name) {
        PlayerData pd = DataCon.getPlayerData(name);
        return pd != null ? pd.getUniqueId() : null;
    }

    public static double getBalance(UUID uuid) {
        PlayerData pd = DataCon.getPlayerData(uuid);
        return pd != null ? pd.getBalance().doubleValue() : 0.0;
    }

    public static String formatBalance(double amount) {
        return DataFormat.shown(DataFormat.formatdouble(amount));
    }

    public static void showBalanceTop() {
        DataCon.baltop();
    }

    public static boolean togglePayState(UUID uuid) {
        PermissionINFO.setRPaymentPermission(uuid);
        return PermissionINFO.getRPaymentPermission(uuid);
    }

    public static void reload() {
        XConomyLoad.LoadConfig();
        XConomyLoad.Initial();
    }
} 