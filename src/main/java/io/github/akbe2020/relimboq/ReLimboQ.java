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
import io.github.akbe2020.relimboq.commands.ReLimboQCommand;
import io.github.akbe2020.relimboq.handler.QueueHandler;
import io.github.akbe2020.relimboq.listener.QueueListener;
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
        id = "relimboq",
        name = "ReLimboQ",
        version = "0.1.2",
        authors = {
                "four4tReS"
        },
        dependencies = {
                @Dependency(id = "limboapi")
        }
)
public class ReLimboQ {
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
        this.configFile = new File(dataDirectoryFile, "config.yml");

        this.factory = (LimboFactory) this.server.getPluginManager().getPlugin("limboapi").flatMap(PluginContainer::getInstance).orElseThrow();
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
        this.reload();
    }

    public void reload() throws APIException {
        Config.IMP.reload(this.configFile);
        ComponentSerializer<Component, Component, String> serializer = Serializers.valueOf(Config.IMP.MAIN.SERIALIZER.toUpperCase(Locale.ROOT))
                .getSerializer();
        if (serializer == null) {
            LOGGER.warn("The specified serializer could not be founded, using default. (LEGACY_AMPERSAND)");
            setSerializer(new Serializer(Objects.requireNonNull(Serializers.LEGACY_AMPERSAND.getSerializer())));
        } else {
            setSerializer(new Serializer(serializer));
        }

        this.queueMessage = Config.IMP.MESSAGES.QUEUE_MESSAGE;
        this.alwaysPutToQueue = Config.IMP.MAIN.ALWAYS_PUT_TO_QUEUE;
        this.serverOfflineMessage = Config.IMP.MESSAGES.SERVER_OFFLINE;
        this.checkInterval = Config.IMP.MAIN.CHECK_INTERVAL;

        VirtualWorld queueWorld = this.factory.createVirtualWorld(Dimension.valueOf(Config.IMP.MAIN.WORLD.DIMENSION),
                Config.IMP.MAIN.WORLD.X,
                Config.IMP.MAIN.WORLD.Y,
                Config.IMP.MAIN.WORLD.Z,
                Config.IMP.MAIN.WORLD.YAW,
                Config.IMP.MAIN.WORLD.PITCH
        );

        this.queueServer = this.factory.createLimbo(queueWorld)
                .setName(Config.IMP.MAIN.WORLD.NAME)
                .setWorldTime(Config.IMP.MAIN.WORLD.WORLDTIME).
                setGameMode(GameMode.valueOf(Config.IMP.MAIN.WORLD.GAMEMODE))
                .setViewDistance(Config.IMP.MAIN.WORLD.VIEWDISTANCE)
                .setSimulationDistance(Config.IMP.MAIN.WORLD.SIMULATIONDISTANCE);
        this.server.getEventManager().register(this, new QueueListener(this));

        CommandManager manager = this.server.getCommandManager();
        manager.unregister("relimboq");
        manager.register("relimboq", new ReLimboQCommand(this), "rlq", "queue");

        Optional<RegisteredServer> server = this.getServer().getServer(Config.IMP.MAIN.SERVER);
        server.ifPresentOrElse(registeredServer -> {
            this.targetServer = registeredServer;
            this.startPingTask();
            this.startQueueTask();
        }, () -> LOGGER.error("Server " + Config.IMP.MAIN.SERVER + " doesn't exists!"));

        this.exarotonEnabled = Config.IMP.EXAROTON.ENABLED;
        if (this.exarotonEnabled) {
            exaroton = new Exaroton(Config.IMP.EXAROTON.TOKEN, Config.IMP.EXAROTON.ADDRESS);
        }
    }

    public boolean isAlwaysPutToQueue() {
        return alwaysPutToQueue;
    }

    public void queuePlayer(Player player) {
        this.queueServer.spawnPlayer(player, new QueueHandler(this));
    }

    public ProxyServer getServer() {
        return this.server;
    }

    public RegisteredServer getTargetServer() {
        return targetServer;
    }

    private void startQueueTask() {
        if (this.queueTask != null) {
            this.queueTask.cancel();
        }
        this.queueTask = this.getServer().getScheduler().buildTask(this, () -> {
            switch (this.serverStatus) {
                case NORMAL -> {
                    if (!this.queuedPlayers.isEmpty()) {
                        this.queuedPlayers.getFirst().disconnect();
                    }
                }
                case FULL -> {
                    AtomicInteger i = new AtomicInteger(0);
                    this.queuedPlayers.forEach(
                            (p) -> p.getProxyPlayer().sendMessage(SERIALIZER.deserialize(MessageFormat.format(this.queueMessage, i.incrementAndGet()))));
                }
                case OFFLINE -> this.queuedPlayers.forEach((p) -> p.getProxyPlayer().sendMessage(SERIALIZER.deserialize(this.serverOfflineMessage)));
            }
        }).repeat(this.checkInterval, TimeUnit.SECONDS).schedule();
    }

    private void startPingTask() {
        if (this.pingTask != null) {
            this.pingTask.cancel();
        }
        this.pingTask = this.getServer().getScheduler().buildTask(this, () -> {
            try {
                ServerPing serverPing = this.targetServer.ping().get();
                if (serverPing.getPlayers().isPresent()) {
                    ServerPing.Players players = serverPing.getPlayers().get();
                    if (players.getOnline() >= players.getMax()) {
                        this.serverStatus = ServerStatus.FULL;
                    } else {
                        this.serverStatus = ServerStatus.NORMAL;
                    }
                }
            } catch (InterruptedException | ExecutionException ignored) {
                this.serverStatus = ServerStatus.OFFLINE;
            }

            if (this.exarotonEnabled) {
                if (exaroton.isOffline()) {
                    this.serverStatus = ServerStatus.OFFLINE;
                }
            }
        }).repeat(this.checkInterval, TimeUnit.SECONDS).schedule();
    }

    enum ServerStatus {
        NORMAL,
        FULL,
        OFFLINE
    }
}
