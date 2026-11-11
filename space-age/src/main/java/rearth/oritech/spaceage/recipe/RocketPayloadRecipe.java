package rearth.oritech.spaceage.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.server.MinecraftServer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.PlacementInfo;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeBookCategories;
import net.minecraft.world.item.crafting.RecipeBookCategory;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import rearth.oritech.spaceage.init.SpaceAgeRecipes;
import rearth.oritech.spaceage.simulation.SpaceBalance;
import rearth.oritech.spaceage.simulation.rocket.ActiveRocketData;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.IntStream;

/**
 * Physical blocks aboard a destroyed rocket produce one world effect. Ordinary recipes supply reload and JEI sync.
 */
public record RocketPayloadRecipe(List<Requirement> ingredients, Effect effect, int durationTicks,
                                  double maxAltitude, int priority) implements Recipe<RocketPayloadRecipe.Input> {

    public static final MapCodec<RocketPayloadRecipe> CODEC = RecordCodecBuilder.<RocketPayloadRecipe>mapCodec(i -> i.group(
            Requirement.CODEC.listOf().fieldOf("ingredients").forGetter(RocketPayloadRecipe::ingredients),
            Effect.CODEC.fieldOf("effect").forGetter(RocketPayloadRecipe::effect),
            Codec.INT.fieldOf("duration").forGetter(RocketPayloadRecipe::durationTicks),
            Codec.DOUBLE.optionalFieldOf("max_earth_altitude", SpaceBalance.ATMOSPHERE_HEIGHT / 2).forGetter(RocketPayloadRecipe::maxAltitude),
            Codec.INT.optionalFieldOf("priority", 0).forGetter(RocketPayloadRecipe::priority)
    ).apply(i, RocketPayloadRecipe::new)).validate(recipe -> !recipe.ingredients.isEmpty()
            && recipe.durationTicks > 0 && recipe.maxAltitude >= 0
            ? DataResult.success(recipe) : DataResult.error(() -> "Payloads need ingredients, a positive duration and a nonnegative altitude"));

    public static Collection<RecipeHolder<RocketPayloadRecipe>> all(MinecraftServer server) {

        return server == null ? List.of() : server.overworld().recipeAccess().recipeMap().byType(SpaceAgeRecipes.ROCKET_PAYLOAD.get());
    }

    public static RecipeSerializer<RocketPayloadRecipe> serializer() {

        return new RecipeSerializer<>(CODEC, ByteBufCodecs.fromCodecWithRegistries(CODEC.codec()));
    }

    public static Optional<RecipeHolder<RocketPayloadRecipe>> select(Collection<RecipeHolder<RocketPayloadRecipe>> recipes,
                                                                     Input input, double altitude) {

        return recipes.stream().filter(holder -> altitude <= holder.value().maxAltitude + 1e-7 && holder.value().matches(input, null))
                .sorted(Comparator.<RecipeHolder<RocketPayloadRecipe>>comparingInt(holder -> holder.value().priority).reversed()
                        .thenComparing(holder -> holder.id().identifier().toString())).findFirst();
    }

    @Override
    public boolean matches(Input input, Level level) {

        var remaining = new ArrayList<>(input.blocks);
        for (var requirement : ingredients) {
            for (int count = 0; count < requirement.count; count++) {
                var index = -1;
                for (int candidate = 0; candidate < remaining.size(); candidate++) {
                    if (requirement.block.test(remaining.get(candidate))) {
                        index = candidate;
                        break;
                    }
                }
                if (index < 0) return false;
                remaining.remove(index);
            }
        }
        return true;
    }

    @Override
    public ItemStack assemble(Input input) {

        return ItemStack.EMPTY;
    }

    @Override
    public boolean isSpecial() {

        return true;
    }

    @Override
    public boolean showNotification() {

        return false;
    }

    @Override
    public String group() {

        return "";
    }

    @Override
    public PlacementInfo placementInfo() {

        return PlacementInfo.NOT_PLACEABLE;
    }

    @Override
    public RecipeBookCategory recipeBookCategory() {

        return RecipeBookCategories.CRAFTING_MISC;
    }

    @Override
    public RecipeType<RocketPayloadRecipe> getType() {

        return SpaceAgeRecipes.ROCKET_PAYLOAD.get();
    }

    @Override
    public RecipeSerializer<RocketPayloadRecipe> getSerializer() {

        return SpaceAgeRecipes.ROCKET_PAYLOAD_SERIALIZER.get();
    }

    public record Requirement(BlockIngredient block, int count) {

        public static final Codec<Requirement> CODEC = RecordCodecBuilder.<Requirement>create(i -> i.group(
                BlockIngredient.CODEC.fieldOf("block").forGetter(Requirement::block),
                Codec.INT.optionalFieldOf("count", 1).forGetter(Requirement::count)
        ).apply(i, Requirement::new)).validate(value -> value.count > 0 ? DataResult.success(value)
                : DataResult.error(() -> "Payload ingredient counts must be positive"));

        public List<ItemStack> display() {

            return block.display().stream().map(stack -> stack.copyWithCount(count)).toList();
        }
    }

    public record Input(List<BlockState> blocks) implements RecipeInput {

        public static Input of(Map<Identifier, Integer> blocks) {

            return new Input(blocks.entrySet().stream().flatMap(entry -> IntStream.range(0, entry.getValue())
                    .mapToObj(index -> BuiltInRegistries.BLOCK.getValue(entry.getKey()).defaultBlockState())).toList());
        }

        public static Input of(ActiveRocketData rocket) {

            return new Input(rocket.getStaticSegments().values().stream()
                    .flatMap(segment -> segment.blocks().stream()).map(block -> block.state()).toList());
        }

        @Override
        public ItemStack getItem(int index) {

            return new ItemStack(blocks.get(index).getBlock());
        }

        @Override
        public int size() {

            return blocks.size();
        }
    }

    public enum Effect implements StringRepresentable {

        CLEAR, RAIN, THUNDER;

        public static final Codec<Effect> CODEC = StringRepresentable.fromEnum(Effect::values);

        public void apply(MinecraftServer server, int ticks) {

            server.setWeatherParameters(this == CLEAR ? ticks : 0, this == CLEAR ? 0 : ticks,
                    this != CLEAR, this == THUNDER);
        }

        @Override
        public String getSerializedName() {

            return name().toLowerCase(Locale.ROOT);
        }
    }
}
