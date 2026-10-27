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

package jp.apple.appleextended.highlight;

import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@SideOnly(Side.CLIENT)
public class HighlightRegistry {

    private static final List<HighlightEntry> ENTRIES = new CopyOnWriteArrayList<>();

    private HighlightRegistry() {
    }

    public static void register(HighlightEntry entry) {
        ENTRIES.add(entry);
    }

    public static List<HighlightEntry> getEntries() {
        return Collections.unmodifiableList(ENTRIES);
    }
}