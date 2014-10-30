fungrenades
===========

A Bukkit plugin for Minecraft; allows players to craft and throw various grenades.

See the project at http://dev.bukkit.org/bukkit-plugins/fungrenades/ 

I'm posting this because I'm not sure how much time I'll have for Bukkit and 
Minecraft in the near future, so please feel free to add and improve if it falls
behind. Please cite me as the original author, and avenge me!

................................................................................
If I had done this correctly from the start, there would be an actual Grenade 
class which would make the code and configs a bit neater. The first version 
added to this repository is v2.2; I did some refactoring to make it a little 
nicer but it's still ugly imo. 

New grenade types can be added by creating a new BukkitRunnable for each 
grenade's effect, to be run when the grenade would detonate. I recommend that
you pick an existing grenade, find every occurrence of it in the code, and do
the same things for your new grenade. Don't forget to add to the configs and 
permissions to plugin.yml.

As mentioned, grenades detonate after a configurable time, but I did fit in a
framework for creating grenades that detonate on impact. The only actual one
I implemented so far is Acme (based on the ACME hole from cartoons). For an
impact grenade, see how the Acme grenade is implemented. Basically it launches
a Projectile instead of an Item, so we can listen for the ProjectileHitEvent.

Aside from just adding new grenades, I'm sure there are other improvements that
could be made, around configs, permissions, mechanics, etc.

Please contact me for insight if needed but keep in mind that I won't often 
have a lot of time to answer in great detail; that's why I'm posting this
in the first place.

waffles87
