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
    @Create
    public EXAROTON EXAROTON;

    public static class MAIN {
        @Comment("Serializers: LEGACY_AMPERSAND, LEGACY_SECTION, MINIMESSAGE")
        public String SERIALIZER = "MINIMESSAGE";
        @Comment("Server from velocity.toml which will checked for online")
        public String SERVER = "survival";
        @Comment("Ignores kick_message and puts the player in the queue even if they have not been kicked by the server")
        public boolean ALWAYS_PUT_TO_QUEUE = true;
        @Comment("Send player to the queue if kick reason contains this text (like \"The server if full!\")")
        public String KICK_MESSAGE = "The server is full";
        @Comment("Server checking interval in seconds")
        public int CHECK_INTERVAL = 2;

        @Create
        public Config.MAIN.WORLD WORLD;

        public static class WORLD {
            @Comment("The name of Limbo appears on the f3 screen")
            public String NAME = "ReLimboQ";

            @Comment("Spawn x coordinate")
            public double X = 0;
            @Comment("Spawn y coordinate")
            public double Y = 100;
            @Comment("Spawn z coordinate")
            public double Z = 0;
            @Comment("Spawn rotation (yaw)")
            public float YAW = 90.0f;
            @Comment("Spawn rotation (pitch)")
            public float PITCH = 0.0f;

            @Comment("Dimensions: OVERWORLD, NETHER, THE_END")
            public String DIMENSION = "OVERWORLD";
            @Comment("DAY - 1000, NOON - 6000, SUNSET - 12000, NIGHT - 13000, MIDNIGHT - 18000, SUNRISE - 23000")
            public long WORLDTIME = 6000;
            @Comment("Gamemodes: SURVIVAL, ADVENTURE, SPECTATOR, CREATIVE")
            public String GAMEMODE = "SPECTATOR";
            public int VIEWDISTANCE = 2;
            public int SIMULATIONDISTANCE = 2;
        }
    }

    public static class MESSAGES {
        public String QUEUE_MESSAGE = "Your position in queue: {0}";
        public String SERVER_OFFLINE = "<red>Server is offline!";
        public String RELOAD = "<green>ReLimboQ reloaded!";
        public String RELOAD_FAILED = "<red>Reload failed!";
    }

    public static class EXAROTON {
        @Comment("Enables Exaroton hosting integration")
        public boolean ENABLED = false;
        @Comment("Your Exaroton API token can be found at: https://exaroton.com/account/")
        public String TOKEN = "example-api-token";
        @Comment("Your Exaroton server ID can be found on the server page")
        public String SERVER_ID = "example-server-id";
    }
}
