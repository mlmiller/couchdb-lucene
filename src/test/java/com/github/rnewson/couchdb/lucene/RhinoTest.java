package com.github.rnewson.couchdb.lucene;

/**
 * Copyright 2009 Robert Newson
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); 
 * you may not use this file except in compliance with the License. 
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0 
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import static org.junit.Assert.assertThat;

import org.apache.lucene.document.Document;
import org.hamcrest.CoreMatchers;
import org.junit.After;
import org.junit.Test;
import org.mozilla.javascript.EcmaError;

public class RhinoTest {

    private Rhino rhino;

    @After
    public void cleanup() {
        if (rhino != null) {
            rhino.close();
        }
    }

    @Test
    public void testRhino() throws Exception {
        rhino = new Rhino("db,", "function(doc) { var ret = new Document(); "
                + "ret.add(doc.size, {\"field\":\"foo\"}); return ret; }");
        final String doc = "{\"deleteme\":\"true\", \"size\":13}";
        Document[] ret = rhino.map("doc", doc);
        assertThat(ret.length, CoreMatchers.equalTo(1));
        assertThat(ret[0].getField("foo"), CoreMatchers.notNullValue());
    }

    @Test
    public void testNoReturn() throws Exception {
        rhino = new Rhino("db", "function(doc) {}");
        Document[] ret = rhino.map("doc", "{}");
        assertThat(ret.length, CoreMatchers.equalTo(0));
    }

    @Test(expected = EcmaError.class)
    public void testBadCode() throws Exception {
        rhino = new Rhino("db", "function(doc) {no_such_function(); }");
        rhino.map("doc", "{}");
    }

    @Test
    public void testBadCodeRecovers() throws Exception {
        try {
            testBadCode();
        } catch (EcmaError e) {
            // Ignored.
        }
        testRhino();
    }

    @Test(expected = RuntimeException.class)
    public void testBadReturn() throws Exception {
        rhino = new Rhino("db", "function(doc) {return 1;}");
        rhino.map("doc", "{}");
    }

    @Test
    public void testCtor() throws Exception {
        rhino = new Rhino("db", "function(doc) { return new Document(1, {\"field\":\"foo\"}); }");
        Document[] ret = rhino.map("doc", "{}");
        assertThat(ret.length, CoreMatchers.equalTo(1));
        assertThat(ret[0].getField("foo"), CoreMatchers.notNullValue());
    }

    @Test
    public void testMultipleReturn() throws Exception {
        rhino = new Rhino("db", "function(doc) { " + "var ret = []; "
                + "for(var v in doc) {var d = new Document(); d.add(doc[v], {\"field\": v}); ret.push(d)} " + "return ret; " + "}");
        Document[] ret = rhino.map("doc", "{\"foo\": 1, \"bar\": 2}");
        assertThat(ret.length, CoreMatchers.equalTo(2));
    }

    @Test
    public void testDate() throws Exception {
        rhino = new Rhino("db", "function(doc) { var ret = new Document(); "
                + "ret.add(new Date(), {\"field\":\"bar\"}); return ret;}");
        Document[] ret = rhino.map("doc", "{\"foo\": 1, \"bar\": 2}");
        assertThat(ret.length, CoreMatchers.equalTo(1));
        assertThat(ret[0].getField("bar"), CoreMatchers.notNullValue());
    }

}
