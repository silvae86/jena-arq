/*
 * (c) Copyright 2007, 2008, 2009 Hewlett-Packard Development Company, LP
 * All rights reserved.
 * [See end of file]
 */

package dev;

import java.util.Iterator;

import arq.sparql;
import arq.sse_query;

import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.QuerySolutionMap;
import com.hp.hpl.jena.query.ResultSetFormatter;
import com.hp.hpl.jena.query.Syntax;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.sparql.algebra.Algebra;
import com.hp.hpl.jena.sparql.algebra.ExtBuilder;
import com.hp.hpl.jena.sparql.algebra.Op;
import com.hp.hpl.jena.sparql.algebra.OpAsQuery;
import com.hp.hpl.jena.sparql.algebra.OpExtRegistry;
import com.hp.hpl.jena.sparql.algebra.op.OpAssign;
import com.hp.hpl.jena.sparql.algebra.op.OpExt;
import com.hp.hpl.jena.sparql.algebra.op.OpFetch;
import com.hp.hpl.jena.sparql.algebra.op.OpFilter;
import com.hp.hpl.jena.sparql.algebra.op.OpProject;
import com.hp.hpl.jena.sparql.algebra.opt.Optimize;
import com.hp.hpl.jena.sparql.core.Prologue;
import com.hp.hpl.jena.sparql.core.QueryCheckException;
import com.hp.hpl.jena.sparql.engine.ExecutionContext;
import com.hp.hpl.jena.sparql.engine.QueryIterator;
import com.hp.hpl.jena.sparql.expr.Expr;
import com.hp.hpl.jena.sparql.mgt.ARQMgt;
import com.hp.hpl.jena.sparql.serializer.SerializationContext;
import com.hp.hpl.jena.sparql.sse.Item;
import com.hp.hpl.jena.sparql.sse.ItemList;
import com.hp.hpl.jena.sparql.sse.SSE;
import com.hp.hpl.jena.sparql.sse.SSEParseException;
import com.hp.hpl.jena.sparql.sse.WriterSSE;
import com.hp.hpl.jena.sparql.sse.builders.BuildException;
import com.hp.hpl.jena.sparql.sse.builders.BuilderExec;
import com.hp.hpl.jena.sparql.util.IndentedLineBuffer;
import com.hp.hpl.jena.sparql.util.IndentedWriter;
import com.hp.hpl.jena.sparql.util.NodeIsomorphismMap;
import com.hp.hpl.jena.sparql.util.StrUtils;
import com.hp.hpl.jena.sparql.util.StringUtils;
import com.hp.hpl.jena.util.FileManager;

public class RunARQ
{
    static String divider = "----------" ;
    static String nextDivider = null ;
    static void divider()
    {
        if ( nextDivider != null )
            System.out.println(nextDivider) ;
        nextDivider = divider ;
    }
    
    public static void main(String[] argv) throws Exception
    {
        qparse("--file=Q.arq", "--print=op") ;
        report() ;
        
        // "-Dcom.sun.management.jmxremote" needed for Java5
        // Efficiently do nothing.
        ARQMgt.init() ;
        System.out.println("Waiting (nearly) forever..."); 
        Thread.sleep(Long.MAX_VALUE);
        System.exit(0) ;
        
        report() ;
        
        if ( false )
            Optimize.noOptimizer() ;
        qparse("--file=testing/ARQ/Negation/neg-08.arq", "--print=query", "--print=op", "--opt") ;
        //execQuery("D.ttl", "Q.arq") ;
        
        String qs = StrUtils.strjoinNL(
                           "PREFIX books:   <http://example.org/book/>",
                           "PREFIX dc:      <http://purl.org/dc/elements/1.1/>",
                           "SELECT ?book ?title" ,
                           "{ ?book dc:title ?title . FILTER regex(?title, 'foo', 'a') }") ;
        Query query = QueryFactory.create(qs) ;
        System.out.println(query) ;
        System.exit(0) ;
        
        execQuery("D.ttl", "Q.arq") ; System.exit(0) ;
        
        divider() ;
    }

    private static void testOpToSyntax(String opStr, String queryString)
    {
        Op op = SSE.parseOp(opStr) ;
        Query queryConverted = OpAsQuery.asQuery(op) ;
        queryConverted.setResultVars() ;
        
        Query queryExpected = QueryFactory.create(queryString) ;
        System.out.println(queryConverted) ;
        System.out.println(queryExpected) ;
        queryExpected.setResultVars() ;
        
        System.out.println( queryExpected.getQueryPattern().equals(queryConverted.getQueryPattern()))  ;
        System.out.println( queryExpected.equals(queryConverted))  ;
    }

    public static void report()
    {
//        Model model=ModelFactory.createDefaultModel() ;
//        
//        model.write(System.out, "TTL") ;
//        System.exit(0) ;
        
        qparse("--print=op", "--print=query", "--file=Q.arq") ; 
        // PathPrimary to be split into Unary and Primary.
        // PathSequence, PathElt
        System.exit(0) ;
    }
    
    public static void check(Query query, boolean optimizeAlgebra)
    {
        Op op = Algebra.compile(query) ;
        check( op, optimizeAlgebra, query) ;        
    }
    
    private static void check(Op op, boolean optimizeAlgebra, Prologue prologue)
    {
        IndentedLineBuffer buff = new IndentedLineBuffer() ;
        if ( optimizeAlgebra )
            op =  Algebra.optimize(op) ;
        WriterSSE.out(buff.getIndentedWriter(), op, prologue) ;
        String str = buff.getBuffer().toString() ;
        
        try {
            Op op2 = SSE.parseOp(str) ;
            if ( op.hashCode() != op2.hashCode() )
            {
                System.out.println(str) ;
                System.out.println(op) ;
                System.out.println(op2) ;
                
                throw new QueryCheckException("reparsed algebra expression hashCode does not equal algebra from query") ;
            }
            System.out.println(op) ;
            System.out.println(op2) ;
            
            // Expression in assignment for op 
            
            if ( ! op.equals(op2) )
            {
                Expr e1 = ((OpFilter)op).getExprs().get(0) ;
                Expr e2 = ((OpFilter)op2).getExprs().get(0) ;

                op = ((OpFilter)op).getSubOp() ;
                op2 = ((OpFilter)op2).getSubOp() ;

                if ( ! op.equals(op2) )
                    System.err.println("Sub patterns unequal") ;
                
                if ( ! e1.equals(e2) )
                {
                    System.err.println(e1) ;
                    System.err.println(e2) ;
                    System.err.println("Expressions unequal") ;
                }
                
                throw new QueryCheckException("reparsed algebra expression does not equal query algebra") ;
            }
        } catch (SSEParseException ex)
        { 
            System.err.println(str);
            throw ex ; 
        }      // Breakpoint
        catch (BuildException ex)
        {
            System.err.println(str);
            throw ex ; 
        }

    }

    static void queryEquality()
    {
        String[] x1 = { "PREFIX  :     <http://example/>", 
            "PREFIX  foaf: <http://xmlns.com/foaf/0.1/>",
            "",
            "SELECT  ?x ?n",
            "WHERE", 
            "  { ?x foaf:name ?n" ,
            "    { SELECT  ?x",
            "      WHERE",
            "        { ?x foaf:knows ?z}",
            "      GROUP BY ?x",
            "      HAVING ( count(*) >= 10 )",
            "    }",
        "  }" } ;
        
        String[] x2 = { "PREFIX  :     <http://example/>", 
            "PREFIX  foaf: <http://xmlns.com/foaf/0.1/>",
            "SELECT  *",
            "{ }",
            "GROUP BY ?x",
            "HAVING ( count(*) )"
        } ;
        
        String[] x3 = { "(filter (>= ?.0 10)",
            "   (group (?x) ((?.0 (count)))" ,
            "   (table unit)",
            "))" } ;
        
//        Op op = SSE.parseOp(StringUtils.join("\n", x3)) ;
//        checkOp(op, false, null) ;
//        System.out.println();
        Query query = QueryFactory.create(StringUtils.join("\n", x2), Syntax.syntaxARQ) ;
        check(query, false) ;
        System.exit(0) ;

    }
    
    private static void compare(String string, Op op1, Op op2)
    {
        divider() ;
        System.out.println("Compare: "+string) ;
        
        if ( op1.hashCode() != op2.hashCode() )
        {
//            System.out.println(str) ;
//            System.out.println(op) ;
//            System.out.println(op2) ;
//            
            throw new QueryCheckException("reparsed algebra expression hashCode does not equal algebra from query") ;
        }
//        System.out.println(op) ;
//        System.out.println(op2) ;
        
        // Expression in assignment for op 
        
        if ( ! op1.equals(op2) )
            throw new QueryCheckException("reparsed algebra expression does not equal query algebra") ;
        
    }
    
    private static void decompose(Op op)
    {
        Op opX = ((OpProject)op).getSubOp() ;
        OpAssign opAssign = (OpAssign)opX ;
        //System.out.print(opAssign) ;
        System.out.println("Assign VarExprList: "+opAssign.getVarExprList()) ;
    }
    
    public static void fetch()
    {
        OpFetch.enable() ;
        sparql.main(new String[]{"--file=Q.arq"}) ;
        //System.out.println("----") ;
        System.exit(0) ; 
    }
    
    public static void opExtension()
    {
        OpExtRegistry.register(new ExtBuilder(){
            public OpExt make(ItemList argList)
            {
                System.out.println("Args: "+argList) ;
                return new OpExtTest(argList) ;
            }

            public String getTagName() { return "ABC" ; }
        }) ;
        
        Op op1 = SSE.parseOp("(ext ABC 123 667)") ;
        System.out.println(op1); 

        Op op2 = SSE.parseOp("(ABC 123 667)") ;
        System.out.println(op2); 

        System.out.println("----") ; System.exit(0) ; 
    }

    static class OpExtTest extends OpExt 
    {
        private ItemList argList ;
    
        public OpExtTest(ItemList argList)
        { super("TAG") ; this.argList = argList ; }
    
        @Override
        public Op effectiveOp()
        {
            return null ;
        }
    
        @Override
        public QueryIterator eval(QueryIterator input, ExecutionContext execCxt)
        {
            return null ;
        }
    
        @Override
        public boolean equalTo(Op other, NodeIsomorphismMap labelMap)
        {
            if ( ! ( other instanceof OpExtTest) ) return false ;
            return argList.equals(((OpExtTest)other).argList) ;
        }
    
        @Override
        public void outputArgs(IndentedWriter out, SerializationContext sCxt)
        {
            boolean first = true ;
            for ( Iterator<Item> iter = argList.iterator() ; iter.hasNext() ; )
            {
                Item item = iter.next();
                if ( first )
                    first = false ;
                else
                    out.print(" ") ;
                out.print(item) ;
            }
        }
        
        @Override
        public int hashCode()
        {
            return argList.hashCode() ;
        }
    }

    private static void qparse(String  ... a)
    {
        arq.qparse.main(a) ;
        System.exit(0) ;
    }
    
    private static void runUpdate()
    {
        String a[] = {/*"--desc=etc/graphstore.ttl",*/ "--update=update.ru", "--dump"} ;
        arq.update.main(a) ;
        System.exit(0) ;
    }
    
    private static void runQTest()
    {
        String DIR = "testing/ARQ/DAWG-Final/" ;
        String []a1 = { "--strict", "--data="+DIR+"data.ttl",
            "--query="+DIR+"assign-01.arq",
            "--result="+DIR+"assign-01.srx"} ;

        arq.qtest.main(a1) ;
        System.exit(0 ) ; 
  
    }

    private static void execQuery(String datafile, String queryfile)
    {
        //QueryEngineMain.register() ;
        String a[] = new String[]{
            //"-v",
            //"--engine=ref", 
            "--data="+datafile,
            "-query="+queryfile , 
        } ;
        
        sparql.main(a) ;
        System.exit(0) ;
    }

    private static void execQuery2(String datafile, String queryfile)
    {
        //QueryEngineMain.register() ;
        String a[] = new String[]{
            //"-v",
            "--data="+datafile,
            "-query="+queryfile , 
        } ;
        
        sparql.main(a) ;
        a = new String[]{
            //"-v",
            "--engine=ref", 
            "--data="+datafile,
            "-query="+queryfile , 
        } ;
        sparql.main(a) ;
        
        System.exit(0) ;
    }
    
    private static void execQuerySSE(String datafile, String queryfile)
    {
        //com.hp.hpl.jena.sparql.engine.ref.QueryEngineRef.register() ;
        String a[] = new String[]{
            //"-v",
            //"--engine=ref",
            "--data="+datafile,
            "-query="+queryfile , 
        } ;
        
        sse_query.main(a) ;
        System.exit(0) ;
        
    }
    
    private static void execQueryCode(String datafile, String queryfile)
    {
        Model model = FileManager.get().loadModel(datafile) ;
        Query query = QueryFactory.read(queryfile) ;
        
        QuerySolutionMap initialBinding = new QuerySolutionMap();
        //initialBinding.add("s", model.createResource("http://example/x1")) ;
        initialBinding.add("o", model.createResource("http://example/z")) ;
        
        QueryExecution qExec = QueryExecutionFactory.create(query, model, initialBinding) ;
        ResultSetFormatter.out(qExec.execSelect()) ;
        System.exit(0) ;
    }

    private static void execRemote()
    {
        System.setProperty("socksProxyHost", "socks-server") ;
    
        String a2[] = { "--service=http://dbpedia.org/sparql",
        "SELECT * WHERE {  <http://dbpedia.org/resource/Angela_Merkel> <http://dbpedia.org/property/reference> ?object.  FILTER  (!isLiteral(?object))}"} ;
        arq.remote.main(a2) ;
        System.exit(0) ;
    }
    
    private static void runExecuteSSE(String[] argv)
    {
        
        String[] a = { "--file=SSE/all.sse" } ;
        BuilderExec.main(a) ;
        System.exit(0) ;
    }
}

/*
 * (c) Copyright 2007, 2008, 2009 Hewlett-Packard Development Company, LP
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