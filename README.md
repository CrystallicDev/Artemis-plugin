<div align="center">

![logo](https://cdn.modrinth.com/data/cached_images/c2c98103a149620c1ab5b08e4e87512bfe3af0cd_0.webp)

# Artemis Plugin

[![Forge](https://img.shields.io/badge/Server-Bukkit-yellow)](https://getbukkit.org)

[![ko-fi](https://ko-fi.com/img/githubbutton_sm.svg)](https://ko-fi.com/nqtsu91)

</div>

**Artemis** is a Forge mod that allows Forge clients to interact with Lunar Client's Apollo plugin, through their open protobuf channel.

The point of Artemis is to create a **compatibility** between Forge and Lunar for servers that relies on Lunar's Glowing, Waypoint, TeamView, Marker or other modules.
This mod is "barebones", meaning there is no way to manually create a Lunar waypoint or marker. Its only purpose is to port some of Lunar's functionalities to Forge.

**Artemis** registers Lunar Client's channels when connecting to a server, so the **official Apollo plugin** can fully interact with the client.

## Artemis Specific Features

On top of porting Apollo's modules, Artemis adds a few features of its own that go beyond what Lunar Client itself exposes on 1.8.9 :

- **Full RGB / Hex text rendering** -> Artemis renders true 24-bit hex colors (the `§x§r§r§g§g§b§b` format) **natively in the font renderer**, so they show up *everywhere* text is drawn: chat, tab list, scoreboard sidebar, team prefixes, floating nametags, and `/title` / `/subtitle`. On 1.8.9 both vanilla and Lunar fall back to the 16 legacy colors in most of these spots — Artemis lifts that limitation.
- **Hex colors in the scoreboard & tab list** -> the vanilla 16-character limit on team prefixes/suffixes is removed client-side, so servers can send fully hex-colored player names and have them display correctly in the tab list and the sidebar.
- **Rendering compatibility** -> the hex renderer hooks the single point every draw call goes through, so it keeps working under **OptiFine** and alongside mods that re-render the HUD themselves (such as **OldAnimations**, which rolls the tab list back to its 1.7.10 version).

These features are driven entirely by what the server sends: Artemis only needs the color to be encoded in the `§x` format inside the relevant string (team prefix, title, chat component, ...).

## Availability 
|Loader|Version|
|--------|--------|
| Bukkit | 1.8.9 |
