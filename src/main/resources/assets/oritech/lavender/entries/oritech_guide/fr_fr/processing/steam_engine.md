```json
{
  "title": "Chaudière à Vapeur",
  "icon": "oritech:steam_boiler_addon",
  "category": "oritech:processing",
  "associated_items": [
    "oritech:steam_engine_block",
    "oritech:steam_boiler_addon"
  ]
}
```

<block;oritech:steam_boiler_addon>

Tous les générateurs (sauf le générateur basique) peuvent être améliorés pour produire de la vapeur. Pour ce faire, ajoutez un module de chaudière à vapeur. La machine ne produira plus directement du RF.

;;;;;


À la place, elle produira de la vapeur à un taux de 2:1. Pour produire de la vapeur, de l'eau sera consommée. L'eau et la vapeur peuvent être pompées directement dans et hors du module, mais pas de la machine elle-même.


Pour utiliser la vapeur, un moteur à vapeur peut être employé pour produire du RF à partir de celle-ci. Il consomme de la vapeur en entrée et renvoie de l'eau en sortie. Cependant, environ 20 % de l'eau sera perdue 

;;;;;

au cours du processus, donc un approvisionnement constant en eau est nécessaire pour les générateurs.


Plusieurs moteurs à vapeur peuvent être enchaînés. Ils partageront le stockage d'énergie, le réservoir d'eau et de vapeur du premier moteur de la chaîne et fonctionneront de manière coopérative. La vitesse d'un moteur à vapeur varie en fonction de la quantité de vapeur stockée. 

;;;;;

Plus de vapeur crée plus de pression, ce qui le fait fonctionner plus rapidement. La vitesse est proportionnelle au pourcentage de remplissage du réservoir de vapeur, avec un multiplicateur maximal de 10 lorsque le réservoir est plein.


Cependant, l'efficacité du moteur varie en fonction de la vitesse. Une efficacité plus élevée entraîne plus de RF par unité de vapeur produite. L'efficacité est 

;;;;;

maximale lorsque le moteur fonctionne à environ 700 % de sa vitesse. Toute vitesse inférieure ou supérieure à ce taux entraîne un rendement moins optimal. L'énergie sera émise depuis les emplacements rouges de la machine, tandis que les ports de fluides sont marqués en bleu.
