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

package jp.ngt.rtm.command;

import jp.ngt.ngtlib.io.NGTLog;
import jp.ngt.rtm.RTMCore;
import jp.ngt.rtm.entity.train.EntityBogie;
import jp.ngt.rtm.entity.train.EntityTrainBase;
import jp.ngt.rtm.entity.train.parts.EntityVehiclePart;
import jp.ngt.rtm.entity.train.util.Formation;
import jp.ngt.rtm.entity.train.util.FormationEntry;
import jp.ngt.rtm.entity.train.util.TrainState.TrainStateType;
import jp.ngt.rtm.network.PacketNotice;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.PlayerNotFoundException;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.MathHelper;

import java.util.Arrays;
import java.util.List;

public class CommandRTM extends CommandBase {

    private static final List<String> stateArray = Arrays.asList("door", "pan", "speed");

    @Override
    public String getName() {
        return "rtm";
    }

    @Override
    public String getUsage(ICommandSender commandSender) {
        return "commands.rtm.usage";
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        EntityPlayer player;
        try {
            player = getCommandSenderAsPlayer(sender);
        } catch (PlayerNotFoundException e) {
            return;
        }

        if (args.length > 0) {
            String subCommand = args[0].toLowerCase();

            if (stateArray.contains(subCommand) && args.length >= 2) {
                try {
                    float val = Float.parseFloat(args[1]);
                    double range = 16.0D; // プレイヤーからの半径

                    List<EntityTrainBase> list = player.world.getEntitiesWithinAABB(
                            EntityTrainBase.class,
                            new AxisAlignedBB(
                                    player.posX - range, player.posY - range, player.posZ - range,
                                    player.posX + range, player.posY + range, player.posZ + range));

                    for (EntityTrainBase train : list) {
                        if (subCommand.equals("door")) {
                            train.setVehicleState(TrainStateType.Door, (byte) val);
                        } else if (subCommand.equals("pan")) {
                            train.setVehicleState(TrainStateType.Pantograph, (byte) val);
                        } else if (subCommand.equals("speed")) {
                            train.setSpeed(val / 72.0F);
                        }
                    }
                    NGTLog.sendChatMessage(player, "Applied " + subCommand + " to " + list.size() + " trains.");
                } catch (NumberFormatException e) {
                    NGTLog.sendChatMessage(player, "Invalid number format.");
                }
                return;
            }
            if (args[0].equals("delAllTrain") || subCommand.equals("dat")) {
                int count = 0;
                List<Entity> list = player.world.loadedEntityList;
                for (Entity entity : list) {
                    if (entity instanceof EntityTrainBase) {
                        entity.setDead();
                        ++count;
                    } else if (entity instanceof EntityBogie) {
                        entity.setDead();
                    } else if (entity instanceof EntityVehiclePart) {
                        if (((EntityVehiclePart) entity).getVehicle() == null) {
                            entity.setDead();
                        }
                    }
                }
                NGTLog.sendChatMessage(player, "Delete " + count + " trains.");
                return;
            }
            if (args[0].equalsIgnoreCase("drf")) {
                if (player.getRidingEntity() instanceof EntityTrainBase) {
                    EntityTrainBase ridingTrain = (EntityTrainBase) player.getRidingEntity();
                    Formation formation = ridingTrain.getFormation();

                    if (formation != null && formation.entries != null) {
                        int count = 0;
                        for (FormationEntry entry : formation.entries) {
                            if (entry != null && entry.train != null) {
                                entry.train.setDead();
                                count++;
                            }
                        }
                        NGTLog.sendChatMessage(player, "Deleted riding formation (" + count + " trains).");
                    } else {
                        ridingTrain.setDead();
                        NGTLog.sendChatMessage(player, "Deleted riding train.");
                    }
                } else {
                    NGTLog.sendChatMessage(player, "You are not riding a train.");
                }
                return;
            }
            if (args[0].equals("dismount")) {
                player.dismountRidingEntity();
                return;
            }
            if (subCommand.equals("flyspeed") && args.length >= 2) {
                try {
                    float speed = Float.parseFloat(args[1]);
                    float clampedSpeed = MathHelper.clamp(speed, 0.1F, 10.0F);
                    if (player instanceof EntityPlayerMP) {
                        EntityPlayerMP playerMP = (EntityPlayerMP) player;
                        RTMCore.NETWORK_WRAPPER.sendTo(
                                new PacketNotice(PacketNotice.Side_CLIENT, "flySpeed," + clampedSpeed),
                                playerMP
                        );
                        NGTLog.sendChatMessage(playerMP, "Set fly speed multiplier to " + clampedSpeed);
                    }
                } catch (NumberFormatException e) {
                    NGTLog.sendChatMessage(sender, "Invalid number format.");
                }
                return;
            }
            NGTLog.sendChatMessage(sender, "/rtm <door|pan|speed> <value> : Control nearby trains");
            NGTLog.sendChatMessage(sender, "/rtm drf : Delete riding formation");
            NGTLog.sendChatMessage(sender, "/rtm delAllTrain : Delete all train");
            NGTLog.sendChatMessage(sender, "/rtm dat : Delete all train");
            NGTLog.sendChatMessage(sender, "/rtm dismount : Dismount player from vehicle");
            NGTLog.sendChatMessage(sender, "/rtm flyspeed <value> : Set player flying speed");
        }
    }
}