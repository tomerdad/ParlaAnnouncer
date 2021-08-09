package me.tomerdad.ParlaAnnouncer.utilities;

import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.scheduler.ScheduledTask;
import me.tomerdad.ParlaAnnouncer.ParlaAnnouncer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import ninja.leaping.configurate.ConfigurationNode;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class utilities {

    private static Map<String, ScheduledTask> timersList;
    private static Map<String, Integer> msgNumbers;

    public static void createTimers(ParlaAnnouncer plugin, ProxyServer server) {
        timersList = new HashMap<>();
        msgNumbers = new HashMap<>();
        Map<Object, ? extends ConfigurationNode> data = Config.nodeConfigMap("announcer");
        for (Object key : data.keySet()) {
            //check if enable
            if (!Config.getrootNode().getNode("announcer", key.toString(), "enable").getBoolean()) {
                continue;
            }

            //message-order checker
            String messageOrder = Config.getrootNode().getNode("announcer", key.toString(), "message-order").getString();
            if (Objects.equals(messageOrder, "order")) {
                    msgNumbers.put(key.toString(), 1);
            } else if (Objects.equals(messageOrder, "random")) {
                msgNumbers.put(key.toString(), -1);
            }

            //timer creator
            ScheduledTask task = createTimer(plugin, server, key.toString());
            timersList.put(key.toString() ,task);
        }
    }

    public static ScheduledTask createTimer(ParlaAnnouncer plugin, ProxyServer server, String key) {
         return server.getScheduler()
            .buildTask(plugin, () -> {
                boolean first = true;
                StringBuilder message = new StringBuilder();
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
                for (String msg : new ArrayList<>(Config.nodeConfigList("announcer", key, "messages", msgNum))){
                    if (first){
                        message = new StringBuilder(msg);
                        first = false;
                    } else {
                        message.append("\n").append(msg);
//                            message = message + "\n" + msg ;
                    }
                }
                //placeholders

                //create message
                Component deserialized = Component.text()
                        .append(LegacyComponentSerializer.legacyAmpersand().deserialize(message.toString()))
                        .build();

                //server mode checker
                String serverMode = Config.getrootNode().getNode("announcer", key, "mode").getString();
                List<String> servers = Config.nodeConfigList("announcer", key, "servers");
                if (Objects.equals(serverMode, "global")) {
                    server.getAllPlayers().forEach(player -> player.sendMessage(deserialized));
                } else if (Objects.equals(serverMode, "servers")) {
                    server.getAllPlayers().forEach(player -> {
                        if (player.getCurrentServer().isPresent()) {
                            if (servers.contains(player.getCurrentServer().get().getServerInfo().getName())) {
                                player.sendMessage(deserialized);
                            }
                        }
                    });
                } else if (Objects.equals(serverMode, "except")) {
                    server.getAllPlayers().forEach(player -> {
                        if (player.getCurrentServer().isPresent()){
                            if (!servers.contains(player.getCurrentServer().get().getServerInfo().getName()))
                            {
                                player.sendMessage(deserialized);
                            }
                        }
                    });
                }
            })
            .repeat(Config.getrootNode().getNode("announcer", key, "Timer").getInt(), TimeUnit.SECONDS)
            .schedule();
    }

    public static void reload(ParlaAnnouncer plugin, ProxyServer server) {
        utilities.removeTimers();
        Config.setupConfig();
        utilities.createTimers(plugin, server);
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
    public static void removeTimer(String key) {
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

    public static void BroadCast(ProxyServer server, Component message) {
        server.getAllPlayers().forEach(player -> player.sendMessage(message));
    }
}
