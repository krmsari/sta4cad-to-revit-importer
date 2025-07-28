package com.kerem.sta4cadimp.service;

import com.kerem.sta4cadimp.dtos.ColumnTypeDefinition;
import com.kerem.sta4cadimp.dtos.RawColumnData;
import com.kerem.sta4cadimp.entity.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;


@Service
public class St4FileParser {

    private static final Logger logger = LoggerFactory.getLogger(St4FileParser.class);

    private long floorIdCounter = 1L;
    private long axisIdCounter = 1L;
    private long columnIdCounter = 1L;
    private long beamIdCounter = 1L;
    private long panelIdCounter = 1L;
    private long slabIdCounter = 1L;
    private long foundationSlabIdCounter = 1L;


    private static class RawBeamData {
        String label; double widthCm; double heightCm; String propertyCode; String planeAxisRef;
        String startSpanAxisRef; String endSpanAxisRef; double eccentricityCode;
        double startZOffsetCm; double endZOffsetCm;
        int isPanelFlag;
    }
    private static class RawSlabData {
        String label;
        double thicknessCm;
        List<String> boundaryAxisRefs = new ArrayList<>();
    }

    private static class RawFoundationSlabData {
        String label;
        double thicknessCm;
        double bottomElevationMetre;
        List<String> boundaryAxisRefs = new ArrayList<>();
    }

    private enum Section {
        NONE, HEADER, STORY, AXIS_DATA, COLUMN_AXIS_DATA, COLUMNS_DATA, BEAMS_DATA,
        FLOORS_DATA,
        SLAB_FOUNDATIONS,
        OTHER
    }

    public Project parse(InputStream st4Stream, String originalFilename) throws IOException {
        Project project = new Project();
        project.setId(1L);
        project.setFileName(originalFilename);
        project.setProjectTitle("");

        List<Floor> parsedFloors = new ArrayList<>();
        Map<Integer, Floor> floorMapBySt4Num = new HashMap<>();
        List<Axis> parsedAxes = new ArrayList<>();
        Map<String, Axis> axisMapByLabel = new HashMap<>();
        List<ColumnTypeDefinition> columnTypeDefinitions = new ArrayList<>();
        Map<String, ColumnTypeDefinition> columnTypeMap = new HashMap<>();
        List<RawColumnData> rawColumnDataList = new ArrayList<>();
        List<RawBeamData> rawBeamsList = new ArrayList<>();
        List<RawSlabData> rawSlabsList = new ArrayList<>();
        List<RawFoundationSlabData> rawFoundationSlabsList = new ArrayList<>();

        Section currentSection = Section.NONE;
        int lineCount = 0;
        int xAxesCount = 0;
        int yAxesCount = 0;
        boolean potentialYAxisBlock = false;
        String lastFoundationLabel = null;

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(st4Stream, StandardCharsets.UTF_8))) {
            String line;
            List<String> storyBuffer = new ArrayList<>();

            while ((line = reader.readLine()) != null) {
                lineCount++;
                String trimmedLine = line.trim();

                if (lineCount == 3) {
                    if(trimmedLine.contains("[")) {
                        project.setProjectTitle(trimmedLine.split("\\[")[0].trim());
                    }
                }

                if (trimmedLine.startsWith("/")) {
                    if (trimmedLine.equalsIgnoreCase("/Story/")) currentSection = Section.STORY;
                    else if (trimmedLine.equalsIgnoreCase("/Axis data/")) {
                        currentSection = Section.AXIS_DATA;
                        potentialYAxisBlock = false;
                        xAxesCount = 0;
                        yAxesCount = 0;
                    }
                    else if (trimmedLine.equalsIgnoreCase("/Column axis data/")) currentSection = Section.COLUMN_AXIS_DATA;
                    else if (trimmedLine.equalsIgnoreCase("/Columns Data/")) currentSection = Section.COLUMNS_DATA;
                    else if (trimmedLine.equalsIgnoreCase("/Beams Data/")) currentSection = Section.BEAMS_DATA;
                    else if (trimmedLine.equalsIgnoreCase("/Floors Data/")) currentSection = Section.FLOORS_DATA;
                    else if (trimmedLine.equalsIgnoreCase("/Slab foundations/")) {
                        currentSection = Section.SLAB_FOUNDATIONS;
                        lastFoundationLabel = null;
                    }
                    else currentSection = Section.OTHER;
                    continue;
                }

                try {
                    switch (currentSection) {
                        case STORY:
                            storyBuffer.add(trimmedLine);
                            if (storyBuffer.size() == 3) {
                                Floor floor = new Floor();
                                floor.setId(floorIdCounter++);
                                floor.setName(storyBuffer.get(0));
                                floor.setOriginalNumber(Integer.parseInt(storyBuffer.get(1)));
                                String[] storyData = storyBuffer.get(2).split(",");
                                floor.setElevation(Double.parseDouble(storyData[0]));
                                floor.setHeight(Double.parseDouble(storyData[2]));
                                parsedFloors.add(floor);
                                floorMapBySt4Num.put(floor.getOriginalNumber(), floor);
                                storyBuffer.clear();
                            }
                            break;
                        case AXIS_DATA:
                            if (trimmedLine.isEmpty() || trimmedLine.startsWith(".")) { continue; }
                            String[] axisParts = trimmedLine.split(",");
                            if (axisParts.length >= 2) {
                                Axis axis = new Axis();
                                axis.setId(axisIdCounter++);
                                double coord = Double.parseDouble(axisParts[1]);
                                if (!potentialYAxisBlock && coord == 0 && xAxesCount > 0 && !parsedAxes.isEmpty() && parsedAxes.get(parsedAxes.size()-1).getCoordinate() != 0) {
                                    potentialYAxisBlock = true;
                                }
                                if (!potentialYAxisBlock) {
                                    axis.setType(AxisType.X);
                                    axis.setLabel("X" + xAxesCount++);
                                } else {
                                    axis.setType(AxisType.Y);
                                    axis.setLabel("Y" + yAxesCount++);
                                }
                                axis.setCoordinate(coord);
                                parsedAxes.add(axis);
                                axisMapByLabel.put(axis.getLabel(), axis);
                            }
                            break;
                        case COLUMNS_DATA:
                            if (!trimmedLine.startsWith("0,")) {
                                String[] colTypeParts = trimmedLine.split(",");
                                if (colTypeParts.length >= 3) {
                                    ColumnTypeDefinition typeDef = new ColumnTypeDefinition(
                                            colTypeParts[0].trim(),
                                            Double.parseDouble(colTypeParts[1]),
                                            Double.parseDouble(colTypeParts[2])
                                    );
                                    columnTypeDefinitions.add(typeDef);
                                    columnTypeMap.put(typeDef.typeLabel, typeDef);
                                }
                            }
                            break;
                        case COLUMN_AXIS_DATA:
                            String[] colPlaceParts = trimmedLine.split(",");
                            if (colPlaceParts.length >= 5) {
                                rawColumnDataList.add(new RawColumnData(
                                        Integer.parseInt(colPlaceParts[0]),
                                        colPlaceParts[1].trim(),
                                        colPlaceParts[2].trim(),
                                        Double.parseDouble(colPlaceParts[3]),
                                        Double.parseDouble(colPlaceParts[4])
                                ));
                            }
                            break;
                        case BEAMS_DATA:
                            if (!trimmedLine.startsWith("0,")) {
                                String[] beamParts = trimmedLine.split(",");
                                if (beamParts.length >= 15) {
                                    RawBeamData rawData = new RawBeamData();
                                    rawData.label = beamParts[0].trim();
                                    rawData.widthCm = Double.parseDouble(beamParts[1]);
                                    rawData.heightCm = Double.parseDouble(beamParts[2]);
                                    rawData.propertyCode = beamParts[3].trim();
                                    rawData.planeAxisRef = beamParts[4].trim();
                                    rawData.startSpanAxisRef = beamParts[5].trim();
                                    rawData.endSpanAxisRef = beamParts[6].trim();
                                    rawData.eccentricityCode = Double.parseDouble(beamParts[7]);
                                    rawData.isPanelFlag = Integer.parseInt(beamParts[14].trim());

                                    if (beamParts.length >= 13) {
                                        rawData.startZOffsetCm = Double.parseDouble(beamParts[8]);
                                        rawData.endZOffsetCm = Double.parseDouble(beamParts[12]);
                                    } else {
                                        rawData.startZOffsetCm = 0.0;
                                        rawData.endZOffsetCm = 0.0;
                                    }
                                    rawBeamsList.add(rawData);
                                }
                            }
                            break;
                        case FLOORS_DATA:
                            if (!trimmedLine.startsWith("0,")) {
                                String[] slabParts = trimmedLine.split(",");
                                if (slabParts.length >= 12) {
                                    RawSlabData rawSlab = new RawSlabData();
                                    rawSlab.label = slabParts[0].trim();
                                    rawSlab.thicknessCm = Double.parseDouble(slabParts[1]);
                                    rawSlab.boundaryAxisRefs.add(slabParts[8].trim());
                                    rawSlab.boundaryAxisRefs.add(slabParts[9].trim());
                                    rawSlab.boundaryAxisRefs.add(slabParts[10].trim());
                                    rawSlab.boundaryAxisRefs.add(slabParts[11].trim());
                                    rawSlabsList.add(rawSlab);
                                }
                            }
                            break;
                        case SLAB_FOUNDATIONS:
                            if (trimmedLine.isEmpty()) continue;
                            if (trimmedLine.startsWith("PL")) {
                                lastFoundationLabel = trimmedLine.split("\\s+")[0];
                            }
                            else if (lastFoundationLabel != null && trimmedLine.contains(",")) {
                                String[] parts = trimmedLine.split(",");
                                if (parts.length >= 6) {
                                    RawFoundationSlabData rawFoundation = new RawFoundationSlabData();
                                    rawFoundation.label = lastFoundationLabel;
                                    rawFoundation.thicknessCm = Double.parseDouble(parts[0]);
                                    rawFoundation.boundaryAxisRefs.addAll(Arrays.asList(parts[1], parts[2], parts[3], parts[4]));
                                    rawFoundation.bottomElevationMetre = Double.parseDouble(parts[5]);
                                    rawFoundationSlabsList.add(rawFoundation);
                                }
                                lastFoundationLabel = null;
                            }
                            break;
                        default:
                            break;
                    }
                } catch (Exception e) {
                    logger.error("Satır işlenirken hata oluştu (L:{}): '{}'. Hata: {}", lineCount, trimmedLine, e.getMessage());
                }
            }
        }

        project.getFloors().addAll(parsedFloors);
        for(Floor f : parsedFloors) f.setProject(project);

        project.getAxes().addAll(parsedAxes);
        for(Axis a : parsedAxes) a.setProject(project);

        for (int i = 0; i < rawColumnDataList.size(); i++) {
            RawColumnData rawCol = rawColumnDataList.get(i);
            if (i >= columnTypeDefinitions.size()) { continue; }
            ColumnTypeDefinition baseTypeDef = columnTypeDefinitions.get(i);
            Axis xAxis = getAxisFromSt4Ref(rawCol.sId, axisMapByLabel);
            Axis yAxis = getAxisFromSt4Ref(rawCol.aId, axisMapByLabel);
            if (xAxis == null || yAxis == null) { continue; }

            for (Floor floor : project.getFloors()) {
                if (floor.getOriginalNumber() == 0) continue;
                String targetTypeLabel = floor.getOriginalNumber() + baseTypeDef.typeLabel.substring(1);
                ColumnTypeDefinition targetTypeDef = columnTypeMap.get(targetTypeLabel);

                if (targetTypeDef != null) {
                    StructuralColumn column = new StructuralColumn();
                    column.setId(columnIdCounter++);
                    column.setSt4Sid(rawCol.sId);
                    column.setSt4Aid(rawCol.aId);
                    column.setOffsetXmm(rawCol.offsetXmm);
                    column.setOffsetYmm(rawCol.offsetYmm);
                    column.setTypeLabel(targetTypeDef.typeLabel);
                    column.setDimensionWidthCm(targetTypeDef.widthCm);
                    column.setDimensionHeightCm(targetTypeDef.heightCm);

                    double colWidthMeters = targetTypeDef.widthCm / 100.0;
                    double colHeightMeters = targetTypeDef.heightCm / 100.0;
                    double offsetX_mm = rawCol.offsetXmm;
                    double offsetY_mm = rawCol.offsetYmm;

                    double finalPositionX;
                    if (offsetX_mm == 1.0) { finalPositionX = xAxis.getCoordinate() - (colWidthMeters / 2.0); }
                    else if (offsetX_mm == -1.0) { finalPositionX = xAxis.getCoordinate() + (colWidthMeters / 2.0); }
                    else if (offsetX_mm == 0.0) { finalPositionX = xAxis.getCoordinate(); }
                    else {
                        double offsetX_m = offsetX_mm / 1000.0;
                        if (offsetX_m > 0) { finalPositionX = xAxis.getCoordinate() + offsetX_m - (colWidthMeters / 2.0); }
                        else { finalPositionX = xAxis.getCoordinate() + offsetX_m + (colWidthMeters / 2.0); }
                    }

                    double finalPositionY;
                    if (offsetY_mm == 1.0) { finalPositionY = yAxis.getCoordinate() - (colHeightMeters / 2.0); }
                    else if (offsetY_mm == -1.0) { finalPositionY = yAxis.getCoordinate() + (colHeightMeters / 2.0); }
                    else if (offsetY_mm == 0.0) { finalPositionY = yAxis.getCoordinate(); }
                    else {
                        double offsetY_m = offsetY_mm / 1000.0;
                        if (offsetY_m > 0) { finalPositionY = yAxis.getCoordinate() + offsetY_m - (colHeightMeters / 2.0); }
                        else { finalPositionY = yAxis.getCoordinate() + offsetY_m + (colHeightMeters / 2.0); }
                    }

                    column.setPositionXMetre(finalPositionX);
                    column.setPositionYMetre(finalPositionY);
                    column.setFloor(floor);
                    floor.addStructuralColumn(column);
                    project.addStructuralColumn(column);
                }
            }
        }

        for (RawBeamData rawData : rawBeamsList) {
            Axis planeAxis = getAxisFromSt4Ref(rawData.planeAxisRef, axisMapByLabel);
            Axis startSpanAxis = getAxisFromSt4Ref(rawData.startSpanAxisRef, axisMapByLabel);
            Axis endSpanAxis = getAxisFromSt4Ref(rawData.endSpanAxisRef, axisMapByLabel);

            Floor targetFloor = null;
            if (rawData.label != null && !rawData.label.isEmpty()) {
                try {
                    int floorNum = Integer.parseInt(rawData.label.substring(0, 1));
                    targetFloor = floorMapBySt4Num.get(floorNum);
                } catch (Exception e) {
                    logger.warn("Etiket '{}' kat numarası için ayrıştırılamadı.", rawData.label);
                }
            }

            if (rawData.isPanelFlag == 1) {
                Panel panel = new Panel();
                panel.setId(panelIdCounter++); // GÜNCELLEME: ID atandı
                panel.setLabel(rawData.label);
                panel.setWidthCm(rawData.widthCm);
                panel.setHeightCm(rawData.heightCm);
                panel.setPropertyCode(rawData.propertyCode);
                panel.setStartZOffsetCm(rawData.startZOffsetCm);
                panel.setEndZOffsetCm(rawData.endZOffsetCm);

                if (planeAxis != null && startSpanAxis != null && endSpanAxis != null) {
                    double panelThicknessMeters = panel.getWidthCm() / 100.0;
                    if (planeAxis.getType() == AxisType.Y) {
                        double finalY = calculateFinalCoordinate(planeAxis.getCoordinate(), panelThicknessMeters, rawData.eccentricityCode);
                        panel.setStartXMetre(startSpanAxis.getCoordinate());
                        panel.setStartYMetre(finalY);
                        panel.setEndXMetre(endSpanAxis.getCoordinate());
                        panel.setEndYMetre(finalY);
                    } else if (planeAxis.getType() == AxisType.X) {
                        double finalX = calculateFinalCoordinate(planeAxis.getCoordinate(), panelThicknessMeters, rawData.eccentricityCode);
                        panel.setStartXMetre(finalX);
                        panel.setStartYMetre(startSpanAxis.getCoordinate());
                        panel.setEndXMetre(finalX);
                        panel.setEndYMetre(endSpanAxis.getCoordinate());
                    }
                }

                project.addPanel(panel);
                if (targetFloor != null) {
                    targetFloor.addPanel(panel);
                }

            }
            else {
                Beam beam = new Beam();
                beam.setId(beamIdCounter++);
                beam.setLabel(rawData.label);
                beam.setWidthCm(rawData.widthCm);
                beam.setHeightCm(rawData.heightCm);
                beam.setPropertyCode(rawData.propertyCode);
                beam.setStartZOffsetCm(rawData.startZOffsetCm);
                beam.setEndZOffsetCm(rawData.endZOffsetCm);

                if (planeAxis != null && startSpanAxis != null && endSpanAxis != null) {
                    double beamWidthMeters = beam.getWidthCm() / 100.0;
                    if (planeAxis.getType() == AxisType.Y) {
                        double finalY = calculateFinalCoordinate(planeAxis.getCoordinate(), beamWidthMeters, rawData.eccentricityCode);
                        beam.setStartXMetre(startSpanAxis.getCoordinate());
                        beam.setStartYMetre(finalY);
                        beam.setEndXMetre(endSpanAxis.getCoordinate());
                        beam.setEndYMetre(finalY);
                    } else if (planeAxis.getType() == AxisType.X) {
                        double finalX = calculateFinalCoordinate(planeAxis.getCoordinate(), beamWidthMeters, rawData.eccentricityCode);
                        beam.setStartXMetre(finalX);
                        beam.setStartYMetre(startSpanAxis.getCoordinate());
                        beam.setEndXMetre(finalX);
                        beam.setEndYMetre(endSpanAxis.getCoordinate());
                    }
                }
                project.addBeam(beam);
                if (targetFloor != null) {
                    targetFloor.addBeam(beam);
                }
            }
        }

        for (RawSlabData rawSlab : rawSlabsList) {
            Slab slab = new Slab();
            slab.setId(slabIdCounter++);
            slab.setLabel(rawSlab.label);
            slab.setThicknessCm(rawSlab.thicknessCm);
            slab.setBoundaryAxisRefs(rawSlab.boundaryAxisRefs);

            Floor targetFloor = null;
            if (rawSlab.label != null && !rawSlab.label.isEmpty()) {
                try {
                    int floorNum = Integer.parseInt(rawSlab.label.substring(0, 1));
                    targetFloor = floorMapBySt4Num.get(floorNum);
                } catch (Exception e) {
                    logger.warn("Döşeme etiketi '{}' kat numarası için ayrıştırılamadı.", rawSlab.label);
                }
            }

            if (targetFloor != null) {
                targetFloor.addSlab(slab);
            } else {
                logger.warn("Döşeme '{}' için uygun kat bulunamadı.", rawSlab.label);
            }
        }

        for (RawFoundationSlabData rawFoundation : rawFoundationSlabsList) {
            FoundationSlab foundationSlab = new FoundationSlab();
            foundationSlab.setId(foundationSlabIdCounter++);
            foundationSlab.setLabel(rawFoundation.label);
            foundationSlab.setThicknessCm(rawFoundation.thicknessCm);

            double topElevation = rawFoundation.bottomElevationMetre + (rawFoundation.thicknessCm / 100.0);
            foundationSlab.setElevationMetre(topElevation);

            foundationSlab.setBoundaryAxisRefs(rawFoundation.boundaryAxisRefs);
            project.addFoundationSlab(foundationSlab);
        }

        return project;
    }

    private double calculateFinalCoordinate(double axisCoordinate, double widthInMeters, double eccentricityCode) {
        if (eccentricityCode == 1.0) {
            return axisCoordinate - (widthInMeters / 2.0);
        } else if (eccentricityCode == -1.0) {
            return axisCoordinate + (widthInMeters / 2.0);
        }
        return axisCoordinate;
    }

    private Axis getAxisFromSt4Ref(String st4Ref, Map<String, Axis> axisMapByLabel) {
        if (st4Ref == null || st4Ref.trim().isEmpty() || st4Ref.length() < 2) { return null; }
        try {
            char typeChar = st4Ref.charAt(0);
            int index = Integer.parseInt(st4Ref.substring(1));
            String programmaticLabelPrefix = (typeChar == '1') ? "X" : (typeChar == '2') ? "Y" : null;
            if (programmaticLabelPrefix == null) { return null; }
            String finalLabel = programmaticLabelPrefix + (index - 1);
            return axisMapByLabel.get(finalLabel);
        } catch (Exception e) { return null; }
    }
}