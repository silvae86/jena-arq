/*
 * (c) Copyright 2007, 2008 Hewlett-Packard Development Company, LP
 * All rights reserved.
 * [See end of file]
 */

package com.hp.hpl.jena.sparql.modify.lang.parser;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import com.hp.hpl.jena.graph.Graph;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.sparql.modify.lang.ParserUpdateBase;
import com.hp.hpl.jena.sparql.syntax.Template;
import com.hp.hpl.jena.sparql.syntax.TemplateGroup;
import com.hp.hpl.jena.sparql.syntax.TemplateTriple;
import com.hp.hpl.jena.sparql.syntax.TemplateVisitor;
import com.hp.hpl.jena.sparql.util.graph.GraphUtils;
import com.hp.hpl.jena.update.UpdateRequest;

public class SPARQLUpdateParserBase
    extends ParserUpdateBase
    implements SPARQLUpdateParserConstants
{
    private UpdateRequest request = null ;

    public void setUpdateRequest(UpdateRequest request)
    {
        setPrologue(request) ;
        this.request = request ;
    }
    
    protected UpdateRequest getRequest() { return request ; }
    
    
    static class TriplesCollector implements TemplateVisitor
    {
        private Collection acc ;
        private int line ;
        private int col ;

        TriplesCollector(Collection acc, int line, int col)
        { 
            this.acc = acc ;
            this.line = line ;
            this.col = col ;
        }
            
        public void visit(TemplateTriple template)
        {
            Triple t = template.getTriple() ;
            if ( t.getSubject().isVariable() ||
                t.getPredicate().isVariable() ||
                t.getObject().isVariable() )
            {
                throwParseException("Triples may not contain variables in ADD or REMOVE", line, col) ;
            }
            acc.add(t) ;
        }

        public void visit(TemplateGroup template)
        {
            for ( Iterator iter = template.getTemplates().iterator() ; iter.hasNext(); )
            {
                Template t = (Template)iter.next();
                t.visit(this) ;
            }
        }
        
    }
    
    protected Graph convertTemplateToTriples(Template template, int line, int col)
    {
        List acc = new ArrayList() ;
        TriplesCollector collector = new TriplesCollector(acc, line, col) ;
        template.visit(collector) ;
        Graph g = GraphUtils.makePlainGraph() ;
        g.getBulkUpdateHandler().add(acc) ;
        return g ;
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