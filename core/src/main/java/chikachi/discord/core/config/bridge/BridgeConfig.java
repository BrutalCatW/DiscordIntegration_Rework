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

package chikachi.discord.core.config.bridge;

import com.google.gson.annotations.Since;

/**
 * Пересылка чужих сообщений во второй мост.
 * <p>
 * Оба моста кладут входящее в игру пакетом каждому игроку, а не событием
 * чата, поэтому друг друга они не видят: игрок отвечает, ответ уходит
 * в обе сети, а сам вопрос виден только в одной. Со стороны это выглядит
 * как беседа с самим собой.
 * <p>
 * Когда мост включён, строка, ушедшая в игру, отправляется вторым модом
 * ещё и в его сеть — как есть, вместе с подписью «[DC] ник »».
 */
public class BridgeConfig {

    /** Выключено по умолчанию: без второго мода включать нечего. */
    @Since(3.0)
    public boolean enabled = false;

    /**
     * modid второго моста. Сообщения уходят ему через IMC, и если мод
     * не установлен, FML отбрасывает их молча — проверка на стороне
     * отправителя не нужна.
     */
    @Since(3.0)
    public String partner = DEFAULT_PARTNER;

    /**
     * Куда слать пришедшее от партнёра.
     * <p>
     * 0 — взять единственный настроенный канал. Если каналов несколько,
     * выбрать за владельца нельзя: мост промолчит и скажет об этом
     * в лог один раз.
     */
    @Since(3.0)
    public long channel = 0L;

    private transient static final String DEFAULT_PARTNER = "brutalcattelegram";

    public void fillFields() {
        if (this.partner == null || this.partner.trim()
            .isEmpty()) {
            this.partner = DEFAULT_PARTNER;
        }
    }
}
