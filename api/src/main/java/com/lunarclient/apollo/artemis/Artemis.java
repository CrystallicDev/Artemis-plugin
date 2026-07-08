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

import java.util.UUID;

/**
 * Server-side bridge to the Artemis client, an in-house Forge 1.8.9 mod that
 * speaks the Apollo protocol and renders true hex colors where the vanilla 1.8
 * protocol cannot (chat especially).
 *
 * <p>Artemis clients register a dedicated plugin-message channel on join; this
 * bridge tracks them and lets plugins push hex chat without touching the wire
 * format. Non-Artemis players are reported as such, so callers can pick their
 * own fallback.</p>
 *
 * <p>Access via {@link com.lunarclient.apollo.Apollo#getArtemis()}. Text uses
 * legacy formatting with the {@code §x§r§r§g§g§b§b} hex form (or plain
 * {@code §} codes); the bridge serializes it internally.</p>
 *
 * @since 1.2.8
 */
public interface Artemis {

    /**
     * Returns whether {@code playerId} is running the Artemis client (its
     * channel is currently registered).
     *
     * @param playerId the player uuid
     * @return {@code true} if the player runs Artemis
     * @since 1.2.8
     */
    boolean isArtemis(UUID playerId);

    /**
     * Displays a rich chat message on the player's Artemis client.
     *
     * @param playerId   the recipient uuid
     * @param legacyText the message, legacy-formatted (hex {@code §x…} allowed)
     * @return a message id usable with {@link #removeChat(UUID, int)}, or
     *         {@code -1} if the player is not an Artemis client (nothing sent)
     * @since 1.2.8
     */
    int chat(UUID playerId, String legacyText);

    /**
     * Removes a previously displayed message from the player's Artemis chat.
     * No-op for non-Artemis clients.
     *
     * @param playerId  the recipient uuid
     * @param messageId the id returned by {@link #chat(UUID, String)}
     * @since 1.2.8
     */
    void removeChat(UUID playerId, int messageId);

    /**
     * Clears the whole Artemis chat of the player. No-op for non-Artemis clients.
     *
     * @param playerId the recipient uuid
     * @since 1.2.8
     */
    void clearChat(UUID playerId);
}
