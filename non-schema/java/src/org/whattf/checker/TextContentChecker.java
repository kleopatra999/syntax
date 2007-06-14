/*
 * Copyright (c) 2006 Henri Sivonen
 *
 * Permission is hereby granted, free of charge, to any person obtaining a 
 * copy of this software and associated documentation files (the "Software"), 
 * to deal in the Software without restriction, including without limitation 
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, 
 * and/or sell copies of the Software, and to permit persons to whom the 
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in 
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR 
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, 
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL 
 * THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER 
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING 
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER 
 * DEALINGS IN THE SOFTWARE.
 */

package org.whattf.checker;

import java.util.LinkedList;

import org.relaxng.datatype.DatatypeException;
import org.relaxng.datatype.DatatypeStreamingValidator;
import org.whattf.datatype.DateOrTimeContent;
import org.whattf.datatype.Ratio;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

/**
 * Checks the <code>textContent</code> of elements whose
 * <code>textContent</code> need special non-schema treatment. To smooth code
 * reuse between a conformance checker and editors that only allow RELAX NG plus
 * custom datatypes, this class uses objects that implement
 * <code>DatatypeStreamingValidator</code>.
 * 
 * <p>
 * Examples of elements handled by this class are <code>time</code>,
 * <code>meter</code> and <code>progress</code>.
 * 
 * @version $Id$
 * @author hsivonen
 */
public final class TextContentChecker extends Checker {

    /**
     * The stack of <code>DatatypeStreamingValidator</code>s corresponding to
     * open elements. Stack entry is <code>null</code> if the corresponding
     * element does not need <code>textContent</code> checking. Grows from the 
     * tail.
     */
    private final LinkedList<DatatypeStreamingValidator> stack = new LinkedList<DatatypeStreamingValidator>();

    /**
     * Constructor.
     */
    public TextContentChecker() {
        super();
    }

    /**
     * Returns a <code>DatatypeStreamingValidator</code> for the element if it 
     * needs <code>textContent</code> checking or <code>null</code> if it does 
     * not.
     * 
     * @param uri the namespace URI of the element
     * @param localName the local name of the element
     * @param atts the attributes
     * @return a <code>DatatypeStreamingValidator</code> or <code>null</code> if 
     * checks not necessary
     */
    private DatatypeStreamingValidator streamingValidatorFor(String uri,
            String localName, Attributes atts) {
        if ("http://www.w3.org/1999/xhtml".equals(uri)) {
            if ("meter".equals(localName) || "progress".equals(localName)) {
                if (atts.getIndex("", "value") < 0) {
                    return Ratio.THE_INSTANCE.createStreamingValidator(null);
                }
            } else if ("time".equals(localName)) {
                if (atts.getIndex("", "datetime") < 0) {
                    return DateOrTimeContent.THE_INSTANCE.createStreamingValidator(null);
                }
            }
        }
        return null;
    }

    /**
     * @see org.whattf.checker.Checker#characters(char[], int, int)
     */
    public void characters(char[] ch, int start, int length)
            throws SAXException {
        for (DatatypeStreamingValidator dsv : stack) {
            if (dsv != null) {
                dsv.addCharacters(ch, start, length);
            }
        }
    }

    /**
     * @see org.whattf.checker.Checker#endElement(java.lang.String,
     *      java.lang.String, java.lang.String)
     */
    public void endElement(String uri, String localName, String qName)
            throws SAXException {
        DatatypeStreamingValidator dsv = stack.removeLast();
        if (dsv != null) {
            try {
                dsv.checkValid();
            } catch (DatatypeException e) {
                String msg = e.getMessage();
                if (msg == null) {
                    err("The text content of element \u201C" + localName
                            + "\u201D from namespace \u201C" + uri
                            + "\u201D was not in the required format.");
                } else {
                    err("The text content of element \u201C" + localName
                            + "\u201D from namespace \u201C" + uri
                            + "\u201D was not in the required format: " + msg);
                }
            }
        }
    }

    /**
     * @see org.whattf.checker.Checker#startElement(java.lang.String,
     *      java.lang.String, java.lang.String, org.xml.sax.Attributes)
     */
    public void startElement(String uri, String localName, String qName,
            Attributes atts) throws SAXException {
        stack.addLast(streamingValidatorFor(uri, localName, atts));
    }

    /**
     * @see org.whattf.checker.Checker#reset()
     */
    public void reset() {
        stack.clear();
    }

}