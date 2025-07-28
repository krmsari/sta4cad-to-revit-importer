using Autodesk.Revit.DB;
using Autodesk.Revit.DB.Structure;
using Autodesk.Revit.UI;
using sta4cad_revit_api.Data;
using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using v01.Utils;

namespace v01.Services
{
    public class RevitModelBuilder
    {
        private readonly Document _doc;
        private readonly Dictionary<int, Level> _createdLevels = new Dictionary<int, Level>();
        private readonly Dictionary<string, Grid> _createdGrids = new Dictionary<string, Grid>();
        private List<Level> _sortedLevels;

        public RevitModelBuilder(Document doc)
        {
            _doc = doc;
        }

        public void Build(ProjectData projectData)
        {
            CreateLevelsAndGrids(projectData.Levels, projectData.Axes);
            CreateColumns(projectData.Levels);
            CreateBeams(projectData.Levels);
            CreatePanelsAsBeams(projectData.Levels);
            CreateSlabs(projectData.Levels, projectData.Axes);
            CreateFoundationSlabs(projectData.FoundationSlabs, projectData.Axes);
        }


        private void CreateLevelsAndGrids(List<LevelData> levelDataList, List<AxisData> axisDataList)
        {
            using (Transaction t = new Transaction(_doc, "Seviyeleri ve Aksları Oluştur"))
            {
                t.Start();
                // kat seviyelerini  oluştur
                foreach (var levelData in levelDataList)
                {
                    double elevationInFeet = UnitConverter.MetersToFeet(levelData.ElevationMetre);
                    Level newLevel = Level.Create(_doc, elevationInFeet);
                    newLevel.Name = $"Kat Seviyesi {levelData.Id} ({levelData.ElevationMetre}m)";
                    _createdLevels.Add(levelData.Id, newLevel);
                }
                _sortedLevels = _createdLevels.Values.OrderBy(l => l.Elevation).ToList();

                double staMinX = axisDataList.Any(a => a.Type == "X") ? axisDataList.Where(a => a.Type == "X").Min(a => a.CoordinateMetre) - 5 : -5;
                double staMaxX = axisDataList.Any(a => a.Type == "X") ? axisDataList.Where(a => a.Type == "X").Max(a => a.CoordinateMetre) + 5 : 5;
                double staMinY = axisDataList.Any(a => a.Type == "Y") ? axisDataList.Where(a => a.Type == "Y").Min(a => a.CoordinateMetre) - 5 : -5;
                double staMaxY = axisDataList.Any(a => a.Type == "Y") ? axisDataList.Where(a => a.Type == "Y").Max(a => a.CoordinateMetre) + 5 : 5;

                foreach (var axisData in axisDataList)
                {
                    XYZ start, end;
                    if (axisData.Type == "X")
                    {
                        double xCoord = UnitConverter.MetersToFeet(axisData.CoordinateMetre);
                        start = new XYZ(xCoord, UnitConverter.MetersToFeet(-staMaxY), 0);
                        end = new XYZ(xCoord, UnitConverter.MetersToFeet(-staMinY), 0);
                    }
                    else // Y Aksı
                    {
                        double yCoord = UnitConverter.MetersToFeet(-axisData.CoordinateMetre);
                        start = new XYZ(UnitConverter.MetersToFeet(staMinX), yCoord, 0);
                        end = new XYZ(UnitConverter.MetersToFeet(staMaxX), yCoord, 0);
                    }
                    Grid newGrid = Grid.Create(_doc, Line.CreateBound(start, end));
                    if (newGrid != null && axisData.Label != null)
                    {
                        newGrid.Name = axisData.Label;
                        _createdGrids.Add(axisData.Label, newGrid);
                    }
                }
                t.Commit();
            }
        }

        private void CreateColumns(List<LevelData> levelDataList)
        {
            using (Transaction t = new Transaction(_doc, "Yapısal Kolonları Oluştur"))
            {
                t.Start();
                foreach (var levelData in levelDataList)
                {
                    if (!_createdLevels.ContainsKey(levelData.Id)) continue;
                    Level topLevel = _createdLevels[levelData.Id];
                    int topLevelIndex = _sortedLevels.FindIndex(l => l.Id == topLevel.Id);
                    if (topLevelIndex <= 0) continue;
                    Level baseLevel = _sortedLevels[topLevelIndex - 1];

                    foreach (var colData in levelData.Columns)
                    {
                        FamilySymbol columnType = GetOrCreateStructuralType($"{colData.DimensionWidthCm}x{colData.DimensionHeightCm} Kolon", colData.DimensionWidthCm, colData.DimensionHeightCm, BuiltInCategory.OST_StructuralColumns, new List<string> { "Concrete", "Rectangular", "Column" }, "Concrete-Rectangular-Column.rfa");
                        if (columnType == null) throw new Exception("Gerekli kolon ailesi bulunamadı veya oluşturulamadı.");
                        if (!columnType.IsActive) { columnType.Activate(); _doc.Regenerate(); }

                        XYZ locationPoint = new XYZ(UnitConverter.MetersToFeet(colData.PositionXMetre), UnitConverter.MetersToFeet(-colData.PositionYMetre), baseLevel.Elevation);
                        FamilyInstance newColumn = _doc.Create.NewFamilyInstance(locationPoint, columnType, baseLevel, StructuralType.Column);

                        newColumn.get_Parameter(BuiltInParameter.FAMILY_BASE_LEVEL_PARAM).Set(baseLevel.Id);
                        newColumn.get_Parameter(BuiltInParameter.FAMILY_TOP_LEVEL_PARAM).Set(topLevel.Id);
                        newColumn.get_Parameter(BuiltInParameter.SCHEDULE_TOP_LEVEL_OFFSET_PARAM).Set(0);
                        newColumn.get_Parameter(BuiltInParameter.SCHEDULE_BASE_LEVEL_OFFSET_PARAM).Set(0);
                    }
                }
                t.Commit();
            }
        }

        private void CreateBeams(List<LevelData> levelDataList)
        {
            using (Transaction t = new Transaction(_doc, "Yapısal Kirişleri Oluştur"))
            {
                t.Start();
                foreach (var levelData in levelDataList)
                {
                    if (!_createdLevels.ContainsKey(levelData.Id)) continue;
                    Level referenceLevel = _createdLevels[levelData.Id];

                    foreach (var beamData in levelData.Beams)
                    {
                        FamilySymbol beamType = GetOrCreateStructuralType($"{beamData.WidthCm}x{beamData.HeightCm} Kiriş", beamData.WidthCm, beamData.HeightCm, BuiltInCategory.OST_StructuralFraming, new List<string> { "Concrete", "Rectangular", "Beam" }, "Concrete-Rectangular Beam.rfa");
                        if (beamType == null) throw new Exception("Gerekli kiriş ailesi bulunamadı veya oluşturulamadı.");
                        if (!beamType.IsActive) { beamType.Activate(); _doc.Regenerate(); }

                        XYZ startPoint = new XYZ(UnitConverter.MetersToFeet(beamData.StartXMetre), UnitConverter.MetersToFeet(-beamData.StartYMetre), referenceLevel.Elevation + UnitConverter.CmToFeet(beamData.StartZOffsetCm));
                        XYZ endPoint = new XYZ(UnitConverter.MetersToFeet(beamData.EndXMetre), UnitConverter.MetersToFeet(-beamData.EndYMetre), referenceLevel.Elevation + UnitConverter.CmToFeet(beamData.EndZOffsetCm));
                        _doc.Create.NewFamilyInstance(Line.CreateBound(startPoint, endPoint), beamType, referenceLevel, StructuralType.Beam);
                    }
                }
                t.Commit();
            }
        }

        private void CreatePanelsAsBeams(List<LevelData> levelDataList)
        {
            using (Transaction t = new Transaction(_doc, "Yapısal Panelleri (Kiriş Olarak) Oluştur"))
            {
                t.Start();
                foreach (var levelData in levelDataList)
                {
                    if (!_createdLevels.ContainsKey(levelData.Id)) continue;
                    Level referenceLevel = _createdLevels[levelData.Id];

                    foreach (var panelData in levelData.Panels)
                    {
                        string typeName = $"Panel Kirişi {panelData.WidthCm}x{panelData.HeightCm}";
                        FamilySymbol panelAsBeamType = GetOrCreateStructuralType(typeName, panelData.WidthCm, panelData.HeightCm, BuiltInCategory.OST_StructuralFraming, new List<string> { "Concrete", "Rectangular", "Beam" }, "Concrete-Rectangular Beam.rfa");
                        if (panelAsBeamType == null)
                        {
                            TaskDialog.Show("Uyarı", $"Panel için kiriş tipi '{typeName}' oluşturulamadı veya bulunamadı. Bu panel atlanacak.");
                            continue;
                        }
                        if (!panelAsBeamType.IsActive) { panelAsBeamType.Activate(); _doc.Regenerate(); }

                        XYZ startPoint = new XYZ(UnitConverter.MetersToFeet(panelData.StartXMetre), UnitConverter.MetersToFeet(-panelData.StartYMetre), referenceLevel.Elevation + UnitConverter.CmToFeet(panelData.StartZOffsetCm));
                        XYZ endPoint = new XYZ(UnitConverter.MetersToFeet(panelData.EndXMetre), UnitConverter.MetersToFeet(-panelData.EndYMetre), referenceLevel.Elevation + UnitConverter.CmToFeet(panelData.EndZOffsetCm));
                        _doc.Create.NewFamilyInstance(Line.CreateBound(startPoint, endPoint), panelAsBeamType, referenceLevel, StructuralType.Beam);
                    }
                }
                t.Commit();
            }
        }

        private void CreateSlabs(List<LevelData> levelDataList, List<AxisData> axisDataList)
        {
            using (Transaction t = new Transaction(_doc, "Kat Döşemelerini Oluştur"))
            {
                t.Start();
                var axisDict = axisDataList.ToDictionary(a => a.Label, a => a);
                foreach (var levelData in levelDataList)
                {
                    if (!_createdLevels.ContainsKey(levelData.Id)) continue;
                    Level referenceLevel = _createdLevels[levelData.Id];
                    foreach (var slabData in levelData.Slabs)
                    {
                        CreateRevitSlab(slabData, referenceLevel, axisDict);
                    }
                }
                t.Commit();
            }
        }

        private void CreateFoundationSlabs(List<FoundationSlabData> foundationSlabs, List<AxisData> axisDataList)
        {
            using (Transaction t = new Transaction(_doc, "Radye Temelleri Oluştur"))
            {
                t.Start();
                var axisDict = axisDataList.ToDictionary(a => a.Label, a => a);
                Level baseLevel = _sortedLevels.FirstOrDefault();
                if (baseLevel != null && foundationSlabs != null)
                {
                    foreach (var foundationData in foundationSlabs)
                    {
                        CreateRevitFoundationSlab(foundationData, baseLevel, axisDict);
                    }
                }
                t.Commit();
            }
        }


        private void CreateRevitSlab(SlabData slabData, Level level, Dictionary<string, AxisData> axisDict)
        {
            FloorType floorType = GetOrCreateFloorSlabType(slabData.ThicknessCm);
            if (floorType == null) return;

            CurveLoop profile = CreateRectangularProfile(slabData.BoundaryAxisRefs, axisDict);
            if (profile == null) return;

            Floor.Create(_doc, new List<CurveLoop> { profile }, floorType.Id, level.Id);
        }

        private void CreateRevitFoundationSlab(FoundationSlabData foundationData, Level baseLevel, Dictionary<string, AxisData> axisDict)
        {
            FloorType foundationType = GetOrCreateFoundationSlabType(foundationData.ThicknessCm);
            if (foundationType == null) return;

            CurveLoop profile = CreateRectangularProfile(foundationData.BoundaryAxisRefs, axisDict);
            if (profile == null) return;

            Floor.Create(_doc, new List<CurveLoop> { profile }, foundationType.Id, baseLevel.Id);
        }

        private CurveLoop CreateRectangularProfile(List<string> boundaryAxisRefs, Dictionary<string, AxisData> axisDict)
        {
            if (boundaryAxisRefs.Count < 4) return null;

            var revitGridNames = boundaryAxisRefs.Select(GetRevitGridNameFromSt4Ref).ToList();
            var xGridNames = revitGridNames.Where(name => !string.IsNullOrEmpty(name) && name.StartsWith("X")).Distinct().ToList();
            var yGridNames = revitGridNames.Where(name => !string.IsNullOrEmpty(name) && name.StartsWith("Y")).Distinct().ToList();

            if (xGridNames.Count < 2 || yGridNames.Count < 2) return null;

            double minX = axisDict[xGridNames[0]].CoordinateMetre;
            double maxX = axisDict[xGridNames[1]].CoordinateMetre;
            double minY = axisDict[yGridNames[0]].CoordinateMetre;
            double maxY = axisDict[yGridNames[1]].CoordinateMetre;

            if (minX > maxX) { var temp = minX; minX = maxX; maxX = temp; }
            if (minY > maxY) { var temp = minY; minY = maxY; maxY = temp; }

            XYZ p1 = new XYZ(UnitConverter.MetersToFeet(minX), UnitConverter.MetersToFeet(-minY), 0);
            XYZ p2 = new XYZ(UnitConverter.MetersToFeet(maxX), UnitConverter.MetersToFeet(-minY), 0);
            XYZ p3 = new XYZ(UnitConverter.MetersToFeet(maxX), UnitConverter.MetersToFeet(-maxY), 0);
            XYZ p4 = new XYZ(UnitConverter.MetersToFeet(minX), UnitConverter.MetersToFeet(-maxY), 0);

            CurveLoop profile = new CurveLoop();
            profile.Append(Line.CreateBound(p1, p2));
            profile.Append(Line.CreateBound(p2, p3));
            profile.Append(Line.CreateBound(p3, p4));
            profile.Append(Line.CreateBound(p4, p1));

            return profile;
        }

        private string GetRevitGridNameFromSt4Ref(string st4Ref)
        {
            if (string.IsNullOrEmpty(st4Ref) || st4Ref.Length < 2) return string.Empty;
            try
            {
                char typeChar = st4Ref[0];
                int index = int.Parse(st4Ref.Substring(1));
                string prefix = (typeChar == '1') ? "X" : (typeChar == '2') ? "Y" : "";
                return $"{prefix}{index - 1}";
            }
            catch { return string.Empty; }
        }

        private FloorType GetOrCreateFloorSlabType(double thicknessCm)
        {
            string typeName = $"Generic Slab {thicknessCm}cm";
            var collector = new FilteredElementCollector(_doc).OfClass(typeof(FloorType));
            FloorType floorType = collector.FirstOrDefault(ft => ft.Name == typeName) as FloorType;
            if (floorType != null) return floorType;

            FloorType genericType = collector.Cast<FloorType>().FirstOrDefault(ft => !ft.IsFoundationSlab);
            if (genericType == null)
            {
                TaskDialog.Show("Hata", "Projede uygun bir standart döşeme tipi bulunamadı.");
                return null;
            }

            floorType = genericType.Duplicate(typeName) as FloorType;
            if (floorType == null) return null;

            CompoundStructure cs = floorType.GetCompoundStructure();
            if (cs.LayerCount > 0)
            {
                cs.SetLayerFunction(0, MaterialFunctionAssignment.Structure);
                cs.SetLayerWidth(0, UnitConverter.CmToFeet(thicknessCm));
            }
            floorType.SetCompoundStructure(cs);
            return floorType;
        }

        private FloorType GetOrCreateFoundationSlabType(double thicknessCm)
        {
            string typeName = $"Foundation Slab {thicknessCm}cm";
            var collector = new FilteredElementCollector(_doc).OfClass(typeof(FloorType));
            FloorType existingType = collector.FirstOrDefault(ft => ft.Name == typeName) as FloorType;
            if (existingType != null) return existingType;

            FloorType template = collector.Cast<FloorType>().FirstOrDefault(ft => ft.IsFoundationSlab);
            if (template == null)
            {
                TaskDialog.Show("Hata", "Projede uygun bir radye temel tipi (Foundation Slab) bulunamadı. Lütfen önce bir tane oluşturun veya yükleyin.");
                return null;
            }

            FloorType newType = template.Duplicate(typeName) as FloorType;
            if (newType == null) return null;

            CompoundStructure cs = newType.GetCompoundStructure();
            if (cs.LayerCount > 0)
            {
                cs.SetLayerWidth(0, UnitConverter.CmToFeet(thicknessCm));
            }
            newType.SetCompoundStructure(cs);
            return newType;
        }

        private FamilySymbol GetOrCreateStructuralType(string typeName, double widthCm, double heightCm, BuiltInCategory category, List<string> keywords, string familyFileName)
        {
            var collector = new FilteredElementCollector(_doc).OfClass(typeof(FamilySymbol)).OfCategory(category);
            FamilySymbol symbol = collector.Cast<FamilySymbol>().FirstOrDefault(s => s.Name == typeName);
            if (symbol != null) return symbol;

            FamilySymbol templateSymbol = collector.Cast<FamilySymbol>().FirstOrDefault(s => keywords.All(key => s.Family.Name.ToLower().Contains(key.ToLower())));

            if (templateSymbol == null)
            {
                string familyPathCategory = category == BuiltInCategory.OST_StructuralColumns ? "Structural Columns" : "Structural Framing";
                string[] libraryPaths = {
                    Path.Combine(_doc.Application.GetLibraryPaths()["Imperial Library"], familyPathCategory, "Concrete", familyFileName),
                    $@"C:\ProgramData\Autodesk\RVT {_doc.Application.VersionNumber}\Libraries\English-Imperial\US\{familyPathCategory}\Concrete\{familyFileName}"
                };

                string foundPath = libraryPaths.FirstOrDefault(p => File.Exists(p));

                if (foundPath != null)
                {
                    if (_doc.LoadFamily(foundPath, out Family loadedFamily))
                    {
                        ISet<ElementId> symbolsIds = loadedFamily.GetFamilySymbolIds();
                        if (symbolsIds.Any())
                        {
                            templateSymbol = _doc.GetElement(symbolsIds.First()) as FamilySymbol;
                        }
                    }
                }
            }

            if (templateSymbol == null)
            {
                TaskDialog.Show("Kritik Hata", $"Gerekli yapısal aile ('{familyFileName}') projenizde yüklü değil ve standart kütüphane yollarında bulunamadı.\n\nLütfen aileyi manuel olarak yükleyip komutu tekrar çalıştırın.");
                return null;
            }

            symbol = templateSymbol.Duplicate(typeName) as FamilySymbol;
            if (symbol == null) return null;

            symbol.LookupParameter("b")?.Set(UnitConverter.CmToFeet(widthCm));
            symbol.LookupParameter("h")?.Set(UnitConverter.CmToFeet(heightCm));
            return symbol;
        }

    }
}
