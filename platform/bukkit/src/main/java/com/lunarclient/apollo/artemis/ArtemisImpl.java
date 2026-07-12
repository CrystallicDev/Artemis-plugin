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

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerRegisterChannelEvent;
import org.bukkit.event.player.PlayerUnregisterChannelEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.messaging.Messenger;

/**
 * Bukkit implementation of the {@link Artemis} bridge.
 *
 * <p>Registers the outgoing Artemis channels on startup, tracks which clients are Artemis (via the
 * chat channel registration) and delivers payloads through Bukkit's plugin messenger. The wire
 * protocol itself lives in {@link AbstractArtemis}.</p>
 *
 * @since 1.2.8
 */
public final class ArtemisImpl extends AbstractArtemis implements Listener {

    private final Plugin plugin;
    private final Set<UUID> clients = ConcurrentHashMap.newKeySet();

    /**
     * Creates the bridge: registers the outgoing channels, listens for channel (un)registration, and
     * adopts already-connected Artemis clients.
     *
     * @param plugin the Apollo plugin
     * @since 1.2.8
     */
    public ArtemisImpl(Plugin plugin) {
        this.plugin = plugin;
        Messenger messenger = plugin.getServer().getMessenger();
        if (!messenger.isOutgoingChannelRegistered(plugin, CHAT_CHANNEL)) {
            messenger.registerOutgoingPluginChannel(plugin, CHAT_CHANNEL);
        }
        if (!messenger.isOutgoingChannelRegistered(plugin, LIGHTNING_CHANNEL)) {
            messenger.registerOutgoingPluginChannel(plugin, LIGHTNING_CHANNEL);
        }
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online.getListeningPluginChannels().contains(CHAT_CHANNEL)) {
                this.clients.add(online.getUniqueId());
            }
        }
    }

    /**
     * Marks a player as Artemis when it registers the chat channel.
     *
     * @param event the channel registration event
     * @since 1.2.8
     */
    @EventHandler
    public void onRegister(PlayerRegisterChannelEvent event) {
        if (CHAT_CHANNEL.equals(event.getChannel())) {
            this.clients.add(event.getPlayer().getUniqueId());
        }
    }

    /**
     * Drops a player's Artemis mark when it unregisters the chat channel.
     *
     * @param event the channel unregistration event
     * @since 1.2.8
     */
    @EventHandler
    public void onUnregister(PlayerUnregisterChannelEvent event) {
        if (CHAT_CHANNEL.equals(event.getChannel())) {
            this.clients.remove(event.getPlayer().getUniqueId());
        }
    }

    @Override
    public boolean isArtemis(UUID playerId) {
        return playerId != null && this.clients.contains(playerId);
    }

    @Override
    protected void sendPayload(UUID playerId, String channel, byte[] data) {
        Player player = playerId != null ? Bukkit.getPlayer(playerId) : null;
        if (player == null) {
            return;
        }
        try {
            player.sendPluginMessage(this.plugin, channel, data);
        } catch (Exception ex) {
            this.plugin.getLogger().log(Level.WARNING,
                "[Artemis] payload send failed for " + player.getName(), ex);
        }
    }
}
