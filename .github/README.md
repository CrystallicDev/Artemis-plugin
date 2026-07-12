<div align="center">

![logo](https://cdn.modrinth.com/data/cached_images/c2c98103a149620c1ab5b08e4e87512bfe3af0cd_0.webp)

# Artemis Plugin

[![Forge](https://img.shields.io/badge/Platform-Bukkit-yellow)](https://dev.bukkit.org)
[![Forge](https://img.shields.io/badge/Platform-Minestom-pink)](https://minestom.net)
[![Forge](https://img.shields.io/badge/Platform-BungeeCord-orange)](https://github.com/SpigotMC/BungeeCord)
[![Forge](https://img.shields.io/badge/Platform-Velocity-green)](https://papermc.io/software/velocity/)
![Modrinth Downloads](https://img.shields.io/modrinth/dt/artemis-plugin?logo=modrinth&label=Downloads&color=00AF5C)

[![ko-fi](https://ko-fi.com/img/githubbutton_sm.svg)](https://ko-fi.com/nqtsu91)

</div>

<div align="center">

**A fork of [Apollo](https://github.com/LunarClient/Apollo) that adds a server-side bridge to
[Artemis](https://modrinth.com/mod/lunar-artemis), an in-house Forge 1.8.9 client that renders true
hex colors where the vanilla 1.8 protocol cannot.**

</div>

> This is a fork, not a replacement. Every standard Apollo module works unchanged. This repository
> only **adds** the Artemis bridge described below. For anything else, use the
> [Apollo documentation](https://lunarclient.dev/apollo/developers).

---

## What this is

- **[Apollo](https://github.com/LunarClient/Apollo)** is Lunar Client's official server API.
- **[Artemis](https://modrinth.com/mod/lunar-artemis)** is a Forge 1.8.9 mod that answers the Apollo
  protocol like a Lunar client, and additionally renders **24-bit hex colors** in spots where 1.8.9
  normally falls back to 16 colors, most importantly, **chat**.

This fork adds the **server side** of that: a small, official-feeling API to **detect** Artemis
clients and to push **hex-colored chat** to them, with everything about the channel and wire format
hidden behind a clean interface.

## Download 

You can download this plugin, and the associated mod on [modrinth](https://modrinth.com/mod/artemis-plugin).

## The Artemis bridge

Access it through Apollo, like any other part of the API:

```java
import com.lunarclient.apollo.Apollo;
import com.lunarclient.apollo.artemis.Artemis;

Artemis artemis = Apollo.getArtemis();
```

> Available on the **Bukkit** platform (it is installed automatically when Apollo enables). On other
> platforms `getArtemis()` returns `null`.

### Detecting Artemis clients

A client counts as Artemis once it has registered the bridge's plugin-message channel.

```java
if (artemis.isArtemis(player.getUniqueId())) {
    // safe to send hex chat to this player
}
```

This lets you branch your own fallback: send hex through the bridge to Artemis players, and a normal
(down-sampled) message to everyone else.

### Hex-colored chat

`chat(...)` takes **legacy-formatted** text, where hex is written in the `§x§r§r§g§g§b§b` form (plain
`§` color/format codes work too). The bridge serializes it and displays it in the client's **vanilla
chat** (native scroll / fade / removal). It returns a **message id** you can use to update or remove
the line later.

```java
UUID id = player.getUniqueId();

// Display a hex-colored line. Returns a message id, or -1 if the player is not an Artemis client.
int msgId = artemis.chat(id, "§x§2§2§9§9§f§fHello §x§2§2§d§d§6§6world!");

// Re-send with the SAME id to replace it (live update), or remove it entirely:
artemis.removeChat(id, msgId);

// Clear the whole Artemis chat of the player:
artemis.clearChat(id);
```

Producing the legacy hex string from an Adventure component:

```java
LegacyComponentSerializer legacy = LegacyComponentSerializer.builder()
    .character('§')
    .hexColors()
    .build();

artemis.chat(id, legacy.serialize(component));
```

All bridge methods are **no-ops for non-Artemis players** (`chat` returns `-1`), so it is always safe
to call them.

---

## API reference

`com.lunarclient.apollo.artemis.Artemis` obtained via `Apollo.getArtemis()`.

| Method | Description |
|--------|-------------|
| `boolean isArtemis(UUID playerId)` | Whether the player is running Artemis (its channel is registered) |
| `int chat(UUID playerId, String legacyText)` | Display a hex chat line; returns a message id, or `-1` if not an Artemis client |
| `void removeChat(UUID playerId, int messageId)` | Remove a previously displayed message |
| `void clearChat(UUID playerId)` | Clear the player's Artemis chat |

---

## Under the hood

- **Channel:** `artemis:chat` (registered outgoing on startup). A client that registers this channel
  is marked as Artemis; unregistering removes the mark.
- **Payload** (`DataOutputStream`): `byte opcode` `0` display (`int id`, `UTF json`), `1` remove
  (`int id`), `2` clear. The JSON is the Adventure Gson form; `§x§r§r§g§g§b§b` hex is preserved.

You never touch any of this directly, it is fully encapsulated by the `Artemis` interface.

---

## Scope

This fork's addition is intentionally small: **Artemis detection and hex chat only.**

Artemis also renders hex in other surfaces (tab list, scoreboard, team prefixes, nametags, titles),
but those are driven **directly by your plugin** (by putting the `§x…` sequence in the relevant
vanilla string and sending it only to Artemis/Lunar clients) — see the
[Artemis documentation](https://modrinth.com/mod/lunar-artemis). They are not part of this API.

## Usage 

Use it exactly like Apollo; `Apollo.getArtemis()` is available once Apollo is enabled.

## Relationship to Apollo and Artemis

- **Apollo (upstream):** all standard modules are present and unmodified. Non-bridge contributions
  belong upstream at [LunarClient/Apollo](https://github.com/LunarClient/Apollo).
- **Artemis (client):** the client-side counterpart. Without Artemis on the client, `isArtemis`
  returns `false` and the bridge methods do nothing.

## License

Inherits Apollo's MIT license. See [`LICENSE`](LICENSE).
