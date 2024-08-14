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

package io.github.akbe2020.relimboq.listener;

import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.proxy.Player;
import io.github.akbe2020.relimboq.Config;
import io.github.akbe2020.relimboq.ReLimboQ;
import net.elytrium.commons.kyori.serialization.Serializer;
import net.elytrium.limboapi.api.event.LoginLimboRegisterEvent;

public class QueueListener {
    private final ReLimboQ plugin;
    private final Serializer serializer = ReLimboQ.getSerializer();

    public QueueListener(ReLimboQ plugin) {
        this.plugin = plugin;
    }

    @Subscribe(order = PostOrder.LAST)
    public void onLogin(LoginLimboRegisterEvent event) {
        if (this.plugin.isAlwaysPutToQueue()) {
            Player player = event.getPlayer();
            event.addOnJoinCallback(() -> this.plugin.queuePlayer(player));
        }
    }

    @Subscribe
    public void onLoginLimboRegister(LoginLimboRegisterEvent event) {
        if (this.plugin.isAlwaysPutToQueue()) {
            return;
        }

        event.setOnKickCallback((kickEvent) -> {
            if (!kickEvent.getServer().equals(this.plugin.getTargetServer())) {
                return false;
            }

            if (kickEvent.getServerKickReason().isEmpty()) {
                return false;
            }

            String reason = this.serializer.serialize(kickEvent.getServerKickReason().get());
            if (reason.contains(Config.IMP.MAIN.KICK_MESSAGE)) {
                this.plugin.queuePlayer(kickEvent.getPlayer());
                return true;
            }
            return false;
        });
    }
}
