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

public class TransitionVectorTest
{
    @Test public void testFindTransition()
    {
        TransitionVector vec = new TransitionVector(8);
        vec.appendTransition(100, new BitVector("00000001", 2));
        vec.appendTransition(110, new BitVector("00000010", 2));
        vec.appendTransition(111, new BitVector("00001000", 2));
        vec.appendTransition(112, new BitVector("00010000", 2));
        vec.appendTransition(115, new BitVector("00010000", 2));

        Transition t = vec.findTransition(99).current();
        assertEquals(t.getTimestamp(), 100);
        assertEquals(0, t.compare(new BitVector("00000001", 2)));

        t = vec.findTransition(100).current();
        assertEquals(100, t.getTimestamp());
        assertEquals(0, t.compare(new BitVector("00000001", 2)));

        t = vec.findTransition(101).current();
        assertEquals(100, t.getTimestamp());
        assertEquals(0, t.compare(new BitVector("00000001", 2)));

        t = vec.findTransition(105).current();
        assertEquals(100, t.getTimestamp());
        assertEquals(0, t.compare(new BitVector("00000001", 2)));

        t = vec.findTransition(109).current();
        assertEquals(100, t.getTimestamp());
        assertEquals(0, t.compare(new BitVector("00000001", 2)));

        t = vec.findTransition(110).current();
        assertEquals(110, t.getTimestamp());
        assertEquals(0, t.compare(new BitVector("00000010", 2)));

        t = vec.findTransition(111).current();
        assertEquals(111, t.getTimestamp());
        assertEquals(0, t.compare(new BitVector("00001000", 2)));

        t = vec.findTransition(112).current();
        assertEquals(112, t.getTimestamp());
        assertEquals(0, t.compare(new BitVector("00010000", 2)));

        t = vec.findTransition(113).current();
        assertEquals(112, t.getTimestamp());
        assertEquals(0, t.compare(new BitVector("00010000", 2)));

        t = vec.findTransition(116).current();
        assertEquals(115, t.getTimestamp());
        assertEquals(0, t.compare(new BitVector("00010000", 2)));

        t = vec.findTransition(20000).current();
        assertEquals(115, t.getTimestamp());
        assertEquals(0, t.compare(new BitVector("00010000", 2)));
    }

    @Test public void testIterator()
    {
        TransitionVector vec = new TransitionVector(8);
        vec.appendTransition(100, new BitVector("00000001", 2));
        vec.appendTransition(110, new BitVector("00000010", 2));
        vec.appendTransition(111, new BitVector("00001000", 2));

        // Note: previously there was a bug where calling current() would
        // iterate to the next value. We call current here multiple times
        // to confirm that works correctly now.
        AbstractTransitionIterator ti = vec.findTransition(99);
        assertEquals(100, ti.current().getTimestamp());
        assertEquals(0, ti.current().compare(new BitVector("00000001", 2)));
        assertTrue(ti.hasNext());
        assertEquals(110, ti.getNextTimestamp());
        assertEquals(-1, ti.getPrevTimestamp());

        ti.next();

        assertEquals(110, ti.current().getTimestamp());
        assertEquals(0, ti.current().compare(new BitVector("00000010", 2)));
        assertTrue(ti.hasNext());
        assertEquals(111, ti.getNextTimestamp());
        assertEquals(100, ti.getPrevTimestamp());

        ti.next();

        assertEquals(111, ti.current().getTimestamp());
        assertEquals(0, ti.current().compare(new BitVector("00001000", 2)));
        assertFalse(ti.hasNext());
        assertEquals(-1, ti.getNextTimestamp());
        assertEquals(110, ti.getPrevTimestamp());
    }
}
