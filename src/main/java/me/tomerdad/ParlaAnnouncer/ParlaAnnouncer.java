package me.tomerdad.ParlaAnnouncer;

import com.google.inject.Inject;
import com.velocitypowered.api.command.CommandManager;
import me.tomerdad.ParlaAnnouncer.utilities.utilities;
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
                    Component deserialized = Component.text()
                            .append(LegacyComponentSerializer.legacyAmpersand().deserialize("&6[&4BroadCast&6]&b " + ctx.getArgument("message", String.class)))
                            .build();
                    utilities.BroadCast(server, deserialized);
                    return 1;
                }).build())
                .build();

        LiteralCommandNode<CommandSource> reload = LiteralArgumentBuilder
                .<CommandSource>literal("parlaannouncer-reload")
                .requires(ctx -> ctx.hasPermission("parlaannouncer.reload"))
                .executes(ctx -> {
                    utilities.reload(this, server);
                    ctx.getSource().sendMessage(Component.text().append(LegacyComponentSerializer.legacyAmpersand().deserialize("&aReload success")).build());
//                    server.sendMessage(Component.text().append(LegacyComponentSerializer.legacyAmpersand().deserialize("&aReload success")).build());
                    return 1;
                })
                .build();

        CommandManager manager = server.getCommandManager();

        manager.register(
                manager.metaBuilder("broadcast").aliases("alert").build(),
                new BrigadierCommand(broadcast)
        );

        manager.register(
                manager.metaBuilder("parlaannouncer-reload").build(),
                new BrigadierCommand(reload)
        );

        logger.info("Commands loaded.");

        utilities.createTimers(this, server);
    }
}