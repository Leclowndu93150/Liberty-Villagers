package com.leclowndu93150.libertyvillagers.cmds;

import com.leclowndu93150.libertyvillagers.LibertyVillagersClientInitializer;
import com.leclowndu93150.libertyvillagers.LibertyVillagersMod;
import com.leclowndu93150.libertyvillagers.LibertyVillagersServerInitializer;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.Filterable;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiRecord;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.animal.CatVariant;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.WrittenBookContent;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

import static com.leclowndu93150.libertyvillagers.LibertyVillagersMod.CONFIG;
import static net.minecraft.commands.Commands.literal;

class ProfessionInfo {
    public VillagerProfession profession;
    public int countVillagersWithProfession;

    public ProfessionInfo(VillagerProfession profession, int countVillagersWithProfession) {
        this.profession = profession;
        this.countVillagersWithProfession = countVillagersWithProfession;
    }

    public static ProfessionInfo mergeProfessionInfo(ProfessionInfo oldVal, ProfessionInfo newVal) {
        return new ProfessionInfo(oldVal.profession,
                oldVal.countVillagersWithProfession + newVal.countVillagersWithProfession);
    }
}

public class VillagerStats {
    private static final int LINES_PER_PAGE = 14;


    public static void processVillagerStats(CommandContext<CommandSourceStack> command) {
        CommandSourceStack source = command.getSource();
        ServerPlayer player = source.getPlayer();
        ServerLevel serverWorld = source.getLevel();

        List<Villager> villagers = serverWorld.getEntitiesOfClass(Villager.class,
                player.getBoundingBox().inflate(CONFIG.debugConfig.villagerStatRange));

        List<Filterable<Component>> pages = new LinkedList<>();
        pages.addAll(splitToPageTags(titlePage(player, villagers, serverWorld)));
        pages.addAll(splitToPageTags(professionPage(villagers)));
        pages.addAll(splitToPageTags(heldWorkstationPage(player, serverWorld)));
        pages.addAll(splitToPageTags(freeWorkstationsPage(player, serverWorld)));
        pages.addAll(splitToPageTags(homelessPage(villagers)));
        pages.addAll(splitToPageTags(availableBedsPage(player, serverWorld)));
        pages.addAll(splitToPageTags(golems(player, serverWorld)));
        pages.addAll(splitToPageTags(cats(player, serverWorld)));

        ItemStack bookStack = new ItemStack(Items.WRITTEN_BOOK);
        WrittenBookContent bookContent = new WrittenBookContent(
                Filterable.passThrough(Component.translatable("text.LibertyVillagers.villagerStats.title").getString()),
                Objects.requireNonNull(player.getDisplayName()).toString(),
                0,
                pages,
                true
        );

        bookStack.set(DataComponents.WRITTEN_BOOK_CONTENT, bookContent);

        if (LibertyVillagersMod.isClient()) {
            LibertyVillagersClientInitializer.openBookScreen(bookStack);
        } else {
            LibertyVillagersServerInitializer.openBookScreen(bookStack, player);
        }
    }


    private static Collection<Filterable<Component>> splitToPageTags(String string) {
        final List<String> lines = LibertyVillagersMod.isClient() ? LibertyVillagersClientInitializer.wrapText(string) :
                LibertyVillagersServerInitializer.wrapText(string);

        List<Filterable<Component>> pageTags = new LinkedList<>();

        int linesRemaining = LINES_PER_PAGE;
        StringBuilder curString = new StringBuilder();
        while (!lines.isEmpty()) {
            curString.append(lines.remove(0));
            linesRemaining--;
            if (linesRemaining <= 0) {
                linesRemaining = LINES_PER_PAGE;
                pageTags.add(Filterable.passThrough(Component.nullToEmpty(curString.toString())));
                curString = new StringBuilder();
            }
        }

        if (!curString.isEmpty()) {
            pageTags.add(Filterable.passThrough(Component.nullToEmpty(curString.toString())));
        }

        return pageTags;
    }


    protected static String titlePage(ServerPlayer player, List<Villager> villagers,
                                      ServerLevel serverWorld) {
        String pageString = Component.translatable("text.LibertyVillagers.villagerStats.title").getString() + "\n\n";

        pageString += Component.translatable("text.LibertyVillagers.villagerStats.format",
                Component.translatable("text.LibertyVillagers.villagerStats.numberOfVillagers").getString(),
                villagers.size()).getString() + "\n";

        int babies = 0;
        int nitwits = 0;
        int unemployed = 0;
        int homeless = 0;
        for (Villager villager : villagers) {
            if (villager.getVillagerData().getProfession() == VillagerProfession.NITWIT) {
                nitwits++;
            }
            if (villager.getVillagerData().getProfession() == VillagerProfession.NONE) {
                unemployed++;
            }
            if (villager.isBaby()) {
                babies++;
            }
            if (!villager.getBrain().hasMemoryValue(MemoryModuleType.HOME)) {
                homeless++;
            }
        }

        pageString += Component.translatable("text.LibertyVillagers.villagerStats.format",
                        Component.translatable("text.LibertyVillagers.villagerStats.numberOfNitwits").getString(), nitwits)
                .getString() + "\n";

        pageString += Component.translatable("text.LibertyVillagers.villagerStats.format",
                        Component.translatable("text.LibertyVillagers.villagerStats.numberOfUnemployed").getString(), unemployed)
                .getString() + "\n";

        pageString += Component.translatable("text.LibertyVillagers.villagerStats.format",
                        Component.translatable("text.LibertyVillagers.villagerStats.numberOfBabies").getString(), babies)
                .getString() + "\n";

        pageString += Component.translatable("text.LibertyVillagers.villagerStats.format",
                        Component.translatable("text.LibertyVillagers.villagerStats.numberOfHomeless").getString(), homeless)
                .getString() + "\n";

        List<IronGolem> golems = serverWorld.getEntitiesOfClass(IronGolem.class,
                player.getBoundingBox().inflate(CONFIG.debugConfig.villagerStatRange));
        pageString += Component.translatable("text.LibertyVillagers.villagerStats.format",
                        Component.translatable("text.LibertyVillagers.villagerStats.numberOfGolems").getString(), golems.size())
                .getString() + "\n";

        List<Cat> cats = serverWorld.getEntitiesOfClass(Cat.class,
                player.getBoundingBox().inflate(CONFIG.debugConfig.villagerStatRange));

        pageString += Component.translatable("text.LibertyVillagers.villagerStats.format",
                        Component.translatable("text.LibertyVillagers.villagerStats.numberOfCats").getString(), cats.size())
                .getString() + "\n";


        return pageString;
    }

    public static String translatedProfession(VillagerProfession profession) {
        String villagerTranslationKey = EntityType.VILLAGER.getDescriptionId();
        return
                Component.translatable(villagerTranslationKey + "." + BuiltInRegistries.VILLAGER_PROFESSION.getKey(profession).getPath()).getString();
    }

    protected static TreeMap<String, ProfessionInfo> createProfessionTreeMap() {
        TreeMap<String, ProfessionInfo> villagerProfessionMap = new TreeMap<>();

        for (Map.Entry<ResourceKey<VillagerProfession>, VillagerProfession> professionEntry :
                BuiltInRegistries.VILLAGER_PROFESSION.entrySet()) {
            VillagerProfession profession = professionEntry.getValue();
            String professionText = translatedProfession(profession);
            villagerProfessionMap.put(professionText, new ProfessionInfo(profession, 0));
        }

        return villagerProfessionMap;
    }

    protected static String professionPage(List<Villager> villagers) {
        String pageString = Component.translatable("text.LibertyVillagers.villagerStats.professions").getString() + "\n\n";
        TreeMap<String, ProfessionInfo> villagerProfessionMap = createProfessionTreeMap();

        for (Villager villager : villagers) {
            if (villager.isBaby()) {
                String babyText = Component.translatable("text.LibertyVillagers.villagerStats.baby").getString();
                villagerProfessionMap.merge(babyText, new ProfessionInfo(villager.getVillagerData().getProfession(), 1),
                        ProfessionInfo::mergeProfessionInfo);
            } else {
                villagerProfessionMap.merge(translatedProfession(villager.getVillagerData().getProfession()),
                        new ProfessionInfo(villager.getVillagerData().getProfession(), 1),
                        ProfessionInfo::mergeProfessionInfo);
            }
        }

        AtomicReference<String> professions = new AtomicReference<>("");
        villagerProfessionMap.forEach((villagerProfession, professionInfo) -> professions.set(professions.get() +
                Component.translatable("text.LibertyVillagers.villagerStats.professionsCountFormat", villagerProfession,
                        professionInfo.countVillagersWithProfession).getString() + "\n"));

        pageString += professions.get() + "\n\n";

        return pageString;
    }


    protected static String heldWorkstationPage(ServerPlayer player, ServerLevel serverWorld) {
        String pageString =
                Component.translatable("text.LibertyVillagers.villagerStats.professionsHeldJobSites").getString() + "\n\n";
        TreeMap<String, ProfessionInfo> villagerProfessionMap = createProfessionTreeMap();
        AtomicReference<String> heldWorkstations = new AtomicReference<>("");
        villagerProfessionMap.forEach((villagerProfession, professionInfo) -> {
            long numOccupiedWorkstations = 0;
            if (!Objects.equals(villagerProfession, "baby")) {
                numOccupiedWorkstations = serverWorld.getPoiManager()
                        .getCountInRange(professionInfo.profession.heldJobSite(), player.blockPosition(),
                                CONFIG.debugConfig.villagerStatRange,
                                PoiManager.Occupancy.IS_OCCUPIED);
            }
            heldWorkstations.set(heldWorkstations.get() +
                    Component.translatable("text.LibertyVillagers.villagerStats.professionsCountFormat", villagerProfession,
                            numOccupiedWorkstations).getString() + "\n");
        });

        pageString += heldWorkstations.get() + "\n\n";

        return pageString;
    }


    protected static String freeWorkstationsPage(ServerPlayer player, ServerLevel serverWorld) {
        String pageString =
                Component.translatable("text.LibertyVillagers.villagerStats.professionsAvailableJobSites").getString() +
                        "\n\n";
        TreeMap<String, ProfessionInfo> villagerProfessionMap = createProfessionTreeMap();

        AtomicReference<String> availableWorkstations = new AtomicReference<>("");
        villagerProfessionMap.forEach((villagerProfession, professionInfo) -> {
            long numAvailableWorkstations = 0;
            if (!Objects.equals(villagerProfession, "baby")) {
                numAvailableWorkstations = serverWorld.getPoiManager()
                        .getCountInRange(professionInfo.profession.acquirableJobSite(), player.blockPosition(),
                                CONFIG.debugConfig.villagerStatRange,
                                PoiManager.Occupancy.HAS_SPACE);
            }

            availableWorkstations.set(availableWorkstations.get() +
                    Component.translatable("text.LibertyVillagers.villagerStats.professionsCountFormat", villagerProfession,
                            numAvailableWorkstations).getString() + "\n");
        });

        pageString += availableWorkstations.get() + "\n\n";

        return pageString;
    }

    protected static String homelessPage(List<Villager> villagers) {
        StringBuilder homelessString = new StringBuilder();
        int numHomeless = 0;
        for (Villager villager : villagers) {
            if (!villager.getBrain().hasMemoryValue(MemoryModuleType.HOME)) {
                numHomeless++;
                homelessString.append(
                        Component.translatable("text.LibertyVillagers.villagerStats.homeless", villager.getDisplayName(),
                                villager.blockPosition().toShortString()).getString()).append("\n");
            }
        }

        String pageString = Component.translatable("text.LibertyVillagers.villagerStats.format",
                        Component.translatable("text.LibertyVillagers.villagerStats.numberOfHomeless").getString(), numHomeless)
                .getString() + "\n\n";
        if (numHomeless > 0) {
            pageString += homelessString;
        }

        return pageString;
    }

    protected static String availableBedsPage(ServerPlayer player, ServerLevel serverWorld) {
        List<PoiRecord> availableBeds = serverWorld.getPoiManager()
                .getInRange(registryEntry -> registryEntry.is(PoiTypes.HOME), player.blockPosition(),
                        CONFIG.debugConfig.villagerStatRange, PoiManager.Occupancy.HAS_SPACE)
                .toList();

        StringBuilder pageString = new StringBuilder(Component.translatable("text.LibertyVillagers.villagerStats.format",
                Component.translatable("text.LibertyVillagers.villagerStats.numberOfAvailableBeds").getString(),
                availableBeds.size()).getString() + "\n\n");

        if (availableBeds.size() > 0) {
            pageString.append(Component.translatable("text.LibertyVillagers.villagerStats.bedsAt").getString()).append("\n");
            for (PoiRecord bed : availableBeds) {
                if (bed != null && bed.getPos() != null) {
                    pageString.append(bed.getPos().toShortString()).append("\n");
                }
            }
        }

        return pageString.toString();
    }

    protected static String golems(ServerPlayer player, ServerLevel serverWorld) {
        List<IronGolem> golems = serverWorld.getEntitiesOfClass(IronGolem.class,
                player.getBoundingBox().inflate(CONFIG.debugConfig.villagerStatRange));

        StringBuilder pageString = new StringBuilder(Component.translatable("text.LibertyVillagers.villagerStats.format",
                        Component.translatable("text.LibertyVillagers.villagerStats.numberOfGolems").getString(), golems.size())
                .getString() + "\n\n");

        if (golems.size() > 0) {
            pageString.append(Component.translatable("text.LibertyVillagers.villagerStats.golemsAt").getString())
                    .append("\n");
            for (IronGolem golem : golems) {
                if (golem != null && golem.blockPosition() != null) {
                    pageString.append(
                            Component.translatable("text.LibertyVillagers.villagerStats.homeless", golem.getDisplayName(),
                                    golem.blockPosition().toShortString()).getString()).append("\n");
                }
            }
        }

        return pageString.toString();
    }

    protected static String translatedCatVariant(String variant) {
        return Component.translatable("text.LibertyVillagers.villagerStats." + variant).getString();
    }

    protected static String cats(ServerPlayer player, ServerLevel serverWorld) {
        List<Cat> cats = serverWorld.getEntitiesOfClass(Cat.class,
                player.getBoundingBox().inflate(CONFIG.debugConfig.villagerStatRange));

        String pageString = Component.translatable("text.LibertyVillagers.villagerStats.format",
                        Component.translatable("text.LibertyVillagers.villagerStats.numberOfCats").getString(), cats.size())
                .getString() + "\n\n";

        TreeMap<String, Integer> catVariantMap = new TreeMap<>();

        for (Map.Entry<ResourceKey<CatVariant>, CatVariant> catVariantEntry : BuiltInRegistries.CAT_VARIANT.entrySet()) {
            catVariantMap.put(translatedCatVariant(catVariantEntry.getKey().location().toShortLanguageKey()), 0);
        }

        if (cats.size() > 0) {
            for (Cat cat : cats) {
                String variant =
                        translatedCatVariant(BuiltInRegistries.CAT_VARIANT.getKey(cat.getVariant().value()).toShortLanguageKey());
                catVariantMap.merge(variant, 1, Integer::sum);
            }

            pageString += Component.translatable("text.LibertyVillagers.villagerStats.catTypes").getString() + "\n";

            AtomicReference<String> catVariants = new AtomicReference<>("");
            catVariantMap.forEach((catVariant, sum) -> catVariants.set(catVariants.get() +
                    Component.translatable("text.LibertyVillagers.villagerStats.professionsCountFormat",
                            catVariant, sum).getString() + "\n"));

            pageString += catVariants;
        }

        return pageString;
    }
}
