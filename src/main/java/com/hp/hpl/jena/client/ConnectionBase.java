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

import java.util.Iterator ;

import com.hp.hpl.jena.graph.Graph ;
import com.hp.hpl.jena.graph.Node ;
import com.hp.hpl.jena.graph.Triple ;
import com.hp.hpl.jena.sparql.core.DatasetGraph ;
import com.hp.hpl.jena.sparql.core.Quad ;

public abstract class ConnectionBase implements DataUpdater, Connection
{

    @Override
    public void add(Node graphName, Triple t)
    {
        add(new Quad(graphName, t));
    }

    @Override
    public void add(Iterable<? extends Quad> quads)
    {
        for (Quad q : quads)
        {
            add(q);
        }
    }

    @Override
    public void add(DatasetGraph dsg)
    {
        for (Iterator<Quad> it = dsg.find(); it.hasNext(); )
        {
            add(it.next());
        }
    }

    @Override
    public void add(Node graphName, Graph g)
    {
        for (Iterator<Triple> it = g.find(null, null, null); it.hasNext(); )
        {
            add(graphName, it.next());
        }
    }

}

