//Copyright (c) 2021, Dan Rosher
//    All rights reserved.
//
//    This source code is licensed under the BSD-style license found in the
//    LICENSE file in the root directory of this source tree.

package com.github.danrosher.solr.search.decayfunction;

public class GaussDecayFunctionValueSourceParser extends DecayFunctionValueSourceParser {
    @Override
    DecayStrategy getDecayStrategy() {
        return new GaussDecay();
    }

    @Override
    String name() {
        return "gauss";
    }
}

final class GaussDecay implements DecayStrategy {

    @Override
    public double scale(double scale, double decay) {
        return 0.5 * Math.pow(scale, 2.0) / Math.log(decay);
    }

    @Override
    public double calculate(double value, double scale) {
        // note that we already computed scale^2 in processScale() so we do
        // not need to square it here.
        return Math.exp(0.5 * Math.pow(value, 2.0) / scale);
    }

    @Override
    public String explain(double scale) {
        return "exp(-0.5*pow(<val>,2.0)/" + -1 * scale + ")";
    }
}
