/*
 * (c) Copyright 2008 Hewlett-Packard Development Company, LP
 * All rights reserved.
 * [See end of file]
 */

package com.hp.hpl.jena.sparql.algebra.opt;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import com.hp.hpl.jena.graph.Graph;
import com.hp.hpl.jena.graph.GraphStatisticsHandler;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.sparql.algebra.Op;
import com.hp.hpl.jena.sparql.algebra.TransformCopy;
import com.hp.hpl.jena.sparql.algebra.op.OpBGP;
import com.hp.hpl.jena.sparql.core.BasicPattern;
import com.hp.hpl.jena.sparql.util.VarUtils;

public class TransformReorderBGP extends TransformCopy
{
    // See AFS/src-dev/opt
    private Graph graph ;

    public TransformReorderBGP(Graph graph) { this.graph = graph ; }
    
    public Op transform(OpBGP opBGP)
    {
        BasicPattern pattern = opBGP.getPattern() ;
        BasicPattern pattern2 = rewrite(pattern, graph) ; 
        return new OpBGP(pattern2) ; 
        //return super.transform(opBGP) ;
    }
    
    // Use an extenal configuration approach. 
    public static BasicPattern rewrite(BasicPattern pattern)
    {
        BasicPattern pattern2 = new BasicPattern() ;
        Set patternVarsScope = new HashSet() ;
        
        // Get optimization rules file. 
        // XXX
        
        // Choose order.
        // Not easy at this point as we do not know the graph yet.
        for ( Iterator iter = pattern.getList().listIterator() ; iter.hasNext() ; )
        {
            Triple triple = (Triple)iter.next();
            System.out.println("Process: "+triple) ;
            
            // Vars in scope.
            // Non-graph specific reordering possible.
            VarUtils.addVarsFromTriple(patternVarsScope, triple) ;
            
            // XXX
            // Decide
            pattern2.add(triple) ;
        }
        return pattern2 ;
    }
    
    // Toy
    public static BasicPattern rewrite(BasicPattern pattern, Graph graph)
    {
        BasicPattern pattern2 = new BasicPattern() ;
        Set patternVarsScope = new HashSet() ;
        
        GraphStatisticsHandler handler = graph.getStatisticsHandler() ;
        
        for ( Iterator iter = pattern.getList().listIterator() ; iter.hasNext() ; )
        {
            Triple triple = (Triple)iter.next();
            Node s = node(triple.getSubject()) ;
            Node p = node(triple.getPredicate()) ;
            Node o = node(triple.getObject()) ;
            
            // What about bound variables?
            long w = handler.getStatistic(s,p,o) ;
            
            System.out.println("Process: "+triple) ;
            
            // Vars in scope.
            VarUtils.addVarsFromTriple(patternVarsScope, triple) ;
            
            pattern2.add(triple) ;
        }
        return pattern2 ;
    }
    
    public static Node node(Node n)
    {
        if ( n == null )
            return Node.ANY ;
        if ( n == Node.ANY )
            return Node.ANY ;
        if ( n.isConcrete() )
            return n ;
        return Node.ANY ;
    }
    
    
}
/*
 * (c) Copyright 2008 Hewlett-Packard Development Company, LP
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