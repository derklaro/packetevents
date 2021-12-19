/*
 * This file is part of packetevents - https://github.com/retrooper/packetevents
 * Copyright (C) 2021 retrooper and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.github.retrooper.packetevents.protocol.entity.data;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.manager.server.ServerVersion;
import com.github.retrooper.packetevents.protocol.chat.component.BaseComponent;
import com.github.retrooper.packetevents.protocol.entity.pose.EntityPose;
import com.github.retrooper.packetevents.protocol.item.ItemStack;
import com.github.retrooper.packetevents.protocol.nbt.NBTCompound;
import com.github.retrooper.packetevents.protocol.world.BlockFace;
import com.github.retrooper.packetevents.util.MappingHelper;
import com.github.retrooper.packetevents.util.Vector3f;
import com.github.retrooper.packetevents.util.Vector3i;
import com.github.retrooper.packetevents.wrapper.PacketWrapper;
import com.google.gson.JsonObject;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Function;

public class EntityDataTypes {
    private static JsonObject MAPPINGS;
    //1.7 -> 1.18 block_position is just 3 ints, not serialized with a long
    //short was removed in 1.9+
    //boolean was added in 1.9
    //nbt was added in 1.12

    private static final Map<String, EntityDataType<?>> ENTITY_DATA_TYPE_MAP = new HashMap<>();
    private static final Map<Integer, EntityDataType<?>> ENTITY_DATA_TYPE_ID_MAP = new HashMap<>();

    public static final EntityDataType<Byte> BYTE = define("byte", PacketWrapper::readByte, PacketWrapper::writeByte);
    public static final EntityDataType<Integer> INT = define("int", readIntDeserializer(), writeIntSerializer());
    public static final EntityDataType<Float> FLOAT = define("float", PacketWrapper::readFloat, PacketWrapper::writeFloat);
    public static final EntityDataType<String> STRING = define("string", PacketWrapper::readString, PacketWrapper::writeString);
    public static final EntityDataType<BaseComponent> COMPONENT = define("component", PacketWrapper::readComponent, PacketWrapper::writeComponent);
    public static final EntityDataType<Optional<BaseComponent>> OPTIONAL_COMPONENT = define("optional_component", readOptionalComponentDeserializer(), writeOptionalComponentSerializer());
    public static final EntityDataType<ItemStack> ITEMSTACK = define("itemstack", PacketWrapper::readItemStack, PacketWrapper::writeItemStack);
    //TODO Complete block states
    public static final EntityDataType<Optional<Object>> OPTIONAL_BLOCK_STATE = define("optional_block_state", readIntDeserializer(), writeIntSerializer());
    public static final EntityDataType<Boolean> BOOLEAN = define("boolean", PacketWrapper::readBoolean, PacketWrapper::writeBoolean);
    //TODO Complete particles
    public static final EntityDataType<Integer> PARTICLE = define("particle", PacketWrapper::readVarInt, PacketWrapper::writeVarInt);
    public static final EntityDataType<Vector3f> ROTATION = define("rotation",
            (PacketWrapper<?> wrapper) -> new Vector3f(wrapper.readFloat(), wrapper.readFloat(), wrapper.readFloat()),
            (PacketWrapper<?> wrapper, Vector3f value) -> {
                wrapper.writeFloat(value.x);
                wrapper.writeFloat(value.y);
                wrapper.writeFloat(value.z);
            });
    public static final EntityDataType<Vector3i> BLOCK_POSITION = define("block_position", readBlockPositionDeserializer(), writeBlockPositionSerializer());
    public static final EntityDataType<Optional<Vector3i>> OPTIONAL_BLOCK_POSITION = define("optional_block_position", readOptionalBlockPositionDeserializer(), writeOptionalBlockPositionSerializer());
    public static final EntityDataType<BlockFace> BLOCK_FACE = define("block_face", (PacketWrapper<?> wrapper) -> {
                int id = wrapper.readVarInt();
                return BlockFace.getBlockFaceByValue(id);
            },
            (PacketWrapper<?> wrapper, BlockFace value) -> wrapper.writeVarInt(value.getFaceValue()));

    public static final EntityDataType<Optional<UUID>> OPTIONAL_UUID = define("optional_uuid", (PacketWrapper<?> wrapper) -> {
                if (wrapper.readBoolean()) {
                    return Optional.of(wrapper.readUUID());
                } else {
                    return Optional.empty();
                }
            },
            (PacketWrapper<?> wrapper, Optional<UUID> value) -> {
                if (value.isPresent()) {
                    wrapper.writeBoolean(true);
                    wrapper.writeUUID(value.get());
                } else {
                    wrapper.writeBoolean(false);
                }
            });

    public static final EntityDataType<NBTCompound> NBT = define("nbt", PacketWrapper::readNBT, PacketWrapper::writeNBT);
    //TODO Complete villager data, its readint 3 var ints
    public static final EntityDataType<Integer> VILLAGER_DATA = define("villager_data", PacketWrapper::readVarInt, PacketWrapper::writeVarInt);
    public static final EntityDataType<Optional<Integer>> OPTIONAL_INT = define("optional_int", (PacketWrapper<?> wrapper) -> {
        if (wrapper.readBoolean()) {
            return Optional.of(wrapper.readVarInt());
        } else {
            return Optional.empty();
        }
    }, (PacketWrapper<?> wrapper, Optional<Integer> value) -> {
        if (value.isPresent()) {
            wrapper.writeBoolean(true);
            wrapper.writeVarInt(value.get());
        } else {
            wrapper.writeBoolean(false);
        }
    });

    public static final EntityDataType<EntityPose> ENTITY_POSE = define("entity_pose", (PacketWrapper<?> wrapper) -> {
        int id = wrapper.readVarInt();
        return EntityPose.getById(id);
    }, (PacketWrapper<?> wrapper, EntityPose value) -> wrapper.writeVarInt(value.getId()));

    @Deprecated
    public static final EntityDataType<Short> SHORT = define("short", PacketWrapper::readShort, PacketWrapper::writeShort);

    private static ServerVersion getMappingsServerVersion(ServerVersion serverVersion) {
        if (serverVersion.isOlderThan(ServerVersion.V_1_9)) {
            return ServerVersion.V_1_8;
        } else if (serverVersion.isOlderThan(ServerVersion.V_1_11)) {
            return ServerVersion.V_1_9;
        } else if (serverVersion.isOlderThan(ServerVersion.V_1_13)) {
            return ServerVersion.V_1_11;
        } else {
            return ServerVersion.V_1_16;
        }
    }

    public static EntityDataType<?> getById(int id) {
        return ENTITY_DATA_TYPE_ID_MAP.get(id);
    }

    public static EntityDataType<?> getByName(String name) {
        return ENTITY_DATA_TYPE_MAP.get(name);
    }

    public static <T> EntityDataType<T> define(String name, Function<PacketWrapper<?>, T> deserializer, BiConsumer<PacketWrapper<?>, T> serializer) {
        if (MAPPINGS == null) {
            MAPPINGS = MappingHelper.getJSONObject("entity/entity_data_type_mappings");
        }
        ServerVersion mappingsVersion = getMappingsServerVersion(PacketEvents.getAPI().getServerManager().getVersion());
        final int id;

        if (MAPPINGS.has(mappingsVersion.name())) {
            JsonObject map = MAPPINGS.getAsJsonObject(mappingsVersion.name());
            if (map.has(name)) {
                id = map.get(name).getAsInt();
            } else {
                id = -1;
            }
        } else {
            throw new IllegalStateException("Failed to find EntityDataType mappings for the " + mappingsVersion.name() + " mappings version!");

        }
        EntityDataType<T> type = new EntityDataType<T>(name, id, deserializer, (BiConsumer<PacketWrapper<?>, Object>) serializer);
        ENTITY_DATA_TYPE_MAP.put(type.getName(), type);
        ENTITY_DATA_TYPE_ID_MAP.put(type.getId(), type);
        return type;
    }

    private static <T> Function<PacketWrapper<?>, T> readIntDeserializer() {
        if (PacketEvents.getAPI().getServerManager().getVersion().isNewerThanOrEquals(ServerVersion.V_1_9)) {
            return (PacketWrapper<?> wrapper) -> (T) ((Object) wrapper.readVarInt());
        } else {
            return (PacketWrapper<?> wrapper) -> (T) ((Object) wrapper.readInt());
        }
    }

    private static <T> BiConsumer<PacketWrapper<?>, T> writeIntSerializer() {
        if (PacketEvents.getAPI().getServerManager().getVersion().isNewerThanOrEquals(ServerVersion.V_1_9)) {
            return (PacketWrapper<?> wrapper, T value) -> {
                wrapper.writeVarInt((int) value);
            };
        } else {
            return (PacketWrapper<?> wrapper, T value) -> {
                wrapper.writeInt((int) value);
            };
        }
    }

    private static <T> Function<PacketWrapper<?>, T> readOptionalComponentDeserializer() {
        return (PacketWrapper<?> wrapper) -> {
            if (wrapper.readBoolean()) {
                return (T) Optional.of(wrapper.readComponent());
            } else {
                return (T) Optional.empty();
            }
        };
    }

    private static <T> BiConsumer<PacketWrapper<?>, T> writeOptionalComponentSerializer() {
        return (PacketWrapper<?> wrapper, T value) -> {
            if (value instanceof Optional) {
                Optional<?> optional = (Optional<?>) value;
                if (optional.isPresent()) {
                    wrapper.writeBoolean(true);
                    wrapper.writeComponent((BaseComponent) optional.get());
                } else {
                    wrapper.writeBoolean(false);
                }
            } else {
                wrapper.writeBoolean(false);
            }
        };
    }

    private static <T> Function<PacketWrapper<?>, T> readBlockPositionDeserializer() {
        if (PacketEvents.getAPI().getServerManager().getVersion().isNewerThanOrEquals(ServerVersion.V_1_9)) {
            return (PacketWrapper<?> wrapper) -> (T) wrapper.readBlockPosition();
        } else {
            return (PacketWrapper<?> wrapper) -> {
                int x = wrapper.readInt();
                int y = wrapper.readInt();
                int z = wrapper.readInt();
                return (T) new Vector3i(x, y, z);
            };
        }
    }

    private static <T> BiConsumer<PacketWrapper<?>, T> writeBlockPositionSerializer() {
        if (PacketEvents.getAPI().getServerManager().getVersion().isNewerThanOrEquals(ServerVersion.V_1_9)) {
            return (PacketWrapper<?> wrapper, T value) -> {
                wrapper.writeBlockPosition((Vector3i) value);
            };
        } else {
            return (PacketWrapper<?> wrapper, T value) -> {
                Vector3i position = (Vector3i) value;
                wrapper.writeInt(position.getX());
                wrapper.writeInt(position.getY());
                wrapper.writeInt(position.getZ());
            };
        }
    }

    private static <T> Function<PacketWrapper<?>, T> readOptionalBlockPositionDeserializer() {
        if (PacketEvents.getAPI().getServerManager().getVersion().isNewerThanOrEquals(ServerVersion.V_1_9)) {
            return (PacketWrapper<?> wrapper) -> {
                if (wrapper.readBoolean()) {
                    return (T) Optional.of(wrapper.readBlockPosition());
                } else {
                    return (T) Optional.empty();
                }
            };
        } else {
            return (PacketWrapper<?> wrapper) -> {
                if (wrapper.readBoolean()) {
                    int x = wrapper.readInt();
                    int y = wrapper.readInt();
                    int z = wrapper.readInt();
                    return (T) Optional.of(new Vector3i(x, y, z));
                } else {
                    return (T) Optional.empty();
                }
            };
        }
    }

    private static <T> BiConsumer<PacketWrapper<?>, T> writeOptionalBlockPositionSerializer() {
        if (PacketEvents.getAPI().getServerManager().getVersion().isNewerThanOrEquals(ServerVersion.V_1_9)) {
            return (PacketWrapper<?> wrapper, T value) -> {
                if (value instanceof Optional) {
                    Optional<?> optional = (Optional<?>) value;
                    if (optional.isPresent()) {
                        wrapper.writeBoolean(true);
                        wrapper.writeBlockPosition((Vector3i) optional.get());
                    } else {
                        wrapper.writeBoolean(false);
                    }
                } else {
                    wrapper.writeBoolean(false);
                }
            };
        } else {
            return (PacketWrapper<?> wrapper, T value) -> {
                if (value instanceof Optional) {
                    Optional<?> optional = (Optional<?>) value;
                    if (optional.isPresent()) {
                        wrapper.writeBoolean(true);
                        Vector3i position = (Vector3i) optional.get();
                        wrapper.writeInt(position.getX());
                        wrapper.writeInt(position.getY());
                        wrapper.writeInt(position.getZ());
                    } else {
                        wrapper.writeBoolean(false);
                    }
                } else {
                    wrapper.writeBoolean(false);
                }
            };
        }
    }
}
