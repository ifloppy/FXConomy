package me.yic.xconomy.economy;

import eu.pb4.common.economy.api.CommonEconomy;
import eu.pb4.common.economy.api.EconomyAccount;
import eu.pb4.common.economy.api.EconomyCurrency;
import eu.pb4.common.economy.api.EconomyProvider;
import eu.pb4.common.economy.api.EconomyTransaction;
import me.yic.xconomy.data.DataCon;
import me.yic.xconomy.data.DataFormat;
import me.yic.xconomy.info.DefaultConfig;
import net.minecraft.server.MinecraftServer;
import net.minecraft.text.Text;
import com.mojang.authlib.GameProfile;
import net.minecraft.util.Identifier;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class XConomyEconomyProvider implements EconomyProvider {
    private static final String PROVIDER_ID = "xconomy";
    private final Map<UUID, EconomyAccount> accountCache = new ConcurrentHashMap<>();
    private final XConomyCurrency currency = new XConomyCurrency();

    @Override
    public Text name() {
        return Text.literal("XConomy");
    }

    @Override
    public String id() {
        return PROVIDER_ID;
    }

    @Override
    public Collection<EconomyCurrency> getCurrencies(MinecraftServer server) {
        return Collections.singleton(currency);
    }

    @Override
    public EconomyCurrency getCurrency(MinecraftServer server, String id) {
        if (id.equals("default")) {
            return currency;
        }
        return null;
    }

    @Override
    public Collection<EconomyAccount> getAccounts(MinecraftServer server, GameProfile profile) {
        EconomyAccount account = accountCache.computeIfAbsent(profile.getId(), uuid -> new XConomyAccount(uuid, profile.getName()));
        return Collections.singleton(account);
    }

    @Override
    public EconomyAccount getAccount(MinecraftServer server, GameProfile profile, String accountId) {
        if (accountId.equals("default")) {
            return accountCache.computeIfAbsent(profile.getId(), uuid -> new XConomyAccount(uuid, profile.getName()));
        }
        return null;
    }

    @Override
    public String defaultAccount(MinecraftServer server, GameProfile profile, EconomyCurrency currency) {
        if (currency == this.currency) {
            return "default";
        }
        return null;
    }

    private class XConomyAccount implements EconomyAccount {
        private final UUID uuid;
        private final String name;

        public XConomyAccount(UUID uuid, String name) {
            this.uuid = uuid;
            this.name = name;
        }

        @Override
        public Text name() {
            return Text.literal(name);
        }

        @Override
        public EconomyProvider provider() {
            return XConomyEconomyProvider.this;
        }

        @Override
        public EconomyCurrency currency() {
            return currency;
        }

        @Override
        public long balance() {
            return DataCon.getPlayerData(uuid).getBalance().longValue();
        }

        @Override
        public EconomyTransaction canDecreaseBalance(long amount) {
            if (amount < 0) {
                return new EconomyTransaction.Simple(false, Text.literal("Amount cannot be negative"), balance(), balance(), amount, this);
            }
            long currentBalance = balance();
            if (currentBalance >= amount) {
                return new EconomyTransaction.Simple(true, Text.literal("Success"), currentBalance - amount, currentBalance, -amount, this);
            }
            return new EconomyTransaction.Simple(false, Text.literal("Insufficient funds"), currentBalance, currentBalance, -amount, this);
        }

        @Override
        public EconomyTransaction canIncreaseBalance(long amount) {
            if (amount < 0) {
                return new EconomyTransaction.Simple(false, Text.literal("Amount cannot be negative"), balance(), balance(), amount, this);
            }
            long currentBalance = balance();
            return new EconomyTransaction.Simple(true, Text.literal("Success"), currentBalance + amount, currentBalance, amount, this);
        }

        @Override
        public void setBalance(long amount) {
            DataCon.changeplayerdata("balance", uuid, BigDecimal.valueOf(amount), false, "API Set Balance", null);
        }

        @Override
        public Identifier id() {
            return Identifier.of(PROVIDER_ID, uuid.toString());
        }

        @Override
        public UUID owner() {
            return uuid;
        }
    }

    private class XConomyCurrency implements EconomyCurrency {
        @Override
        public Text name() {
            return Text.literal(DefaultConfig.config.getString("Currency.singular-name"));
        }

        @Override
        public String formatValue(long value, boolean precise) {
            return String.format(precise ? "%,d" : "%d", value);
        }

        @Override
        public long parseValue(String value) throws NumberFormatException {
            try {
                return Long.parseLong(value);
            } catch (NumberFormatException e) {
                throw new NumberFormatException("Invalid currency value format");
            }
        }

        @Override
        public EconomyProvider provider() {
            return XConomyEconomyProvider.this;
        }

        @Override
        public Identifier id() {
            return Identifier.of(PROVIDER_ID, "default");
        }
    }

    public void register() {
        CommonEconomy.register(PROVIDER_ID, this);
    }
}