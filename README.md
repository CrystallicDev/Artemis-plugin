<div align="center">

![logo](https://cdn.modrinth.com/data/cached_images/c2c98103a149620c1ab5b08e4e87512bfe3af0cd_0.webp)

# Artemis Plugin

[![Apollo](https://img.shields.io/badge/Fork%20of-Apollo-blueviolet)](https://github.com/LunarClient/Apollo)

[![ko-fi](https://ko-fi.com/img/githubbutton_sm.svg)](https://ko-fi.com/nqtsu91)

</div>

**Artemis Plugin** is the server-side companion of [Artemis](https://modrinth.com/mod/lunar-artemis) (the Forge 1.8.9 mod). It is a fork of Lunar Client's [Apollo](https://github.com/LunarClient/Apollo) that adds a small **bridge** to talk to Artemis clients for things the vanilla 1.8 protocol can't express — hex-colored chat and colored lightning.

Everything standard Apollo does (Glowing, Waypoint, TeamView, Marker, Cooldown, Vignette, ...) works **unchanged** — this fork only *adds* the Artemis bridge below.

## The Artemis bridge

Access it through Apollo, the same way on every supported platform:

```java
import com.lunarclient.apollo.Apollo;
import com.lunarclient.apollo.artemis.Artemis;

Artemis artemis = Apollo.getArtemis();
```

- **Detect Artemis clients** — `artemis.isArtemis(uuid)`. A client counts once it registers the Artemis channel (a real Lunar client does not, so you can tell them apart).
- **Hex chat** — `artemis.chat(uuid, legacyText)` displays a message with true 24-bit hex colors (`§x§r§r§g§g§b§b`) inside the client's vanilla chat. Returns an id you can pass to `removeChat(uuid, id)` / `clearChat(uuid)`.
- **Colored lightning** — `artemis.strikeLightning(uuid, x, y, z, color)` spawns a purely visual colored lightning bolt (there is also an overload taking explicit outer-glow and core colors).

All methods are **no-ops for non-Artemis players**, so they are always safe to call. Colors are ARGB ints.

## Supported platforms

| Platform | |
|----------|---|
| Bukkit / Spigot / Paper | ✅ |
| Velocity | ✅ |
| BungeeCord | ✅ |
| Minestom | ✅ |

Folia is not supported (there is no 1.8.9 Folia).

## Download

Grab the plugin for your platform on [Modrinth](https://modrinth.com/mod/lunar-artemis). The client mod is [Artemis](https://modrinth.com/mod/lunar-artemis).

## Relationship to Apollo

This is a fork, not a replacement — all Apollo modules are present and unmodified. For anything other than the Artemis bridge, refer to the [Apollo documentation](https://lunarclient.dev/apollo/developers). Contributions unrelated to Artemis belong upstream at [LunarClient/Apollo](https://github.com/LunarClient/Apollo).

## License

Inherits Apollo's MIT license.
