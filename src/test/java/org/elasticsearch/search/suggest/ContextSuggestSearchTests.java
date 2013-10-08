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
import org.elasticsearch.search.suggest.context.CategoryContextMapping;
import org.elasticsearch.search.suggest.context.CompletionContextMapping;
import org.elasticsearch.search.suggest.context.FieldContextMapping;
import org.elasticsearch.search.suggest.context.GeoContextMapping;
import org.elasticsearch.test.ElasticsearchIntegrationTest;
import org.hamcrest.Matchers;
import org.junit.Test;

import java.io.IOException;
import java.util.*;

import static org.elasticsearch.cluster.metadata.IndexMetaData.SETTING_NUMBER_OF_REPLICAS;
import static org.elasticsearch.cluster.metadata.IndexMetaData.SETTING_NUMBER_OF_SHARDS;
import static org.elasticsearch.common.settings.ImmutableSettings.settingsBuilder;
import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;
import static org.hamcrest.Matchers.is;

public class ContextSuggestSearchTests extends ElasticsearchIntegrationTest {

    private static final String INDEX = "test";
    private static final String TYPE = "testType";
    private static final String FIELD = "testField";

    private static final String[][] HEROS = {
        {"Afari, Jamal", "Jamal Afari", "Jamal"},
        {"Allerdyce, St. John", "Allerdyce, John", "St. John", "St. John Allerdyce"},
        {"Beaubier, Jean-Paul", "Jean-Paul Beaubier", "Jean-Paul"},
        {"Beaubier, Jeanne-Marie", "Jeanne-Marie Beaubier", "Jeanne-Marie"},
        {"Braddock, Elizabeth \"Betsy\"", "Betsy", "Braddock, Elizabeth", "Elizabeth Braddock", "Elizabeth"},
        {"Cody Mushumanski gun Man", "the hunter", "gun man", "Cody Mushumanski"},
        {"Corbo, Adrian", "Adrian Corbo", "Adrian"},
        {"Corbo, Jared", "Jared Corbo", "Jared"},
        {"Creel, Carl \"Crusher\"", "Creel, Carl", "Crusher", "Carl Creel", "Carl"},
        {"Crichton, Lady Jacqueline Falsworth", "Lady Jacqueline Falsworth Crichton", "Lady Jacqueline Falsworth", "Jacqueline Falsworth"},
        {"Crichton, Kenneth", "Kenneth Crichton", "Kenneth"},
        {"MacKenzie, Al", "Al MacKenzie", "Al"},
        {"MacPherran, Mary \"Skeeter\"", "Mary MacPherran \"Skeeter\"", "MacPherran, Mary", "Skeeter", "Mary MacPherran"},
        {"MacTaggert, Moira", "Moira MacTaggert", "Moira"},
        {"Rasputin, Illyana", "Illyana Rasputin", "Illyana"},
        {"Rasputin, Mikhail", "Mikhail Rasputin", "Mikhail"},
        {"Rasputin, Piotr", "Piotr Rasputin", "Piotr"},
        {"Smythe, Alistair", "Alistair Smythe", "Alistair"},
        {"Smythe, Spencer", "Spencer Smythe", "Spencer"},
        {"Whitemane, Aelfyre", "Aelfyre Whitemane", "Aelfyre"},
        {"Whitemane, Kofi", "Kofi Whitemane", "Kofi"}
    };
    
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
        
        double precision = 100.0; // meters
        createIndexAndSettings();
        createMapping(TYPE, new GeoContextMapping(precision, true));
        
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
            XContentBuilder source = jsonBuilder()
                    .startObject().startObject(FIELD)
                    .startArray("input").value(input[i]).endArray()
                    .array("context", locations[i])
                    .field("payload", locations[i])
                    .endObject()
                    .endObject();
            client().prepareIndex(INDEX, TYPE, "" + i).setSource(source).execute().actionGet();
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
    
    @Test
    public void testSimplePrefix() throws Exception {
        createIndexAndSettings();
        createMapping(TYPE, new CategoryContextMapping());

        for (int i = 0; i < HEROS.length; i++) {
            XContentBuilder source = jsonBuilder()
                    .startObject().startObject(FIELD)
                    .startArray("input").value(HEROS[i]).endArray()
                    .array("context", Integer.toString(i % 3))
                    .startObject("payload")
                    .field("group", i  % 3)
                    .field("id", i)
                    .endObject()
                    .endObject()
                    .endObject();
            client().prepareIndex(INDEX, TYPE, "" + i).setSource(source).execute().actionGet();
        }

        refresh();

        assertPrefixSuggestions(0, "a", "Afari, Jamal", "Adrian Corbo", "Adrian");
        assertPrefixSuggestions(0, "b", "Beaubier, Jeanne-Marie");
        assertPrefixSuggestions(0, "c", "Corbo, Adrian", "Crichton, Lady Jacqueline Falsworth");
        assertPrefixSuggestions(0, "mary", "Mary MacPherran \"Skeeter\"", "Mary MacPherran");
        assertPrefixSuggestions(0, "s", "Skeeter", "Smythe, Spencer", "Spencer Smythe", "Spencer");
        assertPrefixSuggestions(1, "s", "St. John", "St. John Allerdyce");
        assertPrefixSuggestions(2, "s", "Smythe, Alistair");
        assertPrefixSuggestions(1, "w", "Whitemane, Aelfyre");
        assertPrefixSuggestions(2, "w", "Whitemane, Kofi");
    }

    @Test
    public void testSimpleField() throws Exception {
        createIndexAndSettings();
        createMapping(TYPE, new FieldContextMapping("category"));

        for (int i = 0; i < HEROS.length; i++) {
            client().prepareIndex(INDEX, TYPE, "" + i)
                    .setSource(jsonBuilder()
                            .startObject()
                            .field("category", Integer.toString(i % 3))
                            .startObject(FIELD)
                            .startArray("input").value(HEROS[i]).endArray()
                            .startArray("context").value(true).endArray()
                            .field("payload", Integer.toString(i  % 3))
                            .endObject()
                            .endObject()
                    )
                    .execute().actionGet();
        }
        
        refresh();
        
        assertFieldSuggestions("0", "a", "Afari, Jamal", "Adrian Corbo", "Adrian");
        assertFieldSuggestions("0", "b", "Beaubier, Jeanne-Marie");
        assertFieldSuggestions("0", "c", "Corbo, Adrian", "Crichton, Lady Jacqueline Falsworth");
        assertFieldSuggestions("0", "mary", "Mary MacPherran \"Skeeter\"", "Mary MacPherran");
        assertFieldSuggestions("0", "s", "Skeeter", "Smythe, Spencer", "Spencer Smythe", "Spencer");
        assertFieldSuggestions("1", "s", "St. John", "St. John Allerdyce");
        assertFieldSuggestions("2", "s", "Smythe, Alistair");
        assertFieldSuggestions("1", "w", "Whitemane, Aelfyre");
        assertFieldSuggestions("2", "w", "Whitemane, Kofi");
        
    }
    
    @Test
    public void testMultiValueField() throws Exception {
        createIndexAndSettings();
        createMapping(TYPE, new FieldContextMapping("category"));

        for (int i = 0; i < HEROS.length; i++) {
            client().prepareIndex(INDEX, TYPE, "" + i)
                    .setSource(jsonBuilder()
                            .startObject()
                            .startArray("category").value(Integer.toString(i % 3)).value("other").endArray()
                            .startObject(FIELD)
                            .startArray("input").value(HEROS[i]).endArray()
                            .array("context", true)
                            .field("payload", Integer.toString(i  % 3))
                            .endObject()
                            .endObject()
                    )
                    .execute().actionGet();
        }
        
        refresh();
        
        assertFieldSuggestions("0", "a", "Afari, Jamal", "Adrian Corbo", "Adrian");
        assertFieldSuggestions("0", "b", "Beaubier, Jeanne-Marie");
        assertFieldSuggestions("0", "c", "Corbo, Adrian", "Crichton, Lady Jacqueline Falsworth");
        assertFieldSuggestions("0", "mary", "Mary MacPherran \"Skeeter\"", "Mary MacPherran");
        assertFieldSuggestions("0", "s", "Skeeter", "Smythe, Spencer", "Spencer Smythe", "Spencer");
        assertFieldSuggestions("1", "s", "St. John", "St. John Allerdyce");
        assertFieldSuggestions("2", "s", "Smythe, Alistair");
        assertFieldSuggestions("1", "w", "Whitemane, Aelfyre");
        assertFieldSuggestions("2", "w", "Whitemane, Kofi");
        
    }
    
    @Test
    public void testMultiContext() throws Exception {
        createIndexAndSettings();
        createMapping(TYPE, new FieldContextMapping("categoryA"), new FieldContextMapping("categoryB"));

        for (int i = 0; i < HEROS.length; i++) {
            client().prepareIndex(INDEX, TYPE, "" + i)
                    .setSource(jsonBuilder()
                            .startObject()
                            .field("categoryA").value(Integer.toString(i % 3))
                            .field("categoryB").value((char)('a' + (i  % 3)))
                            .startObject(FIELD)
                            .startArray("input").value(HEROS[i]).endArray()
                            .array("context", true, true)
                            .field("payload", Integer.toString(i  % 3) + "" + (char)('a' + (i  % 3)))
                            .endObject()
                            .endObject()
                    )
                    .execute().actionGet();
        }
        
        refresh();
        
        assertMultiContextSuggestions("0", "a", "a", "Afari, Jamal", "Adrian Corbo", "Adrian");
        assertMultiContextSuggestions("0", "a", "b", "Beaubier, Jeanne-Marie");
        assertMultiContextSuggestions("0", "a", "c", "Corbo, Adrian", "Crichton, Lady Jacqueline Falsworth");
        assertMultiContextSuggestions("0", "a", "mary", "Mary MacPherran \"Skeeter\"", "Mary MacPherran");
        assertMultiContextSuggestions("0", "a", "s", "Skeeter", "Smythe, Spencer", "Spencer Smythe", "Spencer");
        assertMultiContextSuggestions("1", "b", "s", "St. John", "St. John Allerdyce");
        assertMultiContextSuggestions("2", "c", "s", "Smythe, Alistair");
        assertMultiContextSuggestions("1", "b", "w", "Whitemane, Aelfyre");
        assertMultiContextSuggestions("2", "c", "w", "Whitemane, Kofi");
    }
    
    @Test
    public void testSimpleType() throws Exception {
        String[] types = {TYPE+"A", TYPE+"B", TYPE+"C"};

        createIndexAndSettings();
        for (String type : types) {
            createMapping(type, new FieldContextMapping("_type"));
        }

        for (int i = 0; i < HEROS.length; i++) {
            String type = types[i % types.length];
            client().prepareIndex(INDEX, type, "" + i)
                    .setSource(jsonBuilder()
                            .startObject()
                            .startObject(FIELD)
                            .startArray("input").value(HEROS[i]).endArray()
                            .startArray("context").value(true).endArray()
                            .field("payload", type)
                            .endObject()
                            .endObject()
                    )
                    .execute().actionGet();
        }
        
        refresh();
        
        assertFieldSuggestions(types[0], "a", "Afari, Jamal", "Adrian Corbo", "Adrian");
        assertFieldSuggestions(types[0], "b", "Beaubier, Jeanne-Marie");
        assertFieldSuggestions(types[0], "c", "Corbo, Adrian", "Crichton, Lady Jacqueline Falsworth");
        assertFieldSuggestions(types[0], "mary", "Mary MacPherran \"Skeeter\"", "Mary MacPherran");
        assertFieldSuggestions(types[0], "s", "Skeeter", "Smythe, Spencer", "Spencer Smythe", "Spencer");
        assertFieldSuggestions(types[1], "s", "St. John", "St. John Allerdyce");
        assertFieldSuggestions(types[2], "s", "Smythe, Alistair");
        assertFieldSuggestions(types[1], "w", "Whitemane, Aelfyre");
        assertFieldSuggestions(types[2], "w", "Whitemane, Kofi");
    }
    
    public void assertGeoSuggestionsInRange(String location, String suggest, double precision) throws IOException {
        
        String suggestionName = RandomStrings.randomAsciiOfLength(new Random(), 10);
        CompletionSuggestionBuilder context = new CompletionSuggestionBuilder(suggestionName).field(FIELD).text(suggest).size(10).addGeoLocation(location);
        SuggestRequestBuilder suggestionRequest = client().prepareSuggest(INDEX).addSuggestion(context);
        SuggestResponse suggestResponse = suggestionRequest.execute().actionGet();

        System.out.println(suggestResponse);
        
        Suggest suggest2 = suggestResponse.getSuggest();
        assertTrue(suggest2.iterator().hasNext());
        for (Suggestion<? extends Entry<? extends Option>> s : suggest2) {
            CompletionSuggestion suggestion = (CompletionSuggestion)s; 
            assertTrue(suggestion.iterator().hasNext());
            for (CompletionSuggestion.Entry entry : suggestion) {
                List<CompletionSuggestion.Entry.Option> options = entry.getOptions();
                assertTrue(options.iterator().hasNext());
                for (CompletionSuggestion.Entry.Option option : options) {
                    String target = option.getPayloadAsString();
                    assertThat(distance(location, target), Matchers.lessThanOrEqualTo(precision));
                }
            }
        }
    }

    public void assertPrefixSuggestions(long prefix, String suggest, String...hits) throws IOException {
        String suggestionName = RandomStrings.randomAsciiOfLength(new Random(), 10);
        CompletionSuggestionBuilder context = new CompletionSuggestionBuilder(suggestionName).field(FIELD).text(suggest).size(hits.length + 1).addCategory(Long.toString(prefix));
        SuggestRequestBuilder suggestionRequest = client().prepareSuggest(INDEX).addSuggestion(context);
        SuggestResponse suggestResponse = suggestionRequest.execute().actionGet();

        System.out.println(suggestResponse);
        
        ArrayList<String> suggestions = new ArrayList<String>();
        
        Suggest suggest2 = suggestResponse.getSuggest();
        assertTrue(suggest2.iterator().hasNext());
        for (Suggestion<? extends Entry<? extends Option>> s : suggest2) {
            CompletionSuggestion suggestion = (CompletionSuggestion)s; 
            for (CompletionSuggestion.Entry entry : suggestion) {
                List<CompletionSuggestion.Entry.Option> options = entry.getOptions();
                for (CompletionSuggestion.Entry.Option option : options) {
                    Map<String, Object> payload = option.getPayloadAsMap();
                    int group = (Integer)payload.get("group");
                    String text = option.getText().string();
                    assertEquals(prefix, group);
                    suggestions.add(text);
                }
            }
        }
        assertSuggestionsMatch(suggestions, hits);
    }
    
    public void assertFieldSuggestions(String value, String suggest, String...hits) throws IOException {
        String suggestionName = RandomStrings.randomAsciiOfLength(new Random(), 10);
        CompletionSuggestionBuilder context = new CompletionSuggestionBuilder(suggestionName).field(FIELD).text(suggest).size(10).addContextField(value);
        SuggestRequestBuilder suggestionRequest = client().prepareSuggest(INDEX).addSuggestion(context);
        SuggestResponse suggestResponse = suggestionRequest.execute().actionGet();

        System.out.println(suggestResponse);
        
        ArrayList<String> suggestions = new ArrayList<String>();

        Suggest suggest2 = suggestResponse.getSuggest();
        for (Suggestion<? extends Entry<? extends Option>> s : suggest2) {
            CompletionSuggestion suggestion = (CompletionSuggestion)s; 
            for (CompletionSuggestion.Entry entry : suggestion) {
                List<CompletionSuggestion.Entry.Option> options = entry.getOptions();
                for (CompletionSuggestion.Entry.Option option : options) {
                    String payload = option.getPayloadAsString();
                    String text = option.getText().string();
                    assertEquals(value, payload);
                    suggestions.add(text);
                }
            }
        }
        assertSuggestionsMatch(suggestions, hits);
    }

    public void assertMultiContextSuggestions(String value1, String value2, String suggest, String...hits) throws IOException {
        String suggestionName = RandomStrings.randomAsciiOfLength(new Random(), 10);
        CompletionSuggestionBuilder context = new CompletionSuggestionBuilder(suggestionName).field(FIELD).text(suggest).size(10).addContextField(value1).addContextField(value2);
        SuggestRequestBuilder suggestionRequest = client().prepareSuggest(INDEX).addSuggestion(context);
        SuggestResponse suggestResponse = suggestionRequest.execute().actionGet();

        System.out.println(suggestResponse);
        
        ArrayList<String> suggestions = new ArrayList<String>();

        Suggest suggest2 = suggestResponse.getSuggest();
        for (Suggestion<? extends Entry<? extends Option>> s : suggest2) {
            CompletionSuggestion suggestion = (CompletionSuggestion)s; 
            for (CompletionSuggestion.Entry entry : suggestion) {
                List<CompletionSuggestion.Entry.Option> options = entry.getOptions();
                for (CompletionSuggestion.Entry.Option option : options) {
                    String payload = option.getPayloadAsString();
                    String text = option.getText().string();
                    assertEquals(value1 + value2, payload);
                    suggestions.add(text);
                }
            }
        }
        assertSuggestionsMatch(suggestions, hits);
    }

    private void assertSuggestionsMatch(List<String> suggestions, String...hits) {
        boolean[] suggested = new boolean[hits.length];
        Arrays.sort(hits);
        Arrays.fill(suggested, false);
        int numSuggestions = 0;

        for (String suggestion : suggestions) {
            int hitpos = Arrays.binarySearch(hits, suggestion);
            
            assertEquals(hits[hitpos], suggestion);
            assertTrue(hitpos >= 0);
            assertTrue(!suggested[hitpos]);
            
            suggested[hitpos] = true;
            numSuggestions++;

        }
        assertEquals(hits.length, numSuggestions);
    }
    
    private void createMapping(String type, CompletionContextMapping...context) throws IOException {
        createMapping(type, "simple", "simple", true, false, true, context);
    }

    private ImmutableSettings.Builder createDefaultSettings() {
        int randomShardNumber = between(1, 5);
        int randomReplicaNumber = between(0, cluster().size() - 1);
        return settingsBuilder().put(SETTING_NUMBER_OF_SHARDS, randomShardNumber).put(SETTING_NUMBER_OF_REPLICAS, randomReplicaNumber);
    }

    private void createIndexAndSettings() throws IOException {
        createIndexAndSettings(createDefaultSettings());
    }
    
    private void createIndexAndSettings(Settings.Builder settingsBuilder) throws IOException {
        client().admin().indices().prepareCreate(INDEX)
        .setSettings(settingsBuilder)
        .get();
    }
    
    private void createMapping(String type, String indexAnalyzer, String searchAnalyzer, boolean payloads, boolean preserveSeparators, boolean preservePositionIncrements, CompletionContextMapping...contexts) throws IOException {
        XContentBuilder mapping = jsonBuilder();
        mapping.startObject();
        mapping.startObject(type);
        mapping.startObject("properties");
        mapping.startObject(FIELD);
        mapping.field("type", "completion");
        mapping.field("index_analyzer", indexAnalyzer);
        mapping.field("search_analyzer", searchAnalyzer);
        mapping.field("payloads", payloads);
        mapping.field("preserve_separators", preserveSeparators);
        mapping.field("preserve_position_increments", preservePositionIncrements);
        mapping.startArray("context");
        for (CompletionContextMapping context : contexts) {
            mapping.value(context);
        }
        mapping.endArray();
        mapping.endObject();
        mapping.endObject();
        mapping.endObject();
        mapping.endObject();
        
        System.out.println(mapping.prettyPrint().string());
        
        PutMappingResponse putMappingResponse = client().admin().indices().preparePutMapping(INDEX).setType(type).setSource(mapping).get();

        assertThat(putMappingResponse.isAcknowledged(), is(true));
        ensureYellow();
    }

    public static double distance(String geohash1, String geohash2) {
        GeoPoint p1 = new GeoPoint(geohash1);
        GeoPoint p2 = new GeoPoint(geohash2);
        return GeoFilterTests.distance(p1.lat(), p1.lon(), p2.lat(), p2.lon());
    }
 
}
