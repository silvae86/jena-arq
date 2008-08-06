/*
 * (c) Copyright 2007, 2008 Hewlett-Packard Development Company, LP
 * All rights reserved.
 * [See end of file]
 */

package com.hp.hpl.jena.sparql.engine.main;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.hp.hpl.jena.query.QueryExecException;
import com.hp.hpl.jena.sparql.ARQNotImplemented;
import com.hp.hpl.jena.sparql.algebra.Op;
import com.hp.hpl.jena.sparql.algebra.Table;
import com.hp.hpl.jena.sparql.algebra.op.*;
import com.hp.hpl.jena.sparql.core.BasicPattern;
import com.hp.hpl.jena.sparql.engine.ExecutionContext;
import com.hp.hpl.jena.sparql.engine.QueryIterator;
import com.hp.hpl.jena.sparql.engine.iterator.*;
import com.hp.hpl.jena.sparql.engine.main.iterator.QueryIterGraph;
import com.hp.hpl.jena.sparql.engine.main.iterator.QueryIterJoin;
import com.hp.hpl.jena.sparql.engine.main.iterator.QueryIterLeftJoin;
import com.hp.hpl.jena.sparql.engine.main.iterator.QueryIterOptionalIndex;
import com.hp.hpl.jena.sparql.engine.main.iterator.QueryIterService;
import com.hp.hpl.jena.sparql.engine.main.iterator.QueryIterUnion;
import com.hp.hpl.jena.sparql.expr.Expr;
import com.hp.hpl.jena.sparql.expr.ExprBuild;
import com.hp.hpl.jena.sparql.expr.ExprList;
import com.hp.hpl.jena.sparql.expr.ExprWalker;
import com.hp.hpl.jena.sparql.procedure.ProcEval;
import com.hp.hpl.jena.sparql.procedure.Procedure;

/** Turn an Op expression into an execution of QueryIterators.
 *  Does not consider optimizing the algebra expression (that should happen elsewhere).
 *  BGPs are stil subject to StageBuilding during iterator execution.  
 * 
 * @author Andy Seaborne
 */

public class OpCompiler
{
    // A small (one slot) registry to allow (experimental) alternative  OpCompilers
    public interface Factory { OpCompiler create(ExecutionContext execCxt) ; }
    
    // Set this to a different factory implementation to have a different OpCompiler.  
    public static Factory factory = new Factory(){

        public OpCompiler create(ExecutionContext execCxt)
        {
            return new OpCompiler(execCxt) ;
        }} ;  
    
    // -------
    // Call via QC.compile
    
    static QueryIterator compile(Op op, ExecutionContext execCxt)
    {
        return compile(op, root(execCxt), execCxt) ;
    }
    
    static QueryIterator compile(Op op, QueryIterator qIter, ExecutionContext execCxt)
    {
        OpCompiler compiler = null ;
        if ( factory == null )
            compiler = new OpCompiler(execCxt) ;    // Only if default 'factory' gets lost.
        else
            compiler = factory.create(execCxt) ;
        QueryIterator q = compiler.compileOp(op, qIter) ;
        return q ;
    }

    protected ExecutionContext execCxt ;
    protected CompilerDispatch dispatcher = null ;

    protected OpCompiler(ExecutionContext execCxt)
    { 
        this.execCxt = execCxt ;
        dispatcher = new CompilerDispatch(this) ;
    }

    public QueryIterator compileOp(Op op)
    {
        return compileOp(op, null) ;
    }

    public QueryIterator compileOp(Op op, QueryIterator input)
    {
        return dispatcher.compile(op, input) ;
    }
        
    public QueryIterator compile(OpBGP opBGP, QueryIterator input)
    {
        BasicPattern pattern = opBGP.getPattern() ;
        return StageBuilder.compile(pattern, input, execCxt) ;
    }

    public QueryIterator compile(OpTriple opTriple, QueryIterator input)
    {
        return compile(opTriple.asBGP(), input) ;
    }

    public QueryIterator compile(OpQuadPattern quadPattern, QueryIterator input)
    {
        if ( false )
        {
            if ( quadPattern.isDefaultGraph() )
            {
                // Easy case.
                OpBGP opBGP = new OpBGP(quadPattern.getBasicPattern()) ;
                return compile(opBGP, input) ;  
            }
        }        
        // Turn into a OpGraph/OpBGP.
        throw new ARQNotImplemented("compile/OpQuadPattern") ;
    }

    public QueryIterator compile(OpPath opPath, QueryIterator input)
    {
        return new QueryIterPath(opPath.getTriplePath(), input, execCxt) ;
    }

    public QueryIterator compile(OpProcedure opProc, QueryIterator input)
    {
        Procedure procedure = ProcEval.build(opProc, execCxt) ;
        QueryIterator qIter = compileOp(opProc.getSubOp(), input) ;
        // Delay until query starts executing.
        return new QueryIterProcedure(qIter, procedure, execCxt) ;
    }

    public QueryIterator compile(OpPropFunc opPropFunc, QueryIterator input)
    {
        Procedure procedure = ProcEval.build(opPropFunc.getProperty(), opPropFunc.getSubjectArgs(),opPropFunc.getObjectArgs(), execCxt) ;
        QueryIterator qIter = compileOp(opPropFunc.getSubOp(), input) ;
        return new QueryIterProcedure(qIter, procedure, execCxt) ;
    }


    public QueryIterator compile(OpJoin opJoin, QueryIterator input)
    {
        // Look one level in for any filters with out-of-scope variables.
        boolean canDoLinear = JoinClassifier.isLinear(opJoin) ;

        if ( canDoLinear )
            // Streamed evaluation
            return stream(opJoin.getLeft(), opJoin.getRight(), input) ;
        
        // Can't do purely indexed (e.g. a filter referencing a variable out of scope is in the way)
        // To consider: partial substitution for improved performance (but does it occur for real?)
        
        QueryIterator left = compileOp(opJoin.getLeft(), input) ;
        QueryIterator right = compileOp(opJoin.getRight(), root()) ;
        QueryIterator qIter = new QueryIterJoin(left, right, execCxt) ;
        return qIter ;
        // Worth doing anything about join(join(..))?
    }

    // Pass iterator from left directly into the right.
    protected QueryIterator stream(Op opLeft, Op opRight, QueryIterator input)
    {
        QueryIterator left = compileOp(opLeft, input) ;
        QueryIterator right = compileOp(opRight, left) ;
        return right ;
    }

    // Pass iterator from one step directly into the next.
    public QueryIterator compile(OpSequence opSequence, QueryIterator input)
    {
        QueryIterator qIter = input ;
        
        for ( Iterator iter = opSequence.iterator() ; iter.hasNext() ; )
        {
            Op sub = (Op)iter.next() ;
            qIter = compileOp(sub, qIter) ;
        }
        
        return qIter ;
    }
    
    public QueryIterator compile(OpLeftJoin opLeftJoin, QueryIterator input)
    {
        ExprList exprs = opLeftJoin.getExprs() ;
        if ( exprs != null )
            exprs.prepareExprs(execCxt.getContext()) ;

        QueryIterator left = compileOp(opLeftJoin.getLeft(), input) ;
        // Do an indexed substitute into the right if possible.
        boolean canDoLinear = LeftJoinClassifier.isLinear(opLeftJoin) ;
        
        if ( canDoLinear )
        {
            // Pass left into right for substitution before right side evaluation.
            QueryIterator qIter = new QueryIterOptionalIndex(left, opLeftJoin.getRight(), exprs, execCxt) ;
            return  qIter ;
        }

        // Do it by sub-evaluation of left and right then left join.
        // Can be expensive if RHS returns a lot.
        // To consider: partial substitution for improved performance (but does it occur for real?)

        QueryIterator right = compileOp(opLeftJoin.getRight(), root()) ;
        QueryIterator qIter = new QueryIterLeftJoin(left, right, exprs, execCxt) ;
        return qIter ;
    }

    public QueryIterator compile(OpDiff opDiff, QueryIterator input)
    { 
        QueryIterator left = compileOp(opDiff.getLeft(), input) ;
        QueryIterator right = compileOp(opDiff.getRight(), root()) ;
        return new QueryIterDiff(left, right, execCxt) ;
    }
    
    public QueryIterator compile(OpUnion opUnion, QueryIterator input)
    {
        List x = flattenUnion(opUnion) ;
        QueryIterator cIter = new QueryIterUnion(input, x, execCxt) ;
        return cIter ;
    }
    
    // Based on code from Olaf Hartig.
    protected List flattenUnion(OpUnion opUnion)
    {
        List x = new ArrayList() ;
        flattenUnion(x, opUnion) ;
        return x ;
    }
    
    protected void flattenUnion(List acc, OpUnion opUnion)
    {
        if (opUnion.getLeft() instanceof OpUnion)
            flattenUnion(acc, (OpUnion)opUnion.getLeft()) ;
        else
            acc.add( opUnion.getLeft() ) ;

        if (opUnion.getRight() instanceof OpUnion)
            flattenUnion(acc, (OpUnion)opUnion.getRight()) ;
        else
            acc.add( opUnion.getRight() ) ;
    }
    
    public QueryIterator compile(OpFilter opFilter, QueryIterator input)
    {
        ExprList exprs = opFilter.getExprs() ;
        exprs.prepareExprs(execCxt.getContext()) ;
        
        Op base = opFilter.getSubOp() ;
        
        // Done as a transform prior to compilation to a query plan
        QueryIterator qIter = compileOp(base, input) ;

        for ( Iterator iter = exprs.iterator() ; iter.hasNext(); )
        {
            Expr expr = (Expr)iter.next() ;
            qIter = new QueryIterFilterExpr(qIter, expr, execCxt) ;
        }
        return qIter ;
        
//        FilterPlacement filterPlacement = new FilterPlacement(this, execCxt);
//        if ( base instanceof OpBGP )
//            // Uncompiled => unsplit
//            return filterPlacement.placeFiltersBGP(exprs, ((OpBGP)base).getPattern(), input) ;
//
//        if ( base instanceof OpSequence )
//            return filterPlacement.placeFiltersStage(exprs, (OpSequence)base, input) ;
//        
//        if ( base instanceof OpGraph )
//        {}
//
////        if ( base instanceof OpQuadPattern )
////            return filterPlacement.placeFilter(opFilter.getExpr(), (OpQuadPattern)base, input) ;
//        
//        // Tidy up.
//        if ( base instanceof OpJoin )
//        {
//            OpJoin opJoin = (OpJoin)base ;
//            boolean canDoLinear = JoinClassifier.isLinear(opJoin) ;
//            if ( canDoLinear )
//                return filterPlacement.placeFiltersJoin(exprs, (OpJoin)base, input) ;
//        }
//        
//        // There must be a better way.
//        if ( base instanceof OpLeftJoin )
//        {
//            // Can push in if used only on the LHS 
//        }
//        
//        if ( base instanceof OpUnion )
//        {}
//
//        // Nothing special.
//        return filterPlacement.buildOpFilter(exprs, base, input) ;
    }

    protected void prepareExprs(ExprList exprs)
    {
        for ( Iterator iter = exprs.iterator() ; iter.hasNext() ; )
        {
            Expr expr = (Expr)iter.next() ;
            ExprWalker.walk(new ExprBuild(execCxt.getContext()), expr) ;
        }
    }
    
    public QueryIterator compile(OpGraph opGraph, QueryIterator input)
    { 
        return new QueryIterGraph(input, opGraph, execCxt) ;
    }
    
    public QueryIterator compile(OpService opService, QueryIterator input)
    {
        return new QueryIterService(input, opService, execCxt) ;
    }
    
    public QueryIterator compile(OpDatasetNames dsNames, QueryIterator input)
    { 
        if ( true ) throw new ARQNotImplemented("OpDatasetNames") ;
        
        // Augment (join) iterator with a table.
        Table t = null ;
        Op left = null ; 
        Op right = OpTable.create(t) ;
        Op opJoin = OpJoin.create(left, right) ;
        return compileOp(opJoin , input) ;    //??
    }

    public QueryIterator compile(OpTable opTable, QueryIterator input)
    { 
//        if ( input instanceof QueryIteratorBase )
//        {
//            String x = ((QueryIteratorBase)input).debug();
//            System.out.println(x) ;
//        }
//        
        if ( opTable.isJoinIdentity() )
            return input ;
        if ( input instanceof QueryIterRoot )
        {
            input.close() ;
            return opTable.getTable().iterator(execCxt) ;
        }
        //throw new ARQNotImplemented("Not identity table") ;
        QueryIterator qIterT = opTable.getTable().iterator(execCxt) ;
        //QueryIterator qIterT = root() ;
        QueryIterator qIter = new QueryIterJoin(input, qIterT, execCxt) ;
        return qIter ;
    }

    public QueryIterator compile(OpExt opExt, QueryIterator input)
    { 
        if ( opExt instanceof OpExtMain )
        {
            OpExtMain op = (OpExtMain)opExt ;
            return op.eval(input, execCxt) ;
        }
        
        throw new QueryExecException("Encountered unsupport OpExt: "+opExt.getName()) ;
    }

    public QueryIterator compile(OpNull opNull, QueryIterator input)
    {
        // Loose the input.
        input.close() ;
        return new QueryIterNullIterator(execCxt) ;
    }

    public QueryIterator compile(OpList opList, QueryIterator input)
    {
        return compileOp(opList.getSubOp(), input) ;
    }
    
    public QueryIterator compile(OpOrder opOrder, QueryIterator input)
    { 
        QueryIterator qIter = compileOp(opOrder.getSubOp(), input) ;
        qIter = new QueryIterSort(qIter, opOrder.getConditions(), execCxt) ;
        return qIter ;
    }

    public QueryIterator compile(OpProject opProject, QueryIterator input)
    {
        QueryIterator  qIter = compileOp(opProject.getSubOp(), input) ;
        qIter = new QueryIterProject(qIter, opProject.getVars(), execCxt) ;
        return qIter ;
    }

    public QueryIterator compile(OpSlice opSlice, QueryIterator input)
    { 
        QueryIterator qIter = compileOp(opSlice.getSubOp(), input) ;
        qIter = new QueryIterSlice(qIter, opSlice.getStart(), opSlice.getLength(), execCxt) ;
        return qIter ;
    }
    
    public QueryIterator compile(OpGroupAgg opGroupAgg, QueryIterator input)
    { 
        QueryIterator qIter = compileOp(opGroupAgg.getSubOp(), input) ;
        qIter = new QueryIterGroup(qIter, opGroupAgg.getGroupVars(), opGroupAgg.getAggregators(), execCxt) ;
        return qIter ;
    }
    
    public QueryIterator compile(OpDistinct opDistinct, QueryIterator input)
    {
        QueryIterator qIter = compileOp(opDistinct.getSubOp(), input) ;
        qIter = new QueryIterDistinct(qIter, execCxt) ;
        return qIter ;
    }

    public QueryIterator compile(OpReduced opReduced, QueryIterator input)
    {
        QueryIterator qIter = compileOp(opReduced.getSubOp(), input) ;
        qIter = new QueryIterReduced(qIter, execCxt) ;
        return qIter ;
    }

    public QueryIterator compile(OpAssign opAssign, QueryIterator input)
    {
        QueryIterator qIter = compileOp(opAssign.getSubOp(), input) ;
        qIter = new QueryIterExtend(qIter, opAssign.getVarExprList(), execCxt) ;
        return qIter ;
    }

    protected static QueryIterator root(ExecutionContext execCxt)
    {
        return QueryIterRoot.create(execCxt) ;
    }

    protected QueryIterator root()
    { return root(execCxt) ; }
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