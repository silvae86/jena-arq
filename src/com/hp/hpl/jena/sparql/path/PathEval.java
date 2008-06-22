/*
 * (c) Copyright 2008 Hewlett-Packard Development Company, LP
 * All rights reserved.
 * [See end of file]
 */

package com.hp.hpl.jena.sparql.path;

import java.util.*;

import com.hp.hpl.jena.util.iterator.Map1;
import com.hp.hpl.jena.util.iterator.Map1Iterator;
import com.hp.hpl.jena.util.iterator.SingletonIterator;

import com.hp.hpl.jena.graph.Graph;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.Triple;

import com.hp.hpl.jena.sparql.ARQNotImplemented;

public class PathEval
{
    // LinkedHashSet for predictable order - remove later??
    
    static public Iterator eval(Graph g, Iterator input, Path p) 
    {
        Set acc = new LinkedHashSet() ;
        
        for ( ; input.hasNext() ; )
        {
            Node n = (Node)input.next() ;
            PathEvaluator evaluator = new PathEvaluator(g, n, acc) ;
            // Fills "acc" with nodes found.
            p.visit(evaluator) ;
        }
        return acc.iterator() ;
    }
    
    // Solve from object.
    static public Iterator evalReverse(Graph g, Iterator input, Path p) 
    {
        return eval(g, input, new P_Reverse(p)) ;
    }
    
    static class PathEvaluator implements PathVisitor
    {

        private Graph graph ;
        private Node node ;
        private Set output ;

        public PathEvaluator(Graph g, Node n, Set output)
        {
            this.graph = g ; 
            this.node = n ;
            this.output = output ;
        }

        private void fill(Iterator iter)
        {
            for ( ; iter.hasNext() ; )
                output.add(iter.next()) ;
        }
        
       
        
        //@Override
        public void visit(P_Link pathNode)
        {
            Iterator nodes = doOne(pathNode.getNode()) ;
            fill(nodes) ;
        }

        public void visit(P_Reverse reversePath)
        {
            throw new ARQNotImplemented("eval P_Reverse") ;
        }

        private static Map1 selectObject = new Map1()
        {
            //@Override
            public Object map1(Object triple)
            {
                return ((Triple)triple).getObject() ;
            }
        } ;
        
        private final Iterator doOne(Node property)
        {
            Iterator iter = graph.find(node, property, Node.ANY) ;
            Map1Iterator iter2 = new Map1Iterator(selectObject, iter) ;
            return iter2 ;
        }
        
        //@Override
        public void visit(P_Alt pathAlt)
        {
            // Try both sizes, accumulate into output.
            Iterator iter = eval(graph, new SingletonIterator(node), pathAlt.getLeft()) ;
            fill(iter) ;
            iter = eval(graph, new SingletonIterator(node), pathAlt.getRight()) ;
            fill(iter) ;
        }

        //@Override
        public void visit(P_Seq pathSeq)
        {
            // Feed one side into the other
            Iterator iter = eval(graph, new SingletonIterator(node), pathSeq.getLeft()) ;
            iter = eval(graph, iter, pathSeq.getRight()) ;
            // ConcurrentModificationException possible because P_Seq (etc) uses delayed iterators over output.??
            fill(iter) ;
        }

        // If evaluation of "+" and "*" are sufficiently important, maybe have special classes for them.
        //@Override
        public void visit(P_Mod pathMod)
        {
            if ( pathMod.isZeroOrMore() )
            {
                doZeroOrMore(pathMod.getSubPath()) ;
                return ;
            }
            
            if ( pathMod.isOneOrMore() )
            {
                doOneOrMore(pathMod.getSubPath()) ;
                return ;
            }
            
            if ( pathMod.getMin() == 0 )
                output.add(node) ;

            if ( pathMod.getMax() == 0 )
                return ;
            
            // One step.
            Iterator iter = eval(graph, new SingletonIterator(node), pathMod.getSubPath()) ;

            // The next step
            long min2 = dec(pathMod.getMin()) ;
            long max2 = dec(pathMod.getMax()) ;
            P_Mod nextPath = new P_Mod(pathMod.getSubPath(), min2, max2) ;
            
//            // Debug.
//            Listx = Iter.toList(iter) ;
//            System.out.println(x) ;
//            iter = x.iterator() ;
            
            // Move on one step - now go again.
            
            for ( ; iter.hasNext() ; )
            {
                Node n2 = (Node)iter.next() ;
                Iterator iter2 = eval(graph, new SingletonIterator(n2), nextPath) ;
                fill(iter2) ;
            }
            // If no matches, will not call eval and we drop out.
        }
        
        private static long dec(long x) { return (x<=0) ? x : x-1 ; }

        private void doOneOrMore(Path path)
        {
            // Do one, then do zero or more for each result.
            Iterator iter1 = eval(graph, new SingletonIterator(node), path) ;
            Set visited = new LinkedHashSet() ;
            for ( ; iter1.hasNext() ; )
            {
                Node n1 = (Node)iter1.next();
                closure(graph, n1, path, visited) ;
            }
            output.addAll(visited) ;
        }

        private void doZeroOrMore(Path path)
        {
            Set visited = new LinkedHashSet() ;
            //List visited = new ArrayList() ;
            
            closure(graph, node, path, visited) ;
            // result is visited
            output.addAll(visited) ;
        }

        private static void closure(Graph graph, Node n, Path path, Collection visited)
        {
            if ( visited.contains(n) ) return ;
            visited.add(n) ;
            Iterator iter = eval(graph, new SingletonIterator(n), path) ;
            for ( ; iter.hasNext() ; )
            {
                Node n2 = (Node)iter.next() ;
                closure(graph, n2, path, visited) ;
            }
        }
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