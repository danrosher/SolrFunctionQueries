//Copyright (c) 2021, Dan Rosher
//    All rights reserved.
//
//    This source code is licensed under the BSD-style license found in the
//    LICENSE file in the root directory of this source tree.

package com.github.danrosher.solr.search.decayfunction;

public enum DistanceUnit  {
    INCH(0.0254, "in", "inch"),
    YARD(0.9144, "yd", "yards"),
    FEET(0.3048, "ft", "feet"),
    KILOMETERS(1000.0, "km", "kilometers"),
    NAUTICALMILES(1852.0, "NM", "nmi", "nauticalmiles"),
    MILLIMETERS(0.001, "mm", "millimeters"),
    CENTIMETERS(0.01, "cm", "centimeters"),

    // 'm' is a suffix of 'nmi' so it must follow 'nmi'
    MILES(1609.344, "mi", "miles"),

    // since 'm' is suffix of other unit
    // it must be the last entry of unit
    // names ending with 'm'. otherwise
    // parsing would fail
    METERS(1, "m", "meters");

    public static final DistanceUnit DEFAULT = METERS;

    private final double meters;
    private final String[] names;

    DistanceUnit(double meters, String...names) {
        this.meters = meters;
        this.names = names;
    }


    /**
     * Convert a value to a distance string
     *
     * @param distance value to convert
     * @return String representation of the distance
     */
    public String toString(double distance) {
        return distance + toString();
    }

    @Override
    public String toString() {
        return names[0];
    }

    /**
     * Converts the given distance from the given DistanceUnit, to the given DistanceUnit
     *
     * @param distance Distance to convert
     * @param from     Unit to convert the distance from
     * @param to       Unit of distance to convert to
     * @return Given distance converted to the distance in the given unit
     */
    public static double convert(double distance, DistanceUnit from, DistanceUnit to) {
        if (from == to) {
            return distance;
        } else {
            return distance * from.meters / to.meters;
        }
    }

    /**
     * Parses a given distance and converts it to the specified unit.
     *
     * @param distance String defining a distance (value and unit)
     * @param defaultUnit unit assumed if none is defined
     * @param to unit of result
     * @return parsed distance
     */
    public static double parse(String distance, DistanceUnit defaultUnit, DistanceUnit to) {
        Distance dist = Distance.parseDistance(distance, defaultUnit);
        return convert(dist.value, dist.unit, to);
    }

    /**
     * Parses a given distance and converts it to this unit.
     *
     * @param distance String defining a distance (value and unit)
     * @param defaultUnit unit to expect if none if provided
     * @return parsed distance
     */
    public double parse(String distance, DistanceUnit defaultUnit) {
        return parse(distance, defaultUnit, this);
    }


    /**
     * This class implements a value+unit tuple.
     */
    public static class Distance implements Comparable<Distance> {
        public final double value;
        public final DistanceUnit unit;

        public Distance(double value, DistanceUnit unit) {
            super();
            this.value = value;
            this.unit = unit;
        }

        @Override
        public boolean equals(Object obj) {
            if(obj == null) {
                return false;
            } else if (obj instanceof Distance) {
                Distance other = (Distance) obj;
                return DistanceUnit.convert(value, unit, other.unit) == other.value;
            } else {
                return false;
            }
        }

        @Override
        public int hashCode() {
            return Double.valueOf(value * unit.meters).hashCode();
        }

        @Override
        public int compareTo(Distance o) {
            return Double.compare(value, DistanceUnit.convert(o.value, o.unit, unit));
        }

        @Override
        public String toString() {
            return unit.toString(value);
        }

        /**
         * Parse a {@link Distance} from a given String
         *
         * @param distance String defining a {@link Distance}
         * @param defaultUnit {@link DistanceUnit} to be assumed
         *          if not unit is provided in the first argument
         * @return parsed {@link Distance}
         */
        private static Distance parseDistance(String distance, DistanceUnit defaultUnit) {
            for (DistanceUnit unit : values()) {
                for (String name : unit.names) {
                    if(distance.endsWith(name)) {
                        return new Distance(Double.parseDouble(distance.substring(0, distance.length() - name.length())), unit);
                    }
                }
            }
            return new Distance(Double.parseDouble(distance), defaultUnit);
        }
    }


}

