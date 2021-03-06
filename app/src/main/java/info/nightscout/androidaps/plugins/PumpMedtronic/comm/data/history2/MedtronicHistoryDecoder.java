package info.nightscout.androidaps.plugins.PumpMedtronic.comm.data.history2;


import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import info.nightscout.androidaps.plugins.PumpCommon.utils.ByteUtil;
import info.nightscout.androidaps.plugins.PumpCommon.utils.StringUtil;
import info.nightscout.androidaps.plugins.PumpMedtronic.comm.data.RawHistoryPage;
import info.nightscout.androidaps.plugins.PumpMedtronic.util.MedtronicUtil;

/**
 * Application:   GGC - GNU Gluco Control
 * Plug-in:       GGC PlugIn Base (base class for all plugins)
 * <p>
 * See AUTHORS for copyright information.
 * <p>
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 * <p>
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * <p>
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 59 Temple
 * Place, Suite 330, Boston, MA 02111-1307 USA
 * <p>
 * Filename:     DeviceIdentification
 * Description:  Class for display of Device Identification.
 * <p>
 * Author: Andy {andy@atech-software.com}
 */
public abstract class MedtronicHistoryDecoder {

    private static final Logger LOG = LoggerFactory.getLogger(MedtronicHistoryDecoder.class);

    protected ByteUtil bitUtils;

    // STATISTICS (remove at later time or not)
    protected boolean statisticsEnabled = true;
    protected Map<Integer, Integer> unknownOpCodes;
    protected Map<RecordDecodeStatus, Map<String, String>> mapStatistics;


    public MedtronicHistoryDecoder() {
    }


    public abstract RecordDecodeStatus decodeRecord(MedtronicHistoryEntry record);


    public abstract void postProcess();


    //public abstract void refreshOutputWriter();


    public boolean decodePage(RawHistoryPage dataPage) throws Exception {
        //refreshOutputWriter();

        List<? extends MedtronicHistoryEntry> minimedHistoryRecords = processPageAndCreateRecords(dataPage);

        for(MedtronicHistoryEntry record : minimedHistoryRecords) {
            decodeRecord(record);
        }

        runPostDecodeTasks();

        return true;
    }


    protected abstract void runPostDecodeTasks();


    // TODO_ extend this to also use bigger pages (for now we support only 1024
    // pages)
    public List<Byte> checkPage(RawHistoryPage page) throws RuntimeException {
        List<Byte> byteList = new ArrayList<Byte>();

        if (page.getData().length != 1024 /*page.commandType.getRecordLength()*/) {
            LOG.error("Page size is not correct. Size should be {}, but it was {} instead.", 1024, page.getData().length);
            // throw exception perhaps
            return byteList;
        }

        if (MedtronicUtil.getMedtronicPumpModel() == null) {
            LOG.error("Device Type is not defined.");
            return byteList;
        }

        if (page.isChecksumOK()) {
            return ByteUtil.getListFromByteArray(page.getOnlyData());
        } else {
            return null;
        }
    }


    public abstract List<? extends MedtronicHistoryEntry> processPageAndCreateRecords(RawHistoryPage page) throws Exception;


    protected void prepareStatistics() {
        if (!statisticsEnabled)
            return;

        unknownOpCodes = new HashMap<Integer, Integer>();
        mapStatistics = new HashMap<RecordDecodeStatus, Map<String, String>>();

        for(RecordDecodeStatus stat : RecordDecodeStatus.values()) {
            mapStatistics.put(stat, new HashMap<String, String>());
        }
    }


    protected void addToStatistics(MedtronicHistoryEntry pumpHistoryEntry, RecordDecodeStatus status, Integer opCode) {
        if (!statisticsEnabled)
            return;

        if (opCode != null) {
            if (!unknownOpCodes.containsKey(opCode)) {
                unknownOpCodes.put(opCode, opCode);
            }
            return;
        }

        if (!mapStatistics.get(status).containsKey(pumpHistoryEntry.getEntryType().name())) {
            mapStatistics.get(status).put(pumpHistoryEntry.getEntryType().name(), "");
        }
    }


    protected void showStatistics() {
        StringBuilder sb = new StringBuilder();

        for(Map.Entry unknownEntry : unknownOpCodes.entrySet()) {
            StringUtil.appendToStringBuilder(sb, "" + unknownEntry.getKey(), ", ");
        }

        LOG.debug("STATISTICS OF PUMP DECODE");

        if (unknownOpCodes.size() > 0) {
            LOG.debug("Unknown Op Codes: {}", sb.toString());
        }

        for(Map.Entry<RecordDecodeStatus, Map<String, String>> entry : mapStatistics.entrySet()) {
            sb = new StringBuilder();

            if (entry.getKey() != RecordDecodeStatus.OK) {
                if (entry.getValue().size() == 0)
                    continue;

                for(Map.Entry<String, String> entrysub : entry.getValue().entrySet()) {
                    StringUtil.appendToStringBuilder(sb, entrysub.getKey(), ", ");
                }

                String spaces = StringUtils.repeat(" ", 14 - entry.getKey().name().length());

                LOG.debug("    {}{} - {}. Elements: {}", entry.getKey().name(), spaces, entry.getValue().size(), sb.toString());
            } else {
                LOG.debug("    {}             - {}", entry.getKey().name(), entry.getValue().size());
            }
        }
    }


    private int getUnsignedByte(byte value) {
        if (value < 0)
            return value + 256;
        else
            return value;
    }


    protected int getUnsignedInt(int value) {
        if (value < 0)
            return value + 256;
        else
            return value;
    }


    public String getFormattedFloat(float value, int decimals) {
        return StringUtil.getFormatedValueUS(value, decimals);
    }

}
