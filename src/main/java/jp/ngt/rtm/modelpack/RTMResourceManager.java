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

package jp.ngt.rtm.modelpack;

import com.anatawa12.fixRtm.rtm.RTMResourceManagerKt;
import com.google.common.collect.Lists;
import jp.ngt.ngtlib.io.NGTFileLoader;
import net.minecraft.client.resources.FallbackResourceManager;
import net.minecraft.client.resources.IResource;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.client.resources.SimpleResource;
import net.minecraft.client.resources.data.MetadataSerializer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.io.*;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@SideOnly(Side.CLIENT)
public final class RTMResourceManager extends FallbackResourceManager implements IResourceManager {
    private final Map<ResourceLocation, IResource> resourceCache = new HashMap<>();
    private final MetadataSerializer serializer;
    private final File domain;

    public RTMResourceManager(MetadataSerializer par1, File par2) {
        super(par1);
        this.serializer = par1;
        this.domain = par2;
    }

    @Override
    public Set getResourceDomains() {
        return Collections.<String>emptySet();
    }

    @Override
    public IResource getResource(ResourceLocation par1) throws IOException {
        if (this.domain == null) {
            throw new FileNotFoundException(par1.toString());
        }

        if (this.resourceCache.containsKey(par1)) {
            return this.resourceCache.get(par1);
        }

        String absPath = this.domain.getAbsolutePath();

        if (absPath.contains(par1.getResourceDomain())) {
            InputStream stream = null;
            String suffix = NGTFileLoader.getArchiveSuffix(absPath);
            if (!suffix.isEmpty()) {
                String zipPath = NGTFileLoader.getArchivePath(absPath, suffix);
                try {
                    stream = RTMResourceManagerKt.getInputStreamFromZip(zipPath, par1);
                } catch (IOException ioexception) {
                    ioexception.printStackTrace();
                }
            } else {
                File resource = new File(this.domain, par1.getResourcePath());
                stream = new FileInputStream(resource);
            }

            if (stream != null) {
                IResource resource = new SimpleResource("RTMCustom", par1, stream, null, this.serializer);
                this.resourceCache.put(par1, resource);
                return resource;
            }
        }

        throw new ModelPackException("[RTMResourceManager] Can't get input stream", par1.getResourcePath());
    }

    @Override
    public List getAllResources(ResourceLocation par1) throws IOException {
        List list = Lists.newArrayList();
        list.add(this.getResource(par1));
        if (!list.isEmpty()) {
            return list;
        }
        throw new FileNotFoundException(par1.toString());
    }
}