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

package io.github.akbe2020.relimboq.listeners;

import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.player.ServerPreConnectEvent;
import io.github.akbe2020.relimboq.QueuedPlayer;
import io.github.akbe2020.relimboq.ReLimboQ;
import io.github.akbe2020.relimboq.Server;
import net.elytrium.limboapi.api.player.LimboPlayer;

public class QueueListener {
    private final ReLimboQ plugin;

    public QueueListener(ReLimboQ plugin) {
        this.plugin = plugin;
    }

    @Subscribe(order = PostOrder.LAST)
    public void onLogin(ServerPreConnectEvent event) {
        String serverName = event.getOriginalServer().getServerInfo().getName();

        for (Server server : plugin.getServers()) {
            if (server.getName().equals(serverName)) {
                if (server.isAvailable()) {
                    return;
                }

                plugin.queuePlayer(new QueuedPlayer((LimboPlayer) event.getPlayer(), serverName));
            }
        }
    }
}
