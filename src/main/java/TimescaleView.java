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
import java.awt.*;
import java.util.*;
import java.awt.event.*;

///
/// Draws the ruler with times at the top of the trace view.
///
public class TimescaleView extends JPanel implements TraceViewModelListener, ActionListener
{
    private static final int kMinorTickTop = 18;
    private static final int kMinorTicksPerMajorTick = 10;
    private static final int kMinMinorTickSize = 5;
    private static final int kTimescaleHeight = 25;
    private static final int kMaxTimestampLabelWidth = 65;
    private static final int kTimestampBorder = 2;
    private static final int kTimestampDisappearInterval = 500;

    public TimescaleView(TraceViewModel viewModel, TraceDataModel dataModel)
    {
        fTraceViewModel = viewModel;
        fTraceDataModel = dataModel;
        fTraceViewModel.addListener(this);
        setBackground(AppPreferences.getInstance().kBackgroundColor);
        setPreferredSize(new Dimension(200, kTimescaleHeight));
        setFont(new Font("SansSerif", Font.PLAIN, 9));
        scaleChanged(fTraceViewModel.getHorizontalScale());
        fTimestampDisplayTimer.setRepeats(false);
    }

    private void adjustCanvasSize()
    {
        scaleChanged(fTraceViewModel.getHorizontalScale());
    }

    public void cursorChanged(long oldTimestamp, long newTimestamp)
    {
        int oldX = (int)(oldTimestamp / fTraceViewModel.getHorizontalScale());
        int newX = (int)(newTimestamp / fTraceViewModel.getHorizontalScale());
        int leftEdge = Math.min(oldX, newX) - kMaxTimestampLabelWidth;
        int rightEdge = Math.max(oldX, newX) + kMaxTimestampLabelWidth;
        repaint(leftEdge, 0, rightEdge - leftEdge, getHeight());
        if (fTraceViewModel.getAdjustingCursor())
            fShowTimestamp = true;
        else
            fTimestampDisplayTimer.start();
    }

    public void markerChanged(long timestamp)
    {
        if (timestamp < 0)
            repaint();
        else
        {
            int x = (int)(timestamp / fTraceViewModel.getHorizontalScale());
            repaint(x - 1, 0, 2, kTimescaleHeight);
        }
    }

    public void netsAdded(int firstIndex, int lastIndex)
    {
        adjustCanvasSize();
    }

    public void netsRemoved(int firstIndex, int lastIndex)
    {
    }

    public void scaleChanged(double newScale)
    {
        // Make sure minor ticks are at least 10 pixels wide
        fMinorTickInterval = 1;
        while (fMinorTickInterval / fTraceViewModel.getHorizontalScale() < kMinMinorTickSize)
            fMinorTickInterval *= 10;

        if (fMinorTickInterval < 100)
        {
            fUnitMagnitude = 1;
            fUnit = "ns";
        }
        else if (fMinorTickInterval < 100000)
        {
            fUnitMagnitude = 1000;
            fUnit = "us";
        }
        else if (fMinorTickInterval < 100000000)
        {
            fUnitMagnitude = 1000000;
            fUnit = "ms";
        }
        else
        {
            fUnitMagnitude = 1000000000;
            fUnit = "s";
        }

        Dimension d = getPreferredSize();
        d.width = (int)(fTraceDataModel.getMaxTimestamp() / fTraceViewModel.getHorizontalScale());
        setPreferredSize(d);
        revalidate();
        repaint();
    }

    protected void paintComponent(Graphics g)
    {
        super.paintComponent(g);

        g.setColor(AppPreferences.getInstance().kTraceColor);

        Rectangle visibleRect = getVisibleRect();

        FontMetrics metrics = g.getFontMetrics();

        // The -100 in start time keeps labels that are to the left of the window from not being drawn
        // (which causes artifacts when scrolling).  It needs to be bigger than the largest label.
        long startTime = (long)((visibleRect.x - 100) * fTraceViewModel.getHorizontalScale());
        long endTime = (long) ((visibleRect.x + visibleRect.width) * fTraceViewModel.getHorizontalScale());
        if (startTime < 0)
            startTime = 0;

        startTime = (startTime / fMinorTickInterval) * fMinorTickInterval;    // Round to an event tick boundary
        for (long ts = startTime; ts < endTime; ts += fMinorTickInterval)
        {
            int x = (int)(ts / fTraceViewModel.getHorizontalScale());
            if ((ts / fMinorTickInterval) % kMinorTicksPerMajorTick == 0)
            {
                g.drawLine(x, 5, x, 25);
                g.drawString(Long.toString(ts / fUnitMagnitude) + " " + fUnit, x + 3, kMinorTickTop
                    - metrics.getDescent() - 2);
            }
            else
                g.drawLine(x, kMinorTickTop, x, 25);
        }

        // Draw Markers
        int markerIndex = fTraceViewModel.getMarkerAtTime(startTime);
        while (markerIndex < fTraceViewModel.getMarkerCount())
        {
            long timestamp = fTraceViewModel.getTimestampForMarker(markerIndex);
            if (timestamp > endTime)
                break;

            String labelString = "" + fTraceViewModel.getIdForMarker(markerIndex);
            int labelWidth = g.getFontMetrics().stringWidth(labelString);
            int x = (int) (timestamp / fTraceViewModel.getHorizontalScale());
            g.setColor(AppPreferences.getInstance().kBackgroundColor);
            g.fillRect(x - (labelWidth / 2 + kTimestampBorder), kTimescaleHeight - 15, labelWidth + kTimestampBorder * 2, 15);
            g.setColor(AppPreferences.getInstance().kTraceColor);
            g.drawString(labelString, x - labelWidth / 2, kTimescaleHeight - 3);
            markerIndex++;
        }

        if (fShowTimestamp)
        {
            String timeString = "" + (double) fTraceViewModel.getCursorPosition() / fUnitMagnitude + " " + fUnit;
            int timeWidth = g.getFontMetrics().stringWidth(timeString);
            int cursorX = (int) (fTraceViewModel.getCursorPosition() / fTraceViewModel.getHorizontalScale());
            int labelLeft = cursorX + timeWidth > visibleRect.x + visibleRect.width
                ? cursorX - timeWidth : cursorX;

            g.setColor(AppPreferences.getInstance().kBackgroundColor);
            g.fillRect(labelLeft - kTimestampBorder, kTimescaleHeight - 15, timeWidth + kTimestampBorder * 2, 15);
            g.setColor(AppPreferences.getInstance().kTraceColor);
            g.drawRect(labelLeft - kTimestampBorder, kTimescaleHeight - 15, timeWidth + kTimestampBorder * 2, 15);
            g.setColor(AppPreferences.getInstance().kTraceColor);
            g.drawString(timeString, labelLeft + kTimestampBorder, kTimescaleHeight - kTimestampBorder);
        }
    }

    public void actionPerformed(ActionEvent e)
    {
        fShowTimestamp = false;
        repaint();
    }

    private boolean fShowTimestamp;
    private int fUnitMagnitude = 1;
    private String fUnit = "s";
    private int fMinorTickInterval = 0;    // In time units
    private TraceViewModel fTraceViewModel;
    private TraceDataModel fTraceDataModel;
    private int fOldCursor;
    private javax.swing.Timer fTimestampDisplayTimer = new javax.swing.Timer(kTimestampDisappearInterval, this);
}