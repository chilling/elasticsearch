/*
 * Licensed to ElasticSearch and Shay Banon under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. ElasticSearch licenses this
 * file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.elasticsearch.cluster.routing.allocation.allocator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import org.elasticsearch.cluster.routing.IndexRoutingTable;
import org.elasticsearch.cluster.routing.IndexShardRoutingTable;
import org.elasticsearch.cluster.routing.MutableShardRouting;
import org.elasticsearch.cluster.routing.RoutingNode;
import org.elasticsearch.cluster.routing.RoutingNodes;
import org.elasticsearch.cluster.routing.ShardRouting;
import org.elasticsearch.cluster.routing.ShardRoutingState;
import org.elasticsearch.cluster.routing.allocation.FailedRerouteAllocation;
import org.elasticsearch.cluster.routing.allocation.RoutingAllocation;
import org.elasticsearch.cluster.routing.allocation.StartedRerouteAllocation;
import org.elasticsearch.cluster.routing.allocation.decider.Decision;
import org.elasticsearch.cluster.routing.allocation.decider.Decision.Type;
import org.elasticsearch.common.component.AbstractComponent;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;

/**
 * 1) Maximize the number of nodes that keep a primary
 * 2) Minimize the number of primaries per node
 * 3) Minimize the distance of the maximal and the minimal
 *    number of shards per node
 * 4) Maximize the Number of Indices per Node
 * 5) Keep care of replicas of the same shard on the same Node
 * 6) Minimize the Number of Move-Operations
 * 
 * 
 * @author schilling
 * 
 */
public class BalancedShardsAllocator extends AbstractComponent implements ShardsAllocator {

    WeightFunction balance = new BasicBalance(0.5f, 0.4f, 0.1f);
    float treshold = 1.1f;

    @Inject
    public BalancedShardsAllocator(Settings settings) {
        super(settings);
    }

    @Override
    public void applyStartedShards(StartedRerouteAllocation allocation) {
        // ONLY FOR GATEWAYS
    }

    @Override
    public void applyFailedShards(FailedRerouteAllocation allocation) {
    }

    @Override
    public boolean allocateUnassigned(RoutingAllocation allocation) {
        Balancer balancer = new Balancer(allocation, balance, treshold);
        Collection<MutableShardRouting> replicas = new ArrayList<MutableShardRouting>();
        
        for (ShardRouting replica : allocation.routingTable().shardsWithState(ShardRoutingState.UNASSIGNED)) {
            replicas.addAll(allocation.routingNodes().shardsRoutingFor(replica));
        }
        
        balancer.distributeReplicas(replicas);
        balancer.balance();
        return balancer.apply();
    }

    @Override
    public boolean rebalance(RoutingAllocation allocation) {
        Balancer balancer = new Balancer(allocation, balance, treshold);
        balancer.balance();
        return balancer.apply();
    }

    @Override
    public boolean move(MutableShardRouting shardRouting, RoutingNode node, RoutingAllocation allocation) {
        boolean changed = false;
        return changed;
    }

}

/**
 * Definition of a weight function for {@link NodeInfo} according to
 * a given <code>index</code>. 
 */
interface WeightFunction {
    public float weight(Balancer allocation, NodeInfo node, String index);
}

/**
 * Simple implementation of a {@link WeightFunction}. It is based on three
 * values:
 * <ul><li><code>index balance</code></li>
 *     <li><code>replica balance</code></li>
 *     <li><code>primary balance</code</li></ul>
 * The <code>index balance</code> defines the factor of the distribution of 
 * shards per index on nodes is weighted. The factor <code>replica balance</code>
 * defines the weight of the current number of replicas allocated on a node
 * compared to the average number of replicas per node. Analogically the
 * <code>primary balance</code> factor defines the number of allocated primaries
 * per node according to the average number of primaries per node.<br />
 * <ul>
 * <li><code>weight<sub>index</sub>(node, index) = (indexBalance * node.numReplicas(index)) - avgReplicasPerNode(index)</code></li>    
 * <li><code>weight<sub>node</sub>(node, index) = (replicaBalance * node.numReplicas()) - avgReplicasPerNode</code></li>    
 * <li><code>weight<sub>primary</sub>(node, index) = (primaryBalance * node.numPrimaries()) - avgPrimariesPerNode</code></li>
 * </ul>
 * <code>weight(node, index) = weight<sub>index</sub>(node, index) + weight<sub>node</sub>(node, index) + weight<sub>primary</sub>(node, index)</code>    
 * @author schilling
 */
class BasicBalance implements WeightFunction {

    private final float indexBalance;
    private final float replicaBalance;
    private final float primaryBalance;

    protected BasicBalance(float indexBalance, float replicaBalance, float primaryBalance) {
        super();
        
        float commonDenominator = indexBalance + replicaBalance + primaryBalance;
        
        this.indexBalance = indexBalance/commonDenominator;
        this.replicaBalance = replicaBalance/commonDenominator;
        this.primaryBalance = primaryBalance/commonDenominator;
    }

    @Override
    public float weight(Balancer balancer, NodeInfo node, String index) {
        return    ((indexBalance * node.numReplicas(index)) - balancer.stats.avgReplicasOfIndexPerNode(index))
                + ((replicaBalance * node.numReplicas()) - balancer.stats.avgReplicasPerNode())
                + ((primaryBalance * node.numPrimaries()) - balancer.stats.avgPrimariesPerNode());
    }


}

/**
 * A {@link Comparator} used to order nodes according to
 * a given {@link WeightFunction}
 */
class WeightOrder implements Comparator<NodeInfo> {
    final Balancer balancer;
    final WeightFunction function;
    final String index;
    
    public WeightOrder(Balancer balancer, WeightFunction function, String index) {
        super();
        this.balancer = balancer;
        this.function = function;
        this.index = index;
    }

    public float weight(NodeInfo node) {
        return function.weight(balancer, node, index);
    }
    
    @Override
    public int compare(NodeInfo left, NodeInfo right) {
        final int cmp;
        if((cmp = Float.compare(weight(left), weight(right))) != 0) {
            return cmp;
        } else {
            return left.id.compareTo(right.id);
        }
    }
}

/**
 * A {@link Balancer} 
 * 
 * 
 */
class Balancer {
    
    private final ArrayList<NodeInfo> nodes;
    private final HashSet<String> indices;
    private final RoutingAllocation allocation;
    private final WeightFunction weight;
    
    private float treshold = 0;
    
    public final Stats stats = new Stats();
    
    public Balancer(RoutingAllocation allocation, WeightFunction weight, float treshold) {
        this.allocation = allocation;
        this.nodes = new ArrayList<NodeInfo>();
        this.indices = new HashSet<String>();
        this.weight = weight;
        this.treshold = treshold;
        
        for(RoutingNode node : allocation.routingNodes()) {
            nodes.add(new NodeInfo(node.nodeId()));
        }
        
        RoutingNodes routing = allocation.routingNodes();
        
        for(IndexRoutingTable index : allocation.routingTable().indicesRouting().values()) {
            indices.add(index.index());
            for(IndexShardRoutingTable shard : index.getShards().values()) {
                distributeReplicas(routing.shardsRoutingFor(index.index(), shard.shardId().id()));
            }
        }
    }

    /**
     * Statistics of the balancer. This class defines some common functions
     * for the distributions used by the {@link Balancer}
     */
    class Stats {

        /**
         * @return Number of nodes defined by the Balancer
         */
        public int numNodes() {
            assert nodes.size() > 0;
            return nodes.size();
        }    

        /**
         * @return Number of Replicas handled by the {@link Balancer}
         */
        public int numReplicas() {
            int sum = 0;
            for(NodeInfo node : nodes) {
                sum += node.numReplicas();
            }
            return sum;
        }
        
        /**
         * @return Number of primaries shards of all indices 
         */
        public int numPrimaries() {
            int sum = 0;
            for(NodeInfo node : nodes) {
                sum += node.numPrimaries();
            }
            return sum;
        }
        
        /**
         * @param index Index to use
         * @return number of replicas for the given index 
         */
        public int numReplicas(String index) {
            int sum = 0;
            for(NodeInfo node : nodes) {
                sum += node.numReplicas(index);
            }
            return sum;
        }
        
        /**
         * @param index Index to use
         * @return number of primary shards of the given index
         */
        public int numPrimaries(String index) {
            int sum = 0;
            for(NodeInfo node : nodes) {
                sum += node.numPrimaries(index);
            }
            return sum;
        }
        
        public float avgReplicasOfIndexPerNode(String index) {
            return ((float)numReplicas(index)) / numNodes();
        }
        
        public float avgReplicasPerNode() {
            return ((float)numReplicas()) / numNodes();
        }
        
        public float avgPrimariesPerNode() {
            return ((float)numPrimaries()) / numNodes();
        }

        public float avgPrimariesOfIndexPerNode(String index) {
            return ((float)numPrimaries(index)) / numNodes();
        }
    }
        
    public boolean distributeReplicas(Collection<MutableShardRouting> replicas) {
        Collection<MutableShardRouting> others = new ArrayList<MutableShardRouting>();
        
        boolean failed = false;
        
        /* we need to distribute primary shards
         * before other shards 
         * */
        for (MutableShardRouting replica : replicas) {
            if(replica.primary()) {
                if(!allocateReplica(replica)) {
                    failed = true;
                }
            } else {
                others.add(replica);
            }
        }
        
        for(MutableShardRouting replica : others) {
            if(!allocateReplica(replica)) {
                failed = true;
            }
        }
        
        return !failed;
    }
    
    private boolean allocateReplica(MutableShardRouting replica) {
        assert !nodes.isEmpty();
        // find an allocatable node which weight is minimal

        NodeInfo minNode = null;
        float min = Float.POSITIVE_INFINITY;

        for (NodeInfo node : nodes) {
            float w = weight.weight(this, node, replica.index());
            if (w < min) {
                RoutingNode routing = allocation.routingNodes().node(node.id);
                if (allocation.deciders().canAllocate(replica, routing, allocation).type() == Type.YES) {
                    minNode = node;
                    min = w;
                }
            }
        }

        if (minNode != null) {
            return minNode.addReplica(new ShardInfo(replica));
        }
        return false;
    }
    
    protected ShardInfo relocateSomeReplica(NodeInfo src, NodeInfo dst, String idx) {
        IndexInfo index = src.getIndexInfo(idx);
        if(index == null) {
            return null;
        } else {
            RoutingNode node = allocation.routingNodes().node(dst.id);
            float minCost = Float.POSITIVE_INFINITY;
            ShardInfo candidate = null;
            Decision decision = null;
            
            Collection<ShardInfo> allReplicas = new ArrayList<ShardInfo>(index.replicas);
            
            for(ShardInfo info : allReplicas) {
                Decision d = allocation.deciders().canAllocate(info.replica, node, allocation);
                
                if((d.type() == Type.YES) || (d.type() == Type.THROTTLE)) {
                    if(src.removeReplica(info)) {
                        if(dst.addReplica(info)) {
                            
                            float srcWeight = weight.weight(this, src, idx);
                            float dstWeight = weight.weight(this, dst, idx);
                            float currentCost = dstWeight-srcWeight; 
                            
                            if(currentCost<minCost) {
                                minCost = currentCost;
                                candidate = info;
                                decision = d;
                            }
                            dst.removeReplica(info);
                        }
                        src.addReplica(info);
                    }
                }
            }
            
            if(candidate != null) {
                src.removeReplica(candidate);
                dst.addReplica(candidate);
                candidate.decision = decision;
                return candidate;
            } else {
                return null;
            }
        }
    }
    
    public boolean balance() {
        return balance(treshold);
    }
    
    /**
     * balance the shards allocated on the nodes according to
     * a given <code>treshold</code>. Operations below this
     * value will not be handled.  
     * 
     * @param treshold operations treshold
     * 
     * @return <code>true</code> if the current configuration
     *  has been changed
     */
    public boolean balance(float treshold) {
        if(nodes.size()<=1) {
            return false;
        } else {
            NodeInfo[] nodes = this.nodes.toArray(new NodeInfo[this.nodes.size()]);
            boolean changed = false;
            
            for(String index : indices) {
                WeightOrder order = new WeightOrder(this, weight, index);
                Arrays.sort(nodes, order);
                float lastDiff = Float.POSITIVE_INFINITY;
                
                while(true) {
                    final NodeInfo minNode = nodes[0]; 
                    final NodeInfo maxNode = nodes[nodes.length-1];
        
                    final float minWeight = order.weight(minNode);
                    final float maxWeight = order.weight(maxNode);
                    final float diff = (maxWeight - minWeight);
                    
                    if(lastDiff == diff) {
                        break;
                    } else {
                        lastDiff = diff;
                    }
                    
                    if(diff>=treshold) {
                        ShardInfo replica = relocateSomeReplica(maxNode, minNode, index);

                        if(replica != null) {
                            changed = true;
                        } else {
                            break;
                        }
                        
                    } else {
                        break;
                    }
                }
            }
            return changed;
        }
    }

    /**
     * Apply the balanced configuration to the current {@link RoutingAllocation}
     * @return <code>true</code> if something has changed
     */
    public boolean apply() {
        return apply(0);
    }

    /**
     * Apply the balanced configuration to the current {@link RoutingAllocation}
     * @param maxOperations maximum number of operations that
     *        should be applied to the cluster (<code>0</code> for all operations)
     * @return <code>true</code> if something has changed
     */
    public boolean apply(int maxOperations) {
        int numChanges = 0;
        for(NodeInfo node : nodes) {
            for(IndexInfo index : node) {
                for(ShardInfo info : index.replicas) {
                    if(!info.replica.currentNodeId().equals(node.id)) {
                        if(info.decision.type() == Type.YES) {
                            info.replica.relocate(node.id);
                            numChanges++;
                            if(maxOperations>0 && numChanges>=maxOperations) {
                                return true;
                            }
                        }
                    }
                }
            }
        }
        return numChanges>0;
    }
    
    
    
}

class NodeInfo implements Iterable<IndexInfo> {
    final String id;
    private final Map<String, IndexInfo> indices = new HashMap<String, IndexInfo>();
    
    public NodeInfo(String id) {
        super();
        this.id = id;
    }
    
    public IndexInfo getIndexInfo(String indexId) {
      return indices.get(indexId);
    }
    
    
    public int numReplicas() {
        int sum = 0;
        for(IndexInfo index : indices.values()) {
            sum += index.replicas.size();
        }
        return sum;
    }
    
    public int numReplicas(String idx) {
        IndexInfo index = indices.get(idx);
        if(index == null) {
            return 0;
        } else {
            return index.replicas.size();
        }
    }

    public int numPrimaries(String idx) {
        IndexInfo index = indices.get(idx);
        if(index == null) {
            return 0;
        } else {
            int sum = 0;
            for (ShardInfo info : index.replicas) {
                if(info.replica.primary()) {
                    sum++;
                }
            }
            return sum;
        }
    }

    public int numPrimaries() {
        int sum = 0;
        for(IndexInfo index : indices.values()) {
            for (ShardInfo info : index.replicas) {
                if(info.replica.primary()) {
                    sum++;
                }
            }
        }
        return sum;
    }
    

    public Collection<ShardInfo> replicas() {
        Collection<ShardInfo> result = new ArrayList<ShardInfo>();
        for(IndexInfo index : indices.values()) {
            result.addAll(index.replicas);
        }
        return result;
    }
    
    public boolean addReplica(ShardInfo info) {
        IndexInfo index = indices.get(info.replica.index());
        if(index == null) {
            index = new IndexInfo(info.replica.index());
            indices.put(index.id, index);
        }
        return index.replicas.add(info);
    }
    
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Node("+id+"):\n");
        for(IndexInfo index : indices.values()) {
            sb.append('\t').append("index("+index.id+"):");
            for(ShardInfo shard : index.replicas) {
                sb.append('(').append(shard.replica.id()).append(')');
            }
            sb.append('\n');
        }
        return sb.toString();
    }
    
    public boolean removeReplica(ShardInfo info) {
        IndexInfo index = indices.get(info.replica.index());
        if(index==null){
            return false;
        } else {
            boolean removed = index.removeReplica(info);
            if(removed && index.replicas.isEmpty()) {
                indices.remove(info.replica.index());
            }
            return removed;
        }
    }

    @Override
    public Iterator<IndexInfo> iterator() {
        return indices.values().iterator();
    }    

}

class IndexInfo implements Comparator<ShardInfo> {
    protected final String id;
    final Collection<ShardInfo> replicas = new TreeSet<ShardInfo>(this);
    
    public IndexInfo(String id) {
        super();
        this.id = id;
    }

    public boolean removeReplica(ShardInfo replica) {
        return replicas.remove(replica);
    }
    
    public boolean addReplica(ShardInfo replica) {
        return replicas.add(replica);
    }
    
    @Override
    public int compare(ShardInfo o1, ShardInfo o2) {
        return o1.replica.id()-o2.replica.id();
    }
    
}

class ShardInfo {
    final MutableShardRouting replica;
    Decision decision;
    
    public ShardInfo(MutableShardRouting replica) {
        this(replica, null);
    }
    
    public ShardInfo(MutableShardRouting replica, Decision decision) {
        super();
        this.replica = replica;
        this.decision = decision;
    }
    
    
}

