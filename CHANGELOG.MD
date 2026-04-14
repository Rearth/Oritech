**This update removes owo-lib, and changes the configs to the forge-config-api system. Existing config changes need to be migrated manually, sorry about that**
- Add tainted refinery, a more specialized but different version of the refinery that ties in with the other arcane things of the mod
- Remove owo-lib dependency. This was a huge internal refactor. Configs, particles, recipe serialization, all UIs, and much more have been fully reworked for this.
- Move to forge-config api for config management (available on fabric via forge-config-api-port). Sadly this will break all existing configs / they need to be migrated.
Compatibility for a lot of aspects with other mods should be improved with this change, especially in regards to the UIs. Lots of improvements to the UIs have been made.
- Allow machine gantry worker blocks to be placed on corners
- Add smoke particles when burst addon is overheated
- Add energy storage output limit slider
- Add/improve compatibility recipes for GeOre, Irons' Spellcraft, Evilcraft, AE2, EnderIO
- Allow fluid tanks to be interacted with directly via the UI (with nice UI particles)
- Reduce some foundry + pulverizing/crushing recipe times
- Improve block loot table behaviour for silk touch / fortune (@jshipley)
- Fix oracle index not marked as optional dependency in some manifests
- Fix Biosteel Wiki entry
- Update translations, fix some translation & wiki issues