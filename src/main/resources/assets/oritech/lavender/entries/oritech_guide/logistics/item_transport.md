```json
{
  "title": "Item Transportation",
  "icon": "oritech:item_filter_block",
  "category": "oritech:logistics",
  "associated_items": [
    "oritech:item_pipe",
    "oritech:item_filter_block"
  ]
}
```

Oritech includes item transport pipes and item filter blocks to fulfill your item logistic needs. Item pipes connect to each other and all
neighboring inventories.

;;;;;

Unlike other pipes, the item pipes do not have an inventory. This means that other blocks (such as a hopper)
cannot insert items into the pipe network on their own. 
Instead, an item pipe can be set to extract from a nearby inventory. To do so,
just right click a pipe block (that's connected to something).
If you have multiple inventories connected to the same pipe block,
you'll notice that all connection turn to extraction mode. This is a

;;;;;

current limitation of oritech pipes. If you set a pipe block to extract, it will try to extract
from all blocks that are connected to this pipe block. So to actually transport anything, you'll need the pipe network to consist of at least 2 blocks.

Extracted items will be put into the **closest** available inventory further down the network.

;;;;;

The maximum transfer range is 64 blocks. Any network longer than that needs to be split.


Pipes will always extract from the first non-empty slot in an inventory. If the item cannot be put into an inventory of the pipe network, it'll block the
pipe from extracting from that inventory.

;;;;;

**Item Filters**
<block;oritech:item_filter_block>
To filter which items go where, you can use item filters. They are blocks you can place next to the target inventory. It has 5 input sides, 
and always outputs to the side it's facing.
It only accepts items which match the filter set via the UI, and automatically outputs them to the target inventory.

;;;;;

However, it will not automatically extract items from neighboring inventories.
