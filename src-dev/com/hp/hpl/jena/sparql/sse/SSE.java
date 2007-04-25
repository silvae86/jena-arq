/*
 * (c) Copyright 2006, 2007 Hewlett-Packard Development Company, LP
 * All rights reserved.
 * [See end of file]
 */

package com.hp.hpl.jena.sparql.sse;

import java.io.*;

import com.hp.hpl.jena.graph.Graph;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.shared.NotFoundException;
import com.hp.hpl.jena.shared.PrefixMapping;
import com.hp.hpl.jena.sparql.ARQConstants;
import com.hp.hpl.jena.sparql.ARQException;
import com.hp.hpl.jena.sparql.algebra.Table;
import com.hp.hpl.jena.sparql.core.Quad;
import com.hp.hpl.jena.sparql.expr.Expr;
import com.hp.hpl.jena.sparql.sse.builders.*;
import com.hp.hpl.jena.sparql.sse.parser.ParseException;
import com.hp.hpl.jena.sparql.sse.parser.SSE_Parser;
import com.hp.hpl.jena.sparql.sse.parser.TokenMgrError;
import com.hp.hpl.jena.util.FileUtils;

public class SSE
{
    public static Node parseNode(String s) { return parseNode(s, null) ; }
    
    public static Node parseNode(String s, PrefixMapping pmap)
    { 
        Item item = parseResolve(s, pmap) ;
        if ( !item.isNode() )
            throw new ARQException("Not a node: "+s) ; 
        return item.getNode() ;
    }
    
    public static Quad parseQuad(String s) { return parseQuad(s, null) ; }
    
    public static Quad parseQuad(String s, PrefixMapping pmap)
    {
        Item item = parseResolve(s, pmap) ;
        if ( !item.isList() )
            throw new ARQException("Not a list: "+s) ; 
        return BuilderGraph.buildQuad(item.getList()) ;
    }

    public static Triple parseTriple(String s) { return parseTriple(s, null) ; }
    
    public static Triple parseTriple(String s, PrefixMapping pmap)
    {
        Item item = parseResolve(s, pmap) ;
        if ( !item.isList() )
            throw new ARQException("Not a list: "+s) ; 
        return BuilderGraph.buildTriple(item.getList()) ;
    }
    
    public static Expr parseExpr(String s) { return parseExpr(s, null) ; }
    
    public static Expr parseExpr(String s, PrefixMapping pmap)
    { 
        Item item = parseResolve(s, pmap) ;
        return BuilderExpr.build(item) ;
    }
    
    public static Graph readGraph(String filename) { return readGraph(filename, null) ; }
    
    public static Graph readGraph(String filename, PrefixMapping pmap)
    {
        Item item = readResolve(filename, pmap) ;
        return BuilderGraph.buildGraph(item) ;
    }
    
    public static Table readTable(String filename) { return readTable(filename, null) ; }
    
    public static Table readTable(String filename, PrefixMapping pmap)
    { 
        Item item = readResolve(filename, pmap) ;
        return BuilderTable.build(item) ;
    }
    
    public static Table parseTable(String s) { return parseTable(s, null) ; }
    
    public static Table parseTable(String s, PrefixMapping pmap)
    { 
        Item item = SSE.parseResolve(s, pmap) ;
        return BuilderTable.build(item) ;
    }
    
    public static Item parseResolve(String string)
    { return parseResolve(string, null) ; }
    
    public static Item parseResolve(String string, PrefixMapping pmap)
    {
        if ( pmap == null )
            pmap = ARQConstants.getGlobalPrefixMap() ;
        Item item = parseString(string) ;
        return ResolvePrefixedNames.resolve(item, pmap) ;
    }
    
    public static Item readResolve(String filename) { return readResolve(filename, null) ; }
    
    public static Item readResolve(String filename, PrefixMapping pmap)
    {
        Item item = SSE.readFile(filename) ;
        return ResolvePrefixedNames.resolve(item, pmap) ;
    }
    
    public static Item readFile(String filename)
    {
        try {
            InputStream in = new FileInputStream(filename) ;
            return parse(in) ;
        } 
        catch (FileNotFoundException ex)
        { throw new NotFoundException("Not found: "+filename) ; }
    }
    
    public static Item parseString(String str)
    {
        return parse(new StringReader(str)) ;
    }
    
    public static Item parse(InputStream in)
    {
        Reader reader = FileUtils.asBufferedUTF8(in) ;
        return parse(reader) ;
    }
    
    private static Item parse(Reader reader)
    {
        SSE_Parser p = new SSE_Parser(reader) ;
        try
        {
            return p.parse() ;
       } 
       catch (ParseException ex)
       { throw new SSEParseException(ex.getMessage(), ex.currentToken.beginLine, ex.currentToken.beginColumn) ; }
       catch (TokenMgrError tErr)
       { 
           // Last valid token : not the same as token error message - but this should not happen
           int col = p.token.endColumn ;
           int line = p.token.endLine ;
           throw new SSEParseException(tErr.getMessage(), line, col) ;
       }
       //catch (JenaException ex)  { throw new TurtleParseException(ex.getMessage(), ex) ; }
    }
}

/*
 * (c) Copyright 2006, 2007 Hewlett-Packard Development Company, LP
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