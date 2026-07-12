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
import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

/**
 * Platform-independent base of the {@link Artemis} bridge: it builds every wire payload (chat and
 * colored lightning) and defers the two platform-specific concerns to subclasses — detecting Artemis
 * clients ({@link #isArtemis(UUID)}) and delivering a payload ({@link #sendPayload(UUID, String,
 * byte[])}).
 *
 * @since 1.2.8
 */
public abstract class AbstractArtemis implements Artemis {

    /**
     * The chat channel; its registration by a client marks it as Artemis.
     *
     * @since 1.2.8
     */
    public static final String CHAT_CHANNEL = "artemis:chat";

    /**
     * The colored-lightning channel, registered by Artemis clients alongside {@link #CHAT_CHANNEL}.
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

    private final AtomicInteger nextId = new AtomicInteger(1);

    /**
     * Delivers a built payload to the player on the given channel. Called only for players that are
     * known Artemis clients; implementations resolve the player and send the plugin message.
     *
     * @param playerId the recipient uuid
     * @param channel  the plugin-message channel
     * @param data     the payload bytes
     * @since 1.2.8
     */
    protected abstract void sendPayload(UUID playerId, String channel, byte[] data);

    @Override
    public int chat(UUID playerId, String legacyText) {
        if (playerId == null || legacyText == null || !this.isArtemis(playerId)) {
            return -1;
        }
        int id = this.nextId.getAndIncrement();
        Component component = LEGACY.deserialize(legacyText);
        String json = GsonComponentSerializer.gson().serialize(component);
        this.send(playerId, CHAT_CHANNEL, out -> {
            out.writeByte(OP_DISPLAY);
            out.writeInt(id);
            out.writeUTF(json);
        });
        return id;
    }

    @Override
    public void removeChat(UUID playerId, int messageId) {
        if (playerId == null || !this.isArtemis(playerId)) {
            return;
        }
        this.send(playerId, CHAT_CHANNEL, out -> {
            out.writeByte(OP_REMOVE);
            out.writeInt(messageId);
        });
    }

    @Override
    public void clearChat(UUID playerId) {
        if (playerId == null || !this.isArtemis(playerId)) {
            return;
        }
        this.send(playerId, CHAT_CHANNEL, out -> out.writeByte(OP_CLEAR));
    }

    @Override
    public void strikeLightning(UUID playerId, double x, double y, double z, int mainColor, int coreColor) {
        if (playerId == null || !this.isArtemis(playerId)) {
            return;
        }
        this.send(playerId, LIGHTNING_CHANNEL, out -> {
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

    private void send(UUID playerId, String channel, PayloadWriter writer) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (DataOutputStream out = new DataOutputStream(bytes)) {
            writer.write(out);
        } catch (IOException ex) {
            return; // in-memory buffer: only thrown for an oversized string, drop the message
        }
        this.sendPayload(playerId, channel, bytes.toByteArray());
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

    /**
     * Writes a payload body to the given stream.
     *
     * @since 1.2.8
     */
    protected interface PayloadWriter {

        /**
         * Writes the payload body.
         *
         * @param out the output stream
         * @throws IOException if writing fails
         * @since 1.2.8
         */
        void write(DataOutputStream out) throws IOException;

    }
}
