/**
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

package com.hp.hpl.jena.client;

import java.io.InputStream;

import com.hp.hpl.jena.graph.Graph;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.sparql.core.DatasetGraph;
import com.hp.hpl.jena.sparql.core.Quad;

public interface Deleter
{
    // Quads
    void delete(Quad q);
    void delete(Iterable<? extends Quad> quads);
    void delete(DatasetGraph dsg);
    void delete(InputStream in, RDFFormat lang);  // Lang must be NQUADS or TRIG
    void delete(InputStream in, String base, RDFFormat lang);  // Lang must be NQUADS or TRIG
    
    // Triples
    void delete(Node graphName, Triple t);
    void delete(Node graphName, Iterable<? extends Triple> triples);
    void delete(Node graphName, Graph g);
    void delete(Node graphName, InputStream in, RDFFormat lang);
    void delete(Node graphName, InputStream in, String base, RDFFormat lang);
}

