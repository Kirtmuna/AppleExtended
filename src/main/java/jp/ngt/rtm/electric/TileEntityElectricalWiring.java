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

package jp.ngt.rtm.electric;

import jp.ngt.ngtlib.block.BlockUtil;
import jp.ngt.ngtlib.block.TileEntityPlaceable;
import jp.ngt.ngtlib.math.Vec3;
import jp.ngt.rtm.RTMCore;
import jp.ngt.rtm.RTMItem;
import jp.ngt.rtm.electric.Connection.ConnectionType;
import jp.ngt.rtm.entity.EntityElectricalWiring;
import jp.ngt.rtm.item.ItemWithModel;
import jp.ngt.rtm.modelpack.state.ResourceState;
import jp.ngt.rtm.network.PacketWire;
import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

public abstract class TileEntityElectricalWiring extends TileEntityPlaceable implements ITickable {
    protected List<Connection> connections = new ArrayList<>();

    public boolean isActivated;
    private int signal;
    private int prevSignal = -1;

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        super.readFromNBT(nbt);

        List<Connection> prevConnections = this.connections;
        this.connections = new ArrayList<>();
        this.connections.addAll(Connection.readListFromNBT(nbt));

        if (this.getWorld() != null && this.getWorld().isRemote) {
            for (Connection connection : this.connections) {
                if (!prevConnections.contains(connection)) {
                    WireManager.INSTANCE.addWire(this, connection);//新規Conのみ登録
                }
                prevConnections.remove(connection);//重複Con削除
            }

            for (Connection connection : prevConnections) {
                WireManager.INSTANCE.removeWire(this, connection);//解除されたConを削除
            }
        }
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
        if (this.isBlockTile()) {
            super.writeToNBT(nbt);
        } else {
            nbt.setInteger("x", this.getX());
            nbt.setInteger("y", this.getY());
            nbt.setInteger("z", this.getZ());
        }
        Connection.writeListToNBT(nbt, this.connections);
        return nbt;
    }

    protected Connection getConnection(int x, int y, int z) {
        for (Connection connection : this.connections) {
            if (connection.x == x && connection.y == y && connection.z == z) {
                return connection;
            }
        }
        return null;
    }

    public List<Connection> getConnnectionList() {
        return this.connections;
    }

    public Vec3 getWirePos() {
        return Vec3.ZERO;
    }

    /**
     * Root側で呼び出し
     */
    public boolean setConnectionTo(int x, int y, int z, ConnectionType type, ResourceState state) {
        boolean flag = false;
        if (type == ConnectionType.NONE)//切断
        {
            Connection c0 = this.getConnection(x, y, z);
            if (c0 != null) {
                this.connections.remove(c0);
                TileEntityElectricalWiring tile = this.getWireTileEntity(x, y, z, c0.type);
                if (tile != null) {
                    tile.setConnectionFrom(this.getX(), this.getY(), this.getZ(), ConnectionType.NONE, null);
                }
                flag = true;
            }
        } else if (type == ConnectionType.TO_PLAYER) {
            this.connections.add(new Connection(true, x, y, z, type, state));
            flag = true;
        } else {
            Block block = BlockUtil.getBlock(this.world, x, y, z);
            if (type == ConnectionType.TO_ENTITY || (block != null && block instanceof IBlockConnective)) {
                TileEntityElectricalWiring tile = this.getWireTileEntity(x, y, z, type);
                ConnectionType type2 = (type == ConnectionType.TO_ENTITY) ? ConnectionType.WIRE : type;
                if (type == ConnectionType.DIRECT || (tile != null && tile.setConnectionFrom(this.getX(), this.getY(), this.getZ(), type2, state))) {
                    this.connections.add(new Connection(true, x, y, z, type, state));
                    flag = true;
                }
            }
        }

        if (!this.world.isRemote && flag) {
            this.markDirty();
            this.sendPacket();
        }
        return flag;
    }

    /**
     * Server Only
     */
    private boolean setConnectionFrom(int x, int y, int z, ConnectionType type, ResourceState state) {
        this.setConnection(x, y, z, type, state);
        if (!this.world.isRemote) {
            this.markDirty();
            this.sendPacket();
        }
        return true;
    }

    /**
     * パケット送信なし, Server Only
     */
    private void setConnection(int x, int y, int z, ConnectionType type, ResourceState state) {
        Connection connection = this.getConnection(x, y, z);
        if (type == ConnectionType.NONE) {
            if (connection != null) {
                this.connections.remove(connection);
            }
        } else {
            if (connection == null) {
                this.connections.add(new Connection(false, x, y, z, type, state));
            }
        }
    }

    /**
     * 信号受信
     */
    public void onGetElectricity(int x, int y, int z, int level, int counter) {
        if (level == 0) {
            if (this.prevSignal < 0) {
                this.prevSignal = 0;
            }
        } else if (level > 0) {
            if (level != this.prevSignal) {
                this.prevSignal = level;
            }
        }
    }

    /**
     * 信号送信
     */
    protected void sendElectricity(Connection connection, int level, int counter) {
        TileEntityElectricalWiring tile = connection.getElectricalWiring(this.world);
        if (tile != null)//counter < 128
        {
            tile.onGetElectricity(this.getX(), this.getY(), this.getZ(), level, ++counter);
        }
    }

    /**
     * 全てに信号送信
     */
    protected void sendElectricityToAll(int level) {
        for (Connection connection : this.connections) {
            if (connection.type != ConnectionType.NONE) {
                this.sendElectricity(connection, level, 0);
            }
        }
    }

    @Override
    public void update() {
        if (this.world.isRemote) {
            if (this.isActivated) {
                Random random = this.world.rand;
                for (int l = 0; l < 3; ++l) {
                    double d1 = ((float) this.getX() + random.nextFloat());
                    double d2 = ((float) this.getY() + random.nextFloat());
                    double d3 = ((float) this.getZ() + random.nextFloat());
                    this.world.spawnParticle(EnumParticleTypes.REDSTONE, d1, d2, d3, 0.0D, 0.0D, 0.0D);
                }
            }
        } else {
            //接続が有効かを確認
            List<Connection> list = new ArrayList<>(this.connections);
            for (Connection connection : list) {
                if (!connection.isAvailable(this.world) || connection.type == ConnectionType.NONE) {
                    this.setConnectionTo(connection.x, connection.y, connection.z, ConnectionType.NONE, null);
                }
            }

            //信号の処理
            if (this.prevSignal >= 0 && this.prevSignal != this.signal) {
                this.signal = this.prevSignal;
                this.sendElectricityToAll(this.signal);
            }
            this.prevSignal = -1;
        }
    }

    /**
     * @return プレーヤーがアイテム電線を持っていない場合は""
     */
    public static ResourceState getWireType(EntityPlayer player) {
        ItemStack itemStack = player.inventory.getCurrentItem();
        if (itemStack != null && itemStack.getItem() == RTMItem.itemWire) {
            return ((ItemWithModel) itemStack.getItem()).getModelState(itemStack);
        }
        return null;
    }

    /**
     * プレーヤー右クリック時
     */
    public boolean onRightClick(EntityPlayer player) {
        boolean flag = false;
        if (this.isActivated) {
            this.isActivated = false;
            this.setConnectionTo(player.getEntityId(), -1, 0, ConnectionType.NONE, null);
            flag = true;
        } else {
            ResourceState wireType = getWireType(player);
            if (wireType == null) {
                if (this.disconnection()) {
                    return true;
                } else {
                    this.isActivated = true;
                    flag = true;
                }
            } else//アイテムを持っている
            {
                if (this.createConnection(player, wireType)) {
                    if (!player.capabilities.isCreativeMode) {
                        player.inventory.getCurrentItem().shrink(1);
                    }
                    //this.setConnectionTo(player.getEntityId(), -1, 0, 0);
                    return true;
                } else {
                    this.isActivated = true;
                    this.setConnectionTo(player.getEntityId(), -1, 0, ConnectionType.TO_PLAYER, wireType);
                    flag = true;
                }
            }
        }

        if (flag) {
            this.sendPacket();
        }
        return flag;
    }

    /**
     * 接続解除
     */
    private boolean disconnection() {
        TileEntityElectricalWiring tile = this.searchActiveTEEW();
        if (tile != null) {
            Connection c0 = this.getConnection(tile.getX(), tile.getY(), tile.getZ());
            if (c0 != null) {
                tile.isActivated = false;
                this.setConnectionTo(tile.getX(), tile.getY(), tile.getZ(), ConnectionType.NONE, null);
                return true;
            }
        }
        return false;
    }

    private boolean createConnection(EntityPlayer player, ResourceState state) {
        TileEntityElectricalWiring tile = this.searchActiveTEEW();

        if (tile != null) {
            Connection c0 = this.getConnection(tile.getX(), tile.getY(), tile.getZ());
            if (c0 == null && state != null) {
                boolean isBlock = !(tile instanceof TileEntityDummyEW);
                ConnectionType type = isBlock ? ConnectionType.WIRE : ConnectionType.TO_ENTITY;
                tile.isActivated = false;
                boolean flag = false;
                if (this instanceof TileEntityDummyEW) {
                    type = ConnectionType.TO_ENTITY;
                    flag = tile.setConnectionTo(this.getX(), this.getY(), this.getZ(), type, state);
                } else {
                    flag = this.setConnectionTo(tile.getX(), tile.getY(), tile.getZ(), type, state);
                }

                if (type != ConnectionType.NONE && flag) {
                    //Playerとの接続解除
                    tile.setConnectionTo(player.getEntityId(), -1, 0, ConnectionType.NONE, null);
                    return true;
                }
            }
        }
        return false;
    }

    private TileEntityElectricalWiring searchActiveTEEW() {
        int x = this.getX();
        int y = this.getY();
        int z = this.getZ();
        int range = RTMCore.connectorSearchRange;

        int dis0 = RTMCore.connectorSearchRange * 2;
        int dis1 = dis0 * dis0;
        List<TileEntity> tileEntityList = this.world.loadedTileEntityList;
        if (tileEntityList != null && !tileEntityList.isEmpty()) {
            TileEntityElectricalWiring teew = tileEntityList.stream()
                    .filter(tile -> tile != this)
                    .filter(TileEntityElectricalWiring.class::isInstance)
                    .map(TileEntityElectricalWiring.class::cast)
                    .filter(tile -> tile.isActivated)
                    .filter(tile -> tile.getDistanceSq(x, y, z) < dis1)
                    .min(Comparator.comparingDouble(o -> o.getDistanceSq(x, y, z)))
                    .orElse(null);
            if (teew != null) {
                return teew;
            }
        }

        List<EntityElectricalWiring> list = this.world.getEntitiesWithinAABB(EntityElectricalWiring.class, new AxisAlignedBB(getPos()).grow(range, range, range));
        if (!list.isEmpty()) {
            return list.stream()
                    .map(entity -> entity.tileEW)
                    .filter(tileEW -> tileEW.isActivated)
                    .min(Comparator.comparingDouble(o -> o.getDistanceSq(x, y, z)))
                    .orElse(null);
        }

        return null;
    }

    protected TileEntityElectricalWiring getWireTileEntity(int x, int y, int z, ConnectionType type) {
        return getWireTileEntity(x, y, z, type, true);
    }

    protected TileEntityElectricalWiring getWireTileEntity(int x, int y, int z, ConnectionType type, boolean par5) {
        if (par5) {
            if (type == ConnectionType.TO_ENTITY) {
                return getWireEntity(this.world, x, y, z);
            }

            Connection connection = this.getConnection(x, y, z);
            if (connection != null && connection.getElectricalWiring(this.world) != null) {
                return connection.getElectricalWiring(this.world);
            }
        }

        TileEntity tile = BlockUtil.getTileEntity(this.world, x, y, z);
        if (tile instanceof TileEntityElectricalWiring)//!=nullは高速化のため
        {
            return (TileEntityElectricalWiring) tile;
        }
        return null;
    }

    public static TileEntityElectricalWiring getWireEntity(World world, int x, int y, int z) {
        List<EntityElectricalWiring> list = world.getEntitiesWithinAABB(EntityElectricalWiring.class,
                new AxisAlignedBB((double) (x), (double) (y), (double) (z), (double) (x + 1), (double) (y + 2), (double) (z + 1)));
        if (!list.isEmpty()) {
            return list.get(0).tileEW;
        }
        return null;
    }

    public boolean isBlockTile() {
        return true;
    }

    /**
     * ブロック破壊時に呼ばれる
     */
    public void onBlockBreaked() {
        for (Connection connection : this.connections) {
            this.sendElectricity(connection, 0, 0);
        }
    }

    @Override
    public void sendPacket() {
        if (this.world == null || !this.world.isRemote) {
            if (this instanceof TileEntityDummyEW) {
                EntityElectricalWiring entity = ((TileEntityDummyEW) this).entityEW;
                RTMCore.NETWORK_WRAPPER.sendToAll(new PacketWire(entity, this));
            } else {
                RTMCore.NETWORK_WRAPPER.sendToAll(new PacketWire(this));
            }
        }
    }

    @Override
    public void setPos(int x, int y, int z, int prevX, int prevY, int prevZ) {
        int difX = x - prevX;
        int difY = y - prevY;
        int difZ = z - prevZ;
        for (Connection connection : this.connections) {
            connection.x += difX;
            connection.y += difY;
            connection.z += difZ;
        }
        super.setPos(x, y, z, prevX, prevY, prevZ);
    }
}