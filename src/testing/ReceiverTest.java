package testing;

import java.nio.ByteBuffer;

import junit.framework.TestCase;
import main.ReceiverParser;
import main.SenderParser;

public class ReceiverTest extends TestCase {
	ReceiverParser rp;
	
	public void setUp() {
		rp = new ReceiverParser();
		rp.setLf(14000);
		rp.setS(500);
		rp.setTs(1);
		rp.setM(SenderParser.DIRECT);
	}
	
	public void testLocateAudio() {
		int[] test1 = {1, 4, 2, 4, 1, 4};
		
		rp.locateAudio(test1);
		
		assertEquals(ReceiverParser.Stage.DETERMINE, rp.getCurrentStage());
		
		rp.reset();
		
		int[] test2 = {1, 1, 1, 4, 2, 4, 1, 4, 1, 1};
		
		rp.locateAudio(test2);
		
		assertEquals(ReceiverParser.Stage.DETERMINE, rp.getCurrentStage());
		
		rp.reset();
		
		int[] test3 = {1, 4, 2, 4, 1, 1};
		
		rp.locateAudio(test3);
		
		assertEquals(ReceiverParser.Stage.LOCATE, rp.getCurrentStage());
	}
	
	public void testDecodeData() {
		float[][] test1 = {{14500f, 14000f, 15000f, 14000f, 15000f, 14000f, 14500f, 15000f}};
		
		byte[] bytes = rp.decodeData(test1, 1, 1, 14000, 500);
		assertEquals(61, bytes[0]);
		assertEquals(1, bytes.length);
	}
	
	public void testLocateToDetermineAudio() {
		int[] test1 = {1, 4, 2, 4, 1, 4, 2, 1, 2, 1, 2, 1, 2, 1, 2, 1, 2, 1, 2, 1, 2, 1,
			       2, 1, 2, 1, 2, 1, 2, 1, 2, 1, 2, 1, 2, 1, 2, 4,
			       2, 4, 2, 1, 2, 4, 1, 2, 1, 4, 2, 4, 1, 2, 4, 2,
			       4, 1, 2, 1, 2, 1, 2, 1, 2, 1, 2, 1, 2, 1, 2, 1,
			       2, 4, 2, 1, 2, 1, 4, 1, 4, 1, 4, 1, 4, 2, 4, 2,
			       1, 2, 1, 2, 1, 2, 1, 2, 1, 2, 1, 2, 1, 2, 1, 2,
			       1, 2, 1, 2, 1, 2, 1, 2, 1, 2, 1, 2, 1, 2, 1, 2,
			       1, 2, 1, 2, 1, 2, 1, 2, 1, 2, 1, 2, 1, 2, 4, 2,
			       1, 2, 1, 2, 1, 2, 1, 2, 1, 2, 1, 2, 1, 2, 1, 2,
			       1, 2, 1, 2, 1, 2, 1, 2, 1, 2, 1, 2, 1, 2, 1, 4};
		
		rp.receiveAudio(test1);
		
		assertEquals(1, rp.getTs());
		assertEquals(14000f, rp.getLf(), 0);
		assertEquals(500f, rp.getS(), 0);
		assertEquals(2, rp.getM());
		assertEquals(1, rp.getSize());
	}
	
	public void testLocateToDecodeAudio() {
		
		int[] test1 = {1, 4, 2, 4, 1, 4,
				       2, 1, 2, 1, 2, 1, 2, 1, 2, 1, 2, 1, 2, 1, 2, 1,
			           2, 1, 2, 1, 2, 1, 2, 1, 2, 1, 2, 1, 2, 1, 2, 4,
			           2, 4, 2, 1, 2, 4, 1, 2, 1, 4, 2, 4, 1, 2, 4, 2,
			           4, 1, 2, 1, 2, 1, 2, 1, 2, 1, 2, 1, 2, 1, 2, 1,
			           2, 4, 2, 1, 2, 1, 4, 1, 4, 1, 4, 1, 4, 2, 4, 2,
			           1, 2, 1, 2, 1, 2, 1, 2, 1, 2, 1, 2, 1, 2, 1, 2,
			           1, 2, 1, 2, 1, 2, 1, 2, 1, 2, 1, 2, 1, 2, 1, 2,
			           1, 2, 1, 2, 1, 2, 1, 2, 1, 2, 1, 2, 1, 2, 4, 2,
			           1, 2, 1, 2, 1, 2, 1, 2, 1, 2, 1, 2, 1, 2, 1, 2,
			           1, 2, 1, 2, 1, 2, 1, 2, 1, 2, 1, 2, 1, 4, 2, 1,
			           2, 1, 2, 1, 2, 1, 2, 1, 2, 1, 2, 1, 2, 1, 2, 1,
		               2, 1, 2, 1, 2, 1, 2, 1, 2, 1, 2, 1, 2, 1, 4, 1,
		               1, 4, 2, 4, 1, 4};
		
		rp.receiveAudio(test1);
		
		assertEquals(1, rp.getTs());
		assertEquals(14000f, rp.getLf(), 0);
		assertEquals(500f, rp.getS(), 0);
		assertEquals(2, rp.getM());
		assertEquals(4, rp.getSize());
		
		byte[] data = rp.getData();
				
		ByteBuffer buff = ByteBuffer.wrap(data);
		int value = buff.getInt();
		
		assertEquals(3, value);
		
		assertEquals(4, data.length);
	}
	
	public void testLocateToDecodeAudioDuplicateData() {
		
		int[] test1 = {1, 4, 2, 4, 1, 4,
				       2, 1, 2, 1, 2, 1, 2, 1, 2, 1, 2, 1, 2, 1, 2, 1,
			           2, 1, 2, 1, 2, 1, 2, 1, 2, 1, 2, 1, 2, 1, 2, 4,
			           2, 4, 2, 1, 2, 4, 1, 2, 1, 4, 2, 4, 1, 2, 4, 2,
			           4, 1, 2, 1, 2, 1, 2, 1, 2, 1, 2, 1, 2, 1, 2, 1,
			           2, 4, 2, 1, 2, 1, 4, 1, 4, 1, 4, 1, 4, 2, 4, 2,
			           1, 2, 1, 2, 1, 2, 1, 2, 1, 2, 1, 2, 1, 2, 1, 2,
			           1, 2, 1, 2, 1, 2, 1, 2, 1, 2, 1, 2, 1, 2, 1, 2,
			           1, 2, 1, 2, 1, 2, 1, 2, 1, 2, 1, 2, 1, 2, 4, 2,
			           1, 2, 1, 2, 1, 2, 1, 2, 1, 2, 1, 2, 1, 2, 1, 2,
			           1, 2, 1, 2, 1, 2, 1, 2, 1, 2, 1, 2, 1, 4, 2, 1,
			           2, 1, 2, 1, 2, 1, 2, 1, 2, 1, 1, 1, 2, 1, 2, 1, 2, 1,
		               2, 1, 2, 1, 2, 1, 2, 1, 2, 1, 2, 1, 2, 1, 4, 1,
		               1, 4, 2, 4, 1, 4};
		
		rp.receiveAudio(test1);
		
		assertEquals(1, rp.getTs());
		assertEquals(14000f, rp.getLf(), 0);
		assertEquals(500f, rp.getS(), 0);
		assertEquals(2, rp.getM());
		assertEquals(4, rp.getSize());
		
		byte[] data = rp.getData();
				
		ByteBuffer buff = ByteBuffer.wrap(data);
		int value = buff.getInt();
		
		assertEquals(3, value);
		
		assertEquals(4, data.length);
	}
	
	public void testDecodeAudio() {
		int[] test1 = {2, 1, 2, 1, 2, 1, 2, 1, 2, 1, 2, 1, 2, 1, 2, 1,
			           2, 1, 2, 1, 2, 1, 2, 1, 2, 1, 2, 1, 2, 1, 2, 4,
			           1, 4, 2, 4, 1, 4};
		
		rp.setCurrentStage(ReceiverParser.Stage.DECODE);
		rp.setSize(4); //num bytes
		rp.decodeAudio(test1, 0);
		
		byte[] data = rp.getData();
		
		ByteBuffer buff = ByteBuffer.wrap(data);
		int value = buff.getInt();
		
		assertEquals(1, value);
		
		assertEquals(4, data.length);
	}
	
	public void testDetermineAudio() {
		int[] test1 = {2, 1, 2, 1, 2, 1, 2, 1, 2, 1, 2, 1, 2, 1, 2, 1,
				       2, 1, 2, 1, 2, 1, 2, 1, 2, 1, 2, 1, 2, 1, 2, 4,
				       2, 4, 2, 1, 2, 4, 1, 2, 1, 4, 2, 4, 1, 2, 4, 2,
				       4, 1, 2, 1, 2, 1, 2, 1, 2, 1, 2, 1, 2, 1, 2, 1,
				       2, 4, 2, 1, 2, 1, 4, 1, 4, 1, 4, 1, 4, 2, 4, 2,
				       1, 2, 1, 2, 1, 2, 1, 2, 1, 2, 1, 2, 1, 2, 1, 2,
				       1, 2, 1, 2, 1, 2, 1, 2, 1, 2, 1, 2, 1, 2, 1, 2,
				       1, 2, 1, 2, 1, 2, 1, 2, 1, 2, 1, 2, 1, 2, 4, 2,
				       1, 2, 1, 2, 1, 2, 1, 2, 1, 2, 1, 2, 1, 2, 1, 2,
				       1, 2, 1, 2, 1, 2, 1, 2, 1, 2, 1, 2, 1, 2, 1, 4};
		
		rp.setCurrentStage(ReceiverParser.Stage.DETERMINE);
		rp.determineAudio(test1, 0);
		assertEquals(1, rp.getTs());
		assertEquals(14000f, rp.getLf(), 0);
		assertEquals(500f, rp.getS(), 0);
		assertEquals(2, rp.getM());
		assertEquals(1, rp.getSize());
	}
	
	public void testFloatEquals() {
		assertEquals(true, rp.floatEquals(1.0f, 1.0f));
		assertEquals(false, rp.floatEquals(1.0f, 1.100000000f));
		assertEquals(false, rp.floatEquals(1.0f, 1.0100000000f));
		assertEquals(false, rp.floatEquals(1.0f, 1.00100000000f));
		assertEquals(false, rp.floatEquals(1.0f, 1.000100000000f));
	}
	
	public void testSetFrequencies() {
		float[] frequencies = rp.getFrequencies(1);
		assertEquals(1, frequencies.length);
		assertEquals(14000, frequencies[0], 0);
		
		frequencies = rp.getFrequencies(2);
		assertEquals(1, frequencies.length);
		assertEquals(14500, frequencies[0], 0);
		
		frequencies = rp.getFrequencies(3);
		assertEquals(2, frequencies.length);
		assertEquals(14500, frequencies[0], 0);
		assertEquals(14000, frequencies[1], 0);
		
		frequencies = rp.getFrequencies(4);
		assertEquals(1, frequencies.length);
		assertEquals(15000, frequencies[0], 0);
		
		frequencies = rp.getFrequencies(5);
		assertEquals(2, frequencies.length);
		assertEquals(15000, frequencies[0], 0);
		assertEquals(14000, frequencies[1], 0);
		
		frequencies = rp.getFrequencies(6);
		assertEquals(2, frequencies.length);
		assertEquals(15000, frequencies[0], 0);
		assertEquals(14500, frequencies[1], 0);
		
		frequencies = rp.getFrequencies(7);
		assertEquals(3, frequencies.length);
		assertEquals(15000, frequencies[0], 0);
		assertEquals(14500, frequencies[1], 0);
		assertEquals(14000, frequencies[2], 0);
	}
}
