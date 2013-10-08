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
package org.elasticsearch.search.suggest;

import com.carrotsearch.randomizedtesting.generators.RandomStrings;
import org.elasticsearch.action.admin.indices.mapping.put.PutMappingResponse;
import org.elasticsearch.action.suggest.SuggestRequestBuilder;
import org.elasticsearch.action.suggest.SuggestResponse;
import org.elasticsearch.common.geo.GeoPoint;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.search.geo.GeoFilterTests;
import org.elasticsearch.search.suggest.Suggest.Suggestion;
import org.elasticsearch.search.suggest.Suggest.Suggestion.Entry;
import org.elasticsearch.search.suggest.Suggest.Suggestion.Entry.Option;
import org.elasticsearch.search.suggest.completion.CompletionSuggestion;
import org.elasticsearch.search.suggest.completion.CompletionSuggestionBuilder;
import org.elasticsearch.test.AbstractIntegrationTest;
import org.hamcrest.Matchers;
import org.junit.Test;

import java.io.IOException;
import java.util.List;
import java.util.Random;

import static org.elasticsearch.cluster.metadata.IndexMetaData.SETTING_NUMBER_OF_REPLICAS;
import static org.elasticsearch.cluster.metadata.IndexMetaData.SETTING_NUMBER_OF_SHARDS;
import static org.elasticsearch.common.settings.ImmutableSettings.settingsBuilder;
import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;
import static org.hamcrest.Matchers.is;

public class GeoSuggestSearchTests extends AbstractIntegrationTest {

    private static final String INDEX = "test";
    private static final String TYPE = "testType";
    private static final String FIELD = "testField";

    @Test
    public void testSimpleGeo() throws Exception {
        
        String reinickendorf = "u337p3mp11e2";
        String pankow = "u33e0cyyjur4";
        String koepenick = "u33dm4f7fn40";
        String bernau = "u33etnjf1yjn";
        String berlin = "u33dc1v0xupz";
        String mitte = "u33dc0cpke4q";
        String steglitz = "u336m36rjh2p";
        String wilmersdorf = "u336wmw0q41s";
        String spandau = "u336uqek7gh6";
        String tempelhof = "u33d91jh3by0";
        String schoeneberg = "u336xdrkzbq7";
        String treptow = "u33d9unn7fp7";
        
        double precision = 600.0; // meters
        
        createIndexAndMapping(precision);
        
        String[] locations = {reinickendorf, pankow, koepenick, bernau,
                berlin, mitte, steglitz, wilmersdorf, spandau, tempelhof,
                schoeneberg, treptow};
        
        String[][] input = {
                {"pizza - reinickendorf", "pizza", "food"},
                {"pizza - pankow", "pizza", "food"},
                {"pizza - koepenick", "pizza", "food"},
                {"pizza - bernau", "pizza", "food"},
                {"pizza - berlin", "pizza", "food"},
                {"pizza - mitte", "pizza - berlin mitte", "pizza", "food"},
                {"pizza - steglitz", "pizza - Berlin-Steglitz", "pizza", "food"},
                {"pizza - wilmersdorf", "pizza", "food"},
                {"pizza - spandau", "spandau bei berlin", "pizza", "food"},
                {"pizza - tempelhof", "pizza - berlin-tempelhof", "pizza", "food"},
                {"pizza - schoeneberg", "pizza - schöneberg", "pizza - berlin schoeneberg", "pizza", "food"},
                {"pizza - treptow", "pizza", "food"}};

        
        for (int i = 0; i < locations.length; i++) {
            client().prepareIndex(INDEX, TYPE, "" + i)
                    .setSource(jsonBuilder()
                            .startObject().startObject(FIELD)
                            .startArray("input").value(input[i]).endArray()
                            .field("context", locations[i])
                            .field("payload", locations[i])
                            .endObject()
                            .endObject()
                    )
                    .execute().actionGet();
        }

        refresh();

        assertGeoSuggestionsInRange(berlin, "pizza", precision);
        assertGeoSuggestionsInRange(reinickendorf, "pizza", precision);
        assertGeoSuggestionsInRange(spandau, "pizza", precision);
        assertGeoSuggestionsInRange(koepenick, "pizza", precision);
        assertGeoSuggestionsInRange(schoeneberg, "pizza", precision);
        assertGeoSuggestionsInRange(tempelhof, "pizza", precision);
        assertGeoSuggestionsInRange(bernau, "pizza", precision);
        assertGeoSuggestionsInRange(pankow, "pizza", precision);
        assertGeoSuggestionsInRange(mitte, "pizza", precision);
        assertGeoSuggestionsInRange(steglitz, "pizza", precision);
        assertGeoSuggestionsInRange(mitte, "pizza", precision);
        assertGeoSuggestionsInRange(wilmersdorf, "pizza", precision);
        assertGeoSuggestionsInRange(treptow, "pizza", precision);

    }

    public void assertGeoSuggestionsInRange(String location, String suggest, double precision) throws IOException {
        
        String suggestionName = RandomStrings.randomAsciiOfLength(new Random(), 10);
        CompletionSuggestionBuilder context = new CompletionSuggestionBuilder(suggestionName).field(FIELD).text(suggest).size(10).setGeoLocation(location);
        SuggestRequestBuilder suggestionRequest = client().prepareSuggest(INDEX).addSuggestion(context);
        
        SuggestResponse suggestResponse = suggestionRequest.execute().actionGet();

        Suggest suggest2 = suggestResponse.getSuggest();
        for (Suggestion<? extends Entry<? extends Option>> s : suggest2) {
            CompletionSuggestion suggestion = (CompletionSuggestion)s; 
            for (CompletionSuggestion.Entry entry : suggestion) {
                List<CompletionSuggestion.Entry.Option> options = entry.getOptions();
                for (CompletionSuggestion.Entry.Option option : options) {
                    String target = option.getPayloadAsString();
                    assertThat(distance(location, target), Matchers.lessThanOrEqualTo(precision));
                }
            }
        }
    }

    private void createIndexAndMapping(double precision) throws IOException {
        createIndexAndMapping("simple", "simple", true, false, true, precision);
    }

    private void createIndexAndMapping(String indexAnalyzer, String searchAnalyzer, boolean payloads, boolean preserveSeparators, boolean preservePositionIncrements, double precision) throws IOException {
        createIndexAndMappingAndSettings(createDefaultSettings(), indexAnalyzer, searchAnalyzer, payloads, preserveSeparators, preservePositionIncrements, precision);
    }

    private ImmutableSettings.Builder createDefaultSettings() {
        int randomShardNumber = between(1, 5);
        int randomReplicaNumber = between(0, cluster().numNodes() - 1);
        return settingsBuilder().put(SETTING_NUMBER_OF_SHARDS, randomShardNumber).put(SETTING_NUMBER_OF_REPLICAS, randomReplicaNumber);
    }

    private void createIndexAndMappingAndSettings(Settings.Builder settingsBuilder, String indexAnalyzer, String searchAnalyzer, boolean payloads, boolean preserveSeparators, boolean preservePositionIncrements, double precision) throws IOException {
        client().admin().indices().prepareDelete().get();
        client().admin().indices().prepareCreate(INDEX)
                .setSettings(settingsBuilder)
                .get();
        
        XContentBuilder mapping = jsonBuilder().startObject()
                .startObject(TYPE).startObject("properties")
                .startObject(FIELD)
                .field("type", "completion")
                .field("index_analyzer", indexAnalyzer)
                .field("search_analyzer", searchAnalyzer)
                .field("payloads", payloads)
                .field("preserve_separators", preserveSeparators)
                .field("preserve_position_increments", preservePositionIncrements)
                
                .startObject("context")
                .startObject("geolocation")
                .field("precision", precision+"m")
                .endObject()
                .endObject()
                
                .endObject()
                .endObject().endObject()
                .endObject();
        
        PutMappingResponse putMappingResponse = client().admin().indices().preparePutMapping(INDEX).setType(TYPE).setSource(mapping).get();
        assertThat(putMappingResponse.isAcknowledged(), is(true));
        ensureYellow();
    }

    public static double distance(String geohash1, String geohash2) {
        GeoPoint p1 = new GeoPoint(geohash1);
        GeoPoint p2 = new GeoPoint(geohash2);
        return GeoFilterTests.distance(p1.lat(), p1.lon(), p2.lat(), p2.lon());
    }
 
}
