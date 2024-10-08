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
import com.exaroton.api.ExarotonClient;
import com.exaroton.api.server.Server;
import com.exaroton.api.server.ServerStatus;

public final class Exaroton {
    private final Server server;
    private final String address;

    public Exaroton(String token, String address) throws APIException {
        this.address = address;
        server = bruteforceServerArray(new ExarotonClient(token).getServers());
    }

    public boolean isOffline() {
        return !server.hasStatus(ServerStatus.ONLINE);
    }

    private Server bruteforceServerArray(Server[] servers) {
        for (Server server : servers) {
            if (server.getAddress().equals(address)) {
                return server;
            }
        }

        return null;
    }
}
