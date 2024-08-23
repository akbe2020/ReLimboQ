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

import com.exaroton.api.APIException;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import dev.dejvokep.boostedyaml.YamlDocument;
import dev.dejvokep.boostedyaml.block.implementation.Section;
import dev.dejvokep.boostedyaml.dvs.versioning.BasicVersioning;
import dev.dejvokep.boostedyaml.route.Route;
import dev.dejvokep.boostedyaml.settings.dumper.DumperSettings;
import dev.dejvokep.boostedyaml.settings.general.GeneralSettings;
import dev.dejvokep.boostedyaml.settings.loader.LoaderSettings;
import dev.dejvokep.boostedyaml.settings.updater.UpdaterSettings;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Set;

public class Config {
    private final ReLimboQ plugin;
    private YamlDocument document;

    private String serializer;

    public String getSerializer() {
        return serializer;
    }

    public int getPingIntervalMs() {
        return pingIntervalMs;
    }

    public String getLimboName() {
        return limboName;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getZ() {
        return z;
    }

    public float getYaw() {
        return yaw;
    }

    public float getPitch() {
        return pitch;
    }

    public String getDimension() {
        return dimension;
    }

    public int getWorldTime() {
        return worldTime;
    }

    public String getGamemode() {
        return gamemode;
    }

    public int getRenderDistance() {
        return renderDistance;
    }

    public int getSimulationDistance() {
        return simulationDistance;
    }

    public String getQueueMessage() {
        return queueMessage;
    }

    public String getConnectingMessage() {
        return connectingMessage;
    }

    public String getServerOfflineMessage() {
        return serverOfflineMessage;
    }

    public String getReloadedMessage() {
        return reloadedMessage;
    }

    private int pingIntervalMs;

    private String limboName;
    private int x;
    private int y;
    private int z;
    private float yaw;
    private float pitch;

    private String dimension;
    private int worldTime;
    private String gamemode;
    private int renderDistance;
    private int simulationDistance;

    private String queueMessage;
    private String connectingMessage;
    private String serverOfflineMessage;
    private String reloadedMessage;

    public Config(ReLimboQ plugin, @DataDirectory Path dataDirectory) {
        this.plugin = plugin;

        try {
            document = YamlDocument.create(new File(dataDirectory.toFile(), "config.yaml"),
                    getClass().getResourceAsStream("config.yaml"),
                    GeneralSettings.DEFAULT,
                    LoaderSettings
                            .builder()
                            .setAutoUpdate(true)
                            .build(),
                    DumperSettings.DEFAULT,
                    UpdaterSettings
                            .builder().
                            setVersioning(new BasicVersioning("file-version"))
                            .setOptionSorting(UpdaterSettings.OptionSorting.SORT_BY_DEFAULTS)
                            .build()
            );
        } catch (IOException ignored) {
            loadFailed();
        }
    }

    public void reload() {
        try {
            document.update();
            document.save();
            loadFields();
        } catch (IOException | APIException ignored) {
            loadFailed();
        }
    }

    private void loadFields() throws APIException {
        serializer = document.getString("serializer");
        pingIntervalMs = document.getInt("ping_interval_ms");

        Set<Route> routesSet = document.getSection("servers").getRoutes(false);

        for (Route route: routesSet) {
            Exaroton exaroton = null;

            if (hasExaroton(document.getSection(route))) {
                exaroton = new Exaroton(
                        document.getString(route.add("exaroton.token")),
                        document.getString(route.add("exaroton.address"))
                );
            }

            plugin.addServer(
                    new Server(ReLimboQ.getProxyServer(),
                    (String) document.getSection(route).adaptKey(String.class),
                    exaroton,
                    plugin.getConfig()
            ));
        }

        limboName = document.getString("limbo.name");
        x = document.getInt("limbo.location.x");
        y = document.getInt("limbo.location.y");
        z = document.getInt("limbo.location.z");
        yaw = document.getFloat("limbo.location.yaw");
        pitch = document.getFloat("limbo.location.pitch");

        dimension = document.getString("limbo.dimension");
        worldTime = document.getInt("limbo.world_time");
        gamemode = document.getString("limbo.gamemode");
        renderDistance = document.getInt("limbo.render_distance");
        simulationDistance = document.getInt("limbo.simulation_distance");

        queueMessage = document.getString("limbo.messages.queue");
        connectingMessage = document.getString("limbo.messages.connecting");
        serverOfflineMessage = document.getString("limbo.messages.server_offline");
        reloadedMessage = document.getString("limbo.messages.reloaded");
    }

    private boolean hasExaroton(Section server) {
        for (Route route: server.getRoutes(false)) {
            if (document.getString(route).equals("exaroton")) {
                return true;
            }
        }

        return false;
    }

    private void loadFailed() {
        ReLimboQ.getLogger().error("Could not create/load plugin config! The plugin will shut down itself");
        ReLimboQ.getProxyServer()
                .getPluginManager()
                .getPlugin(ReLimboQ.SLUG)
                .ifPresent(pluginContainer -> pluginContainer.getExecutorService().shutdown());
    }
}
