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

import net.elytrium.limboapi.api.player.LimboPlayer;

import javax.annotation.Nullable;
import java.util.ArrayList;

public record QueuedPlayer(LimboPlayer limboPlayer, String targetServerName) {
    @Nullable
    public static QueuedPlayer bruteforceQueuedPlayer(ArrayList<QueuedPlayer> queuedPlayers, LimboPlayer limboPlayer) {
        for (QueuedPlayer queuedPlayer : queuedPlayers) {
            if (queuedPlayer.limboPlayer().equals(limboPlayer)) {
                return queuedPlayer;
            }
        }

        return null;
    }
}