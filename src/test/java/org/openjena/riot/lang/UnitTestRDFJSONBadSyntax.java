/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.openjena.riot.lang;

import java.io.InputStream ;

import junit.framework.TestCase ;
import org.openjena.atlas.io.IO ;
import org.openjena.atlas.lib.SinkNull ;
import org.openjena.riot.RiotException ;
import org.openjena.riot.RiotReader ;

import com.hp.hpl.jena.graph.Triple ;


public class UnitTestRDFJSONBadSyntax extends TestCase
{
    String uri ;
    public UnitTestRDFJSONBadSyntax(String name, String uri) { super(name) ; this.uri = uri ; }
    
    @Override
    public void runTest()
    {
        InputStream in = IO.openFile(uri) ;
        assertNotNull(in) ;
        LangRIOT parser = RiotReader.createParserRdfJson(in, new SinkNull<Triple>()) ;
        try {
            parser.parse() ;
        } catch (RiotException ex) { return ; }
        fail("Bad syntax RDF/JSON test succeed in parsing the file") ;
    }
}
