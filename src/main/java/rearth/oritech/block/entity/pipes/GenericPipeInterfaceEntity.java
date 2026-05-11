package rearth.oritech.block.entity.pipes;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.*;
import net.minecraft.util.Tuple;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import rearth.oritech.Oritech;
import rearth.oritech.block.blocks.pipes.AbstractPipeBlock;
import rearth.oritech.block.blocks.pipes.GenericPipeBlock;
import rearth.oritech.block.entity.interaction.PipeBoosterBlockEntity;

import java.util.*;
import java.util.stream.Collectors;

import static rearth.oritech.block.blocks.pipes.GenericPipeBlock.NO_CONNECTION;


public abstract class GenericPipeInterfaceEntity extends BlockEntity implements BlockEntityTicker<GenericPipeInterfaceEntity> {
    
    public static final int MAX_SEARCH_COUNT = 2048;
    
    public BlockPos connectedBooster = BlockPos.ZERO;

    private PipeBoosterBlockEntity cachedBooster;

    public GenericPipeInterfaceEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

	public boolean isBoostAvailable() {
		var booster = tryGetCachedBooster();
		return booster != null && booster.canUseBoost();
	}

	public void onBoostUsed() {
		var booster = tryGetCachedBooster();
		if (booster != null) booster.useBoost();
	}

	@Nullable
	private PipeBoosterBlockEntity tryGetCachedBooster() {

		// booster was removed
		if (cachedBooster != null && cachedBooster.isRemoved()) {
			cachedBooster = null;
			connectedBooster = BlockPos.ZERO;
			return null;
		}

		if (connectedBooster == BlockPos.ZERO) {  // no booster set
			if (cachedBooster != null) cachedBooster = null;
			return null;
		} else if (cachedBooster == null) { // booster freshly set
			var candidate = Objects.requireNonNull(level).getBlockEntity(connectedBooster);
			if (candidate instanceof PipeBoosterBlockEntity booster) {
				cachedBooster = booster;
				return cachedBooster;
			} else {
				connectedBooster = BlockPos.ZERO;
				return null;
			}
		} else {    // no change
			return cachedBooster;
		}
	}

    public static void addNode(Level world, BlockPos pos, boolean isInterface, BlockState newState, PipeNetworkData data) {
        Oritech.LOGGER.debug("registering/updating node: " + pos);

        data.pipes.add(pos);
        var connectedMachines = new HashSet<BlockPos>(6);
        var block = (AbstractPipeBlock) newState.getBlock();
        for (var neighbor : Direction.values()) {
            var neighborPos = pos.relative(neighbor);
            var neighborMap = data.machinePipeNeighbors.getOrDefault(neighborPos, new HashSet<>());
            if (block.hasMachineInDirection(neighbor, world, pos, block.apiValidationFunction())) {
                if (block.isConnectingInDirection(newState, neighbor, pos, world, false))
                    connectedMachines.add(pos.relative(neighbor));

                neighborMap.add(neighbor.getOpposite());
            } else {
                neighborMap.remove(neighbor.getOpposite());
            }

            if (!neighborMap.isEmpty()) data.machinePipeNeighbors.put(neighborPos, neighborMap);
            else data.machinePipeNeighbors.remove(neighborPos);
        }

        if (isInterface) {
            data.machineInterfaces.put(pos, connectedMachines);
        } else {
            data.machineInterfaces.remove(pos);
        }

        updateFromNode(world, pos, data);
    }

    public static void removeNode(Level world, BlockPos pos, boolean wasInterface, BlockState oldState, PipeNetworkData data) {
        Oritech.LOGGER.debug("removing node: " + pos + " | " + wasInterface);

        var oldNetwork = data.pipeNetworkLinks.getOrDefault(pos, -1);

        data.pipes.remove(pos);
        if (wasInterface) data.machineInterfaces.remove(pos);

        removeStaleMachinePipeNeighbors(pos, data);

        data.pipeNetworks.remove(oldNetwork);
        data.pipeNetworkInterfaces.remove(oldNetwork);
        data.pipeNetworkLinks.remove(pos);

        // re-calculate old network, is either shorter or split into multiple ones (starting from ones this block was connected to)
        if (oldNetwork != -1) {
            var block = oldState.getBlock();
            for (var direction : Direction.values()) {
                if (block instanceof GenericPipeBlock pipeBlock && oldState.getValue(pipeBlock.directionToProperty(direction)) == NO_CONNECTION) {
                    continue;
                }

                updateFromNode(world, pos.relative(direction), data);
            }
        }

        data.setDirty();
    }

    private static void updateFromNode(Level world, BlockPos pos, PipeNetworkData data) {

        var searchInstance = new FloodFillSearch(pos, data.pipes, world);
        var foundNetwork = new HashSet<>(searchInstance.complete());
        var foundMachines = findConnectedMachines(foundNetwork, data);

        Oritech.LOGGER.debug("Nodes:    " + foundNetwork.size() + " | " + foundNetwork);
        Oritech.LOGGER.debug("Machines: " + foundMachines.size() + " | " + foundMachines.stream().map(elem -> elem.getA() + ":" + elem.getB()).toList());

        var netID = foundNetwork.hashCode();
        data.pipeNetworks.put(netID, foundNetwork);
        data.pipeNetworkInterfaces.put(netID, foundMachines);

        // these networks will be replaced, since these nodes now belong to the new network
        var networksToRemove = new HashSet<Integer>();

        for (var node : foundNetwork) {
            networksToRemove.add(data.pipeNetworkLinks.getOrDefault(node, -1));
            data.pipeNetworkLinks.put(node, netID);
        }

        networksToRemove.stream().filter(i -> i != -1 && i != netID).forEach(i -> {
            data.pipeNetworks.remove(i);
            data.pipeNetworkInterfaces.remove(i);
        });

        data.setDirty();
    }

    private static Set<Tuple<BlockPos, Direction>> findConnectedMachines(Set<BlockPos> network, PipeNetworkData data) {

        var res = new HashSet<Tuple<BlockPos, Direction>>();

        for (var node : network) {
            if (data.machineInterfaces.containsKey(node)) {
                for (var machinePos : data.machineInterfaces.get(node)) {
                    var offset = machinePos.subtract(node);
                    var direction = Direction.fromDelta(offset.getX(), offset.getY(), offset.getZ()).getOpposite();
                    res.add(new Tuple<>(machinePos, direction));
                }
            }
        }

        return res;
    }

    public static Set<Tuple<BlockPos, Direction>> findNetworkTargets(BlockPos from, PipeNetworkData data) {
        var connectedNetwork = data.pipeNetworkLinks.getOrDefault(from, -1);
        if (connectedNetwork == -1) return new HashSet<>();

        return data.pipeNetworkInterfaces.get(connectedNetwork);
    }

    /**
     * Removes any stale machine -> neighboring pipes mappings
     * Used when a pipe node is destroyed
     *
     * @param pos  position of the destroyed node
     * @param data network data
     */
    public static void removeStaleMachinePipeNeighbors(BlockPos pos, PipeNetworkData data) {
        for (var neighbor : Direction.values()) {
            var machine = pos.relative(neighbor);
            var machineNeighbors = data.machinePipeNeighbors.get(machine);
            if (machineNeighbors == null) continue;

            machineNeighbors.remove(Direction.getNearest(Vec3.atLowerCornerOf(pos.subtract(machine))));
            if (machineNeighbors.isEmpty())
                data.machinePipeNeighbors.remove(machine);
            else
                data.machinePipeNeighbors.put(machine, machineNeighbors);
        }
    }

    private static class FloodFillSearch {

        final HashSet<BlockPos> checkedPositions = new HashSet<>();
        final HashSet<BlockPos> nextTargets = new HashSet<>();
        final Deque<BlockPos> foundTargets = new ArrayDeque<>();
        final HashSet<BlockPos> pipes;
        final Level world;

        public FloodFillSearch(BlockPos startPosition, HashSet<BlockPos> pipes, Level world) {
            this.pipes = pipes;
            this.world = world;
            nextTargets.add(startPosition);
        }

        public Deque<BlockPos> complete() {
            var active = true;
            while (active) {
                active = !nextGeneration();
            }

            return foundTargets;
        }

        // returns true when done
        @SuppressWarnings("unchecked")
        public boolean nextGeneration() {

            var currentGeneration = (HashSet<BlockPos>) nextTargets.clone();

            for (var target : currentGeneration) {
                if (isValidTarget(target)) {
                    foundTargets.addLast(target);
                    addNeighborsToQueue(target);
                }

                checkedPositions.add(target);
                nextTargets.remove(target);
            }

            if (cutoffSearch()) nextTargets.clear();

            return nextTargets.isEmpty();
        }

        private boolean cutoffSearch() {
            return foundTargets.size() >= MAX_SEARCH_COUNT;
        }

        private boolean isValidTarget(BlockPos target) {
            return pipes.contains(target);
        }

        private void addNeighborsToQueue(BlockPos self) {
            var targetState = world.getBlockState(self);

            if (!(targetState.getBlock() instanceof AbstractPipeBlock targetBlock)) return;
            for (var direction : Direction.values()) {
                var neighbor = self.relative(direction);
                if (checkedPositions.contains(neighbor)) continue;
                if (!isValidTarget(neighbor)) {
                    checkedPositions.add(neighbor);
                    continue;
                }

                // check if the target can connect to the neighbor
                if (!targetBlock.isConnectingInDirection(targetState, direction, self, world, false)) continue;

                nextTargets.add(neighbor);
            }
        }
    }

    public static final class PipeNetworkData extends SavedData {
        public final HashMap<BlockPos, Integer> pipeNetworkLinks = new HashMap<>(); // which blockpos belongs to which network (ID)
        public final HashSet<BlockPos> pipes = new HashSet<>();
        public final HashMap<BlockPos, Set<BlockPos>> machineInterfaces = new HashMap<>(); // list of machines per interface/connection block
        public final HashMap<Integer, Set<BlockPos>> pipeNetworks = new HashMap<>();   // networks are never updated, and instead always replaced by new ones with different ids
        public final HashMap<Integer, Set<Tuple<BlockPos, Direction>>> pipeNetworkInterfaces = new HashMap<>(); // list of machines that are connected to the network

        public final HashMap<BlockPos, Set<Direction>> machinePipeNeighbors = new HashMap<>(); // List of neighboring pipes per machine, and the direction they are in. Missing direction means no connection

        @Override
        public int hashCode() {
            int result = pipeNetworkLinks.hashCode();
            result = 31 * result + pipes.hashCode();
            result = 31 * result + machineInterfaces.hashCode();
            result = 31 * result + pipeNetworks.hashCode();
            result = 31 * result + pipeNetworkInterfaces.hashCode();
            return result;
        }

        public static Factory<PipeNetworkData> TYPE = new Factory<>(PipeNetworkData::new, PipeNetworkData::fromNbt, null);

        public static PipeNetworkData fromNbt(CompoundTag nbt, HolderLookup.Provider registryLookup) {

            var result = new PipeNetworkData();

            if (nbt.contains("pipeNetworkLinks", Tag.TAG_LIST)) {
                var pipeNetworkLinksList = nbt.getList("pipeNetworkLinks", Tag.TAG_COMPOUND);
                for (var element : pipeNetworkLinksList) {
                    var entry = (CompoundTag) element;
                    var pos = BlockPos.of(entry.getLong("pos"));
                    var id = entry.getInt("id");
                    result.pipeNetworkLinks.put(pos, id);
                }
            }

            // Deserialize pipes
            if (nbt.contains("pipes", Tag.TAG_LIST)) {
                var pipesList = nbt.getList("pipes", Tag.TAG_LONG);
                pipesList.stream().map(element -> BlockPos.of(((LongTag) element).getAsLong())).forEach(result.pipes::add);
            }

            // Deserialize machineInterfaces
            if (nbt.contains("machineInterfaces", Tag.TAG_COMPOUND)) {
                var machineInterfacesNbt = nbt.getCompound("machineInterfaces");
                for (var key : machineInterfacesNbt.getAllKeys()) {
                    var interfacePos = BlockPos.of(Long.parseLong(key));
                    var machinesArray = machineInterfacesNbt.getLongArray(key);
                    var machines = Arrays.stream(machinesArray)
                                     .mapToObj(BlockPos::of)
                                     .collect(Collectors.toSet());
                    result.machineInterfaces.put(interfacePos, machines);
                }
            }

            // Deserialize pipeNetworks
            if (nbt.contains("pipeNetworks", Tag.TAG_COMPOUND)) {
                var pipeNetworksNbt = nbt.getCompound("pipeNetworks");
                for (var key : pipeNetworksNbt.getAllKeys()) {
                    var id = Integer.parseInt(key);
                    var networkArray = pipeNetworksNbt.getLongArray(key);
                    var network = Arrays.stream(networkArray)
                                    .mapToObj(BlockPos::of)
                                    .collect(Collectors.toSet());
                    result.pipeNetworks.put(id, network);
                }
            }

            // Deserialize pipeNetworkInterfaces
            if (nbt.contains("pipeNetworkInterfaces", Tag.TAG_COMPOUND)) {
                var pipeNetworkInterfacesNbt = nbt.getCompound("pipeNetworkInterfaces");
                for (var key : pipeNetworkInterfacesNbt.getAllKeys()) {
                    var id = Integer.parseInt(key);
                    var interfacesList = pipeNetworkInterfacesNbt.getList(key, Tag.TAG_COMPOUND);
                    var interfaces = new HashSet<Tuple<BlockPos, Direction>>();
                    for (var interfaceElement : interfacesList) {
                        var pairNbt = (CompoundTag) interfaceElement;
                        var pos = BlockPos.of(pairNbt.getLong("pos"));
                        var direction = Direction.byName(pairNbt.getString("direction"));
                        interfaces.add(new Tuple<>(pos, direction));
                    }
                    result.pipeNetworkInterfaces.put(id, interfaces);
                }
            }

            // Deserialize machinePipeNeighbors
            if (nbt.contains("machinePipeNeighbors", Tag.TAG_COMPOUND)) {
                var connectionPipeNeighborsNbt = nbt.getCompound("machinePipeNeighbors");
                for (var key : connectionPipeNeighborsNbt.getAllKeys()) {
                    var pos = BlockPos.of(Long.parseLong(key));
                    var neighborsList = connectionPipeNeighborsNbt.getList(key, Tag.TAG_STRING);
                    var neighbors = new HashSet<Direction>();
                    for (var neighborElement : neighborsList) {
                        var direction = Direction.byName(neighborElement.getAsString());
                        neighbors.add(direction);
                    }
                    result.machinePipeNeighbors.put(pos, neighbors);
                }
            }

            result.setDirty();

            return result;
        }

        @Override
        public CompoundTag save(CompoundTag nbt, HolderLookup.Provider registryLookup) {

            // Serialize pipeNetworkLinks
            var pipeNetworkLinksList = new ListTag();
            pipeNetworkLinks.forEach((pos, id) -> {
                var entry = new CompoundTag();
                entry.putLong("pos", pos.asLong());
                entry.putInt("id", id);
                pipeNetworkLinksList.add(entry);
            });
            nbt.put("pipeNetworkLinks", pipeNetworkLinksList);

            // Serialize pipes
            var pipesList = new ListTag();
            pipes.forEach(pos -> pipesList.add(LongTag.valueOf(pos.asLong())));
            nbt.put("pipes", pipesList);

            // Serialize machineInterfaces
            var machineInterfacesNbt = new CompoundTag();
            machineInterfaces.forEach((interfacePos, machines) -> {
                machineInterfacesNbt.putLongArray(Long.toString(interfacePos.asLong()), machines.stream().map(BlockPos::asLong).collect(Collectors.toList()));
            });
            nbt.put("machineInterfaces", machineInterfacesNbt);

            // Serialize pipeNetworks
            var pipeNetworksNbt = new CompoundTag();
            pipeNetworks.forEach((id, network) -> {
                pipeNetworksNbt.putLongArray(id.toString(), network.stream().map(BlockPos::asLong).collect(Collectors.toList()));
            });
            nbt.put("pipeNetworks", pipeNetworksNbt);

            // Serialize pipeNetworkInterfaces
            var pipeNetworkInterfacesNbt = new CompoundTag();
            pipeNetworkInterfaces.forEach((id, interfaces) -> {
                var interfacesList = new ListTag();
                interfaces.forEach(pair -> {
                    var pairNbt = new CompoundTag();
                    pairNbt.putLong("pos", pair.getA().asLong());
                    pairNbt.putString("direction", pair.getB().getName());
                    interfacesList.add(pairNbt);
                });
                pipeNetworkInterfacesNbt.put(id.toString(), interfacesList);
            });
            nbt.put("pipeNetworkInterfaces", pipeNetworkInterfacesNbt);

            // Serialize machinePipeNeighbors
            var connectionPipeNeighborsNbt = new CompoundTag();
            machinePipeNeighbors.forEach((pos, neighbors) -> {
                var neighborsList = new ListTag();
                neighbors.forEach(direction -> {
                    var nbtElement = StringTag.valueOf(direction.getName());
                    neighborsList.add(nbtElement);
                });
                connectionPipeNeighborsNbt.put(Long.toString(pos.asLong()), neighborsList);
            });
            nbt.put("machinePipeNeighbors", connectionPipeNeighborsNbt);

            return nbt;
        }
    }

}
