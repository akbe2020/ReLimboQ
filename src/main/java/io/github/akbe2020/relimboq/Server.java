/*
 * Copyright (C) 2024 four4tReS
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

import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.proxy.server.ServerPing;
import com.velocitypowered.api.scheduler.ScheduledTask;
import net.elytrium.limboapi.api.player.LimboPlayer;

import javax.annotation.Nullable;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class Server {
    private final Config config;
    private final ProxyServer proxyServer;
    private RegisteredServer server;
    private ServerStatus status;
    private final ArrayList<QueuedPlayer> queuedPlayers = new ArrayList<>();
    private ScheduledTask pingTask;
    private ScheduledTask queueTask;
    private final Exaroton exaroton;

    public ArrayList<QueuedPlayer> getQueuedPlayers() {
        return queuedPlayers;
    }

    public String getName() {
        return server.getServerInfo().getName();
    }

    public boolean isAvailable() {
        return status == ServerStatus.AVAILABLE;
    }

    public Server(ProxyServer proxyServer, String name, @Nullable Exaroton exaroton, Config config) {
        this.config = config;
        this.proxyServer = proxyServer;
        this.exaroton = exaroton;

        Optional<RegisteredServer> optionalRegisteredServer = this.proxyServer.getServer(name);
        optionalRegisteredServer.ifPresentOrElse(registeredServer -> {
            server = registeredServer;
            startPingTask();
            startQueueTask();
        }, () -> ReLimboQ.getLogger().error("Server {} doesn't exists!", name));
    }

    @Nullable
    public static Server bruteforceName(ArrayList<Server> servers, String name) {
        for (Server server : servers) {
            if (server.getName().equals(name)) {
                return server;
            }
        }

        return null;
    }

    private ArrayList<QueuedPlayer> getOwnQueuedPlayers() {
        ArrayList<QueuedPlayer> ownQueuedPlayers = new ArrayList<>();

        for (QueuedPlayer queuedPlayer : queuedPlayers) {
            if (queuedPlayer.targetServerName().equals(getName())) {
                ownQueuedPlayers.add(queuedPlayer);
            }
        }

        return ownQueuedPlayers;
    }

    private void startPingTask() {
        if (pingTask != null) {
            pingTask.cancel();
        }

        pingTask = proxyServer.getScheduler().buildTask(this, this::ping).repeat(config.getPingIntervalMs(), TimeUnit.MILLISECONDS).schedule();
    }

    private void startQueueTask() {
        if (queueTask != null) {
            queueTask.cancel();
        }

        queueTask = proxyServer.getScheduler().buildTask(this, this::queue).repeat(config.getPingIntervalMs(), TimeUnit.MILLISECONDS).schedule();
    }

    private void ping() {
        try {
            ServerPing serverPing = server.ping().get();
            if (serverPing.getPlayers().isPresent()) {
                ServerPing.Players players = serverPing.getPlayers().get();
                if (players.getOnline() >= players.getMax()) {
                    status = ServerStatus.FULL;
                } else {
                    status = ServerStatus.AVAILABLE;
                }
            }
        } catch (InterruptedException | ExecutionException ignored) {
            status = ServerStatus.OFFLINE;
        }

        if (exaroton != null && exaroton.isOffline()) {
            status = ServerStatus.OFFLINE;
        }
    }

    private void queue() {
        switch (status) {
            case AVAILABLE -> {
                if (!getOwnQueuedPlayers().isEmpty()) {
                    LimboPlayer player = getOwnQueuedPlayers().get(0).limboPlayer();
                    player.getProxyPlayer().sendMessage(
                            ReLimboQ.getSerializer().deserialize(
                                    config.getConnectingMessage()
                            )
                    );
                    player.disconnect();
                }
            }
            case FULL -> {
                AtomicInteger i = new AtomicInteger(0);
                getOwnQueuedPlayers().forEach(
                        (queuedPlayer) -> queuedPlayer.limboPlayer().getProxyPlayer().sendMessage(
                                ReLimboQ.getSerializer().deserialize(
                                        MessageFormat.format(config.getQueueMessage(), i.incrementAndGet())
                                )
                        )
                );
            }
            case OFFLINE ->
                    getOwnQueuedPlayers().forEach((queuedPlayer) -> queuedPlayer.limboPlayer().getProxyPlayer().sendMessage(
                            ReLimboQ.getSerializer().deserialize(
                                    config.getServerOfflineMessage()
                            )
                    ));
        }
    }
}

enum ServerStatus {
    AVAILABLE,
    FULL,
    OFFLINE
}
