/*
 * (c) Copyright 2007 Hewlett-Packard Development Company, LP
 * All rights reserved.
 * [See end of file]
 */

package com.hp.hpl.jena.sparql.algebra;

import com.hp.hpl.jena.graph.Graph;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.sparql.core.DataSourceGraphImpl;
import com.hp.hpl.jena.sparql.core.DatasetGraph;
import com.hp.hpl.jena.sparql.engine.QueryIterator;
import com.hp.hpl.jena.sparql.engine.main.OpExecMain;
import com.hp.hpl.jena.sparql.engine.ref.OpExecRef;

import com.hp.hpl.jena.query.ARQ;
import com.hp.hpl.jena.query.Dataset;

public class Algebra
{

    // Execute!

    static public QueryIterator exec(Op op, Dataset ds)
    {
        return exec(op, new DataSourceGraphImpl(ds)) ;
    }

    static public QueryIterator exec(Op op, Model model)
    {
        return exec(op, model.getGraph()) ;
    }

    static public QueryIterator exec(Op op, DatasetGraph ds)
    {
        // QueryEngineRef.eval
        QueryIterator qIter = new OpExecMain().eval(op, ds, ARQ.getContext()) ;
        return qIter ;
    }

    static public QueryIterator exec(Op op, Graph graph)
    {
        QueryIterator qIter = new OpExecMain().eval(op, graph) ;
        return qIter ;
    }

    //  Reference engine
    //  Should we do the registery thing here?
    //  Extends QueryExecutionGraph or a separate interface for exec(op)? 

    static public QueryIterator execRef(Op op, Dataset ds)
    {
        return execRef(op, new DataSourceGraphImpl(ds)) ;
    }

    static public QueryIterator execRef(Op op, Model model)
    {
        return execRef(op, model.getGraph()) ;
    }

    static public QueryIterator execRef(Op op, DatasetGraph ds)
    {
        // QueryEngineRef.eval
        QueryIterator qIter = new OpExecRef().eval(op, ds, ARQ.getContext()) ;
        return qIter ;
    }

    static public QueryIterator execRef(Op op, Graph graph)
    {
        QueryIterator qIter = new OpExecRef().eval(op, graph) ;
        return qIter ;
    }
}
/*
 * (c) Copyright 2007 Hewlett-Packard Development Company, LP
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. The name of the author may not be used to endorse or promote products
 *    derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 * THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */