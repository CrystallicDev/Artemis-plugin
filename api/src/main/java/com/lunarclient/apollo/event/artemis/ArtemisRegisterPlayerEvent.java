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
package com.lunarclient.apollo.event.artemis;

import com.lunarclient.apollo.event.Event;
import java.util.UUID;
import lombok.Value;

/**
 * Event for when an Artemis client is registered on the server.
 *
 * <p>Posted once per session, when the player's Artemis chat channel is first
 * seen (on join, or right after the client mod connects). Artemis clients are
 * not {@code ApolloPlayer}s, so this is the Artemis counterpart to
 * {@link com.lunarclient.apollo.event.player.ApolloRegisterPlayerEvent}; the
 * player is identified by uuid, resolve it with your platform's player lookup.</p>
 *
 * @since 1.2.8
 */
@Value
public class ArtemisRegisterPlayerEvent implements Event {

    /**
     * Returns the uuid of the Artemis client that was registered.
     *
     * @return the player uuid
     * @since 1.2.8
     */
    UUID player;

}
