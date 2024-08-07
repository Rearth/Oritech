```json
{
  "title": "Drone Port",
  "icon": "oritech:drone_port_block",
  "category": "oritech:logistics"
}
```

<block;oritech:drone_port_block>

The drone port allows you to transport items across vast distances using flying drones. It requires a drone port at the takeoff
and landing position to be built and powered.

;;;;;

Once built, you need to assign the target port using a target designator item. Bind it to the targeted drone port by shift+right-clicking the
target port. Then, on the port you wish the send items from, open the UI, and put the designator into the special item slot.


The target port needs to be at least 50 blocks away. The area it's in also needs to be chunk-loaded.

;;;;;

A drone port can only send items to one specific target port, but a port can receive items from multiple ports. However, each drone
takes a few seconds to land, so if items are coming in too often, one receiving port may be overloaded when targeted by multiple ports.


The time it takes to deliver items is constant, no matter how far the drone has to fly. However, the energy cost increases with distance. 

;;;;;

The square root of the distance is used in the energy usage calculation.