package com.glisco.deathlog.server;

import com.glisco.deathlog.DeathLogCommon;
import com.glisco.deathlog.client.DeathInfo;
import com.glisco.deathlog.network.DeathLogPackets;
import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.GameProfileArgument;
import net.minecraft.server.permissions.Permission;
import net.minecraft.server.permissions.PermissionLevel;
import net.minecraft.server.players.NameAndId;
import net.minecraft.server.players.PlayerList;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

import static net.minecraft.commands.Commands.*;

public class DeathLogServer implements DedicatedServerModInitializer {

    private static final DynamicCommandExceptionType INVALID_INDEX = new DynamicCommandExceptionType(o -> Component.literal("No DeathInfo found for index " + o));
    private static final DynamicCommandExceptionType NO_PLAYER_FOR_PROFILE = new DynamicCommandExceptionType(o -> Component.literal("Player " + ((GameProfile) o).name() + " is not online"));
    private static final SimpleCommandExceptionType NO_DEATHS = new SimpleCommandExceptionType(Component.literal("No DeathInfo found"));

    private static ServerDeathLogStorage storage;

    @Override
    public void onInitializeServer() {
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            storage = new ServerDeathLogStorage();
            DeathLogCommon.setStorage(storage);
        });

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(literal("deathlog").then(literal("list").requires(hasPermission("deathlog.list"))
                            .then(createProfileArgument().executes(context -> executeList(context, null))
                                    .then(argument("search_term", StringArgumentType.string())
                                            .executes(context -> executeList(context, StringArgumentType.getString(context, "search_term"))))))
                    .then(literal("view").requires(hasPermission("deathlog.view")).then(createProfileArgument().executes(context -> {
                        var player = context.getSource().getPlayer();
                        var profileId = getProfile(context).id();

                        DeathLogPackets.CHANNEL.serverHandle(player).send(new DeathLogPackets.OpenScreen(
                                profileId,
                                player.level().getServer().getPlayerList().getPlayer(profileId) != null,
                                DeathLogServer.getStorage().getDeathInfoList(profileId)
                        ));
                        return 0;
                    }))).then(literal("restore").requires(hasPermission("deathlog.restore")).then(createProfileArgument().then(argument("index", IntegerArgumentType.integer()).executes(context -> {
                        int index = IntegerArgumentType.getInteger(context, "index");
                        return executeRestore(context, index);
                    })).then(literal("latest").executes(DeathLogServer::executeRestoreLatest)))));
        });
    }

    private int executeList(CommandContext<CommandSourceStack> context, @Nullable String filter) throws CommandSyntaxException {
        var profile = getProfile(context);

        var deathInfoList = DeathLogServer.getStorage().getDeathInfoList(profile.id());
        if (filter != null) deathInfoList = deathInfoList.stream().filter(info -> info.createSearchString().contains(filter.toLowerCase())).toList();

        final var infoListSize = deathInfoList.size();
        for (int i = 0; i < infoListSize; i++) {
            DeathInfo deathInfo = deathInfoList.get(i);
            var leftText = deathInfo.getLeftColumnText().iterator();
            var rightText = deathInfo.getRightColumnText().iterator();

            int idx = i;

            context.getSource().sendSuccess(() -> Component.literal(""), false);
            context.getSource().sendSuccess(() -> Component.literal("§7-- §aBegin §bDeath Info Entry [" + idx + "]§7--"), false);
            while (leftText.hasNext()) {
                context.getSource().sendSuccess(() -> ((MutableComponent) leftText.next()).append(Component.literal(": ")).append(((MutableComponent) rightText.next()).withStyle(ChatFormatting.WHITE)), false);
            }
            context.getSource().sendSuccess(() -> Component.literal("§7-- §cEnd §bDeath Info Entry [" + idx + "]§7--"), false);
        }

        if (infoListSize > 0) context.getSource().sendSuccess(() -> Component.literal(""), false);
        context.getSource().sendSuccess(() -> Component.literal("Queried §b" + infoListSize + "§r death info entries for player ").append("§b" + profile.name()), false);

        return infoListSize;
    }

    private static Predicate<CommandSourceStack> hasPermission(String node) {
        return DeathLogCommon.usePermissions()
                ? source -> ((java.util.function.Predicate) me.lucko.fabric.api.permissions.v0.Permissions.require(node, 4)).test(source)
                : source -> source.permissions().hasPermission(new Permission.HasCommandLevel(PermissionLevel.ADMINS));
    }

    public static boolean hasPermission(ServerPlayer player, String node) {
        return DeathLogCommon.usePermissions()
                ? ((java.util.function.Predicate) me.lucko.fabric.api.permissions.v0.Permissions.require(node, 4)).test(player.createCommandSourceStack())
                : player.permissions().hasPermission(new Permission.HasCommandLevel(PermissionLevel.ADMINS));
    }

    private static int executeRestoreLatest(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        restore(context, deathInfos -> deathInfos.size() - 1, index -> NO_DEATHS.create());
        return 0;
    }

    private static int executeRestore(CommandContext<CommandSourceStack> context, int index) throws CommandSyntaxException {
        restore(context, deathInfos -> index, INVALID_INDEX::create);
        return 0;
    }

    private static void restore(CommandContext<CommandSourceStack> context, Function<List<DeathInfo>, Integer> indexProvider, Function<Integer, CommandSyntaxException> exceptionProvider) throws CommandSyntaxException {
        final var targetProfile = getProfile(context);
        final var deathInfoList = DeathLogServer.getStorage().getDeathInfoList(targetProfile.id());

        final var targetPlayer = context.getSource().getServer().getPlayerList().getPlayer(targetProfile.id());
        if (targetPlayer == null) throw NO_PLAYER_FOR_PROFILE.create(targetProfile);

        final int index = indexProvider.apply(deathInfoList);
        if (deathInfoList.isEmpty() || index > deathInfoList.size() - 1) throw exceptionProvider.apply(index);

        deathInfoList.get(index).restore(targetPlayer);
    }

    private static NameAndId getProfile(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        var profileArgument = GameProfileArgument.getGameProfiles(context, "player");
        return profileArgument.iterator().next();
    }

    private static RequiredArgumentBuilder<CommandSourceStack, GameProfileArgument.Result> createProfileArgument() {
        return argument("player", GameProfileArgument.gameProfile()).suggests((context, builder) -> {
            PlayerList playerManager = context.getSource().getServer().getPlayerList();
            return SharedSuggestionProvider.suggest(playerManager.getPlayers().stream().map((player) -> player.getGameProfile().name()), builder);
        });
    }

    public static ServerDeathLogStorage getStorage() {
        return storage;
    }
}
