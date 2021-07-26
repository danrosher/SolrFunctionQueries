//Copyright (c) 2021, Dan Rosher
//    All rights reserved.
//
//    This source code is licensed under the BSD-style license found in the
//    LICENSE file in the root directory of this source tree.

package com.github.danrosher.solr.search.decayfunction;

public class LinearDecayFunctionValueSourceParser extends DecayFunctionValueSourceParser {

    @Override
    DecayStrategy getDecayStrategy() {
        return new LinearDecay();
    }

    @Override
    String name() {
        return "linear";
    }
}

final class LinearDecay implements DecayStrategy {

    @Override
    public double scale(double scale, double decay) {
        return scale / (1.0 - decay);
    }

    @Override
    public double calculate(double value, double scale) {
        return Math.max(0.0, (scale - value) / scale);
    }

    @Override
    public String explain(double scale) {
        return "max(0.0, ((" + scale + " - <val>)/" + scale + ")";
    }
}
