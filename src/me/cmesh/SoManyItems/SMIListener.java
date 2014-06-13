package me.cmesh.SoManyItems;

import java.util.*;
import java.util.Map.Entry;

import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.CraftingInventory;
import org.bukkit.inventory.FurnaceRecipe;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;

public class SMIListener implements Listener {
	final List<ItemStack> allItems;
	
	HashMap<UUID, Integer> position = new HashMap<UUID, Integer>();
	HashMap<UUID, List<Recipe>> queue = new HashMap<UUID, List<Recipe>>();
	//HashMap<Character, Integer> craftingmap = new HashMap<Character, Integer>();
	
	@SuppressWarnings("deprecation")
	public SMIListener() {
		allItems = new ArrayList<ItemStack>();
		for (Material mat : Material.values()) {
			ItemStack item = new ItemStack(mat, 1);
			switch (mat) {
			case AIR:
			case BED_BLOCK:
			case PISTON_EXTENSION:
			case PISTON_MOVING_PIECE:
			case REDSTONE_WIRE:
			case CROPS:
			case BURNING_FURNACE:
			case SIGN_POST:
			case WOODEN_DOOR:
			case WALL_SIGN:
			case IRON_DOOR_BLOCK:
			case GLOWING_REDSTONE_ORE:
			case REDSTONE_TORCH_OFF:
			case SUGAR_CANE_BLOCK:
			case CAKE_BLOCK:
			case DIODE_BLOCK_OFF:
			case DIODE_BLOCK_ON:
			case LOCKED_CHEST:
			case PUMPKIN_STEM:
			case MELON_STEM:
			case NETHER_WARTS:
			case BREWING_STAND:
			case CAULDRON:
			case REDSTONE_LAMP_ON:
			case TRIPWIRE:
			case FLOWER_POT:
			case SKULL:
			case REDSTONE_COMPARATOR_OFF:
			case REDSTONE_COMPARATOR_ON:
				break;
			default:
				//Bukkit.getLogger().info(allItems.size() +" "+ mat.name());
				allItems.add(item);
			}
		}
		
		/*craftingmap.put('a', 0);
		craftingmap.put('b', 1);
		craftingmap.put('c', 2);
		craftingmap.put('d', 3);
		craftingmap.put('e', 4);
		craftingmap.put('f', 5);
		craftingmap.put('g', 6);
		craftingmap.put('h', 7);
		craftingmap.put('i', 8);*/
	}
	
	private void delayedOpenSMI(final Player p) {
		Bukkit.getScheduler().scheduleSyncDelayedTask(SoManyItems.Instance, new Runnable() {
			@Override
			public void run() {
				openSMI(p);
			}
		},1);
	}
	
	private void openSMI(Player p) {
		//p.sendMessage("Open smi");
		Inventory inv = Bukkit.getServer().createInventory(null, 9*6, "So Many Items!");
    	p.openInventory(generateInventory(0, inv));
    	position.put(p.getUniqueId(), 0);
	}
	
	@EventHandler
    public boolean handleCommand(Player sender, Command cmd, String label, String[] args) {
        if(cmd.getName().equals("smi")) {
        	openSMI(sender);
            return true;
        }
        return false;
    }
	
	private Inventory generateInventory(int offset, Inventory inv) {
		inv.clear();
		int begginofset = offset == 0 ? 0 : 1;
		
		for(int i = begginofset; i < Math.min(9*6-1, allItems.size() - offset); i++) {
			int item = i + offset;
			inv.setItem(i, allItems.get(item));
		}
		return inv;
	}
	
	private void ShowRecipe(Player p, Recipe r) {
		if (r instanceof ShapedRecipe) {
			ShapedRecipe sr = (ShapedRecipe)r;
			
			InventoryView invv = p.openWorkbench(null, true);
			CraftingInventory inv  = (CraftingInventory) invv.getTopInventory();
			
			Map<Character, ItemStack> itemmap = sr.getIngredientMap();
			ItemStack[] arr = new ItemStack[9];
			
			for (Entry<Character, ItemStack> i : itemmap.entrySet()) {
				int ind = i.getKey().charValue() - 97;
				arr[ind] = i.getValue();
			}
			inv.setMatrix(arr);
		}
		if (r instanceof ShapelessRecipe) {
			ShapelessRecipe sr = (ShapelessRecipe)r;
			
			InventoryView invv = p.openWorkbench(null, true);
			CraftingInventory inv  = (CraftingInventory) invv.getTopInventory();
			
			Collection<ItemStack> list = sr.getIngredientList();
			ItemStack[] arr = new ItemStack[list.size()];
			arr = list.toArray(arr);
			inv.setMatrix(arr);
		}
		if (r instanceof FurnaceRecipe) {
			FurnaceRecipe fr = (FurnaceRecipe)r;
			InventoryView inv = p.openInventory(Bukkit.getServer().createInventory(null, InventoryType.FURNACE));
			inv.getTopInventory().setItem(0, fr.getInput());
			inv.getTopInventory().setItem(1, new ItemStack(Material.COAL, 64));
			inv.getTopInventory().setItem(2, fr.getResult());
		}
	}
	
	private void delayedNextRecipes(final Player p) {
		Bukkit.getScheduler().scheduleSyncDelayedTask(SoManyItems.Instance, new Runnable() {
			@Override
			public void run() {
				NextRecipes(p);
			}
		},2);
	}
	
	private void NextRecipes(Player p) {
		List<Recipe> rs = queue.get(p.getUniqueId());
		//p.sendMessage("Next " + rs.size());
		
		if (rs.isEmpty()) {
			queue.remove(p.getUniqueId());
			p.closeInventory();
			delayedOpenSMI(p);
		}
		
		Recipe r = rs.get(0);
		rs.remove(rs.get(0));
		
		queue.put(p.getUniqueId(), rs);
		
		ShowRecipe(p, r);
	}
	
	
	@EventHandler
	public void playerInv(org.bukkit.event.inventory.InventoryClickEvent event) {
		if (!(event.getWhoClicked() instanceof Player)) {
			return;
		}
		
		Player p = (Player) event.getWhoClicked();
		
		UUID id = p.getUniqueId();
		int slot = event.getRawSlot();
		ItemStack item = event.getCurrentItem();
		
		//p.sendMessage(position.containsKey(id) + "");
		
		if (position.containsKey(id) && slot < 9*6) {
			event.setCancelled(true);
			int offset = position.get(id);
			
			if (item == null || item.getAmount() == 0) {
				if (slot == 0) { //prev page
					offset -= 9;
				} else if (slot == (9*6-1) && 9*6 + offset < allItems.size()) { //next page
					offset += 9;
				}
				generateInventory(offset, event.getInventory());
				position.put(id, offset);
				
				return;
			}
			
			
			
			List<Recipe> recipies = Bukkit.getServer().getRecipesFor(item);
			if (!recipies.isEmpty()) {
				queue.put(id, recipies);
				p.closeInventory();
				delayedNextRecipes(p);
			}
			return;
		}
		
		if (queue.containsKey(id)) {
			//p.sendMessage("here");
			event.setCancelled(true);
			
			if(item != null && item.getAmount() != 0) {
				List<Recipe> recipies = Bukkit.getServer().getRecipesFor(item);
				if (!recipies.isEmpty()) {
					queue.put(id, recipies);
					p.closeInventory();
				}
			}
		}
	}
	
	@EventHandler
	public void playerCloseInv(final org.bukkit.event.inventory.InventoryCloseEvent event) {
		/*if (! (event.getPlayer() instanceof Player)) {
			return;
		}*/
		
		Player p = (Player) event.getPlayer(); 
		UUID id = p.getUniqueId();
		if (position.containsKey(id)) {
			//((Player)event.getPlayer()).sendMessage("Close SMI");
			event.getInventory().clear();
			position.remove(id);
		} else if (queue.containsKey(id)) {
			//((Player)event.getPlayer()).sendMessage("Close craft");
			event.getInventory().clear();
			delayedNextRecipes(p);
		}
	}
}
