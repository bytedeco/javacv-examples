import org.bytedeco.javacpp.GenICam3.IInteger;
import org.bytedeco.javacpp.Pylon5.CImageFormatConverter;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Jarek Sacha
 */
public final class IIntegerAccessTest {

    @Test
    public void testIFloatAccess() throws Exception {

        CImageFormatConverter converter = new CImageFormatConverter();
        IInteger outputPaddingX = converter.OutputPaddingX();
        assertEquals("outputPaddingX.GetValue()", 0, outputPaddingX.GetValue());

        outputPaddingX.SetValue(8);
        assertEquals("outputPaddingX.GetValue()", 8, outputPaddingX.GetValue());
    }
}