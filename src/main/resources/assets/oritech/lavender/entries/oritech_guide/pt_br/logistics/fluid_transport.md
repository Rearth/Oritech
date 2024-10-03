```json
{
  "title": "Fluidos",
  "icon": "oritech:fluid_pipe",
  "category": "oritech:logistics",
  "associated_items": [
    "oritech:fluid_pipe",
    "oritech:small_tank_block"
  ]
}
```

Os tubos de fluido se comportam de maneira semelhante aos tubos de itens. No entanto, eles também possuem um pequeno armazenamento interno (em cada conexão). Assim como os tubos de itens,
quando estão configurados para extrair, eles extrairão de todos os blocos vizinhos. No entanto, ao contrário dos tubos de itens, os blocos também podem empurrar fluidos
para um tubo, que o tubo então move para o próximo armazenamento de fluido disponível.

;;;;;

Para armazenar fluidos, você pode usar um tanque de fluido. Os pequenos tanques de fluido armazenam até *256* baldes de fluido. Quando quebrado, o pequeno tanque mantém
odo o seu conteúdo no NBT do item. A saída do comparador refletirá o status de preenchimento do tanque. Tanques empilhados permitirão automaticamente que o fluido flua para baixo.

<block;oritech:small_tank_block>
