```json
{
  "title": "Enchanting",
  "icon": "oritech:enchanter_block",
  "category": "oritech:arcane",
  "associated_items": [
    "oritech:enchantment_catalyst_block",
    "oritech:enchanter_block"
  ]
}
```

Oritech has two methods of applying enchantments. All Oritech equipment and tools are enchantable. The first option is the stabilised enchanter, which allows applying "normal" enchantments to equipment.
The second option is the arcane catalyst, which can apply any enchantment from an enchanted book to any item, at any level.

;;;;;

<block;oritech:enchanter_block>

The stabilized enchanter can operate on a single item. When an item is inserted, you can select the enchantment to be applied from the GUI. 
The enchantment is stored for the next items, but can also be changed.
Both energy and souls are required to operate. The enchanter doesn't collect souls

;;;;;

itself. Instead, nearby enchantment catalysts are needed with souls stored in them. 
The number of required catalysts depends on the level of the enchantment. Only catalysts with stored souls are counted.

;;;;;

<block;oritech:enchantment_catalyst_block>

The arcane catalyst block can store souls and provide them to the stabilized enchanter. It may also enchantment items using books. The used book needs to be at the maximum level of the enchantment.
It can apply any enchantment to any item.

;;;;;

*Hyper Enchanting*:
If a tool is already at the maximum or higher level of the applied enchantment, it will be hyper enchanted. This massively increases the enchantment cost, but allows you to go over the default max level.

;;;;;

*Stabilizing*:
By default the arcane catalyst can only store 50 souls. However, hyper enchanting will require more than 50 souls. You can stabilize the arcane catalyst using enderic lasers. Each laser
increases the amount of souls that can be stored. However, consequences can be catastrophic if the stabilization is lost while souls are stored.
