# Vril

A Minecraft **1.20.1 Forge** mod that adds **Vril Ore** and **Deepslate Vril Ore**. Each ore block drops exactly **one Vril** crystal, with no bonus from Fortune.

## Features
- `vril:vril_ore` (stone) and `vril:deepslate_vril_ore` (deepslate)
- `vril:vril` item (always exactly 1 per ore block)
- Silk Touch drops the ore block itself
- Needs an iron pickaxe or better, and gives 1-3 XP
- Generates in the Overworld between Y=-64 and Y=48
- Forge tags: `forge:ores/vril`, `forge:gems/vril`

## Character selection
The first time a player joins a world or server with the mod, an Origins-style screen asks them to pick a character. The choice is saved per world and cannot be dismissed with ESC. Ops can re-open it with `/vril character`.

**Character 1**
- Faster movement (+30%) and +2 attack damage
- Incredibly slow swim speed (-85%)
- Short breath: air drains 3x faster underwater (about 5 seconds)
- One block taller (2.8 blocks) with a step height of 1.1, so full blocks can be walked up

Characters are defined in `character/Characters.java`; adding a new one is a new entry plus lang keys and a portrait in `textures/gui/`.

## Building
Requires JDK 17.

```
./gradlew build
```

The jar is written to `build/libs/`. Drop it in your `mods` folder with Forge 47.3.0+.

## Development
```
./gradlew runClient
```

## Tweaking world generation
Edit `src/main/resources/data/vril/worldgen/placed_feature/vril_ore.json` (`count`, height range) and `configured_feature/vril_ore.json` (`size`).

## License
MIT
