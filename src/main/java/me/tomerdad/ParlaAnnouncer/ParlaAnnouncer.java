package me.tomerdad.ParlaAnnouncer;

import com.google.inject.Inject;
import com.velocitypowered.api.command.CommandManager;
import me.tomerdad.ParlaAnnouncer.commands.PACommand;
import me.tomerdad.ParlaAnnouncer.utilities.Utilities;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import me.tomerdad.ParlaAnnouncer.utilities.Config;
import net.kyori.adventure.text.Component;
import java.nio.file.Path;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.slf4j.Logger;

@Plugin(id = "parlaannouncer", name = "Parla Announcer", version = "1.0",
        description = "Announcer plugin", authors = {"tomerdad"})
public class ParlaAnnouncer {

    private final ProxyServer server;
    private final Logger logger;
    public static Path configpath;

    @Inject
    public ParlaAnnouncer(ProxyServer serverr, Logger loggerr, @DataDirectory Path userConfigDirectory) {
        loggerr.info("Loading ParlaAnnouncer");
        server = serverr;
        logger = loggerr;
        configpath = userConfigDirectory;
        Config.setupConfig();

        logger.info("ParlaAnnouncer loaded");

    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {

        LiteralCommandNode<CommandSource> broadcast = LiteralArgumentBuilder
                .<CommandSource>literal("broadcast")
                .requires(ctx -> ctx.hasPermission("parlaannouncer.broadcast"))
                .then(RequiredArgumentBuilder.<CommandSource, String>argument("message", StringArgumentType.greedyString())
                .executes(ctx -> {
                    String broadcastPrefix = Config.getrootNode().getNode("prefix").getString();
                    Component deserialized = Component.text()
                            .append(LegacyComponentSerializer.legacyAmpersand().deserialize(broadcastPrefix + ctx.getArgument("message", String.class)))
                            .build();
                    Utilities.BroadCast(server, deserialized);
                    return 1;
                }).build())
                .build();

        CommandManager manager = server.getCommandManager();

        //parlaAnnouncer command
        manager.register("parlaAnnouncer", new PACommand(this, server, logger));

        manager.register(
                manager.metaBuilder("broadcast").aliases("alert").build(),
                new BrigadierCommand(broadcast)
        );

        logger.info("Commands loaded.");

        Utilities.createTimers(this, server, logger);
        logger.info("Tasks created.");
    }
}