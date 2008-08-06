/*
 * (c) Copyright 2007, 2008 Hewlett-Packard Development Company, LP
 * All rights reserved.
 * [See end of file]
 */

package com.hp.hpl.jena.sparql.engine.iterator;

import java.util.Iterator;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.query.QueryExecException;
import com.hp.hpl.jena.sparql.core.Var;
import com.hp.hpl.jena.sparql.core.VarExprList;
import com.hp.hpl.jena.sparql.engine.ExecutionContext;
import com.hp.hpl.jena.sparql.engine.QueryIterator;
import com.hp.hpl.jena.sparql.engine.binding.Binding;
import com.hp.hpl.jena.sparql.engine.binding.BindingMap;
import com.hp.hpl.jena.sparql.function.FunctionEnv;

/** Extend each solution by an expression */ 

public class QueryIterExtend extends QueryIterConvert
{
    public QueryIterExtend(QueryIterator input, VarExprList exprs, ExecutionContext qCxt)
    {
        super(input, new Extend(exprs, qCxt), qCxt) ;
    }
    
    static
    class Extend implements QueryIterConvert.Converter
    {
        FunctionEnv funcEnv ;
        VarExprList exprs ; 

        Extend(VarExprList exprs, ExecutionContext qCxt)
        { 
            this.exprs = exprs ;
            funcEnv = qCxt ;
        }

        public Binding convert(Binding bind)
        {
            Binding b = new BindingMap(bind) ;
            for ( Iterator iter = exprs.getVars().iterator() ; iter.hasNext(); )
            {
                Var v = (Var)iter.next();
                // Not ... expressions not seeing new bindings.
                //Node n = exprs.get(v, bind, funcEnv) ;
                // which give (Lisp) "let" semantics, not "let*" semantics 
                Node n = exprs.get(v, b, funcEnv) ;
                
                if ( n == null )
                {
                    // Failed to evaluate
                    continue ;
                }
                // Only add those variables that have expressions associated with them
                // The parent, bind, already has bound variables for the non-expressions. 
                if ( b.contains(v) )
                {
                    Node n2 = b.get(v) ;
                    if ( ! n2.sameValueAs(n) )
                        throw new QueryExecException("Already set: "+v) ;
                    continue ;
                }
                b.add(v, n) ;
            }
            return b ;
        }
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