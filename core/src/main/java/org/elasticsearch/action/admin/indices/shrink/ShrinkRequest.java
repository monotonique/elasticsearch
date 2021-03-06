/*
 * Licensed to Elasticsearch under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Elasticsearch licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
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
package org.elasticsearch.action.admin.indices.shrink;

import org.elasticsearch.ElasticsearchParseException;
import org.elasticsearch.action.ActionRequestValidationException;
import org.elasticsearch.action.IndicesRequest;
import org.elasticsearch.action.admin.indices.create.CreateIndexRequest;
import org.elasticsearch.action.support.IndicesOptions;
import org.elasticsearch.action.support.master.AcknowledgedRequest;
import org.elasticsearch.common.ParseField;
import org.elasticsearch.common.ParseFieldMatcher;
import org.elasticsearch.common.ParseFieldMatcherSupplier;
import org.elasticsearch.common.bytes.BytesReference;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;
import org.elasticsearch.common.xcontent.ObjectParser;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.common.xcontent.XContentType;

import java.io.IOException;
import java.util.Map;
import java.util.Objects;

import static org.elasticsearch.action.ValidateActions.addValidationError;

/**
 * Request class to shrink an index into a single shard
 */
public class ShrinkRequest extends AcknowledgedRequest<ShrinkRequest> implements IndicesRequest {

    public static ObjectParser<ShrinkRequest, ParseFieldMatcherSupplier> PARSER =
        new ObjectParser<>("shrink_request", null);
    static {
        PARSER.declareField((parser, request, parseFieldMatcherSupplier) ->
                request.getShrinkIndexRequest().settings(parser.map()),
            new ParseField("settings"), ObjectParser.ValueType.OBJECT);
        PARSER.declareField((parser, request, parseFieldMatcherSupplier) ->
                request.getShrinkIndexRequest().aliases(parser.map()),
            new ParseField("aliases"), ObjectParser.ValueType.OBJECT);
    }

    private CreateIndexRequest shrinkIndexRequest;
    private String sourceIndex;

    ShrinkRequest() {}

    public ShrinkRequest(String targetIndex, String sourceindex) {
        this.shrinkIndexRequest = new CreateIndexRequest(targetIndex);
        this.sourceIndex = sourceindex;
    }

    @Override
    public ActionRequestValidationException validate() {
        ActionRequestValidationException validationException = shrinkIndexRequest == null ? null : shrinkIndexRequest.validate();
        if (sourceIndex == null) {
            validationException = addValidationError("source index is missing", validationException);
        }
        if (shrinkIndexRequest == null) {
            validationException = addValidationError("shrink index request is missing", validationException);
        }
        return validationException;
    }

    public void setSourceIndex(String index) {
        this.sourceIndex = index;
    }

    @Override
    public void readFrom(StreamInput in) throws IOException {
        super.readFrom(in);
        shrinkIndexRequest = new CreateIndexRequest();
        shrinkIndexRequest.readFrom(in);
        sourceIndex = in.readString();
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        super.writeTo(out);
        shrinkIndexRequest.writeTo(out);
        out.writeString(sourceIndex);
    }

    @Override
    public String[] indices() {
        return new String[] {sourceIndex};
    }

    @Override
    public IndicesOptions indicesOptions() {
        return IndicesOptions.lenientExpandOpen();
    }

    public void setShrinkIndex(CreateIndexRequest shrinkIndexRequest) {
        this.shrinkIndexRequest = Objects.requireNonNull(shrinkIndexRequest, "shrink index request must not be null");
    }

    /**
     * Returns the {@link CreateIndexRequest} for the shrink index
     */
    public CreateIndexRequest getShrinkIndexRequest() {
        return shrinkIndexRequest;
    }

    /**
     * Returns the source index name
     */
    public String getSourceIndex() {
        return sourceIndex;
    }

    public void source(BytesReference source) {
        XContentType xContentType = XContentFactory.xContentType(source);
        if (xContentType != null) {
            try (XContentParser parser = XContentFactory.xContent(xContentType).createParser(source)) {
                PARSER.parse(parser, this, () -> ParseFieldMatcher.EMPTY);
            } catch (IOException e) {
                throw new ElasticsearchParseException("failed to parse source for shrink index", e);
            }
        } else {
            throw new ElasticsearchParseException("failed to parse content type for shrink index source");
        }
    }
}
