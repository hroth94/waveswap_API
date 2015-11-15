import java.io.*;
import java.nio.ByteBuffer;

import net.beadsproject.beads.data.SampleAudioFormat;
import net.beadsproject.beads.data.audiofile.AudioFileType;
import net.beadsproject.beads.data.audiofile.FileFormatException;
import net.beadsproject.beads.data.audiofile.OperationUnsupportedException;
import net.beadsproject.beads.data.audiofile.WavFileReaderWriter;

/**
 * Parses a file to eventually be converted to a sound wave.
 * @authors Hannah Roth, Matthew Montera
 * @date September 15th, 2015
 */
public class SenderParser {

	private static final int sampleRate = 44100;
	
	public static final int DIRECT = 1;
	public static final int BIT_BY_BIT = 2;
	public static final int CASCADE = 3;
	
	//Assume that we're looking at a lowFrequency of 12000, a sensitivity of 500,
	//and a transmission speed of 1 DIRECT for the locator and descriptor.  This means that
	//the frequencies are as follows for sending the locator and descriptor:
	// Locator: Each frequency represents itself
	// Descriptor:
	// 13000 : 1
	// 12500 : 0
	// 12000 : repeat of last value
	public static float   transmissionLowFrequency = 14000;
	public static float   transmissionHighFrequency = 15000;
	public static float[] transmissionLocator = {14000, 15000, 14500, 15000, 14000, 15000 };
	                                             //1,      4,     2,     4,     1,     4

	public float[] createLocator(int replicationAmount) {
		float[] data = new float[transmissionLocator.length * replicationAmount];

		for (int i = 0; i < transmissionLocator.length; i++) {
			for (int k = i * replicationAmount, j = 0; j < replicationAmount; k++, j++) {
				data[k] = transmissionLocator[i];
			}
		}

		return data;
	}
	
	public float[] createDescriptor(int transmissionSpeed, float lowFrequency, float sensitivity, int replicationAmount, int method) {
		//number of bits to send = 32 + 32 + 32 + 32
		byte[] bytes = new byte[16];		
		
		byte[] transmissionSpeedBytes = ByteBuffer.allocate(4).putInt(transmissionSpeed).array();
		byte[] lowFrequencyBytes = ByteBuffer.allocate(4).putFloat(lowFrequency).array();
		byte[] sensitivityBytes = ByteBuffer.allocate(4).putFloat(sensitivity).array();
		byte[] methodBytes = ByteBuffer.allocate(4).putInt(method).array();
		
		for(int i = 0; i < transmissionSpeedBytes.length; i++) {
			bytes[i] = transmissionSpeedBytes[i];
		}
		for(int i = 0; i < lowFrequencyBytes.length; i++) {
			bytes[i+transmissionSpeedBytes.length] = lowFrequencyBytes[i]; 
		}
		for(int i = 0; i < sensitivityBytes.length; i++) {
			bytes[i+transmissionSpeedBytes.length+lowFrequencyBytes.length] = sensitivityBytes[i]; 
		}
		for(int i = 0; i < methodBytes.length; i++) {
			bytes[i+transmissionSpeedBytes.length+lowFrequencyBytes.length+sensitivityBytes.length] = methodBytes[i];
		}
		
		return createData(bytes, 1, lowFrequency, sensitivity, replicationAmount);
	}

	/**
	 * In order to account for sampleRate being variable, as well as
	 * similar frequencies being copied over time, sending a transmission
	 * now has new semantics:
	 * lowFrequency is UNUSED as a byte->frequency conversion.  Rather,
	 * lowFrequency appears when the SAME frequency would be used twice in a row.
	 * So, for example, [5 5 5 4] becomes [5 lowFrequency 5 4].
	 * Lastly, replicationAmount refers to the amount of copies that data will have,
	 * so [5 5 5 4] with replicationAmount 2 becomes [5 5 lf lf 5 5 4 4]
	 */
	public float[] createData(byte[] bytes, int transmissionSpeed,
			float lowFrequency, float sensitivity, int replicationAmount) {
		int numBytes = bytes.length;
		int numBits = numBytes * 8;
		int paddingBits = (numBits % transmissionSpeed > 0) ? (transmissionSpeed - numBits
				% transmissionSpeed) : 0;
		int totalBits = numBits + paddingBits; // Padding at the end of the  transmission
		int dataSize = totalBits / transmissionSpeed * replicationAmount;

		// First dimension is the number of channels (frequency, overlay)
		// Second dimension is the number of frames (time)
		float[] data = new float[dataSize];
		int byteIndex = 0;
		int bitIndex = 0;
		int dataIndex = 0;
		int maskShiftAmount = 7; // Shifts left this amount
		byte baseMask = 1;
		byte currentByte = bytes[byteIndex];
		float frequency;
		float previousFrequency = -999999f; //Used to check for lowFrequency

		while (dataIndex < dataSize) {
			// Set up start of loop
			frequency = lowFrequency + sensitivity;

			// Loop equal to transmission speed
			for (int i = 0; i < transmissionSpeed; i++) {
				// If you overrun the current byte, fetch a new one
				if (bitIndex > 7) {
					byteIndex++;

					// Normal scenario, there are more bytes to process
					// So reset variables for the new byte
					if (byteIndex < numBytes) {
						currentByte = bytes[byteIndex];
						bitIndex = 0;
						maskShiftAmount = 7;
					}
					// Padding scenario, all bytes have been processed and now
					// padding must be added.
					else {
						currentByte = 0;
						bitIndex = 0;
						maskShiftAmount = 7;
					}
				}
				byte currentBit = (byte) (currentByte & (baseMask << maskShiftAmount));
				if (currentBit != 0) {
					frequency += Math.pow(2, transmissionSpeed - i - 1)
							* sensitivity;
				}
				bitIndex++;
				maskShiftAmount--;
			}
			
			float effectiveFrequency = frequency == previousFrequency ? lowFrequency : frequency;
			
			for (int i = 0; i < replicationAmount; i++) {
				data[dataIndex + i] = effectiveFrequency;
			}
			
			previousFrequency = effectiveFrequency;
			
			dataIndex += replicationAmount;
		}
		return data;
	}
	
	//The data in frequencies should have the replication amount reflect the 
	//amount of time that the user would like to play the sound, so no need to worry
	//about that.
	private static float[][] createSineWave(float[] frequencies, int transmissionSpeed, float lowFrequency, float sensitivity, int method)
	{
		if(method == DIRECT)
		{
			//This is the usual method, which takes frequencies and directly translates it
			//into the proper sine waves without any kind of additional tinkering.
			return createSineWaveDirect(frequencies, lowFrequency);
		}
		
		if(method == BIT_BY_BIT)
		{
			//To use BIT_BY_BIT, float[][] frequencies MUST have been made
			//by createData(...) using a transmissonSpeed of 1.
			//When using BIT_BY_BIT, "parameters" is: (float targetFrequency, int numChannels)
			//Where "targetFrequency" is lowFrequency + sensitivity (since only that frequency should play)
			return createSineWaveBitByBit(frequencies, lowFrequency, sensitivity, transmissionSpeed);
		}
		
		if(method == CASCADE)
		{
			//To use CASCADE, float[][] frequencies MUST have been made
			//by createData(...) using any transmission speed, but for now a
			//sensitivity of "500".
			//When using CASCADE, "parameters"
			return createSineWaveCascade(frequencies);
		}
		//Used the below as reference
		/*float[][] buffer = new float[1][frequencies[0].length];
		double phi = 0;
		for(int i = 0; i < buffer[0].length; i++){
			phi += 2*Math.PI*1.0/sampleRate*frequencies[0][i];
			buffer[0][i] = (float)Math.sin(phi);
		}
		return buffer;*/
		return null;
	}
	
	private static float[][] createSineWaveDirect(float[] frequencies, float lowFrequency)
	{
		float[][] buffer = new float[1][frequencies.length];
		double phi = 0;
		
		for(int i = 0; i < frequencies.length; i++) {
			phi += 2*Math.PI*1.0/sampleRate*frequencies[i];
			buffer[0][i] = (float)Math.sin(phi);
		}
		
		return buffer;
	}

	
	/**
	 * Concerns: size of frequencies may not be divisible by numChannels, resulting in
	 *           erroneous "0"s added to the end of the decoding.  However, since in the 
	 *           final stage the descriptor should give the number of bytes(bits?), this
	 *           shouldn't be a problem.
	 *           
	 *           Output: sine wave of floats on "transmissionSpeed" channels 
	 *           
	 * Semantics: Outputs a sine wave with "transmissionSpeed" + 2 channels, where the 1st
	 *            extra channel is used to signal "0" and the second extra channel is used
	 *            to signal "repeat".  So, for example, a rawFrequencies input of:
	 *            0 0 0 1 1 1 0 1
	 *            Will become this float[] after createData with a transmission speed of 1: (for BIT_BY_BIT)
	 *             (where lf = lowFrequency and s = sensitivity)
	 *            lf+1*s lf lf+1*s lf+2*s lf lf+2*s lf+1*s lf+2*s
	 *            Since lf = repeat, lf+1*s = 0, lf+2*s = 1
	 */
	private static float[][] createSineWaveBitByBit(float[] rawFrequencies, float lowFrequency, float sensitivity, int numChannels)
	{
		int numFrequencies = rawFrequencies.length;
		//Determine how much padding is needed for the channel conversion
		int paddingFrequencies = (numFrequencies % numChannels > 0) ? (numChannels - numFrequencies
				% numChannels) : 0;
		int totalFrequencies = numFrequencies + paddingFrequencies;
		//Determine the number of frequencies each channel must play (including replication)
		int frequenciesPerChannel = totalFrequencies / numChannels;
		
		float[] frequencies = new float[totalFrequencies];
		for(int i = 0; i < numFrequencies; i++) {
			frequencies[i] = rawFrequencies[i];
		}
		for(int i = numFrequencies; i < totalFrequencies; i++) {
			frequencies[i] = lowFrequency + sensitivity; //Correspond to 0
		}
		
		float[][] buffer = new float[numChannels][frequenciesPerChannel];
		double phi = 0;
		
		//Add the frequency to the buffer if it is equivalent to a "1", and not if it is equivalent
		//to a "0".  If no frequencies are "1", play "lowFrequency" on Channel 0.  Channel 0 plays at "lowFrequency + sensitivity",
		//and so on.  
		int currentChannel = 0;
		int currentFrequency = 0;
		int numChannelsPlaying = 0;
		float targetFrequency = lowFrequency + sensitivity * 2;
		boolean wasTargetFrequency = false;
		for(int i = 0; i < frequencies.length; i++)
		{
			float frequency = frequencies[i];
			float relevantFrequency;
			
			if(frequency == targetFrequency || (frequency == lowFrequency && wasTargetFrequency))
			{
				//Change frequency to channel equivalent.
				//Channel 0: lowFrequency + sensitivity
				//Channel 1: lowFrequency + 2*sensitivity, etc.
				relevantFrequency = lowFrequency + sensitivity * (currentChannel + 1);
				phi += 2*Math.PI*1.0/sampleRate*relevantFrequency;
				buffer[currentChannel][currentFrequency] = (float)Math.sin(phi);
				wasTargetFrequency = true;
				numChannelsPlaying += 1;
			}
			else
			{
				buffer[currentChannel][currentFrequency] = 0;
				wasTargetFrequency = false;
			}
			
			currentChannel++;
			if(currentChannel >= numChannels)
			{
				if(numChannelsPlaying == 0) {
					phi += 2*Math.PI*1.0/sampleRate*(lowFrequency);
					buffer[0][currentFrequency] = (float)Math.sin(phi);
				}
				currentChannel = 0;
				currentFrequency += 1;
				numChannelsPlaying = 0;
			}
		}
		
		return buffer;
	}
	
	private static float[][] createSineWaveCascade(float[] frequencies)
	{
		return null;
	}
	
	
	/**
	 * Converts a byte array into an audio file
	 * 
	 * @param filePath Path to place the audio file
	 * @param fileName Audio file name
	 * @param transmissionSpeed Bits to read per transmission e.g., 2 = 00, 01, 10, 11
	 * @param lowFrequency Frequency that transmission begins at, e.g., lowFrequency = 10Hz, 00 = 10Hz
	 * @param sensitivity Difference between frequencies that hardware can detect
	 */
	public void createAudioFile(byte[] bytes, String filePath,
			int transmissionSpeed, float lowFrequency, float sensitivity, int replication, int method) {
		
		float[] parsedLocator = createLocator(replication);
		//float[] parsedDescriptor = createDescriptor(transmissionSpeed, lowFrequency, sensitivity, replication, method);
		//float[] parsedData = createData(bytes, method == BIT_BY_BIT ? 1 : transmissionSpeed, lowFrequency, sensitivity, replication);
		
		float[][] locator = createSineWave(parsedLocator, 1, lowFrequency, sensitivity, DIRECT);
		//float[][] descriptor = createSineWave(parsedDescriptor, 1, lowFrequency, sensitivity, DIRECT);
		//float[][] data = createSineWave(parsedData, transmissionSpeed, lowFrequency, sensitivity, method);
		float[][] transmission = new float[1][locator[0].length];
		//float[][] transmission = new float[data.length][locator[0].length + descriptor[0].length + data[0].length];

		for(int i = 0; i < locator.length; i++) {
			for(int j = 0; j < locator[0].length; j++) {
				transmission[i][j] = locator[i][j];
			}
		}
		/*
		for (int i = 0; i < descriptor.length; i++) {
			for (int j = 0; j < descriptor[0].length; j++)
			{
				transmission[i][j+locator[0].length] = descriptor[i][j];
			}
		}

		for (int i = 0; i < data.length; i++) {
			for (int j = 0; j < data[0].length; j++)
			{
				transmission[i][j+locator[0].length+descriptor[0].length] = data[i][j];
			}
		}*/

		WavFileReaderWriter wfrw = new WavFileReaderWriter();
		try {
			SampleAudioFormat format = new SampleAudioFormat(sampleRate, 32, 1); //FIX THIS
			wfrw.writeAudioFile(transmission, filePath, AudioFileType.WAV, format);
		} catch (IOException e) {
			e.printStackTrace();
		} catch (OperationUnsupportedException e) {
			e.printStackTrace();
		} catch (FileFormatException e) {
			e.printStackTrace();
		}
	}

	public void parseFile(File file, String filePath, int transmissionSpeed, float lowFrequency, 
			float sensitivity, int replication, int method) {
		try {
			// Put file bytes into fileData byte array
			byte[] fileData = new byte[(int) file.length()];
			FileInputStream in = new FileInputStream(file);
			in.read(fileData);
			in.close();

			createAudioFile(fileData, filePath, transmissionSpeed, lowFrequency, sensitivity, replication, method);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}