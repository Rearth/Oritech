```json
{
  "title": "Steam Power",
  "icon": "oritech:steam_boiler_addon",
  "category": "oritech:processing",
  "associated_items": [
    "oritech:steam_engine_block",
    "oritech:steam_boiler_addon"
  ]
}
```

<block;oritech:steam_boiler_addon>


All generators (except the basic generator) can be upgraded to produce steam. To do so, add a steam boiler addon. When added, the machine will no longer directly produce RF.

;;;;;

Instead, it'll produce steam at a rate of 2:1. To produce steam, water will be consumed. Water and steam liquids can be pumped in and out directly from the addon, but not the machine itself.

To use the steam, a steam engine can be used to produce RF from it. It takes steam as input and outputs water. However, during the process around 20% of the water will be\
lost, so a steady supply of water is needed for the generators.

;;;;;

Multiple steam engines can be chained together. They'll share the energy storage, water and steam tank from the first engine in line. They'll work cooperative.
A steam engines speed varies based on the stored steam. More steam will result in more pressure, making it operate faster. 
The speed is scaled linear based on the steam tanks fill percentage, with a maximum multiplier of 10 when the tank is full.

;;;;;

However, the efficiency of the engine varies based on the speed. A higher efficiency results in more RF per steam unit produced. The machine efficiency is highest when operating at around 700% speed.
Anything lower or higher than that will result in a less ideal yield. Energy will be output from the red machine slots. Fluids ports are marked blue.
