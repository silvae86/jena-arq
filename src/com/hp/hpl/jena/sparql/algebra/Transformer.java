/*
 * (c) Copyright 2006, 2007, 2008 Hewlett-Packard Development Company, LP
 * All rights reserved.
 * [See end of file]
 */

package com.hp.hpl.jena.sparql.algebra;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Stack;

import com.hp.hpl.jena.sparql.algebra.op.*;
import com.hp.hpl.jena.sparql.util.ALog;

public class Transformer
{
    static boolean noDupIfSame = true ;
    
    public static Op transform(Transform transform, Op op)
    { return transform(transform, op, null) ; }
    
    public static Op transform(Transform transform, Op op, OpVisitor monitor)
    {
        if ( op == null )
        {
            ALog.warn(Transformer.class, "Attempt to transform a null Op - ignored") ;
            return op ;
        }
        
        TransformApply v = new TransformApply(transform, monitor) ;
        op.visit(v) ;
        Op r = v.result() ;
        return r ;
    }
    
    private Transformer() { }
    
    private static final 
    class TransformApply implements OpVisitor
    {
        private Transform transform = null ;
        // Called before recursing.  Can be null.  Must not mutate anything.
        private OpVisitor beforeVisitor = null ; 
        private Stack stack = new Stack() ;
        private Op pop() { return (Op)stack.pop(); }
        private void push(Op op)
        { 
            // Including nulls
            stack.push(op) ;
        }
        
        public TransformApply(Transform transform, OpVisitor beforeVisitor)
        { 
            this.transform = transform ;
            this.beforeVisitor = beforeVisitor ;
        }
        
        public Op result()
        { 
            if ( stack.size() != 1 )
                ALog.warn(this, "Stack is not aligned") ;
            return pop() ; 
        }

        private void visit0(Op0 op)
        {
            if ( beforeVisitor != null )
                op.visit(beforeVisitor) ;
            push(op.apply(transform)) ;
        }
        
        private void visit1(Op1 op)
        {
            if ( beforeVisitor != null )
                op.visit(beforeVisitor) ;
            Op subOp = null ;
            if ( op.getSubOp() != null )
            {
                op.getSubOp().visit(this) ;
                subOp = pop() ;
            }
            push(op.apply(transform, subOp)) ;
        }

        private void visit2(Op2 op)
        { 
            if ( beforeVisitor != null )
                op.visit(beforeVisitor) ;
            Op left = null ;
            Op right = null ;

            if ( op.getLeft() != null )
            {
                op.getLeft().visit(this) ;
                left = pop() ;
            }
            if ( op.getRight() != null )
            {
                op.getRight().visit(this) ;
                right = pop() ;
            }
            Op opX = op.apply(transform, left, right) ; 
            push(opX) ;
        }
        
        private void visitN(OpN op)
        {
            if ( beforeVisitor != null )
                op.visit(beforeVisitor) ;
            List x = new ArrayList(op.size()) ;
            for ( Iterator iter = op.iterator() ; iter.hasNext() ; )
            {
                Op sub = (Op)iter.next() ;
                sub.visit(this) ;
                Op r = pop() ;
                // Skip nulls.
                if ( r != null )
                    x.add(r) ;
            }
            Op opX = op.apply(transform, x) ;  
            push(opX) ;
        }
        
        public void visit(OpTable opTable)
        { visit0(opTable) ; }
        
        public void visit(OpQuadPattern quadPattern)
        { visit0(quadPattern) ; }

        public void visit(OpPath opPath)
        { visit0(opPath) ; }
        
        public void visit(OpTriple opTriple)
        { visit0(opTriple) ; }
        
        public void visit(OpDatasetNames dsNames)
        { visit0(dsNames) ; }

        public void visit(OpBGP op)
        { visit0(op); } 
        
        public void visit(OpProcedure opProc)
        { visit1(opProc) ; }
        
        public void visit(OpPropFunc opPropFunc)
        { visit1(opPropFunc) ; }
        
        public void visit(OpJoin opJoin)
        { visit2(opJoin) ; }

        public void visit(OpSequence opSequence)
        { visitN(opSequence) ; }
        
        public void visit(OpLeftJoin opLeftJoin)
        { visit2(opLeftJoin) ; }

        public void visit(OpDiff opDiff)
        { visit2(opDiff) ; }
        
        public void visit(OpUnion opUnion)
        { visit2(opUnion) ; }

        public void visit(OpFilter opFilter)
        { visit1(opFilter) ; }

        public void visit(OpGraph opGraph)
        { visit1(opGraph) ; }

        public void visit(OpService opService)
        { visit1(opService) ; }
        
        public void visit(OpExt opExt)
        { push(transform.transform(opExt)) ; }
        
        public void visit(OpNull opNull)
        { visit0(opNull) ; }
        
        public void visit(OpLabel opLabel)
        { visit1(opLabel) ; }
        
        public void visit(OpList opList)
        { visit1(opList) ; }
        
        public void visit(OpOrder opOrder)
        { visit1(opOrder) ; }
        
        public void visit(OpProject opProject)
        { visit1(opProject) ; }
        
        public void visit(OpDistinct opDistinct)
        { visit1(opDistinct) ; }
        
        public void visit(OpReduced opReduced)
        { visit1(opReduced) ; }
        
        public void visit(OpAssign opAssign)
        { visit1(opAssign) ; }
        
        public void visit(OpSlice opSlice)
        { visit1(opSlice) ; }
        
        public void visit(OpGroupAgg opGroupAgg)
        { visit1(opGroupAgg) ; }
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