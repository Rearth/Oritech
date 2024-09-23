```json
{
  "title": "Mob Spawning",
  "icon": "oritech:spawner_controller_block",
  "category": "oritech:arcane",
  "associated_items": [
    "oritech:spawner_controller_block",
    "oritech:spawner_cage_block"
  ]
}
```

<block;oritech:spawner_controller_block>

Mobs can be spawned by combining a spawner controller with a spawner cage below. Depending on the size of the spawned entity, a different cage size will be needed.
The type of the spawned mob is determined by 

;;;;;

the first mob to walk over the controller, and can only be changed by replacing the controller. The controller will collect souls
and use them to spawn the set mob. The soul cost depends on the HP of the spawned mob. It will only spawn mobs if an empty surface is available nearby.
When a mob type is set (or when right-clicked if invalid), the spawner will highlight what size of spawner cage is required below. You can also right-click the spawner to

;;;;;

get some information on the current operational state.
