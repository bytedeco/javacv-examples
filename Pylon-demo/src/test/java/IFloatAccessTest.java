import org.bytedeco.javacpp.GenICam3.IFloat;
import org.bytedeco.javacpp.Pylon5.CImageFormatConverter;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Jarek Sacha
 */
public final class IFloatAccessTest {


    @Test
    public void testIFloatAccess() throws Exception {

        CImageFormatConverter converter = new CImageFormatConverter();
        IFloat gamma = converter.Gamma();

        // This should execute without crashing JVM
        double gammaValue = gamma.GetValue();

        // Set to new value
        gamma.SetValue(1.2);

        assertEquals("GetValue", 1.2, gamma.GetValue(), 0.001);
    }
}