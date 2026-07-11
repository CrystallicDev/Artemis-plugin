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

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
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
 * <p>Handles the server side of the link: the Artemis plugin-message channels
 * ({@value #CHANNEL}, {@value #LIGHTNING_CHANNEL}), registered on startup, tracking
 * which clients are Artemis (a client counts once it registers the chat channel),
 * and the wire protocols. Callers only touch {@link Artemis}, never the channels
 * or payload formats.</p>
 *
 * <p>Chat payload: a {@code byte} opcode, then {@code 0} display ({@code int id},
 * {@code UTF json}), {@code 1} remove ({@code int id}) or {@code 2} clear. The
 * JSON is Adventure's gson form; {@code §x§r§r§g§g§b§b} hex is kept.</p>
 *
 * <p>Lightning payload: {@code double x}, {@code double y}, {@code double z}, then
 * {@code int mainColor} and {@code int coreColor} (ARGB).</p>
 *
 * @since 1.2.8
 */
public final class ArtemisImpl implements Artemis, Listener {

    /**
     * The dedicated channel; its registration by a client marks it as Artemis.
     *
     * @since 1.2.8
     */
    public static final String CHANNEL = "artemis:chat";

    /**
     * The colored-lightning channel, registered by Artemis clients alongside {@link #CHANNEL}.
     *
     * @since 1.2.8
     */
    public static final String LIGHTNING_CHANNEL = "artemis:lightning";

    private static final int OP_DISPLAY = 0;
    private static final int OP_REMOVE = 1;
    private static final int OP_CLEAR = 2;

    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.builder()
        .character(LegacyComponentSerializer.SECTION_CHAR)
        .hexColors()
        .useUnusualXRepeatedCharacterHexFormat()
        .build();

    private final Plugin plugin;
    private final Set<UUID> clients = ConcurrentHashMap.newKeySet();
    private final AtomicInteger nextId = new AtomicInteger(1);

    /**
     * Creates the bridge: registers the outgoing channel, listens for channel
     * (un)registration, and adopts already-connected Artemis clients.
     *
     * @param plugin the Apollo plugin
     * @since 1.2.8
     */
    public ArtemisImpl(Plugin plugin) {
        this.plugin = plugin;
        Messenger messenger = plugin.getServer().getMessenger();
        if (!messenger.isOutgoingChannelRegistered(plugin, CHANNEL)) {
            messenger.registerOutgoingPluginChannel(plugin, CHANNEL);
        }
        if (!messenger.isOutgoingChannelRegistered(plugin, LIGHTNING_CHANNEL)) {
            messenger.registerOutgoingPluginChannel(plugin, LIGHTNING_CHANNEL);
        }
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online.getListeningPluginChannels().contains(CHANNEL)) {
                this.clients.add(online.getUniqueId());
            }
        }
    }

    /**
     * Marks a player as Artemis when it registers the channel.
     *
     * @param event the channel registration event
     * @since 1.2.8
     */
    @EventHandler
    public void onRegister(PlayerRegisterChannelEvent event) {
        if (CHANNEL.equals(event.getChannel())) {
            this.clients.add(event.getPlayer().getUniqueId());
        }
    }

    /**
     * Drops a player's Artemis mark when it unregisters the channel.
     *
     * @param event the channel unregistration event
     * @since 1.2.8
     */
    @EventHandler
    public void onUnregister(PlayerUnregisterChannelEvent event) {
        if (CHANNEL.equals(event.getChannel())) {
            this.clients.remove(event.getPlayer().getUniqueId());
        }
    }

    @Override
    public boolean isArtemis(UUID playerId) {
        return playerId != null && this.clients.contains(playerId);
    }

    @Override
    public int chat(UUID playerId, String legacyText) {
        Player player = playerId != null ? Bukkit.getPlayer(playerId) : null;
        if (player == null || legacyText == null || !this.isArtemis(playerId)) {
            return -1;
        }
        int id = this.nextId.getAndIncrement();
        Component component = LEGACY.deserialize(legacyText);
        String json = GsonComponentSerializer.gson().serialize(component);
        this.send(player, CHANNEL, out -> {
            out.writeByte(OP_DISPLAY);
            out.writeInt(id);
            out.writeUTF(json);
        });
        return id;
    }

    @Override
    public void removeChat(UUID playerId, int messageId) {
        Player player = playerId != null ? Bukkit.getPlayer(playerId) : null;
        if (player == null || !this.isArtemis(playerId)) {
            return;
        }
        this.send(player, CHANNEL, out -> {
            out.writeByte(OP_REMOVE);
            out.writeInt(messageId);
        });
    }

    @Override
    public void clearChat(UUID playerId) {
        Player player = playerId != null ? Bukkit.getPlayer(playerId) : null;
        if (player == null || !this.isArtemis(playerId)) {
            return;
        }
        this.send(player, CHANNEL, out -> out.writeByte(OP_CLEAR));
    }

    @Override
    public void strikeLightning(UUID playerId, double x, double y, double z, int mainColor, int coreColor) {
        Player player = playerId != null ? Bukkit.getPlayer(playerId) : null;
        if (player == null || !this.isArtemis(playerId)) {
            return;
        }
        this.send(player, LIGHTNING_CHANNEL, out -> {
            out.writeDouble(x);
            out.writeDouble(y);
            out.writeDouble(z);
            out.writeInt(mainColor);
            out.writeInt(coreColor);
        });
    }

    @Override
    public void strikeLightning(UUID playerId, double x, double y, double z, int color) {
        this.strikeLightning(playerId, x, y, z, color, brightCore(color));
    }

    // Derives a brighter core color by moving each RGB channel halfway to white.
    private static int brightCore(int color) {
        int red = (color >> 16) & 0xFF;
        int green = (color >> 8) & 0xFF;
        int blue = color & 0xFF;
        red += (0xFF - red) / 2;
        green += (0xFF - green) / 2;
        blue += (0xFF - blue) / 2;
        return 0xFF000000 | (red << 16) | (green << 8) | blue;
    }

    private interface PayloadWriter {

        void write(DataOutputStream out) throws Exception;

    }

    private void send(Player player, String channel, PayloadWriter writer) {
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            try (DataOutputStream out = new DataOutputStream(bytes)) {
                writer.write(out);
            }
            player.sendPluginMessage(this.plugin, channel, bytes.toByteArray());
        } catch (Exception ex) {
            this.plugin.getLogger().log(Level.WARNING,
                "[Artemis] payload send failed for " + player.getName(), ex);
        }
    }

}
