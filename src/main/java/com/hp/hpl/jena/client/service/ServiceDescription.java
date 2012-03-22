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

package com.hp.hpl.jena.client.service;

import com.hp.hpl.jena.graph.Graph;

/**
 * Information about the database.
 * 
 * @see <a href="http://www.w3.org/TR/sparql11-service-description/">SPARQL 1.1 Service Description</a>
 */
public interface ServiceDescription
{
    /**
     * Returns this service description as a Graph.
     * @see <a href="http://www.w3.org/TR/sparql11-service-description/">SPARQL 1.1 Service Description</a>
     */
    Graph asGraph();
    
    
    
    
    // -- Convenience methods ------------------------------
    
    //Collection<Syntax> supportedLanguages();
    
    
    
    
    // -- Custom Jena properties below ---------------------
    
    
    
    
}

