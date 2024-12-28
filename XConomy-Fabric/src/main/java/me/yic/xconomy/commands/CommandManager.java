package me.yic.xconomy.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import me.lucko.fabric.api.permissions.v0.Permissions;
import me.yic.xconomy.info.DefaultConfig;
import me.yic.xconomy.lang.MessagesManager;
import me.yic.xconomy.utils.FabricEconomyCore;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.UUID;

public class CommandManager {

    public static void registerCommands(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess) {
        // /money command
        dispatcher.register(LiteralArgumentBuilder.<ServerCommandSource>literal("money")
                .requires(Permissions.require("xconomy.command.money", true))
                .executes(context -> {
                    ServerPlayerEntity player = context.getSource().getPlayer();
                    if (player == null) {
                        context.getSource().sendMessage(Text.literal(MessagesManager.systemMessage("console")));
                        return 0;
                    }
                    showBalance(player.getUuid(), context);
                    return 1;
                })
                .then(RequiredArgumentBuilder.<ServerCommandSource, String>argument("player", StringArgumentType.word())
                        .executes(context -> {
                            String targetName = StringArgumentType.getString(context, "player");
                            UUID targetUUID = FabricEconomyCore.getUUIDByName(targetName);
                            if (targetUUID == null) {
                                context.getSource().sendMessage(Text.literal(MessagesManager.systemMessage("noplayer")));
                                return 0;
                            }
                            showBalance(targetUUID, context);
                            return 1;
                        })));

        // /balance command (alias for /money)
        dispatcher.register(LiteralArgumentBuilder.<ServerCommandSource>literal("balance")
                .requires(Permissions.require("xconomy.command.balance", true))
                .executes(context -> {
                    ServerPlayerEntity player = context.getSource().getPlayer();
                    if (player == null) {
                        context.getSource().sendMessage(Text.literal(MessagesManager.systemMessage("console")));
                        return 0;
                    }
                    showBalance(player.getUuid(), context);
                    return 1;
                }));

        // /pay command
        dispatcher.register(LiteralArgumentBuilder.<ServerCommandSource>literal("pay")
                .requires(Permissions.require("xconomy.command.pay", true))
                .then(RequiredArgumentBuilder.<ServerCommandSource, String>argument("player", StringArgumentType.word())
                        .then(RequiredArgumentBuilder.<ServerCommandSource, String>argument("amount", StringArgumentType.greedyString())
                                .executes(context -> {
                                    ServerPlayerEntity player = context.getSource().getPlayer();
                                    String targetName = StringArgumentType.getString(context, "player");
                                    String amount = StringArgumentType.getString(context, "amount");
                                    
                                    if (!DefaultConfig.config.getBoolean("Economy.Pay.Enable")) {
                                        context.getSource().sendMessage(Text.literal(MessagesManager.systemMessage("paydisabled")));
                                        return 0;
                                    }

                                    UUID targetUUID = FabricEconomyCore.getUUIDByName(targetName);
                                    if (targetUUID == null) {
                                        context.getSource().sendMessage(Text.literal(MessagesManager.systemMessage("noplayer")));
                                        return 0;
                                    }

                                    if (targetUUID.equals(player.getUuid())) {
                                        context.getSource().sendMessage(Text.literal(MessagesManager.systemMessage("payself")));
                                        return 0;
                                    }

                                    try {
                                        double value = Double.parseDouble(amount);
                                        if (value <= 0) {
                                            context.getSource().sendMessage(Text.literal(MessagesManager.systemMessage("invalidamount")));
                                            return 0;
                                        }

                                        if (FabricEconomyCore.pay(player.getUuid(), targetUUID, value)) {
                                            String formattedAmount = FabricEconomyCore.formatBalance(value);
                                            context.getSource().sendMessage(Text.literal(MessagesManager.systemMessage("paysuccess")
                                                    .replace("%amount%", formattedAmount)
                                                    .replace("%player%", targetName)));
                                            return 1;
                                        } else {
                                            context.getSource().sendMessage(Text.literal(MessagesManager.systemMessage("notenough")));
                                            return 0;
                                        }
                                    } catch (NumberFormatException e) {
                                        context.getSource().sendMessage(Text.literal(MessagesManager.systemMessage("invalidamount")));
                                        return 0;
                                    }
                                }))));

        // /baltop command
        dispatcher.register(LiteralArgumentBuilder.<ServerCommandSource>literal("baltop")
                .requires(Permissions.require("xconomy.command.baltop", true))
                .executes(context -> {
                    FabricEconomyCore.showBalanceTop();
                    return 1;
                }));

        // /paytoggle command
        dispatcher.register(LiteralArgumentBuilder.<ServerCommandSource>literal("paytoggle")
                .requires(Permissions.require("xconomy.command.paytoggle", true))
                .executes(context -> {
                    ServerPlayerEntity player = context.getSource().getPlayer();
                    if (player == null) {
                        context.getSource().sendMessage(Text.literal(MessagesManager.systemMessage("console")));
                        return 0;
                    }
                    boolean newState = FabricEconomyCore.togglePayState(player.getUuid());
                    context.getSource().sendMessage(Text.literal(MessagesManager.systemMessage(newState ? "paytoggle_on" : "paytoggle_off")));
                    return 1;
                }));

        // /xconomy reload command
        dispatcher.register(LiteralArgumentBuilder.<ServerCommandSource>literal("xconomy")
                .requires(Permissions.require("xconomy.admin.reload", true))
                .then(LiteralArgumentBuilder.<ServerCommandSource>literal("reload")
                        .executes(context -> {
                            FabricEconomyCore.reload();
                            context.getSource().sendMessage(Text.literal(MessagesManager.systemMessage("reload")));
                            return 1;
                        })));
    }

    private static void showBalance(UUID uuid, CommandContext<ServerCommandSource> context) {
        double balance = FabricEconomyCore.getBalance(uuid);
        String formattedBalance = FabricEconomyCore.formatBalance(balance);
        context.getSource().sendMessage(Text.literal(MessagesManager.systemMessage("balance")
                .replace("%balance%", formattedBalance)));
    }
} 