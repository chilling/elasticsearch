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

package org.elasticsearch.search.suggest.context;

import org.elasticsearch.common.geo.GeoHashUtils;
import org.elasticsearch.common.geo.GeoUtils;
import org.elasticsearch.common.unit.DistanceUnit;

public class GeoMappingBuilder extends ContextBuilder<GeoContextMapping> {
    
    private int levels; // length of the geohashes
    private boolean neighbors; // take neighbor cell on the lowest level into account
    private String defaultLocation;

    protected GeoMappingBuilder() {
        this(GeoHashUtils.PRECISION, true);
    }

    protected GeoMappingBuilder(int levels, boolean neighbors) {
        precision(levels);
        neighbors(neighbors);
    }

    public GeoMappingBuilder precision(String precision) {
        return precision(DistanceUnit.parse(precision, DistanceUnit.METERS, DistanceUnit.METERS));
    }

    public GeoMappingBuilder precision(double precision, DistanceUnit unit) {
        return precision(unit.toMeters(precision));
    }

    public GeoMappingBuilder precision(double meters) {
        return precision(GeoUtils.geoHashLevelsForPrecision(meters));
    }
    
    public GeoMappingBuilder precision(int levels) {
        this.levels = levels;
        return this;
    }
    
    public GeoMappingBuilder neighbors(boolean neighbors) {
        this.neighbors = neighbors;
        return this;
    }
    
    public GeoMappingBuilder defaultLocation(String geohash) {
        this.defaultLocation = geohash;
        return this;
    }
    
    @Override
    public GeoContextMapping build() {
        return new GeoContextMapping(levels, neighbors, defaultLocation);
    }
    
}
