```json
{
  "title": "Énergie",
  "icon": "oritech:energy_pipe",
  "category": "oritech:logistics",
  "associated_items": [
    "oritech:energy_pipe",
    "oritech:small_storage_block",
    "oritech:large_storage_block"
  ]
}
```

Oritech utilise le {gold}**RF**{} pour alimenter toutes ses machines. Il utilise l'API Reborn Energy pour ce faire. Cela signifie qu'Oritech est compatible avec tous les mods utilisant le système d'énergie de Tech Reborn, qui inclut actuellement presque tous les mods utilisant l'énergie sur Fabric.

;;;;;

Il n'existe qu'un seul niveau de câble, capable de transférer jusqu'à {gold}10k RF/t{}.


Les générateurs émettent l'énergie, et toutes les autres machines acceptent l'énergie de tous les côtés (et ne la renvoient pas). Les câbles eux-mêmes stockent jusqu'à {gold}10k RF{} dans chaque connexion de machine si l'énergie ne peut pas être transmise.

;;;;;

Vous pouvez utiliser des blocs de stockage d'énergie. Ils sont disponibles en 2 tailles et peuvent être étendus avec des modules. C'est blocs acceptent l'énergie avec un port {green}vert{}, et peuvent la sortir que par le seul port {red}rouge{}.

Un signal de redstone désactivera toute sortie d'énergie.
<block;oritech:small_storage_block>

;;;;;

L'énergie peut également être transférée sans fil en utilisant un [laser enderique](^oritech:interaction/enderic_laser).

![enderic laser](oritech:textures/book/enderic_laser.png,fit)
