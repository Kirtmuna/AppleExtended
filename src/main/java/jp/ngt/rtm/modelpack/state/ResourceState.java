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

package jp.ngt.rtm.modelpack.state;

import com.anatawa12.fixRtm.rtm.HooksKt;
import jp.ngt.rtm.RTMCore;
import jp.ngt.rtm.entity.util.CollisionObj;
import jp.ngt.rtm.modelpack.ModelPackManager;
import jp.ngt.rtm.modelpack.ResourceType;
import jp.ngt.rtm.modelpack.modelset.ModelSetBase;
import jp.ngt.rtm.modelpack.modelset.ResourceSet;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.AxisAlignedBB;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class ResourceState<T extends ResourceSet> {
    public final ResourceType type;
    public final DataMap dataMap = new DataMap();
    public final List<String> exclusionParts = new ArrayList<>();

    public int version;
    private String name;
    private String modelName;
    private T modelSet;
    public int color = 0xFFFFFF;

    public ResourceState(ResourceType type, @Nullable Object entity) {
        this.type = type;
        this.modelName = type.defaultName;
        this.dataMap.setEntity(entity);
    }

    public void readFromNBT(NBTTagCompound nbt) {
        this.version = nbt.getInteger("Version");
        this.setName(nbt.getString("Name"));
        this.setResourceName(nbt.getString("ResourceName"));
        this.color = nbt.getInteger("Color");
        this.getResourceSet();
        this.dataMap.readFromNBT(nbt.getCompoundTag("DataMap"));
    }

    public NBTTagCompound writeToNBT() {
        NBTTagCompound nbt = new NBTTagCompound();
        nbt.setString("Name", this.getName());
        nbt.setInteger("Version", RTMCore.BUILD_NO);
        nbt.setString("ResourceName", this.modelName);
        nbt.setInteger("Color", this.color);
        nbt.setTag("DataMap", this.dataMap.writeToNBT());
        return nbt;
    }

    public String getName() {
        this.setName(this.name);
        return this.name;
    }

    public void setName(String par1) {
        if (par1 == null || par1.isEmpty()) {
            par1 = "no_name";
        }
        this.name = par1;
    }

    public String getArg() {
        return this.dataMap.getArg();
    }

    public void setArg(String par1, boolean overwrite) {
        this.dataMap.setArg(par1, overwrite);
    }

    public String getResourceName() {
        return this.modelName;
    }

    public void setResourceName(String par1) {
        if (!par1.isEmpty()) {
            this.modelName = par1;
            this.modelSet = null;
        }
    }

    public void setResourceToDefault() {
        this.setResourceName(this.type.defaultName);
    }

    public T getResourceSet() {
        if (this.modelSet == null || this.modelSet.isDummy()) {
            this.modelSet = (T) HooksKt.eraseNullForModelSet(ModelPackManager.INSTANCE.getResourceSet(this.type, this.modelName), this.type);
            if (!this.modelSet.isDummy()) {
                this.modelSet.dataFormatter.initDataMap(this.dataMap);
            }
        }
        return this.modelSet;
    }

    public DataMap getDataMap() {
        this.getResourceSet();
        return this.dataMap;
    }

    public void applyCollison(Entity target, Entity myself, AxisAlignedBB playerAABB, List<AxisAlignedBB> list) {
        if (this.modelSet instanceof ModelSetBase) {
            CollisionObj obj = ((ModelSetBase) this.modelSet).getCollisionObj();
            if (obj != null) {
                obj.applyCollison(target, myself, playerAABB, list, this.exclusionParts);
            }
        }
    }

    public void addExclusionParts(String... names) {
        for (String name : names) {
            if (!this.exclusionParts.contains(name)) {
                this.exclusionParts.add(name);
            }
        }
    }

    public void removeExclusionParts(String... names) {
        for (String name : names) {
            if (this.exclusionParts.contains(name)) {
                this.exclusionParts.remove(name);
            }
        }
    }
}