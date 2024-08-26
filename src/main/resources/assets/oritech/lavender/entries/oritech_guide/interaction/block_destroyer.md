```json
{
  "title": "Block Destroyer",
  "icon": "oritech:destroyer_block",
  "category": "oritech:interaction",
  "associated_items": [
    "oritech:destroyer_block"
  ],
  "ordinal": 3
}
```

<block;oritech:destroyer_block[machine_assembled=true]>

The block destroyer is used to, as you guessed it, destroy blocks. It's a [multi-block](^oritech:processing/multiblocks) that operates on a [machine frame](^oritech:interaction:machine_frames), and targets the block layer directly below the frame.

;;;;;

The time and energy it takes to break a block is based on the blocks' hardness. The block destroyer tries to destroy all blocks below. To allow farming usage, the crop filter addon can be installed.
This will cause the block destroyer to skip all non-finished crops.


<block;oritech:crop_filter_addon>

;;;;;

By adding quarry addons, the block destroyer can also be used as a quarry. Each quarry addon multiplies the range by 8. 

This means one addon gives it 8 range, 2 addons give 64 range, and 3 addons give 512 range.

<block;oritech:quarry_addon>