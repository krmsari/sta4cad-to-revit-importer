using Newtonsoft.Json;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace sta4cad_revit_api.Data
{
    public class ColumnData
    {
        [JsonProperty("dimensionWidthCm")]
        public double DimensionWidthCm { get; set; }

        [JsonProperty("dimensionHeightCm")]
        public double DimensionHeightCm { get; set; }

        [JsonProperty("positionXMetre")]
        public double PositionXMetre { get; set; }

        [JsonProperty("positionYMetre")]
        public double PositionYMetre { get; set; }
    }
}
