package com.trc202.CombatTagListeners;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import com.topcat.npclib.NPCManager;
import com.topcat.npclib.entity.NPC;
import com.trc202.CombatTag.CombatTag;
import com.trc202.Containers.PlayerDataContainer;
import com.trc202.Containers.Settings;

public class NoPvpPlayerListener implements Listener{
	
	private final CombatTag plugin;
	public static int explosionDamage = -1;
	public NPCManager npcm;
	
    public NoPvpPlayerListener(CombatTag instance) {
    	plugin = instance;
    }
    
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerJoin(PlayerJoinEvent e){
		Player loginPlayer = e.getPlayer();
		if(plugin.settings.getCurrentMode() == Settings.SettingsType.NPC){
			onPlayerJoinNPCMode(loginPlayer);
		}else if(plugin.settings.getCurrentMode() == Settings.SettingsType.TIMED){
			onPlayerJoinTimedMode(loginPlayer);
		}
	}
	
    @EventHandler
	public void onPlayerQuit(PlayerQuitEvent e){
		Player quitPlr = e.getPlayer();
		if(plugin.settings.getCurrentMode() == Settings.SettingsType.NPC){
			onPlayerQuitNPCMode(quitPlr);
		}else if(plugin.settings.getCurrentMode() == Settings.SettingsType.TIMED){
			onPlayerQuitTimedMode(quitPlr);
		}
	}
	
	private void onPlayerQuitTimedMode(Player quitPlr){
		if(plugin.hasDataContainer(quitPlr.getName())){
			PlayerDataContainer quitDataContainer = plugin.getPlayerData(quitPlr.getName());
			if(!quitDataContainer.hasPVPtagExpired()){
				quitDataContainer.setHealth(quitPlr.getHealth());
				quitDataContainer.setPlayerArmor(quitPlr.getInventory().getArmorContents());
				quitDataContainer.setPlayerInventory(quitPlr.getInventory().getContents());
				quitDataContainer.setExp(quitPlr.getExp());
				plugin.scheduleDelayedKill(quitPlr.getName());	
				}
			}
	}
	
	private void onPlayerQuitNPCMode(Player quitPlr){
		if(plugin.hasDataContainer(quitPlr.getName())){
			//Player is likely in pvp
			PlayerDataContainer quitDataContainer = plugin.getPlayerData(quitPlr.getName());
			if(!quitDataContainer.hasPVPtagExpired()){
				//Player has logged out before the pvp battle is considered over by the plugin
				if(plugin.isDebugEnabled()){plugin.log.info("[CombatTag] Player has logged of during pvp!");}
				if(plugin.settings.isInstaKill()){
					quitPlr.setHealth(0);
					plugin.removeDataContainer(quitPlr.getName());
				}else{
					NPC npc = plugin.spawnNpc(quitPlr.getName(),quitPlr.getLocation());
					if(npc.getBukkitEntity() instanceof Player){
						Player npcPlayer = (Player) npc.getBukkitEntity();
						plugin.copyContentsNpc(npc, quitPlr);
						String plrName = quitPlr.getName(); //tempfix
						plugin.npcm.rename(plrName, plugin.getNpcName(plrName)); //tempfix
						npcPlayer.setHealth(quitPlr.getHealth());
						quitDataContainer.setSpawnedNPC(true);
						quitDataContainer.setNPCId(quitPlr.getName());
						quitDataContainer.setShouldBePunished(true);
						quitPlr.getWorld().createExplosion(quitPlr.getLocation(), explosionDamage); //Create the smoke effect //
					}
				}
			}
		}
	}

	private void onPlayerJoinNPCMode(Player loginPlayer){
		if(plugin.hasDataContainer(loginPlayer.getName())){
			//Player has a data container and is likely to need some sort of punishment
			PlayerDataContainer loginDataContainer = plugin.getPlayerData(loginPlayer.getName());
			if(loginDataContainer.hasSpawnedNPC()){
				//Player has pvplogged and has not been killed yet
				//despawn the npc and transfer any effects over to the player
				if(plugin.isDebugEnabled()){plugin.log.info("[CombatTag] Player logged in and has npc");}
				plugin.despawnNPC(loginDataContainer);
			}
			if(plugin.isDebugEnabled()){plugin.log.info("[CombatTag] " + loginDataContainer.getPlayerName() +" should be punushed");}
			if(loginDataContainer.shouldBePunished()){
				if(plugin.isDebugEnabled()){plugin.log.info("[CombatTag] Getting info from NPC and putting it back into the player");}
				loginPlayer.setExp(loginDataContainer.getExp());
				loginPlayer.getInventory().setArmorContents(loginDataContainer.getPlayerArmor());
				loginPlayer.getInventory().setContents(loginDataContainer.getPlayerInventory());
				loginPlayer.setHealth(loginDataContainer.getHealth());
				assert(loginPlayer.getHealth() == loginDataContainer.getHealth());
				loginPlayer.setLastDamageCause(new EntityDamageEvent(loginPlayer, DamageCause.ENTITY_EXPLOSION, 0));
			}
			plugin.removeDataContainer(loginPlayer.getName());
			plugin.createPlayerData(loginPlayer.getName()).setPvPTimeout(plugin.getTagDuration());
		}
	}
	
	private void onPlayerJoinTimedMode(Player joinedPlr){
		//TODO
	}


}