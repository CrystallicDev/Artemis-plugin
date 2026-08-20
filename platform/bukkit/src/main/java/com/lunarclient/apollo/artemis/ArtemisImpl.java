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

import com.lunarclient.apollo.event.artemis.ArtemisRegisterPlayerEvent;
import com.lunarclient.apollo.event.artemis.ArtemisUnregisterPlayerEvent;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRegisterChannelEvent;
import org.bukkit.event.player.PlayerUnregisterChannelEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.messaging.Messenger;
import org.bukkit.plugin.messaging.PluginMessageListener;

/**
 * Bukkit implementation of the {@link Artemis} bridge.
 *
 * <p>Registers the outgoing Artemis channels on startup, tracks which clients are Artemis (via the
 * chat channel registration), delivers payloads through Bukkit's plugin messenger, and forwards the
 * incoming {@link #EVENTS_CHANNEL} messages to {@link AbstractArtemis#handleIncomingEvent}. The wire
 * protocol itself lives in {@link AbstractArtemis}.</p>
 *
 * @since 1.2.8
 */
public final class ArtemisImpl extends AbstractArtemis implements Listener, PluginMessageListener {

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
        if (!messenger.isIncomingChannelRegistered(plugin, EVENTS_CHANNEL)) {
            messenger.registerIncomingPluginChannel(plugin, EVENTS_CHANNEL, this);
        }
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online.getListeningPluginChannels().contains(CHAT_CHANNEL)) {
                this.clients.add(online.getUniqueId());
            }
        }
    }

    /**
     * Marks a player as Artemis when it registers the chat channel, and posts an
     * {@link ArtemisRegisterPlayerEvent} the first time it is seen this session.
     *
     * @param event the channel registration event
     * @since 1.2.8
     */
    @EventHandler
    public void onRegister(PlayerRegisterChannelEvent event) {
        if (CHAT_CHANNEL.equals(event.getChannel()) && this.clients.add(event.getPlayer().getUniqueId())) {
            this.post(new ArtemisRegisterPlayerEvent(event.getPlayer().getUniqueId()));
        }
    }

    /**
     * Drops a player's Artemis mark when it unregisters the chat channel, and posts
     * an {@link ArtemisUnregisterPlayerEvent}.
     *
     * @param event the channel unregistration event
     * @since 1.2.8
     */
    @EventHandler
    public void onUnregister(PlayerUnregisterChannelEvent event) {
        if (CHAT_CHANNEL.equals(event.getChannel())) {
            this.forget(event.getPlayer().getUniqueId());
        }
    }

    /**
     * Drops a quitting player's Artemis mark (channels are not unregistered on quit)
     * so the session ends with a single {@link ArtemisUnregisterPlayerEvent}.
     *
     * @param event the quit event
     * @since 1.2.8
     */
    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        this.forget(event.getPlayer().getUniqueId());
    }

    // Removes an Artemis client and posts the unregister event exactly once (no-op if unknown).
    private void forget(UUID playerId) {
        if (this.clients.remove(playerId)) {
            this.post(new ArtemisUnregisterPlayerEvent(playerId));
        }
    }

    /**
     * Forwards an incoming {@link #EVENTS_CHANNEL} payload from an Artemis client to the shared parser.
     *
     * @param channel the plugin-message channel
     * @param player  the sending player
     * @param message the payload bytes
     * @since 1.2.8
     */
    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] message) {
        if (EVENTS_CHANNEL.equals(channel) && player != null) {
            this.handleIncomingEvent(player.getUniqueId(), message);
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
