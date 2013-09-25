/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2002-2003 The Apache Software Foundation.  All rights
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution, if
 *    any, must include the following acknowlegement:
 *       "This product includes software developed by the
 *        Apache Software Foundation (http://www.codehaus.org/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 *
 * 4. The names "Ant" and "Apache Software
 *    Foundation" must not be used to endorse or promote products derived
 *    from this software without prior written permission. For written
 *    permission, please contact codehaus@codehaus.org.
 *
 * 5. Products derived from this software may not be called "Apache"
 *    nor may "Apache" appear in their names without prior written
 *    permission of the Apache Group.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE APACHE SOFTWARE FOUNDATION OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.codehaus.org/>.
 */

package org.codehaus.plexus.util;

import java.io.File;
import java.util.StringTokenizer;
import java.util.Vector;

/**
 * <p>This is a utility class used by selectors and DirectoryScanner. The
 * functionality more properly belongs just to selectors, but unfortunately
 * DirectoryScanner exposed these as protected methods. Thus we have to
 * support any subclasses of DirectoryScanner that may access these methods.
 * </p>
 * <p>This is a Singleton.</p>
 *
 * @author Arnout J. Kuiper
 * <a href="mailto:ajkuiper@wxs.nl">ajkuiper@wxs.nl</a>
 * @author Magesh Umasankar
 * @author <a href="mailto:bruce@callenish.com">Bruce Atherton</a>
 * @since 1.5
 * @version $Id$
 */
public final class SelectorUtils
{

    public static final String PATTERN_HANDLER_PREFIX = "[";
    
    public static final String PATTERN_HANDLER_SUFFIX = "]";
    
    public static final String REGEX_HANDLER_PREFIX = "%regex" + PATTERN_HANDLER_PREFIX;
    
    public static final String ANT_HANDLER_PREFIX = "%ant" + PATTERN_HANDLER_PREFIX;
    
    /**
     * Private Constructor
     */
    private SelectorUtils()
    {
    }

    /**
     * Tests whether or not a given path matches a given pattern.
     *
     * @param pattern The pattern to match against. Must not be
     *                <code>null</code>.
     * @param str     The path to match, as a String. Must not be
     *                <code>null</code>.
     *
     * @return <code>true</code> if the pattern matches against the string,
     *         or <code>false</code> otherwise.
     */
    public static boolean matchPath( String pattern, String str )
    {
        return matchPath( pattern, str, true );
    }

    /**
     * Tests whether or not a given path matches a given pattern.
     *
     * @param pattern The pattern to match against. Must not be
     *                <code>null</code>.
     * @param str     The path to match, as a String. Must not be
     *                <code>null</code>.
     * @param isCaseSensitive Whether or not matching should be performed
     *                        case sensitively.
     *
     * @return <code>true</code> if the pattern matches against the string,
     *         or <code>false</code> otherwise.
     */
    public static boolean matchPath( String pattern, String str,
                                     boolean isCaseSensitive )
    {
        return matchPath( pattern, str, File.separator, isCaseSensitive );
    }

    public static boolean matchPath( String pattern, String str, String separator, boolean isCaseSensitive )
    {
        if ( pattern.length() > ( REGEX_HANDLER_PREFIX.length() + PATTERN_HANDLER_SUFFIX.length() + 1 )
            && pattern.startsWith( REGEX_HANDLER_PREFIX ) && pattern.endsWith( PATTERN_HANDLER_SUFFIX ) )
        {
            pattern = pattern.substring( REGEX_HANDLER_PREFIX.length(), pattern.length()
                                         - PATTERN_HANDLER_SUFFIX.length() );

            return str.matches( pattern );
        }
        else
        {
            if ( pattern.length() > ( ANT_HANDLER_PREFIX.length() + PATTERN_HANDLER_SUFFIX.length() + 1 )
                && pattern.startsWith( ANT_HANDLER_PREFIX ) && pattern.endsWith( PATTERN_HANDLER_SUFFIX ) )
            {
                pattern =
                    pattern.substring( ANT_HANDLER_PREFIX.length(), pattern.length() - PATTERN_HANDLER_SUFFIX.length() );
            }
            
            return matchAntPathPattern( pattern, str, separator, isCaseSensitive );
        }
    }

    private static boolean matchAntPathPattern( String pattern, String str, String separator, boolean isCaseSensitive )
    {
        // When str starts with a separator, pattern has to start with a
        // separator.
        // When pattern starts with a separator, str has to start with a
        // separator.
        if ( str.startsWith( separator ) !=
            pattern.startsWith( separator ) )
        {
            return false;
        }

        Vector<String> patDirs = tokenizePath( pattern, separator );
        Vector<String> strDirs = tokenizePath( str, separator );

        int patIdxStart = 0;
        int patIdxEnd = patDirs.size() - 1;
        int strIdxStart = 0;
        int strIdxEnd = strDirs.size() - 1;

        // up to first '**'
        while ( patIdxStart <= patIdxEnd && strIdxStart <= strIdxEnd )
        {
            String patDir = (String) patDirs.elementAt( patIdxStart );
            if ( patDir.equals( "**" ) )
            {
                break;
            }
            if ( !match( patDir, (String) strDirs.elementAt( strIdxStart ),
                         isCaseSensitive ) )
            {
                patDirs = null;
                strDirs = null;
                return false;
            }
            patIdxStart++;
            strIdxStart++;
        }
        if ( strIdxStart > strIdxEnd )
        {
            // String is exhausted
            for ( int i = patIdxStart; i <= patIdxEnd; i++ )
            {
                if ( !patDirs.elementAt( i ).equals( "**" ) )
                {
                    patDirs = null;
                    strDirs = null;
                    return false;
                }
            }
            return true;
        }
        else
        {
            if ( patIdxStart > patIdxEnd )
            {
                // String not exhausted, but pattern is. Failure.
                patDirs = null;
                strDirs = null;
                return false;
            }
        }

        // up to last '**'
        while ( patIdxStart <= patIdxEnd && strIdxStart <= strIdxEnd )
        {
            String patDir = (String) patDirs.elementAt( patIdxEnd );
            if ( patDir.equals( "**" ) )
            {
                break;
            }
            if ( !match( patDir, (String) strDirs.elementAt( strIdxEnd ),
                         isCaseSensitive ) )
            {
                patDirs = null;
                strDirs = null;
                return false;
            }
            patIdxEnd--;
            strIdxEnd--;
        }
        if ( strIdxStart > strIdxEnd )
        {
            // String is exhausted
            for ( int i = patIdxStart; i <= patIdxEnd; i++ )
            {
                if ( !patDirs.elementAt( i ).equals( "**" ) )
                {
                    patDirs = null;
                    strDirs = null;
                    return false;
                }
            }
            return true;
        }

        while ( patIdxStart != patIdxEnd && strIdxStart <= strIdxEnd )
        {
            int patIdxTmp = -1;
            for ( int i = patIdxStart + 1; i <= patIdxEnd; i++ )
            {
                if ( patDirs.elementAt( i ).equals( "**" ) )
                {
                    patIdxTmp = i;
                    break;
                }
            }
            if ( patIdxTmp == patIdxStart + 1 )
            {
                // '**/**' situation, so skip one
                patIdxStart++;
                continue;
            }
            // Find the pattern between padIdxStart & padIdxTmp in str between
            // strIdxStart & strIdxEnd
            int patLength = ( patIdxTmp - patIdxStart - 1 );
            int strLength = ( strIdxEnd - strIdxStart + 1 );
            int foundIdx = -1;
            strLoop:
                        for ( int i = 0; i <= strLength - patLength; i++ )
                        {
                            for ( int j = 0; j < patLength; j++ )
                            {
                                String subPat = (String) patDirs.elementAt( patIdxStart + j + 1 );
                                String subStr = (String) strDirs.elementAt( strIdxStart + i + j );
                                if ( !match( subPat, subStr, isCaseSensitive ) )
                                {
                                    continue strLoop;
                                }
                            }

                            foundIdx = strIdxStart + i;
                            break;
                        }

            if ( foundIdx == -1 )
            {
                patDirs = null;
                strDirs = null;
                return false;
            }

            patIdxStart = patIdxTmp;
            strIdxStart = foundIdx + patLength;
        }

        for ( int i = patIdxStart; i <= patIdxEnd; i++ )
        {
            if ( !patDirs.elementAt( i ).equals( "**" ) )
            {
                patDirs = null;
                strDirs = null;
                return false;
            }
        }

        return true;
    }

    /**
     * Tests whether or not a string matches against a pattern.
     * The pattern may contain two special characters:<br>
     * '*' means zero or more characters<br>
     * '?' means one and only one character
     *
     * @param pattern The pattern to match against.
     *                Must not be <code>null</code>.
     * @param str     The string which must be matched against the pattern.
     *                Must not be <code>null</code>.
     *
     * @return <code>true</code> if the string matches against the pattern,
     *         or <code>false</code> otherwise.
     */
    public static boolean match( String pattern, String str )
    {
        return match( pattern, str, true );
    }

    /**
     * Tests whether or not a string matches against a pattern.
     * The pattern may contain two special characters:<br>
     * '*' means zero or more characters<br>
     * '?' means one and only one character
     *
     * @param pattern The pattern to match against.
     *                Must not be <code>null</code>.
     * @param str     The string which must be matched against the pattern.
     *                Must not be <code>null</code>.
     * @param isCaseSensitive Whether or not matching should be performed
     *                        case sensitively.
     *
     *
     * @return <code>true</code> if the string matches against the pattern,
     *         or <code>false</code> otherwise.
     */
    public static boolean match( String pattern, String str,
                                 boolean isCaseSensitive )
    {
        char[] patArr = pattern.toCharArray();
        char[] strArr = str.toCharArray();
        int patIdxStart = 0;
        int patIdxEnd = patArr.length - 1;
        int strIdxStart = 0;
        int strIdxEnd = strArr.length - 1;
        char ch;

        boolean containsStar = false;
        for ( int i = 0; i < patArr.length; i++ )
        {
            if ( patArr[i] == '*' )
            {
                containsStar = true;
                break;
            }
        }

        if ( !containsStar )
        {
            // No '*'s, so we make a shortcut
            if ( patIdxEnd != strIdxEnd )
            {
                return false; // Pattern and string do not have the same size
            }
            for ( int i = 0; i <= patIdxEnd; i++ )
            {
                ch = patArr[i];
                if ( ch != '?' && !equals( ch, strArr[i], isCaseSensitive ) )
                {
                    return false; // Character mismatch
                }
            }
            return true; // String matches against pattern
        }

        if ( patIdxEnd == 0 )
        {
            return true; // Pattern contains only '*', which matches anything
        }

        // Process characters before first star
        while ( ( ch = patArr[patIdxStart] ) != '*' && strIdxStart <= strIdxEnd )
        {
            if ( ch != '?' && !equals( ch, strArr[strIdxStart], isCaseSensitive ) )
            {
                return false; // Character mismatch
            }
            patIdxStart++;
            strIdxStart++;
        }
        if ( strIdxStart > strIdxEnd )
        {
            // All characters in the string are used. Check if only '*'s are
            // left in the pattern. If so, we succeeded. Otherwise failure.
            for ( int i = patIdxStart; i <= patIdxEnd; i++ )
            {
                if ( patArr[i] != '*' )
                {
                    return false;
                }
            }
            return true;
        }

        // Process characters after last star
        while ( ( ch = patArr[patIdxEnd] ) != '*' && strIdxStart <= strIdxEnd )
        {
            if ( ch != '?' && !equals( ch, strArr[strIdxEnd], isCaseSensitive ) )
            {
                return false; // Character mismatch
            }
            patIdxEnd--;
            strIdxEnd--;
        }
        if ( strIdxStart > strIdxEnd )
        {
            // All characters in the string are used. Check if only '*'s are
            // left in the pattern. If so, we succeeded. Otherwise failure.
            for ( int i = patIdxStart; i <= patIdxEnd; i++ )
            {
                if ( patArr[i] != '*' )
                {
                    return false;
                }
            }
            return true;
        }

        // process pattern between stars. padIdxStart and patIdxEnd point
        // always to a '*'.
        while ( patIdxStart != patIdxEnd && strIdxStart <= strIdxEnd )
        {
            int patIdxTmp = -1;
            for ( int i = patIdxStart + 1; i <= patIdxEnd; i++ )
            {
                if ( patArr[i] == '*' )
                {
                    patIdxTmp = i;
                    break;
                }
            }
            if ( patIdxTmp == patIdxStart + 1 )
            {
                // Two stars next to each other, skip the first one.
                patIdxStart++;
                continue;
            }
            // Find the pattern between padIdxStart & padIdxTmp in str between
            // strIdxStart & strIdxEnd
            int patLength = ( patIdxTmp - patIdxStart - 1 );
            int strLength = ( strIdxEnd - strIdxStart + 1 );
            int foundIdx = -1;
            strLoop:
            for ( int i = 0; i <= strLength - patLength; i++ )
            {
                for ( int j = 0; j < patLength; j++ )
                {
                    ch = patArr[patIdxStart + j + 1];
                    if ( ch != '?' && !equals( ch, strArr[strIdxStart + i + j], isCaseSensitive ) )
                    {
                        continue strLoop;
                    }
                }

                foundIdx = strIdxStart + i;
                break;
            }

            if ( foundIdx == -1 )
            {
                return false;
            }

            patIdxStart = patIdxTmp;
            strIdxStart = foundIdx + patLength;
        }

        // All characters in the string are used. Check if only '*'s are left
        // in the pattern. If so, we succeeded. Otherwise failure.
        for ( int i = patIdxStart; i <= patIdxEnd; i++ )
        {
            if ( patArr[i] != '*' )
            {
                return false;
            }
        }
        return true;
    }

    /**
     * Tests whether two characters are equal.
     */
    private static boolean equals( char c1, char c2, boolean isCaseSensitive )
    {
        if ( c1 == c2 )
        {
            return true;
        }
        if ( !isCaseSensitive )
        {
            // NOTE: Try both upper case and lower case as done by String.equalsIgnoreCase()
            if ( Character.toUpperCase( c1 ) == Character.toUpperCase( c2 ) ||
                Character.toLowerCase( c1 ) == Character.toLowerCase( c2 ) )
            {
                return true;
            }
        }
        return false;
    }

    /**
     * Breaks a path up into a Vector<String> of path elements, tokenizing on
     * <code>File.separator</code>.
     *
     * @param path Path to tokenize. Must not be <code>null</code>.
     *
     * @return a Vector<String> of path elements from the tokenized path
     */
    public static Vector<String> tokenizePath( String path )
    {
        return tokenizePath( path, File.separator );
    }
    
    public static Vector<String> tokenizePath( String path, String separator )
    {
        Vector<String> ret = new Vector<String>();
        StringTokenizer st = new StringTokenizer( path, separator );
        while ( st.hasMoreTokens() )
        {
            ret.addElement( st.nextToken() );
        }
        return ret;
    }
}
