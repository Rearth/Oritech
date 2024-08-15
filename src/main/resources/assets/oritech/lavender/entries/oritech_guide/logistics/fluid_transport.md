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
when they are set to extract, they'll extract from all neighboring blocks. However, unlike item pipes, blocks can also push fluids
into a pipe, which the pipe then moves to the next available fluid storage.

;;;;;

To store fluids, you can use a fluid tank. The small fluid tanks stores up to *256* buckets of fluid. When broken, the small tank keep's
all of it's content in the item nbt. A comparator output will reflect the tanks fill status. Stacked tanks will automatically let the fluid flow down.

<block;oritech:small_tank_block>
