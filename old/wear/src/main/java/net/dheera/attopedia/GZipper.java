package net.dheera.attopedia;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Convenient static methods for compressing and decompressing
 * byte arrays without having to deal with all the stream mess.
 */
public class GZipper {

    /**
     * gzips a byte array and returns a byte array
     * @param data uncompressed byte array
     * @return compressed array
     * @throws IOException
     */
    public static byte[] compress(byte[] data) throws IOException {
        ByteArrayOutputStream os = new ByteArrayOutputStream(data.length);
        GZIPOutputStream gos = new GZIPOutputStream(os);
        gos.write(data);
        gos.close();
        byte[] compressed = os.toByteArray();
        os.close();
        return compressed;
    }

    /**
     * gunzips a byte array and returns a byte array
     * @param compressed compressed byte array
     * @return uncompressed array
     * @throws IOException
     */
    public static byte[] decompress(byte[] compressed) throws IOException {
        final int BUFFER_SIZE = 32;
        ByteArrayInputStream is = new ByteArrayInputStream(compressed);
        GZIPInputStream gis = new GZIPInputStream(is, BUFFER_SIZE);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] data = new byte[BUFFER_SIZE];
        int bytesRead;
        while ((bytesRead = gis.read(data)) != -1) {
            out.write(data, 0, bytesRead);
        }
        gis.close();
        is.close();
        return out.toByteArray();
    }
}
