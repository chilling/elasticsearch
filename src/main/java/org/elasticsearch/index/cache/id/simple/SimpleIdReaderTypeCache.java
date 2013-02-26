/*
 * Licensed to Elastic Search and Shay Banon under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. Elastic Search licenses this
 * file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.elasticsearch.index.cache.id.simple;

import org.elasticsearch.common.RamUsage;
import org.elasticsearch.common.bytes.HashedBytesArray;
import org.elasticsearch.index.cache.id.IdReaderTypeCache;

import com.carrotsearch.hppc.ObjectIntOpenHashMap;
import com.carrotsearch.hppc.hash.MurmurHash3;

/**
 *
 */
public class SimpleIdReaderTypeCache implements IdReaderTypeCache {

    private final String type;

    private final ObjectIntOpenHashMap<HashedBytesArray> idToDoc;

    private final HashedBytesArray[] docIdToId;

    private final HashedBytesArray[] parentIdsValues;

    private final int[] parentIdsOrdinals;

    private long sizeInBytes = -1;

    public SimpleIdReaderTypeCache(String type, ObjectIntOpenHashMap<HashedBytesArray> idToDoc, HashedBytesArray[] docIdToId,
                                   HashedBytesArray[] parentIdsValues, int[] parentIdsOrdinals) {
        this.type = type;
        this.idToDoc = idToDoc;
        this.docIdToId = docIdToId;
//        this.idToDoc.trimToSize();
        this.parentIdsValues = parentIdsValues;
        this.parentIdsOrdinals = parentIdsOrdinals;
    }

    public String type() {
        return this.type;
    }

    public HashedBytesArray parentIdByDoc(int docId) {
        return parentIdsValues[parentIdsOrdinals[docId]];
    }

    public int docById(HashedBytesArray uid) {
        return idToDoc.get(uid);
    }

    public HashedBytesArray idByDoc(int docId) {
        return docIdToId[docId];
    }

    public long sizeInBytes() {
        if (sizeInBytes == -1) {
            sizeInBytes = computeSizeInBytes();
        }
        return sizeInBytes;
    }

    /**
     * Returns an already stored instance if exists, if not, returns null;
     */
    public HashedBytesArray canReuse(HashedBytesArray id) {
        final Object[] keys = idToDoc.keys;
        final boolean[] allocated = idToDoc.allocated;
        final int hash = id.hashCode();
        final int slot = MurmurHash3.hash(hash) & (keys.length-1);

        int assigned = idToDoc.assigned;
        
        for (int i = 0; assigned>0; i++) {
            int index = (slot + i) % allocated.length;
            if(allocated[index]) {
                if(keys[index].hashCode() == hash) {
                    if(id.equals(keys[index])) {
                        return (HashedBytesArray)keys[index];
                    }
                }
                assigned--;
            } else {
                break;
            }
        }
        
        return null;

    }

    long computeSizeInBytes() {
        long sizeInBytes = 0;
        // Ignore type field
        //  sizeInBytes += ((type.length() * RamUsage.NUM_BYTES_CHAR) + (3 * RamUsage.NUM_BYTES_INT)) + RamUsage.NUM_BYTES_OBJECT_HEADER;
        sizeInBytes += RamUsage.NUM_BYTES_ARRAY_HEADER + (idToDoc.allocated.length * RamUsage.NUM_BYTES_INT);
        sizeInBytes += (idToDoc.allocated.length - idToDoc.assigned) * RamUsage.NUM_BYTES_OBJECT_REF;
        
        final boolean[] allocated = idToDoc.allocated;
        final Object[] keys = idToDoc.keys;
        int assigned = idToDoc.assigned;
        
        for (int i = 0; assigned>0; i++) {
            if(allocated[i]) {
                sizeInBytes += RamUsage.NUM_BYTES_OBJECT_HEADER + (((HashedBytesArray)keys[i]).length() + RamUsage.NUM_BYTES_INT);
                assigned--;
            }
        }
        
       
//        for (Object o : idToDoc._set) {
//            if (o == TObjectHash.FREE || o == TObjectHash.REMOVED) {
//                sizeInBytes += RamUsage.NUM_BYTES_OBJECT_REF;
//            } else {
//                HashedBytesArray bytesArray = (HashedBytesArray) o;
//                sizeInBytes += RamUsage.NUM_BYTES_OBJECT_HEADER + (bytesArray.length() + RamUsage.NUM_BYTES_INT);
//            }
//        }

        // The docIdToId array contains references to idToDoc for this segment or other segments, so we can use OBJECT_REF
        sizeInBytes += RamUsage.NUM_BYTES_ARRAY_HEADER + (RamUsage.NUM_BYTES_OBJECT_REF * docIdToId.length);
        for (HashedBytesArray bytesArray : parentIdsValues) {
            if (bytesArray == null) {
                sizeInBytes += RamUsage.NUM_BYTES_OBJECT_REF;
            } else {
                sizeInBytes += RamUsage.NUM_BYTES_OBJECT_HEADER + (bytesArray.length() + RamUsage.NUM_BYTES_INT);
            }
        }
        sizeInBytes += RamUsage.NUM_BYTES_ARRAY_HEADER + (RamUsage.NUM_BYTES_INT * parentIdsOrdinals.length);

        return sizeInBytes;
    }

}
