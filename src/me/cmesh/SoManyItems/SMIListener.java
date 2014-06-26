package me.cmesh.SoManyItems;

import java.util.*;

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
	
	HashMap<UUID, Integer> position = new HashMap<UUID, Integer>();
	HashMap<UUID, List<Recipe>> queue = new HashMap<UUID, List<Recipe>>();
	HashMap<UUID, Integer> index = new HashMap<UUID, Integer>();
	HashMap<UUID, Boolean> isClosing = new HashMap<UUID, Boolean>();
	
	private List<ItemStack> getAllItems() {
		List<ItemStack> all = new ArrayList<ItemStack>();
		
		Recipe curr = null;
		for (Iterator<Recipe> r  = Bukkit.getServer().recipeIterator(); r.hasNext(); curr = r.next()) {
			if (curr != null) {
				ItemStack res = curr.getResult();
				res.setAmount(1);
				if (!all.contains(res)) {
					all.add(res);
				}
			}
		}
		
		return all;
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
		
		 List<ItemStack> allItems = getAllItems();
		
		for(int i = begginofset; i < Math.min(9*6-1, allItems.size() - offset); i++) {
			int item = i + offset;
			inv.setItem(i, allItems.get(item));
		}
		return inv;
	}
	
	private void ShowRecipe(final Player p, Recipe r) {
		if (r instanceof ShapedRecipe) {
			ShapedRecipe sr = (ShapedRecipe)r;
			
			InventoryView invv = p.openWorkbench(null, true);
			CraftingInventory inv  = (CraftingInventory) invv.getTopInventory();
			Map<Character, ItemStack> itemmap = sr.getIngredientMap();
			
			int ri = 0;
			for (String map : sr.getShape()) {
				int ci = 0;
				for (char c : map.toCharArray()) {
					ItemStack curr = itemmap.get(c);
					inv.setItem(ci + ri*3 + 1, curr);
					ci++;
				}
				ri ++;
			}
		}
		if (r instanceof ShapelessRecipe) {
			ShapelessRecipe sr = (ShapelessRecipe)r;
			
			InventoryView invv = p.openWorkbench(null, true);
			CraftingInventory inv  = (CraftingInventory) invv.getTopInventory();
			
			Collection<ItemStack> list = sr.getIngredientList();
			ItemStack[] arr = new ItemStack[9];
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
	
	private void NextRecipes(final Player p) {
		UUID key = p.getUniqueId();
		List<Recipe> rs = queue.get(key);
		Integer i = index.get(key);
		
		//Check for inv closed
		if (i == null) {
			return;
		}
		
		isClosing.put(key, true);
		p.closeInventory();
		isClosing.put(key, false);
		
		//p.sendMessage("Next " + i);
		Recipe r = rs.get(i);
		
		if (i + 1 == rs.size()) {
			i = 0;
		} else {
			i++;
		}
		
		index.put(p.getUniqueId(), i);
		
		ShowRecipe(p, r);
		if(rs.size() > 1) {
			Bukkit.getScheduler().scheduleSyncDelayedTask(SoManyItems.Instance, new Runnable() {
	
				@Override
				public void run() {
					NextRecipes(p);
				}
				
			}, 30l);
		}
	}
	
	private List<Recipe> filterRecipies(List<Recipe> list, ItemStack compare) {
		List<Recipe> res = new ArrayList<Recipe>();
		for(Recipe r : list) {
			if (r.getResult().equals(compare)) {
				res.add(r);
			}
		}
		return res;
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
				
		if (position.containsKey(id)){
			List<ItemStack> allItems = getAllItems();
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
			recipies = filterRecipies(recipies, item);
			if (!recipies.isEmpty()) {
				queue.put(id, recipies);
				p.closeInventory();
				index.put(id, 0);
				delayedNextRecipes(p);
			}
			return;
		}
		
		if (queue.containsKey(id)) {
			event.setCancelled(true);
			if(item != null && item.getAmount() != 0) {
				List<Recipe> recipies = Bukkit.getServer().getRecipesFor(item);
				recipies = filterRecipies(recipies, item);
				if (recipies.isEmpty()) {
					recipies = Bukkit.getServer().getRecipesFor(new ItemStack(item.getType(), 1));//HACK
				}
				
				if (!recipies.isEmpty()) {
					index.put(id, 0);
					queue.put(id, recipies);
					isClosing.put(id, true);
					p.closeInventory();
					isClosing.put(id, false);
					NextRecipes(p);
				}
			}
		}
	}
	
	@EventHandler
	public void playerCloseInv(final org.bukkit.event.inventory.InventoryCloseEvent event) {
		Player p = (Player) event.getPlayer(); 
		UUID id = p.getUniqueId();
		if (position.containsKey(id)) {
			//p.sendMessage("Close SMI");
			event.getInventory().clear();
			position.remove(id);
		} else if (queue.containsKey(id)) {
			if (!isClosing.get(id)) {
				//p.sendMessage("Close craft");
				queue.remove(p.getUniqueId());
				index.remove(p.getUniqueId());
				
				event.getInventory().clear();
				
				delayedOpenSMI(p);
			} else {
				event.getInventory().clear();
			}
		}
	}
}
