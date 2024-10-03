```json
{
  "title": "Laser Endérico",
  "icon": "oritech:laser_arm_block",
  "category": "oritech:interaction",
  "associated_items": [
    "oritech:laser_arm_block",
    "oritech:fluxite",
    "minecraft:amethyst_cluster",
    "minecraft:amethyst_shard"
  ],
  "ordinal": 6
}
```

![laser endérico](oritech:textures/book/enderic_laser.png,fit)

O laser endérico utiliza

;;;;;

quantidades enormes de energia para disparar um feixe de laser em uma direção específica.

 Na maioria dos casos, isso resulta na destruição do bloco. 
Os blocos caídos são então colocados no inventário. Qualquer bloco que não couber no inventário será perdido, então você pode querer usar um [tubo de item](^oritech:logistics/item_transport) para continuar recebendo itens.

;;;;;

**Controle**

Para definir a direção do alvo do laser, selecione um alvo com o item [designador de alvo](^oritech:tools/target_designator). Em seguida, shift + clique com o botão direito no bloco **inferior** do laser para atribuir o alvo. O laser continuará disparando na direção do alvo enquanto houver algo para mirar. 

*Observe que você está apenas definindo a direção do alvo. Isso significa que o laser também destruirá blocos antes e atrás do alvo.*.
Um sinal de redstone desabilita o laser.

O alcance máximo é 64.

;;;;;

**Colheita de Fluxita**


As enormes quantidades de energia do laser endérico fazem com que aglomerados de ametista crescidos se transformem em fluxita quando são destruídos.
<block;minecraft:amethyst_cluster>

Eles também aceleram o crescimento da ametista quando mirados na ametista em brotação.

;;;;;

**Transferência de Energia**

Quando o laser endérico está mirando um bloco que pode armazenar energia (por exemplo, qualquer máquina), ele preencherá o armazenamento de energia da máquina.
O laser ignora todos os limites de entrada e saída e pode preencher o armazenamento de energia de máquinas que podem não aceitar energia diretamente dos cabos.

;;;;;

**Mais detalhes**

O feixe de laser mirará qualquer bloco, mas passa através do vidro (e ametistas não crescidas). Os addons de pedreira aumentarão a largura da área escavada.

O laser em si possui apenas 1 slot de addon disponível na parte inferior. Os addons afetarão apenas a velocidade e a eficiência da quebra de blocos; a transferência de energia só pode se beneficiar de upgrades de velocidade.

Um bloco-alvo impedirá que o laser avance mais, sem ser destruído.