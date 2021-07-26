package com.github.danrosher.solr.search.decayfunction;

public interface DecayStrategy {
    double scale(double scale, double decay);
    double calculate(double value, double scale);
    String explain( double scale);
}
