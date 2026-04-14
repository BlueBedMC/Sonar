package com.bluebed.sonar.command;

import com.bluebed.sonar.Sonar;
import com.bluebed.sonar.constructor.SonarCodes;
import com.bluebed.sonar.constructor.SonarJukebox;
import com.bluebed.sonar.constructor.SonarManager;
import com.bluebed.sonar.gui.settings.SonarSettings;
import com.bluebed.sonar.util.ConfigUtil;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import static io.papermc.paper.command.brigadier.Commands.argument;
import static io.papermc.paper.command.brigadier.Commands.literal;

public class SonarCommands {

    public static long lastPlay = System.currentTimeMillis();

    public static LiteralArgumentBuilder<CommandSourceStack> build() {
        return literal("sonar")
                .then(literal("create")
                        .then(argument("id", StringArgumentType.string())
                                .executes(SonarCommands::create)
                        )
                )
                .then(literal("settings")
                        .then(argument("id", StringArgumentType.string())
                                .executes(SonarCommands::settings)
                        ))
                .then(literal("reload")
                        .executes(SonarCommands::reload));
    }

    private static int create(CommandContext<CommandSourceStack> ctx) {
        CommandSender sender = ctx.getSource().getSender();

        if (!perm(sender, "sonar.create")) {
            sender.sendMessage("§cNo permission.");
            return Command.SINGLE_SUCCESS;
        }

        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cOnly players can use this command.");
            return Command.SINGLE_SUCCESS;
        }

        String id = StringArgumentType.getString(ctx, "id");

        SonarCodes code = SonarManager.createJukebox(id, player.getLocation());

        if (code == SonarCodes.ALREADY_EXISTS) {
            player.sendMessage("§cThat jukebox id already exists! Choose another.");
            return Command.SINGLE_SUCCESS;
        }

        player.sendMessage("§aCreated a new jukebox!");
        return Command.SINGLE_SUCCESS;
    }

    private static int settings(CommandContext<CommandSourceStack> ctx) {
        CommandSender sender = ctx.getSource().getSender();

        if (!perm(sender, "sonar.settings")) {
            sender.sendMessage("§cNo permission.");
            return Command.SINGLE_SUCCESS;
        }

        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cOnly players can use this command.");
            return Command.SINGLE_SUCCESS;
        }

        String id = StringArgumentType.getString(ctx, "id");

        SonarJukebox jukebox = SonarManager.getJukebox(id);

        if (jukebox == null) {
            player.sendMessage("§cThat jukebox doesn't exist! Are you sure that's the right ID?");
            return Command.SINGLE_SUCCESS;
        }

        SonarSettings.open(player, jukebox);
        return Command.SINGLE_SUCCESS;
    }

    private static int reload(CommandContext<CommandSourceStack> ctx) {
        CommandSender sender = ctx.getSource().getSender();

        if (!perm(sender, "sonar.reload")) {
            sender.sendMessage("§cYou do not have permission to use this command.");
            return Command.SINGLE_SUCCESS;
        }

        sender.sendMessage("§aReloading plugin...");
        Sonar.getPlugin().reload();
        return Command.SINGLE_SUCCESS;
    }

    private static boolean perm(CommandSender player, String perm) {
        return player.hasPermission(perm);
    }
}
