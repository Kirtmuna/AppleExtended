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

package jp.ngt.rtm.entity.train.util;

import jp.ngt.rtm.entity.vehicle.EntityVehicleBase;
import jp.ngt.rtm.modelpack.cfg.TrainConfig;
import jp.ngt.rtm.modelpack.cfg.VehicleBaseConfig;

public enum TrainState {
    Door_Close(TrainStateType.Door, 0, "close"),
    Door_OpenRight(TrainStateType.Door, 1, "open_right"),
    Door_OpenLeft(TrainStateType.Door, 2, "open_left"),
    Door_OpenAll(TrainStateType.Door, 3, "open_all"),

    Light_Off(TrainStateType.Light, 0, "off"),
    Light_Head(TrainStateType.Light, 1, "on_0"),
    Light_Head_Tail(TrainStateType.Light, 2, "on_1"),

    Pantograph_Down(TrainStateType.Pantograph, 0, "down"),
    Pantograph_Up(TrainStateType.Pantograph, 1, "up"),

    Role_Front(TrainStateType.Role, 0, "front"),
    Role_Center(TrainStateType.Role, 1, "center"),
    Role_Back(TrainStateType.Role, 2, "back"),

    InteriorLight_Off(TrainStateType.InteriorLight, 0, "off"),
    InteriorLight_On(TrainStateType.InteriorLight, 1, "on_0"),
    InteriorLight_Rainbow(TrainStateType.InteriorLight, 2, "on_1"),
    ;

    public final TrainStateType type;
    public final byte data;
    public final String stateName;

    private TrainState(TrainStateType par1, int par2, String par3) {
        this.type = par1;
        this.data = (byte) par2;
        this.stateName = par3;
    }

    public static TrainState getState(TrainStateType par1, byte par2Data) {
        for (TrainState state : TrainState.values()) {
            if (state.type == par1 && state.data == par2Data) {
                return state;
            }
        }
        return Door_Close;
    }

    public static TrainStateType getStateType(int par1Id) {
        for (TrainStateType state : TrainStateType.values()) {
            if (state.id == par1Id) {
                return state;
            }
        }
        return TrainStateType.Door;
    }

    public enum TrainStateType {
        Direction(0, "train_dir", 0, 1),
        Notch(1, "notch", -8, 5) {
            @Override
            protected byte getMin0(EntityVehicleBase<?> train) {
                VehicleBaseConfig config = train.getResourceState().getResourceSet().getConfig();
                if (config instanceof TrainConfig)
                    return (byte) -(((TrainConfig) config).deccelerations.length - 1);
                return super.getMin0(train);
            }

            @Override
            protected byte getMax0(EntityVehicleBase<?> train) {
                VehicleBaseConfig config = train.getResourceState().getResourceSet().getConfig();
                if (config instanceof TrainConfig)
                    return (byte) ((TrainConfig) config).accelerateions.length;
                return super.getMax0(train);
            }
        },
        Signal(2, "signal", 0, 127),
        Door(4, "door", 0, 3),
        Light(5, "light", 0, 2),
        Pantograph(6, "pantograph", 0, 1),
        ChunkLoader(7, "chunk_loader", 0, 8),
        Destination(8, "destination", 0, 127),
        Announcement(9, "announcement", 0, 127),
        Role(10, "direction", 0, 2),
        InteriorLight(11, "interior_light", 0, 2);

        public final int id;
        public final String stateName;
        public final byte min;
        public final byte max;

        private TrainStateType(int par1, String par2, int par3, int par4) {
            this.id = par1;
            this.stateName = par2;
            this.min = (byte) par3;
            this.max = (byte) par4;
        }

        public static TrainStateType get(int id) {
            for (TrainStateType type : TrainStateType.values()) {
                if (type.id == id) {
                    return type;
                }
            }
            return Direction;
        }

        public byte clap(byte value, EntityVehicleBase<?> train) {
            byte min = getMin(train);
            byte max = getMax(train);
            return value < min ? min
                    : value > max ? max
                    : value;
        }

        public byte getMax(EntityVehicleBase<?> train) {
            try {
                return getMax0(train);
            } catch (Throwable t) {
                t.printStackTrace();
                return max;
            }
        }

        protected byte getMax0(EntityVehicleBase<?> train) {
            return max;
        }

        public byte getMin(EntityVehicleBase<?> train) {
            try {
                return getMin0(train);
            } catch (Throwable t) {
                t.printStackTrace();
                return min;
            }
        }

        protected byte getMin0(EntityVehicleBase<?> train) {
            return min;
        }
    }
}