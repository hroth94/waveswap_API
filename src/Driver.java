import java.io.IOException;

public class Driver {
	public static void main(String[] args) throws IOException
	{
		System.out.println("TEST");
		
		SenderParser sender = new SenderParser();
		// Put file bytes into fileData byte array
		byte[] fileData = "TEST".getBytes();
		sender.createAudioFile(fileData, "C:\\Users\\Matthew\\Documents\\Test.wav", 2, 14000, 500, 5512, SenderParser.BIT_BY_BIT);
		//float[][] data = sender.createData(fileData, 1, 1, 1, 2);
		//byte[] returnBytes = ReceiverParser.decodeData(data, fileData.length, 1, 1, 1);
		
		/*
		float[] frequencies = {14000f, 14500f, 15000f};
		//float[] frequencies = {14000f};
		float[] phis = new float[frequencies.length];
		float[][] buffer = new float[2][44100 * 5];
		//float change = 3000f / 44100f;
		for(int i = 0; i < buffer[0].length; i++){
			buffer[0][i] = buffer[1][i] = 0;
			for(int f = 0; f < frequencies.length; f++)
			{
				//frequencies[f] += change * f %2 == 0 ? 1 : -1;
				phis[f] += 2*Math.PI*frequencies[f]*(1f/44100f);
				buffer[0][i] = buffer[1][i] += (float)Math.sin(phis[f])/frequencies.length;
			}
		}
		
		WavFileReaderWriter wfrw = new WavFileReaderWriter();
		try {
			SampleAudioFormat format = new SampleAudioFormat(44100, 16, 2, true, true);
			wfrw.writeAudioFile(buffer, "C:\\Users\\theholypiggy\\Documents\\Test.wav", AudioFileType.WAV, format);
		} catch (IOException e) {
			e.printStackTrace();
		} catch (OperationUnsupportedException e) {
			e.printStackTrace();
		} catch (FileFormatException e) {
			e.printStackTrace();
		}*/
		
		return;
	}
}
