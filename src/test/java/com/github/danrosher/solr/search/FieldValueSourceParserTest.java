package com.github.danrosher.solr.search;

import org.apache.solr.SolrTestCaseJ4;
import org.junit.BeforeClass;
import org.junit.Test;

//Copyright (c) 2021, Dan Rosher
//    All rights reserved.
//
//    This source code is licensed under the BSD-style license found in the
//    LICENSE file in the root directory of this source tree.


public class FieldValueSourceParserTest  extends SolrTestCaseJ4  {

    @BeforeClass
    public static void beforeClass() throws Exception {
        System.setProperty("enable.update.log", "false"); // schema12 doesn't support _version_
        initCore("solrconfig.xml", "schema12.xml");
    }

    @Test
    public void testFieldValueSource() throws Exception {
        assertU(adoc("id", "0", "features", "f1","features","f2"));
        assertU(adoc("id", "1", "features","f2"));

        assertU(commit());
        assertJQ(req(
            "df","id",
            "b","mul(fvboost(features,\"f1\",2),fvboost(features,\"f2\",3),fvboost(features,\"f3\",5))",
            "q", "{!boost b=$b v=$qq}",
            "qq","{!edismax }*:*",
            "sort","score desc",
            "fl", "*,score,f1:fvboost(features,\"f1\",2),f3:fvboost(features,\"f3\",2),f1f2:$b"),

            "/response/docs/[0]/features== ['f1','f2']",
            "/response/docs/[0]/f1f2==6.0",
            "/response/docs/[0]/score==6.0",

            "/response/docs/[1]/features== ['f2']",
            "/response/docs/[1]/f1f2==3.0",
            "/response/docs/[1]/score==3.0"
            );
    }




}