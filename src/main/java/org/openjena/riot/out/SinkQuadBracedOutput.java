package org.openjena.riot.out;

import java.io.OutputStream;

import org.openjena.atlas.io.IndentedWriter;
import org.openjena.atlas.lib.Closeable;
import org.openjena.atlas.lib.Lib;
import org.openjena.atlas.lib.Sink;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.sparql.core.Quad;
import com.hp.hpl.jena.sparql.serializer.SerializationContext;
import com.hp.hpl.jena.sparql.util.FmtUtils;

/**
 * A class that print quads, SPARQL style (maybe good for Trig too?)
 */
public class SinkQuadBracedOutput implements Sink<Quad>, Closeable
{
    protected static final int BLOCK_INDENT = 2 ;
    
    protected final IndentedWriter out;
    protected final SerializationContext sCxt;
    protected boolean opened = false;
    
    protected Node currentGraph;
    
    public SinkQuadBracedOutput(OutputStream out)
    {
        this(out, null);
    }
    
    public SinkQuadBracedOutput(OutputStream out, SerializationContext sCxt)
    {
        this(new IndentedWriter(out), sCxt);
    }
    
    public SinkQuadBracedOutput(IndentedWriter out, SerializationContext sCxt)
    {
        if (out == null)
        {
            throw new IllegalArgumentException("out may not be null") ;
        }
        
        if (sCxt == null)
        {
            sCxt = new SerializationContext();
        }
        
        this.out = out;
        this.sCxt = sCxt;
    }
    
    public void open()
    {
        out.println("{");
        out.incIndent(BLOCK_INDENT);
        opened = true;
    }
    
    private void checkOpen()
    {
        if (!opened)
        {
            throw new IllegalStateException("SinkQuadBracedOutput is not opened.  Call open() first.");
        }
    }
    
    @Override
    public void send(Quad quad)
    {
        send(quad.getGraph(), quad.asTriple());
    }
    
    public void send(Node graphName, Triple triple)
    {
        checkOpen();
        if (Quad.isDefaultGraph(graphName))
        {
            graphName = null;
        }
        
        if (!Lib.equal(currentGraph, graphName))
        {
            if (null != currentGraph)
            {
                out.decIndent(BLOCK_INDENT);
                out.println("}");
            }
            
            if (null != graphName)
            {
                out.print("GRAPH ");
                output(graphName);
                out.println(" {");
                out.incIndent(BLOCK_INDENT);
            }
        }
        
        output(triple);
        out.println(" .");
        
        currentGraph = graphName;
    }
    
    private void output(Node node)
    { 
        String n = FmtUtils.stringForNode(node, sCxt);
        out.print(n);
    }
    
    private void output(Triple triple)
    {
        String s = FmtUtils.stringForNode(triple.getSubject(), sCxt) ;
        String p = FmtUtils.stringForNode(triple.getPredicate(), sCxt) ;
        String o = FmtUtils.stringForNode(triple.getObject(), sCxt) ;
        
        out.print(s);
        out.print(" ");
        out.print(p);
        out.print(" ");
        out.print(o);
    }

    @Override
    public void flush()
    {
        out.flush();
    }
    
    @Override
    public void close()
    {
        if (opened)
        {
            if (null != currentGraph)
            {
                out.decIndent(BLOCK_INDENT);
                out.println("}");
            }
            
            out.decIndent(BLOCK_INDENT);
            out.print("}");
            
            // Since we didn't create the OutputStream, we'll just flush it
            flush();
            opened = false;
        }
    }
}
