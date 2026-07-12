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

import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.connection.Server;
import net.md_5.bungee.api.event.PlayerDisconnectEvent;
import net.md_5.bungee.api.event.PluginMessageEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

/**
 * BungeeCord implementation of the {@link Artemis} bridge.
 *
 * <p>Registers the outgoing Artemis channels, tracks which proxied players are Artemis (by watching
 * the client's channel-registration plugin message for the chat channel) and delivers payloads to
 * them. Must be registered as a Bungee listener. The wire protocol lives in {@link AbstractArtemis}.</p>
 *
 * @since 1.2.8
 */
public final class BungeeArtemis extends AbstractArtemis implements Listener {

    private final ProxyServer server;
    private final Set<UUID> clients = ConcurrentHashMap.newKeySet();

    /**
     * Creates the bridge and registers the outgoing Artemis channels on the proxy.
     *
     * @param server the proxy server
     * @since 1.2.8
     */
    public BungeeArtemis(ProxyServer server) {
        this.server = server;
        server.registerChannel(CHAT_CHANNEL);
        server.registerChannel(LIGHTNING_CHANNEL);
    }

    /**
     * Marks a player as Artemis when its client registers the chat channel.
     *
     * @param event the plugin-message event
     * @since 1.2.8
     */
    @EventHandler
    public void onPluginMessage(PluginMessageEvent event) {
        if (!(event.getSender() instanceof ProxiedPlayer) || !(event.getReceiver() instanceof Server)) {
            return;
        }
        String tag = event.getTag();
        if (!tag.equalsIgnoreCase("register") && !tag.equalsIgnoreCase("minecraft:register")) {
            return;
        }
        String channels = new String(event.getData(), StandardCharsets.UTF_8);
        if (channels.contains(CHAT_CHANNEL)) {
            this.clients.add(((ProxiedPlayer) event.getSender()).getUniqueId());
        }
    }

    /**
     * Drops a player's Artemis mark when it disconnects.
     *
     * @param event the disconnect event
     * @since 1.2.8
     */
    @EventHandler
    public void onDisconnect(PlayerDisconnectEvent event) {
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
        ProxiedPlayer player = this.server.getPlayer(playerId);
        if (player != null) {
            player.sendData(channel, data);
        }
    }
}
