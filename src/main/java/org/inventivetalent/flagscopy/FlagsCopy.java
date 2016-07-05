package org.inventivetalent.flagscopy;

import com.sk89q.worldguard.protection.flags.Flag;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.inventivetalent.apihelper.APIManager;
import org.inventivetalent.pluginannotations.PluginAnnotations;
import org.inventivetalent.pluginannotations.command.Command;
import org.inventivetalent.pluginannotations.command.OptionalArg;
import org.inventivetalent.pluginannotations.command.Permission;
import org.inventivetalent.regionapi.RegionAPI;

import java.util.Map;

public class FlagsCopy extends JavaPlugin implements Listener {

	@Override
	public void onLoad() {
		APIManager.require(RegionAPI.class, this);
	}

	@Override
	public void onEnable() {
		APIManager.initAPI(RegionAPI.class);
		PluginAnnotations.loadAll(this, this);
	}

	@Command(name = "copyFlags",
			 aliases = {},
			 usage = "<source region> <destination region> [world] [merge|replace]",
			 description = "Copy flags from one region to another\n"
					 + "\n"
					 + "- merge will keep existing flags and only add new flags,\n"
					 + "- replace clears all flags first and then sets the new ones",
			 min = 2,
			 max = 4,
			 fallbackPrefix = "regioncopy")
	@Permission("flagscopy.copy")
	public void copyFlags(CommandSender sender, String source, String dest, @OptionalArg String worldName, @OptionalArg(def = "merge") String flagMode) {
		if (!(sender instanceof Player) && (worldName == null || worldName.isEmpty())) {
			sender.sendMessage("§cPlease specify a world");
			return;
		}
		World world;
		if (sender instanceof Player) {
			world = ((Player) sender).getWorld();
		} else {
			world = Bukkit.getWorld(worldName);
			if (world == null) {
				sender.sendMessage("§cWorld '" + worldName + "' not found");
				return;
			}
		}

		RegionManager regionManager = RegionAPI.getRegionManager(world);
		if (regionManager == null) {
			sender.sendMessage("§cThis world doesn't have a region manager, what's going on?!");
			return;
		}
		ProtectedRegion sourceRegion = regionManager.getRegion(source);
		ProtectedRegion destRegion = regionManager.getRegion(dest);

		if (sourceRegion == null) {
			sender.sendMessage("§cSource region '" + source + "' not found");
			return;
		}
		if (destRegion == null) {
			sender.sendMessage("§cDestination region '" + dest + "' not found");
			return;
		}

		if ("replace".equals(flagMode.toLowerCase())) {
			destRegion.setFlags(sourceRegion.getFlags());
			sender.sendMessage("§aFlags replaced.");
			return;
		} else {// merge
			int addCount = 0;
			int skipCount = 0;
			for (Map.Entry<Flag<?>, Object> entry : sourceRegion.getFlags().entrySet()) {
				if (destRegion.getFlag(entry.getKey()) == null) {
					destRegion.getFlags().put(entry.getKey(), entry.getValue());
					addCount++;
				} else {
					skipCount++;
				}
			}
			sender.sendMessage("§aFlags merged, added §7" + addCount + "§a flags, skipped §7" + skipCount + "§a existing flags.");
			return;
		}
	}

}
