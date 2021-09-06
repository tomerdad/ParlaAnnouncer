package me.tomerdad.ParlaAnnouncer.utilities;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.scheduler.ScheduledTask;
import me.tomerdad.ParlaAnnouncer.ParlaAnnouncer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import ninja.leaping.configurate.ConfigurationNode;
import org.slf4j.Logger;

import java.util.*;
import java.util.concurrent.TimeUnit;

public class Utilities {

    private static Map<String, ScheduledTask> timersList;
    private static Map<String, Integer> msgNumbers;

    public static void createTimers(ParlaAnnouncer plugin, ProxyServer server, Logger logger) {
        timersList = new HashMap<>();
        msgNumbers = new HashMap<>();
        Map<Object, ? extends ConfigurationNode> data = Config.nodeConfigMap("announcer");
        for (Object key : data.keySet()) {
            //check if enable
            if (!Config.getrootNode().getNode("announcer", key.toString(), "enable").getBoolean()) {
                continue;
            }

            //timer creator
            try {
                createTimer(plugin, server, key.toString(), logger);
            } catch (Exception e) {
                logger.info(e.toString());
            }
        }
    }

    public static void createTimer(ParlaAnnouncer plugin, ProxyServer server, String key, Logger logger) throws Exception {
        if (Config.getrootNode().getNode("announcer", key).getString() == null) {
            throw new Exception("Task not found");
        } else if (timersList.containsKey(key)) {
            throw new Exception("Task already runs");
        }

        //message-order checker
        messagesOrder(key);

        ScheduledTask task = server.getScheduler()
            .buildTask(plugin, () -> {
                boolean first = true;
                final StringBuilder message = new StringBuilder();
                int msgNum;
                int msgCount = Config.nodeConfigMap("announcer", key, "messages").size();

                //message-order checker
                if (msgNumbers.get(key) == -1 ) {
                    Random rand = new Random();
                    msgNum = rand.nextInt(msgCount)+1;
                } else {
                    if (msgNumbers.get(key) <= msgCount) {
                        msgNum = msgNumbers.get(key);
                        msgNumbers.replace(key, msgNum+1);
                    } else {
                        msgNum = 1;
                        msgNumbers.replace(key, 1);
                    }
                }
                //create message string
                TextComponent.Builder text = Component.text();
                for (String msg : new ArrayList<>(Config.nodeConfigList("announcer", key, "messages", msgNum))){
                    if (first){
                        message.append(msg);
                        text.append(LegacyComponentSerializer.legacyAmpersand().deserialize(msg));
                        first = false;
                    } else {
                        message.append("\n").append(msg);
                        text.append(LegacyComponentSerializer.legacyAmpersand().deserialize("\n" + msg).clickEvent(ClickEvent.clickEvent(ClickEvent.Action.OPEN_URL, "https://google.com")));
                    }
                }

                //placeholders


                //server mode checker
                //Check to which servers send the messages
                String serverMode = Config.getrootNode().getNode("announcer", key, "mode").getString();
                List<String> servers = Config.nodeConfigList("announcer", key, "servers");

                if (Objects.equals(serverMode, "global")) {
                    //all servers
                    server.getAllPlayers().forEach(player -> player.sendMessage(updatePlaceholders(player, message.toString())));

                } else if (Objects.equals(serverMode, "servers")) {
                    //only to servers
                    server.getAllPlayers().forEach(player -> {
                        if (player.getCurrentServer().isPresent()) {
                            if (servers.contains(player.getCurrentServer().get().getServerInfo().getName())) {
                                player.sendMessage(updatePlaceholders(player, message.toString()));
                            }
                        }
                    });

                } else if (Objects.equals(serverMode, "except")) {
                    //not servers
                    server.getAllPlayers().forEach(player -> {
                        if (player.getCurrentServer().isPresent()){
                            if (!servers.contains(player.getCurrentServer().get().getServerInfo().getName()))
                            {
                                player.sendMessage(updatePlaceholders(player, message.toString()));
                            }
                        }
                    });
                }
            })
            .repeat(Config.getrootNode().getNode("announcer", key, "Timer").getInt(), TimeUnit.SECONDS)
            .schedule();
        timersList.put(key ,task);
    }

    public static void messagesOrder(String key) {
        String messageOrder = Config.getrootNode().getNode("announcer", key, "message-order").getString();
        if (Objects.equals(messageOrder, "order")) {
            msgNumbers.put(key, 1);
        } else if (Objects.equals(messageOrder, "random")) {
            msgNumbers.put(key, -1);
        }
    }

    public static void reload(ParlaAnnouncer plugin, ProxyServer server, Logger logger) {
        Utilities.removeTimers();
        Config.setupConfig();
        Utilities.createTimers(plugin, server, logger);
    }

    public static void removeTimers() {
        for (String key : timersList.keySet()) {
            ScheduledTask task = timersList.get(key);
            task.cancel();
        }
        timersList = new HashMap<>();
        msgNumbers = new HashMap<>();
    }

    //for future command
    public static void removeTimer(String key) throws Exception {
        if (!timersList.containsKey(key)) {
            throw new Exception("Task not found");
        }
        ScheduledTask task = timersList.get(key);
        task.cancel();
        if (timersList.keySet().size() == 1){
            timersList = new HashMap<>();
            msgNumbers = new HashMap<>();
        } else {
            timersList.remove(key);
            msgNumbers.remove(key);
        }
    }

    public static List<String> getTimers() {
        return new ArrayList<>(timersList.keySet());
    }

    public static void BroadCast(ProxyServer server, Component message) {
        server.getAllPlayers().forEach(player -> player.sendMessage(message));
    }

    //async
    public static synchronized Component updatePlaceholders(Player player, String msg) {
        if (player.getCurrentServer().isPresent()) {
            msg = msg.replace("%player%", player.getUsername()).replace("%server%", player.getCurrentServer().get().getServerInfo().getName());
        }
        return MiniMessage.get().parse(msg);
    }

    public static TextComponent buildMessage(String msg) {
        String prefix = "[ParlaAnnouncer] ";
        return LegacyComponentSerializer.legacyAmpersand().deserialize(prefix + msg);
    }

}
