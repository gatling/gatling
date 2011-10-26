/**
 * Copyright 2011 eBusiness Information, Groupe Excilys (www.excilys.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.ximpleware;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipFile;

import com.ximpleware.parser.XMLChar;

public class CustomVTDGen extends VTDGen {

	// attr_name_array size
	private final static int ATTR_NAME_ARRAY_SIZE = 16;

	// max Token length
	private final static int STATE_ATTR_NAME = 3;
	private final static int STATE_ATTR_VAL = 4;
	private final static int STATE_CDATA = 12;
	private final static int STATE_COMMENT = 11;
	private final static int STATE_DEC_ATTR_NAME = 10;
	private final static int STATE_DOC_END = 7; // end of document
	private final static int STATE_DOC_START = 6; // beginning of document
	private final static int STATE_DOCTYPE = 13;
	private final static int STATE_END_COMMENT = 14;
	// comment appear after the last ending tag
	private final static int STATE_END_PI = 15;
	private final static int STATE_END_TAG = 2;
	// internal parser state

	private final static int STATE_LT_SEEN = 0; // encounter the first <
	private final static int STATE_PI_TAG = 8;
	private final static int STATE_PI_VAL = 9;
	private final static int STATE_START_TAG = 1;
	private final static int STATE_TEXT = 5;

	// tag_stack size
	private final static int TAG_STACK_SIZE = 256;

	// token type
	private long[] attr_name_array;
	private int attr_count;
	private long[] prefixed_attr_name_array;
	private int[] prefix_URL_array;
	private int prefixed_attr_count;
	private boolean BOM_detected;
	private int ch;
	private int ch_temp;
	private int length1, length2;
	private int increment;

	private int last_depth;
	private int last_l1_index;
	private int last_l2_index;
	private int last_l3_index;
	private int last_l4_index;
	private boolean must_utf_8;

	private int temp_offset;

	private static final ResourceBundle ENTITIES = ResourceBundle.getBundle("com/ximpleware/entities");

	/**
	 * VTDGen constructor method.
	 */
	public CustomVTDGen() {
		attr_name_array = new long[ATTR_NAME_ARRAY_SIZE];
		prefixed_attr_name_array = new long[ATTR_NAME_ARRAY_SIZE];
		prefix_URL_array = new int[ATTR_NAME_ARRAY_SIZE];
		tag_stack = new long[TAG_STACK_SIZE];
		// scratch_buffer = new int[10];
		VTDDepth = 0;
		LcDepth = 3;

		br = false;
		e = new EOFException("permature EOF reached, XML document incomplete");
		ws = false;
		nsBuffer1 = new FastIntBuffer(4);
		nsBuffer2 = new FastLongBuffer(4);
		nsBuffer3 = new FastLongBuffer(4);
		currentElementRecord = 0;
		singleByteEncoding = true;
		shallowDepth = true;
		// offset_adj = 1;
	}

	/**
	 * Clear internal states so VTDGEn can process the next file.
	 */
	public void clear() {
		if (br == false) {
			VTDBuffer = null;
			l1Buffer = null;
			l2Buffer = null;
			l3Buffer = null;
			_l3Buffer = null;
			_l4Buffer = null;
			_l5Buffer = null;
		}
		XMLDoc = null;
		offset = temp_offset = 0;
		last_depth = last_l1_index = last_l2_index = last_l3_index = last_l4_index = 0;
		rootIndex = 0;
		depth = -1;
		increment = 1;
		BOM_detected = false;
		must_utf_8 = false;
		ch = ch_temp = 0;
		nsBuffer1.size = 0;
		nsBuffer2.size = 0;
		nsBuffer3.size = 0;
		currentElementRecord = 0;
	}

	/**
	 * Write white space records that are ignored by default
	 */
	private void addWhiteSpaceRecord() {
		if (depth > -1) {
			int length1 = offset - increment - temp_offset;
			if (length1 != 0)
				if (singleByteEncoding)// if (encoding < FORMAT_UTF_16BE)
					writeVTDText(temp_offset, length1, TOKEN_CHARACTER_DATA, depth);
				else
					writeVTDText(temp_offset >> 1, length1 >> 1, TOKEN_CHARACTER_DATA, depth);
		}
	}

	/**
	 * A private method that detects the BOM and decides document encoding
	 * 
	 * @throws EncodingException
	 * @throws ParseException
	 */
	private void decide_encoding() throws EncodingException, ParseException {
		if (XMLDoc.length == 0)
			throw new EncodingException("Document is zero sized ");
		if (XMLDoc[offset] == -2) {
			increment = 2;
			if (XMLDoc[offset + 1] == -1) {
				offset += 2;
				encoding = FORMAT_UTF_16BE;
				BOM_detected = true;
				r = new UTF16BEReader();
			} else
				throw new EncodingException("Unknown Character encoding: should be 0xff 0xfe");
		} else if (XMLDoc[offset] == -1) {
			increment = 2;
			if (XMLDoc[offset + 1] == -2) {
				offset += 2;
				encoding = FORMAT_UTF_16LE;
				BOM_detected = true;
				r = new UTF16LEReader();
			} else
				throw new EncodingException("Unknown Character encoding: not UTF-16LE");
		} else if (XMLDoc[offset] == -17) {
			if (XMLDoc[offset + 1] == -69 && XMLDoc[offset + 2] == -65) {
				offset += 3;
				must_utf_8 = true;
			} else
				throw new EncodingException("Unknown Character encoding: not UTF-8");
		} else if (XMLDoc[offset] == 0) {
			if (XMLDoc[offset + 1] == 0x3c && XMLDoc[offset + 2] == 0 && XMLDoc[offset + 3] == 0x3f) {
				encoding = FORMAT_UTF_16BE;
				increment = 2;
				r = new UTF16BEReader();
			} else
				throw new EncodingException("Unknown Character encoding: not UTF-16BE");
		} else if (XMLDoc[offset] == 0x3c) {
			if (XMLDoc[offset + 1] == 0 && XMLDoc[offset + 2] == 0x3f && XMLDoc[offset + 3] == 0) {
				increment = 2;
				encoding = FORMAT_UTF_16LE;
				r = new UTF16LEReader();
			}
		}
		// check for max file size exception
		if (encoding < FORMAT_UTF_16BE) {
			if (ns) {
				if ((offset + (long) docLen) >= 1L << 30)
					throw new ParseException("Other error: file size too big >=1GB ");
			} else {
				if ((offset + (long) docLen) >= 1L << 31)
					throw new ParseException("Other error: file size too big >=2GB ");
			}
		} else {
			// offset_adj = 2;
			if ((offset + (long) docLen) >= 1L << 31)
				throw new ParseException("Other error: file size too large >= 2GB");
		}
		if (encoding >= FORMAT_UTF_16BE)
			singleByteEncoding = false;
	}

	public static void main(String[] args) {

		System.out.println(ENTITIES.getString("Eacute"));
		System.out.println(ENTITIES.getString("toto"));
	}

	/**
	 * This method will detect whether the entity is valid or not and increment
	 * offset.
	 * 
	 * @return int
	 * @throws com.ximpleware.ParseException
	 *             Super class for any exception during parsing.
	 * @throws com.ximpleware.EncodingException
	 *             UTF/native encoding exception.
	 * @throws com.ximpleware.EOFException
	 *             End of file exception.
	 */
	private int entityIdentifier() throws EntityException, EncodingException, EOFException, ParseException {
		int ch = r.getChar();
		int val = 0;

		switch (ch) {
		case '#':
			ch = r.getChar();
			if (ch == 'x') {
				while (true) {
					ch = r.getChar();
					if (ch >= '0' && ch <= '9') {
						val = (val << 4) + (ch - '0');
					} else if (ch >= 'a' && ch <= 'f') {
						val = (val << 4) + (ch - 'a' + 10);
					} else if (ch >= 'A' && ch <= 'F') {
						val = (val << 4) + (ch - 'A' + 10);
					} else if (ch == ';') {
						return val;
					} else
						throw new EntityException("Errors in char reference: Illegal char following &#x.");
				}
			} else {
				while (true) {
					if (ch >= '0' && ch <= '9') {
						val = val * 10 + (ch - '0');
					} else if (ch == ';') {
						break;
					} else
						throw new EntityException("Errors in char reference: Illegal char following &#.");
					ch = r.getChar();
				}
			}
			if (!XMLChar.isValidChar(val)) {
				throw new EntityException("Errors in entity reference: Invalid XML char.");
			}
			return val;
			// break;

		default:
			int loop = 0;
			int maxLoop = 10;
			StringBuilder builder = new StringBuilder();
			while (loop < maxLoop) {
				loop++;
				if (ch == ';') {
					break;
				} else if (loop == maxLoop) {
					throw new EntityException("Errors in char reference: Illegal char following &.");
				}
				builder.append((char) ch);
				ch = r.getChar();
			}

			try {
				String valString = ENTITIES.getString(builder.toString());
				return Integer.valueOf(valString);

			} catch (MissingResourceException e) {
				throw new EntityException("unknown entity" + builder.toString());
			}
		}
		// return val;
	}

	/**
	 * Write the remaining portion of LC info
	 * 
	 */
	private void finishUp() {
		if (shallowDepth) {
			if (last_depth == 1) {
				l1Buffer.append(((long) last_l1_index << 32) | 0xffffffffL);
			} else if (last_depth == 2) {
				l2Buffer.append(((long) last_l2_index << 32) | 0xffffffffL);
			}
		} else {
			if (last_depth == 1) {
				l1Buffer.append(((long) last_l1_index << 32) | 0xffffffffL);
			} else if (last_depth == 2) {
				l2Buffer.append(((long) last_l2_index << 32) | 0xffffffffL);
			} else if (last_depth == 3) {
				_l3Buffer.append(((long) last_l3_index << 32) | 0xffffffffL);
			} else if (last_depth == 4) {
				_l4Buffer.append(((long) last_l4_index << 32) | 0xffffffffL);
			}
		}
	}

	/**
	 * Format the string indicating the position (line number:offset)of the
	 * offset if there is an exception.
	 * 
	 * @return java.lang.String indicating the line number and offset of the
	 *         exception
	 */
	private String formatLineNumber() {
		return formatLineNumber(offset);
	}

	private String formatLineNumber(int os) {
		int so = docOffset;
		int lineNumber = 0;
		int lineOffset = 0;

		if (encoding < FORMAT_UTF_16BE) {
			while (so <= os - 1) {
				if (XMLDoc[so] == '\n') {
					lineNumber++;
					lineOffset = so;
				}
				// lineOffset++;
				so++;
			}
			lineOffset = os - lineOffset;
		} else if (encoding == FORMAT_UTF_16BE) {
			while (so <= os - 2) {
				if (XMLDoc[so + 1] == '\n' && XMLDoc[so] == 0) {
					lineNumber++;
					lineOffset = so;
				}
				so += 2;
			}
			lineOffset = (os - lineOffset) >> 1;
		} else {
			while (so <= os - 2) {
				if (XMLDoc[so] == '\n' && XMLDoc[so + 1] == 0) {
					lineNumber++;
					lineOffset = so;
				}
				so += 2;
			}
			lineOffset = (os - lineOffset) >> 1;
		}
		return "\nLine Number: " + (lineNumber + 1) + " Offset: " + (lineOffset - 1);
	}

	/**
	 * The entity ignorant version of getCharAfterS.
	 * 
	 * @return int
	 * @throws ParseException
	 * @throws EncodingException
	 * @throws com.ximpleware.EOFException
	 */
	final private int getCharAfterS() throws ParseException, EncodingException, EOFException {
		int n;

		do {
			n = r.getChar();
			if ((n == ' ' || n == '\n' || n == '\t' || n == '\r')) {
				// if (XMLChar.isSpaceChar(n) ) {
			} else
				return n;
			n = r.getChar();
			if ((n == ' ' || n == '\n' || n == '\t' || n == '\r')) {
			} else
				return n;
			/*
			 * if (n == ' ' || n == '\n' || n =='\t'|| n == '\r' ) { } else
			 * return n;
			 */
		} while (true);
		// throw new EOFException("should never come here");
	}

	/**
	 * The entity aware version of getCharAfterS
	 * 
	 * @return int
	 * @throws ParseException
	 *             Super class for any exception during parsing.
	 * @throws EncodingException
	 *             UTF/native encoding exception.
	 * @throws com.ximpleware.EOFException
	 *             End of file exception.
	 */
	// private int getCharAfterSe()
	// throws ParseException, EncodingException, EOFException {
	// int n = 0;
	// int temp; //offset saver
	// while (true) {
	// n = r.getChar();
	// if (!XMLChar.isSpaceChar(n)) {
	// if (n != '&')
	// return n;
	// else {
	// temp = offset;
	// if (!XMLChar.isSpaceChar(entityIdentifier())) {
	// offset = temp; // rewind
	// return '&';
	// }
	// }
	// }
	// n = r.getChar();
	// if (!XMLChar.isSpaceChar(n)) {
	// if (n != '&')
	// return n;
	// else {
	// temp = offset;
	// if (!XMLChar.isSpaceChar(entityIdentifier())) {
	// offset = temp; // rewind
	// return '&';
	// }
	// }
	// }
	// }
	// }

	/**
	 * Pre-compute the size of VTD+XML index
	 * 
	 * @return size of the index
	 * 
	 */

	public long getIndexSize() {
		int size;
		if ((docLen & 7) == 0)
			size = docLen;
		else
			size = ((docLen >> 3) + 1) << 3;

		size += (VTDBuffer.size << 3) + (l1Buffer.size << 3) + (l2Buffer.size << 3);

		if ((l3Buffer.size & 1) == 0) { // even
			size += l3Buffer.size << 2;
		} else {
			size += (l3Buffer.size + 1) << 2; // odd
		}
		return size + 64;
	}

	/**
	 * This method returns the VTDNav object after parsing, it also cleans
	 * internal state so VTDGen can process the next file.
	 * 
	 * @return com.ximpleware.VTDNav
	 */
	public VTDNav getNav() {
		// call VTDNav constructor
		VTDNav vn;
		if (shallowDepth)
			vn = new VTDNav(rootIndex, encoding, ns, VTDDepth, new UniByteBuffer(XMLDoc), VTDBuffer, l1Buffer, l2Buffer, l3Buffer, docOffset, docLen);
		else
			vn = new VTDNav_L5(rootIndex, encoding, ns, VTDDepth, new UniByteBuffer(XMLDoc), VTDBuffer, l1Buffer, l2Buffer, _l3Buffer, _l4Buffer, _l5Buffer, docOffset, docLen);
		clear();
		r = new UTF8Reader();
		return vn;
	}

	/**
	 * Get the offset value of previous character.
	 * 
	 * @return int
	 * @throws ParseException
	 *             Super class for exceptions during parsing.
	 */
	private int getPrevOffset() throws ParseException {
		int prevOffset = offset;
		int temp;
		switch (encoding) {
		case FORMAT_UTF8:
			do {
				prevOffset--;
			} while (XMLDoc[prevOffset] < 0 && ((XMLDoc[prevOffset] & (byte) 0xc0) == (byte) 0x80));
			return prevOffset;
		case FORMAT_ASCII:
		case FORMAT_ISO_8859_1:
		case FORMAT_ISO_8859_2:
		case FORMAT_ISO_8859_3:
		case FORMAT_ISO_8859_4:
		case FORMAT_ISO_8859_5:
		case FORMAT_ISO_8859_6:
		case FORMAT_ISO_8859_7:
		case FORMAT_ISO_8859_8:
		case FORMAT_ISO_8859_9:
		case FORMAT_ISO_8859_10:
		case FORMAT_ISO_8859_11:
		case FORMAT_ISO_8859_13:
		case FORMAT_ISO_8859_14:
		case FORMAT_ISO_8859_15:
		case FORMAT_WIN_1250:
		case FORMAT_WIN_1251:
		case FORMAT_WIN_1252:
		case FORMAT_WIN_1253:
		case FORMAT_WIN_1254:
		case FORMAT_WIN_1255:
		case FORMAT_WIN_1256:
		case FORMAT_WIN_1257:
		case FORMAT_WIN_1258:
			return offset - 1;
		case FORMAT_UTF_16LE:
			temp = (XMLDoc[offset] & 0xff) << 8 | (XMLDoc[offset + 1] & 0xff);
			if (temp < 0xd800 || temp > 0xdfff) {
				return offset - 2;
			} else
				return offset - 4;
		case FORMAT_UTF_16BE:
			temp = (XMLDoc[offset] & 0xff) << 8 | (XMLDoc[offset + 1] & 0xff);
			if (temp < 0xd800 || temp > 0xdfff) {
				return offset - 2;
			} else
				return offset - 4;
		default:
			throw new ParseException("Other Error: Should never happen");
		}
	}

	/**
	 * This method loads the VTD+XML from a byte array
	 * 
	 * @return VTDNav
	 * @param ba
	 * @throws IOException
	 * @throws IndexReadException
	 * 
	 */
	public VTDNav loadIndex(byte[] ba) throws IOException, IndexReadException {
		IndexHandler.readIndex(ba, this);
		return getNav();
	}

	/**
	 * This method loads the VTD+XML from an input stream
	 * 
	 * @return VTDNav
	 * @param is
	 * @throws IOException
	 * @throws IndexReadException
	 * 
	 */
	public VTDNav loadIndex(InputStream is) throws IOException, IndexReadException {
		IndexHandler.readIndex(is, this);
		return getNav();
	}

	/**
	 * This method loads the VTD+XML from a file
	 * 
	 * @return VTDNav
	 * @param fileName
	 * @throws IOException
	 * @throws IndexReadException
	 * 
	 */
	public VTDNav loadIndex(String fileName) throws IOException, IndexReadException {
		FileInputStream fis = null;
		try {
			fis = new FileInputStream(fileName);
			return loadIndex(fis);
		} finally {
			if (fis != null)
				fis.close();
		}
	}

	/**
	 * Load the separate VTD index and XmL file. Refer to persistence model of
	 * separate vtd index for more details
	 * 
	 * @param XMLFileName
	 *            name of xml file
	 * @param VTDIndexName
	 *            name of the vtd index file
	 * @return VTDNav object
	 * @throws IOException
	 * @throws IndexReadException
	 * 
	 */
	public VTDNav loadSeparateIndex(String XMLFileName, String VTDIndexName) throws IOException, IndexReadException {
		FileInputStream xfis = null;
		FileInputStream vfis = null;
		xfis = new FileInputStream(XMLFileName);
		int size = (int) (new File(XMLFileName)).length();
		vfis = new FileInputStream(VTDIndexName);
		IndexHandler.readSeparateIndex(vfis, xfis, size, this);
		return getNav();
	}

	private void matchCPEncoding() throws ParseException {
		if ((r.skipChar('p') || r.skipChar('P')) && r.skipChar('1') && r.skipChar('2') && r.skipChar('5')) {
			if (encoding <= FORMAT_UTF_16LE) {
				if (must_utf_8)
					throw new EncodingException("Can't switch from UTF-8" + formatLineNumber());
				if (r.skipChar('0')) {
					encoding = FORMAT_WIN_1250;
					r = new WIN1250Reader();
					_writeVTD(temp_offset, 6, TOKEN_DEC_ATTR_VAL, depth);
				} else if (r.skipChar('1')) {
					encoding = FORMAT_WIN_1251;
					r = new WIN1251Reader();
					_writeVTD(temp_offset, 6, TOKEN_DEC_ATTR_VAL, depth);
				} else if (r.skipChar('2')) {
					encoding = FORMAT_WIN_1252;
					r = new WIN1252Reader();
					_writeVTD(temp_offset, 6, TOKEN_DEC_ATTR_VAL, depth);
				} else if (r.skipChar('3')) {
					encoding = FORMAT_WIN_1253;
					r = new WIN1253Reader();
					_writeVTD(temp_offset, 6, TOKEN_DEC_ATTR_VAL, depth);
				} else if (r.skipChar('4')) {
					encoding = FORMAT_WIN_1254;
					r = new WIN1254Reader();
					_writeVTD(temp_offset, 6, TOKEN_DEC_ATTR_VAL, depth);
				} else if (r.skipChar('5')) {
					encoding = FORMAT_WIN_1255;
					r = new WIN1255Reader();
					_writeVTD(temp_offset, 6, TOKEN_DEC_ATTR_VAL, depth);
				} else if (r.skipChar('6')) {
					encoding = FORMAT_WIN_1256;
					r = new WIN1256Reader();
					_writeVTD(temp_offset, 6, TOKEN_DEC_ATTR_VAL, depth);
				} else if (r.skipChar('7')) {
					encoding = FORMAT_WIN_1257;
					r = new WIN1257Reader();
					_writeVTD(temp_offset, 6, TOKEN_DEC_ATTR_VAL, depth);
				} else if (r.skipChar('8')) {
					encoding = FORMAT_WIN_1258;
					r = new WIN1258Reader();
					_writeVTD(temp_offset, 6, TOKEN_DEC_ATTR_VAL, depth);
				} else
					throw new ParseException("XML decl error: Invalid Encoding" + formatLineNumber());
				if (r.skipChar(ch_temp))
					return;
			} else
				throw new ParseException("XML decl error: Can't switch encoding to ISO-8859" + formatLineNumber());

		}
		throw new ParseException("XML decl error: Invalid Encoding" + formatLineNumber());
	}

	private void matchISOEncoding() throws ParseException {
		if ((r.skipChar('s') || r.skipChar('S')) && (r.skipChar('o') || r.skipChar('O')) && r.skipChar('-') && r.skipChar('8') && r.skipChar('8') && r.skipChar('5')
				&& r.skipChar('9') && r.skipChar('-')) {
			if (encoding <= FORMAT_UTF_16LE) {
				if (must_utf_8)
					throw new EncodingException("Can't switch from UTF-8" + formatLineNumber());
				if (r.skipChar('1')) {
					if (r.skipChar(ch_temp)) {
						encoding = FORMAT_ISO_8859_1;
						r = new ISO8859_1Reader();
						_writeVTD(temp_offset, 10, TOKEN_DEC_ATTR_VAL, depth);
						return;
					} else if (r.skipChar('0')) {
						encoding = FORMAT_ISO_8859_10;
						r = new ISO8859_10Reader();
						_writeVTD(temp_offset, 11, TOKEN_DEC_ATTR_VAL, depth);
					} else if (r.skipChar('1')) {
						encoding = FORMAT_ISO_8859_11;
						r = new ISO8859_11Reader();
						_writeVTD(temp_offset, 11, TOKEN_DEC_ATTR_VAL, depth);
					} else if (r.skipChar('3')) {
						encoding = FORMAT_ISO_8859_13;
						r = new ISO8859_13Reader();
						_writeVTD(temp_offset, 11, TOKEN_DEC_ATTR_VAL, depth);
					} else if (r.skipChar('4')) {
						encoding = FORMAT_ISO_8859_14;
						r = new ISO8859_14Reader();
						_writeVTD(temp_offset, 11, TOKEN_DEC_ATTR_VAL, depth);
					} else if (r.skipChar('5')) {
						encoding = FORMAT_ISO_8859_15;
						r = new ISO8859_15Reader();
						_writeVTD(temp_offset, 15, TOKEN_DEC_ATTR_VAL, depth);
					} else
						throw new ParseException("XML decl error: Invalid Encoding" + formatLineNumber());
				} else if (r.skipChar('2')) {
					encoding = FORMAT_ISO_8859_2;
					r = new ISO8859_2Reader();
					_writeVTD(temp_offset, 10, TOKEN_DEC_ATTR_VAL, depth);
				} else if (r.skipChar('3')) {
					r = new ISO8859_3Reader();
					encoding = FORMAT_ISO_8859_3;
					_writeVTD(temp_offset, 10, TOKEN_DEC_ATTR_VAL, depth);
				} else if (r.skipChar('4')) {
					r = new ISO8859_4Reader();
					encoding = FORMAT_ISO_8859_4;
					_writeVTD(temp_offset, 10, TOKEN_DEC_ATTR_VAL, depth);
				} else if (r.skipChar('5')) {
					encoding = FORMAT_ISO_8859_5;
					r = new ISO8859_5Reader();
					_writeVTD(temp_offset, 10, TOKEN_DEC_ATTR_VAL, depth);
				} else if (r.skipChar('6')) {
					encoding = FORMAT_ISO_8859_6;
					r = new ISO8859_6Reader();
					_writeVTD(temp_offset, 10, TOKEN_DEC_ATTR_VAL, depth);
				} else if (r.skipChar('7')) {
					encoding = FORMAT_ISO_8859_7;
					r = new ISO8859_7Reader();
					_writeVTD(temp_offset, 10, TOKEN_DEC_ATTR_VAL, depth);
				} else if (r.skipChar('8')) {
					encoding = FORMAT_ISO_8859_8;
					r = new ISO8859_8Reader();
					_writeVTD(temp_offset, 10, TOKEN_DEC_ATTR_VAL, depth);
				} else if (r.skipChar('9')) {
					encoding = FORMAT_ISO_8859_9;
					r = new ISO8859_9Reader();
					_writeVTD(temp_offset, 10, TOKEN_DEC_ATTR_VAL, depth);
				} else
					throw new ParseException("XML decl error: Invalid Encoding" + formatLineNumber());
				if (r.skipChar(ch_temp))
					return;
			} else
				throw new ParseException("XML decl error: Can't switch encoding to ISO-8859" + formatLineNumber());
		}
		throw new ParseException("XML decl error: Invalid Encoding" + formatLineNumber());
	}

	private void matchUTFEncoding() throws ParseException {
		if ((r.skipChar('s') || r.skipChar('S')))
			if (r.skipChar('-') && (r.skipChar('a') || r.skipChar('A')) && (r.skipChar('s') || r.skipChar('S')) && (r.skipChar('c') || r.skipChar('C'))
					&& (r.skipChar('i') || r.skipChar('I')) && (r.skipChar('i') || r.skipChar('I')) && r.skipChar(ch_temp)) {
				if (singleByteEncoding) {
					if (must_utf_8)
						throw new EncodingException("Can't switch from UTF-8" + formatLineNumber());
					encoding = FORMAT_ASCII;
					r = new ASCIIReader();
					_writeVTD(temp_offset, 8, TOKEN_DEC_ATTR_VAL, depth);

					return;
				} else
					throw new ParseException("XML decl error: Can't switch encoding to US-ASCII" + formatLineNumber());
			} else
				throw new ParseException("XML decl error: Invalid Encoding" + formatLineNumber());

		if ((r.skipChar('t') || r.skipChar('T')) && (r.skipChar('f') || r.skipChar('F')) && r.skipChar('-')) {
			if (r.skipChar('8') && r.skipChar(ch_temp)) {
				if (singleByteEncoding) {
					// encoding = FORMAT_UTF8;
					_writeVTD(temp_offset, 5, TOKEN_DEC_ATTR_VAL, depth);
					return;
				} else
					throw new ParseException("XML decl error: Can't switch encoding to UTF-8" + formatLineNumber());
			}
			if (r.skipChar('1') && r.skipChar('6')) {
				if (r.skipChar(ch_temp)) {
					if (!singleByteEncoding) {
						if (!BOM_detected)
							throw new EncodingException("BOM not detected for UTF-16" + formatLineNumber());
						_writeVTD(temp_offset >> 1, 6, TOKEN_DEC_ATTR_VAL, depth);
						return;
					}
					throw new ParseException("XML decl error: Can't switch encoding to UTF-16" + formatLineNumber());
				} else if ((r.skipChar('l') || r.skipChar('L')) && (r.skipChar('e') || r.skipChar('E')) && r.skipChar(ch_temp)) {
					if (encoding == FORMAT_UTF_16LE) {
						r = new UTF16LEReader();
						_writeVTD(temp_offset >> 1, 8, TOKEN_DEC_ATTR_VAL, depth);
						return;
					}
					throw new ParseException("XML del error: Can't switch encoding to UTF-16LE" + formatLineNumber());
				} else if ((r.skipChar('b') || r.skipChar('B')) && (r.skipChar('e') || r.skipChar('E')) && r.skipChar(ch_temp)) {
					if (encoding == FORMAT_UTF_16BE) {
						_writeVTD(temp_offset >> 1, 8, TOKEN_DEC_ATTR_VAL, depth);
						return;
					}
					throw new ParseException("XML del error: Can't swtich encoding to UTF-16BE" + formatLineNumber());
				}

				throw new ParseException("XML decl error: Invalid encoding" + formatLineNumber());
			}
		}
	}

	private void matchWindowsEncoding() throws ParseException {
		if ((r.skipChar('i') || r.skipChar('I')) && (r.skipChar('n') || r.skipChar('N')) && (r.skipChar('d') || r.skipChar('D')) && (r.skipChar('o') || r.skipChar('O'))
				&& (r.skipChar('w') || r.skipChar('W')) && (r.skipChar('s') || r.skipChar('S')) && r.skipChar('-') && r.skipChar('1') && r.skipChar('2') && r.skipChar('5')) {
			if (encoding <= FORMAT_UTF_16LE) {
				if (must_utf_8)
					throw new EncodingException("Can't switch from UTF-8" + formatLineNumber());
				if (r.skipChar('0')) {
					encoding = FORMAT_WIN_1250;
					r = new WIN1250Reader();
					_writeVTD(temp_offset, 12, TOKEN_DEC_ATTR_VAL, depth);
				} else if (r.skipChar('1')) {
					encoding = FORMAT_WIN_1251;
					r = new WIN1251Reader();
					_writeVTD(temp_offset, 12, TOKEN_DEC_ATTR_VAL, depth);
				} else if (r.skipChar('2')) {
					encoding = FORMAT_WIN_1252;
					r = new WIN1252Reader();
					_writeVTD(temp_offset, 12, TOKEN_DEC_ATTR_VAL, depth);
				} else if (r.skipChar('3')) {
					encoding = FORMAT_WIN_1253;
					r = new WIN1253Reader();
					_writeVTD(temp_offset, 12, TOKEN_DEC_ATTR_VAL, depth);
				} else if (r.skipChar('4')) {
					encoding = FORMAT_WIN_1254;
					r = new WIN1254Reader();
					_writeVTD(temp_offset, 12, TOKEN_DEC_ATTR_VAL, depth);
				} else if (r.skipChar('5')) {
					encoding = FORMAT_WIN_1255;
					r = new WIN1255Reader();
					_writeVTD(temp_offset, 12, TOKEN_DEC_ATTR_VAL, depth);
				} else if (r.skipChar('6')) {
					encoding = FORMAT_WIN_1256;
					r = new WIN1256Reader();
					_writeVTD(temp_offset, 12, TOKEN_DEC_ATTR_VAL, depth);
				} else if (r.skipChar('7')) {
					encoding = FORMAT_WIN_1257;
					r = new WIN1257Reader();
					_writeVTD(temp_offset, 12, TOKEN_DEC_ATTR_VAL, depth);
				} else if (r.skipChar('8')) {
					encoding = FORMAT_WIN_1258;
					r = new WIN1258Reader();
					_writeVTD(temp_offset, 12, TOKEN_DEC_ATTR_VAL, depth);
				} else
					throw new ParseException("XML decl error: Invalid Encoding" + formatLineNumber());
				if (r.skipChar(ch_temp))
					return;

			} else
				throw new ParseException("XML decl error: Can't switch encoding to ISO-8859" + formatLineNumber());
		}
		throw new ParseException("XML decl error: Invalid Encoding" + formatLineNumber());
	}

	/**
	 * Generating VTD tokens and Location cache info. When set to true, VTDGen
	 * conforms to XML namespace 1.0 spec
	 * 
	 * @param NS
	 *            boolean Enable namespace or not
	 * @throws ParseException
	 *             Super class for any exceptions during parsing.
	 * @throws EOFException
	 *             End of file exception.
	 * @throws EntityException
	 *             Entity resolution exception.
	 * @throws EncodingException
	 *             UTF/native encoding exception.
	 */
	public void parse(boolean NS) throws EncodingException, EOFException, EntityException, ParseException {

		// define internal variables
		ns = NS;
		length1 = length2 = 0;
		attr_count = prefixed_attr_count = 0 /* , ch = 0, ch_temp = 0 */;
		int parser_state = STATE_DOC_START;
		// boolean has_amp = false;
		is_ns = false;
		encoding = FORMAT_UTF8;
		boolean helper = false;
		boolean default_ns = false; // true xmlns='abc'
		boolean isXML = false; // true only for xmlns:xml
		singleByteEncoding = true;
		// first check first several bytes to figure out the encoding
		decide_encoding();

		// enter the main finite state machine
		try {
			_writeVTD(0, 0, TOKEN_DOCUMENT, depth);
			while (true) {
				switch (parser_state) {
				case STATE_LT_SEEN: // if (depth < -1)
					// throw new ParseException("Other Errors: Invalid depth");
					temp_offset = offset;
					ch = r.getChar();
					if (XMLChar.isNameStartChar(ch)) {
						depth++;
						parser_state = STATE_START_TAG;
					} else {
						switch (ch) {
						case '/':
							parser_state = STATE_END_TAG;
							break;
						case '?':
							parser_state = process_qm_seen();
							break;
						case '!': // three possibility (comment, CDATA, DOCTYPE)
							parser_state = process_ex_seen();
							break;
						default:
							throw new ParseException("Other Error: Invalid char after <" + formatLineNumber());
						}
					}
					break;

				case STATE_START_TAG: // name space is handled by
					do {
						ch = r.getChar();
						if (XMLChar.isNameChar(ch)) {
							if (ch == ':') {
								length2 = offset - temp_offset - increment;
								if (ns && checkPrefix2(temp_offset, length2))
									throw new ParseException("xmlns can't be an element prefix " + formatLineNumber(offset));
							}
						} else
							break;
						ch = r.getChar();
						if (XMLChar.isNameChar(ch)) {
							if (ch == ':') {
								length2 = offset - temp_offset - increment;
								if (ns && checkPrefix2(temp_offset, length2))
									throw new ParseException("xmlns can't be an element prefix " + formatLineNumber(offset));
							}
						} else
							break;
					} while (true);
					length1 = offset - temp_offset - increment;
					if (depth > MAX_DEPTH) {
						throw new ParseException("Other Error: Depth exceeds MAX_DEPTH" + formatLineNumber());
					}
					// writeVTD(offset, TOKEN_STARTING_TAG, length2:length1,
					// depth)
					long x = ((long) length1 << 32) + temp_offset;
					tag_stack[depth] = x;

					// System.out.println(
					// " " + (temp_offset) + " " + length2 + ":" + length1 +
					// " startingTag " + depth);
					if (depth > VTDDepth)
						VTDDepth = depth;
					// if (encoding < FORMAT_UTF_16BE){
					if (singleByteEncoding) {
						if (length2 > MAX_PREFIX_LENGTH || length1 > MAX_QNAME_LENGTH)
							throw new ParseException("Token Length Error: Starting tag prefix or qname length too long" + formatLineNumber());
						if (this.shallowDepth)
							writeVTD((temp_offset), (length2 << 11) | length1, TOKEN_STARTING_TAG, depth);
						else
							writeVTD_L5((temp_offset), (length2 << 11) | length1, TOKEN_STARTING_TAG, depth);
					} else {
						if (length2 > (MAX_PREFIX_LENGTH << 1) || length1 > (MAX_QNAME_LENGTH << 1))
							throw new ParseException("Token Length Error: Starting tag prefix or qname length too long" + formatLineNumber());
						if (this.shallowDepth)
							writeVTD((temp_offset) >> 1, (length2 << 10) | (length1 >> 1), TOKEN_STARTING_TAG, depth);
						else
							writeVTD_L5((temp_offset) >> 1, (length2 << 10) | (length1 >> 1), TOKEN_STARTING_TAG, depth);
					}
					if (ns) {
						if (length2 != 0) {
							length2 += increment;
							currentElementRecord = (((long) ((length2 << 16) | length1)) << 32) | temp_offset;
						} else
							currentElementRecord = 0;

						if (depth <= nsBuffer1.size - 1) {
							nsBuffer1.size = depth;
							int t = nsBuffer1.intAt(depth - 1) + 1;
							nsBuffer2.size = t;
							nsBuffer3.size = t;
						}
					}
					// offset += length1;
					length2 = 0;
					if (XMLChar.isSpaceChar(ch)) {
						ch = getCharAfterS();
						if (XMLChar.isNameStartChar(ch)) {
							// seen an attribute here
							temp_offset = getPrevOffset();
							parser_state = STATE_ATTR_NAME;
							break;
						}
					}
					helper = true;
					if (ch == '/') {
						depth--;
						helper = false;
						ch = r.getChar();
					}
					if (ch == '>') {
						if (ns) {
							nsBuffer1.append(nsBuffer3.size - 1);
							if (currentElementRecord != 0)
								qualifyElement();
						}

						// parser_state = processElementTail(helper);
						if (depth != -1) {
							temp_offset = offset;
							// ch = getCharAfterSe(); // consume WSs
							ch = getCharAfterS(); // consume WSs
							if (ch == '<') {
								if (ws)
									addWhiteSpaceRecord();
								parser_state = STATE_LT_SEEN;
								if (r.skipChar('/')) {
									if (helper) {
										length1 = offset - temp_offset - (increment << 1);
										// if (length1 > 0) {
										// if (encoding < FORMAT_UTF_16BE)
										if (singleByteEncoding)
											writeVTDText((temp_offset), length1, TOKEN_CHARACTER_DATA, depth);
										else
											writeVTDText((temp_offset) >> 1, (length1 >> 1), TOKEN_CHARACTER_DATA, depth);
										// }
									}
									parser_state = STATE_END_TAG;
									break;
								}
							} else if (XMLChar.isContentChar(ch)) {
								// temp_offset = offset;
								parser_state = STATE_TEXT;
							} else {
								parser_state = STATE_TEXT;
								handleOtherTextChar2(ch);
							}
						} else {
							parser_state = STATE_DOC_END;
						}
						break;
					}
					throw new ParseException("Starting tag Error: Invalid char in starting tag" + formatLineNumber());

				case STATE_END_TAG:
					temp_offset = offset;
					int sos = (int) tag_stack[depth];
					int sl = (int) (tag_stack[depth] >> 32);

					offset = temp_offset + sl;

					if (offset >= endOffset)
						throw new EOFException("permature EOF reached, XML document incomplete");
					for (int i = 0; i < sl; i++) {
						if (XMLDoc[sos + i] != XMLDoc[temp_offset + i])
							throw new ParseException("Ending tag error: Start/ending tag mismatch" + formatLineNumber());
					}
					depth--;
					ch = getCharAfterS();
					if (ch != '>')
						throw new ParseException("Ending tag error: Invalid char in ending tag " + formatLineNumber());

					if (depth != -1) {
						temp_offset = offset;
						ch = getCharAfterS();
						if (ch == '<') {
							if (ws)
								addWhiteSpaceRecord();
							parser_state = STATE_LT_SEEN;
						} else if (XMLChar.isContentChar(ch)) {
							parser_state = STATE_TEXT;
						} else {
							handleOtherTextChar2(ch);
							parser_state = STATE_TEXT;
						}
					} else
						parser_state = STATE_DOC_END;
					break;

				case STATE_ATTR_NAME:

					if (ch == 'x') {
						if (r.skipChar('m') && r.skipChar('l') && r.skipChar('n') && r.skipChar('s')) {
							ch = r.getChar();
							if (ch == '=' || XMLChar.isSpaceChar(ch)) {
								is_ns = true;
								default_ns = true;
							} else if (ch == ':') {
								is_ns = true; // break;
								default_ns = false;
							}
						}
					}
					do {
						if (XMLChar.isNameChar(ch)) {
							if (ch == ':') {
								length2 = offset - temp_offset - increment;
							}
						} else
							break;
						ch = r.getChar();
					} while (true);
					length1 = getPrevOffset() - temp_offset;
					if (is_ns && ns) {
						// make sure postfix isn't xmlns
						if (!default_ns) {
							if (increment == 1 && (length1 - length2 - 1 == 5) || (increment == 2 && (length1 - length2 - 2 == 10)))
								disallow_xmlns(temp_offset + length2 + increment);

							// if the post fix is xml, signal it
							if (increment == 1 && (length1 - length2 - 1 == 3) || (increment == 2 && (length1 - length2 - 2 == 6)))
								isXML = matchXML(temp_offset + length2 + increment);
						}
					}
					// check for uniqueness here
					checkAttributeUniqueness();
					// boolean unique = true;
					// boolean unequal;
					// for (int i = 0; i < attr_count; i++) {
					// unequal = false;
					// int prevLen = (int) attr_name_array[i];
					// if (length1 == prevLen) {
					// int prevOffset =
					// (int) (attr_name_array[i] >> 32);
					// for (int j = 0; j < prevLen; j++) {
					// if (XMLDoc[prevOffset + j]
					// != XMLDoc[temp_offset + j]) {
					// unequal = true;
					// break;
					// }
					// }
					// } else
					// unequal = true;
					// unique = unique && unequal;
					// }
					// if (!unique && attr_count != 0)
					// throw new ParseException(
					// "Error in attr: Attr name not unique"
					// + formatLineNumber());
					// unique = true;
					// if (attr_count < attr_name_array.length) {
					// attr_name_array[attr_count] =
					// ((long) (temp_offset) << 32) + length1;
					// attr_count++;
					// } else // grow the attr_name_array by 16
					// {
					// long[] temp_array = attr_name_array;
					// /*System.out.println(
					// "size increase from "
					// + temp_array.length
					// + "  to "
					// + (attr_count + 16));*/
					// attr_name_array =
					// new long[attr_count + ATTR_NAME_ARRAY_SIZE];
					// System.arraycopy(temp_array, 0,attr_name_array, 0,
					// attr_count);
					// /*for (int i = 0; i < attr_count; i++) {
					// attr_name_array[i] = temp_array[i];
					// }*/
					// attr_name_array[attr_count] =
					// ((long) (temp_offset) << 32) + length1;
					// attr_count++;
					// }

					// after checking, write VTD
					if (is_ns) { // if the prefix is xmlns: or xmlns
						// if (encoding < FORMAT_UTF_16BE){
						if (singleByteEncoding) {
							if (length2 > MAX_PREFIX_LENGTH || length1 > MAX_QNAME_LENGTH)
								throw new ParseException("Token length overflow error: Attr NS tag prefix or qname length too long" + formatLineNumber());
							_writeVTD(temp_offset, (length2 << 11) | length1, TOKEN_ATTR_NS, depth);
						} else {
							if (length2 > (MAX_PREFIX_LENGTH << 1) || length1 > (MAX_QNAME_LENGTH << 1))
								throw new ParseException("Token length overflow error: Attr NS prefix or qname length too long" + formatLineNumber());
							_writeVTD(temp_offset >> 1, (length2 << 10) | (length1 >> 1), TOKEN_ATTR_NS, depth);
						}
						// append to nsBuffer2
						if (ns) {
							// unprefixed xmlns are not recorded
							if (length2 != 0 && !isXML) {
								// nsBuffer2.append(VTDBuffer.size() - 1);
								long l = ((long) ((length2 << 16) | length1)) << 32 | temp_offset;
								nsBuffer3.append(l); // byte offset and byte
								// length
							}
						}

					} else {
						// if (encoding < FORMAT_UTF_16BE){
						if (singleByteEncoding) {
							if (length2 > MAX_PREFIX_LENGTH || length1 > MAX_QNAME_LENGTH)
								throw new ParseException("Token Length Error: Attr name prefix or qname length too long" + formatLineNumber());
							_writeVTD(temp_offset, (length2 << 11) | length1, TOKEN_ATTR_NAME, depth);
						} else {
							if (length2 > (MAX_PREFIX_LENGTH << 1) || length1 > (MAX_QNAME_LENGTH << 1))
								throw new ParseException("Token Length overflow error: Attr name prefix or qname length too long" + formatLineNumber());
							_writeVTD(temp_offset >> 1, (length2 << 10) | (length1 >> 1), TOKEN_ATTR_NAME, depth);
						}
					}
					/*
					 * System.out.println( " " + temp_offset + " " + length2 +
					 * ":" + length1 + " attr name " + depth);
					 */
					length2 = 0;
					if (XMLChar.isSpaceChar(ch)) {
						ch = getCharAfterS();
					}
					if (ch != '=')
						throw new ParseException("Error in attr: invalid char" + formatLineNumber());
					ch_temp = getCharAfterS();
					if (ch_temp != '"' && ch_temp != '\'')
						throw new ParseException("Error in attr: invalid char (should be ' or \" )" + formatLineNumber());
					temp_offset = offset;
					parser_state = STATE_ATTR_VAL;
					break;

				case STATE_ATTR_VAL:
					do {
						ch = r.getChar();
						if (XMLChar.isValidChar(ch) && ch != '<') {
							if (ch == ch_temp)
								break;
							if (ch == '&') {
								// as in vtd spec, we mark attr val with
								// entities
								if (!XMLChar.isValidChar(entityIdentifier())) {
									throw new ParseException("Error in attr: Invalid XML char" + formatLineNumber());
								}
							}
						} else
							throw new ParseException("Error in attr: Invalid XML char" + formatLineNumber());
					} while (true);

					length1 = offset - temp_offset - increment;
					if (ns && is_ns) {
						if (!default_ns && length1 == 0) {
							throw new ParseException(" non-default ns URL can't be empty" + formatLineNumber());
						}
						// identify nsURL return 0,1,2
						int t = identifyNsURL(temp_offset, length1);
						if (isXML) {// xmlns:xml
							if (t != 1)
								// URL points to
								// "http://www.w3.org/XML/1998/namespace"
								throw new ParseException("xmlns:xml can only point to" + "\"http://www.w3.org/XML/1998/namespace\"" + formatLineNumber());

						} else {
							if (!default_ns)
								nsBuffer2.append(((long) temp_offset << 32) | length1);
							if (t != 0) {
								if (t == 1)
									throw new ParseException("namespace declaration can't point to" + " \"http://www.w3.org/XML/1998/namespace\"" + formatLineNumber());
								throw new ParseException("namespace declaration can't point to" + " \"http://www.w3.org/2000/xmlns/\"" + formatLineNumber());
							}
						}
						// no ns URL points to
						// "http://www.w3.org/2000/xmlns/"

						// no ns URL points to
						// "http://www.w3.org/XML/1998/namespace"
					}

					if (singleByteEncoding) {
						// if (encoding < FORMAT_UTF_16BE){
						if (length1 > MAX_TOKEN_LENGTH)
							throw new ParseException("Token Length Error:" + " Attr val too long (>0xfffff)" + formatLineNumber());
						_writeVTD(temp_offset, length1, TOKEN_ATTR_VAL, depth);
					} else {
						if (length1 > (MAX_TOKEN_LENGTH << 1))
							throw new ParseException("Token Length Error:" + " Attr val too long (>0xfffff)" + formatLineNumber());
						_writeVTD(temp_offset >> 1, length1 >> 1, TOKEN_ATTR_VAL, depth);
					}

					isXML = false;
					is_ns = false;

					ch = r.getChar();
					if (XMLChar.isSpaceChar(ch)) {
						ch = getCharAfterS();
						if (XMLChar.isNameStartChar(ch)) {
							temp_offset = offset - increment;
							parser_state = STATE_ATTR_NAME;
							break;
						}
					}

					helper = true;
					if (ch == '/') {
						depth--;
						helper = false;
						ch = r.getChar();
					}

					if (ch == '>') {
						if (ns) {
							nsBuffer1.append(nsBuffer3.size - 1);
							if (prefixed_attr_count > 0)
								qualifyAttributes();
							if (prefixed_attr_count > 1) {
								checkQualifiedAttributeUniqueness();
							}
							if (currentElementRecord != 0)
								qualifyElement();
							prefixed_attr_count = 0;
						}
						attr_count = 0;
						// parser_state = processElementTail(helper);
						if (depth != -1) {
							temp_offset = offset;
							// ch = getCharAfterSe();
							ch = getCharAfterS();

							if (ch == '<') {
								if (ws)
									addWhiteSpaceRecord();
								parser_state = STATE_LT_SEEN;
								if (r.skipChar('/')) {
									if (helper) {
										length1 = offset - temp_offset - (increment << 1);
										// if (length1 > 0) {
										if (singleByteEncoding)// if (encoding <
																// FORMAT_UTF_16BE)
											writeVTDText((temp_offset), length1, TOKEN_CHARACTER_DATA, depth);
										else
											writeVTDText((temp_offset) >> 1, (length1 >> 1), TOKEN_CHARACTER_DATA, depth);
										// }
									}
									parser_state = STATE_END_TAG;
									break;
								}
							} else if (XMLChar.isContentChar(ch)) {
								// temp_offset = offset;
								parser_state = STATE_TEXT;
							} else {
								handleOtherTextChar2(ch);
								parser_state = STATE_TEXT;
							}
						} else {
							parser_state = STATE_DOC_END;
						}
						break;
					}

					throw new ParseException("Starting tag Error: Invalid char in starting tag" + formatLineNumber());

				case STATE_TEXT:
					if (depth == -1)
						throw new ParseException("Error in text content: Char data at the wrong place" + formatLineNumber());
					do {
						ch = r.getChar();
						// System.out.println(""+(char)ch);
						if (XMLChar.isContentChar(ch)) {
						} else if (ch == '<') {
							break;
						} else
							handleOtherTextChar(ch);
						ch = r.getChar();
						if (XMLChar.isContentChar(ch)) {
						} else if (ch == '<') {
							break;
						} else
							handleOtherTextChar(ch);
					} while (true);

					length1 = offset - increment - temp_offset;

					if (singleByteEncoding) // if (encoding < FORMAT_UTF_16BE)
						writeVTDText(temp_offset, length1, TOKEN_CHARACTER_DATA, depth);
					else
						writeVTDText(temp_offset >> 1, length1 >> 1, TOKEN_CHARACTER_DATA, depth);

					// has_amp = true;
					parser_state = STATE_LT_SEEN;
					break;
				case STATE_DOC_START:
					parser_state = process_start_doc();
					break;
				case STATE_DOC_END:
					// docEnd = true;
					parser_state = process_end_doc();
					break;
				case STATE_PI_TAG:
					parser_state = process_pi_tag();
					break;
				// throw new ParseException("Error in PI: Invalid char");
				case STATE_PI_VAL:
					parser_state = process_pi_val();
					break;

				case STATE_DEC_ATTR_NAME:
					parser_state = process_dec_attr();
					break;

				case STATE_COMMENT:
					parser_state = process_comment();
					break;

				case STATE_CDATA:
					parser_state = process_cdata();
					break;

				case STATE_DOCTYPE:
					parser_state = process_doc_type();
					break;

				case STATE_END_COMMENT:
					parser_state = process_end_comment();
					break;

				case STATE_END_PI:
					parser_state = process_end_pi();
					break;

				default:
					throw new ParseException("Other error: invalid parser state" + formatLineNumber());
				}
			}
		} catch (EOFException e) {
			if (parser_state != STATE_DOC_END)
				throw e;
			finishUp();
		}
	}

	private void checkQualifiedAttributeUniqueness() throws ParseException {
		// TODO Auto-generated method stub
		int preLen1, os1, postLen1, URLLen1, URLOs1, preLen2, os2, postLen2, URLLen2, URLOs2, k;
		for (int i = 0; i < prefixed_attr_count; i++) {
			preLen1 = (int) ((prefixed_attr_name_array[i] & 0xffff0000L) >> 16);
			postLen1 = (int) ((prefixed_attr_name_array[i] & 0xffffL)) - preLen1 - increment;
			os1 = (int) (prefixed_attr_name_array[i] >> 32) + preLen1 + increment;
			URLLen1 = nsBuffer2.lower32At(prefix_URL_array[i]);
			URLOs1 = nsBuffer2.upper32At(prefix_URL_array[i]);
			for (int j = i + 1; j < prefixed_attr_count; j++) {
				// prefix of i matches that of j
				preLen2 = (int) ((prefixed_attr_name_array[j] & 0xffff0000L) >> 16);
				postLen2 = (int) ((prefixed_attr_name_array[j] & 0xffffL)) - preLen2 - increment;
				os2 = (int) (prefixed_attr_name_array[j] >> 32) + preLen2 + increment;
				// System.out.println(new String(XMLDoc,os1, postLen1)
				// +" "+ new String(XMLDoc, os2, postLen2));
				if (postLen1 == postLen2) {
					k = 0;
					for (; k < postLen1; k++) {
						// System.out.println(i+" "+(char)(XMLDoc[os+k])+"<===>"+(char)(XMLDoc[preOs+k]));
						if (XMLDoc[os1 + k] != XMLDoc[os2 + k])
							break;
					}
					if (k == postLen1) {
						// found the match
						URLLen2 = nsBuffer2.lower32At(prefix_URL_array[j]);
						URLOs2 = nsBuffer2.upper32At(prefix_URL_array[j]);
						// System.out.println(" URLOs1 ===>" + URLOs1);
						// System.out.println("nsBuffer2 ===>"+nsBuffer2.longAt(i)+" i==>"+i);
						// System.out.println("URLLen2 "+ URLLen2+" URLLen1 "+
						// URLLen1+" ");
						if (matchURL(URLOs1, URLLen1, URLOs2, URLLen2))
							throw new ParseException(" qualified attribute names collide " + formatLineNumber(os2));
					}
				}
			}
			// System.out.println("======");
		}
	}

	private void qualifyAttributes() throws ParseException {
		int i1 = nsBuffer3.size - 1;
		int j = 0, i = 0;
		// two cases:
		// 1. the current element has no prefix, look for xmlns
		// 2. the current element has prefix, look for xmlns:something
		while (j < prefixed_attr_count) {
			int preLen = (int) ((prefixed_attr_name_array[j] & 0xffff0000L) >> 16);
			int preOs = (int) (prefixed_attr_name_array[j] >> 32);
			// System.out.println(new String(XMLDoc, preOs, preLen)+"===");
			i = i1;
			while (i >= 0) {
				int t = nsBuffer3.upper32At(i);
				// with prefix, get full length and prefix length
				if ((t & 0xffff) - (t >> 16) == preLen + increment) {
					// doing byte comparison here
					int os = nsBuffer3.lower32At(i) + (t >> 16) + increment;
					// System.out.println(new String(XMLDoc, os, preLen)+"");
					int k = 0;
					for (; k < preLen; k++) {
						// System.out.println(i+" "+(char)(XMLDoc[os+k])+"<===>"+(char)(XMLDoc[preOs+k]));
						if (XMLDoc[os + k] != XMLDoc[preOs + k])
							break;
					}
					if (k == preLen) {
						break; // found the match
					}
				}
				/*
				 * if ( (nsBuffer3.upper32At(i) & 0xffff0000) == 0){ return; }
				 */
				i--;
			}
			if (i < 0)
				throw new ParseException("Name space qualification Exception: prefixed attribute not qualified\n" + formatLineNumber(preOs));
			else
				prefix_URL_array[j] = i;
			j++;
			// no need to check if xml is the prefix
		}
		// for (int h=0;h<prefixed_attr_count;h++)
		// System.out.print(" "+prefix_URL_array[h]);

		// System.out.println();
		// print line # column# and full element name
		// throw new
		// ParseException("Name space qualification Exception: Element not qualified\n"
		// +formatLineNumber((int)pref));

	}

	// return 0, 1 or 2
	private int identifyNsURL(int byte_offset, int length) {
		// TODO Auto-generated method stub
		// URL points to "http://www.w3.org/XML/1998/namespace" return 1
		// URL points to "http://www.w3.org/2000/xmlns/" return 2
		String URL1 = "2000/xmlns/";
		String URL2 = "http://www.w3.org/XML/1998/namespace";
		long l;
		int i, t, g = byte_offset + length;
		int os = byte_offset;
		if (length < 29 || (increment == 2 && length < 58))
			return 0;

		for (i = 0; i < 18 && os < g; i++) {
			l = _getCharResolved(os);
			// System.out.println("char ==>"+(char)l);
			if (URL2.charAt(i) != (int) l)
				return 0;
			os += (int) (l >> 32);
		}

		// store offset value
		t = os;

		for (i = 0; i < 11 && os < g; i++) {
			l = _getCharResolved(os);
			if (URL1.charAt(i) != (int) l)
				break;
			os += (int) (l >> 32);
		}
		if (os == g)
			return 2;

		// so far a match
		os = t;
		for (i = 18; i < 36 && os < g; i++) {
			l = _getCharResolved(os);
			if (URL2.charAt(i) != (int) l)
				return 0;
			os += (int) (l >> 32);
		}
		if (os == g)
			return 1;

		return 0;
	}

	private boolean matchXML(int byte_offset) {
		// TODO Auto-generated method stub
		if (encoding < FORMAT_UTF_16BE) {
			if (XMLDoc[byte_offset] == 'x' && XMLDoc[byte_offset + 1] == 'm' && XMLDoc[byte_offset + 2] == 'l')
				return true;
		} else {
			if (encoding == FORMAT_UTF_16LE) {
				if (XMLDoc[byte_offset] == 'x' && XMLDoc[byte_offset + 1] == 0 && XMLDoc[byte_offset + 2] == 'm' && XMLDoc[byte_offset + 3] == 0 && XMLDoc[byte_offset + 4] == 'l'
						&& XMLDoc[byte_offset + 5] == 0)
					return true;
			} else {
				if (XMLDoc[byte_offset] == 0 && XMLDoc[byte_offset + 1] == 'x' && XMLDoc[byte_offset + 2] == 0 && XMLDoc[byte_offset + 3] == 'm' && XMLDoc[byte_offset + 4] == 0
						&& XMLDoc[byte_offset + 5] == 'l')
					return true;
			}
		}
		return false;
	}

	private void disallow_xmlns(int byte_offset) throws ParseException {
		// TODO Auto-generated method stub
		if (encoding < FORMAT_UTF_16BE) {
			if (XMLDoc[byte_offset] == 'x' && XMLDoc[byte_offset + 1] == 'm' && XMLDoc[byte_offset + 2] == 'l' && XMLDoc[byte_offset + 3] == 'n' && XMLDoc[byte_offset + 4] == 's')
				throw new ParseException("xmlns as a ns prefix can't be re-declared" + formatLineNumber(byte_offset));

		} else {
			if (encoding == FORMAT_UTF_16LE) {
				if (XMLDoc[byte_offset] == 'x' && XMLDoc[byte_offset + 1] == 0 && XMLDoc[byte_offset + 2] == 'm' && XMLDoc[byte_offset + 3] == 0 && XMLDoc[byte_offset + 4] == 'l'
						&& XMLDoc[byte_offset + 5] == 0 && XMLDoc[byte_offset + 6] == 'n' && XMLDoc[byte_offset + 7] == 0 && XMLDoc[byte_offset + 8] == 's'
						&& XMLDoc[byte_offset + 9] == 0)
					throw new ParseException("xmlns as a ns prefix can't be re-declared" + formatLineNumber(byte_offset));
			} else {
				if (XMLDoc[byte_offset] == 0 && XMLDoc[byte_offset + 1] == 'x' && XMLDoc[byte_offset + 2] == 0 && XMLDoc[byte_offset + 3] == 'm' && XMLDoc[byte_offset + 4] == 0
						&& XMLDoc[byte_offset + 5] == 'l' && XMLDoc[byte_offset + 6] == 0 && XMLDoc[byte_offset + 7] == 'n' && XMLDoc[byte_offset + 8] == 0
						&& XMLDoc[byte_offset + 9] == 's')
					throw new ParseException("xmlns as a ns prefix can't be re-declared" + formatLineNumber(byte_offset));
			}
		}
	}

	/**
	 * This method parses the XML file and returns a boolean indicating if it is
	 * successful or not.When set to true, VTDGen conforms to XML namespace 1.0
	 * spec
	 * 
	 * @param fileName
	 * @param ns
	 *            namespace aware or not
	 * @return boolean indicating whether the parseFile is a success
	 * 
	 */
	public boolean parseFile(String fileName, boolean ns) {
		FileInputStream fis = null;
		File f = null;
		try {
			f = new File(fileName);
			fis = new FileInputStream(f);
			byte[] b = new byte[(int) f.length()];

			// fis.read(b);

			int offset = 0;
			int numRead = 0;
			int numOfBytes = 1048576;// I choose this value randomally,
			// any other (not too big) value also can be here.
			if (b.length - offset < numOfBytes) {
				numOfBytes = b.length - offset;
			}
			while (offset < b.length && (numRead = fis.read(b, offset, numOfBytes)) >= 0) {
				offset += numRead;
				if (b.length - offset < numOfBytes) {
					numOfBytes = b.length - offset;
				}
			}
			// fis.read(b);
			this.setDoc(b);
			this.parse(ns); // set namespace awareness to true
			return true;
		} catch (java.io.IOException e) {
			System.out.println("IOException: " + e);
		} catch (ParseException e) {
			System.out.println("ParserException: " + e);
		} finally {
			if (fis != null) {
				try {
					fis.close();
				} catch (Exception e) {
				}
			}
		}
		return false;
	}

	/**
	 * This method inflates then parses GZIP'ed XML file and returns a boolean
	 * indicating if it is successful or not.When set to true, VTDGen conforms
	 * to XML namespace 1.0 spec
	 * 
	 * @param fileName
	 * @param ns
	 * @return
	 */
	public boolean parseGZIPFile(String GZIPfileName, boolean ns) {
		FileInputStream fis = null;
		// File f = null;
		try {
			fis = new FileInputStream(GZIPfileName);
			InputStream in = new GZIPInputStream(fis);
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			byte[] ba = new byte[65536];
			int noRead;
			while ((noRead = in.read(ba)) != -1) {
				baos.write(ba, 0, noRead);
			}
			this.setDoc(baos.toByteArray());
			this.parse(ns);
			return true;
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ParseException e) {
			System.out.println("ParserException: " + e);
		} finally {
			if (fis != null) {
				try {
					fis.close();
				} catch (Exception e) {
				}
			}
		}
		return false;
	}

	/**
	 * This method inflates then parses ZIP'ed XML file and returns a boolean
	 * indicating if it is successful or not.When set to true, VTDGen conforms
	 * to XML namespace 1.0 spec
	 * 
	 * @param ZIPfileName
	 * @param XMLName
	 * @param ns
	 * @return
	 */
	public boolean parseZIPFile(String ZIPfileName, String XMLName, boolean ns) {
		InputStream is = null;
		ZipFile zf = null;

		try {
			zf = new ZipFile(ZIPfileName);
			is = zf.getInputStream(zf.getEntry(XMLName));
			// InputStream in = new ZipInputStream(fis);
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			byte[] ba = new byte[65536];
			int noRead;
			while ((noRead = is.read(ba)) != -1) {
				baos.write(ba, 0, noRead);
			}
			this.setDoc(baos.toByteArray());
			this.parse(ns);
			return true;
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ParseException e) {
			System.out.println("ParserException: " + e);
		} finally {
			if (zf != null) {
				try {
					zf.close();
				} catch (Exception e) {
				}
			}
		}
		return false;
	}

	/**
	 * This method retrieves an XML document from the net using HTTP request If
	 * the returned content type is "application xml" then it will proceed with
	 * the parsing. Also notice that the content size can't be zero or negative,
	 * it must be a positive integer matching the size of the document
	 * 
	 * no exception is thrown in the case of failure, this method will simply
	 * return false
	 * 
	 * @param url
	 * @return boolean (status of parsing the XML referenced by the HTTP
	 *         request)
	 * 
	 */
	public boolean parseHttpUrl(String url, boolean ns) {
		URL url1 = null;
		InputStream in = null;
		HttpURLConnection urlConnection = null;
		try {
			url1 = new URL(url);
			in = url1.openStream();
			urlConnection = (HttpURLConnection) url1.openConnection();
			if (urlConnection.getResponseCode() == HttpURLConnection.HTTP_OK) {
				int len = urlConnection.getContentLength();
				if (len > 0) {
					// System.out.println("len  ===> " + len + "  "
					// + urlConnection.getContentType());
					byte[] ba = new byte[len];
					int k = len, offset = 0;
					while (offset < len & k > 0) {
						k = in.read(ba, offset, len - offset);
						offset += k;
					}
					this.setDoc(ba);
					this.parse(ns);
					return true;
				} else {
					ByteArrayOutputStream baos = new ByteArrayOutputStream();
					byte[] ba = new byte[4096];
					int k = -1;
					while ((k = in.read(ba)) > 0) {
						baos.write(ba, 0, k);
					}
					this.setDoc(baos.toByteArray());
					this.parse(ns);
					return true;
					// baos.w
				}
			}
		} catch (IOException e) {

		} catch (ParseException e) {

		} finally {
			try {
				if (in != null)
					in.close();
				if (urlConnection != null)
					urlConnection.disconnect();
			} catch (Exception e) {
			}
		}
		return false;
	}

	// private

	/**
	 * This private method processes CDATA section
	 * 
	 * @return the parser state after which the parser loop jumps to
	 * @throws ParseException
	 * @throws EncodingException
	 * @throws EOFException
	 */
	private int process_cdata() throws ParseException, EncodingException, EOFException {
		int parser_state;
		while (true) {
			ch = r.getChar();
			if (XMLChar.isValidChar(ch)) {
				if (ch == ']' && r.skipChar(']')) {
					while (r.skipChar(']'))
						;
					if (r.skipChar('>')) {
						break;
					} /*
					 * else throw new ParseException(
					 * "Error in CDATA: Invalid termination sequence" +
					 * formatLineNumber());
					 */
				}
			} else
				throw new ParseException("Error in CDATA: Invalid Char" + formatLineNumber());
		}
		length1 = offset - temp_offset - (increment << 1) - increment;
		if (singleByteEncoding) {// if (encoding < FORMAT_UTF_16BE){

			writeVTDText(temp_offset, length1, TOKEN_CDATA_VAL, depth);
		} else {

			writeVTDText(temp_offset >> 1, length1 >> 1, TOKEN_CDATA_VAL, depth);
		}
		// System.out.println(" " + (temp_offset) + " " + length1 + " CDATA " +
		// depth);
		temp_offset = offset;
		// ch = getCharAfterSe();
		ch = getCharAfterS();

		if (ch == '<') {
			if (ws)
				addWhiteSpaceRecord();
			parser_state = STATE_LT_SEEN;
		} else if (XMLChar.isContentChar(ch)) {
			// temp_offset = offset-1;
			parser_state = STATE_TEXT;
		} else if (ch == '&') {
			// has_amp = true;
			// temp_offset = offset-1;
			entityIdentifier();
			parser_state = STATE_TEXT;
			// temp_offset = offset;
		} else if (ch == ']') {
			// temp_offset = offset-1;
			if (r.skipChar(']')) {
				while (r.skipChar(']')) {
				}
				if (r.skipChar('>'))
					throw new ParseException("Error in text content: ]]> in text content" + formatLineNumber());
			}
			parser_state = STATE_TEXT;
		} else
			throw new ParseException("Other Error: Invalid char in xml" + formatLineNumber());
		return parser_state;
	}

	/**
	 * This private method process comment
	 * 
	 * @return the parser state after which the parser loop jumps to
	 * @throws ParseException
	 * @throws EncodingException
	 * @throws EOFException
	 */
	private int process_comment() throws ParseException, EncodingException, EOFException {
		int parser_state;
		while (true) {
			ch = r.getChar();
			if (XMLChar.isValidChar(ch)) {
				if (ch == '-' && r.skipChar('-')) {
					length1 = offset - temp_offset - (increment << 1);
					break;
				}
			} else
				throw new ParseException("Error in comment: Invalid Char" + formatLineNumber());
		}
		if (r.getChar() == '>') {
			// System.out.println(" " + (temp_offset) + " " + length1 +
			// " comment " + depth);
			if (singleByteEncoding)// if (encoding < FORMAT_UTF_16BE)
				writeVTDText(temp_offset, length1, TOKEN_COMMENT, depth);
			else
				writeVTDText(temp_offset >> 1, length1 >> 1, TOKEN_COMMENT, depth);
			// length1 = 0;
			temp_offset = offset;
			// ch = getCharAfterSe();
			ch = getCharAfterS();
			if (ch == '<') {
				if (ws)
					addWhiteSpaceRecord();
				parser_state = STATE_LT_SEEN;
			} else if (XMLChar.isContentChar(ch)) {
				// temp_offset = offset;
				parser_state = STATE_TEXT;
			} else if (ch == '&') {
				// has_amp = true;
				// temp_offset = offset;
				entityIdentifier();
				parser_state = STATE_TEXT;
			} else if (ch == ']') {
				if (r.skipChar(']')) {
					while (r.skipChar(']')) {
					}
					if (r.skipChar('>'))
						throw new ParseException("Error in text content: ]]> in text content" + formatLineNumber());
				}
				parser_state = STATE_TEXT;
			} else
				throw new ParseException("Error in text content: Invalid char" + formatLineNumber());
			return parser_state;
		} else
			throw new ParseException("Error in comment: Invalid terminating sequence" + formatLineNumber());
	}

	/**
	 * This private method processes declaration attributes
	 * 
	 * @return the parser state after which the parser loop jumps to
	 * @throws ParseException
	 * @throws EncodingException
	 * @throws EOFException
	 */
	private int process_dec_attr() throws ParseException, EncodingException, EOFException {
		int parser_state;
		if (ch == 'v' && r.skipChar('e') && r.skipChar('r') && r.skipChar('s') && r.skipChar('i') && r.skipChar('o') && r.skipChar('n')) {
			ch = getCharAfterS();
			if (ch == '=') {
				/*
				 * System.out.println( " " + (temp_offset - 1) + " " + 7 +
				 * " dec attr name version " + depth);
				 */
				if (singleByteEncoding)
					_writeVTD(temp_offset - 1, 7, TOKEN_DEC_ATTR_NAME, depth);
				else
					_writeVTD((temp_offset - 2) >> 1, 7, TOKEN_DEC_ATTR_NAME, depth);
			} else
				throw new ParseException("XML decl error: Invalid char" + formatLineNumber());
		} else
			throw new ParseException("XML decl error: should be version" + formatLineNumber());
		ch_temp = getCharAfterS();
		if (ch_temp != '\'' && ch_temp != '"')
			throw new ParseException("XML decl error: Invalid char to start attr name" + formatLineNumber());
		temp_offset = offset;
		// support 1.0 or 1.1
		if (r.skipChar('1') && r.skipChar('.') && (r.skipChar('0') || r.skipChar('1'))) {
			/*
			 * System.out.println( " " + temp_offset + " " + 3 +
			 * " dec attr val (version)" + depth);
			 */
			if (singleByteEncoding)
				_writeVTD(temp_offset, 3, TOKEN_DEC_ATTR_VAL, depth);
			else
				_writeVTD(temp_offset >> 1, 3, TOKEN_DEC_ATTR_VAL, depth);
		} else
			throw new ParseException("XML decl error: Invalid version(other than 1.0 or 1.1) detected" + formatLineNumber());
		if (!r.skipChar(ch_temp))
			throw new ParseException("XML decl error: version not terminated properly" + formatLineNumber());
		ch = r.getChar();
		// ? space or e
		if (XMLChar.isSpaceChar(ch)) {
			ch = getCharAfterS();
			temp_offset = offset - increment;
			if (ch == 'e') {
				if (r.skipChar('n') && r.skipChar('c') && r.skipChar('o') && r.skipChar('d') && r.skipChar('i') && r.skipChar('n') && r.skipChar('g')) {
					ch = r.getChar();
					if (XMLChar.isSpaceChar(ch))
						ch = getCharAfterS();
					if (ch == '=') {
						/*
						 * System.out.println( " " + (temp_offset) + " " + 8 +
						 * " dec attr name (encoding) " + depth);
						 */
						if (singleByteEncoding)
							_writeVTD(temp_offset, 8, TOKEN_DEC_ATTR_NAME, depth);
						else
							_writeVTD(temp_offset >> 1, 8, TOKEN_DEC_ATTR_NAME, depth);
					} else
						throw new ParseException("XML decl error: Invalid char" + formatLineNumber());
					ch_temp = getCharAfterS();
					if (ch_temp != '"' && ch_temp != '\'')
						throw new ParseException("XML decl error: Invalid char to start attr name" + formatLineNumber());
					temp_offset = offset;
					ch = r.getChar();
					switch (ch) {
					case 'a':
					case 'A':
						if ((r.skipChar('s') || r.skipChar('S')) && (r.skipChar('c') || r.skipChar('C')) && (r.skipChar('i') || r.skipChar('I'))
								&& (r.skipChar('i') || r.skipChar('I')) && r.skipChar(ch_temp)) {
							if (encoding != FORMAT_UTF_16LE && encoding != FORMAT_UTF_16BE) {
								if (must_utf_8)
									throw new EncodingException("Can't switch from UTF-8" + formatLineNumber());
								encoding = FORMAT_ASCII;
								r = new ASCIIReader();
								/*
								 * System.out.println( " " + (temp_offset) + " "
								 * + 5 + " dec attr val (encoding) " + depth);
								 */

								_writeVTD(temp_offset, 5, TOKEN_DEC_ATTR_VAL, depth);

								break;
							} else
								throw new ParseException("XML decl error: Can't switch encoding to ASCII" + formatLineNumber());
						}
						throw new ParseException("XML decl error: Invalid Encoding" + formatLineNumber());
					case 'c':
					case 'C':
						matchCPEncoding();
						break;
					case 'i':
					case 'I':
						matchISOEncoding();
						break;
					case 'u':
					case 'U':
						matchUTFEncoding();
						break;
					// now deal with windows encoding
					case 'w':
					case 'W':
						matchWindowsEncoding();
						break;
					default:
						throw new ParseException("XML decl Error: invalid encoding" + formatLineNumber());
					}
					ch = r.getChar();
					if (XMLChar.isSpaceChar(ch))
						ch = getCharAfterS();
					temp_offset = offset - increment;
				} else
					throw new ParseException("XML decl Error: Invalid char" + formatLineNumber());
			}

			if (ch == 's') {
				if (r.skipChar('t') && r.skipChar('a') && r.skipChar('n') && r.skipChar('d') && r.skipChar('a') && r.skipChar('l') && r.skipChar('o') && r.skipChar('n')
						&& r.skipChar('e')) {

					ch = getCharAfterS();
					if (ch != '=')
						throw new ParseException("XML decl error: Invalid char" + formatLineNumber());
					/*
					 * System.out.println( " " + temp_offset + " " + 3 +
					 * " dec attr name (standalone) " + depth);
					 */
					if (singleByteEncoding)
						_writeVTD(temp_offset, 10, TOKEN_DEC_ATTR_NAME, depth);
					else
						_writeVTD(temp_offset >> 1, 10, TOKEN_DEC_ATTR_NAME, depth);
					ch_temp = getCharAfterS();
					temp_offset = offset;
					if (ch_temp != '"' && ch_temp != '\'')
						throw new ParseException("XML decl error: Invalid char to start attr name" + formatLineNumber());
					ch = r.getChar();
					if (ch == 'y') {
						if (r.skipChar('e') && r.skipChar('s') && r.skipChar(ch_temp)) {
							/*
							 * System.out.println( " " + (temp_offset) + " " + 3
							 * + " dec attr val (standalone) " + depth);
							 */
							if (singleByteEncoding)
								_writeVTD(temp_offset, 3, TOKEN_DEC_ATTR_VAL, depth);
							else
								_writeVTD(temp_offset >> 1, 3, TOKEN_DEC_ATTR_VAL, depth);
						} else
							throw new ParseException("XML decl error: invalid val for standalone" + formatLineNumber());
					} else if (ch == 'n') {
						if (r.skipChar('o') && r.skipChar(ch_temp)) {
							/*
							 * System.out.println( " " + (temp_offset) + " " + 2
							 * + " dec attr val (standalone)" + depth);
							 */
							if (singleByteEncoding)
								_writeVTD(temp_offset, 2, TOKEN_DEC_ATTR_VAL, depth);
							else
								_writeVTD(temp_offset >> 1, 2, TOKEN_DEC_ATTR_VAL, depth);
						} else
							throw new ParseException("XML decl error: invalid val for standalone" + formatLineNumber());
					} else
						throw new ParseException("XML decl error: invalid val for standalone" + formatLineNumber());
				} else
					throw new ParseException("XML decl error" + formatLineNumber());
				ch = r.getChar();
				if (XMLChar.isSpaceChar(ch))
					ch = getCharAfterS();
			}
		}

		if (ch == '?' && r.skipChar('>')) {
			temp_offset = offset;
			ch = getCharAfterS();
			if (ch == '<') {
				parser_state = STATE_LT_SEEN;
			} else
				throw new ParseException("Other Error: Invalid Char in XML" + formatLineNumber());
		} else
			throw new ParseException("XML decl Error: Invalid termination sequence" + formatLineNumber());
		return parser_state;
	}

	/**
	 * This private method process DTD
	 * 
	 * @return the parser state after which the parser loop jumps to
	 * @throws ParseException
	 * @throws EncodingException
	 * @throws EOFException
	 */
	private int process_doc_type() throws ParseException, EncodingException, EOFException {
		int z = 1, parser_state;
		while (true) {
			ch = r.getChar();
			if (XMLChar.isValidChar(ch)) {
				if (ch == '>')
					z--;
				else if (ch == '<')
					z++;
				if (z == 0)
					break;
			} else
				throw new ParseException("Error in DOCTYPE: Invalid char" + formatLineNumber());
		}
		length1 = offset - temp_offset - increment;
		/*
		 * System.out.println( " " + (temp_offset) + " " + length1 +
		 * " DOCTYPE val " + depth);
		 */
		if (singleByteEncoding) {// if (encoding < FORMAT_UTF_16BE){
			if (length1 > MAX_TOKEN_LENGTH)
				throw new ParseException("Token Length Error:" + " DTD val too long (>0xfffff)" + formatLineNumber());
			_writeVTD(temp_offset, length1, TOKEN_DTD_VAL, depth);
		} else {
			if (length1 > (MAX_TOKEN_LENGTH << 1))
				throw new ParseException("Token Length Error:" + " DTD val too long (>0xfffff)" + formatLineNumber());
			_writeVTD(temp_offset >> 1, length1 >> 1, TOKEN_DTD_VAL, depth);
		}
		ch = getCharAfterS();
		if (ch == '<') {
			parser_state = STATE_LT_SEEN;
		} else
			throw new ParseException("Other Error: Invalid char in xml" + formatLineNumber());
		return parser_state;
	}

	/**
	 * This private method process the comment after the root document
	 * 
	 * @return the parser state after which the parser loop jumps to
	 * @throws ParseException
	 */
	private int process_end_comment() throws ParseException {
		int parser_state;
		while (true) {
			ch = r.getChar();
			if (XMLChar.isValidChar(ch)) {
				if (ch == '-' && r.skipChar('-')) {
					length1 = offset - temp_offset - (increment << 1);
					break;
				}
			} else
				throw new ParseException("Error in comment: Invalid Char" + formatLineNumber());
		}
		if (r.getChar() == '>') {
			// System.out.println(" " + temp_offset + " " + length1 +
			// " comment " + depth);
			if (singleByteEncoding) // if (encoding < FORMAT_UTF_16BE)
				writeVTDText(temp_offset, length1, TOKEN_COMMENT, depth);
			else
				writeVTDText(temp_offset >> 1, length1 >> 1, TOKEN_COMMENT, depth);
			parser_state = STATE_DOC_END;
			return parser_state;
		}
		throw new ParseException("Error in comment: '-->' expected" + formatLineNumber());

	}

	private int process_end_doc() throws ParseException, EncodingException, EOFException {
		int parser_state;
		ch = getCharAfterS();
		/* eof exception should be thrown here for premature ending */
		if (ch == '<') {

			if (r.skipChar('?')) {
				/* processing instruction after end tag of root element */
				temp_offset = offset;
				parser_state = STATE_END_PI;
				return parser_state;
			} else if (r.skipChar('!') && r.skipChar('-') && r.skipChar('-')) {
				// comments allowed after the end tag of the root element
				temp_offset = offset;
				parser_state = STATE_END_COMMENT;
				return parser_state;
			}
		}
		throw new ParseException("Other Error: XML not terminated properly" + formatLineNumber());
	}

	/**
	 * This private method processes PI after root document
	 * 
	 * @return the parser state after which the parser loop jumps to
	 * @throws ParseException
	 * @throws EncodingException
	 * @throws EOFException
	 */
	private int process_end_pi() throws ParseException, EncodingException, EOFException {
		int parser_state;
		ch = r.getChar();
		if (XMLChar.isNameStartChar(ch)) {
			if ((ch == 'x' || ch == 'X') && (r.skipChar('m') || r.skipChar('M')) && (r.skipChar('l') && r.skipChar('L'))) {
				// temp_offset = offset;
				ch = r.getChar();
				if (XMLChar.isSpaceChar(ch) || ch == '?')
					throw new ParseException("Error in PI: [xX][mM][lL] not a valid PI target" + formatLineNumber());
				// offset = temp_offset;
			}

			while (true) {
				// ch = getChar();
				if (!XMLChar.isNameChar(ch)) {
					break;
				}
				ch = r.getChar();
			}

			length1 = offset - temp_offset - increment;
			/*
			 * System.out.println( "" + (char) XMLDoc[temp_offset] + " " +
			 * (temp_offset) + " " + length1 + " PI Target " + depth);
			 */
			if (singleByteEncoding) {// if (encoding < FORMAT_UTF_16BE){
				if (length1 > MAX_TOKEN_LENGTH)
					throw new ParseException("Token Length Error:" + "PI name too long (>0xfffff)" + formatLineNumber());
				_writeVTD(temp_offset, length1, TOKEN_PI_NAME, depth);
			} else {
				if (length1 > (MAX_TOKEN_LENGTH << 1))
					throw new ParseException("Token Length Error:" + "PI name too long (>0xfffff)" + formatLineNumber());
				_writeVTD(temp_offset >> 1, length1 >> 1, TOKEN_PI_NAME, depth);
			}
			// length1 = 0;
			temp_offset = offset;
			if (XMLChar.isSpaceChar(ch)) {
				ch = getCharAfterS();

				while (true) {
					if (XMLChar.isValidChar(ch)) {
						if (ch == '?') {
							if (r.skipChar('>')) {
								parser_state = STATE_DOC_END;
								break;
							} else
								throw new ParseException("Error in PI: invalid termination sequence" + formatLineNumber());
						}
					} else
						throw new ParseException("Error in PI: Invalid char in PI val" + formatLineNumber());
					ch = r.getChar();
				}
				length1 = offset - temp_offset - (increment << 1);
				if (singleByteEncoding) {
					if (length1 > MAX_TOKEN_LENGTH)
						throw new ParseException("Token Length Error:" + "PI val too long (>0xfffff)" + formatLineNumber());
					_writeVTD(temp_offset, length1, TOKEN_PI_VAL, depth);
				} else {
					if (length1 > (MAX_TOKEN_LENGTH << 1))
						throw new ParseException("Token Length Error:" + "PI val too long (>0xfffff)" + formatLineNumber());
					_writeVTD(temp_offset >> 1, length1 >> 1, TOKEN_PI_VAL, depth);
				}
				// System.out.println(" " + temp_offset + " " + length1 +
				// " PI val " + depth);
			} else {
				if (singleByteEncoding) {
					_writeVTD((temp_offset), 0, TOKEN_PI_VAL, depth);
				} else {
					_writeVTD((temp_offset) >> 1, 0, TOKEN_PI_VAL, depth);
				}
				if ((ch == '?') && r.skipChar('>')) {
					parser_state = STATE_DOC_END;
				} else
					throw new ParseException("Error in PI: invalid termination sequence" + formatLineNumber());
			}
			// parser_state = STATE_DOC_END;
		} else
			throw new ParseException("Error in PI: invalid char in PI target" + formatLineNumber());
		return parser_state;
	}

	private int process_ex_seen() throws ParseException, EncodingException, EOFException {
		int parser_state;
		boolean hasDTD = false;
		ch = r.getChar();
		switch (ch) {
		case '-':
			if (r.skipChar('-')) {
				temp_offset = offset;
				parser_state = STATE_COMMENT;
				break;
			} else
				throw new ParseException("Error in comment: Invalid char sequence to start a comment" + formatLineNumber());
		case '[':
			if (r.skipChar('C') && r.skipChar('D') && r.skipChar('A') && r.skipChar('T') && r.skipChar('A') && r.skipChar('[') && (depth != -1)) {
				temp_offset = offset;
				parser_state = STATE_CDATA;
				break;
			} else {
				if (depth == -1)
					throw new ParseException("Error in CDATA: Wrong place for CDATA" + formatLineNumber());
				throw new ParseException("Error in CDATA: Invalid char sequence for CDATA" + formatLineNumber());
			}

		case 'D':
			if (r.skipChar('O') && r.skipChar('C') && r.skipChar('T') && r.skipChar('Y') && r.skipChar('P') && r.skipChar('E') && (depth == -1) && !hasDTD) {
				hasDTD = true;
				temp_offset = offset;
				parser_state = STATE_DOCTYPE;
				break;
			} else {
				if (hasDTD == true)
					throw new ParseException("Error for DOCTYPE: Only DOCTYPE allowed" + formatLineNumber());
				if (depth != -1)
					throw new ParseException("Error for DOCTYPE: DTD at wrong place" + formatLineNumber());
				throw new ParseException("Error for DOCTYPE: Invalid char sequence for DOCTYPE" + formatLineNumber());
			}
		default:
			throw new ParseException("Other Error: Unrecognized char after <!" + formatLineNumber());
		}
		return parser_state;
	}

	/**
	 * This private method processes PI tag
	 * 
	 * @return the parser state after which the parser loop jumps to
	 * @throws ParseException
	 * @throws EncodingException
	 * @throws EOFException
	 */
	private int process_pi_tag() throws ParseException, EncodingException, EOFException {
		int parser_state;
		while (true) {
			ch = r.getChar();
			if (!XMLChar.isNameChar(ch))
				break;
			// System.out.println(" ch ==> "+(char)ch);
		}

		length1 = offset - temp_offset - increment;
		/*
		 * System.out.println( ((char) XMLDoc[temp_offset]) + " " +
		 * (temp_offset) + " " + length1 + " PI Target " + depth);
		 */
		// if (encoding < FORMAT_UTF_16BE){
		if (singleByteEncoding) {
			if (length1 > MAX_TOKEN_LENGTH)
				throw new ParseException("Token Length Error:" + " PI name too long (>0xfffff)" + formatLineNumber());
			_writeVTD((temp_offset), length1, TOKEN_PI_NAME, depth);
		} else {
			if (length1 > (MAX_TOKEN_LENGTH << 1))
				throw new ParseException("Token Length Error:" + " PI name too long (>0xfffff)" + formatLineNumber());
			_writeVTD((temp_offset) >> 1, (length1 >> 1), TOKEN_PI_NAME, depth);
		}
		// length1 = 0;
		// temp_offset = offset;
		/*
		 * if (XMLChar.isSpaceChar(ch)) { ch = r.getChar(); }
		 */
		// ch = r.getChar();
		if (ch == '?') {
			// insert zero length pi name tag
			if (singleByteEncoding) {
				_writeVTD((temp_offset), 0, TOKEN_PI_VAL, depth);
			} else {
				_writeVTD((temp_offset) >> 1, (0), TOKEN_PI_VAL, depth);
			}
			if (r.skipChar('>')) {
				temp_offset = offset;
				// ch = getCharAfterSe();
				ch = getCharAfterS();
				if (ch == '<') {
					if (ws)
						addWhiteSpaceRecord();
					parser_state = STATE_LT_SEEN;
				} else if (XMLChar.isContentChar(ch)) {
					parser_state = STATE_TEXT;
				} else if (ch == '&') {
					// has_amp = true;
					entityIdentifier();
					parser_state = STATE_TEXT;
				} else if (ch == ']') {
					if (r.skipChar(']')) {
						while (r.skipChar(']')) {
						}
						if (r.skipChar('>'))
							throw new ParseException("Error in text content: ]]> in text content" + formatLineNumber());
					}
					parser_state = STATE_TEXT;
				} else
					throw new ParseException("Error in text content: Invalid char" + formatLineNumber());
				return parser_state;
			} else
				throw new ParseException("Error in PI: invalid termination sequence" + formatLineNumber());
		}
		parser_state = STATE_PI_VAL;
		return parser_state;
	}

	/**
	 * This private method processes PI val
	 * 
	 * @return the parser state after which the parser loop jumps to
	 * @throws ParseException
	 * @throws EncodingException
	 * @throws EOFException
	 */
	private int process_pi_val() throws ParseException, EncodingException, EOFException {
		int parser_state;
		if (!XMLChar.isSpaceChar(ch))
			throw new ParseException("Error in PI: invalid termination sequence" + formatLineNumber());
		temp_offset = offset;
		ch = r.getChar();
		while (true) {
			if (XMLChar.isValidChar(ch)) {
				// System.out.println(""+(char)ch);
				if (ch == '?')
					if (r.skipChar('>')) {
						break;
					} /*
					 * else throw new ParseException(
					 * "Error in PI: invalid termination sequence for PI" +
					 * formatLineNumber());
					 */
			} else
				throw new ParseException("Errors in PI: Invalid char in PI val" + formatLineNumber());
			ch = r.getChar();
		}
		length1 = offset - temp_offset - (increment << 1);
		/*
		 * System.out.println( ((char) XMLDoc[temp_offset]) + " " +
		 * (temp_offset) + " " + length1 + " PI val " + depth);
		 */
		// if (length1 != 0)
		if (singleByteEncoding) {// if (encoding < FORMAT_UTF_16BE){
			if (length1 > MAX_TOKEN_LENGTH)
				throw new ParseException("Token Length Error:" + "PI VAL too long (>0xfffff)" + formatLineNumber());
			_writeVTD(temp_offset, length1, TOKEN_PI_VAL, depth);
		} else {
			if (length1 > (MAX_TOKEN_LENGTH << 1))
				throw new ParseException("Token Length Error:" + "PI VAL too long (>0xfffff)" + formatLineNumber());
			_writeVTD(temp_offset >> 1, length1 >> 1, TOKEN_PI_VAL, depth);
		}
		// length1 = 0;
		temp_offset = offset;
		// ch = getCharAfterSe();
		ch = getCharAfterS();
		if (ch == '<') {
			if (ws)
				addWhiteSpaceRecord();
			parser_state = STATE_LT_SEEN;
		} else if (XMLChar.isContentChar(ch)) {
			// temp_offset = offset;
			parser_state = STATE_TEXT;
		} else if (ch == '&') {
			// has_amp = true;
			// temp_offset = offset;
			entityIdentifier();
			parser_state = STATE_TEXT;
		} else if (ch == ']') {
			if (r.skipChar(']')) {
				while (r.skipChar(']')) {
				}
				if (r.skipChar('>'))
					throw new ParseException("Error in text content: ]]> in text content" + formatLineNumber());

			}
			parser_state = STATE_TEXT;
		} else
			throw new ParseException("Error in text content: Invalid char" + formatLineNumber());
		return parser_state;

	}

	private int process_qm_seen() throws ParseException, EncodingException, EOFException {
		temp_offset = offset;
		ch = r.getChar();
		if (XMLChar.isNameStartChar(ch)) {
			// temp_offset = offset;
			if ((ch == 'x' || ch == 'X') && (r.skipChar('m') || r.skipChar('M')) && (r.skipChar('l') || r.skipChar('L'))) {
				ch = r.getChar();
				if (ch == '?' || XMLChar.isSpaceChar(ch))
					throw new ParseException("Error in PI: [xX][mM][lL] not a valid PI targetname" + formatLineNumber());
				offset = getPrevOffset();
			}
			return STATE_PI_TAG;
		}
		throw new ParseException("Other Error: First char after <? invalid" + formatLineNumber());
	}

	private int process_start_doc() throws ParseException, EncodingException, EOFException {
		int c = r.getChar();
		if (c == '<') {
			temp_offset = offset;
			// xml decl has to be right after the start of the document
			if (r.skipChar('?') && (r.skipChar('x') || r.skipChar('X')) && (r.skipChar('m') || r.skipChar('M')) && (r.skipChar('l') || r.skipChar('L'))) {
				if (r.skipChar(' ') || r.skipChar('\t') || r.skipChar('\n') || r.skipChar('\r')) {
					ch = getCharAfterS();
					temp_offset = offset;
					return STATE_DEC_ATTR_NAME;
				} else if (r.skipChar('?'))
					throw new ParseException("Error in XML decl: Premature ending" + formatLineNumber());
			}
			offset = temp_offset;
			return STATE_LT_SEEN;
		} else if (c == ' ' || c == '\n' || c == '\r' || c == '\t') {
			if (getCharAfterS() == '<') {
				return STATE_LT_SEEN;
			}
		}
		throw new ParseException("Other Error: XML not starting properly" + formatLineNumber());
	}

	/**
	 * Set the XMLDoc container.
	 * 
	 * @param ba
	 *            byte[]
	 */
	public void setDoc(byte[] ba) {
		setDoc(ba, 0, ba.length);
	}

	/**
	 * Set the XMLDoc container. Also set the offset and len of the document
	 * with respect to the container.
	 * 
	 * @param ba
	 *            byte[]
	 * @param os
	 *            int (in byte)
	 * @param len
	 *            int (in byte)
	 */
	public void setDoc(byte[] ba, int os, int len) {
		if (ba == null || os < 0 || len == 0 || ba.length < os + len) {
			throw new IllegalArgumentException("Illegal argument for setDoc");
		}
		int a;
		br = false;
		depth = -1;
		increment = 1;
		BOM_detected = false;
		must_utf_8 = false;
		ch = ch_temp = 0;
		temp_offset = 0;
		XMLDoc = ba;
		docOffset = offset = os;
		docLen = len;
		endOffset = os + len;
		last_l1_index = last_l2_index = last_l3_index = last_l4_index = last_depth = 0;

		currentElementRecord = 0;
		nsBuffer1.size = 0;
		nsBuffer2.size = 0;
		nsBuffer3.size = 0;
		r = new UTF8Reader();
		if (shallowDepth) {
			int i1 = 8, i2 = 9, i3 = 11;
			if (docLen <= 1024) {
				// a = 1024; //set the floor
				a = 6;
				i1 = 5;
				i2 = 5;
				i3 = 5;
			} else if (docLen <= 4096) {
				a = 7;
				i1 = 6;
				i2 = 6;
				i3 = 6;
			} else if (docLen <= 1024 * 16) {
				a = 8;
				i1 = 7;
				i2 = 7;
				i3 = 7;
			} else if (docLen <= 1024 * 16 * 4) {
				// a = 2048;
				a = 11;
			} else if (docLen <= 1024 * 256) {
				// a = 1024 * 4;
				a = 12;
			} else {
				// a = 1 << 15;
				a = 15;
			}

			VTDBuffer = new FastLongBuffer(a, len >> (a + 1));
			l1Buffer = new FastLongBuffer(i1);
			l2Buffer = new FastLongBuffer(i2);
			l3Buffer = new FastIntBuffer(i3);
		} else {

			int i1 = 7, i2 = 9, i3 = 11, i4 = 11, i5 = 11;
			if (docLen <= 1024) {
				// a = 1024; //set the floor
				a = 6;
				i1 = 5;
				i2 = 5;
				i3 = 5;
				i4 = 5;
				i5 = 5;
			} else if (docLen <= 4096) {
				a = 7;
				i1 = 6;
				i2 = 6;
				i3 = 6;
				i4 = 6;
				i5 = 6;
			} else if (docLen <= 1024 * 16) {
				a = 8;
				i1 = 7;
				i2 = 7;
				i3 = 7;
				i4 = 7;
				i5 = 7;
			} else if (docLen <= 1024 * 16 * 4) {
				// a = 2048;
				a = 11;
				i2 = 8;
				i3 = 8;
				i4 = 8;
				i5 = 8;
			} else if (docLen <= 1024 * 256) {
				// a = 1024 * 4;
				a = 12;
				i1 = 8;
				i2 = 9;
				i3 = 9;
				i4 = 9;
				i5 = 9;
			} else {
				// a = 1 << 15;
				a = 15;
			}

			VTDBuffer = new FastLongBuffer(a, len >> (a + 1));
			l1Buffer = new FastLongBuffer(i1);
			l2Buffer = new FastLongBuffer(i2);
			_l3Buffer = new FastLongBuffer(i3);
			_l4Buffer = new FastLongBuffer(i4);
			_l5Buffer = new FastIntBuffer(i5);
		}
	}

	/**
	 * The buffer-reuse version of setDoc The concept is to reuse LC and VTD
	 * buffer for XML parsing, instead of allocating every time
	 * 
	 * @param ba
	 * 
	 */
	public void setDoc_BR(byte[] ba) {
		setDoc_BR(ba, 0, ba.length);
	}

	/**
	 * The buffer-reuse version of setDoc The concept is to reuse LC and VTD
	 * buffer for XML parsing, instead of allocating every time
	 * 
	 * @param ba
	 *            byte[]
	 * @param os
	 *            int (in byte)
	 * @param len
	 *            int (in byte)
	 * 
	 */
	public void setDoc_BR(byte[] ba, int os, int len) {
		if (ba == null || os < 0 || len == 0 || ba.length < os + len) {
			throw new IllegalArgumentException("Illegal argument for setDoc_BR");
		}
		int a;
		br = true;
		depth = -1;
		increment = 1;
		BOM_detected = false;
		must_utf_8 = false;
		ch = ch_temp = 0;
		temp_offset = 0;
		XMLDoc = ba;
		docOffset = offset = os;
		docLen = len;
		endOffset = os + len;
		last_l1_index = last_l2_index = last_depth = last_l3_index = last_l4_index = 0;
		currentElementRecord = 0;
		nsBuffer1.size = 0;
		nsBuffer2.size = 0;
		nsBuffer3.size = 0;
		r = new UTF8Reader();
		if (shallowDepth) {
			int i1 = 8, i2 = 9, i3 = 11;
			if (docLen <= 1024) {
				// a = 1024; //set the floor
				a = 6;
				i1 = 5;
				i2 = 5;
				i3 = 5;
			} else if (docLen <= 4096) {
				a = 7;
				i1 = 6;
				i2 = 6;
				i3 = 6;
			} else if (docLen <= 1024 * 16) {
				a = 8;
				i1 = 7;
				i2 = 7;
				i3 = 7;
			} else if (docLen <= 1024 * 16 * 4) {
				// a = 2048;
				a = 11;
				i2 = 8;
				i3 = 8;
			} else if (docLen <= 1024 * 256) {
				// a = 1024 * 4;
				a = 12;
			} else {
				// a = 1 << 15;
				a = 15;
			}
			if (VTDBuffer == null) {
				VTDBuffer = new FastLongBuffer(a, len >> (a + 1));
				l1Buffer = new FastLongBuffer(i1);
				l2Buffer = new FastLongBuffer(i2);
				l3Buffer = new FastIntBuffer(i3);
			} else {
				VTDBuffer.size = 0;
				l1Buffer.size = 0;
				l2Buffer.size = 0;
				l3Buffer.size = 0;
			}
		} else {
			int i1 = 8, i2 = 9, i3 = 11, i4 = 11, i5 = 11;
			if (docLen <= 1024) {
				// a = 1024; //set the floor
				a = 6;
				i1 = 5;
				i2 = 5;
				i3 = 5;
				i4 = 5;
				i5 = 5;
			} else if (docLen <= 4096) {
				a = 7;
				i1 = 6;
				i2 = 6;
				i3 = 6;
				i4 = 6;
				i5 = 6;
			} else if (docLen <= 1024 * 16) {
				a = 8;
				i1 = 7;
				i2 = 7;
				i3 = 7;
			} else if (docLen <= 1024 * 16 * 4) {
				// a = 2048;
				a = 11;
				i2 = 8;
				i3 = 8;
				i4 = 8;
				i5 = 8;
			} else if (docLen <= 1024 * 256) {
				// a = 1024 * 4;
				a = 12;
				i1 = 8;
				i2 = 9;
				i3 = 9;
				i4 = 9;
				i5 = 9;
			} else if (docLen <= 1024 * 1024) {
				// a = 1024 * 4;
				a = 12;
				i1 = 8;
				i3 = 10;
				i4 = 10;
				i5 = 10;
			} else {
				// a = 1 << 15;
				a = 15;
				i1 = 8;
			}
			if (VTDBuffer == null) {
				VTDBuffer = new FastLongBuffer(a, len >> (a + 1));
				l1Buffer = new FastLongBuffer(i1);
				l2Buffer = new FastLongBuffer(i2);
				_l3Buffer = new FastLongBuffer(i3);
				_l4Buffer = new FastLongBuffer(i4);
				_l5Buffer = new FastIntBuffer(i5);
			} else {
				VTDBuffer.size = 0;
				l1Buffer.size = 0;
				l2Buffer.size = 0;
				_l3Buffer.size = 0;
				_l4Buffer.size = 0;
				_l5Buffer.size = 0;
			}
		}
	}

	/**
	 * This method writes the VTD+XML into an outputStream
	 * 
	 * @param os
	 * @throws IOException
	 * @throws IndexWriteException
	 * 
	 */
	public void writeIndex(OutputStream os) throws IOException, IndexWriteException {
		if (shallowDepth)
			IndexHandler.writeIndex_L3((byte) 1, this.encoding, this.ns, true, this.VTDDepth, 3, this.rootIndex, this.XMLDoc, this.docOffset, this.docLen, this.VTDBuffer,
					this.l1Buffer, this.l2Buffer, this.l3Buffer, os);
		else
			IndexHandler.writeIndex_L5((byte) 1, this.encoding, this.ns, true, this.VTDDepth, 5, this.rootIndex, this.XMLDoc, this.docOffset, this.docLen, this.VTDBuffer,
					this.l1Buffer, this.l2Buffer, this._l3Buffer, this._l4Buffer, this._l5Buffer, os);
	}

	/**
	 * This method writes the VTDs and LCs into an outputStream
	 * 
	 * @param os
	 * @throws IOException
	 * @throws IndexWriteException
	 * 
	 */
	public void writeSeparateIndex(OutputStream os) throws IOException, IndexWriteException {
		if (shallowDepth)
			IndexHandler.writeSeparateIndex_L3((byte) 2, this.encoding, this.ns, true, this.VTDDepth, 3, this.rootIndex,
			// this.XMLDoc,
					this.docOffset, this.docLen, this.VTDBuffer, this.l1Buffer, this.l2Buffer, this.l3Buffer, os);
		else
			IndexHandler.writeSeparateIndex_L5((byte) 2, this.encoding, this.ns, true, this.VTDDepth, 5, this.rootIndex,
			// this.XMLDoc,
					this.docOffset, this.docLen, this.VTDBuffer, this.l1Buffer, this.l2Buffer, this._l3Buffer, this._l4Buffer, this._l5Buffer, os);
	}

	/**
	 * This method writes the VTD+XML file into a file of the given name
	 * 
	 * @param fileName
	 * @throws IOException
	 * @throws IndexWriteException
	 * 
	 */
	public void writeIndex(String fileName) throws IOException, IndexWriteException {
		FileOutputStream fos = new FileOutputStream(fileName);
		writeIndex(fos);
		fos.close();
	}

	/**
	 * This method writes the VTDs and LCs into a file of the given name XML is
	 * not part of the index please refer to VTD-XML web site for the spec and
	 * explanation
	 * 
	 * @param fileName
	 * @throws IOException
	 * @throws IndexWriteException
	 * 
	 */
	public void writeSeparateIndex(String fileName) throws IOException, IndexWriteException {
		FileOutputStream fos = new FileOutputStream(fileName);
		writeSeparateIndex(fos);
		fos.close();
	}

	/**
	 * Write the VTD and LC into their storage container for where LC depth is
	 * 5.
	 * 
	 * @param offset
	 *            int
	 * @param length
	 *            int
	 * @param token_type
	 *            int
	 * @param depth
	 *            int
	 */
	private void writeVTD(int offset, int length, int token_type, int depth) {

		VTDBuffer.append(((long) ((token_type << 28) | ((depth & 0xff) << 20) | length) << 32) | offset);

		switch (depth) {
		case 0:
			rootIndex = VTDBuffer.size - 1;
			break;
		case 1:
			if (last_depth == 1) {
				l1Buffer.append(((long) last_l1_index << 32) | 0xffffffffL);
			} else if (last_depth == 2) {
				l2Buffer.append(((long) last_l2_index << 32) | 0xffffffffL);
			}
			last_l1_index = VTDBuffer.size - 1;
			last_depth = 1;
			break;
		case 2:
			if (last_depth == 1) {
				l1Buffer.append(((long) last_l1_index << 32) + l2Buffer.size);
			} else if (last_depth == 2) {
				l2Buffer.append(((long) last_l2_index << 32) | 0xffffffffL);
			}
			last_l2_index = VTDBuffer.size - 1;
			last_depth = 2;
			break;

		case 3:
			l3Buffer.append(VTDBuffer.size - 1);
			if (last_depth == 2) {
				l2Buffer.append(((long) last_l2_index << 32) + l3Buffer.size - 1);
			}
			last_depth = 3;
			break;
		default:
			// rootIndex = VTDBuffer.size() - 1;
		}
	}

	private void _writeVTD(int offset, int length, int token_type, int depth) {
		VTDBuffer.append(((long) ((token_type << 28) | ((depth & 0xff) << 20) | length) << 32) | offset);
	}

	private void writeVTDText(int offset, int length, int token_type, int depth) {
		if (length > MAX_TOKEN_LENGTH) {
			int k;
			int r_offset = offset;
			for (k = length; k > MAX_TOKEN_LENGTH; k = k - MAX_TOKEN_LENGTH) {
				VTDBuffer.append(((long) ((token_type << 28) | ((depth & 0xff) << 20) | MAX_TOKEN_LENGTH) << 32) | r_offset);
				r_offset += MAX_TOKEN_LENGTH;
			}
			VTDBuffer.append(((long) ((token_type << 28) | ((depth & 0xff) << 20) | k) << 32) | r_offset);
		} else {
			VTDBuffer.append(((long) ((token_type << 28) | ((depth & 0xff) << 20) | length) << 32) | offset);
		}
	}

	/**
	 * Write the VTD and LC into their storage container.
	 * 
	 * @param offset
	 *            int
	 * @param length
	 *            int
	 * @param token_type
	 *            int
	 * @param depth
	 *            int
	 */
	private void writeVTD_L5(int offset, int length, int token_type, int depth) {

		VTDBuffer.append(((long) ((token_type << 28) | ((depth & 0xff) << 20) | length) << 32) | offset);

		switch (depth) {
		case 0:
			rootIndex = VTDBuffer.size - 1;
			break;
		case 1:
			if (last_depth == 1) {
				l1Buffer.append(((long) last_l1_index << 32) | 0xffffffffL);
			} else if (last_depth == 2) {
				l2Buffer.append(((long) last_l2_index << 32) | 0xffffffffL);
			} else if (last_depth == 3) {
				_l3Buffer.append(((long) last_l3_index << 32) | 0xffffffffL);
			} else if (last_depth == 4) {
				_l4Buffer.append(((long) last_l4_index << 32) | 0xffffffffL);
			}
			last_l1_index = VTDBuffer.size - 1;
			last_depth = 1;
			break;
		case 2:
			if (last_depth == 1) {
				l1Buffer.append(((long) last_l1_index << 32) + l2Buffer.size);
			} else if (last_depth == 2) {
				l2Buffer.append(((long) last_l2_index << 32) | 0xffffffffL);
			} else if (last_depth == 3) {
				_l3Buffer.append(((long) last_l3_index << 32) | 0xffffffffL);
			} else if (last_depth == 4) {
				_l4Buffer.append(((long) last_l4_index << 32) | 0xffffffffL);
			}
			last_l2_index = VTDBuffer.size - 1;
			last_depth = 2;
			break;

		case 3:
			/*
			 * if (last_depth == 1) { l1Buffer.append(((long) last_l1_index <<
			 * 32) + l2Buffer.size); } else
			 */
			if (last_depth == 2) {
				l2Buffer.append(((long) last_l2_index << 32) + _l3Buffer.size);
			} else if (last_depth == 3) {
				_l3Buffer.append(((long) last_l3_index << 32) | 0xffffffffL);
			} else if (last_depth == 4) {
				_l4Buffer.append(((long) last_l4_index << 32) | 0xffffffffL);
			}
			last_l3_index = VTDBuffer.size - 1;
			last_depth = 3;
			break;

		case 4:
			/*
			 * if (last_depth == 1) { l1Buffer.append(((long) last_l1_index <<
			 * 32) + l2Buffer.size); } else if (last_depth == 2) {
			 * l2Buffer.append(((long) last_l2_index << 32) | 0xffffffffL); }
			 * else
			 */
			if (last_depth == 3) {
				_l3Buffer.append(((long) last_l3_index << 32) + _l4Buffer.size);
			} else if (last_depth == 4) {
				_l4Buffer.append(((long) last_l4_index << 32) | 0xffffffffL);
			}
			last_l4_index = VTDBuffer.size - 1;
			last_depth = 4;
			break;
		case 5:
			_l5Buffer.append(VTDBuffer.size - 1);
			if (last_depth == 4) {
				_l4Buffer.append(((long) last_l4_index << 32) + _l5Buffer.size - 1);
			}
			last_depth = 5;
			break;

		// default:
		// rootIndex = VTDBuffer.size() - 1;
		}
	}

	/**
	 * 
	 * @throws ParseException
	 */
	private void qualifyElement() throws ParseException {
		int i = nsBuffer3.size - 1;
		// two cases:
		// 1. the current element has no prefix, look for xmlns
		// 2. the current element has prefix, look for xmlns:something

		int preLen = (int) ((currentElementRecord & 0xffff000000000000L) >> 48);
		int preOs = (int) currentElementRecord;
		while (i >= 0) {
			int t = nsBuffer3.upper32At(i);
			// with prefix, get full length and prefix length
			if ((t & 0xffff) - (t >> 16) == preLen) {
				// doing byte comparison here
				int os = nsBuffer3.lower32At(i) + (t >> 16) + increment;
				int k = 0;
				for (; k < preLen - increment; k++) {
					if (XMLDoc[os + k] != XMLDoc[preOs + k])
						break;
				}
				if (k == preLen - increment)
					return; // found the match
			}
			/*
			 * if ( (nsBuffer3.upper32At(i) & 0xffff0000) == 0){ return; }
			 */
			i--;
		}
		// no need to check if xml is the prefix
		if (checkPrefix(preOs, preLen))
			return;

		// print line # column# and full element name
		throw new ParseException("Name space qualification Exception: Element not qualified\n" + formatLineNumber((int) currentElementRecord));
	}

	private boolean checkPrefix(int os, int len) {
		// int i=0;
		if (encoding < FORMAT_UTF_16BE) {
			if (len == 4 && XMLDoc[os] == 'x' && XMLDoc[os + 1] == 'm' && XMLDoc[os + 2] == 'l') {
				return true;
			}
		} else if (encoding == FORMAT_UTF_16BE) {
			if (len == 8 && XMLDoc[os] == 0 && XMLDoc[os + 1] == 'x' && XMLDoc[os + 2] == 0 && XMLDoc[os + 3] == 'm' && XMLDoc[os + 4] == 0 && XMLDoc[os + 5] == 'l') {
				return true;
			}
		} else {
			if (len == 8 && XMLDoc[os] == 'x' && XMLDoc[os + 1] == 0 && XMLDoc[os + 2] == 'm' && XMLDoc[os + 3] == 0 && XMLDoc[os + 4] == 'l' && XMLDoc[os + 5] == 0) {
				return true;
			}
		}
		return false;
	}

	private boolean checkPrefix2(int os, int len) {
		// int i=0;
		if (encoding < FORMAT_UTF_16BE) {
			if (len == 5 && XMLDoc[os] == 'x' && XMLDoc[os + 1] == 'm' && XMLDoc[os + 2] == 'l' && XMLDoc[os + 3] == 'n' && XMLDoc[os + 4] == 's') {
				return true;
			}
		} else if (encoding == FORMAT_UTF_16BE) {
			if (len == 10 && XMLDoc[os] == 0 && XMLDoc[os + 1] == 'x' && XMLDoc[os + 2] == 0 && XMLDoc[os + 3] == 'm' && XMLDoc[os + 4] == 0 && XMLDoc[os + 5] == 'l'
					&& XMLDoc[os + 6] == 0 && XMLDoc[os + 7] == 'n' && XMLDoc[os + 8] == 0 && XMLDoc[os + 9] == 's') {
				return true;
			}
		} else {
			if (len == 10 && XMLDoc[os] == 'x' && XMLDoc[os + 1] == 0 && XMLDoc[os + 2] == 'm' && XMLDoc[os + 3] == 0 && XMLDoc[os + 4] == 'l' && XMLDoc[os + 5] == 0
					&& XMLDoc[os + 6] == 'n' && XMLDoc[os + 3] == 0 && XMLDoc[os + 8] == 's' && XMLDoc[os + 5] == 0) {
				return true;
			}
		}
		return false;
	}

	private long _getCharResolved(int byte_offset) {

		int ch = 0;
		int val = 0;
		long inc = 2 << (increment - 1);
		long l = r._getChar(byte_offset);

		ch = (int) l;

		if (ch != '&')
			return l;

		// let us handle references here
		// currentOffset++;
		byte_offset += increment;
		ch = getCharUnit(byte_offset);
		byte_offset += increment;
		switch (ch) {
		case '#':

			ch = getCharUnit(byte_offset);

			if (ch == 'x') {
				while (true) {
					byte_offset += increment;
					inc += increment;
					ch = getCharUnit(byte_offset);

					if (ch >= '0' && ch <= '9') {
						val = (val << 4) + (ch - '0');
					} else if (ch >= 'a' && ch <= 'f') {
						val = (val << 4) + (ch - 'a' + 10);
					} else if (ch >= 'A' && ch <= 'F') {
						val = (val << 4) + (ch - 'A' + 10);
					} else if (ch == ';') {
						inc += increment;
						break;
					}
				}
			} else {
				while (true) {
					ch = getCharUnit(byte_offset);
					byte_offset += increment;
					inc += increment;
					if (ch >= '0' && ch <= '9') {
						val = val * 10 + (ch - '0');
					} else if (ch == ';') {
						break;
					}
				}
			}
			break;

		case 'a':
			ch = getCharUnit(byte_offset);
			if (encoding < FORMAT_UTF_16BE) {
				if (ch == 'm') {
					if (getCharUnit(byte_offset + 1) == 'p' && getCharUnit(byte_offset + 2) == ';') {
						inc = 5;
						val = '&';
					}
				} else if (ch == 'p') {
					if (getCharUnit(byte_offset + 1) == 'o' && getCharUnit(byte_offset + 2) == 's' && getCharUnit(byte_offset + 3) == ';') {
						inc = 6;
						val = '\'';
					}
				}
			} else {
				if (ch == 'm') {
					if (getCharUnit(byte_offset + 2) == 'p' && getCharUnit(byte_offset + 4) == ';') {
						inc = 10;
						val = '&';
					}
				} else if (ch == 'p') {
					if (getCharUnit(byte_offset + 2) == 'o' && getCharUnit(byte_offset + 4) == 's' && getCharUnit(byte_offset + 6) == ';') {
						inc = 12;
						val = '\'';
					}
				}
			}
			break;

		case 'q':

			if (encoding < FORMAT_UTF_16BE) {
				if (getCharUnit(byte_offset) == 'u' && getCharUnit(byte_offset + 1) == 'o' && getCharUnit(byte_offset + 2) == 't' && getCharUnit(byte_offset + 3) == ';') {
					inc = 6;
					val = '\"';
				}
			} else {
				if (getCharUnit(byte_offset) == 'u' && getCharUnit(byte_offset + 2) == 'o' && getCharUnit(byte_offset + 4) == 't' && getCharUnit(byte_offset + 6) == ';') {
					inc = 12;
					val = '\"';
				}
			}
			break;
		case 'l':
			if (encoding < FORMAT_UTF_16BE) {
				if (getCharUnit(byte_offset) == 't' && getCharUnit(byte_offset + 1) == ';') {
					// offset += 2;
					inc = 4;
					val = '<';
				}
			} else {
				if (getCharUnit(byte_offset) == 't' && getCharUnit(byte_offset + 2) == ';') {
					// offset += 2;
					inc = 8;
					val = '<';
				}
			}
			break;
		case 'g':
			if (encoding < FORMAT_UTF_16BE) {
				if (getCharUnit(byte_offset) == 't' && getCharUnit(byte_offset + 1) == ';') {
					inc = 4;
					val = '>';
				}
			} else {
				if (getCharUnit(byte_offset) == 't' && getCharUnit(byte_offset + 2) == ';') {
					inc = 8;
					val = '>';
				}
			}
			break;
		}

		// currentOffset++;
		return val | (inc << 32);
	}

	// return 0;

	private int getCharUnit(int byte_offset) {
		return (encoding <= 2) ? XMLDoc[byte_offset] & 0xff : (encoding < FORMAT_UTF_16BE) ? r.decode(byte_offset)
				: (encoding == FORMAT_UTF_16BE) ? (((int) XMLDoc[byte_offset]) << 8 | XMLDoc[byte_offset + 1]) : (((int) XMLDoc[byte_offset + 1]) << 8 | XMLDoc[byte_offset]);
	}

	private boolean matchURL(int bos1, int len1, int bos2, int len2) {
		long l1, l2;
		int i1 = bos1, i2 = bos2, i3 = bos1 + len1, i4 = bos2 + len2;
		// System.out.println("--->"+new String(XMLDoc, bos1, len1)+" "+new
		// String(XMLDoc,bos2,len2));
		while (i1 < i3 && i2 < i4) {
			l1 = _getCharResolved(i1);
			l2 = _getCharResolved(i2);
			if ((int) l1 != (int) l2)
				return false;
			i1 += (int) (l1 >> 32);
			i2 += (int) (l2 >> 32);
		}
		if (i1 == i3 && i2 == i4)
			return true;
		return false;
	}

	private void checkAttributeUniqueness() throws ParseException {
		boolean unique = true;
		boolean unequal;
		for (int i = 0; i < attr_count; i++) {
			unequal = false;
			int prevLen = (int) attr_name_array[i];
			if (length1 == prevLen) {
				int prevOffset = (int) (attr_name_array[i] >> 32);
				for (int j = 0; j < prevLen; j++) {
					if (XMLDoc[prevOffset + j] != XMLDoc[temp_offset + j]) {
						unequal = true;
						break;
					}
				}
			} else
				unequal = true;
			unique = unique && unequal;
		}
		if (!unique && attr_count != 0)
			throw new ParseException("Error in attr: Attr name not unique" + formatLineNumber());
		unique = true;
		if (attr_count < attr_name_array.length) {
			attr_name_array[attr_count] = ((long) (temp_offset) << 32) | length1;
			attr_count++;
		} else // grow the attr_name_array by 16
		{
			long[] temp_array = attr_name_array;
			/*
			 * System.out.println( "size increase from " + temp_array.length +
			 * "  to " + (attr_count + 16));
			 */
			attr_name_array = new long[attr_count + ATTR_NAME_ARRAY_SIZE];
			System.arraycopy(temp_array, 0, attr_name_array, 0, attr_count);
			/*
			 * for (int i = 0; i < attr_count; i++) { attr_name_array[i] =
			 * temp_array[i]; }
			 */
			attr_name_array[attr_count] = ((long) (temp_offset) << 32) | length1;
			attr_count++;
		}
		// insert prefix attr node into the prefixed_attr_name array
		// xml:something will not be inserted
		// System.out.println(" prefixed attr count ===>"+prefixed_attr_count);
		// System.out.println(" length2 ===>"+length2);
		if (ns && !is_ns && length2 != 0) {
			if ((increment == 1 && length2 == 3 && matchXML(temp_offset)) || (increment == 2 && length2 == 6 && matchXML(temp_offset))) {
				return;
			} else if (prefixed_attr_count < prefixed_attr_name_array.length) {
				prefixed_attr_name_array[prefixed_attr_count] = ((long) (temp_offset) << 32) | (length2 << 16) | length1;
				prefixed_attr_count++;
			} else {
				long[] temp_array1 = prefixed_attr_name_array;
				prefixed_attr_name_array = new long[prefixed_attr_count + ATTR_NAME_ARRAY_SIZE];
				prefix_URL_array = new int[prefixed_attr_count + ATTR_NAME_ARRAY_SIZE];
				System.arraycopy(temp_array1, 0, prefixed_attr_name_array, 0, prefixed_attr_count);
				// System.arraycopy(temp_array1, 0, prefixed_attr_val_array, 0,
				// prefixed_attr_count)
				/*
				 * for (int i = 0; i < attr_count; i++) { attr_name_array[i] =
				 * temp_array[i]; }
				 */
				prefixed_attr_name_array[prefixed_attr_count] = ((long) (temp_offset) << 32) | (length2 << 16) | length1;
				prefixed_attr_count++;
			}
		}
	}

	private void handleOtherTextChar(int ch) throws ParseException {
		if (ch == '&') {
			// has_amp = true;
			if (!XMLChar.isValidChar(entityIdentifier()))
				throw new ParseException("Error in text content: Invalid char in text content " + formatLineNumber());
			// parser_state = STATE_TEXT;
		} else if (ch == ']') {
			if (r.skipChar(']')) {
				while (r.skipChar(']')) {
				}
				if (r.skipChar('>'))
					throw new ParseException("Error in text content: ]]> in text content" + formatLineNumber());
			}
		} else
			throw new ParseException("Error in text content: Invalid char in text content " + formatLineNumber());
	}

	private void handleOtherTextChar2(int ch) throws ParseException {
		if (ch == '&') {
			// has_amp = true;
			// temp_offset = offset;
			entityIdentifier();
			// parser_state = STATE_TEXT;
		} else if (ch == ']') {
			if (r.skipChar(']')) {
				while (r.skipChar(']')) {
				}
				if (r.skipChar('>'))
					throw new ParseException("Error in text content: ]]> in text content" + formatLineNumber());
			}
			// parser_state = STATE_TEXT;
		} else
			throw new ParseException("Error in text content: Invalid char" + formatLineNumber());
	}
}
