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

import com.anatawa12.fixRtm.DummyModelPackManager;
import com.anatawa12.fixRtm.UtilsKt;
import com.anatawa12.fixRtm.asm.config.MainConfig;
import com.anatawa12.fixRtm.network.NetworkHandler;
import com.anatawa12.fixRtm.network.SentAllModels;
import com.anatawa12.fixRtm.rtm.modelpack.init.UnconstructSetsQueue;
import jp.apple.appleextended.script.ScriptLoader;
import jp.apple.appleextended.script.api.gif.ScriptGifRenderer;
import jp.ngt.ngtlib.io.*;
import jp.ngt.ngtlib.renderer.model.*;
import jp.ngt.ngtlib.util.NGTUtil;
import jp.ngt.rtm.RTMCore;
import jp.ngt.rtm.RTMResource;
import jp.ngt.rtm.block.tt.TimeTableManager;
import jp.ngt.rtm.entity.util.CollisionHelper;
import jp.ngt.rtm.modelpack.cfg.ModelConfig;
import jp.ngt.rtm.modelpack.cfg.RRSConfig;
import jp.ngt.rtm.modelpack.cfg.ResourceConfig;
import jp.ngt.rtm.modelpack.init.ModelPackLoadThread;
import jp.ngt.rtm.modelpack.init.ProgressStateHolder;
import jp.ngt.rtm.modelpack.init.ProgressStateHolder.ProgressState;
import jp.ngt.rtm.modelpack.model.RTMClassModels;
import jp.ngt.rtm.modelpack.modelset.ModelSetBase;
import jp.ngt.rtm.modelpack.modelset.ResourceSet;
import jp.ngt.rtm.network.PacketModelSet;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ModelPackManager {
    public static final ModelPackManager INSTANCE = new ModelPackManager();

    private static final Pattern SC_INCLUDE = Pattern.compile("//include <(.+)>");
    private static final String[] ENCODING = {"UTF-8", "SJIS"};

    private final Map<String, ResourceType> typeMap = new HashMap<>(32);
    public final Map<ResourceType, Map<String, ResourceSet>> allModelSetMap = new HashMap<>(256);
    public final Map<ResourceType, Map<String, ResourceSet>> smpModelSetMap = new HashMap<>(256);
    public final Map<String, ResourceSet> dummyMap = new HashMap<>(32);
    public final ReadWriteLock modelSetMapLock = new ReentrantReadWriteLock();
    public final UnconstructSetsQueue unconstructSets = new UnconstructSetsQueue();

    private final Map<String, IModelNGT> modelCache = new HashMap<>(128);
    private final Map<String, Map<String, ResourceLocation>> resourceCache = new HashMap<>(256);
    private final Map<String, String> scriptCache = new HashMap<>(64);
    public final List<File> fileCache = new ArrayList<>(1024);

    public boolean modelLoaded;
    public volatile boolean modelConstructed;

    private ModelPackManager() {
    }

    public void load(ModelPackLoadThread par1) {
        List<File> fileListTemp = NGTFileLoader.findFile((file) -> {
            String path = file.getAbsolutePath();
            String name = file.getName();
            return !path.contains("block") && !path.contains("item") && !name.endsWith(".json");
        });
        this.fileCache.addAll(fileListTemp);

        NGTLog.debug("[ModelPack] Start searching json");
        par1.setBarValue(ProgressStateHolder.BAR_MAIN, ProgressState.SEARCHING_MODEL);
        par1.setBarMaxValue(ProgressStateHolder.BAR_SUB, 0, "Searching...");

        List<File> fileList = NGTFileLoader.findFile((file) -> {
            String path = file.getAbsolutePath();
            String name = file.getName();
            return !path.contains("block") && !path.contains("item") && name.endsWith(".json");
        });
        NGTLog.debug("[ModelPack] Find %d json", fileList.size());

        NGTLog.debug("[ModelPack] Start registering json");
        par1.setBarValue(ProgressStateHolder.BAR_MAIN, ProgressState.LOADING_MODEL);
        par1.setBarMaxValue(ProgressStateHolder.BAR_SUB, fileList.size(), "");
        int count = 0;
        for (File file : fileList) {
            if (!file.getName().contains("_")) {
                continue;
            }
            String typeName = file.getName().split("_")[0];

            par1.setBarValue(ProgressStateHolder.BAR_SUB, count + 1, file.getName());

            if (!this.typeMap.containsKey(typeName)) {
                continue;
            }
            ResourceType type = this.typeMap.get(typeName);

            try {
                this.registerResourceSet(type, file);
            } catch (ModelPackException e) {
                throw e;
            } catch (Throwable e) {
                throw new ModelPackException("Can't load model", file.getAbsolutePath(), e);
            }
            ++count;
        }
        NGTLog.debug("[ModelPack] Register %d json", count);

        par1.setBarValue(ProgressStateHolder.BAR_MAIN, ProgressState.SEARCHING_RRS);
        par1.setBarMaxValue(ProgressStateHolder.BAR_SUB, 0, "Loading...");
        List<File> fileList2 = NGTFileLoader.findFile((file) -> {
            return file.getName().startsWith("rrs_") && file.getName().endsWith(".png");
        });

        par1.setBarValue(ProgressStateHolder.BAR_MAIN, ProgressState.LOADING_RRS);
        par1.setBarMaxValue(ProgressStateHolder.BAR_SUB, fileList2.size(), "");
        count = 0;
        for (File file2 : fileList2) {
            ++count;
            String s1 = file2.getName();
            par1.setBarValue(1, count, s1);
            RRSConfig rrsconfig = new RRSConfig(s1);
            rrsconfig.file = file2;
            this.registerResourceSet(RTMResource.RRS, rrsconfig, "dummy_str");
        }

        TimeTableManager.INSTANCE.load();

        par1.loadFinished = true;
        this.modelLoaded = true;
    }

    public void registerType(ResourceType type) {
        boolean isSubType = (type.hasSubType && type.subType != null);
        if (isSubType) {
            ResourceSet dummy = this.getNewModelSet(type, new Class[]{});
            this.dummyMap.put(type.subType, dummy);
            this.unconstructSets.add(dummy);
        } else {
            this.typeMap.put(type.name, type);
            this.allModelSetMap.put(type, new HashMap<String, ResourceSet>());
            this.smpModelSetMap.put(type, new HashMap<String, ResourceSet>());

            ResourceSet dummy = this.getNewModelSet(type, new Class[]{});
            this.dummyMap.put(type.name, dummy);
        }
    }

    public ResourceType getType(String name) {
        return this.typeMap.get(name);
    }

    public String registerResourceSet(ResourceType type, File file) throws IOException {
        List<String> json = NGTText.readText(file, null);
        ResourceConfig resourceconfig = (ResourceConfig) NGTJson.getObjectFromJson(UtilsKt.joinLinesForJsonReading(json), type.cfgClass);
        resourceconfig.file = file;
        return this.registerResourceSet(type, resourceconfig, NGTText.append(json, false));
    }

    public String registerResourceSet(ResourceType type, ResourceConfig cfg, String origJson) {
        cfg.init();
        ResourceSet resourceset = this.getNewModelSet(type, new Class[]{type.cfgClass}, cfg);
        if (resourceset != null) {
            if (!MainConfig.reduceConstructModelLog) {
                NGTLog.debug("Registr resource : %s (%s)", cfg.getName(), type.name);
            } else {
                NGTLog.trace("Registr resource : %s (%s)", cfg.getName(), type.name);
            }
            modelSetMapLock.writeLock().lock();
            try {
                this.allModelSetMap.get(type).put(cfg.getName(), resourceset);
            } finally {
                modelSetMapLock.writeLock().unlock();
            }
            this.unconstructSets.add(resourceset);
            resourceset.md5 = EncryptedModel.getMD5(EncryptedModel.formatJson(origJson));
            return cfg.getName();
        } else {
            throw new ModelPackException("Failed to create ResourceSet", cfg.getName());
        }
    }

    public void sendModelSetsToClient(EntityPlayerMP player) {
        int i = 0;
        for (Entry<ResourceType, Map<String, ResourceSet>> entry : this.allModelSetMap.entrySet()) {
            for (ResourceSet resourceset : entry.getValue().values()) {
                ResourceConfig resourceconfig = resourceset.getConfig();
                RTMCore.NETWORK_WRAPPER.sendTo(new PacketModelSet(i, (entry.getKey()).name, resourceconfig.getName()), player);
                NGTLog.trace("[RTM] Send model to client : " + resourceconfig.getName());
                ++i;
            }
        }
        NetworkHandler.sendPacketEPM(new SentAllModels(), player);
    }

    public void addModelSetName(int count, String typeName, String name) {
        assert NGTUtil.isSMP() && !NGTUtil.isServer();
        if (count == 0) {
            for (Map<String, ResourceSet> map : this.smpModelSetMap.values()) {
                map.clear();
            }
        }

        ResourceType type = this.typeMap.get(typeName);
        ResourceSet modelSet = this.allModelSetMap.get(type).get(name);
        if (modelSet != null) {
            this.smpModelSetMap.get(type).put(name, modelSet);
            NGTLog.debug("[RTM] Add model to SMP map : " + name);

            if (modelSet instanceof ModelSetBase) {
                CollisionHelper.INSTANCE.syncCollisionObj(type, (ModelSetBase) modelSet);
            }
        }
    }

    private ResourceSet getNewModelSet(ResourceType type, Class[] parameterTypes, Object... parameters) {
        try {
            Constructor cons0 = type.setClass.getConstructor(parameterTypes);
            return (ResourceSet) cons0.newInstance(parameters);
        } catch (ReflectiveOperationException e) {
            String name = "";
            if (parameters.length > 0) {
                name = ((ResourceConfig) parameters[0]).getName();
            }
            throw new ModelPackException("On construct ModelSet", name, e);
        }
    }

    public <T extends ResourceSet> T getResourceSet(ResourceType type, String name) {
        ResourceType resourcetype = type;
        if (type.parent != null) {
            resourcetype = type.parent;
        }

        boolean flag = NGTUtil.isSMP() && !NGTUtil.isServer();
        Map<ResourceType, Map<String, ResourceSet>> map = flag ? this.smpModelSetMap : this.allModelSetMap;
        T t = (T) map.get(resourcetype).get(name);
        if (t == null) {
            return (T) DummyModelPackManager.getSet(type, name);
        }
        return t;
    }

    public List<ResourceSet> getModelList(ResourceType type) {
        if (type.parent != null) {
            type = type.parent;
        }

        List<ResourceSet> list = new ArrayList<>();
        Map<ResourceType, Map<String, ResourceSet>> map = NGTUtil.isSMP() ? this.smpModelSetMap : this.allModelSetMap;
        list.addAll(map.get(type).values());
        Collections.sort(list, (o1, o2) -> {
            return o1.getConfig().getName().compareTo(o2.getConfig().getName());
        });
        return list;
    }

    @SideOnly(Side.CLIENT)
    public IModelNGT loadModel(String modelName, int drawMode, boolean addModelMap, ModelConfig cfg, byte[] md5) {
        if (addModelMap && this.modelCache.containsKey(modelName)) {
            return this.modelCache.get(modelName);
        }

        VecAccuracy accuracy = (cfg.accuracy == null || cfg.accuracy.equals(VecAccuracy.MEDIUM.toString())) ? VecAccuracy.MEDIUM : VecAccuracy.LOW;
        String resource = "models/" + modelName;
        IModelNGT model = null;

        try {
            if (FileType.CLASS.match(modelName)) {
                model = RTMClassModels.getModel(modelName);
            } else if (FileType.NGTO.match(modelName)) {
                model = new NGTOModel(new ResourceLocationCustom("minecraft", resource), cfg.scale);
            } else if (FileType.NGTZ.match(modelName)) {
                model = new NGTZModel(new ResourceLocationCustom("minecraft", resource), cfg.scale);
            } else {
                model = ModelLoader.loadModel(resource, accuracy, new Object[]{drawMode, md5});
            }
        } catch (ModelFormatException e) {
            throw new ModelFormatException("Can't load model : " + modelName, e);
        }

        if (model == null) {
            throw new ModelPackException("Can't find model file", cfg.getName());
        }

        if (addModelMap) {
            this.modelCache.put(modelName, model);
        }

        return model;
    }

    private static final String DEFAULT_DOMAIN = "minecraft";

    public ResourceLocation getResource(String path) {
        String domain = DEFAULT_DOMAIN;
        if (path.contains(":")) {
            String[] sa = path.split(":");
            domain = sa[0];
            path = sa[1];
        }
        return this.getResource(domain, path);
    }

    public ResourceLocation getResource(String domain, String path) {
        Map<String, ResourceLocation> map = this.resourceCache.get(domain);
        if (map == null) {
            map = new HashMap<>();
            this.resourceCache.put(domain, map);
        } else if (map.containsKey(path)) {
            return map.get(path);
        }

        ResourceLocation resource = new ResourceLocationCustom(domain, path);
        map.put(path, resource);
        return resource;
    }

    public String getScript(String fileName) {
        try {
            return this.loadScript(fileName);
        } catch (IOException e) {
            throw new ModelPackException("Failed to load script", fileName, e);
        }
    }

    private String loadScript(String fileName) throws IOException {
        if (this.scriptCache.containsKey(fileName)) {
            return this.scriptCache.get(fileName);
        }

        String rawScript = NGTText.append(NGTText.readText(this.getResource(fileName)), true);
        while (true) {
            Matcher matcher = SC_INCLUDE.matcher(rawScript);
            if (matcher.find()) {
                String path = matcher.group(1);
                String rep = this.loadScript(path);
                rawScript = matcher.replaceFirst(Matcher.quoteReplacement(rep));
            } else {
                break;
            }
        }

        this.scriptCache.put(fileName, rawScript);
        return rawScript;
    }

    public void clearCache() {
        this.modelCache.clear();
        this.resourceCache.clear();
        this.scriptCache.clear();
        this.fileCache.clear();
        ScriptLoader.clearGroovyCache();
        ScriptGifRenderer.clearCache();
    }
}