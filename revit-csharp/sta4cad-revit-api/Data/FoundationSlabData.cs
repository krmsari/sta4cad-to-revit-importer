using Newtonsoft.Json;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace sta4cad_revit_api.Data
{
    public class FoundationSlabData
    {
        [JsonProperty("label")]
        public string Label { get; set; }

        [JsonProperty("thicknessCm")]
        public double ThicknessCm { get; set; }

        [JsonProperty("elevationMetre")]
        public double ElevationMetre { get; set; }

        [JsonProperty("boundaryAxisRefs")]
        public List<string> BoundaryAxisRefs { get; set; } = new List<string>();
    }
}
