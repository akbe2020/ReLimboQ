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

package io.github.akbe2020.relimboq.commands;

import io.github.akbe2020.relimboq.Config;
import io.github.akbe2020.relimboq.ReLimboQ;
import net.elytrium.commons.kyori.serialization.Serializer;

public class Reload extends Command {
    private final ReLimboQ plugin = getPlugin();
    private final Serializer serializer = getSerializer();

    public Reload(ReLimboQ plugin) {
        super(plugin, "reload");
    }

    public void run(Invocation invocation) {
        try {
            plugin.reload();
            invocation.source().sendMessage(serializer.deserialize(Config.IMP.MESSAGES.RELOAD));
        } catch (Exception e) {
            e.printStackTrace();
            invocation.source().sendMessage(serializer.deserialize(Config.IMP.MESSAGES.RELOAD_FAILED));
        }
    }
}
