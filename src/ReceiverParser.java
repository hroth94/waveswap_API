
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
	
	private enum Stage {
		LOCATE, DETERMINE, DECODE, FINISH
	}
	
	private Stage currentStage;
	
	//----All Stages---- VARIABLES
	private int   transmissionSpeed;
	private float lowFrequency;
	private float sensitivity;
	private int   method;
	
	private int   currentLocatorIndex; //Which part of SenderParser.transmissionLocator are we on?
	private int   currentLocatorConvFreq; //Janky to facilitate other method
	private float currentLocatorFrequency; //Frequency equivalent to above
	
	private ArrayList<Byte> currentDescriptorBytes;
	private int             previousConvFreq; //If this is 1, then its a repeat
	
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
		
		this.currentLocatorIndex = 0;
		this.currentLocatorFrequency = SenderParser.transmissionLocator[currentLocatorIndex];
		this.currentLocatorConvFreq = 1; //Janky, corresponds to transmissionLocator values
		
		this.currentDescriptorBytes = new ArrayList<Byte>();
	}
	
	public void receiveAudio(int[] convertedFrequencies) {
		switch(currentStage) {
		case LOCATE: locateAudio(convertedFrequencies); break;
		case DETERMINE: determineAudio(convertedFrequencies, 0); break;
		case DECODE: decodeAudio(convertedFrequencies, 0); break;
		case FINISH: finishAudio(convertedFrequencies, 0); break;
		}
	}
	
	/**
	 * convertedFrequencies = the array that the other audio API gives
	 */
	public void locateAudio(int[] convertedFrequencies) {
		for(int i = 0; i < convertedFrequencies.length; i++) {
			//Get the first value in the the array
			int currentValue = convertedFrequencies[i];
			//If that value 
			if(currentValue == currentLocatorConvFreq) {
				currentLocatorIndex += 1;
				if(currentLocatorIndex >= SenderParser.transmissionLocator.length) {
					//Found the entire locator
					currentStage = Stage.DETERMINE;
					//HERE IS WHERE YOU SHOULD PRINT A STATEMENT SAYING LOCATOR FOUND
					determineAudio(convertedFrequencies, i+1);
					return;
				} else {
					currentLocatorFrequency = SenderParser.transmissionLocator[currentLocatorIndex];
					if(currentLocatorFrequency == 12) {
						currentLocatorConvFreq = 1;
					} else if(currentLocatorFrequency == 13){
						currentLocatorConvFreq = 2;
					}
				}
			} else {
				//Here we can add fault tolerance; however, that's already built into
				//the convertedFrequencies that I'm receiving so probably unnecessary.
				currentLocatorIndex = 0;
				currentLocatorFrequency = SenderParser.transmissionLocator[currentLocatorIndex];
				currentLocatorConvFreq = 1;
			}
		}
		//UNLOCATED
	}
	
	public void determineAudio(int[] convertedFrequencies, int index) {
		if(index >= convertedFrequencies.length) return;
		float[][] frequencies = new float[1][convertedFrequencies.length];
		
		for(int i = index; i < convertedFrequencies.length; i++) {
			int currentConvertedFrequency = convertedFrequencies[i];
			
			if(currentConvertedFrequency == 1) {
				//This is lowFrequency
				frequencies[0][i] = 12000;
			}
			else if(currentConvertedFrequency == 2) {
				frequencies[0][i] = 12500;
			}
			else if(currentConvertedFrequency == 4) {
				frequencies[0][i] = 13000;
			}
		}
		
		byte[] decoded = decodeData(frequencies, 16, 1, 12000, 500);
		
		if(lastDecodeBytes >= 16) {
			//Next Stage, Success!
			ByteBuffer buff = ByteBuffer.wrap(decoded);
			transmissionSpeed = buff.getInt();
			lowFrequency = buff.getFloat();
			sensitivity = buff.getFloat();
			method = buff.getInt();
		} else {
			//Can do this later, should barely ever happen anyway
		}
	}
	
	public void decodeAudio(int[] convertedFrequencies, int index) {
		
	}
	
	public void finishAudio(int[] convertedFrequencies, int index) {
		
	}
	
	/**
	 * Extract the frequencies detected when given a certain value.  This method assumes
	 * the following value conversion for a BIT_BY_BIT transmission with a transmission
	 * speed of 2, where the "ground" frequency of (lowFrequency) equates to all 0's:
	 *                                |  
	 * lowFrequency + sensitivity * 2 |----------- = 2^2, Second bit (b)
	 *                                |
	 * lowFrequency + sensitivity     |----------- = 2^1, First bit  (a)
	 *                                |
	 * lowFrequency                   |----------- = 2^0
	 *                                |___________
	 * Where the value sent here is "ab".
	 * Thus, the highest value possible in this scenario would be 6,
	 * or (2^(transmission speed) - 1) * 2, where a value of:
	 * 1: 00 3: X
	 * 2: 10 5: X
	 * 4: 01 
	 * 6: 11
	 */
	public float[] extractFrequenciesBitByBit(int value) {
		return null;
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
			
			if(currentFrequency == lowFrequency)
			{
				//This means that there is a repeated bit sequence, so set
				//the frequency to the frequency BEFORE the lowFrequency
				currentFrequency = previousFrequency;
				previousFrequency = lowFrequency;
			}
			else if(currentFrequency == previousFrequency)
			{
				//This means that there is a repeated frequency, which is
				//regulated by "replicationAmount" in SenderParser, and does
				//not mean that there are any additional bits.  By definition,
				//each successive float that defines a bit sequence is different
				//from the floats that surround it.
				continue;
			}
			
			// Should be an exact division
			int currentValue = Math.round((currentFrequency - lowFrequency - sensitivity) / sensitivity); 
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
		}
		return bytes;
	}

	public void createFile(byte[] bytes, String filepath) throws IOException{
		FileOutputStream stream = new FileOutputStream(filepath);
		stream.write(bytes);
		stream.close();
	}
}