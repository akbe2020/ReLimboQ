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
import com.velocitypowered.api.proxy.ProxyServer;
import io.github.akbe2020.relimboq.commands.Reload;
import io.github.akbe2020.relimboq.handlers.QueueHandler;
import io.github.akbe2020.relimboq.listeners.QueueListener;
import net.elytrium.commons.kyori.serialization.Serializer;
import net.elytrium.commons.kyori.serialization.Serializers;
import net.elytrium.limboapi.api.Limbo;
import net.elytrium.limboapi.api.LimboFactory;
import net.elytrium.limboapi.api.chunk.Dimension;
import net.elytrium.limboapi.api.player.GameMode;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.ComponentSerializer;
import org.slf4j.Logger;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Objects;

@Plugin(
        id = ReLimboQ.SLUG,
        name = "ReLimboQ",
        version = "0.2.0",
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
    private static ProxyServer proxyServer;
    private final LimboFactory factory;
    private final Config config;
    private Limbo queueServer;
    private final ArrayList<Server> servers;

    public static Logger getLogger() {
        return LOGGER;
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

    public Config getConfig() {
        return config;
    }

    public static ProxyServer getProxyServer() {
        return proxyServer;
    }

    public ArrayList<Server> getServers() {
        return servers;
    }

    public void addServer(Server server) {
        servers.add(server);
    }

    @Inject
    public ReLimboQ(Logger logger, ProxyServer proxyServer, @DataDirectory Path dataDirectory) {
        setLogger(logger);

        ReLimboQ.proxyServer = proxyServer;
        factory = (LimboFactory) ReLimboQ.proxyServer.getPluginManager().getPlugin("limboapi").flatMap(PluginContainer::getInstance).orElseThrow();

        config = new Config(this, dataDirectory);
        servers = new ArrayList<>();
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) throws APIException {
        reload();
    }

    public void reload() throws APIException {
        config.reload();

        ComponentSerializer<Component, Component, String> serializer = Serializers.valueOf(config.getSerializer()).getSerializer();
        if (serializer == null) {
            LOGGER.warn("The specified serializer could not be founded, using default. (LEGACY_AMPERSAND)");
            setSerializer(new Serializer(Objects.requireNonNull(Serializers.LEGACY_AMPERSAND.getSerializer())));
        } else {
            setSerializer(new Serializer(serializer));
        }
        
        queueServer = createQueueServer();
        proxyServer.getEventManager().register(this, new QueueListener(this));

        {
            CommandManager manager = proxyServer.getCommandManager();
            manager.unregister(SLUG);
            manager.register(SLUG, new Reload(this), "rlq", "queue");
        }
    }

    private Limbo createQueueServer() {
        return factory.createLimbo(
                        factory.createVirtualWorld(
                                Dimension.valueOf(config.getDimension()),
                                config.getX(),
                                config.getY(),
                                config.getZ(),
                                config.getYaw(),
                                config.getPitch()
                        ))
                .setName(config.getLimboName())
                .setWorldTime(config.getWorldTime())
                .setGameMode(GameMode.valueOf(config.getGamemode()))
                .setViewDistance(config.getRenderDistance())
                .setSimulationDistance(config.getSimulationDistance());
    }

    public void queuePlayer(QueuedPlayer queuedPlayer) {
        queueServer.spawnPlayer(
                queuedPlayer.limboPlayer().getProxyPlayer(),
                new QueueHandler(
                        Server.bruteforceName(
                                servers,
                                queuedPlayer.targetServerName()
                        )
                )
        );
    }
}
