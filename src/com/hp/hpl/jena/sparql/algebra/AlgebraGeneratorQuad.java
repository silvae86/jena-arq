/*
 * (c) Copyright 2006, 2007, 2008 Hewlett-Packard Development Company, LP
 * All rights reserved.
 * [See end of file]
 */

package com.hp.hpl.jena.sparql.algebra;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.sparql.algebra.op.OpBGP;
import com.hp.hpl.jena.sparql.algebra.op.OpDatasetNames;
import com.hp.hpl.jena.sparql.algebra.op.OpQuadPattern;
import com.hp.hpl.jena.sparql.core.BasicPattern;
import com.hp.hpl.jena.sparql.core.PathBlock;
import com.hp.hpl.jena.sparql.core.Quad;
import com.hp.hpl.jena.sparql.syntax.ElementGroup;
import com.hp.hpl.jena.sparql.syntax.ElementNamedGraph;
import com.hp.hpl.jena.sparql.util.ALog;
import com.hp.hpl.jena.sparql.util.Context;

public class AlgebraGeneratorQuad extends AlgebraGenerator 
{
    private Node currentGraph = Quad.defaultGraphNode ;
    
    public AlgebraGeneratorQuad(Context context) { super(context) ; }
    public AlgebraGeneratorQuad()                { super() ; }
    
    protected Op compileBasicPattern(BasicPattern pattern)
    {
        return convertToQuad(pattern) ;
    }
    
    protected Op compilePathBlock(PathBlock pathBlock)
    {
        Op op = super.compilePathBlock(pathBlock) ;
        if ( OpBGP.isBGP(op) )
            return convertToQuad(((OpBGP)op).getPattern()) ;
        ALog.warn(this, "Complex path seen - not implemented yet for quads yet") ;
        return op ;
    }

    private Op convertToQuad(BasicPattern pattern)
    {
        return new OpQuadPattern(currentGraph, pattern) ;
    }
    
    //protected Op compilePathBlock(PathBlock pattern)
    
    protected Op compileElementGraph(ElementNamedGraph eltGraph)
    {
        Node graphNode = eltGraph.getGraphNameNode() ;
        Node g = currentGraph ;
        currentGraph = graphNode ;
        
        if ( eltGraph.getElement() instanceof ElementGroup )
        {
            if ( ((ElementGroup)eltGraph.getElement()).isEmpty() )
            {
                // Ths case of ...
                // GRAPH ?g {} or GRAPH <v> {}
                return new OpDatasetNames(graphNode) ;
            }
        }
        Op sub = compileElement(eltGraph.getElement()) ;
        currentGraph = g ;
        return sub ;
    }
}

/*
 * (c) Copyright 2006, 2007, 2008 Hewlett-Packard Development Company, LP
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