package org.uma.jmetal.problem.multiobjective.dfsp.decoder;

import org.uma.jmetal.problem.multiobjective.dfsp.model.DhhfspFourVectorSolution;
import org.uma.jmetal.problem.multiobjective.dfsp.model.DhhfspInstance;

public interface DhhfspDecoder {
  DecodeResult decode(
      DhhfspInstance instance, DhhfspFourVectorSolution solution, DecodeOptions options);
}
