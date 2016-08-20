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

/// @todo does not test signal aliases...
public class TraceDataModelTest
{
    @Test public void testTraceDataModel()
    {
        TraceDataModel model = new TraceDataModel();
        TraceBuilder builder = model.startBuilding();

        builder.enterModule("mod1");
        int net1 = builder.newNet("net1", -1, 1);
        int net2 = builder.newNet("net2", -1, 3);
        builder.enterModule("mod2");
        int net3 = builder.newNet("net3", -1, 2);
        builder.exitModule();
        builder.exitModule();

        assertEquals(1, builder.getNetWidth(net1));
        assertEquals(3, builder.getNetWidth(net2));
        assertEquals(2, builder.getNetWidth(net3));

        builder.appendTransition(net1, 10, new BitVector("1", 2));
        builder.appendTransition(net1, 20, new BitVector("0", 2));
        builder.appendTransition(net2, 10, new BitVector("100", 2));
        builder.appendTransition(net2, 15, new BitVector("010", 2));
        builder.appendTransition(net3, 12, new BitVector("11", 2));
        builder.appendTransition(net3, 17, new BitVector("01", 2));

        builder.loadFinished();

        NetTreeModel netTree = model.getNetTree();
        Object root = netTree.getRoot();
        assertEquals(3, netTree.getChildCount(root));
        Object kid0 = netTree.getChild(root, 0);
        Object kid1 = netTree.getChild(root, 1);
        Object kid2 = netTree.getChild(root, 2);
        Object grandkid0 = netTree.getChild(kid2, 0);

        assertEquals("net1", kid0.toString());
        assertTrue(netTree.isLeaf(kid0));
        assertEquals("net2", kid1.toString());
        assertTrue(netTree.isLeaf(kid1));
        assertEquals("mod2", kid2.toString());
        assertTrue(!netTree.isLeaf(kid2));
        assertEquals("net3", grandkid0.toString());
        assertTrue(netTree.isLeaf(grandkid0));

        // Make sure the net IDs in the tree match what was given during building
        assertEquals(net1, model.getNetFromTreeObject(kid0));
        assertEquals(net2, model.getNetFromTreeObject(kid1));
        assertEquals(net3, model.getNetFromTreeObject(grandkid0));

        assertEquals(3, model.getTotalNetCount());
        assertEquals(20, model.getMaxTimestamp());

        assertEquals(net1, model.findNet("mod1.net1"));
        assertEquals(net2, model.findNet("mod1.net2"));
        assertEquals(net3, model.findNet("mod1.mod2.net3"));
        assertEquals(1, model.getNetWidth(net1));
        assertEquals(3, model.getNetWidth(net2));
        assertEquals(2, model.getNetWidth(net3));
        assertEquals("net1", model.getShortNetName(net1));
        assertEquals("net2", model.getShortNetName(net2));
        assertEquals("net3", model.getShortNetName(net3));
        assertEquals("mod1.net1", model.getFullNetName(net1));
        assertEquals("mod1.net2", model.getFullNetName(net2));
        assertEquals("mod1.mod2.net3", model.getFullNetName(net3));

        AbstractTransitionIterator ati  = model.findTransition(net2, 12);
        assertEquals(10, ati.current().getTimestamp());
        assertEquals(0, ati.current().compare(new BitVector("100", 2)));
    }
}