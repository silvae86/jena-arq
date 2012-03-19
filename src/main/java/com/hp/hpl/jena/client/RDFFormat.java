/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hp.hpl.jena.client ;


/**
 * Enum that specifies the various RDF formats Jena can handle. The .toString()
 * method will return a string that can be used for Jena methods requiring a <code>lang</code> string.
 */
public enum RDFFormat
{
    /** RDF/XML format */
    RDFXML   (new String[] { "RDF/XML", "rdfxml", "rdf/xml-abbrev" }, new String[] { "rdf", "xml", "owl" }),

    /** N-Triples format */
    NTRIPLES (new String[] { "N-TRIPLES", "ntriples", "nt"         }, new String[] { "nt"                }),

    /** N3 format */
    N3       (new String[] { "N3"                                  }, new String[] { "n3"                }),

    /** Turtle format */
    TURTLE   (new String[] { "TURTLE", "ttl"                       }, new String[] { "ttl"               }),

    /** Trig format */
    TRIG     (new String[] { "TURTLE"                              }, new String[] { "trig"              }),

    /** NQuads format */
    NQUADS   (new String[] { "NQUADS", "nq"                        }, new String[] { "nquads", "nq"      });
    
    // TODO Design question: should we use null or UNKNOWN for the parse* methods?
    //UNKNOWN  (new String[] { "UNKNOWN"                             }, new String[] { "unknown"           });
    
    private final String[] formatStrings;
    private final String[] fileNameExtensions;

    /**
     * @param formatStrings The format strings.  The first item is the canonical format string.
     * @param fileNameExtensions The filename extensions.  The first item is the canonical extension.
     */
    private RDFFormat(String[] formatStrings, String[] fileNameExtensions)
    {
        this.formatStrings = formatStrings;
        this.fileNameExtensions = fileNameExtensions;
    }

    /**
     * Parses a string into the proper enum type. This can handle slightly non-standard variations.
     * 
     * @param formatStr The string containing the format.
     * @return An enum corresponding to the given string.
     * @throws IllegalArgumentException if the format string does not correspond to a known RDF format
     */
    public static RDFFormat parse(String formatStr)
    {
        RDFFormat toReturn = tryParse(formatStr);
        if (null != toReturn)
        {
            return toReturn;
        }
        throw new IllegalArgumentException("Unknown RDF format: " + formatStr);
    }
    
    /**
     * Tries to parses a string into the proper enum type. This can handle slightly non-standard variations.
     * 
     * @param formatStr The string containing the format.
     * @return An enum corresponding to the given string, or null if the format string does not correspond to a known RDF format
     */
    public static RDFFormat tryParse(String formatStr)
    {
        if (null != formatStr)
        {
            for (RDFFormat f : RDFFormat.values())
            {
                for (String s : f.formatStrings)
                {
                    if (s.equalsIgnoreCase(formatStr))
                    {
                        return f;
                    }
                }
            }
        }
        return null;
    }
    
    /**
     * Parses a given filename, examines its extension and returns an RDFFormat
     * that matches that extension.
     * 
     * @param filename The filename to parse
     * @return The RDFFormat that matches the filename extension
     * @throws IllegalArgumentException if the filename does not have an extension that corresponds to a known RDF format
     */
    public static RDFFormat parseFilename(String filename)
    {
        RDFFormat toReturn = tryParseFilename(filename);
        if (null != toReturn)
        {
            return toReturn;
        }
        throw new IllegalArgumentException("Unknown RDF extension for file: " + filename);
    }
    
    /**
     * Tries to parses a given filename, examines its extension and returns an RDFFormat
     * that matches that extension.
     * 
     * @param filename The filename to parse
     * @return The RDFFormat that matches the filename extension, or null if the filename does not have an extension that corresponds to a known RDF format
     */
    public static RDFFormat tryParseFilename(String filename)
    {
        if (null != filename)
        {
            int dotIndex = filename.lastIndexOf('.') ;
            if ((dotIndex >= 0) && (dotIndex + 1 < filename.length()))
            {
                return tryParseExtension(filename.substring(dotIndex + 1));
            }
        }
        return null;
    }

    /**
     * Parses a given extension string and returns the proper RDFFormat.
     * 
     * @param extensionStr The extension string to parse
     * @return The RDFFormat that matches the given extension string
     * @throws IllegalArgumentException if the extension does not correspond to a known RDF format
     */
    public static RDFFormat parseExtension(String extensionStr)
    {
        RDFFormat toReturn = tryParseExtension(extensionStr);
        if (null != toReturn)
        {
            return toReturn;
        }
        throw new IllegalArgumentException("Unknown RDF extension: " + extensionStr);
    }
    
    /**
     * Tries to parse a given extension string and return the proper RDFFormat.
     * 
     * @param extensionStr The extension string to parse
     * @return The RDFFormat that matches the given extension string, or null if the extension does not correspond to a known RDF format
     */
    public static RDFFormat tryParseExtension(String extensionStr)
    {
        if (null != extensionStr)
        {
            for (RDFFormat f : RDFFormat.values())
            {
                for (String s : f.fileNameExtensions)
                {
                    if (s.equalsIgnoreCase(extensionStr))
                    {
                        return f;
                    }
                }
            }
        }
        return null;
    }

    /**
     * Returns a filename extension string that matches this RDFFormat.
     * 
     * @return Returns a filename extension string that matches this RDFFormat
     */
    public String getExtension()
    {
        return this.fileNameExtensions[0];
    }
    
    /**
     * Returns a String that is suitable for passing into Jena methods that call
     * for a <code>lang</code> parameter.
     */
    public String getFormatString()
    {
        return this.formatStrings[0];
    }
    
    /**
     * Returns a String that is suitable for passing into Jena methods that call
     * for a <code>lang</code> parameter.
     */
    @Override
    public String toString()
    {
        return getFormatString();
    }

}
