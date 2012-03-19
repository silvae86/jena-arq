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

import java.io.InputStream ;

import org.apache.jena.iri.IRI ;

import com.hp.hpl.jena.graph.Graph ;
import com.hp.hpl.jena.graph.Node ;
import com.hp.hpl.jena.graph.Triple ;
import com.hp.hpl.jena.sparql.core.DatasetGraph ;
import com.hp.hpl.jena.sparql.core.Quad ;

public interface DataUpdater
{
    // Quads
    void add(Quad q);
    void add(Iterable<? extends Quad> quads);
    void add(DatasetGraph dsg);
    void add(InputStream in, RDFFormat lang);  // Lang must be NQUADS or TRIG
    void add(InputStream in, String base, RDFFormat lang);  // Lang must be NQUADS or TRIG
    
    void delete(Quad q);
    void delete(Iterable<? extends Quad> quads);
    void delete(DatasetGraph dsg);
    void delete(InputStream in, RDFFormat lang);  // Lang must be NQUADS or TRIG
    void delete(InputStream in, String base, RDFFormat lang);  // Lang must be NQUADS or TRIG
    
    // Triples
    void add(Node graphName, Triple t);
    void add(Node graphName, Iterable<? extends Triple> triples);
    void add(Node graphName, Graph g);
    void add(Node graphName, InputStream in, RDFFormat lang);
    void add(Node graphName, InputStream in, String base, RDFFormat lang);
    void add(Node graphName, IRI sourceDocument);
    
    void put(Node graphName, Triple t);
    void put(Node graphName, Iterable<? extends Triple> triples);
    void put(Node graphName, Graph g);
    void put(Node graphName, InputStream in, RDFFormat lang);
    void put(Node graphName, InputStream in, String base, RDFFormat lang);
    
    void delete(Node graphName, Triple t);
    void delete(Node graphName, Iterable<? extends Triple> triples);
    void delete(Node graphName, Graph g);
    void delete(Node graphName, InputStream in, RDFFormat lang);
    void delete(Node graphName, InputStream in, String base, RDFFormat lang);
    void clear(Node graphName);
    
    void create(Node graphName);
    void drop(Node graphName);
    void copy(Node sourceGraphName, Node destGraphName);
    void move(Node sourceGraphName, Node destGraphName);
    void add(Node sourceGraphName, Node destGraphName);
}

