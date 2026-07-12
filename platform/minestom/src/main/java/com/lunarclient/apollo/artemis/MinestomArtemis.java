/*
 * This file is part of Apollo, licensed under the MIT License.
 *
 * Copyright (c) 2026 Moonsworth
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.lunarclient.apollo.artemis;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minestom.server.entity.Player;
import net.minestom.server.event.Event;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.player.PlayerDisconnectEvent;
import net.minestom.server.event.player.PlayerPluginMessageEvent;

/**
 * Minestom implementation of the {@link Artemis} bridge.
 *
 * <p>Tracks which players are Artemis (by watching the client's channel-registration plugin message
 * for the chat channel) and delivers payloads to them. The connected {@link Player} is kept per uuid
 * so payloads can be sent without a lookup. The wire protocol lives in {@link AbstractArtemis}.</p>
 *
 * @since 1.2.8
 */
public final class MinestomArtemis extends AbstractArtemis {

    private final Map<UUID, Player> clients = new ConcurrentHashMap<>();

    /**
     * Creates the bridge and registers its listeners on the given event node.
     *
     * @param node the Apollo event node
     * @since 1.2.8
     */
    public MinestomArtemis(EventNode<Event> node) {
        node.addListener(PlayerPluginMessageEvent.class, this::onPluginMessage);
        node.addListener(PlayerDisconnectEvent.class, this::onDisconnect);
    }

    private void onPluginMessage(PlayerPluginMessageEvent event) {
        String identifier = event.getIdentifier();
        Player player = event.getPlayer();
        if ("minecraft:register".equals(identifier)) {
            if (event.getMessageString().contains(CHAT_CHANNEL)) {
                this.clients.put(player.getUuid(), player);
            }
        } else if ("minecraft:unregister".equals(identifier)) {
            if (event.getMessageString().contains(CHAT_CHANNEL)) {
                this.clients.remove(player.getUuid());
            }
        }
    }

    private void onDisconnect(PlayerDisconnectEvent event) {
        this.clients.remove(event.getPlayer().getUuid());
    }

    @Override
    public boolean isArtemis(UUID playerId) {
        return playerId != null && this.clients.containsKey(playerId);
    }

    @Override
    protected void sendPayload(UUID playerId, String channel, byte[] data) {
        if (playerId == null) {
            return;
        }
        Player player = this.clients.get(playerId);
        if (player != null) {
            player.sendPluginMessage(channel, data);
        }
    }
}
