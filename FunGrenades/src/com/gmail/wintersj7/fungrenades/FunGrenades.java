package com.gmail.wintersj7.fungrenades;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Bat;
import org.bukkit.entity.EnderPearl;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Fireball;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.SmallFireball;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BlockIterator;
import org.bukkit.util.Vector;

import com.gmail.wintersj7.fungrenades.FunGrenades;



public class FunGrenades extends JavaPlugin {
	private String SMOKEBOMB_NAME, GLUEBOMB_NAME, PINEAPPLE_NAME, CLASSIC_NAME, NAPALM_NAME, TEARDROP_NAME;
	private String ICICLE_NAME, JACKHAMMER_NAME;
	private String PESTER_NAME, ZAPPER_NAME, DECAY_NAME;
	private String ACME_NAME, STICKY_NAME, HOMER_NAME, PNEUMATIC_NAME, NULL_NAME, GROWTH_NAME;
	
	private int THROW_DELAY; // (ticks; 20 = 1s)
	private int BAT_DURATION;
	private int ZAP_DAMAGE;
	private int ZAP_PROPEGATION;
	private int DECAY_DAMAGE;
	private int ACME_HOLE_RADIUS;
	private final int PICKUP_DELAY = Integer.MAX_VALUE;
	private final double GRENADE_VECTOR_FACTOR = 1.6;
	
	
	// List of players that are on grenade throw cooldown.
	private static ArrayList<Player> playerList;
	
	// Itemstacks for all FunGrenades (v1.1)
	ItemStack Smokebomb, Gluebomb, Pineapple, Classic, Napalm, Teardrop, Icicle, Jackhammer;
	
	// Itemstacks for all FunGrenades (v2.0)
	ItemStack Pester, Zapper,  Decay;
	
	// Itemstacks (v2.2)
	ItemStack Acme;
	
	// Unimplemented
	ItemStack Sticky, Homer, Pneumatic, Null, Growth;
	
	// Crafting Recipes
	HashMap<String,Recipe> grenadeRecipes;
	
	// These HashMaps keep track of who throws projectiles for detonation on impact.
	private HashMap<UUID, BukkitRunnable> impactProjectiles;
	private HashMap<UUID, Location> impactLocations;
	private HashMap<UUID, Player> impactPlayers;
	
	// The list of materials that are safe to rightclick without launching a grenade.
	private ArrayList<Material> safeMaterials;
	
	
	@Override
	public void onEnable() {
		new FunGrenadesListener(this);
		this.saveDefaultConfig();
		THROW_DELAY = this.getConfig().getInt("throw_cooldown");
		ZAP_PROPEGATION = this.getConfig().getInt("zap_propegation");
		ZAP_DAMAGE = this.getConfig().getInt("zap_damage");
		BAT_DURATION = 220;
		DECAY_DAMAGE = this.getConfig().getInt("decay_damage");
		ACME_HOLE_RADIUS = 2;
		FunGrenades.playerList = new ArrayList<Player>();
		
		safeMaterials = new ArrayList<Material>();
		List<String> tempList = this.getConfig().getStringList("right_click_safe_materials");
		Iterator<String> it = tempList.iterator();
		while (it.hasNext()){
			String s = it.next();
			try {
				Material m = Material.getMaterial(s);
				safeMaterials.add(m);
			}
			catch (Exception e){
				System.out.println("Invalid material in safe_materials config: " + s);
			}
		}
		
		impactProjectiles = new HashMap<UUID, BukkitRunnable>();
		impactLocations = new HashMap<UUID, Location>();
		impactPlayers = new HashMap<UUID, Player>();
		
		// If renaming is disabled, put §6 at the front of the grenade names.
		boolean rename = this.getConfig().getBoolean("renaming_allowed");
		SMOKEBOMB_NAME = rename ? "Smokebomb" : "§6Smokebomb";
		GLUEBOMB_NAME = rename ? "Gluebomb" : "§6Gluebomb";
		PINEAPPLE_NAME = rename ? "Pineapple" : "§6Pineapple";
		CLASSIC_NAME = rename ? "Classic" : "§6Classic";
		NAPALM_NAME = rename ? "Napalm" : "§6Napalm";
		TEARDROP_NAME = rename ? "Teardrop" : "§6Teardrop";
		ICICLE_NAME = rename ? "Icicle" : "§6Icicle";
		JACKHAMMER_NAME = rename ? "Jackhammer" : "§6Jackhammer";
		PESTER_NAME = rename ? "Pester" : "§6Pester";
		ZAPPER_NAME = rename ? "Zapper" : "§6Zapper";
		DECAY_NAME = rename ? "Decay" : "§6Decay";
		ACME_NAME = rename ? "Acme" : "§6Acme";
		STICKY_NAME = rename ? "Sticky" : "§6Sticky";
		HOMER_NAME = rename ? "Homer" : "§6Homer";
		PNEUMATIC_NAME = rename ? "Pneumatic" : "§6Pneumatic";
		NULL_NAME = rename ? "Null" : "§6Null";
		GROWTH_NAME = rename ? "Growth" : "§6Growth";
			
		ArrayList<ItemStack> itemList = new ArrayList<ItemStack>(); 
		
		Smokebomb = new ItemStack(Material.CLAY_BALL);
		ItemMeta im = Smokebomb.getItemMeta();
		ArrayList<String> lore = new ArrayList<String>();
		lore.add("Cloud of smoke!");
		im.setDisplayName(SMOKEBOMB_NAME);
		im.setLore(lore);
		Smokebomb.setItemMeta(im);
		itemList.add(Smokebomb);
		
		Gluebomb = new ItemStack(Material.SUGAR);
		im = Gluebomb.getItemMeta();
		lore = new ArrayList<String>();
		lore.add("Ensnare!");
		im.setDisplayName(GLUEBOMB_NAME);
		im.setLore(lore);
		Gluebomb.setItemMeta(im);
		itemList.add(Gluebomb);
		
		Pineapple = new ItemStack(Material.EMERALD);
		im = Pineapple.getItemMeta();
		lore = new ArrayList<String>();
		lore.add("Spikey!");
		im.setDisplayName(PINEAPPLE_NAME);
		im.setLore(lore);
		Pineapple.setItemMeta(im);
		itemList.add(Pineapple);
		
		Classic = new ItemStack(Material.FIREWORK_CHARGE);
		im = Pineapple.getItemMeta();
		lore = new ArrayList<String>();
		lore.clear();
		lore.add("Kaboom!");
		im.setDisplayName(CLASSIC_NAME);
		im.setLore(lore);
		Classic.setItemMeta(im);
		itemList.add(Classic);
		
		Napalm = new ItemStack(Material.MAGMA_CREAM);
		im = Napalm.getItemMeta();
		lore = new ArrayList<String>();
		lore.add("Fire!");
		im.setDisplayName(NAPALM_NAME);
		im.setLore(lore);
		Napalm.setItemMeta(im);
		itemList.add(Napalm);
		
		Teardrop = new ItemStack(Material.SLIME_BALL);
		im = Teardrop.getItemMeta();
		lore = new ArrayList<String>();
		lore.add("Sploosh!");
		im.setDisplayName(TEARDROP_NAME);
		im.setLore(lore);
		Teardrop.setItemMeta(im);
		itemList.add(Teardrop);
		
		Icicle = new ItemStack(Material.DIAMOND);
		im = Icicle.getItemMeta();
		lore = new ArrayList<String>();
		lore.add("Freeze!");
		im.setDisplayName(ICICLE_NAME);
		im.setLore(lore);
		Icicle.setItemMeta(im);
		itemList.add(Icicle);
		
		Jackhammer = new ItemStack(Material.FLINT);
		im = Jackhammer.getItemMeta();
		lore = new ArrayList<String>();
		lore.add("Hammer Time!");
		im.setDisplayName(JACKHAMMER_NAME);
		im.setLore(lore);
		Jackhammer.setItemMeta(im);
		itemList.add(Jackhammer);
			
		// V2 Grenades *************************** 
		//Pester, Zapper, Acme, Sticky, Homing, Decay, Pneumatic, Null, Growth;
		Pester = new ItemStack(Material.LEATHER);
		im = Pester.getItemMeta();
		lore = new ArrayList<String>();
		lore.add("They're in my hair!");
		im.setDisplayName(PESTER_NAME);
		im.setLore(lore);
		Pester.setItemMeta(im);
		itemList.add(Pester);
		
		Acme = new ItemStack(Material.COAL);
		im = Acme.getItemMeta();
		lore = new ArrayList<String>();
		lore.add("Slap a hole on any surface!");
		im.setDisplayName(ACME_NAME);
		im.setLore(lore);
		Acme.setItemMeta(im);
		itemList.add(Acme);
		
		Zapper = new ItemStack(Material.NETHER_STAR);
		im = Zapper.getItemMeta();
		lore = new ArrayList<String>();
		lore.add("Stay dry...");
		im.setDisplayName(ZAPPER_NAME);
		im.setLore(lore);
		Zapper.setItemMeta(im);
		itemList.add(Zapper);
		
		Sticky = new ItemStack(Material.GLOWSTONE_DUST);
		im = Sticky.getItemMeta();
		lore = new ArrayList<String>();
		lore.add("Get it off!");
		im.setDisplayName(STICKY_NAME);
		im.setLore(lore);
		Sticky.setItemMeta(im);
		itemList.add(Homer);
		
		Homer = new ItemStack(Material.SPIDER_EYE);
		im = Homer.getItemMeta();
		lore = new ArrayList<String>();
		lore.add("Seek and destroy.");
		im.setDisplayName(HOMER_NAME);
		im.setLore(lore);
		Homer.setItemMeta(im);
		
		Decay = new ItemStack(Material.FERMENTED_SPIDER_EYE);
		im = Decay.getItemMeta();
		lore = new ArrayList<String>();
		lore.add("What's that stench...");
		im.setDisplayName(DECAY_NAME);
		im.setLore(lore);
		Decay.setItemMeta(im);
		itemList.add(Decay);
		
		Pneumatic = new ItemStack(Material.STRING);
		im = Pneumatic.getItemMeta();
		lore = new ArrayList<String>();
		lore.add("Air blast!");
		im.setDisplayName(PNEUMATIC_NAME);
		im.setLore(lore);
		Pneumatic.setItemMeta(im);
		itemList.add(Pneumatic);
		
		Null = new ItemStack(Material.GHAST_TEAR);
		im = Null.getItemMeta();
		lore = new ArrayList<String>();
		lore.add("Arresto Momentum!");
		im.setDisplayName(NULL_NAME);
		im.setLore(lore);
		Null.setItemMeta(im);
		itemList.add(Null);
		
		Growth = new ItemStack(Material.GOLD_NUGGET);
		im = Growth.getItemMeta();
		lore = new ArrayList<String>();
		lore.add("Rejeuvenating!");
		im.setDisplayName(GROWTH_NAME);
		im.setLore(lore);
		Growth.setItemMeta(im);
		itemList.add(Growth);
		
		// Crafting Recipes
		grenadeRecipes = new HashMap<String,Recipe>();
		grenadeRecipes.clear();
		
		ShapedRecipe classicRecipe = new ShapedRecipe(Classic);
		grenadeRecipes.put("classic",classicRecipe);
		
		ShapedRecipe smokeRecipe = new ShapedRecipe(Smokebomb);
		grenadeRecipes.put("smokebomb",smokeRecipe);
		
		ShapedRecipe glueRecipe = new ShapedRecipe(Gluebomb);
		grenadeRecipes.put("gluebomb",glueRecipe);
		
		ShapedRecipe pineappleRecipe = new ShapedRecipe(Pineapple);
		grenadeRecipes.put("pineapple",pineappleRecipe);
		
		ShapedRecipe napalmRecipe = new ShapedRecipe(Napalm);
		grenadeRecipes.put("napalm",napalmRecipe);
		
		ShapedRecipe teardropRecipe = new ShapedRecipe(Teardrop);
		grenadeRecipes.put("teardrop",teardropRecipe);
		
		ShapedRecipe icicleRecipe = new ShapedRecipe(Icicle);
		grenadeRecipes.put("icicle",icicleRecipe);
		
		ShapedRecipe jackhammerRecipe = new ShapedRecipe(Jackhammer);
		grenadeRecipes.put("jackhammer",jackhammerRecipe);
		
		ShapedRecipe pesterRecipe = new ShapedRecipe(Pester);
		grenadeRecipes.put("pester",pesterRecipe);
		
		ShapedRecipe zapperRecipe = new ShapedRecipe(Zapper);
		grenadeRecipes.put("zapper",zapperRecipe);
		
		ShapedRecipe decayRecipe = new ShapedRecipe(Decay);
		grenadeRecipes.put("decay",decayRecipe);
		
		ShapedRecipe acmeRecipe = new ShapedRecipe(Acme);
		grenadeRecipes.put("acme",acmeRecipe);
		
		reloadRecipes();
		
		FunGrenades.playerList.clear();
	}
	 
	@Override
	public void onDisable() {
		// Stop all async tasks.
	    getServer().getScheduler().cancelTasks(this);
	}
	
	public void reloadRecipes(){
		List<String> shape;
		Map<String,Object> materials;
		Set<String> materialKeys;
		ConfigurationSection section;
		ConfigurationSection mSection;
        
		if (this.getConfig().getBoolean("allow_crafting")){
			Iterator<String> git = grenadeRecipes.keySet().iterator();
			while (git.hasNext()) {
				String g = git.next();
				ShapedRecipe r = (ShapedRecipe) grenadeRecipes.get(g);
				section = this.getConfig().getConfigurationSection(g);
				mSection = section.getConfigurationSection("materials");
				materials = mSection.getValues(false);
				shape = section.getStringList("recipe");
				r.shape(shape.toArray(new String[0]));
				
				materialKeys = materials.keySet();
				Iterator<String> it = materialKeys.iterator();
				while (it.hasNext()) {
					String key = it.next();
					String ingredient = (String) materials.get(key);
					Material m = Material.valueOf(ingredient);
					r.setIngredient(key.charAt(0), m);
				}	
				getServer().addRecipe(r);
			}
			
			System.out.println("FunGrenades configs reloaded with crafting recipes enabled.");
		}
		else {
			System.out.println("FunGrenades configs reloaded with crafting recipes disabled.");
		}
	}
	
	
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		if (cmd.getName().equalsIgnoreCase("fg.reload")) { 
			
			if (sender.hasPermission("fg.reload")){
				reloadConfig();
				
				// Add all recipes to a collection, omit FunGrenades recipes
				HashSet<Recipe> savedRecipes = new HashSet<Recipe>();
				Iterator<Recipe> recipeIterator = getServer().recipeIterator();
				while (recipeIterator.hasNext()){
					Recipe r = recipeIterator.next();
					
					Iterator<String> git = grenadeRecipes.keySet().iterator();
					boolean isFunGrenade = false;
					
					while (git.hasNext()){
						
						String name = grenadeRecipes.get(git.next()).getResult().getItemMeta().getDisplayName();
					
						if ((r.getResult().hasItemMeta()) &&
							(r.getResult().getItemMeta().hasDisplayName()) &&
						 	(r.getResult().getItemMeta().getDisplayName().equals(name)))
						{
							//System.out.println("Ommitted " + name + " from saved server recipes.");
							isFunGrenade = true;
						}
					}
					
					if (!isFunGrenade) {
						savedRecipes.add(r);
						//if (r.getResult().hasItemMeta() && r.getResult().getItemMeta().hasDisplayName())
							//System.out.println("Saved a recipe: " + r.getResult().getItemMeta().getDisplayName());
					}
				}
				
				// clear server recipes, add the remaining ones back.
				this.getServer().clearRecipes();
				Iterator<Recipe> rit = savedRecipes.iterator();
				while (rit.hasNext()){
					Recipe r = rit.next();
					this.getServer().addRecipe(r);
					//if (r.getResult().hasItemMeta() && r.getResult().getItemMeta().hasDisplayName())
						//System.out.println("Recipe Added to Server: " + r.getResult().getItemMeta().getDisplayName());
				}
				
				// the updated ones from FunGrenades should be added in reloadRecipes()
				//reloadRecipes();
				onEnable();
				
				//System.out.println("Reloaded FunGrenades configs and crafting recipes.");
				if (sender instanceof Player){
					sender.sendMessage("Reloaded FunGrenades configs and crafting recipes.");
				}
				return true;
			}
		}
		
		if (cmd.getName().equalsIgnoreCase("fg.give")) { 
			
		  if (sender.hasPermission("fg.give")){
			
			if (args.length < 2)
				return false;
			
			int amount = 1;
			
			if (args.length > 2) {
				try {
					amount = Integer.parseInt(args[2]);
				}
				catch (Exception e) {
					sender.sendMessage("Invalid amount specified (" + args[2] + "); defaulting to 1.");
				}
			}
			
			@SuppressWarnings("deprecation")
			Player target = (Bukkit.getServer().getPlayer(args[0]));
			
			// This block may fix fg.give if getPlayer(String) ever disappears.
			// I don't think it's any different though; we just give the grenade to
			// the first player with the matching name.
			/*
			Player target = null;
			Player[] players = getServer().getOnlinePlayers();
			for(int i = 0; i < players.length; i++){
				if (players[i].getDisplayName().equalsIgnoreCase(args[0])){
					target = players[i];
					i = players.length;
				}
				else {
					target = null;
				}
			}*/

			if (target == null) {
				sender.sendMessage(args[0] + " is not online!");
				return false;
		    } 
			else
			{
				ItemStack newItem;
				
				if (args[1].equalsIgnoreCase("smokebomb")){
					if (target.hasPermission("fg.smokebomb")) {
						newItem = new ItemStack(Smokebomb);
						newItem.setAmount(amount);
					} 
					else {
						sender.sendMessage("Target player does not have permission for that FunGrenade.");
						return false;
					}
				}
				
				else if (args[1].equalsIgnoreCase("gluebomb")){
					if (target.hasPermission("fg.gluebomb")) {
						newItem = new ItemStack(Gluebomb);
						newItem.setAmount(amount);
					} 
					else {
						sender.sendMessage("Target player does not have permission for that FunGrenade.");
						return false;
					}
				}
				
				else if (args[1].equalsIgnoreCase("pineapple")){
					if (target.hasPermission("fg.pineapple")) {
						newItem = new ItemStack(Pineapple);
						newItem.setAmount(amount);
					} 
					else {
						sender.sendMessage("Target player does not have permission for that FunGrenade.");
						return false;
					}
				}
				
				else if (args[1].equalsIgnoreCase("classic")){
					if (target.hasPermission("fg.classic")) {
						newItem = new ItemStack(Classic);
						newItem.setAmount(amount);
					} 
					else {
						sender.sendMessage("Target player does not have permission for that FunGrenade.");
						return false;
					}
				}
				
				else if (args[1].equalsIgnoreCase("napalm")){
					if (target.hasPermission("fg.napalm")) {
						newItem = new ItemStack(Napalm);
						newItem.setAmount(amount);
					} 
					else {
						sender.sendMessage("Target player does not have permission for that FunGrenade.");
						return false;
					}
				}
				
				else if (args[1].equalsIgnoreCase("teardrop")){
					if (target.hasPermission("fg.teardrop")) {
						newItem = new ItemStack(Teardrop);
						newItem.setAmount(amount);
					} 
					else {
						sender.sendMessage("Target player does not have permission for that FunGrenade.");
						return false;
					}
				}
				
				else if (args[1].equalsIgnoreCase("icicle")){
					if (target.hasPermission("fg.icicle")) {
						newItem = new ItemStack(Icicle);
						newItem.setAmount(amount);
					} 
					else {
						sender.sendMessage("Target player does not have permission for that FunGrenade.");
						return false;
					}
				}
				
				else if (args[1].equalsIgnoreCase("jackhammer")){
					if (target.hasPermission("fg.jackhammer")) {
						newItem = new ItemStack(Jackhammer);
						newItem.setAmount(amount);
					} 
					else {
						sender.sendMessage("Target player does not have permission for that FunGrenade.");
						return false;
					}
				}
				
				else if (args[1].equalsIgnoreCase("pester")){
					if (target.hasPermission("fg.pester")) {
						newItem = new ItemStack(Pester);
						newItem.setAmount(amount);
					} 
					else {
						sender.sendMessage("Target player does not have permission for that FunGrenade.");
						return false;
					}
				}
				
				else if (args[1].equalsIgnoreCase("zapper")){
					if (target.hasPermission("fg.zapper")) {
						newItem = new ItemStack(Zapper);
						newItem.setAmount(amount);
					} 
					else {
						sender.sendMessage("Target player does not have permission for that FunGrenade.");
						return false;
					}
				}
				
				else if (args[1].equalsIgnoreCase("decay")){
					if (target.hasPermission("fg.decay")) {
						newItem = new ItemStack(Decay);
						newItem.setAmount(amount);
					} 
					else {
						sender.sendMessage("Target player does not have permission for that FunGrenade.");
						return false;
					}
				}
				
				else if (args[1].equalsIgnoreCase("acme")){
					if (target.hasPermission("fg.acme")) {
						newItem = new ItemStack(Acme);
						newItem.setAmount(amount);
					} 
					else {
						sender.sendMessage("Target player does not have permission for that FunGrenade.");
						return false;
					}
				}
				
				else {
					sender.sendMessage("Invalid grenade type (" + args[1] + ").");
					return false;
				}
				
				String plu = "";
				if (amount != 1)
					plu = "s";
				
				target.getInventory().addItem(newItem);
				sender.sendMessage("Successfully gave " + amount + " " + args[1] + plu + " to " + target.getDisplayName());
				
				if (!sender.equals(target)){
					target.sendMessage("You picked up " + amount + " " + args[1] + plu  + ".");
				}
				return true;
				
			}
		  } 
		  else {
			 sender.sendMessage("You do not have permission to give FunGrenades."); 
			
		  }
		}
		
		return false;
	  
	}
	
	
	public ArrayList<Location> getNearbyLocations(Location loc, int r) {
		ArrayList<Location> list = new ArrayList<Location>();
		list.add(loc);
		World w = loc.getWorld();
		
		Location addLoc;
		for (int y = 1 - r; y < r; y++){
		  for (int x = 0 - r; x <= r; x++){
			for (int z = 0 - r; z <= r; z++){
				if (((z == r) || (z == 0-r)) && ((x == r) || (x == 0-r))) {
					// do not add corner
				}
				else {
					addLoc = new Location(w, loc.getX() - x, loc.getY() - y, loc.getZ() - z);
					list.add(addLoc);
				}
			}
		  }
		}
		
		ArrayList<Location> shuffled = new ArrayList<Location>();
		int n = list.size();
		
		for (int i = 0; i < n; i++) {
			int rand = (int) (Math.random() * list.size());
			shuffled.add(list.get(rand));
			list.remove(rand);
		}

		return shuffled;
	}
	
	
	class DelayTickerTask extends BukkitRunnable {
		private Player fgp;
		
		public DelayTickerTask(Player player) {
			this.fgp = player;
		}
		
		@Override
		public void run() {
			FunGrenades.playerList.remove(fgp); 
			this.cancel();
		}
		

	}

	class SmokeBombTask extends BukkitRunnable {
		private FunGrenades fungrenades;
		private int duration;
		private Item proj;
		ArrayList<Location> area;
		
		public SmokeBombTask (Item proj, int duration, FunGrenades fungrenades) {
			this.fungrenades = fungrenades;
			this.duration = duration;
			this.proj = proj;
			this.area = new ArrayList<Location>();
			
		}
		
		public SmokeBombTask (int duration, ArrayList<Location> area, FunGrenades fungrenades, Item proj) {
			this.fungrenades = fungrenades;
			this.area = area;
			this.proj = proj;
			this.duration = duration - 1;
			
			if (area.size() < 10) {
			Location loc;
			loc = area.get(0).clone();
			loc.add(2, 0, 0);
			area.add(loc);
			
			loc = area.get(0).clone();
			loc.add(1, 0, 2);
			area.add(loc);
			
			loc = area.get(0).clone();
			loc.add(2, 0, 1);
			area.add(loc);
			
			loc = area.get(0).clone();
			loc.add(1, 0, 2);
			area.add(loc);
			
			loc = area.get(0).clone();
			loc.add(2, 0, 2);
			area.add(loc);
			
			loc = area.get(0).clone();
			loc.add(-1, 0, -1);
			area.add(loc);
			
			loc = area.get(0).clone();
			loc.add(2, 0, -2);
			area.add(loc);
			
			loc = area.get(0).clone();
			loc.add(-2, 0, 2);
			area.add(loc);
			
			loc = area.get(0).clone();
			loc.add(1, 0, -2);
			area.add(loc);
			
			loc = area.get(0).clone();
			loc.add(-2, 0, 1);
			area.add(loc);
			
			loc = area.get(0).clone();
			loc.add(2, 0, -1);
			area.add(loc);
			
			loc = area.get(0).clone();
			loc.add(-1, 0, 2);
			area.add(loc);
			
			loc = area.get(0).clone();
			loc.add(1, 1, -1);
			area.add(loc);
		
			loc = area.get(0).clone();
			loc.add(-1, 1, 1);
			area.add(loc);
			}
		}

		@Override
		public void run() {
			if (this.area.size() < 1) {
				this.area.add(proj.getLocation());
			
				Location loc;
				loc = area.get(0).clone();
				loc.add(1, 0, 0);
				area.add(loc);
				
				loc = area.get(0).clone();
				loc.add(0, 0, 1);
				area.add(loc);
				
				loc = area.get(0).clone();
				loc.add(0, 0, -1);
				area.add(loc);
				
				loc = area.get(0).clone();
				loc.add(1, 0, 1);
				area.add(loc);
				
				loc = area.get(0).clone();
				loc.add(0, 1, 0);
				area.add(loc);
			
				loc = area.get(0).clone();
				loc.add(0, 0, 1);
				area.add(loc);
			
				loc = area.get(0).clone();
				loc.add(1, 0, -1);
				area.add(loc);
			
				loc = area.get(0).clone();
				loc.add(-1, 0, 1);
				area.add(loc);
				proj.getWorld().playEffect(proj.getLocation(), Effect.SMOKE, 4);
				proj.remove();
			}
			else {
				for (int i = 0; i < area.size(); i++) {
					new SmokeDelayTask(fungrenades, area.get(i)).runTaskAsynchronously(fungrenades); 
				}
			}
			if (duration > 0) {
				proj.getWorld().playSound(area.get(0), Sound.FIZZ, 1, 1);
				new SmokeBombTask(duration, area, fungrenades, proj).runTaskLater(fungrenades,20);
				
				ArrayList<Entity> players = (ArrayList<Entity>)proj.getNearbyEntities(50, 50, 50);

				ArrayList<Entity> near_entities = (ArrayList<Entity>) proj.getNearbyEntities(3, 1, 3);
	  			for (int i = 0; i < near_entities.size(); i++) {
	  			  if (near_entities.get(i) instanceof Player) {
	  				Player one_player = (Player) near_entities.get(i);
	  				PotionEffect pe = new PotionEffect(PotionEffectType.BLINDNESS,40,0);
            		one_player.addPotionEffect(pe, true);
            		//one_player.sendMessage("You are in a cloud of smoke."); 
            		for (int j = 0; j < players.size(); j++){
    					if (players.get(j) instanceof Player) {
    						Player p = (Player) players.get(j);
    						p.hidePlayer(one_player);
    					}
    				}
    				
            		new RestoreVisibilityTask(one_player, players).runTaskLater(fungrenades, 120);	
	  			  } 
	  			}
				
				this.cancel();
			}	
			else {
				this.cancel();
			}
		}
	}
	
	class SmokeTickTask extends BukkitRunnable {
		private Location loc;
		
		public SmokeTickTask (Location loc){
			this.loc = loc;
		}
		
		@Override
		public void run() {
			loc.getWorld().playEffect(loc, Effect.SMOKE, 4);	
			this.cancel();
		}
	}
	
	class SmokeDelayTask extends BukkitRunnable {
		private FunGrenades fungrenades;
		private Location loc;
		
		public SmokeDelayTask (FunGrenades fungrenades, Location loc){
			this.fungrenades = fungrenades;
			this.loc = loc;
		}
		
		@Override
		public void run() {
			new SmokeTickTask(loc).runTaskLater(fungrenades, (long)(Math.random() * 20));
			this.cancel();
		}
	}
	
	
	class GlueBombTask extends BukkitRunnable {
		private FunGrenades fungrenades;
		private int duration;
		private Item proj;
		ArrayList<Location> area;
		
		public GlueBombTask (Item proj, int duration, FunGrenades fungrenades) {
			this.fungrenades = fungrenades;
			this.duration = duration * 10;
			this.proj = proj;
		}
		

		@Override
		public void run() {
			
			area = getNearbyLocations(proj.getLocation(), 2);
			
			proj.getWorld().playEffect(proj.getLocation(), Effect.POTION_BREAK, 2);
			proj.remove();
			
			for (int i = 0; i < area.size(); i++) {
				if (i < 10)
					new GlueDelayTask(area.get(i),proj.getWorld()).runTask(fungrenades);
				else
					new GlueDelayTask(area.get(i),proj.getWorld()).runTaskLater(fungrenades,i); 
			}
			
			proj.getWorld().playSound(area.get(0), Sound.LAVA, 3, 1);
			
			for (int i = area.size(); i > 0; i--) {
				new GlueRemoveTask(proj.getWorld(), area.get(i - 1)).runTaskLater(fungrenades,(i*2)+duration);
			}
			this.cancel();
		}	
		
	}
	
	
	class GlueDelayTask extends BukkitRunnable {
		private Location loc;
		private World world;
		
		public GlueDelayTask (Location loc, World world){
			this.loc = loc;
			this.world = world;
		}
		
		@Override
		public void run() {
			if (world.getBlockAt(loc).getType().equals(Material.AIR) ||
				world.getBlockAt(loc).getType().equals(Material.SNOW) 
			){
				world.getBlockAt(loc).setType(Material.WEB);
				
			}
			this.cancel();
		}
	}
	
	class GlueRemoveTask extends BukkitRunnable {
		private World world;
		private Location loc;
		
		public GlueRemoveTask (World world, Location loc){
			this.world = world;
			this.loc = loc;
		}
		
		@Override
		public void run() {
			//getServer().broadcastMessage("Pop!");
			
			if (world.getBlockAt(loc).getType().equals(Material.WEB)){
				world.getBlockAt(loc).setType(Material.AIR);
				world.playSound(loc, Sound.LAVA_POP, 1, 1);
			}

			this.cancel();
		}
	}
	
	
	class PineappleTask extends BukkitRunnable {
		private FunGrenades fungrenades;
		private Item proj;
		private int duration;
		
		public PineappleTask (Item proj, FunGrenades fungrenades, int duration) {
			this.fungrenades = fungrenades;
			this.proj = proj;
			this.duration = duration - 1;
			
		}

		@Override
		public void run() {
			
			
			proj.getWorld().playSound(proj.getLocation(), Sound.ZOMBIE_WOODBREAK, 2, 1);
			
			new ArrowSchedulerTask(proj, fungrenades).runTaskAsynchronously(fungrenades);

			if (duration > 0){
				new PineappleTask(proj, fungrenades, duration).runTaskLater(fungrenades,12);
				this.cancel();
			}
			else {
				new OneExplosion(proj.getLocation(), fungrenades.getConfig().getInt("explosion_strength_pineapple"), fungrenades.getConfig().getBoolean("causes_fire_pineapple"), fungrenades.getConfig().getBoolean("block_destruction_pineapple")).runTask(fungrenades);
				proj.remove();
				this.cancel();
			}
		}	
	}
	
	
	class ArrowSchedulerTask extends BukkitRunnable {

		Item proj;
		FunGrenades fungrenades;
		
		public ArrowSchedulerTask(Item proj, FunGrenades fungrenades) {
			this.proj = proj;
			this.fungrenades = fungrenades;
		}
		
		@Override
		public void run() {
			Vector v;
			int do_y = 1;
			if (!proj.isOnGround()) {
				do_y = -3;
			}
				
			for (int j = do_y; j < 11; j = j + 2) {
			
			for (int k = -1; k < 2; k = k + 2){	
				
			for (int i = 0; i < 2; i++) {
				v = new Vector(i * k,j,0);
				
				new ArrowAddTask(proj.getLocation(), v, proj.getWorld(), fungrenades).runTask(fungrenades);
				//new EntityRemoveTask((Entity)a).runTaskLater(fungrenades, arrowDuration); 
			}
			
			for (int i = 0; i < 5; i = i + 2) {
				v = new Vector(0,j,i * k);
			
				new ArrowAddTask(proj.getLocation(), v, proj.getWorld(), fungrenades).runTask(fungrenades);
				//new EntityRemoveTask((Entity)a).runTaskLater(fungrenades, arrowDuration); 
			}
			
			for (int i = 0; i < 5; i= i + 2) {
				v = new Vector(i * k,j,i * k);
		
				new ArrowAddTask(proj.getLocation(), v, proj.getWorld(),fungrenades).runTask(fungrenades);
				//new EntityRemoveTask((Entity)a).runTaskLater(fungrenades, arrowDuration); 
			}
			
			for (int i = 0; i < 5; i= i + 2) {
				v = new Vector(i * k,j,(-1 * i) * k);
				
				new ArrowAddTask(proj.getLocation(), v, proj.getWorld(), fungrenades).runTask(fungrenades);
				//new EntityRemoveTask((Entity)a).runTaskLater(fungrenades, arrowDuration); 
			}
			
			} // end k
			
			} // end j 
			
			this.cancel();
		}
		
	}
	
	class ArrowAddTask extends BukkitRunnable {
		private Vector v;
		private Location l;
		private World w;
	
		private FunGrenades fungrenades;
		
		public ArrowAddTask (Location l, Vector v, World w, FunGrenades fungrenades) {
			this.v = v;
			this.l = l;
			this.w = w;
			this.fungrenades = fungrenades;
		}
		
		@Override
		public void run() {
			Arrow a = w.spawnArrow(l, v, (float) 1.0, 12);
			new EntityRemoveTask((Entity)a).runTaskLater(fungrenades, 40); // 40 is duration of arrow
			this.cancel();
		}
	}
	
	class EntityRemoveTask extends BukkitRunnable {
		private Entity e;
		
		public EntityRemoveTask (Entity e) {
			this.e = e;
		}
		
		@Override
		public void run() {
			e.remove();		
			this.cancel();
		}
	}
	
	class ClassicTask extends BukkitRunnable {
		private Item proj;
		private FunGrenades fungrenades;
		
		public ClassicTask (Item proj, FunGrenades fungrenades){
			this.proj = proj;
			this.fungrenades = fungrenades;
		}
		
		@Override
		public void run() {
			proj.getWorld().createExplosion(proj.getLocation().getX(), proj.getLocation().getY(), proj.getLocation().getZ(), fungrenades.getConfig().getInt("explosion_strength_he"), fungrenades.getConfig().getBoolean("causes_fire_he"), fungrenades.getConfig().getBoolean("block_destruction_he"));
			proj.remove();
			this.cancel();
			
		}
	}
	
	class NapalmTask extends BukkitRunnable {
		private Item proj;
		private FunGrenades fungrenades;
		
		public NapalmTask (Item proj, FunGrenades fungrenades){
			this.proj = proj;
			this.fungrenades = fungrenades;
		}
		
		@Override
		public void run() {
			Location loc = proj.getLocation();
			
			for (int i = 0; i < 46; i = i + 2){
				new OneNapalmTask(proj, fungrenades).runTaskLater(fungrenades, i);
			}
			Location below = new Location(proj.getWorld(),loc.getX(),loc.getY()-1,loc.getZ());
			ArrayList<Location> area = getNearbyLocations(proj.getLocation(),2);
			
			for (int i = 0; i < area.size(); i++) {
				if (proj.getWorld().getBlockAt(area.get(i)).getType().equals(Material.SNOW)) {
					proj.getWorld().getBlockAt(area.get(i)).setType(Material.FIRE);
				}
				else if (proj.getWorld().getBlockAt(area.get(i)).getType().equals(Material.ICE)) {
					proj.getWorld().getBlockAt(area.get(i)).setType(Material.WATER);
				}
				else if (proj.getWorld().getBlockAt(area.get(i)).getType().equals(Material.AIR) &&
						!proj.getWorld().getBlockAt(below).isEmpty()) {
					proj.getWorld().getBlockAt(area.get(i)).setType(Material.FIRE);
				}
			}
			
			proj.remove();
			this.cancel();
			
		}
	}
	
	class OneNapalmTask extends BukkitRunnable {
		private Item proj;
		private FunGrenades fungrenades;
		
		public OneNapalmTask (Item proj, FunGrenades fungrenades){
			this.proj = proj;
			this.fungrenades = fungrenades;
		}

		@Override
		public void run() {
			ItemStack is = new ItemStack(Material.BLAZE_POWDER);
			Location l = proj.getLocation().add(0, 1, 0);
			Item fireball = proj.getWorld().dropItemNaturally(l, is);
			fireball.setPickupDelay(PICKUP_DELAY);
			Vector v = fireball.getVelocity();
			v.setX(v.getX() * 4.5);
			v.setZ(v.getZ() * 4.5);
			v.setY(v.getY() * 2);
			fireball.setVelocity(v);
			new FireSwapTask(fireball).runTaskLater(fungrenades, 18);
			
			proj.getWorld().playSound(proj.getLocation(), Sound.GHAST_FIREBALL, 2, 1);
			this.cancel();
		}
	}
	
	
	class FireSwapTask extends BukkitRunnable {
		private Item proj;
		
		public FireSwapTask (Item proj){
			this.proj = proj;
		}

		@Override
		public void run() {
			Fireball sfb = proj.getWorld().spawn(proj.getLocation(), SmallFireball.class);
			sfb.setDirection(proj.getVelocity().clone().setY(-1));
			proj.remove();
			this.cancel();
		}
	}
	
	class TeardropTask extends BukkitRunnable {
		private Item proj;
		private FunGrenades fungrenades;
		
		public TeardropTask (Item proj, FunGrenades fungrenades){
			this.proj = proj;
			this.fungrenades = fungrenades;
		}

		@Override
		public void run() {
			Location loc = proj.getLocation().clone();
			Material m = proj.getWorld().getBlockAt(loc).getType();
			loc.setY(loc.getY() + 1);
			Material m2 = proj.getWorld().getBlockAt(loc).getType();
			proj.getWorld().getBlockAt(loc).setType(Material.WATER);
			proj.getWorld().getBlockAt(proj.getLocation()).setType(Material.WATER);
			new TeardropRemoveTask(loc,m, m2,proj.getWorld()).runTaskLater(fungrenades, 50);
			proj.getWorld().playEffect(proj.getLocation(), Effect.POTION_BREAK, 2);
			proj.getWorld().playSound(proj.getLocation(), Sound.SPLASH, 2, 1);
			proj.remove();
			this.cancel();
		}
	}
	
	class TeardropRemoveTask extends BukkitRunnable {
		private Location loc;
		private Material m, m2;
		private World w;
		
		public TeardropRemoveTask (Location loc, Material m, Material m2, World w){
			this.loc = loc;
			this.m = m;
			this.m2 = m2;
			this.w = w;
		}

		@Override
		public void run() {
			w.getBlockAt(loc).setType(m2);
			Location loc2 = loc.clone();
			loc2.setY(loc.getY() - 1);
			w.getBlockAt(loc2).setType(m);
			this.cancel();
		}
	}
	
	
	class IcicleTask extends BukkitRunnable {
		private Item proj;
		private FunGrenades fungrenades;
		
		public IcicleTask (Item proj, FunGrenades fungrenades){
			this.proj = proj;
			this.fungrenades = fungrenades;
		}

		@Override
		public void run() {
			ArrayList<Location> area = getNearbyLocations(proj.getLocation(), 3);
			
			for (int i = 0; i < area.size(); i++) {
				int j = (int)Math.floor(0.1 * i);
				new OneFreezeTask(area.get(i), proj.getWorld()).runTaskLater(fungrenades, j);
			}
			
			ArrayList<Entity> players = (ArrayList<Entity>)proj.getNearbyEntities(4, 2, 4);
			
			for (int i = 0; i < players.size(); i++){
				if (players.get(i) instanceof LivingEntity){
					LivingEntity le = (LivingEntity)players.get(i);
					le.setFireTicks(0);
					le.damage(2);
					if (players.get(i) instanceof Player) {
						Player p = (Player) players.get(i);
						p.setWalkSpeed((float)0.08);
						new RestoreTask(p).runTaskLater(fungrenades, 120);
					}
				}
			}
			
			proj.remove();
			this.cancel();
		}
	}
	
	class RestoreTask extends BukkitRunnable {
		private Player p;
		
		public RestoreTask (Player p){
			this.p = p;
		}

		@Override
		public void run() {
			p.setWalkSpeed((float)0.2);
			this.cancel();
		}
		
	}
	
	class RestoreVisibilityTask extends BukkitRunnable {
		private Player p;
		private ArrayList<Entity> players;
		
		public RestoreVisibilityTask (Player p, ArrayList<Entity> players){
			this.p = p;
			this.players = players;
		}

		@Override
		public void run() {
			for (int j = 0; j < players.size(); j++){
				if (players.get(j) instanceof Player) {
					Player player = (Player) players.get(j);
					player.showPlayer(p);
				}
			}
			this.cancel();
		}	
	}
	
	
	class OneFreezeTask extends BukkitRunnable {
		private Location loc;
		private World w;
		
		public OneFreezeTask (Location loc, World w){
			this.loc = loc.clone();
			this.w = w;
		}

		@Override
		public void run() {
			w.playEffect(loc, Effect.POTION_BREAK, 2);
			Location below = new Location(w,loc.getX(),loc.getY()-1,loc.getZ());
			if ((w.getBlockAt(loc).getType().equals(Material.WATER)) ||
					(w.getBlockAt(loc).getType().equals(Material.STATIONARY_WATER))) {
				w.getBlockAt(loc).setType(Material.ICE);
			}
			else if (w.getBlockAt(loc).getType().equals(Material.LAVA)) {
				w.getBlockAt(loc).setType(Material.COBBLESTONE);
			}
			else if (w.getBlockAt(loc).getType().equals(Material.STATIONARY_LAVA)) {
				w.getBlockAt(loc).setType(Material.OBSIDIAN);
			}
			else if (w.getBlockAt(loc).getType().equals(Material.FIRE)) {
				w.getBlockAt(loc).setType(Material.AIR);
			}
			else if ((!w.getBlockAt(below).isEmpty() && !w.getBlockAt(below).getType().equals(Material.SNOW)) 
				&& w.getBlockAt(loc).isEmpty())  
			{
				w.getBlockAt(loc).setType(Material.AIR);
				w.getBlockAt(loc).setType(Material.SNOW);
			}

			this.cancel();
		}
	}
	
	
	class JackhammerTask extends BukkitRunnable {
		private Item proj;
		private FunGrenades fungrenades;
		
		public JackhammerTask (Item proj, FunGrenades fungrenades){
			this.proj = proj;
			this.fungrenades = fungrenades;
		}

		@Override
		public void run() {
			Location loc = proj.getLocation();
			proj.getWorld().createExplosion(loc.getX(), loc.getY(), loc.getZ(), 3, false, true);
			
			for (int i = 0; i < 3; i++){
				new OneJackTask(loc, fungrenades).runTaskLater(fungrenades, i * 45);
				loc.setY(loc.getY() - 2);
			}
			
			proj.remove();
			this.cancel();
		}
	}
	
	
	class OneJackTask extends BukkitRunnable {
		private Location loc;
		private FunGrenades fungrenades;
		private World w;
		
		public OneJackTask (Location loc, FunGrenades fungrenades){
			this.loc = loc.clone();
			this.fungrenades = fungrenades;
			this.w = loc.getWorld();
		}

		@SuppressWarnings("deprecation")
		@Override
		public void run() {
			FallingBlock fb = w.spawnFallingBlock(loc, Material.COBBLESTONE, (byte) 0);
			
			fb.setVelocity(new Vector(0,1,0));
			new OneExplosion(fb.getLocation(), 3, fungrenades.getConfig().getBoolean("causes_fire_jack"), true).runTaskLater(fungrenades, 42);
			new EntityRemoveTask((Entity)fb).runTaskLater(fungrenades, 40);
			this.cancel();
		}
	}
	
	class OneExplosion extends BukkitRunnable {
		private Location loc;
		private boolean fire;
		private boolean blockDamage;
		private float str;
	
		public OneExplosion (Location loc, float str, boolean f, boolean bd){
			this.loc = loc;
			this.fire = f;
			this.blockDamage = bd;
			this.str = str;
		}
		
		@Override
		public void run() {
			loc.getWorld().createExplosion(loc.getX(), loc.getY(), loc.getZ(), str, fire, blockDamage);	
		}
		
	}
	
	
	class FlasherTask extends BukkitRunnable {
		private FunGrenades fungrenades;
		private Item proj;
		private int time;
		private Material m1, m2;
		
		public FlasherTask (FunGrenades fungrenades, Item proj, Material m2, int time){
			this.fungrenades = fungrenades;
			this.proj = proj;
			this.time = time;
			this.m1 = proj.getItemStack().getType();
			this.m2 = m2;
		}
		
		@Override
		public void run() {
			for (int i = 5; i < time; i = i + 5) {
				new OneFlashTask(proj, m1, m2).runTaskLater(fungrenades, (long) i);
			}
			
			this.cancel();
		}
	}
	
	class OneFlashTask extends BukkitRunnable {
		private Item proj;
		private Material m1, m2;
		
		public OneFlashTask (Item proj, Material m1, Material m2){
			this.proj = proj;
			this.m1 = m1;
			this.m2 = m2;
		}
		
		@Override
		public void run() {
			ItemStack is1 = new ItemStack(m1);
			ItemStack is2 = new ItemStack(m2);
			
			
			if (proj.getItemStack().getType().equals(is1.getType())) {
				proj.setItemStack(is2);
			}
			else
				proj.setItemStack(is1);
			
			this.cancel();
		}
	}
	
	// ***************************** pester
	class PesterTask extends BukkitRunnable {
		private FunGrenades fungrenades;
		private Item proj;
		
		public PesterTask (Item proj, FunGrenades fungrenades) {
			this.fungrenades = fungrenades;
			this.proj = proj;
		}

		@Override
		public void run() {
			proj.getWorld().playSound(proj.getLocation(), Sound.BAT_LOOP, 2, 1);
			proj.getWorld().playSound(proj.getLocation(), Sound.BAT_IDLE, 2, 1);
			
			new BatSchedulerTask(proj, fungrenades).runTaskAsynchronously(fungrenades);

			proj.remove();
			this.cancel();
			
		}	
	}
	
	
	class BatSchedulerTask extends BukkitRunnable {
		Item proj;
		FunGrenades fungrenades;
		
		public BatSchedulerTask(Item proj, FunGrenades fungrenades) {
			this.proj = proj;
			this.fungrenades = fungrenades;
		}
		
		@Override
		public void run() {
				
			for (int j = 0; j < 40; j++) {
				new BatAddTask(proj.getLocation(), proj.getWorld(), fungrenades).runTaskLater(fungrenades,j);
			} // end j 
			
			this.cancel();
		}
		
	}
	
	class BatAddTask extends BukkitRunnable {
		private Location l;
		private World w;
	
		private FunGrenades fungrenades;
		
		public BatAddTask (Location l, World w, FunGrenades fungrenades) {
			this.l = l;
			this.w = w;
			this.fungrenades = fungrenades;
		}
		
		@Override
		public void run() {
			Bat b = (Bat) w.spawnEntity(l, EntityType.BAT);
			b.setVelocity(new Vector(Math.random() * 0.5,1.2,Math.random()*0.5));
			int d = (int) (Math.random() * (BAT_DURATION / 2));
			d = d + (BAT_DURATION / 2);
			new EntityRemoveTask((Entity)b).runTaskLater(fungrenades, d); 
			this.cancel();
		}
	}

	
	//*******************Zapper
	
	class ZapperTask extends BukkitRunnable {
		private FunGrenades fungrenades;
		private Item proj;
		
		public ZapperTask (Item proj, FunGrenades fungrenades) {
			this.fungrenades = fungrenades;
			this.proj = proj;
		}

		@Override
		public void run() {
			proj.getWorld().playSound(proj.getLocation(), Sound.FIREWORK_TWINKLE, 2, 1);
			new ZapAddTask(proj.getLocation(), proj.getWorld(), fungrenades).runTask(fungrenades);
			ArrayList<Location> locs = new ArrayList<Location>();
			locs.add(proj.getLocation());
			
			Location newLoc = proj.getLocation().getBlock().getRelative(BlockFace.UP).getLocation().clone();
			new ZapPropegateTask(newLoc, proj.getWorld(), locs, ZAP_PROPEGATION, fungrenades).runTaskLater(fungrenades, 2);
				
			newLoc = proj.getLocation().getBlock().getRelative(BlockFace.DOWN).getLocation().clone();
			new ZapPropegateTask(newLoc, proj.getWorld(), locs, ZAP_PROPEGATION, fungrenades).runTaskLater(fungrenades, 2);
			
			newLoc = proj.getLocation().getBlock().getRelative(BlockFace.NORTH).getLocation().clone();
			new ZapPropegateTask(newLoc, proj.getWorld(), locs, ZAP_PROPEGATION, fungrenades).runTaskLater(fungrenades, 2);
			
			newLoc = proj.getLocation().getBlock().getRelative(BlockFace.SOUTH).getLocation().clone();
			new ZapPropegateTask(newLoc, proj.getWorld(), locs, ZAP_PROPEGATION, fungrenades).runTaskLater(fungrenades, 2);
			
			newLoc = proj.getLocation().getBlock().getRelative(BlockFace.WEST).getLocation().clone();
			new ZapPropegateTask(newLoc, proj.getWorld(), locs, ZAP_PROPEGATION, fungrenades).runTaskLater(fungrenades, 2);
			
			newLoc = proj.getLocation().getBlock().getRelative(BlockFace.EAST).getLocation().clone();
			new ZapPropegateTask(newLoc, proj.getWorld(), locs, ZAP_PROPEGATION, fungrenades).runTaskLater(fungrenades, 2);
			
			newLoc = proj.getLocation().getBlock().getRelative(BlockFace.NORTH_EAST).getLocation().clone();
			new ZapPropegateTask(newLoc, proj.getWorld(), locs, ZAP_PROPEGATION, fungrenades).runTaskLater(fungrenades, 2);
			
			newLoc = proj.getLocation().getBlock().getRelative(BlockFace.NORTH_WEST).getLocation().clone();
			new ZapPropegateTask(newLoc, proj.getWorld(), locs, ZAP_PROPEGATION, fungrenades).runTaskLater(fungrenades, 2);
			
			newLoc = proj.getLocation().getBlock().getRelative(BlockFace.SOUTH_EAST).getLocation().clone();
			new ZapPropegateTask(newLoc, proj.getWorld(), locs, ZAP_PROPEGATION, fungrenades).runTaskLater(fungrenades, 2);
			
			newLoc = proj.getLocation().getBlock().getRelative(BlockFace.SOUTH_WEST).getLocation().clone();
			new ZapPropegateTask(newLoc, proj.getWorld(), locs, ZAP_PROPEGATION, fungrenades).runTaskLater(fungrenades, 2);
			
			proj.remove();
			this.cancel();
			
		}	
	}
	
	class ZapPropegateTask extends BukkitRunnable {
		private Location l;
		private World w;
		private int count;
		ArrayList<Location> locs;
	
		private FunGrenades fungrenades;
		
		public ZapPropegateTask (Location l, World w, ArrayList<Location> locs, int count, FunGrenades fungrenades) {
			this.l = l;
			this.locs = locs;
			this.w = w;
			this.count = count - 1;
			this.fungrenades = fungrenades;
		}
		
		@Override
		public void run() {
			if (count <= 0)
				this.cancel();
			else {
				int delay = 1;
				new ZapAddTask(l, w, fungrenades).runTask(fungrenades);
				Block b = l.getBlock();
				
				Location newLoc = b.getRelative(BlockFace.DOWN).getLocation();
				if ((newLoc.getBlock().getType().equals(Material.WATER) || newLoc.getBlock().getType().equals(Material.STATIONARY_WATER)) && 
						newLoc.getBlock().getRelative(BlockFace.UP).getType().equals(Material.AIR)){
					if (!locs.contains(newLoc)) {
						locs.add(newLoc);
						new ZapPropegateTask(newLoc, w, locs, count - 1, fungrenades).runTaskLater(fungrenades, delay);
					}
				}
				
				newLoc = b.getRelative(BlockFace.UP).getLocation();
				if (newLoc.getBlock().getType().equals(Material.WATER) || newLoc.getBlock().getType().equals(Material.STATIONARY_WATER)){
					if (!locs.contains(newLoc)) {
						locs.add(newLoc);
						new ZapPropegateTask(newLoc, w, locs, count - 1, fungrenades).runTaskLater(fungrenades, delay);
					}
				}
				
				newLoc = b.getRelative(BlockFace.SOUTH).getLocation();
				if ((newLoc.getBlock().getType().equals(Material.WATER) || newLoc.getBlock().getType().equals(Material.STATIONARY_WATER)) && 
						newLoc.getBlock().getRelative(BlockFace.UP).getType().equals(Material.AIR)){
					if (!locs.contains(newLoc)) {
						locs.add(newLoc);
						new ZapPropegateTask(newLoc, w, locs, count - 1, fungrenades).runTaskLater(fungrenades, delay);
					}
				}
				
				newLoc = b.getRelative(BlockFace.NORTH).getLocation();
				if ((newLoc.getBlock().getType().equals(Material.WATER) || newLoc.getBlock().getType().equals(Material.STATIONARY_WATER)) && 
						newLoc.getBlock().getRelative(BlockFace.UP).getType().equals(Material.AIR)){
					if (!locs.contains(newLoc)) {
						locs.add(newLoc);
						new ZapPropegateTask(newLoc, w, locs, count - 1, fungrenades).runTaskLater(fungrenades, delay);
					}
				}
				
				newLoc = b.getRelative(BlockFace.EAST).getLocation();
				if ((newLoc.getBlock().getType().equals(Material.WATER) || newLoc.getBlock().getType().equals(Material.STATIONARY_WATER)) && 
						newLoc.getBlock().getRelative(BlockFace.UP).getType().equals(Material.AIR)){
					if (!locs.contains(newLoc)) {
						locs.add(newLoc);
						new ZapPropegateTask(newLoc, w, locs, count - 1, fungrenades).runTaskLater(fungrenades, delay);
					}
				}
				
				newLoc = b.getRelative(BlockFace.WEST).getLocation();
				if ((newLoc.getBlock().getType().equals(Material.WATER) || newLoc.getBlock().getType().equals(Material.STATIONARY_WATER)) && 
						newLoc.getBlock().getRelative(BlockFace.UP).getType().equals(Material.AIR)){
					if (!locs.contains(newLoc)) {
						locs.add(newLoc);
						new ZapPropegateTask(newLoc, w, locs, count - 1, fungrenades).runTaskLater(fungrenades, delay);
					}
				}
			}

			this.cancel();
		}
	}
	
	class ZapAddTask extends BukkitRunnable {
		private Location l;
		private World w;
	
		private FunGrenades fungrenades;
		
		public ZapAddTask (Location l, World w, FunGrenades fungrenades) {
			this.l = l;
			this.w = w;
			this.fungrenades = fungrenades;
		}

		@Override
		public void run() {
			ItemStack zap = new ItemStack(Material.NETHER_STAR);
			zap.setAmount(1);
			Item zapItem = w.dropItemNaturally(l, zap);
			zapItem.setPickupDelay(Integer.MAX_VALUE);
			new EntityRemoveTask((Entity)zapItem).runTaskLater(fungrenades, 2); 
			w.playSound(l, Sound.FIREWORK_TWINKLE, 2, 1);
			
			
			ArrayList<Entity> entities = (ArrayList<Entity>)zapItem.getNearbyEntities(1, 1, 1);
			
			for (int i = 0; i < entities.size(); i++){
				if (entities.get(i) instanceof LivingEntity){
					LivingEntity le = (LivingEntity)entities.get(i);
					le.damage(ZAP_DAMAGE);
					if (entities.get(i) instanceof Player) {
						Player p = (Player) entities.get(i);
						p.addPotionEffect(new PotionEffect(PotionEffectType.CONFUSION,20,1));
					}
				}
			}
			
			this.cancel();
		}
	}
	
	
	class OneDecayTask extends BukkitRunnable {
		private Location l;
		private World w;
		private FunGrenades fungrenades;
		private Block b;
		private int timer;
		
		public OneDecayTask (Location l, World w, int timer, FunGrenades fungrenades) {
			this.l = l;
			this.w = w;
			this.timer = timer;
			this.fungrenades = fungrenades;
			this.b = l.getBlock();
		}
		
		@Override
		public void run() {
			int roll = (int) (Math.random() * 100);
			
			
			if (roll > 13){
				if (b.getType().equals(Material.DIRT) || b.getType().equals(Material.GRASS)) {
					b.setType(Material.MYCEL);
				}
				else if (b.getType().equals(Material.STONE)) {
					b.setType(Material.COBBLESTONE);
				}
				else if (b.getType().equals(Material.COBBLESTONE)) {
					b.setType(Material.GRAVEL);
				}
			}
			else if (roll > 5) {
				if (b.getType().equals(Material.AIR))
					w.playEffect(l, Effect.SMOKE, 1);
				else
					w.playEffect(l, Effect.ENDER_SIGNAL, 1);
			}
			else if (roll > 4) {
				ItemStack skull = new ItemStack(Material.BONE);
				skull.setAmount(1);
				Item skullItem = w.dropItemNaturally(l, skull);
				skullItem.setPickupDelay(Integer.MAX_VALUE);
				new EntityRemoveTask((Entity)skullItem).runTaskLater(fungrenades, 10); 
				
				ArrayList<Entity> entities = (ArrayList<Entity>)skullItem.getNearbyEntities(2, 2, 2);
				
				for (int i = 0; i < entities.size(); i++){
					if (entities.get(i) instanceof LivingEntity){
						LivingEntity le = (LivingEntity)entities.get(i);
						le.damage(DECAY_DAMAGE);
					}
				}
			}
			else if (roll > 3) {
				ItemStack skull = new ItemStack(Material.SKULL_ITEM);
				skull.setAmount(1);
				Item skullItem = w.dropItemNaturally(l, skull);
				skullItem.setPickupDelay(Integer.MAX_VALUE);
				new EntityRemoveTask((Entity)skullItem).runTaskLater(fungrenades, 10); 
				
				ArrayList<Entity> entities = (ArrayList<Entity>)skullItem.getNearbyEntities(2, 2, 2);
				
				for (int i = 0; i < entities.size(); i++){
					if (entities.get(i) instanceof LivingEntity){
						LivingEntity le = (LivingEntity)entities.get(i);
						le.damage(DECAY_DAMAGE);
					}
				}
			}
			else if (roll > 2) {
				w.playSound(l, Sound.ZOMBIE_IDLE, 1, 1);
			}
			else {
			  if(b.getRelative(BlockFace.UP).getType().equals(Material.AIR)){
				  if (b.getType().equals(Material.STONE) || b.getType().equals(Material.MYCEL) || b.getType().equals(Material.COBBLESTONE))
					  b.getRelative(BlockFace.UP).setType(Material.BROWN_MUSHROOM);
			  }
			}
			
			if (b.getType().equals(Material.SAND))
				b.setType(Material.GRAVEL);
			else if (b.getType().equals(Material.LOG) || b.getType().equals(Material.LOG_2))
				b.setType(Material.SOUL_SAND);
			else if (b.getType().equals(Material.LEAVES) || b.getType().equals(Material.LEAVES_2)){
				b.setType(Material.AIR);
				ItemStack leaf = new ItemStack(Material.DEAD_BUSH);
				leaf.setAmount(1);
				Item leafItem = w.dropItemNaturally(l, leaf);
				leafItem.setPickupDelay(Integer.MAX_VALUE);
				new EntityRemoveTask((Entity)leafItem).runTaskLater(fungrenades, 20); 
			}
				
			
			int delay = (int) (Math.random() * 50); 
			timer = timer - 1;
			
			if (timer > 0)
				new OneDecayTask(l, w, timer, fungrenades).runTaskLater(fungrenades, delay);
			
			this.cancel();
		}
	}
	
	class DecayTask extends BukkitRunnable {
		private FunGrenades fungrenades;
		private int duration;
		private Item proj;
		ArrayList<Location> area;
		
		public DecayTask (Item proj, int duration, FunGrenades fungrenades) {
			this.fungrenades = fungrenades;
			this.duration = duration;
			this.proj = proj;
		}
		

		@Override
		public void run() {
			
			area = getNearbyLocations(proj.getLocation(), 3);

			proj.getWorld().playEffect(proj.getLocation(), Effect.POTION_BREAK, 2);
			proj.remove();
			
			for (int i = 0; i < area.size(); i++) {
				if (i < 10)
					new OneDecayTask(area.get(i), proj.getWorld(), duration, fungrenades).runTask(fungrenades);
				else
					new OneDecayTask(area.get(i), proj.getWorld(), duration, fungrenades).runTaskLater(fungrenades, i);
			}
			
			proj.getWorld().playSound(area.get(0), Sound.BURP, 3, 1);
			
			this.cancel();
		}	
		
	}
	
	
	class AcmeTask extends BukkitRunnable {
		private FunGrenades fungrenades;
		private Location location;
		
		
		public AcmeTask (Location location, FunGrenades fungrenades) {
			this.fungrenades = fungrenades;
			this.location = location;
			
		}
		
		@Override
		public void run() {
			HashMap<Block, Material> blocks = new HashMap<Block, Material>();
			location.setYaw(location.getYaw() * -1);
			location.setPitch(location.getPitch() * -1);
			Vector dir = location.getDirection().normalize();
			
			BlockIterator bi = new BlockIterator(location.getWorld(), location.toVector(), dir, 0, fungrenades.getConfig().getInt("acme_hole_depth"));
		    Block b = null;
		    boolean done = false;
		    
		    // Eat up the first few blocks on the path.
		    bi.next();
		    bi.next();

		    while(bi.hasNext() && !done){
		        b = bi.next();
		        if (b.getType().equals(Material.AIR))
		        	done = true;
		        
		        Iterator<Location> li = fungrenades.getNearbyLocations(b.getLocation(),ACME_HOLE_RADIUS).iterator();
		        while (li.hasNext()){
		        	Block b1 = li.next().getBlock();
		        	if (!b1.getType().equals(Material.AIR)){
		        		blocks.put(b1, b1.getType());
		        		b1.setType(Material.AIR);
		        	}
		        }
		        
		    }
		    
		    new RestoreBlocksTask(blocks).runTaskLater(fungrenades, fungrenades.getConfig().getInt("duration_seconds_acme") * 20);
			
			location.getWorld().playSound(location, Sound.DIG_GRAVEL, 3, 1);
			this.cancel();
		}	
		
	}
	
	class RestoreBlocksTask extends BukkitRunnable {
		HashMap<Block, Material> blocks;
		
		public RestoreBlocksTask (HashMap<Block, Material> blocks) {
			this.blocks = blocks;
			
		}
		
		@Override
		public void run() {
			Iterator<Block> it = blocks.keySet().iterator();
			boolean first = true;
			
			while (it.hasNext()){
				Block b = it.next();
				b.setType(blocks.get(b));
				if (first) {
					b.getWorld().playSound(b.getLocation(), Sound.DIG_WOOD, 3, 1);
					first = false;
				}
			}
			
			this.cancel();
		}	
		
	}
	
	
	
	class FunGrenadesListener implements Listener {
		 
		FunGrenades fungrenades;
		
	    public FunGrenadesListener (FunGrenades fungrenades) {
	    	this.fungrenades = fungrenades;
	        fungrenades.getServer().getPluginManager().registerEvents(this, fungrenades);
	    }
	    
	    @EventHandler
	    public void craftBlocker (CraftItemEvent evt) {
	    	Inventory inv = evt.getInventory();
	    	ItemStack[] items = inv.getContents();
	    	Player player = (Player)evt.getWhoClicked();
	    	ItemStack result = evt.getRecipe().getResult();
	    	
	    	// Moved 'allow_crafting' check to crafting recipe reload method.
	    	/*if (!fungrenades.getConfig().getBoolean("allow_crafting")){
	    		Iterator<String> rip = fungrenades.grenadeRecipes.keySet().iterator();
	    		while (rip.hasNext()){
	    			String key = rip.next();
	    			if (fungrenades.grenadeRecipes.get(key).getResult().equals(evt.getRecipe().getResult())){
	    				player.sendMessage("Crafting of FunGrenades is disabled.");
	    				evt.setCancelled(true);
	    				return;
	    			}
	    			else {
	    				System.out.println("Recipe: " + evt.getRecipe().getResult().toString());
	    			}
	    		}
	    	}*/
	    	
	    	// Check permission for fungrenade crafting
	    	if (result.hasItemMeta() && result.getItemMeta().hasDisplayName()){
	    		String dname = result.getItemMeta().getDisplayName();
	    		
	    		if (dname.equalsIgnoreCase(SMOKEBOMB_NAME)) {
	    			if (!player.hasPermission("fg.smokebomb")){
	    				player.sendMessage("You don't have permission to craft or use that FunGrenade.");
	    				evt.setCancelled(true);
	    			}
	    		}
	    		else if (dname.equalsIgnoreCase(GLUEBOMB_NAME)) {
	    			if (!player.hasPermission("fg.gluebomb")){
	    				player.sendMessage("You don't have permission to craft or use that FunGrenade.");
	    				evt.setCancelled(true);
	    			}
	    		}
	    		else if (dname.equalsIgnoreCase(PINEAPPLE_NAME)) {
	    			if (!player.hasPermission("fg.pineapple")){
	    				player.sendMessage("You don't have permission to craft or use that FunGrenade.");
	    				evt.setCancelled(true);
	    			}
	    		}
	    		else if (dname.equalsIgnoreCase(CLASSIC_NAME)) {
	    			if (!player.hasPermission("fg.classic")){
	    				player.sendMessage("You don't have permission to craft or use that FunGrenade.");
	    				evt.setCancelled(true);
	    			}
	    		}
	
	    		else if (dname.equalsIgnoreCase(NAPALM_NAME)) {
	    			if (!player.hasPermission("fg.napalm")){
	    				player.sendMessage("You don't have permission to craft or use that FunGrenade.");
	    				evt.setCancelled(true);
	    			}
	    		}
	    		else if (dname.equalsIgnoreCase(TEARDROP_NAME)) {
	    			if (!player.hasPermission("fg.teardrop")){
	    				player.sendMessage("You don't have permission to craft or use that FunGrenade.");
	    				evt.setCancelled(true);
	    			}
	    		}
	    		else if (dname.equalsIgnoreCase(ICICLE_NAME)) {
	    			if (!player.hasPermission("fg.icicle")){
	    				player.sendMessage("You don't have permission to craft or use that FunGrenade.");
	    				evt.setCancelled(true);
	    			}
	    		}
	    		else if (dname.equalsIgnoreCase(JACKHAMMER_NAME)) {
	    			if (!player.hasPermission("fg.jackhammer")){
	    				player.sendMessage("You don't have permission to craft or use that FunGrenade.");
	    				evt.setCancelled(true);
	    			}
	    		}
	    		else if (dname.equalsIgnoreCase(PESTER_NAME)) {
	    			if (!player.hasPermission("fg.pester")){
	    				player.sendMessage("You don't have permission to craft or use that FunGrenade.");
	    				evt.setCancelled(true);
	    			}
	    		}
	    		else if (dname.equalsIgnoreCase(ZAPPER_NAME)) {
	    			if (!player.hasPermission("fg.zapper")){
	    				player.sendMessage("You don't have permission to craft or use that FunGrenade.");
	    				evt.setCancelled(true);
	    			}
	    		}
	    		else if (dname.equalsIgnoreCase(ACME_NAME)) {
	    			if (!player.hasPermission("fg.acme")){
	    				player.sendMessage("You don't have permission to craft or use that FunGrenade.");
	    				evt.setCancelled(true);
	    			}
	    		}
	    		else if (dname.equalsIgnoreCase(STICKY_NAME)) {
	    			if (!player.hasPermission("fg.sticky")){
	    				player.sendMessage("You don't have permission to craft or use that FunGrenade.");
	    				evt.setCancelled(true);
	    			}
	    		}
	    		else if (dname.equalsIgnoreCase(HOMER_NAME)) {
	    			if (!player.hasPermission("fg.homer")){
	    				player.sendMessage("You don't have permission to craft or use that FunGrenade.");
	    				evt.setCancelled(true);
	    			}
	    		}
	    		else if (dname.equalsIgnoreCase(DECAY_NAME)) {
	    			if (!player.hasPermission("fg.decay")){
	    				player.sendMessage("You don't have permission to craft or use that FunGrenade.");
	    				evt.setCancelled(true);
	    			}
	    		}
	    		else if (dname.equalsIgnoreCase(PNEUMATIC_NAME)) {
	    			if (!player.hasPermission("fg.pneumatic")){
	    				player.sendMessage("You don't have permission to craft or use that FunGrenade.");
	    				evt.setCancelled(true);
	    			}
	    		}
	    		else if (dname.equalsIgnoreCase(NULL_NAME)) {
	    			if (!player.hasPermission("fg.null")){
	    				player.sendMessage("You don't have permission to craft or use that FunGrenade.");
	    				evt.setCancelled(true);
	    			}
	    		}
	    		else if (dname.equalsIgnoreCase(GROWTH_NAME)) {
	    			if (!player.hasPermission("fg.growth")){
	    				player.sendMessage("You don't have permission to craft or use that FunGrenade.");
	    				evt.setCancelled(true);
	    			}
	    		}
	    		
	    		
	    	}
	    	
	    	
	    	// Block fungrenades from being used as materials
	    	for (int i = 0; i < items.length; i++){
	    		if (items[i].hasItemMeta() &&
	    			!items[i].equals(evt.getInventory().getResult())){
	    			String dname = items[i].getItemMeta().getDisplayName();
	    			if (	dname.equalsIgnoreCase(SMOKEBOMB_NAME) || 
	    					dname.equalsIgnoreCase(GLUEBOMB_NAME) || 
	    					dname.equalsIgnoreCase(PINEAPPLE_NAME) || 
	    					dname.equalsIgnoreCase(CLASSIC_NAME) || 
	    					dname.equalsIgnoreCase(NAPALM_NAME) || 
	    					dname.equalsIgnoreCase(TEARDROP_NAME) || 
	    					dname.equalsIgnoreCase(ICICLE_NAME) || 
	    					dname.equalsIgnoreCase(JACKHAMMER_NAME) || 
	    					dname.equalsIgnoreCase(PESTER_NAME) || 
	    					dname.equalsIgnoreCase(ZAPPER_NAME) || 
	    					dname.equalsIgnoreCase(ACME_NAME) || 
	    					dname.equalsIgnoreCase(STICKY_NAME) || 
	    					dname.equalsIgnoreCase(HOMER_NAME) || 
	    					dname.equalsIgnoreCase(DECAY_NAME) || 
	    					dname.equalsIgnoreCase(PNEUMATIC_NAME) || 
	    					dname.equalsIgnoreCase(NULL_NAME) || 
	    					dname.equalsIgnoreCase(GROWTH_NAME)
	    			){
	    				player.sendMessage("You cannot craft with FunGrenades as materials.");
	    				evt.setCancelled(true);
	    				return;
	    			}
	    		}
	    	}
	    	
	    }
	    	    
	    @EventHandler
	    public void teleportListener(PlayerTeleportEvent evt) {
	    	if (evt.getCause() == TeleportCause.ENDER_PEARL){
	    		Iterator<UUID> it = impactPlayers.keySet().iterator();
	    		while (it.hasNext()){
	    			UUID uuid = it.next();
	    			if (impactPlayers.get(uuid).equals(evt.getPlayer())){
	    				evt.setCancelled(true);
	    			}
	    		}
	    		
	    		
	    	}
	    }
	    
	    @EventHandler
	    public void projectileGrenadeListener(ProjectileHitEvent evt) {
	    	UUID uuid = evt.getEntity().getUniqueId();
	    	
	    	if (impactProjectiles.containsKey(uuid)){
	    		Location l = evt.getEntity().getLocation();
	    		
	    		// Update the Task's location to the projectile's impact location.
	    		impactLocations.get(uuid).setY(l.getY());
	    		impactLocations.get(uuid).setX(l.getX());
	    		impactLocations.get(uuid).setZ(l.getZ());
	    		impactLocations.get(uuid).setPitch(l.getPitch());
	    		impactLocations.get(uuid).setYaw(l.getYaw());
	    		impactLocations.get(uuid).setDirection(l.getDirection());
	    		
	    		// Run the task.
	    		impactProjectiles.get(uuid).runTask(fungrenades);
	    		impactProjectiles.remove(uuid);
	    		impactLocations.remove(uuid);
	    		impactPlayers.remove(uuid);
	    	}
	    }

	    private Item throwGrenade(Player player, ItemStack proj, Material offMaterial, int delay){
	    	Vector v = player.getEyeLocation().getDirection().multiply(GRENADE_VECTOR_FACTOR);
    		ItemStack newProj = new ItemStack (proj);
	    	newProj.setAmount(1);
    		
    		Item flyProj = player.getWorld().dropItem(player.getLocation().add(new Vector(0, GRENADE_VECTOR_FACTOR, 0)), newProj);
    		flyProj.setVelocity(v);
    		flyProj.setPickupDelay(PICKUP_DELAY);
    		FunGrenades.playerList.add(player);
    		if (proj.getAmount() > 1)
    			proj.setAmount(proj.getAmount() - 1);
    		else
    			player.getInventory().setItemInHand(null);
    		
    		new FlasherTask(fungrenades,flyProj,offMaterial,delay).runTaskAsynchronously(fungrenades);
    		new DelayTickerTask(player).runTaskLater(fungrenades,(THROW_DELAY));
    		player.getWorld().playSound(player.getLocation(), Sound.IRONGOLEM_THROW, 3, 1);
    		
    		return flyProj;
	    }
	    
	    private void detonateGrenade(BukkitRunnable GrenadeTask, int delay){	
    		GrenadeTask.runTaskLater(fungrenades, delay);
	    }
	    
	    private void throwImpactGrenade(Player player, ItemStack proj, Class<? extends Projectile> projType, BukkitRunnable impactTask, Location l){
	    	// Spawn a projectile, store its UUID and resulting task in impactProjectiles map
	    	Vector v = player.getEyeLocation().getDirection().multiply(GRENADE_VECTOR_FACTOR);
    		
	    	Projectile projectile = player.launchProjectile(projType, v);
	    	UUID uuid = projectile.getUniqueId();
	    	impactProjectiles.put(uuid, impactTask);
	    	impactLocations.put(uuid, l);
	    	impactPlayers.put(uuid, player);

    		FunGrenades.playerList.add(player);
    		
    		
    		if (proj.getAmount() > 1)
    			proj.setAmount(proj.getAmount() - 1);
    		else
    			player.getInventory().setItemInHand(null);
	    	
    		new DelayTickerTask(player).runTaskLater(fungrenades,(THROW_DELAY));
    		player.getWorld().playSound(player.getLocation(), Sound.IRONGOLEM_THROW, 3, 1);
	    	
	    }
	    
	    
	    // Interact Event
	    @EventHandler
	    public void onInteract(PlayerInteractEvent evt){
	    //public void onPlayerArmSwing(PlayerAnimationEvent evt) {
	    	Player player = evt.getPlayer();
	    	ItemStack proj = player.getItemInHand();
	    	Material material = Material.AIR;
	    	
	    	// We only care about right-clicks.
	    	if (!(evt.getAction().equals(Action.RIGHT_CLICK_BLOCK) || evt.getAction().equals(Action.RIGHT_CLICK_AIR))){
	    		return;
	    	}
	    	
	    	if (evt.getClickedBlock() != null) {
	    		material = evt.getClickedBlock().getType();
	    	}
	    	
	    	// If the material is safe, get out of here.
	    	// We aren't canceling the event, so we shouldn't have to do this for each grenade.
	    	if (fungrenades.safeMaterials.contains(material)){
	    		return;
	    	}
	    	
	    	if (proj.hasItemMeta() && proj.getItemMeta().hasDisplayName() && !(proj.getItemMeta() instanceof LeatherArmorMeta)){
	    	  if (proj.getItemMeta().getDisplayName().equals(SMOKEBOMB_NAME) && (!FunGrenades.playerList.contains(player)) && player.hasPermission("fg.smokebomb")) {
	    		 Item flyProj = throwGrenade(player,proj,Material.FIREWORK_CHARGE,fungrenades.getConfig().getInt("detonation_time_smoke"));
	    		 detonateGrenade(new SmokeBombTask(flyProj, fungrenades.getConfig().getInt("duration_seconds_smoke"), fungrenades), fungrenades.getConfig().getInt("detonation_time_smoke"));
	    	  }
	    	  
	    	  else if (proj.getItemMeta().getDisplayName().equals(GLUEBOMB_NAME) && (!FunGrenades.playerList.contains(player)) && player.hasPermission("fg.gluebomb")) {
	    		  Item flyProj = throwGrenade(player,proj,Material.SNOW_BALL,fungrenades.getConfig().getInt("detonation_time_glue"));
	    		  detonateGrenade(new GlueBombTask(flyProj, fungrenades.getConfig().getInt("duration_seconds_glue"), fungrenades), fungrenades.getConfig().getInt("detonation_time_glue"));	    		 	
	    	  }
	    	
	    	  else if (proj.getItemMeta().getDisplayName().equals(PINEAPPLE_NAME) && (!FunGrenades.playerList.contains(player)) && player.hasPermission("fg.pineapple")) {
	    		  Item flyProj = throwGrenade(player,proj,Material.CACTUS,fungrenades.getConfig().getInt("detonation_time_pineapple"));
	    		  detonateGrenade(new PineappleTask(flyProj, fungrenades, fungrenades.getConfig().getInt("duration_seconds_pineapple")), fungrenades.getConfig().getInt("detonation_time_pineapple"));
	    	  }
	    	  
	    	  else if (proj.getItemMeta().getDisplayName().equals(CLASSIC_NAME) && (!FunGrenades.playerList.contains(player)) && player.hasPermission("fg.classic")) {
	    		  Item flyProj = throwGrenade(player,proj,Material.NETHERRACK,fungrenades.getConfig().getInt("detonation_time_he"));
	    		  detonateGrenade(new ClassicTask(flyProj, fungrenades), fungrenades.getConfig().getInt("detonation_time_he"));
		      }
	    	  
	    	  else if (proj.getItemMeta().getDisplayName().equals(NAPALM_NAME) && (!FunGrenades.playerList.contains(player)) && player.hasPermission("fg.napalm")) {
	    		  Item flyProj = throwGrenade(player,proj,Material.BLAZE_POWDER,fungrenades.getConfig().getInt("detonation_time_napalm"));
	    		  detonateGrenade(new NapalmTask(flyProj, fungrenades), fungrenades.getConfig().getInt("detonation_time_napalm"));
		      }
  
	    	  else if (proj.getItemMeta().getDisplayName().equals(TEARDROP_NAME) && (!FunGrenades.playerList.contains(player)) && player.hasPermission("fg.teardrop")) {
	    		  Item flyProj = throwGrenade(player,proj,Material.LAPIS_ORE,fungrenades.getConfig().getInt("detonation_time_teardrop"));
	    		  detonateGrenade(new TeardropTask(flyProj, fungrenades), fungrenades.getConfig().getInt("detonation_time_teardrop"));
		      }
	    	  
	    	  else if (proj.getItemMeta().getDisplayName().equals(ICICLE_NAME) && (!FunGrenades.playerList.contains(player)) && player.hasPermission("fg.icicle")) {
	    		  Item flyProj = throwGrenade(player,proj,Material.SNOW_BALL,fungrenades.getConfig().getInt("detonation_time_icicle"));
	    		  detonateGrenade(new IcicleTask(flyProj, fungrenades), fungrenades.getConfig().getInt("detonation_time_icicle"));
		      }
	    	  
	    	  else if (proj.getItemMeta().getDisplayName().equals(JACKHAMMER_NAME) && (!FunGrenades.playerList.contains(player)) && player.hasPermission("fg.jackhammer")) {
	    		  Item flyProj = throwGrenade(player,proj,Material.COAL,fungrenades.getConfig().getInt("detonation_time_jack"));
	    		  detonateGrenade(new JackhammerTask(flyProj, fungrenades), fungrenades.getConfig().getInt("detonation_time_jack"));
		      }
	    	  
	    	  else if (proj.getItemMeta().getDisplayName().equals(PESTER_NAME) && (!FunGrenades.playerList.contains(player)) && player.hasPermission("fg.pester")) {
	    		  Item flyProj = throwGrenade(player,proj,Material.COAL,fungrenades.getConfig().getInt("detonation_time_pester"));
	    		  detonateGrenade(new PesterTask(flyProj, fungrenades), fungrenades.getConfig().getInt("detonation_time_pester"));
		      }
	    	  
	    	  else if (proj.getItemMeta().getDisplayName().equals(ZAPPER_NAME) && (!FunGrenades.playerList.contains(player)) && player.hasPermission("fg.zapper")) {
	    		  Item flyProj = throwGrenade(player,proj,Material.GOLD_NUGGET,fungrenades.getConfig().getInt("detonation_time_zapper"));
	    		  detonateGrenade(new ZapperTask(flyProj, fungrenades), fungrenades.getConfig().getInt("detonation_time_zapper"));
		      }
	    	  
	    	  else if (proj.getItemMeta().getDisplayName().equals(DECAY_NAME) && (!FunGrenades.playerList.contains(player)) && player.hasPermission("fg.zapper")) {
	    		  Item flyProj = throwGrenade(player,proj,Material.BROWN_MUSHROOM,fungrenades.getConfig().getInt("detonation_time_decay"));
	    		  detonateGrenade(new DecayTask(flyProj, fungrenades.getConfig().getInt("duration_seconds_decay"), fungrenades), fungrenades.getConfig().getInt("detonation_time_decay"));
		      }
	    	  else if (proj.getItemMeta().getDisplayName().equals(ACME_NAME) && (!FunGrenades.playerList.contains(player)) && player.hasPermission("fg.acme")) {
	    		  Location futureLocation = player.getLocation().clone();
	    		  throwImpactGrenade(player, proj, EnderPearl.class, new AcmeTask(futureLocation, fungrenades), futureLocation);
		      }
	    	  
	    	} // end item meta check
	    	
	    }
	    
	    // Block place event- for stopping placement of bombs that are modeled after placeable blocks...
	    // For now I've avoided using materials that can be placed.
	    /*
	    
	    @EventHandler
	    public void stopBombPlacement(BlockPlaceEvent evt) {
	    	Player player = evt.getPlayer();
	    	
	    	if (player.getItemInHand().hasItemMeta()) {
	    		if (player.getItemInHand().getItemMeta().getDisplayName().equalsIgnoreCase(SMOKEBOMB_NAME)) {
	    			evt.setCancelled(true);
	    		}
	    	}
	    }
	    */
	    
	    
	} // End Listener

	
	
} // End FunGrenades








    
    