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

package io.github.akbe2020.relimboq.handler;

import io.github.akbe2020.relimboq.ReLimboQ;
import net.elytrium.limboapi.api.Limbo;
import net.elytrium.limboapi.api.LimboSessionHandler;
import net.elytrium.limboapi.api.player.LimboPlayer;

public class QueueHandler implements LimboSessionHandler {

    private final ReLimboQ plugin;
    private LimboPlayer player;

    public QueueHandler(ReLimboQ plugin) {
        this.plugin = plugin;
    }

    @Override
    public void onSpawn(Limbo server, LimboPlayer player) {
        this.player = player;
        this.player.disableFalling();
        this.plugin.queuedPlayers.add(player);
    }

    @Override
    public void onDisconnect() {
        this.plugin.queuedPlayers.remove(this.player);
    }
}
