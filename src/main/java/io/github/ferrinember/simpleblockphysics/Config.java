package io.github.ferrinember.simpleblockphysics;


import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.*;
import java.util.stream.Collectors;


@Mod.EventBusSubscriber(modid = SimpleBlockPhysics.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class Config
{
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    // a list of strings that are treated as resource locations for items
    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> INDUS_BLOCK_STRINGS = BUILDER
            .comment("A list of indestructible blocks that will always count as support and never fall themselves.")
            .defineListAllowEmpty("indestructibleBlocks", List.of("minecraft:bedrock", "minecraft:command_block", "minecraft:barrier", "minecraft:structure_block", "minecraft:structure_void", "minecraft:reinforced_deepslate", "minecraft:end_portal_frame"), Config::validateBlockName);

    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> ALLOWED_DIMENSIONS = BUILDER
            .comment("A list of dimensions allowed to have physics behavior. The Nether, and especially the End, are not recommended as both can easily experience catastrophic structural failure.")
            .defineListAllowEmpty("allowedDimensions", List.of("minecraft:overworld"),  e -> e instanceof String && ((String) e).contains(":"));

    private static final ForgeConfigSpec.DoubleValue BLOCK_BREAK_VOLUME = BUILDER
            .comment("Block Break Volume (caused by mod).")
            .defineInRange("blockBreakVolume",1,0,Double.MAX_VALUE);

    private static final ForgeConfigSpec.IntValue SUPPORT_LENGTH_MAX = BUILDER
            .comment("Support Strength Max. This value will be used by anything with a default hardness equal to or greater than 7 (iron blocks, obsidian, etc...).")
            .defineInRange("supportLengthMax", 10, 1, Integer.MAX_VALUE);

    private static final ForgeConfigSpec.IntValue SUPPORT_LENGTH_MIN = BUILDER
            .comment("Support Strength Min. This value will be used by anything with a default hardness equal to 0 (honey blocks, slime blocks, etc...).")
            .defineInRange("supportLengthMin", 1, 1, Integer.MAX_VALUE);

    private static final ForgeConfigSpec.IntValue DMG_DIST = BUILDER
            .comment("Base block entity fall damage inflicted per block fallen.")
            .defineInRange("dmgDist", 1, 0, Integer.MAX_VALUE);

    private static final ForgeConfigSpec.IntValue DMG_MAX = BUILDER
            .comment("Max block entity fall damage inflicted. Max is taken from support strength (i.e. falling obsidian hurts more than dirt), but is overwritten if greater than this value.")
            .defineInRange("dmgMax", 10, 0, Integer.MAX_VALUE);

    private static final ForgeConfigSpec.IntValue MAX_FALLING_BLOCK = BUILDER
            .comment("Max number of falling entities allowed to be generated by the mod at any one time. Blocks that should 'fall' will wait to avoid exceeding this number. Lower for better performance, raise for faster collapses, set to 0 to turn the mod off.")
            .defineInRange("maxFallingBlockEntity", 5000, 0, Integer.MAX_VALUE);

    private static final ForgeConfigSpec.IntValue SUPPORT_SEARCH_ITER = BUILDER
            .comment("Max number of iterative supports to scan.")
            .defineInRange("supportSearchIter", 4, 0, Integer.MAX_VALUE);

    private static final ForgeConfigSpec.DoubleValue FALLING_BLOCK_BREAK_FACTOR = BUILDER
            .comment("A factor influencing the chance of a falling block to break on ground contact, rather than be placed or shift over, which is also influenced by speed. Increase this value to make contact breaks more frequent (1 will always break), and decrease to make them less frequent (0 will never break).")
            .defineInRange("fallingBlockBreakFactor", 0.5, 0, 1);

    private static final ForgeConfigSpec.DoubleValue FALLING_BLOCK_ITEM_DROP_CHANCE = BUILDER
            .comment("Percent chance that a destroyed falling block will drop its item. 100 will always drop, 0 will never drop.")
            .defineInRange("fallingBlockItemDropChance", 0, 0, 1D);

    private static final ForgeConfigSpec.BooleanValue REMOVE_BLOCKS_INSTEAD_OF_FALL = BUILDER
            .comment("Causes collapsing blocks to be destroyed directly instead of generating a falling block entity. Dramatically improves performance. Uses fallingBlockItemDropChance to determine how often they should drop their item. Uses maxFallingBlockEntity to determine how many blocks to allow to break per tick.")
            .define("removeBlocksInsteadOfFall",false);

    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> OVERWRITE_TAG_STRINGS = BUILDER
            .comment("A list of vanilla blocktags to assign custom support strength values (rather than the hardness-based default). Listed blocktags without a matching support value at its index (below) will use default values.")
            .defineListAllowEmpty("overwriteBlockTags", List.of("minecraft:leaves"), Config::validateTagName);

    private static final ForgeConfigSpec.ConfigValue<List<? extends Integer>> OVERWRITE_TAG_INTS = BUILDER
            .comment("A list of support strength values to override native (hardness based) blocktag support strength, matched by index (order) to above list.")
            .defineListAllowEmpty("overwriteBlockTagValues", List.of(4), e -> e instanceof Integer);

    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> OVERWRITE_BLOCK_STRINGS = BUILDER
            .comment("As the blocktag list above, but with individual blocks. Specified blocks will overwrite both default and blocktag specified support values.")
            .defineListAllowEmpty("overwriteBlocks", List.of("minecraft:netherrack"), Config::validateBlockName);

    private static final ForgeConfigSpec.ConfigValue<List<? extends Integer>> OVERWRITE_BLOCK_INTS = BUILDER
            .comment("A list of support strength values to override native (hardness based) individual block support strength, matched by index to above list.")
            .defineListAllowEmpty("overwriteBlockValues", List.of(4), e -> e instanceof Integer);


    static final ForgeConfigSpec SPEC = BUILDER.build();

    public static Set<Block> indestructibleBlocks;
    public static Set<ResourceKey<Level>> allowedDimensions;
    public static Double blockBreakVolume;
    public static Integer supportLengthMax;
    public static Integer supportLengthMin;
    public static Integer dmgDist;
    public static Integer dmgMax;
    public static Integer maxFallingBlockEntity;
    public static Integer supportSearchIter;
    public static Double fallingBlockBreakFactor;
    public static Double fallingBlockItemDropChance;
    public static Boolean removeBlocksInsteadOfFall;
    public static List<TagKey<Block>> overwrittenBlockTags;
    public static List<Integer> overwrittenBlockTagValues;
    public static HashMap<Block,Integer> overwrittenBlockMap;
    public static Set<Block> overwrittenBlocks;
    public static List<Integer> overwrittenBlockValues;
    public static HashMap<TagKey<Block>,Integer> overwrittenTagMap;

    private static boolean validateBlockName(final Object obj)
    {
        return obj instanceof final String blockName && ForgeRegistries.BLOCKS.containsKey(new ResourceLocation(blockName));
    }


    private static boolean validateTagName(final Object obj)
    {
        return obj instanceof final String tagName && ForgeRegistries.BLOCKS.tags().isKnownTagName(ForgeRegistries.BLOCKS.tags().createTagKey(new ResourceLocation(tagName)));
    }

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event)
    {
        //handle indestructible blocks
        indestructibleBlocks = INDUS_BLOCK_STRINGS.get().stream()
                .map(blockName -> ForgeRegistries.BLOCKS.getValue(new ResourceLocation(blockName)))
                .collect(Collectors.toSet());
        blockBreakVolume = BLOCK_BREAK_VOLUME.get();
        supportLengthMax = SUPPORT_LENGTH_MAX.get();
        supportLengthMin = SUPPORT_LENGTH_MIN.get();
        dmgDist = DMG_DIST.get();
        dmgMax = DMG_MAX.get();
        maxFallingBlockEntity = MAX_FALLING_BLOCK.get();
        supportSearchIter = SUPPORT_SEARCH_ITER.get();
        fallingBlockBreakFactor = FALLING_BLOCK_BREAK_FACTOR.get();
        fallingBlockItemDropChance = FALLING_BLOCK_ITEM_DROP_CHANCE.get();
        removeBlocksInsteadOfFall = REMOVE_BLOCKS_INSTEAD_OF_FALL.get();

        //handle dimensions
        allowedDimensions = ALLOWED_DIMENSIONS.get().stream()
                .map(dimName -> ResourceKey.create(Registries.DIMENSION, new ResourceLocation(dimName)))
                .collect(Collectors.toSet());

        //handle block overwrites
        overwrittenBlocks = OVERWRITE_BLOCK_STRINGS.get().stream()
                .map(blockName -> ForgeRegistries.BLOCKS.getValue(new ResourceLocation(blockName)))
                .collect(Collectors.toSet());
        overwrittenBlockValues = new ArrayList<>(OVERWRITE_BLOCK_INTS.get());
        overwrittenBlockMap = new HashMap<>();
        overwrittenBlocks.forEach(block -> {
            if (overwrittenBlockValues.isEmpty()) {
                overwrittenBlockMap.put(block,-1);
            }
            else {
                overwrittenBlockMap.put(block, overwrittenBlockValues.remove(0));
            }
        });

        //handle blockTag overwrites
        overwrittenBlockTags = OVERWRITE_TAG_STRINGS.get().stream()
                .map(tagName -> ForgeRegistries.BLOCKS.tags().createTagKey(new ResourceLocation(tagName)))
                .collect(Collectors.toList());
        overwrittenBlockTagValues = new ArrayList<>(OVERWRITE_TAG_INTS.get());
        overwrittenTagMap = new HashMap<>();
        overwrittenBlockTags.forEach(blockTagKey -> {
            if (overwrittenBlockTagValues.isEmpty()) {
                overwrittenTagMap.put(blockTagKey,-1);
            }
            else {
                overwrittenTagMap.put(blockTagKey, overwrittenBlockTagValues.remove(0));
            }
        });



    }
}
