/*
 *
 *  * AppleExtended
 *  *
 *  * Original code (c) 2020 anatawa12 and other contributors.
 *  * Modifications (c) 2026 Applepie.
 *  *
 *  * This file is part of AppleExtended, which is a derivative work of fixRTM.
 *  * Both are licensed under the GNU Lesser General Public License version 3.
 *  * See LICENSE.txt in the mod root for full license text.
 *
 *
 */

package jp.ngt.ngtlib.command;

import com.anatawa12.fixRtm.ngtlib.command.CommandPermitKt;
import jp.ngt.ngtlib.io.NGTLog;
import jp.ngt.ngtlib.util.PermissionManager;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;

import java.util.List;

public class CommandPermit extends CommandBase {
    @Override
    public int getRequiredPermissionLevel() {
        return 3;
    }

    @Override
    public String getName() {
        return "permit";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "commands.permit.usage";
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        if (args.length > 0) {
            if (args[0].equals("list")) {
                PermissionManager.INSTANCE.showPermissionList(sender);
                return;
            } else if (args[0].equals("myname")) {
                NGTLog.sendChatMessage(sender, "My name is " + sender.getName());
                return;
            } else if (args.length >= 3) {
                String playerName = args[1];
                String target = args[2];

                if (args[0].equals("add")) {
                    PermissionManager.INSTANCE.addPermission(sender, playerName, target);
                    return;
                } else if (args[0].equals("remove")) {
                    PermissionManager.INSTANCE.removePermission(sender, playerName, target);
                    return;
                }
            }
        }

        NGTLog.sendChatMessage(sender, "/permit add <player or -all> <category>");
        NGTLog.sendChatMessage(sender, "/permit remove <player or -all> <category>");
        NGTLog.sendChatMessage(sender, "/permit list");
        NGTLog.sendChatMessage(sender, "/permit myname");
    }

    @Override
    public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, BlockPos targetPos) {
        return CommandPermitKt.getTabCompletions(server, args);
    }
}