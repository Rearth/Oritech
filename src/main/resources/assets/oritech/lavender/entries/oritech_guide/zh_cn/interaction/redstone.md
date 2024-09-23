```json
{
  "title": "Redstone",
  "icon": "oritech:energy_pipe",
  "category": "oritech:interaction",
  "associated_items": [
    "oritech:machine_redstone_addon",
    "minecraft:redstone"
  ]
}
```

Some Oritech machines can directly interact with redstone, such as the portable tank and the portable energy storage. The portable tanks content can be measured using a comparator, 
and the portable energy storages' output can be disabled with a redstone signal. For all other blocks, a "Redstone Addon Controller" is required.

;;;;;

The redstone addon controller can be attached just like any other addon, and can be configured via the UI. When configured, the data can be read using a comparator. The comparator signal will be
output from the addon, not the machine itself. The machine can also be disabled with a redstone signal to the addon.