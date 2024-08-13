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

import net.elytrium.commons.config.YamlConfig;

public class Config extends YamlConfig {

    @Ignore
    public static final Config IMP = new Config();

    @Create
    public MAIN MAIN;
    @Create
    public MESSAGES MESSAGES;

    public static class MAIN {

        @Comment("Serializers: LEGACY_AMPERSAND, LEGACY_SECTION, MINIMESSAGE")
        public String SERIALIZER = "MINIMESSAGE";
        @Comment("Server from velocity.toml which will checked for online")
        public String SERVER = "survival";
        @Comment("Send player to the queue if kick reason contains this text (like \"The server if full!\")")
        public String KICK_MESSAGE = "The server is full";
        @Comment("Server checking interval in seconds")
        public int CHECK_INTERVAL = 2;

        @Create
        public Config.MAIN.WORLD WORLD;

        public static class WORLD {

            @Comment("Dimensions: OVERWORLD, NETHER, THE_END")
            public String DIMENSION = "OVERWORLD";
        }

    }

    public static class MESSAGES {

        public String QUEUE_MESSAGE = "Your position in queue: {0}";
        public String SERVER_OFFLINE = "<red>Server is offline!";
        public String RELOAD = "<green>ReLimboQ reloaded!";
        public String RELOAD_FAILED = "<red>Reload failed!";
    }
}
