namespace v01.Utils
{
    public static class UnitConverter
    {
        private const double METER_TO_FEET = 3.28084;
        private const double CM_TO_FEET = 0.0328084;

        public static double MetersToFeet(double meters) => meters * METER_TO_FEET;

        public static double CmToFeet(double cm) => cm * CM_TO_FEET;
    }
}
