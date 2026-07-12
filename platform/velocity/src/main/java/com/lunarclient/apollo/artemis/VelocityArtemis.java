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

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.player.PlayerChannelRegisterEvent;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.messages.ChannelIdentifier;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Velocity implementation of the {@link Artemis} bridge.
 *
 * <p>Registers the outgoing Artemis channels, tracks which proxied players are Artemis (via the chat
 * channel registration) and delivers payloads to them. Must be registered as a Velocity event
 * listener. The wire protocol lives in {@link AbstractArtemis}.</p>
 *
 * @since 1.2.8
 */
public final class VelocityArtemis extends AbstractArtemis {

    private static final ChannelIdentifier CHAT_ID = MinecraftChannelIdentifier.create("artemis", "chat");
    private static final ChannelIdentifier LIGHTNING_ID = MinecraftChannelIdentifier.create("artemis", "lightning");

    private final ProxyServer server;
    private final Set<UUID> clients = ConcurrentHashMap.newKeySet();

    /**
     * Creates the bridge and registers the outgoing Artemis channels on the proxy.
     *
     * @param server the proxy server
     * @since 1.2.8
     */
    public VelocityArtemis(ProxyServer server) {
        this.server = server;
        server.getChannelRegistrar().register(CHAT_ID, LIGHTNING_ID);
    }

    /**
     * Marks a player as Artemis when it registers the chat channel.
     *
     * @param event the channel registration event
     * @since 1.2.8
     */
    @Subscribe
    public void onRegisterChannel(PlayerChannelRegisterEvent event) {
        for (ChannelIdentifier channel : event.getChannels()) {
            if (channel.getId().equals(CHAT_CHANNEL)) {
                this.clients.add(event.getPlayer().getUniqueId());
                return;
            }
        }
    }

    /**
     * Drops a player's Artemis mark when it disconnects.
     *
     * @param event the disconnect event
     * @since 1.2.8
     */
    @Subscribe
    public void onDisconnect(DisconnectEvent event) {
        this.clients.remove(event.getPlayer().getUniqueId());
    }

    @Override
    public boolean isArtemis(UUID playerId) {
        return playerId != null && this.clients.contains(playerId);
    }

    @Override
    protected void sendPayload(UUID playerId, String channel, byte[] data) {
        if (playerId == null) {
            return;
        }
        ChannelIdentifier identifier = CHAT_CHANNEL.equals(channel) ? CHAT_ID : LIGHTNING_ID;
        this.server.getPlayer(playerId).ifPresent(player -> player.sendPluginMessage(identifier, data));
    }
}
