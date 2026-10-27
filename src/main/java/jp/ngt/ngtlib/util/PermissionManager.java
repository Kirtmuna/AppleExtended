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

package jp.ngt.ngtlib.util;

import com.anatawa12.fixRtm.asm.config.MainConfig;
import com.anatawa12.fixRtm.utils.SortedSetListView;
import jp.ngt.ngtlib.NGTCore;
import jp.ngt.ngtlib.io.NGTLog;
import jp.ngt.ngtlib.io.NGTText;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

public final class PermissionManager {
    public static final PermissionManager INSTANCE = new PermissionManager();

    private static boolean DEBUG_MODE = false;
    private static String ALL = "-all";

    private final File saveDir;
    private final File saveFile;
    private final Map<String, SortedSet<String>> permissionMap = new HashMap<>();

    private PermissionManager() {
        this.saveDir = new File(NGTCore.proxy.getMinecraftDirectory("ngt").getAbsolutePath());
        this.saveFile = new File(this.saveDir, "permission.txt");
    }

    public void save() {
        String[] sa = new String[this.permissionMap.size()];
        int i = 0;
        for (Entry<String, SortedSet<String>> entry : this.permissionMap.entrySet()) {
            StringBuilder sb = new StringBuilder();
            sb.append(entry.getKey()).append(":");
            for (String s : entry.getValue()) {
                sb.append(s).append(",");
            }
            sa[i] = sb.toString();
            ++i;
        }
        NGTText.writeToText(this.saveFile, sa);
    }

    public void load() throws IOException {
        this.initFile();

        List<String> slist = NGTText.readText(this.saveFile, "");
        for (String s : slist) {
            String[] sa2 = s.split(":");
            if (sa2.length == 2) {
                Set<String> list = this.getPlayerSet(sa2[0]);
                String[] sa3 = sa2[1].split(",");
                Collections.addAll(list, sa3);
            }
        }
    }

    private void initFile() {
        if (!this.saveDir.exists()) {
            this.saveDir.mkdirs();
        }

        if (!this.saveFile.exists()) {
            try {
                this.saveFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public List<String> getPlayerList(String par1) {
        return new SortedSetListView<>(getPlayerSortedSet(par1));
    }

    public Set<String> getPlayerSet(String par1) {
        return getPlayerSortedSet(par1);
    }

    private SortedSet<String> getPlayerSortedSet(String par1) {
        if (!this.permissionMap.containsKey(par1)) {
            this.permissionMap.put(par1, new TreeSet<>());
        }
        return this.permissionMap.get(par1);
    }

    public void showPermissionList(ICommandSender player) {
        for (Entry<String, SortedSet<String>> entry : this.permissionMap.entrySet()) {
            StringBuilder sb = new StringBuilder();
            sb.append(entry.getKey()).append(":");
            for (String s : entry.getValue()) {
                sb.append(s).append(",");
            }
            NGTLog.sendChatMessage(player, sb.toString());
        }
    }

    public void addPermission(ICommandSender player, String targetPlayerName, String category) {
        if (this.isOp(player)) {
            this.getPlayerSet(category).add(targetPlayerName);
            NGTLog.sendChatMessageToAll("Add permission (%s) to %s.", category, targetPlayerName);
            this.save();
        } else {
            NGTLog.sendChatMessage(player, "Only operator can use this command.");
        }
    }

    public void removePermission(ICommandSender player, String targetPlayerName, String category) {
        if (this.isOp(player)) {
            this.getPlayerSet(category).remove(targetPlayerName);
            NGTLog.sendChatMessageToAll("Remove permission (%s) from %s.", category, targetPlayerName);
            this.save();
        } else {
            NGTLog.sendChatMessage(player, "Only operator can use this command.");
        }
    }

    public boolean hasPermission(ICommandSender player, String category) {
        boolean has = hasPermissionInternal(player, category);
        if (!has)
            NGTLog.sendChatMessage(player, "%s need permission (%s).", player.getName(), category);
        return has;
    }

    public boolean hasPermissionInternal(ICommandSender player, String category) {
        if (MainConfig.addNegativePermissionEnabled) {
            if (this.getPlayerSet("negative.".concat(category)).contains(player.getName())) {
                return false;
            }
        }
        if (MainConfig.addAllowAllPermissionEnabled) {
            if (!category.equals("fixrtm.all_permit") && hasPermissionInternal(player, "fixrtm.all_permit"))
                return true;
        }
        if (this.isOp(player)) {
            return true;
        } else {
            Set<String> list = this.getPlayerSet(category);
            if (list.contains(player.getName()) || list.contains(ALL)) {
                return true;
            } else {
                return false;
            }
        }
    }

    public boolean isOp(ICommandSender player) {
        if (!DEBUG_MODE && !NGTUtil.isSMP()) {
            return true;
        } else if (player == null) {
            return true;
        } else if (player instanceof EntityPlayerMP) {
            String[] names = ((EntityPlayerMP) player).mcServer.getPlayerList().getOppedPlayerNames();
            for (String name : names) {
                if (player.getName().equals(name)) {
                    return true;
                }
            }
        } else if (player instanceof MinecraftServer) {
            return true;
        }
        return false;
    }
}