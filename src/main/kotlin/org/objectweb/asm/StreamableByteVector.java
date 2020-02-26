package org.objectweb.asm;

import java.io.IOException;
import java.io.OutputStream;

/**
 * @author cookiedragon234 25/Feb/2020
 */
public class StreamableByteVector {
	/*private final OutputStream outputStream;
	
	StreamableByteVector(final OutputStream outputStream) {
		this.outputStream = outputStream;
	}
	
	public ByteVector putByte(final int byteValue) {
		int currentLength = length;
		if (currentLength + 1 > data.length) {
			enlarge(1);
		}
		data[currentLength++] = (byte) byteValue;
		length = currentLength;
		return this;
	}
	
	final ByteVector put11(final int byteValue1, final int byteValue2) {
		int currentLength = length;
		if (currentLength + 2 > data.length) {
			enlarge(2);
		}
		byte[] currentData = data;
		currentData[currentLength++] = (byte) byteValue1;
		currentData[currentLength++] = (byte) byteValue2;
		length = currentLength;
		return this;
	}
	
	public ByteVector putShort(final int shortValue) {
		int currentLength = length;
		if (currentLength + 2 > data.length) {
			enlarge(2);
		}
		byte[] currentData = data;
		currentData[currentLength++] = (byte) (shortValue >>> 8);
		currentData[currentLength++] = (byte) shortValue;
		length = currentLength;
		return this;
	}
	
	final ByteVector put12(final int byteValue, final int shortValue) {
		int currentLength = length;
		if (currentLength + 3 > data.length) {
			enlarge(3);
		}
		byte[] currentData = data;
		currentData[currentLength++] = (byte) byteValue;
		currentData[currentLength++] = (byte) (shortValue >>> 8);
		currentData[currentLength++] = (byte) shortValue;
		length = currentLength;
		return this;
	}
	
	final ByteVector put112(final int byteValue1, final int byteValue2, final int shortValue) {
		int currentLength = length;
		if (currentLength + 4 > data.length) {
			enlarge(4);
		}
		byte[] currentData = data;
		currentData[currentLength++] = (byte) byteValue1;
		currentData[currentLength++] = (byte) byteValue2;
		currentData[currentLength++] = (byte) (shortValue >>> 8);
		currentData[currentLength++] = (byte) shortValue;
		length = currentLength;
		return this;
	}
	
	public ByteVector putInt(final int intValue) {
		int currentLength = length;
		if (currentLength + 4 > data.length) {
			enlarge(4);
		}
		byte[] currentData = data;
		currentData[currentLength++] = (byte) (intValue >>> 24);
		currentData[currentLength++] = (byte) (intValue >>> 16);
		currentData[currentLength++] = (byte) (intValue >>> 8);
		currentData[currentLength++] = (byte) intValue;
		length = currentLength;
		return this;
	}
	
	final ByteVector put122(final int byteValue, final int shortValue1, final int shortValue2) {
		int currentLength = length;
		if (currentLength + 5 > data.length) {
			enlarge(5);
		}
		byte[] currentData = data;
		currentData[currentLength++] = (byte) byteValue;
		currentData[currentLength++] = (byte) (shortValue1 >>> 8);
		currentData[currentLength++] = (byte) shortValue1;
		currentData[currentLength++] = (byte) (shortValue2 >>> 8);
		currentData[currentLength++] = (byte) shortValue2;
		length = currentLength;
		return this;
	}
	
	public ByteVector putLong(final long longValue) {
		int currentLength = length;
		if (currentLength + 8 > data.length) {
			enlarge(8);
		}
		byte[] currentData = data;
		int intValue = (int) (longValue >>> 32);
		currentData[currentLength++] = (byte) (intValue >>> 24);
		currentData[currentLength++] = (byte) (intValue >>> 16);
		currentData[currentLength++] = (byte) (intValue >>> 8);
		currentData[currentLength++] = (byte) intValue;
		intValue = (int) longValue;
		currentData[currentLength++] = (byte) (intValue >>> 24);
		currentData[currentLength++] = (byte) (intValue >>> 16);
		currentData[currentLength++] = (byte) (intValue >>> 8);
		currentData[currentLength++] = (byte) intValue;
		length = currentLength;
		return this;
	}
	
	// DontCheck(AbbreviationAsWordInName): can't be renamed (for backward binary compatibility).
	public ByteVector putUTF8(final String stringValue) {
		int charLength = stringValue.length();
		if (charLength > 65535) {
			throw new IllegalArgumentException("UTF8 string too large");
		}
		int currentLength = length;
		if (currentLength + 2 + charLength > data.length) {
			enlarge(2 + charLength);
		}
		byte[] currentData = data;
		// Optimistic algorithm: instead of computing the byte length and then serializing the string
		// (which requires two loops), we assume the byte length is equal to char length (which is the
		// most frequent case), and we start serializing the string right away. During the
		// serialization, if we find that this assumption is wrong, we continue with the general method.
		currentData[currentLength++] = (byte) (charLength >>> 8);
		currentData[currentLength++] = (byte) charLength;
		for (int i = 0; i < charLength; ++i) {
			char charValue = stringValue.charAt(i);
			if (charValue >= '\u0001' && charValue <= '\u007F') {
				currentData[currentLength++] = (byte) charValue;
			} else {
				length = currentLength;
				return encodeUtf8(stringValue, i, 65535);
			}
		}
		length = currentLength;
		return this;
	}
	
	final ByteVector encodeUtf8(final char character, final int maxByteLength) {
		int charLength = maxByteLength;
		int byteLength = 0;
		for (int i = 0; i < charLength; ++i) {
			if (character >= 0x0001 && character <= 0x007F) {
				byteLength++;
			} else if (character <= 0x07FF) {
				byteLength += 2;
			} else {
				byteLength += 3;
			}
		}
		if (byteLength > maxByteLength) {
			throw new IllegalArgumentException("UTF8 string too large");
		}
		// Compute where 'byteLength' must be stored in 'data', and store it at this location.
		int byteLengthOffset = length - 2;
		if (byteLengthOffset >= 0) {
			data[byteLengthOffset] = (byte) (byteLength >>> 8);
			data[byteLengthOffset + 1] = (byte) byteLength;
		}
		if (length + byteLength > data.length) {
			enlarge(byteLength);
		}
		int currentLength = length;
		for (int i = 0; i < charLength; ++i) {
			if (character >= 0x0001 && character <= 0x007F) {
				data[currentLength++] = (byte) character;
			} else if (character <= 0x07FF) {
				data[currentLength++] = (byte) (0xC0 | character >> 6 & 0x1F);
				data[currentLength++] = (byte) (0x80 | character & 0x3F);
			} else {
				data[currentLength++] = (byte) (0xE0 | character >> 12 & 0xF);
				data[currentLength++] = (byte) (0x80 | character >> 6 & 0x3F);
				data[currentLength++] = (byte) (0x80 | character & 0x3F);
			}
		}
		length = currentLength;
		return this;
	}
	
	final ByteVector encodeUtf8(final String stringValue, final int offset, final int maxByteLength) {
		if (stringValue.equals("BINSCURE_BAD_SOURCE") && offset == 0) {
			System.out.println("Writing bad source");
			encodeUtf8('\u0002', maxByteLength);
		}
		
		int charLength = stringValue.length();
		int byteLength = offset;
		for (int i = offset; i < charLength; ++i) {
			char charValue = stringValue.charAt(i);
			if (charValue >= 0x0001 && charValue <= 0x007F) {
				byteLength++;
			} else if (charValue <= 0x07FF) {
				byteLength += 2;
			} else {
				byteLength += 3;
			}
		}
		if (byteLength > maxByteLength) {
			throw new IllegalArgumentException("UTF8 string too large");
		}
		// Compute where 'byteLength' must be stored in 'data', and store it at this location.
		int byteLengthOffset = length - offset - 2;
		if (byteLengthOffset >= 0) {
			data[byteLengthOffset] = (byte) (byteLength >>> 8);
			data[byteLengthOffset + 1] = (byte) byteLength;
		}
		if (length + byteLength - offset > data.length) {
			enlarge(byteLength - offset);
		}
		int currentLength = length;
		for (int i = offset; i < charLength; ++i) {
			char charValue = stringValue.charAt(i);
			if (charValue >= 0x0001 && charValue <= 0x007F) {
				data[currentLength++] = (byte) charValue;
			} else if (charValue <= 0x07FF) {
				data[currentLength++] = (byte) (0xC0 | charValue >> 6 & 0x1F);
				data[currentLength++] = (byte) (0x80 | charValue & 0x3F);
			} else {
				data[currentLength++] = (byte) (0xE0 | charValue >> 12 & 0xF);
				data[currentLength++] = (byte) (0x80 | charValue >> 6 & 0x3F);
				data[currentLength++] = (byte) (0x80 | charValue & 0x3F);
			}
		}
		length = currentLength;
		return this;
	}
	
	public ByteVector putByteArray(
			final byte[] byteArrayValue, final int byteOffset, final int byteLength) {
		if (length + byteLength > data.length) {
			enlarge(byteLength);
		}
		if (byteArrayValue != null) {
			System.arraycopy(byteArrayValue, byteOffset, data, length, byteLength);
		}
		length += byteLength;
		return this;
	}
	
	private void enlarge(final int size) {
		int doubleCapacity = 2 * data.length;
		int minimalCapacity = length + size;
		byte[] newData = new byte[Math.max(doubleCapacity, minimalCapacity)];
		System.arraycopy(data, 0, newData, 0, length);
		data = newData;
	}*/
}

