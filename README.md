# Repulser GP
Adaption of Genetic Programming to limit Overfitting

## Idea

Detect overfitting solutions during execution of Genetic Programming and classify these as repulsers. 
In further generations transform standard GP into a multi-objective system, with objectives made of the fitness of the solutions and a distance metric to the repulsers.
By maximizing the distance to repulsers and still looking for improved fitness, the system should help to limit overfitting of long running executions.

