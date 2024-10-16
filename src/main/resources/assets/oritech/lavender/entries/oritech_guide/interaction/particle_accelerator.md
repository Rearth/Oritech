```json
{
  "title": "Particle Accelerator",
  "icon": "oritech:accelerator_ring",
  "category": "oritech:interaction",
  "associated_items": [
    "oritech:accelerator_ring",
    "oritech:accelerator_motor",
    "oritech:accelerator_controller",
    "oritech:accelerator_sensor",
    "oritech:black_hole_block"
  ],
  "ordinal": 6
}
```

<block;oritech:accelerator_ring>

The particle accelerator can be used to, as you guessed it, accelerate particles. The particle starts from the particle accelerator block. In it, you can insert any item that
will be used as particle. A straight guide

;;;;;

ring needs to be placed right behind the controller, facing to the side. The visuals should match up.


Once inserted, the particle will travel along a route defined with the guide rings. They can be right-clicked to add a 45-degree turn on one side. Another guide ring needs
to be in the path of the particle as defined by the guide rings. Depending on the speed, the allowed distance between guide blocks increases.

;;;;;

The maximum distance is calculated with the following formula:

> clamp(sqrt(speed), 2, 10)

This basically means that at higher speed the distance between guide blocks can be bigger.

;;;;;

**Redstone Switches**

When a redstone signal is given to a non-straight guide block, it will turn 
into a switch block.
When it is powered on, it will lead particles straight, and when powered off it will guide it into the original curved direction. However, a particle can also 
enter from the 'other' direction. This is visualized with the smaller red glass tube. It will follow the path of the white tube, and can enter from both the red and white tube.

;;;;;

At higher speeds, the particle will not be able to take too tight turns anymore. If the last full 90-degree turn is too close behind, it will exit the guided path and instead
shoot out into the world. If the distance between guide blocks is too big, or no next guide is found, it will also shoot out into the world. The minimum distance between turns
is calculated this way:

> sqrt(speed) / 3

;;;;;

**Interactions**

Entities hit by the particle will take damage based on the current speed of the particle. When exiting the guided path, it will also hurt entities in its path and destroy blocks
until no more momentum is available. When two particle collide (from different controllers), they can create new items.


When passing through an accelerator motor, the particle will speed up by 1 m/s. This requires the motor

;;;;;

to be powered. The power requirement increases with speed.


Both particle motors and sensors can be used as straight guides.

;;;;;

**Speed Sensors**

The particles' speed can be measured with a particle sensor. A comparator can then be used to get a redstone signal based on the particles speed.
The following table shows the required speed for each redstone level:


1. 0
2. 10
3. 50
4. 75
5. 100

;;;;;


6. 150
7. 250
8. 500
9. 750
10. 1000
11. 2500
12. 5000
13. 7500
14. 10000
15. 15000

;;;;;

**Accelerator Design**

The particles accelerators can be built in various ways, depending on their goals. You can build a straight line of motors to just shoot the particles at something. However,
if you want to reach higher speeds, a circular design is usually much more efficient. When trying to reach certain elements, very large rings might be needed. As slow
particles require guide rings to be close together, it often makes sense to first start the

;;;;;

particle in a small ring, and then use redstone to take it into a bigger ring.

Multiple ring stages might be needed for some cases.

;;;;;

**Dimensional Incursions**

When certain elements collide with too much energy, you will rip a hole into space-time, creating a small dimensional incursion. Since the amount of energy required
to achieve this is immense, very little is known about these incursions and what triggers them. Researchers have noted that colliding fire charges with a collision
energy over 5000J seems to bring the nether closer. Ender pearls and more than 10000J appear to

;;;;;

be doing the same for the end dimension. 


There are rumors of scientists trying
to bombard one of these incursions with speeds eclipsing the highest measured values. However, none lived to tell the tale.


