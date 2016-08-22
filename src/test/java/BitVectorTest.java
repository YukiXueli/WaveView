//
// Copyright 2016 Jeff Bush
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
//

import static org.junit.Assert.*;
import org.junit.*;

public class BitVectorTest {
    @Test
    public void testParseHex() {
        assertEquals(0, (new BitVector("0", 16)).intValue());
        assertEquals(1, (new BitVector("1", 16)).intValue());
        assertEquals(2, (new BitVector("2", 16)).intValue());
        assertEquals(12, (new BitVector("c", 16)).intValue());
        assertEquals(12, (new BitVector("C", 16)).intValue());
        assertEquals(16, (new BitVector("10", 16)).intValue());
        assertEquals(17, (new BitVector("11", 16)).intValue());
        assertEquals(432, (new BitVector("1B0", 16)).intValue());
        assertEquals(512, (new BitVector("200", 16)).intValue());
        assertEquals(513, (new BitVector("201", 16)).intValue());

        BitVector bv = new BitVector("123456789abcdefABCDEF", 16);
        assertFalse(bv.isZ());
        assertFalse(bv.isX());
        assertEquals(84, bv.getWidth());
        assertEquals("123456789ABCDEFABCDEF", bv.toString(16));
    }

    @Test
    public void testParseBinary() {
        BitVector bv = new BitVector("110111010100100100101100101001001010001001", 2);
        assertFalse(bv.isZ());
        assertFalse(bv.isX());
        assertEquals(42, bv.getWidth());
        assertEquals("110111010100100100101100101001001010001001", bv.toString(2));
    }

    @Test
    public void testParseDecimal() {
        BitVector bv = new BitVector("3492343343482759676947735281634934371324", 10);
        assertFalse(bv.isZ());
        assertFalse(bv.isX());
        assertEquals("3492343343482759676947735281634934371324", bv.toString(10));
    }

    @Test
    public void testParseX() {
        BitVector bv = new BitVector("xxxxxxxxxxx", 2);
        assertEquals(11, bv.getWidth());
        assertFalse(bv.isZ());
        assertTrue(bv.isX());
        assertEquals("xxxxxxxxxxx", bv.toString(2));
    }

    @Test
    public void testParseZ() {
        BitVector bv = new BitVector("zzzzzzzz", 2);
        assertEquals(8, bv.getWidth());
        assertTrue(bv.isZ());
        assertTrue(bv.isX());
        assertEquals("zzzzzzzz", bv.toString(2));
    }

    @Test
    public void testNumberFormatException() {
        try {
            BitVector bv = new BitVector("12345", 2);
            fail("Did not throw exception");
        } catch (NumberFormatException exc) {
        }

        try {
            BitVector bv = new BitVector("ABCDEFGH", 16);
            fail("Did not throw exception");
        } catch (NumberFormatException exc) {
        }

        try {
            BitVector bv = new BitVector("1234a", 10);
            fail("Did not throw exception");
        } catch (NumberFormatException exc) {
        }

        try {
            BitVector bv = new BitVector("z", 10);
            fail("Did not throw exception");
        } catch (NumberFormatException exc) {
        }

        try {
            BitVector bv = new BitVector("x", 10);
            fail("Did not throw exception");
        } catch (NumberFormatException exc) {
        }
    }

    @Test
    public void testSetBit() {
        BitVector bv = new BitVector(8);
        bv.setBit(7, BitVector.VALUE_1);
        bv.setBit(6, BitVector.VALUE_0);
        bv.setBit(5, BitVector.VALUE_1);
        bv.setBit(4, BitVector.VALUE_Z);
        bv.setBit(3, BitVector.VALUE_0);
        bv.setBit(2, BitVector.VALUE_1);
        bv.setBit(1, BitVector.VALUE_X);
        bv.setBit(0, BitVector.VALUE_1);
        assertEquals("101z01x1", bv.toString(2));
    }

    @Test
    public void testGetBit() {
        BitVector bv = new BitVector("1x010z10", 2);
        assertEquals(BitVector.VALUE_1, bv.getBit(7));
        assertEquals(BitVector.VALUE_X, bv.getBit(6));
        assertEquals(BitVector.VALUE_0, bv.getBit(5));
        assertEquals(BitVector.VALUE_1, bv.getBit(4));
        assertEquals(BitVector.VALUE_0, bv.getBit(3));
        assertEquals(BitVector.VALUE_Z, bv.getBit(2));
        assertEquals(BitVector.VALUE_1, bv.getBit(1));
        assertEquals(BitVector.VALUE_0, bv.getBit(0));
    }

    @Test
    public void testCompare() {
        BitVector bv1 = new BitVector("1", 2);      // Shorter than others
        BitVector bv2 = new BitVector("01001", 2);  // Has leading zeros
        BitVector bv3 = new BitVector("10100", 2);
        BitVector bv4 = new BitVector("10101", 2);  // Same length as last, trailing 1

        assertEquals(0, bv1.compare(bv1));
        assertEquals(-1, bv1.compare(bv2));
        assertEquals(-1, bv1.compare(bv3));
        assertEquals(-1, bv1.compare(bv4));
        assertEquals(1, bv2.compare(bv1));
        assertEquals(0, bv2.compare(bv2));
        assertEquals(-1, bv2.compare(bv3));
        assertEquals(-1, bv2.compare(bv4));
        assertEquals(1, bv3.compare(bv1));
        assertEquals(1, bv3.compare(bv2));
        assertEquals(0, bv3.compare(bv3));
        assertEquals(-1, bv3.compare(bv4));
        assertEquals(1, bv3.compare(bv1));
        assertEquals(1, bv3.compare(bv2));
        assertEquals(0, bv3.compare(bv3));
        assertEquals(-1, bv3.compare(bv4));
    }
}
