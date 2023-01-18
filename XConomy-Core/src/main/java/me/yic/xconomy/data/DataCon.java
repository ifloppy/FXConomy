/*
 *  This file (DataCon.java) is a part of project XConomy
 *  Copyright (C) YiC and contributors
 *
 *  This program is free software: you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License as published by the
 *  Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 *  or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 *  for more details.
 *
 *  You should have received a copy of the GNU General Public License along
 *  with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package me.yic.xconomy.data;

import me.yic.xconomy.AdapterManager;
import me.yic.xconomy.XConomy;
import me.yic.xconomy.XConomyLoad;
import me.yic.xconomy.adapter.comp.CPlayer;
import me.yic.xconomy.adapter.comp.CallAPI;
import me.yic.xconomy.adapter.comp.DataLink;
import me.yic.xconomy.data.caches.Cache;
import me.yic.xconomy.data.caches.CacheNonPlayer;
import me.yic.xconomy.data.redis.RedisPublisher;
import me.yic.xconomy.data.syncdata.PlayerData;
import me.yic.xconomy.data.syncdata.SyncBalanceAll;
import me.yic.xconomy.data.syncdata.SyncData;
import me.yic.xconomy.data.syncdata.SyncDelData;
import me.yic.xconomy.info.MessageConfig;
import me.yic.xconomy.info.RecordInfo;
import me.yic.xconomy.utils.SendPluginMessage;

import java.math.BigDecimal;
import java.util.UUID;

public class DataCon {
    private static final DataLink DataLink = AdapterManager.DATALINK;

    public static PlayerData getPlayerData(UUID uuid) {
        return getPlayerDatai(uuid);
    }

    public static PlayerData getPlayerData(String username) {
        return getPlayerDatai(username);
    }

    public static BigDecimal getAccountBalance(String account) {
        if (XConomyLoad.Config.DISABLE_CACHE){
            DataLink.getBalNonPlayer(account);
        }
        return CacheNonPlayer.getBalanceFromCacheOrDB(account);
    }

    private static <T> PlayerData getPlayerDatai(T u) {
        PlayerData pd = null;

        if (XConomyLoad.Config.DISABLE_CACHE) {
            DataLink.getPlayerData(u);
        }

        if (Cache.CacheContainsKey(u)) {
            pd = Cache.getDataFromCache(u);
        } else {
            DataLink.getPlayerData(u);
            if (Cache.CacheContainsKey(u)) {
                pd = Cache.getDataFromCache(u);
            }
        }
        if (AdapterManager.PLUGIN.getOnlinePlayersisEmpty()) {
            Cache.clearCache();
        }
        return pd;
    }

    public static void deletePlayerData(PlayerData pd) {
        DataLink.deletePlayerData(pd.getUniqueId());
        Cache.removefromCache(pd.getUniqueId());

        if (!(pd instanceof SyncDelData) && XConomyLoad.Config.BUNGEECORD_ENABLE) {
            SendMessTask(new SyncDelData(pd));
        }

        CPlayer cp = DataLink.getplayer(pd);
        if(cp.isOnline()){
            cp.kickPlayer("[XConomy] " + AdapterManager.translateColorCodes(MessageConfig.DELETE_DATA));
        }
    }

    public static boolean hasaccountdatacache(String name) {
        return CacheNonPlayer.CacheContainsKey(name);

    }

    public static boolean containinfieldslist(String name) {
        if (XConomyLoad.Config.NON_PLAYER_ACCOUNT_SUBSTRING != null) {
            for (String field : XConomyLoad.Config.NON_PLAYER_ACCOUNT_SUBSTRING) {
                if (name.contains(field)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static void changeplayerdata(final String type, final UUID uid, final BigDecimal amount, final Boolean isAdd, final String command, final Object comment) {
        PlayerData pd = getPlayerData(uid);
        UUID u = pd.getUniqueId();
        BigDecimal newvalue = amount;
        BigDecimal bal = pd.getBalance();

        RecordInfo ri = new RecordInfo(type, command, comment);

        CallAPI.CallPlayerAccountEvent(u, pd.getName(), bal, amount, isAdd, ri);

        if (isAdd != null) {
            if (isAdd) {
                newvalue = bal.add(amount);
            } else {
                newvalue = bal.subtract(amount);
            }
        }

        Cache.updateIntoCache(u, pd, newvalue);

        if (XConomyLoad.DConfig.canasync && Thread.currentThread().getName().equalsIgnoreCase("Server thread")) {
            XConomyLoad.runTaskAsynchronously(() -> DataLink.save(pd, isAdd, amount, ri));
        } else {
            DataLink.save(pd, isAdd, amount, ri);
        }

        if (XConomyLoad.Config.BUNGEECORD_ENABLE) {
            SendMessTask(pd);
        }
    }


    @SuppressWarnings("ConstantConditions")
    public static void changeaccountdata(final String type, final String u, final BigDecimal amount, final Boolean isAdd, final String command) {
        BigDecimal newvalue = amount;
        BigDecimal balance = CacheNonPlayer.getBalanceFromCacheOrDB(u);

        RecordInfo ri = new RecordInfo(type, command, null);

        CallAPI.CallNonPlayerAccountEvent(u, balance, amount, isAdd, type);
        if (isAdd != null) {
            if (isAdd) {
                newvalue = balance.add(amount);
            } else {
                newvalue = balance.subtract(amount);
            }
        }
        CacheNonPlayer.insertIntoCache(u, newvalue);

        if (XConomyLoad.DConfig.canasync && Thread.currentThread().getName().equalsIgnoreCase("Server thread")) {
            final BigDecimal fnewvalue = newvalue;
            XConomyLoad.runTaskAsynchronously(() -> DataLink.saveNonPlayer(u, amount, fnewvalue, isAdd, ri));
        } else {
            DataLink.saveNonPlayer(u, amount, newvalue, isAdd, ri);
        }
    }

    public static void changeallplayerdata(String targettype, String type, BigDecimal amount, Boolean isAdd, String command, StringBuilder comment) {
        Cache.clearCache();

        RecordInfo ri = new RecordInfo(type, command, comment);

        if (XConomyLoad.DConfig.canasync && Thread.currentThread().getName().equalsIgnoreCase("Server thread")) {
            XConomyLoad.runTaskAsynchronously(() -> DataLink.saveall(targettype, amount, isAdd, ri));
        } else {
            DataLink.saveall(targettype, amount, isAdd, ri);
        }

        boolean isallbool = targettype.equals("all");
        //if (targettype.equals("all")) {
        //} else

        if (XConomyLoad.Config.BUNGEECORD_ENABLE) {
            SendMessTask(new SyncBalanceAll(isallbool, isAdd, amount));
        }
    }

    public static void baltop() {
        Cache.baltop.clear();
        Cache.baltop_papi.clear();
        sumbal();
        DataLink.getTopBal();
    }


    public static void sumbal() {
        Cache.sumbalance = DataFormat.formatString(DataLink.getBalSum());
    }


    public static void SendMessTask(SyncData pd) {
        if (XConomyLoad.DConfig.CacheType().equalsIgnoreCase("Redis")) {
            RedisPublisher publisher = new RedisPublisher(pd.toByteArray(XConomy.syncversion).toByteArray());
            publisher.start();
        }else{
            SendPluginMessage.SendMessTask("xconomy:acb", pd);
        }
    }

}
