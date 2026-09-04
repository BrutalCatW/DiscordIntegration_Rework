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

package chikachi.discord.core;

import chikachi.discord.core.config.Configuration;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.requests.Request;
import net.dv8tion.jda.core.requests.Response;
import net.dv8tion.jda.core.requests.RestAction;
import net.dv8tion.jda.core.requests.Route;
import org.json.JSONObject;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

class WebhookMessage {
    private String content;
    private String username;
    private String avatarUrl;

    WebhookMessage(String content, String username, String avatarUrl) {
        this.content = content;
        this.username = username;
        this.avatarUrl = avatarUrl;
    }

    boolean queue(JDA jda, Long channelId) {
        if (this.content == null || this.content.trim().length() == 0) {
            return false;
        }

        String webhook = Configuration.getConfig().discord.channels.channels.get(channelId).webhook.trim();
        // Discord переехал на discord.com в 2020 году и выдаёт ссылки
        // именно там. Старый шаблон знал только discordapp.com, поэтому
        // современная ссылка не подходила, queue() возвращал false,
        // и сообщение МОЛЧА уходило от имени бота вместо вебхука —
        // без единой строки в логе. Принимаем оба домена и поддомены.
        Matcher matcher = Pattern
            .compile("https://(ptb\\.|canary\\.)?discord(app)?\\.com/api/(v\\d+/)?webhooks/(\\d+)/([\\w-]+)")
            .matcher(webhook);
        if (matcher.matches()) {
            String webhookId = matcher.group(4);
            String webhookToken = matcher.group(5);

            Route.CompiledRoute route = Route.Webhooks.EXECUTE_WEBHOOK.compile(webhookId, webhookToken);

            JSONObject json = new JSONObject();
            if (this.username != null) {
                json.put("username", this.username);
            }
            if (this.avatarUrl != null) {
                json.put("avatar_url", this.avatarUrl);
            }

            String text = this.content;

            if (text.length() > 2000) {
                text = text.substring(0, 1997) + "...";
            }

            json.put("content", text);

            new RestAction<Void>(jda, route, json) {
                protected void handleResponse(Response response, Request<Void> request) {
                    try {
                        if (response.isOk()) {
                            request.onSuccess(null);
                        } else {
                            request.onFailure(response);
                        }
                    } catch (Exception ignored) {
                    }
                }
            }.queue();
            return true;
        }
        return false;
    }
}
