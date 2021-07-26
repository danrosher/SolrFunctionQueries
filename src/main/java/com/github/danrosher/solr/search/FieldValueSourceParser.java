//Copyright (c) 2021, Dan Rosher
//    All rights reserved.
//
//    This source code is licensed under the BSD-style license found in the
//    LICENSE file in the root directory of this source tree.

package com.github.danrosher.solr.search;

import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.queries.function.FunctionValues;
import org.apache.lucene.queries.function.ValueSource;
import org.apache.lucene.queries.function.docvalues.DoubleDocValues;
import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.BytesRefBuilder;
import org.apache.solr.schema.FieldType;
import org.apache.solr.search.FunctionQParser;
import org.apache.solr.search.SyntaxError;
import org.apache.solr.search.ValueSourceParser;

import java.io.IOException;
import java.util.Map;
import java.util.Objects;

public class FieldValueSourceParser extends ValueSourceParser {

    @Override
    public ValueSource parse(FunctionQParser fp) throws SyntaxError {
        String field = fp.parseArg();
        String value = fp.parseArg();
        double boost = fp.parseDouble();
        FieldType ft = fp.getReq()
            .getSchema()
            .getFieldTypeNoEx(field);
        BytesRefBuilder indexedBytes = new BytesRefBuilder();
        ft.readableToIndexed(value, indexedBytes);
        return new FieldValueSource(field, value,indexedBytes.toBytesRef(), boost);
    }
}

final class FieldValueSource extends ValueSource {

    private final String field;
    private final String value;
    private final BytesRef indexedBytes;
    private final double boost;

    public FieldValueSource(String field, String value, BytesRef indexedBytes, double boost) {
        this.field = field;
        this.value = value;
        this.indexedBytes = indexedBytes;
        this.boost = boost;
    }

    @Override
    public FunctionValues getValues(Map context, LeafReaderContext readerContext) throws IOException {
        final Terms terms = readerContext.reader()
            .terms(field);
        return new DoubleDocValues(this) {
            DocIdSetIterator docs;
            int atDoc = -1;
            int lastDocRequested = -1;

            { reset(); }

            public void reset() throws IOException {
                if (terms != null) {
                    final TermsEnum termsEnum = terms.iterator();
                    if (termsEnum.seekExact(indexedBytes)) {
                        docs = termsEnum.postings(null);
                    }
                    if (docs == null) {
                        docs = DocIdSetIterator.empty();
                    }
                }
            }

            @Override
            public double doubleVal(int doc) {
                try {

                    if (doc < lastDocRequested) {
                        // out-of-order access.... reset
                        reset();
                    }
                    lastDocRequested = doc;
                    if (atDoc < doc) {
                        atDoc = docs.advance(doc);
                    }
                    if (atDoc > doc) {
                        // term doesn't match this document... either because we hit the
                        // end, or because the next doc is after this doc.
                        return 1;
                    }
                    // a match!
                    return boost;

                } catch (IOException e) {
                    throw new RuntimeException("caught exception in function " + description() + " : doc=" + doc, e);
                }
            }
        };
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) return true;
        if (!(o instanceof FieldValueSource)) {
            return false;
        }
        FieldValueSource other = (FieldValueSource) o;
        return boost == other.boost &&
            Objects.equals(indexedBytes, other.indexedBytes) &&
            Objects.equals(field, other.field);
    }

    @Override
    public int hashCode() {
        return Objects.hash(boost, indexedBytes, field);
    }

    @Override
    public String description() {
        return "fieldvalueboost("+field+","+value+","+boost+")";
    }
}
