/*
 * (c) Copyright 2005, 2006, 2007 Hewlett-Packard Development Company, LP
 * All rights reserved.
 * [See end of file]
 */

package com.hp.hpl.jena.sparql.function.library;

import java.io.PrintStream;
import java.io.PrintWriter;

//import org.apache.commons.logging.*;
import com.hp.hpl.jena.query.QueryBuildException;
import com.hp.hpl.jena.sparql.ARQInternalErrorException;
import com.hp.hpl.jena.sparql.engine.binding.Binding;
import com.hp.hpl.jena.sparql.expr.Expr;
import com.hp.hpl.jena.sparql.expr.ExprEvalException;
import com.hp.hpl.jena.sparql.expr.ExprList;
import com.hp.hpl.jena.sparql.expr.NodeValue;
import com.hp.hpl.jena.sparql.function.Function;
import com.hp.hpl.jena.sparql.function.FunctionEnv;
import com.hp.hpl.jena.sparql.serializer.FmtExprARQSubst;
import com.hp.hpl.jena.sparql.util.Utils;
import com.hp.hpl.jena.util.FileUtils;

/** Function that prints what happens to an expression - always returns true.
 *  Example:
 *  <pre>
 *    PREFIX  afn:   &lt;http://jena.hpl.hp.com/ARQ/function#&gt;
 *    . . . 
 *    FILTER afn:print("?v =",?v,"?w =",>w)
 *    FILTER afn:trace(?v = ?w)
 *    FILTER (?v = ?w)
 *  </pre>
 * 
 * @author Andy Seaborne
 * @version $Id: trace.java,v 1.13 2007/02/06 17:06:15 andy_seaborne Exp $
 */

public class trace implements Function
{
    private static PrintWriter out = FileUtils.asPrintWriterUTF8(System.out) ; 
    static public void setStream(PrintStream stream) { out = FileUtils.asPrintWriterUTF8(stream) ; }
    
    public void build(String uri, ExprList args)
    { 
        if ( args.size() != 1 )
            throw new QueryBuildException("Function '"+Utils.className(this)+"' takes one argument") ;
    }

    public NodeValue exec(Binding binding, ExprList args, String uri, FunctionEnv env)
    {
        if ( args == null )
            // The contract on the function interface is that this should not happen.
            throw new ARQInternalErrorException("Function '"+Utils.className(this)+" Null args list") ;
        
        if ( args.size() != 1 )
            throw new ExprEvalException("Function '"+Utils.className(this)+" Wanted 1, got "+args.size()) ;
        
        Expr expr = args.get(0) ;
        String s = FmtExprARQSubst.format(expr, binding) ;
        
        try {
            NodeValue x = expr.eval(binding, env) ;
            out.println(s+" => "+x) ;
            out.flush() ;
        } catch (ExprEvalException ex)
        { out.println(s+" => Exception("+ex.getMessage()+")") ; }
        
        out.flush() ;
        
        return NodeValue.booleanReturn(true) ;
    }  
    
}

/*
 * (c) Copyright 2005, 2006, 2007 Hewlett-Packard Development Company, LP
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