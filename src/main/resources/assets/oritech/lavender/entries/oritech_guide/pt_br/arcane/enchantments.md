```json
{
  "title": "Encantamento",
  "icon": "oritech:enchanter_block",
  "category": "oritech:arcane",
  "associated_items": [
    "oritech:enchantment_catalyst_block",
    "oritech:enchanter_block"
  ]
}
```

O Oritech possui dois métodos de aplicação de encantamentos. Todos os equipamentos e ferramentas Oritech são encantáveis. A primeira opção é o encantador estabilizado, que permite aplicar encantamentos
"normais" aos equipamentos. A segunda opção é o catalisador arcano, que pode aplicar qualquer encantamento de um livro encantado a qualquer item, em qualquer nível.

;;;;;

<block;oritech:enchanter_block>

O encantador estabilizado pode operar em um único item. Quando um item é inserido, você pode selecionar o encantamento a ser aplicado na GUI.
O encantamento é armazenado para os próximos itens, mas também pode ser alterado.
Tanto energia quanto almas são necessárias para operar. O encantador não coleta almas

;;;;;

por si só. Em vez disso, são necessários catalisadores de encantamento próximos com almas armazenadas.
O número de catalisadores necessários depende do nível do encantamento. Apenas catalisadores com almas armazenadas são contabilizados.

;;;;;

<block;oritech:enchantment_catalyst_block>

O bloco de catalisador arcano pode armazenar almas e fornecê-las ao encantador estabilizado. Ele também pode encantar itens usando livros. O livro usado precisa estar no nível máximo do encantamento.
Pode aplicar qualquer encantamento a qualquer item.

;;;;;

*Hiperencantamento*:
Se uma ferramenta já estiver no nível máximo ou superior do encantamento aplicado, ela será hiperencantada. Isso aumenta massivamente o custo do encantamento, mas permite que você ultrapasse o nível máximo padrão.

;;;;;

*Estabilização*:
Por padrão, o catalisador arcano pode armazenar apenas 50 almas. No entanto, o encantamento hiper exigirá mais de 50 almas. Você pode estabilizar o catalisador arcano usando lasers endéricos. Cada laser
aumenta a quantidade de almas que podem ser armazenadas. No entanto, as consequências podem ser catastróficas se a estabilização for perdida enquanto as almas estão armazenadas.
