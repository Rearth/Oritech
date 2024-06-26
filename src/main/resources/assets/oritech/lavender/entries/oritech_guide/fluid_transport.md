```json
{
  "title": "Fluids",
  "icon": "oritech:fluid_pipe",
  "category": "oritech:logistics",
  "associated_items": [
    "oritech:fluid_pipe",
    "oritech:small_tank_block"
  ]
}
```

Fluid pipes behave similar to item pipes. However, they also have a small internal storage (on each connection). Like item pipes,
when they are set to extract, they'll extract from all neighboring blocks. However, unlike item pipes, blocks can also push liquids
into a pipe, which the pipe then moves to the next available fluid storage.

;;;;;

To store fluids, you can use a fluid tank. The small fluid tanks stores up to *256* buckets of liquid. When broken, the small tank keep's
all of it's content in the item nbt. When placing it again, it'll keep its content.

<block;oritech:small_tank_block>
