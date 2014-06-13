package me.cmesh.SoManyItems;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

/*
 * @author Christian Mesh
 * So many items, much wow! 
 * 
 */

public class SoManyItems extends JavaPlugin {
	private SMIListener listener;
	public static SoManyItems Instance;
	
	public SoManyItems() {
		Instance = this;
		listener = new SMIListener();
	}
	
	public void onEnable() {
        this.getServer().getPluginManager().registerEvents(listener, this);
    }
	
	@Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        return sender instanceof Player && listener.handleCommand((Player) sender, cmd, label, args);
    }
}
