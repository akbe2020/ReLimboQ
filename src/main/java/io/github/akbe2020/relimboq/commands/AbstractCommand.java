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

import com.google.common.collect.ImmutableList;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import io.github.akbe2020.relimboq.ReLimboQ;
import net.elytrium.commons.kyori.serialization.Serializer;

import java.util.List;

public abstract class AbstractCommand implements SimpleCommand {
    private final ReLimboQ plugin;
    private final Serializer serializer;
    private final String slug;

    public AbstractCommand(ReLimboQ plugin, String slug) {
        this.plugin = plugin;
        this.slug = slug;
        serializer = ReLimboQ.getSerializer();
    }

    public ReLimboQ getPlugin() {
        return plugin;
    }

    public Serializer getSerializer() {
        return serializer;
    }

    @Override
    public List<String> suggest(Invocation invocation) {
        if (invocation.arguments().length == 0) {
            return ImmutableList.of(slug);
        }

        return ImmutableList.of();
    }

    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();
        String[] args = invocation.arguments();

        if (args.length == 0 || !source.hasPermission(ReLimboQ.SLUG + "." + slug)) {
            return;
        }

        run(invocation);
    }

    public abstract void run(Invocation invocation);
}
