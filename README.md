# FlowRanks

A client-side Fabric mod for the FlowPvP server that displays player ELO ratings and ranks directly in Minecraft.

## Features

- ELO and rank icons in the tab list next to every player's name
- Rank display above player name tags in the world
- Leaderboards for all 9 gamemodes (Sword, Axe, UHC, Vanilla, Mace, Pot, NethOP, SMP, DiamondSMP)
- Full player stats with ELO history and recent match history
- Customizable HUD showing your gamemode, your ELO, and your opponent's ELO during matches
- Skin previews and cached avatars in the leaderboard

## Keybinds

- `G` — Open leaderboard
- `Y` — Cycle gamemode
- `O` — Toggle name tags
- `H` — Open HUD editor

All keys can be rebound in Options → Controls → Key Binds → FlowRanks.

## Requirements

- Minecraft 1.21.11
- Fabric Loader 0.18.2+
- Fabric API 0.139.5+1.21.11

## Build

```powershell
.\gradlew.bat build
```

The built jar will be in `build/libs`.
