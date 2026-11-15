# DoiBow (Fabric, Minecraft 1.21.11)

A railgun-style bow. Release a drawn DoiBow and 20 arrows leave the bow in the same tick,
in a very tight cone, at 7 blocks/tick.

## Get it
    /function doibow:give        (needs permission level 2, i.e. op / command blocks / datapack functions)
or `/give @s doibow:doibow`. It is also in the Combat creative tab.

## Build the .jar
Requires JDK 21.

    ./gradlew build          (Windows: gradlew.bat build)

Output: `build/libs/doibow-1.0.0.jar`  (ignore the `-sources` jar).
Drop it in `mods/` together with Fabric API 0.141.6+1.21.11 (or newer for 1.21.11) on Fabric Loader >= 0.20.1.
Clients AND the server both need the mod (it registers an item).

## Tuning
Everything is in `src/main/java/com/doibow/DoiBowConfig.java`
(arrow count, speed, spread, durability wear, cooldown, totem behaviour).

## How the damage works
* Arrows are normal vanilla arrows spawned server-side, so damage, armour, enchantments and the
  Totem of Undying are all handled by Minecraft itself.
* Vanilla gives a target 10 ticks of hurt-immunity, which would make 20 simultaneous arrows
  count as one. For DoiBow arrows only, that timer is reset right before each hit (Fabric
  ServerLivingEntityEvents.ALLOW_DAMAGE), so every arrow lands.
* Each arrow that lands also removes extra durability from every armour piece (more for Netherite).
* Arrows can't be picked up, and are discarded when they hit something or after 80 ticks.
