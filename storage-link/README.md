# Backpack Storage Link

Fabric mod for Minecraft 26.3 that links [Tom's Simple Storage](https://github.com/tom5454/Toms-Storage)
(2.12.1) with [Traveler's Backpack](https://github.com/Tiviacz1337/Travelers-Backpack) (11.4.1).
Both are required.

Every Tom's terminal (Storage Terminal, Crafting Terminal, Wireless and Advanced Wireless Terminal) also
lists the items in your backpacks while you use it:

- **Which backpacks:** the one you wear (back slot or Trinkets slot) and every backpack item in your
  inventory, hotbar and offhand. Only the main storage counts, not the tool slots. Backpacks inside other
  containers are not included.
- **Display:** backpack items are merged into the normal list. The tooltip shows how many of them are in
  your backpacks.
- **Taking items:** the network is used first; only what it can't supply comes from your backpacks.
- **Putting items in:** always goes into the network, never into a backpack.
- **Crafting Terminal:** crafting, grid refill and recipe filling (recipe book, JEI) can use backpack items.
- **Toggle:** a backpack button below the terminal's other buttons turns it on or off. The setting is per
  player and saved; it starts on.
- **Multiplayer:** each player only sees and uses their own backpacks.

Tom's Simple Storage has no API for this, so the mod hooks into its terminal menu and may need an update when
Tom's Simple Storage changes.
