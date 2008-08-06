/*
 * (c) Copyright 2007, 2008 Hewlett-Packard Development Company, LP
 * All rights reserved.
 * [See end of file]
 */

package com.hp.hpl.jena.sparql.engine.main;

import java.util.*;

import com.hp.hpl.jena.graph.Triple;

import com.hp.hpl.jena.sparql.algebra.Op;
import com.hp.hpl.jena.sparql.algebra.OpVars;
import com.hp.hpl.jena.sparql.algebra.op.OpBGP;
import com.hp.hpl.jena.sparql.algebra.op.OpJoin;
import com.hp.hpl.jena.sparql.algebra.op.OpProcedure;
import com.hp.hpl.jena.sparql.algebra.op.OpSequence;
import com.hp.hpl.jena.sparql.core.BasicPattern;
import com.hp.hpl.jena.sparql.engine.ExecutionContext;
import com.hp.hpl.jena.sparql.engine.QueryIterator;
import com.hp.hpl.jena.sparql.engine.iterator.QueryIterFilterExpr;
import com.hp.hpl.jena.sparql.expr.Expr;
import com.hp.hpl.jena.sparql.expr.ExprList;
import com.hp.hpl.jena.sparql.util.VarUtils;

import com.hp.hpl.jena.query.ARQ;

public class FilterPlacement
{
    // Broken out to keep OpCompiler more manageable.
    private OpCompiler compiler ;
    private ExecutionContext execCxt ;
    
    boolean doFilterPlacement = false ;

    public FilterPlacement(OpCompiler compiler, ExecutionContext execCxt)
    {
        this.compiler = compiler ;
        this.execCxt = execCxt ;
        doFilterPlacement = execCxt.getContext().isTrueOrUndef(ARQ.filterPlacement) ;
    }
    
    // --------------------------------
    // Basic Graph Patterns
    
    public QueryIterator X_placeFiltersBGP(ExprList exprs, BasicPattern pattern, QueryIterator input)
    {
        if ( ! doFilterPlacement )
        {
            QueryIterator qIter = StageBuilder.compile(pattern, input, execCxt) ;
            return buildFilter(exprs, qIter) ;
        }
        
        // Destructive use of exprs - copy it.
        exprs = new ExprList(exprs) ;
        Set patternVarsScope = new HashSet() ;
        BasicPattern accPattern = new BasicPattern() ;
        
        QueryIterator qIter = insertAnyFilter(exprs, patternVarsScope, accPattern, input) ;
        if ( qIter == null )
            qIter = input ;
        
        qIter = placeFilters(exprs, pattern, patternVarsScope, qIter) ;
        // any remaining filters
        qIter = buildFilter(exprs, qIter) ;
        return qIter ;
    }

    private QueryIterator placeFilters(ExprList exprs, BasicPattern pattern, Set varScope, QueryIterator input)
    {
        BasicPattern accPattern = new BasicPattern() ;
        //Set patternVarsScope = new HashSet() ;
        QueryIterator qIter = input ;
        
        qIter = insertAnyFilter(exprs, varScope, accPattern, qIter) ;
        if ( qIter == null )
            qIter = input ;
        
        for ( Iterator iter = pattern.iterator() ; iter.hasNext() ; )
        {
            Triple triple = (Triple)iter.next() ;
            
            accPattern.add(triple) ;
            VarUtils.addVarsFromTriple(varScope, triple) ;
            
            QueryIterator qIter2 = insertAnyFilter(exprs, varScope, accPattern, qIter) ;
            if ( qIter2 != null )
            {
                accPattern = new BasicPattern() ;
                qIter = qIter2 ;
            }
        }
        
        // Remaining triples.
        qIter = StageBuilder.compile(accPattern, qIter, execCxt) ;
        return qIter ;
    }
    
    private QueryIterator insertAnyFilter(ExprList exprs, Set patternVarsScope, BasicPattern accPattern, QueryIterator qIter)
    {
        boolean doneSomething = false ;
        for ( Iterator iter = exprs.iterator() ; iter.hasNext() ; )
        {
            Expr expr = (Expr)iter.next() ;
            // Cache
            Set exprVars = expr.getVarsMentioned() ;
            if ( patternVarsScope.containsAll(exprVars) )
            {
                QueryIterator qIter2 = buildPatternFilter(expr, accPattern, qIter) ;
                iter.remove() ;
                qIter = qIter2 ;
                doneSomething = true ;
            }
        }
        
        return ( doneSomething ? qIter : null ) ;
    }

    // --------------------------------
    // Placement in stages and joins.
    // Joins may, in turn, involve stages and BGPs.
    
    public QueryIterator X_placeFiltersStage(ExprList exprs, OpSequence opSequence, QueryIterator input)
    {
        if ( ! doFilterPlacement )
            return buildOpFilter(exprs, opSequence, input) ;
        Set varScope = new HashSet() ;
        QueryIterator qIter = placeFilters(exprs, opSequence, varScope, input) ;
        // Insert any remaining filter expressions regardless.
        qIter = buildFilter(exprs, qIter) ;
        return qIter ;
    }
    
    private QueryIterator placeFilters(ExprList exprs, OpSequence opSequence, Set varScope, QueryIterator input)
    {
        List ops = stages(opSequence) ;
        return placeFilters(exprs, ops, varScope, input) ;
    }

    public QueryIterator X_placeFiltersJoin(ExprList exprs, OpJoin opJoin, QueryIterator input)
    {
        if ( ! doFilterPlacement )
            return buildOpFilter(exprs, opJoin, input) ;
        Set varScope = new HashSet() ;
        QueryIterator qIter = placeFilters(exprs, opJoin, varScope, input) ;
        // Insert any remaining filter expressions regardless.
        qIter = buildFilter(exprs, qIter) ;
        return qIter ;
    }
    
    private QueryIterator placeFilters(ExprList exprs, OpJoin opJoin, Set varScope, QueryIterator input)
    {
        // Look for a join chain
        List ops = joins(opJoin) ;
        return placeFilters(exprs, ops, varScope, input) ;
    }
     
    // Err - not sure this is useful.
    public QueryIterator X_placeFiltersProcedure(ExprList exprs, OpProcedure opProc, QueryIterator input)
    {
        if ( ! doFilterPlacement )
            return buildOpFilter(exprs, opProc, input) ;
        Set varScope = new HashSet() ;
        QueryIterator qIter = placeFilters(exprs, opProc, varScope, input) ;
        // Insert any remaining filter expressions regardless.
        qIter = buildFilter(exprs, qIter) ;
        return qIter ;
    }
    
    private QueryIterator placeFilters(ExprList exprs, OpProcedure opProc, Set varScope, QueryIterator input)
    {
        return compiler.compileOp(opProc, input) ;
    }
    
    // --------------------------------
    
    // Placement for filters in a list of ops where the filter can be placed solely
    // on var definedness (so Join, Sequence).
    
    private QueryIterator placeFilters(ExprList exprs, List ops, Set varScope, QueryIterator input)
    { 
        //Set varScope = new HashSet() ;
        QueryIterator qIter = input ;
        
        qIter = insertAnyFilter(exprs, varScope, qIter) ;
        if ( qIter == null )
            qIter = input ;
        
        for ( Iterator iter = ops.iterator() ; iter.hasNext() ; )
        {
            Op op = (Op)iter.next() ;
            
            // And push into any BGPs or OpSequence if possible.
            if ( op instanceof OpBGP )
            {
                OpBGP bgp = (OpBGP)op ;
                BasicPattern pattern = bgp.getPattern() ;
                qIter = placeFilters(exprs, pattern, varScope, qIter) ;
            }
            else if ( op instanceof OpSequence )
                qIter = placeFilters(exprs, (OpSequence)op, varScope, qIter) ;
            else if ( op instanceof OpJoin )
                qIter = placeFilters(exprs, (OpJoin)op, varScope, qIter) ;
            else if ( op instanceof OpProcedure )
                qIter = placeFilters(exprs, (OpProcedure)op, varScope, qIter) ;
            else
                // Not something we can do anything about.
                // Compile without being clever with filter placement.
                qIter = compiler.compileOp(op, qIter) ;
            
            OpVars.patternVars(op, varScope) ;
            qIter = insertAnyFilter(exprs, varScope, qIter) ;
        }
        return qIter ;
    }
    
    // A chain of joins A join B join C becomes (join (join A B) C)
    // This code flattens join trees.
    
    // --------------------------------
    // Stages

    // Flattens OpSequence trees.
    // (Which are usually left-nested lists).
    // See joins.  Mutter, mutter.

    private static List stages(OpSequence base)
    {
        return base.getElements() ;
    }
    
//    private static List stages(OpSequence base)
//    {
//        List stages = new ArrayList() ;
//        stages(base, stages) ;
//        return stages ;
//    }
//
//    private static void stages(Op base, List stages)
//    {
//        while ( base instanceof OpSequence )
//        {
//            OpSequence join = (OpSequence)base ;
//            Op left = join.getLeft() ; 
//            stages(left, stages) ;
//            base = join.getRight() ;
//        }
//        // Not a stage - add it.
//        stages.add(base) ;
//    }

    private static List joins(OpJoin base)
    {
        List joinElts = new ArrayList() ;
        joins(base, joinElts) ;
        return joinElts ;
   }
    
    private static void joins(Op base, List joinElts)
    {
        while ( base instanceof OpJoin )
        {
            OpJoin join = (OpJoin)base ;
            Op left = join.getLeft() ; 
            joins(left, joinElts) ;
            base = join.getRight() ;
        }
        // Not a join - add it.
        joinElts.add(base) ;
    }

    private QueryIterator insertAnyFilter(ExprList exprs, Set varScope, QueryIterator qIter)
    {
        for ( Iterator iter = exprs.iterator() ; iter.hasNext() ; )
        {
            Expr expr = (Expr)iter.next() ;
            // Cache
            Set exprVars = expr.getVarsMentioned() ;
            // Insert filter if satisified.
            if ( varScope.containsAll(exprVars) )
            {
                qIter = new QueryIterFilterExpr(qIter, expr, execCxt) ;
                iter.remove() ;
            }
        }
        return qIter ;
    }

    // ----------------
    // Build a series of filters around a op which is compiled
    public QueryIterator buildOpFilter(ExprList exprs, Op sub, QueryIterator input)
    {
        QueryIterator qIter = compiler.compileOp(sub, input) ;

        for ( Iterator iter = exprs.iterator() ; iter.hasNext(); )
        {
            Expr expr = (Expr)iter.next() ;
            qIter = new QueryIterFilterExpr(qIter, expr, execCxt) ;
        }
        return qIter ;
    }
    
    private QueryIterator buildOpFilter(Expr expr, Op op, QueryIterator input)
    {
        QueryIterator qIter = compiler.compileOp(op, input) ;
        qIter = new QueryIterFilterExpr(qIter, expr, execCxt) ;
        return qIter ;
    }

    // Build a series of filters around a BasicPattern
    private QueryIterator buildPatternFilter(Expr expr, BasicPattern pattern, QueryIterator input)
    {
        QueryIterator qIter = StageBuilder.compile(pattern, input, execCxt) ;
        qIter = new QueryIterFilterExpr(qIter, expr, execCxt) ;
        return qIter ;
    }

    // Insert filters around a query iterator.
    private QueryIterator buildFilter(ExprList exprs, QueryIterator qIter)
    {
        if ( exprs.isEmpty() )
            return qIter ;
    
        for ( Iterator iter = exprs.iterator() ; iter.hasNext() ; )
        {
            Expr expr = (Expr)iter.next() ;
            qIter = new QueryIterFilterExpr(qIter, expr, execCxt) ;
            iter.remove();
        }
        return qIter ;
    }
}

/*
 * (c) Copyright 2007, 2008 Hewlett-Packard Development Company, LP
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