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

import gnu.trove.map.hash.TObjectIntHashMap;

import org.elasticsearch.cluster.node.DiscoveryNode;
import org.elasticsearch.cluster.routing.IndexRoutingTable;
import org.elasticsearch.cluster.routing.IndexShardRoutingTable;
import org.elasticsearch.cluster.routing.MutableShardRouting;
import org.elasticsearch.cluster.routing.RoutingNode;
import org.elasticsearch.cluster.routing.RoutingNodes;
import org.elasticsearch.cluster.routing.ShardRouting;
import org.elasticsearch.cluster.routing.allocation.FailedRerouteAllocation;
import org.elasticsearch.cluster.routing.allocation.RoutingAllocation;
import org.elasticsearch.cluster.routing.allocation.StartedRerouteAllocation;
import org.elasticsearch.cluster.routing.allocation.decider.Decision;
import org.elasticsearch.common.component.AbstractComponent;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;

import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Vector;

import static org.elasticsearch.cluster.routing.ShardRoutingState.INITIALIZING;
import static org.elasticsearch.cluster.routing.ShardRoutingState.STARTED;


/**
 * 1)	Maximize the number of nodes that keep a primary
 * 2)	Minimize the number of primaries per node
 * 3)	Minimize the distance of the maximal and the minimal number of shards per node
 * 4)	Maximize the Number of Indices per Node
 * 5)	Keep care of replicas of the same shard on the same Node
 * 6)	Minimize the Number of Move-Operations   
 * 
 * 
 * @author schilling
 *
 */
public class BalancedShardsAllocator extends AbstractComponent implements ShardsAllocator {

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
        RoutingNodes routingNodes = allocation.routingNodes();

        // order nodes by number of shards (asc) 
        RoutingNode[] nodes = sortedNodesLeastToHigh(allocation);

        Iterator<MutableShardRouting> unassignedIterator = routingNodes.unassigned().iterator();
        int lastNode = 0;

        while (unassignedIterator.hasNext()) {
            MutableShardRouting shard = unassignedIterator.next();
            // do the allocation, finding the least "busy" node
            for (int i = 0; i < nodes.length; i++) {
                RoutingNode node = nodes[lastNode];
                lastNode++;
                if (lastNode == nodes.length) {
                    lastNode = 0;
                }

                Decision decision = allocation.deciders().canAllocate(shard, node, allocation);
                if (decision.type() == Decision.Type.YES) {
                    int numberOfShardsToAllocate = routingNodes.requiredAverageNumberOfShardsPerNode() - node.shards().size();
                    if (numberOfShardsToAllocate <= 0) {
                        continue;
                    }

                    changed = true;
                    node.add(shard);
                    unassignedIterator.remove();
                    break;
                }
            }
        }

        // allocate all the unassigned shards above the average per node.
        for (Iterator<MutableShardRouting> it = routingNodes.unassigned().iterator(); it.hasNext(); ) {
            MutableShardRouting shard = it.next();
            // go over the nodes and try and allocate the remaining ones
            for (RoutingNode routingNode : sortedNodesLeastToHigh(allocation)) {
                Decision decision = allocation.deciders().canAllocate(shard, routingNode, allocation);
                if (decision.type() == Decision.Type.YES) {
                    changed = true;
                    routingNode.add(shard);
                    it.remove();
                    break;
                }
            }
        }
        return changed;
    }
    
    public void distibuteIndices(RoutingAllocation allocation) {
    	/* First we try to assign one primary of every index to a node
    	 * The list <code>nodesWithoutAnyIndex</code> contains
    	 * ids of nodes that have no index assigned. initially
    	 * this list contains all node ids */
    	Set<String> nodesWithoutAnyIndex = new TreeSet<String>();
    	for (RoutingNode node : allocation.routingNodes())
    		nodesWithoutAnyIndex.add(node.nodeId());
    	
    	/* the <code>indicesToAssign</code> set contains the ids of
    	 * all the indices to ditribute. Initially this set contains
    	 * all index ids.*/
    	Set<String> indicesToAssign = new TreeSet<String>();
    	for (String index : allocation.routingTable().indicesRouting().keySet())
    		indicesToAssign.add(index);

    	/* Now lets find out which nodes contain which indices by checking the
    	 * primary shard of every index. The Map <code>nodeIndexMap</code>
    	 * maps the node ids to the index ids which it contains */
    	Map<String, Set<String>> nodeIndexMap = new TreeMap<String, Set<String>>();
    	for (String index : allocation.routingTable().indicesRouting().keySet()) {
        	IndexRoutingTable routing = allocation.routingTable().index(index);
        	for(IndexShardRoutingTable shard : routing.shards().values()) {
        		ShardRouting primary = shard.primaryShard();
        		if(nodesWithoutAnyIndex.contains(primary.currentNodeId())) {
            		nodesWithoutAnyIndex.remove(primary.currentNodeId());
            		indicesToAssign.remove(index);
            		Set<String> indices = nodeIndexMap.get(primary.currentNodeId());
            		if(indices == null)
            			nodeIndexMap.put(primary.currentNodeId(), indices = new TreeSet<String>());
            		indices.add(index);
            		break;
        		}
        	}
		}

    	/* After this step some nodes do not serve any index. And some indices have not been
    	 * mapped to any node. So iterate over the indices and assign one of its primary shards
    	 * to a node that does not serve any index, if those nodes still exist. Otherwise we take
    	 * some node the cluster. Once this step is done the indices per node distribution has
    	 * been maximized.*/ 
    	Iterator<String> nodes = nodesWithoutAnyIndex.iterator();
    	for (String index : indicesToAssign) {
    		IndexRoutingTable routing = allocation.routingTable().index(index);
    		// Get some primary shard of the current index
        	MutableShardRouting shard = allocation.routingNodes().findPrimaryForReplica(routing.shards().values().asList().get(0).primaryShard());
        	if(!nodes.hasNext())
        		allocation.routingNodes().getNodesToShards().keySet().iterator();
        	String node = nodes.next();
        	shard.relocate(node);
        	nodesWithoutAnyIndex.remove(node);		// be careful: we modify the set we are iterating over
        	
    		Set<String> indices = nodeIndexMap.get(node);
    		if(indices == null)
    			nodeIndexMap.put(node, indices = new TreeSet<String>());
    		indices.add(index);
		}
    	
    	/* Now we are going to assign the remaining shards of each index to
    	 * nodes that should not contain the index they already belong to */
    	
    	/* In the last step we are going to assign the replicas of each shard
    	 * under balance condition */
    	
    }

    @Override
    public boolean rebalance(RoutingAllocation allocation) {
    	RoutingNode[] routingNodes =  null;
    	
    	Map<String, Set<String>> primariesOnNode = new TreeMap<String, Set<String>>();
    	
    	Map<String, IndexRoutingTable> indexRouting = allocation.routingTable().indicesRouting();
    	
    	for(Entry<String, IndexRoutingTable> entry : indexRouting.entrySet()) {
    		String index = entry.getKey();
    		IndexRoutingTable table = entry.getValue();
    		
    		for(Entry<Integer, IndexShardRoutingTable> routing : table.getShards().entrySet()) {
    			IndexShardRoutingTable shard = routing.getValue();
    			
    			shard.
    			
    			Set<String> indices = primariesOnNode.get(shard.primaryShard().currentNodeId());
    			if(indices == null)
    				primariesOnNode.put(shard.primaryShard().currentNodeId(), indices = new TreeSet<String>());
    			indices.add(shard.primaryShard().index());
    		}
    		
    	}
    	
    	
    	
    	
    	// take shards form busy nodes and move them to less busy nodes
        boolean changed = false;
        RoutingNode[] sortedNodesLeastToHigh = sortedNodesLeastToHigh(allocation);
        if (sortedNodesLeastToHigh.length == 0) {
            return false;
        }
        int lowIndex = 0;
        int highIndex = sortedNodesLeastToHigh.length - 1;
        boolean relocationPerformed;
        do {
            relocationPerformed = false;
            while (lowIndex != highIndex) {
                RoutingNode lowRoutingNode = sortedNodesLeastToHigh[lowIndex];
                RoutingNode highRoutingNode = sortedNodesLeastToHigh[highIndex];
                int averageNumOfShards = allocation.routingNodes().requiredAverageNumberOfShardsPerNode();

                // only active shards can be removed so must count only active ones.
                if (highRoutingNode.numberOfOwningShards() <= averageNumOfShards) {
                    highIndex--;
                    continue;
                }

                if (lowRoutingNode.shards().size() >= averageNumOfShards) {
                    lowIndex++;
                    continue;
                }

                // Take a started shard from a "busy" node and move it to less busy node and go on 
                boolean relocated = false;
                List<MutableShardRouting> startedShards = highRoutingNode.shardsWithState(STARTED);
                for (MutableShardRouting startedShard : startedShards) {
                    Decision rebalanceDecision = allocation.deciders().canRebalance(startedShard, allocation);
                    if (rebalanceDecision.type() == Decision.Type.NO) {
                        continue;
                    }

                    Decision allocateDecision = allocation.deciders().canAllocate(startedShard, lowRoutingNode, allocation);
                    if (allocateDecision.type() == Decision.Type.YES) {
                        changed = true;
                        lowRoutingNode.add(new MutableShardRouting(startedShard.index(), startedShard.id(),
                                lowRoutingNode.nodeId(), startedShard.currentNodeId(),
                                startedShard.primary(), INITIALIZING, startedShard.version() + 1));

                        startedShard.relocate(lowRoutingNode.nodeId());
                        relocated = true;
                        relocationPerformed = true;
                        break;
                    }
                }

                if (!relocated) {
                    highIndex--;
                }
            }
        } while (relocationPerformed);
        return changed;
    }

    @Override
    public boolean move(MutableShardRouting shardRouting, RoutingNode node, RoutingAllocation allocation) {
        if (!shardRouting.started()) {
            return false;
        }
        boolean changed = false;
        RoutingNode[] sortedNodesLeastToHigh = sortedNodesLeastToHigh(allocation);
        if (sortedNodesLeastToHigh.length == 0) {
            return false;
        }

        for (RoutingNode nodeToCheck : sortedNodesLeastToHigh) {
            // check if its the node we are moving from, no sense to check on it
            if (nodeToCheck.nodeId().equals(node.nodeId())) {
                continue;
            }
            Decision decision = allocation.deciders().canAllocate(shardRouting, nodeToCheck, allocation);
            if (decision.type() == Decision.Type.YES) {
                nodeToCheck.add(new MutableShardRouting(shardRouting.index(), shardRouting.id(),
                        nodeToCheck.nodeId(), shardRouting.currentNodeId(),
                        shardRouting.primary(), INITIALIZING, shardRouting.version() + 1));

                shardRouting.relocate(nodeToCheck.nodeId());
                changed = true;
                break;
            }
        }

        return changed;
    }

    private RoutingNode[] sortedNodesLeastToHigh(RoutingAllocation allocation) {
        // create count per node id, taking into account relocations
        final TObjectIntHashMap<String> nodeCounts = new TObjectIntHashMap<String>();
        for (RoutingNode node : allocation.routingNodes()) {
            for (int i = 0; i < node.shards().size(); i++) {
                ShardRouting shardRouting = node.shards().get(i);
                String nodeId = shardRouting.relocating() ? shardRouting.relocatingNodeId() : shardRouting.currentNodeId();
                nodeCounts.adjustOrPutValue(nodeId, 1, 1);
            }
        }
        RoutingNode[] nodes = allocation.routingNodes().nodesToShards().values().toArray(new RoutingNode[allocation.routingNodes().nodesToShards().values().size()]);
        Arrays.sort(nodes, new Comparator<RoutingNode>() {
            @Override
            public int compare(RoutingNode o1, RoutingNode o2) {
                return nodeCounts.get(o1.nodeId()) - nodeCounts.get(o2.nodeId());
            }
        });
        return nodes;
    }
}
