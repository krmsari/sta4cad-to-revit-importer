using Newtonsoft.Json;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace sta4cad_revit_api.Data
{
    public class LevelData
    {
        [JsonProperty("id")]
        public int Id { get; set; }

        [JsonProperty("elevation")]
        public double ElevationMetre { get; set; }

        [JsonProperty("columns")]
        public List<ColumnData> Columns { get; set; } = new List<ColumnData>();

        [JsonProperty("beams")]
        public List<BeamData> Beams { get; set; } = new List<BeamData>();

        [JsonProperty("slabs")]
        public List<SlabData> Slabs { get; set; } = new List<SlabData>();

        [JsonProperty("panels")]
        public List<PanelData> Panels { get; set; } = new List<PanelData>();
    }
}
