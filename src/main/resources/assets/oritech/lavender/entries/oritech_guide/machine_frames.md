```json
{
  "title": "Machine Frames",
  "icon": "oritech:machine_frame_block",
  "category": "oritech:interaction",
  "associated_items": [
    "oritech:machine_frame_block",
    "oritech:destroyer_block",
    "oritech:placer_block",
    "oritech:fertilizer_block"
  ]
}
```

The *[block placer](^oritech:block_placer), [block destroyer](^oritech:block_destroyer) and [fertilizer](^oritech:fertilizer)* all operate on a gantry that is built with machine frames. The machine frame designates
the area the machines operate in. The machines always target the blocks **below** the frame. The frame itself needs to be rectangular and empty inside.

Any number of machines can operate on the same machine frame. To do so, simple place

;;;;;

multiple machines on the frame. The machines always iterate through all blocks in the frame area. 


All machines that operate on machine frames can used most addons. 
The speed addon increases both the movement and operation speed.
