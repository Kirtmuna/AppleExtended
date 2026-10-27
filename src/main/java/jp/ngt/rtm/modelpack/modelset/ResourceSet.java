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

package jp.ngt.rtm.modelpack.modelset;

import com.anatawa12.fixRtm.rtm.modelpack.ModelState;
import jp.ngt.rtm.modelpack.cfg.ResourceConfig;
import jp.ngt.rtm.modelpack.state.DataFormatter;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public abstract class ResourceSet<T extends ResourceConfig> {
    protected final T cfg;
    public final DataFormatter dataFormatter;
    private boolean isDummySet;

    public byte[] md5;
    public ModelState state = ModelState.INITIALIZED;

    public ResourceSet() {
        this.cfg = this.getDummyConfig();
        this.dataFormatter = new DataFormatter(this.cfg);
        this.isDummySet = true;
    }

    public ResourceSet(T par1) {
        this.cfg = par1;
        this.dataFormatter = new DataFormatter(this.cfg);
        this.isDummySet = false;
    }

    public void constructOnServer() {
    }

    @SideOnly(Side.CLIENT)
    public void constructOnClient() {
    }

    @SideOnly(Side.CLIENT)
    public void finishConstruct() {
    }

    public T getConfig() {
        return this.cfg;
    }

    public abstract T getDummyConfig();

    public boolean isDummy() {
        return this.isDummySet;
    }
}