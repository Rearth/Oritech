```json
{
  "title": "Destructeur de Blocs",
  "icon": "oritech:destroyer_block",
  "category": "oritech:interaction",
  "associated_items": [
    "oritech:destroyer_block"
  ],
  "ordinal": 3
}
```

<block;oritech:destroyer_block[machine_assembled=true]>

Le destructeur de blocs est utilisé pour ... détruire des blocs! C'est une structure [multi-bloc](^oritech:processing/multiblocks) qui fonctionne avec un [cadre de machine](^oritech:interaction/machine_frames) et cible la couche de blocs située directement sous le cadre.

;;;;;

Le temps et l'énergie pour détruire un bloc dépendent de la dureté de celui-ci. Le destructeur tente de détruire tous les blocs en dessous. Pour une utilisation agricole, le module de filtre de culture peut être installé, il fera en sorte que le destructeur ignore les cultures non mûres.<block;oritech:crop_filter_addon>

;;;;;

En ajoutant des modules de carrière, le destructeur de blocs peut également être utilisé comme une carrière. Chaque module de carrière multiplie la portée par 8.


Cela signifie qu'un module donne une portée de 8, deux module donnent une portée de 64, etc.
<block;oritech:quarry_addon>