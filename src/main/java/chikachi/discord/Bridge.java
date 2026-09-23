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

package chikachi.discord;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import net.minecraft.nbt.NBTTagCompound;

import chikachi.discord.core.DiscordClient;
import chikachi.discord.core.DiscordIntegrationLogger;
import chikachi.discord.core.Message;
import chikachi.discord.core.config.ConfigWrapper;
import chikachi.discord.core.config.Configuration;
import chikachi.discord.core.config.bridge.BridgeConfig;
import chikachi.discord.core.config.discord.DiscordChannelConfig;
import chikachi.discord.core.config.types.MessageConfig;
import cpw.mods.fml.common.event.FMLInterModComms;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;

/**
 * Обмен сообщениями со вторым мостом — Telegram.
 * <p>
 * <b>Зачем.</b> Оба моста кладут входящее в игру пакетом каждому игроку
 * ({@code player.addChatMessage}), а не событием чата. {@code ServerChatEvent}
 * при этом не возникает, и второй мод сообщения первого не видит физически.
 * Из-за этого разговор выглядел как беседа игрока с самим собой: ответ уходил
 * в обе сети, а вопрос был виден только в одной.
 * <p>
 * <b>Что пересылаем.</b> Ровно ту строку, которая ушла в игру, — она уже
 * собрана по шаблону и выглядит как {@code [DC] ник » текст}. Шаблон
 * не дублируется, подпись совпадает с игровой сама собой.
 * <p>
 * <b>Почему не через штатный IMC-API мода.</b> Подписка через
 * {@code registerListener} проходит проверку {@code IMCConfig.isAllowed},
 * а там по умолчанию режим «белый список» с пустым списком — партнёр молча
 * получил бы «Not Allowed». Настраивать пришлось бы в двух местах, и первая же
 * забытая строка выглядела бы как поломка. Здесь адресат задан в конфиге явно,
 * рукопожатие не нужно, и штатный API остаётся нетронутым для чужих модов.
 * <p>
 * 🔴 <b>Почему всё делается на тике сервера.</b> {@code FMLInterModComms}
 * хранит очередь в обычном {@code ArrayListMultimap} — она не потокобезопасна.
 * Сообщения из Discord приходят в потоке JDA, поэтому {@code send} только
 * кладёт строку в свою очередь, а обращения к FML идут исключительно
 * из {@link #onServerTick}.
 * <p>
 * Петля невозможна по устройству: наружу уходит только то, что пришло
 * из своей сети, а полученное от партнёра в игру не возвращается.
 */
public class Bridge {

    /** Ключ IMC. Свой, чтобы не пересекаться с sendMessage чужих модов. */
    private static final String KEY = "bridgeChat";

    /**
     * Предохранитель: если сервер встал или партнёр не загружен,
     * очередь не должна расти бесконечно.
     */
    private static final int MAX_PENDING = 256;

    private static final Queue<String> OUTBOX = new ConcurrentLinkedQueue<>();

    /** Жалобы в лог — по одному разу, иначе зальют его на каждом сообщении. */
    private static boolean warnedNoTarget = false;
    private static boolean warnedManyChannels = false;

    /**
     * Поставить строку в очередь на отправку партнёру.
     * <p>
     * Вызывается из потока JDA, поэтому здесь только очередь: с FML работает
     * тик сервера.
     */
    public static void send(String text) {
        if (text == null || text.trim()
            .isEmpty()) {
            return;
        }

        BridgeConfig config = config();
        if (config == null || !config.enabled) {
            return;
        }

        if (OUTBOX.size() >= MAX_PENDING) {
            DiscordIntegrationLogger.Log("Bridge queue is full - message dropped", true);
            return;
        }

        OUTBOX.add(text);
    }

    public static void clear() {
        OUTBOX.clear();
    }

    /** Конфиг может быть ещё не прочитан — до preInit здесь null. */
    private static BridgeConfig config() {
        ConfigWrapper wrapper = Configuration.getConfig();
        return wrapper == null ? null : wrapper.bridge;
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        BridgeConfig config = config();
        boolean enabled = config != null && config.enabled;

        if (enabled) {
            pushOutgoing(config);
        } else {
            OUTBOX.clear();
        }

        // Входящую очередь забираем всегда, даже с выключенным мостом:
        // партнёр мог остаться включённым, и его сообщения копились бы
        // в FML без конца. Чужое отбрасывается — штатный runtime-IMC
        // этот мод и раньше не разбирал.
        pullIncoming(enabled ? config : null);
    }

    /** Наше — партнёру. */
    private void pushOutgoing(BridgeConfig config) {
        String text;
        while ((text = OUTBOX.poll()) != null) {
            NBTTagCompound tag = new NBTTagCompound();
            tag.setString("text", text);

            try {
                // Если партнёр не установлен, FML отбрасывает сообщение сам:
                // внутри стоит проверка Loader.isModLoaded.
                FMLInterModComms.sendRuntimeMessage(DiscordIntegration.instance, config.partner, KEY, tag);
            } catch (Throwable t) {
                DiscordIntegrationLogger
                    .Log("Failed to send a message to bridge " + config.partner + ": " + t.getMessage(), true);
            }
        }
    }

    /** Партнёрское — в Discord. */
    private void pullIncoming(BridgeConfig config) {
        for (FMLInterModComms.IMCMessage imcMessage : FMLInterModComms
            .fetchRuntimeMessages(DiscordIntegration.instance)) {

            if (config == null) {
                continue;
            }

            if (!KEY.equalsIgnoreCase(imcMessage.key) || !imcMessage.isNBTMessage()) {
                continue;
            }

            // Ключ наш, но прислать его мог кто угодно.
            if (!config.partner.equals(imcMessage.getSender())) {
                continue;
            }

            String text = imcMessage.getNBTValue()
                .getString("text");
            if (text == null || text.trim()
                .isEmpty()) {
                continue;
            }

            long channelId = targetChannel(config);
            if (channelId == 0L) {
                continue;
            }

            try {
                // Шаблон «{MESSAGE}»: текст уже готов, добавлять к нему нечего.
                // Разрешение упоминаний остаётся тем же, что и для сообщений
                // из игры, — им заведуют canMention* в разделе minecraft.
                HashMap<String, String> arguments = new HashMap<>();
                arguments.put("MESSAGE", text);

                DiscordClient.getInstance()
                    .broadcast(new Message(new MessageConfig("{MESSAGE}"), arguments),
                        Collections.singletonList(channelId));
            } catch (Throwable t) {
                DiscordIntegrationLogger
                    .Log("Failed to relay a message from " + config.partner + ": " + t.getMessage(), true);
            }
        }
    }

    /**
     * Куда слать. Явно заданный канал или единственный настроенный.
     * <p>
     * Выбирать за владельца из нескольких нельзя: угадывать, в какой канал
     * пойдёт чужой разговор, — худшее из решений. Лучше промолчать и сказать.
     */
    private long targetChannel(BridgeConfig config) {
        if (config.channel != 0L) {
            return config.channel;
        }

        Map<Long, DiscordChannelConfig> channels = Configuration.getConfig().discord.channels.channels;

        if (channels == null || channels.isEmpty()) {
            if (!warnedNoTarget) {
                warnedNoTarget = true;
                DiscordIntegrationLogger.Log("Bridge is enabled but no channels are configured", true);
            }
            return 0L;
        }

        if (channels.size() > 1) {
            if (!warnedManyChannels) {
                warnedManyChannels = true;
                DiscordIntegrationLogger.Log("Bridge is enabled but there are several channels - set bridge.channel", true);
            }
            return 0L;
        }

        return channels.keySet()
            .iterator()
            .next();
    }
}
