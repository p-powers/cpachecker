scripts/cpa.sh -noout -heap 2000M -preprocess -config config/unmaintained/components/kInduction/pdr.properties -setprop pdr.abstractionStrategy=ALLSAT_BASED_PREDICATE_ABSTRACTION -setprop pdr.liftingStrategy=ABSTRACTION_BASED_LIFTING -setprop pdr.invariantRefinementStrategy=UNSAT_CORE_BASED_STRENGTHENING -timelimit 60s -stats -spec sv-comp-reachability $1