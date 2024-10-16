```json
{
  "title": "Enderic Laser",
  "icon": "oritech:laser_arm_block",
  "category": "oritech:interaction",
  "associated_items": [
    "oritech:laser_arm_block",
    "oritech:fluxite",
    "minecraft:amethyst_cluster",
    "minecraft:amethyst_shard"
  ],
  "ordinal": 6
}
```

![enderic laser](oritech:textures/book/enderic_laser.png,fit)

The enderic laser utilizes

;;;;;

huge amounts of energy to fire a laser beam in a specific direction.

 In most cases, this results in the block being destroyed. 
The dropped blocks are then placed in the inventory. Any blocks that don't fit into the inventory will be lost, so you may want to use an [item pipe](^oritech:logistics/item_transport) to keep getting items.

;;;;;

**Control**

To set the target direction of the laser, select a target with the [target designator](^oritech:tools/target_designator) item. Then shift+right-click the **bottom** laser block to assign the target. The laser will keep firing in the target direction as long as there is something to target. 

*Note that you're only setting the target direction. This means that the laser will also destroy blocks before and behind the target*.
A redstone signal disables the laser.

The maximum range is 64.

;;;;;

**Fluxite Harvesting**


The huge amounts of energy from the enderic laser cause grown amethyst clusters to transform into fluxite when they are destroyed.
<block;minecraft:amethyst_cluster>

They also speed up amethyst growth when aimed at the budding amethyst itself.

;;;;;

**Energy Transfer**

When the enderic laser is targeting a block that can store energy (e.g. any machine), it will fill the energy storage of the machine.
The laser ignores all input and output limits, and can fill the energy storage of machines that may not accept energy from cables directly.

;;;;;

**Machine Hunter Addon**

When the enderic laser has a machine hunter addon, it will target mobs instead of blocks. Addon upgrades still work when hunting mobs: the yield addon will make the
mobs drop more loot, the more hunter addons will extend the target scan range, speed addons will increase the damage done (and reduce the energy efficiency), and the crop filter will avoid killing baby animals.

;;;;;

If you are wearing an [exo chestplate](^oritech:tools/exo_armor) then the enderic laser will charge it for you.

**Hunter Targeting**

Use a [target designator](^oritech:tools/target_designator) on the enderic laser to change targeting modes. The default is attacking monsters only, but you can also have it attack animals or even wandering traders. Be careful where you use it and how you set the targeting, because you could end up killing things you don't want to kill.

;;;;;

**More details**

The laser beam will target any block, but it passes through glass (and not-grown amethysts). Quarry addons will increase the width of the excavated area.

The laser itself only has 1 addon slot available at the bottom. Addons will only affect the speed and efficiency of block breaking, energy transfer can only benefit from speed upgrades.

A target block will stop the laser from going any further, while not being destroyed.