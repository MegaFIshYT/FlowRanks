# FlowRanks

Fabric client mod for Minecraft 1.21.11 with a declared metadata range through 26.2.

## Features

- FlowRanks name tag ELO using `https://flowpvp.gg/api`.
- The selected mode's icon and rating appear directly before nearby players' Minecraft names.
- Leaderboard and player profile UI with search, stats, history, ranks, and skin preview.
- Keybinds for opening the UI, cycling the tracked game mode, and toggling FlowTiers name tags.

## Controls

- `G`: open FlowTiers leaderboard.
- `Y`: cycle game mode for the overlay.
- `O`: toggle FlowTiers name tags.

All keys can be changed in Minecraft Options -> Controls -> Key Binds -> FlowTiers.

## Build

```powershell
.\gradlew.bat build
```

The built jar will be in `build/libs`.
