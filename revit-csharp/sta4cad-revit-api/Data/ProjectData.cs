using Newtonsoft.Json;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace sta4cad_revit_api.Data
{
    public class ProjectData
    {
        [JsonProperty("floors")]
        public List<LevelData> Levels { get; set; } = new List<LevelData>();

        [JsonProperty("axes")]
        public List<AxisData> Axes { get; set; } = new List<AxisData>();

        [JsonProperty("foundationSlabs")]
        public List<FoundationSlabData> FoundationSlabs { get; set; } = new List<FoundationSlabData>();
    }
}
