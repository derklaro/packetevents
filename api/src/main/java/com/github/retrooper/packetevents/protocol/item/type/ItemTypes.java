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

package com.github.retrooper.packetevents.protocol.item.type;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.manager.server.ServerVersion;
import com.github.retrooper.packetevents.protocol.chat.component.serializer.ComponentSerializer;
import com.github.retrooper.packetevents.protocol.resources.ResourceLocation;
import com.github.retrooper.packetevents.util.MappingHelper;
import com.google.gson.JsonObject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.*;

public class ItemTypes {
    private static final Map<String, ItemType> ITEM_TYPE_MAP = new HashMap<>();
    private static final Map<Integer, ItemType> ITEM_TYPE_ID_MAP = new HashMap<>();
    private static JsonObject MAPPINGS;

    private enum ItemAttribute {
        //TODO Add more
        MUSIC_DISC, EDIBLE, FIRE_RESISTANT, WOOD_TIER, STONE_TIER, IRON_TIER, DIAMOND_TIER, GOLD_TIER, NETHERITE_TIER;
    }

    private static ServerVersion getMappingServerVersion(ServerVersion serverVersion) {
        if (serverVersion.isOlderThan(ServerVersion.V_1_13)) {
            return ServerVersion.V_1_12;
        } else if (serverVersion.isOlderThan(ServerVersion.V_1_13_2)) {
            return ServerVersion.V_1_13;
        } else if (serverVersion.isOlderThan(ServerVersion.V_1_14)) {
            return ServerVersion.V_1_13_2;
        } else if (serverVersion.isOlderThan(ServerVersion.V_1_15)) {
            return ServerVersion.V_1_14;
        } else if (serverVersion.isOlderThan(ServerVersion.V_1_16)) {
            return ServerVersion.V_1_15;
        } else if (serverVersion.isOlderThan(ServerVersion.V_1_17)) {
            return ServerVersion.V_1_16;
        } else if (serverVersion.isOlderThan(ServerVersion.V_1_18)) {
            return ServerVersion.V_1_17;
        } else {
            return ServerVersion.V_1_18;
        }
    }

    public static ItemType define(int maxAmount, String key, ItemAttribute... attributesArr) {
        return define(maxAmount, key, null, 0, attributesArr);
    }

    public static ItemType define(int maxAmount, String key, int maxDurability) {
        return define(maxAmount, key, null, maxDurability);
    }

    public static ItemType define(int maxAmount, String key, ItemType craftRemainder, ItemAttribute... attributesArr) {
        return define(maxAmount, key, craftRemainder, 0, attributesArr);
    }

    private static ItemType define(int maxAmount, String key, int maxDurability, ItemAttribute... attributesArr) {
        return define(maxAmount, key, null, maxDurability, attributesArr);
    }

    public static ItemType define(int maxAmount, String key, ItemType craftRemainder, int maxDurability, ItemAttribute... attributesArr) {
        if (MAPPINGS == null) {
            MAPPINGS = MappingHelper.getJSONObject("item/item_type_mappings");
        }
        Set<ItemAttribute> attributes = new HashSet<>(Arrays.asList(attributesArr));

        ResourceLocation identifier = ResourceLocation.minecraft(key);

        final int id;
        ServerVersion mappingsVersion = getMappingServerVersion(PacketEvents.getAPI().getServerManager().getVersion());
        if (MAPPINGS.has(mappingsVersion.name())) {
            JsonObject map = MAPPINGS.getAsJsonObject(mappingsVersion.name());
            if (map.has(identifier.toString())) {
                id = map.get(identifier.toString()).getAsInt();
            } else {
                id = -1;
            }
        } else {
            throw new IllegalStateException("Failed to find ItemType mappings for the " + mappingsVersion.name() + " mappings version!");
        }

        boolean musicDisc = attributes.contains(ItemAttribute.MUSIC_DISC);


        ItemType type = new ItemType() {
            @Override
            public int getMaxAmount() {
                return maxAmount;
            }

            @Override
            public int getMaxDurability() {
                return maxDurability;
            }

            @Override
            public ResourceLocation getIdentifier() {
                return identifier;
            }

            @Override
            public int getId() {
                return id;
            }

            @Override
            public boolean isMusicDisc() {
                return musicDisc;
            }

            @Override
            public ItemType getCraftRemainder() {
                return craftRemainder;
            }

            @Override
            public boolean equals(Object obj) {
                if (obj instanceof ItemType) {
                    return getId() == ((ItemType) obj).getId();
                }
                return false;
            }
        };
        ITEM_TYPE_MAP.put(type.getIdentifier().getKey(), type);
        ITEM_TYPE_ID_MAP.put(type.getId(), type);
        return type;
    }

    @Nullable
    public static ItemType getByKey(String key) {
        return ITEM_TYPE_MAP.get(key);
    }

    @NotNull
    public static ItemType getById(int id) {
        ItemType cache = ITEM_TYPE_ID_MAP.get(id);
        if (cache == null) {
            cache = ItemTypes.AIR;
        }
        return cache;
    }

    private static String paste(String content) throws IOException {
        URL url = new URL("https://www.toptal.com/developers/hastebin/documents");
        URLConnection con = url.openConnection();
        con.addRequestProperty("User-Agent", "Mozilla/4.0");
        HttpURLConnection http = (HttpURLConnection) con;
        http.setRequestMethod("POST");
        http.setDoOutput(true);

        try (OutputStream out = http.getOutputStream()) {
            out.write(content.getBytes());
            out.flush();
        }

        InputStream in = new BufferedInputStream(http.getInputStream());
        BufferedReader reader = new BufferedReader(new InputStreamReader(in));

        StringBuilder entirePage = new StringBuilder();
        String inputLine;
        while ((inputLine = reader.readLine()) != null) {
            entirePage.append(inputLine);
        }
        reader.close();
        JsonObject jsonObj = ComponentSerializer.GSON.fromJson(entirePage.toString(), JsonObject.class);
        String keyValue = jsonObj.get("key").getAsString();
        return "https://www.toptal.com/developers/hastebin/raw/" + keyValue;
    }


    static {
        /*

        String content = "";

        for (Object keyObj : modernItemTypesJSONObject.keySet()) {
            String itemTypeKey = (String) keyObj;
            Long keyID = (Long) modernItemTypesJSONObject.get(itemTypeKey);
            //LogManager.debug("key id: " + keyID + ", item key: " + itemTypeKey);
            String line = "public static final ItemType " + itemTypeKey.toUpperCase() + " = define(64, \"" + itemTypeKey + "\");";
            content += line;
        }
        String finalContent = content;
        new Thread(() -> {
            LogManager.debug("LOADING!");
            String pageName = null;
            try {
                pageName = paste(finalContent);
            } catch (IOException | ParseException e) {
                e.printStackTrace();
            }
            LogManager.debug("page name: " + pageName);
        }).start();
         */

    }

    // TODO: Add rarity, craft remainder, durability, is food, food use duration
    public static final ItemType GILDED_BLACKSTONE = define(64, "gilded_blackstone");
    public static final ItemType NETHER_BRICK_SLAB = define(64, "nether_brick_slab");
    public static final ItemType ANDESITE_SLAB = define(64, "andesite_slab");
    public static final ItemType EGG = define(16, "egg");
    public static final ItemType MUSIC_DISC_STAL = define(1, "music_disc_stal", ItemAttribute.MUSIC_DISC);
    public static final ItemType PIGLIN_BRUTE_SPAWN_EGG = define(64, "piglin_brute_spawn_egg");
    public static final ItemType BIRCH_STAIRS = define(64, "birch_stairs");
    public static final ItemType SPRUCE_SIGN = define(16, "spruce_sign");
    public static final ItemType DRAGON_HEAD = define(64, "dragon_head");
    public static final ItemType HONEY_BLOCK = define(64, "honey_block");
    public static final ItemType GREEN_DYE = define(64, "green_dye");
    public static final ItemType DIAMOND_ORE = define(64, "diamond_ore");
    public static final ItemType DEBUG_STICK = define(1, "debug_stick");
    public static final ItemType BLACK_STAINED_GLASS_PANE = define(64, "black_stained_glass_pane");
    public static final ItemType SPRUCE_FENCE_GATE = define(64, "spruce_fence_gate");
    public static final ItemType AZURE_BLUET = define(64, "azure_bluet");
    public static final ItemType SLIME_BALL = define(64, "slime_ball");
    public static final ItemType RABBIT = define(64, "rabbit", ItemAttribute.EDIBLE);
    public static final ItemType AMETHYST_CLUSTER = define(64, "amethyst_cluster");
    public static final ItemType PRISMARINE_BRICK_SLAB = define(64, "prismarine_brick_slab");
    public static final ItemType DRAGON_EGG = define(64, "dragon_egg");
    public static final ItemType PARROT_SPAWN_EGG = define(64, "parrot_spawn_egg");
    public static final ItemType WEATHERED_CUT_COPPER_SLAB = define(64, "weathered_cut_copper_slab");
    public static final ItemType LIGHT_GRAY_STAINED_GLASS_PANE = define(64, "light_gray_stained_glass_pane");
    public static final ItemType SCAFFOLDING = define(64, "scaffolding");
    public static final ItemType WARPED_PRESSURE_PLATE = define(64, "warped_pressure_plate");
    public static final ItemType MULE_SPAWN_EGG = define(64, "mule_spawn_egg");
    public static final ItemType SUSPICIOUS_STEW = define(1, "suspicious_stew", ItemAttribute.EDIBLE);
    public static final ItemType MAGENTA_STAINED_GLASS_PANE = define(64, "magenta_stained_glass_pane");
    public static final ItemType LARGE_FERN = define(64, "large_fern");
    public static final ItemType LIGHT_BLUE_CONCRETE = define(64, "light_blue_concrete");
    public static final ItemType LAPIS_ORE = define(64, "lapis_ore");
    public static final ItemType LIGHT_BLUE_BED = define(1, "light_blue_bed");
    public static final ItemType BIRCH_PRESSURE_PLATE = define(64, "birch_pressure_plate");
    public static final ItemType HONEYCOMB = define(64, "honeycomb");
    public static final ItemType GOLD_BLOCK = define(64, "gold_block");
    public static final ItemType WRITABLE_BOOK = define(1, "writable_book");
    public static final ItemType DRIPSTONE_BLOCK = define(64, "dripstone_block");
    public static final ItemType ACACIA_LOG = define(64, "acacia_log");
    public static final ItemType TROPICAL_FISH_SPAWN_EGG = define(64, "tropical_fish_spawn_egg");
    public static final ItemType ZOMBIE_SPAWN_EGG = define(64, "zombie_spawn_egg");
    public static final ItemType GLOW_ITEM_FRAME = define(64, "glow_item_frame");
    public static final ItemType WHITE_DYE = define(64, "white_dye");
    public static final ItemType REDSTONE = define(64, "redstone");
    public static final ItemType BONE_BLOCK = define(64, "bone_block");
    public static final ItemType DEAD_TUBE_CORAL_FAN = define(64, "dead_tube_coral_fan");
    public static final ItemType TURTLE_SPAWN_EGG = define(64, "turtle_spawn_egg");
    public static final ItemType MILK_BUCKET = define(1, "milk_bucket", ItemTypes.BUCKET);
    public static final ItemType BIRCH_FENCE = define(64, "birch_fence");
    public static final ItemType CYAN_TERRACOTTA = define(64, "cyan_terracotta");
    public static final ItemType PRISMARINE_STAIRS = define(64, "prismarine_stairs");
    public static final ItemType IRON_BOOTS = define(1, "iron_boots", 195);
    public static final ItemType BROWN_CONCRETE_POWDER = define(64, "brown_concrete_powder");
    public static final ItemType END_STONE = define(64, "end_stone");
    public static final ItemType GLISTERING_MELON_SLICE = define(64, "glistering_melon_slice");
    public static final ItemType NETHER_SPROUTS = define(64, "nether_sprouts");
    public static final ItemType GREEN_CONCRETE = define(64, "green_concrete");
    public static final ItemType ACACIA_DOOR = define(64, "acacia_door");
    public static final ItemType GOLDEN_AXE = define(1, "golden_axe", 32, ItemAttribute.GOLD_TIER);
    public static final ItemType WHITE_STAINED_GLASS_PANE = define(64, "white_stained_glass_pane");
    public static final ItemType COBBLESTONE_WALL = define(64, "cobblestone_wall");
    public static final ItemType WHITE_GLAZED_TERRACOTTA = define(64, "white_glazed_terracotta");
    public static final ItemType END_STONE_BRICK_WALL = define(64, "end_stone_brick_wall");
    public static final ItemType COOKED_RABBIT = define(64, "cooked_rabbit", ItemAttribute.EDIBLE);
    public static final ItemType RED_MUSHROOM_BLOCK = define(64, "red_mushroom_block");
    public static final ItemType CRIMSON_SLAB = define(64, "crimson_slab");
    public static final ItemType AMETHYST_SHARD = define(64, "amethyst_shard");
    public static final ItemType CHARCOAL = define(64, "charcoal");
    public static final ItemType NETHER_WART_BLOCK = define(64, "nether_wart_block");
    public static final ItemType DEEPSLATE_GOLD_ORE = define(64, "deepslate_gold_ore");
    public static final ItemType INFESTED_STONE = define(64, "infested_stone");
    public static final ItemType STRIPPED_OAK_LOG = define(64, "stripped_oak_log");
    public static final ItemType LIGHT_GRAY_CONCRETE_POWDER = define(64, "light_gray_concrete_powder");
    public static final ItemType COOKED_PORKCHOP = define(64, "cooked_porkchop", ItemAttribute.EDIBLE);
    public static final ItemType NETHERITE_HELMET = define(1, "netherite_helmet", null, 407, ItemAttribute.FIRE_RESISTANT);
    public static final ItemType BLACK_CANDLE = define(64, "black_candle");
    public static final ItemType CYAN_CONCRETE_POWDER = define(64, "cyan_concrete_powder");
    public static final ItemType SADDLE = define(1, "saddle");
    public static final ItemType OAK_SIGN = define(16, "oak_sign");
    public static final ItemType REDSTONE_ORE = define(64, "redstone_ore");
    public static final ItemType NETHER_GOLD_ORE = define(64, "nether_gold_ore");
    public static final ItemType HORN_CORAL_FAN = define(64, "horn_coral_fan");
    public static final ItemType STRIPPED_WARPED_HYPHAE = define(64, "stripped_warped_hyphae");
    public static final ItemType COOKED_BEEF = define(64, "cooked_beef", ItemAttribute.EDIBLE);
    public static final ItemType DEEPSLATE_EMERALD_ORE = define(64, "deepslate_emerald_ore");
    public static final ItemType FARMLAND = define(64, "farmland");
    public static final ItemType BLACK_CONCRETE = define(64, "black_concrete");
    public static final ItemType CHISELED_DEEPSLATE = define(64, "chiseled_deepslate");
    public static final ItemType RED_WOOL = define(64, "red_wool");
    public static final ItemType WAXED_CUT_COPPER_SLAB = define(64, "waxed_cut_copper_slab");
    public static final ItemType BLACK_WOOL = define(64, "black_wool");
    public static final ItemType GOLD_INGOT = define(64, "gold_ingot");
    public static final ItemType CRACKED_DEEPSLATE_BRICKS = define(64, "cracked_deepslate_bricks");
    public static final ItemType STONE_BUTTON = define(64, "stone_button");
    public static final ItemType MELON = define(64, "melon");
    public static final ItemType INFESTED_CHISELED_STONE_BRICKS = define(64, "infested_chiseled_stone_bricks");
    public static final ItemType MUSIC_DISC_STRAD = define(1, "music_disc_strad", ItemAttribute.MUSIC_DISC);
    public static final ItemType STRUCTURE_BLOCK = define(64, "structure_block");
    public static final ItemType STICKY_PISTON = define(64, "sticky_piston");
    public static final ItemType GRAY_STAINED_GLASS = define(64, "gray_stained_glass");
    public static final ItemType LIGHT_GRAY_SHULKER_BOX = define(1, "light_gray_shulker_box");
    public static final ItemType DARK_OAK_BUTTON = define(64, "dark_oak_button");
    public static final ItemType NETHERITE_AXE = define(1, "netherite_axe", 2031, ItemAttribute.FIRE_RESISTANT, ItemAttribute.NETHERITE_TIER);
    public static final ItemType SAND = define(64, "sand");
    public static final ItemType POLISHED_GRANITE_SLAB = define(64, "polished_granite_slab");
    public static final ItemType DARK_OAK_DOOR = define(64, "dark_oak_door");
    public static final ItemType MOJANG_BANNER_PATTERN = define(1, "mojang_banner_pattern");
    public static final ItemType BEACON = define(64, "beacon");
    public static final ItemType BIRCH_WOOD = define(64, "birch_wood");
    public static final ItemType MUSHROOM_STEW = define(1, "mushroom_stew", ItemAttribute.EDIBLE);
    public static final ItemType FLINT = define(64, "flint");
    public static final ItemType SMOOTH_SANDSTONE_SLAB = define(64, "smooth_sandstone_slab");
    public static final ItemType WARPED_PLANKS = define(64, "warped_planks");
    public static final ItemType MUSHROOM_STEM = define(64, "mushroom_stem");
    public static final ItemType EMERALD = define(64, "emerald");
    public static final ItemType BLACKSTONE = define(64, "blackstone");
    public static final ItemType HOGLIN_SPAWN_EGG = define(64, "hoglin_spawn_egg");
    public static final ItemType DEAD_BRAIN_CORAL_BLOCK = define(64, "dead_brain_coral_block");
    public static final ItemType OXIDIZED_COPPER = define(64, "oxidized_copper");
    public static final ItemType SHULKER_SPAWN_EGG = define(64, "shulker_spawn_egg");
    public static final ItemType BEEHIVE = define(64, "beehive");
    public static final ItemType POLISHED_BASALT = define(64, "polished_basalt");
    public static final ItemType PURPLE_WOOL = define(64, "purple_wool");
    public static final ItemType PINK_GLAZED_TERRACOTTA = define(64, "pink_glazed_terracotta");
    public static final ItemType CHORUS_FLOWER = define(64, "chorus_flower");
    public static final ItemType LILAC = define(64, "lilac");
    public static final ItemType CRACKED_DEEPSLATE_TILES = define(64, "cracked_deepslate_tiles");
    public static final ItemType SHEEP_SPAWN_EGG = define(64, "sheep_spawn_egg");
    public static final ItemType SMALL_DRIPLEAF = define(64, "small_dripleaf");
    public static final ItemType SOUL_TORCH = define(64, "soul_torch");
    public static final ItemType POLISHED_BLACKSTONE_BRICK_STAIRS = define(64, "polished_blackstone_brick_stairs");
    public static final ItemType SPRUCE_FENCE = define(64, "spruce_fence");
    public static final ItemType COAL_BLOCK = define(64, "coal_block");
    public static final ItemType STRIPPED_CRIMSON_HYPHAE = define(64, "stripped_crimson_hyphae");
    public static final ItemType WOODEN_PICKAXE = define(1, "wooden_pickaxe", 59, ItemAttribute.WOOD_TIER);
    public static final ItemType BIRCH_LEAVES = define(64, "birch_leaves");
    public static final ItemType DIAMOND_PICKAXE = define(1, "diamond_pickaxe", 1561, ItemAttribute.DIAMOND_TIER);
    public static final ItemType FLOWER_POT = define(64, "flower_pot");
    public static final ItemType ACACIA_BUTTON = define(64, "acacia_button");
    public static final ItemType STRIPPED_DARK_OAK_WOOD = define(64, "stripped_dark_oak_wood");
    public static final ItemType PINK_TERRACOTTA = define(64, "pink_terracotta");
    public static final ItemType PURPLE_CANDLE = define(64, "purple_candle");
    public static final ItemType MAGENTA_TERRACOTTA = define(64, "magenta_terracotta");
    public static final ItemType DEEPSLATE_COPPER_ORE = define(64, "deepslate_copper_ore");
    public static final ItemType GRAY_DYE = define(64, "gray_dye");
    public static final ItemType BLACK_SHULKER_BOX = define(1, "black_shulker_box");
    public static final ItemType OCELOT_SPAWN_EGG = define(64, "ocelot_spawn_egg");
    public static final ItemType WAXED_EXPOSED_CUT_COPPER_STAIRS = define(64, "waxed_exposed_cut_copper_stairs");
    public static final ItemType POLISHED_BLACKSTONE_WALL = define(64, "polished_blackstone_wall");
    public static final ItemType BRAIN_CORAL_FAN = define(64, "brain_coral_fan");
    public static final ItemType RED_NETHER_BRICK_SLAB = define(64, "red_nether_brick_slab");
    public static final ItemType SUGAR_CANE = define(64, "sugar_cane");
    public static final ItemType FLOWERING_AZALEA_LEAVES = define(64, "flowering_azalea_leaves");
    public static final ItemType TALL_GRASS = define(64, "tall_grass");
    public static final ItemType ORANGE_STAINED_GLASS = define(64, "orange_stained_glass");
    public static final ItemType MAGENTA_CONCRETE = define(64, "magenta_concrete");
    public static final ItemType CHAIN_COMMAND_BLOCK = define(64, "chain_command_block");
    public static final ItemType IRON_CHESTPLATE = define(1, "iron_chestplate", 240);
    public static final ItemType WEEPING_VINES = define(64, "weeping_vines");
    public static final ItemType OXIDIZED_CUT_COPPER_SLAB = define(64, "oxidized_cut_copper_slab");
    public static final ItemType GLOWSTONE = define(64, "glowstone");
    public static final ItemType SNOW_BLOCK = define(64, "snow_block");
    public static final ItemType GREEN_STAINED_GLASS = define(64, "green_stained_glass");
    public static final ItemType PRISMARINE_BRICKS = define(64, "prismarine_bricks");
    public static final ItemType WHITE_TULIP = define(64, "white_tulip");
    public static final ItemType IRON_SWORD = define(1, "iron_sword", 250, ItemAttribute.IRON_TIER);
    public static final ItemType COPPER_BLOCK = define(64, "copper_block");
    public static final ItemType MAGENTA_BED = define(1, "magenta_bed");
    public static final ItemType WARPED_NYLIUM = define(64, "warped_nylium");
    public static final ItemType DIORITE = define(64, "diorite");
    public static final ItemType SPRUCE_WOOD = define(64, "spruce_wood");
    public static final ItemType CYAN_SHULKER_BOX = define(1, "cyan_shulker_box");
    public static final ItemType COBWEB = define(64, "cobweb");
    public static final ItemType BLAZE_SPAWN_EGG = define(64, "blaze_spawn_egg");
    public static final ItemType GRAVEL = define(64, "gravel");
    public static final ItemType WITCH_SPAWN_EGG = define(64, "witch_spawn_egg");
    public static final ItemType ELYTRA = define(1, "elytra", 432);
    public static final ItemType ACACIA_FENCE_GATE = define(64, "acacia_fence_gate");
    public static final ItemType JIGSAW = define(64, "jigsaw");
    public static final ItemType BLUE_GLAZED_TERRACOTTA = define(64, "blue_glazed_terracotta");
    public static final ItemType FLINT_AND_STEEL = define(1, "flint_and_steel", 64);
    public static final ItemType TNT = define(64, "tnt");
    public static final ItemType PINK_SHULKER_BOX = define(1, "pink_shulker_box");
    public static final ItemType MOSSY_STONE_BRICK_SLAB = define(64, "mossy_stone_brick_slab");
    public static final ItemType YELLOW_CARPET = define(64, "yellow_carpet");
    public static final ItemType TINTED_GLASS = define(64, "tinted_glass");
    public static final ItemType AIR = define(64, "air");
    public static final ItemType JUNGLE_FENCE_GATE = define(64, "jungle_fence_gate");
    public static final ItemType SANDSTONE = define(64, "sandstone");
    public static final ItemType BLUE_TERRACOTTA = define(64, "blue_terracotta");
    public static final ItemType DARK_PRISMARINE_SLAB = define(64, "dark_prismarine_slab");
    public static final ItemType CONDUIT = define(64, "conduit");
    public static final ItemType TROPICAL_FISH = define(64, "tropical_fish", ItemAttribute.EDIBLE);
    public static final ItemType IRON_INGOT = define(64, "iron_ingot");
    public static final ItemType NETHER_STAR = define(64, "nether_star");
    public static final ItemType OAK_STAIRS = define(64, "oak_stairs");
    public static final ItemType PLAYER_HEAD = define(64, "player_head");
    public static final ItemType LIGHT_BLUE_CANDLE = define(64, "light_blue_candle");
    public static final ItemType BEDROCK = define(64, "bedrock");
    public static final ItemType POTATO = define(64, "potato", ItemAttribute.EDIBLE);
    public static final ItemType DEEPSLATE_LAPIS_ORE = define(64, "deepslate_lapis_ore");
    public static final ItemType NETHER_BRICKS = define(64, "nether_bricks");
    public static final ItemType POISONOUS_POTATO = define(64, "poisonous_potato", ItemAttribute.EDIBLE);
    public static final ItemType BROWN_STAINED_GLASS = define(64, "brown_stained_glass");
    public static final ItemType BLACK_DYE = define(64, "black_dye");
    public static final ItemType CHISELED_NETHER_BRICKS = define(64, "chiseled_nether_bricks");
    public static final ItemType POLISHED_BLACKSTONE_SLAB = define(64, "polished_blackstone_slab");
    public static final ItemType POLISHED_ANDESITE_SLAB = define(64, "polished_andesite_slab");
    public static final ItemType MAGENTA_BANNER = define(16, "magenta_banner");
    public static final ItemType LIGHT_GRAY_STAINED_GLASS = define(64, "light_gray_stained_glass");
    public static final ItemType TROPICAL_FISH_BUCKET = define(1, "tropical_fish_bucket");
    public static final ItemType GREEN_CONCRETE_POWDER = define(64, "green_concrete_powder");
    public static final ItemType PURPUR_BLOCK = define(64, "purpur_block");
    public static final ItemType BLUE_BANNER = define(16, "blue_banner");
    public static final ItemType SMITHING_TABLE = define(64, "smithing_table");
    public static final ItemType COMPARATOR = define(64, "comparator");
    public static final ItemType GRAY_SHULKER_BOX = define(1, "gray_shulker_box");
    public static final ItemType INFESTED_CRACKED_STONE_BRICKS = define(64, "infested_cracked_stone_bricks");
    public static final ItemType YELLOW_CONCRETE_POWDER = define(64, "yellow_concrete_powder");
    public static final ItemType BLACKSTONE_WALL = define(64, "blackstone_wall");
    public static final ItemType COD = define(64, "cod", ItemAttribute.EDIBLE);
    public static final ItemType SMOOTH_STONE = define(64, "smooth_stone");
    public static final ItemType SPRUCE_PRESSURE_PLATE = define(64, "spruce_pressure_plate");
    public static final ItemType SPRUCE_SAPLING = define(64, "spruce_sapling");
    public static final ItemType ACACIA_FENCE = define(64, "acacia_fence");
    public static final ItemType WARPED_ROOTS = define(64, "warped_roots");
    public static final ItemType ARROW = define(64, "arrow");
    public static final ItemType CRIMSON_HYPHAE = define(64, "crimson_hyphae");
    public static final ItemType CLAY_BALL = define(64, "clay_ball");
    public static final ItemType CRIMSON_BUTTON = define(64, "crimson_button");
    public static final ItemType BROWN_MUSHROOM = define(64, "brown_mushroom");
    public static final ItemType BUDDING_AMETHYST = define(64, "budding_amethyst");
    public static final ItemType ENDERMAN_SPAWN_EGG = define(64, "enderman_spawn_egg");
    public static final ItemType IRON_NUGGET = define(64, "iron_nugget");
    public static final ItemType DONKEY_SPAWN_EGG = define(64, "donkey_spawn_egg");
    public static final ItemType STONECUTTER = define(64, "stonecutter");
    public static final ItemType CHAINMAIL_BOOTS = define(1, "chainmail_boots, 195");
    public static final ItemType TERRACOTTA = define(64, "terracotta");
    public static final ItemType LIME_STAINED_GLASS_PANE = define(64, "lime_stained_glass_pane");
    public static final ItemType STRUCTURE_VOID = define(64, "structure_void");
    public static final ItemType DEAD_BRAIN_CORAL = define(64, "dead_brain_coral");
    public static final ItemType GREEN_WOOL = define(64, "green_wool");
    public static final ItemType CRIMSON_STAIRS = define(64, "crimson_stairs");
    public static final ItemType CLOCK = define(64, "clock");
    public static final ItemType LLAMA_SPAWN_EGG = define(64, "llama_spawn_egg");
    public static final ItemType LIGHT_BLUE_STAINED_GLASS_PANE = define(64, "light_blue_stained_glass_pane");
    public static final ItemType DEAD_FIRE_CORAL_FAN = define(64, "dead_fire_coral_fan");
    public static final ItemType CREEPER_SPAWN_EGG = define(64, "creeper_spawn_egg");
    public static final ItemType OAK_LOG = define(64, "oak_log");
    public static final ItemType JUNGLE_PLANKS = define(64, "jungle_planks");
    public static final ItemType SNOW = define(64, "snow");
    public static final ItemType MAGENTA_CARPET = define(64, "magenta_carpet");
    public static final ItemType BIG_DRIPLEAF = define(64, "big_dripleaf");
    public static final ItemType GRANITE_STAIRS = define(64, "granite_stairs");
    public static final ItemType POWERED_RAIL = define(64, "powered_rail");
    public static final ItemType LEATHER_HELMET = define(1, "leather_helmet", 55);
    public static final ItemType EMERALD_ORE = define(64, "emerald_ore");
    public static final ItemType STRIPPED_SPRUCE_LOG = define(64, "stripped_spruce_log");
    public static final ItemType CUT_RED_SANDSTONE = define(64, "cut_red_sandstone");
    public static final ItemType CRIMSON_FENCE = define(64, "crimson_fence");
    public static final ItemType BLUE_CARPET = define(64, "blue_carpet");
    public static final ItemType IRON_HOE = define(1, "iron_hoe", 250, ItemAttribute.IRON_TIER);
    public static final ItemType CHICKEN = define(64, "chicken", ItemAttribute.EDIBLE);
    public static final ItemType CRIMSON_STEM = define(64, "crimson_stem");
    public static final ItemType DEAD_HORN_CORAL_BLOCK = define(64, "dead_horn_coral_block");
    public static final ItemType CYAN_BANNER = define(16, "cyan_banner");
    public static final ItemType WARPED_DOOR = define(64, "warped_door");
    public static final ItemType SCULK_SENSOR = define(64, "sculk_sensor");
    public static final ItemType BREWING_STAND = define(64, "brewing_stand");
    public static final ItemType LIME_CANDLE = define(64, "lime_candle");
    public static final ItemType STONE_BRICKS = define(64, "stone_bricks");
    public static final ItemType STRIPPED_OAK_WOOD = define(64, "stripped_oak_wood");
    public static final ItemType BUBBLE_CORAL_FAN = define(64, "bubble_coral_fan");
    public static final ItemType OAK_PRESSURE_PLATE = define(64, "oak_pressure_plate");
    public static final ItemType CYAN_GLAZED_TERRACOTTA = define(64, "cyan_glazed_terracotta");
    public static final ItemType BASALT = define(64, "basalt");
    public static final ItemType JUNGLE_DOOR = define(64, "jungle_door");
    public static final ItemType BROWN_CARPET = define(64, "brown_carpet");
    public static final ItemType FISHING_ROD = define(1, "fishing_rod", 64);
    public static final ItemType HORSE_SPAWN_EGG = define(64, "horse_spawn_egg");
    public static final ItemType GRAY_CONCRETE_POWDER = define(64, "gray_concrete_powder");
    public static final ItemType RED_CANDLE = define(64, "red_candle");
    public static final ItemType QUARTZ = define(64, "quartz");
    public static final ItemType RAW_COPPER = define(64, "raw_copper");
    public static final ItemType BEETROOT = define(64, "beetroot", ItemAttribute.EDIBLE);
    public static final ItemType DEAD_FIRE_CORAL = define(64, "dead_fire_coral");
    public static final ItemType MUSIC_DISC_MALL = define(1, "music_disc_mall", ItemAttribute.MUSIC_DISC);
    public static final ItemType LADDER = define(64, "ladder");
    public static final ItemType LODESTONE = define(64, "lodestone");
    public static final ItemType RAVAGER_SPAWN_EGG = define(64, "ravager_spawn_egg");
    public static final ItemType NETHERITE_HOE = define(1, "netherite_hoe", 2031, ItemAttribute.FIRE_RESISTANT, ItemAttribute.NETHERITE_TIER);
    public static final ItemType INFESTED_STONE_BRICKS = define(64, "infested_stone_bricks");
    public static final ItemType END_STONE_BRICK_SLAB = define(64, "end_stone_brick_slab");
    public static final ItemType LEATHER_BOOTS = define(1, "leather_boots", 65);
    public static final ItemType WATER_BUCKET = define(1, "water_bucket", ItemTypes.BUCKET);
    public static final ItemType LIGHT_BLUE_DYE = define(64, "light_blue_dye");
    public static final ItemType WARPED_STAIRS = define(64, "warped_stairs");
    public static final ItemType DEAD_BUBBLE_CORAL = define(64, "dead_bubble_coral");
    public static final ItemType CHAINMAIL_HELMET = define(1, "chainmail_helmet", 165);
    public static final ItemType OAK_SLAB = define(64, "oak_slab");
    public static final ItemType SPRUCE_DOOR = define(64, "spruce_door");
    public static final ItemType ZOMBIE_HEAD = define(64, "zombie_head");
    public static final ItemType DEAD_TUBE_CORAL = define(64, "dead_tube_coral");
    public static final ItemType CHORUS_FRUIT = define(64, "chorus_fruit", ItemAttribute.EDIBLE);
    public static final ItemType HORN_CORAL = define(64, "horn_coral");
    public static final ItemType PRISMARINE_CRYSTALS = define(64, "prismarine_crystals");
    public static final ItemType WHITE_CONCRETE_POWDER = define(64, "white_concrete_powder");
    public static final ItemType GRANITE_SLAB = define(64, "granite_slab");
    public static final ItemType SANDSTONE_SLAB = define(64, "sandstone_slab");
    public static final ItemType CAKE = define(1, "cake");
    public static final ItemType ACACIA_LEAVES = define(64, "acacia_leaves");
    public static final ItemType YELLOW_SHULKER_BOX = define(1, "yellow_shulker_box");
    public static final ItemType MOSS_CARPET = define(64, "moss_carpet");
    public static final ItemType BROWN_BANNER = define(16, "brown_banner");
    public static final ItemType GUNPOWDER = define(64, "gunpowder");
    public static final ItemType PUFFERFISH_BUCKET = define(1, "pufferfish_bucket");
    public static final ItemType NETHER_BRICK = define(64, "nether_brick");
    public static final ItemType PINK_STAINED_GLASS_PANE = define(64, "pink_stained_glass_pane");
    public static final ItemType GLOW_SQUID_SPAWN_EGG = define(64, "glow_squid_spawn_egg");
    public static final ItemType BAMBOO = define(64, "bamboo");
    public static final ItemType RED_SAND = define(64, "red_sand");
    public static final ItemType PURPLE_SHULKER_BOX = define(1, "purple_shulker_box");
    public static final ItemType CLAY = define(64, "clay");
    public static final ItemType CHISELED_STONE_BRICKS = define(64, "chiseled_stone_bricks");
    public static final ItemType LECTERN = define(64, "lectern");
    public static final ItemType DIAMOND_LEGGINGS = define(1, "diamond_leggings", 495);
    public static final ItemType DIAMOND_HELMET = define(1, "diamond_helmet", 363);
    public static final ItemType WARPED_SLAB = define(64, "warped_slab");
    public static final ItemType QUARTZ_BLOCK = define(64, "quartz_block");
    public static final ItemType DIAMOND_CHESTPLATE = define(1, "diamond_chestplate", 528);
    public static final ItemType MOSSY_COBBLESTONE_SLAB = define(64, "mossy_cobblestone_slab");
    public static final ItemType WOODEN_HOE = define(1, "wooden_hoe", 59, ItemAttribute.WOOD_TIER);
    public static final ItemType MUSIC_DISC_BLOCKS = define(1, "music_disc_blocks", ItemAttribute.MUSIC_DISC);
    public static final ItemType WHITE_WOOL = define(64, "white_wool");
    public static final ItemType HANGING_ROOTS = define(64, "hanging_roots");
    public static final ItemType END_STONE_BRICK_STAIRS = define(64, "end_stone_brick_stairs");
    public static final ItemType EXPOSED_COPPER = define(64, "exposed_copper");
    public static final ItemType CHAINMAIL_CHESTPLATE = define(1, "chainmail_chestplate", 240);
    public static final ItemType IRON_LEGGINGS = define(1, "iron_leggings", 225);
    public static final ItemType PURPLE_STAINED_GLASS = define(64, "purple_stained_glass");
    public static final ItemType PURPLE_TERRACOTTA = define(64, "purple_terracotta");
    public static final ItemType GREEN_BED = define(1, "green_bed");
    public static final ItemType RED_CONCRETE_POWDER = define(64, "red_concrete_powder");
    public static final ItemType REPEATER = define(64, "repeater");
    public static final ItemType MYCELIUM = define(64, "mycelium");
    public static final ItemType CHISELED_SANDSTONE = define(64, "chiseled_sandstone");
    public static final ItemType LINGERING_POTION = define(1, "lingering_potion");
    public static final ItemType CUT_COPPER_STAIRS = define(64, "cut_copper_stairs");
    public static final ItemType CALCITE = define(64, "calcite");
    public static final ItemType STRIPPED_BIRCH_LOG = define(64, "stripped_birch_log");
    public static final ItemType HAY_BLOCK = define(64, "hay_block");
    public static final ItemType LIGHT_BLUE_CONCRETE_POWDER = define(64, "light_blue_concrete_powder");
    public static final ItemType PINK_DYE = define(64, "pink_dye");
    public static final ItemType ORANGE_CARPET = define(64, "orange_carpet");
    public static final ItemType MAGENTA_CONCRETE_POWDER = define(64, "magenta_concrete_powder");
    public static final ItemType ANDESITE_WALL = define(64, "andesite_wall");
    public static final ItemType YELLOW_CONCRETE = define(64, "yellow_concrete");
    public static final ItemType WARPED_FUNGUS_ON_A_STICK = define(1, "warped_fungus_on_a_stick", 100);
    public static final ItemType WAXED_WEATHERED_CUT_COPPER = define(64, "waxed_weathered_cut_copper");
    public static final ItemType COBBLESTONE_SLAB = define(64, "cobblestone_slab");
    public static final ItemType ARMOR_STAND = define(16, "armor_stand");
    public static final ItemType RED_NETHER_BRICKS = define(64, "red_nether_bricks");
    public static final ItemType LIGHT_GRAY_CONCRETE = define(64, "light_gray_concrete");
    public static final ItemType GLASS = define(64, "glass");
    public static final ItemType CHEST = define(64, "chest");
    public static final ItemType SEAGRASS = define(64, "seagrass");
    public static final ItemType WARPED_TRAPDOOR = define(64, "warped_trapdoor");
    public static final ItemType STONE_STAIRS = define(64, "stone_stairs");
    public static final ItemType RED_TERRACOTTA = define(64, "red_terracotta");
    public static final ItemType FURNACE_MINECART = define(1, "furnace_minecart");
    public static final ItemType END_PORTAL_FRAME = define(64, "end_portal_frame");
    public static final ItemType GRINDSTONE = define(64, "grindstone");
    public static final ItemType LEATHER_LEGGINGS = define(1, "leather_leggings", 75);
    public static final ItemType WAXED_COPPER_BLOCK = define(64, "waxed_copper_block");
    public static final ItemType DARK_OAK_LEAVES = define(64, "dark_oak_leaves");
    public static final ItemType LIME_SHULKER_BOX = define(1, "lime_shulker_box");
    public static final ItemType JUNGLE_SAPLING = define(64, "jungle_sapling");
    public static final ItemType AMETHYST_BLOCK = define(64, "amethyst_block");
    public static final ItemType CREEPER_HEAD = define(64, "creeper_head");
    public static final ItemType WEATHERED_COPPER = define(64, "weathered_copper");
    public static final ItemType GRAY_BANNER = define(16, "gray_banner");
    public static final ItemType STRING = define(64, "string");
    public static final ItemType WHITE_TERRACOTTA = define(64, "white_terracotta");
    public static final ItemType BOOK = define(64, "book");
    public static final ItemType WOODEN_SHOVEL = define(1, "wooden_shovel", 59, ItemAttribute.WOOD_TIER);
    public static final ItemType BLACKSTONE_SLAB = define(64, "blackstone_slab");
    public static final ItemType JUNGLE_TRAPDOOR = define(64, "jungle_trapdoor");
    public static final ItemType BLACK_CARPET = define(64, "black_carpet");
    public static final ItemType FIRE_CORAL = define(64, "fire_coral");
    public static final ItemType MAGENTA_GLAZED_TERRACOTTA = define(64, "magenta_glazed_terracotta");
    public static final ItemType GRAY_BED = define(1, "gray_bed");
    public static final ItemType TRIDENT = define(1, "trident", 250);
    public static final ItemType WET_SPONGE = define(64, "wet_sponge");
    public static final ItemType YELLOW_WOOL = define(64, "yellow_wool");
    public static final ItemType CHICKEN_SPAWN_EGG = define(64, "chicken_spawn_egg");
    public static final ItemType DRIED_KELP_BLOCK = define(64, "dried_kelp_block");
    public static final ItemType BONE = define(64, "bone");
    public static final ItemType YELLOW_TERRACOTTA = define(64, "yellow_terracotta");
    public static final ItemType TARGET = define(64, "target");
    public static final ItemType WAXED_WEATHERED_COPPER = define(64, "waxed_weathered_copper");
    public static final ItemType MAGMA_BLOCK = define(64, "magma_block");
    public static final ItemType CAULDRON = define(64, "cauldron");
    public static final ItemType PINK_CONCRETE_POWDER = define(64, "pink_concrete_powder");
    public static final ItemType WITHER_SKELETON_SPAWN_EGG = define(64, "wither_skeleton_spawn_egg");
    public static final ItemType CROSSBOW = define(1, "crossbow", 465);
    public static final ItemType SOUL_SAND = define(64, "soul_sand");
    public static final ItemType HORN_CORAL_BLOCK = define(64, "horn_coral_block");
    public static final ItemType DIAMOND_SHOVEL = define(1, "diamond_shovel", 1561, ItemAttribute.DIAMOND_TIER);
    public static final ItemType PRISMARINE_BRICK_STAIRS = define(64, "prismarine_brick_stairs");
    public static final ItemType EXPERIENCE_BOTTLE = define(64, "experience_bottle");
    public static final ItemType GOLDEN_HORSE_ARMOR = define(1, "golden_horse_armor");
    public static final ItemType BLUE_CANDLE = define(64, "blue_candle");
    public static final ItemType ORANGE_TULIP = define(64, "orange_tulip");
    public static final ItemType DEEPSLATE = define(64, "deepslate");
    public static final ItemType BEE_NEST = define(64, "bee_nest");
    public static final ItemType SMOOTH_RED_SANDSTONE_SLAB = define(64, "smooth_red_sandstone_slab");
    public static final ItemType CUT_SANDSTONE_SLAB = define(64, "cut_sandstone_slab");
    public static final ItemType GRASS_BLOCK = define(64, "grass_block");
    public static final ItemType MUSIC_DISC_PIGSTEP = define(1, "music_disc_pigstep", ItemAttribute.MUSIC_DISC);
    public static final ItemType BLACK_BED = define(1, "black_bed");
    public static final ItemType WAXED_OXIDIZED_COPPER = define(64, "waxed_oxidized_copper");
    public static final ItemType MINECART = define(1, "minecart");
    public static final ItemType DEAD_HORN_CORAL_FAN = define(64, "dead_horn_coral_fan");
    public static final ItemType LIGHT = define(64, "light");
    public static final ItemType SPECTRAL_ARROW = define(64, "spectral_arrow");
    public static final ItemType JUNGLE_STAIRS = define(64, "jungle_stairs");
    public static final ItemType NETHERITE_SHOVEL = define(1, "netherite_shovel", 2031, ItemAttribute.FIRE_RESISTANT, ItemAttribute.NETHERITE_TIER);
    public static final ItemType PIGLIN_SPAWN_EGG = define(64, "piglin_spawn_egg");
    public static final ItemType OXEYE_DAISY = define(64, "oxeye_daisy");
    public static final ItemType WAXED_OXIDIZED_CUT_COPPER_STAIRS = define(64, "waxed_oxidized_cut_copper_stairs");
    public static final ItemType SMOOTH_SANDSTONE_STAIRS = define(64, "smooth_sandstone_stairs");
    public static final ItemType LEATHER_CHESTPLATE = define(1, "leather_chestplate", 80);
    public static final ItemType BLUE_WOOL = define(64, "blue_wool");
    public static final ItemType AXOLOTL_BUCKET = define(1, "axolotl_bucket");
    public static final ItemType POPPED_CHORUS_FRUIT = define(64, "popped_chorus_fruit");
    public static final ItemType CREEPER_BANNER_PATTERN = define(1, "creeper_banner_pattern");
    public static final ItemType SMALL_AMETHYST_BUD = define(64, "small_amethyst_bud");
    public static final ItemType WAXED_EXPOSED_CUT_COPPER_SLAB = define(64, "waxed_exposed_cut_copper_slab");
    public static final ItemType POLAR_BEAR_SPAWN_EGG = define(64, "polar_bear_spawn_egg");
    public static final ItemType STRIPPED_DARK_OAK_LOG = define(64, "stripped_dark_oak_log");
    public static final ItemType RED_BED = define(1, "red_bed");
    public static final ItemType LAVA_BUCKET = define(1, "lava_bucket", ItemTypes.BUCKET);
    public static final ItemType YELLOW_BANNER = define(16, "yellow_banner");
    public static final ItemType BARREL = define(64, "barrel");
    public static final ItemType CHAIN = define(64, "chain");
    public static final ItemType DEAD_BRAIN_CORAL_FAN = define(64, "dead_brain_coral_fan");
    public static final ItemType ROTTEN_FLESH = define(64, "rotten_flesh", ItemAttribute.EDIBLE);
    public static final ItemType SLIME_BLOCK = define(64, "slime_block");
    public static final ItemType EMERALD_BLOCK = define(64, "emerald_block");
    public static final ItemType PURPLE_BANNER = define(16, "purple_banner");
    public static final ItemType OAK_FENCE = define(64, "oak_fence");
    public static final ItemType TNT_MINECART = define(1, "tnt_minecart");
    public static final ItemType CHAINMAIL_LEGGINGS = define(1, "chainmail_leggings", 225);
    public static final ItemType PINK_CANDLE = define(64, "pink_candle");
    public static final ItemType ACACIA_PLANKS = define(64, "acacia_planks");
    public static final ItemType IRON_ORE = define(64, "iron_ore");
    public static final ItemType BLAZE_POWDER = define(64, "blaze_powder");
    public static final ItemType QUARTZ_PILLAR = define(64, "quartz_pillar");
    public static final ItemType DEAD_BUBBLE_CORAL_BLOCK = define(64, "dead_bubble_coral_block");
    public static final ItemType PURPLE_CONCRETE_POWDER = define(64, "purple_concrete_powder");
    public static final ItemType POLISHED_DIORITE_SLAB = define(64, "polished_diorite_slab");
    public static final ItemType SMOOTH_STONE_SLAB = define(64, "smooth_stone_slab");
    public static final ItemType RAW_IRON = define(64, "raw_iron");
    public static final ItemType GOLDEN_SWORD = define(1, "golden_sword", 32, ItemAttribute.GOLD_TIER);
    public static final ItemType PRISMARINE = define(64, "prismarine");
    public static final ItemType WAXED_WEATHERED_CUT_COPPER_SLAB = define(64, "waxed_weathered_cut_copper_slab");
    public static final ItemType CRIMSON_TRAPDOOR = define(64, "crimson_trapdoor");
    public static final ItemType FILLED_MAP = define(64, "filled_map");
    public static final ItemType LIME_CONCRETE = define(64, "lime_concrete");
    public static final ItemType MOSSY_COBBLESTONE_STAIRS = define(64, "mossy_cobblestone_stairs");
    public static final ItemType IRON_BLOCK = define(64, "iron_block");
    public static final ItemType BIRCH_SIGN = define(16, "birch_sign");
    public static final ItemType PORKCHOP = define(64, "porkchop", ItemAttribute.EDIBLE);
    public static final ItemType PINK_TULIP = define(64, "pink_tulip");
    public static final ItemType WARPED_FENCE_GATE = define(64, "warped_fence_gate");
    public static final ItemType BLUE_ICE = define(64, "blue_ice");
    public static final ItemType WOLF_SPAWN_EGG = define(64, "wolf_spawn_egg");
    public static final ItemType DEEPSLATE_TILE_STAIRS = define(64, "deepslate_tile_stairs");
    public static final ItemType STRIPPED_WARPED_STEM = define(64, "stripped_warped_stem");
    public static final ItemType CRYING_OBSIDIAN = define(64, "crying_obsidian");
    public static final ItemType BROWN_TERRACOTTA = define(64, "brown_terracotta");
    public static final ItemType VINE = define(64, "vine");
    public static final ItemType DARK_OAK_FENCE = define(64, "dark_oak_fence");
    public static final ItemType QUARTZ_STAIRS = define(64, "quartz_stairs");
    public static final ItemType RAIL = define(64, "rail");
    public static final ItemType WHITE_BANNER = define(16, "white_banner");
    public static final ItemType MOSS_BLOCK = define(64, "moss_block");
    public static final ItemType BLUE_STAINED_GLASS = define(64, "blue_stained_glass");
    public static final ItemType GREEN_TERRACOTTA = define(64, "green_terracotta");
    public static final ItemType IRON_HORSE_ARMOR = define(1, "iron_horse_armor");
    public static final ItemType RED_CARPET = define(64, "red_carpet");
    public static final ItemType WHITE_CONCRETE = define(64, "white_concrete");
    public static final ItemType FLOWER_BANNER_PATTERN = define(1, "flower_banner_pattern");
    public static final ItemType OAK_WOOD = define(64, "oak_wood");
    public static final ItemType GLOW_LICHEN = define(64, "glow_lichen");
    public static final ItemType LIME_CONCRETE_POWDER = define(64, "lime_concrete_powder");
    public static final ItemType RED_SHULKER_BOX = define(1, "red_shulker_box");
    public static final ItemType LIGHT_BLUE_TERRACOTTA = define(64, "light_blue_terracotta");
    public static final ItemType BLUE_DYE = define(64, "blue_dye");
    public static final ItemType SUGAR = define(64, "sugar");
    public static final ItemType CAT_SPAWN_EGG = define(64, "cat_spawn_egg");
    public static final ItemType MUSIC_DISC_FAR = define(1, "music_disc_far", ItemAttribute.MUSIC_DISC);
    public static final ItemType BROWN_GLAZED_TERRACOTTA = define(64, "brown_glazed_terracotta");
    public static final ItemType COPPER_INGOT = define(64, "copper_ingot");
    public static final ItemType COD_BUCKET = define(1, "cod_bucket");
    public static final ItemType CRIMSON_PLANKS = define(64, "crimson_planks");
    public static final ItemType INK_SAC = define(64, "ink_sac");
    public static final ItemType NOTE_BLOCK = define(64, "note_block");
    public static final ItemType BOWL = define(64, "bowl");
    public static final ItemType CRACKED_STONE_BRICKS = define(64, "cracked_stone_bricks");
    public static final ItemType SKELETON_SKULL = define(64, "skeleton_skull");
    public static final ItemType PURPUR_STAIRS = define(64, "purpur_stairs");
    public static final ItemType ORANGE_DYE = define(64, "orange_dye");
    public static final ItemType YELLOW_BED = define(1, "yellow_bed");
    public static final ItemType CUT_COPPER = define(64, "cut_copper");
    public static final ItemType JUNGLE_SIGN = define(16, "jungle_sign");
    public static final ItemType GREEN_GLAZED_TERRACOTTA = define(64, "green_glazed_terracotta");
    public static final ItemType SCUTE = define(64, "scute");
    public static final ItemType GOLDEN_CHESTPLATE = define(1, "golden_chestplate", 112);
    public static final ItemType NETHERITE_LEGGINGS = define(1, "netherite_leggings", null, 555, ItemAttribute.FIRE_RESISTANT);
    public static final ItemType GOLDEN_SHOVEL = define(1, "golden_shovel", 32, ItemAttribute.GOLD_TIER);
    public static final ItemType SPRUCE_STAIRS = define(64, "spruce_stairs");
    public static final ItemType BIRCH_PLANKS = define(64, "birch_planks");
    public static final ItemType GRAY_WOOL = define(64, "gray_wool");
    public static final ItemType SILVERFISH_SPAWN_EGG = define(64, "silverfish_spawn_egg");
    public static final ItemType WHITE_STAINED_GLASS = define(64, "white_stained_glass");
    public static final ItemType ANCIENT_DEBRIS = define(64, "ancient_debris", 32, ItemAttribute.FIRE_RESISTANT);
    public static final ItemType GREEN_STAINED_GLASS_PANE = define(64, "green_stained_glass_pane");
    public static final ItemType SMOOTH_BASALT = define(64, "smooth_basalt");
    public static final ItemType DIAMOND = define(64, "diamond");
    public static final ItemType BLACK_CONCRETE_POWDER = define(64, "black_concrete_powder");
    public static final ItemType RABBIT_SPAWN_EGG = define(64, "rabbit_spawn_egg");
    public static final ItemType LIME_CARPET = define(64, "lime_carpet");
    public static final ItemType BLUE_CONCRETE_POWDER = define(64, "blue_concrete_powder");
    public static final ItemType MAGENTA_CANDLE = define(64, "magenta_candle");
    public static final ItemType PURPUR_SLAB = define(64, "purpur_slab");
    public static final ItemType HOPPER = define(64, "hopper");
    public static final ItemType STRIDER_SPAWN_EGG = define(64, "strider_spawn_egg");
    public static final ItemType DRAGON_BREATH = define(64, "dragon_breath", ItemTypes.GLASS_BOTTLE);
    public static final ItemType POLISHED_DIORITE = define(64, "polished_diorite");
    public static final ItemType LIME_TERRACOTTA = define(64, "lime_terracotta");
    public static final ItemType BEEF = define(64, "beef", ItemAttribute.EDIBLE);
    public static final ItemType BAKED_POTATO = define(64, "baked_potato", ItemAttribute.EDIBLE);
    public static final ItemType MOSSY_COBBLESTONE = define(64, "mossy_cobblestone");
    public static final ItemType BRICK_WALL = define(64, "brick_wall");
    public static final ItemType BRAIN_CORAL_BLOCK = define(64, "brain_coral_block");
    public static final ItemType BIRCH_FENCE_GATE = define(64, "birch_fence_gate");
    public static final ItemType MUSIC_DISC_CHIRP = define(1, "music_disc_chirp", ItemAttribute.MUSIC_DISC);
    public static final ItemType NETHERITE_SWORD = define(1, "netherite_sword", 2031, ItemAttribute.FIRE_RESISTANT, ItemAttribute.NETHERITE_TIER);
    public static final ItemType COBBLED_DEEPSLATE = define(64, "cobbled_deepslate");
    public static final ItemType BROWN_CANDLE = define(64, "brown_candle");
    public static final ItemType YELLOW_STAINED_GLASS_PANE = define(64, "yellow_stained_glass_pane");
    public static final ItemType DIRT_PATH = define(64, "dirt_path");
    public static final ItemType DARK_OAK_PLANKS = define(64, "dark_oak_planks");
    public static final ItemType PHANTOM_MEMBRANE = define(64, "phantom_membrane");
    public static final ItemType WOODEN_SWORD = define(1, "wooden_sword", 59, ItemAttribute.WOOD_TIER);
    public static final ItemType ALLIUM = define(64, "allium");
    public static final ItemType JUNGLE_LEAVES = define(64, "jungle_leaves");
    public static final ItemType CHORUS_PLANT = define(64, "chorus_plant");
    public static final ItemType INFESTED_DEEPSLATE = define(64, "infested_deepslate");
    public static final ItemType BUCKET = define(16, "bucket");
    public static final ItemType WARPED_BUTTON = define(64, "warped_button");
    public static final ItemType OAK_TRAPDOOR = define(64, "oak_trapdoor");
    public static final ItemType BLACK_STAINED_GLASS = define(64, "black_stained_glass");
    public static final ItemType GOLDEN_HELMET = define(1, "golden_helmet", 77);
    public static final ItemType DARK_OAK_PRESSURE_PLATE = define(64, "dark_oak_pressure_plate");
    public static final ItemType WEATHERED_CUT_COPPER_STAIRS = define(64, "weathered_cut_copper_stairs");
    public static final ItemType CUT_RED_SANDSTONE_SLAB = define(64, "cut_red_sandstone_slab");
    public static final ItemType LIME_BED = define(1, "lime_bed");
    public static final ItemType BLAST_FURNACE = define(64, "blast_furnace");
    public static final ItemType SPONGE = define(64, "sponge");
    public static final ItemType CARTOGRAPHY_TABLE = define(64, "cartography_table");
    public static final ItemType NETHERITE_INGOT = define(64, "netherite_ingot", ItemAttribute.FIRE_RESISTANT);
    public static final ItemType LIGHT_GRAY_DYE = define(64, "light_gray_dye");
    public static final ItemType DAYLIGHT_DETECTOR = define(64, "daylight_detector");
    public static final ItemType SLIME_SPAWN_EGG = define(64, "slime_spawn_egg");
    public static final ItemType BEETROOT_SOUP = define(64, "beetroot_soup", ItemAttribute.EDIBLE);
    public static final ItemType RAW_COPPER_BLOCK = define(64, "raw_copper_block");
    public static final ItemType LIGHT_GRAY_CARPET = define(64, "light_gray_carpet");
    public static final ItemType MUSIC_DISC_WARD = define(1, "music_disc_ward", ItemAttribute.MUSIC_DISC);
    public static final ItemType GRASS = define(64, "grass");
    public static final ItemType END_CRYSTAL = define(64, "end_crystal");
    public static final ItemType VINDICATOR_SPAWN_EGG = define(64, "vindicator_spawn_egg");
    public static final ItemType WHEAT = define(64, "wheat");
    public static final ItemType END_ROD = define(64, "end_rod");
    public static final ItemType DEEPSLATE_COAL_ORE = define(64, "deepslate_coal_ore");
    public static final ItemType PHANTOM_SPAWN_EGG = define(64, "phantom_spawn_egg");
    public static final ItemType STONE_PICKAXE = define(1, "stone_pickaxe", 131, ItemAttribute.STONE_TIER);
    public static final ItemType IRON_HELMET = define(1, "iron_helmet", 165);
    public static final ItemType GUARDIAN_SPAWN_EGG = define(64, "guardian_spawn_egg");
    public static final ItemType PINK_STAINED_GLASS = define(64, "pink_stained_glass");
    public static final ItemType PISTON = define(64, "piston");
    public static final ItemType DEAD_FIRE_CORAL_BLOCK = define(64, "dead_fire_coral_block");
    public static final ItemType CYAN_CANDLE = define(64, "cyan_candle");
    public static final ItemType WAXED_CUT_COPPER = define(64, "waxed_cut_copper");
    public static final ItemType MELON_SLICE = define(64, "melon_slice", ItemAttribute.EDIBLE);
    public static final ItemType ENDER_CHEST = define(64, "ender_chest");
    public static final ItemType KELP = define(64, "kelp");
    public static final ItemType LIGHT_GRAY_WOOL = define(64, "light_gray_wool");
    public static final ItemType SUNFLOWER = define(64, "sunflower");
    public static final ItemType LIGHTNING_ROD = define(64, "lightning_rod");
    public static final ItemType BROWN_BED = define(1, "brown_bed");
    public static final ItemType RAW_IRON_BLOCK = define(64, "raw_iron_block");
    public static final ItemType HEART_OF_THE_SEA = define(64, "heart_of_the_sea");
    public static final ItemType POLISHED_BLACKSTONE_BRICKS = define(64, "polished_blackstone_bricks");
    public static final ItemType SPYGLASS = define(1, "spyglass");
    public static final ItemType JACK_O_LANTERN = define(64, "jack_o_lantern");
    public static final ItemType POLISHED_GRANITE = define(64, "polished_granite");
    public static final ItemType SMOOTH_RED_SANDSTONE = define(64, "smooth_red_sandstone");
    public static final ItemType DEAD_BUBBLE_CORAL_FAN = define(64, "dead_bubble_coral_fan");
    public static final ItemType FURNACE = define(64, "furnace");
    public static final ItemType POLISHED_DEEPSLATE = define(64, "polished_deepslate");
    public static final ItemType SPORE_BLOSSOM = define(64, "spore_blossom");
    public static final ItemType RED_STAINED_GLASS = define(64, "red_stained_glass");
    public static final ItemType STONE_SLAB = define(64, "stone_slab");
    public static final ItemType DANDELION = define(64, "dandelion");
    public static final ItemType PINK_BED = define(1, "pink_bed");
    public static final ItemType ZOMBIFIED_PIGLIN_SPAWN_EGG = define(64, "zombified_piglin_spawn_egg");
    public static final ItemType ROSE_BUSH = define(64, "rose_bush");
    public static final ItemType BLAZE_ROD = define(64, "blaze_rod");
    public static final ItemType IRON_TRAPDOOR = define(64, "iron_trapdoor");
    public static final ItemType DROWNED_SPAWN_EGG = define(64, "drowned_spawn_egg");
    public static final ItemType STONE_PRESSURE_PLATE = define(64, "stone_pressure_plate");
    public static final ItemType IRON_BARS = define(64, "iron_bars");
    public static final ItemType SMOOTH_RED_SANDSTONE_STAIRS = define(64, "smooth_red_sandstone_stairs");
    public static final ItemType QUARTZ_BRICKS = define(64, "quartz_bricks");
    public static final ItemType WHEAT_SEEDS = define(64, "wheat_seeds");
    public static final ItemType BROWN_CONCRETE = define(64, "brown_concrete");
    public static final ItemType CYAN_CONCRETE = define(64, "cyan_concrete");
    public static final ItemType STONE_SHOVEL = define(1, "stone_shovel", 131, ItemAttribute.STONE_TIER);
    public static final ItemType STRIPPED_BIRCH_WOOD = define(64, "stripped_birch_wood");
    public static final ItemType BUBBLE_CORAL_BLOCK = define(64, "bubble_coral_block");
    public static final ItemType ZOMBIE_HORSE_SPAWN_EGG = define(64, "zombie_horse_spawn_egg");
    public static final ItemType WAXED_OXIDIZED_CUT_COPPER_SLAB = define(64, "waxed_oxidized_cut_copper_slab");
    public static final ItemType GREEN_BANNER = define(16, "green_banner");
    public static final ItemType LIGHT_GRAY_BANNER = define(16, "light_gray_banner");
    public static final ItemType DIAMOND_SWORD = define(1, "diamond_sword", 1561, ItemAttribute.DIAMOND_TIER);
    public static final ItemType RABBIT_FOOT = define(64, "rabbit_foot");
    public static final ItemType NETHERITE_BLOCK = define(64, "netherite_block", ItemAttribute.FIRE_RESISTANT);
    public static final ItemType BAT_SPAWN_EGG = define(64, "bat_spawn_egg");
    public static final ItemType DIAMOND_HORSE_ARMOR = define(1, "diamond_horse_armor");
    public static final ItemType GLOWSTONE_DUST = define(64, "glowstone_dust");
    public static final ItemType CRIMSON_FENCE_GATE = define(64, "crimson_fence_gate");
    public static final ItemType WHITE_CARPET = define(64, "white_carpet");
    public static final ItemType FLETCHING_TABLE = define(64, "fletching_table");
    public static final ItemType RED_CONCRETE = define(64, "red_concrete");
    public static final ItemType COCOA_BEANS = define(64, "cocoa_beans");
    public static final ItemType CRACKED_POLISHED_BLACKSTONE_BRICKS = define(64, "cracked_polished_blackstone_bricks");
    public static final ItemType PURPLE_GLAZED_TERRACOTTA = define(64, "purple_glazed_terracotta");
    public static final ItemType ACACIA_SLAB = define(64, "acacia_slab");
    public static final ItemType WITHER_SKELETON_SKULL = define(64, "wither_skeleton_skull");
    public static final ItemType FIRE_CORAL_FAN = define(64, "fire_coral_fan");
    public static final ItemType HUSK_SPAWN_EGG = define(64, "husk_spawn_egg");
    public static final ItemType ZOMBIE_VILLAGER_SPAWN_EGG = define(64, "zombie_villager_spawn_egg");
    public static final ItemType BUBBLE_CORAL = define(64, "bubble_coral");
    public static final ItemType DISPENSER = define(64, "dispenser");
    public static final ItemType STRIPPED_CRIMSON_STEM = define(64, "stripped_crimson_stem");
    public static final ItemType LIME_WOOL = define(64, "lime_wool");
    public static final ItemType RAW_GOLD_BLOCK = define(64, "raw_gold_block");
    public static final ItemType REDSTONE_LAMP = define(64, "redstone_lamp");
    public static final ItemType DIAMOND_AXE = define(1, "diamond_axe", 1561, ItemAttribute.DIAMOND_TIER);
    public static final ItemType SOUL_LANTERN = define(64, "soul_lantern");
    public static final ItemType ORANGE_WOOL = define(64, "orange_wool");
    public static final ItemType TURTLE_HELMET = define(1, "turtle_helmet", 275);
    public static final ItemType JUNGLE_WOOD = define(64, "jungle_wood");
    public static final ItemType HEAVY_WEIGHTED_PRESSURE_PLATE = define(64, "heavy_weighted_pressure_plate");
    public static final ItemType POTION = define(1, "potion");
    public static final ItemType GOLD_NUGGET = define(64, "gold_nugget");
    public static final ItemType RED_SANDSTONE_WALL = define(64, "red_sandstone_wall");
    public static final ItemType CRIMSON_DOOR = define(64, "crimson_door");
    public static final ItemType WARPED_STEM = define(64, "warped_stem");
    public static final ItemType ACACIA_TRAPDOOR = define(64, "acacia_trapdoor");
    public static final ItemType MOSSY_COBBLESTONE_WALL = define(64, "mossy_cobblestone_wall");
    public static final ItemType POLISHED_BLACKSTONE_PRESSURE_PLATE = define(64, "polished_blackstone_pressure_plate");
    public static final ItemType CHIPPED_ANVIL = define(64, "chipped_anvil");
    public static final ItemType DEEPSLATE_IRON_ORE = define(64, "deepslate_iron_ore");
    public static final ItemType PURPUR_PILLAR = define(64, "purpur_pillar");
    public static final ItemType RED_STAINED_GLASS_PANE = define(64, "red_stained_glass_pane");
    public static final ItemType FEATHER = define(64, "feather");
    public static final ItemType TRADER_LLAMA_SPAWN_EGG = define(64, "trader_llama_spawn_egg");
    public static final ItemType HONEY_BOTTLE = define(16, "honey_bottle", ItemTypes.GLASS_BOTTLE, ItemAttribute.EDIBLE);
    public static final ItemType ACACIA_STAIRS = define(64, "acacia_stairs");
    public static final ItemType DROPPER = define(64, "dropper");
    public static final ItemType DEEPSLATE_BRICK_SLAB = define(64, "deepslate_brick_slab");
    public static final ItemType LIGHT_BLUE_BANNER = define(16, "light_blue_banner");
    public static final ItemType COBBLED_DEEPSLATE_STAIRS = define(64, "cobbled_deepslate_stairs");
    public static final ItemType PURPLE_BED = define(1, "purple_bed");
    public static final ItemType LIGHT_BLUE_WOOL = define(64, "light_blue_wool");
    public static final ItemType POLISHED_BLACKSTONE_STAIRS = define(64, "polished_blackstone_stairs");
    public static final ItemType LIGHT_BLUE_STAINED_GLASS = define(64, "light_blue_stained_glass");
    public static final ItemType ACACIA_SAPLING = define(64, "acacia_sapling");
    public static final ItemType FIREWORK_ROCKET = define(64, "firework_rocket");
    public static final ItemType WAXED_WEATHERED_CUT_COPPER_STAIRS = define(64, "waxed_weathered_cut_copper_stairs");
    public static final ItemType ORANGE_GLAZED_TERRACOTTA = define(64, "orange_glazed_terracotta");
    public static final ItemType BLACKSTONE_STAIRS = define(64, "blackstone_stairs");
    public static final ItemType CHISELED_POLISHED_BLACKSTONE = define(64, "chiseled_polished_blackstone");
    public static final ItemType BLACK_GLAZED_TERRACOTTA = define(64, "black_glazed_terracotta");
    public static final ItemType PURPLE_CONCRETE = define(64, "purple_concrete");
    public static final ItemType COOKIE = define(64, "cookie", ItemAttribute.EDIBLE);
    public static final ItemType BOOKSHELF = define(64, "bookshelf");
    public static final ItemType ORANGE_STAINED_GLASS_PANE = define(64, "orange_stained_glass_pane");
    public static final ItemType PINK_CARPET = define(64, "pink_carpet");
    public static final ItemType CUT_COPPER_SLAB = define(64, "cut_copper_slab");
    public static final ItemType BELL = define(64, "bell");
    public static final ItemType WARPED_HYPHAE = define(64, "warped_hyphae");
    public static final ItemType POLISHED_DEEPSLATE_SLAB = define(64, "polished_deepslate_slab");
    public static final ItemType INFESTED_MOSSY_STONE_BRICKS = define(64, "infested_mossy_stone_bricks");
    public static final ItemType MOSSY_STONE_BRICK_STAIRS = define(64, "mossy_stone_brick_stairs");
    public static final ItemType WARPED_FUNGUS = define(64, "warped_fungus");
    public static final ItemType STRIPPED_JUNGLE_LOG = define(64, "stripped_jungle_log");
    public static final ItemType OAK_BOAT = define(1, "oak_boat");
    public static final ItemType NAUTILUS_SHELL = define(64, "nautilus_shell");
    public static final ItemType DEAD_BUSH = define(64, "dead_bush");
    public static final ItemType DETECTOR_RAIL = define(64, "detector_rail");
    public static final ItemType CARROT_ON_A_STICK = define(1, "carrot_on_a_stick", 25);
    public static final ItemType SHIELD = define(1, "shield", 336);
    public static final ItemType DAMAGED_ANVIL = define(64, "damaged_anvil");
    public static final ItemType ANVIL = define(64, "anvil");
    public static final ItemType AZALEA_LEAVES = define(64, "azalea_leaves");
    public static final ItemType TOTEM_OF_UNDYING = define(1, "totem_of_undying");
    public static final ItemType RED_DYE = define(64, "red_dye");
    public static final ItemType MAGENTA_STAINED_GLASS = define(64, "magenta_stained_glass");
    public static final ItemType LAPIS_LAZULI = define(64, "lapis_lazuli");
    public static final ItemType MUSIC_DISC_WAIT = define(1, "music_disc_wait", ItemAttribute.MUSIC_DISC);
    public static final ItemType NETHERITE_PICKAXE = define(1, "netherite_pickaxe", 2031, ItemAttribute.FIRE_RESISTANT, ItemAttribute.NETHERITE_TIER);
    public static final ItemType BUNDLE = define(1, "bundle");
    public static final ItemType MOOSHROOM_SPAWN_EGG = define(64, "mooshroom_spawn_egg");
    public static final ItemType MAP = define(64, "map");
    public static final ItemType DARK_OAK_SIGN = define(16, "dark_oak_sign");
    public static final ItemType NETHERITE_BOOTS = define(1, "netherite_boots", null, 481, ItemAttribute.FIRE_RESISTANT);
    public static final ItemType BREAD = define(64, "bread", ItemAttribute.EDIBLE);
    public static final ItemType DEEPSLATE_TILE_SLAB = define(64, "deepslate_tile_slab");
    public static final ItemType POLISHED_BLACKSTONE_BUTTON = define(64, "polished_blackstone_button");
    public static final ItemType DEEPSLATE_REDSTONE_ORE = define(64, "deepslate_redstone_ore");
    public static final ItemType TORCH = define(64, "torch");
    public static final ItemType LANTERN = define(64, "lantern");
    public static final ItemType OAK_BUTTON = define(64, "oak_button");
    public static final ItemType IRON_SHOVEL = define(1, "iron_shovel", 250, ItemAttribute.IRON_TIER);
    public static final ItemType STRAY_SPAWN_EGG = define(64, "stray_spawn_egg");
    public static final ItemType SPAWNER = define(64, "spawner");
    public static final ItemType BLACK_BANNER = define(16, "black_banner");
    public static final ItemType CANDLE = define(64, "candle");
    public static final ItemType BROWN_WOOL = define(64, "brown_wool");
    public static final ItemType WARPED_WART_BLOCK = define(64, "warped_wart_block");
    public static final ItemType COOKED_SALMON = define(64, "cooked_salmon", ItemAttribute.EDIBLE);
    public static final ItemType GREEN_CARPET = define(64, "green_carpet");
    public static final ItemType WAXED_EXPOSED_COPPER = define(64, "waxed_exposed_copper");
    public static final ItemType MOSSY_STONE_BRICKS = define(64, "mossy_stone_bricks");
    public static final ItemType BIRCH_DOOR = define(64, "birch_door");
    public static final ItemType STRIPPED_ACACIA_WOOD = define(64, "stripped_acacia_wood");
    public static final ItemType COW_SPAWN_EGG = define(64, "cow_spawn_egg");
    public static final ItemType MUSIC_DISC_13 = define(1, "music_disc_13", ItemAttribute.MUSIC_DISC);
    public static final ItemType DIORITE_WALL = define(64, "diorite_wall");
    public static final ItemType MUSIC_DISC_11 = define(1, "music_disc_11", ItemAttribute.MUSIC_DISC);
    public static final ItemType BLUE_CONCRETE = define(64, "blue_concrete");
    public static final ItemType WOODEN_AXE = define(1, "wooden_axe", 59, ItemAttribute.WOOD_TIER);
    public static final ItemType VEX_SPAWN_EGG = define(64, "vex_spawn_egg");
    public static final ItemType BRAIN_CORAL = define(64, "brain_coral");
    public static final ItemType SHEARS = define(1, "shears", 238);
    public static final ItemType SPRUCE_PLANKS = define(64, "spruce_planks");
    public static final ItemType WARPED_FENCE = define(64, "warped_fence");
    public static final ItemType SPLASH_POTION = define(1, "splash_potion");
    public static final ItemType LIGHT_BLUE_GLAZED_TERRACOTTA = define(64, "light_blue_glazed_terracotta");
    public static final ItemType WRITTEN_BOOK = define(16, "written_book");
    public static final ItemType CYAN_STAINED_GLASS_PANE = define(64, "cyan_stained_glass_pane");
    public static final ItemType GHAST_TEAR = define(64, "ghast_tear");
    public static final ItemType GLASS_PANE = define(64, "glass_pane");
    public static final ItemType NETHER_QUARTZ_ORE = define(64, "nether_quartz_ore");
    public static final ItemType SEA_PICKLE = define(64, "sea_pickle");
    public static final ItemType WAXED_OXIDIZED_CUT_COPPER = define(64, "waxed_oxidized_cut_copper");
    public static final ItemType ENCHANTED_BOOK = define(1, "enchanted_book");
    public static final ItemType CARVED_PUMPKIN = define(64, "carved_pumpkin");
    public static final ItemType GRANITE_WALL = define(64, "granite_wall");
    public static final ItemType REDSTONE_TORCH = define(64, "redstone_torch");
    public static final ItemType SANDSTONE_WALL = define(64, "sandstone_wall");
    public static final ItemType POLISHED_GRANITE_STAIRS = define(64, "polished_granite_stairs");
    public static final ItemType GLOW_BERRIES = define(64, "glow_berries", ItemAttribute.EDIBLE);
    public static final ItemType DEAD_HORN_CORAL = define(64, "dead_horn_coral");
    public static final ItemType DIRT = define(64, "dirt");
    public static final ItemType ORANGE_CONCRETE_POWDER = define(64, "orange_concrete_powder");
    public static final ItemType DARK_OAK_WOOD = define(64, "dark_oak_wood");
    public static final ItemType AZALEA = define(64, "azalea");
    public static final ItemType LEVER = define(64, "lever");
    public static final ItemType BLUE_BED = define(1, "blue_bed");
    public static final ItemType ACACIA_BOAT = define(1, "acacia_boat");
    public static final ItemType ACACIA_PRESSURE_PLATE = define(64, "acacia_pressure_plate");
    public static final ItemType ANDESITE_STAIRS = define(64, "andesite_stairs");
    public static final ItemType SKELETON_HORSE_SPAWN_EGG = define(64, "skeleton_horse_spawn_egg");
    public static final ItemType DARK_OAK_FENCE_GATE = define(64, "dark_oak_fence_gate");
    public static final ItemType COMPASS = define(64, "compass");
    public static final ItemType POLISHED_ANDESITE_STAIRS = define(64, "polished_andesite_stairs");
    public static final ItemType COAL = define(64, "coal");
    public static final ItemType WAXED_EXPOSED_CUT_COPPER = define(64, "waxed_exposed_cut_copper");
    public static final ItemType JUNGLE_PRESSURE_PLATE = define(64, "jungle_pressure_plate");
    public static final ItemType SMOOTH_QUARTZ_SLAB = define(64, "smooth_quartz_slab");
    public static final ItemType CYAN_BED = define(1, "cyan_bed");
    public static final ItemType STRIPPED_ACACIA_LOG = define(64, "stripped_acacia_log");
    public static final ItemType GOLDEN_PICKAXE = define(1, "golden_pickaxe", 32, ItemAttribute.GOLD_TIER);
    public static final ItemType SWEET_BERRIES = define(64, "sweet_berries", ItemAttribute.EDIBLE);
    public static final ItemType BOW = define(1, "bow", 384);
    public static final ItemType CRIMSON_ROOTS = define(64, "crimson_roots");
    public static final ItemType SMOOTH_SANDSTONE = define(64, "smooth_sandstone");
    public static final ItemType DEEPSLATE_BRICKS = define(64, "deepslate_bricks");
    public static final ItemType COBBLESTONE = define(64, "cobblestone");
    public static final ItemType REDSTONE_BLOCK = define(64, "redstone_block");
    public static final ItemType COOKED_COD = define(64, "cooked_cod", ItemAttribute.EDIBLE);
    public static final ItemType BONE_MEAL = define(64, "bone_meal");
    public static final ItemType HONEYCOMB_BLOCK = define(64, "honeycomb_block");
    public static final ItemType BRICK = define(64, "brick");
    public static final ItemType YELLOW_DYE = define(64, "yellow_dye");
    public static final ItemType CYAN_STAINED_GLASS = define(64, "cyan_stained_glass");
    public static final ItemType BRICKS = define(64, "bricks");
    public static final ItemType NETHERITE_CHESTPLATE = define(1, "netherite_chestplate", null, 592, ItemAttribute.FIRE_RESISTANT);
    public static final ItemType ORANGE_BED = define(1, "orange_bed");
    public static final ItemType PETRIFIED_OAK_SLAB = define(64, "petrified_oak_slab");
    public static final ItemType SALMON_SPAWN_EGG = define(64, "salmon_spawn_egg");
    public static final ItemType LEATHER_HORSE_ARMOR = define(1, "leather_horse_armor");
    public static final ItemType ORANGE_BANNER = define(16, "orange_banner");
    public static final ItemType TUBE_CORAL = define(64, "tube_coral");
    public static final ItemType PIG_SPAWN_EGG = define(64, "pig_spawn_egg");
    public static final ItemType STONE_BRICK_SLAB = define(64, "stone_brick_slab");
    public static final ItemType DARK_PRISMARINE = define(64, "dark_prismarine");
    public static final ItemType PAINTING = define(64, "painting");
    public static final ItemType PUMPKIN = define(64, "pumpkin");
    public static final ItemType TRAPPED_CHEST = define(64, "trapped_chest");
    public static final ItemType BROWN_SHULKER_BOX = define(1, "brown_shulker_box");
    public static final ItemType ORANGE_CONCRETE = define(64, "orange_concrete");
    public static final ItemType PUMPKIN_SEEDS = define(64, "pumpkin_seeds");
    public static final ItemType OAK_SAPLING = define(64, "oak_sapling");
    public static final ItemType BROWN_STAINED_GLASS_PANE = define(64, "brown_stained_glass_pane");
    public static final ItemType NETHER_BRICK_FENCE = define(64, "nether_brick_fence");
    public static final ItemType WHITE_BED = define(1, "white_bed");
    public static final ItemType BIRCH_SLAB = define(64, "birch_slab");
    public static final ItemType LILY_PAD = define(64, "lily_pad");
    public static final ItemType OBSERVER = define(64, "observer");
    public static final ItemType PODZOL = define(64, "podzol");
    public static final ItemType CYAN_WOOL = define(64, "cyan_wool");
    public static final ItemType STICK = define(64, "stick");
    public static final ItemType ANDESITE = define(64, "andesite");
    public static final ItemType DIAMOND_BOOTS = define(1, "diamond_boots", 429);
    public static final ItemType FIRE_CORAL_BLOCK = define(64, "fire_coral_block");
    public static final ItemType REPEATING_COMMAND_BLOCK = define(64, "repeating_command_block");
    public static final ItemType PACKED_ICE = define(64, "packed_ice");
    public static final ItemType DEEPSLATE_TILES = define(64, "deepslate_tiles");
    public static final ItemType GRAY_CARPET = define(64, "gray_carpet");
    public static final ItemType ENCHANTING_TABLE = define(64, "enchanting_table");
    public static final ItemType COD_SPAWN_EGG = define(64, "cod_spawn_egg");
    public static final ItemType GRAY_STAINED_GLASS_PANE = define(64, "gray_stained_glass_pane");
    public static final ItemType GLOW_INK_SAC = define(64, "glow_ink_sac");
    public static final ItemType EXPOSED_CUT_COPPER = define(64, "exposed_cut_copper");
    public static final ItemType LIME_DYE = define(64, "lime_dye");
    public static final ItemType WAXED_CUT_COPPER_STAIRS = define(64, "waxed_cut_copper_stairs");
    public static final ItemType ACACIA_SIGN = define(16, "acacia_sign");
    public static final ItemType POLISHED_BLACKSTONE_BRICK_WALL = define(64, "polished_blackstone_brick_wall");
    public static final ItemType RABBIT_STEW = define(1, "rabbit_stew", ItemAttribute.EDIBLE);
    public static final ItemType PEONY = define(64, "peony");
    public static final ItemType STONE_AXE = define(1, "stone_axe", 131, ItemAttribute.STONE_TIER);
    public static final ItemType DARK_OAK_LOG = define(64, "dark_oak_log");
    public static final ItemType POINTED_DRIPSTONE = define(64, "pointed_dripstone");
    public static final ItemType ROOTED_DIRT = define(64, "rooted_dirt");
    public static final ItemType POPPY = define(64, "poppy");
    public static final ItemType NETHERITE_SCRAP = define(64, "netherite_scrap", ItemAttribute.FIRE_RESISTANT);
    public static final ItemType RED_NETHER_BRICK_WALL = define(64, "red_nether_brick_wall");
    public static final ItemType ENDER_EYE = define(64, "ender_eye");
    public static final ItemType STONE_SWORD = define(1, "stone_sword", 131, ItemAttribute.STONE_TIER);
    public static final ItemType CUT_SANDSTONE = define(64, "cut_sandstone");
    public static final ItemType CHEST_MINECART = define(1, "chest_minecart");
    public static final ItemType STONE_BRICK_STAIRS = define(64, "stone_brick_stairs");
    public static final ItemType JUNGLE_SLAB = define(64, "jungle_slab");
    public static final ItemType CACTUS = define(64, "cactus");
    public static final ItemType SPRUCE_TRAPDOOR = define(64, "spruce_trapdoor");
    public static final ItemType NAME_TAG = define(64, "name_tag");
    public static final ItemType SPRUCE_LOG = define(64, "spruce_log");
    public static final ItemType BLACK_TERRACOTTA = define(64, "black_terracotta");
    public static final ItemType GRAY_GLAZED_TERRACOTTA = define(64, "gray_glazed_terracotta");
    public static final ItemType BIRCH_BUTTON = define(64, "birch_button");
    public static final ItemType RED_SANDSTONE_SLAB = define(64, "red_sandstone_slab");
    public static final ItemType OXIDIZED_CUT_COPPER_STAIRS = define(64, "oxidized_cut_copper_stairs");
    public static final ItemType ORANGE_TERRACOTTA = define(64, "orange_terracotta");
    public static final ItemType BROWN_DYE = define(64, "brown_dye");
    public static final ItemType OXIDIZED_CUT_COPPER = define(64, "oxidized_cut_copper");
    public static final ItemType LIGHT_GRAY_BED = define(1, "light_gray_bed");
    public static final ItemType SOUL_CAMPFIRE = define(64, "soul_campfire");
    public static final ItemType SALMON_BUCKET = define(1, "salmon_bucket");
    public static final ItemType BRICK_SLAB = define(64, "brick_slab");
    public static final ItemType SPRUCE_LEAVES = define(64, "spruce_leaves");
    public static final ItemType COMMAND_BLOCK_MINECART = define(1, "command_block_minecart");
    public static final ItemType LIGHT_GRAY_TERRACOTTA = define(64, "light_gray_terracotta");
    public static final ItemType SPRUCE_SLAB = define(64, "spruce_slab");
    public static final ItemType RED_NETHER_BRICK_STAIRS = define(64, "red_nether_brick_stairs");
    public static final ItemType YELLOW_CANDLE = define(64, "yellow_candle");
    public static final ItemType GRAY_TERRACOTTA = define(64, "gray_terracotta");
    public static final ItemType SEA_LANTERN = define(64, "sea_lantern");
    public static final ItemType ICE = define(64, "ice");
    public static final ItemType GREEN_SHULKER_BOX = define(1, "green_shulker_box");
    public static final ItemType OAK_DOOR = define(64, "oak_door");
    public static final ItemType DARK_OAK_STAIRS = define(64, "dark_oak_stairs");
    public static final ItemType BLUE_SHULKER_BOX = define(1, "blue_shulker_box");
    public static final ItemType IRON_AXE = define(1, "iron_axe", 250, ItemAttribute.IRON_TIER);
    public static final ItemType SMOKER = define(64, "smoker");
    public static final ItemType LOOM = define(64, "loom");
    public static final ItemType POLISHED_BLACKSTONE = define(64, "polished_blackstone");
    public static final ItemType NETHER_WART = define(64, "nether_wart");
    public static final ItemType OAK_LEAVES = define(64, "oak_leaves");
    public static final ItemType COBBLED_DEEPSLATE_SLAB = define(64, "cobbled_deepslate_slab");
    public static final ItemType COMPOSTER = define(64, "composter");
    public static final ItemType MUTTON = define(64, "mutton", ItemAttribute.EDIBLE);
    public static final ItemType COPPER_ORE = define(64, "copper_ore");
    public static final ItemType KNOWLEDGE_BOOK = define(1, "knowledge_book");
    public static final ItemType OBSIDIAN = define(64, "obsidian");
    public static final ItemType CYAN_CARPET = define(64, "cyan_carpet");
    public static final ItemType SKULL_BANNER_PATTERN = define(1, "skull_banner_pattern");
    public static final ItemType FIREWORK_STAR = define(64, "firework_star");
    public static final ItemType MUSIC_DISC_MELLOHI = define(1, "music_disc_mellohi", ItemAttribute.MUSIC_DISC);
    public static final ItemType PURPLE_CARPET = define(64, "purple_carpet");
    public static final ItemType GOLDEN_HOE = define(1, "golden_hoe", 32, ItemAttribute.GOLD_TIER);
    public static final ItemType COOKED_CHICKEN = define(64, "cooked_chicken", ItemAttribute.EDIBLE);
    public static final ItemType DOLPHIN_SPAWN_EGG = define(64, "dolphin_spawn_egg");
    public static final ItemType COARSE_DIRT = define(64, "coarse_dirt");
    public static final ItemType WHITE_CANDLE = define(64, "white_candle");
    public static final ItemType DARK_PRISMARINE_STAIRS = define(64, "dark_prismarine_stairs");
    public static final ItemType JUNGLE_BUTTON = define(64, "jungle_button");
    public static final ItemType DEAD_TUBE_CORAL_BLOCK = define(64, "dead_tube_coral_block");
    public static final ItemType DARK_OAK_BOAT = define(1, "dark_oak_boat");
    public static final ItemType COOKED_MUTTON = define(64, "cooked_mutton", ItemAttribute.EDIBLE);
    public static final ItemType JUNGLE_FENCE = define(64, "jungle_fence");
    public static final ItemType JUKEBOX = define(64, "jukebox");
    public static final ItemType PURPLE_STAINED_GLASS_PANE = define(64, "purple_stained_glass_pane");
    public static final ItemType BIRCH_TRAPDOOR = define(64, "birch_trapdoor");
    public static final ItemType APPLE = define(64, "apple", ItemAttribute.EDIBLE);
    public static final ItemType ELDER_GUARDIAN_SPAWN_EGG = define(64, "elder_guardian_spawn_egg");
    public static final ItemType SPIDER_EYE = define(64, "spider_eye", ItemAttribute.EDIBLE);
    public static final ItemType ZOGLIN_SPAWN_EGG = define(64, "zoglin_spawn_egg");
    public static final ItemType PIGLIN_BANNER_PATTERN = define(1, "piglin_banner_pattern");
    public static final ItemType GOLDEN_BOOTS = define(1, "golden_boots", 91);
    public static final ItemType LILY_OF_THE_VALLEY = define(64, "lily_of_the_valley");
    public static final ItemType BLUE_ORCHID = define(64, "blue_orchid");
    public static final ItemType PUMPKIN_PIE = define(64, "pumpkin_pie", ItemAttribute.EDIBLE);
    public static final ItemType RED_SANDSTONE_STAIRS = define(64, "red_sandstone_stairs");
    public static final ItemType SQUID_SPAWN_EGG = define(64, "squid_spawn_egg");
    public static final ItemType CRAFTING_TABLE = define(64, "crafting_table");
    public static final ItemType CAVE_SPIDER_SPAWN_EGG = define(64, "cave_spider_spawn_egg");
    public static final ItemType COBBLESTONE_STAIRS = define(64, "cobblestone_stairs");
    public static final ItemType BROWN_MUSHROOM_BLOCK = define(64, "brown_mushroom_block");
    public static final ItemType LIGHT_GRAY_CANDLE = define(64, "light_gray_candle");
    public static final ItemType DIAMOND_BLOCK = define(64, "diamond_block");
    public static final ItemType END_STONE_BRICKS = define(64, "end_stone_bricks");
    public static final ItemType GOLDEN_CARROT = define(64, "golden_carrot", ItemAttribute.EDIBLE);
    public static final ItemType STONE = define(64, "stone");
    public static final ItemType NETHER_BRICK_WALL = define(64, "nether_brick_wall");
    public static final ItemType CRIMSON_SIGN = define(16, "crimson_sign");
    public static final ItemType DARK_OAK_TRAPDOOR = define(64, "dark_oak_trapdoor");
    public static final ItemType PRISMARINE_WALL = define(64, "prismarine_wall");
    public static final ItemType ENCHANTED_GOLDEN_APPLE = define(64, "enchanted_golden_apple", ItemAttribute.EDIBLE);
    public static final ItemType MAGMA_CREAM = define(64, "magma_cream");
    public static final ItemType GHAST_SPAWN_EGG = define(64, "ghast_spawn_egg");
    public static final ItemType PINK_BANNER = define(16, "pink_banner");
    public static final ItemType EXPOSED_CUT_COPPER_SLAB = define(64, "exposed_cut_copper_slab");
    public static final ItemType MELON_SEEDS = define(64, "melon_seeds");
    public static final ItemType MUSIC_DISC_CAT = define(1, "music_disc_cat", ItemAttribute.MUSIC_DISC);
    public static final ItemType RED_SANDSTONE = define(64, "red_sandstone");
    public static final ItemType PURPLE_DYE = define(64, "purple_dye");
    public static final ItemType COBBLED_DEEPSLATE_WALL = define(64, "cobbled_deepslate_wall");
    public static final ItemType FIRE_CHARGE = define(64, "fire_charge");
    public static final ItemType CHISELED_RED_SANDSTONE = define(64, "chiseled_red_sandstone");
    public static final ItemType TUBE_CORAL_BLOCK = define(64, "tube_coral_block");
    public static final ItemType SANDSTONE_STAIRS = define(64, "sandstone_stairs");
    public static final ItemType POWDER_SNOW_BUCKET = define(1, "powder_snow_bucket");
    public static final ItemType AXOLOTL_SPAWN_EGG = define(64, "axolotl_spawn_egg");
    public static final ItemType WHITE_SHULKER_BOX = define(1, "white_shulker_box");
    public static final ItemType DEEPSLATE_BRICK_STAIRS = define(64, "deepslate_brick_stairs");
    public static final ItemType FERN = define(64, "fern");
    public static final ItemType SKELETON_SPAWN_EGG = define(64, "skeleton_spawn_egg");
    public static final ItemType PUFFERFISH_SPAWN_EGG = define(64, "pufferfish_spawn_egg");
    public static final ItemType GOAT_SPAWN_EGG = define(64, "goat_spawn_egg");
    public static final ItemType LIGHT_BLUE_CARPET = define(64, "light_blue_carpet");
    public static final ItemType DIORITE_SLAB = define(64, "diorite_slab");
    public static final ItemType LIME_BANNER = define(16, "lime_banner");
    public static final ItemType SOUL_SOIL = define(64, "soul_soil");
    public static final ItemType GOLDEN_LEGGINGS = define(1, "golden_leggings", 105);
    public static final ItemType DARK_OAK_SAPLING = define(64, "dark_oak_sapling");
    public static final ItemType POLISHED_DIORITE_STAIRS = define(64, "polished_diorite_stairs");
    public static final ItemType ENDERMITE_SPAWN_EGG = define(64, "endermite_spawn_egg");
    public static final ItemType TUBE_CORAL_FAN = define(64, "tube_coral_fan");
    public static final ItemType LIME_GLAZED_TERRACOTTA = define(64, "lime_glazed_terracotta");
    public static final ItemType MEDIUM_AMETHYST_BUD = define(64, "medium_amethyst_bud");
    public static final ItemType MAGENTA_DYE = define(64, "magenta_dye");
    public static final ItemType CRIMSON_FUNGUS = define(64, "crimson_fungus");
    public static final ItemType LEATHER = define(64, "leather");
    public static final ItemType CRACKED_NETHER_BRICKS = define(64, "cracked_nether_bricks");
    public static final ItemType BIRCH_BOAT = define(1, "birch_boat");
    public static final ItemType NETHER_BRICK_STAIRS = define(64, "nether_brick_stairs");
    public static final ItemType COMMAND_BLOCK = define(64, "command_block");
    public static final ItemType WANDERING_TRADER_SPAWN_EGG = define(64, "wandering_trader_spawn_egg");
    public static final ItemType VILLAGER_SPAWN_EGG = define(64, "villager_spawn_egg");
    public static final ItemType TUFF = define(64, "tuff");
    public static final ItemType SNOWBALL = define(16, "snowball");
    public static final ItemType GRAY_CONCRETE = define(64, "gray_concrete");
    public static final ItemType LIGHT_BLUE_SHULKER_BOX = define(1, "light_blue_shulker_box");
    public static final ItemType SMOOTH_QUARTZ_STAIRS = define(64, "smooth_quartz_stairs");
    public static final ItemType GREEN_CANDLE = define(64, "green_candle");
    public static final ItemType STONE_BRICK_WALL = define(64, "stone_brick_wall");
    public static final ItemType GLASS_BOTTLE = define(64, "glass_bottle");
    public static final ItemType RESPAWN_ANCHOR = define(64, "respawn_anchor");
    public static final ItemType RED_BANNER = define(16, "red_banner");
    public static final ItemType CRIMSON_NYLIUM = define(64, "crimson_nylium");
    public static final ItemType WEATHERED_CUT_COPPER = define(64, "weathered_cut_copper");
    public static final ItemType ACACIA_WOOD = define(64, "acacia_wood");
    public static final ItemType BEE_SPAWN_EGG = define(64, "bee_spawn_egg");
    public static final ItemType LARGE_AMETHYST_BUD = define(64, "large_amethyst_bud");
    public static final ItemType LIME_STAINED_GLASS = define(64, "lime_stained_glass");
    public static final ItemType MAGENTA_SHULKER_BOX = define(1, "magenta_shulker_box");
    public static final ItemType GLOBE_BANNER_PATTERN = define(1, "globe_banner_pattern");
    public static final ItemType POLISHED_DEEPSLATE_STAIRS = define(64, "polished_deepslate_stairs");
    public static final ItemType TIPPED_ARROW = define(64, "tipped_arrow");
    public static final ItemType ORANGE_CANDLE = define(64, "orange_candle");
    public static final ItemType YELLOW_STAINED_GLASS = define(64, "yellow_stained_glass");
    public static final ItemType ENDER_PEARL = define(16, "ender_pearl");
    public static final ItemType DEEPSLATE_DIAMOND_ORE = define(64, "deepslate_diamond_ore");
    public static final ItemType GOLDEN_APPLE = define(64, "golden_apple", ItemAttribute.EDIBLE);
    public static final ItemType PRISMARINE_SLAB = define(64, "prismarine_slab");
    public static final ItemType DRIED_KELP = define(64, "dried_kelp", ItemAttribute.EDIBLE);
    public static final ItemType EVOKER_SPAWN_EGG = define(64, "evoker_spawn_egg");
    public static final ItemType PRISMARINE_SHARD = define(64, "prismarine_shard");
    public static final ItemType LEAD = define(64, "lead");
    public static final ItemType LAPIS_BLOCK = define(64, "lapis_block");
    public static final ItemType MAGENTA_WOOL = define(64, "magenta_wool");
    public static final ItemType STONE_HOE = define(1, "stone_hoe", 131, ItemAttribute.STONE_TIER);
    public static final ItemType BEETROOT_SEEDS = define(64, "beetroot_seeds");
    public static final ItemType PANDA_SPAWN_EGG = define(64, "panda_spawn_egg");
    public static final ItemType CORNFLOWER = define(64, "cornflower");
    public static final ItemType SHULKER_SHELL = define(64, "shulker_shell");
    public static final ItemType OAK_FENCE_GATE = define(64, "oak_fence_gate");
    public static final ItemType STRIPPED_JUNGLE_WOOD = define(64, "stripped_jungle_wood");
    public static final ItemType ORANGE_SHULKER_BOX = define(1, "orange_shulker_box");
    public static final ItemType IRON_DOOR = define(64, "iron_door");
    public static final ItemType SPRUCE_BOAT = define(1, "spruce_boat");
    public static final ItemType SMOOTH_QUARTZ = define(64, "smooth_quartz");
    public static final ItemType BARRIER = define(64, "barrier");
    public static final ItemType GRAY_CANDLE = define(64, "gray_candle");
    public static final ItemType RABBIT_HIDE = define(64, "rabbit_hide");
    public static final ItemType PINK_WOOL = define(64, "pink_wool");
    public static final ItemType PILLAGER_SPAWN_EGG = define(64, "pillager_spawn_egg");
    public static final ItemType CAMPFIRE = define(64, "campfire");
    public static final ItemType DEEPSLATE_BRICK_WALL = define(64, "deepslate_brick_wall");
    public static final ItemType WITHER_ROSE = define(64, "wither_rose");
    public static final ItemType LIGHT_GRAY_GLAZED_TERRACOTTA = define(64, "light_gray_glazed_terracotta");
    public static final ItemType JUNGLE_BOAT = define(1, "jungle_boat");
    public static final ItemType EXPOSED_CUT_COPPER_STAIRS = define(64, "exposed_cut_copper_stairs");
    public static final ItemType SALMON = define(64, "salmon", ItemAttribute.EDIBLE);
    public static final ItemType FOX_SPAWN_EGG = define(64, "fox_spawn_egg");
    public static final ItemType DIAMOND_HOE = define(1, "diamond_hoe", 1561, ItemAttribute.DIAMOND_TIER);
    public static final ItemType POLISHED_BLACKSTONE_BRICK_SLAB = define(64, "polished_blackstone_brick_slab");
    public static final ItemType TWISTING_VINES = define(64, "twisting_vines");
    public static final ItemType TURTLE_EGG = define(64, "turtle_egg");
    public static final ItemType RED_GLAZED_TERRACOTTA = define(64, "red_glazed_terracotta");
    public static final ItemType ITEM_FRAME = define(64, "item_frame");
    public static final ItemType RED_TULIP = define(64, "red_tulip");
    public static final ItemType COAL_ORE = define(64, "coal_ore");
    public static final ItemType BIRCH_SAPLING = define(64, "birch_sapling");
    public static final ItemType POLISHED_ANDESITE = define(64, "polished_andesite");
    public static final ItemType BRICK_STAIRS = define(64, "brick_stairs");
    public static final ItemType DIORITE_STAIRS = define(64, "diorite_stairs");
    public static final ItemType SHROOMLIGHT = define(64, "shroomlight");
    public static final ItemType SPIDER_SPAWN_EGG = define(64, "spider_spawn_egg");
    public static final ItemType TRIPWIRE_HOOK = define(64, "tripwire_hook");
    public static final ItemType STRIPPED_SPRUCE_WOOD = define(64, "stripped_spruce_wood");
    public static final ItemType SPRUCE_BUTTON = define(64, "spruce_button");
    public static final ItemType PAPER = define(64, "paper");
    public static final ItemType MAGMA_CUBE_SPAWN_EGG = define(64, "magma_cube_spawn_egg");
    public static final ItemType DEEPSLATE_TILE_WALL = define(64, "deepslate_tile_wall");
    public static final ItemType JUNGLE_LOG = define(64, "jungle_log");
    public static final ItemType IRON_PICKAXE = define(1, "iron_pickaxe", 250, ItemAttribute.IRON_TIER);
    public static final ItemType DARK_OAK_SLAB = define(64, "dark_oak_slab");
    public static final ItemType NETHERRACK = define(64, "netherrack");
    public static final ItemType POLISHED_DEEPSLATE_WALL = define(64, "polished_deepslate_wall");
    public static final ItemType BIRCH_LOG = define(64, "birch_log");
    public static final ItemType LIGHT_WEIGHTED_PRESSURE_PLATE = define(64, "light_weighted_pressure_plate");
    public static final ItemType WARPED_SIGN = define(16, "warped_sign");
    public static final ItemType ACTIVATOR_RAIL = define(64, "activator_rail");
    public static final ItemType PUFFERFISH = define(64, "pufferfish", ItemAttribute.EDIBLE);
    public static final ItemType OAK_PLANKS = define(64, "oak_planks");
    public static final ItemType GOLD_ORE = define(64, "gold_ore");
    public static final ItemType SHULKER_BOX = define(1, "shulker_box");
    public static final ItemType RAW_GOLD = define(64, "raw_gold");
    public static final ItemType CRIMSON_PRESSURE_PLATE = define(64, "crimson_pressure_plate");
    public static final ItemType FERMENTED_SPIDER_EYE = define(64, "fermented_spider_eye");
    public static final ItemType CHISELED_QUARTZ_BLOCK = define(64, "chiseled_quartz_block");
    public static final ItemType HOPPER_MINECART = define(1, "hopper_minecart");
    public static final ItemType FLOWERING_AZALEA = define(64, "flowering_azalea");
    public static final ItemType YELLOW_GLAZED_TERRACOTTA = define(64, "yellow_glazed_terracotta");
    public static final ItemType CYAN_DYE = define(64, "cyan_dye");
    public static final ItemType QUARTZ_SLAB = define(64, "quartz_slab");
    public static final ItemType BLUE_STAINED_GLASS_PANE = define(64, "blue_stained_glass_pane");
    public static final ItemType MOSSY_STONE_BRICK_WALL = define(64, "mossy_stone_brick_wall");
    public static final ItemType GRANITE = define(64, "granite");
    public static final ItemType RED_MUSHROOM = define(64, "red_mushroom");
    public static final ItemType INFESTED_COBBLESTONE = define(64, "infested_cobblestone");
    public static final ItemType PINK_CONCRETE = define(64, "pink_concrete");
    public static final ItemType CARROT = define(64, "carrot", ItemAttribute.EDIBLE);
}
