/*
 * Licensed to Elastic Search and Shay Banon under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. ElasticSearch licenses this
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
package org.elasticsearch.test.integration.rest.helper;

import java.util.List;
import java.util.Map;
import java.util.HashMap;

public class HttpClientResponse {
    private final String response;
    private final int errorCode;
    private Map<String, List<String>> headers;
    private final Throwable e;

    public HttpClientResponse(String response, int errorCode, Map<String, List<String>> headers,  Throwable e) {
        this.response = response;
        this.errorCode = errorCode;
        this.headers = headers;
        this.e = e;
    }

    public String response() {
        return response;
    }

    public int errorCode() {
        return errorCode;
    }

    public Throwable cause() {
        return e;
    }

    public Map<String, List<String>> getHeaders() {
        return headers;
    }

    public String getHeader(String name) {
        if (headers == null) {
            return null;
        }
        List<String> vals = headers.get(name);
        if (vals == null || vals.size() == 0) {
            return null;
        }
        return vals.iterator().next();
    }
}
