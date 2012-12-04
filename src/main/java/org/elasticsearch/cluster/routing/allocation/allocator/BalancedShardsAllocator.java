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
import java.util.HashSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Vector;

import org.elasticsearch.cluster.routing.IndexRoutingTable;
import org.elasticsearch.cluster.routing.IndexShardRoutingTable;
import org.elasticsearch.cluster.routing.MutableShardRouting;
import org.elasticsearch.cluster.routing.RoutingNode;
import org.elasticsearch.cluster.routing.RoutingNodes;
import org.elasticsearch.cluster.routing.ShardRouting;
import org.elasticsearch.cluster.routing.allocation.FailedRerouteAllocation;
import org.elasticsearch.cluster.routing.allocation.RoutingAllocation;
import org.elasticsearch.cluster.routing.allocation.StartedRerouteAllocation;
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

    @Inject
    public BalancedShardsAllocator(Settings settings) {
        super(settings);
    }

    @Override
    public void applyStartedShards(StartedRerouteAllocation allocation) {
    }

    @Override
    public void applyFailedShards(FailedRerouteAllocation allocation) {
    }

    @Override
    public boolean allocateUnassigned(RoutingAllocation allocation) {
        boolean changed = false;
        return changed;
    }

    @Override
    public boolean rebalance(RoutingAllocation allocation) {
        AllocationTree allocator = new AllocationTree(allocation, balance);
        allocator.balance(1.1f);
        return allocator.apply();
    }

    @Override
    public boolean move(MutableShardRouting shardRouting, RoutingNode node, RoutingAllocation allocation) {
        boolean changed = false;
        return changed;
    }

}

abstract class WeightFunction {
    public abstract float weight(AllocationTree allocation, NodeInfo node, String index);
}

class BasicBalance extends WeightFunction {

    private final float indexBalance;
    private final float shardBalance;
    private final float primaryBalance;

    protected BasicBalance(float indexBalance, float shardBalance, float primaryBalance) {
        super();
        
        float x = indexBalance + shardBalance + primaryBalance;
        
        this.indexBalance = indexBalance/x;
        this.shardBalance = shardBalance/x;
        this.primaryBalance = primaryBalance/x;
    }

    @Override
    public float weight(AllocationTree allocation, NodeInfo node, String index) {
        return    ((indexBalance * node.numReplicas(index)) - allocation.avgReplicasOfIndexPerNode(index))
                + ((shardBalance * node.numReplicas()) - allocation.avgReplicasPerNode())
                + ((primaryBalance * node.numPrimaries()) - allocation.avgPrimariesPerNode());
    }


}


class WeightOrder implements Comparator<NodeInfo> {
    final AllocationTree allocation;
    final WeightFunction function;
    final String index;
    
    public WeightOrder(AllocationTree allocation, WeightFunction function, String index) {
        super();
        this.allocation = allocation;
        this.function = function;
        this.index = index;
    }

    public float weight(NodeInfo node) {
        return function.weight(allocation, node, index);
    }
    
    @Override
    public int compare(NodeInfo n1, NodeInfo n2) {
        final float w1 = weight(n1);
        final float w2 = weight(n2);
        if(w1<w2) {
            return -1;
        } else if(w1>w2) {
            return 1;
        } else {
            return n1.id.compareTo(n2.id);
        }
    }
}

class AllocationTree {
    
    private final ArrayList<NodeInfo> nodes;
    private final HashSet<String> indices;
    private final RoutingAllocation allocation;
    private final WeightFunction weight;
    
    public AllocationTree(RoutingAllocation allocation,WeightFunction weight) {
        this.allocation = allocation;
        this.nodes = new ArrayList<NodeInfo>();
        this.indices = new HashSet<String>();
        this.weight = weight;
        
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
    
    public int numReplicas() {
        int sum = 0;
        for(NodeInfo node : nodes) {
            sum += node.numReplicas();
        }
        return sum;
    }
    
    public int numPrimaries() {
        int sum = 0;
        for(NodeInfo node : nodes) {
            sum += node.numPrimaries();
        }
        return sum;
    }
    
    public int numReplicas(String index) {
        int sum = 0;
        for(NodeInfo node : nodes) {
            sum+=node.numReplicas(index);
        }
        return sum;
    }
    
    public int numPrimaries(String index) {
        int sum = 0;
        for(NodeInfo node : nodes) {
            sum += node.numPrimaries(index);
        }
        return sum;
    }
    
    public float avgReplicasOfIndexPerNode(String index) {
        return 1.0f * numReplicas(index) / numNodes();
    }
    
    public float avgReplicasPerNode() {
        return 1.0f * numReplicas() / numNodes();
    }
    
    public float avgPrimariesPerNode() {
        return 1.0f * numPrimaries() / numNodes();
    }

    public float avgPrimariesOfIndexPerNode(String index) {
        return 1.0f * numPrimaries(index) / numNodes();
    }
    
    public boolean distributeReplicas(Collection<MutableShardRouting> replicas) {
        Collection<MutableShardRouting> others = new Vector<MutableShardRouting>();
        
        boolean failed = false;
        
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
        if(nodes.size()>0) {
        
            NodeInfo minNode = null;
            float min = Float.POSITIVE_INFINITY;
            
            for(NodeInfo node : nodes) {
                float w = weight.weight(this, node, replica.index());
                if(w < min) {
                    minNode = node;
                    min = w;
                }
            }
                    
            NodeInfo node = minNode;
            RoutingNode routing = allocation.routingNodes().node(node.id);
            if(allocation.deciders().canAllocate(replica, routing, allocation).type() == Type.YES) {
                return node.addReplica(replica);
            }
        }
        return false;
    }
    
    protected MutableShardRouting relocateSomeShard(NodeInfo src, NodeInfo dst, String idx) {
        IndexInfo index = src.indices.get(idx);
        if(index == null) {
            return null;
        } else {
            RoutingNode node = allocation.routingNodes().node(dst.id);
            float minCost = Float.POSITIVE_INFINITY;
            MutableShardRouting candidate = null;
            
            for(MutableShardRouting replica : index.replicas) {
                if(allocation.deciders().canAllocate(replica, node, allocation).type() == Type.YES) {
                    if(src.removeReplica(replica)) {
                        if(dst.addReplica(replica)) {
                            
                            float srcWeight = weight.weight(this, src, idx);
                            float dstWeight = weight.weight(this, dst, idx);
                            float currentCost = dstWeight-srcWeight; 
                            
                            if(currentCost<minCost) {
                                minCost = currentCost;
                                candidate = replica;
                            }
                            dst.removeReplica(replica);
                        }
                        src.addReplica(replica);
                    }
                    
                }
            }
            
            if(candidate != null) {
                src.removeReplica(candidate);
                dst.addReplica(candidate);
                return candidate;
            } else {
                return null;
            }
        }
    }
    
    protected boolean balance(float treshold) {
        if(nodes.size()<=1) {
            return false;
        } else {
            NodeInfo[] nodes = this.nodes.toArray(new NodeInfo[numNodes()]);
            boolean changed = false;
            
            for(String index : indices) {
                WeightOrder order = new WeightOrder(this, weight, index);
                Arrays.sort(nodes, order);
                float lastDiff = Float.POSITIVE_INFINITY;
                
                while(true) {
                    final NodeInfo minNode = nodes[0]; 
                    final NodeInfo maxNode = nodes[nodes.length-1];
        
                    float minWeight = order.weight(minNode);
                    float maxWeight = order.weight(maxNode);
                    float diff = (maxWeight - minWeight);
                    
                    if(lastDiff == diff) {
                        break;
                    } else {
                        lastDiff = diff;
                    }
                    
                    if(diff>treshold) {
                        if(relocateSomeShard(maxNode, minNode, index) != null) {
                            changed = true;
                        }
                    } else {
                        break;
                    }
                }
            }
            return changed;
        }
    }
    
    public int numNodes() {
        return nodes.size();
    }    
        
    public boolean apply() {
        boolean changed = false;
        for(NodeInfo node : nodes) {
            for(IndexInfo index : node.indices.values()) {
                for(MutableShardRouting shard : index.replicas) {
                    if(!shard.currentNodeId().equals(node.id)) {
                        shard.relocate(node.id);
                        changed = true;
                    }
                }
            }
        }
        return changed;
    }
    
}

class NodeInfo {
    final String id;
    final TreeMap<String, IndexInfo> indices = new TreeMap<String, IndexInfo>();
    
    public NodeInfo(String id) {
        super();
        this.id = id;
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
            for (ShardRouting replica : index.replicas) {
                if(replica.primary()) {
                    sum++;
                }
            }
            return sum;
        }
    }

    public int numPrimaries() {
        int sum = 0;
        for(IndexInfo index : indices.values()) {
            for (ShardRouting replica : index.replicas) {
                if(replica.primary()) {
                    sum++;
                }
            }
        }
        return sum;
    }
    

    public Collection<MutableShardRouting> replicas() {
        Collection<MutableShardRouting> result = new Vector<MutableShardRouting>();
        for(IndexInfo index : indices.values()) {
            result.addAll(index.replicas);
        }
        return result;
    }
    
    public boolean addReplica(MutableShardRouting replica) {
        IndexInfo index = indices.get(replica.index());
        if(index == null) {
            index = new IndexInfo(replica.index());
            indices.put(index.id, index);
        }
        return index.replicas.add(replica);
    }
    
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Node("+id+"):\n");
        for(IndexInfo index : indices.values()) {
            sb.append('\t').append("index("+index.id+"):");
            for(ShardRouting shard : index.replicas) {
                sb.append('(').append(shard.id()).append(')');
            }
            sb.append('\n');
        }
        return sb.toString();
    }
    
    public boolean removeReplica(MutableShardRouting replica) {
        IndexInfo index = indices.get(replica.index());
        if(index==null){
            return false;
        } else {
            boolean removed = index.removeReplica(replica);
            if(removed && index.replicas.isEmpty()) {
                indices.remove(replica.index());
            }
            return removed;
        }
    }    

}

class IndexInfo implements Comparator<MutableShardRouting> {
    final String id;
    final TreeSet<MutableShardRouting> replicas = new TreeSet<MutableShardRouting>(this);
    
    public IndexInfo(String id) {
        super();
        this.id = id;
    }

    public boolean removeReplica(MutableShardRouting replica) {
        return replicas.remove(replica);
    }
    
    public boolean addReplica(MutableShardRouting replica) {
        return replicas.add(replica);
    }
    
    @Override
    public int compare(MutableShardRouting o1, MutableShardRouting o2) {
        return o1.id()-o2.id();
    }
    
    
}

