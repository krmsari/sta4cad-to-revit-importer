using Newtonsoft.Json;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace sta4cad_revit_api.Data
{
    public class BeamData
    {
        [JsonProperty("widthCm")]
        public double WidthCm { get; set; }

        [JsonProperty("heightCm")]
        public double HeightCm { get; set; }

        [JsonProperty("startXMetre")]
        public double StartXMetre { get; set; }

        [JsonProperty("startYMetre")]
        public double StartYMetre { get; set; }

        [JsonProperty("endXMetre")]
        public double EndXMetre { get; set; }

        [JsonProperty("endYMetre")]
        public double EndYMetre { get; set; }

        [JsonProperty("startZOffsetCm")]
        public double StartZOffsetCm { get; set; }

        [JsonProperty("endZOffsetCm")]
        public double EndZOffsetCm { get; set; }
    }
}
