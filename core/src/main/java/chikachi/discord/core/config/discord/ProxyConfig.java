/*
 * Copyright (C) 2018 Chikachi and other contributors
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses.
 */

package chikachi.discord.core.config.discord;

import com.google.gson.annotations.Since;

public class ProxyConfig {
    @Since(3.0)
    public boolean enabled = false;
    @Since(3.0)
    public String type = "HTTP";
    @Since(3.0)
    public String host = "";
    @Since(3.0)
    public int port = 0;
    @Since(3.0)
    public String username = "";
    @Since(3.0)
    public String password = "";

    public void fillFields() {
        if (this.type == null) {
            this.type = "HTTP";
        }

        if (this.host == null) {
            this.host = "";
        }

        if (this.username == null) {
            this.username = "";
        }

        if (this.password == null) {
            this.password = "";
        }
    }
}
