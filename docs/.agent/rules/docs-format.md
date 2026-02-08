---
trigger: always_on
---

- Use the knowledge of the existing dirs as source knowledge. Files in .content and its subdirectories are new / being created and not yet always accurate. That what you're here working on.
- The entries are for the moddedmc wiki. You are writing content pages, in addition to some more detailed documentation pages that will follow later.
- Don't change the frontmatter of the pages, except for adding a title! Always add a title.
- Don't be too formal. Keep the text simple to understand, and don't use too fancy words. Avoid overusing jargon or corporate speak.
- Dont run any commands
- Always remove the first heading to not duplicate the title from the frontmatter!
- Dont make lines too long. Use markdown soft-line breaks to make the source viewable.
- Pages already include a picture of the block/item. Don't create a picture of just the block id at the begin. Special other images (e.g. area/something) could work though.
- Don't make it sounds like ai. Keep texts direct.
- Don't invent anything. Only describe facts if they're in the existing documentation or the code. Ensure everything is from one of the context sources.
- Normal default markdown is also possible / desired.
- Create links to other content pages where applicable. The format is "[Link Title](@oritech:CONTENT_NAME)"
- Create links to other documentation pages where applicable. they're used to explain some more complex topics in depth (e.g. particle acceleration, reactors, addon concepts, etc. The format is "[]($DOCS_NAME)"
- The content pages shouldn't go too deep if its a topic that include multiple parts. There's specific docs pages that explain the whole concept later (e.g. for particle acceleration, reactors, souls, etc.).
- Use these markdown features where applicable:
<center>
<ModAsset location="oritech:area/fluxite_mining" width={512} />
</center>

or:
<Callout variant="warning">
    Any blocks that don't fit into the inventory will be lost, so you may want to use an [item pipe](../logistics/item_transport) to continue collecting items.
</Callout>

---

**Control**

To set the target direction of the laser, select a target with the [target designator](../tools/target_designator) item.
Then shift + right-click the **bottom** laser block to assign the target. The laser will keep firing in the target direction as long as there is something to target.

A redstone signal disables the laser. If it tries to mine a target block, it will stop the beam there.

The maximum range is 128 blocks.

<Callout variant="danger">
    The laser only has a target direction, not position. This means that it'll keep destroying blocks in that direction till there's nothing left. Be careful setting when setting it
    up in your base.
</Callout>
