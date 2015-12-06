package main;

import java.io.*;
import java.nio.ByteBuffer;
import java.util.ArrayList;

/**
 * Parses a file to eventually be converted to a sound wave.
 * @authors Hannah Roth, Matthew Montera
 * @date September 15th, 2015
 */
public class ReceiverParser {
	private static final int BYTE_LENGTH = 8;
	private static final int INT_LENGTH = 32;
	
	public enum Stage {
		LOCATE, DETERMINE, DECODE, FINISH
	}
	
	private Stage currentStage;
	
	//----All Stages---- VARIABLES
	private int   transmissionSpeed;
	private float lowFrequency;
	private float sensitivity;
	private int   method;
	private int   size;
	
	private int   currentLocatorIndex; //Which part of SenderParser.transmissionLocator are we on?
	private float currentLocatorFrequency; //Frequency equivalent to above
	
	private ArrayList<Byte> currentDescriptorBytes;
	private int             previousConvFreq; //If this is 1, then its a repeat
	
	private ArrayList<Byte> retrievedData;
	
	/**
	 * NOTES:
	 * 
	 * DO NOT modify the signature in SenderParser.  Additionally, the range of frequencies
	 * that the LOCATE and DETERMINE stages use is set within SenderParser as well.  For now,
	 * we do NO enhancements upon the transmission until the data stage.  That is, the LOCATE and 
	 * DETERMINE stages are both sent with a transmission speed of 1
	 * 
	 */
	public ReceiverParser() {
		this.currentStage = Stage.LOCATE;
		
		this.transmissionSpeed = 0;
		this.lowFrequency = 0;
		this.sensitivity = 0;
		this.size = 0;
		
		this.currentLocatorIndex = 0;
		this.currentLocatorFrequency = SenderParser.transmissionLocator[currentLocatorIndex];
		
		this.currentDescriptorBytes = new ArrayList<Byte>();
		
		this.retrievedData = new ArrayList<Byte>();
	}
	
	public void setTs(int ts) { this.transmissionSpeed = ts; }
	public void setLf(float lf) { this.lowFrequency = lf; }
	public void setS(float s) { this.sensitivity = s; }
	public void setM(int m) { this.method = m; }
	public void setSize(int size) { this.size = size; }
	public void setCurrentStage(Stage stage) { this.currentStage = stage; }
	public void reset() {
		this.currentStage = Stage.LOCATE;
		this.currentLocatorIndex = 0;
		this.currentLocatorFrequency = SenderParser.transmissionLocator[currentLocatorIndex];
	}
	public Stage getCurrentStage() { return this.currentStage; }
	public int getTs() { return transmissionSpeed; }
	public float getLf() { return lowFrequency; }
	public float getS() { return sensitivity; }
	public int getM() { return method; }
	public int getSize() { return size; }
	public byte[] getData() { 
		Byte[] bytes = retrievedData.toArray(new Byte[retrievedData.size()]);
		
		byte[] toReturn = new byte[bytes.length];
		
		for(int i = 0; i < bytes.length; i++) {
			toReturn[i] = bytes[i];
		}
		
		return toReturn;
	}
	
	public float[] getFrequencies(int value) {
		int[] convertedFrequencies = new int[transmissionSpeed + 2];
		int maxValue = (int)Math.round(Math.pow(2, transmissionSpeed + 1));
		int currentIndex = 0;
		
		while(value > 0) {
			if(value >= maxValue) {
				convertedFrequencies[currentIndex] = maxValue;
				value -= maxValue;
				currentIndex += 1;
			}
			maxValue /= 2;
		}
		
		float[] frequencies = new float[currentIndex];
		for(int k = 0; k < currentIndex; k++) {
			frequencies[k] = lowFrequency + sensitivity * (float)(Math.log10(convertedFrequencies[k]) / Math.log10(2));
		}
		
		return frequencies;
	}
	
	public boolean floatEquals(float a, float b) {
		return a-Float.MIN_NORMAL <= b && a+Float.MIN_NORMAL >= b;
	}
	
	public void receiveAudio(int[] convertedFrequencies) {
		switch(currentStage) {
		case LOCATE: locateAudio(convertedFrequencies); break;
		case DETERMINE: determineAudio(convertedFrequencies, 0); break;
		case DECODE: decodeAudio(convertedFrequencies, 0); break;
		case FINISH: break;
		}
	}
	
	/**
	 * convertedFrequencies = the array that the other audio API gives
	 */
	public void locateAudio(int[] convertedFrequencies) {
		for(int i = 0; i < convertedFrequencies.length; i++) {
			//Get the first value in the the array
			int currentValue = convertedFrequencies[i];
			
			float[] frequencies = getFrequencies(currentValue);
			
			//Just for now, may change
			if(frequencies.length > 1) {
				currentLocatorIndex = 0;
				currentLocatorFrequency = SenderParser.transmissionLocator[currentLocatorIndex];
			}
			
			float frequency = frequencies[0];
			
			//If that value 
			if(floatEquals(frequency, currentLocatorFrequency)) {
				currentLocatorIndex += 1;
				if(currentLocatorIndex >= SenderParser.transmissionLocator.length) {
					//Found the entire locator
					currentStage = Stage.DETERMINE;
					//HERE IS WHERE YOU SHOULD PRINT A STATEMENT SAYING LOCATOR FOUND
					determineAudio(convertedFrequencies, i+1);
					return;
				} else {
					currentLocatorFrequency = SenderParser.transmissionLocator[currentLocatorIndex];
				}
			} else {
				//Here we can add fault tolerance; however, that's already built into
				//the convertedFrequencies that I'm receiving so probably unnecessary.
				currentLocatorIndex = 0;
				currentLocatorFrequency = SenderParser.transmissionLocator[currentLocatorIndex];
			}
		}
		//UNLOCATED
	}
	
	public static String byteToString(byte b) {
        String str = "";
        int mask = 1 << 7;
        
        for(int i = 0; i < 8; i++) {
            if((b & mask) != 0) {
                str += "1";
            } else {
                str += "0";
            }
            mask = mask >>> 1;
        }
        
        return str;
    }
	
	public void determineAudio(int[] convertedFrequencies, int index) {
		if(index >= convertedFrequencies.length) return;
		float[][] frequencies = new float[1][convertedFrequencies.length];
		
		for(int k = index, currentIndex = 0; k < convertedFrequencies.length; k++, currentIndex++) {
			frequencies[0][currentIndex] = getFrequencies(convertedFrequencies[k])[0];
		}
		
		byte[] decoded = decodeData(frequencies, 20, 1, 14000, 500);
		
		if(lastDecodeBytes >= 20) {
			//Next Stage, Success!
			ByteBuffer buff = ByteBuffer.wrap(decoded);
			transmissionSpeed = buff.getInt();
			lowFrequency = buff.getFloat();
			sensitivity = buff.getFloat();
			method = buff.getInt();
			size = buff.getInt();
			
			currentStage = Stage.DECODE;
			decodeAudio(convertedFrequencies, index += 20 * 8);
		} else {
			//Can do this later, should barely ever happen anyway
		}
	}
	
	//Look for the descriptor at the end of the decode
	public void decodeAudio(int[] convertedFrequencies, int index) {
		currentLocatorIndex = 0;
		currentLocatorFrequency = SenderParser.transmissionLocator[currentLocatorIndex];
		float[] data = new float[convertedFrequencies.length];
		int   currentDataIndex = 0;
		int i, j;
		for(i = index, j = 0;
				i < convertedFrequencies.length &&
				j < (size * 8 + SenderParser.transmissionLocator.length);
				i++, j++) {
			//Get the first value in the the array
			int currentValue = convertedFrequencies[i];
						
			float[] frequencies = getFrequencies(currentValue);
			
			//Just for now, may change
			if(frequencies.length > 1) {
				System.out.println("Over One");
				currentLocatorIndex = 0;
				currentLocatorFrequency = SenderParser.transmissionLocator[currentLocatorIndex];
			}
			
			float frequency = frequencies[0];
						
			//If that value 
			if(floatEquals(frequency, currentLocatorFrequency)) {
				currentLocatorIndex += 1;
				if(currentLocatorIndex >= SenderParser.transmissionLocator.length) {
					//Found the entire locator
					currentStage = Stage.FINISH;
					//HERE IS WHERE YOU SHOULD PRINT A STATEMENT SAYING DATA FOUND
					finishAudio(data);
					currentLocatorIndex = 0;
					break;
				} else {
					currentLocatorFrequency = SenderParser.transmissionLocator[currentLocatorIndex];
				}
			} else {
				//Here we can add fault tolerance; however, that's already built into
				//the convertedFrequencies that I'm receiving so probably unnecessary.
				for(int k = 0; k < currentLocatorIndex; k++) {
					data[currentDataIndex + k] = SenderParser.transmissionLocator[k];
				}
				currentDataIndex += currentLocatorIndex;
				data[currentDataIndex] = frequency;
				currentDataIndex += 1;
				
				currentLocatorIndex = 0;
				currentLocatorFrequency = SenderParser.transmissionLocator[currentLocatorIndex];
			}
		}
				
		float[][] frequencies = new float[1][currentDataIndex];
		for(int k = 0; k < currentDataIndex; k++) {
			frequencies[0][k] = data[k];
		}
		
		byte[] bytes = decodeData(frequencies, currentDataIndex / 8, transmissionSpeed, lowFrequency, sensitivity);
		
		for(int k = 0; k < bytes.length; k++) {
			retrievedData.add(bytes[k]);
		}
		
		if(j < size) {
			//Did not get full transmission
		}
	}
	
	public void finishAudio(float[] data) {
		
	}
	
	private int lastDecodeBytes;

	/**
	 * Decode the audio file AFTER relevant bits have been taken from the front
	 * of the transmission that determine: Size TransmissionSpeed LowFrequency
	 * Sensitivity
	 */
	public byte[] decodeData(float[][] audioMatrix, int size,
			int transmissionSpeed, float lowFrequency, float sensitivity) {
		// Restrict to one channel for now until we know what they are for.
		byte[] bytes = new byte[size];

		byte currentByte = 0; // e.g. 00000000
		int currentByteIndex = 0; // e.g. 0-(size-1)
		byte currentByteBitsRemaining = BYTE_LENGTH; // e.g. 0-8
		float currentFrequency;
		float previousFrequency = -99999f;

		// Where "currentByte" is the byte that is currently being constructed.
		// If information is
		// added to "currentByte", but it is not full, the information is
		// left-shifted leaving space
		// on the right for new information.
		// "currentBits" is the value of the data, i.e. 15 = 1111
		// "currentBitsSize" is how many bits are relevant in "currentBits"
		// "bitsInCurrentByte" is how many bits in "currentByte" are full

		// Loop over the entire audioMatrix
		for (int index = 0; index < audioMatrix[0].length; index++) {
			currentFrequency = audioMatrix[0][index];
			
			float relevantFrequency;
			
			
			if(currentFrequency == previousFrequency)
			{
				//This means that there is a repeated frequency, which is
				//regulated by "replicationAmount" in SenderParser, and does
				//not mean that there are any additional bits.  By definition,
				//each successive float that defines a bit sequence is different
				//from the floats that surround it.
				continue;
			}
			else if(currentFrequency == lowFrequency)
			{
				//This means that there is a repeated bit sequence, so set
				//the frequency to the frequency BEFORE the lowFrequency
				relevantFrequency = previousFrequency;
			}
			else
			{
				relevantFrequency = currentFrequency;
			}
			
			// Should be an exact division
			int currentValue = Math.round((relevantFrequency - lowFrequency - sensitivity) / sensitivity); 
			int currentValueSize = transmissionSpeed;
			
			// Fill up bytes until you no longer can
			while (currentValueSize >= currentByteBitsRemaining) {
				// Construct a byte whose value is the necessary bits in "currentValue"
				// Shifted all the way to the right, with 0's placed in
				byte rightShiftedCurrentValue = (byte) (currentValue >>> (currentValueSize - currentByteBitsRemaining));

				// Since the currentValue by virtue of this branch fills up the
				// remaining space in currentByte,
				// we don't need to do any left shifting, just |= to currentByte
				currentByte |= rightShiftedCurrentValue;

				// Add to the array of bytes
				bytes[currentByteIndex] = currentByte;
								
				lastDecodeBytes += 1;

				// Maintain variables for correct execution
				currentValueSize -= currentByteBitsRemaining; // Subtract the
				// number of bits that went into currentByte
				currentValue &= -1 >>> (INT_LENGTH - currentValueSize); // Erase
				// the bits we just used up
				currentByteBitsRemaining = BYTE_LENGTH; // Reset
				currentByteIndex++; // Increment
				currentByte = 0; // Reset

				// Check to see if the array is finished
				if (currentByteIndex >= size) {
					currentValueSize = 0;
					break;
				}
			}

			// Add the rest of the currentValue to currentByte, and left shift it to the very end
			if (currentValueSize > 0) {
				byte leftShiftedCurrentValue = (byte) (currentValue << (currentByteBitsRemaining - currentValueSize));
				currentByte |= leftShiftedCurrentValue;
				// Maintain variables for correct execution
				currentByteBitsRemaining -= currentValueSize;
			}
			previousFrequency = currentFrequency;
			
			if(currentByteIndex >= size) {
				break;
			}
		}
		return bytes;
	}

	public void createFile(byte[] bytes, String filepath) throws IOException{
		FileOutputStream stream = new FileOutputStream(filepath);
		stream.write(bytes);
		stream.close();
	}
}