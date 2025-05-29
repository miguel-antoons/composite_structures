# Modeling and Solving Composite Structure Design with Constraint Programming

This repository contains all the models that are presented in the master thesis named "Modeling and Solving Composite Structure Design with Constraint Programming" performed at UCLouvain.

## Rules

### Mandatory

- **B1**: Blending Constraint.
- **B2**: No Ply Crossing Constraint.
- **B3**: Continuous Surface Plies Constraint.
- **B4**: A maximum drop-off of 3 consecutive layers is allowed between 2 sequences.
- **D1bis**: A Minimum of 8% of plies of every angle must be present in every sequence.

### Optional

- **D1**: A given number of plies must be present in every sequence (specified by user or minimum 8% (**D1bis**)).
- **D2**: Angle difference cannot be equal to 90°.
- **D3**: Maximum 3 consecutive plies with the same angle.
- **D4**: Vertical Symmetry Constraint with the exception of a dysymmetry in th middle 2 or 3 plies in an even or odd thickness respectively.
- **D5**: Surface plies can only take -45°/45° angle.


## MiniZinc

This directory contains all the models that were created in the MiniZinc language.
In general, the models ending with `_chuffed` are the ones used for the Chuffed solver, while the others are used for the CP-SAT and the Gecode solvers.

### atMostSeqCard

[This directory](minizinc/atMostSeqCard) contains all the models used for simulating the atMostSeqCard constraint on the composite structures problem. The models starting with `standard` are normal models with all constraint activated. The models starting with `atMostSeqCard` are the ones that implement the table constraint that simulates the atMostSeqCard constraint.

### d2_d3_comparison

[This directory](minizinc/d2_d3_comparisond) contains the 3 configurations that were tested for the D2 and the D3 constraints. More specifically, it contains the following files:

- [`conf1.mzn`](minizinc/d2_d3_comparison/conf1.mzn) describes configuration 1, enforcing the D2 and the D3 constraints in their simplest forms.
- [`conf2.mzn`](minizinc/d2_d3_comparison/conf2.mzn) describes the D3 constraint using a table constraint, the D2 constraint remains unchanged.
- [`conf3.mzn`](minizinc/d2_d3_comparison/conf3.mzn) describes both the D2 and the D3 constraint using a table constraint.

### feasibility

In [this directory](minizinc/feasibility), all the models that simply check feasibility are stored. These are also the models that were compared against the commercial solution of our industrial partner.
More specifically, the models stored implement the following rules, in addition to the mandatory rules:

- [`model_template.mzn`](minizinc/feasibility/model_template.mzn) only implements the mandatory rules.
- [`model1.mzn`](minizinc/feasibility/model1.mzn) implements the D1-D5 rules.
- [`model2.mzn`](minizinc/feasibility/model2.mzn) implements the D1 rule.
- [`model3.mzn`](minizinc/feasibility/model3.mzn) implements the D2 constraint.
- [`model4.mzn`](minizinc/feasibility/model3.mzn) implements the D3 constraint.
- [`model5.mzn`](minizinc/feasibility/model5.mzn) implements the D4 constraint.
- [`model6.mzn`](minizinc/feasibility/model6.mzn) implements the D5 constraint.
- [`model7.mzn`](minizinc/feasibility/model7.mzn) implements the D3 and the D4 constraints.
- [`model8.mzn`](minizinc/feasibility/model8.mzn) implements the D1, D2, D3 and D5 constraints.

### solve_unfeasible

In [this directory](minizinc/solve_unfeasible), all the models that relax or remove a constraint are stored. It also contains the models that translate said removed/relaxed constraint into an objective function.
More specifically, it contains the following models:

- [`model_90gap_chuffed.mzn`](minizinc/solve_unfeasible/model_90gap_chuffed.mzn) removes the D2 constraint.
- [`model_90gap_optimize_chuffed.mzn`](minizinc/solve_unfeasible/model_90gap_optimize_chuffed.mzn) removes the D2 constraint and translates it into an objective function that minimizes the amount of 90° angle differences.
- [`model_dissymmetry_chuffed.mzn`](minizinc/solve_unfeasible/model_dissymmetry_chuffed.mzn) removes the D4 constraint.
- [`model_max4consecutive_chuffed.mzn`](minizinc/solve_unfeasible/model_max4consecutive_chuffed.mzn) relaxes the D3 constraint to allow up to 4 consecutive plies with the same angle instead of 3.
- [`model_max4consecutive_optimize_chuffed.mzn`](minizinc/solve_unfeasible/model_max4consecutive_optimize_chuffed.mzn) relaxes the D3 constraint to allow up to 4 consecutive plies with the same angle instead of 3 and translates the constraint into an objective function that minimizes the amount of sequences that contain 4 consecutive plies with the same angle.
- [`model_max5consecutive_chuffed.mzn`](minizinc/solve_unfeasible/model_max4consecutive_chuffed.mzn) relaxes the D3 constraint to allow up to 5 consecutive plies with the same angle instead of 3.
- [`model_max5consecutive_optimize_chuffed.mzn`](minizinc/solve_unfeasible/model_max4consecutive_optimize_chuffed.mzn) relaxes the D3 constraint to allow up to 5 consecutive plies with the same angle instead of 3 and translates the constraint into an objective function that minimizes the amount of sequences that contain 4 or 5 consecutive plies with the same angle.
- [`model_nocard_chuffed.mzn`](minizinc/solve_unfeasible/model_nocard_chuffed.mzn) removes the D1 constraint.
- [`model_nocard_optimize_chuffed.mzn`](minizinc/solve_unfeasible/model_nocard_optimize_chuffed.mzn) removes the D1 constraint and translates it into an objective function that tries to minimize the cardinality differences with the ones specified by the user.

## MaxiCP

This directory contains the whole [MaxiCP](https://github.com/aia-uclouvain/maxicp) solver together with the files of the composite structure problem.

The files concerning the composite structure problem are contained in the [composite  directory](maxicp/src/main/java/org/maxicp/cp/examples/raw/composite), under the path `maxicp/src/main/java/org/maxicp/cp/examples/raw/composite` and contains the following relevant files:

- [`AtMostSeqCardSim.java`](maxicp/src/main/java/org/maxicp/cp/examples/raw/composite/AtMostSeqCardSim.java) contains the model used to generate the tuples allowing us to simulate the effect of the atMostSeqCard constraint.
- [`CompositeStructureChecker.java`](maxicp/src/main/java/org/maxicp/cp/examples/raw/composite/CompositeStructureChecker.java) contains a checker class that checks which constraints are satisfied in a solution.
- [`CompositeStructureGenerator.java`](maxicp/src/main/java/org/maxicp/cp/examples/raw/composite/CompositeStructureGenerator.java), [`CompositeStructureGeneratorSymmetrical.java`](maxicp/src/main/java/org/maxicp/cp/examples/raw/composite/CompositeStructureGeneratorSymmetrical.java) and [`CompositeStructureGeneratorSymmetricalBalanced.java`](maxicp/src/main/java/org/maxicp/cp/examples/raw/composite/CompositeStructureGeneratorSymmetricalBalanced.java) contain the code that was used to generate the synthetic benchmark instances of the problem.
- [`CompositeStructureInstance.java`](maxicp/src/main/java/org/maxicp/cp/examples/raw/composite/CompositeStructureInstance.java) contains all function to parse or create an instance of the composite structure problem.
- [`CompositeStructures.java`](maxicp/src/main/java/org/maxicp/cp/examples/raw/composite/CompositeStructures.java) contains the MaxiCP model that was used to solve composite structure instances.
- [`CompositeStructureSolution.java`](maxicp/src/main/java/org/maxicp/cp/examples/raw/composite/CompositeStructureSolution.java) represents a solution to an instance and contains the function to print, parse or create a solution file.
- [`CSSearch.java`](maxicp/src/main/java/org/maxicp/cp/examples/raw/composite/CSSearch.java) contains the different variable selectors that were tested for this problem.
