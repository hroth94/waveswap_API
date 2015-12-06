package main;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;

public class ByteRepresentation
{
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
    
    public static void main(String[] args) {
        String toEvaluate = "TEST";
        int toEvaluateI = 2;
        
        byte[] toEvaluateBytesDEFAULT = toEvaluate.getBytes();
        byte[] toEvaluateBytesUTF8 = {};
        byte[] toEvaluateBytesUTF16 = {};
        try
        {
            toEvaluateBytesUTF8 = toEvaluate.getBytes("UTF-8");
        }
        catch (UnsupportedEncodingException e1)
        {
            e1.printStackTrace();
        }
        try
        {
            toEvaluateBytesUTF16 = toEvaluate.getBytes("UTF-16");
        }
        catch (UnsupportedEncodingException e)
        {
            e.printStackTrace();
        }
        byte[] toEvaluateIBytes = ByteBuffer.allocate(4).putInt(toEvaluateI).array();
        
        System.out.println("----DEFAULT----");
        for(int i = 0; i < toEvaluateBytesDEFAULT.length; i++) {
            System.out.println(byteToString(toEvaluateBytesDEFAULT[i]));
        }
        System.out.println("----UTF-8----");
        for(int i = 0; i < toEvaluateBytesUTF8.length; i++) {
            System.out.println(byteToString(toEvaluateBytesUTF8[i]));
        }
        System.out.println("----UTF-16----");
        for(int i = 0; i < toEvaluateBytesUTF16.length; i++) {
            System.out.println(byteToString(toEvaluateBytesUTF16[i]));
        }
        System.out.println("----INT----");
        for(int i = 0; i < toEvaluateIBytes.length; i++) {
            System.out.println(byteToString(toEvaluateIBytes[i]));
        }
    }
}
