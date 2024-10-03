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

Para atualizar máquinas no Oritech, são usados addons. Eles são blocos que precisam ser anexados à própria máquina ou a um extensor de máquina conectado. Os addons podem fazer uma variedade
de coisas, como aumentar a velocidade, a eficiência energética, dar acesso a slots de inventário específicos e muito mais.

;;;;;

As máquinas só podem aceitar addons em posições específicas. Para visualizar essas posições, verifique a página de UI de "addons" ou procure por esses marcadores na máquina:
![machine_marker](oritech:textures/book/addon_marker.png,fit)

;;;;;

Os addons serão ativados quando a máquina for clicada com o botão direito. Para indicar isso, as partes rosas de um addon ficam azuis quando estão em uso. Para estender o número disponível de slots de addons,
você pode usar extensores de máquina. Esses são addons específicos que não influenciam diretamente a máquina, mas permitem que addons sejam colocados neles, que então contam para a máquina à qual estão conectados.


O número máximo de camadas de extensores de máquina que você pode

;;;;;

usar depende da qualidade da máquina. Se você tiver uma máquina com uma qualidade de núcleo de 1, não poderá usar nenhum extensor. Cada extensor adicional que
passa **por** outro extensor requer um aumento na qualidade do núcleo de 1.

A qualidade do núcleo nunca conta diretamente a quantidade de extensores de máquina que você tem ativos. Em vez disso, conta quantos
extensores um addon precisa atravessar para se conectar à máquina. Se esse número for maior que a qualidade do núcleo, o addon

;;;;;

não será conectado. Veja esta imagem para uma pequena demonstração:
![machine_addons](oritech:textures/book/extenders.png,fit)

;;;;;

As mentioned before, only the number of extenders between a machine and an addon is counted. This means you can branch the extenders and everything will work:
![addon_branching](oritech:textures/book/addon_branching.png,fit)
