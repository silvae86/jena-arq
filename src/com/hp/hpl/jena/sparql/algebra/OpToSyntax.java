/*
 * (c) Copyright 2007 Hewlett-Packard Development Company, LP
 * All rights reserved.
 * [See end of file]
 */

package com.hp.hpl.jena.sparql.algebra;

import java.util.Iterator;
import java.util.List;
import java.util.Stack;

import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.SortCondition;
import com.hp.hpl.jena.sparql.ARQNotImplemented;
import com.hp.hpl.jena.sparql.algebra.op.*;
import com.hp.hpl.jena.sparql.core.BasicPattern;
import com.hp.hpl.jena.sparql.core.Var;
import com.hp.hpl.jena.sparql.expr.Expr;
import com.hp.hpl.jena.sparql.expr.ExprList;
import com.hp.hpl.jena.sparql.syntax.Element;
import com.hp.hpl.jena.sparql.syntax.ElementFilter;
import com.hp.hpl.jena.sparql.syntax.ElementGroup;
import com.hp.hpl.jena.sparql.syntax.ElementNamedGraph;
import com.hp.hpl.jena.sparql.syntax.ElementOptional;
import com.hp.hpl.jena.sparql.syntax.ElementTriplesBlock;
import com.hp.hpl.jena.sparql.syntax.ElementUnion;

/** Convert an Op expression in SPARQL syntax, that is, the reverse of algebra generation */   
public class OpToSyntax
{
//    public static void main(String[] argv)
//    {
//        Op op1 = SSE.parseOp("(filter ?x (filter ?o (bgp (?s ?p _:a)) ) )") ;
//        // Not canonicalized: two nested filters.
//        Op op2 = SSE.parseOp("(filter (&& ?o ?x) (bgp (?s ?p _:a)) )") ;
//        // Not canonicalized: Expression list, length 1, (&& ?o ?x)
//        
//        // Canonical:one filter, Expression list, length 2, ?o ?x
//        
//        if ( ! op1.equals(op2) )
//        {
//            System.out.println("**** Different") ;
//            System.out.print(op1) ;
//            System.out.print(op2) ;
//            System.out.println("------------") ;
//        }
//        else
//            System.out.println("Same") ;
//        
//        // Non-canonical: { { .. FILTER } FILTER }
//        System.out.print(asQuery(op1)) ;
//        System.out.print(asQuery(op2)) ;
//
//        System.out.println("------------") ;
//        System.exit(0) ;
//        
//        String [] a1 = new String[]{
//            "PREFIX : <http://example/>",
//            "SELECT ?o",
//            "{ ?s ?p ?o . }"
//        } ;
//
//        String [] a2 = new String[]{
//            "PREFIX : <http://example/>",
//            "SELECT *",
//            "{ ?s ?p ?o . }"
//        } ;
//        
//        String [] a3 = new String[]{
//            "PREFIX : <http://example/>",
//            "SELECT *",
//            "{ ?s ?p ?o . FILTER(?o) FILTER(?z) }"
//        } ;
//
//        String [] a5 = new String[]{
//            "PREFIX : <http://example/>",
//            "SELECT ?s",
//            "{ ?s ?p ?o . ?s ?p ?o2 OPTIONAL { ?s1 ?p1 ?o1} }"
//        } ;
//
//        String [] a6 = new String[]{
//            "PREFIX : <http://example/>",
//            "SELECT ?s1 ?p2",
//            "{ ",
//            "  { ?s1 ?p1 ?o1 } UNION { ?s2 ?p2 ?o2 } ",
//            "  ?a ?b ?c",
//            "}"
//        } ;
//        
//        String [] a7 = new String[]{
//            "PREFIX : <http://example/>",
//            "SELECT ?s1 ?p2",
//            "{ { ?s1 ?p1 ?o1 } UNION { ?s2 ?p2 ?o2 } }" //UNION { ?s3 ?p3 ?o3 } }"
//        } ;
//
//        String [] a8 = new String[]{
//            "PREFIX : <http://example/>",
//            "SELECT ?s1 ?p2",
//            "{ { ?s1 ?p1 ?o1 } UNION { ?s2 ?p2 ?o2 } UNION { ?s3 ?p3 ?o3 } }"
//        } ;
//
//
//        String [] a9 = new String[]{
//            "PREFIX : <http://example/>",
//            "SELECT ?x",
//            " { {?s ?p ?o }",
//            "   UNION {",
//            "     ?s ?p ?o . ",
//            "     FILTER(?o)",
//            "     OPTIONAL{?s :p :z }",
//            "   } UNION { ?a ?b ?c }",
//            "}"
//        } ;
//
//        // Graph
////        one(a1, false) ;
////        one(a2, false) ;
////        one(a3, false) ;
////        one(a5, false) ;
////        one(a6, false) ;
////        one(a7, false) ;
////        one(a8, false) ;
////        one(a9, false) ;
////        System.out.println("====") ;
////        System.exit(0) ;
//        
//
//        checkOp("(join (bgp (?s ?p _:a)) (bgp (?s ?p ?o)) )") ;
//    }
//
//    public static void checkOp(String str)
//    {
//        check(SSE.parseOp(str)) ;
//    }
//    
//    public static void check(Op op)
//    {
//        Query query = asQuery(op) ;
//        Op op2 = Algebra.compile(query) ;
//        
//        if ( ! op.equals(op2) )
//        {
//            System.out.println("**** Different algebra expressions after converting to syntax and back") ;
//            System.out.print(op) ;
//            System.out.print(query) ;
//            System.out.print(op2) ;
//            System.out.println("------------") ;
//        }
//    }
//    
//    public static void one(String[] a, boolean verbose)
//    {
//        String qs = StringUtils.join("\n", a) ;
//        
//        Query query = QueryFactory.create(qs) ;
//        
//        Op op = Algebra.compile(query) ;
//        
//        // Check the query.
//        if ( verbose )
//        {
//            System.out.print(op) ;
//            System.out.println() ;
//        }
//        Query query2 = asQuery(op) ;
//        if ( verbose )
//        {
//            System.out.print(query2) ;
//            System.out.println() ;
//        }
//        Op op2 = Algebra.compile(query2) ;
//        if ( verbose )
//            System.out.print(op2) ;
//        
//        if ( ! op.equals(op2) )
//        {
//            System.out.println("**** query algebra expression not equal to it's serialized/parsed form") ;
//            System.out.print(op) ;
//            System.out.print(op2) ;
//            System.out.println("------------") ;
//        }
//        else if ( verbose )
//            System.out.println("------------") ;
//        
//        
//        // Check the Op.
//        check(op) ;
//        
//
//    }
    
    public static Query asQuery(Op op)
    {
        Query query = QueryFactory.make() ;
        query.setQueryResultStar(true) ;
        
        Converter v = new Converter(query) ;
        //OpWalker.walk(op, v) ;
        op.visit(v) ;
        
        query.setQueryPattern(v.currentGroup) ;
        query.setQuerySelectType() ;
        
        query.setResultVars() ; 
        // Always name the variables, no "SELECT *"
        //query.setQueryResultStar(false) ;
        return query ; 
    }
    
    static class Converter implements OpVisitor
    {
        Query query ;
        Element element = null ;
        ElementGroup currentGroup = null ;
        Stack stack = new Stack() ;
        public Converter(Query query)
        {
            this.query = query ;
            currentGroup = new ElementGroup() ;
        }

        Element asElement(Op op)
        {
            ElementGroup g = asElementGroup(op) ;
            if ( g.getElements().size() == 1 )
                return (Element)g.getElements().get(0) ;
            return g ;
        }
        
        ElementGroup asElementGroup(Op op)
        {
            startSubGroup() ;
            op.visit(this) ;
            return endSubGroup() ;
        }

        public void visit(OpBGP opBGP)
        {
            currentGroup().addElement(process(opBGP.getPattern())) ;
        }

        private ElementTriplesBlock process(BasicPattern pattern)
        {
            ElementTriplesBlock e = new ElementTriplesBlock() ;
            Iterator iter = pattern.iterator() ;
            for ( ; iter.hasNext() ; )
            {
                Triple t = (Triple)iter.next() ;
                // Leave bNode variables as they are
                // Queruy serialization will deal with them. 
                e.addTriple(t) ;
            }
            return e ;
        }
        
        public void visit(OpQuadPattern quadPattern)
        { throw new ARQNotImplemented("OpQuadPattern") ; }

        public void visit(OpJoin opJoin)
        {
            // XXX Amalgamate adjacent BGPs? No bnodes variables by now.
            
            // start/stop subgroups - no - joins are linear groups.
            // Have option to explicitly {} sub elements.
            
            // Keep things clearly separated.
            Element eLeft = asElement(opJoin.getLeft()) ;
            Element eRight = asElementGroup(opJoin.getRight()) ;
            
//            // Allow adjacent BGPs to merge
//            Element eLeft = asElement(opJoin.getLeft()) ;
//            Element eRight = asElement(opJoin.getRight()) ;
            
            
            ElementGroup g = currentGroup() ;
            g.addElement(eLeft) ;
            g.addElement(eRight) ;
            return ;
        }

        public void visit(OpLeftJoin opLeftJoin)
        {
            Element eLeft = asElement(opLeftJoin.getLeft()) ;
            Element eRight = asElementGroup(opLeftJoin.getRight()) ;
            ElementGroup g = currentGroup() ;
            g.addElement(eLeft) ;
            ElementOptional opt = new ElementOptional(eRight) ;
            g.addElement(opt) ;
        }

        public void visit(OpDiff opDiff)
        { throw new ARQNotImplemented("OpDiff") ; }

        public void visit(OpUnion opUnion)
        {
            Element eLeft = asElementGroup(opUnion.getLeft()) ;
            Element eRight = asElementGroup(opUnion.getRight()) ;
            if ( eLeft instanceof ElementUnion )
            {
                ElementUnion elUnion = (ElementUnion)eLeft ;
                elUnion.addElement(eRight) ;
                return ;
            }
            
//            if ( eRight instanceof ElementUnion )
//            {
//                ElementUnion elUnion = (ElementUnion)eRight ;
//                elUnion.getElements().add(0, eLeft) ;
//                return ;
//            }
            
            ElementUnion elUnion = new ElementUnion() ;
            elUnion.addElement(eLeft) ;
            elUnion.addElement(eRight) ;
            currentGroup().addElement(elUnion) ;
        }

        public void visit(OpFilter opFilter)
        {
            // XXX
            // (filter .. (filter ( ... ))   (non-canonicalizing OpFilters)
            // Inner gets Grouped unnecessarily. 
            Element e = asElement(opFilter.getSubOp()) ;
            if ( currentGroup() != e )
                currentGroup().addElement(e) ;
            element = currentGroup() ;      // Was cleared by asElement. 
            
            ExprList exprs = opFilter.getExprs() ;
            for ( Iterator iter = exprs.iterator() ; iter.hasNext(); )
            {
                Expr expr = (Expr)iter.next();
                ElementFilter f = new ElementFilter(expr) ;
                currentGroup().addElement(f) ;
            }
        }

        public void visit(OpGraph opGraph)
        {
            startSubGroup() ;
            Element e = asElement(opGraph.getSubOp()) ;
            ElementGroup g = endSubGroup() ;
            
            Element graphElt = new ElementNamedGraph(opGraph.getNode(), e) ;
            currentGroup().addElement(graphElt) ;
        }

        public void visit(OpService opService)
        { throw new ARQNotImplemented("OpService") ; }

        public void visit(OpDatasetNames dsNames)
        { throw new ARQNotImplemented("OpDatasetNames") ; }

        public void visit(OpTable opTable)
        { throw new ARQNotImplemented("OpTable") ; }

        public void visit(OpExt opExt)
        { throw new ARQNotImplemented("OpExt") ; }

        public void visit(OpNull opNull)
        { throw new ARQNotImplemented("OpNull") ; }

        public void visit(OpList opList)
        { /* No action */ }

        public void visit(OpOrder opOrder)
        {
            List x = opOrder.getConditions() ;
            Iterator iter = x.iterator() ;
            for ( ; iter.hasNext(); )
            {
                SortCondition sc = (SortCondition)iter.next();
                query.addOrderBy(sc);
            }
        }

        public void visit(OpProject opProject)
        {
            query.setQueryResultStar(false) ;
            Iterator iter = opProject.getVars().iterator() ;
            for ( ; iter.hasNext() ; )
            {
                Var v = (Var)iter.next();
                query.addResultVar(v) ;
            }
            opProject.getSubOp().visit(this) ;
        }

        public void visit(OpReduced opReduced)
        { query.setReduced(true) ; }

        public void visit(OpDistinct opDistinct)
        { query.setDistinct(true) ; }

        public void visit(OpSlice opSlice)
        {
            if ( opSlice.getStart() != Query.NOLIMIT )
                query.setOffset(opSlice.getStart()) ;
            if ( opSlice.getLength() != Query.NOLIMIT )
                query.setLimit(opSlice.getLength()) ;
        }

        public void visit(OpGroupAgg opGroupAgg)
        { throw new ARQNotImplemented("OpGroupAgg") ; }
        
        private Element lastElement()
        {
            ElementGroup g = currentGroup ;
            if ( g == null || g.getElements().size() == 0 )
                return null ;
            int len = g.getElements().size() ;
            return (Element)g.getElements().get(len-1) ;
        }

        private void startSubGroup()
        {
            push(currentGroup) ;
            ElementGroup g = new ElementGroup() ;
            currentGroup = g ;
        }
        
        private ElementGroup endSubGroup()
        {
            ElementGroup g = pop() ;
            ElementGroup r = currentGroup ;
            currentGroup = g ;
            return r ;
        }
        
//        private void endCurrentGroup()
//        {
//            currentGroup = null ;
//            element = null ; //??
//        }
        
        private ElementGroup currentGroup()
        {
//            if ( currentGroup == null )
//                startSubGroup() ;
            return currentGroup ;
        }
        
        private ElementGroup peek()
        {
            if ( stack.size() == 0 )
                return null ;
            return (ElementGroup)stack.peek();
        }
        private ElementGroup pop() { return (ElementGroup)stack.pop(); }
        private void push(ElementGroup el) { stack.push(el); }
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