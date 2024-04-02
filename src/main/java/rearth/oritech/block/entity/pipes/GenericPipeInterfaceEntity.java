package rearth.oritech.block.entity.pipes;

import net.minecraft.block.BlockState;
import net.minecraft.block.ConnectingBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtLong;
import net.minecraft.util.Pair;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.PersistentState;
import rearth.oritech.block.blocks.pipes.GenericPipeConnectionBlock;

import java.util.*;
import java.util.stream.Collectors;

public abstract class GenericPipeInterfaceEntity extends BlockEntity implements BlockEntityTicker<GenericPipeInterfaceEntity> {
    
    public static final int MAX_SEARCH_COUNT = 256;
    
    public GenericPipeInterfaceEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }
    
    public static void addNode(BlockPos pos, boolean isInterface, BlockState newState, PipeNetworkData data) {
        System.out.println("registering/updating node: " + pos);
        
        data.pipes.add(pos);
        if (isInterface) {
            var connectedMachines = new HashSet<BlockPos>(6);
            if (newState.get(GenericPipeConnectionBlock.INTERFACE_NORTH))
                connectedMachines.add(pos.north());
            if (newState.get(GenericPipeConnectionBlock.INTERFACE_SOUTH))
                connectedMachines.add(pos.south());
            if (newState.get(GenericPipeConnectionBlock.INTERFACE_EAST))
                connectedMachines.add(pos.east());
            if (newState.get(GenericPipeConnectionBlock.INTERFACE_WEST))
                connectedMachines.add(pos.west());
            if (newState.get(GenericPipeConnectionBlock.INTERFACE_UP))
                connectedMachines.add(pos.up());
            if (newState.get(GenericPipeConnectionBlock.INTERFACE_DOWN))
                connectedMachines.add(pos.down());
            
            data.machineInterfaces.put(pos, connectedMachines);
        } else {
            data.machineInterfaces.remove(pos);
        }
        
        updateFromNode(pos, data);
    }
    
    public static void removeNode(BlockPos pos, boolean wasInterface, BlockState oldState, PipeNetworkData data) {
        System.out.println("removing node: " + pos);
        
        var oldNetwork = data.pipeNetworkLinks.getOrDefault(pos, -1);
        
        data.pipes.remove(pos);
        if (wasInterface) data.machineInterfaces.remove(pos);
        
        data.pipeNetworks.remove(oldNetwork);
        data.pipeNetworkInterfaces.remove(oldNetwork);
        
        // re-calculate old network, is either shorter or split into multiple ones (starting from ones this block was connected to)
        if (oldNetwork != -1) {
            if (oldState.get(ConnectingBlock.NORTH))
                updateFromNode(pos.north(), data);
            if (oldState.get(ConnectingBlock.SOUTH))
                updateFromNode(pos.south(), data);
            if (oldState.get(ConnectingBlock.EAST))
                updateFromNode(pos.east(), data);
            if (oldState.get(ConnectingBlock.WEST))
                updateFromNode(pos.west(), data);
            if (oldState.get(ConnectingBlock.UP))
                updateFromNode(pos.up(), data);
            if (oldState.get(ConnectingBlock.DOWN))
                updateFromNode(pos.down(), data);
        }
        
        data.markDirty();
    }
    
    private static void updateFromNode(BlockPos pos, PipeNetworkData data) {
        
        var searchInstance = new FloodFillSearch(pos, data.pipes);
        var foundNetwork = new HashSet<>(searchInstance.complete());
        var foundMachines = findConnectedMachines(foundNetwork, data);
        
        System.out.println("Nodes:    " + foundNetwork.size() + " | " + foundNetwork);
        System.out.println("Machines: " + foundMachines.size() + " | " + foundMachines.stream().map(elem -> elem.getLeft() + ":" + elem.getRight()).toList());
        
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
        
        data.markDirty();
    }
    
    private static Set<Pair<BlockPos, Direction>> findConnectedMachines(Set<BlockPos> network, PipeNetworkData data) {
        
        var res = new HashSet<Pair<BlockPos, Direction>>();
        
        for (var node : network) {
            if (data.machineInterfaces.containsKey(node)) {
                for (var machinePos : data.machineInterfaces.get(node)) {
                    var offset = machinePos.subtract(node);
                    var direction = Direction.fromVector(offset.getX(), offset.getY(), offset.getZ()).getOpposite();
                    res.add(new Pair<>(machinePos, direction));
                }
            }
        }
        
        return res;
    }
    
    public static Set<Pair<BlockPos, Direction>> findNetworkTargets(BlockPos from, PipeNetworkData data) {
        var connectedNetwork = data.pipeNetworkLinks.getOrDefault(from, -1);
        if (connectedNetwork == -1) return new HashSet<>();
        
        return data.pipeNetworkInterfaces.get(connectedNetwork);
    }
    
    @Override
    public void markRemoved() {
        super.markRemoved();
    }
    
    private static class FloodFillSearch {
        
        final HashSet<BlockPos> checkedPositions = new HashSet<>();
        final HashSet<BlockPos> nextTargets = new HashSet<>();
        final Deque<BlockPos> foundTargets = new ArrayDeque<>();
        final HashSet<BlockPos> pipes;
        
        public FloodFillSearch(BlockPos startPosition, HashSet<BlockPos> pipes) {
            this.pipes = pipes;
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
            
            for (var neighbor : getNeighbors(self)) {
                if (checkedPositions.contains(neighbor)) continue;
                nextTargets.add(neighbor);
            }
            
        }
        
        private List<BlockPos> getNeighbors(BlockPos pos) {
            return List.of(pos.up(), pos.down(), pos.north(), pos.east(), pos.south(), pos.west());
        }
        
    }
    
    public static final class PipeNetworkData extends PersistentState {
        public final HashMap<BlockPos, Integer> pipeNetworkLinks = new HashMap<>();
        public final HashSet<BlockPos> pipes = new HashSet<>();
        public final HashMap<BlockPos, Set<BlockPos>> machineInterfaces = new HashMap<>(); // list of machines per interface
        public final HashMap<Integer, Set<BlockPos>> pipeNetworks = new HashMap<>();   // networks are never updated, and instead always replaced by new ones with different ids
        public final HashMap<Integer, Set<Pair<BlockPos, Direction>>> pipeNetworkInterfaces = new HashMap<>(); // list of machines that are connected to the network
        
        @Override
        public int hashCode() {
            int result = pipeNetworkLinks.hashCode();
            result = 31 * result + pipes.hashCode();
            result = 31 * result + machineInterfaces.hashCode();
            result = 31 * result + pipeNetworks.hashCode();
            result = 31 * result + pipeNetworkInterfaces.hashCode();
            return result;
        }
        
        public static PersistentState.Type<PipeNetworkData> TYPE = new Type<>(PipeNetworkData::new, PipeNetworkData::fromNbt, null);
        
        public static PipeNetworkData fromNbt(NbtCompound nbt) {
            
            var result = new PipeNetworkData();
            
            if (nbt.contains("pipeNetworkLinks", NbtElement.LIST_TYPE)) {
                var pipeNetworkLinksList = nbt.getList("pipeNetworkLinks", NbtElement.COMPOUND_TYPE);
                for (var element : pipeNetworkLinksList) {
                    var entry = (NbtCompound) element;
                    var pos = BlockPos.fromLong(entry.getLong("pos"));
                    var id = entry.getInt("id");
                    result.pipeNetworkLinks.put(pos, id);
                }
            }
            
            // Deserialize pipes
            if (nbt.contains("pipes", NbtElement.LIST_TYPE)) {
                var pipesList = nbt.getList("pipes", NbtElement.LONG_TYPE);
                pipesList.stream().map(element -> BlockPos.fromLong(((NbtLong) element).longValue())).forEach(result.pipes::add);
            }
            
            // Deserialize machineInterfaces
            if (nbt.contains("machineInterfaces", NbtElement.COMPOUND_TYPE)) {
                var machineInterfacesNbt = nbt.getCompound("machineInterfaces");
                for (var key : machineInterfacesNbt.getKeys()) {
                    var interfacePos = BlockPos.fromLong(Long.parseLong(key));
                    var machinesArray = machineInterfacesNbt.getLongArray(key);
                    var machines = Arrays.stream(machinesArray)
                                     .mapToObj(BlockPos::fromLong)
                                     .collect(Collectors.toSet());
                    result.machineInterfaces.put(interfacePos, machines);
                }
            }
            
            // Deserialize pipeNetworks
            if (nbt.contains("pipeNetworks", NbtElement.COMPOUND_TYPE)) {
                var pipeNetworksNbt = nbt.getCompound("pipeNetworks");
                for (var key : pipeNetworksNbt.getKeys()) {
                    var id = Integer.parseInt(key);
                    var networkArray = pipeNetworksNbt.getLongArray(key);
                    var network = Arrays.stream(networkArray)
                                    .mapToObj(BlockPos::fromLong)
                                    .collect(Collectors.toSet());
                    result.pipeNetworks.put(id, network);
                }
            }
            
            // Deserialize pipeNetworkInterfaces
            if (nbt.contains("pipeNetworkInterfaces", NbtElement.COMPOUND_TYPE)) {
                var pipeNetworkInterfacesNbt = nbt.getCompound("pipeNetworkInterfaces");
                for (var key : pipeNetworkInterfacesNbt.getKeys()) {
                    var id = Integer.parseInt(key);
                    var interfacesList = pipeNetworkInterfacesNbt.getList(key, NbtElement.COMPOUND_TYPE);
                    var interfaces = new HashSet<Pair<BlockPos, Direction>>();
                    for (var interfaceElement : interfacesList) {
                        var pairNbt = (NbtCompound) interfaceElement;
                        var pos = BlockPos.fromLong(pairNbt.getLong("pos"));
                        var direction = Direction.byName(pairNbt.getString("direction"));
                        interfaces.add(new Pair<>(pos, direction));
                    }
                    result.pipeNetworkInterfaces.put(id, interfaces);
                }
            }
            
            return result;
        }
        
        @Override
        public NbtCompound writeNbt(NbtCompound nbt) {
            
            // Serialize pipeNetworkLinks
            var pipeNetworkLinksList = new NbtList();
            pipeNetworkLinks.forEach((pos, id) -> {
                var entry = new NbtCompound();
                entry.putLong("pos", pos.asLong());
                entry.putInt("id", id);
                pipeNetworkLinksList.add(entry);
            });
            nbt.put("pipeNetworkLinks", pipeNetworkLinksList);
            
            // Serialize pipes
            var pipesList = new NbtList();
            pipes.forEach(pos -> pipesList.add(NbtLong.of(pos.asLong())));
            nbt.put("pipes", pipesList);
            
            // Serialize machineInterfaces
            var machineInterfacesNbt = new NbtCompound();
            machineInterfaces.forEach((interfacePos, machines) -> {
                machineInterfacesNbt.putLongArray(Long.toString(interfacePos.asLong()), machines.stream().map(BlockPos::asLong).collect(Collectors.toList()));
            });
            nbt.put("machineInterfaces", machineInterfacesNbt);
            
            // Serialize pipeNetworks
            var pipeNetworksNbt = new NbtCompound();
            pipeNetworks.forEach((id, network) -> {
                pipeNetworksNbt.putLongArray(id.toString(), network.stream().map(BlockPos::asLong).collect(Collectors.toList()));
            });
            nbt.put("pipeNetworks", pipeNetworksNbt);
            
            // Serialize pipeNetworkInterfaces
            var pipeNetworkInterfacesNbt = new NbtCompound();
            pipeNetworkInterfaces.forEach((id, interfaces) -> {
                var interfacesList = new NbtList();
                interfaces.forEach(pair -> {
                    var pairNbt = new NbtCompound();
                    pairNbt.putLong("pos", pair.getLeft().asLong());
                    pairNbt.putString("direction", pair.getRight().getName());
                    interfacesList.add(pairNbt);
                });
                pipeNetworkInterfacesNbt.put(id.toString(), interfacesList);
            });
            nbt.put("pipeNetworkInterfaces", pipeNetworkInterfacesNbt);
            
            return nbt;
        }
    }
    
}
