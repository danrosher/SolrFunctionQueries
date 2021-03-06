//Copyright (c) 2021, Dan Rosher
//    All rights reserved.
//
//    This source code is licensed under the BSD-style license found in the
//    LICENSE file in the root directory of this source tree.

package com.github.danrosher.solr.search.decayfunction;

public class ExponentialDecayFunctionValueSourceParser extends DecayFunctionValueSourceParser {
    @Override
    DecayStrategy getDecayStrategy() {
        return new ExponentialDecay();
    }

    @Override
    String name() {
        return "exp";
    }
}

final class ExponentialDecay implements DecayStrategy {

    @Override
    public double scale(double scale, double decay) {
        return Math.log(decay) / scale;
    }

    @Override
    public double calculate(double value, double scale) {
        return Math.exp(scale * value);
    }

    @Override
    public String explain(double scale) {
        return "exp(- <val> * " + -1 * scale + ")";
    }
}
