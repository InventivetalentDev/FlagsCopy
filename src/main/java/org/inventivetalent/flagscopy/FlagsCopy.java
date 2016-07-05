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
import org.inventivetalent.pluginannotations.command.*;
import org.inventivetalent.regionapi.RegionAPI;
import org.mcstats.MetricsLite;

import java.util.List;
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

		try {
			MetricsLite metrics = new MetricsLite(this);
			if (metrics.start()) {
				getLogger().info("Metrics started");
			}
		} catch (Exception e) {
		}
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
			 fallbackPrefix = "flagcopy")
	@Permission("flagscopy.copy")
	public void copyFlags(CommandSender sender, String source, String destString, @OptionalArg String worldName, @OptionalArg(def = "merge") String flagMode) {
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
		if (sourceRegion == null) {
			sender.sendMessage("§cSource region '" + source + "' not found");
			return;
		}

		String[] dests = destString.split(",");
		ProtectedRegion[] destRegions = new ProtectedRegion[dests.length];
		for (int i = 0; i < dests.length; i++) {
			if (source.equals(dests[i])) {
				sender.sendMessage("§cCan't copy from " + source + " to " + dests[i]);
				return;
			}
			ProtectedRegion region = regionManager.getRegion(dests[i]);
			if (region == null) {
				sender.sendMessage("§cDestination region '" + destString + "' not found");
				return;
			}
			destRegions[i] = region;

		}

		if ("replace".equals(flagMode.toLowerCase())) {
			for (ProtectedRegion region : destRegions) {
				region.setFlags(sourceRegion.getFlags());
			}
			sender.sendMessage("§aFlags replaced for §7" + destRegions.length + "§a regions.");
			return;
		} else {// merge
			int addCount = 0;
			int skipCount = 0;
			for (ProtectedRegion region : destRegions) {
				for (Map.Entry<Flag<?>, Object> entry : sourceRegion.getFlags().entrySet()) {
					if (region.getFlag(entry.getKey()) == null) {
						region.getFlags().put(entry.getKey(), entry.getValue());
						addCount++;
					} else {
						skipCount++;
					}
				}
			}
			sender.sendMessage("§aFlags merged, added §7" + addCount + "§a flags, skipped §7" + skipCount + "§a existing flags for §7" + destRegions.length + "§a regions.");
			return;
		}
	}

	@Completion
	public void copyFlags(List<String> completions, CommandSender sender, String source, String dest, String world, String flagMode) {
		if (sender instanceof Player) {
			RegionManager regionManager = RegionAPI.getRegionManager(((Player) sender).getWorld());
			if (regionManager != null) {
				if ((source == null || source.isEmpty() || regionManager.getRegion(source) == null) || dest == null || dest.isEmpty() || regionManager.getRegion(dest) == null) {
					completions.addAll(regionManager.getRegions().keySet());
					return;
				}
			}
		}
		if (world == null || world.isEmpty() || Bukkit.getWorld(world) == null) {
			for (World world1 : Bukkit.getWorlds()) {
				completions.add(world1.getName());
			}
			return;
		}
		if (flagMode == null || flagMode.isEmpty() || (!"merge".equalsIgnoreCase(flagMode) || !"replace".equalsIgnoreCase(flagMode))) {
			completions.add("merge");
			completions.add("replace");
		}
	}

	@Command(name = "batchFlag",
			 aliases = { "multiFlag" },
			 usage = "<regions> <flag name> <value>",
			 description = "Set a flag for multiple regions (separated by commas)",
			 min = 3,
			 max = -1,
			 fallbackPrefix = "flagcopy")
	@Permission("flagscopy.batch")
	public void batchFlag(Player sender, String regionsString, String flag, @JoinedArg String value) {
		RegionManager regionManager = RegionAPI.getRegionManager(sender.getWorld());
		if (regionManager == null) {
			sender.sendMessage("§cThis world doesn't have a region manager, what's going on?!");
			return;
		}
		String[] regionNames = regionsString.split(",");
		ProtectedRegion[] regions = new ProtectedRegion[regionNames.length];
		for (int i = 0; i < regionNames.length; i++) {
			ProtectedRegion region = regionManager.getRegion(regionNames[i]);
			if (region == null) {
				sender.sendMessage("§cRegion '" + regionNames[i] + "' not found");
				return;
			}
			regions[i] = region;
		}

		for (ProtectedRegion region : regions) {
			sender.chat("/worldguard:region flag " + region.getId() + " " + flag + " " + value);
		}
	}

	@Completion
	public void batchFlag(List<String> completions, Player sender, String regionsString, String flag, String value) {
		RegionManager regionManager = RegionAPI.getRegionManager(sender.getWorld());
		if (regionManager != null) {
			if (flag == null || flag.isEmpty()) {
				completions.addAll(regionManager.getRegions().keySet());
				return;
			}
		}
	}

}
