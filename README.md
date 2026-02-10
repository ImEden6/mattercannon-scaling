# Matter Cannon Scaling

A Fabric mod for Minecraft 1.20.1 that allows the Applied Energistics 2 Matter Cannon to scale with the Ranged Weapon API's damage attribute.

## Features

- **Damage Scaling**: Matter Cannon damage now scales with the `ranged_weapon:damage` attribute.
- **Configurable Multiplier & Additive**: Modpack makers can configure a multiplier and additive value via `config/mattercannon-scaling.properties`.

## Configuration

After first run, a config file will be created at `config/mattercannon-scaling.properties`:

```properties
# Matter Cannon Scaling Configuration
# Formula: (BaseDamage * damage_multiplier) + damage_additive + attribute_bonuses
# damage_multiplier: Multiplier for the base penetration damage (default 1.0)
# damage_additive: Flat damage added after multiplier (default 0.0)
# scaling_entries: Semicolon-separated list of entries in format: attribute,operation,value
#   attribute: The attribute ID (e.g. ranged_weapon:damage, minecraft:generic.attack_damage)
#   operation: ADD or MULTIPLY
#   value: The multiplier for the attribute value
#   Example: ranged_weapon:damage,ADD,1.0;minecraft:generic.attack_damage,MULTIPLY,0.5
damage_multiplier=1.0
damage_additive=0.0
scaling_entries=ranged_weapon:damage,ADD,1.0
```

## License

MIT
