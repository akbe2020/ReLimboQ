/*
 * Copyright (C) 2024 four4tReS
 * Copyright (C) 2022 - 2023 Elytrium
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package io.github.akbe2020.relimboq;

import com.exaroton.api.APIException;
import com.google.inject.Inject;
import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Dependency;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.PluginContainer;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.proxy.server.ServerPing;
import com.velocitypowered.api.scheduler.ScheduledTask;
import io.github.akbe2020.relimboq.commands.Reload;
import io.github.akbe2020.relimboq.handlers.QueueHandler;
import io.github.akbe2020.relimboq.listeners.QueueListener;
import net.elytrium.commons.kyori.serialization.Serializer;
import net.elytrium.commons.kyori.serialization.Serializers;
import net.elytrium.limboapi.api.Limbo;
import net.elytrium.limboapi.api.LimboFactory;
import net.elytrium.limboapi.api.chunk.Dimension;
import net.elytrium.limboapi.api.chunk.VirtualWorld;
import net.elytrium.limboapi.api.player.GameMode;
import net.elytrium.limboapi.api.player.LimboPlayer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.ComponentSerializer;
import org.slf4j.Logger;

import java.io.File;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.LinkedList;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Plugin(
        id = ReLimboQ.SLUG,
        name = "ReLimboQ",
        version = "0.1.3",
        authors = {
                "four4tReS"
        },
        dependencies = {
                @Dependency(id = "limboapi")
        }
)
public class ReLimboQ {
    public static final String SLUG = "relimboq";
    @Inject
    private static Logger LOGGER;
    private static Serializer SERIALIZER;
    private final ProxyServer server;
    private final File configFile;
    private final LimboFactory factory;
    public LinkedList<LimboPlayer> queuedPlayers = new LinkedList<>();
    private RegisteredServer targetServer;
    private ServerStatus serverStatus = ServerStatus.NORMAL;
    private Limbo queueServer;
    private boolean alwaysPutToQueue;
    private String queueMessage;
    private String connectingMessage;
    private String serverOfflineMessage;
    private int checkInterval;
    private ScheduledTask queueTask;
    private ScheduledTask pingTask;
    private Exaroton exaroton;
    private boolean exarotonEnabled;

    @Inject
    public ReLimboQ(Logger logger, ProxyServer server, @DataDirectory Path dataDirectory) {
        setLogger(logger);

        this.server = server;

        File dataDirectoryFile = dataDirectory.toFile();
        configFile = new File(dataDirectoryFile, "config.yml");

        factory = (LimboFactory) this.server.getPluginManager().getPlugin("limboapi").flatMap(PluginContainer::getInstance).orElseThrow();
    }

    private static void setLogger(Logger logger) {
        LOGGER = logger;
    }

    public static Serializer getSerializer() {
        return SERIALIZER;
    }

    private static void setSerializer(Serializer serializer) {
        SERIALIZER = serializer;
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) throws APIException {
        reload();
    }

    public void reload() throws APIException {
        Config.IMP.reload(configFile);
        ComponentSerializer<Component, Component, String> serializer = Serializers.valueOf(Config.IMP.MAIN.SERIALIZER.toUpperCase(Locale.ROOT))
                .getSerializer();
        if (serializer == null) {
            LOGGER.warn("The specified serializer could not be founded, using default. (LEGACY_AMPERSAND)");
            setSerializer(new Serializer(Objects.requireNonNull(Serializers.LEGACY_AMPERSAND.getSerializer())));
        } else {
            setSerializer(new Serializer(serializer));
        }

        queueMessage = Config.IMP.MESSAGES.QUEUE_MESSAGE;
        connectingMessage = Config.IMP.MESSAGES.CONNECTING_MESSAGE;
        alwaysPutToQueue = Config.IMP.MAIN.ALWAYS_PUT_TO_QUEUE;
        serverOfflineMessage = Config.IMP.MESSAGES.SERVER_OFFLINE;
        checkInterval = Config.IMP.MAIN.CHECK_INTERVAL;

        VirtualWorld queueWorld = factory.createVirtualWorld(Dimension.valueOf(Config.IMP.MAIN.WORLD.DIMENSION),
                Config.IMP.MAIN.WORLD.X,
                Config.IMP.MAIN.WORLD.Y,
                Config.IMP.MAIN.WORLD.Z,
                Config.IMP.MAIN.WORLD.YAW,
                Config.IMP.MAIN.WORLD.PITCH
        );

        queueServer = factory.createLimbo(queueWorld)
                .setName(Config.IMP.MAIN.WORLD.NAME)
                .setWorldTime(Config.IMP.MAIN.WORLD.WORLD_TIME)
                .setGameMode(GameMode.valueOf(Config.IMP.MAIN.WORLD.GAMEMODE))
                .setViewDistance(Config.IMP.MAIN.WORLD.VIEW_DISTANCE)
                .setSimulationDistance(Config.IMP.MAIN.WORLD.SIMULATION_DISTANCE);
        server.getEventManager().register(this, new QueueListener(this));

        {
            CommandManager manager = server.getCommandManager();
            manager.unregister(SLUG);
            manager.register(SLUG, new Reload(this), "rlq", "queue");
        }

        Optional<RegisteredServer> server = getServer().getServer(Config.IMP.MAIN.SERVER);
        server.ifPresentOrElse(registeredServer -> {
            targetServer = registeredServer;
            startPingTask();
            startQueueTask();
        }, () -> LOGGER.error("Server {} doesn't exists!", Config.IMP.MAIN.SERVER));

        exarotonEnabled = Config.IMP.EXAROTON.ENABLED;
        if (exarotonEnabled) {
            exaroton = new Exaroton(Config.IMP.EXAROTON.TOKEN, Config.IMP.EXAROTON.ADDRESS);
        }
    }

    public boolean isAlwaysPutToQueue() {
        return alwaysPutToQueue;
    }

    public void queuePlayer(Player player) {
        queueServer.spawnPlayer(player, new QueueHandler(this));
    }

    public ProxyServer getServer() {
        return server;
    }

    public RegisteredServer getTargetServer() {
        return targetServer;
    }

    private void startQueueTask() {
        if (queueTask != null) {
            queueTask.cancel();
        }

        queueTask = getServer().getScheduler().buildTask(this, () -> {
            switch (serverStatus) {
                case NORMAL -> {
                    if (!queuedPlayers.isEmpty()) {
                        LimboPlayer player = queuedPlayers.getFirst();
                        player.getProxyPlayer().sendMessage(SERIALIZER.deserialize(connectingMessage));
                        player.disconnect();
                    }
                }
                case FULL -> {
                    AtomicInteger i = new AtomicInteger(0);
                    queuedPlayers.forEach(
                            (p) -> p.getProxyPlayer().sendMessage(SERIALIZER.deserialize(MessageFormat.format(queueMessage, i.incrementAndGet()))));
                }
                case OFFLINE ->
                        queuedPlayers.forEach((p) -> p.getProxyPlayer().sendMessage(SERIALIZER.deserialize(serverOfflineMessage)));
            }
        }).repeat(checkInterval, TimeUnit.SECONDS).schedule();
    }

    private void startPingTask() {
        if (pingTask != null) {
            pingTask.cancel();
        }

        pingTask = getServer().getScheduler().buildTask(this, () -> {
            try {
                ServerPing serverPing = targetServer.ping().get();
                if (serverPing.getPlayers().isPresent()) {
                    ServerPing.Players players = serverPing.getPlayers().get();
                    if (players.getOnline() >= players.getMax()) {
                        serverStatus = ServerStatus.FULL;
                    } else {
                        serverStatus = ServerStatus.NORMAL;
                    }
                }
            } catch (InterruptedException | ExecutionException ignored) {
                serverStatus = ServerStatus.OFFLINE;
            }

            if (exarotonEnabled) {
                if (exaroton.isOffline()) {
                    serverStatus = ServerStatus.OFFLINE;
                }
            }
        }).repeat(checkInterval, TimeUnit.SECONDS).schedule();
    }

    enum ServerStatus {
        NORMAL,
        FULL,
        OFFLINE
    }
}
