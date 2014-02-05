/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package jp.co.antenna.XfoJavaCtl;

import java.io.*;
import java.util.*;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.stream.StreamSource;
import javax.xml.transform.stream.StreamResult;

/**
 * XfoObj Class is the object class of XSL Formatter
 *
 * @author Test User
 */
public class XfoObj {
    // Consts
    public static final int EST_NONE = 0;
    public static final int EST_STDOUT = 1;
    public static final int EST_STDERR = 2;
    private static final String[] AH_HOME_ENV = {
        "AHF61_64_HOME", "AHF61_HOME",
		"AHF60_64_HOME", "AHF60_HOME",
		"AHF53_64_HOME", "AHF53_HOME",
		"AHF52_64_HOME", "AHF52_HOME",
		"AHF51_64_HOME", "AHF51_HOME",
		"AHF50_64_HOME", "AHF50_HOME",
		"AXF43_64_HOME", "AXF43_HOME",
		"AXF42_HOME",
		"AXF41_HOME",
		"AXF4_HOME"};

	private static final HashMap<String, Integer> AH_VER_MAP;
	static {
		Integer zero = new Integer(0);
		AH_VER_MAP = new HashMap<String, Integer>();
		AH_VER_MAP.put("AXF43_64_HOME", zero);
		AH_VER_MAP.put("AXF43_HOME", zero);
		AH_VER_MAP.put("AXF42_HOME", zero);
		AH_VER_MAP.put("AXF41_HOME", zero);
		AH_VER_MAP.put("AXF4_HOME", zero);
	}
    
    public static final int S_PDF_EMBALLFONT_PART = 0;
    public static final int S_PDF_EMBALLFONT_ALL = 1;
    public static final int S_PDF_EMBALLFONT_BASE14 = 2;

    public static final int S_FORMATTERTYPE_AUTO = 0;
    public static final int S_FORMATTERTYPE_HTML = 1;
    public static final int S_FORMATTERTYPE_XHTML = 2;
    public static final int S_FORMATTERTYPE_XMLCSS = 3;
    public static final int S_FORMATTERTYPE_XSLFO = 4;

    public static final int S_PDF_VERSION_13 = 0;
    public static final int S_PDF_VERSION_14 = 1;
    public static final int S_PDF_VERSION_15 = 2;
    public static final int S_PDF_VERSION_16 = 3;
    public static final int S_PDF_VERSION_17 = 4;
    public static final int S_PDF_VERSION_A_1a_2005 = 200;
    public static final int S_PDF_VERSION_A_1b_2005 = 400;
    public static final int S_PDF_VERSION_X_1a_2001 = 101;
    public static final int S_PDF_VERSION_X_1a_2003 = 104;
    public static final int S_PDF_VERSION_X_2_2003 = 105;
    public static final int S_PDF_VERSION_X_3_2002 = 103;
    public static final int S_PDF_VERSION_X_3_2003 = 106;
	
    // Attributes
    private String executable;
    private Runtime r;
    private MessageListener messageListener;
    private LinkedHashMap<String, String> args;
	private ArrayList<String> userCSS;
	private XfoException lastError;
    
    // Methods
    /**
     * Create the instance of XfoObj, and initialize it.
     * 
     * @throws XfoException
     */
    public XfoObj () throws XfoException {
        // Check EVs and test if XslCmd.exe exists.
		String os;
		try {
			os = System.getProperty("os.name");
			if ((os == null) || os.equals(""))
				throw new Exception();
		} catch (Exception e) {
			throw new XfoException(4, 0, "Could not determine OS");
		}
		String axf_home;
		axf_home = System.getProperty("axf.home");
		int axf_ver = 1;
		if ((axf_home == null) || axf_home.equals("")) {
			try {
				Map<String, String> env = System.getenv();
				for (String key: AH_HOME_ENV) {
					if (env.containsKey(key)) {
						axf_home = env.get(key);
						if (AH_VER_MAP.containsKey(key)) {
							axf_ver = AH_VER_MAP.get(key).intValue();
						}
						break;
					}
				}
				if ((axf_home == null) || axf_home.equals(""))
					throw new Exception();
			} catch (Exception e) {
				throw new XfoException(4, 1, "Could not locate Formatter's environment variables");
			}
		}
		String separator = System.getProperty("file.separator");
		this.executable = axf_home + separator;
		if (System.getProperty("axf.bin") == null) {
			if (os.equals("Linux") || os.equals("SunOS") || os.equals("AIX") || os.equals("Mac OS X")) {
				if (axf_ver == 0)
					this.executable += "bin" + separator + "XSLCmd";
				else
					this.executable += "bin" + separator + "AHFCmd";
			}
			else if (os.contains("Windows")) {
				if (axf_ver == 0)
					this.executable += "XSLCmd.exe";
				else
					this.executable += "AHFCmd.exe";
			}
			else
				throw new XfoException(4, 2, "Unsupported OS: " + os);
		}
		else {
			this.executable += System.getProperty("axf.bin");
		}
        // setup attributes
        this.clear();
    }
    
    /**
     * Cleanup (initialize) XSL Formatter engine.
     */
    public void clear () {
        // reset attributes        
        this.r = Runtime.getRuntime();
        this.args = new LinkedHashMap<String, String>();
        this.messageListener = null;
	this.userCSS = new ArrayList<String>();
	this.lastError = null;
    }
    
    /**
     * Execute formatting and outputs to a PDF. 
     * 
     * @throws jp.co.antenna.XfoJavaCtl.XfoException
     */
    public void execute () throws XfoException {
		ArrayList<String> cmdArray = new ArrayList<String>();
		cmdArray.add(this.executable);
		for (String arg : this.args.keySet()) {
			cmdArray.add(arg);
			if (this.args.get(arg) != null)
				cmdArray.add(this.args.get(arg));
		}
		for (String css : this.userCSS) {
			cmdArray.add("-css");
			cmdArray.add(css);
		}
        // Run Formatter with Runtime.exec()
        Process process;
        ErrorParser errorParser = null;
	StreamFlusher outputFlush = null;
        int exitCode = -1;
        try {
	    String[] s = new String[0];
	    try {
		process = this.r.exec(cmdArray.toArray(s));
	    } catch (IOException ioex) {
		System.err.println("couldn't invoke axfo: " + ioex.getMessage());
		return;
	    }
	    try {
		InputStream StdErr = process.getErrorStream();
		InputStream StdOut = process.getInputStream();
		errorParser = new ErrorParser(StdErr, this.messageListener);
		errorParser.start();
		outputFlush = new StreamFlusher(StdOut);
		outputFlush.start();
	    } catch (Exception e) {
		System.err.println("Exception getting streams: " + e.getMessage());
	    }
	    try {
		exitCode = process.waitFor();
	    } catch (InterruptedException e) {
		System.err.println("InterruptedException waiting for axfo to finish: " + e.getMessage());
	    }
        } catch (Exception e) {
	    System.err.println("Exception waiting for axfo to finish: " + e.getMessage());
	}

	if (outputFlush != null) {
	    try {
		outputFlush.join();
	    } catch (InterruptedException e) {
		System.err.println("Exception output flush: " + e.getMessage());
	    }
	}

	if (errorParser != null) {
	    try {
		errorParser.join();
	    } catch (InterruptedException e) {
		System.err.println("Exception joining error parser: " + e.getMessage());
	    }
	}

        if (exitCode != 0) {
            if (errorParser != null && errorParser.LastErrorCode != 0) {
                this.lastError = new XfoException(errorParser.LastErrorLevel, errorParser.LastErrorCode, errorParser.LastErrorMessage);
				throw this.lastError;
            } else {
                throw new XfoException(4, 0, "Failed to parse last error. Exit code: " + exitCode);
            }
        }
    }

	public int getErrorCode () throws XfoException {
		if (this.lastError == null)
			return 0;
		else
			return this.lastError.getErrorCode();
	}

	public int getErrorLevel () throws XfoException {
		if (this.lastError == null)
			return 0;
		else
			return this.lastError.getErrorLevel();
	}

	public String getErrorMessage () throws XfoException {
		if (this.lastError == null)
			return null;
		else
			return this.lastError.getErrorMessage();
	}
    
    public int getExitLevel () throws XfoException {
        String opt = "-extlevel";
        if (this.args.containsKey(opt))
            return Integer.parseInt(this.args.get(opt));
        return 2;  // default exit level
    }

	public void releaseObject () {
		this.clear();
	}

    public void releaseObjectEx () throws XfoException {
        releaseObject();
    }
    
    /**
     * Executes the formatting of XSL-FO document specified for src, and outputs it to dst in the output form specified for dst.
     * 
     * @param src   XSL-FO Document
     * @param dst   output stream
     * @param outDevice output device. Please refer to a setPrinterName method about the character string to specify. 
     * @throws jp.co.antenna.XfoJavaCtl.XfoException
     */
    public void render (InputStream src, OutputStream dst, String outDevice) throws XfoException {
		ArrayList<String> cmdArray = new ArrayList<String>();
		cmdArray.add(this.executable);
		for (String arg : this.args.keySet()) {
			cmdArray.add(arg);
			if (this.args.get(arg) != null)
				cmdArray.add(this.args.get(arg));
		}
		for (String css : this.userCSS) {
			cmdArray.add("-css");
			cmdArray.add(css);
		}
		cmdArray.add("-d");
		cmdArray.add("@STDIN");
		cmdArray.add("-o");
		cmdArray.add("@STDOUT");
		cmdArray.add("-p");
		cmdArray.add(outDevice);

		Process process;
		ErrorParser errorParser = null;
		StreamCopyThread scInput = null;
		StreamCopyThread scOutput = null;

		int exitCode = -1;

		try {
			String[] s = new String[0];
			try {
			    process = this.r.exec(cmdArray.toArray(s));
			} catch (IOException ioex) {
			    System.err.println("render() couldn't invoke axfo: " + ioex.getMessage());
			    return;
			}
			try {
				InputStream StdErr = process.getErrorStream();
				errorParser = new ErrorParser(StdErr, this.messageListener);
				errorParser.start();

				scInput = new StreamCopyThread(process.getInputStream(), dst);
				scInput.start();
				scOutput = new StreamCopyThread(src, process.getOutputStream());
				scOutput.start();

			} catch (Exception e) {
			    System.err.println("Exception creating threads in render(): " + e.getMessage());
			    e.printStackTrace();
			}
			exitCode = process.waitFor();
		} catch (Exception e) {
		    System.err.println("Exception in render(): " + e.getMessage());
		    e.printStackTrace();
		}

		if (scInput != null) {
		    try {
			scInput.join();
		    } catch (InterruptedException e) {
			System.err.println("Exception joining render input: " + e.getMessage());
		    }
		}

		if (scOutput != null) {
		    try {
			scOutput.join();
		    } catch (InterruptedException e) {
			System.err.println("Exception joining render output: " + e.getMessage());
		    }
		}

		if (errorParser != null) {
		    try {
			errorParser.join();
		    } catch (InterruptedException e) {
			System.err.println("Exception joining error parser: " + e.getMessage());
		    }
		}

		if (exitCode != 0) {
			if (errorParser != null && errorParser.LastErrorCode != 0) {
				this.lastError = new XfoException(errorParser.LastErrorLevel, errorParser.LastErrorCode, errorParser.LastErrorMessage);
				throw this.lastError;
			} else {
				throw new XfoException(4, 0, "Failed to parse last error. Exit code: " + exitCode + " render 2");
			}
		}
    }

	/**
	 * Transforms an XML document specified to xmlSrc using an XSL stylesheet specified to xslSrc. 
	 * Then executes the formatting of XSL-FO document and outputs it to dst in the output form specified for outDevice. 
	 * Xalan of JAXP (Java API for XML Processing) is used for the XSLT conversion. The setExternalXSLT method and 
	 * the setting of XSLT processor in the option setting file is disregarded.
	 *
	 * @param xmlSrc XML Document
	 * @param xslSrc XSL Document
	 * @param dst output stream
	 * @param outDevice output device. Please refer to a setPrinterName method about the character string to specify.
	 * @throws jp.co.antenna.XfoJavaCtl.XfoException
	 */
	public void render(InputStream xmlSrc, InputStream xslSrc, OutputStream dst, String outDevice) throws XfoException {
		try {
			ByteArrayOutputStream baosFO = new ByteArrayOutputStream();
			TransformerFactory transFactory = TransformerFactory.newInstance();
			Transformer transformer = transFactory.newTransformer(new StreamSource(xslSrc));
			transformer.transform(new StreamSource(xmlSrc), new StreamResult(baosFO));
			ByteArrayInputStream baisFO = new ByteArrayInputStream(baosFO.toByteArray());
			this.render(baisFO, dst, outDevice);
		} catch (XfoException xfoe) {
			throw xfoe;
		} catch (Exception e) {
			throw new XfoException(4, 0, "XSLT Transformation failed: " + e.toString() + " render 2");
		}
	}
    
	/**
	 * Sets the default base URI.
	 *
	 * @param uri Base URI
	 * @throws jp.co.antenna.XfoJavaCtl.XfoException
	 */
	public void setBaseURI (String uri) {
		String opt = "-base";
		if (uri != null && !uri.equals("")) {
			if (this.args.containsKey(opt))
				this.args.remove(opt);
			this.args.put(opt, uri);
		} else {
			this.args.remove(opt);
		}
	}

    public void setBatchPrint (boolean bat) {
        // Fake it. 
    }
    
    /**
     * Set the URI of XML document to be formatted.
     * <br>If specified "@STDIN", XML document reads from stdin. The document that is read from stdin is assumed to be FO. 
     * 
     * @param uri URI of XML document
     * @throws jp.co.antenna.XfoJavaCtl.XfoException
     */
    public void setDocumentURI (String uri) throws XfoException {
        // Set the URI...
        String opt = "-d";
        if (uri != null && !uri.equals("")) {
            if (this.args.containsKey(opt))
                this.args.remove(opt);
            this.args.put(opt, uri);
        }
        else {
            this.args.remove(opt);
        }
    }
    
    public void setErrorStreamType (int type) {
        // Fake it.
    }
    
    /**
     * Set the error level to abort formatting process.
     * <br>XSL Formatter will stop formatting when the detected error level is equal to setExitLevel setting or higher.
     * <br>The default value is 2 (Warning). Thus if an error occurred and error level is 2 (Warning) or higher, the 
     * formatting process will be aborted. Please use the value from 1 to 4. When the value of 5 or more is specified, 
     * it is considered to be the value of 4. If a error-level:4 (fatal error) occurs, the formatting process will be 
     * aborted unconditionally. Note: An error is not displayed regardless what value may be specified to be this property. 
     * 
     * @param level Error level to abort
     * @throws jp.co.antenna.XfoJavaCtl.XfoException
     */
    public void setExitLevel (int level) throws XfoException {
        // Set the level...
        String opt = "-extlevel";
        if (this.args.containsKey(opt))
            this.args.remove(opt);
        this.args.put(opt, String.valueOf(level));
    }
    
    /**
     * Set the command-line string for external XSLT processor. For example:
     * <DL><DD>xslt -o %3 %1 %2 %param</DL>
     * %1 to %3 means following:
     * <DL><DD>%1 : XML Document 
     * <DD>%2 : XSL Stylesheet 
     * <DD>%3 : XSLT Output File 
     * <DD>%param : xsl:param</DL>
     * %1 to %3 are used to express only parameter positions. Do not replace them with actual file names.
     * <br>In case you use XSL:param for external XSLT processor, set the name and value here.
     * <br>In Windows version, default MSXML3 will be used. 
     * 
     * @param cmd Command-line string for external XSLT processor
     * @throws jp.co.antenna.XfoJavaCtl.XfoException
     */
    public void setExternalXSLT (String cmd) throws XfoException {
        // Fill this in....
    }
    
    /**
     * Register the MessageListener interface to the instance of implemented class.
     * <br>The error that occurred during the formatting process can be received as the event. 
     * 
     * @param listener The instance of implemented class
     */
    public void setMessageListener (MessageListener listener) {
        if (listener != null)
            this.messageListener = listener;
        else
            this.messageListener = null;
    }
    
    public void setMultiVolume (boolean multiVol) {
        String opt = "-multivol";
        if (multiVol) {
            this.args.put(opt, null);
        } else {
            this.args.remove(opt);
        }
    }
    
    /**
     * Specifies the output file path of the formatted result.
     * <br>When the printer is specified as an output format by setPrinterName, a
     * printing result is saved to the specified file by the printer driver.
     * <br>When output format other than a printer is specified, it is saved at the 
     * specified file with the specified output format.
     * <br>When omitted, or when "@STDOUT" is specified, it comes to standard output. 
     * 
     * @param path Path name of output file
     * @throws jp.co.antenna.XfoJavaCtl.XfoException
     */
    public void setOutputFilePath (String path) throws XfoException {
        // Set the path...
        String opt = "-o";
        if (path != null && !path.equals("")) {
            if (this.args.containsKey(opt))
                this.args.remove(opt);
            this.args.put(opt, path);
        }
        else {
            this.args.remove(opt);
        }
    }

    public void setFormatterType (int formatterType) {
        String opt = "-f";
        if (this.args.containsKey(opt))
            this.args.remove(opt);
        switch (formatterType) {
            case 0: this.args.put(opt, "AUTO"); break;
            case 1: this.args.put(opt, "HTML"); break;
            case 2: this.args.put(opt, "XHTML"); break;
            case 3: this.args.put(opt, "XMLCSS"); break;
            case 4: this.args.put(opt, "XSLFO"); break;
        }
    }

    public void setHtmlDefaultCharset (String charset) {
        String opt = "-htmlcs";
        if (charset != null && !charset.equals("")) {
            if (this.args.containsKey(opt))
                this.args.remove(opt);
            this.args.put(opt, charset);
        }
        else {
            this.args.remove(opt);
        }
    }

    public void addUserStylesheetURI (String uri) {
        if (uri != null && !uri.equals(""))
            this.userCSS.add(uri);
    }
    
    public void setPdfEmbedAllFontsEx (int embedLevel) throws XfoException {
        // fill it in
        String opt = "-peb";
        if (embedLevel != -1) {
            this.args.put(opt, String.valueOf(embedLevel));
        } else {
            this.args.remove(opt);
        }
    }
    
    public void setPdfImageCompression (int compressionMethod) {
        // fill it in
    }

    public void setPdfNoAccessibility (boolean newVal) {
        String opt = "-nab";
        if (newVal) {
            this.args.put(opt, null);
        } else {
            this.args.remove(opt);
        }
    }

    public void setPdfNoAddingOrChangingComments (boolean newVal) {
        String opt = "-nca";
        if (newVal) {
            this.args.put(opt, null);
        } else {
            this.args.remove(opt);
        }
    }

    public void setPdfNoAssembleDoc (boolean newVal) {
        String opt = "-nad";
        if (newVal) {
            this.args.put(opt, null);
        } else {
            this.args.remove(opt);
        }
    }

    public void setPdfNoChanging (boolean newVal) {
        String opt = "-ncg";
        if (newVal) {
            this.args.put(opt, null);
        } else {
            this.args.remove(opt);
        }
    }

    public void setPdfNoContentCopying (boolean newVal) {
        String opt = "-ncc";
        if (newVal) {
            this.args.put(opt, null);
        } else {
            this.args.remove(opt);
        }
    }

    public void setPdfNoFillForm (boolean newVal) {
        String opt = "-nff";
        if (newVal) {
            this.args.put(opt, null);
        } else {
            this.args.remove(opt);
        }
    }

    public void setPdfNoPrinting (boolean newVal) {
        String opt = "-npt";
        if (newVal) {
            this.args.put(opt, null);
        } else {
            this.args.remove(opt);
        }
    }

    /**
     * Specifies the owner password for PDF. The password must be within 32 bytes.
     * Effective when outputting to PDF.
     *
     * @param newVal Owner password
     */
    public void setPdfOwnerPassword (String newVal) {
        String opt = "-ownerpwd";
        if (newVal != null && !newVal.equals("")) {
            if (this.args.containsKey(opt))
                this.args.remove(opt);
            this.args.put(opt, newVal);
        }
        else {
            this.args.remove(opt);
        }
    }

    public void setPdfTag (boolean newVal) {
        String opt = "-tpdf";
        if (newVal) {
            this.args.put(opt, null);
        } else {
            this.args.remove(opt);
        }
    }

    public void setPdfVersion (int newVal) {
        String opt = "-pdfver";
        String version = null;
        if (this.args.containsKey(opt))
            this.args.remove(opt);
        switch (newVal) {
            case S_PDF_VERSION_13:
                version = "PDF1.3";
                break;
            case S_PDF_VERSION_14:
                version = "PDF1.4";
                break;
            case S_PDF_VERSION_15:
                version = "PDF1.5";
                break;
            case S_PDF_VERSION_16:
                version = "PDF1.6";
                break;
            case S_PDF_VERSION_17:
                version = "PDF1.7";
                break;
            case S_PDF_VERSION_A_1a_2005:
                version = "PDF/A-1a:2005";
                break;
            case S_PDF_VERSION_A_1b_2005:
                version = "PDF/A-1b:2005";
                break;
            case S_PDF_VERSION_X_1a_2001:
                version = "PDF/X-1a:2001";
                break;
            case S_PDF_VERSION_X_1a_2003:
                version = "PDF/X-1a:2003";
                break;
            case S_PDF_VERSION_X_2_2003:
                version = "PDF/X-2:2003";
                break;
            case S_PDF_VERSION_X_3_2002:
                version = "PDF/X-3:2002";
                break;
            case S_PDF_VERSION_X_3_2003:
                version = "PDF/X-3:2003";
                break;
        }
        if (version != null)
            this.args.put(opt, version);
    }
    
    public void setPrinterName (String prn) {
        String opt = "-p";
        if (prn != null && !prn.equals("")) {
            if (this.args.containsKey(opt))
                this.args.remove(opt);
            this.args.put(opt, prn);
        }
        else {
            this.args.remove(opt);
        }
    }
    
    public void setStylesheetURI (String uri) {
        String opt = "-s";
        if (uri != null && !uri.equals("")) {
            if (this.args.containsKey(opt))
                this.args.remove(opt);
            this.args.put(opt, uri);
        }
        else {
            this.args.remove(opt);
        }
    }
    
    /**
     * Specifies the two pass format.
     *
     * @param val specification of two pass format
     */
    public void setTwoPassFormatting (boolean val) {
        String opt = "-2pass";
        if (val) {
            this.args.put(opt, null);
        } else {
            this.args.remove(opt);
        }
    }

    /**
     * Get the specification of two pass format.
     *
     * @return specification of two pass format.
     */
    public boolean getTwoPassFormatting () {
        if (this.args.containsKey("-2pass")) {
            return true;
        }
        return false;
    }

    public void setOptionFileURI (String path) {
        String opt = "-i";
        if (path != null && !path.equals("")) {
            if (this.args.containsKey(opt))
                this.args.remove(opt);
            this.args.put(opt, path);
        }
        else {
            this.args.remove(opt);
        }
    }

    public void addOptionFileURI (String path) {
        this.setOptionFileURI(path);
    }
    
    public void setXSLTParam (String paramName, String value) {
        // fill it in
    }
}

class StreamCopyThread extends Thread {
	private InputStream inStream;
	private OutputStream outStream;

	public StreamCopyThread (InputStream inStream, OutputStream outStream) {
		this.inStream = inStream;
		this.outStream = outStream;
	}

	@Override
	public void run () {
		try {
			int COUNT = 1024;
			byte[] buff = new byte[COUNT];
			int len;
			while ((len = this.inStream.read(buff, 0, COUNT)) != -1) {
				this.outStream.write(buff, 0, len);
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {inStream.close();} catch (Exception e) {}
			try {outStream.close();} catch (Exception e) {}
		}
	}
}

class StreamFlusher extends Thread {
    private InputStream stream;

    public StreamFlusher (InputStream stream) {
	this.stream = stream;
    }

    @Override
    public void run () {
	BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
	try {
	    String line = reader.readLine();
	    while (line != null) {
		line = reader.readLine();
	    }
	} catch (IOException e) {

	}
    }

}

class ErrorParser extends Thread {
    private InputStream ErrorStream;
    private MessageListener listener;
    public int LastErrorLevel;
    public int LastErrorCode;
    public String LastErrorMessage;


    public ErrorParser (InputStream ErrorStream, MessageListener listener) {
        this.ErrorStream = ErrorStream;
        this.listener = listener;
    }
    
    @Override
    public void run () {
        try {
            // stuff
            BufferedReader reader = new BufferedReader(new InputStreamReader(this.ErrorStream));
            String line = reader.readLine();
			System.err.println(line);
            while (line != null) {
                if (line.startsWith("XSLCmd :") || line.startsWith("AHFCmd :")) {
                    if (line.contains("Error Level")) {
						System.err.println(line);
                        try {
                            int ErrorLevel = Integer.parseInt(line.substring(line.length() - 1, line.length()));
                            line = reader.readLine();
                            int ErrorCode = Integer.parseInt(line.split(" ")[line.split(" ").length - 2]);
                            line = reader.readLine();
                            String ErrorMessage = line.split(" ", 3)[2];
                            line = reader.readLine();
                            if (line.startsWith("XSLCmd :") || line.startsWith("AHFCmd :")) {
                                ErrorMessage += "\n" + line.split(" ", 3)[2];
                            }
                            this.LastErrorLevel = ErrorLevel;
                            this.LastErrorCode = ErrorCode;
                            this.LastErrorMessage = ErrorMessage;
			    if (this.listener != null)
				this.listener.onMessage(ErrorLevel, ErrorCode, ErrorMessage);
                        } catch (Exception e) {}
                    }
                } else if (line.startsWith("Invalid license.")) {
					int ErrorLevel = 4;
					int ErrorCode = 24579;
					String ErrorMessage = line 
						+ "\n" + reader.readLine() 
						+ "\n" + reader.readLine();
					this.LastErrorLevel = ErrorLevel;
					this.LastErrorCode = ErrorCode;
					this.LastErrorMessage = ErrorMessage;
					if (this.listener != null)
						this.listener.onMessage(ErrorLevel, ErrorCode, ErrorMessage);
				}
                line = reader.readLine();
            }
        } catch (Exception e) {}
    }
}
