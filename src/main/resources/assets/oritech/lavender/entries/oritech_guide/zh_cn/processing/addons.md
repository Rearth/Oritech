```json
{
  "title": "Addons",
  "icon": "oritech:machine_extender",
  "category": "oritech:processing",
  "associated_items": [
    "oritech:machine_extender",
    "oritech:capacitor_addon_extender",
    "oritech:machine_speed_addon",
    "oritech:machine_efficiency_addon",
    "oritech:machine_capacitor_addon",
    "oritech:machine_fluid_addon",
    "oritech:machine_yield_addon",
    "oritech:crop_filter_addon",
    "oritech:quarry_addon",
    "oritech:machine_acceptor_addon",
    "oritech:machine_inventory_proxy_addon"
  ],
  "ordinal": 2
}
```

To upgrade machines in oritech, addons are used. They are blocks that need to be attached to the machine itself or a connected machine extender. Addons can do a variety
of things, such as increasing speed, energy efficiency, giving access to specific inventory slots, and much more.

;;;;;

Machines can only accept addons at specific positions. To view these, either check the "addons" UI page, or look for these markers on the machine:
![machine_marker](oritech:textures/book/addon_marker.png,fit)

;;;;;

Addons will be activated when the machine is right-clicked. To indicate that, the pink parts of an addon turn blue when in use. To extend the available number of addon slots,
you can use machine extenders. These are specific addons that don't directly influence the machine, but allow addons to be placed on them, which then count towards the machine they are connected to.


The maximum layers of machine extenders you can

;;;;;

use depends on the machine quality. If you have a machine with a core quality of 1, you can't use any extender. Each additional extender that 
goes **through** another extender requires an increase in core quality of 1.

The core quality never directly counts the amount of machine extenders you have active. Instead, it counts through how many
extenders an addon has to go to be connected to the machine. If this number is greater than the core quality, the addon 

;;;;;

will not be connected. See this image for a small demonstration:
![machine_addons](oritech:textures/book/extenders.png,fit)

;;;;;

As mentioned before, only the number of extenders between a machine and an addon is counted. This means you can branch the extenders and everything will work:
![addon_branching](oritech:textures/book/addon_branching.png,fit)
