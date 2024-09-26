```json
{
  "title": "Transporte de Itens",
  "icon": "oritech:item_filter_block",
  "category": "oritech:logistics",
  "associated_items": [
    "oritech:item_pipe",
    "oritech:item_filter_block"
  ]
}
```

O Oritech inclui tubos de transporte de itens e blocos de filtro de itens para atender às suas necessidades logísticas de itens. Os tubos de itens se conectam entre si e a todos
os inventários vizinhos.

;;;;;

Ao contrário de outros tubos, os tubos de itens não possuem um inventário. Isso significa que outros blocos (como um funil)
não podem inserir itens na rede de tubos por conta própria.
Em vez disso, um tubo de itens pode ser configurado para extrair de um inventário próximo. Para fazer isso,
basta clicar com o botão direito em um bloco de tubo (que esteja conectado a algo).
Se você tiver múltiplos inventários conectados ao mesmo bloco de tubo,
notará que todas as conexões mudam para o modo de extração. Essa é uma

;;;;;

limitação atual dos tubos do Oritech. Se você configurar um bloco de tubo para extrair, ele tentará extrair
de todos os blocos que estão conectados a esse bloco de tubo. Portanto, para realmente transportar algo, você precisará que a rede de tubos consista em pelo menos 2 blocos.

Os itens extraídos serão colocados no inventário disponível **mais próximo** mais adiante na rede.

;;;;;

O alcance máximo de transferência é de 64 blocos. Qualquer rede mais longa do que isso precisa ser dividida.


Os tubos sempre extrairão do primeiro slot não vazio em um inventário. Se o item não puder ser colocado em um inventário da rede de tubos, isso bloqueará
o tubo de extrair desse inventário.

;;;;;

**Filtros de Itens**
<block;oritech:item_filter_block>
Para filtrar quais itens vão para onde, você pode usar filtros de itens. Eles são blocos que você pode colocar ao lado do inventário alvo. Possui 5 lados de entrada, 
e sempre envia a saída para o lado em que está virado.
Ele só aceita itens que correspondem ao filtro definido pela interface do usuário e os envia automaticamente para o inventário alvo.

;;;;;

No entanto, ele não extrairá automaticamente itens de inventários vizinhos.
