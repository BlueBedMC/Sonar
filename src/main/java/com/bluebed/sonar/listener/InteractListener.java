package com.bluebed.sonar.listener;

import com.bluebed.sonar.Sonar;
import com.bluebed.sonar.constructor.SonarJukebox;
import com.bluebed.sonar.constructor.SonarManager;
import com.bluebed.sonar.gui.selector.SonarSongSelector;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

import static com.bluebed.sonar.util.BlockUtil.getClosestBlockOfType;

public class InteractListener implements Listener {

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        Block block = event.getClickedBlock();
        if (block == null) return;
        if (block.getType() != Material.JUKEBOX) return;
        SonarJukebox jukebox = SonarManager.getClosestJukebox(player.getLocation(), 5);
        if (jukebox == null) return;
        event.setCancelled(true);
        SonarSongSelector.open(player, jukebox);
    }

}
