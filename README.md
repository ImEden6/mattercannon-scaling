# Matter Cannon Scaling

Matter Cannon Scaling is a Fabric mod designed for Minecraft 1.20.1 that introduces dynamic damage scaling for the Applied Energistics 2 Matter Cannon. By default, the Matter Cannon's damage is static; this mod allows it to scale with the Ranged Weapon API and other configurable attributes, providing better integration into modpacks with progressive difficulty.

## Features

- **Attribute-Based Scaling**: Seamlessly integrates with the Ranged Weapon API to scale projectile damage based on player attributes.
- **Dynamic Configuration**: A robust configuration system allows for fine-tuning multipliers, flat additives, and specific attribute mappings.
- **Seamless Integration**: Designed to work with Applied Energistics 2, respecting the base mechanics of the Matter Cannon while enhancing its utility in late-game scenarios.

## Configuration

The mod generates a configuration file at `config/mattercannon-scaling.properties` upon initial launch. This file allows for deep customization of the damage formula.

### Damage Formula
The final damage is calculated using the following logic:
`(BaseDamage * damage_multiplier) + damage_additive + attribute_bonuses`

### Config Options
- **damage_multiplier**: A global multiplier applied to the base damage of the projectile.
- **damage_additive**: A flat damage value added to the projectile's power.
- **scaling_entries**: A semicolon-separated list of attribute mappings. Each entry follows the format `attribute,operation,value`.
    - **attribute**: The namespace ID of the attribute (e.g., `ranged_weapon:damage`).
    - **operation**: The mathematical operation to apply (`ADD` or `MULTIPLY`).
    - **value**: The scaling factor for that specific attribute.

## Requirements

The following dependencies are required for the mod to function:

- **Fabric Loader** (0.15.0 or newer)
- **Fabric API**
- **Applied Energistics 2** (15.0.0 or newer)
- **Ranged Weapon API**

## Installation

1. Ensure the correct version of Fabric Loader is installed.
2. Download the latest release from GitHub or CurseForge.
3. Place the mod JAR and all required dependencies into your Minecraft `mods` folder.
4. Launch the game to generate the configuration file.

## Building

To build the mod from source, use the included Gradle wrapper:

```bash
./gradlew build
```

The compiled JAR can be found in `build/libs/`.

---

### License
This project is licensed under the MIT License.
