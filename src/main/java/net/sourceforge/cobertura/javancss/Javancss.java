/*
 * Cobertura - http://cobertura.sourceforge.net/
 *
 * This file was taken from JavaNCSS
 * http://www.kclee.com/clemens/java/javancss/
 * Copyright (C) 2000 Chr. Clemens Lee <clemens a.t kclee d.o.t com>
 *
 * Cobertura is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published
 * by the Free Software Foundation; either version 2 of the License,
 * or (at your option) any later version.
 *
 * Cobertura is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Cobertura; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
 * USA
 */


/*
 *
 * WARNING   WARNING   WARNING   WARNING   WARNING   WARNING   WARNING   WARNING   WARNING  
 *
 * WARNING TO COBERTURA DEVELOPERS
 *
 * DO NOT MODIFY THIS FILE!
 *
 * MODIFY THE FILES UNDER THE JAVANCSS DIRECTORY LOCATED AT THE ROOT OF THE COBERTURA PROJECT.
 *
 * FOLLOW THE PROCEDURE FOR MERGING THE LATEST JAVANCSS INTO COBERTURA LOCATED AT
 * javancss/coberturaREADME.txt
 *
 * WARNING   WARNING   WARNING   WARNING   WARNING   WARNING   WARNING   WARNING   WARNING   
 */

package net.sourceforge.cobertura.javancss;

import net.sourceforge.cobertura.javancss.ccl.Exitable;
import net.sourceforge.cobertura.javancss.ccl.Init;
import net.sourceforge.cobertura.javancss.ccl.Util;
import net.sourceforge.cobertura.javancss.parser.JavaParser;
import net.sourceforge.cobertura.javancss.parser.JavaParserInterface;
import net.sourceforge.cobertura.javancss.parser.ParseException;
import net.sourceforge.cobertura.javancss.parser.TokenMgrError;
import net.sourceforge.cobertura.javancss.parser.debug.JavaParserDebug;
import net.sourceforge.cobertura.javancss.parser.java15.JavaParser15;
import net.sourceforge.cobertura.javancss.parser.java15.debug.JavaParser15Debug;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * While the Java parser class might be the heart of JavaNCSS,
 * this class is the brain. This class controls input and output and
 * invokes the Java parser.
 *
 * @author    Chr. Clemens Lee <clemens@kclee.com>
 *            , recursive feature by Pääkö Hannu
 *            , additional javadoc metrics by Emilio Gongora <emilio@sms.nl>
 *            , and Guillermo Rodriguez <guille@sms.nl>.
 * @version   $Id: Javancss.java 676 2009-09-04 13:42:13Z lewijw $
 */
public class Javancss implements Exitable {

    private List<File> _vJavaSourceFiles = null;
    private String encoding = null;

    private String _sErrorMessage = null;

    private JavaParserInterface _pJavaParser = null;
    private List<FunctionMetric> _vFunctionMetrics = new ArrayList<FunctionMetric>();
    private Map<String,PackageMetric> _htPackages = null;
    private Object[] _aoPackage = null;

    private void _measureSource( File sSourceFile_ ) throws Exception
    {
        Reader reader;

        // opens the file
        try {
            reader = newReader(sSourceFile_, this.encoding);
        }
        catch ( IOException pIOException )
        {
            if ( Util.isEmpty( _sErrorMessage ) )
            {
                _sErrorMessage = "";
            }
            else
            {
                _sErrorMessage += "\n";
            }
            _sErrorMessage += "File not found: " + sSourceFile_.getAbsolutePath();

            throw pIOException;
        }

        String sTempErrorMessage = _sErrorMessage;
        try
        {
            // the same method but with a Reader
            _measureSource( reader );
        }
        catch ( Exception pParseException )
        {
            if ( sTempErrorMessage == null )
            {
                sTempErrorMessage = "";
            }
            sTempErrorMessage += "ParseException in " + sSourceFile_.getAbsolutePath() +
                   "\nLast useful checkpoint: \"" + _pJavaParser.getLastFunction() + "\"\n";
            sTempErrorMessage += pParseException.getMessage() + "\n";

            _sErrorMessage = sTempErrorMessage;

            throw pParseException;
        }
        catch ( Error pTokenMgrError )
        {
            if ( sTempErrorMessage == null )
            {
                sTempErrorMessage = "";
            }
            sTempErrorMessage += "TokenMgrError in " + sSourceFile_.getAbsolutePath() +
                   "\n" + pTokenMgrError.getMessage() + "\n";
            _sErrorMessage = sTempErrorMessage;

            throw pTokenMgrError;
        }
    }

    private void _measureSource( Reader reader ) throws Exception
    {
      Util.debug( "_measureSource(Reader).ENTER" );
      //Util.debug( "_measureSource(Reader).parser15: -->" + (_pInit.getOptions().get( "parser15" ) + "<--" );
      //Util.panicIf( _pInit == null );
      //Util.panicIf( _pInit.getOptions() == null );
      Util.debug( "_measureSource(Reader).ENTER2" );
      try
      {
        // create a parser object
        if (!Util.isDebug())
        {
          if ( _pInit == null || _pInit.getOptions() == null || _pInit.getOptions().get( "parser15" ) == null ) {
            Util.debug( "creating JavaParser" );
            _pJavaParser = new JavaParser(reader);
          } else {
            Util.debug( "creating JavaParser15" );
            _pJavaParser = new JavaParser15(reader);
          }
        } else {
          if ( _pInit == null || _pInit.getOptions() == null || _pInit.getOptions().get( "parser15" ) == null ) {
            Util.debug( "creating JavaParserDebug" );
            Util.println( "creating JavaParserDebug" );
            _pJavaParser = new JavaParserDebug(reader);
          } else {
            Util.debug( "creating JavaParser15Debug" );
            _pJavaParser = new JavaParser15Debug(reader);
          }
        }

            // execute the parser
            _pJavaParser.parse();
            Util.debug("Javancss._measureSource(DataInputStream).SUCCESSFULLY_PARSED");

            // add new data to global vector
            _vFunctionMetrics.addAll(_pJavaParser.getFunction() );
            Map<String, PackageMetric> htNewPackages = _pJavaParser.getPackage();

            /* List vNewPackages = new Vector(); */
            for (Map.Entry<String, PackageMetric> entry : htNewPackages.entrySet()) {
                String sPackage = entry.getKey();
                PackageMetric pckmNext = entry.getValue();
                pckmNext.name = sPackage;
                PackageMetric pckmPrevious = _htPackages.get(sPackage);
                pckmNext.add(pckmPrevious);
                _htPackages.put(sPackage, pckmNext);
            }
        }
        catch (Exception pParseException) {
            if (_sErrorMessage == null) {
                _sErrorMessage = "";
            }
            _sErrorMessage += "ParseException in STDIN";
            if (_pJavaParser != null) {
                _sErrorMessage += "\nLast useful checkpoint: \"" + _pJavaParser.getLastFunction() + "\"\n";
            }
            _sErrorMessage += pParseException.getMessage() + "\n";

            throw pParseException;
        }
        catch ( Error pTokenMgrError ) {
            if ( _sErrorMessage == null ) {
                _sErrorMessage = "";
            }
            _sErrorMessage += "TokenMgrError in STDIN\n";
            _sErrorMessage += pTokenMgrError.getMessage() + "\n";

            throw pTokenMgrError;
        }
    }

    private void _measureFiles(List<File> vJavaSourceFiles) throws IOException, ParseException, TokenMgrError {
        // for each file
        for (File file : vJavaSourceFiles) {

            try {
                _measureSource( file );
            }
            catch ( Throwable pThrowable ) {
                // hmm, do nothing? Use getLastError() or so to check for details.
            }
        }
    }

    /**
     * If arguments were provided, they are used, otherwise
     * the input stream is used.
     */
    private void _measureRoot( Reader reader ) throws Exception {
        _htPackages = new HashMap<String, PackageMetric>();

        // either there are argument files, or stdin is used
        if ( _vJavaSourceFiles == null ) {
            _measureSource( reader );
        }
        else {
            // the collection of files get measured
            _measureFiles( _vJavaSourceFiles );
        }
    }

    /**
     * Return info about package statement.
     * First element has name of package,
     * then begin of line, etc.
     */
    public Object[] getPackage() {
        return _aoPackage;
    }


    /*
     * cobertura:  add this next constructor so any input stream can be used.
     *
     * It should be a copy of the Javancss(String) constructor, but just
     * make sure _vJavaSourceFiles is null.   _measureRoot will
     * use the input stream if it is null.
     */
    public Javancss(Reader reader) {
        _sErrorMessage = null;
        _vJavaSourceFiles = null;

        try {
            _measureRoot(reader);
        } catch(Exception e) {
            Util.debug( "Javancss.<init>(InputStream).e: " + e );
            e.printStackTrace();
        } catch(TokenMgrError pError) {
            Util.debug( "Javancss.<init>(InputStream).pError: " + pError );
            pError.printStackTrace();
        }
    }

    /*
     * cobertura:  add this next constructor so any input stream can be used.
     * 
     * It should be a copy of the Javancss(String) constructor, but just
     * make sure _vJavaSourceFiles is null.   _measureRoot will
     * use the input stream if it is null.
     */
    public Javancss(InputStream isJavaSource_) throws UnsupportedEncodingException {
        this(newReader(isJavaSource_, null));
    }

    private Init _pInit = null;

    public List getFunctionMetrics() {
        return _vFunctionMetrics;
    }

    public String getLastErrorMessage() {
        if (_sErrorMessage == null) {
            return null;
        }
        return _sErrorMessage;
    }

    public void setExit() {}


    private static Reader newReader(InputStream stream, String encoding) throws UnsupportedEncodingException {
        return (encoding == null) ? new InputStreamReader(stream) : new InputStreamReader(stream, encoding);
    }

    private static Reader newReader(File file, String encoding) throws FileNotFoundException, UnsupportedEncodingException {
        return newReader(new FileInputStream(file), encoding);
    }
}
