//
// Copyright 2011-2012 Jeff Bush
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

import javax.swing.*;
import javax.swing.event.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.awt.datatransfer.*;

///
/// Displays names of nets next to waveforms, along with value at cursor
///
class NetNameView extends JList<Integer> implements TraceViewModel.Listener {
    private static final int kDragThreshold = 15;

    class NetNameRenderer extends JPanel implements ListCellRenderer<Integer> {
        private int fCurrentNet;
        private boolean fCurrentNetIsSelected;
        private int fLabelBaseline = -1;
        private int fValueBaseline;
        private Font fLabelFont = new Font("SansSerif", Font.BOLD, 10);
        private Font fValueFont = new Font("SansSerif", Font.PLAIN, 8);

        NetNameRenderer() {
            setOpaque(true);
        }

        @Override
        public Component getListCellRendererComponent(
            JList<? extends Integer> list,
            Integer value,
            int index,
            boolean isSelected,
            boolean cellHasFocus) {
            fCurrentNet = value.intValue();
            fCurrentNetIsSelected = isSelected;

            return this;
        }

        @Override
        protected void paintComponent(Graphics g) {
            AppPreferences prefs = AppPreferences.getInstance();
            setBackground(prefs.kBackgroundColor);

            super.paintComponent(g);

            if (fCurrentNet == -1)
                return;

            if (fLabelBaseline == -1) {
                // Initialize this stuff once, then cache results
                FontMetrics labelMetrics = g.getFontMetrics(fLabelFont);
                FontMetrics valueMetrics = g.getFontMetrics(fValueFont);
                fLabelBaseline = labelMetrics.getAscent();
                fValueBaseline = fLabelBaseline + labelMetrics.getDescent() + labelMetrics.getLeading()
                                 + valueMetrics.getAscent();
                int totalHeight = fValueBaseline + valueMetrics.getDescent();
                int border = (DrawMetrics.WAVEFORM_V_SPACING - totalHeight) / 2;
                fLabelBaseline += border;
                fValueBaseline += border;
            }

            if (fCurrentNetIsSelected)
                g.setColor(prefs.kListSelectionBgColor);
            else
                g.setColor(prefs.kBackgroundColor);

            g.fillRect(0, 0, getWidth(), DrawMetrics.WAVEFORM_V_SPACING);
            g.setFont(fLabelFont);
            g.setColor(fCurrentNetIsSelected ? prefs.kListSelectionFgColor
                       : prefs.kTraceColor);

            int netId = fTraceViewModel.getVisibleNet(fCurrentNet);
            String name = fTraceDataModel.getShortNetName(netId);
            g.drawString(name, 1, fLabelBaseline);

            g.setColor(fCurrentNetIsSelected ? prefs.kListSelectionFgColor
                       : prefs.kValueColor);
            g.setFont(fValueFont);

            Transition t = fTraceDataModel.findTransition(netId,
                           fTraceViewModel.getCursorPosition()).next();
            g.drawString(fTraceViewModel.getValueFormatter(fCurrentNet).format(t),
                         1, fValueBaseline);
        }

        @Override
        public String getToolTipText(MouseEvent event) {
            return fTraceDataModel.getFullNetName(fTraceViewModel.getVisibleNet(fCurrentNet));
        }
    }

    class ListModelAdapter implements ListModel<Integer>, TraceViewModel.Listener {
        public ListModelAdapter() {
            fTraceViewModel.addListener(this);
        }

        @Override
        public void addListDataListener(ListDataListener l) {
            assert fListener == null;
            fListener = l;
        }

        @Override
        public void removeListDataListener(ListDataListener l) {
            assert fListener != null;
            fListener = null;
        }

        @Override
        public Integer getElementAt(int index) {
            return new Integer(index);  // @todo preallocate?
        }

        @Override
        public int getSize() {
            return fTraceViewModel.getVisibleNetCount();
        }

        @Override
        public void cursorChanged(long oldTimestamp, long newTimestamp) {
        }

        @Override
        public void netsAdded(int firstIndex, int lastIndex) {
            fListener.intervalAdded(new ListDataEvent(this, ListDataEvent.INTERVAL_ADDED,
                firstIndex, lastIndex));
        }

        @Override
        public void netsRemoved(int firstIndex, int lastIndex) {
            fListener.intervalRemoved(new ListDataEvent(this, ListDataEvent.INTERVAL_REMOVED,
                firstIndex, lastIndex));
        }

        @Override
        public void markerChanged(long timestamp) {
        }

        @Override
        public void scaleChanged(double newScale) {
        }

        private ListDataListener fListener;
    }

    class NetTransferHandler extends TransferHandler {
        @Override
        public int getSourceActions(JComponent component) {
            return MOVE;
        }

        @Override
        public Transferable createTransferable(JComponent component) {
            fLocalIndices = ((JList) component).getSelectedIndices();
            fIsLocalDrop = true;
            return new StringSelection("");
        }

        @Override
        public void exportDone(JComponent component, Transferable transfer, int action) {
            fIsLocalDrop = false;
            // XXX do nothing
        }

        @Override
        public boolean canImport(TransferHandler.TransferSupport support) {
            return true;
        }

        @Override
        public boolean importData(TransferHandler.TransferSupport support) {
            String data;
            try {
                data = (String) support.getTransferable().getTransferData(DataFlavor.stringFlavor);
            } catch (Exception exc) {
                System.out.println(exc.toString());
                return false;
            }

            JList.DropLocation location = (JList.DropLocation) support.getDropLocation();
            int insertionPoint = location.getIndex();
            if (fIsLocalDrop) {
                fTraceViewModel.moveNets(fLocalIndices, insertionPoint);
            } else {
                // Drag from another window (for example, net tree)
                String[] values = data.split("\n");
                for (String value : values)
                    fTraceViewModel.makeNetVisible(insertionPoint++, fTraceDataModel.findNet(value));
            }

            // @todo Deal with selection changes.  Should probably just clear the selection.

            return true;
        }

        private boolean fIsLocalDrop = false;
        private int[] fLocalIndices;
    }

    public NetNameView(TraceViewModel viewModel, TraceDataModel dataModel) {
        fTraceViewModel = viewModel;
        fTraceDataModel = dataModel;
        viewModel.addListener(this);
        setModel(new ListModelAdapter());
        setCellRenderer(new NetNameRenderer());
        computeBounds();
        setFixedCellHeight(DrawMetrics.WAVEFORM_V_SPACING);
        setDragEnabled(true);
        setDropMode(DropMode.ON_OR_INSERT);
        setTransferHandler(new NetTransferHandler());
    }

    private void computeBounds() {
        Dimension d = getPreferredSize();
        d.width = 200;
        d.height = fTraceViewModel.getVisibleNetCount() * DrawMetrics.WAVEFORM_V_SPACING;
        setPreferredSize(d);
        validate();
        repaint();
    }

    @Override
    protected void processKeyEvent(KeyEvent e) {
        /// @bug Eat key events so up/down arrows don't change selection
        /// If we allow arrows to change selection, the net name view scrolls without moving the other views.
        /// That really seems like a bug in the swing components.
    }

    @Override
    public void scaleChanged(double newScale) {
    }

    @Override
    public void netsAdded(int firstIndex, int lastIndex) {
        computeBounds();
    }

    @Override
    public void netsRemoved(int firstIndex, int lastIndex) {
        computeBounds();
    }

    @Override
    public void markerChanged(long timestamp) {
    }

    @Override
    public void cursorChanged(long oldTimestamp, long newTimestamp) {
        repaint();
    }

    private TraceViewModel fTraceViewModel;
    private TraceDataModel fTraceDataModel;
}
